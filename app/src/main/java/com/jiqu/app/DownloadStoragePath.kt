package com.jiqu.app

import java.net.URI

internal const val DOWNLOADS_PICTURE_RELATIVE_PATH = "Download/Jiqu/Picture"
internal const val DOWNLOADS_MUSIC_RELATIVE_PATH = "Download/Jiqu/Music"
internal const val DOWNLOADS_VIDEO_RELATIVE_PATH = "Download/Jiqu/video"

internal fun downloadRelativePathFor(download: ParsedDownload): String =
    when {
        download.label.contains("音乐") -> DOWNLOADS_MUSIC_RELATIVE_PATH
        download.label.contains("图片") && !isAnimatedImageUrl(download.url) -> DOWNLOADS_PICTURE_RELATIVE_PATH
        else -> DOWNLOADS_VIDEO_RELATIVE_PATH
    }

private fun isAnimatedImageUrl(url: String): Boolean = runCatching {
    val uri = URI(url)
    val path = uri.path.orEmpty().lowercase()
    val query = uri.query.orEmpty().lowercase()
    path.endsWith(".gif") || path.endsWith(".webp") || query.contains("format=gif")
}.getOrDefault(false)
