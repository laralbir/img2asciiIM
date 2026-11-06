package com.laralbir.img2asciiim.domain.model

/**
 * Downsampled and EXIF-normalized image ready for in-memory ASCII/Braille conversion.
 */
data class DecodedImage(
  val pixelGrid: PixelGrid,
  val originalWidth: Int,
  val originalHeight: Int,
  val label: String? = null,
)
