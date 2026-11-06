package com.laralbir.img2asciiim.domain.repository

import com.laralbir.img2asciiim.domain.model.WorkspaceReadiness
import kotlinx.coroutines.flow.Flow

interface WorkspaceReadinessRepository {
  val readiness: Flow<WorkspaceReadiness>
}
