package com.laralbir.img2asciiim.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laralbir.img2asciiim.R
import com.laralbir.img2asciiim.domain.model.MessagingTarget
import com.laralbir.img2asciiim.domain.model.WorkspaceReadiness
import com.laralbir.img2asciiim.ui.theme.AsciiMonospaceTextStyle
import com.laralbir.img2asciiim.ui.theme.Img2asciiIMTheme

object MainScreenTestTags {
  const val SCREEN = "main_screen"
  const val TOP_BAR = "main_top_bar"
  const val OFFLINE_BANNER = "offline_readiness_banner"
  const val WORKSPACE_STATUS_CARD = "workspace_status_card"
  const val TARGETS_SECTION = "messaging_targets_section"
  const val LOADING_INDICATOR = "main_loading_indicator"
  const val ERROR_MESSAGE = "main_error_message"
}

@Composable
fun MainScreen(
  modifier: Modifier = Modifier,
  viewModel: MainScreenViewModel = viewModel { MainScreenViewModel() },
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  MainScreen(
    uiState = uiState,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
  uiState: MainScreenUiState,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier.fillMaxSize().testTag(MainScreenTestTags.SCREEN),
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Text(
            text = stringResource(R.string.app_name),
            modifier = Modifier.semantics { heading() },
          )
        },
        modifier = Modifier.testTag(MainScreenTestTags.TOP_BAR),
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
          ),
      )
    },
  ) { innerPadding ->
    when (uiState) {
      MainScreenUiState.Loading -> {
        val loadingDescription = stringResource(R.string.main_loading_description)
        Box(
          modifier = Modifier.fillMaxSize().padding(innerPadding),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator(
            modifier =
              Modifier.testTag(MainScreenTestTags.LOADING_INDICATOR).semantics {
                contentDescription = loadingDescription
              }
          )
        }
      }
      is MainScreenUiState.Ready -> {
        ReadyShellContent(
          readiness = uiState.readiness,
          modifier = Modifier.fillMaxSize().padding(innerPadding),
        )
      }
      is MainScreenUiState.Error -> {
        Box(
          modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text =
              stringResource(
                R.string.main_error_message,
                uiState.throwable.message.orEmpty(),
              ),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag(MainScreenTestTags.ERROR_MESSAGE),
          )
        }
      }
    }
  }
}

@Composable
private fun ReadyShellContent(
  readiness: WorkspaceReadiness,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    if (readiness.isOfflineReady) {
      ElevatedCard(
        modifier = Modifier.fillMaxWidth().testTag(MainScreenTestTags.OFFLINE_BANNER),
        colors =
          CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
          ),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = stringResource(R.string.main_offline_badge),
            style = AsciiMonospaceTextStyle,
          )
          Text(
            text = stringResource(R.string.main_offline_subtitle),
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
    }

    OutlinedCard(
      modifier = Modifier.fillMaxWidth().testTag(MainScreenTestTags.WORKSPACE_STATUS_CARD),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = stringResource(R.string.main_workspace_title),
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.semantics { heading() },
        )
        Text(
          text = stringResource(R.string.main_workspace_description),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    Column(
      modifier = Modifier.fillMaxWidth().testTag(MainScreenTestTags.TARGETS_SECTION),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = stringResource(R.string.main_targets_heading),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.semantics { heading() },
      )
      readiness.supportedTargets.chunked(2).forEach { rowTargets ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          rowTargets.forEach { target ->
            MessagingTargetBadge(
              target = target,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MessagingTargetBadge(
  target: MessagingTarget,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = MaterialTheme.shapes.small,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
  ) {
    Text(
      text = target.displayLabel,
      style = MaterialTheme.typography.labelLarge,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
    )
  }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScreenPreview() {
  Img2asciiIMTheme {
    MainScreen(uiState = MainScreenUiState.Ready(WorkspaceReadiness()))
  }
}
