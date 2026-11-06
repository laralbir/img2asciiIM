package com.laralbir.img2asciiim.data.update

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class AppReleaseInfo(
  val tagName: String,
  val releaseNotes: String,
  val apkDownloadUrl: String?,
  val htmlUrl: String,
  val isNewerVersion: Boolean,
)

sealed interface UpdateCheckResult {
  data class UpdateAvailable(val releaseInfo: AppReleaseInfo) : UpdateCheckResult
  data class UpToDate(val currentVersion: String) : UpdateCheckResult
  data class OfflineOrError(val message: String) : UpdateCheckResult
}

/**
 * In-app update checker querying GitHub Releases API (`RF-08`).
 *
 * Operates safely without crashing when offline, in airplane mode, or under captive Wi-Fi.
 */
class GitHubUpdateChecker(
  private val endpointUrl: String = DEFAULT_ENDPOINT,
  private val connectTimeoutMs: Int = 4000,
  private val readTimeoutMs: Int = 4000,
) {

  fun parseReleaseJson(jsonString: String, currentVersionName: String): AppReleaseInfo {
    val json = JSONObject(jsonString)
    val tagName = json.optString("tag_name", "")
    val htmlUrl = json.optString("html_url", "")
    val body = json.optString("body", "")
    val assets = json.optJSONArray("assets") ?: JSONArray()

    var apkUrl: String? = null
    for (i in 0 until assets.length()) {
      val asset = assets.optJSONObject(i) ?: continue
      val name = asset.optString("name", "")
      val downloadUrl = asset.optString("browser_download_url", "")
      if (name.endsWith(".apk", ignoreCase = true) && downloadUrl.isNotBlank()) {
        apkUrl = downloadUrl
        break
      }
    }

    val isNewer = isNewer(remote = tagName, local = currentVersionName)
    return AppReleaseInfo(
      tagName = tagName,
      releaseNotes = body,
      apkDownloadUrl = apkUrl,
      htmlUrl = htmlUrl,
      isNewerVersion = isNewer,
    )
  }

  fun isNewer(remote: String, local: String): Boolean {
    val rParts = remote.removePrefix("v").removePrefix("V").split('.').mapNotNull { it.toIntOrNull() }
    val lParts = local.removePrefix("v").removePrefix("V").split('.').mapNotNull { it.toIntOrNull() }

    val maxLen = maxOf(rParts.size, lParts.size)
    for (i in 0 until maxLen) {
      val r = rParts.getOrElse(i) { 0 }
      val l = lParts.getOrElse(i) { 0 }
      if (r > l) return true
      if (r < l) return false
    }
    return false
  }

  fun check(currentVersionName: String): UpdateCheckResult {
    return try {
      val url = URL(endpointUrl)
      val conn = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = connectTimeoutMs
        readTimeout = readTimeoutMs
        setRequestProperty("Accept", "application/vnd.github.v3+json")
        setRequestProperty("User-Agent", "img2asciiIM-Android-App")
      }

      val responseCode = conn.responseCode
      if (responseCode !in 200..299) {
        return UpdateCheckResult.OfflineOrError("Respuesta de servidor HTTP $responseCode")
      }

      val content = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
      val release = parseReleaseJson(content, currentVersionName)

      if (release.isNewerVersion) {
        UpdateCheckResult.UpdateAvailable(release)
      } else {
        UpdateCheckResult.UpToDate(currentVersionName)
      }
    } catch (_: Exception) {
      UpdateCheckResult.OfflineOrError("No se pudo comprobar actualizaciones (sin conexión o modo avión)")
    }
  }

  companion object {
    const val DEFAULT_ENDPOINT = "https://api.github.com/repos/laralbir/img2asciiIM/releases/latest"
  }
}
