package com.jiqu.app

import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewPlayerTest {
    @Test
    fun ordersPreferredThenH264ThenRemainingPreviewCandidates() {
        val preferredUrl = "https://example.com/original.mp4"
        val h265Url = "https://example.com/h265.mp4"
        val h264Url = "https://example.com/h264.mp4"
        val videoOnlyUrl = "https://example.com/video-only.mp4"

        assertEquals(
            listOf(preferredUrl, h264Url, h265Url),
            previewCandidateUrls(
                preferredUrl = preferredUrl,
                downloads = listOf(
                    ParsedDownload("Original", preferredUrl, isOriginal = true),
                    ParsedDownload("H.265", h265Url, codec = "h265", hasAudio = true),
                    ParsedDownload("H.264", h264Url, codec = "avc1", hasAudio = true),
                    ParsedDownload("Video only", videoOnlyUrl, codec = "h264", hasAudio = false)
                )
            )
        )
    }

    @Test
    fun convertsKnownDurationToUiMilliseconds() {
        assertEquals(12_345, playbackDurationMillis(12_345L))
    }

    @Test
    fun convertsUnknownDurationToZero() {
        assertEquals(0, playbackDurationMillis(-1L))
    }

    @Test
    fun clampsDurationToTheUiIntegerRange() {
        assertEquals(Int.MAX_VALUE, playbackDurationMillis(Long.MAX_VALUE))
    }
}
