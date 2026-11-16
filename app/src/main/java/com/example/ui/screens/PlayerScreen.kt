package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.ui.PlayerView
import com.example.data.AudioTrackOption
import com.example.data.EventStatusState
import com.example.data.SubtitleTrackOption
import com.example.data.TranslationLanguage
import com.example.data.UiState
import com.example.data.VideoQualityOption
import com.example.ui.components.LiveSignalBars
import com.example.ui.components.ScoreComparisonBar
import com.example.ui.components.SportVisualBadge
import com.example.ui.components.sportAccentColor
import com.example.ui.theme.arenaColors
import com.example.ui.viewmodel.SportsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Composable
fun PlayerScreen(
    viewModel: SportsViewModel,
    onBack: () -> Unit
) {
    val arena = MaterialTheme.arenaColors
    val context = LocalContext.current
    val activity = context as? Activity

    val activeChannel by viewModel.activeChannel.collectAsStateWithLifecycle()
    val activeEvent by viewModel.activeEvent.collectAsStateWithLifecycle()
    val eventSummary by viewModel.activeEventSummary.collectAsStateWithLifecycle()
    val channelsState by viewModel.channelsState.collectAsStateWithLifecycle()
    val eventsState by viewModel.eventsState.collectAsStateWithLifecycle()

    val captionsEnabled by viewModel.captionsEnabled.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val translatedCaption by viewModel.translatedCaption.collectAsStateWithLifecycle()
    val translatedPlays by viewModel.translatedPlays.collectAsStateWithLifecycle()
    val isTranslating by viewModel.isTranslating.collectAsStateWithLifecycle()
    val translationError by viewModel.translationError.collectAsStateWithLifecycle()

    val autoSwitchOnError by viewModel.autoSwitchOnError.collectAsStateWithLifecycle()
    val showScoreHud by viewModel.showScoreHudOverlay.collectAsStateWithLifecycle()

    val channel = activeChannel ?: viewModel.backupStreams.first()

    var isFullscreen by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var playbackStatus by remember { mutableStateOf("CONNECTING") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var audioWarning by remember { mutableStateOf<String?>(null) }
    var retriedAsHls by remember(channel.id) { mutableStateOf(false) }
    var reloadTrigger by remember(channel.id) { mutableIntStateOf(0) }
    var autoHopCount by remember { mutableIntStateOf(0) }

    var showQualitySheet by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var selectedQualityId by remember { mutableStateOf("auto") }
    var activeResolutionLabel by remember { mutableStateOf("Auto") }
    var detectedQualities by remember { mutableStateOf<List<VideoQualityOption>>(emptyList()) }
    var detectedAudioTracks by remember { mutableStateOf<List<AudioTrackOption>>(emptyList()) }
    var detectedSubtitleTracks by remember { mutableStateOf<List<SubtitleTrackOption>>(emptyList()) }
    var currentTracksRef by remember { mutableStateOf<Tracks>(Tracks.EMPTY) }
    var isBehindLiveEdge by remember { mutableStateOf(false) }

    val standardQualities = remember {
        listOf(
            VideoQualityOption("auto", "Auto (Best available)", Int.MAX_VALUE, Int.MAX_VALUE),
            VideoQualityOption("1080p", "1080p HD", 1920, 1080),
            VideoQualityOption("720p", "720p HD", 1280, 720),
            VideoQualityOption("480p", "480p SD", 854, 480),
            VideoQualityOption("360p", "360p Data Saver", 640, 360)
        )
    }

    val handleExit: () -> Unit = {
        if (isFullscreen) {
            isFullscreen = false
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            onBack()
        }
    }

    BackHandler { handleExit() }

    val exoPlayer = remember {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 10_000,
                /* maxBufferMs = */ 30_000,
                /* bufferForPlaybackMs = */ 1_500,
                /* bufferForPlaybackAfterRebufferMs = */ 3_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

        val trackSelector = DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setAllowAudioMixedMimeTypeAdaptiveness(true)
                .setAllowAudioMixedSampleRateAdaptiveness(true)
                .setAllowAudioMixedChannelCountAdaptiveness(true)
                .setAllowAudioNonSeamlessAdaptiveness(true)
                .setExceedAudioConstraintsIfNecessary(true)
                .setExceedRendererCapabilitiesIfNecessary(true)
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                .build()
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                volume = 1f
            }
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(captionsEnabled) {
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !captionsEnabled)
            .build()
    }

    fun buildHttpFactory(userAgentOverride: String?, referrer: String?): DefaultHttpDataSource.Factory {
        val factory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(8_000)
            .setReadTimeoutMs(10_000)
            .setUserAgent(
                userAgentOverride
                    ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
            )
        referrer?.let { ref ->
            factory.setDefaultRequestProperties(mapOf("Referer" to ref))
        }
        return factory
    }

    LaunchedEffect(channel.id, channel.streamUrl, reloadTrigger) {
        errorMessage = null
        audioWarning = null
        playbackStatus = "CONNECTING"
        retriedAsHls = false
        detectedQualities = emptyList()
        detectedAudioTracks = emptyList()
        detectedSubtitleTracks = emptyList()

        // Reset temporary auto-fallback constraints when switching to a fresh channel
        if (selectedQualityId == "auto") {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .clearVideoSizeConstraints()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                .build()
        }

        val httpFactory = buildHttpFactory(channel.userAgent, channel.referrer)
        val mediaSourceFactory = DefaultMediaSourceFactory(httpFactory)
            .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(2))

        val mediaItem = MediaItem.Builder()
            .setUri(channel.streamUrl)
            .setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(5_000)
                    .setMinPlaybackSpeed(0.96f)
                    .setMaxPlaybackSpeed(1.04f)
                    .build()
            )
            .build()
        val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)

        exoPlayer.stop()
        exoPlayer.volume = if (isMuted) 0f else 1f
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    // Watchdog: prevent continuous endless buffering on stalled IPTV servers
    LaunchedEffect(playbackStatus, channel.id, reloadTrigger) {
        if (playbackStatus == "CONNECTING" || playbackStatus == "BUFFERING" || playbackStatus == "RETRYING") {
            delay(9_000)
            if (playbackStatus == "CONNECTING" || playbackStatus == "BUFFERING" || playbackStatus == "RETRYING") {
                if (selectedQualityId == "auto") {
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                        .buildUpon()
                        .setMaxVideoSize(854, 480)
                        .build()
                    activeResolutionLabel = "Auto (480p)"
                }
                if (exoPlayer.isCurrentMediaItemLive) {
                    exoPlayer.seekToDefaultPosition()
                }
            }

            delay(9_000)
            if (playbackStatus == "CONNECTING" || playbackStatus == "BUFFERING" || playbackStatus == "RETRYING") {
                if (autoSwitchOnError && autoHopCount < 3 && !channel.isBackupTestStream) {
                    autoHopCount += 1
                    playbackStatus = "SWITCHING"
                    viewModel.switchToNextChannel()
                } else {
                    exoPlayer.pause()
                    playbackStatus = "STALLED"
                    errorMessage = "This stream is buffering too slowly or is temporarily overloaded."
                }
            }
        }
    }

    DisposableEffect(exoPlayer, channel.id) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> if (errorMessage == null) playbackStatus = "BUFFERING"
                    Player.STATE_READY -> {
                        playbackStatus = "LIVE"
                        errorMessage = null
                        autoHopCount = 0
                    }
                    Player.STATE_ENDED -> playbackStatus = "ENDED"
                    Player.STATE_IDLE -> {}
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                currentTracksRef = tracks
                val qualities = mutableListOf<VideoQualityOption>()
                val audios = mutableListOf<AudioTrackOption>()
                val subtitles = mutableListOf<SubtitleTrackOption>()

                var activeHeight = 0
                var hasAnyAudioTrackInStream = false
                var hasSelectedSupportedAudio = false
                var unsupportedAudioCodecLabel: String? = null

                tracks.groups.forEachIndexed { groupIdx, group ->
                    when (group.type) {
                        C.TRACK_TYPE_VIDEO -> {
                            for (i in 0 until group.length) {
                                if (!group.isTrackSupported(i)) continue
                                val fmt = group.getTrackFormat(i)
                                if (group.isTrackSelected(i) && fmt.height > 0) {
                                    activeHeight = fmt.height
                                }
                                if (fmt.height > 0) {
                                    qualities.add(
                                        VideoQualityOption(
                                            id = "${fmt.height}p",
                                            label = "${fmt.height}p",
                                            maxWidth = fmt.width.coerceAtLeast(640),
                                            maxHeight = fmt.height
                                        )
                                    )
                                }
                            }
                        }

                        C.TRACK_TYPE_AUDIO -> {
                            for (i in 0 until group.length) {
                                hasAnyAudioTrackInStream = true
                                val fmt = group.getTrackFormat(i)
                                if (!group.isTrackSupported(i)) {
                                    if (unsupportedAudioCodecLabel == null) {
                                        unsupportedAudioCodecLabel = fmt.sampleMimeType ?: "unsupported"
                                    }
                                    continue
                                }
                                val isSel = group.isTrackSelected(i)
                                if (isSel) hasSelectedSupportedAudio = true
                                val langCode = fmt.language ?: "und"
                                val displayLang = if (langCode != "und") {
                                    Locale.forLanguageTag(langCode).getDisplayLanguage(Locale.ENGLISH)
                                        .replaceFirstChar { it.uppercase() }
                                } else {
                                    fmt.label ?: "Audio ${audios.size + 1}"
                                }
                                audios.add(
                                    AudioTrackOption(
                                        id = "aud_${groupIdx}_$i",
                                        label = fmt.label?.takeIf { it.isNotBlank() && it != displayLang }
                                            ?.let { "$displayLang ($it)" } ?: displayLang,
                                        languageCode = langCode,
                                        groupIndex = groupIdx,
                                        trackIndex = i,
                                        isSelected = isSel
                                    )
                                )
                            }
                        }

                        C.TRACK_TYPE_TEXT -> {
                            for (i in 0 until group.length) {
                                if (!group.isTrackSupported(i)) continue
                                val fmt = group.getTrackFormat(i)
                                val langCode = fmt.language ?: "und"
                                val displayLang = if (langCode != "und") {
                                    Locale.forLanguageTag(langCode).getDisplayLanguage(Locale.ENGLISH)
                                        .replaceFirstChar { it.uppercase() }
                                } else {
                                    fmt.label ?: "CC ${subtitles.size + 1}"
                                }
                                subtitles.add(
                                    SubtitleTrackOption(
                                        id = "sub_${groupIdx}_$i",
                                        label = displayLang,
                                        languageCode = langCode,
                                        groupIndex = groupIdx,
                                        trackIndex = i,
                                        isSelected = group.isTrackSelected(i)
                                    )
                                )
                            }
                        }
                    }
                }

                // Auto-select a supported audio track if stream defaulted to none or an unsupported track
                if (audios.isNotEmpty() && !hasSelectedSupportedAudio) {
                    val fallbackAudio = audios.first()
                    val groups = tracks.groups
                    if (fallbackAudio.groupIndex in groups.indices) {
                        val mediaGroup = groups[fallbackAudio.groupIndex].mediaTrackGroup
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                            .setOverrideForType(TrackSelectionOverride(mediaGroup, fallbackAudio.trackIndex))
                            .build()
                    }
                }

                audioWarning = if (hasAnyAudioTrackInStream && audios.isEmpty()) {
                    "Audio codec (${unsupportedAudioCodecLabel ?: "surround"}) unsupported on this device"
                } else {
                    null
                }

                detectedQualities = qualities.distinctBy { it.id }.sortedByDescending { it.maxHeight }
                detectedAudioTracks = audios
                detectedSubtitleTracks = subtitles
                activeResolutionLabel = when {
                    selectedQualityId != "auto" -> selectedQualityId
                    activeHeight > 0 -> "Auto (${activeHeight}p)"
                    else -> "Auto"
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (!retriedAsHls) {
                    retriedAsHls = true
                    playbackStatus = "RETRYING"
                    val httpFactory = buildHttpFactory(channel.userAgent, channel.referrer)
                    val hlsItem = MediaItem.Builder()
                        .setUri(channel.streamUrl)
                        .setMimeType(MimeTypes.APPLICATION_M3U8)
                        .setLiveConfiguration(
                            MediaItem.LiveConfiguration.Builder()
                                .setTargetOffsetMs(5_000)
                                .setMinPlaybackSpeed(0.96f)
                                .setMaxPlaybackSpeed(1.04f)
                                .build()
                        )
                        .build()
                    val mediaSource = DefaultMediaSourceFactory(httpFactory)
                        .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(2))
                        .createMediaSource(hlsItem)
                    exoPlayer.setMediaSource(mediaSource)
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                } else if (autoSwitchOnError && autoHopCount < 3 && !channel.isBackupTestStream) {
                    autoHopCount += 1
                    playbackStatus = "SWITCHING"
                    viewModel.switchToNextChannel()
                } else {
                    playbackStatus = "UNAVAILABLE"
                    errorMessage = "Feed unavailable or geo-blocked (${error.errorCodeName})."
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            exoPlayer.release()
        }
    }

    LaunchedEffect(activeEvent?.id) {
        while (isActive) {
            isBehindLiveEdge = exoPlayer.isCurrentMediaItemLive && exoPlayer.currentLiveOffset > 15_000L
            delay(3_000)
        }
    }

    LaunchedEffect(activeEvent?.id) {
        if (activeEvent != null) {
            while (isActive) {
                delay(15_000)
                viewModel.refreshActiveEvent()
            }
        }
    }

    val allChannels = (channelsState as? UiState.Success)?.data ?: viewModel.backupStreams
    val relatedChannels = remember(allChannels, activeEvent?.sport, channel.sport) {
        val targetSport = activeEvent?.sport ?: channel.sport
        val matching = allChannels.filter { it.sport == targetSport && !it.isGeoRestrictedHint }
        val rest = allChannels.filter { it.sport != targetSport && !it.isGeoRestrictedHint }
        (matching + rest).take(25)
    }

    val liveMatchesToAttach = remember(eventsState) {
        ((eventsState as? UiState.Success)?.data ?: emptyList())
            .filter { it.statusState == EventStatusState.LIVE }
            .take(8)
    }

    Scaffold(
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = activeEvent?.shortName ?: channel.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = arena.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (activeEvent != null) channel.name else channel.sport.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = arena.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = handleExit, modifier = Modifier.testTag("player_back_button")) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = arena.textPrimary
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.toggleDarkLight(arena.isDark) },
                            modifier = Modifier.testTag("player_theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (arena.isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (arena.isDark) "Switch to light mode" else "Switch to dark mode",
                                tint = arena.textPrimary
                            )
                        }
                        IconButton(
                            onClick = {
                                autoHopCount = 0
                                viewModel.switchToNextChannel()
                            },
                            modifier = Modifier.testTag("next_channel_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Channel",
                                tint = arena.textPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = arena.background)
                )
            }
        },
        containerColor = arena.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullscreen) PaddingValues(0.dp) else innerPadding)
        ) {
            // Video Player Viewport (stays black for broadcast clarity)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isFullscreen) Modifier.fillMaxSize() else Modifier.height(236.dp))
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            keepScreenOn = true
                            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                            subtitleView?.visibility = if (captionsEnabled) View.VISIBLE else View.GONE
                        }
                    },
                    update = { view ->
                        if (view.player !== exoPlayer) {
                            view.player = exoPlayer
                        }
                        view.subtitleView?.visibility = if (captionsEnabled) View.VISIBLE else View.GONE
                    },
                    modifier = Modifier.fillMaxSize()
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            isBehindLiveEdge -> Color(0xE61A202C)
                            playbackStatus == "LIVE" -> arena.liveRed
                            else -> Color.Black.copy(alpha = 0.75f)
                        },
                        modifier = Modifier.clickable(enabled = isBehindLiveEdge) {
                            exoPlayer.seekToDefaultPosition()
                            isBehindLiveEdge = false
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (playbackStatus == "LIVE" && !isBehindLiveEdge) {
                                LiveSignalBars(
                                    color = Color.White,
                                    modifier = Modifier.size(width = 11.dp, height = 9.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (isBehindLiveEdge) arena.goldAccent else Color.White,
                                            CircleShape
                                        )
                                )
                            }
                            Text(
                                text = if (isBehindLiveEdge) "GO LIVE" else playbackStatus,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.72f),
                            modifier = Modifier
                                .clickable { showQualitySheet = true }
                                .testTag("quality_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Video quality",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (selectedQualityId == "auto") "Auto" else selectedQualityId,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (captionsEnabled) arena.accentGreen else Color.Black.copy(alpha = 0.72f),
                            modifier = Modifier
                                .clickable { viewModel.toggleCaptions() }
                                .testTag("captions_toggle_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (captionsEnabled) Icons.Default.ClosedCaption else Icons.Default.ClosedCaptionDisabled,
                                    contentDescription = "Toggle Captions",
                                    tint = if (captionsEnabled) arena.onAccentGreen else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "CC",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (captionsEnabled) arena.onAccentGreen else Color.White
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedLanguage != TranslationLanguage.ORIGINAL) arena.accentGreen else Color.Black.copy(alpha = 0.72f),
                            modifier = Modifier
                                .clickable { showLanguageSheet = true }
                                .testTag("language_translate_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = "Language and Translation",
                                    tint = if (selectedLanguage != TranslationLanguage.ORIGINAL) arena.onAccentGreen else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = selectedLanguage.code.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selectedLanguage != TranslationLanguage.ORIGINAL) arena.onAccentGreen else Color.White
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isMuted) arena.liveRed else Color.Black.copy(alpha = 0.72f),
                            modifier = Modifier
                                .clickable { isMuted = !isMuted }
                                .testTag("mute_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (isMuted) "Unmute Audio" else "Mute Audio",
                                tint = Color.White,
                                modifier = Modifier.padding(6.dp).size(18.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.72f),
                            modifier = Modifier
                                .clickable {
                                    isFullscreen = !isFullscreen
                                    activity?.requestedOrientation = if (isFullscreen) {
                                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                    }
                                }
                                .testTag("fullscreen_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.padding(6.dp).size(18.dp)
                            )
                        }
                    }
                }

                if (audioWarning != null && errorMessage == null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.78f),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 44.dp, start = 12.dp, end = 12.dp)
                    ) {
                        Text(
                            text = audioWarning.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = arena.goldAccent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                if (playbackStatus == "CONNECTING" || playbackStatus == "BUFFERING" || playbackStatus == "RETRYING" || playbackStatus == "SWITCHING") {
                    CircularProgressIndicator(
                        color = arena.accentGreen,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                if (errorMessage != null) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.88f),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        autoHopCount = 0
                                        viewModel.switchToNextChannel()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = arena.accentGreen,
                                        contentColor = arena.onAccentGreen
                                    )
                                ) {
                                    Text("Next Live Channel", fontWeight = FontWeight.SemiBold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        errorMessage = null
                                        reloadTrigger += 1
                                    }
                                ) {
                                    Text("Retry", color = Color.White)
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.switchToChannel(viewModel.backupStreams.first())
                                    }
                                ) {
                                    Text("Backup Feed", color = Color.White)
                                }
                            }
                        }
                    }
                }

                val captionText = translatedCaption
                androidx.compose.animation.AnimatedVisibility(
                    visible = captionsEnabled && !captionText.isNullOrBlank() && errorMessage == null,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = if (isFullscreen) 18.dp else 10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.82f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (selectedLanguage != TranslationLanguage.ORIGINAL) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = arena.accentGreen.copy(alpha = 0.22f)
                                ) {
                                    Text(
                                        text = selectedLanguage.code.uppercase(),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = arena.accentGreen
                                    )
                                }
                            }
                            Text(
                                text = if (isTranslating) "Translating live caption..." else captionText.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (!isFullscreen) {
                Surface(
                    color = arena.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = { showQualitySheet = true },
                            leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text("Quality: $activeResolutionLabel", style = MaterialTheme.typography.labelSmall) }
                        )
                        AssistChip(
                            onClick = { viewModel.toggleCaptions() },
                            leadingIcon = { Icon(Icons.Default.ClosedCaption, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text(if (captionsEnabled) "Captions: On" else "Captions: Off", style = MaterialTheme.typography.labelSmall) }
                        )
                        AssistChip(
                            onClick = { showLanguageSheet = true },
                            leadingIcon = { Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text(selectedLanguage.nativeLabel, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                HorizontalDivider(color = arena.cardBorder, thickness = 1.dp)

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(relatedChannels, key = { it.id }) { item ->
                        val selected = item.id == channel.id
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) arena.accentGreen else arena.surfaceElevated,
                            border = BorderStroke(1.dp, if (selected) arena.accentGreen else arena.cardBorder),
                            modifier = Modifier
                                .clickable {
                                    autoHopCount = 0
                                    viewModel.switchToChannel(item)
                                }
                                .minimumInteractiveComponentSize()
                        ) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) arena.onAccentGreen else arena.textPrimary,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .widthIn(max = 150.dp)
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (translationError != null) {
                        item {
                            Surface(
                                color = arena.liveRed.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, arena.liveRed.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Translation error: $translationError",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = arena.textPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { viewModel.retryTranslation() }) {
                                        Text("Retry", color = arena.accentGreen, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }

                    val event = activeEvent
                    if (event != null && showScoreHud) {
                        item {
                            val sportTint = sportAccentColor(event.sport)
                            Surface(
                                color = arena.surfaceElevated,
                                border = BorderStroke(1.dp, arena.cardBorder),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            SportVisualBadge(category = event.sport, size = 26.dp)
                                            Text(
                                                text = "${event.sport.displayName} • ${event.league}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = arena.textSecondary
                                            )
                                        }
                                        Text(
                                            text = event.statusDetail,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (event.statusState == EventStatusState.LIVE) arena.liveRed else arena.textSecondary
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = event.awayTeam, style = MaterialTheme.typography.titleMedium, color = arena.textPrimary)
                                        Text(text = event.awayScore, style = MaterialTheme.typography.titleMedium, color = arena.textPrimary, fontWeight = FontWeight.Bold)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = event.homeTeam, style = MaterialTheme.typography.titleMedium, color = arena.textPrimary)
                                        Text(text = event.homeScore, style = MaterialTheme.typography.titleMedium, color = arena.textPrimary, fontWeight = FontWeight.Bold)
                                    }
                                    ScoreComparisonBar(
                                        awayScoreRaw = event.awayScore,
                                        homeScoreRaw = event.homeScore,
                                        awayColor = sportTint,
                                        homeColor = arena.accentGreen
                                    )
                                    if (event.lineScoresAway.isNotEmpty() || event.lineScoresHome.isNotEmpty()) {
                                        HorizontalDivider(color = arena.cardBorder)
                                        Text(
                                            text = "${event.awayAbbr}: ${event.lineScoresAway.joinToString("  ")}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = arena.textSecondary
                                        )
                                        Text(
                                            text = "${event.homeAbbr}: ${event.lineScoresHome.joinToString("  ")}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = arena.textSecondary
                                        )
                                    }
                                }
                            }
                        }

                        val playsToDisplay = if (translatedPlays.isNotEmpty()) {
                            translatedPlays
                        } else {
                            eventSummary?.recentPlays.orEmpty()
                        }

                        if (playsToDisplay.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Live Commentary",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = arena.textPrimary
                                    )
                                    if (selectedLanguage != TranslationLanguage.ORIGINAL) {
                                        Text(
                                            text = "Translated to ${selectedLanguage.displayName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = arena.accentGreen
                                        )
                                    }
                                }
                            }
                            items(playsToDisplay) { play ->
                                Surface(
                                    color = arena.surfaceElevated,
                                    border = BorderStroke(1.dp, arena.cardBorder),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = play,
                                        modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = arena.textPrimary
                                    )
                                }
                            }
                        }
                    } else if (liveMatchesToAttach.isNotEmpty()) {
                        item {
                            Text(
                                text = "Attach Live Match Captions & Scores",
                                style = MaterialTheme.typography.titleSmall,
                                color = arena.textPrimary
                            )
                        }
                        items(liveMatchesToAttach, key = { "${it.sport.name}_${it.id}" }) { liveMatch ->
                            Surface(
                                color = arena.surfaceElevated,
                                border = BorderStroke(1.dp, arena.cardBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.attachEventToPlayer(liveMatch) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        SportVisualBadge(category = liveMatch.sport, size = 30.dp)
                                        Column {
                                            Text(
                                                text = liveMatch.shortName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = arena.textPrimary,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "${liveMatch.sport.displayName} • ${liveMatch.awayAbbr} ${liveMatch.awayScore} - ${liveMatch.homeAbbr} ${liveMatch.homeScore}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = arena.textSecondary
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Attach",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = arena.accentGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQualitySheet) {
        ModalBottomSheet(
            onDismissRequest = { showQualitySheet = false },
            containerColor = arena.surface
        ) {
            val options = remember(detectedQualities) {
                if (detectedQualities.isNotEmpty()) {
                    listOf(VideoQualityOption("auto", "Auto (Best available)", Int.MAX_VALUE, Int.MAX_VALUE)) + detectedQualities
                } else {
                    standardQualities
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Video Quality",
                    style = MaterialTheme.typography.titleMedium,
                    color = arena.textPrimary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                options.forEach { option ->
                    val isSelected = selectedQualityId == option.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedQualityId = option.id
                                activeResolutionLabel = option.id.replaceFirstChar { it.uppercase() }
                                val builder = exoPlayer.trackSelectionParameters.buildUpon()
                                if (option.id == "auto") {
                                    builder.clearVideoSizeConstraints()
                                } else {
                                    builder.setMaxVideoSize(option.maxWidth, option.maxHeight)
                                }
                                exoPlayer.trackSelectionParameters = builder.build()
                                showQualitySheet = false
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) arena.accentGreen else arena.textPrimary
                        )
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = arena.accentGreen)
                        }
                    }
                }
            }
        }
    }

    if (showLanguageSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLanguageSheet = false },
            containerColor = arena.surface
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (detectedAudioTracks.isNotEmpty()) {
                    item {
                        Text(
                            text = "Stream Audio Track",
                            style = MaterialTheme.typography.titleMedium,
                            color = arena.textPrimary,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    items(detectedAudioTracks, key = { it.id }) { audio ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val groups = currentTracksRef.groups
                                    if (audio.groupIndex in groups.indices) {
                                        val mediaGroup = groups[audio.groupIndex].mediaTrackGroup
                                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                            .buildUpon()
                                            .setOverrideForType(TrackSelectionOverride(mediaGroup, audio.trackIndex))
                                            .build()
                                    }
                                    showLanguageSheet = false
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = audio.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (audio.isSelected) arena.accentGreen else arena.textPrimary
                            )
                            if (audio.isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = arena.accentGreen)
                            }
                        }
                    }
                    item {
                        HorizontalDivider(color = arena.cardBorder, modifier = Modifier.padding(vertical = 8.dp))
                    }
                }

                if (detectedSubtitleTracks.isNotEmpty()) {
                    item {
                        Text(
                            text = "Stream Embedded Subtitles",
                            style = MaterialTheme.typography.titleMedium,
                            color = arena.textPrimary,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    items(detectedSubtitleTracks, key = { it.id }) { sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setCaptionsEnabled(true)
                                    val groups = currentTracksRef.groups
                                    if (sub.groupIndex in groups.indices) {
                                        val mediaGroup = groups[sub.groupIndex].mediaTrackGroup
                                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                            .buildUpon()
                                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                            .setOverrideForType(TrackSelectionOverride(mediaGroup, sub.trackIndex))
                                            .build()
                                    }
                                    showLanguageSheet = false
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sub.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (sub.isSelected && captionsEnabled) arena.accentGreen else arena.textPrimary
                            )
                            if (sub.isSelected && captionsEnabled) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = arena.accentGreen)
                            }
                        }
                    }
                    item {
                        HorizontalDivider(color = arena.cardBorder, modifier = Modifier.padding(vertical = 8.dp))
                    }
                }

                item {
                    Text(
                        text = "Live Caption & Commentary Translation",
                        style = MaterialTheme.typography.titleMedium,
                        color = arena.textPrimary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "Automatically translates live match captions and play-by-play while you watch.",
                        style = MaterialTheme.typography.bodySmall,
                        color = arena.textSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(TranslationLanguage.entries, key = { it.code }) { lang ->
                    val isSelected = selectedLanguage == lang
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setCaptionsEnabled(true)
                                viewModel.selectTranslationLanguage(lang)
                                showLanguageSheet = false
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = lang.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) arena.accentGreen else arena.textPrimary
                            )
                            if (lang != TranslationLanguage.ORIGINAL) {
                                Text(
                                    text = lang.nativeLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = arena.textSecondary
                                )
                            }
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = arena.accentGreen)
                        }
                    }
                }
            }
        }
    }
}
