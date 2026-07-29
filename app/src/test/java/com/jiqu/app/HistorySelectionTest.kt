package com.jiqu.app

import org.junit.Assert.assertEquals
import org.junit.Test

class HistorySelectionTest {
    @Test
    fun deduplicatesSourceUrlsWhilePreservingTheirOrder() {
        assertEquals(
            linkedSetOf("https://example.com/first", "https://example.com/second"),
            historySourceUrls(
                listOf(
                    historyEntry("https://example.com/first", "首次解析"),
                    historyEntry("https://example.com/second", "第二次解析"),
                    historyEntry("https://example.com/first", "重复解析")
                )
            )
        )
    }

    @Test
    fun doesNotTreatAnEmptyHistoryAsFullySelected() {
        org.junit.Assert.assertFalse(areAllHistoryUrlsSelected(emptyList(), emptySet()))
    }

    @Test
    fun keepsOnlyTheLatestEntryForTheSameSourceUrl() {
        val oldEntry = historyEntry(sourceUrl = "https://example.com/video", title = "旧标题")
        val otherEntry = historyEntry(sourceUrl = "https://example.com/other", title = "其他视频")
        val latestEntry = historyEntry(sourceUrl = "https://example.com/video", title = "新标题")

        assertEquals(
            listOf(latestEntry, otherEntry),
            replaceHistoryEntry(listOf(oldEntry, otherEntry), latestEntry, maximumEntries = 50)
        )
    }

    @Test
    fun recognizesAllSelectedWhenLegacyHistoryContainsDuplicateUrls() {
        val firstEntry = historyEntry(sourceUrl = "https://example.com/video", title = "旧标题")
        val duplicateEntry = historyEntry(sourceUrl = "https://example.com/video", title = "新标题")
        val otherEntry = historyEntry(sourceUrl = "https://example.com/other", title = "其他视频")
        val history = listOf(firstEntry, duplicateEntry, otherEntry)

        assertEquals(setOf("https://example.com/video", "https://example.com/other"), historySourceUrls(history))
        org.junit.Assert.assertTrue(areAllHistoryUrlsSelected(history, historySourceUrls(history)))
    }

    private fun historyEntry(sourceUrl: String, title: String): ParseHistoryEntry = ParseHistoryEntry(
        sourceUrl = sourceUrl,
        title = title,
        platformName = "测试平台",
        mediaType = "视频",
        parsedAt = "07-29 08:00",
        coverUrl = null,
        previewUrl = null
    )
}
