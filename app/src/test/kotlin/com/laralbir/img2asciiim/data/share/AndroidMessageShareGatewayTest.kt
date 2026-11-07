package com.laralbir.img2asciiim.data.share

import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import com.laralbir.img2asciiim.domain.model.ImPlatformPreset
import com.laralbir.img2asciiim.domain.model.PixelGrid
import com.laralbir.img2asciiim.domain.model.ShareOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidMessageShareGatewayTest {

  private val context: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun copiesExactTextToSystemClipboard() {
    val gateway = AndroidMessageShareGateway(context)
    val art = "```.:-=+*#%@```"

    gateway.copyToClipboard(art)

    val clipboard = context.getSystemService(ClipboardManager::class.java)
    val clip = clipboard.primaryClip
    assertTrue(clip != null && clip.itemCount == 1)
    assertEquals("img2asciiIM", clip.description.label.toString())
    assertEquals(art, clip.getItemAt(0).text.toString())
  }

  @Test
  fun returnsNullWhenClipboardIsEmpty() {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard.clearPrimaryClip()
    val gateway = AndroidMessageShareGateway(context)

    val text = gateway.getClipboardText()

    assertEquals(null, text)
  }

  @Test
  fun readsTextFromClipboardSuccessfully() {
    val gateway = AndroidMessageShareGateway(context)
    val testPayload = "https://laralbir.github.io/img2asciiIM/#d=testpayload"
    gateway.copyToClipboard(testPayload)

    val text = gateway.getClipboardText()

    assertEquals(testPayload, text)
  }

  @Test
  fun opensCanonicalTelegramPackageDirectlyWhenInstalled() {
    installPackage("org.telegram.messenger")
    val launchedIntents = mutableListOf<Intent>()
    val gateway = AndroidMessageShareGateway(
      context = context,
      activityStarter = { launchedIntents += it },
    )

    val outcome = gateway.shareToPlatform("⠀⠀⣿⣿⠀⠀", ImPlatformPreset.TELEGRAM)

    assertEquals(
      ShareOutcome.OpenedDirectApp(packageName = "org.telegram.messenger", copiedToClipboard = true),
      outcome,
    )
    assertEquals(1, launchedIntents.size)
    val sent = launchedIntents.single()
    assertEquals(Intent.ACTION_SEND, sent.action)
    assertEquals("text/plain", sent.type)
    assertEquals("org.telegram.messenger", sent.`package`)
    assertEquals("⠀⠀⣿⣿⠀⠀", sent.getStringExtra(Intent.EXTRA_TEXT))
    assertTrue((sent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
  }

  @Test
  fun resolvesWhatsAppBusinessFallbackPackageWhenCanonicalWhatsAppIsMissing() {
    // DD-04 Opción A: com.whatsapp not installed, com.whatsapp.w4b installed
    installPackage("com.whatsapp.w4b")
    val launchedIntents = mutableListOf<Intent>()
    val gateway = AndroidMessageShareGateway(
      context = context,
      activityStarter = { launchedIntents += it },
    )

    val outcome = gateway.shareToPlatform("```@%#```", ImPlatformPreset.WHATSAPP)

    assertEquals(
      ShareOutcome.OpenedDirectApp(packageName = "com.whatsapp.w4b", copiedToClipboard = true),
      outcome,
    )
    assertEquals("com.whatsapp.w4b", launchedIntents.single().`package`)
  }

  @Test
  fun copiesToClipboardFirstForInstagramDirectAndFallsBackToSystemChooserWhenActivityThrowsOrMissing() {
    // Install com.instagram.android, but simulate Instagram rejecting plain-text ACTION_SEND with ActivityNotFoundException
    installPackage("com.instagram.android")
    val launchedIntents = mutableListOf<Intent>()
    val gateway = AndroidMessageShareGateway(
      context = context,
      activityStarter = { intent ->
        if (intent.`package` == "com.instagram.android") {
          throw ActivityNotFoundException("Instagram text/plain handler unavailable")
        }
        launchedIntents += intent
      },
    )

    val igArt = "⠀⣿⠀⣿⠀"
    val outcome = gateway.shareToPlatform(igArt, ImPlatformPreset.INSTAGRAM_DIRECT)

    assertEquals(ShareOutcome.OpenedSystemChooser(copiedToClipboard = true), outcome)

    // Verify Clipboard-First copied exact text
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    assertEquals(igArt, clipboard.primaryClip?.getItemAt(0)?.text?.toString())

    // Verify Chooser intent was launched safely
    assertEquals(1, launchedIntents.size)
    val chooser = launchedIntents.single()
    assertEquals(Intent.ACTION_CHOOSER, chooser.action)
    assertTrue((chooser.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
  }

  @Test
  fun saveImageToGallery_savesBitmapToMediaStoreSuccessfully() {
    val gateway = AndroidMessageShareGateway(context)
    val grid = PixelGrid(
      width = 4,
      height = 4,
      argbPixels = IntArray(16) { 0xFF00FF00.toInt() },
    )

    val saved = gateway.saveImageToGallery(grid, filenamePrefix = "test_img")

    assertTrue(saved)
  }

  private fun installPackage(packageName: String) {
    val pkgInfo = PackageInfo().apply {
      this.packageName = packageName
    }
    shadowOf(context.packageManager).installPackage(pkgInfo)
  }
}
