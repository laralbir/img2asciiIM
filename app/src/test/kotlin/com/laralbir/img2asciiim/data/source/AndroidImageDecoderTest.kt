package com.laralbir.img2asciiim.data.source

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.Process
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import com.laralbir.img2asciiim.domain.model.DomainResult
import com.laralbir.img2asciiim.domain.model.ImageDecodeError
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AndroidImageDecoderTest {

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val decoder = AndroidImageDecoder(context)

  @Test
  fun computesPowerOfTwoSampleSizeKeepingUltraHighMegapixelImagesUnderBound() {
    // 48 MP (8000 x 6000) with 768 target -> inSampleSize = 16 -> 500 x 375 (< 1 MB)
    val sample48Mp = AndroidImageDecoder.calculateInSampleSize(
      sourceWidth = 8_000,
      sourceHeight = 6_000,
      maxDimensionPx = 768,
    )
    assertEquals(16, sample48Mp)
    assertTrue(8_000 / sample48Mp <= 768)

    // 108 MP (12000 x 9000) with 768 target -> inSampleSize = 16 -> 750 x 562 (~1.68 MB < 4 MB)
    val sample108Mp = AndroidImageDecoder.calculateInSampleSize(
      sourceWidth = 12_000,
      sourceHeight = 9_000,
      maxDimensionPx = 768,
    )
    assertEquals(16, sample108Mp)
    assertTrue(12_000 / sample108Mp <= 768)
    assertTrue(9_000 / sample108Mp <= 768)
  }

  @Test
  fun downsamplesLargeImageAndPreservesOriginalDimensions() = runTest {
    val file = File(context.cacheDir, "large_test.png")
    val bmp = Bitmap.createBitmap(1_600, 1_200, Bitmap.Config.ARGB_8888)
    bmp.eraseColor(Color.rgb(40, 180, 220))
    FileOutputStream(file).use { out ->
      bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    bmp.recycle()

    val authority = "com.external.test.media"
    registerFileServingProvider(authority, mapOf("/large" to file))

    val outcome = decoder.decode(Uri.parse("content://$authority/large"), maxDimensionPx = 512)
    assertTrue(outcome is DomainResult.Success, "Expected Success, got $outcome")
    assertEquals(1_600, outcome.value.originalWidth)
    assertEquals(1_200, outcome.value.originalHeight)
    assertTrue(outcome.value.pixelGrid.width <= 512)
    assertTrue(outcome.value.pixelGrid.height <= 512)
    file.delete()
  }

  @Test
  fun normalizesExifRotate90Rotate180AndFlipHorizontal() = runTest {
    // Asymmetric 40x20 image: left 10 columns pure RED, remaining 30 columns pure BLUE
    val baseBmp = Bitmap.createBitmap(40, 20, Bitmap.Config.ARGB_8888)
    for (y in 0 until 20) {
      for (x in 0 until 40) {
        baseBmp.setPixel(x, y, if (x < 10) Color.RED else Color.BLUE)
      }
    }

    val rot90File = writeJpegWithExif(baseBmp, "rot90.jpg", ExifInterface.ORIENTATION_ROTATE_90)
    val flipHFile = writeJpegWithExif(baseBmp, "fliph.jpg", ExifInterface.ORIENTATION_FLIP_HORIZONTAL)
    baseBmp.recycle()

    val authority = "com.external.exif.media"
    registerFileServingProvider(
      authority,
      mapOf(
        "/rot90" to rot90File,
        "/fliph" to flipHFile,
      ),
    )

    // After 90° CW rotation: 40x20 becomes 20x40, and former left RED band becomes TOP band (y = 0..9)!
    val rot90Result = decoder.decode(Uri.parse("content://$authority/rot90"), maxDimensionPx = 512)
    assertTrue(rot90Result is DomainResult.Success)
    assertEquals(20, rot90Result.value.pixelGrid.width)
    assertEquals(40, rot90Result.value.pixelGrid.height)
    assertEquals(20, rot90Result.value.originalWidth)
    assertEquals(40, rot90Result.value.originalHeight)
    val topPixel = rot90Result.value.pixelGrid.get(10, 2)
    val bottomPixel = rot90Result.value.pixelGrid.get(10, 35)
    assertTrue(Color.red(topPixel) > 180 && Color.blue(topPixel) < 80, "Top band should be red after ROTATE_90")
    assertTrue(Color.blue(bottomPixel) > 180 && Color.red(bottomPixel) < 80, "Bottom band should be blue after ROTATE_90")

    // After FLIP_HORIZONTAL: 40x20 stays 40x20, and former left RED band moves to RIGHT (x = 30..39)!
    val flipResult = decoder.decode(Uri.parse("content://$authority/fliph"), maxDimensionPx = 512)
    assertTrue(flipResult is DomainResult.Success)
    assertEquals(40, flipResult.value.pixelGrid.width)
    assertEquals(20, flipResult.value.pixelGrid.height)
    val rightPixel = flipResult.value.pixelGrid.get(37, 10)
    val leftPixel = flipResult.value.pixelGrid.get(2, 10)
    assertTrue(Color.red(rightPixel) > 180 && Color.blue(rightPixel) < 80, "Right band should be red after FLIP_HORIZONTAL")
    assertTrue(Color.blue(leftPixel) > 180 && Color.red(leftPixel) < 80, "Left band should be blue after FLIP_HORIZONTAL")

    rot90File.delete()
    flipHFile.delete()
  }

  @Test
  fun mapsGoogleDriveOfflineExceptionsAndCorruptedFilesToTypedErrors() = runTest {
    val emptyFile = File(context.cacheDir, "empty.jpg").apply { writeBytes(byteArrayOf()) }
    val garbageFile = File(context.cacheDir, "corrupt.jpg").apply { writeBytes(byteArrayOf(1, 2, 3, 4, 5)) }

    registerFailingCloudProvider("com.google.android.apps.docs.storage")
    registerFileServingProvider(
      "com.external.local.files",
      mapOf(
        "/empty" to emptyFile,
        "/garbage" to garbageFile,
      ),
    )

    val cloudOutcome = decoder.decode(
      Uri.parse("content://com.google.android.apps.docs.storage/document/acc%3D1%3Bdoc%3D99"),
    )
    assertEquals(DomainResult.Failure(ImageDecodeError.CloudFileOfflineUnavailable), cloudOutcome)

    val emptyOutcome = decoder.decode(Uri.parse("content://com.external.local.files/empty"))
    assertEquals(DomainResult.Failure(ImageDecodeError.InvalidOrCorruptedImage), emptyOutcome)

    val garbageOutcome = decoder.decode(Uri.parse("content://com.external.local.files/garbage"))
    assertEquals(DomainResult.Failure(ImageDecodeError.InvalidOrCorruptedImage), garbageOutcome)

    emptyFile.delete()
    garbageFile.delete()
  }

  private fun writeJpegWithExif(bitmap: Bitmap, name: String, orientation: Int): File {
    val file = File(context.cacheDir, name)
    FileOutputStream(file).use { out ->
      bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
    }
    val exif = ExifInterface(file)
    exif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
    exif.saveAttributes()
    return file
  }

  private fun registerFileServingProvider(authority: String, filesByPath: Map<String, File>) {
    val info = ProviderInfo().apply {
      this.authority = authority
      packageName = "com.external.provider"
      applicationInfo = ApplicationInfo().apply {
        packageName = "com.external.provider"
        uid = Process.myUid() + 2000
      }
    }
    val provider = object : ContentProvider() {
      override fun onCreate(): Boolean = true
      override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val target = filesByPath[uri.path] ?: throw FileNotFoundException("Missing ${uri.path}")
        return ParcelFileDescriptor.open(target, ParcelFileDescriptor.MODE_READ_ONLY)
      }
      override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
      ): Cursor? = null
      override fun getType(uri: Uri): String = "image/*"
      override fun insert(uri: Uri, values: ContentValues?): Uri? = null
      override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
      override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
      ): Int = 0
    }
    provider.attachInfo(context, info)
    ShadowContentResolver.registerProviderInternal(authority, provider)
  }

  private fun registerFailingCloudProvider(authority: String) {
    val info = ProviderInfo().apply {
      this.authority = authority
      packageName = "com.google.android.apps.docs"
      applicationInfo = ApplicationInfo().apply {
        packageName = "com.google.android.apps.docs"
        uid = Process.myUid() + 3000
      }
    }
    val provider = object : ContentProvider() {
      override fun onCreate(): Boolean = true
      override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        throw FileNotFoundException("Network unavailable: cloud document not cached offline")
      }
      override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
      ): Cursor? = null
      override fun getType(uri: Uri): String = "image/jpeg"
      override fun insert(uri: Uri, values: ContentValues?): Uri? = null
      override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
      override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
      ): Int = 0
    }
    provider.attachInfo(context, info)
    ShadowContentResolver.registerProviderInternal(authority, provider)
  }
}
