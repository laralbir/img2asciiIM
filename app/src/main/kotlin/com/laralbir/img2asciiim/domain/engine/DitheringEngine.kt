package com.laralbir.img2asciiim.domain.engine

import com.laralbir.img2asciiim.domain.model.DitherMode
import com.laralbir.img2asciiim.domain.model.GrayscaleGrid
import kotlin.math.roundToInt

/**
 * Quantizes an ink-density [GrayscaleGrid] (`0.0f..1.0f`) into integer levels `0..(levels - 1)`
 * using direct thresholding, Floyd-Steinberg (`7/16, 3/16, 5/16, 1/16`), or Atkinson (`6 x 1/8`).
 */
class DitheringEngine {

  fun quantize(
    grid: GrayscaleGrid,
    levels: Int,
    mode: DitherMode,
  ): IntArray {
    require(levels >= 2) { "levels must be >= 2, was $levels" }
    val width = grid.width
    val height = grid.height
    val maxLevel = levels - 1
    val maxLevelFloat = maxLevel.toFloat()
    val invMaxLevel = 1.0f / maxLevelFloat
    val out = IntArray(width * height)

    if (mode == DitherMode.NONE) {
      val src = grid.luminance
      for (i in src.indices) {
        out[i] = (src[i].coerceIn(0.0f, 1.0f) * maxLevelFloat).roundToInt().coerceIn(0, maxLevel)
      }
      return out
    }

    val work = grid.luminance.copyOf()
    for (y in 0 until height) {
      val rowOffset = y * width
      for (x in 0 until width) {
        val idx = rowOffset + x
        val current = work[idx].coerceIn(0.0f, 1.0f)
        val qIndex = (current * maxLevelFloat).roundToInt().coerceIn(0, maxLevel)
        out[idx] = qIndex

        val qValue = qIndex * invMaxLevel
        val error = current - qValue
        if (error == 0.0f) continue

        when (mode) {
          DitherMode.FLOYD_STEINBERG -> {
            if (x + 1 < width) {
              work[idx + 1] += error * (7.0f / 16.0f)
            }
            if (y + 1 < height) {
              val nextRow = rowOffset + width
              if (x - 1 >= 0) {
                work[nextRow + x - 1] += error * (3.0f / 16.0f)
              }
              work[nextRow + x] += error * (5.0f / 16.0f)
              if (x + 1 < width) {
                work[nextRow + x + 1] += error * (1.0f / 16.0f)
              }
            }
          }

          DitherMode.ATKINSON -> {
            val share = error * 0.125f // 1/8
            if (x + 1 < width) {
              work[idx + 1] += share
            }
            if (x + 2 < width) {
              work[idx + 2] += share
            }
            if (y + 1 < height) {
              val nextRow = rowOffset + width
              if (x - 1 >= 0) {
                work[nextRow + x - 1] += share
              }
              work[nextRow + x] += share
              if (x + 1 < width) {
                work[nextRow + x + 1] += share
              }
            }
            if (y + 2 < height) {
              work[(y + 2) * width + x] += share
            }
          }

          DitherMode.NONE -> Unit
        }
      }
    }

    return out
  }
}
