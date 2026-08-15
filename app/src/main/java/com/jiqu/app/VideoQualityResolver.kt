package com.jiqu.app

import java.util.Locale
import kotlin.math.roundToInt

internal data class VideoTechnicalMetadata(
    val width: Int? = null,
    val height: Int? = null,
    val codec: String? = null,
    val bitRate: Long? = null,
    val frameRate: Float? = null,
    val hasAudio: Boolean? = null
)

internal fun applyVideoTechnicalMetadata(
    download: ParsedDownload,
    metadata: VideoTechnicalMetadata
): ParsedDownload {
    val updatedDownload = download.copy(
        width = metadata.width ?: download.width,
        height = metadata.height ?: download.height,
        codec = metadata.codec ?: download.codec,
        bitRate = metadata.bitRate ?: download.bitRate,
        frameRate = metadata.frameRate ?: download.frameRate,
        hasAudio = metadata.hasAudio ?: download.hasAudio
    )
    return updatedDownload.copy(label = videoDownloadLabel(updatedDownload, download.label))
}

internal fun mergeVideoDownload(
    existingDownload: ParsedDownload?,
    incomingDownload: ParsedDownload
): ParsedDownload {
    val mergedDownload = incomingDownload.copy(
        width = incomingDownload.width ?: existingDownload?.width,
        height = incomingDownload.height ?: existingDownload?.height,
        codec = incomingDownload.codec ?: existingDownload?.codec,
        bitRate = incomingDownload.bitRate ?: existingDownload?.bitRate,
        hasAudio = incomingDownload.hasAudio ?: existingDownload?.hasAudio,
        frameRate = incomingDownload.frameRate ?: existingDownload?.frameRate,
        isOriginal = incomingDownload.isOriginal || existingDownload?.isOriginal == true
    )
    val fallbackLabel = incomingDownload.label.takeUnless { it == VIDEO_METADATA_PENDING_LABEL }
        ?: existingDownload?.label
        ?: incomingDownload.label
    return mergedDownload.copy(label = videoDownloadLabel(mergedDownload, fallbackLabel))
}

internal fun selectPreviewVideoUrl(
    currentPreviewUrl: String?,
    downloads: List<ParsedDownload>
): String? {
    val playableDownloads = downloads.filter { it.hasAudio != false }
    return currentPreviewUrl?.takeIf { currentUrl ->
        downloads.none { it.url == currentUrl && it.hasAudio == false }
    }
        ?: playableDownloads.firstOrNull { normalizeVideoCodec(it.codec) == "H.264" }?.url
        ?: playableDownloads.firstOrNull()?.url
        ?: currentPreviewUrl
}

internal fun videoQualityLabel(width: Int?, height: Int?): String? {
    val shorterSide = listOfNotNull(width, height).minOrNull()?.takeIf { it > 0 } ?: return null
    return "${shorterSide}P"
}

internal fun videoDownloadLabel(download: ParsedDownload, fallbackLabel: String = download.label): String {
    val fallbackResolution = QUALITY_RESOLUTION_PATTERN.find(fallbackLabel)
        ?.groupValues
        ?.getOrNull(1)
        ?.let { "${it}P" }
    val resolution = videoQualityLabel(download.width, download.height) ?: fallbackResolution
    val codec = normalizeVideoCodec(download.codec)
    val frameRate = download.frameRate?.takeIf { it.isFinite() && it > 0f }?.let(::formatFrameRate)
    val bitRate = download.bitRate?.takeIf { it > 0L }?.let(::formatVideoBitRate)
    val technicalLabel = listOfNotNull(resolution, codec, frameRate, bitRate).joinToString(" · ")
    return technicalLabel.ifBlank {
        if (download.isOriginal) ORIGINAL_METADATA_PENDING_LABEL else fallbackLabel
    }
}

internal fun normalizeVideoCodec(codec: String?): String? {
    val normalizedCodec = codec.orEmpty().trim().lowercase(Locale.US)
    return when {
        normalizedCodec.isBlank() -> null
        normalizedCodec.contains("avc1") || normalizedCodec.contains("avc") ||
            normalizedCodec.contains("h264") -> "H.264"
        normalizedCodec.contains("hev1") || normalizedCodec.contains("hvc1") ||
            normalizedCodec.contains("hevc") || normalizedCodec.contains("h265") -> "H.265"
        normalizedCodec.contains("av01") || normalizedCodec.contains("av1") -> "AV1"
        normalizedCodec.contains("vp09") || normalizedCodec.contains("vp9") -> "VP9"
        else -> codec?.trim()?.uppercase(Locale.US)
    }
}

internal fun formatVideoBitRate(bitsPerSecond: Long): String =
    (bitsPerSecond / BITS_PER_MEGABIT).let { megabitsPerSecond ->
        if (megabitsPerSecond < 0.1f) {
            "%.2fMbps".format(Locale.US, megabitsPerSecond)
        } else {
            "%.1fMbps".format(Locale.US, megabitsPerSecond)
        }
    }

private fun formatFrameRate(frameRate: Float): String {
    val roundedFrameRate = frameRate.roundToInt()
    return if (kotlin.math.abs(frameRate - roundedFrameRate) < FRAME_RATE_INTEGER_TOLERANCE) {
        "${roundedFrameRate}fps"
    } else {
        "%.2ffps".format(Locale.US, frameRate)
    }
}

internal const val ORIGINAL_METADATA_PENDING_LABEL = "原始视频 · 信息获取中"
internal const val VIDEO_METADATA_PENDING_LABEL = "视频 · 信息获取中"
private val QUALITY_RESOLUTION_PATTERN = Regex("(\\d{3,4})p", RegexOption.IGNORE_CASE)
private const val BITS_PER_MEGABIT = 1_000_000f
private const val FRAME_RATE_INTEGER_TOLERANCE = 0.01f
