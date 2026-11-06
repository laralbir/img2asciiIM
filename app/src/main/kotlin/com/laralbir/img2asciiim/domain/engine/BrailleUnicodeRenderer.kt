package com.laralbir.img2asciiim.domain.engine

/**
 * Encodes binary `2x4` subpixel blocks into ISO/TR 11548-1 Unicode Braille (`U+2800..U+28FF`).
 * Empty cells strictly emit `U+2800` (`'⠀'`), never ASCII space `U+0020`.
 */
class BrailleUnicodeRenderer {

  fun render(
    subpixelLevels: IntArray,
    sampleWidth: Int,
    sampleHeight: Int,
  ): String {
    require(sampleWidth >= 2 && sampleWidth % 2 == 0) {
      "sampleWidth must be a positive multiple of 2, was $sampleWidth"
    }
    require(sampleHeight >= 4 && sampleHeight % 4 == 0) {
      "sampleHeight must be a positive multiple of 4, was $sampleHeight"
    }
    require(subpixelLevels.size == sampleWidth * sampleHeight) {
      "subpixelLevels.size (${subpixelLevels.size}) must equal ${sampleWidth * sampleHeight}"
    }

    val cols = sampleWidth / 2
    val rows = sampleHeight / 4
    val sb = StringBuilder(rows * (cols + 1))

    for (cellY in 0 until rows) {
      if (cellY > 0) {
        sb.append('\n')
      }
      val y0 = (cellY * 4) * sampleWidth
      val y1 = y0 + sampleWidth
      val y2 = y1 + sampleWidth
      val y3 = y2 + sampleWidth

      for (cellX in 0 until cols) {
        val x0 = cellX * 2
        val x1 = x0 + 1
        var mask = 0

        if (subpixelLevels[y0 + x0] > 0) mask = mask or 0x01 // (0,0) -> Dot 1
        if (subpixelLevels[y1 + x0] > 0) mask = mask or 0x02 // (0,1) -> Dot 2
        if (subpixelLevels[y2 + x0] > 0) mask = mask or 0x04 // (0,2) -> Dot 3
        if (subpixelLevels[y3 + x0] > 0) mask = mask or 0x40 // (0,3) -> Dot 7
        if (subpixelLevels[y0 + x1] > 0) mask = mask or 0x08 // (1,0) -> Dot 4
        if (subpixelLevels[y1 + x1] > 0) mask = mask or 0x10 // (1,1) -> Dot 5
        if (subpixelLevels[y2 + x1] > 0) mask = mask or 0x20 // (1,2) -> Dot 6
        if (subpixelLevels[y3 + x1] > 0) mask = mask or 0x80 // (1,3) -> Dot 8

        sb.append(Char(BRAILLE_BASE_CODE_POINT + mask))
      }
    }

    return sb.toString()
  }

  companion object {
    const val BRAILLE_BASE_CODE_POINT: Int = 0x2800
  }
}
