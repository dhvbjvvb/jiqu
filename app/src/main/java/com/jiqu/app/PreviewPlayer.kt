@file:androidx.annotation.OptIn(
    markerClass = [androidx.media3.common.util.UnstableApi::class]
)

package com.jiqu.app

import android.content.Context
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheKeyFactory
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import java.io.File
import java.security.MessageDigest

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
internal fun createPreviewPlayer(context: Context, previewUrl: String): ExoPlayer {
    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(PREVIEW_CONNECT_TIMEOUT_MILLIS)
        .setReadTimeoutMs(PREVIEW_READ_TIMEOUT_MILLIS)
        .setDefaultRequestProperties(PREVIEW_REQUEST_HEADERS)
    val upstreamDataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
    val playbackDataSourceFactory: DataSource.Factory = runCatching {
        CacheDataSource.Factory()
            .setCache(PreviewVideoCache.get(context))
            .setUpstreamDataSourceFactory(upstreamDataSourceFactory)
            .setCacheKeyFactory(HASHED_CACHE_KEY_FACTORY)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }.getOrElse {
        Log.w(PERFORMANCE_LOG_TAG, "preview_cache_unavailable")
        upstreamDataSourceFactory
    }
    val mediaSourceFactory = DefaultMediaSourceFactory(context)
        .setDataSourceFactory(playbackDataSourceFactory)
    val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            PREVIEW_MINIMUM_BUFFER_MILLIS,
            PREVIEW_MAXIMUM_BUFFER_MILLIS,
            PREVIEW_START_BUFFER_MILLIS,
            PREVIEW_REBUFFER_MILLIS
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()

    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(mediaSourceFactory)
        .setLoadControl(loadControl)
        .build()
        .apply {
            setAudioAttributes(AudioAttributes.DEFAULT, true)
            setHandleAudioBecomingNoisy(true)
            switchPreviewSource(this, previewUrl)
        }
}

internal fun warmPreviewVideoCache(context: Context) {
    runCatching { PreviewVideoCache.get(context) }
        .onFailure { Log.w(PERFORMANCE_LOG_TAG, "preview_cache_warmup_failed") }
}

internal fun switchPreviewSource(player: ExoPlayer, previewUrl: String) {
    player.setMediaItem(
        MediaItem.Builder()
            .setMediaId(previewUrl)
            .setUri(previewUrl)
            .build()
    )
    player.playWhenReady = true
    player.prepare()
}

internal fun previewCandidateUrls(
    preferredUrl: String?,
    downloads: List<ParsedDownload>
): List<String> = buildList {
    preferredUrl?.let(::add)
    downloads.asSequence()
        .filter { it.hasAudio != false && normalizeVideoCodec(it.codec) == "H.264" }
        .map(ParsedDownload::url)
        .forEach(::add)
    downloads.asSequence()
        .filter { it.hasAudio != false }
        .map(ParsedDownload::url)
        .forEach(::add)
}.distinct()

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
internal fun extractPlayerVideoMetadata(player: ExoPlayer): VideoTechnicalMetadata? {
    val videoFormat = player.videoFormat ?: return null
    return VideoTechnicalMetadata(
        width = videoFormat.width.takeIf { it > 0 },
        height = videoFormat.height.takeIf { it > 0 },
        codec = videoFormat.codecs ?: videoFormat.sampleMimeType,
        bitRate = videoFormat.averageBitrate.takeIf { it > 0 }?.toLong()
            ?: videoFormat.peakBitrate.takeIf { it > 0 }?.toLong(),
        frameRate = videoFormat.frameRate.takeIf { it.isFinite() && it > 0f },
        hasAudio = player.audioFormat != null
    )
}

internal fun playbackDurationMillis(durationMillis: Long): Int =
    durationMillis.takeIf { it >= 0 }
        ?.coerceAtMost(Int.MAX_VALUE.toLong())
        ?.toInt()
        ?: 0

private const val PREVIEW_CONNECT_TIMEOUT_MILLIS = 10_000
private const val PREVIEW_READ_TIMEOUT_MILLIS = 20_000
private const val PREVIEW_MINIMUM_BUFFER_MILLIS = 2_000
private const val PREVIEW_MAXIMUM_BUFFER_MILLIS = 15_000
private const val PREVIEW_START_BUFFER_MILLIS = 500
private const val PREVIEW_REBUFFER_MILLIS = 1_000
private const val PREVIEW_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36"
private val PREVIEW_REQUEST_HEADERS = mapOf(
    "User-Agent" to PREVIEW_USER_AGENT,
    "Accept" to "video/mp4,video/*;q=0.9,*/*;q=0.8",
    "Accept-Encoding" to "identity"
)

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
private val HASHED_CACHE_KEY_FACTORY = CacheKeyFactory { dataSpec ->
    MessageDigest.getInstance("SHA-256")
        .digest(dataSpec.uri.toString().toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
}

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
private object PreviewVideoCache {
    private var cache: SimpleCache? = null

    @Synchronized
    @androidx.annotation.OptIn(markerClass = [UnstableApi::class])
    fun get(context: Context): SimpleCache = cache ?: SimpleCache(
        File(context.applicationContext.cacheDir, PREVIEW_CACHE_DIRECTORY_NAME),
        LeastRecentlyUsedCacheEvictor(PREVIEW_CACHE_MAXIMUM_BYTES),
        StandaloneDatabaseProvider(context.applicationContext)
    ).also { cache = it }
}

private const val PREVIEW_CACHE_DIRECTORY_NAME = "preview-video"
private const val PREVIEW_CACHE_MAXIMUM_BYTES = 50L * 1024L * 1024L
private const val PERFORMANCE_LOG_TAG = "JiquPerformance"
