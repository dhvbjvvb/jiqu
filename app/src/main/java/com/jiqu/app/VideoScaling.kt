package com.jiqu.app

internal data class VideoScale(val scaleX: Float, val scaleY: Float)

internal fun calculateVideoScale(
    viewWidth: Int,
    viewHeight: Int,
    videoWidth: Int,
    videoHeight: Int
): VideoScale {
    if (viewWidth <= 0 || viewHeight <= 0 || videoWidth <= 0 || videoHeight <= 0) {
        return VideoScale(1f, 1f)
    }

    val videoAspect = videoWidth.toFloat() / videoHeight
    val viewAspect = viewWidth.toFloat() / viewHeight
    return if (videoAspect > viewAspect) {
        VideoScale(scaleX = 1f, scaleY = viewAspect / videoAspect)
    } else {
        VideoScale(scaleX = videoAspect / viewAspect, scaleY = 1f)
    }
}
