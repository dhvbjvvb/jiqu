package com.jiqu.app

import kotlin.math.roundToInt

internal fun calculateScrollIndicatorOffset(
    containerHeight: Int,
    thumbFraction: Float,
    scrollValue: Int,
    maxScrollValue: Int
): Int {
    if (containerHeight <= 0 || maxScrollValue <= 0 || !thumbFraction.isFinite()) return 0

    val availableHeight = containerHeight * (1f - thumbFraction.coerceIn(0f, 1f))
    val scrollFraction = scrollValue.coerceIn(0, maxScrollValue).toFloat() / maxScrollValue
    return (availableHeight * scrollFraction).roundToInt()
}
