package com.jiqu.app

import java.net.URI

internal fun buildDownloadFileName(
    title: String,
    label: String,
    sourceUrl: String,
    sequenceNumber: Int? = null
): String {
    val safeTitle = title
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .trim()
        .take(MAX_FILE_TITLE_LENGTH)
        .ifBlank { "media" }
    val extension = runCatching {
        URI(sourceUrl).path
            ?.substringAfterLast('.', "")
            ?.lowercase()
            ?.takeIf { it.matches(EXTENSION_PATTERN) }
    }.getOrNull() ?: defaultExtension(label)

    val suffix = sequenceNumber?.let { "_${it + 1}" }.orEmpty()
    return "$safeTitle$suffix.$extension"
}

internal fun isHttpDownloadUrl(url: String): Boolean = runCatching {
    URI(url).scheme?.lowercase() in setOf("http", "https")
}.getOrDefault(false)

private fun defaultExtension(label: String): String = when {
    label.contains("音乐") -> "mp3"
    label.contains("图片") -> "jpg"
    else -> "mp4"
}

private const val MAX_FILE_TITLE_LENGTH = 80
private val EXTENSION_PATTERN = Regex("[a-z0-9]{1,5}")
