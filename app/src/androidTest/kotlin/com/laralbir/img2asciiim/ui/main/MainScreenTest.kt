package com.laralbir.img2asciiim.ui.main

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.laralbir.img2asciiim.R
import com.laralbir.img2asciiim.domain.model.MessagingTarget
import com.laralbir.img2asciiim.domain.model.WorkspaceReadiness
import com.laralbir.img2asciiim.ui.theme.Img2asciiIMTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Instrumented Compose UI tests for [MainScreen]. */
class MainScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun wakeDeviceForPhysicalDebugging() {
    composeTestRule.activityRule.scenario.onActivity { activity ->
      activity.setShowWhenLocked(true)
      activity.setTurnScreenOn(true)
      activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
  }

  @Test
  fun readyState_displaysSpanishShellAndMessagingTargets() {
    composeTestRule.setContent {
      Img2asciiIMTheme {
        MainScreen(uiState = MainScreenUiState.Ready(WorkspaceReadiness()))
      }
    }

    val activity = composeTestRule.activity
    composeTestRule.onNodeWithText(activity.getString(R.string.app_name)).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MainScreenTestTags.OFFLINE_BANNER).assertIsDisplayed()
    composeTestRule
      .onNodeWithText(activity.getString(R.string.main_offline_subtitle))
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText(activity.getString(R.string.main_workspace_title))
      .assertIsDisplayed()

    MessagingTarget.entries.forEach { target ->
      composeTestRule.onNodeWithText(target.displayLabel).assertIsDisplayed()
    }
  }
}
