package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChannelCategory
import com.example.data.ChannelFeedFilter
import com.example.data.EventStatusState
import com.example.data.EventSummaryDetail
import com.example.data.LiveChannel
import com.example.data.SportCategory
import com.example.data.SportEvent
import com.example.data.TranslationLanguage
import com.example.data.UiState
import com.example.data.abstraction.CustomImportOutcome
import com.example.data.abstraction.LiveCaptionTranslator
import com.example.data.abstraction.SportsDataRepository
import com.example.data.abstraction.ThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SportsViewModel(
    private val repository: SportsDataRepository,
    private val translator: LiveCaptionTranslator
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = repository.themeMode
    val autoSwitchOnError: StateFlow<Boolean> = repository.autoSwitchOnError
    val preferHttpsOnly: StateFlow<Boolean> = repository.preferHttpsOnly
    val showScoreHudOverlay: StateFlow<Boolean> = repository.showScoreHudOverlay
    val customChannels: StateFlow<List<LiveChannel>> = repository.customChannels
    val backupStreams: List<LiveChannel> get() = repository.backupStreams

    private val _channelsState = MutableStateFlow<UiState<List<LiveChannel>>>(UiState.Loading)
    val channelsState: StateFlow<UiState<List<LiveChannel>>> = _channelsState.asStateFlow()

    private val _isRefreshingChannels = MutableStateFlow(false)
    val isRefreshingChannels: StateFlow<Boolean> = _isRefreshingChannels.asStateFlow()

    private val _eventsState = MutableStateFlow<UiState<List<SportEvent>>>(UiState.Loading)
    val eventsState: StateFlow<UiState<List<SportEvent>>> = _eventsState.asStateFlow()

    private val _isRefreshingScores = MutableStateFlow(false)
    val isRefreshingScores: StateFlow<Boolean> = _isRefreshingScores.asStateFlow()

    private val _selectedChannelCategory = MutableStateFlow(ChannelCategory.ALL)
    val selectedChannelCategory: StateFlow<ChannelCategory> = _selectedChannelCategory.asStateFlow()

    private val _selectedChannelFilter = MutableStateFlow(ChannelFeedFilter.ALL)
    val selectedChannelFilter: StateFlow<ChannelFeedFilter> = _selectedChannelFilter.asStateFlow()

    private val _selectedScoreSport = MutableStateFlow(SportCategory.ALL)
    val selectedScoreSport: StateFlow<SportCategory> = _selectedScoreSport.asStateFlow()

    private val _channelSearchQuery = MutableStateFlow("")
    val channelSearchQuery: StateFlow<String> = _channelSearchQuery.asStateFlow()

    private val _activeChannel = MutableStateFlow<LiveChannel?>(null)
    val activeChannel: StateFlow<LiveChannel?> = _activeChannel.asStateFlow()

    private val _activeEvent = MutableStateFlow<SportEvent?>(null)
    val activeEvent: StateFlow<SportEvent?> = _activeEvent.asStateFlow()

    private val _activeEventSummary = MutableStateFlow<EventSummaryDetail?>(null)
    val activeEventSummary: StateFlow<EventSummaryDetail?> = _activeEventSummary.asStateFlow()

    private val _captionsEnabled = MutableStateFlow(true)
    val captionsEnabled: StateFlow<Boolean> = _captionsEnabled.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(TranslationLanguage.ORIGINAL)
    val selectedLanguage: StateFlow<TranslationLanguage> = _selectedLanguage.asStateFlow()

    private val _translatedCaption = MutableStateFlow<String?>(null)
    val translatedCaption: StateFlow<String?> = _translatedCaption.asStateFlow()

    private val _translatedPlays = MutableStateFlow<List<String>>(emptyList())
    val translatedPlays: StateFlow<List<String>> = _translatedPlays.asStateFlow()

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    private val _translationError = MutableStateFlow<String?>(null)
    val translationError: StateFlow<String?> = _translationError.asStateFlow()

    private var translationJob: Job? = null
    private var channelsLoadJob: Job? = null
    private var scoresLoadJob: Job? = null

    init {
        val instantChannels = repository.getCachedLiveChannels()
        if (instantChannels.isNotEmpty()) {
            _channelsState.value = UiState.Success(instantChannels)
        }
        loadChannels(forceRefresh = false)
        loadScores(SportCategory.ALL, forceRefresh = false)
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun toggleDarkLight(currentlyDark: Boolean) {
        viewModelScope.launch { repository.toggleDarkLight(currentlyDark) }
    }

    fun setAutoSwitchOnError(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoSwitchOnError(enabled) }
    }

    fun setPreferHttpsOnly(enabled: Boolean) {
        viewModelScope.launch { repository.setPreferHttpsOnly(enabled) }
    }

    fun setShowScoreHudOverlay(enabled: Boolean) {
        viewModelScope.launch { repository.setShowScoreHudOverlay(enabled) }
    }

    fun loadChannels(forceRefresh: Boolean = true) {
        channelsLoadJob?.cancel()
        channelsLoadJob = viewModelScope.launch {
            val hasVisibleData = _channelsState.value is UiState.Success
            if (hasVisibleData) {
                _isRefreshingChannels.value = true
            } else {
                _channelsState.value = UiState.Loading
            }

            val result = repository.getLiveChannels(forceRefresh = forceRefresh)
            _isRefreshingChannels.value = false
            result.fold(
                onSuccess = { list ->
                    _channelsState.value = UiState.Success(list)
                },
                onFailure = { err ->
                    if (!hasVisibleData) {
                        _channelsState.value = UiState.Error(err.message ?: "Unable to load live sports channels")
                    }
                }
            )
        }
    }

    fun loadScores(
        category: SportCategory = _selectedScoreSport.value,
        forceRefresh: Boolean = false
    ) {
        _selectedScoreSport.value = category
        scoresLoadJob?.cancel()
        scoresLoadJob = viewModelScope.launch {
            val cached = repository.getCachedScoreboard(category)
            val hasData = cached.isNotEmpty()
            if (hasData) {
                _eventsState.value = UiState.Success(cached)
                _isRefreshingScores.value = true
            } else if (_eventsState.value !is UiState.Success) {
                _eventsState.value = UiState.Loading
            } else {
                _isRefreshingScores.value = true
            }

            val result = repository.getScoreboard(category, forceRefresh = forceRefresh)
            _isRefreshingScores.value = false
            result.fold(
                onSuccess = { list ->
                    _eventsState.value = UiState.Success(list)
                },
                onFailure = { err ->
                    if (_eventsState.value !is UiState.Success) {
                        _eventsState.value = UiState.Error(err.message ?: "Unable to fetch live scores")
                    }
                }
            )
        }
    }

    fun refreshActiveEvent() {
        val currentEvent = _activeEvent.value ?: return
        viewModelScope.launch {
            repository.getScoreboard(currentEvent.sport, forceRefresh = true).onSuccess { events ->
                events.firstOrNull { it.id == currentEvent.id }?.let { updated ->
                    _activeEvent.value = updated
                }
            }
            _activeEventSummary.value = repository.getEventSummary(currentEvent.sport, currentEvent.id)
            translateActiveContent()
        }
    }

    fun setChannelCategory(category: ChannelCategory) {
        _selectedChannelCategory.value = category
    }

    fun setChannelFeedFilter(filter: ChannelFeedFilter) {
        _selectedChannelFilter.value = filter
    }

    fun setChannelSearchQuery(query: String) {
        _channelSearchQuery.value = query
    }

    fun toggleCaptions() {
        _captionsEnabled.value = !_captionsEnabled.value
    }

    fun setCaptionsEnabled(enabled: Boolean) {
        _captionsEnabled.value = enabled
    }

    fun selectTranslationLanguage(language: TranslationLanguage) {
        _selectedLanguage.value = language
        _translationError.value = null
        translateActiveContent()
    }

    fun retryTranslation() {
        _translationError.value = null
        translateActiveContent()
    }

    fun openChannelInPlayer(channel: LiveChannel) {
        _activeChannel.value = channel
        val liveEvents = (_eventsState.value as? UiState.Success)?.data.orEmpty()
        val matchingLive = liveEvents.firstOrNull {
            it.statusState == EventStatusState.LIVE && it.sport == channel.sport
        } ?: liveEvents.firstOrNull {
            it.statusState == EventStatusState.LIVE
        }
        _activeEvent.value = matchingLive
        _activeEventSummary.value = null
        _translatedCaption.value = null
        _translatedPlays.value = emptyList()

        if (matchingLive != null) {
            viewModelScope.launch {
                _activeEventSummary.value = repository.getEventSummary(matchingLive.sport, matchingLive.id)
                translateActiveContent()
            }
        } else {
            translateActiveContent()
        }
    }

    fun openEventInPlayer(event: SportEvent) {
        _activeEvent.value = event
        _activeEventSummary.value = null
        _translatedCaption.value = null
        _translatedPlays.value = emptyList()

        val availableChannels = (_channelsState.value as? UiState.Success)?.data.orEmpty()
        val matchingChannel = availableChannels.firstOrNull {
            !it.isGeoRestrictedHint && it.isHttps && it.sport == event.sport
        } ?: availableChannels.firstOrNull {
            !it.isGeoRestrictedHint && it.sport == event.sport
        } ?: availableChannels.firstOrNull {
            !it.isGeoRestrictedHint && it.isHttps
        } ?: availableChannels.firstOrNull()
            ?: repository.backupStreams.first()

        _activeChannel.value = matchingChannel

        viewModelScope.launch {
            _activeEventSummary.value = repository.getEventSummary(event.sport, event.id)
            translateActiveContent()
        }
    }

    fun attachEventToPlayer(event: SportEvent) {
        _activeEvent.value = event
        _activeEventSummary.value = null
        _translatedCaption.value = null
        _translatedPlays.value = emptyList()
        viewModelScope.launch {
            _activeEventSummary.value = repository.getEventSummary(event.sport, event.id)
            translateActiveContent()
        }
    }

    fun switchToChannel(channel: LiveChannel) {
        _activeChannel.value = channel
        translateActiveContent()
    }

    fun switchToNextChannel(): LiveChannel? {
        val list = (_channelsState.value as? UiState.Success)?.data.orEmpty()
        if (list.isEmpty()) return null
        val current = _activeChannel.value ?: return list.first()

        val sameCategory = list.filter {
            it.channelCategory == current.channelCategory && !it.isGeoRestrictedHint
        }
        val pool = if (sameCategory.size > 1) sameCategory else list.filter { !it.isGeoRestrictedHint }.ifEmpty { list }
        val idx = pool.indexOfFirst { it.id == current.id }
        val next = if (idx == -1 || idx + 1 >= pool.size) pool.first() else pool[idx + 1]
        _activeChannel.value = next
        translateActiveContent()
        return next
    }

    fun playCustomUrl(
        name: String,
        url: String,
        onPlaySingle: () -> Unit,
        onPlaylistImported: (Int) -> Unit,
        onValidationError: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.importCustomStreamOrPlaylist(name, url).onSuccess { outcome ->
                when (outcome) {
                    is CustomImportOutcome.SingleStream -> {
                        _activeChannel.value = outcome.channel
                        _activeEvent.value = null
                        _activeEventSummary.value = null
                        loadChannels(forceRefresh = false)
                        onPlaySingle()
                    }
                    is CustomImportOutcome.PlaylistImported -> {
                        val existing = (_channelsState.value as? UiState.Success)?.data.orEmpty()
                        val merged = (outcome.channels + existing).distinctBy { it.streamUrl }
                        _channelsState.value = UiState.Success(merged)
                        onPlaylistImported(outcome.channels.size)
                    }
                }
            }.onFailure { err ->
                onValidationError(err.message ?: "Invalid stream or playlist URL")
            }
        }
    }

    private fun translateActiveContent() {
        val lang = _selectedLanguage.value
        val event = _activeEvent.value
        val channel = _activeChannel.value
        val summary = _activeEventSummary.value

        val rawCaption = buildRawCaption(event, summary, channel)
        val rawPlays = summary?.recentPlays.orEmpty()

        if (lang == TranslationLanguage.ORIGINAL) {
            _translatedCaption.value = rawCaption
            _translatedPlays.value = rawPlays
            _isTranslating.value = false
            _translationError.value = null
            return
        }

        val batch = buildList {
            if (!rawCaption.isNullOrBlank()) add(rawCaption)
            addAll(rawPlays)
        }
        if (batch.isEmpty()) return

        translationJob?.cancel()
        translationJob = viewModelScope.launch {
            _isTranslating.value = true
            _translationError.value = null
            val result = translator.translateBatch(batch, lang)
            _isTranslating.value = false
            result.onSuccess { translated ->
                if (!rawCaption.isNullOrBlank()) {
                    _translatedCaption.value = translated.firstOrNull()
                    _translatedPlays.value = translated.drop(1)
                } else {
                    _translatedCaption.value = null
                    _translatedPlays.value = translated
                }
            }.onFailure { err ->
                _translationError.value = err.message ?: "Unable to translate captions."
                _translatedCaption.value = rawCaption
                _translatedPlays.value = rawPlays
            }
        }
    }

    private fun buildRawCaption(
        event: SportEvent?,
        summary: EventSummaryDetail?,
        channel: LiveChannel?
    ): String? {
        if (event == null) {
            return channel?.let {
                val tag = if (it.sport != SportCategory.ALL) it.sport.displayName else it.channelCategory.displayName
                "Live broadcast: ${it.name} ($tag)"
            }
        }
        val latestPlay = summary?.recentPlays?.firstOrNull()
        if (!latestPlay.isNullOrBlank()) return latestPlay
        val liveLine = event.liveCaption
        return if (!liveLine.isNullOrBlank()) {
            "${event.awayAbbr} ${event.awayScore} - ${event.homeAbbr} ${event.homeScore} • $liveLine"
        } else {
            "${event.awayTeam} ${event.awayScore} vs ${event.homeTeam} ${event.homeScore} (${event.statusDetail})"
        }
    }
}
