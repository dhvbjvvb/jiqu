package com.jiqu.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ParsedMediaCacheTest {
    @Test
    fun rejectsInvalidConfiguration() {
        assertThrows(IllegalArgumentException::class.java) {
            ParsedMediaCache(maximumEntries = 0, timeToLiveMillis = 1_000)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ParsedMediaCache(maximumEntries = 1, timeToLiveMillis = 0)
        }
    }

    @Test
    fun returnsCachedMediaBeforeItExpires() {
        val cache = ParsedMediaCache(maximumEntries = 2, timeToLiveMillis = 1_000)
        val media = parsedMedia("https://example.com/one")

        cache.put(media, nowMillis = 100)

        assertEquals(media, cache.get(media.sourceUrl, nowMillis = 1_100))
    }

    @Test
    fun removesExpiredMedia() {
        val cache = ParsedMediaCache(maximumEntries = 2, timeToLiveMillis = 1_000)
        val media = parsedMedia("https://example.com/one")

        cache.put(media, nowMillis = 100)

        assertNull(cache.get(media.sourceUrl, nowMillis = 1_101))
        assertNull(cache.get(media.sourceUrl, nowMillis = 1_102))
    }

    @Test
    fun evictsTheLeastRecentlyUsedMedia() {
        val cache = ParsedMediaCache(maximumEntries = 2, timeToLiveMillis = 10_000)
        val first = parsedMedia("https://example.com/one")
        val second = parsedMedia("https://example.com/two")
        val third = parsedMedia("https://example.com/three")
        cache.put(first, nowMillis = 100)
        cache.put(second, nowMillis = 200)
        cache.get(first.sourceUrl, nowMillis = 300)

        cache.put(third, nowMillis = 400)

        assertEquals(first, cache.get(first.sourceUrl, nowMillis = 500))
        assertNull(cache.get(second.sourceUrl, nowMillis = 500))
        assertEquals(third, cache.get(third.sourceUrl, nowMillis = 500))
    }

    private fun parsedMedia(sourceUrl: String) = ParsedMedia(
        sourceUrl = sourceUrl,
        platformName = "Test",
        title = "Title",
        description = "Description",
        coverUrl = null,
        mediaType = "视频",
        previewUrl = "https://example.com/video.mp4",
        videoDownloads = listOf(ParsedDownload("720P", "https://example.com/video.mp4")),
        galleryItems = emptyList(),
        music = null
    )
}
