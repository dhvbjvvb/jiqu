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
}
