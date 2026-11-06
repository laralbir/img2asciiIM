package com.laralbir.img2asciiim.data.source

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
 *   y la calidad JPEG al presupuesto de caracteres de la plataforma destino:
 *   - WhatsApp: [DeepLinkImageCodec.EXPANDED_WHATSAPP_MAX_DIMENSION] px, calidad [DeepLinkImageCodec.EXPANDED_WHATSAPP_QUALITY]
 *     (payload ~20–30 KB, cabe perfectamente en los 65.536 caracteres).
 *   - Telegram: [DeepLinkImageCodec.EXPANDED_TELEGRAM_MAX_DIMENSION] px, calidad [DeepLinkImageCodec.EXPANDED_TELEGRAM_QUALITY]
 *     (payload ~2.5–3 KB en Base64, cabe holgadamente en los 4.096 caracteres).
 */
enum class DeepLinkMode {
  STANDARD,
  EXPANDED_HD,
}

/**
 * Autonomous client-side encoder and decoder for self-contained image URLs (`RF-07`):
 * `https://laralbir.github.io/img2asciiIM/#data=<base64_jpeg>`
 *
 * Allows zero-server deep link sharing across WhatsApp, Telegram, and mobile web.
 */
open class DeepLinkImageCodec {

  data class QualityProfile(
    val maxDimension: Int,
    val quality: Int,
  )

  /**
   * Resolves the target maximum dimension and JPEG compression quality
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
   * according to the specified [mode] and platform [preset].
   */
  open fun encodeToDeepLink(
    pixelGrid: PixelGrid,
    mode: DeepLinkMode = DeepLinkMode.STANDARD,
    preset: ImPlatformPreset = ImPlatformPreset.WHATSAPP,
  ): String {
    val initialProfile = resolveProfile(mode, preset)
    var currentDim = initialProfile.maxDimension
    var currentQuality = initialProfile.quality
    var url = encodeToDeepLink(pixelGrid, maxDimension = currentDim, quality = currentQuality)

    val maxBudget = preset.maxCharacters
    if (url.length > maxBudget) {
      while (url.length > maxBudget && currentQuality > 20) {
        currentQuality -= 5
        url = encodeToDeepLink(pixelGrid, maxDimension = currentDim, quality = currentQuality)
      }
      while (url.length > maxBudget && currentDim > 80) {
        currentDim = (currentDim * 0.85f).toInt().coerceAtLeast(80)
        url = encodeToDeepLink(pixelGrid, maxDimension = currentDim, quality = currentQuality)
      }
    }
    return url
  }

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
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(10, 100), outputStream)
    if (scaledBitmap != srcBitmap) {
      scaledBitmap.recycle()
    }
    srcBitmap.recycle()

    val jpegBytes = outputStream.toByteArray()
    val base64Payload = Base64.encodeToString(
      jpegBytes,
      Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
    )
    return "$DEEP_LINK_BASE#data=$base64Payload"
  }

  fun decodeFromDeepLink(urlOrUri: String): DomainResult<DecodedImage, ImageDecodeError> {
    if (urlOrUri.isBlank()) {
      return DomainResult.Failure(ImageDecodeError.InvalidUriScheme)
    }

    val base64Payload = extractPayload(urlOrUri)
      ?: return DomainResult.Failure(ImageDecodeError.InvalidUriScheme)

    val bytes = try {
      Base64.decode(base64Payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
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

  private fun extractPayload(url: String): String? {
    val hashIdx = url.indexOf("#data=")
    if (hashIdx != -1) {
      return url.substring(hashIdx + 6).trim().takeIf { it.isNotEmpty() }
    }
    val queryIdx = url.indexOf("?data=")
    if (queryIdx != -1) {
      val afterQuery = url.substring(queryIdx + 6)
      val ampIdx = afterQuery.indexOf('&')
      val payload = if (ampIdx != -1) afterQuery.substring(0, ampIdx) else afterQuery
      return payload.trim().takeIf { it.isNotEmpty() }
    }
    return null
  }

  companion object {
    const val DEEP_LINK_BASE = "https://laralbir.github.io/img2asciiIM/"

    // Modo estándar (cuando se incluye arte ASCII)
    const val STANDARD_MAX_DIMENSION = 320
    const val STANDARD_QUALITY = 60

    // Modo expandido/HD (cuando NO se incluye arte ASCII)
    // WhatsApp: presupuesto holgado (65.536 caracteres)
    const val EXPANDED_WHATSAPP_MAX_DIMENSION = 720
    const val EXPANDED_WHATSAPP_QUALITY = 75

    // Telegram: límite 4.096 caracteres
    const val EXPANDED_TELEGRAM_MAX_DIMENSION = 200
    const val EXPANDED_TELEGRAM_QUALITY = 45

    // Aliases para compatibilidad hacia atrás
    const val DEFAULT_MAX_DIMENSION = STANDARD_MAX_DIMENSION
    const val DEFAULT_QUALITY = STANDARD_QUALITY
  }
}
