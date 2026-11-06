package com.laralbir.img2asciiim

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.laralbir.img2asciiim.ui.main.MainScreen

@Composable
fun MainNavigation(modifier: Modifier = Modifier) {
  val backStack = rememberNavBackStack(Main)

  NavDisplay(
    backStack = backStack,
    modifier = modifier,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          MainScreen()
        }
      },
  )
}
