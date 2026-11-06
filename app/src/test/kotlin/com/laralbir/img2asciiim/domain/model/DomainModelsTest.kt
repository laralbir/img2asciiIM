package com.laralbir.img2asciiim.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DomainModelsTest {

  @Test
  fun pixelGridValidatesDimensionsAndProvidesCoordinateAccess() {
    val pixels = intArrayOf(0xFF112233.toInt(), 0xFF445566.toInt(), 0xFF778899.toInt(), 0xFFAABBCC.toInt())
    val grid = PixelGrid(width = 2, height = 2, argbPixels = pixels)

    assertEquals(2, grid.width)
    assertEquals(2, grid.height)
    assertEquals(0xFF445566.toInt(), grid.get(1, 0))
    assertEquals(0xFF778899.toInt(), grid.get(0, 1))

    assertFailsWith<IllegalArgumentException> {
      PixelGrid(width = 0, height = 2, argbPixels = intArrayOf())
    }
    assertFailsWith<IllegalArgumentException> {
      PixelGrid(width = 2, height = 0, argbPixels = intArrayOf())
    }
    assertFailsWith<IllegalArgumentException> {
      PixelGrid(width = 2, height = 2, argbPixels = intArrayOf(1, 2, 3))
    }
  }

  @Test
  fun grayscaleGridValidatesRangeAndProvidesCoordinateAccess() {
    val grid = GrayscaleGrid(width = 2, height = 2, luminance = floatArrayOf(0.0f, 0.25f, 0.75f, 1.0f))

    assertEquals(0.25f, grid.get(1, 0))
    assertEquals(0.75f, grid.get(0, 1))

    assertFailsWith<IllegalArgumentException> {
      GrayscaleGrid(width = 1, height = 1, luminance = floatArrayOf(1.2f))
    }
    assertFailsWith<IllegalArgumentException> {
      GrayscaleGrid(width = 1, height = 1, luminance = floatArrayOf(-0.1f))
    }
  }

  @Test
  fun presetsExposeApprovedMetadataAndBudgets() {
    assertEquals("WhatsApp", ImPlatformPreset.WHATSAPP.displayName)
    assertEquals("com.whatsapp", ImPlatformPreset.WHATSAPP.canonicalPackage)
    assertEquals(listOf("com.whatsapp.w4b"), ImPlatformPreset.WHATSAPP.fallbackPackages)
    assertEquals(CharsetMode.BRAILLE_UNICODE, ImPlatformPreset.WHATSAPP.defaultCharset)
    assertEquals(30, ImPlatformPreset.WHATSAPP.defaultWidthColumns)
    assertEquals(65_536, ImPlatformPreset.WHATSAPP.maxCharacters)
    assertNull(ImPlatformPreset.WHATSAPP.maxUtf8Bytes)
    assertFalse(ImPlatformPreset.WHATSAPP.usesClipboardFirst)

    assertEquals("Telegram", ImPlatformPreset.TELEGRAM.displayName)
    assertEquals("org.telegram.messenger", ImPlatformPreset.TELEGRAM.canonicalPackage)
    assertEquals(
      listOf("org.thunderdog.challegram", "org.telegram.messenger.web"),
      ImPlatformPreset.TELEGRAM.fallbackPackages,
    )
    assertEquals(CharsetMode.BRAILLE_UNICODE, ImPlatformPreset.TELEGRAM.defaultCharset)
    assertEquals(32, ImPlatformPreset.TELEGRAM.defaultWidthColumns)
    assertEquals(4_096, ImPlatformPreset.TELEGRAM.maxCharacters)
    assertNull(ImPlatformPreset.TELEGRAM.maxUtf8Bytes)
    assertFalse(ImPlatformPreset.TELEGRAM.usesClipboardFirst)

    assertEquals("Instagram DM", ImPlatformPreset.INSTAGRAM_DIRECT.displayName)
    assertEquals("com.instagram.android", ImPlatformPreset.INSTAGRAM_DIRECT.canonicalPackage)
    assertEquals(emptyList(), ImPlatformPreset.INSTAGRAM_DIRECT.fallbackPackages)
    assertEquals(CharsetMode.BRAILLE_UNICODE, ImPlatformPreset.INSTAGRAM_DIRECT.defaultCharset)
    assertEquals(22, ImPlatformPreset.INSTAGRAM_DIRECT.defaultWidthColumns)
    assertEquals(1_000, ImPlatformPreset.INSTAGRAM_DIRECT.maxCharacters)
    assertEquals(1_000, ImPlatformPreset.INSTAGRAM_DIRECT.maxUtf8Bytes)
    assertTrue(ImPlatformPreset.INSTAGRAM_DIRECT.usesClipboardFirst)
  }

  @Test
  fun conversionParamsDeriveDefaultsFromSelectedPreset() {
    val igParams = ConversionParams(preset = ImPlatformPreset.INSTAGRAM_DIRECT)
    assertEquals(CharsetMode.BRAILLE_UNICODE, igParams.charsetMode)
    assertEquals(22, igParams.widthColumns)
    assertEquals(0.0f, igParams.brightness)
    assertEquals(1.0f, igParams.contrast)
    assertTrue(igParams.invert)
    assertEquals(DitherMode.FLOYD_STEINBERG, igParams.ditherMode)
  }

  @Test
  fun domainResultRepresentsSuccessAndFailureCleanly() {
    val ok: DomainResult<Int, ImageDecodeError> = DomainResult.Success(42)
    val err: DomainResult<Int, ImageDecodeError> = DomainResult.Failure(ImageDecodeError.InvalidUriScheme)

    assertTrue(ok.isSuccess)
    assertEquals(42, ok.getOrNull())
    assertTrue(err.isFailure)
    assertEquals(ImageDecodeError.InvalidUriScheme, err.errorOrNull())
  }
}
