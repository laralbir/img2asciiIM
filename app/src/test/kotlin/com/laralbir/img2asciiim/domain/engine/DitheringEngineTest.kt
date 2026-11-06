package com.laralbir.img2asciiim.domain.engine

import com.laralbir.img2asciiim.domain.model.DitherMode
import com.laralbir.img2asciiim.domain.model.GrayscaleGrid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DitheringEngineTest {

  private val engine = DitheringEngine()

  @Test
  fun diffusesHalfLuminanceIntoBalancedDotDensityForFloydSteinbergAndAtkinson() {
    // CA-02.4: 50% gray (even slightly below 0.5 threshold, e.g. 0.48f, or exact 0.5f)
    // must distribute active dots in [35%..65%] instead of collapsing to 0% or 100%.
    val uniformHalf = GrayscaleGrid(
      width = 24,
      height = 24,
      luminance = FloatArray(24 * 24) { 0.48f },
    )

    val fsLevels = engine.quantize(uniformHalf, levels = 2, mode = DitherMode.FLOYD_STEINBERG)
    val fsDensity = fsLevels.count { it == 1 }.toFloat() / fsLevels.size
    assertTrue(fsDensity in 0.35f..0.65f, "Floyd-Steinberg density out of range: $fsDensity")

    val atkinsonLevels = engine.quantize(uniformHalf, levels = 2, mode = DitherMode.ATKINSON)
    val atkinsonDensity = atkinsonLevels.count { it == 1 }.toFloat() / atkinsonLevels.size
    assertTrue(atkinsonDensity in 0.35f..0.65f, "Atkinson density out of range: $atkinsonDensity")
  }

  @Test
  fun quantizesDirectlyWithoutDiffusionWhenModeIsNoneAndRespectsGridBoundaries() {
    val rampInput = GrayscaleGrid(
      width = 5,
      height = 1,
      luminance = floatArrayOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f),
    )
    val quantized = engine.quantize(rampInput, levels = 5, mode = DitherMode.NONE)
    assertEquals(listOf(0, 1, 2, 3, 4), quantized.toList())

    // 1x1 boundary check for Atkinson (which looks +2 ahead in x and y)
    val tiny = GrayscaleGrid(width = 1, height = 1, luminance = floatArrayOf(0.7f))
    val tinyQuantized = engine.quantize(tiny, levels = 9, mode = DitherMode.ATKINSON)
    assertEquals(1, tinyQuantized.size)
    assertTrue(tinyQuantized[0] in 0..8)
  }
}
