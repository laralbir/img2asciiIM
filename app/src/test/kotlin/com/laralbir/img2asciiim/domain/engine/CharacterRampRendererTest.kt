package com.laralbir.img2asciiim.domain.engine

import com.laralbir.img2asciiim.domain.model.CharsetMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CharacterRampRendererTest {

  private val renderer = CharacterRampRenderer()

  @Test
  fun rendersAsciiClassicMonotonicallyWithoutSpaceOrBacktick() {
    assertEquals(9, renderer.levelCount(CharsetMode.ASCII_CLASSIC, usesMonospaceFence = true))
    val quantized = IntArray(9) { it }
    val output = renderer.render(
      quantizedLevels = quantized,
      columns = 9,
      rows = 1,
      charsetMode = CharsetMode.ASCII_CLASSIC,
      usesMonospaceFence = true,
    )
    assertEquals(".:-=+*#%@", output)
    assertFalse(output.contains(' '))
    assertFalse(output.contains('`'))
  }

  @Test
  fun switchesUnicodeBlocksLightestGlyphBetweenMonospaceFenceAndProportionalUnfenced() {
    assertEquals(5, renderer.levelCount(CharsetMode.UNICODE_BLOCKS, usesMonospaceFence = true))
    val quantized = intArrayOf(0, 1, 2, 3, 4)

    val fenced = renderer.render(
      quantizedLevels = quantized,
      columns = 5,
      rows = 1,
      charsetMode = CharsetMode.UNICODE_BLOCKS,
      usesMonospaceFence = true,
    )
    assertEquals(".░▒▓█", fenced)
    assertFalse(fenced.contains(' '))

    val unfenced = renderer.render(
      quantizedLevels = quantized,
      columns = 5,
      rows = 1,
      charsetMode = CharsetMode.UNICODE_BLOCKS,
      usesMonospaceFence = false,
    )
    assertEquals("\u2800░▒▓█", unfenced)
    assertFalse(unfenced.contains(' '))
  }
}
