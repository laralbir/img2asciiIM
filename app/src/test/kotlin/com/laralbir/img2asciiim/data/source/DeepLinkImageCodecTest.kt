package com.laralbir.img2asciiim.data.source

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.util.Base64
import com.laralbir.img2asciiim.domain.model.DomainResult
import com.laralbir.img2asciiim.domain.model.ImageDecodeError
import com.laralbir.img2asciiim.domain.model.ImPlatformPreset
import com.laralbir.img2asciiim.domain.model.PixelGrid
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DeepLinkImageCodecTest {

  private val codec = DeepLinkImageCodec()

  private fun createSampleGrid(width: Int = 1000, height: Int = 750): PixelGrid {
    val pixels = IntArray(width * height) { idx ->
      val x = idx % width
      val y = idx / width
      val r = ((x * 255) / width).coerceIn(0, 255)
      val g = ((y * 255) / height).coerceIn(0, 255)
      val b = ((x + y) % 256).coerceIn(0, 255)
      Color.rgb(r, g, b)
    }
    return PixelGrid(width = width, height = height, argbPixels = pixels)
  }

  @Test
  fun resolvesCorrectQualityProfilesForModesAndPlatforms() {
    val standardProfile = codec.resolveProfile(DeepLinkMode.STANDARD, ImPlatformPreset.WHATSAPP)
    assertEquals(320, standardProfile.maxDimension)
    assertEquals(60, standardProfile.quality)

    val whatsAppHdProfile = codec.resolveProfile(DeepLinkMode.EXPANDED_HD, ImPlatformPreset.WHATSAPP)
    assertEquals(720, whatsAppHdProfile.maxDimension)
    assertEquals(75, whatsAppHdProfile.quality)

    val telegramHdProfile = codec.resolveProfile(DeepLinkMode.EXPANDED_HD, ImPlatformPreset.TELEGRAM)
    assertEquals(200, telegramHdProfile.maxDimension)
    assertEquals(45, telegramHdProfile.quality)
  }

  @Test
  fun expandedModeWhatsAppGeneratesValidLinkAndRespectsBudget() {
    val grid = createSampleGrid(width = 1200, height = 900)

    val deepLink = codec.encodeToDeepLink(
      pixelGrid = grid,
      mode = DeepLinkMode.EXPANDED_HD,
      preset = ImPlatformPreset.WHATSAPP,
    )

    assertTrue(deepLink.startsWith(DeepLinkImageCodec.DEEP_LINK_BASE + "#d="))
    assertFalse(deepLink.contains("\n"))
    assertFalse(deepLink.contains(" "))

    // WhatsApp expanded HD budget is 60,000 characters
    assertTrue(
      deepLink.length <= DeepLinkImageCodec.WHATSAPP_EXPANDED_BUDGET,
      "WhatsApp link exceeded max character budget: ${deepLink.length} > ${DeepLinkImageCodec.WHATSAPP_EXPANDED_BUDGET}",
    )
    // Payload should be rich/HD (> 3,000 Base64 characters)
    assertTrue(deepLink.length > 3_000, "WhatsApp HD payload unexpectedly small: ${deepLink.length}")

    // Verify round-trip decoding
    val outcome = codec.decodeFromDeepLink(deepLink)
    assertTrue(outcome is DomainResult.Success)
    val decoded = outcome.value
    assertEquals(720, decoded.originalWidth)
    assertEquals(540, decoded.originalHeight)
    assertEquals(720, decoded.pixelGrid.width)
    assertEquals(540, decoded.pixelGrid.height)
  }

  @Test
  fun expandedModeTelegramGeneratesValidLinkAndStrictlyRespects3900CharBudget() {
    val grid = createSampleGrid(width = 1200, height = 900)

    val deepLink = codec.encodeToDeepLink(
      pixelGrid = grid,
      mode = DeepLinkMode.EXPANDED_HD,
      preset = ImPlatformPreset.TELEGRAM,
    )

    assertTrue(deepLink.startsWith(DeepLinkImageCodec.DEEP_LINK_BASE + "#d="))
    assertFalse(deepLink.contains("\n"))
    assertFalse(deepLink.contains(" "))

    // Telegram budget is strictly 3,900 characters (leaving safety margin for 4,096 char limit)
    assertTrue(
      deepLink.length <= DeepLinkImageCodec.TELEGRAM_BUDGET,
      "Telegram link exceeded max character budget: ${deepLink.length} > ${DeepLinkImageCodec.TELEGRAM_BUDGET}",
    )
    assertTrue(deepLink.length <= 3_900)

    // Verify round-trip decoding
    val outcome = codec.decodeFromDeepLink(deepLink)
    assertTrue(outcome is DomainResult.Success)
    val decoded = outcome.value
    assertEquals(200, decoded.originalWidth)
    assertEquals(150, decoded.originalHeight)
    assertEquals(200, decoded.pixelGrid.width)
    assertEquals(150, decoded.pixelGrid.height)
  }

  @Test
  fun expandedModeTelegramStrictlyRespectsBudgetEvenWithHighEntropyNoise() {
    // Generate high-entropy pseudo-random pixels to stress-test compression
    val width = 400
    val height = 400
    val noisyPixels = IntArray(width * height) { idx ->
      val hash = (idx * 1664525 + 1013904223)
      Color.rgb((hash shr 16) and 0xFF, (hash shr 8) and 0xFF, hash and 0xFF)
    }
    val noisyGrid = PixelGrid(width = width, height = height, argbPixels = noisyPixels)

    val deepLink = codec.encodeToDeepLink(
      pixelGrid = noisyGrid,
      mode = DeepLinkMode.EXPANDED_HD,
      preset = ImPlatformPreset.TELEGRAM,
    )

    assertTrue(deepLink.startsWith(DeepLinkImageCodec.DEEP_LINK_BASE + "#d="))
    assertTrue(
      deepLink.length <= DeepLinkImageCodec.TELEGRAM_BUDGET,
      "Telegram noisy image link exceeded budget: ${deepLink.length} > ${DeepLinkImageCodec.TELEGRAM_BUDGET}",
    )
    assertTrue(deepLink.length <= 3_900)

    val outcome = codec.decodeFromDeepLink(deepLink)
    assertTrue(outcome is DomainResult.Success)
    assertTrue(outcome.value.pixelGrid.width <= 200)
    assertTrue(outcome.value.pixelGrid.height <= 200)
  }

  @Test
  fun standardModeScalesTo320PxMaxDimensionAndRespectsStandardBudget() {
    val grid = createSampleGrid(width = 800, height = 600)

    val deepLink = codec.encodeToDeepLink(
      pixelGrid = grid,
      mode = DeepLinkMode.STANDARD,
      preset = ImPlatformPreset.WHATSAPP,
    )

    assertTrue(deepLink.startsWith(DeepLinkImageCodec.DEEP_LINK_BASE + "#d="))
    assertTrue(
      deepLink.length <= DeepLinkImageCodec.WHATSAPP_STANDARD_BUDGET,
      "Standard mode link exceeded budget: ${deepLink.length} > ${DeepLinkImageCodec.WHATSAPP_STANDARD_BUDGET}",
    )

    val outcome = codec.decodeFromDeepLink(deepLink)
    assertTrue(outcome is DomainResult.Success)
    val decoded = outcome.value
    assertEquals(320, decoded.originalWidth)
    assertEquals(240, decoded.originalHeight)
  }

  @Test
  fun encodesPixelGridToCompactBase64UrlSafeDeepLink() {
    val pixels = IntArray(64 * 48) { idx ->
      val x = idx % 64
      val y = idx / 64
      Color.rgb((x * 4) % 256, (y * 5) % 256, 128)
    }
    val grid = PixelGrid(width = 64, height = 48, argbPixels = pixels)

    val deepLink = codec.encodeToDeepLink(grid, maxDimension = 64, quality = 60)

    assertTrue(deepLink.startsWith(DeepLinkImageCodec.DEEP_LINK_BASE + "#d="))
    assertFalse(deepLink.contains("\n"))
    assertFalse(deepLink.contains(" "))
    // Check reasonable size (< 6 KB for a 64x48 thumbnail)
    assertTrue(deepLink.length < 6_000, "DeepLink too long: ${deepLink.length}")
  }

  @Test
  fun decodesValidDeepLinkBackToPixelGrid() {
    val pixels = IntArray(32 * 32) { Color.BLUE }
    val grid = PixelGrid(width = 32, height = 32, argbPixels = pixels)
    val deepLink = codec.encodeToDeepLink(grid, maxDimension = 32, quality = 80)

    val outcome = codec.decodeFromDeepLink(deepLink)
    assertTrue(outcome is DomainResult.Success)
    val decoded = outcome.value
    assertEquals(32, decoded.originalWidth)
    assertEquals(32, decoded.originalHeight)
    assertEquals(32, decoded.pixelGrid.width)
    assertEquals(32, decoded.pixelGrid.height)
    assertNotNull(decoded.label)
  }

  @Test
  fun adaptiveResizingStrictlyRespectsExplicitMaxCharacterBudget() {
    val grid = createSampleGrid(width = 1600, height = 1200)
    val strictBudget = 2_500

    val deepLink = codec.encodeToDeepLink(
      pixelGrid = grid,
      mode = DeepLinkMode.EXPANDED_HD,
      preset = ImPlatformPreset.WHATSAPP,
      maxCharacterBudget = strictBudget,
    )

    assertTrue(deepLink.startsWith(DeepLinkImageCodec.DEEP_LINK_BASE + "#d="))
    assertTrue(
      deepLink.length <= strictBudget,
      "Deep link length ${deepLink.length} exceeded strict maxCharacterBudget $strictBudget",
    )

    val outcome = codec.decodeFromDeepLink(deepLink)
    assertTrue(outcome is DomainResult.Success)
    val decoded = outcome.value
    assertTrue(decoded.originalWidth > 0)
    assertTrue(decoded.originalHeight > 0)
    assertTrue(decoded.originalWidth <= 720)
  }

  @Test
  fun decodesLegacyJpegImagesCorrectly() {
    val width = 40
    val height = 40
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    for (x in 0 until width) {
      for (y in 0 until height) {
        bitmap.setPixel(x, y, Color.rgb(x * 6, y * 6, 128))
      }
    }

    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
    val jpegBytes = stream.toByteArray()
    val base64Jpeg = Base64.encodeToString(
      jpegBytes,
      Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
    )

    // Test legacy `#data=` format
    val legacyDataUrl = "${DeepLinkImageCodec.DEEP_LINK_BASE}#data=$base64Jpeg"
    val outcomeData = codec.decodeFromDeepLink(legacyDataUrl)
    assertTrue(outcomeData is DomainResult.Success, "Failed to decode legacy #data= JPEG deep link")
    assertEquals(width, outcomeData.value.originalWidth)
    assertEquals(height, outcomeData.value.originalHeight)

    // Test legacy `?data=` format
    val legacyQueryDataUrl = "${DeepLinkImageCodec.DEEP_LINK_BASE}?data=$base64Jpeg"
    val outcomeQuery = codec.decodeFromDeepLink(legacyQueryDataUrl)
    assertTrue(outcomeQuery is DomainResult.Success, "Failed to decode legacy ?data= JPEG deep link")
    assertEquals(width, outcomeQuery.value.originalWidth)
  }

  @Test
  fun decodesAllSupportedPrefixFormats() {
    val pixels = IntArray(20 * 20) { Color.GREEN }
    val grid = PixelGrid(width = 20, height = 20, argbPixels = pixels)
    val standardUrl = codec.encodeToDeepLink(grid, maxDimension = 20, quality = 80)
    val payload = standardUrl.substringAfter("#d=")

    val prefixes = listOf("#d=", "#data=", "?d=", "?data=")
    for (prefix in prefixes) {
      val testUrl = "${DeepLinkImageCodec.DEEP_LINK_BASE}$prefix$payload"
      val result = codec.decodeFromDeepLink(testUrl)
      assertTrue(result is DomainResult.Success, "Failed to decode prefix format '$prefix'")
      assertEquals(20, result.value.originalWidth)
      assertEquals(20, result.value.originalHeight)
    }
  }

  @Test
  fun decodesCustomUriSchemesWithQueryAndHashPayload() {
    val pixels = IntArray(20 * 20) { Color.MAGENTA }
    val grid = PixelGrid(width = 20, height = 20, argbPixels = pixels)
    val standardUrl = codec.encodeToDeepLink(grid, maxDimension = 20, quality = 80)
    val payload = standardUrl.substringAfter("#d=")

    // img2asciiim://view?d=...
    val uri1 = "img2asciiim://view?d=$payload"
    val res1 = codec.decodeFromDeepLink(uri1)
    assertTrue(res1 is DomainResult.Success, "Failed to decode img2asciiim://view?d=...")
    assertEquals(20, res1.value.originalWidth)

    // img2asciiim://view#d=...
    val uri2 = "img2asciiim://view#d=$payload"
    val res2 = codec.decodeFromDeepLink(uri2)
    assertTrue(res2 is DomainResult.Success, "Failed to decode img2asciiim://view#d=...")
    assertEquals(20, res2.value.originalWidth)

    // img2ascii://view?d=...
    val uri3 = "img2ascii://view?d=$payload"
    val res3 = codec.decodeFromDeepLink(uri3)
    assertTrue(res3 is DomainResult.Success, "Failed to decode img2ascii://view?d=...")
    assertEquals(20, res3.value.originalWidth)
  }

  @Test
  fun rejectsCorruptedOrMissingDataPayloads() {
    val invalidLink = "https://laralbir.github.io/img2asciiIM/#d=corrupted_not_a_valid_image"
    val outcome = codec.decodeFromDeepLink(invalidLink)
    assertTrue(outcome is DomainResult.Failure)
    assertTrue(
      outcome.error is ImageDecodeError.InvalidOrCorruptedImage ||
        outcome.error is ImageDecodeError.InvalidUriScheme,
    )

    val emptyLink = "https://laralbir.github.io/img2asciiIM/"
    val emptyOutcome = codec.decodeFromDeepLink(emptyLink)
    assertTrue(emptyOutcome is DomainResult.Failure)
    assertEquals(ImageDecodeError.InvalidUriScheme, emptyOutcome.error)
  }
}
