package com.laralbir.img2asciiim.data.source

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ProviderInfo
import android.net.Uri
import android.os.BadParcelableException
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import com.laralbir.img2asciiim.domain.model.DomainResult
import com.laralbir.img2asciiim.domain.model.ImageDecodeError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class IncomingShareValidatorTest {

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val validator = IncomingShareValidator(context)

  @Test
  fun acceptsValidExternalContentUriAndClearsIntentSelector() {
    val externalProvider = ProviderInfo().apply {
      name = "com.external.gallery.ExternalMediaProvider"
      authority = "com.external.gallery.provider"
      packageName = "com.external.gallery"
      applicationInfo = ApplicationInfo().apply {
        packageName = "com.external.gallery"
        uid = Process.myUid() + 1000
      }
    }
    shadowOf(context.packageManager).addOrUpdateProvider(externalProvider)

    val sharedUri = Uri.parse("content://com.external.gallery.provider/media/101")
    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "image/jpeg"
      putExtra(Intent.EXTRA_STREAM, sharedUri)
      selector = Intent("com.evil.HIJACK")
    }

    val outcome = validator.validate(intent)
    assertNull(intent.selector, "Validator must neutralize intent.selector")
    assertTrue(outcome is DomainResult.Success)
    assertEquals(sharedUri, outcome.value)
  }

  @Test
  fun rejectsFileHttpAndAndroidResourceUriSchemes() {
    val invalidUris = listOf(
      Uri.parse("file:///sdcard/DCIM/secret.jpg"),
      Uri.parse("http://192.168.1.1/photo.png"),
      Uri.parse("https://example.com/photo.png"),
      Uri.parse("android.resource://com.laralbir.img2asciiim/drawable/ic_launcher_foreground"),
    )

    for (uri in invalidUris) {
      val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, uri)
      }
      val outcome = validator.validate(intent)
      assertEquals(
        DomainResult.Failure(ImageDecodeError.InvalidUriScheme),
        outcome,
        "Expected InvalidUriScheme for $uri",
      )
    }
  }

  @Test
  fun blocksConfusedDeputyUrisTargetingOwnPackageOrOwnUid() {
    // Case 1: Authority prefixed with own packageName
    val ownFileProviderIntent = Intent(Intent.ACTION_SEND).apply {
      type = "image/jpeg"
      putExtra(
        Intent.EXTRA_STREAM,
        Uri.parse("content://${context.packageName}.fileprovider/camera_captures/private.jpg"),
      )
    }
    assertEquals(
      DomainResult.Failure(ImageDecodeError.SecurityViolation),
      validator.validate(ownFileProviderIntent),
    )

    // Case 2: Disguised authority name resolving to Process.myUid()
    val disguisedOwnProvider = ProviderInfo().apply {
      name = "${context.packageName}.DisguisedInternalProvider"
      authority = "custom.internal.cache.authority"
      packageName = context.packageName
      enabled = true
      applicationInfo = ApplicationInfo(context.applicationInfo).apply {
        packageName = context.packageName
        uid = Process.myUid()
      }
    }
    shadowOf(context.packageManager).addOrUpdateProvider(disguisedOwnProvider)

    val disguisedIntent = Intent(Intent.ACTION_SEND).apply {
      type = "image/png"
      putExtra(Intent.EXTRA_STREAM, Uri.parse("content://custom.internal.cache.authority/item/1"))
    }
    assertEquals(
      DomainResult.Failure(ImageDecodeError.SecurityViolation),
      validator.validate(disguisedIntent),
    )
  }

  @Test
  fun handlesMalformedParcelableExtraWithoutCrashing() {
    val brokenBundle = Bundle().apply {
      putParcelable(Intent.EXTRA_STREAM, ThrowingParcelable())
    }
    val marshaled = Parcel.obtain()
    brokenBundle.writeToParcel(marshaled, 0)
    marshaled.setDataPosition(0)
    val unmarshaledBundle = Bundle()
    unmarshaledBundle.readFromParcel(marshaled)
    marshaled.recycle()

    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "image/*"
      putExtras(unmarshaledBundle)
    }

    val outcome = validator.validate(intent)
    assertTrue(outcome.isFailure)
  }

  private class ThrowingParcelable : Parcelable {
    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {
      dest.writeInt(1)
    }

    companion object {
      @JvmField
      val CREATOR = object : Parcelable.Creator<ThrowingParcelable> {
        override fun createFromParcel(source: Parcel): ThrowingParcelable {
          throw BadParcelableException("Simulated corrupted EXTRA_STREAM parcel")
        }

        override fun newArray(size: Int): Array<ThrowingParcelable?> = arrayOfNulls(size)
      }
    }
  }
}
