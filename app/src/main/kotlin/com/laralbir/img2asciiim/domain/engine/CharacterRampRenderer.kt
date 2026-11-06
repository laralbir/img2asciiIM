package com.laralbir.img2asciiim.domain.engine

import com.laralbir.img2asciiim.domain.model.CharsetMode

/**
 * Renders quantized ink levels onto safe character ramps free of ASCII spaces (`U+0020`)
 * and backticks (`U+0060`) to prevent Markdown fence breaks or proportional whitespace collapse.
 */
class CharacterRampRenderer {

  fun rampFor(charsetMode: CharsetMode, usesMonospaceFence: Boolean): String = when (charsetMode) {
    CharsetMode.ASCII_CLASSIC -> ASCII_CLASSIC_RAMP
    CharsetMode.UNICODE_BLOCKS -> if (usesMonospaceFence) {
      UNICODE_BLOCKS_FENCED_RAMP
    } else {
      UNICODE_BLOCKS_UNFENCED_RAMP
    }
    CharsetMode.BRAILLE_UNICODE -> error("Use BrailleUnicodeRenderer for BRAILLE_UNICODE")
  }

  fun levelCount(charsetMode: CharsetMode, usesMonospaceFence: Boolean): Int = when (charsetMode) {
    CharsetMode.BRAILLE_UNICODE -> 2
    CharsetMode.ASCII_CLASSIC,
    CharsetMode.UNICODE_BLOCKS -> rampFor(charsetMode, usesMonospaceFence).length
  }

  fun render(
    quantizedLevels: IntArray,
    columns: Int,
    rows: Int,
    charsetMode: CharsetMode,
    usesMonospaceFence: Boolean,
  ): String {
    require(columns > 0 && rows > 0) { "Dimensions must be > 0, got ${columns}x$rows" }
    require(quantizedLevels.size == columns * rows) {
      "quantizedLevels.size (${quantizedLevels.size}) must equal ${columns * rows}"
    }

    val ramp = rampFor(charsetMode, usesMonospaceFence)
    val maxIdx = ramp.length - 1
    val sb = StringBuilder(rows * (columns + 1))

    for (y in 0 until rows) {
      if (y > 0) {
        sb.append('\n')
      }
      val rowOffset = y * columns
      for (x in 0 until columns) {
        val level = quantizedLevels[rowOffset + x].coerceIn(0, maxIdx)
        sb.append(ramp[level])
      }
    }

    return sb.toString()
  }

  companion object {
    const val ASCII_CLASSIC_RAMP: String = ".:-=+*#%@"
    const val UNICODE_BLOCKS_FENCED_RAMP: String = ".░▒▓█"
    const val UNICODE_BLOCKS_UNFENCED_RAMP: String = "\u2800░▒▓█"
  }
}
