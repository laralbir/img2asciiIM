package com.laralbir.img2asciiim.data.source

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.laralbir.img2asciiim.domain.model.DecodedImage
import com.laralbir.img2asciiim.domain.model.DomainResult
import com.laralbir.img2asciiim.domain.model.ImageDecodeError
import com.laralbir.img2asciiim.domain.model.PixelGrid
import com.laralbir.img2asciiim.domain.repository.ImageSourceRepository
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Memory-bounded (`< 4 MB` RAM even on 108 MP camera photos) image & EXIF decoder.
 * Spools single-shot SAF/cloud streams once into a private temporary file (cleaned up in `finally`)
 * so mid-stream cloud `IOException`s are never swallowed by `BitmapFactory` and EXIF + bounds +
 * `inSampleSize` passes operate reliably on piped providers like Google Drive.
 */
class AndroidImageDecoder(
  private val context: Context,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

  suspend fun decode(
    uri: Uri,
    maxDimensionPx: Int = ImageSourceRepository.DEFAULT_MAX_DIMENSION_PX,
  ): DomainResult<DecodedImage, ImageDecodeError> = withContext(ioDispatcher) {
    val spoolDir = File(context.cacheDir, SPOOL_SUBDIR).apply { mkdirs() }
    val spoolFile = File(spoolDir, "decode_${UUID.randomUUID()}.tmp")

    try {
      val copiedBytes = spoolUriToTempFile(uri, spoolFile)
      if (copiedBytes <= 0L) {
        return@withContext DomainResult.Failure(ImageDecodeError.InvalidOrCorruptedImage)
      }

      val boundsOptions = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
      }
      BitmapFactory.decodeFile(spoolFile.absolutePath, boundsOptions)
      val rawWidth = boundsOptions.outWidth
      val rawHeight = boundsOptions.outHeight
      if (rawWidth <= 0 || rawHeight <= 0) {
        return@withContext DomainResult.Failure(ImageDecodeError.InvalidOrCorruptedImage)
      }

      val exifOrientation = readExifOrientation(spoolFile)
      val sampleSize = calculateInSampleSize(
        sourceWidth = rawWidth,
        sourceHeight = rawHeight,
        maxDimensionPx = maxDimensionPx,
      )

      val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
      }
      val decodedBitmap = BitmapFactory.decodeFile(spoolFile.absolutePath, decodeOptions)
        ?: return@withContext DomainResult.Failure(ImageDecodeError.InvalidOrCorruptedImage)

      val argbBitmap = if (decodedBitmap.config != Bitmap.Config.ARGB_8888) {
        val converted = decodedBitmap.copy(Bitmap.Config.ARGB_8888, false)
        decodedBitmap.recycle()
        converted ?: return@withContext DomainResult.Failure(ImageDecodeError.InvalidOrCorruptedImage)
      } else {
        decodedBitmap
      }

      val orientedBitmap = applyExifOrientation(argbBitmap, exifOrientation)
      val width = orientedBitmap.width
      val height = orientedBitmap.height
      val pixels = IntArray(width * height)
      orientedBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
      orientedBitmap.recycle()

      val swapsAxes = exifSwapsDimensions(exifOrientation)
      val normalizedOrigWidth = if (swapsAxes) rawHeight else rawWidth
      val normalizedOrigHeight = if (swapsAxes) rawWidth else rawHeight

      DomainResult.Success(
        DecodedImage(
          pixelGrid = PixelGrid(width = width, height = height, argbPixels = pixels),
          originalWidth = normalizedOrigWidth,
          originalHeight = normalizedOrigHeight,
        ),
      )
    } catch (_: SecurityException) {
      DomainResult.Failure(ImageDecodeError.SecurityViolation)
    } catch (e: FileNotFoundException) {
      DomainResult.Failure(mapStreamException(uri, e))
    } catch (e: IOException) {
      DomainResult.Failure(mapStreamException(uri, e))
    } finally {
      spoolFile.delete()
    }
  }

  private fun spoolUriToTempFile(uri: Uri, destination: File): Long {
    val input = context.contentResolver.openInputStream(uri)
      ?: if (isCloudAuthority(uri.authority)) {
        throw IOException("Cloud provider returned null stream while offline")
      } else {
        throw FileNotFoundException("Unable to open stream for $uri")
      }

    return input.use { stream ->
      FileOutputStream(destination).use { out ->
        val buffer = ByteArray(SPOOL_BUFFER_BYTES)
        var total = 0L
        while (true) {
          val read = stream.read(buffer)
          if (read < 0) break
          if (read == 0) continue
          total += read
          if (total > MAX_SPOOL_BYTES) {
            throw IOException("Image stream exceeds safety cap of $MAX_SPOOL_BYTES bytes")
          }
          out.write(buffer, 0, read)
        }
        total
      }
    }
  }

  private fun readExifOrientation(file: File): Int = try {
    ExifInterface(file).getAttributeInt(
      ExifInterface.TAG_ORIENTATION,
      ExifInterface.ORIENTATION_NORMAL,
    )
  } catch (_: IOException) {
    ExifInterface.ORIENTATION_NORMAL
  }

  private fun applyExifOrientation(source: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
      ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
      ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
      ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
      ExifInterface.ORIENTATION_TRANSPOSE -> {
        matrix.setRotate(90f)
        matrix.postScale(-1f, 1f)
      }
      ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
      ExifInterface.ORIENTATION_TRANSVERSE -> {
        matrix.setRotate(270f)
        matrix.postScale(-1f, 1f)
      }
      ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
      else -> return source
    }

    val transformed = Bitmap.createBitmap(
      source,
      0,
      0,
      source.width,
      source.height,
      matrix,
      true,
    )
    if (transformed !== source) {
      source.recycle()
    }
    return transformed
  }

  private fun exifSwapsDimensions(orientation: Int): Boolean = when (orientation) {
    ExifInterface.ORIENTATION_TRANSPOSE,
    ExifInterface.ORIENTATION_ROTATE_90,
    ExifInterface.ORIENTATION_TRANSVERSE,
    ExifInterface.ORIENTATION_ROTATE_270 -> true
    else -> false
  }

  private fun mapStreamException(uri: Uri, error: IOException): ImageDecodeError {
    if (isCloudAuthority(uri.authority) || isOfflineNetworkFailure(error)) {
      return ImageDecodeError.CloudFileOfflineUnavailable
    }
    return ImageDecodeError.IoFailure(error.message)
  }

  private fun isCloudAuthority(authority: String?): Boolean {
    if (authority.isNullOrBlank()) return false
    val lower = authority.lowercase()
    return CLOUD_AUTHORITY_MARKERS.any { marker -> lower.contains(marker) }
  }

  private fun isOfflineNetworkFailure(error: IOException): Boolean {
    if (error is UnknownHostException || error is SocketTimeoutException || error is ConnectException) {
      return true
    }
    val msg = error.message?.lowercase().orEmpty()
    return OFFLINE_MESSAGE_MARKERS.any { marker -> msg.contains(marker) }
  }

  companion object {
    private const val SPOOL_SUBDIR: String = "incoming_images"
    private const val SPOOL_BUFFER_BYTES: Int = 64 * 1024
    private const val MAX_SPOOL_BYTES: Long = 64L * 1024L * 1024L

    private val CLOUD_AUTHORITY_MARKERS = listOf(
      "com.google.android.apps.docs",
      "com.google.android.apps.photos",
      "com.microsoft.skydrive",
      "com.dropbox.",
      "org.nextcloud",
      "com.box.android",
      "cloud",
      "drive",
    )

    private val OFFLINE_MESSAGE_MARKERS = listOf(
      "offline",
      "network",
      "unreachable",
      "timed out",
      "timeout",
      "remote",
    )

    /**
     * Computes a power-of-two `inSampleSize` so both decoded bitmap dimensions stay `<= maxDimensionPx`.
     */
    fun calculateInSampleSize(
      sourceWidth: Int,
      sourceHeight: Int,
      maxDimensionPx: Int = ImageSourceRepository.DEFAULT_MAX_DIMENSION_PX,
    ): Int {
      if (sourceWidth <= 0 || sourceHeight <= 0) return 1
      val targetBound = maxDimensionPx.coerceAtLeast(1)
      var sampleSize = 1
      while ((sourceWidth / sampleSize) > targetBound || (sourceHeight / sampleSize) > targetBound) {
        sampleSize *= 2
      }
      return sampleSize
    }
  }
}
