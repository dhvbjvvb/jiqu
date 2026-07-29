package com.jiqu.app

internal data class DownloadByteRange(
    val startInclusive: Long,
    val endInclusive: Long
)

internal fun createDownloadByteRanges(
    totalBytes: Long,
    maximumWorkers: Int,
    minimumSegmentBytes: Long
): List<DownloadByteRange> {
    require(totalBytes > 0) { "totalBytes must be positive" }
    require(maximumWorkers > 0) { "maximumWorkers must be positive" }
    require(minimumSegmentBytes > 0) { "minimumSegmentBytes must be positive" }

    val workersForFileSize = ((totalBytes - 1) / minimumSegmentBytes) + 1
    val workerCount = minOf(maximumWorkers.toLong(), workersForFileSize).toInt()
    val baseSegmentBytes = totalBytes / workerCount
    val segmentsWithExtraByte = totalBytes % workerCount
    var nextStart = 0L

    return List(workerCount) { index ->
        val segmentBytes = baseSegmentBytes + if (index < segmentsWithExtraByte) 1 else 0
        DownloadByteRange(nextStart, nextStart + segmentBytes - 1).also {
            nextStart = it.endInclusive + 1
        }
    }
}
