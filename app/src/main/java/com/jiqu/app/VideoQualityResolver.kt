package com.jiqu.app

import android.media.MediaMetadataRetriever

internal fun resolveVideoQualityLabels(downloads: List<ParsedDownload>): List<ParsedDownload> =
    downloads.map { download ->
        if (download.label == ORIGINAL_QUALITY_LABEL) {
            resolveVideoQualityLabel(download.url)?.let { label -> download.copy(label = label) } ?: download
        } else {
            download
        }
    }

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
