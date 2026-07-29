package com.jiqu.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DouyinDescriptionTest {
    @Test
    fun extractsTheCompleteDescriptionFromTheItemList() {
        val pageHtml = """{"item_list":[{"aweme_id":"123","desc":"第一段\n第二段\n完整文案"}]}"""

        assertEquals("第一段\n第二段\n完整文案", extractDouyinDescription(pageHtml))
    }

    @Test
    fun ignoresPagesWithoutAnItemList() {
        assertNull(extractDouyinDescription("{\"desc\":\"其他字段\"}"))
    }

    @Test
    fun removesTheApiTruncationNoticeWhenFallbackIsUsed() {
        assertEquals(
            "正文内容",
            sanitizeDescription("正文内容 ……版本过低，升级后可展示全部信息")
        )
    }
}
