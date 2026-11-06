package com.laralbir.img2asciiim.domain.engine

import com.laralbir.img2asciiim.domain.model.CharsetMode
import com.laralbir.img2asciiim.domain.model.ConversionParams
import com.laralbir.img2asciiim.domain.model.ImPlatformPreset
import com.laralbir.img2asciiim.domain.model.PixelGrid
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImMessageFormatterAndUseCaseTest {

  private val useCase = ImageToAsciiUseCase()

  @Test
  fun formatsWhatsAppWithCompactSixCharTripleBacktickFenceAndNoLeadingSpace() {
    val sample = DemoSampleImageFactory.createInFlightWindowSample(width = 80, height = 60)
    val result = useCase(
      pixelGrid = sample.pixelGrid,
      params = ConversionParams(
        preset = ImPlatformPreset.WHATSAPP,
        charsetMode = CharsetMode.ASCII_CLASSIC,
        widthColumns = 32,
      ),
    )

    assertTrue(result.usesMonospaceFence)
    assertFalse(result.proportionalFontWarning)
    assertTrue(result.formattedMessage.startsWith("```"))
    assertFalse(result.formattedMessage.startsWith("``` "))
    assertFalse(result.formattedMessage.startsWith("```\n"))
    assertTrue(result.formattedMessage.endsWith("```"))
    assertEquals(result.rawArt.length + 6, result.formattedMessage.length)
    assertTrue(result.charCount <= ImPlatformPreset.WHATSAPP.maxCharacters)
  }

  @Test
  fun formatsTelegramWithoutFenceForBrailleAndWithNewlineFenceForAsciiAndEnforces4096Budget() {
    // Tall 9:21 screenshot (90x210) at requested 70 columns would exceed 4,096 chars!
    val tallPixels = IntArray(90 * 210) { idx -> if (idx % 3 == 0) 0xFFFFFFFF.toInt() else 0xFF102030.toInt() }
    val tallGrid = PixelGrid(width = 90, height = 210, argbPixels = tallPixels)

    val tgBraille = useCase(
      pixelGrid = tallGrid,
      params = ConversionParams(
        preset = ImPlatformPreset.TELEGRAM,
        charsetMode = CharsetMode.BRAILLE_UNICODE,
        widthColumns = 70,
      ),
    )
    assertFalse(tgBraille.usesMonospaceFence)
    assertFalse(tgBraille.formattedMessage.contains("```"))
    assertTrue(tgBraille.wasAutoScaled)
    assertTrue(tgBraille.charCount <= 4_096, "Telegram Braille exceeded 4096 chars: ${tgBraille.charCount}")

    val tgAscii = useCase(
      pixelGrid = tallGrid,
      params = ConversionParams(
        preset = ImPlatformPreset.TELEGRAM,
        charsetMode = CharsetMode.ASCII_CLASSIC,
        widthColumns = 70,
      ),
    )
    assertTrue(tgAscii.usesMonospaceFence)
    assertTrue(tgAscii.formattedMessage.startsWith("```\n"))
    assertTrue(tgAscii.formattedMessage.endsWith("\n```"))
    assertEquals(tgAscii.rawArt.length + 8, tgAscii.formattedMessage.length)
    assertTrue(tgAscii.wasAutoScaled)
    assertTrue(tgAscii.charCount <= 4_096, "Telegram ASCII exceeded 4096 chars: ${tgAscii.charCount}")
  }

  @Test
  fun enforcesInstagramDirectStrict1000CharAnd1000Utf8ByteBudgetWithoutBackticks() {
    val sample = DemoSampleImageFactory.createInFlightWindowSample(width = 160, height = 120)

    // At 40 columns of Braille, UTF-8 byte count (3 bytes per Braille char) would exceed 1,000 bytes!
    val igBraille = useCase(
      pixelGrid = sample.pixelGrid,
      params = ConversionParams(
        preset = ImPlatformPreset.INSTAGRAM_DIRECT,
        charsetMode = CharsetMode.BRAILLE_UNICODE,
        widthColumns = 40,
      ),
    )
    assertFalse(igBraille.usesMonospaceFence)
    assertFalse(igBraille.proportionalFontWarning)
    assertFalse(igBraille.formattedMessage.contains("`"))
    assertFalse(igBraille.formattedMessage.contains(" "))
    assertTrue(igBraille.wasAutoScaled)
    assertTrue(igBraille.charCount <= 1_000, "IG charCount=${igBraille.charCount}")
    assertTrue(igBraille.utf8ByteCount <= 1_000, "IG utf8ByteCount=${igBraille.utf8ByteCount}")
    assertEquals(igBraille.formattedMessage.encodeToByteArray().size, igBraille.utf8ByteCount)

    // Selecting ASCII_CLASSIC on Instagram Direct raises proportionalFontWarning
    val igAscii = useCase(
      pixelGrid = sample.pixelGrid,
      params = ConversionParams(
        preset = ImPlatformPreset.INSTAGRAM_DIRECT,
        charsetMode = CharsetMode.ASCII_CLASSIC,
        widthColumns = 22,
      ),
    )
    assertTrue(igAscii.proportionalFontWarning)
    assertFalse(igAscii.usesMonospaceFence)
    assertTrue(igAscii.charCount <= 1_000)
    assertTrue(igAscii.utf8ByteCount <= 1_000)
  }

  @Test
  fun ultraTallOneToFiftyStripTruncatesTrailingRowsSafelyEvenWhenMinFourColumnsExceedsBudget() {
    // 10 x 500 (1:50 aspect ratio): even at cols = 4, rows = round(4 * 50 * 0.5) = 100 rows -> > 1,200 UTF-8 bytes in Braille!
    val strip = PixelGrid(width = 10, height = 500, argbPixels = IntArray(10 * 500) { 0xFFFFFFFF.toInt() })
    val result = useCase(
      pixelGrid = strip,
      params = ConversionParams(
        preset = ImPlatformPreset.INSTAGRAM_DIRECT,
        charsetMode = CharsetMode.BRAILLE_UNICODE,
        widthColumns = 22,
      ),
    )

    assertTrue(result.wasAutoScaled)
    assertTrue(result.rows >= 1)
    assertTrue(result.charCount <= 1_000)
    assertTrue(result.utf8ByteCount <= 1_000)
  }

  @Test
  fun domainPackageContainsZeroAndroidOrAndroidxImports() {
    val domainRoot = File("src/main/kotlin/com/laralbir/img2asciiim/domain")
    assertTrue(domainRoot.exists(), "Domain root not found at ${domainRoot.absolutePath}")
    val kotlinFiles = domainRoot.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    assertTrue(kotlinFiles.isNotEmpty())

    for (file in kotlinFiles) {
      val forbiddenLines = file.readLines().filter { line ->
        val trimmed = line.trim()
        trimmed.startsWith("import android.") || trimmed.startsWith("import androidx.")
      }
      assertTrue(
        forbiddenLines.isEmpty(),
        "Forbidden Android import in ${file.name}: $forbiddenLines",
      )
    }
  }
}
