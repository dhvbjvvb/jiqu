package com.jiqu.app

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal data class SupportedPlatform(
    val id: String,
    val displayName: String,
    val hostMarkers: Set<String>
)

internal data class SupportedLink(
    val platform: SupportedPlatform,
    val url: String
)

internal object PlatformDetector {
    private val supportedPlatforms = listOf(
        SupportedPlatform("bilibili", "哔哩哔哩", setOf("bilibili.com", "b23.tv")),
        SupportedPlatform("douyin", "抖音", setOf("douyin.com", "iesdouyin.com")),
        SupportedPlatform("kuaishou", "快手", setOf("kuaishou.com", "kwai.com")),
        SupportedPlatform("pipixia", "皮皮虾", setOf("pipix.com", "pipixia.com")),
        SupportedPlatform("pipigx", "皮皮搞笑", setOf("pipigx.com", "ippzone.com")),
        SupportedPlatform("toutiao", "今日头条", setOf("toutiao.com")),
        SupportedPlatform("weibo", "微博", setOf("weibo.com", "weibo.cn", "t.cn")),
        SupportedPlatform("wxsph", "微信视频号", setOf("channels.weixin.qq.com", "weixin.qq.com")),
        SupportedPlatform("xiaohongshu", "小红书", setOf("xiaohongshu.com", "xhslink.com", "xhslink.cn", "xhs.com")),
        SupportedPlatform("zuiyou", "最右", setOf("izuiyou.com", "xiaochuankeji.cn"))
    )

    private val urlPattern = Regex("https?://[^\\s\\u3000]+", RegexOption.IGNORE_CASE)

    fun findSupportedLink(input: String): SupportedLink? =
        urlPattern.findAll(input)
            .map { it.value.trimEnd('.', ',', '!', '?', '，', '。', '！', '？') }
            .firstNotNullOfOrNull { url ->
                val host = runCatching { URI(url).host?.lowercase() }.getOrNull() ?: return@firstNotNullOfOrNull null
                supportedPlatforms.firstOrNull { platform ->
                    platform.hostMarkers.any { marker -> host == marker || host.endsWith(".$marker") }
                }?.let { platform -> SupportedLink(platform, url) }
            }

    fun findSupportedPlatform(input: String): SupportedPlatform? = findSupportedLink(input)?.platform

    fun extractUrl(input: String): String? = findSupportedLink(input)?.url

    fun supportedPlatformList(): List<SupportedPlatform> = supportedPlatforms
}

internal data class ParsedDownload(
    val label: String,
    val url: String
)

internal fun isDirectlyDownloadableVideoFormat(format: String): Boolean =
    !format.equals("dash", ignoreCase = true)

internal fun normalizeVideoUrlForPlayback(url: String, platformId: String): String = runCatching {
    val uri = URI(url)
    if (platformId == BILIBILI_PLATFORM_ID && uri.host.orEmpty().matches(BILIBILI_AKAMAI_HOST_PATTERN)) {
        URI(uri.scheme, uri.userInfo, BILIBILI_PLAYBACK_MIRROR_HOST, uri.port, uri.path, uri.query, uri.fragment).toString()
    } else {
        url
    }
}.getOrDefault(url)

internal data class ParsedMedia(
    val sourceUrl: String,
    val platformName: String,
    val title: String,
    val description: String,
    val coverUrl: String?,
    val mediaType: String,
    val previewUrl: String?,
    val videoDownloads: List<ParsedDownload>,
    val music: ParsedDownload?
)

internal fun extractCoverUrl(fields: Map<String, Any?>): String? {
    val directCover = COVER_URL_KEYS.asSequence()
        .map(fields::get)
        .mapNotNull(::findCoverUrlCandidate)
        .firstOrNull()
    if (directCover != null) return directCover

    return COVER_FALLBACK_KEYS.asSequence()
        .map(fields::get)
        .mapNotNull(::findCoverUrlCandidate)
        .firstOrNull()
}

private fun findCoverUrlCandidate(value: Any?): String? = when (value) {
    is String -> value.normalizeCoverUrl()
    is Iterable<*> -> value.asSequence()
        .mapNotNull(::findCoverUrlCandidate)
        .firstOrNull()
    is Map<*, *> -> COVER_OBJECT_URL_KEYS.asSequence()
        .map(value::get)
        .mapNotNull(::findCoverUrlCandidate)
        .firstOrNull()
    else -> null
}

private fun JSONObject.toCoverFieldMap(): Map<String, Any?> = buildMap {
    keys().forEach { key -> put(key, opt(key).toCoverFieldValue()) }
}

private fun Any?.toCoverFieldValue(): Any? = when (this) {
    is JSONObject -> toCoverFieldMap()
    is JSONArray -> (0 until length()).map { index -> opt(index).toCoverFieldValue() }
    else -> this
}

internal fun String.normalizeCoverUrl(): String? {
    val normalizedUrl = trim().let { url -> if (url.startsWith("//")) "https:$url" else url }
    if (!normalizedUrl.isHttpUrl()) return null

    return runCatching {
        val uri = URI(normalizedUrl)
        if (uri.host.orEmpty().endsWith("hdslb.com")) {
            URI("https", uri.userInfo, uri.host, uri.port, uri.path, uri.query, uri.fragment).toString()
        } else {
            normalizedUrl
        }
    }.getOrNull()
}

private fun String.isHttpUrl(): Boolean = startsWith("https://") || startsWith("http://")

internal fun extractCoverUrlForPlatform(platformId: String, fields: Map<String, Any?>): String? =
    if (platformId == ZUIYOU_PLATFORM_ID) null else extractCoverUrl(fields)

internal sealed interface ParseResult {
    data class Success(val media: ParsedMedia) : ParseResult
    data class Failure(val message: String) : ParseResult
}

internal class BugPkApiClient {
    fun parse(sharedText: String): ParseResult {
        val supportedLink = PlatformDetector.findSupportedLink(sharedText)
            ?: return ParseResult.Failure("仅支持哔哩哔哩、抖音、快手、皮皮虾、皮皮搞笑、今日头条、微博、微信视频号、小红书和最右链接")

        return runCatching {
            val response = request(supportedLink.url, supportedLink.platform.id)
            val root = JSONObject(response)
            if (root.optInt("code", 0) != 200) {
                return ParseResult.Failure(root.optString("msg", "解析服务暂时不可用"))
            }

            val data = root.optJSONObject("data")
                ?: return ParseResult.Failure("解析服务未返回媒体数据")
            val videoDownloads = extractVideoDownloads(data, supportedLink.platform.id)
            if (videoDownloads.isEmpty()) {
                return ParseResult.Failure("未获取到可下载的媒体地址")
            }
            val title = data.firstNonBlank("title", "desc", "description") ?: "未命名媒体"
            val apiDescription = data.firstNonBlank("desc", "description", "title") ?: title
            val description = if (supportedLink.platform.id == DOUYIN_PLATFORM_ID) {
                fetchDouyinDescription(supportedLink.url) ?: sanitizeDescription(apiDescription)
            } else {
                apiDescription
            }

            ParseResult.Success(
                ParsedMedia(
                    sourceUrl = supportedLink.url,
                    platformName = supportedLink.platform.displayName,
                    title = title,
                    description = description,
                    coverUrl = extractCoverUrlForPlatform(
                        platformId = supportedLink.platform.id,
                        fields = data.toCoverFieldMap()
                    ),
                    mediaType = data.optString("type", "video").toDisplayName(),
                    previewUrl = data.optString("type", "video").takeIf { it == "video" }?.let {
                        data.optString("url").takeIf { url -> url.isHttpUrl() }?.let { url ->
                            normalizeVideoUrlForPlayback(url, supportedLink.platform.id)
                        }
                            ?: videoDownloads.firstOrNull { download -> download.label.contains("视频") }?.url
                            ?: videoDownloads.firstOrNull()?.url
                    },
                    videoDownloads = videoDownloads,
                    music = data.optJSONObject("music")?.optString("url")
                        ?.takeIf { it.isHttpUrl() }
                        ?.let { ParsedDownload("背景音乐", it) }
                )
            )
        }.getOrElse { error ->
            ParseResult.Failure(error.message ?: "网络请求失败，请稍后重试")
        }
    }

    private fun request(sharedUrl: String, platformId: String): String {
        val query = "url=${URLEncoder.encode(sharedUrl, StandardCharsets.UTF_8.name())}&platform=$platformId"
        val connection = (java.net.URL("https://api.bugpk.com/api/short_videos?$query").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Jiqu-Android/1.0")
        }
        return try {
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            stream?.readUtf8() ?: throw IllegalStateException("解析服务未返回内容")
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchDouyinDescription(sharedUrl: String): String? = runCatching {
        val connection = (java.net.URL(sharedUrl).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            setRequestProperty("Accept", "text/html")
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/125.0 Mobile Safari/537.36"
            )
        }
        try {
            if (connection.responseCode !in 200..299) return@runCatching null
            extractDouyinDescription(connection.inputStream.readUtf8())
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

    private fun extractVideoDownloads(data: JSONObject, platformId: String): List<ParsedDownload> {
        val downloads = linkedMapOf<String, ParsedDownload>()
        val type = data.optString("type", "video")
        when (type) {
            "image" -> collectUrls(data.opt("images"), "图片", downloads)
            "live" -> {
                collectUrls(data.opt("images"), "实况图片", downloads)
                collectLivePhotoUrls(data.opt("live_photo"), downloads)
            }
            else -> {
                data.optString("url")
                    .takeIf { isDirectlyDownloadableVideoFormat(data.optString("format")) }
                    ?.takeIf { it.isHttpUrl() }
                    ?.let {
                    val playableUrl = normalizeVideoUrlForPlayback(it, platformId)
                    downloads[playableUrl] = ParsedDownload(ORIGINAL_QUALITY_LABEL, playableUrl)
                }
                collectPlayableVideoUrls(data.opt("videos"), ORIGINAL_QUALITY_LABEL, platformId, downloads)
                collectPlayableVideoUrls(data.opt("video_backup"), "备用清晰度", platformId, downloads)
            }
        }
        return downloads.values.toList()
    }

    private fun collectPlayableVideoUrls(
        value: Any?,
        defaultLabel: String,
        platformId: String,
        downloads: MutableMap<String, ParsedDownload>
    ) {
        when (value) {
            is JSONArray -> (0 until value.length()).forEach {
                collectPlayableVideoUrls(value.opt(it), defaultLabel, platformId, downloads)
            }
            is JSONObject -> {
                if (!isDirectlyDownloadableVideoFormat(value.optString("format"))) return
                val label = value.firstNonBlank("quality", "label", "name") ?: defaultLabel
                value.optString("url").takeIf { it.isHttpUrl() }?.let { url ->
                    val playableUrl = normalizeVideoUrlForPlayback(url, platformId)
                    downloads[playableUrl] = ParsedDownload(label, playableUrl)
                }
                value.optString("video").takeIf { it.isHttpUrl() }?.let { url ->
                    val playableUrl = normalizeVideoUrlForPlayback(url, platformId)
                    downloads[playableUrl] = ParsedDownload(label, playableUrl)
                }
                value.keys().forEach { key ->
                    if (key !in mediaKeys) collectPlayableVideoUrls(value.opt(key), key, platformId, downloads)
                }
            }
            is String -> value.takeIf { it.isHttpUrl() }?.let { url ->
                val playableUrl = normalizeVideoUrlForPlayback(url, platformId)
                downloads[playableUrl] = ParsedDownload(defaultLabel, playableUrl)
            }
        }
    }

    private fun collectUrls(value: Any?, defaultLabel: String, downloads: MutableMap<String, ParsedDownload>) {
        when (value) {
            is JSONArray -> (0 until value.length()).forEach { collectUrls(value.opt(it), defaultLabel, downloads) }
            is JSONObject -> {
                val label = value.firstNonBlank("quality", "label", "name") ?: defaultLabel
                value.optString("url").takeIf { it.isHttpUrl() }?.let { downloads[it] = ParsedDownload(label, it) }
                value.optString("image").takeIf { it.isHttpUrl() }?.let { downloads[it] = ParsedDownload(defaultLabel, it) }
                value.optString("video").takeIf { it.isHttpUrl() }?.let { downloads[it] = ParsedDownload(defaultLabel, it) }
                value.keys().forEach { key ->
                    if (key !in mediaKeys) collectUrls(value.opt(key), key, downloads)
                }
            }
            is String -> value.takeIf { it.isHttpUrl() }?.let { downloads[it] = ParsedDownload(defaultLabel, it) }
        }
    }

    private fun collectLivePhotoUrls(value: Any?, downloads: MutableMap<String, ParsedDownload>) {
        when (value) {
            is JSONArray -> (0 until value.length()).forEach { collectLivePhotoUrls(value.opt(it), downloads) }
            is JSONObject -> {
                LIVE_PHOTO_URL_KEYS.forEach { key ->
                    value.optString(key).takeIf { it.isHttpUrl() }?.let { url ->
                        downloads[url] = ParsedDownload("实况动态内容", url)
                    }
                }
                value.optString("url")
                    .takeIf { it.isHttpUrl() && value.optString("format").isDynamicLivePhotoFormat() }
                    ?.let { url -> downloads[url] = ParsedDownload("实况动态内容", url) }
                LIVE_PHOTO_NESTED_KEYS.forEach { key -> collectLivePhotoUrls(value.opt(key), downloads) }
            }
            is String -> value.takeIf { it.isHttpUrl() && it.hasDynamicLivePhotoExtension() }?.let { url ->
                downloads[url] = ParsedDownload("实况动态内容", url)
            }
        }
    }

    private fun JSONObject.firstNonBlank(vararg keys: String): String? =
        keys.asSequence().map { optString(it).trim() }.firstOrNull { it.isNotEmpty() }

    private fun String.isDynamicLivePhotoFormat(): Boolean = lowercase() in DYNAMIC_LIVE_PHOTO_FORMATS

    private fun String.hasDynamicLivePhotoExtension(): Boolean = runCatching {
        val path = URI(this).path.orEmpty().lowercase()
        DYNAMIC_LIVE_PHOTO_FILE_EXTENSIONS.any(path::endsWith)
    }.getOrDefault(false)

    private fun String.toDisplayName(): String = when (lowercase()) {
        "image" -> "图集"
        "live" -> "实况"
        else -> "视频"
    }

    private fun InputStream.readUtf8(): String =
        BufferedReader(InputStreamReader(this, StandardCharsets.UTF_8)).use { it.readText() }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 30_000
        val mediaKeys = setOf("url", "image", "video", "quality", "label", "name")
        val LIVE_PHOTO_URL_KEYS = setOf("video", "live", "live_url", "file")
        val LIVE_PHOTO_NESTED_KEYS = setOf("live_photo", "live_video", "motion")
        val DYNAMIC_LIVE_PHOTO_FORMATS = setOf("live", "video", "mp4", "mov", "webm", "gif", "animated_webp")
        val DYNAMIC_LIVE_PHOTO_FILE_EXTENSIONS = setOf(".mp4", ".mov", ".webm", ".gif")
    }
}

internal fun extractDouyinDescription(pageHtml: String): String? {
    val itemListIndex = pageHtml.indexOf("\"item_list\"")
    if (itemListIndex < 0) return null
    val descriptionKeyIndex = pageHtml.indexOf("\"desc\"", itemListIndex)
    if (descriptionKeyIndex < 0) return null
    val valueStart = pageHtml.indexOf('"', pageHtml.indexOf(':', descriptionKeyIndex) + 1)
    if (valueStart < 0) return null
    val valueEnd = findJsonStringEnd(pageHtml, valueStart)
    if (valueEnd < 0) return null
    return decodeJsonString(pageHtml.substring(valueStart + 1, valueEnd))
        .trim()
        .takeIf(String::isNotEmpty)
}

internal fun sanitizeDescription(description: String): String =
    description.replace(DOUYIN_TRUNCATION_NOTICE_PATTERN, "").trim()

private fun findJsonStringEnd(source: String, startIndex: Int): Int {
    var isEscaped = false
    for (index in startIndex + 1 until source.length) {
        when {
            isEscaped -> isEscaped = false
            source[index] == '\\' -> isEscaped = true
            source[index] == '"' -> return index
        }
    }
    return -1
}

private fun decodeJsonString(value: String): String {
    val decoded = StringBuilder(value.length)
    var index = 0
    while (index < value.length) {
        val character = value[index]
        if (character != '\\' || index == value.lastIndex) {
            decoded.append(character)
            index++
            continue
        }
        when (val escapedCharacter = value[index + 1]) {
            'n' -> decoded.append('\n')
            'r' -> decoded.append('\r')
            't' -> decoded.append('\t')
            'b' -> decoded.append('\b')
            'f' -> decoded.append('\u000C')
            '"', '\\', '/' -> decoded.append(escapedCharacter)
            'u' -> {
                val codePointEnd = index + 6
                val unicodeCharacter = value.takeIf { codePointEnd <= value.length }
                    ?.substring(index + 2, codePointEnd)
                    ?.toIntOrNull(16)
                    ?.toChar()
                if (unicodeCharacter != null) {
                    decoded.append(unicodeCharacter)
                    index = codePointEnd
                    continue
                }
                decoded.append("\\u")
            }
            else -> decoded.append(escapedCharacter)
        }
        index += 2
    }
    return decoded.toString()
}

private const val BILIBILI_PLATFORM_ID = "bilibili"
private const val BILIBILI_PLAYBACK_MIRROR_HOST = "upos-sz-mirrorcos.bilivideo.com"
private val BILIBILI_AKAMAI_HOST_PATTERN = Regex("upos-[a-z0-9-]+-mirrorakam\\.akamaized\\.net")
private const val DOUYIN_PLATFORM_ID = "douyin"
private const val ZUIYOU_PLATFORM_ID = "zuiyou"
private val DOUYIN_TRUNCATION_NOTICE_PATTERN = Regex("\\s*[.…。]{2,}\\s*版本过低，升级后可展示全部信息.*$")
private val COVER_URL_KEYS = listOf(
    "cover",
    "cover_url",
    "coverUrl",
    "thumbnail",
    "thumbnail_url",
    "thumbnailUrl",
    "poster",
    "poster_url",
    "image_url"
)
private val COVER_FALLBACK_KEYS = listOf("images", "image_list", "image_urls", "pictures")
private val COVER_OBJECT_URL_KEYS = listOf("url", "image", "src", "uri", "url_list", "urlList")
