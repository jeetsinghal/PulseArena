package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.EventStatusState
import com.example.data.SportCategory
import com.example.data.SportEvent
import com.example.data.UiState
import com.example.ui.components.EmptyBroadcastView
import com.example.ui.components.MatchSkeletonList
import com.example.ui.components.ScoreSpotlightBanner
import com.example.ui.components.SportEventCard
import com.example.ui.components.sportIconFor
import com.example.ui.theme.arenaColors
import com.example.ui.viewmodel.SportsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreboardScreen(
    viewModel: SportsViewModel,
    onWatchEvent: (SportEvent) -> Unit,
    onOpenSettings: () -> Unit
) {
    val arena = MaterialTheme.arenaColors
    val eventsState by viewModel.eventsState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshingScores.collectAsStateWithLifecycle()
    val selectedSport by viewModel.selectedScoreSport.collectAsStateWithLifecycle()
    var liveOnly by remember { mutableStateOf(false) }

    val scoreboardSports = remember {
        listOf(
            SportCategory.ALL,
            SportCategory.INDIAN_SPORTS,
            SportCategory.CRICKET,
            SportCategory.SOCCER,
            SportCategory.BASKETBALL,
            SportCategory.FOOTBALL,
            SportCategory.TENNIS,
            SportCategory.HOCKEY,
            SportCategory.BASEBALL,
            SportCategory.RACING,
            SportCategory.COMBAT
        )
    }

    val topAuroraGradient = Brush.verticalGradient(
        colors = listOf(
            arena.gradientStart.copy(alpha = if (arena.isDark) 0.24f else 0.10f),
            arena.gradientMid.copy(alpha = if (arena.isDark) 0.12f else 0.05f),
            arena.background
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Matches",
                        style = MaterialTheme.typography.titleLarge,
                        color = arena.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    FilterChip(
                        selected = liveOnly,
                        onClick = { liveOnly = !liveOnly },
                        label = { Text("Live Only", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = arena.liveRed,
                            selectedLabelColor = Color.White,
                            containerColor = arena.surface,
                            labelColor = arena.textSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = liveOnly,
                            borderColor = arena.cardBorder,
                            selectedBorderColor = arena.liveRed
                        ),
                        modifier = Modifier.padding(end = 2.dp)
                    )
                    IconButton(
                        onClick = { viewModel.toggleDarkLight(arena.isDark) },
                        modifier = Modifier.testTag("scores_theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (arena.isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (arena.isDark) "Switch to light mode" else "Switch to dark mode",
                            tint = arena.textPrimary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.loadScores(selectedSport, forceRefresh = true) },
                        modifier = Modifier.testTag("refresh_scores_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh scores", tint = arena.textPrimary)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = arena.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = arena.background,
        modifier = Modifier.background(topAuroraGradient)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(topAuroraGradient)
                .padding(innerPadding)
        ) {
            AnimatedVisibility(
                visible = isRefreshing,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                LinearProgressIndicator(
                    color = arena.accentGreen,
                    trackColor = arena.surfaceHighlight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                )
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(scoreboardSports, key = { it.name }) { sport ->
                    val isSelected = selectedSport == sport
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.loadScores(sport, forceRefresh = false) },
                        leadingIcon = {
                            Icon(
                                imageVector = sportIconFor(sport),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(sport.displayName, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = arena.accentGreen,
                            selectedLabelColor = arena.onAccentGreen,
                            selectedLeadingIconColor = arena.onAccentGreen,
                            containerColor = arena.surface,
                            labelColor = arena.textSecondary,
                            iconColor = arena.textSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = arena.cardBorder,
                            selectedBorderColor = arena.accentGreen
                        )
                    )
                }
            }

            AnimatedContent(
                targetState = eventsState,
                transitionSpec = {
                    fadeIn(tween(220)) togetherWith fadeOut(tween(160))
                },
                label = "scores_state_transition"
            ) { state ->
                when (state) {
                    is UiState.Loading -> MatchSkeletonList()

                    is UiState.Error -> {
                        EmptyBroadcastView(
                            title = "Unable to load matches",
                            subtitle = state.message,
                            actionLabel = "Retry",
                            onAction = { viewModel.loadScores(selectedSport, forceRefresh = true) }
                        )
                    }

                    is UiState.Success -> {
                        val liveCount = remember(state.data) {
                            state.data.count { it.statusState == EventStatusState.LIVE }
                        }
                        val upcomingCount = remember(state.data) {
                            state.data.count { it.statusState == EventStatusState.UPCOMING }
                        }
                        val finalCount = remember(state.data) {
                            state.data.count { it.statusState == EventStatusState.FINAL }
                        }

                        val filtered = remember(state.data, liveOnly) {
                            if (liveOnly) state.data.filter { it.statusState == EventStatusState.LIVE }
                            else state.data
                        }

                        if (filtered.isEmpty()) {
                            EmptyBroadcastView(
                                title = if (liveOnly) "No live ${selectedSport.displayName} matches right now" else "No matches scheduled",
                                subtitle = if (liveOnly) "Switch off Live Only to view upcoming fixtures and final scores." else "Try another sport category or refresh.",
                                actionLabel = if (liveOnly) "Show All Matches" else "Refresh",
                                onAction = {
                                    if (liveOnly) liveOnly = false else viewModel.loadScores(selectedSport, forceRefresh = true)
                                }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item(key = "score_spotlight") {
                                    ScoreSpotlightBanner(
                                        liveCount = liveCount,
                                        upcomingCount = upcomingCount,
                                        finalCount = finalCount,
                                        sportTitle = selectedSport.displayName
                                    )
                                }

                                items(filtered, key = { "${it.sport.name}_${it.id}" }) { event ->
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp)
                                            .animateItem()
                                    ) {
                                        SportEventCard(
                                            event = event,
                                            onWatchLive = { onWatchEvent(event) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
