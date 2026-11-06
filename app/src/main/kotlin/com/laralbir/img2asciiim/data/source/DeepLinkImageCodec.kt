package com.laralbir.img2asciiim.data.source

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Base64
import com.laralbir.img2asciiim.domain.model.DecodedImage
import com.laralbir.img2asciiim.domain.model.DomainResult
import com.laralbir.img2asciiim.domain.model.ImageDecodeError
import com.laralbir.img2asciiim.domain.model.ImPlatformPreset
import com.laralbir.img2asciiim.domain.model.PixelGrid
import java.io.ByteArrayOutputStream

/**
 * Compression mode for deep link generation (`RF-07`).
 *
 * - [STANDARD]: Modo estándar (cuando se incluye arte ASCII). Conserva dimensiones compactas
 *   ([DeepLinkImageCodec.STANDARD_MAX_DIMENSION] px, calidad [DeepLinkImageCodec.STANDARD_QUALITY]),
 *   generando un payload de ~6–10 KB para acompañar al arte de texto sin desbordar los límites del mensaje.
 * - [EXPANDED_HD]: Modo expandido / HD (cuando NO se incluye arte ASCII). Adapta la resolución
 *   y la calidad WebP al presupuesto de caracteres de la plataforma destino:
 *   - WhatsApp: [DeepLinkImageCodec.EXPANDED_WHATSAPP_MAX_DIMENSION] px, calidad [DeepLinkImageCodec.EXPANDED_WHATSAPP_QUALITY]
 *     (presupuesto hasta [DeepLinkImageCodec.WHATSAPP_EXPANDED_BUDGET] caracteres).
 *   - Telegram: [DeepLinkImageCodec.EXPANDED_TELEGRAM_MAX_DIMENSION] px, calidad [DeepLinkImageCodec.EXPANDED_TELEGRAM_QUALITY]
 *     (presupuesto estricto de [DeepLinkImageCodec.TELEGRAM_BUDGET] caracteres con margen de seguridad).
 */
enum class DeepLinkMode {
  STANDARD,
  EXPANDED_HD,
}

/**
 * Autonomous client-side encoder and decoder for self-contained image URLs (`RF-07`):
 * `https://laralbir.github.io/img2asciiIM/#d=<base64_webp>`
 *
 * Allows zero-server deep link sharing across WhatsApp, Telegram, and mobile web.
 */
open class DeepLinkImageCodec {

  data class QualityProfile(
    val maxDimension: Int,
    val quality: Int,
  )

  /**
   * Resolves the target maximum dimension and compression quality
   * based on the given [mode] and [preset].
   */
  open fun resolveProfile(
    mode: DeepLinkMode = DeepLinkMode.STANDARD,
    preset: ImPlatformPreset = ImPlatformPreset.WHATSAPP,
  ): QualityProfile = when (mode) {
    DeepLinkMode.STANDARD -> QualityProfile(
      maxDimension = STANDARD_MAX_DIMENSION,
      quality = STANDARD_QUALITY,
    )
    DeepLinkMode.EXPANDED_HD -> when (preset) {
      ImPlatformPreset.WHATSAPP -> QualityProfile(
        maxDimension = EXPANDED_WHATSAPP_MAX_DIMENSION,
        quality = EXPANDED_WHATSAPP_QUALITY,
      )
      ImPlatformPreset.TELEGRAM,
      ImPlatformPreset.INSTAGRAM_DIRECT -> QualityProfile(
        maxDimension = EXPANDED_TELEGRAM_MAX_DIMENSION,
        quality = EXPANDED_TELEGRAM_QUALITY,
      )
    }
  }

  /**
   * Encodes a [PixelGrid] to a deep link URL adapting resolution and quality
   * according to the specified [mode], platform [preset], and optional [maxCharacterBudget].
   */
  open fun encodeToDeepLink(
    pixelGrid: PixelGrid,
    mode: DeepLinkMode = DeepLinkMode.STANDARD,
    preset: ImPlatformPreset = ImPlatformPreset.WHATSAPP,
    maxCharacterBudget: Int? = null,
  ): String {
    val initialProfile = resolveProfile(mode, preset)
    var currentDim = initialProfile.maxDimension
    var currentQuality = initialProfile.quality
    var url = encodeToDeepLink(pixelGrid, maxDimension = currentDim, quality = currentQuality)

    val budget = maxCharacterBudget ?: when (preset) {
      ImPlatformPreset.TELEGRAM -> TELEGRAM_BUDGET
      ImPlatformPreset.WHATSAPP -> when (mode) {
        DeepLinkMode.STANDARD -> WHATSAPP_STANDARD_BUDGET
        DeepLinkMode.EXPANDED_HD -> WHATSAPP_EXPANDED_BUDGET
      }
      ImPlatformPreset.INSTAGRAM_DIRECT -> 900
    }

    if (url.length > budget) {
      // 1. Progresivamente reduce la calidad WebP (p. ej. de 50 a 20)
      while (url.length > budget && currentQuality > 20) {
        currentQuality = (currentQuality - 5).coerceAtLeast(20)
        url = encodeToDeepLink(pixelGrid, maxDimension = currentDim, quality = currentQuality)
      }

      // 2. Reduce progresivamente las dimensiones (15% cada iteración hasta mínimo de 48px)
      while (url.length > budget && currentDim > 48) {
        val nextDim = (currentDim * 0.85f).toInt()
        currentDim = if (nextDim >= currentDim) currentDim - 1 else nextDim
        currentDim = currentDim.coerceAtLeast(48)
        url = encodeToDeepLink(pixelGrid, maxDimension = currentDim, quality = currentQuality)
      }

      // 3. Reduce la calidad hasta el mínimo de 15
      while (url.length > budget && currentQuality > 15) {
        currentQuality = (currentQuality - 1).coerceAtLeast(15)
        url = encodeToDeepLink(pixelGrid, maxDimension = currentDim, quality = currentQuality)
      }

      // 4. Garantía estricta: si aún sobrepasa el presupuesto, continúa reduciendo dimensiones y calidad
      while (url.length > budget && currentDim > 16) {
        currentDim = (currentDim * 0.80f).toInt().coerceAtLeast(16)
        url = encodeToDeepLink(pixelGrid, maxDimension = currentDim, quality = currentQuality)
      }
      while (url.length > budget && currentQuality > 5) {
        currentQuality = (currentQuality - 2).coerceAtLeast(5)
        url = encodeToDeepLink(pixelGrid, maxDimension = currentDim, quality = currentQuality)
      }
      while (url.length > budget && currentDim > 1) {
        currentDim = (currentDim - 1).coerceAtLeast(1)
        url = encodeToDeepLink(pixelGrid, maxDimension = currentDim, quality = currentQuality)
      }
    }
    return url
  }

  /**
   * Overload for backward compatibility with 3-arg callers and mock subclasses.
   */
  open fun encodeToDeepLink(
    pixelGrid: PixelGrid,
    mode: DeepLinkMode,
    preset: ImPlatformPreset,
  ): String = encodeToDeepLink(pixelGrid, mode, preset, maxCharacterBudget = null)

  /**
   * Encodes a [PixelGrid] to a self-contained deep link with explicit dimensions and quality.
   */
  fun encodeToDeepLink(
    pixelGrid: PixelGrid,
    maxDimension: Int,
    quality: Int = DEFAULT_QUALITY,
  ): String {
    val srcW = pixelGrid.width
    val srcH = pixelGrid.height
    val scale = if (srcW > maxDimension || srcH > maxDimension) {
      maxDimension.toFloat() / maxOf(srcW, srcH).toFloat()
    } else {
      1.0f
    }
    val targetW = (srcW * scale).toInt().coerceAtLeast(1)
    val targetH = (srcH * scale).toInt().coerceAtLeast(1)

    val srcBitmap = Bitmap.createBitmap(pixelGrid.argbPixels, srcW, srcH, Bitmap.Config.ARGB_8888)
    val scaledBitmap = if (targetW != srcW || targetH != srcH) {
      Bitmap.createScaledBitmap(srcBitmap, targetW, targetH, true)
    } else {
      srcBitmap
    }

    val outputStream = ByteArrayOutputStream()
    val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      Bitmap.CompressFormat.WEBP_LOSSY
    } else {
      @Suppress("DEPRECATION")
      Bitmap.CompressFormat.WEBP
    }
    scaledBitmap.compress(format, quality.coerceIn(1, 100), outputStream)
    if (scaledBitmap != srcBitmap) {
      scaledBitmap.recycle()
    }
    srcBitmap.recycle()

    val compressedBytes = outputStream.toByteArray()
    val base64Payload = Base64.encodeToString(
      compressedBytes,
      Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
    )
    return "$DEEP_LINK_BASE#d=$base64Payload"
  }

  fun decodeFromDeepLink(urlOrUri: String): DomainResult<DecodedImage, ImageDecodeError> {
    if (urlOrUri.isBlank()) {
      return DomainResult.Failure(ImageDecodeError.InvalidUriScheme)
    }

    val base64Payload = extractPayload(urlOrUri)
      ?: return DomainResult.Failure(ImageDecodeError.InvalidUriScheme)

    val bytes = try {
      try {
        Base64.decode(base64Payload, Base64.URL_SAFE or Base64.NO_WRAP)
      } catch (_: IllegalArgumentException) {
        Base64.decode(base64Payload, Base64.DEFAULT)
      }
    } catch (_: IllegalArgumentException) {
      return DomainResult.Failure(ImageDecodeError.InvalidOrCorruptedImage)
    }

    if (bytes.isEmpty()) {
      return DomainResult.Failure(ImageDecodeError.InvalidOrCorruptedImage)
    }

    val bitmap = try {
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: RuntimeException) {
      null
    } ?: return DomainResult.Failure(ImageDecodeError.InvalidOrCorruptedImage)

    val w = bitmap.width
    val h = bitmap.height
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
    bitmap.recycle()

    return DomainResult.Success(
      DecodedImage(
        pixelGrid = PixelGrid(width = w, height = h, argbPixels = pixels),
        originalWidth = w,
        originalHeight = h,
        label = "Foto en alta definición (Deep Link)",
      ),
    )
  }

  internal fun extractPayload(url: String): String? {
    val dIdx = url.indexOf("#d=")
    if (dIdx != -1) {
      return sanitizePayload(url.substring(dIdx + 3))
    }
    val hashIdx = url.indexOf("#data=")
    if (hashIdx != -1) {
      return sanitizePayload(url.substring(hashIdx + 6))
    }
    val queryDIdx = url.indexOf("?d=")
    if (queryDIdx != -1) {
      return extractParamValue(url, queryDIdx + 3)
    }
    val ampDIdx = url.indexOf("&d=")
    if (ampDIdx != -1) {
      return extractParamValue(url, ampDIdx + 3)
    }
    val queryIdx = url.indexOf("?data=")
    if (queryIdx != -1) {
      return extractParamValue(url, queryIdx + 6)
    }
    val ampDataIdx = url.indexOf("&data=")
    if (ampDataIdx != -1) {
      return extractParamValue(url, ampDataIdx + 6)
    }
    return null
  }

  private fun extractParamValue(url: String, startIndex: Int): String? {
    val remainder = url.substring(startIndex)
    val ampIdx = remainder.indexOf('&')
    val hashIdx = remainder.indexOf('#')
    val cutIdx = when {
      ampIdx != -1 && hashIdx != -1 -> minOf(ampIdx, hashIdx)
      ampIdx != -1 -> ampIdx
      hashIdx != -1 -> hashIdx
      else -> -1
    }
    val raw = if (cutIdx != -1) remainder.substring(0, cutIdx) else remainder
    return sanitizePayload(raw)
  }

  private fun sanitizePayload(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    val decoded = try {
      if (trimmed.contains("%")) {
        java.net.URLDecoder.decode(trimmed, "UTF-8")
      } else {
        trimmed
      }
    } catch (_: Exception) {
      trimmed
    }
    return decoded.trim().takeIf { it.isNotEmpty() }
  }

  companion object {
    const val DEEP_LINK_BASE = "https://laralbir.github.io/img2asciiIM/"

    // Character budgets
    const val TELEGRAM_BUDGET = 3_900
    const val WHATSAPP_STANDARD_BUDGET = 8_000
    const val WHATSAPP_EXPANDED_BUDGET = 60_000

    // Modo estándar (cuando se incluye arte ASCII)
    const val STANDARD_MAX_DIMENSION = 320
    const val STANDARD_QUALITY = 60

    // Modo expandido/HD (cuando NO se incluye arte ASCII)
    // WhatsApp: presupuesto holgado (60.000 caracteres)
    const val EXPANDED_WHATSAPP_MAX_DIMENSION = 720
    const val EXPANDED_WHATSAPP_QUALITY = 75

    // Telegram: presupuesto estricto (3.900 caracteres)
    const val EXPANDED_TELEGRAM_MAX_DIMENSION = 200
    const val EXPANDED_TELEGRAM_QUALITY = 45

    // Aliases para compatibilidad hacia atrás
    const val DEFAULT_MAX_DIMENSION = STANDARD_MAX_DIMENSION
    const val DEFAULT_QUALITY = STANDARD_QUALITY
  }
}
