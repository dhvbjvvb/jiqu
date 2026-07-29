package com.jiqu.app

import org.junit.Assert.assertEquals
import org.junit.Test

class BilibiliMediaUrlTest {
    @Test
    fun replacesTheUnreliableBilibiliAkamaiMirror() {
        val sourceUrl = "https://upos-hz-mirrorakam.akamaized.net/upgcxcode/video.mp4?platform=html5&deadline=1"

        assertEquals(
            "https://upos-sz-mirrorcos.bilivideo.com/upgcxcode/video.mp4?platform=html5&deadline=1",
            normalizeVideoUrlForPlayback(sourceUrl, "bilibili")
        )
    }

    @Test
    fun keepsOtherPlatformUrlsUnchanged() {
        val sourceUrl = "https://example.com/video.mp4?quality=1080p"

        assertEquals(sourceUrl, normalizeVideoUrlForPlayback(sourceUrl, "douyin"))
    }
}
