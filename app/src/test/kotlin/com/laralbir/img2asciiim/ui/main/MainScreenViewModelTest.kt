package com.laralbir.img2asciiim.ui.main

import app.cash.turbine.test
import com.laralbir.img2asciiim.domain.model.MessagingTarget
import com.laralbir.img2asciiim.domain.model.WorkspaceReadiness
import com.laralbir.img2asciiim.domain.repository.WorkspaceReadinessRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {
  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  @Test
  fun uiState_startsLoadingThenEmitsReadyWhenRepositoryEmits() = runTest {
    val repository = FakeWorkspaceReadinessRepository()
    val viewModel = MainScreenViewModel(repository)

    viewModel.uiState.test {
      assertEquals(MainScreenUiState.Loading, awaitItem())

      val expected = WorkspaceReadiness(isOfflineReady = true)
      repository.emit(expected)

      assertEquals(MainScreenUiState.Ready(expected), awaitItem())
    }
  }

  @Test
  fun uiState_withDefaultRepository_emitsOfflineReadyWithAllMessagingTargets() = runTest {
    val viewModel = MainScreenViewModel()

    viewModel.uiState.test {
      val readyState = assertIs<MainScreenUiState.Ready>(awaitItem())
      assertTrue(readyState.readiness.isOfflineReady)
      assertEquals(MessagingTarget.entries, readyState.readiness.supportedTargets)
    }
  }

  @Test
  fun uiState_whenRepositoryFails_emitsError() = runTest {
    val failure = IllegalStateException("Error simulado")
    val failingRepository =
      object : WorkspaceReadinessRepository {
        override val readiness: Flow<WorkspaceReadiness> = flow { throw failure }
      }
    val viewModel = MainScreenViewModel(failingRepository)

    viewModel.uiState.test {
      val errorState = assertIs<MainScreenUiState.Error>(awaitItem())
      assertEquals("Error simulado", errorState.throwable.message)
    }
  }
}

private class FakeWorkspaceReadinessRepository : WorkspaceReadinessRepository {
  private val backingFlow = MutableSharedFlow<WorkspaceReadiness>()
  override val readiness: Flow<WorkspaceReadiness> = backingFlow

  suspend fun emit(value: WorkspaceReadiness) {
    backingFlow.emit(value)
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
  private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
  override fun starting(description: Description) {
    Dispatchers.setMain(testDispatcher)
  }

  override fun finished(description: Description) {
    Dispatchers.resetMain()
  }
}
