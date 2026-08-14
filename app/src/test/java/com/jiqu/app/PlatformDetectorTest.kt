package com.jiqu.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlatformDetectorTest {
    @Test
    fun detectsEachAllowedPlatformFromShareText() {
        val cases = mapOf(
            "https://b23.tv/example" to "bilibili",
            "https://v.douyin.com/example/" to "douyin",
            "https://v.kuaishou.com/example" to "kuaishou",
            "https://h5.pipix.com/example" to "pipixia",
            "https://h5.ippzone.com/example" to "pipigx",
            "https://www.toutiao.com/video/example" to "toutiao",
            "https://weibo.com/tv/show/example" to "weibo",
            "https://t.cn/example" to "weibo",
            "https://channels.weixin.qq.com/example" to "wxsph",
            "https://weixin.qq.com/cgi-bin/readtemplate?t=pages/video_player&oid=example&vid=example" to "wxsph",
            "https://xhslink.com/example" to "xiaohongshu",
            "https://share.izuiyou.com/example" to "zuiyou"
        )

        cases.forEach { (url, expectedPlatform) ->
            assertEquals(expectedPlatform, PlatformDetector.findSupportedPlatform("分享链接：$url").orEmptyId())
        }
    }

    @Test
    fun detectsExtendedPlatformWithoutChangingDisplayedList() {
        assertEquals("youtube", PlatformDetector.findSupportedPlatform("https://www.youtube.com/watch?v=example").orEmptyId())
        assertEquals("instagram", PlatformDetector.findSupportedPlatform("https://www.instagram.com/reel/example").orEmptyId())
        assertEquals("oasis", PlatformDetector.findSupportedPlatform("https://oasis.weibo.cn/example").orEmptyId())
        assertEquals("qishui_music", PlatformDetector.findSupportedPlatform("https://qishui.douyin.com/video/example").orEmptyId())
        assertEquals(10, PlatformDetector.supportedPlatformList().size)
        assertNull(PlatformDetector.supportedPlatformList().firstOrNull { it.id == "youtube" })
    }

    @Test
    fun choosesAllowedLinkWhenShareTextContainsAnotherLinkFirst() {
        val shareText = "详情：https://example.com 作品：https://v.douyin.com/example/"

        assertEquals("douyin", PlatformDetector.findSupportedPlatform(shareText).orEmptyId())
        assertEquals("https://v.douyin.com/example/", PlatformDetector.extractUrl(shareText))
    }

    @Test
    fun extractsOnlySupportedLinkFromShareMessage() {
        val shareText = "复制此链接，打开抖音搜索，查看视频 https://v.douyin.com/example/"

        assertEquals("https://v.douyin.com/example/", PlatformDetector.extractUrl(shareText))
    }

    private fun SupportedPlatform?.orEmptyId(): String = this?.id.orEmpty()
}
