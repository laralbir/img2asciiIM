package com.laralbir.img2asciiim.domain.repository

import com.laralbir.img2asciiim.domain.model.ImPlatformPreset
import com.laralbir.img2asciiim.domain.model.ShareOutcome

/**
 * Pure Kotlin port for copying formatted art to clipboard and dispatching to IM apps.
 */
interface MessageShareGateway {
  fun copyToClipboard(text: String)
  fun shareToPlatform(text: String, preset: ImPlatformPreset): ShareOutcome
}
