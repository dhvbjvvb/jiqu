package com.jiqu.app

import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryNavigationTest {
    @Test
    fun movesToNextImageAfterLeftSwipe() {
        assertEquals(1, galleryIndexForSwipe(currentIndex = 0, itemCount = 3, horizontalDragDistance = -100f))
    }

    @Test
    fun movesToPreviousImageAfterRightSwipeAndWraps() {
        assertEquals(2, galleryIndexForSwipe(currentIndex = 0, itemCount = 3, horizontalDragDistance = 100f))
    }

    @Test
    fun keepsCurrentImageForShortDragOrSingleImage() {
        assertEquals(1, galleryIndexForSwipe(currentIndex = 1, itemCount = 3, horizontalDragDistance = 20f))
        assertEquals(0, galleryIndexForSwipe(currentIndex = 0, itemCount = 1, horizontalDragDistance = -100f))
    }
}
