package com.laralbir.img2asciiim.data.repository

import com.laralbir.img2asciiim.domain.model.WorkspaceReadiness
import com.laralbir.img2asciiim.domain.repository.WorkspaceReadinessRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class DefaultWorkspaceReadinessRepository : WorkspaceReadinessRepository {
  override val readiness: Flow<WorkspaceReadiness> = flowOf(WorkspaceReadiness())
}
