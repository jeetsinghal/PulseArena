package com.example.data

import com.example.data.abstraction.ChannelDirectorySource
import com.example.data.abstraction.CustomImportOutcome
import com.example.data.abstraction.ScoreboardDataSource
import com.example.data.abstraction.SportsDataRepository
import com.example.data.abstraction.ThemeMode
import com.example.data.abstraction.UserPreferencesStore
import com.example.data.security.UrlSecurityValidator
import kotlinx.coroutines.flow.StateFlow

class DefaultSportsDataRepository(
    private val channelSource: ChannelDirectorySource,
    private val scoreboardSource: ScoreboardDataSource,
    private val preferencesStore: UserPreferencesStore
) : SportsDataRepository {

    override val customChannels: StateFlow<List<LiveChannel>> = preferencesStore.customChannels
    override val autoSwitchOnError: StateFlow<Boolean> = preferencesStore.autoSwitchOnError
    override val preferHttpsOnly: StateFlow<Boolean> = preferencesStore.preferHttpsOnly
    override val showScoreHudOverlay: StateFlow<Boolean> = preferencesStore.showScoreHudOverlay
    override val themeMode: StateFlow<ThemeMode> = preferencesStore.themeMode

    override val backupStreams: List<LiveChannel> = listOf(
        LiveChannel(
            id = "backup_mux",
            name = "Backup ABR Test Feed (1080p)",
            streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            logoUrl = null,
            groupTitle = "Backup Stream",
            sport = SportCategory.ALL,
            isHttps = true,
            isBackupTestStream = true,
            qualityLabel = "1080p"
        ),
        LiveChannel(
            id = "backup_apple",
            name = "Backup Multi-Audio HLS (fMP4)",
            streamUrl = "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
            logoUrl = null,
            groupTitle = "Backup Stream",
            sport = SportCategory.ALL,
            isHttps = true,
            isBackupTestStream = true,
            qualityLabel = "Multi-Audio"
        )
    )

    override fun getCachedLiveChannels(): List<LiveChannel> {
        val cached = channelSource.getCachedChannels()
        return if (cached.isEmpty()) emptyList() else preferencesStore.customChannels.value + cached
    }

    override fun getCachedScoreboard(category: SportCategory): List<SportEvent> {
        return scoreboardSource.getCachedEvents(category)
    }

    override suspend fun getLiveChannels(forceRefresh: Boolean): Result<List<LiveChannel>> {
        return channelSource.fetchChannels(forceRefresh).map { fetched ->
            preferencesStore.customChannels.value + fetched
        }
    }

    override suspend fun getScoreboard(
        category: SportCategory,
        forceRefresh: Boolean
    ): Result<List<SportEvent>> {
        return scoreboardSource.fetchEvents(category, forceRefresh)
    }

    override suspend fun getEventSummary(sport: SportCategory, eventId: String): EventSummaryDetail? {
        return scoreboardSource.fetchEventSummary(sport, eventId)
    }

    override suspend fun addCustomStream(
        name: String,
        url: String,
        sport: SportCategory
    ): Result<LiveChannel> {
        val validated = UrlSecurityValidator.validateStreamUrl(url)
        return validated.map { safeUrl ->
            val channel = LiveChannel(
                id = "custom_${System.currentTimeMillis()}",
                name = name.trim().ifBlank { "Custom Live Stream" },
                streamUrl = safeUrl,
                logoUrl = null,
                groupTitle = "Custom URL",
                sport = sport,
                isHttps = safeUrl.startsWith("https://", ignoreCase = true),
                qualityLabel = "CUSTOM",
                channelCategory = if (sport != SportCategory.ALL) ChannelCategory.SPORTS else M3uParser.classifyChannelCategory(
                    name = name,
                    group = "Custom URL",
                    streamUrl = safeUrl,
                    sport = sport
                )
            )
            preferencesStore.addCustomChannel(channel)
            channel
        }
    }

    override suspend fun importCustomStreamOrPlaylist(
        name: String,
        url: String,
        sport: SportCategory
    ): Result<CustomImportOutcome> {
        val safeUrl = UrlSecurityValidator.validateStreamUrl(url).getOrElse {
            return Result.failure(it)
        }

        val cleanPath = safeUrl.substringBefore('?').substringBefore('#').lowercase()
        val looksLikePlaylist = cleanPath.endsWith(".m3u") && !cleanPath.endsWith(".m3u8")

        if (looksLikePlaylist) {
            val playlistResult = channelSource.fetchPlaylist(safeUrl)
            val parsed = playlistResult.getOrElse { err ->
                return Result.failure(err)
            }.filter { ch ->
                UrlSecurityValidator.validateStreamUrl(ch.streamUrl).isSuccess
            }
            if (parsed.isEmpty()) {
                return Result.failure(IllegalArgumentException("No playable channels found in this .m3u playlist."))
            }
            preferencesStore.addCustomChannels(parsed)
            return Result.success(CustomImportOutcome.PlaylistImported(parsed))
        }

        return addCustomStream(name = name, url = safeUrl, sport = sport)
            .map { CustomImportOutcome.SingleStream(it) }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        preferencesStore.setThemeMode(mode)
    }

    override suspend fun toggleDarkLight(currentlyDark: Boolean) {
        preferencesStore.toggleDarkLight(currentlyDark)
    }

    override suspend fun setAutoSwitchOnError(enabled: Boolean) {
        preferencesStore.setAutoSwitchOnError(enabled)
    }

    override suspend fun setPreferHttpsOnly(enabled: Boolean) {
        preferencesStore.setPreferHttpsOnly(enabled)
    }

    override suspend fun setShowScoreHudOverlay(enabled: Boolean) {
        preferencesStore.setShowScoreHudOverlay(enabled)
    }
}
