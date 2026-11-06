package com.laralbir.img2asciiim.domain.model

/**
 * Supported monochrome text art rendering modes (`DD-03 Opción A`).
 */
enum class CharsetMode(val displayName: String) {
  BRAILLE_UNICODE("Braille HD"),
  ASCII_CLASSIC("ASCII"),
  UNICODE_BLOCKS("Bloques"),
}
