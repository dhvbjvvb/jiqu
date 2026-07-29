package com.jiqu.app

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoScalingTest {
    @Test
    fun keepsWideVideoInsideSixteenByNinePreview() {
        val scale = calculateVideoScale(1_600, 900, 1_920, 1_080)

        assertEquals(1f, scale.scaleX, 0.001f)
        assertEquals(1f, scale.scaleY, 0.001f)
    }

    @Test
    fun letterboxesPortraitVideoInsideSixteenByNinePreview() {
        val scale = calculateVideoScale(1_600, 900, 1_080, 1_920)

        assertEquals(0.316f, scale.scaleX, 0.001f)
        assertEquals(1f, scale.scaleY, 0.001f)
    }

    @Test
    fun retainsNeutralScaleForInvalidDimensions() {
        val scale = calculateVideoScale(0, 900, 1_080, 1_920)

        assertEquals(1f, scale.scaleX, 0f)
        assertEquals(1f, scale.scaleY, 0f)
    }
}
