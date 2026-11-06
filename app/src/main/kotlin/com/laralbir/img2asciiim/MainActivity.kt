package com.laralbir.img2asciiim

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laralbir.img2asciiim.data.share.AndroidMessageShareGateway
import com.laralbir.img2asciiim.data.source.AndroidImageSourceRepository
import com.laralbir.img2asciiim.data.source.DeepLinkImageCodec
import com.laralbir.img2asciiim.ui.converter.ConverterScreen
import com.laralbir.img2asciiim.ui.converter.ConverterViewModel
import com.laralbir.img2asciiim.ui.theme.Img2asciiIMTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  private lateinit var imageSourceRepo: AndroidImageSourceRepository
  private lateinit var shareGateway: AndroidMessageShareGateway
  private var activeViewModel: ConverterViewModel? = null
  private var pendingViewIntent: Intent? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)

    setShowWhenLocked(true)
    setTurnScreenOn(true)
    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    imageSourceRepo = AndroidImageSourceRepository(applicationContext)
    shareGateway = AndroidMessageShareGateway(this)

    enableEdgeToEdge()

    setContent {
      Img2asciiIMTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background,
        ) {
          val vm: ConverterViewModel = viewModel(
            factory = ConverterViewModel.provideFactory(imageSourceRepo, shareGateway),
          )
          activeViewModel = vm

          LaunchedEffect(vm) {
            val pending = pendingViewIntent
            if (pending != null) {
              pendingViewIntent = null
              processIncomingIntent(pending, vm)
            } else {
              vm.checkClipboardOnResume()
            }
          }

          ConverterScreen(
            viewModel = vm,
            onPrepareCameraCaptureTarget = {
              imageSourceRepo.cameraCaptureFileManager.createCaptureTarget().uri
            },
          )
        }
      }
    }

    handleIncomingIntent(intent)
  }

  override fun onResume() {
    super.onResume()
    activeViewModel?.checkClipboardOnResume()
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) {
      activeViewModel?.checkClipboardOnResume()
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIncomingIntent(intent)
  }

  private fun handleIncomingIntent(incomingIntent: Intent?) {
    if (incomingIntent == null) return
    val vm = activeViewModel
    if (vm == null) {
      pendingViewIntent = incomingIntent
      return
    }
    processIncomingIntent(incomingIntent, vm)
  }

  private fun processIncomingIntent(incomingIntent: Intent, vm: ConverterViewModel) {
    if (incomingIntent.action == Intent.ACTION_SEND && incomingIntent.type?.startsWith("image/") == true) {
      lifecycleScope.launch {
        val outcome = imageSourceRepo.decodeIncomingShare(incomingIntent)
        vm.onIncomingShareResult(outcome)
      }
    } else if (incomingIntent.action == Intent.ACTION_VIEW && incomingIntent.data != null) {
      val dataString = incomingIntent.data.toString()
      lifecycleScope.launch {
        val outcome = DeepLinkImageCodec().decodeFromDeepLink(dataString)
        vm.onIncomingShareResult(outcome)
      }
    }
  }
}
