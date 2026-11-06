package com.laralbir.img2asciiim.data.source

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.laralbir.img2asciiim.domain.engine.DemoSampleImageFactory
import com.laralbir.img2asciiim.domain.model.DecodedImage
import com.laralbir.img2asciiim.domain.model.DomainResult
import com.laralbir.img2asciiim.domain.model.ImageDecodeError
import com.laralbir.img2asciiim.domain.repository.ImageSourceRepository

/**
 * Android implementation of [ImageSourceRepository] uniting safe URI/Intent validation,
 * subsampled EXIF-aware bitmap decoding, and `FileProvider` camera capture lifecycle management.
 */
class AndroidImageSourceRepository(
  context: Context,
  val incomingShareValidator: IncomingShareValidator = IncomingShareValidator(context),
  val imageDecoder: AndroidImageDecoder = AndroidImageDecoder(context),
  val cameraCaptureFileManager: CameraCaptureFileManager = CameraCaptureFileManager(context),
) : ImageSourceRepository {

  override suspend fun decodeFromUri(
    uriString: String,
    maxDimensionPx: Int,
  ): DomainResult<DecodedImage, ImageDecodeError> {
    if (uriString.isBlank()) {
      return DomainResult.Failure(ImageDecodeError.InvalidUriScheme)
    }

    val parsedUri = Uri.parse(uriString)
    val isMintedCameraUri = cameraCaptureFileManager.isMintedCaptureUri(parsedUri)
    val validation = incomingShareValidator.validateContentUri(
      uri = parsedUri,
      allowOwnAuthorityPredicate = { isMintedCameraUri },
    )

    val validUri = when (validation) {
      is DomainResult.Success -> validation.value
      is DomainResult.Failure -> return validation
    }

    return try {
      imageDecoder.decode(uri = validUri, maxDimensionPx = maxDimensionPx)
    } finally {
      if (isMintedCameraUri) {
        cameraCaptureFileManager.revokeTemporaryPermission(validUri)
      }
    }
  }

  suspend fun decodeIncomingShare(
    intent: Intent,
    maxDimensionPx: Int = ImageSourceRepository.DEFAULT_MAX_DIMENSION_PX,
  ): DomainResult<DecodedImage, ImageDecodeError> {
    val validation = incomingShareValidator.validate(intent)
    val validUri = when (validation) {
      is DomainResult.Success -> validation.value
      is DomainResult.Failure -> return validation
    }
    return imageDecoder.decode(uri = validUri, maxDimensionPx = maxDimensionPx)
  }

  override fun loadDemoSample(): DecodedImage =
    DemoSampleImageFactory.createInFlightWindowSample()
}
