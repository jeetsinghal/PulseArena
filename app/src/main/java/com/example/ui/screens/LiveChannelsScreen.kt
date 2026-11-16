package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.EventStatusState
import com.example.data.LiveChannel
import com.example.data.SportCategory
import com.example.data.UiState
import com.example.ui.components.ChannelSkeletonList
import com.example.ui.components.EmptyBroadcastView
import com.example.ui.components.FeaturedArenaHeroCard
import com.example.ui.components.LiveChannelCard
import com.example.ui.components.sportIconFor
import com.example.ui.theme.arenaColors
import com.example.ui.viewmodel.SportsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveChannelsScreen(
    viewModel: SportsViewModel,
    onPlayChannel: (LiveChannel) -> Unit,
    onOpenSettings: () -> Unit
) {
    val arena = MaterialTheme.arenaColors
    val channelsState by viewModel.channelsState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshingChannels.collectAsStateWithLifecycle()
    val eventsState by viewModel.eventsState.collectAsStateWithLifecycle()
    val selectedSport by viewModel.selectedChannelSport.collectAsStateWithLifecycle()
    val searchQuery by viewModel.channelSearchQuery.collectAsStateWithLifecycle()
    val preferHttpsOnly by viewModel.preferHttpsOnly.collectAsStateWithLifecycle()

    var searchExpanded by remember { mutableStateOf(false) }

    val liveMatchCount by remember {
        derivedStateOf {
            (eventsState as? UiState.Success)?.data?.count { it.statusState == EventStatusState.LIVE } ?: 0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PulseArena",
                        style = MaterialTheme.typography.titleLarge,
                        color = arena.textPrimary
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleDarkLight(arena.isDark) },
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (arena.isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (arena.isDark) "Switch to light mode" else "Switch to dark mode",
                            tint = arena.textPrimary
                        )
                    }
                    IconButton(
                        onClick = {
                            searchExpanded = !searchExpanded
                            if (!searchExpanded) viewModel.setChannelSearchQuery("")
                        },
                        modifier = Modifier.testTag("toggle_search_button")
                    ) {
                        Icon(
                            imageVector = if (searchExpanded) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search channels",
                            tint = arena.textPrimary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.loadChannels(forceRefresh = true) },
                        modifier = Modifier.testTag("refresh_channels_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh channels",
                            tint = arena.textPrimary
                        )
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("open_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = arena.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = arena.background)
            )
        },
        containerColor = arena.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
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

            AnimatedVisibility(
                visible = searchExpanded,
                enter = fadeIn(tween(180)) + expandVertically(tween(200)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(180))
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setChannelSearchQuery(it) },
                    placeholder = { Text("Search sports channels...", color = arena.textSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = arena.accentGreen,
                        unfocusedBorderColor = arena.cardBorder,
                        focusedContainerColor = arena.surface,
                        unfocusedContainerColor = arena.surface,
                        focusedTextColor = arena.textPrimary,
                        unfocusedTextColor = arena.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("channel_search_input")
                )
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SportCategory.entries, key = { it.name }) { category ->
                    val isSelected = selectedSport == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setChannelSportFilter(category) },
                        leadingIcon = {
                            Icon(
                                imageVector = sportIconFor(category),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
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
                targetState = channelsState,
                transitionSpec = {
                    fadeIn(tween(220)) togetherWith fadeOut(tween(160))
                },
                label = "channels_state_transition"
            ) { state ->
                when (state) {
                    is UiState.Loading -> ChannelSkeletonList()

                    is UiState.Error -> {
                        EmptyBroadcastView(
                            title = "Unable to load live channels",
                            subtitle = state.message,
                            actionLabel = "Retry",
                            onAction = { viewModel.loadChannels(forceRefresh = true) }
                        )
                    }

                    is UiState.Success -> {
                        val filtered = remember(state.data, selectedSport, searchQuery, preferHttpsOnly) {
                            state.data.filter { ch ->
                                val matchesSport = when (selectedSport) {
                                    SportCategory.ALL -> true
                                    SportCategory.INDIAN_SPORTS -> ch.isIndianOrigin || ch.sport == SportCategory.INDIAN_SPORTS
                                    else -> ch.sport == selectedSport
                                }
                                val matchesSearch = searchQuery.isBlank() ||
                                    ch.name.contains(searchQuery, ignoreCase = true) ||
                                    ch.sport.displayName.contains(searchQuery, ignoreCase = true) ||
                                    (ch.isIndianOrigin && "india".contains(searchQuery, ignoreCase = true))
                                val matchesHttps = !preferHttpsOnly || ch.isHttps
                                matchesSport && matchesSearch && matchesHttps
                            }
                        }

                        if (filtered.isEmpty()) {
                            EmptyBroadcastView(
                                title = "No channels matched",
                                subtitle = "Try another sport category or clear your search filter.",
                                actionLabel = if (searchQuery.isNotBlank() || selectedSport != SportCategory.ALL) "Show All" else null,
                                onAction = {
                                    viewModel.setChannelSearchQuery("")
                                    viewModel.setChannelSportFilter(SportCategory.ALL)
                                }
                            )
                        } else {
                            val featuredChannel = filtered.firstOrNull { !it.isGeoRestrictedHint && it.isHttps }
                                ?: filtered.firstOrNull()

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (searchQuery.isBlank()) {
                                    item(key = "hero_banner") {
                                        FeaturedArenaHeroCard(
                                            featuredChannel = featuredChannel,
                                            totalChannelCount = filtered.size,
                                            liveMatchCount = liveMatchCount,
                                            onWatchFeatured = onPlayChannel
                                        )
                                    }
                                }

                                item(key = "channel_count_header") {
                                    Text(
                                        text = "${filtered.size} Live Channels",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = arena.textSecondary,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                                    )
                                }

                                items(filtered, key = { it.id }) { channel ->
                                    LiveChannelCard(
                                        channel = channel,
                                        onPlay = { onPlayChannel(channel) },
                                        modifier = Modifier.animateItem()
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
