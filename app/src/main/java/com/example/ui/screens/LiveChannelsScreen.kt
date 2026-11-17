package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ChannelCategory
import com.example.data.ChannelFeedFilter
import com.example.data.EventStatusState
import com.example.data.LiveChannel
import com.example.data.M3uParser
import com.example.data.SportCategory
import com.example.data.UiState
import com.example.ui.components.ChannelSkeletonList
import com.example.ui.components.EmptyBroadcastView
import com.example.ui.components.FeaturedArenaHeroCard
import com.example.ui.components.LiveChannelCard
import com.example.ui.components.channelCategoryAccentColor
import com.example.ui.components.channelCategoryIconFor
import com.example.ui.theme.arenaColors
import com.example.ui.viewmodel.SportsViewModel
import kotlinx.coroutines.launch

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
    val selectedCategory by viewModel.selectedChannelCategory.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedChannelFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.channelSearchQuery.collectAsStateWithLifecycle()
    val preferHttpsOnly by viewModel.preferHttpsOnly.collectAsStateWithLifecycle()

    var searchExpanded by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    val liveMatchCount by remember {
        derivedStateOf {
            (eventsState as? UiState.Success)?.data?.count { it.statusState == EventStatusState.LIVE } ?: 0
        }
    }

    val allLoadedChannels = (channelsState as? UiState.Success)?.data.orEmpty()
    val categoryCounts = remember(allLoadedChannels, preferHttpsOnly) {
        val visible = if (preferHttpsOnly) allLoadedChannels.filter { it.isHttps } else allLoadedChannels
        ChannelCategory.entries.associateWith { cat ->
            if (cat == ChannelCategory.ALL) visible.size
            else visible.count { it.channelCategory == cat }
        }
    }

    val topAuroraGradient = Brush.verticalGradient(
        colors = listOf(
            arena.gradientStart.copy(alpha = if (arena.isDark) 0.24f else 0.10f),
            arena.gradientMid.copy(alpha = if (arena.isDark) 0.12f else 0.05f),
            arena.background
        )
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = arena.surface,
                drawerContentColor = arena.textPrimary,
                modifier = Modifier.widthIn(max = 310.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        arena.gradientStart,
                                        arena.gradientMid,
                                        arena.gradientEnd
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "PulseArena",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Indian & Global Channels • OTT",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.88f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${allLoadedChannels.size} Playable Live Feeds",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "CHANNEL CATEGORIES",
                        style = MaterialTheme.typography.labelSmall,
                        color = arena.textSecondary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )

                    ChannelCategory.entries.forEach { category ->
                        val isSelected = selectedCategory == category
                        val catColor = channelCategoryAccentColor(category)
                        val count = categoryCounts[category] ?: 0
                        NavigationDrawerItem(
                            selected = isSelected,
                            onClick = {
                                viewModel.setChannelCategory(category)
                                scope.launch { drawerState.close() }
                            },
                            icon = {
                                Icon(
                                    imageVector = channelCategoryIconFor(category),
                                    contentDescription = category.displayName,
                                    tint = if (isSelected) catColor else arena.textSecondary
                                )
                            },
                            label = {
                                Text(
                                    text = category.displayName,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            badge = {
                                Surface(
                                    color = if (isSelected) catColor.copy(alpha = 0.22f) else arena.surfaceHighlight,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = count.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) catColor else arena.textSecondary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = catColor.copy(alpha = 0.14f),
                                selectedIconColor = catColor,
                                selectedTextColor = arena.textPrimary,
                                unselectedContainerColor = Color.Transparent,
                                unselectedIconColor = arena.textSecondary,
                                unselectedTextColor = arena.textPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("drawer_category_${category.name.lowercase()}")
                        )
                    }

                    HorizontalDivider(
                        color = arena.cardBorder,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    NavigationDrawerItem(
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onOpenSettings()
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings & Playlists",
                                tint = arena.textSecondary
                            )
                        },
                        label = { Text("Playlist & Stream Settings") },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            unselectedTextColor = arena.textPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("open_category_drawer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open channel categories menu",
                                tint = arena.textPrimary
                            )
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = "PulseArena",
                                style = MaterialTheme.typography.titleLarge,
                                color = arena.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = selectedCategory.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = channelCategoryAccentColor(selectedCategory),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
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

                AnimatedVisibility(
                    visible = searchExpanded,
                    enter = fadeIn(tween(180)) + expandVertically(tween(200)),
                    exit = fadeOut(tween(150)) + shrinkVertically(tween(180))
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setChannelSearchQuery(it) },
                        placeholder = { Text("Search Hindi, News, Movies, Songs, Kids, OTT...", color = arena.textSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
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
                            .onFocusChanged { if (it.isFocused) keyboardController?.show() }
                            .testTag("channel_search_input")
                    )
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ChannelFeedFilter.entries, key = { it.name }) { filter ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setChannelFeedFilter(filter) },
                            leadingIcon = {
                                Icon(
                                    imageVector = filterIconFor(filter),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = filter.displayName,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = arena.accentGreen,
                                selectedLabelColor = arena.onAccentGreen,
                                selectedLeadingIconColor = arena.onAccentGreen,
                                containerColor = arena.surface.copy(alpha = 0.85f),
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
                            val filtered = remember(
                                state.data,
                                selectedCategory,
                                selectedFilter,
                                searchQuery,
                                preferHttpsOnly
                            ) {
                                state.data.filter { ch ->
                                    val matchesCategory = selectedCategory == ChannelCategory.ALL ||
                                        ch.channelCategory == selectedCategory
                                    val matchesFilter = when (selectedFilter) {
                                        ChannelFeedFilter.ALL -> true
                                        ChannelFeedFilter.INDIAN_SPORTS ->
                                            ch.sport == SportCategory.INDIAN_SPORTS ||
                                                (ch.isIndianOrigin && ch.channelCategory == ChannelCategory.SPORTS)
                                        ChannelFeedFilter.HINDI -> ch.languageTag.equals("Hindi", ignoreCase = true)
                                        ChannelFeedFilter.ENGLISH -> ch.languageTag.equals("English", ignoreCase = true)
                                        ChannelFeedFilter.REGIONAL -> M3uParser.isRegionalLanguage(ch.languageTag)
                                        ChannelFeedFilter.CRICKET -> ch.sport == SportCategory.CRICKET
                                        ChannelFeedFilter.SOCCER -> ch.sport == SportCategory.SOCCER
                                        ChannelFeedFilter.ALL_SPORTS ->
                                            ch.channelCategory == ChannelCategory.SPORTS ||
                                                ch.sport != SportCategory.ALL
                                    }
                                    val matchesSearch = searchQuery.isBlank() ||
                                        ch.name.contains(searchQuery, ignoreCase = true) ||
                                        ch.languageTag.contains(searchQuery, ignoreCase = true) ||
                                        ch.groupTitle.contains(searchQuery, ignoreCase = true) ||
                                        ch.channelCategory.displayName.contains(searchQuery, ignoreCase = true) ||
                                        ch.sport.displayName.contains(searchQuery, ignoreCase = true) ||
                                        (ch.isIndianOrigin && "india".contains(searchQuery, ignoreCase = true))
                                    val matchesHttps = !preferHttpsOnly || ch.isHttps
                                    matchesCategory && matchesFilter && matchesSearch && matchesHttps
                                }
                            }

                            if (filtered.isEmpty()) {
                                EmptyBroadcastView(
                                    title = "No channels matched",
                                    subtitle = "Try another category from the menu, language filter, or clear search.",
                                    actionLabel = if (searchQuery.isNotBlank() ||
                                        selectedFilter != ChannelFeedFilter.ALL ||
                                        selectedCategory != ChannelCategory.ALL
                                    ) "Show All Channels" else null,
                                    onAction = {
                                        viewModel.setChannelSearchQuery("")
                                        viewModel.setChannelFeedFilter(ChannelFeedFilter.ALL)
                                        viewModel.setChannelCategory(ChannelCategory.ALL)
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
                                            text = "${filtered.size} ${selectedCategory.displayName}",
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
}

private fun filterIconFor(filter: ChannelFeedFilter): ImageVector = when (filter) {
    ChannelFeedFilter.ALL -> Icons.Default.LiveTv
    ChannelFeedFilter.INDIAN_SPORTS -> Icons.Default.Flag
    ChannelFeedFilter.HINDI -> Icons.Default.Translate
    ChannelFeedFilter.ENGLISH -> Icons.Default.Language
    ChannelFeedFilter.REGIONAL -> Icons.Default.Public
    ChannelFeedFilter.CRICKET -> Icons.Default.SportsCricket
    ChannelFeedFilter.SOCCER -> Icons.Default.SportsSoccer
    ChannelFeedFilter.ALL_SPORTS -> Icons.Default.Sports
}
