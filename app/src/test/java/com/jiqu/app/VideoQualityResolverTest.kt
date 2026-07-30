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
    fun keepsTheApiProvidedQualityLabelWithoutReadingTheVideoMetadata() {
        val download = ParsedDownload("1080P", "https://example.com/video.mp4")

        assertEquals(listOf(download), resolveVideoQualityLabels(listOf(download)))
    }

    @Test
    fun prefersAnAudioH264DownloadForPreview() {
        val h265 = ParsedDownload("1920p · H265", "https://example.com/h265.mp4", codec = "h265", hasAudio = true)
        val h264 = ParsedDownload("1920p · H264", "https://example.com/h264.mp4", codec = "h264", hasAudio = true)

        assertEquals(h264.url, selectPreviewVideoUrl(null, listOf(h265, h264)))
    }

    @Test
    fun doesNotChooseKnownVideoOnlyDownloadForPreview() {
        val videoOnly = ParsedDownload("2560p · H265", "https://example.com/video-only.mp4", codec = "h265", hasAudio = false)
        val fallback = "https://example.com/fallback.mp4"

        assertEquals(fallback, selectPreviewVideoUrl(fallback, listOf(videoOnly)))
    }
}
