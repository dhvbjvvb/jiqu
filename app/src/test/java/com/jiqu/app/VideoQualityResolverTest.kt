package com.jiqu.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoQualityResolverTest {
    @Test
    fun createsAQualityLabelFromLandscapeVideoDimensions() {
        assertEquals("1080P", videoQualityLabel(width = 1920, height = 1080))
    }

    @Test
    fun createsAQualityLabelFromPortraitVideoDimensions() {
        assertEquals("720P", videoQualityLabel(width = 720, height = 1280))
    }

    @Test
    fun returnsNoLabelWhenTheDimensionsAreUnavailable() {
        assertNull(videoQualityLabel(width = null, height = null))
    }

    @Test
    fun formatsResolvedOriginalVideoMetadata() {
        val originalDownload = ParsedDownload(
            label = ORIGINAL_METADATA_PENDING_LABEL,
            url = "https://example.com/original.mp4",
            isOriginal = true
        )

        val resolvedDownload = applyVideoTechnicalMetadata(
            originalDownload,
            VideoTechnicalMetadata(
                width = 1280,
                height = 720,
                codec = "hvc1.1.6.L93.B0",
                bitRate = 656_457,
                frameRate = 30f,
                hasAudio = true
            )
        )

        assertEquals("720P · H.265 · 30fps · 0.7Mbps", resolvedDownload.label)
        assertEquals(true, resolvedDownload.isOriginal)
        assertEquals(true, resolvedDownload.hasAudio)
    }

    @Test
    fun keepsOriginalPendingLabelWhenMetadataIsUnavailable() {
        val download = ParsedDownload(
            label = ORIGINAL_METADATA_PENDING_LABEL,
            url = "https://example.com/original.mp4",
            isOriginal = true
        )

        assertEquals(ORIGINAL_METADATA_PENDING_LABEL, videoDownloadLabel(download))
    }

    @Test
    fun preservesOriginalIdentityWhenAnUnstructuredDuplicateArrives() {
        val originalDownload = ParsedDownload(
            label = ORIGINAL_METADATA_PENDING_LABEL,
            url = "https://example.com/original.mp4",
            isOriginal = true
        )

        val mergedDownload = mergeVideoDownload(
            existingDownload = originalDownload,
            incomingDownload = ParsedDownload(VIDEO_METADATA_PENDING_LABEL, originalDownload.url)
        )

        assertEquals(true, mergedDownload.isOriginal)
        assertEquals(ORIGINAL_METADATA_PENDING_LABEL, mergedDownload.label)
    }

    @Test
    fun normalizesCommonCodecIdentifiers() {
        assertEquals("H.264", normalizeVideoCodec("avc1.640028"))
        assertEquals("H.265", normalizeVideoCodec("hevc"))
        assertEquals("AV1", normalizeVideoCodec("av01.0.08M.08"))
        assertEquals("VP9", normalizeVideoCodec("vp09.00.51.08"))
    }

    @Test
    fun formatsDecimalFrameRateAndBitRate() {
        val download = ParsedDownload(
            label = "video",
            url = "https://example.com/video.mp4",
            width = 1920,
            height = 1080,
            frameRate = 29.97f,
            bitRate = 4_250_000
        )

        assertEquals("1080P · 29.97fps · 4.3Mbps", videoDownloadLabel(download))
    }

    @Test
    fun prefersAnAudioH264DownloadForPreview() {
        val h265 = ParsedDownload("1920p · H265", "https://example.com/h265.mp4", codec = "h265", hasAudio = true)
        val h264 = ParsedDownload("1920p · H264", "https://example.com/h264.mp4", codec = "h264", hasAudio = true)

        assertEquals(h264.url, selectPreviewVideoUrl(null, listOf(h265, h264)))
    }

    @Test
    fun keepsTheOriginalPreviewBeforeTryingFallbacks() {
        val original = ParsedDownload(
            ORIGINAL_METADATA_PENDING_LABEL,
            "https://example.com/original.mp4",
            isOriginal = true
        )
        val h264 = ParsedDownload("720P · H.264", "https://example.com/h264.mp4", codec = "h264")

        assertEquals(original.url, selectPreviewVideoUrl(original.url, listOf(original, h264)))
    }

    @Test
    fun doesNotChooseKnownVideoOnlyDownloadForPreview() {
        val videoOnly = ParsedDownload("2560p · H265", "https://example.com/video-only.mp4", codec = "h265", hasAudio = false)
        val fallback = "https://example.com/fallback.mp4"

        assertEquals(fallback, selectPreviewVideoUrl(fallback, listOf(videoOnly)))
    }
}
