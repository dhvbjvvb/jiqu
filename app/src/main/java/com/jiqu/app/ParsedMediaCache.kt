package com.jiqu.app

internal class ParsedMediaCache(
    private val maximumEntries: Int = DEFAULT_MAXIMUM_ENTRIES,
    private val timeToLiveMillis: Long = DEFAULT_TIME_TO_LIVE_MILLIS
) {
    init {
        require(maximumEntries > 0) { "maximumEntries must be positive" }
        require(timeToLiveMillis > 0) { "timeToLiveMillis must be positive" }
    }

    private data class CacheEntry(
        val media: ParsedMedia,
        val storedAtMillis: Long
    )

    private val entries = linkedMapOf<String, CacheEntry>()

    @Synchronized
    fun get(sourceUrl: String, nowMillis: Long = System.currentTimeMillis()): ParsedMedia? {
        val entry = entries[sourceUrl] ?: return null
        if (nowMillis - entry.storedAtMillis > timeToLiveMillis) {
            entries.remove(sourceUrl)
            return null
        }
        entries.remove(sourceUrl)
        entries[sourceUrl] = entry
        return entry.media
    }

    @Synchronized
    fun put(media: ParsedMedia, nowMillis: Long = System.currentTimeMillis()) {
        entries.remove(media.sourceUrl)
        entries[media.sourceUrl] = CacheEntry(media, nowMillis)
        while (entries.size > maximumEntries) {
            entries.remove(entries.keys.first())
        }
    }

    private companion object {
        const val DEFAULT_MAXIMUM_ENTRIES = 20
        const val DEFAULT_TIME_TO_LIVE_MILLIS = 5 * 60 * 1_000L
    }
}
