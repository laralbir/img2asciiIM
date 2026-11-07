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
    assertEquals(260, standardProfile.maxDimension)
    assertEquals(55, standardProfile.quality)

    val whatsAppHdProfile = codec.resolveProfile(DeepLinkMode.EXPANDED_HD, ImPlatformPreset.WHATSAPP)
    assertEquals(300, whatsAppHdProfile.maxDimension)
    assertEquals(60, whatsAppHdProfile.quality)

    val telegramHdProfile = codec.resolveProfile(DeepLinkMode.EXPANDED_HD, ImPlatformPreset.TELEGRAM)
    assertEquals(220, telegramHdProfile.maxDimension)
    assertEquals(50, telegramHdProfile.quality)
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

    // WhatsApp expanded HD budget is 3,800 characters
    assertTrue(
      deepLink.length <= DeepLinkImageCodec.WHATSAPP_EXPANDED_BUDGET,
      "WhatsApp link exceeded max character budget: ${deepLink.length} > ${DeepLinkImageCodec.WHATSAPP_EXPANDED_BUDGET}",
    )
    assertTrue(deepLink.length <= 3_800)

    // Verify round-trip decoding
    val outcome = codec.decodeFromDeepLink(deepLink)
    assertTrue(outcome is DomainResult.Success)
    val decoded = outcome.value
    assertEquals(300, decoded.originalWidth)
    assertEquals(225, decoded.originalHeight)
    assertEquals(300, decoded.pixelGrid.width)
    assertEquals(225, decoded.pixelGrid.height)
  }

  @Test
  fun expandedModeTelegramGeneratesValidLinkAndStrictlyRespects3800CharBudget() {
    val grid = createSampleGrid(width = 1200, height = 900)

    val deepLink = codec.encodeToDeepLink(
      pixelGrid = grid,
      mode = DeepLinkMode.EXPANDED_HD,
      preset = ImPlatformPreset.TELEGRAM,
    )

    assertTrue(deepLink.startsWith(DeepLinkImageCodec.DEEP_LINK_BASE + "#d="))
    assertFalse(deepLink.contains("\n"))
    assertFalse(deepLink.contains(" "))

    // Telegram budget is strictly 3,800 characters (leaving safety margin for 4,096 char limit)
    assertTrue(
      deepLink.length <= DeepLinkImageCodec.TELEGRAM_BUDGET,
      "Telegram link exceeded max character budget: ${deepLink.length} > ${DeepLinkImageCodec.TELEGRAM_BUDGET}",
    )
    assertTrue(deepLink.length <= 3_800)

    // Verify round-trip decoding
    val outcome = codec.decodeFromDeepLink(deepLink)
    assertTrue(outcome is DomainResult.Success)
    val decoded = outcome.value
    assertEquals(220, decoded.originalWidth)
    assertEquals(165, decoded.originalHeight)
    assertEquals(220, decoded.pixelGrid.width)
    assertEquals(165, decoded.pixelGrid.height)
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
    assertTrue(deepLink.length <= 3_800)

    val outcome = codec.decodeFromDeepLink(deepLink)
    assertTrue(outcome is DomainResult.Success)
    assertTrue(outcome.value.pixelGrid.width <= 220)
    assertTrue(outcome.value.pixelGrid.height <= 220)
  }

  @Test
  fun standardModeScalesTo260PxMaxDimensionAndRespectsStandardBudget() {
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
    assertTrue(deepLink.length <= 3_800)

    val outcome = codec.decodeFromDeepLink(deepLink)
    assertTrue(outcome is DomainResult.Success)
    val decoded = outcome.value
    assertEquals(260, decoded.originalWidth)
    assertEquals(195, decoded.originalHeight)
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
    assertTrue(decoded.originalWidth <= 300)
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

  @Test
  fun truncatedRiffWebpPayloadReturnsInvalidOrCorruptedImage() {
    val truncatedWhatsAppPayload =
      "https://laralbir.github.io/img2asciiIM/#d=UklGRmRQAABXRUJQVlA4WAoAAAAgAAAA_QEAfwEASUNDUMgBAAAAAAHIAAAAAAQwAABtbnRyUkdCIFhZWiAH4AABAAEAAAAAAABhY3NwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQAA9tYAAQAAAADTLQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAlkZXNjAAAA8AAAACRyWFlaAAABFAAAABRnWFlaAAABKAAAABRiWFlaAAABPAAAABR3dHB0AAABUAAAABRyVFJDAAABZAAAAChnVFJDAAABZAAAAChiVFJDAAABZAAAAChjcHJ0AAABjAAAADxtbHVjAAAAAAAAAAEAAAAMZW5VUwAAAAgAAAAcAHMAUgBHAEJYWVogAAAAAAAAb6IAADj1AAADkFhZWiAAAAAAAABimQAAt4UAABjaWFlaIAAAAAAAACSgAAAPhAAAts9YWVogAAAAAAAA9tYAAQAAAADTLXBhcmEAAAAAAAQAAAACZmYAAPKnAAANWQAAE9AAAApbAAAAAAAAAABtbHVjAAAAAAAAAAEAAAAMZW5VUwAAACAAAAAcAEcAbwBvAGcAbABlACAASQBuAGMALgAgADIAMAAxADZWUDggdk4AAHB0AZ0BKv4BgAE-jTyYR6UjoqEv07yAoBGJZ21f-t4z3oAABBdw8gWllF1mqOwvA2Wh5318_p35j_0_1Xtm3y_jvBj-3f2_QZ_0-Hf79_vebV5gH-ffId7_9eS1_w8-P67_0dKPhqHsP_vzG_zX_y9NHrhp7Epi44yAhbLnwPRY4zEGTtcLIg-7ooXta7UfQfSv-OTGTN_ewsG4TbvHHIWTzUqFskiwI5cokf-pyhuS_1bn9IO_HfTOQX8EI98ADUIY6hK9B0cV7YQsnRcrNMAY6E2HcsQre4dxuq2B1y-2ahK7M20HqXydGyJSjL51ahzYNMEXRQFTY8g3VoE6hgt2ts42TwWLM7frHVUWtd4nALSxQE-65RqGwzlY61EGYYbU1dwEj5Zm-KbzN-Gz9hvWm4oJGFmBOQ-ymxAayYsQiMDU-lDoswub3x42hCjndGmOD_5ueswaubWTRZm7_yWXXm9gXqyvtEfWvnzUhOgSOCfL0ZifL8bnNQxewd6FxVtEcrnpfdn39-HK5hZ-fOXqC0BJPLbK74oVnd6OcL8l47Q6UikFgGVroG_HWDJsqXGPGoWx-wLQztglpJ0SAPEqfMNxwnNOW9BAcuwFWIVkHx4ZX4aGdg3gK4l2zGHu96yFNRP1gGO7PtR7w-m2CqMKQ2AAb2WZPQhj5hWvWdZbTzO43VuEBWGGkf2Q3mw1SL3z1-OtsaoIVFqH0pWC9Mnmq43LcqDrTKDvlk73e-Z5lIikLXJxL0YoqGxg-OWPQRIQ6VRjXW5iPK1lk-dcophxmFr28Y4cwpsUSaiHjJiwtVQSrbMv9qRw3qbBI4OiIu2uemvkR-mMMw0Gu4B5yFMQFdJzV55n5cU9BcuUgD7xraZT1XazEVGql4h6yi6RHUA-Xgxh7U2_gAoZ--Dk1Xq1yojlnhFMsTdrxjrU-8Iu5HyF_xVHtpQyCQqaiMF0iO0dnYateL457ReaUYXCn5njSdU2u9w1zcf_xO9Sx7cQ6VMOmpN1Ymd2UV6Bi0qMQt3pj3UieCIPaE9Phwaq1cIUJuZ7H65WNpNjPG_rD-l29LHTNXB7dgrjQJ_7mYVoSkFGyMjdQYOctsWb6T67W0HxzJeA3-coge1bQg0EGYMucb_ibQ69O-wyvk_po_IbLhKZ8zQxHBSAH0XETKjil4p5G4Qjj_mztZ95-fqK_wdsnoL1CQ40aj6sbbsjH4be46wI6oUQXcXZqSawdfxesmmkMv-cLxfzyYxPoeuRXk3XmayFBuX-DFaBvEfZeuYBxoxf8xGvhQDDkOsJ_-WwDgwEbfv4zOV65ie22nIzv_pembR2h9VO6LT-GXCU43qQ8uiB3lb_bOJgAiCMiIedWAvV5qZQxIiYGpQNlUIC8rAo3KWNuvUCrlg-cIxtmVh9IZougdiHwo1ASYyurDJtQ7LqFwVllqiZfJ_nuIRr7CYGRce2co5lmtjFaDZuWbFWnzQbExBK0e_OL61dCcoaZ_7LhdQAvaLqjeOsJ3iSGTlnmlBjspwJ8nL5FvycwSj20R5dGJDzaE9A1eJ_M2AmBlwzr5N3AJaTGD85bC9K2_gn0Uu0Ekz-cKm3czM0L_afrFtAbkUzhr_2YCJeb40jbWg9Ybm4liWzds8lGdhBGZ3Q1mJoJGAn0OixTLGq8CK6CcjY-te3jB36NUgNFmOxJRFF2tGPpqacOr6epBOHcxkmKCZrzCTiypzm-OPfv31Z7TuFGMZbe6I7tw9Y1-_uQq4qWqnQhysmt52TJ3d7Ske_btLz1MMUrEkGRKvDozOdNcLyi-MA6H7Qq65_8RNaTUthpdwjHNh6-WLwg9WWTVZfyUq1NXka2tznWNriZwxFdSqZRS3n-Wjb1AO0W8jPgea5L3_kg9Gbj6ofycNzcJIyxQU22gW2hwICi466tjX1-34dGk4LzDmFlFJKtZU1elQ1ap5l_JKHqauvw_O_kKn7qhfqduKicChFCZIRD8E2OxXG1YMzF_2pxfN46kov-DSFvGbChlpOdTgOZpArlJ1oowTTD0eB4-xUDe0blTInxg9tdSKcmBAHrPY3c1_IlP5TjoKRacfUM-Q3tH6MuZLXkLVQeuSGsKAfLYKmP0KoEjzZBUtxB1QK3IpwIp0KGlu3eTr2mK6Ng4c5DFv8qoK1kNf--x1Ua4K3Kdouj_rKclRt5bKfyH71Ueq2FtbU5Y6zxXTcdtm1d-QUEvj9LQK4FLiC__ZfkeD6dLL5Ccf-Qr-e6Usx2ydNnFDvostTQuzwvxsIDwncjs27HrFDJUGTGypx6RoGGKGQFJjskrSUmHXlHJlQOqvqBVOzeMW31L0mv1TmZd7M4e0dhH3ERntZJfIGmCkW26HKFy_fhdhKW658OHygCxubS-H04sTlr8ogtEuRU7vdlTcPDBAQdp6VkbPLMBQa61QhYqKQiww1d0qgeZL2HBsv7PuoYqaO1duUUod6YqpjcIoLM2QsDzsx25XNQeNXC9XFWh1pG9e8i1JTAyzfwiDrORIuhhE_v3RalSuf4FLdp1Eem5TlCWb0WOIDtP8DuOArLkD3SmZG7jA7BWzGEYcRiYWRLxMbqeG1GzVxl_-n_XMc5mvRzb6N2oSlzwYXesnBXT9FTIlKcVQHezjy1LbCwOtNY6Dn126GOf1bmxR8pPvNrIfoYqDRPsPvu535PmIFiI7GZEpenXCmxFWW4TEF_plxbP9p46EKlwfCmvPKGUYkr6gM1S8v6xtP2JpQA6J67UqNHy_Fk68nDuKif7_0bIVB5pt9Er5vHFSauxeNCl-guhfG_TiKyIxsClFaZKisLvrsEcNez6GjfpBNAof6xsOr75JUszMUjx3Z8MW7aww_svNsJGgzfYBeIQlS9aQ_L6-W7Wq-dLrO8tdy09dHQi_mfF56jd3FdtKhz1EpwEZ_QmNtPrc7KgbXGKm0smTQQ12mi2s16-JeVSxCWRYYZNTR1MG4Mjue0GxWsIDbKXlYzdBhMmNYf0E454eeeqLpX6H9fvHOgTsAH6sPNQAUh0F3iTB3dspo7dF5pYFr929__Ka_LeSDn8tYAENrajGzUU7VAvkKrXCS_aEmvsCffVJIOYBkwSxoqe_QPTLvdmkKDqvorYlWJY1yicX-v1KL0JPq3_XDf2I69iZvTJdkeLDV8CNdZRuz2Bgg-5uhZpdeMJnotleCQ7RHmNZf1cIcl2zt9Ar-BbEzfoOTIqFOUF2G9l2Zpf34C4197VNgP-3GgEtdU2JSJCS8Al5vMk02mOkOVwKXPPOLYwU0CPT7VuvD2V4sSwjOCiPVIzc5z0tWoIOnL7Vh4RhOsXHSbnhDJDHNTWsFgzW22ji1ayv70V8BzhVTJbV-IlbqL0_huBbeYNDqjYhOgcKSpq3HK9_koz"

    // Exactly 4096 characters (WhatsApp limit cutoff)
    assertEquals(4096, truncatedWhatsAppPayload.length)

    val result = codec.decodeFromDeepLink(truncatedWhatsAppPayload)
    assertTrue(result is DomainResult.Failure)
    assertEquals(ImageDecodeError.InvalidOrCorruptedImage, result.error)
  }
}
