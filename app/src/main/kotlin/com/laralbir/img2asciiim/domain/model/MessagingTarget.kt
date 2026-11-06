package com.laralbir.img2asciiim.domain.model

/**
 * Foundational instant messaging channels targeted by img2asciiIM.
 */
enum class MessagingTarget(val displayLabel: String) {
  WHATSAPP("WhatsApp"),
  TELEGRAM("Telegram"),
  INSTAGRAM("Instagram"),
  GENERIC("Genérico"),
}
