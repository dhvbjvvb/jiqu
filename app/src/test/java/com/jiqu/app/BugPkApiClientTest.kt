package com.jiqu.app

import org.junit.Assert.assertEquals
import org.junit.Test

class BugPkApiClientTest {
    @Test
    fun apiFailureMessage_usesMessageBeforeError() {
        assertEquals("API Key 无效", apiFailureMessage("API Key 无效", "invalid"))
    }

    @Test
    fun apiFailureMessage_fallsBackWhenResponseHasNoDetails() {
        assertEquals("解析服务暂时不可用", apiFailureMessage(null, null))
    }
}
