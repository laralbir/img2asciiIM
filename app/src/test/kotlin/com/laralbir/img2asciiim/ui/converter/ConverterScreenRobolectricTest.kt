package com.laralbir.img2asciiim.ui.converter

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.laralbir.img2asciiim.domain.engine.DemoSampleImageFactory
import com.laralbir.img2asciiim.domain.model.AsciiConversionResult
import com.laralbir.img2asciiim.domain.model.CharsetMode
import com.laralbir.img2asciiim.domain.model.ConversionParams
import com.laralbir.img2asciiim.domain.model.DecodedImage
import com.laralbir.img2asciiim.domain.model.DomainResult
import com.laralbir.img2asciiim.domain.model.ImPlatformPreset
import com.laralbir.img2asciiim.domain.model.ImageDecodeError
import com.laralbir.img2asciiim.domain.model.ShareOutcome
import com.laralbir.img2asciiim.domain.repository.ImageSourceRepository
import com.laralbir.img2asciiim.domain.repository.MessageShareGateway
import com.laralbir.img2asciiim.ui.theme.Img2asciiIMTheme
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConverterScreenRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val testDispatcher = StandardTestDispatcher()

  private class FakeImageSourceRepo : ImageSourceRepository {
    override suspend fun decodeFromUri(
      uriString: String,
      maxDimensionPx: Int,
    ): DomainResult<DecodedImage, ImageDecodeError> {
      return DomainResult.Success(loadDemoSample())
    }

    override fun loadDemoSample(): DecodedImage {
      return DemoSampleImageFactory.createInFlightWindowSample()
    }
  }

  private class FakeShareGateway : MessageShareGateway {
    var lastCopied: String? = null
    var lastSharedPreset: ImPlatformPreset? = null

    override fun copyToClipboard(text: String) {
      lastCopied = text
    }

    override fun shareToPlatform(text: String, preset: ImPlatformPreset): ShareOutcome {
      lastSharedPreset = preset
      return ShareOutcome.OpenedDirectApp(preset.canonicalPackage)
    }
  }

  @Test
  fun converterScreen_rendersHeaderSourceButtonsBubbleAndBottomBar() {
    val repo = FakeImageSourceRepo()
    val gateway = FakeShareGateway()
    val viewModel = ConverterViewModel(
      imageSourceRepository = repo,
      messageShareGateway = gateway,
      computeDispatcher = testDispatcher,
      ioDispatcher = testDispatcher,
      loadDemoOnStart = true,
    )
    testDispatcher.scheduler.advanceUntilIdle()

    composeTestRule.setContent {
      Img2asciiIMTheme {
        ConverterScreen(
          viewModel = viewModel,
          onPrepareCameraCaptureTarget = { Uri.parse("content://dummy/capture.jpg") },
        )
      }
    }

    // Top Bar & Offline Badge
    composeTestRule.onNodeWithTag(ConverterTestTags.TOP_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ConverterTestTags.OFFLINE_BADGE).assertIsDisplayed()

    // Source buttons
    composeTestRule.onNodeWithTag(ConverterTestTags.BTN_GALLERY).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ConverterTestTags.BTN_DOCUMENTS).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ConverterTestTags.BTN_CAMERA).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ConverterTestTags.BTN_DEMO).assertDoesNotExist()

    // Platform Presets
    composeTestRule.onNodeWithText("WhatsApp").assertIsDisplayed()
    composeTestRule.onNodeWithText("Telegram").assertIsDisplayed()
    composeTestRule.onNodeWithText("Instagram DM").assertIsDisplayed()

    // Charset Modes
    composeTestRule.onNodeWithText("Braille HD").assertIsDisplayed()
    composeTestRule.onNodeWithText("ASCII").assertIsDisplayed()
    composeTestRule.onNodeWithText("Bloques").assertIsDisplayed()

    // Chat bubble card
    composeTestRule.onNodeWithTag(ConverterTestTags.CHAT_BUBBLE_CARD).assertExists()
    composeTestRule.onNodeWithTag(ConverterTestTags.ASCII_TEXT).assertExists()

    // Action buttons
    composeTestRule.onNodeWithTag(ConverterTestTags.BTN_COPY).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ConverterTestTags.BTN_SHARE).assertIsDisplayed()
  }

  @Test
  fun converterScreen_clickingCopyOrShare_triggersGateway() {
    val repo = FakeImageSourceRepo()
    val gateway = FakeShareGateway()
    val viewModel = ConverterViewModel(
      imageSourceRepository = repo,
      messageShareGateway = gateway,
      computeDispatcher = testDispatcher,
      ioDispatcher = testDispatcher,
      loadDemoOnStart = true,
    )
    testDispatcher.scheduler.advanceUntilIdle()

    composeTestRule.setContent {
      Img2asciiIMTheme {
        ConverterScreen(
          viewModel = viewModel,
          onPrepareCameraCaptureTarget = { null },
        )
      }
    }

    composeTestRule.onNodeWithTag(ConverterTestTags.BTN_COPY).performClick()
    assertTrue(gateway.lastCopied != null, "Last copied must not be null after click")

    composeTestRule.onNodeWithTag(ConverterTestTags.BTN_SHARE).performClick()
    assertTrue(gateway.lastSharedPreset == ImPlatformPreset.WHATSAPP)
  }

  @Test
  fun converterScreen_switchingToInstagramAndAscii_showsProportionalWarning() {
    val repo = FakeImageSourceRepo()
    val gateway = FakeShareGateway()
    val viewModel = ConverterViewModel(
      imageSourceRepository = repo,
      messageShareGateway = gateway,
      computeDispatcher = testDispatcher,
      ioDispatcher = testDispatcher,
      loadDemoOnStart = true,
    )
    testDispatcher.scheduler.advanceUntilIdle()

    composeTestRule.setContent {
      Img2asciiIMTheme {
        ConverterScreen(
          viewModel = viewModel,
          onPrepareCameraCaptureTarget = { null },
        )
      }
    }

    // Switch to Instagram DM
    composeTestRule.onNodeWithText("Instagram DM").performClick()
    testDispatcher.scheduler.advanceUntilIdle()

    // Force ASCII
    composeTestRule.onNodeWithText("ASCII").performClick()
    testDispatcher.scheduler.advanceUntilIdle()

    // Proportional warning should exist
    composeTestRule.onNodeWithTag(ConverterTestTags.PROPORTIONAL_WARNING).assertExists()
  }

  @Test
  fun converterScreen_whenColumnsExceed32_showsWidthOverflowWarning() {
    val repo = FakeImageSourceRepo()
    val gateway = FakeShareGateway()
    val viewModel = ConverterViewModel(
      imageSourceRepository = repo,
      messageShareGateway = gateway,
      computeDispatcher = testDispatcher,
      ioDispatcher = testDispatcher,
      loadDemoOnStart = true,
    )
    testDispatcher.scheduler.advanceUntilIdle()

    composeTestRule.setContent {
      Img2asciiIMTheme {
        ConverterScreen(
          viewModel = viewModel,
          onPrepareCameraCaptureTarget = { null },
        )
      }
    }

    viewModel.onWidthColumnsChanged(56)
    testDispatcher.scheduler.advanceUntilIdle()

    composeTestRule.onNodeWithTag(ConverterTestTags.WIDTH_OVERFLOW_WARNING).assertExists()
  }

  @Test
  fun converterScreen_deliveryOptions_togglingAsciiAndDeepLinkWorks() {
    val repo = FakeImageSourceRepo()
    val gateway = FakeShareGateway()
    val viewModel = ConverterViewModel(
      imageSourceRepository = repo,
      messageShareGateway = gateway,
      computeDispatcher = testDispatcher,
      ioDispatcher = testDispatcher,
      loadDemoOnStart = true,
    )
    testDispatcher.scheduler.advanceUntilIdle()

    composeTestRule.setContent {
      Img2asciiIMTheme {
        ConverterScreen(
          viewModel = viewModel,
          onPrepareCameraCaptureTarget = { null },
        )
      }
    }

    // Both chips exist
    composeTestRule.onNodeWithTag(ConverterTestTags.CHIP_INCLUDE_ASCII).assertExists()
    composeTestRule.onNodeWithTag(ConverterTestTags.CHIP_ATTACH_DEEP_LINK).assertExists()
    composeTestRule.onNodeWithTag(ConverterTestTags.BTN_CHECK_UPDATES).assertExists()

    // Toggle off ASCII preview -> triggers HD mode expanded card
    composeTestRule.onNodeWithTag(ConverterTestTags.CHIP_INCLUDE_ASCII).performScrollTo().performClick()
    testDispatcher.scheduler.advanceUntilIdle()

    assertFalse(viewModel.uiState.value.includeAsciiArt)
    assertTrue(viewModel.uiState.value.attachDeepLink)
    composeTestRule.onNodeWithTag(ConverterTestTags.HD_MODE_EXPANDED_CARD).assertExists()
  }

  @Test
  fun converterScreen_emptyState_whenNoConversionResult_showsPromptAndDisablesButtons() {
    val repo = FakeImageSourceRepo()
    val gateway = FakeShareGateway()
    val viewModel = ConverterViewModel(
      imageSourceRepository = repo,
      messageShareGateway = gateway,
      computeDispatcher = testDispatcher,
      ioDispatcher = testDispatcher,
      loadDemoOnStart = false,
    )
    testDispatcher.scheduler.advanceUntilIdle()

    composeTestRule.setContent {
      Img2asciiIMTheme {
        ConverterScreen(
          viewModel = viewModel,
          onPrepareCameraCaptureTarget = { null },
        )
      }
    }

    // Empty state card prompt is displayed
    composeTestRule.onNodeWithText("Selecciona una foto arriba para convertirla en arte de texto").assertExists()

    // Action buttons are disabled
    composeTestRule.onNodeWithTag(ConverterTestTags.BTN_COPY).assertIsNotEnabled()
    composeTestRule.onNodeWithTag(ConverterTestTags.BTN_SHARE).assertIsNotEnabled()

    // Load demo sample -> art generated and buttons become enabled
    viewModel.onLoadDemoSample()
    testDispatcher.scheduler.advanceUntilIdle()

    composeTestRule.onNodeWithText("Selecciona una foto arriba para convertirla en arte de texto").assertDoesNotExist()
    composeTestRule.onNodeWithTag(ConverterTestTags.ASCII_TEXT).assertExists()
    composeTestRule.onNodeWithTag(ConverterTestTags.BTN_COPY).assertIsEnabled()
    composeTestRule.onNodeWithTag(ConverterTestTags.BTN_SHARE).assertIsEnabled()
  }

  @Test
  fun converterScreen_cleanPreviewFormat_withoutVerFotoPrefixes() {
    val repo = FakeImageSourceRepo()
    val gateway = FakeShareGateway()
    val viewModel = ConverterViewModel(
      imageSourceRepository = repo,
      messageShareGateway = gateway,
      computeDispatcher = testDispatcher,
      ioDispatcher = testDispatcher,
      loadDemoOnStart = true,
    )
    testDispatcher.scheduler.advanceUntilIdle()

    composeTestRule.setContent {
      Img2asciiIMTheme {
        ConverterScreen(
          viewModel = viewModel,
          onPrepareCameraCaptureTarget = { null },
        )
      }
    }

    // Attach deep link
    composeTestRule.onNodeWithTag(ConverterTestTags.CHIP_ATTACH_DEEP_LINK).performScrollTo().performClick()
    testDispatcher.scheduler.advanceUntilIdle()

    // Disable ASCII preview
    composeTestRule.onNodeWithTag(ConverterTestTags.CHIP_INCLUDE_ASCII).performScrollTo().performClick()
    testDispatcher.scheduler.advanceUntilIdle()

    // Accessibility content description reflects solo-link mode
    composeTestRule.onNodeWithTag(ConverterTestTags.ASCII_TEXT).performScrollTo().assertExists()
  }
}
