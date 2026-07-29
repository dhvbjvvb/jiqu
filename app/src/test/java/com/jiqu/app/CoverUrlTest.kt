package com.jiqu.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoverUrlTest {
    @Test
    fun extractsStandardCoverUrl() {
        val media = mapOf("cover" to "https://cdn.example.com/cover.jpg")

        assertEquals("https://cdn.example.com/cover.jpg", extractCoverUrl(media))
    }

    @Test
    fun extractsNestedCoverUrl() {
        val media = mapOf("cover_url" to mapOf("url" to "https://cdn.example.com/cover.webp"))

        assertEquals("https://cdn.example.com/cover.webp", extractCoverUrl(media))
    }

    @Test
    fun fallsBackToFirstImageUrl() {
        val media = mapOf("images" to listOf(mapOf("image" to "//cdn.example.com/first.jpg")))

        assertEquals("https://cdn.example.com/first.jpg", extractCoverUrl(media))
    }

    @Test
    fun rejectsNonHttpCoverUrl() {
        val media = mapOf("cover" to "javascript:alert(1)")

        assertNull(extractCoverUrl(media))
    }

    @Test
    fun usesVideoFrameInsteadOfTheIncorrectZuiyouCover() {
        val media = mapOf("cover" to "https://cdn.example.com/incorrect-cover.jpg")

        assertNull(extractCoverUrlForPlatform("zuiyou", media))
    }

    @Test
    fun addsBilibiliRefererAfterTheInitialRequest() {
        assertEquals(
            listOf(null, "https://www.bilibili.com/"),
            coverReferersFor("https://i0.hdslb.com/bfs/archive/cover.jpg")
        )
    }

    @Test
    fun addsXiaohongshuRefererAfterTheInitialRequest() {
        assertEquals(
            listOf(null, "https://www.xiaohongshu.com/"),
            coverReferersFor("https://sns-webpic-qc.xhscdn.com/cover.webp")
        )
    }
}
