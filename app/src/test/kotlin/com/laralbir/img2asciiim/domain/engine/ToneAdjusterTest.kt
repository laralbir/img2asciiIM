package com.laralbir.img2asciiim.domain.engine

import com.laralbir.img2asciiim.domain.model.GrayscaleGrid
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class ToneAdjusterTest {

  private val adjuster = ToneAdjuster()

  @Test
  fun mapsInkDensitySymmetricallyBetweenLightAndDarkBubbleTargets() {
    // CA-02.2: Left pixel pure black (Y=0), right pixel pure white (Y=1)
    val input = GrayscaleGrid(width = 2, height = 1, luminance = floatArrayOf(0.0f, 1.0f))

    val forLightBubble = adjuster.adjust(input, brightness = 0.0f, contrast = 1.0f, invert = false)
    assertClose(1.0f, forLightBubble.get(0, 0)) // Black pixel -> max ink (1.0f)
    assertClose(0.0f, forLightBubble.get(1, 0)) // White pixel -> zero ink (0.0f)

    val forDarkBubble = adjuster.adjust(input, brightness = 0.0f, contrast = 1.0f, invert = true)
    assertClose(0.0f, forDarkBubble.get(0, 0)) // Black pixel -> zero ink (0.0f)
    assertClose(1.0f, forDarkBubble.get(1, 0)) // White pixel -> max ink (1.0f)
  }

  @Test
  fun pivotsContrastAroundMidpointAndClampsBrightnessSafely() {
    val input = GrayscaleGrid(width = 3, height = 1, luminance = floatArrayOf(0.25f, 0.5f, 0.75f))

    val highContrast = adjuster.adjust(input, brightness = 0.0f, contrast = 2.0f, invert = true)
    assertClose(0.0f, highContrast.get(0, 0))
    assertClose(0.5f, highContrast.get(1, 0))
    assertClose(1.0f, highContrast.get(2, 0))

    val saturatedBright = adjuster.adjust(input, brightness = 0.9f, contrast = 1.0f, invert = true)
    assertClose(1.0f, saturatedBright.get(0, 0))
    assertClose(1.0f, saturatedBright.get(1, 0))
    assertClose(1.0f, saturatedBright.get(2, 0))
  }

  private fun assertClose(expected: Float, actual: Float, epsilon: Float = 0.0005f) {
    assertTrue(
      abs(expected - actual) <= epsilon,
      "Expected $expected ± $epsilon, got $actual",
    )
  }
}
