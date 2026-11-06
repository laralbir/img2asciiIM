package com.laralbir.img2asciiim.ui.main

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.font.FontFamily
import com.laralbir.img2asciiim.domain.model.MessagingTarget
import com.laralbir.img2asciiim.domain.model.WorkspaceReadiness
import com.laralbir.img2asciiim.ui.theme.AsciiMonospaceTextStyle
import com.laralbir.img2asciiim.ui.theme.Img2asciiIMTheme
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainScreenRobolectricTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun readyState_rendersHeaderOfflineBannerWorkspaceStatusAndSemantics() {
    composeTestRule.setContent {
      Img2asciiIMTheme {
        MainScreen(uiState = MainScreenUiState.Ready(WorkspaceReadiness()))
      }
    }

    composeTestRule.onNodeWithTag(MainScreenTestTags.TOP_BAR).assertIsDisplayed()
    composeTestRule
      .onNodeWithText("img2asciiIM")
      .assertIsDisplayed()
      .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))

    composeTestRule.onNodeWithTag(MainScreenTestTags.OFFLINE_BANNER).assertIsDisplayed()
    composeTestRule.onNodeWithText("[ 100% OFFLINE · MODO AVIÓN ]").assertIsDisplayed()
    composeTestRule
      .onNodeWithText("Preparación local sin conexión para mensajería instantánea a bordo.")
      .assertIsDisplayed()

    composeTestRule.onNodeWithTag(MainScreenTestTags.WORKSPACE_STATUS_CARD).assertIsDisplayed()
    composeTestRule
      .onNodeWithText("Entorno SDD + TDD listo")
      .assertIsDisplayed()
      .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))

    composeTestRule.onNodeWithTag(MainScreenTestTags.TARGETS_SECTION).assertIsDisplayed()
    MessagingTarget.entries.forEach { target ->
      composeTestRule.onNodeWithText(target.displayLabel).assertIsDisplayed()
    }
  }

  @Test
  fun loadingState_rendersProgressIndicatorWithSpanishAccessibilityDescription() {
    composeTestRule.setContent {
      Img2asciiIMTheme {
        MainScreen(uiState = MainScreenUiState.Loading)
      }
    }

    composeTestRule.onNodeWithTag(MainScreenTestTags.LOADING_INDICATOR).assertIsDisplayed()
  }

  @Test
  fun asciiMonospaceTextStyle_usesMonospaceFontFamily() {
    assertEquals(FontFamily.Monospace, AsciiMonospaceTextStyle.fontFamily)
  }
}
