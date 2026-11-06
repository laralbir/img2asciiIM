package com.laralbir.img2asciiim.ui.converter

/**
 * One-off UI effects emitted by [ConverterViewModel].
 */
sealed interface ConverterUiEffect {
  data class ShowSnackbar(val message: String) : ConverterUiEffect
}
