package com.laralbir.img2asciiim.domain.engine

import com.laralbir.img2asciiim.domain.model.CharsetMode
import com.laralbir.img2asciiim.domain.model.ImPlatformPreset

/**
 * Platform-specific Markdown fence and typography policy (`RF-03`):
 * - WhatsApp: compact ```` ```<rawArt>``` ```` (`+6` chars, zero leading space so Roboto Mono engages).
 * - Telegram + ASCII: ```` ```\n<rawArt>\n``` ```` (`+8` chars, `\n` protects row 1 from language tag parsing).
 * - Telegram + Braille/Blocks: unwrapped plain text (`ACTION_SEND` avoids literal backticks).
 * - Instagram Direct: never fenced (`usesMonospaceFence = false`), warns if `ASCII_CLASSIC` is forced.
 */
class ImMessageFormatter {

  fun usesMonospaceFence(preset: ImPlatformPreset, charsetMode: CharsetMode): Boolean = when (preset) {
    ImPlatformPreset.WHATSAPP -> charsetMode == CharsetMode.ASCII_CLASSIC
    ImPlatformPreset.TELEGRAM -> charsetMode == CharsetMode.ASCII_CLASSIC
    ImPlatformPreset.INSTAGRAM_DIRECT -> false
  }

  fun hasProportionalFontWarning(preset: ImPlatformPreset, charsetMode: CharsetMode): Boolean =
    (preset == ImPlatformPreset.INSTAGRAM_DIRECT || preset == ImPlatformPreset.WHATSAPP) &&
      charsetMode == CharsetMode.ASCII_CLASSIC

  fun format(
    rawArt: String,
    preset: ImPlatformPreset,
    charsetMode: CharsetMode,
  ): String {
    val sanitized = rawArt.replace('`', '\'').replace("\r\n", "\n").replace('\r', '\n')
    return when (preset) {
      ImPlatformPreset.WHATSAPP -> if (charsetMode == CharsetMode.ASCII_CLASSIC) {
        "```\n$sanitized\n```"
      } else {
        sanitized
      }
      ImPlatformPreset.TELEGRAM -> if (charsetMode == CharsetMode.ASCII_CLASSIC) {
        "```\n$sanitized\n```"
      } else {
        sanitized
      }
      ImPlatformPreset.INSTAGRAM_DIRECT -> sanitized
    }
  }
}
