package com.laralbir.img2asciiim.domain.model

/**
 * Quantization and error-diffusion dithering algorithms.
 */
enum class DitherMode(val displayName: String) {
  NONE("Sin trama"),
  FLOYD_STEINBERG("Floyd-Steinberg"),
  ATKINSON("Atkinson"),
}
