package com.laralbir.img2asciiim

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laralbir.img2asciiim.data.share.AndroidMessageShareGateway
import com.laralbir.img2asciiim.data.source.AndroidImageSourceRepository
import com.laralbir.img2asciiim.ui.converter.ConverterScreen
import com.laralbir.img2asciiim.ui.converter.ConverterViewModel
import com.laralbir.img2asciiim.ui.theme.Img2asciiIMTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  private lateinit var imageSourceRepo: AndroidImageSourceRepository
  private lateinit var shareGateway: AndroidMessageShareGateway
  private var activeViewModel: ConverterViewModel? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)

    imageSourceRepo = AndroidImageSourceRepository(applicationContext)
    shareGateway = AndroidMessageShareGateway(this)

    enableEdgeToEdge()

    setContent {
      Img2asciiIMTheme {
        val vm: ConverterViewModel = viewModel(
          factory = ConverterViewModel.provideFactory(imageSourceRepo, shareGateway),
        )
        activeViewModel = vm

        ConverterScreen(
          viewModel = vm,
          onPrepareCameraCaptureTarget = {
            imageSourceRepo.cameraCaptureFileManager.createCaptureTarget().uri
          },
        )
      }
    }

    handleIncomingShare(intent)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIncomingShare(intent)
  }

  private fun handleIncomingShare(incomingIntent: Intent?) {
    if (incomingIntent == null) return
    if (incomingIntent.action == Intent.ACTION_SEND && incomingIntent.type?.startsWith("image/") == true) {
      lifecycleScope.launch {
        val outcome = imageSourceRepo.decodeIncomingShare(incomingIntent)
        activeViewModel?.onIncomingShareResult(outcome)
      }
    }
  }
}
