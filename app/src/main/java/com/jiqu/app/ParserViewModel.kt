package com.jiqu.app

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.core.content.edit
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jiqu.app.ui.theme.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class ParseHistoryEntry(
    val sourceUrl: String,
    val title: String,
    val platformName: String,
    val mediaType: String,
    val parsedAt: String,
    val coverUrl: String?,
    val previewUrl: String?
)

internal fun replaceHistoryEntry(
    history: List<ParseHistoryEntry>,
    latestEntry: ParseHistoryEntry,
    maximumEntries: Int
): List<ParseHistoryEntry> =
    (listOf(latestEntry) + history.filterNot { it.sourceUrl == latestEntry.sourceUrl })
        .take(maximumEntries)

internal fun historySourceUrls(history: List<ParseHistoryEntry>): Set<String> =
    history.mapTo(linkedSetOf(), ParseHistoryEntry::sourceUrl)

internal fun areAllHistoryUrlsSelected(
    history: List<ParseHistoryEntry>,
    selectedUrls: Set<String>
): Boolean = history.isNotEmpty() && selectedUrls == historySourceUrls(history)

internal enum class DownloadTaskStatus {
    DOWNLOADING,
    CANCELLING,
    COMPLETED,
    FAILED,
    CANCELLED
}

internal data class DownloadTaskUiState(
    val title: String,
    val qualityLabel: String,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = -1,
    val bytesPerSecond: Long = 0,
    val status: DownloadTaskStatus = DownloadTaskStatus.DOWNLOADING
)

internal class ParserViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val apiClient = BugPkApiClient()

    var linkText by mutableStateOf("")
        private set
    var result by mutableStateOf<ParseResult?>(null)
        private set
    var isParsing by mutableStateOf(false)
        private set
    var autoDetect by mutableStateOf(preferences.getBoolean(AUTO_DETECT_KEY, true))
        private set
    var autoPasteLinks by mutableStateOf(preferences.getBoolean(AUTO_PASTE_LINKS_KEY, true))
        private set
    var themeMode by mutableStateOf(ThemeMode.fromPreference(preferences.getString(THEME_MODE_KEY, null)))
        private set
    var history by mutableStateOf(loadHistory())
        private set
    var downloadTask by mutableStateOf<DownloadTaskUiState?>(null)
        private set
    private var activeDownloadJob: Job? = null
    private var downloadCancellationController: DownloadCancellationController? = null

    fun updateLinkText(value: String) {
        linkText = if (autoDetect) PlatformDetector.extractUrl(value) ?: value else value
        result = null
    }

    fun parseCurrent(): Boolean {
        if (linkText.isBlank() || isParsing) return false
        val sharedText = linkText
        isParsing = true
        result = null
        viewModelScope.launch {
            val parseResult = withContext(Dispatchers.IO) { apiClient.parse(sharedText) }
            completeParsing(parseResult)
            if (parseResult is ParseResult.Success) {
                val resolvedDownloads = withContext(Dispatchers.IO) {
                    resolveVideoQualityLabels(parseResult.media.videoDownloads)
                }
                if (result === parseResult) {
                    result = ParseResult.Success(parseResult.media.copy(videoDownloads = resolvedDownloads))
                }
            }
        }
        return true
    }

    fun reparse(entry: ParseHistoryEntry): Boolean {
        linkText = entry.sourceUrl
        return parseCurrent()
    }

    fun deleteHistory(entries: Set<String>) {
        history = history.filterNot { it.sourceUrl in entries }
        saveHistory(history)
    }

    private fun completeParsing(parseResult: ParseResult) {
        isParsing = false
        result = parseResult
        if (parseResult is ParseResult.Success) addHistory(parseResult.media)
    }

    fun updateAutoDetection(enabled: Boolean) {
        autoDetect = enabled
        preferences.edit { putBoolean(AUTO_DETECT_KEY, enabled) }
        if (enabled) PlatformDetector.extractUrl(linkText)?.let { linkText = it }
    }

    fun updateAutoPasteLinks(enabled: Boolean) {
        autoPasteLinks = enabled
        preferences.edit { putBoolean(AUTO_PASTE_LINKS_KEY, enabled) }
    }

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        preferences.edit { putString(THEME_MODE_KEY, mode.preferenceValue) }
    }

    fun pasteClipboardLinkIfEligible(clipboardText: String?): Boolean {
        if (!autoPasteLinks || linkText.isNotBlank() || isParsing) return false
        val supportedUrl = PlatformDetector.extractUrl(clipboardText.orEmpty()) ?: return false
        linkText = supportedUrl
        return parseCurrent()
    }

    fun startDownload(title: String, download: ParsedDownload): Boolean {
        return startDownloads(title, listOf(download))
    }

    fun startDownloads(
        title: String,
        downloads: List<ParsedDownload>,
        mediaSequenceNumbers: Map<String, Int> = emptyMap()
    ): Boolean {
        val validDownloads = downloads.distinctBy(ParsedDownload::url).filter { isHttpDownloadUrl(it.url) }
        if (activeDownloadJob?.isActive == true || validDownloads.isEmpty()) return false

        val application = getApplication<Application>()
        val cancellationController = DownloadCancellationController()

        downloadCancellationController = cancellationController
        downloadTask = DownloadTaskUiState(
            title = title,
            qualityLabel = validDownloads.first().batchLabel(0, validDownloads.size)
        )
        activeDownloadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                validDownloads.forEachIndexed { index, download ->
                    downloadSingleMedia(
                        application = application,
                        title = title,
                        download = download,
                        sequenceNumber = sequenceNumberForDownload(
                            downloadUrl = download.url,
                            mediaSequenceNumbers = mediaSequenceNumbers,
                            batchIndex = index,
                            batchSize = validDownloads.size
                        ),
                        batchIndex = index,
                        batchSize = validDownloads.size,
                        cancellationController = cancellationController
                    )
                }
                publishDownloadState(DownloadTaskStatus.COMPLETED, bytesPerSecond = 0)
            } catch (_: CancellationException) {
                publishDownloadState(DownloadTaskStatus.CANCELLED)
            } catch (_: Exception) {
                publishDownloadState(DownloadTaskStatus.FAILED)
            } finally {
                downloadCancellationController = null
            }
        }
        return true
    }

    private suspend fun downloadSingleMedia(
        application: Application,
        title: String,
        download: ParsedDownload,
        sequenceNumber: Int?,
        batchIndex: Int,
        batchSize: Int,
        cancellationController: DownloadCancellationController
    ) {
        val startedAtMillis = System.currentTimeMillis()
        var lastProgressBytes = 0L
        var lastProgressAtMillis = startedAtMillis
        publishDownloadState(
            status = DownloadTaskStatus.DOWNLOADING,
            downloadedBytes = 0,
            totalBytes = -1,
            bytesPerSecond = 0,
            qualityLabel = download.batchLabel(batchIndex, batchSize)
        )
        downloadWithMediaStore(
            context = application,
            title = title,
            fileName = buildDownloadFileName(title, download.label, download.url, sequenceNumber),
            relativePath = downloadRelativePathFor(download),
            sourceUrl = download.url,
            cancellationController = cancellationController
        ) { downloadedBytes, totalBytes ->
            val now = System.currentTimeMillis()
            val elapsedMillis = (now - lastProgressAtMillis).coerceAtLeast(1)
            val bytesPerSecond = ((downloadedBytes - lastProgressBytes) * 1_000 / elapsedMillis).coerceAtLeast(0)
            lastProgressBytes = downloadedBytes
            lastProgressAtMillis = now
            publishDownloadState(
                DownloadTaskStatus.DOWNLOADING,
                downloadedBytes,
                totalBytes,
                bytesPerSecond
            )
        }
    }

    fun cancelDownload() {
        val currentTask = downloadTask ?: return
        if (currentTask.status != DownloadTaskStatus.DOWNLOADING) return
        downloadTask = currentTask.copy(status = DownloadTaskStatus.CANCELLING)
        downloadCancellationController?.cancel()
        activeDownloadJob?.cancel()
    }

    fun dismissDownloadTask() {
        if (downloadTask?.status != DownloadTaskStatus.DOWNLOADING &&
            downloadTask?.status != DownloadTaskStatus.CANCELLING
        ) {
            downloadTask = null
        }
    }

    private fun publishDownloadState(
        status: DownloadTaskStatus,
        downloadedBytes: Long? = null,
        totalBytes: Long? = null,
        bytesPerSecond: Long? = null,
        qualityLabel: String? = null
    ) {
        val updateState = updateState@{
            val currentTask = downloadTask ?: return@updateState
            val updatedTask = currentTask.copy(
                downloadedBytes = downloadedBytes ?: currentTask.downloadedBytes,
                totalBytes = totalBytes ?: currentTask.totalBytes,
                bytesPerSecond = bytesPerSecond ?: currentTask.bytesPerSecond,
                qualityLabel = qualityLabel ?: currentTask.qualityLabel,
                status = status
            )
            downloadTask = if (status == DownloadTaskStatus.COMPLETED && updatedTask.totalBytes > 0) {
                updatedTask.copy(
                    downloadedBytes = updatedTask.totalBytes,
                    bytesPerSecond = 0
                )
            } else {
                updatedTask
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            updateState()
        } else {
            runBlocking(Dispatchers.Main.immediate) { updateState() }
        }
    }

    override fun onCleared() {
        downloadCancellationController?.cancel()
        activeDownloadJob?.cancel()
    }

    private fun ParsedDownload.batchLabel(index: Int, total: Int): String =
        if (total > 1) "$label (${index + 1}/$total)" else label

    private fun addHistory(media: ParsedMedia) {
        val entry = ParseHistoryEntry(
            sourceUrl = media.sourceUrl,
            title = media.title,
            platformName = media.platformName,
            mediaType = media.mediaType,
            parsedAt = formatHistoryTimestamp(),
            coverUrl = media.coverUrl,
            previewUrl = media.previewUrl
        )
        history = replaceHistoryEntry(history, entry, MAX_HISTORY_ENTRIES)
        saveHistory(history)
    }

    private fun loadHistory(): List<ParseHistoryEntry> = runCatching {
        val array = JSONArray(preferences.getString(HISTORY_KEY, "[]"))
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            ParseHistoryEntry(
                sourceUrl = item.optString("sourceUrl"),
                title = item.optString("title", "未命名媒体"),
                platformName = item.optString("platformName", "未知平台"),
                mediaType = item.optString("mediaType", "视频"),
                parsedAt = item.optString("parsedAt", ""),
                coverUrl = item.optString("coverUrl").ifBlank { null },
                previewUrl = item.optString("previewUrl").ifBlank { null }
            )
        }.filter { it.sourceUrl.isNotBlank() }
    }.getOrDefault(emptyList())

    private fun saveHistory(entries: List<ParseHistoryEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("sourceUrl", entry.sourceUrl)
                    .put("title", entry.title)
                    .put("platformName", entry.platformName)
                    .put("mediaType", entry.mediaType)
                    .put("parsedAt", entry.parsedAt)
                    .put("coverUrl", entry.coverUrl)
                    .put("previewUrl", entry.previewUrl)
            )
        }
        preferences.edit { putString(HISTORY_KEY, array.toString()) }
    }

    private fun formatHistoryTimestamp(): String =
        SimpleDateFormat(HISTORY_TIMESTAMP_PATTERN, Locale.getDefault()).format(Date())

    private companion object {
        const val PREFERENCES_NAME = "parser_preferences"
        const val AUTO_DETECT_KEY = "auto_detect"
        const val AUTO_PASTE_LINKS_KEY = "auto_paste_links"
        const val THEME_MODE_KEY = "theme_mode"
        const val HISTORY_KEY = "parse_history"
        const val MAX_HISTORY_ENTRIES = 50
        const val HISTORY_TIMESTAMP_PATTERN = "MM-dd HH:mm"
    }
}
