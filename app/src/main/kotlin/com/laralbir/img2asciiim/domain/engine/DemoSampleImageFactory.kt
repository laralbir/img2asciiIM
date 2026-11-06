package com.laralbir.img2asciiim.domain.engine

import com.laralbir.img2asciiim.domain.model.DecodedImage
import com.laralbir.img2asciiim.domain.model.PixelGrid
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Pure Kotlin procedural generator producing a recognizable high-contrast airplane window scene
 * (window bezel + bright sun/moon disc + horizon cloud deck + swept wing silhouette) so the app
 * boots with an instant interactive preview and tests have a deterministic rich fixture.
 */
object DemoSampleImageFactory {

  fun createInFlightWindowSample(
    width: Int = 160,
    height: Int = 120,
  ): DecodedImage {
    require(width > 0 && height > 0) { "Sample dimensions must be > 0, got ${width}x$height" }
    val pixels = IntArray(width * height)

    for (y in 0 until height) {
      val ny = (y + 0.5f) / height.toFloat()
      for (x in 0 until width) {
        val nx = (x + 0.5f) / width.toFloat()

        // Rounded-stadium aircraft window aperture centered at (0.5, 0.5)
        val dx = (nx - 0.5f) / 0.36f
        val dy = (ny - 0.5f) / 0.42f
        val stadiumDist = roundedBoxDistance(dx, dy, cornerRadius = 0.65f)

        val argb = if (stadiumDist > 1.0f) {
          // Dark cabin interior wall outside the airplane window
          packRgb(14, 18, 26)
        } else if (stadiumDist > 0.90f) {
          // Subtle inner window rim highlight
          packRgb(92, 108, 128)
        } else {
          renderWindowSkyAndWing(nx, ny)
        }

        pixels[y * width + x] = argb
      }
    }

    return DecodedImage(
      pixelGrid = PixelGrid(width = width, height = height, argbPixels = pixels),
      originalWidth = width,
      originalHeight = height,
      label = DEFAULT_SAMPLE_LABEL,
    )
  }

  private fun renderWindowSkyAndWing(nx: Float, ny: Float): Int {
    // Swept aircraft wing + winglet silhouette in the lower-right foreground
    val leadingEdgeY = 0.78f - 0.30f * (nx - 0.24f)
    val trailingEdgeY = 0.90f - 0.16f * (nx - 0.24f)
    val inMainWing = nx >= 0.26f && nx <= 0.80f && ny >= leadingEdgeY && ny <= trailingEdgeY
    val inWinglet = nx >= 0.76f && nx <= 0.82f && ny >= (leadingEdgeY - 0.11f) && ny <= trailingEdgeY

    if (inMainWing || inWinglet) {
      // Dark metallic wing silhouette with slight top-edge glint
      val edgeHighlight = if (ny - leadingEdgeY in 0.0f..0.02f) 70 else 24
      return packRgb(edgeHighlight, edgeHighlight + 6, edgeHighlight + 14)
    }

    // Sun / moon disc in upper-left sky
    val sunDx = nx - 0.35f
    val sunDy = (ny - 0.27f) * 0.85f
    val sunDist = sqrt(sunDx * sunDx + sunDy * sunDy)
    if (sunDist <= 0.085f) {
      return packRgb(252, 250, 240)
    }

    // Fluffy cloud deck along the horizon (ny ~ 0.56..0.70)
    val inCloudPuff1 = circleContains(nx, ny, cx = 0.30f, cy = 0.62f, rx = 0.14f, ry = 0.07f)
    val inCloudPuff2 = circleContains(nx, ny, cx = 0.48f, cy = 0.60f, rx = 0.16f, ry = 0.085f)
    val inCloudPuff3 = circleContains(nx, ny, cx = 0.67f, cy = 0.63f, rx = 0.14f, ry = 0.065f)
    if (inCloudPuff1 || inCloudPuff2 || inCloudPuff3) {
      return packRgb(236, 242, 248)
    }

    // Atmospheric high-altitude sky gradient (darker stratosphere above, bright horizon band)
    val horizonFactor = (1.0f - kotlin.math.abs(ny - 0.62f) * 2.1f).coerceIn(0.0f, 1.0f)
    val r = (34 + 165 * horizonFactor).toInt().coerceIn(0, 255)
    val g = (62 + 165 * horizonFactor).toInt().coerceIn(0, 255)
    val b = (112 + 125 * horizonFactor).toInt().coerceIn(0, 255)
    return packRgb(r, g, b)
  }

  private fun roundedBoxDistance(dx: Float, dy: Float, cornerRadius: Float): Float {
    val ax = kotlin.math.abs(dx)
    val ay = kotlin.math.abs(dy)
    val inner = 1.0f - cornerRadius
    val qx = max(ax - inner, 0.0f)
    val qy = max(ay - inner, 0.0f)
    return (min(max(ax - inner, ay - inner), 0.0f) + sqrt(qx * qx + qy * qy)) / cornerRadius + inner
  }

  private fun circleContains(
    x: Float,
    y: Float,
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
  ): Boolean {
    val dx = (x - cx) / rx
    val dy = (y - cy) / ry
    return dx * dx + dy * dy <= 1.0f
  }

  private fun packRgb(r: Int, g: Int, b: Int): Int =
    (0xFF shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)

  const val DEFAULT_SAMPLE_LABEL: String = "Ventanilla en vuelo (Demo)"
}
