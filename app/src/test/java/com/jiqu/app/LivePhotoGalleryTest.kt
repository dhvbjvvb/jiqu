package com.jiqu.app
import org.junit.Assert.assertEquals
import org.junit.Test

class LivePhotoGalleryTest {
    @Test
    fun keepsEveryImagePageAndUsesTheMatchingLiveVideoForLivePages() {
        val galleryItems = buildGalleryItems(
            imageUrls = listOf(
                "https://cdn.example.com/first.jpg",
                "https://cdn.example.com/live.jpg",
                "https://cdn.example.com/last.jpg"
            ),
            liveDownloadsByImageUrl = mapOf(
                "https://cdn.example.com/live.jpg" to ParsedDownload(
                    "实况动态内容",
                    "https://cdn.example.com/live.mp4"
                )
            ),
            unpairedLiveDownloads = emptyList()
        )

        assertEquals(3, galleryItems.size)
        assertEquals(
            listOf(
                "https://cdn.example.com/first.jpg",
                "https://cdn.example.com/live.jpg",
                "https://cdn.example.com/last.jpg"
            ),
            galleryItems.map(ParsedGalleryItem::previewUrl)
        )
        assertEquals(
            listOf("图片", "实况动态内容", "图片"),
            galleryItems.map { it.download.label }
        )
        assertEquals("https://cdn.example.com/live.mp4", galleryItems[1].download.url)
    }

    @Test
    fun keepsAnAllStaticGalleryWhenTheApiMarksItAsLiveWithoutMatchingVideos() {
        val galleryItems = buildGalleryItems(
            imageUrls = listOf(
                "https://cdn.example.com/one.jpg",
                "https://cdn.example.com/two.jpg"
            ),
            liveDownloadsByImageUrl = emptyMap(),
            unpairedLiveDownloads = emptyList()
        )

        assertEquals(2, galleryItems.size)
        assertEquals(listOf("图片", "图片"), galleryItems.map { it.download.label })
    }

    @Test
    fun preservesUnpairedLiveVideosWithoutDroppingTheStaticImages() {
        val galleryItems = buildGalleryItems(
            imageUrls = listOf("https://cdn.example.com/static.jpg"),
            liveDownloadsByImageUrl = emptyMap(),
            unpairedLiveDownloads = listOf(
                ParsedDownload("实况动态内容", "https://cdn.example.com/live.mp4")
            )
        )

        assertEquals(2, galleryItems.size)
        assertEquals("https://cdn.example.com/static.jpg", galleryItems[0].previewUrl)
        assertEquals("图片", galleryItems[0].download.label)
        assertEquals(null, galleryItems[1].previewUrl)
        assertEquals("实况动态内容", galleryItems[1].download.label)
    }
}
