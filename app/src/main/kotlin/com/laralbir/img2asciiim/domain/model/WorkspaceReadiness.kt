package com.laralbir.img2asciiim.domain.model

/**
 * Describes the initial readiness state of the offline SDD/TDD foundation shell.
 */
data class WorkspaceReadiness(
  val isOfflineReady: Boolean = true,
  val supportedTargets: List<MessagingTarget> = MessagingTarget.entries,
)
