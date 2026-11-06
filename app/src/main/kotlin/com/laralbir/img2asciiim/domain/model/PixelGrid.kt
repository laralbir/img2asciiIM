package com.laralbir.img2asciiim.domain.model

/**
 * Immutable 2D grid of packed 32-bit straight ARGB (`0xAARRGGBB`) pixels.
 */
class PixelGrid(
  val width: Int,
  val height: Int,
  val argbPixels: IntArray,
) {
  init {
    require(width > 0) { "width must be > 0, was $width" }
    require(height > 0) { "height must be > 0, was $height" }
    require(argbPixels.size == width * height) {
      "argbPixels.size (${argbPixels.size}) must equal width * height (${width * height})"
    }
  }

  fun get(x: Int, y: Int): Int {
    require(x in 0 until width && y in 0 until height) {
      "Coordinate ($x, $y) out of bounds for ${width}x$height grid"
    }
    return argbPixels[y * width + x]
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is PixelGrid) return false
    return width == other.width &&
      height == other.height &&
      argbPixels.contentEquals(other.argbPixels)
  }

  override fun hashCode(): Int {
    var result = width
    result = 31 * result + height
    result = 31 * result + argbPixels.contentHashCode()
    return result
  }
}
