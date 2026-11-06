package com.laralbir.img2asciiim.domain.model

/**
 * Outcome of dispatching a formatted ASCII/Braille message to an IM platform.
 */
sealed interface ShareOutcome {
  data class OpenedDirectApp(
    val packageName: String,
    val copiedToClipboard: Boolean = false,
  ) : ShareOutcome

  data class OpenedSystemChooser(
    val copiedToClipboard: Boolean = false,
  ) : ShareOutcome
}
