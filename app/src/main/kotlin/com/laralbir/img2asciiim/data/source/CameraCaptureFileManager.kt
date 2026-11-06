package com.laralbir.img2asciiim.data.source

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class CameraCaptureTarget(
  val file: File,
  val uri: Uri,
)

/**
 * Manages temporary `.jpg` files inside `cacheDir/camera_captures` scoped strictly to
 * `@xml/file_paths`, revoking temporary URI permissions after capture and pruning stale captures.
 */
class CameraCaptureFileManager(
  private val context: Context,
  private val clock: () -> Long = System::currentTimeMillis,
) {

  private val mintedUris: MutableSet<String> = ConcurrentHashMap.newKeySet()
  private val filesByUri: ConcurrentHashMap<String, File> = ConcurrentHashMap()

  val authority: String
    get() = "${context.packageName}$FILE_PROVIDER_SUFFIX"

  fun createCaptureTarget(): CameraCaptureTarget {
    pruneStaleCaptures()
    val captureDir = File(context.cacheDir, CAPTURE_SUBDIR).apply { mkdirs() }
    val captureFile = File(captureDir, "capture_${UUID.randomUUID()}.jpg").apply {
      createNewFile()
    }
    val uri = FileProvider.getUriForFile(context, authority, captureFile)
    val key = uri.toString()
    mintedUris.add(key)
    filesByUri[key] = captureFile
    return CameraCaptureTarget(file = captureFile, uri = uri)
  }

  fun isMintedCaptureUri(uri: Uri): Boolean = mintedUris.contains(uri.toString())

  fun onCaptureCompleted(uri: Uri, captured: Boolean) {
    revokeTemporaryPermission(uri)
    if (!captured) {
      deleteCaptureFile(uri)
    }
  }

  fun revokeTemporaryPermission(uri: Uri) {
    try {
      context.revokeUriPermission(
        uri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
      )
    } catch (_: RuntimeException) {
      // Safe no-op if platform already cleared temporary URI grant
    }
  }

  fun deleteCaptureFile(uri: Uri) {
    val key = uri.toString()
    mintedUris.remove(key)
    filesByUri.remove(key)?.delete()
  }

  fun pruneStaleCaptures(maxAgeMillis: Long = DEFAULT_MAX_AGE_MILLIS): Int {
    val captureDir = File(context.cacheDir, CAPTURE_SUBDIR)
    if (!captureDir.exists()) return 0

    val now = clock()
    var deletedCount = 0
    val files = captureDir.listFiles().orEmpty()
    for (file in files) {
      if (file.isFile && (now - file.lastModified()) >= maxAgeMillis) {
        if (file.delete()) {
          deletedCount++
          filesByUri.entries.removeIf { entry ->
            val matches = entry.value == file
            if (matches) mintedUris.remove(entry.key)
            matches
          }
        }
      }
    }
    return deletedCount
  }

  companion object {
    const val CAPTURE_SUBDIR: String = "camera_captures"
    const val FILE_PROVIDER_SUFFIX: String = ".fileprovider"
    const val DEFAULT_MAX_AGE_MILLIS: Long = 30L * 60L * 1_000L // 30 minutes
  }
}
