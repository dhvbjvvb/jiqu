package com.jiqu.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadFileNameTest {
    @Test
    fun removesIllegalCharactersAndUsesUrlExtension() {
        assertEquals(
            "A_B_C_D_E_F.mp4",
            buildDownloadFileName("A/B:C*D?E\"F", "原始清晰度", "https://cdn.example.com/video.mp4?token=1")
        )
    }

    @Test
    fun fallsBackToMediaTypeExtensionWhenUrlHasNoExtension() {
        assertEquals(
            "music.mp3",
            buildDownloadFileName("music", "背景音乐", "https://cdn.example.com/resource")
        )
    }

    @Test
    fun appendsSequenceNumberForBatchDownloads() {
        assertEquals(
            "photos_2.jpg",
            buildDownloadFileName("photos", "图片", "https://cdn.example.com/image.jpg", sequenceNumber = 1)
        )
    }

    @Test
    fun keepsGalleryFileNamesStableAcrossSeparateDownloads() {
        val mediaSequenceNumbers = mapOf(
            "https://cdn.example.com/first.jpg" to 0,
            "https://cdn.example.com/second.jpg" to 1
        )
        assertEquals(
            "photos_1.jpg",
            buildDownloadFileName(
                "photos",
                "图片",
                "https://cdn.example.com/first.jpg",
                sequenceNumber = sequenceNumberForDownload(
                    downloadUrl = "https://cdn.example.com/first.jpg",
                    mediaSequenceNumbers = mediaSequenceNumbers,
                    batchIndex = 0,
                    batchSize = 1
                )
            )
        )
        assertEquals(
            "photos_2.jpg",
            buildDownloadFileName(
                "photos",
                "图片",
                "https://cdn.example.com/second.jpg",
                sequenceNumber = sequenceNumberForDownload(
                    downloadUrl = "https://cdn.example.com/second.jpg",
                    mediaSequenceNumbers = mediaSequenceNumbers,
                    batchIndex = 0,
                    batchSize = 1
                )
            )
        )
    }

    @Test
    fun acceptsOnlyHttpAndHttpsDownloadUrls() {
        assertTrue(isHttpDownloadUrl("https://cdn.example.com/video.mp4"))
        assertFalse(isHttpDownloadUrl("file:///sdcard/video.mp4"))
        assertFalse(isHttpDownloadUrl("not a url"))
    }
}
