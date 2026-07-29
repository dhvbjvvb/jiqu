package com.jiqu.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectVideoFormatTest {
    @Test
    fun rejectsDashVideoStreamsThatCannotContainMuxedAudio() {
        assertFalse(isDirectlyDownloadableVideoFormat("dash"))
        assertFalse(isDirectlyDownloadableVideoFormat("DASH"))
    }

    @Test
    fun acceptsProgressiveMp4AndUnspecifiedFormats() {
        assertTrue(isDirectlyDownloadableVideoFormat("mp4"))
        assertTrue(isDirectlyDownloadableVideoFormat(""))
    }
}
