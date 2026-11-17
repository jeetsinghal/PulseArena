package com.example.data.abstraction

import com.example.data.EventSummaryDetail
import com.example.data.LiveChannel
import com.example.data.SportCategory
import com.example.data.SportEvent
import com.example.data.TranslationLanguage
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

interface ChannelDirectorySource {
    fun getCachedChannels(): List<LiveChannel> = emptyList()
    suspend fun fetchChannels(forceRefresh: Boolean = false): Result<List<LiveChannel>>
    suspend fun fetchPlaylist(playlistUrl: String): Result<List<LiveChannel>> = Result.failure(UnsupportedOperationException())
}

interface ScoreboardDataSource {
    fun getCachedEvents(category: SportCategory): List<SportEvent> = emptyList()
    suspend fun fetchEvents(category: SportCategory, forceRefresh: Boolean = false): Result<List<SportEvent>>
    suspend fun fetchEventSummary(sport: SportCategory, eventId: String): EventSummaryDetail?
}

interface LiveCaptionTranslator {
    suspend fun translateBatch(
        lines: List<String>,
        targetLanguage: TranslationLanguage
    ): Result<List<String>>
}

interface UserPreferencesStore {
    val themeMode: StateFlow<ThemeMode>
    val autoSwitchOnError: StateFlow<Boolean>
    val preferHttpsOnly: StateFlow<Boolean>
    val showScoreHudOverlay: StateFlow<Boolean>
    val customChannels: StateFlow<List<LiveChannel>>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun toggleDarkLight(currentlyDark: Boolean)
    suspend fun setAutoSwitchOnError(enabled: Boolean)
    suspend fun setPreferHttpsOnly(enabled: Boolean)
    suspend fun setShowScoreHudOverlay(enabled: Boolean)
    suspend fun addCustomChannel(channel: LiveChannel)
    suspend fun addCustomChannels(channels: List<LiveChannel>)
}

sealed interface CustomImportOutcome {
    data class SingleStream(val channel: LiveChannel) : CustomImportOutcome
    data class PlaylistImported(val channels: List<LiveChannel>) : CustomImportOutcome
}

interface SportsDataRepository {
    val customChannels: StateFlow<List<LiveChannel>>
    val autoSwitchOnError: StateFlow<Boolean>
    val preferHttpsOnly: StateFlow<Boolean>
    val showScoreHudOverlay: StateFlow<Boolean>
    val themeMode: StateFlow<ThemeMode>
    val backupStreams: List<LiveChannel>

    fun getCachedLiveChannels(): List<LiveChannel>
    fun getCachedScoreboard(category: SportCategory): List<SportEvent>
    suspend fun getLiveChannels(forceRefresh: Boolean = false): Result<List<LiveChannel>>
    suspend fun getScoreboard(category: SportCategory, forceRefresh: Boolean = false): Result<List<SportEvent>>
    suspend fun getEventSummary(sport: SportCategory, eventId: String): EventSummaryDetail?
    suspend fun addCustomStream(
        name: String,
        url: String,
        sport: SportCategory = SportCategory.ALL
    ): Result<LiveChannel>
    suspend fun importCustomStreamOrPlaylist(
        name: String,
        url: String,
        sport: SportCategory = SportCategory.ALL
    ): Result<CustomImportOutcome>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun toggleDarkLight(currentlyDark: Boolean)
    suspend fun setAutoSwitchOnError(enabled: Boolean)
    suspend fun setPreferHttpsOnly(enabled: Boolean)
    suspend fun setShowScoreHudOverlay(enabled: Boolean)
}
