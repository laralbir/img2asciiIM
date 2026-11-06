package com.laralbir.img2asciiim.domain.engine

import com.laralbir.img2asciiim.domain.model.CharsetMode
import com.laralbir.img2asciiim.domain.model.GrayscaleGrid
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class TargetGridDimensions(
  val columns: Int,
  val rows: Int,
  val sampleWidth: Int,
  val sampleHeight: Int,
)

/**
 * Area/box-averaging resampler with typographic cell aspect ratio compensation (`0.5f`)
 * and `2x4` subpixel expansion for Unicode Braille (`U+2800..U+28FF`).
 */
class AspectRatioResampler {

  fun computeTargetDimensions(
    sourceWidth: Int,
    sourceHeight: Int,
    columns: Int,
    charsetMode: CharsetMode,
    cellWidthToHeightRatio: Float = DEFAULT_CELL_WIDTH_TO_HEIGHT_RATIO,
  ): TargetGridDimensions {
    require(sourceWidth > 0 && sourceHeight > 0) {
      "Source dimensions must be > 0, got ${sourceWidth}x$sourceHeight"
    }
    val safeCols = columns.coerceAtLeast(1)
    val rows = ((safeCols.toFloat() * sourceHeight.toFloat() / sourceWidth.toFloat()) * cellWidthToHeightRatio)
      .roundToInt()
      .coerceAtLeast(1)

    val (sampleWidth, sampleHeight) = when (charsetMode) {
      CharsetMode.BRAILLE_UNICODE -> (safeCols * 2) to (rows * 4)
      CharsetMode.ASCII_CLASSIC,
      CharsetMode.UNICODE_BLOCKS -> safeCols to rows
    }

    return TargetGridDimensions(
      columns = safeCols,
      rows = rows,
      sampleWidth = sampleWidth,
      sampleHeight = sampleHeight,
    )
  }

  fun resample(
    grid: GrayscaleGrid,
    sampleWidth: Int,
    sampleHeight: Int,
  ): GrayscaleGrid {
    val dstW = sampleWidth.coerceAtLeast(1)
    val dstH = sampleHeight.coerceAtLeast(1)
    val srcW = grid.width
    val srcH = grid.height

    if (srcW == dstW && srcH == dstH) {
      return grid
    }

    val src = grid.luminance
    val out = FloatArray(dstW * dstH)

    val scaleX = srcW.toDouble() / dstW.toDouble()
    val scaleY = srcH.toDouble() / dstH.toDouble()

    // Precompute horizontal span bounds to avoid repeated math inside the inner loop
    val xStartCoord = DoubleArray(dstW) { dx -> dx * scaleX }
    val xEndCoord = DoubleArray(dstW) { dx -> (dx + 1) * scaleX }
    val ixStart = IntArray(dstW) { dx -> floor(xStartCoord[dx]).toInt().coerceIn(0, srcW - 1) }
    val ixEnd = IntArray(dstW) { dx ->
      (ceil(xEndCoord[dx]).toInt() - 1).coerceIn(ixStart[dx], srcW - 1)
    }

    for (dy in 0 until dstH) {
      val y0 = dy * scaleY
      val y1 = (dy + 1) * scaleY
      val iy0 = floor(y0).toInt().coerceIn(0, srcH - 1)
      val iy1 = (ceil(y1).toInt() - 1).coerceIn(iy0, srcH - 1)
      val dstRowOffset = dy * dstW

      for (dx in 0 until dstW) {
        val x0 = xStartCoord[dx]
        val x1 = xEndCoord[dx]
        val sx0 = ixStart[dx]
        val sx1 = ixEnd[dx]

        var weightedSum = 0.0
        var totalWeight = 0.0

        for (sy in iy0..iy1) {
          val wy = min(y1, (sy + 1).toDouble()) - max(y0, sy.toDouble())
          if (wy <= 0.0) continue
          val srcRowOffset = sy * srcW
          for (sx in sx0..sx1) {
            val wx = min(x1, (sx + 1).toDouble()) - max(x0, sx.toDouble())
            if (wx <= 0.0) continue
            val w = wx * wy
            weightedSum += src[srcRowOffset + sx] * w
            totalWeight += w
          }
        }

        val averaged = if (totalWeight > 0.0) {
          (weightedSum / totalWeight).toFloat()
        } else {
          src[iy0 * srcW + sx0]
        }
        out[dstRowOffset + dx] = averaged.coerceIn(0.0f, 1.0f)
      }
    }

    return GrayscaleGrid(
      width = dstW,
      height = dstH,
      luminance = out,
    )
  }

  companion object {
    const val DEFAULT_CELL_WIDTH_TO_HEIGHT_RATIO: Float = 0.5f
  }
}
