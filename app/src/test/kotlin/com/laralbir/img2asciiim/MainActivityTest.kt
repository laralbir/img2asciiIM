package com.laralbir.img2asciiim

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import kotlin.test.Test
import kotlin.test.assertNotNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MainActivityTest {

  @Test
  fun manifestResolvesCustomUriSchemeIntents() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val packageManager = context.packageManager

    // Test img2asciiim://view?d=test
    val intent1 = Intent(Intent.ACTION_VIEW, Uri.parse("img2asciiim://view?d=test"))
    val resolvedList1 = packageManager.queryIntentActivities(intent1, 0)
    assertNotNull(
      resolvedList1.firstOrNull { it.activityInfo.name == MainActivity::class.java.name },
      "MainActivity must resolve img2asciiim://view?d=...",
    )

    // Test img2ascii://view?d=test
    val intent2 = Intent(Intent.ACTION_VIEW, Uri.parse("img2ascii://view?d=test"))
    val resolvedList2 = packageManager.queryIntentActivities(intent2, 0)
    assertNotNull(
      resolvedList2.firstOrNull { it.activityInfo.name == MainActivity::class.java.name },
      "MainActivity must resolve img2ascii://view?d=...",
    )

    // Test https://laralbir.github.io/img2asciiIM/#d=test
    val intent3 = Intent(Intent.ACTION_VIEW, Uri.parse("https://laralbir.github.io/img2asciiIM/#d=test"))
    val resolvedList3 = packageManager.queryIntentActivities(intent3, 0)
    assertNotNull(
      resolvedList3.firstOrNull { it.activityInfo.name == MainActivity::class.java.name },
      "MainActivity must resolve https://laralbir.github.io/img2asciiIM/...",
    )
  }

  @Test
  fun launchesWithCustomUriSchemeWithoutCrashing() {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("img2asciiim://view?d=samplePayload"))
    val scenario = ActivityScenario.launch<MainActivity>(intent)
    scenario.onActivity { activity ->
      assertNotNull(activity)
    }
    scenario.close()
  }
}
