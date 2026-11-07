package com.laralbir.img2asciiim.ui.converter

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.laralbir.img2asciiim.data.source.DeepLinkImageCodec
import com.laralbir.img2asciiim.data.source.DeepLinkMode
import com.laralbir.img2asciiim.data.update.GitHubUpdateChecker
import com.laralbir.img2asciiim.data.update.UpdateCheckResult
import com.laralbir.img2asciiim.domain.engine.ImageToAsciiUseCase
import com.laralbir.img2asciiim.domain.model.CharsetMode
import com.laralbir.img2asciiim.domain.model.ConversionParams
import com.laralbir.img2asciiim.domain.model.DecodedImage
import com.laralbir.img2asciiim.domain.model.DitherMode
import com.laralbir.img2asciiim.domain.model.DomainResult
import com.laralbir.img2asciiim.domain.model.ImPlatformPreset
import com.laralbir.img2asciiim.domain.model.ImageDecodeError
import com.laralbir.img2asciiim.domain.model.ShareOutcome
import com.laralbir.img2asciiim.domain.repository.ImageSourceRepository
import com.laralbir.img2asciiim.domain.repository.MessageShareGateway
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Unidirectional state & effect coordinator for the image-to-IM-art converter screen.
 *
 * Raw decoded pixels are cached privately in [currentDecodedImage] (never exposed in [uiState])
 * and converted via a `collectLatest` pipeline so rapid slider movements cancel stale work.
 */
class ConverterViewModel(
  private val imageSourceRepository: ImageSourceRepository,
  private val messageShareGateway: MessageShareGateway,
  private val imageToAsciiUseCase: ImageToAsciiUseCase = ImageToAsciiUseCase(),
  private val deepLinkCodec: DeepLinkImageCodec = DeepLinkImageCodec(),
  private val updateChecker: GitHubUpdateChecker = GitHubUpdateChecker(),
  private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
  loadDemoOnStart: Boolean = false,
) : ViewModel() {

  private val _uiState = MutableStateFlow(ConverterUiState())
  val uiState: StateFlow<ConverterUiState> = _uiState.asStateFlow()

  private val _effects = Channel<ConverterUiEffect>(Channel.BUFFERED)
  val effects: Flow<ConverterUiEffect> = _effects.receiveAsFlow()

  private var currentDecodedImage: DecodedImage? = null
  private var triggerGeneration: Long = 0L
  private val pipelineTrigger = MutableStateFlow<PipelineTrigger?>(null)
  private var decodeJob: Job? = null
  private var lastCheckedClipboardText: String? = null

  init {
    viewModelScope.launch {
      observeConversionPipeline()
    }
    if (loadDemoOnStart) {
      onLoadDemoSample()
    }
  }

  fun onGalleryUriSelected(uriString: String?) {
    if (uriString.isNullOrBlank()) return
    loadFromUri(
      uriString = uriString,
      origin = ImageSourceOrigin.GALLERY,
      defaultLabel = DEFAULT_GALLERY_LABEL,
    )
  }

  fun onDocumentUriSelected(uriString: String?) {
    if (uriString.isNullOrBlank()) return
    loadFromUri(
      uriString = uriString,
      origin = ImageSourceOrigin.DOCUMENTS_OR_CLOUD,
      defaultLabel = DEFAULT_DOCUMENT_LABEL,
    )
  }

  fun onCameraCaptureResult(uriString: String?, captured: Boolean) {
    if (!captured || uriString.isNullOrBlank()) return
    loadFromUri(
      uriString = uriString,
      origin = ImageSourceOrigin.CAMERA,
      defaultLabel = DEFAULT_CAMERA_LABEL,
    )
  }

  fun onIncomingShareUri(uriString: String?) {
    if (uriString.isNullOrBlank()) return
    loadFromUri(
      uriString = uriString,
      origin = ImageSourceOrigin.INCOMING_SHARE,
      defaultLabel = DEFAULT_INCOMING_SHARE_LABEL,
    )
  }

  fun onIncomingShareResult(outcome: DomainResult<DecodedImage, ImageDecodeError>) {
    decodeJob?.cancel()
    val origin = if (outcome is DomainResult.Success && outcome.value.label?.contains("Deep Link") == true) {
      ImageSourceOrigin.DEEP_LINK
    } else {
      ImageSourceOrigin.INCOMING_SHARE
    }
    applyDecodeOutcome(
      outcome = outcome,
      origin = origin,
      defaultLabel = DEFAULT_INCOMING_SHARE_LABEL,
    )
  }

  fun onLoadFromClipboard() {
    val clipboardText = messageShareGateway.getClipboardText()?.trim()
    if (clipboardText.isNullOrBlank()) {
      emitEffect(ConverterUiEffect.ShowSnackbar("El portapapeles está vacío."))
      return
    }
    if (!containsDeepLink(clipboardText)) {
      emitEffect(ConverterUiEffect.ShowSnackbar("No se encontró ningún enlace de imagen válido en el portapapeles."))
      return
    }

    lastCheckedClipboardText = clipboardText
    decodeJob?.cancel()
    decodeJob = viewModelScope.launch {
      _uiState.update { state ->
        state.copy(
          isLoadingImage = true,
          errorBannerMessage = null,
        )
      }
      val outcome = withContext(ioDispatcher) {
        deepLinkCodec.decodeFromDeepLink(clipboardText)
      }
      ensureActive()
      when (outcome) {
        is DomainResult.Success -> {
          applyDecodeOutcome(
            outcome = outcome,
            origin = ImageSourceOrigin.CLIPBOARD,
            defaultLabel = DEFAULT_CLIPBOARD_LABEL,
          )
        }
        is DomainResult.Failure -> {
          val errorMessage = when (outcome.error) {
            is ImageDecodeError.IncompleteMultiPart -> mapDecodeErrorMessage(outcome.error)
            else -> "El enlace de la imagen está incompleto o dañado (WhatsApp recorta enlaces que superan 4096 caracteres)."
          }
          _uiState.update { state ->
            state.copy(
              isLoadingImage = false,
              errorBannerMessage = errorMessage,
            )
          }
          emitEffect(ConverterUiEffect.ShowSnackbar(errorMessage))
        }
      }
    }
  }

  fun checkClipboardOnResume() {
    val clipboardText = messageShareGateway.getClipboardText()?.trim() ?: return
    if (clipboardText.isBlank() || clipboardText == lastCheckedClipboardText) return
    if (!containsDeepLink(clipboardText)) return

    lastCheckedClipboardText = clipboardText

    if (currentDecodedImage == null) {
      decodeJob?.cancel()
      decodeJob = viewModelScope.launch {
        _uiState.update { state ->
          state.copy(
            isLoadingImage = true,
            errorBannerMessage = null,
          )
        }
        val outcome = withContext(ioDispatcher) {
          deepLinkCodec.decodeFromDeepLink(clipboardText)
        }
        ensureActive()
        if (outcome is DomainResult.Success) {
          applyDecodeOutcome(
            outcome = outcome,
            origin = ImageSourceOrigin.CLIPBOARD,
            defaultLabel = DEFAULT_CLIPBOARD_LABEL,
          )
        } else {
          _uiState.update { it.copy(isLoadingImage = false) }
        }
      }
    } else {
      emitEffect(
        ConverterUiEffect.ShowSnackbar(
          message = "Enlace de imagen detectado en el portapapeles. ¿Deseas cargarlo?",
          actionLabel = "Cargar",
          onAction = { onLoadFromClipboard() },
        ),
      )
    }
  }

  internal fun containsDeepLink(text: String): Boolean {
    val trimmed = text.trim()
    return trimmed.contains("#d=") ||
      trimmed.contains("#data=") ||
      trimmed.contains("?d=") ||
      trimmed.contains("&d=") ||
      trimmed.contains("?data=") ||
      trimmed.contains("&data=") ||
      trimmed.contains("#p=") ||
      trimmed.contains("?p=") ||
      trimmed.contains("&p=") ||
      trimmed.contains("img2asciiim://") ||
      trimmed.contains("img2ascii://") ||
      trimmed.startsWith("UklGR") ||
      trimmed.startsWith("data:image/")
  }

  fun onLoadDemoSample() {
    decodeJob?.cancel()
    val decoded = imageSourceRepository.loadDemoSample()
    applyDecodedImage(
      decoded = decoded,
      origin = ImageSourceOrigin.DEMO,
      defaultLabel = DEFAULT_DEMO_LABEL,
    )
  }

  fun onSelectPreset(preset: ImPlatformPreset) {
    updateParams { current ->
      current.copy(
        preset = preset,
        charsetMode = preset.defaultCharset,
        widthColumns = preset.defaultWidthColumns,
      )
    }
  }

  fun onSelectCharsetMode(mode: CharsetMode) {
    updateParams { current -> current.copy(charsetMode = mode) }
  }

  fun onWidthColumnsChanged(columns: Int) {
    val clamped = columns.coerceIn(
      ImageToAsciiUseCase.MIN_REQUESTED_COLUMNS,
      ImageToAsciiUseCase.MAX_REQUESTED_COLUMNS,
    )
    updateParams { current -> current.copy(widthColumns = clamped) }
  }

  fun onBrightnessChanged(brightness: Float) {
    if (!brightness.isFinite()) return
    val clamped = brightness.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
    updateParams { current -> current.copy(brightness = clamped) }
  }

  fun onContrastChanged(contrast: Float) {
    if (!contrast.isFinite()) return
    val clamped = contrast.coerceIn(MIN_CONTRAST, MAX_CONTRAST)
    updateParams { current -> current.copy(contrast = clamped) }
  }

  fun onInvertToggled(invert: Boolean) {
    updateParams { current -> current.copy(invert = invert) }
  }

  fun onSelectDitherMode(mode: DitherMode) {
    updateParams { current -> current.copy(ditherMode = mode) }
  }

  fun onResetAdjustments() {
    updateParams { current ->
      current.copy(
        charsetMode = current.preset.defaultCharset,
        widthColumns = current.preset.defaultWidthColumns,
        brightness = DEFAULT_BRIGHTNESS,
        contrast = DEFAULT_CONTRAST,
        invert = DEFAULT_INVERT,
        ditherMode = DitherMode.FLOYD_STEINBERG,
      )
    }
  }

  fun onTogglePreviewBubbleTheme() {
    _uiState.update { state -> state.copy(previewDarkBubble = !state.previewDarkBubble) }
  }

  fun onSelectPreviewTab(tab: PreviewTab) {
    _uiState.update { it.copy(previewTab = tab) }
  }

  fun onSaveImageToGallery() {
    val decoded = currentDecodedImage
    if (decoded == null) {
      emitEffect(ConverterUiEffect.ShowSnackbar("No se pudo guardar la foto"))
      return
    }
    val success = messageShareGateway.saveImageToGallery(decoded.pixelGrid)
    if (success) {
      emitEffect(ConverterUiEffect.ShowSnackbar("Foto guardada en la Galería"))
    } else {
      emitEffect(ConverterUiEffect.ShowSnackbar("No se pudo guardar la foto"))
    }
  }

  fun onToggleIncludeAsciiArt(include: Boolean) {
    _uiState.update { state ->
      val newInclude = include
      val newAttach = if (!newInclude) true else state.attachDeepLink
      state.copy(includeAsciiArt = newInclude, attachDeepLink = newAttach)
    }
  }

  fun onToggleAttachDeepLink(attach: Boolean) {
    _uiState.update { state ->
      val newAttach = attach
      val newInclude = if (!newAttach) true else state.includeAsciiArt
      state.copy(attachDeepLink = newAttach, includeAsciiArt = newInclude)
    }
  }

  fun onCheckForUpdates(currentVersionName: String) {
    if (_uiState.value.isCheckingUpdate) return
    _uiState.update { it.copy(isCheckingUpdate = true) }
    viewModelScope.launch(ioDispatcher) {
      val result = updateChecker.check(currentVersionName)
      withContext(Dispatchers.Main) {
        _uiState.update { it.copy(isCheckingUpdate = false, updateCheckResult = result) }
      }
    }
  }

  fun onDismissUpdateDialog() {
    _uiState.update { it.copy(updateCheckResult = null) }
  }

  fun buildShareableMessage(): String? {
    val state = _uiState.value
    val result = state.conversionResult ?: return null
    val preset = state.params.preset

    if (preset == ImPlatformPreset.INSTAGRAM_DIRECT || !state.attachDeepLink) {
      return result.formattedMessage
    }

    val decoded = currentDecodedImage ?: return result.formattedMessage

    return if (!state.includeAsciiArt) {
      // EXPANDED_HD mode: ASCII preview omitted, dedicating all safe bytes to the photo!
      val deepLinkUrl = deepLinkCodec.encodeToDeepLink(
        pixelGrid = decoded.pixelGrid,
        mode = DeepLinkMode.EXPANDED_HD,
        preset = preset,
        maxCharacterBudget = DeepLinkImageCodec.MAX_SAFE_URL_LENGTH,
      )
      deepLinkUrl
    } else {
      // STANDARD mode: Both ASCII art and deep link thumbnail
      val art = result.formattedMessage
      val maxUrlBudget = when (preset) {
        ImPlatformPreset.TELEGRAM -> (preset.maxCharacters - art.length - 1)
          .coerceIn(200, DeepLinkImageCodec.MAX_SAFE_URL_LENGTH)
        ImPlatformPreset.WHATSAPP -> DeepLinkImageCodec.MAX_SAFE_URL_LENGTH
        ImPlatformPreset.INSTAGRAM_DIRECT -> (preset.maxCharacters - art.length - 1)
          .coerceIn(200, DeepLinkImageCodec.MAX_SAFE_URL_LENGTH)
      }
      val deepLinkUrl = deepLinkCodec.encodeToDeepLink(
        pixelGrid = decoded.pixelGrid,
        mode = DeepLinkMode.STANDARD,
        preset = preset,
        maxCharacterBudget = maxUrlBudget,
      )
      "$art\n$deepLinkUrl"
    }
  }

  fun onDismissErrorBanner() {
    _uiState.update { state -> state.copy(errorBannerMessage = null) }
  }

  fun onCopyClicked() {
    val message = buildShareableMessage() ?: return
    messageShareGateway.copyToClipboard(message)
    emitEffect(
      ConverterUiEffect.ShowSnackbar(
        "Arte copiado al portapapeles (${message.length} caracteres)",
      ),
    )
  }

  fun onShareClicked() {
    val state = _uiState.value
    val message = buildShareableMessage() ?: return
    val preset = state.params.preset
    val outcome = messageShareGateway.shareToPlatform(message, preset)
    emitEffect(
      ConverterUiEffect.ShowSnackbar(
        formatShareOutcomeMessage(
          outcome = outcome,
          preset = preset,
          charCount = message.length,
        ),
      ),
    )
  }

  override fun onCleared() {
    _effects.close()
    super.onCleared()
  }

  private suspend fun observeConversionPipeline() {
    pipelineTrigger
      .filterNotNull()
      .collectLatest { trigger ->
        _uiState.update { state -> state.copy(isConverting = true) }
        val result = withContext(computeDispatcher) {
          ensureActive()
          imageToAsciiUseCase(trigger.decodedImage.pixelGrid, trigger.params)
        }
        currentCoroutineContext().ensureActive()
        _uiState.update { state ->
          state.copy(
            isConverting = false,
            conversionResult = result,
          )
        }
      }
  }

  private fun loadFromUri(
    uriString: String,
    origin: ImageSourceOrigin,
    defaultLabel: String,
  ) {
    decodeJob?.cancel()
    decodeJob = viewModelScope.launch {
      _uiState.update { state ->
        state.copy(
          isLoadingImage = true,
          errorBannerMessage = null,
        )
      }
      val outcome = withContext(ioDispatcher) {
        imageSourceRepository.decodeFromUri(uriString)
      }
      ensureActive()
      applyDecodeOutcome(
        outcome = outcome,
        origin = origin,
        defaultLabel = defaultLabel,
      )
    }
  }

  private fun applyDecodeOutcome(
    outcome: DomainResult<DecodedImage, ImageDecodeError>,
    origin: ImageSourceOrigin,
    defaultLabel: String,
  ) {
    when (outcome) {
      is DomainResult.Success -> applyDecodedImage(
        decoded = outcome.value,
        origin = origin,
        defaultLabel = defaultLabel,
      )
      is DomainResult.Failure -> {
        val errorMessage = mapDecodeErrorMessage(outcome.error)
        _uiState.update { state ->
          state.copy(
            isLoadingImage = false,
            errorBannerMessage = errorMessage,
          )
        }
        emitEffect(ConverterUiEffect.ShowSnackbar(errorMessage))
      }
    }
  }

  private fun applyDecodedImage(
    decoded: DecodedImage,
    origin: ImageSourceOrigin,
    defaultLabel: String,
  ) {
    currentDecodedImage = decoded
    val resolvedLabel = if (origin == ImageSourceOrigin.CLIPBOARD) {
      defaultLabel
    } else {
      decoded.label?.takeIf { it.isNotBlank() } ?: defaultLabel
    }
    val dimensionsLabel = formatDimensions(decoded.originalWidth, decoded.originalHeight)
    val activeParams = _uiState.value.params

    val bitmap = try {
      Bitmap.createBitmap(
        decoded.pixelGrid.argbPixels,
        decoded.pixelGrid.width,
        decoded.pixelGrid.height,
        Bitmap.Config.ARGB_8888,
      )
    } catch (_: Throwable) {
      null
    }

    _uiState.update { state ->
      val resolvedTab = if (
        origin == ImageSourceOrigin.CLIPBOARD ||
        origin == ImageSourceOrigin.DEEP_LINK ||
        origin == ImageSourceOrigin.INCOMING_SHARE
      ) {
        PreviewTab.REAL_IMAGE
      } else {
        state.previewTab
      }

      state.copy(
        isLoadingImage = false,
        isConverting = true,
        sourceOrigin = origin,
        sourceLabel = resolvedLabel,
        sourceDimensionsLabel = dimensionsLabel,
        previewBitmap = bitmap,
        previewTab = resolvedTab,
        errorBannerMessage = null,
      )
    }
    enqueueConversion(decoded = decoded, params = activeParams)
  }

  private inline fun updateParams(transform: (ConversionParams) -> ConversionParams) {
    val currentState = _uiState.value
    val updatedParams = transform(currentState.params)
    if (updatedParams == currentState.params) return

    val decoded = currentDecodedImage
    _uiState.update { state ->
      state.copy(
        params = updatedParams,
        isConverting = decoded != null,
      )
    }
    if (decoded != null) {
      enqueueConversion(decoded = decoded, params = updatedParams)
    }
  }

  private fun enqueueConversion(decoded: DecodedImage, params: ConversionParams) {
    pipelineTrigger.value = PipelineTrigger(
      generation = ++triggerGeneration,
      decodedImage = decoded,
      params = params,
    )
  }

  private fun emitEffect(effect: ConverterUiEffect) {
    _effects.trySend(effect)
  }

  private fun formatDimensions(width: Int, height: Int): String = "$width × $height px"

  private fun mapDecodeErrorMessage(error: ImageDecodeError): String = when (error) {
    ImageDecodeError.CloudFileOfflineUnavailable -> ERROR_CLOUD_OFFLINE
    ImageDecodeError.InvalidUriScheme,
    ImageDecodeError.SecurityViolation -> ERROR_SECURITY_BLOCKED
    ImageDecodeError.InvalidOrCorruptedImage -> ERROR_CORRUPTED_IMAGE
    is ImageDecodeError.IncompleteMultiPart ->
      "Parte ${error.currentPart} de ${error.totalParts} cargada. Abre o copia la parte restante para completar la foto."
    is ImageDecodeError.IoFailure -> ERROR_IO_FAILURE
  }

  private fun formatShareOutcomeMessage(
    outcome: ShareOutcome,
    preset: ImPlatformPreset,
    charCount: Int,
  ): String = when (outcome) {
    is ShareOutcome.OpenedDirectApp -> {
      if (outcome.copiedToClipboard) {
        "Copiado al portapapeles. Si ${preset.displayName} recorta el mensaje, mantén pulsado y pulsa Pegar."
      } else {
        "Abriendo ${preset.displayName} para enviar arte ($charCount caracteres)."
      }
    }
    is ShareOutcome.OpenedSystemChooser -> {
      if (outcome.copiedToClipboard) {
        "Arte copiado al portapapeles (${preset.displayName} no detectado). Elige una app y pega el mensaje."
      } else {
        "${preset.displayName} no detectado; abriendo selector del sistema."
      }
    }
  }

  /**
   * Placing [generation] first guarantees O(1) short-circuit inequality inside [MutableStateFlow]
   * without comparing [DecodedImage] pixel buffers.
   */
  private data class PipelineTrigger(
    val generation: Long,
    val decodedImage: DecodedImage,
    val params: ConversionParams,
  )

  companion object {
    const val DEFAULT_DEMO_LABEL: String = "Ventanilla a bordo (Demo)"
    const val DEFAULT_GALLERY_LABEL: String = "Foto de galería"
    const val DEFAULT_DOCUMENT_LABEL: String = "Archivo / Google Drive"
    const val DEFAULT_CAMERA_LABEL: String = "Foto de cámara"
    const val DEFAULT_INCOMING_SHARE_LABEL: String = "Imagen recibida"
    const val DEFAULT_CLIPBOARD_LABEL: String = "Enlace del portapapeles"

    const val MIN_BRIGHTNESS: Float = -1.0f
    const val MAX_BRIGHTNESS: Float = 1.0f
    const val MIN_CONTRAST: Float = 0.5f
    const val MAX_CONTRAST: Float = 3.0f
    const val DEFAULT_BRIGHTNESS: Float = 0.0f
    const val DEFAULT_CONTRAST: Float = 1.0f
    const val DEFAULT_INVERT: Boolean = true

    const val ERROR_CLOUD_OFFLINE: String =
      "Este archivo en la nube (ej. Google Drive) no está disponible sin conexión. En modo avión usa una imagen local o marcada como «Disponible sin conexión»."
    const val ERROR_SECURITY_BLOCKED: String =
      "Origen de imagen bloqueado por validación de seguridad."
    const val ERROR_CORRUPTED_IMAGE: String =
      "El archivo seleccionado no es una imagen válida o está dañado."
    const val ERROR_IO_FAILURE: String =
      "No se pudo leer la imagen seleccionada."

    fun provideFactory(
      imageSourceRepository: ImageSourceRepository,
      messageShareGateway: MessageShareGateway,
    ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(ConverterViewModel::class.java)) {
          "Unknown ViewModel class: ${modelClass.name}"
        }
        return ConverterViewModel(
          imageSourceRepository = imageSourceRepository,
          messageShareGateway = messageShareGateway,
        ) as T
      }

      override fun <T : ViewModel> create(modelClass: Class<T>): T =
        create(modelClass, CreationExtras.Empty)
    }
  }
}
