package com.laralbir.img2asciiim.data.update

import android.os.Build
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class GitHubUpdateCheckerTest {

  private val checker = GitHubUpdateChecker()

  @Test
  fun parsesValidGitHubReleaseJsonCorrectly() {
    val sampleJson = """
      {
        "tag_name": "v1.1.0",
        "name": "img2asciiIM v1.1.0",
        "html_url": "https://github.com/laralbir/img2asciiIM/releases/tag/v1.1.0",
        "body": "### Novedades\n- Soporte para enlaces de alta resolución.",
        "assets": [
          {
            "name": "img2asciiIM-v1.1.0.apk",
            "browser_download_url": "https://github.com/laralbir/img2asciiIM/releases/download/v1.1.0/img2asciiIM-v1.1.0.apk"
          },
          {
            "name": "checksum.sha256",
            "browser_download_url": "https://github.com/laralbir/img2asciiIM/releases/download/v1.1.0/checksum.sha256"
          }
        ]
      }
    """.trimIndent()

    val release = checker.parseReleaseJson(sampleJson, currentVersionName = "1.0.0")

    assertEquals("v1.1.0", release.tagName)
    assertTrue(release.isNewerVersion)
    assertEquals(
      "https://github.com/laralbir/img2asciiIM/releases/download/v1.1.0/img2asciiIM-v1.1.0.apk",
      release.apkDownloadUrl,
    )
    assertTrue(release.releaseNotes.contains("Soporte para enlaces"))
  }

  @Test
  fun detectsWhenCurrentVersionIsAlreadyLatestOrNewer() {
    val sampleJson = """
      {
        "tag_name": "v1.0.0",
        "html_url": "https://github.com/laralbir/img2asciiIM/releases/tag/v1.0.0",
        "body": "Initial release",
        "assets": []
      }
    """.trimIndent()

    val release = checker.parseReleaseJson(sampleJson, currentVersionName = "1.0.0")
    assertFalse(release.isNewerVersion)

    val olderRelease = checker.parseReleaseJson(sampleJson, currentVersionName = "1.1.0")
    assertFalse(olderRelease.isNewerVersion)
  }

  @Test
  fun comparesSemanticVersionsAccurately() {
    assertTrue(checker.isNewer(remote = "v1.0.1", local = "1.0.0"))
    assertTrue(checker.isNewer(remote = "v1.2.0", local = "1.1.9"))
    assertTrue(checker.isNewer(remote = "v2.0.0", local = "1.9.9"))
    assertFalse(checker.isNewer(remote = "v1.0.0", local = "1.0.0"))
    assertFalse(checker.isNewer(remote = "v1.0.0", local = "1.0.1"))
    assertFalse(checker.isNewer(remote = "v0.9.0", local = "1.0.0"))
  }
}
