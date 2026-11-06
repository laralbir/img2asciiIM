package com.laralbir.img2asciiim.domain.model

/**
 * Instant messaging platform presets with exact typography, package, and payload constraints.
 */
enum class ImPlatformPreset(
  val displayName: String,
  val canonicalPackage: String,
  val fallbackPackages: List<String>,
  val defaultCharset: CharsetMode,
  val defaultWidthColumns: Int,
  val maxCharacters: Int,
  val maxUtf8Bytes: Int?,
  val usesClipboardFirst: Boolean,
) {
  WHATSAPP(
    displayName = "WhatsApp",
    canonicalPackage = "com.whatsapp",
    fallbackPackages = listOf("com.whatsapp.w4b"),
    defaultCharset = CharsetMode.BRAILLE_UNICODE,
    defaultWidthColumns = 30,
    maxCharacters = 65_536,
    maxUtf8Bytes = null,
    usesClipboardFirst = false,
  ),
  TELEGRAM(
    displayName = "Telegram",
    canonicalPackage = "org.telegram.messenger",
    fallbackPackages = listOf("org.thunderdog.challegram", "org.telegram.messenger.web"),
    defaultCharset = CharsetMode.BRAILLE_UNICODE,
    defaultWidthColumns = 32,
    maxCharacters = 4_096,
    maxUtf8Bytes = null,
    usesClipboardFirst = false,
  ),
  INSTAGRAM_DIRECT(
    displayName = "Instagram DM",
    canonicalPackage = "com.instagram.android",
    fallbackPackages = emptyList(),
    defaultCharset = CharsetMode.BRAILLE_UNICODE,
    defaultWidthColumns = 22,
    maxCharacters = 1_000,
    maxUtf8Bytes = 1_000,
    usesClipboardFirst = true,
  );

  val candidatePackages: List<String>
    get() = buildList {
      add(canonicalPackage)
      addAll(fallbackPackages)
    }
}
