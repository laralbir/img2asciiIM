package com.laralbir.img2asciiim.domain.engine

import com.laralbir.img2asciiim.domain.model.PixelGrid
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class LuminanceExtractorTest {

  private val extractor = LuminanceExtractor()

  @Test
  fun computesExactBt709LuminanceOnOpaquePrimariesAndExtremes() {
    val grid = PixelGrid(
      width = 5,
      height = 1,
      argbPixels = intArrayOf(
        0xFF000000.toInt(), // Black
        0xFFFFFFFF.toInt(), // White
        0xFFFF0000.toInt(), // Red
        0xFF00FF00.toInt(), // Green
        0xFF0000FF.toInt(), // Blue
      ),
    )

    val result = extractor.extract(grid, invert = true)

    assertClose(0.0f, result.get(0, 0))
    assertClose(1.0f, result.get(1, 0))
    assertClose(0.2126f, result.get(2, 0))
    assertClose(0.7152f, result.get(3, 0))
    assertClose(0.0722f, result.get(4, 0))
  }

  @Test
  fun compositesTransparentPixelsOverBlackWhenInvertedAndWhiteWhenNotInverted() {
    val transparentAndHalfWhite = PixelGrid(
      width = 2,
      height = 1,
      argbPixels = intArrayOf(
        0x00000000, // Fully transparent RGBA(0,0,0,0)
        0x80FFFFFF.toInt(), // ~50.2% alpha white (128/255)
      ),
    )

    val darkBubbleTarget = extractor.extract(transparentAndHalfWhite, invert = true)
    assertClose(0.0f, darkBubbleTarget.get(0, 0))
    assertClose(128f / 255f, darkBubbleTarget.get(1, 0), epsilon = 0.005f)

    val lightBubbleTarget = extractor.extract(transparentAndHalfWhite, invert = false)
    assertClose(1.0f, lightBubbleTarget.get(0, 0))
    assertClose(1.0f, lightBubbleTarget.get(1, 0), epsilon = 0.005f)
  }

  private fun assertClose(expected: Float, actual: Float, epsilon: Float = 0.0005f) {
    assertTrue(
      abs(expected - actual) <= epsilon,
      "Expected $expected ± $epsilon, got $actual",
    )
  }
}
