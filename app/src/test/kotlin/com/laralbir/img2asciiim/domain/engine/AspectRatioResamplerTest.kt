package com.laralbir.img2asciiim.domain.engine

import com.laralbir.img2asciiim.domain.model.CharsetMode
import com.laralbir.img2asciiim.domain.model.GrayscaleGrid
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AspectRatioResamplerTest {

  private val resampler = AspectRatioResampler()

  @Test
  fun computesSquareAspectRatioWithCellCompensationForAsciiAndBraille() {
    // CA-02.3: 1000x1000 image at 32 columns with 0.5 cell aspect ratio -> 16 character rows
    val asciiDims = resampler.computeTargetDimensions(
      sourceWidth = 1_000,
      sourceHeight = 1_000,
      columns = 32,
      charsetMode = CharsetMode.ASCII_CLASSIC,
    )
    assertEquals(32, asciiDims.columns)
    assertEquals(16, asciiDims.rows)
    assertEquals(32, asciiDims.sampleWidth)
    assertEquals(16, asciiDims.sampleHeight)

    val brailleDims = resampler.computeTargetDimensions(
      sourceWidth = 1_000,
      sourceHeight = 1_000,
      columns = 32,
      charsetMode = CharsetMode.BRAILLE_UNICODE,
    )
    assertEquals(32, brailleDims.columns)
    assertEquals(16, brailleDims.rows)
    assertEquals(64, brailleDims.sampleWidth)
    assertEquals(64, brailleDims.sampleHeight)
  }

  @Test
  fun coercesExtremePanoramicDimensionsToAtLeastOneRow() {
    // CB-03: 10000 x 200 panoramic strip
    val dims = resampler.computeTargetDimensions(
      sourceWidth = 10_000,
      sourceHeight = 200,
      columns = 12,
      charsetMode = CharsetMode.UNICODE_BLOCKS,
    )
    assertEquals(12, dims.columns)
    assertEquals(1, dims.rows)
    assertEquals(12, dims.sampleWidth)
    assertEquals(1, dims.sampleHeight)
  }

  @Test
  fun usesAreaBoxAveragingInsteadOfNearestNeighborAliasing() {
    // 4x4 fine checkerboard of 0.0f and 1.0f downsampled to 2x2 must average each 2x2 quadrant to 0.5f
    // (Nearest-neighbor would alias every sample to 0.0f!)
    val checkerboard = FloatArray(16) { idx ->
      val x = idx % 4
      val y = idx / 4
      if ((x + y) % 2 == 0) 0.0f else 1.0f
    }
    val grid = GrayscaleGrid(width = 4, height = 4, luminance = checkerboard)

    val downsampled = resampler.resample(grid, sampleWidth = 2, sampleHeight = 2)
    assertEquals(2, downsampled.width)
    assertEquals(2, downsampled.height)
    for (y in 0 until 2) {
      for (x in 0 until 2) {
        assertTrue(
          abs(downsampled.get(x, y) - 0.5f) <= 0.001f,
          "Expected box average 0.5f at ($x,$y), got ${downsampled.get(x, y)}",
        )
      }
    }
  }
}
