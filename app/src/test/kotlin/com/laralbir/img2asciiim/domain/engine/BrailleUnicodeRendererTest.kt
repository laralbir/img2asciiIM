package com.laralbir.img2asciiim.domain.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BrailleUnicodeRendererTest {

  private val renderer = BrailleUnicodeRenderer()

  @Test
  fun mapsAllEightIso11548SubpixelBitsExactly() {
    val expectedMasks = mapOf(
      (0 to 0) to 0x01,
      (0 to 1) to 0x02,
      (0 to 2) to 0x04,
      (0 to 3) to 0x40, // Critical: dot 7 is 0x40, NOT 0x08!
      (1 to 0) to 0x08, // Critical: dot 4 is 0x08, NOT 0x40!
      (1 to 1) to 0x10,
      (1 to 2) to 0x20,
      (1 to 3) to 0x80,
    )

    for ((coord, mask) in expectedMasks) {
      val (dx, dy) = coord
      val subpixels = IntArray(2 * 4)
      subpixels[dy * 2 + dx] = 1

      val rendered = renderer.render(subpixels, sampleWidth = 2, sampleHeight = 4)
      assertEquals(1, rendered.length)
      assertEquals(Char(0x2800 + mask), rendered[0], "Mismatch at subpixel ($dx, $dy)")
    }
  }

  @Test
  fun emitsBrailleBlankFullBlockAndDiagonalWithoutAsciiSpaceAndSurvivesTrim() {
    // CA-02.1 + CB-05: 3 cells side by side -> sampleWidth = 6, sampleHeight = 4
    // Cell 0: empty (all 0) -> U+2800 '⠀'
    // Cell 1: full (all 1) -> U+28FF '⣿'
    // Cell 2: (0,0) + (1,3) -> U+2881 '⢁'
    // Cell 3: empty (all 0) -> U+2800 '⠀' (to test trailing trim!)
    val sampleWidth = 8
    val sampleHeight = 4
    val subpixels = IntArray(sampleWidth * sampleHeight)

    // Fill cell 1 (x = 2..3, y = 0..3)
    for (y in 0 until 4) {
      for (x in 2..3) {
        subpixels[y * sampleWidth + x] = 1
      }
    }
    // Cell 2 (baseX = 4): (0,0) -> (4,0) and (1,3) -> (5,3)
    subpixels[0 * sampleWidth + 4] = 1
    subpixels[3 * sampleWidth + 5] = 1

    val rendered = renderer.render(subpixels, sampleWidth = sampleWidth, sampleHeight = sampleHeight)
    assertEquals("\u2800\u28FF\u2881\u2800", rendered)
    assertFalse(rendered.contains(' '), "Braille output must NEVER contain ASCII space U+0020")
    assertEquals(rendered, rendered.trim(), "U+2800 must survive standard Kotlin String.trim()")
  }
}
