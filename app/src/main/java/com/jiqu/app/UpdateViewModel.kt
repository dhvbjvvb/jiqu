package com.jiqu.app

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets

internal data class AppUpdate(
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val assetSizeBytes: Long
)

internal sealed interface UpdateCheckResult {
    data class Available(val update: AppUpdate) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data object Failed : UpdateCheckResult
}

private const val GITHUB_HOST = "github.com"
private const val RELEASE_DOWNLOAD_PATH_PREFIX = "/dhvbjvvb/jiqu/releases/download/"
private const val RELEASE_APK_NAME = "app-release.apk"

internal fun isRemoteVersionNewer(remoteVersion: String, currentVersion: String): Boolean {
    val remoteParts = remoteVersion.toVersionParts() ?: return false
    val currentParts = currentVersion.toVersionParts() ?: return false
    val partCount = maxOf(remoteParts.size, currentParts.size)

    repeat(partCount) { index ->
        val remotePart = remoteParts.getOrElse(index) { 0 }
        val currentPart = currentParts.getOrElse(index) { 0 }
        if (remotePart != currentPart) return remotePart > currentPart
    }
    return false
}

internal fun isTrustedGitHubAssetUrl(url: String): Boolean = runCatching {
    val uri = URI(url)
    uri.scheme.equals("https", ignoreCase = true) &&
        uri.host.equals(GITHUB_HOST, ignoreCase = true) &&
        uri.path.startsWith(RELEASE_DOWNLOAD_PATH_PREFIX) &&
        uri.path.endsWith("/$RELEASE_APK_NAME")
}.getOrDefault(false)

private fun String.toVersionParts(): List<Int>? {
    val normalized = trim().removePrefix("v").removePrefix("V")
    if (normalized.isBlank()) return null

    return normalized.split('.').map { part ->
        part.toIntOrNull()?.takeIf { it >= 0 } ?: return null
    }
}

internal class GitHubReleaseUpdateClient {
    fun checkForUpdate(currentVersion: String): UpdateCheckResult = runCatching {
        val root = JSONObject(requestLatestRelease())
        if (root.optBoolean("draft") || root.optBoolean("prerelease")) return UpdateCheckResult.UpToDate

        val remoteVersion = root.optString("tag_name")
        if (!isRemoteVersionNewer(remoteVersion, currentVersion)) return UpdateCheckResult.UpToDate

        val asset = root.optJSONArray("assets")
            ?.findApkAsset()
            ?: return UpdateCheckResult.Failed
        val downloadUrl = asset.optString("browser_download_url")
        if (!isTrustedGitHubAssetUrl(downloadUrl)) return UpdateCheckResult.Failed

        UpdateCheckResult.Available(
            AppUpdate(
                versionName = remoteVersion.removePrefix("v").removePrefix("V"),
                releaseNotes = root.optString("body").trim(),
                downloadUrl = downloadUrl,
                assetSizeBytes = asset.optLong("size").coerceAtLeast(0)
            )
        )
    }.getOrDefault(UpdateCheckResult.Failed)

    private fun requestLatestRelease(): String {
        val connection = (java.net.URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Jiqu-Android")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }
        return try {
            if (connection.responseCode !in 200..299) throw IllegalStateException("GitHub release request failed")
            connection.inputStream.readUtf8()
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONArray.findApkAsset(): JSONObject? =
        (0 until length()).asSequence()
            .map(::optJSONObject)
            .firstOrNull { asset -> asset?.optString("name") == RELEASE_APK_NAME }

    private fun InputStream.readUtf8(): String =
        BufferedReader(InputStreamReader(this, StandardCharsets.UTF_8)).use { it.readText() }

    private companion object {
        const val LATEST_RELEASE_URL = "https://api.github.com/repos/dhvbjvvb/jiqu/releases/latest"
        const val CONNECT_TIMEOUT_MILLIS = 10_000
        const val READ_TIMEOUT_MILLIS = 15_000
    }
}

internal class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val updateClient = GitHubReleaseUpdateClient()

    var availableUpdate by mutableStateOf<AppUpdate?>(null)
        private set
    var isChecking by mutableStateOf(false)
        private set
    var checkMessage by mutableStateOf<String?>(null)
        private set

    init {
        checkForUpdate(force = false)
    }

    fun checkForUpdate(force: Boolean) {
        if (isChecking || (!force && !shouldCheckAutomatically())) return

        isChecking = true
        checkMessage = null
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { updateClient.checkForUpdate(BuildConfig.VERSION_NAME) }
            isChecking = false
            when (result) {
                is UpdateCheckResult.Available -> {
                    recordSuccessfulCheck()
                    availableUpdate = result.update
                }
                UpdateCheckResult.UpToDate -> {
                    recordSuccessfulCheck()
                    if (force) checkMessage = "当前已是最新版本"
                }
                UpdateCheckResult.Failed -> if (force) checkMessage = "检查更新失败，请稍后重试"
            }
        }
    }

    fun dismissUpdate() {
        availableUpdate = null
    }

    fun consumeCheckMessage() {
        checkMessage = null
    }

    private fun shouldCheckAutomatically(): Boolean =
        System.currentTimeMillis() - preferences.getLong(LAST_CHECKED_AT_KEY, 0) >= AUTO_CHECK_INTERVAL_MILLIS

    private fun recordSuccessfulCheck() {
        preferences.edit { putLong(LAST_CHECKED_AT_KEY, System.currentTimeMillis()) }
    }

    private companion object {
        const val PREFERENCES_NAME = "update_preferences"
        const val LAST_CHECKED_AT_KEY = "last_checked_at"
        const val AUTO_CHECK_INTERVAL_MILLIS = 6 * 60 * 60 * 1_000L
    }
}
