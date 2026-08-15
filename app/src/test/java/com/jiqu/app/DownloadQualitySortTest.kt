package com.jiqu.app

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadQualitySortTest {
    @Test
    fun putsOriginalVideoFirstAfterItsLabelContainsTechnicalMetadata() {
        val downloads = listOf(
            ParsedDownload("720P · H.264", "https://example.com/720.mp4", width = 1280, height = 720),
            ParsedDownload(
                "1080P · H.265 · 30fps",
                "https://example.com/original.mp4",
                width = 1920,
                height = 1080,
                isOriginal = true
            ),
            ParsedDownload("1440P · H.265", "https://example.com/1440.mp4", width = 2560, height = 1440),
            ParsedDownload("1080P · H.264", "https://example.com/1080.mp4", width = 1920, height = 1080)
        )

        assertEquals(
            listOf(
                "1080P · H.265 · 30fps",
                "1440P · H.265",
                "1080P · H.264",
                "720P · H.264"
            ),
            sortDownloadQualities(downloads).map { it.label }
        )
    }
}
