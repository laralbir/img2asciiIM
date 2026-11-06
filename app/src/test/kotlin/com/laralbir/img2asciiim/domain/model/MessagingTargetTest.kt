package com.laralbir.img2asciiim.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class MessagingTargetTest {
  @Test
  fun entries_exposeExpectedTargetsAndDisplayLabels() {
    assertEquals(
      listOf(
        MessagingTarget.WHATSAPP to "WhatsApp",
        MessagingTarget.TELEGRAM to "Telegram",
        MessagingTarget.INSTAGRAM to "Instagram",
        MessagingTarget.GENERIC to "Genérico",
      ),
      MessagingTarget.entries.map { it to it.displayLabel },
    )
  }
}
