package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.UiState
import com.example.data.abstraction.ThemeMode
import com.example.ui.theme.arenaColors
import com.example.ui.viewmodel.SportsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesSettingsScreen(
    viewModel: SportsViewModel,
    onBack: () -> Unit,
    onPlayCustomStream: () -> Unit
) {
    BackHandler { onBack() }

    val arena = MaterialTheme.arenaColors
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val autoSwitchOnError by viewModel.autoSwitchOnError.collectAsStateWithLifecycle()
    val preferHttpsOnly by viewModel.preferHttpsOnly.collectAsStateWithLifecycle()
    val showScoreHudOverlay by viewModel.showScoreHudOverlay.collectAsStateWithLifecycle()
    val channelsState by viewModel.channelsState.collectAsStateWithLifecycle()
    val eventsState by viewModel.eventsState.collectAsStateWithLifecycle()

    var customName by remember { mutableStateOf("") }
    var customUrl by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = arena.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = arena.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = arena.background)
            )
        },
        containerColor = arena.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = arena.surfaceElevated,
                    border = BorderStroke(1.dp, arena.cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Appearance",
                            style = MaterialTheme.typography.titleMedium,
                            color = arena.textPrimary
                        )
                        Text(
                            text = "Switch between Stadium Light, Broadcast Dark, or follow your device setting.",
                            style = MaterialTheme.typography.bodySmall,
                            color = arena.textSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ThemePreviewOptionCard(
                                label = "Dark",
                                icon = Icons.Default.DarkMode,
                                previewBg = Color(0xFF0A0D12),
                                previewCard = Color(0xFF1A202C),
                                previewAccent = Color(0xFF1ED760),
                                selected = themeMode == ThemeMode.DARK,
                                onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_option_dark")
                            )
                            ThemePreviewOptionCard(
                                label = "Light",
                                icon = Icons.Default.LightMode,
                                previewBg = Color(0xFFF3F6FA),
                                previewCard = Color(0xFFFFFFFF),
                                previewAccent = Color(0xFF0D9F4F),
                                selected = themeMode == ThemeMode.LIGHT,
                                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_option_light")
                            )
                            ThemePreviewOptionCard(
                                label = "System",
                                icon = Icons.Default.SettingsBrightness,
                                previewBg = Color(0xFF334155),
                                previewCard = Color(0xFF64748B),
                                previewAccent = arena.accentGreen,
                                selected = themeMode == ThemeMode.SYSTEM,
                                onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_option_system")
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = arena.surfaceElevated,
                    border = BorderStroke(1.dp, arena.cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Custom Stream URL",
                            style = MaterialTheme.typography.titleMedium,
                            color = arena.textPrimary
                        )
                        Text(
                            text = "Paste an https:// or http:// .m3u8 live stream URL to open it in the player.",
                            style = MaterialTheme.typography.bodySmall,
                            color = arena.textSecondary
                        )
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text("Channel Name (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = arena.accentGreen,
                                unfocusedBorderColor = arena.cardBorder,
                                focusedTextColor = arena.textPrimary,
                                unfocusedTextColor = arena.textPrimary
                            )
                        )
                        OutlinedTextField(
                            value = customUrl,
                            onValueChange = {
                                customUrl = it
                                urlError = null
                            },
                            label = { Text("Stream URL (https://...m3u8)") },
                            singleLine = true,
                            isError = urlError != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_stream_url_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = arena.accentGreen,
                                unfocusedBorderColor = arena.cardBorder,
                                focusedTextColor = arena.textPrimary,
                                unfocusedTextColor = arena.textPrimary
                            )
                        )
                        if (urlError != null) {
                            Text(
                                text = urlError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = arena.liveRed
                            )
                        }
                        Button(
                            onClick = {
                                viewModel.playCustomUrl(
                                    name = customName,
                                    url = customUrl,
                                    onSuccess = onPlayCustomStream,
                                    onValidationError = { msg -> urlError = msg }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = arena.accentGreen,
                                contentColor = arena.onAccentGreen
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("play_custom_url_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play Stream", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = arena.surfaceElevated,
                    border = BorderStroke(1.dp, arena.cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Playback & Security",
                            style = MaterialTheme.typography.titleMedium,
                            color = arena.textPrimary
                        )

                        SettingToggleRow(
                            title = "Auto-Switch on Stream Error",
                            subtitle = "Switch to the next live feed if a stream is unavailable",
                            checked = autoSwitchOnError,
                            onCheckedChange = { viewModel.setAutoSwitchOnError(it) }
                        )

                        SettingToggleRow(
                            title = "HTTPS Streams Only",
                            subtitle = "Restrict Live TV directory to encrypted HTTPS feeds",
                            checked = preferHttpsOnly,
                            onCheckedChange = { viewModel.setPreferHttpsOnly(it) }
                        )

                        SettingToggleRow(
                            title = "Match Scoreboard Panel",
                            subtitle = "Show live scoreboard details below the player",
                            checked = showScoreHudOverlay,
                            onCheckedChange = { viewModel.setShowScoreHudOverlay(it) }
                        )
                    }
                }
            }

            item {
                val channelCount = (channelsState as? UiState.Success)?.data?.size ?: 0
                val eventCount = (eventsState as? UiState.Success)?.data?.size ?: 0
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = arena.surfaceElevated,
                    border = BorderStroke(1.dp, arena.cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = arena.accentGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Feed & Release Security",
                                style = MaterialTheme.typography.titleMedium,
                                color = arena.textPrimary
                            )
                        }
                        Text(
                            text = "Live TV Directory: $channelCount channels • Live Matches: $eventCount events",
                            style = MaterialTheme.typography.bodySmall,
                            color = arena.textSecondary
                        )
                        Text(
                            text = "Protected by Firebase App Check, HTTPS API transport, URL scheme sanitization, and R8 release minification with zero bundled API keys.",
                            style = MaterialTheme.typography.bodySmall,
                            color = arena.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePreviewOptionCard(
    label: String,
    icon: ImageVector,
    previewBg: Color,
    previewCard: Color,
    previewAccent: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val arena = MaterialTheme.arenaColors
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) arena.accentGreen.copy(alpha = 0.12f) else arena.surfaceHighlight,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) arena.accentGreen else arena.cardBorder
        ),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(previewBg)
                    .padding(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(previewCard)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(previewAccent)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (selected) Icons.Default.CheckCircle else icon,
                    contentDescription = null,
                    tint = if (selected) arena.accentGreen else arena.textSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) arena.accentGreen else arena.textPrimary
                )
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val arena = MaterialTheme.arenaColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = arena.textPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = arena.textSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = arena.onAccentGreen,
                checkedTrackColor = arena.accentGreen
            )
        )
    }
}
