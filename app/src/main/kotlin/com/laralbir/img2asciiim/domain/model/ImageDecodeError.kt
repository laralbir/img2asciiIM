package com.laralbir.img2asciiim.domain.model

/**
 * Typed domain failure reasons when ingesting or decoding an external image source.
 */
sealed interface ImageDecodeError {
  data object InvalidUriScheme : ImageDecodeError
  data object SecurityViolation : ImageDecodeError
  data object CloudFileOfflineUnavailable : ImageDecodeError
  data object InvalidOrCorruptedImage : ImageDecodeError
  data class IncompleteMultiPart(val currentPart: Int, val totalParts: Int) : ImageDecodeError
  data class IoFailure(val reason: String? = null) : ImageDecodeError
}
