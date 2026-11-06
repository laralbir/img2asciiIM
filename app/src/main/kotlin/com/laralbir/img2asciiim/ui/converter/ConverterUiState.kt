package com.laralbir.img2asciiim.ui.converter

import com.laralbir.img2asciiim.data.update.UpdateCheckResult
import com.laralbir.img2asciiim.domain.model.AsciiConversionResult
import com.laralbir.img2asciiim.domain.model.ConversionParams

/**
 * Origin of the currently active source image displayed in the converter workspace.
 */
enum class ImageSourceOrigin(val displayName: String) {
  DEMO("Muestra a bordo"),
  GALLERY("Galería"),
  DOCUMENTS_OR_CLOUD("Archivos / Drive"),
  CAMERA("Cámara"),
  INCOMING_SHARE("Compartido"),
  DEEP_LINK("Enlace HD"),
}

/**
 * Immutable UI state for the converter screen.
 *
 * Raw pixel buffers (`PixelGrid` / `DecodedImage`) are intentionally excluded from UI state
 * and cached privately inside [ConverterViewModel] so Compose state equality checks stay O(1).
 */
data class ConverterUiState(
  val isLoadingImage: Boolean = false,
  val isConverting: Boolean = false,
  val sourceOrigin: ImageSourceOrigin = ImageSourceOrigin.DEMO,
  val sourceLabel: String = "Ventanilla a bordo (Demo)",
  val sourceDimensionsLabel: String = "",
  val params: ConversionParams = ConversionParams(),
  val previewDarkBubble: Boolean = true,
  val includeAsciiArt: Boolean = true,
  val attachDeepLink: Boolean = false,
  val conversionResult: AsciiConversionResult? = null,
  val errorBannerMessage: String? = null,
  val isCheckingUpdate: Boolean = false,
  val updateCheckResult: UpdateCheckResult? = null,
)
