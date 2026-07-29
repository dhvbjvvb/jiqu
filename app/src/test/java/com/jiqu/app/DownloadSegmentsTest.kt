package com.jiqu.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadSegmentsTest {
    @Test(expected = IllegalArgumentException::class)
    fun rejectsAnEmptyFile() {
        createDownloadByteRanges(
            totalBytes = 0,
            maximumWorkers = 20,
            minimumSegmentBytes = 1_048_576
        )
    }

    @Test
    fun createsContiguousRangesForTheWholeFile() {
        val ranges = createDownloadByteRanges(
            totalBytes = 10_485_760,
            maximumWorkers = 20,
            minimumSegmentBytes = 1_048_576
        )

        assertEquals(10, ranges.size)
        assertEquals(0, ranges.first().startInclusive)
        assertEquals(10_485_759, ranges.last().endInclusive)
        assertTrue(ranges.zipWithNext().all { (left, right) -> left.endInclusive + 1 == right.startInclusive })
    }

    @Test
    fun limitsSegmentsToTwentyWorkers() {
        val ranges = createDownloadByteRanges(
            totalBytes = 41_943_040,
            maximumWorkers = 20,
            minimumSegmentBytes = 1_048_576
        )

        assertEquals(20, ranges.size)
        assertTrue(ranges.all { it.endInclusive - it.startInclusive + 1 == 2_097_152L })
    }
}
