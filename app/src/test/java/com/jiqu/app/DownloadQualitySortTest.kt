package com.jiqu.app

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadQualitySortTest {
    @Test
    fun putsOriginalQualityFirstThenSortsByResolutionDescending() {
        val downloads = listOf(
            ParsedDownload("720p", "https://example.com/720.mp4"),
            ParsedDownload("原始清晰度", "https://example.com/original.mp4"),
            ParsedDownload("1440p", "https://example.com/1440.mp4"),
            ParsedDownload("1080p", "https://example.com/1080.mp4")
        )

        assertEquals(
            listOf("原始清晰度", "1440p", "1080p", "720p"),
            sortDownloadQualities(downloads).map { it.label }
        )
    }
}
