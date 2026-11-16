package com.example.data.sources

import com.example.data.LiveChannel
import com.example.data.M3uParser
import com.example.data.SportCategory
import com.example.data.abstraction.ChannelDirectorySource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class IptvChannelDirectorySource(
    private val httpClient: OkHttpClient,
    private val cacheDir: File? = null
) : ChannelDirectorySource {

    @Volatile
    private var memoryCache: List<LiveChannel> = emptyList()

    @Volatile
    private var lastFetchMs: Long = 0L

    private val snapshotFile: File?
        get() = cacheDir?.let { File(it, SNAPSHOT_FILE_NAME) }

    override fun getCachedChannels(): List<LiveChannel> {
        if (memoryCache.isNotEmpty()) return memoryCache
        val disk = readDiskSnapshot()
        if (disk.isNotEmpty()) {
            memoryCache = disk
        }
        return disk
    }

    override suspend fun fetchChannels(forceRefresh: Boolean): Result<List<LiveChannel>> =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            if (!forceRefresh && memoryCache.isNotEmpty() && (now - lastFetchMs) < MEMORY_TTL_MS) {
                return@withContext Result.success(memoryCache)
            }

            runCatching {
                val (globalChannels, indianChannels) = supervisorScope {
                    val globalDeferred = async {
                        runCatching {
                            val m3u = downloadM3u(IPTV_SPORTS_M3U_URL, forceRefresh)
                            M3uParser.parse(m3u, filterSportsOnly = false, markAllAsIndian = false)
                        }
                    }
                    val indiaDeferred = async {
                        runCatching {
                            val m3u = downloadM3u(IPTV_INDIA_M3U_URL, forceRefresh)
                            M3uParser.parse(m3u, filterSportsOnly = true, markAllAsIndian = true)
                        }
                    }
                    Pair(globalDeferred.await(), indiaDeferred.await())
                }

                val globalList = globalChannels.getOrDefault(emptyList())
                val indiaList = indianChannels.getOrDefault(emptyList())

                if (globalList.isEmpty() && indiaList.isEmpty()) {
                    throw (globalChannels.exceptionOrNull()
                        ?: indianChannels.exceptionOrNull()
                        ?: IllegalStateException("Unable to load live sports channels"))
                }

                // Merge & deduplicate by streamUrl, preserving Indian origin flag if present in either feed
                val byUrl = LinkedHashMap<String, LiveChannel>()
                indiaList.forEach { ch ->
                    byUrl[ch.streamUrl] = ch
                }
                globalList.forEach { ch ->
                    val existing = byUrl[ch.streamUrl]
                    if (existing == null) {
                        byUrl[ch.streamUrl] = ch
                    } else if (!existing.isIndianOrigin && ch.isIndianOrigin) {
                        byUrl[ch.streamUrl] = existing.copy(isIndianOrigin = true)
                    }
                }

                val merged = M3uParser.sortChannels(byUrl.values.toList())
                memoryCache = merged
                lastFetchMs = System.currentTimeMillis()
                writeDiskSnapshot(merged)
                merged
            }
        }

    private fun downloadM3u(url: String, forceRefresh: Boolean): String {
        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", "PulseArena/1.0 (Android)")

        if (forceRefresh) {
            requestBuilder.cacheControl(
                CacheControl.Builder().maxAge(0, TimeUnit.SECONDS).build()
            )
        }

        httpClient.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                error("Stream directory returned HTTP ${response.code}")
            }
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) {
                error("Empty stream directory response")
            }
            return body
        }
    }

    private fun writeDiskSnapshot(channels: List<LiveChannel>) {
        val file = snapshotFile ?: return
        runCatching {
            val arr = JSONArray()
            channels.take(MAX_DISK_SNAPSHOT_ITEMS).forEach { ch ->
                arr.put(
                    JSONObject().apply {
                        put("id", ch.id)
                        put("name", ch.name)
                        put("streamUrl", ch.streamUrl)
                        if (ch.logoUrl != null) put("logoUrl", ch.logoUrl)
                        put("groupTitle", ch.groupTitle)
                        put("sport", ch.sport.name)
                        if (ch.userAgent != null) put("userAgent", ch.userAgent)
                        if (ch.referrer != null) put("referrer", ch.referrer)
                        put("isHttps", ch.isHttps)
                        put("isGeoRestrictedHint", ch.isGeoRestrictedHint)
                        put("qualityLabel", ch.qualityLabel)
                        put("isIndianOrigin", ch.isIndianOrigin)
                    }
                )
            }
            file.writeText(arr.toString())
        }
    }

    private fun readDiskSnapshot(): List<LiveChannel> {
        val file = snapshotFile ?: return emptyList()
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(file.readText())
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val streamUrl = obj.optString("streamUrl").takeIf { it.isNotBlank() } ?: continue
                    val sport = SportCategory.entries.firstOrNull { it.name == obj.optString("sport") }
                        ?: SportCategory.ALL
                    add(
                        LiveChannel(
                            id = obj.optString("id"),
                            name = obj.optString("name"),
                            streamUrl = streamUrl,
                            logoUrl = obj.optString("logoUrl").takeIf { it.isNotBlank() },
                            groupTitle = obj.optString("groupTitle", "Sports"),
                            sport = sport,
                            userAgent = obj.optString("userAgent").takeIf { it.isNotBlank() },
                            referrer = obj.optString("referrer").takeIf { it.isNotBlank() },
                            isHttps = obj.optBoolean("isHttps", streamUrl.startsWith("https://", true)),
                            isGeoRestrictedHint = obj.optBoolean("isGeoRestrictedHint", false),
                            qualityLabel = obj.optString("qualityLabel", "LIVE"),
                            isIndianOrigin = obj.optBoolean("isIndianOrigin", false)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val IPTV_SPORTS_M3U_URL = "https://iptv-org.github.io/iptv/categories/sports.m3u"
        const val IPTV_INDIA_M3U_URL = "https://iptv-org.github.io/iptv/countries/in.m3u"
        const val SNAPSHOT_FILE_NAME = "channels_snapshot_v2.json"
        const val MEMORY_TTL_MS = 3 * 60 * 1000L
        const val MAX_DISK_SNAPSHOT_ITEMS = 150
    }
}
