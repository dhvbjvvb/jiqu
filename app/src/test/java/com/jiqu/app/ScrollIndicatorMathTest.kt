package com.jiqu.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ScrollIndicatorMathTest {
    @Test
    fun returnsZeroWhenScrollRangeIsTemporarilyUnavailable() {
        assertEquals(0, calculateScrollIndicatorOffset(500, 0.3f, 40, 0))
    }

    @Test
    fun clampsScrollValueWithinTheAvailableRange() {
        assertEquals(350, calculateScrollIndicatorOffset(500, 0.3f, 200, 100))
    }

    @Test
    fun returnsZeroForInvalidThumbFraction() {
        assertEquals(0, calculateScrollIndicatorOffset(500, Float.NaN, 40, 100))
    }
}
