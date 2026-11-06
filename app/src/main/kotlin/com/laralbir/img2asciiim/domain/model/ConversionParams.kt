package com.laralbir.img2asciiim.domain.model

/**
 * User-configurable parameters controlling image-to-text conversion.
 */
data class ConversionParams(
  val preset: ImPlatformPreset = ImPlatformPreset.WHATSAPP,
  val charsetMode: CharsetMode = preset.defaultCharset,
  val widthColumns: Int = preset.defaultWidthColumns,
  val brightness: Float = 0.0f,
  val contrast: Float = 1.0f,
  val invert: Boolean = true,
  val ditherMode: DitherMode = DitherMode.FLOYD_STEINBERG,
)
