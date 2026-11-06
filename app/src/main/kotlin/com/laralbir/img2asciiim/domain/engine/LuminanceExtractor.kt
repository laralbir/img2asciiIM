package com.laralbir.img2asciiim.domain.engine

import com.laralbir.img2asciiim.domain.model.GrayscaleGrid
import com.laralbir.img2asciiim.domain.model.PixelGrid

/**
 * Extracts perceptual BT.709 luminance (`Y`) from packed ARGB pixels while deterministically
 * compositing translucent/transparent pixels over solid black (`invert = true`, dark bubble)
 * or solid white (`invert = false`, light bubble).
 */
class LuminanceExtractor {

  fun extract(pixelGrid: PixelGrid, invert: Boolean): GrayscaleGrid {
    val src = pixelGrid.argbPixels
    val out = FloatArray(src.size)
    val inv255 = 1.0f / 255.0f

    for (i in src.indices) {
      val argb = src[i]
      val a = ((argb ushr 24) and 0xFF) * inv255
      val rRaw = ((argb ushr 16) and 0xFF) * inv255
      val gRaw = ((argb ushr 8) and 0xFF) * inv255
      val bRaw = (argb and 0xFF) * inv255

      val r: Float
      val g: Float
      val b: Float
      if (invert) {
        // Composite over solid black (0, 0, 0) for dark chat bubble targets
        r = rRaw * a
        g = gRaw * a
        b = bRaw * a
      } else {
        // Composite over solid white (1, 1, 1) for light chat bubble targets
        val bg = 1.0f - a
        r = rRaw * a + bg
        g = gRaw * a + bg
        b = bRaw * a + bg
      }

      val y = (0.2126f * r + 0.7152f * g + 0.0722f * b).coerceIn(0.0f, 1.0f)
      out[i] = y
    }

    return GrayscaleGrid(
      width = pixelGrid.width,
      height = pixelGrid.height,
      luminance = out,
    )
  }
}
