package com.laralbir.img2asciiim.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laralbir.img2asciiim.data.repository.DefaultWorkspaceReadinessRepository
import com.laralbir.img2asciiim.domain.model.WorkspaceReadiness
import com.laralbir.img2asciiim.domain.repository.WorkspaceReadinessRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MainScreenViewModel(
  readinessRepository: WorkspaceReadinessRepository = DefaultWorkspaceReadinessRepository(),
) : ViewModel() {
  val uiState: StateFlow<MainScreenUiState> =
    readinessRepository.readiness
      .map<WorkspaceReadiness, MainScreenUiState>(MainScreenUiState::Ready)
      .catch { throwable -> emit(MainScreenUiState.Error(throwable)) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainScreenUiState.Loading,
      )
}

sealed interface MainScreenUiState {
  data object Loading : MainScreenUiState

  data class Ready(val readiness: WorkspaceReadiness) : MainScreenUiState

  data class Error(val throwable: Throwable) : MainScreenUiState
}
