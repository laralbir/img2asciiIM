package com.laralbir.img2asciiim.domain.repository

import com.laralbir.img2asciiim.domain.model.DecodedImage
import com.laralbir.img2asciiim.domain.model.DomainResult
import com.laralbir.img2asciiim.domain.model.ImageDecodeError

/**
 * Pure Kotlin gateway for loading and decoding user-selected or shared images offline.
 */
interface ImageSourceRepository {

  suspend fun decodeFromUri(
    uriString: String,
    maxDimensionPx: Int = DEFAULT_MAX_DIMENSION_PX,
  ): DomainResult<DecodedImage, ImageDecodeError>

  fun loadDemoSample(): DecodedImage

  companion object {
    const val DEFAULT_MAX_DIMENSION_PX: Int = 768
  }
}
