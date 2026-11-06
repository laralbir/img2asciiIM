package com.laralbir.img2asciiim.ui.branding

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.laralbir.img2asciiim.R
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class BrandAssetsAndSplashRobolectricTest {

  private val context: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun testAdaptiveLauncherIconResolvesWithLayers() {
    val launcherDrawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
    assertNotNull(launcherDrawable, "ic_launcher must resolve as drawable")
    assertTrue(
      launcherDrawable is AdaptiveIconDrawable,
      "ic_launcher must be an AdaptiveIconDrawable on API 26+",
    )

    assertNotNull(launcherDrawable.background, "ic_launcher background layer must not be null")
    assertNotNull(launcherDrawable.foreground, "ic_launcher foreground layer must not be null")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      assertNotNull(launcherDrawable.monochrome, "ic_launcher monochrome layer must not be null on API 33+")
    }
  }

  @Test
  fun testSplashLogoBaseResolvesAsVectorDrawable() {
    val splashBase = ContextCompat.getDrawable(context, R.drawable.ic_splash_logo_base)
    assertNotNull(splashBase, "ic_splash_logo_base must resolve")
    assertTrue(splashBase is VectorDrawable, "ic_splash_logo_base must be a VectorDrawable")
    assertEquals(240, splashBase.intrinsicWidth)
    assertEquals(240, splashBase.intrinsicHeight)
  }

  @Test
  fun testSplashBrandingResolvesAsVectorDrawable() {
    val branding = ContextCompat.getDrawable(context, R.drawable.ic_splash_branding)
    assertNotNull(branding, "ic_splash_branding must resolve")
    assertTrue(branding is VectorDrawable, "ic_splash_branding must be a VectorDrawable")
    assertEquals(200, branding.intrinsicWidth)
    assertEquals(80, branding.intrinsicHeight)
  }

  @Test
  fun testMasterBrandAssetsExistAndHaveValidDimensions() {
    val candidatesIcon = listOf(File("docs/assets/icon-512.png"), File("../docs/assets/icon-512.png"))
    val icon512File = candidatesIcon.firstOrNull { it.exists() }
    assertNotNull(icon512File, "docs/assets/icon-512.png must exist")
    val (iconW, iconH) = readPngDimensions(icon512File)
    assertEquals(512, iconW)
    assertEquals(512, iconH)

    val candidatesFeature = listOf(
      File("docs/assets/launch-feature-1024x500.png"),
      File("../docs/assets/launch-feature-1024x500.png"),
    )
    val featureFile = candidatesFeature.firstOrNull { it.exists() }
    assertNotNull(featureFile, "docs/assets/launch-feature-1024x500.png must exist")
    val (featureW, featureH) = readPngDimensions(featureFile)
    assertEquals(1024, featureW)
    assertEquals(500, featureH)
  }

  private fun readPngDimensions(file: File): Pair<Int, Int> {
    file.inputStream().use { stream ->
      val header = ByteArray(24)
      val read = stream.read(header)
      assertTrue(read >= 24, "File too small to be a PNG")
      val width = ((header[16].toInt() and 0xFF) shl 24) or
        ((header[17].toInt() and 0xFF) shl 16) or
        ((header[18].toInt() and 0xFF) shl 8) or
        (header[19].toInt() and 0xFF)
      val height = ((header[20].toInt() and 0xFF) shl 24) or
        ((header[21].toInt() and 0xFF) shl 16) or
        ((header[22].toInt() and 0xFF) shl 8) or
        (header[23].toInt() and 0xFF)
      return Pair(width, height)
    }
  }
}
