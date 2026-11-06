package com.laralbir.img2asciiim.domain.engine

import com.laralbir.img2asciiim.domain.model.GrayscaleGrid

/**
 * Applies brightness (`[-1f, 1f]`) and contrast (`[0.5f, 3.0f]` around midpoint `0.5f`)
 * and converts normalized luminance into ink density (`0f` = blank background, `1f` = max ink).
 */
class ToneAdjuster {

  fun adjust(
    grid: GrayscaleGrid,
    brightness: Float = 0.0f,
    contrast: Float = 1.0f,
    invert: Boolean = true,
  ): GrayscaleGrid {
    val clampedBrightness = brightness.coerceIn(-1.0f, 1.0f)
    val clampedContrast = contrast.coerceIn(0.5f, 3.0f)
    val src = grid.luminance
    val out = FloatArray(src.size)

    for (i in src.indices) {
      val v = (((src[i] + clampedBrightness) - 0.5f) * clampedContrast + 0.5f).coerceIn(0.0f, 1.0f)
      val ink = if (invert) v else (1.0f - v)
      out[i] = ink.coerceIn(0.0f, 1.0f)
    }

    return GrayscaleGrid(
      width = grid.width,
      height = grid.height,
      luminance = out,
    )
  }
}
