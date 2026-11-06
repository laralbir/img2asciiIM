package com.laralbir.img2asciiim.data.source

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.net.Uri
import android.os.BadParcelableException
import android.os.Build
import android.os.Process
import androidx.core.content.IntentCompat
import com.laralbir.img2asciiim.domain.model.DomainResult
import com.laralbir.img2asciiim.domain.model.ImageDecodeError

/**
 * Hardens incoming `Intent.ACTION_SEND` (`image/...`) shares and external URIs against
 * selector hijacking, `file://` / `http://` injection, malformed parcels, and Confused-Deputy attacks.
 */
class IncomingShareValidator(
  private val context: Context,
  private val myUidProvider: () -> Int = Process::myUid,
) {

  fun isImageShareIntent(intent: Intent?): Boolean {
    if (intent == null || intent.action != Intent.ACTION_SEND) return false
    val mime = intent.type ?: return false
    return mime == "image/*" || mime.startsWith("image/")
  }

  fun validate(intent: Intent): DomainResult<Uri, ImageDecodeError> {
    // Neutralize selector hijacking unconditionally before inspecting any intent fields
    intent.selector = null

    if (!isImageShareIntent(intent)) {
      return DomainResult.Failure(ImageDecodeError.InvalidOrCorruptedImage)
    }

    val streamUri: Uri = try {
      IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
    } catch (_: BadParcelableException) {
      return DomainResult.Failure(ImageDecodeError.InvalidOrCorruptedImage)
    } catch (_: RuntimeException) {
      return DomainResult.Failure(ImageDecodeError.InvalidOrCorruptedImage)
    } ?: return DomainResult.Failure(ImageDecodeError.InvalidOrCorruptedImage)

    return validateContentUri(
      uri = streamUri,
      allowOwnAuthorityPredicate = { false },
    )
  }

  fun validateContentUri(
    uri: Uri,
    allowOwnAuthorityPredicate: (Uri) -> Boolean = { false },
  ): DomainResult<Uri, ImageDecodeError> {
    if (uri.scheme != ContentResolver.SCHEME_CONTENT) {
      return DomainResult.Failure(ImageDecodeError.InvalidUriScheme)
    }

    val authority = uri.authority?.takeIf { it.isNotBlank() }
      ?: return DomainResult.Failure(ImageDecodeError.InvalidUriScheme)

    val startsWithOwnPackage = authority.startsWith(context.packageName)
    val providerInfo = resolveProvider(authority)
    val myUid = myUidProvider()
    val ownAppUid = context.applicationInfo.uid
    val providerUid = providerInfo?.applicationInfo?.uid
    val resolvesToOwnApp = providerInfo != null &&
      (providerUid == myUid || providerUid == ownAppUid || providerInfo.packageName == context.packageName)

    if ((startsWithOwnPackage || resolvesToOwnApp) && !allowOwnAuthorityPredicate(uri)) {
      return DomainResult.Failure(ImageDecodeError.SecurityViolation)
    }

    return DomainResult.Success(uri)
  }

  private fun resolveProvider(authority: String): ProviderInfo? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      try {
        return context.packageManager.resolveContentProvider(
          authority,
          PackageManager.ComponentInfoFlags.of(0),
        )
      } catch (_: UnsupportedOperationException) {
        // Fallback for environments (e.g. Robolectric) that only implement the int-flags overload
      } catch (_: RuntimeException) {
        return null
      }
    }
    return try {
      @Suppress("DEPRECATION")
      context.packageManager.resolveContentProvider(authority, 0)
    } catch (_: RuntimeException) {
      null
    }
  }
}
