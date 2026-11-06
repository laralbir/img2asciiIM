package com.laralbir.img2asciiim.data.share

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.laralbir.img2asciiim.domain.model.ImPlatformPreset
import com.laralbir.img2asciiim.domain.model.ShareOutcome
import com.laralbir.img2asciiim.domain.repository.MessageShareGateway

/**
 * Android implementation of [MessageShareGateway] enforcing Clipboard-First delivery for
 * Instagram Direct, smart canonical + fallback package resolution (`DD-04 Opción A`),
 * and zero-crash chooser fallback on `ActivityNotFoundException` or `SecurityException`.
 */
class AndroidMessageShareGateway(
  private val context: Context,
  private val chooserTitle: String = DEFAULT_CHOOSER_TITLE,
  private val activityStarter: (Intent) -> Unit = { intent -> context.startActivity(intent) },
) : MessageShareGateway {

  override fun copyToClipboard(text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    val clip = ClipData.newPlainText(CLIPBOARD_LABEL, text)
    clipboard.setPrimaryClip(clip)
  }

  override fun shareToPlatform(
    text: String,
    preset: ImPlatformPreset,
  ): ShareOutcome {
    val copiedFirst = if (preset.usesClipboardFirst) {
      copyToClipboard(text)
      true
    } else {
      false
    }

    for (candidatePkg in preset.candidatePackages) {
      if (!isPackageInstalled(candidatePkg)) continue

      val directIntent = Intent(Intent.ACTION_SEND).apply {
        type = MIME_TEXT_PLAIN
        putExtra(Intent.EXTRA_TEXT, text)
        setPackage(candidatePkg)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }

      val launchedDirect = try {
        activityStarter(directIntent)
        true
      } catch (_: ActivityNotFoundException) {
        false
      } catch (_: SecurityException) {
        false
      }

      if (launchedDirect) {
        return ShareOutcome.OpenedDirectApp(
          packageName = candidatePkg,
          copiedToClipboard = copiedFirst,
        )
      }
    }

    val baseSendIntent = Intent(Intent.ACTION_SEND).apply {
      type = MIME_TEXT_PLAIN
      putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooserIntent = Intent.createChooser(baseSendIntent, chooserTitle).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    activityStarter(chooserIntent)

    return ShareOutcome.OpenedSystemChooser(copiedToClipboard = copiedFirst)
  }

  private fun isPackageInstalled(packageName: String): Boolean = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      context.packageManager.getPackageInfo(
        packageName,
        PackageManager.PackageInfoFlags.of(0),
      )
    } else {
      @Suppress("DEPRECATION")
      context.packageManager.getPackageInfo(packageName, 0)
    }
    true
  } catch (_: PackageManager.NameNotFoundException) {
    false
  }

  companion object {
    const val CLIPBOARD_LABEL: String = "img2asciiIM"
    const val MIME_TEXT_PLAIN: String = "text/plain"
    const val DEFAULT_CHOOSER_TITLE: String = "Enviar arte de texto"
  }
}
