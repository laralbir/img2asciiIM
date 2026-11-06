package com.laralbir.img2asciiim.domain.model

/**
 * Normalized 2D floating-point grid where every cell is within `0.0f..1.0f`.
 */
class GrayscaleGrid(
  val width: Int,
  val height: Int,
  val luminance: FloatArray,
) {
  init {
    require(width > 0) { "width must be > 0, was $width" }
    require(height > 0) { "height must be > 0, was $height" }
    require(luminance.size == width * height) {
      "luminance.size (${luminance.size}) must equal width * height (${width * height})"
    }
    for (i in luminance.indices) {
      val v = luminance[i]
      require(v in 0.0f..1.0f) {
        "luminance[$i] must be in 0.0f..1.0f, was $v"
      }
    }
  }

  fun get(x: Int, y: Int): Float {
    require(x in 0 until width && y in 0 until height) {
      "Coordinate ($x, $y) out of bounds for ${width}x$height grid"
    }
    return luminance[y * width + x]
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is GrayscaleGrid) return false
    return width == other.width &&
      height == other.height &&
      luminance.contentEquals(other.luminance)
  }

  override fun hashCode(): Int {
    var result = width
    result = 31 * result + height
    result = 31 * result + luminance.contentHashCode()
    return result
  }
}
