package com.laralbir.img2asciiim.ui.converter

import app.cash.turbine.test
import com.laralbir.img2asciiim.domain.engine.DemoSampleImageFactory
import com.laralbir.img2asciiim.domain.model.CharsetMode
import com.laralbir.img2asciiim.domain.model.DecodedImage
import com.laralbir.img2asciiim.domain.model.DitherMode
import com.laralbir.img2asciiim.domain.model.DomainResult
import com.laralbir.img2asciiim.domain.model.ImPlatformPreset
import com.laralbir.img2asciiim.domain.model.ImageDecodeError
import com.laralbir.img2asciiim.domain.model.PixelGrid
import com.laralbir.img2asciiim.domain.model.ShareOutcome
import com.laralbir.img2asciiim.domain.repository.ImageSourceRepository
import com.laralbir.img2asciiim.domain.repository.MessageShareGateway
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ConverterViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @AfterTest
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun coldStart_loadsDemoSampleAndEmitsNonEmptyWhatsAppConversionResult() =
    runTest(testDispatcher) {
      val repository = FakeImageSourceRepository()
      val shareGateway = FakeMessageShareGateway()
      val viewModel = ConverterViewModel(
        imageSourceRepository = repository,
        messageShareGateway = shareGateway,
        computeDispatcher = testDispatcher,
        ioDispatcher = testDispatcher,
        loadDemoOnStart = true,
      )

      viewModel.uiState.test {
        val initial = awaitItem()
        assertEquals(ImageSourceOrigin.DEMO, initial.sourceOrigin)
        assertEquals(DemoSampleImageFactory.DEFAULT_SAMPLE_LABEL, initial.sourceLabel)
        assertEquals("160 × 120 px", initial.sourceDimensionsLabel)
        assertEquals(ImPlatformPreset.WHATSAPP, initial.params.preset)

        advanceUntilIdle()
        val settled = expectMostRecentItem()
        assertFalse(settled.isLoadingImage)
        assertFalse(settled.isConverting)
        assertNull(settled.errorBannerMessage)

        val result = assertNotNull(settled.conversionResult)
        assertTrue(result.rawArt.isNotBlank())
        assertEquals(CharsetMode.BRAILLE_UNICODE, settled.params.charsetMode)
        assertEquals(result.rawArt, result.formattedMessage)
        assertFalse(result.usesMonospaceFence)
        assertTrue(result.charCount <= ImPlatformPreset.WHATSAPP.maxCharacters)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun selectingGalleryDocumentCameraAndIncomingShareUris_decodesAndUpdatesOriginDimensionsAndArt() =
    runTest(testDispatcher) {
      val repository = FakeImageSourceRepository()
      val shareGateway = FakeMessageShareGateway()

      val galleryUri = "content://media/external/images/media/101"
      val documentUri = "content://com.android.providers.downloads.documents/document/202"
      val cameraUri = "content://com.laralbir.img2asciiim.fileprovider/camera_captures/cap_303.jpg"
      val shareUri = "content://com.whatsapp.provider.media/item/404"

      repository.uriResponses[galleryUri] = DomainResult.Success(
        createGradientDecodedImage(width = 64, height = 80, origW = 1080, origH = 1350, label = null),
      )
      repository.uriResponses[documentUri] = DomainResult.Success(
        createGradientDecodedImage(
          width = 80,
          height = 40,
          origW = 2400,
          origH = 1200,
          label = "boarding-pass.png",
        ),
      )
      repository.uriResponses[cameraUri] = DomainResult.Success(
        createGradientDecodedImage(width = 60, height = 60, origW = 3024, origH = 3024, label = null),
      )
      repository.uriResponses[shareUri] = DomainResult.Success(
        createGradientDecodedImage(width = 48, height = 64, origW = 720, origH = 960, label = null),
      )

      val viewModel = ConverterViewModel(
        imageSourceRepository = repository,
        messageShareGateway = shareGateway,
        computeDispatcher = testDispatcher,
        ioDispatcher = testDispatcher,
        loadDemoOnStart = true,
      )
      advanceUntilIdle()

      viewModel.uiState.test {
        awaitItem() // initial demo state

        viewModel.onGalleryUriSelected(galleryUri)
        advanceUntilIdle()
        val galleryState = expectMostRecentItem()
        assertEquals(ImageSourceOrigin.GALLERY, galleryState.sourceOrigin)
        assertEquals("Foto de galería", galleryState.sourceLabel)
        assertEquals("1080 × 1350 px", galleryState.sourceDimensionsLabel)
        assertNotNull(galleryState.conversionResult)

        viewModel.onDocumentUriSelected(documentUri)
        advanceUntilIdle()
        val docState = expectMostRecentItem()
        assertEquals(ImageSourceOrigin.DOCUMENTS_OR_CLOUD, docState.sourceOrigin)
        assertEquals("boarding-pass.png", docState.sourceLabel)
        assertEquals("2400 × 1200 px", docState.sourceDimensionsLabel)
        assertNotNull(docState.conversionResult)

        viewModel.onCameraCaptureResult(cameraUri, captured = true)
        advanceUntilIdle()
        val cameraState = expectMostRecentItem()
        assertEquals(ImageSourceOrigin.CAMERA, cameraState.sourceOrigin)
        assertEquals("Foto de cámara", cameraState.sourceLabel)
        assertEquals("3024 × 3024 px", cameraState.sourceDimensionsLabel)
        assertNotNull(cameraState.conversionResult)

        viewModel.onIncomingShareUri(shareUri)
        advanceUntilIdle()
        val shareUriState = expectMostRecentItem()
        assertEquals(ImageSourceOrigin.INCOMING_SHARE, shareUriState.sourceOrigin)
        assertEquals("Imagen recibida", shareUriState.sourceLabel)
        assertEquals("720 × 960 px", shareUriState.sourceDimensionsLabel)
        assertNotNull(shareUriState.conversionResult)

        val directShareImage = createGradientDecodedImage(
          width = 50,
          height = 50,
          origW = 640,
          origH = 640,
          label = "shared_meme.webp",
        )
        viewModel.onIncomingShareResult(DomainResult.Success(directShareImage))
        advanceUntilIdle()
        val directShareState = expectMostRecentItem()
        assertEquals(ImageSourceOrigin.INCOMING_SHARE, directShareState.sourceOrigin)
        assertEquals("shared_meme.webp", directShareState.sourceLabel)
        assertEquals("640 × 640 px", directShareState.sourceDimensionsLabel)
        assertNotNull(directShareState.conversionResult)

        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun pickerAndCameraCancellation_preservesPreviousConversionResultUntouched() =
    runTest(testDispatcher) {
      val repository = FakeImageSourceRepository()
      val shareGateway = FakeMessageShareGateway()
      val viewModel = ConverterViewModel(
        imageSourceRepository = repository,
        messageShareGateway = shareGateway,
        computeDispatcher = testDispatcher,
        ioDispatcher = testDispatcher,
        loadDemoOnStart = true,
      )
      advanceUntilIdle()

      viewModel.uiState.test {
        val before = awaitItem()
        val previousArt = assertNotNull(before.conversionResult)

        viewModel.onGalleryUriSelected(null)
        viewModel.onGalleryUriSelected("   ")
        viewModel.onDocumentUriSelected(null)
        viewModel.onDocumentUriSelected("")
        viewModel.onCameraCaptureResult("content://camera/temp.jpg", captured = false)
        viewModel.onCameraCaptureResult(null, captured = true)
        viewModel.onIncomingShareUri(null)
        viewModel.onIncomingShareUri(" ")
        advanceUntilIdle()

        expectNoEvents()
        assertEquals(0, repository.decodeCallCount)
        assertEquals(previousArt, viewModel.uiState.value.conversionResult)
        assertEquals(ImageSourceOrigin.DEMO, viewModel.uiState.value.sourceOrigin)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun switchingToInstagramDirect_selectsBrailleStripsBackticksFits1000AndWarnsOnForcedAscii() =
    runTest(testDispatcher) {
      val repository = FakeImageSourceRepository(
        demoImage = createGradientDecodedImage(width = 120, height = 240, origW = 1200, origH = 2400),
      )
      val shareGateway = FakeMessageShareGateway()
      val viewModel = ConverterViewModel(
        imageSourceRepository = repository,
        messageShareGateway = shareGateway,
        computeDispatcher = testDispatcher,
        ioDispatcher = testDispatcher,
        loadDemoOnStart = true,
      )
      advanceUntilIdle()

      viewModel.uiState.test {
        awaitItem()

        viewModel.onSelectPreset(ImPlatformPreset.INSTAGRAM_DIRECT)
        advanceUntilIdle()
        val igBrailleState = expectMostRecentItem()
        assertEquals(ImPlatformPreset.INSTAGRAM_DIRECT, igBrailleState.params.preset)
        assertEquals(CharsetMode.BRAILLE_UNICODE, igBrailleState.params.charsetMode)
        assertEquals(
          ImPlatformPreset.INSTAGRAM_DIRECT.defaultWidthColumns,
          igBrailleState.params.widthColumns,
        )

        val igBrailleResult = assertNotNull(igBrailleState.conversionResult)
        assertFalse(igBrailleResult.formattedMessage.contains("`"))
        assertFalse(igBrailleResult.usesMonospaceFence)
        assertFalse(igBrailleResult.proportionalFontWarning)
        assertTrue(igBrailleResult.charCount <= 1_000)
        assertTrue(igBrailleResult.utf8ByteCount <= 1_000)

        // Manually forcing ASCII_CLASSIC on Instagram Direct activates proportionalFontWarning (CA-03.3)
        viewModel.onSelectCharsetMode(CharsetMode.ASCII_CLASSIC)
        advanceUntilIdle()
        val igAsciiState = expectMostRecentItem()
        assertEquals(CharsetMode.ASCII_CLASSIC, igAsciiState.params.charsetMode)
        val igAsciiResult = assertNotNull(igAsciiState.conversionResult)
        assertTrue(igAsciiResult.proportionalFontWarning)
        assertFalse(igAsciiResult.formattedMessage.contains("`"))
        assertTrue(igAsciiResult.charCount <= 1_000)
        assertTrue(igAsciiResult.utf8ByteCount <= 1_000)

        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun rapidSliderBursts_cancelObsoleteWorkViaCollectLatestAndSettleOnFinalParams() =
    runTest(testDispatcher) {
      val separateComputeDispatcher = StandardTestDispatcher()
      val countingComputeDispatcher = CountingDispatcher(separateComputeDispatcher)
      val repository = FakeImageSourceRepository()
      val shareGateway = FakeMessageShareGateway()

      val viewModel = ConverterViewModel(
        imageSourceRepository = repository,
        messageShareGateway = shareGateway,
        computeDispatcher = countingComputeDispatcher,
        ioDispatcher = testDispatcher,
        loadDemoOnStart = true,
      )

      // Settle initial load across both schedulers
      testDispatcher.scheduler.runCurrent()
      separateComputeDispatcher.scheduler.advanceUntilIdle()
      testDispatcher.scheduler.advanceUntilIdle()
      val dispatchesAfterInit = countingComputeDispatcher.dispatchCount.get()
      assertEquals(1, dispatchesAfterInit)

      viewModel.uiState.test {
        val initial = awaitItem()
        assertNotNull(initial.conversionResult)

        // First slider drag enters collectLatest and suspends waiting for computeDispatcher
        viewModel.onWidthColumnsChanged(18)
        testDispatcher.scheduler.runCurrent()

        // Rapid burst of slider changes arrives BEFORE the obsolete width=18 compute task runs
        viewModel.onWidthColumnsChanged(28)
        viewModel.onWidthColumnsChanged(42)
        viewModel.onBrightnessChanged(0.25f)
        viewModel.onContrastChanged(1.6f)
        viewModel.onContrastChanged(2.2f)
        testDispatcher.scheduler.runCurrent()

        // Execute queued compute work and settle main state updates
        separateComputeDispatcher.scheduler.advanceUntilIdle()
        testDispatcher.scheduler.advanceUntilIdle()

        val finalState = expectMostRecentItem()
        assertFalse(finalState.isConverting)
        assertEquals(42, finalState.params.widthColumns)
        assertEquals(0.25f, finalState.params.brightness)
        assertEquals(2.2f, finalState.params.contrast)

        val finalResult = assertNotNull(finalState.conversionResult)
        assertEquals(42, finalResult.requestedColumns)
        assertEquals(42, finalResult.columns)
        // Intermediate burst values (28, 1.6f) were coalesced, not individually dispatched!
        assertTrue(countingComputeDispatcher.dispatchCount.get() - dispatchesAfterInit <= 2)

        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun cloudFileOfflineUnavailable_surfacesSpanishErrorBannerAndSnackbarWithoutErasingPreviousArt() =
    runTest(testDispatcher) {
      val offlineCloudUri = "content://com.google.android.apps.docs.storage/document/acc%3D1%3Bdoc%3D9"
      val corruptedUri = "content://media/external/images/media/corrupt"
      val blockedUri = "file:///sdcard/secret.png"

      val repository = FakeImageSourceRepository().apply {
        uriResponses[offlineCloudUri] =
          DomainResult.Failure(ImageDecodeError.CloudFileOfflineUnavailable)
        uriResponses[corruptedUri] =
          DomainResult.Failure(ImageDecodeError.InvalidOrCorruptedImage)
        uriResponses[blockedUri] =
          DomainResult.Failure(ImageDecodeError.InvalidUriScheme)
      }
      val shareGateway = FakeMessageShareGateway()
      val viewModel = ConverterViewModel(
        imageSourceRepository = repository,
        messageShareGateway = shareGateway,
        computeDispatcher = testDispatcher,
        ioDispatcher = testDispatcher,
        loadDemoOnStart = true,
      )
      advanceUntilIdle()

      val originalDemoResult = assertNotNull(viewModel.uiState.value.conversionResult)

      viewModel.effects.test {
        viewModel.onDocumentUriSelected(offlineCloudUri)
        advanceUntilIdle()

        val stateAfterOfflineError = viewModel.uiState.value
        assertFalse(stateAfterOfflineError.isLoadingImage)
        assertEquals(originalDemoResult, stateAfterOfflineError.conversionResult)
        assertEquals(ImageSourceOrigin.DEMO, stateAfterOfflineError.sourceOrigin)

        val expectedOfflineMsg =
          "Este archivo en la nube (ej. Google Drive) no está disponible sin conexión. " +
            "En modo avión usa una imagen local o marcada como «Disponible sin conexión»."
        assertEquals(expectedOfflineMsg, stateAfterOfflineError.errorBannerMessage)
        val offlineSnackbar = assertIs<ConverterUiEffect.ShowSnackbar>(awaitItem())
        assertEquals(expectedOfflineMsg, offlineSnackbar.message)

        // Dismissing error banner clears only the banner, keeping art intact
        viewModel.onDismissErrorBanner()
        assertNull(viewModel.uiState.value.errorBannerMessage)
        assertEquals(originalDemoResult, viewModel.uiState.value.conversionResult)

        // Corrupted image & security violation also preserve art and emit Spanish messages
        viewModel.onGalleryUriSelected(corruptedUri)
        advanceUntilIdle()
        assertEquals(
          "El archivo seleccionado no es una imagen válida o está dañado.",
          viewModel.uiState.value.errorBannerMessage,
        )
        assertEquals(originalDemoResult, viewModel.uiState.value.conversionResult)
        assertIs<ConverterUiEffect.ShowSnackbar>(awaitItem())

        viewModel.onIncomingShareResult(DomainResult.Failure(ImageDecodeError.SecurityViolation))
        assertEquals(
          "Origen de imagen bloqueado por validación de seguridad.",
          viewModel.uiState.value.errorBannerMessage,
        )
        assertEquals(originalDemoResult, viewModel.uiState.value.conversionResult)
        assertIs<ConverterUiEffect.ShowSnackbar>(awaitItem())

        viewModel.onIncomingShareResult(DomainResult.Failure(ImageDecodeError.IoFailure("disk error")))
        assertEquals(
          "No se pudo leer la imagen seleccionada.",
          viewModel.uiState.value.errorBannerMessage,
        )
        assertEquals(originalDemoResult, viewModel.uiState.value.conversionResult)
        assertIs<ConverterUiEffect.ShowSnackbar>(awaitItem())
      }
    }

  @Test
  fun onCopyAndOnShareClicked_invokeMessageShareGatewayAndEmitSpanishSnackbarEffects() =
    runTest(testDispatcher) {
      val repository = FakeImageSourceRepository()
      val shareGateway = FakeMessageShareGateway()
      val viewModel = ConverterViewModel(
        imageSourceRepository = repository,
        messageShareGateway = shareGateway,
        computeDispatcher = testDispatcher,
        ioDispatcher = testDispatcher,
        loadDemoOnStart = true,
      )
      advanceUntilIdle()

      val whatsAppResult = assertNotNull(viewModel.uiState.value.conversionResult)

      viewModel.effects.test {
        // 1. Copy to clipboard (CA-04.1)
        viewModel.onCopyClicked()
        assertEquals(whatsAppResult.formattedMessage, shareGateway.lastCopiedText)
        val copySnackbar = assertIs<ConverterUiEffect.ShowSnackbar>(awaitItem())
        assertEquals(
          "Arte copiado al portapapeles (${whatsAppResult.charCount} caracteres)",
          copySnackbar.message,
        )

        // 2. Direct share to WhatsApp (CA-04.2)
        shareGateway.nextShareOutcome = ShareOutcome.OpenedDirectApp(
          packageName = "com.whatsapp",
          copiedToClipboard = false,
        )
        viewModel.onShareClicked()
        assertEquals(whatsAppResult.formattedMessage, shareGateway.lastSharedText)
        assertEquals(ImPlatformPreset.WHATSAPP, shareGateway.lastSharedPreset)
        val whatsAppShareSnackbar = assertIs<ConverterUiEffect.ShowSnackbar>(awaitItem())
        assertTrue(whatsAppShareSnackbar.message.contains("WhatsApp"))

        // 3. Switch to Instagram Direct and share -> clipboard-first paste hint in Spanish
        viewModel.onSelectPreset(ImPlatformPreset.INSTAGRAM_DIRECT)
        advanceUntilIdle()
        val igResult = assertNotNull(viewModel.uiState.value.conversionResult)
        shareGateway.nextShareOutcome = ShareOutcome.OpenedDirectApp(
          packageName = "com.instagram.android",
          copiedToClipboard = true,
        )
        viewModel.onShareClicked()
        assertEquals(igResult.formattedMessage, shareGateway.lastSharedText)
        assertEquals(ImPlatformPreset.INSTAGRAM_DIRECT, shareGateway.lastSharedPreset)
        val igSnackbar = assertIs<ConverterUiEffect.ShowSnackbar>(awaitItem())
        assertTrue(igSnackbar.message.contains("portapapeles", ignoreCase = true))
        assertTrue(igSnackbar.message.contains("pega", ignoreCase = true))

        // 4. System chooser fallback when target app is not installed (CA-04.3)
        shareGateway.nextShareOutcome = ShareOutcome.OpenedSystemChooser(copiedToClipboard = false)
        viewModel.onSelectPreset(ImPlatformPreset.TELEGRAM)
        advanceUntilIdle()
        viewModel.onShareClicked()
        val chooserSnackbar = assertIs<ConverterUiEffect.ShowSnackbar>(awaitItem())
        assertTrue(chooserSnackbar.message.contains("Telegram"))
      }
    }

  @Test
  fun adjustmentsResetBubbleToggleFactoryAndUiStateHygiene_workAsSpecified() =
    runTest(testDispatcher) {
      val repository = FakeImageSourceRepository()
      val shareGateway = FakeMessageShareGateway()
      val viewModel = ConverterViewModel(
        imageSourceRepository = repository,
        messageShareGateway = shareGateway,
        computeDispatcher = testDispatcher,
        ioDispatcher = testDispatcher,
        loadDemoOnStart = true,
      )
      advanceUntilIdle()

      // Toggle bubble theme
      assertTrue(viewModel.uiState.value.previewDarkBubble)
      viewModel.onTogglePreviewBubbleTheme()
      assertFalse(viewModel.uiState.value.previewDarkBubble)

      // Mutate all adjustments + clamping bounds, then resetAdjustments
      viewModel.onSelectPreset(ImPlatformPreset.TELEGRAM)
      viewModel.onSelectCharsetMode(CharsetMode.UNICODE_BLOCKS)
      viewModel.onWidthColumnsChanged(500) // clamps to MAX_REQUESTED_COLUMNS (72)
      viewModel.onBrightnessChanged(-5f) // clamps to -1f
      viewModel.onContrastChanged(9f) // clamps to 3f
      viewModel.onInvertToggled(false)
      viewModel.onSelectDitherMode(DitherMode.ATKINSON)
      advanceUntilIdle()

      val mutatedParams = viewModel.uiState.value.params
      assertEquals(72, mutatedParams.widthColumns)
      assertEquals(-1f, mutatedParams.brightness)
      assertEquals(3f, mutatedParams.contrast)
      assertFalse(mutatedParams.invert)
      assertEquals(DitherMode.ATKINSON, mutatedParams.ditherMode)

      viewModel.onResetAdjustments()
      advanceUntilIdle()
      val resetParams = viewModel.uiState.value.params
      assertEquals(ImPlatformPreset.TELEGRAM, resetParams.preset)
      assertEquals(ImPlatformPreset.TELEGRAM.defaultCharset, resetParams.charsetMode)
      assertEquals(ImPlatformPreset.TELEGRAM.defaultWidthColumns, resetParams.widthColumns)
      assertEquals(0f, resetParams.brightness)
      assertEquals(1f, resetParams.contrast)
      assertTrue(resetParams.invert)
      assertEquals(DitherMode.FLOYD_STEINBERG, resetParams.ditherMode)

      // Non-finite float inputs are ignored safely
      viewModel.onBrightnessChanged(Float.NaN)
      viewModel.onContrastChanged(Float.POSITIVE_INFINITY)
      assertEquals(0f, viewModel.uiState.value.params.brightness)
      assertEquals(1f, viewModel.uiState.value.params.contrast)

      // Cold start without demo keeps conversionResult null until onLoadDemoSample() is invoked
      val coldVm = ConverterViewModel(
        imageSourceRepository = repository,
        messageShareGateway = shareGateway,
        computeDispatcher = testDispatcher,
        ioDispatcher = testDispatcher,
        loadDemoOnStart = false,
      )
      assertNull(coldVm.uiState.value.conversionResult)
      coldVm.onCopyClicked()
      coldVm.onShareClicked()
      assertNull(shareGateway.lastCopiedText)

      coldVm.onIncomingShareResult(DomainResult.Failure(ImageDecodeError.InvalidUriScheme))
      assertNotNull(coldVm.uiState.value.errorBannerMessage)
      coldVm.onLoadDemoSample()
      advanceUntilIdle()
      assertNull(coldVm.uiState.value.errorBannerMessage)
      assertEquals(ImageSourceOrigin.DEMO, coldVm.uiState.value.sourceOrigin)
      assertNotNull(coldVm.uiState.value.conversionResult)

      // Verify ConverterUiState never embeds PixelGrid or DecodedImage fields
      val fieldTypes = ConverterUiState::class.java.declaredFields.map { it.type }
      assertFalse(fieldTypes.contains(PixelGrid::class.java))
      assertFalse(fieldTypes.contains(DecodedImage::class.java))

      // Verify provideFactory instantiates ConverterViewModel cleanly
      val factoryVm = ConverterViewModel.provideFactory(repository, shareGateway)
        .create(ConverterViewModel::class.java)
      assertNotNull(factoryVm)
    }

  @Test
  fun togglingAttachDeepLinkAppendsUrlToFormattedMessage() = runTest(testDispatcher) {
    val repository = FakeImageSourceRepository()
    val shareGateway = FakeMessageShareGateway()
    val fakeCodec = object : com.laralbir.img2asciiim.data.source.DeepLinkImageCodec() {
      override fun encodeToDeepLink(
        pixelGrid: PixelGrid,
        mode: com.laralbir.img2asciiim.data.source.DeepLinkMode,
        preset: ImPlatformPreset,
      ): String = "https://laralbir.github.io/img2asciiIM/#data=fake_payload"
    }
    val viewModel = ConverterViewModel(
      imageSourceRepository = repository,
      messageShareGateway = shareGateway,
      deepLinkCodec = fakeCodec,
      computeDispatcher = testDispatcher,
      ioDispatcher = testDispatcher,
      loadDemoOnStart = true,
    )
    advanceUntilIdle()

    // By default, attachDeepLink is false
    assertFalse(viewModel.uiState.value.attachDeepLink)
    val standardMessage = viewModel.buildShareableMessage()
    assertNotNull(standardMessage)
    assertFalse(standardMessage.contains("https://laralbir.github.io/img2asciiIM/#data="))

    // Toggle on attachDeepLink
    viewModel.onToggleAttachDeepLink(true)
    assertTrue(viewModel.uiState.value.attachDeepLink)

    val linkedMessage = viewModel.buildShareableMessage()
    assertNotNull(linkedMessage)
    assertTrue(linkedMessage.contains("https://laralbir.github.io/img2asciiIM/#data=fake_payload"))
    assertTrue(linkedMessage.contains("🔗 Ver foto en alta definición:"))

    // Sharing sends the linked message
    viewModel.onShareClicked()
    advanceUntilIdle()
    assertEquals(linkedMessage, shareGateway.lastSharedText)
  }

  @Test
  fun togglingOffAsciiArtEnablesExpandedHdDeepLinkWithoutAsciiArt() = runTest(testDispatcher) {
    val repository = FakeImageSourceRepository()
    val shareGateway = FakeMessageShareGateway()
    val fakeCodec = object : com.laralbir.img2asciiim.data.source.DeepLinkImageCodec() {
      override fun encodeToDeepLink(
        pixelGrid: PixelGrid,
        mode: com.laralbir.img2asciiim.data.source.DeepLinkMode,
        preset: ImPlatformPreset,
      ): String = "https://laralbir.github.io/img2asciiIM/#data=fake_hd_payload"
    }
    val viewModel = ConverterViewModel(
      imageSourceRepository = repository,
      messageShareGateway = shareGateway,
      deepLinkCodec = fakeCodec,
      computeDispatcher = testDispatcher,
      ioDispatcher = testDispatcher,
      loadDemoOnStart = true,
    )
    advanceUntilIdle()

    // User chooses to exclude ASCII art preview to allocate all bytes to the HD photo
    viewModel.onToggleIncludeAsciiArt(false)
    assertFalse(viewModel.uiState.value.includeAsciiArt)
    // attachDeepLink automatically becomes true
    assertTrue(viewModel.uiState.value.attachDeepLink)

    val message = viewModel.buildShareableMessage()
    assertNotNull(message)
    assertEquals(
      "📸 Ver foto en alta resolución:\nhttps://laralbir.github.io/img2asciiIM/#data=fake_hd_payload",
      message,
    )
    // Must NOT contain braille or ascii blocks
    assertFalse(message.contains("```"))
    assertFalse(message.contains("\n\n🔗 Ver foto"))

    viewModel.onCopyClicked()
    advanceUntilIdle()
    assertEquals(message, shareGateway.lastCopiedText)
  }

  @Test
  fun cannotTurnOffBothAsciiArtAndDeepLinkSimultaneously() = runTest(testDispatcher) {
    val repository = FakeImageSourceRepository()
    val shareGateway = FakeMessageShareGateway()
    val viewModel = ConverterViewModel(
      imageSourceRepository = repository,
      messageShareGateway = shareGateway,
      computeDispatcher = testDispatcher,
      ioDispatcher = testDispatcher,
      loadDemoOnStart = true,
    )
    advanceUntilIdle()

    // Turn off ASCII art -> deep link turns on
    viewModel.onToggleIncludeAsciiArt(false)
    assertFalse(viewModel.uiState.value.includeAsciiArt)
    assertTrue(viewModel.uiState.value.attachDeepLink)

    // Attempting to turn off deep link while ASCII art is off forces ASCII art back on
    viewModel.onToggleAttachDeepLink(false)
    assertTrue(viewModel.uiState.value.includeAsciiArt)
    assertFalse(viewModel.uiState.value.attachDeepLink)
  }
}

private class FakeImageSourceRepository(
  private val demoImage: DecodedImage = DemoSampleImageFactory.createInFlightWindowSample(),
) : ImageSourceRepository {

  val uriResponses = mutableMapOf<String, DomainResult<DecodedImage, ImageDecodeError>>()
  var decodeCallCount: Int = 0
    private set

  override suspend fun decodeFromUri(
    uriString: String,
    maxDimensionPx: Int,
  ): DomainResult<DecodedImage, ImageDecodeError> {
    decodeCallCount++
    return uriResponses[uriString]
      ?: DomainResult.Failure(ImageDecodeError.IoFailure("Unconfigured URI: $uriString"))
  }

  override fun loadDemoSample(): DecodedImage = demoImage
}

private class FakeMessageShareGateway : MessageShareGateway {
  var lastCopiedText: String? = null
    private set
  var lastSharedText: String? = null
    private set
  var lastSharedPreset: ImPlatformPreset? = null
    private set
  var nextShareOutcome: ShareOutcome = ShareOutcome.OpenedDirectApp(
    packageName = "com.whatsapp",
    copiedToClipboard = false,
  )

  override fun copyToClipboard(text: String) {
    lastCopiedText = text
  }

  override fun shareToPlatform(text: String, preset: ImPlatformPreset): ShareOutcome {
    lastSharedText = text
    lastSharedPreset = preset
    return nextShareOutcome
  }
}

private class CountingDispatcher(
  private val delegate: CoroutineDispatcher,
) : CoroutineDispatcher() {
  val dispatchCount = AtomicInteger(0)

  override fun dispatch(context: CoroutineContext, block: Runnable) {
    dispatchCount.incrementAndGet()
    delegate.dispatch(context, block)
  }
}

private fun createGradientDecodedImage(
  width: Int,
  height: Int,
  origW: Int = width,
  origH: Int = height,
  label: String? = null,
): DecodedImage {
  val pixels = IntArray(width * height) { index ->
    val x = index % width
    val y = index / width
    val luma = (((x + y) * 255) / (width + height).coerceAtLeast(1)).coerceIn(0, 255)
    (0xFF shl 24) or (luma shl 16) or (luma shl 8) or luma
  }
  return DecodedImage(
    pixelGrid = PixelGrid(width = width, height = height, argbPixels = pixels),
    originalWidth = origW,
    originalHeight = origH,
    label = label,
  )
}
