package com.laralbir.img2asciiim.data.source

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.ProviderInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Process
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import com.laralbir.img2asciiim.R
import com.laralbir.img2asciiim.domain.model.DomainResult
import com.laralbir.img2asciiim.domain.model.ImageDecodeError
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CameraCaptureFileManagerTest {

  private val context: Context = ApplicationProvider.getApplicationContext()

  @BeforeTest
  fun setUpFileProviderRegistration() {
    val authority = "${context.packageName}.fileprovider"
    val providerInfo = ProviderInfo().apply {
      this.authority = authority
      name = FileProvider::class.java.name
      packageName = context.packageName
      exported = false
      grantUriPermissions = true
      applicationInfo = ApplicationInfo(context.applicationInfo).apply {
        uid = Process.myUid()
      }
      metaData = Bundle().apply {
        putInt("android.support.FILE_PROVIDER_PATHS", R.xml.file_paths)
      }
    }
    shadowOf(context.packageManager).addOrUpdateProvider(providerInfo)
    val provider = FileProvider()
    provider.attachInfo(context, providerInfo)
    ShadowContentResolver.registerProviderInternal(authority, provider)
  }

  @Test
  fun filePathsXmlEnforcesLeastPrivilegeCameraCapturesCacheSubdirectoryOnly() {
    val xmlFile = File("src/main/res/xml/file_paths.xml")
    assertTrue(xmlFile.exists(), "Expected file_paths.xml at ${xmlFile.absolutePath}")
    val xmlText = xmlFile.readText()
    assertTrue(xmlText.contains("<cache-path"))
    assertTrue(xmlText.contains("camera_captures/"))
    assertFalse(xmlText.contains("<root-path"), "root-path is strictly forbidden")
    assertFalse(xmlText.contains("<external-path"), "external-path is strictly forbidden")
    assertFalse(xmlText.contains("path=\".\""), "wildcard '.' path is strictly forbidden")
  }

  @Test
  fun mintsUuidJpgUriPrunesStaleCapturesAndIntegratesWithRepositorySecurityModel() = runTest {
    var fakeNowMillis = 1_000_000L
    val manager = CameraCaptureFileManager(context = context, clock = { fakeNowMillis })
    val repository = AndroidImageSourceRepository(
      context = context,
      cameraCaptureFileManager = manager,
    )

    // Create first capture and write a valid image into it
    val firstTarget = manager.createCaptureTarget()
    assertTrue(firstTarget.file.exists())
    assertEquals("camera_captures", firstTarget.file.parentFile?.name)
    assertTrue(firstTarget.file.name.endsWith(".jpg"))
    assertEquals("content", firstTarget.uri.scheme)
    assertEquals("${context.packageName}.fileprovider", firstTarget.uri.authority)
    val bmp = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
    bmp.eraseColor(Color.WHITE)
    FileOutputStream(firstTarget.file).use { out ->
      bmp.compress(Bitmap.CompressFormat.JPEG, 95, out)
    }
    bmp.recycle()
    firstTarget.file.setLastModified(fakeNowMillis)

    // Minted camera capture URI can be decoded by our own user flow
    val cameraDecodeOutcome = repository.decodeFromUri(firstTarget.uri.toString())
    assertTrue(cameraDecodeOutcome is DomainResult.Success, "Expected Success, got $cameraDecodeOutcome")
    assertEquals(24, cameraDecodeOutcome.value.pixelGrid.width)

    // Unminted URI under our own FileProvider authority MUST be blocked as SecurityViolation
    val spoofedInternalUri = "content://${context.packageName}.fileprovider/camera_captures/not_minted.jpg"
    assertEquals(
      DomainResult.Failure(ImageDecodeError.SecurityViolation),
      repository.decodeFromUri(spoofedInternalUri),
    )

    // Advance clock past max age -> creating a new capture prunes the stale one!
    fakeNowMillis += CameraCaptureFileManager.DEFAULT_MAX_AGE_MILLIS + 5_000L
    val secondTarget = manager.createCaptureTarget()
    assertFalse(firstTarget.file.exists(), "Stale capture file must be pruned")
    assertTrue(secondTarget.file.exists())

    // Cancelled capture cleans up immediately
    manager.onCaptureCompleted(secondTarget.uri, captured = false)
    assertFalse(secondTarget.file.exists(), "Cancelled capture temp file must be deleted")
  }
}
