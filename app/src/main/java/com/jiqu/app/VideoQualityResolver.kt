package com.jiqu.app

import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

internal fun resolveVideoQualityLabels(downloads: List<ParsedDownload>): List<ParsedDownload> =
    downloads.map { download ->
        if (download.label == ORIGINAL_QUALITY_LABEL) {
            resolveVideoQualityLabel(download.url)?.let { label -> download.copy(label = label) } ?: download
        } else {
            download
        }
    }

internal suspend fun resolveVideoDownloads(downloads: List<ParsedDownload>): List<ParsedDownload> =
    coroutineScope {
        val resolvedDownloads = downloads.map { download ->
            async(Dispatchers.IO) {
                withTimeoutOrNull(METADATA_PROBE_TIMEOUT_MILLIS) { resolveVideoDownload(download) } ?: download
            }
        }.awaitAll()
        val audioDownloads = resolvedDownloads.filter { it.hasAudio == true }
        if (audioDownloads.isNotEmpty()) audioDownloads else resolvedDownloads
    }

internal fun selectPreviewVideoUrl(
    currentPreviewUrl: String?,
    downloads: List<ParsedDownload>
): String? {
    val playableDownloads = downloads.filter { it.hasAudio != false }
    return playableDownloads.firstOrNull { it.codec.equals("h264", ignoreCase = true) }?.url
        ?: playableDownloads.firstOrNull()?.url
        ?: currentPreviewUrl
}

private fun resolveVideoDownload(download: ParsedDownload): ParsedDownload {
    val metadata = runCatching {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(download.url, VIDEO_METADATA_HEADERS)
            VideoMetadata(
                width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull(),
                height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull(),
                hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
                    ?.let { it == "yes" || it == "1" }
            )
        }
    }.getOrNull() ?: return download

    return download.copy(
        label = if (download.label == ORIGINAL_QUALITY_LABEL) {
            videoQualityLabel(metadata.width, metadata.height) ?: download.label
        } else download.label,
        width = download.width ?: metadata.width,
        height = download.height ?: metadata.height,
        hasAudio = download.hasAudio ?: metadata.hasAudio,
        bitRate = download.bitRate
    )
}

private data class VideoMetadata(
    val width: Int?,
    val height: Int?,
    val hasAudio: Boolean?
)

internal fun videoQualityLabel(width: Int?, height: Int?): String? {
    val shorterSide = listOfNotNull(width, height).minOrNull()?.takeIf { it > 0 } ?: return null
    return "${shorterSide}P"
}

private fun resolveVideoQualityLabel(url: String): String? = runCatching {
    MediaMetadataRetriever().use { retriever ->
        retriever.setDataSource(url, VIDEO_METADATA_HEADERS)
        videoQualityLabel(
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull(),
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
        )
    }
}.getOrNull()

private val VIDEO_METADATA_HEADERS = mapOf(
    "User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36"
)

private const val METADATA_PROBE_TIMEOUT_MILLIS = 4_000L
