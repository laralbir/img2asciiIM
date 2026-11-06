package com.laralbir.img2asciiim.data.source

import android.graphics.Color
import android.os.Build
import com.laralbir.img2asciiim.domain.model.DomainResult
import com.laralbir.img2asciiim.domain.model.ImageDecodeError
import com.laralbir.img2asciiim.domain.model.ImPlatformPreset
import com.laralbir.img2asciiim.domain.model.PixelGrid
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

    assertTrue(deepLink.startsWith(DeepLinkImageCodec.DEEP_LINK_BASE + "#data="))
    assertFalse(deepLink.contains("\n"))
    assertFalse(deepLink.contains(" "))

    // WhatsApp budget is 65,536 characters
    assertTrue(
      deepLink.length <= ImPlatformPreset.WHATSAPP.maxCharacters,
      "WhatsApp link exceeded max character budget: ${deepLink.length} > ${ImPlatformPreset.WHATSAPP.maxCharacters}",
    )
    // Payload should be rich/HD (~15KB - 40KB Base64)
    assertTrue(deepLink.length > 5_000, "WhatsApp HD payload unexpectedly small: ${deepLink.length}")

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
  fun expandedModeTelegramGeneratesValidLinkAndStrictlyRespects4096CharBudget() {
    val grid = createSampleGrid(width = 1200, height = 900)

    val deepLink = codec.encodeToDeepLink(
      pixelGrid = grid,
      mode = DeepLinkMode.EXPANDED_HD,
      preset = ImPlatformPreset.TELEGRAM,
    )

    assertTrue(deepLink.startsWith(DeepLinkImageCodec.DEEP_LINK_BASE + "#data="))
    assertFalse(deepLink.contains("\n"))
    assertFalse(deepLink.contains(" "))

    // Telegram budget is strictly 4,096 characters
    assertTrue(
      deepLink.length <= ImPlatformPreset.TELEGRAM.maxCharacters,
      "Telegram link exceeded max character budget: ${deepLink.length} > ${ImPlatformPreset.TELEGRAM.maxCharacters}",
    )
    assertTrue(deepLink.length <= 4_096)

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

    assertTrue(
      deepLink.length <= ImPlatformPreset.TELEGRAM.maxCharacters,
      "Telegram noisy image link exceeded budget: ${deepLink.length} > 4096",
    )
    val outcome = codec.decodeFromDeepLink(deepLink)
    assertTrue(outcome is DomainResult.Success)
    assertTrue(outcome.value.pixelGrid.width <= 200)
    assertTrue(outcome.value.pixelGrid.height <= 200)
  }

  @Test
  fun standardModeScalesTo320PxMaxDimension() {
    val grid = createSampleGrid(width = 800, height = 600)

    val deepLink = codec.encodeToDeepLink(
      pixelGrid = grid,
      mode = DeepLinkMode.STANDARD,
    )

    assertTrue(deepLink.startsWith(DeepLinkImageCodec.DEEP_LINK_BASE + "#data="))
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

    assertTrue(deepLink.startsWith("https://laralbir.github.io/img2asciiIM/#data="))
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
  fun decodesQueryParameterFallback() {
    val pixels = IntArray(16 * 16) { Color.RED }
    val grid = PixelGrid(width = 16, height = 16, argbPixels = pixels)
    val deepLink = codec.encodeToDeepLink(grid, maxDimension = 16, quality = 80)
    // Test with ?data= instead of #data=
    val queryLink = deepLink.replace("#data=", "?data=")

    val outcome = codec.decodeFromDeepLink(queryLink)
    assertTrue(outcome is DomainResult.Success)
    assertEquals(16, outcome.value.originalWidth)
  }

  @Test
  fun rejectsCorruptedOrMissingDataPayloads() {
    val invalidLink = "https://laralbir.github.io/img2asciiIM/#data=corrupted_not_a_valid_image"
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
