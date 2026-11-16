package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.LiveChannelsScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.ScoreboardScreen
import com.example.ui.screens.SourcesSettingsScreen
import com.example.ui.theme.PulseArenaTheme
import com.example.ui.theme.arenaColors
import com.example.ui.viewmodel.SportsViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check before UI, ViewModels, or Firebase operations.
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        val appContainer = (application as PulseArenaApp).container

        setContent {
            val sportsViewModel: SportsViewModel = viewModel(factory = appContainer.viewModelFactory)
            val themeMode by sportsViewModel.themeMode.collectAsStateWithLifecycle()

            PulseArenaTheme(themeMode = themeMode) {
                val arena = MaterialTheme.arenaColors
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val showBottomBar = currentRoute == "live_tv" || currentRoute == "scores"

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = arena.background,
                    bottomBar = {
                        AnimatedVisibility(
                            visible = showBottomBar,
                            enter = slideInVertically(tween(220, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(180)),
                            exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(150))
                        ) {
                            NavigationBar(
                                containerColor = arena.surface
                            ) {
                                NavigationBarItem(
                                    selected = currentRoute == "live_tv",
                                    onClick = {
                                        navController.navigate("live_tv") {
                                            popUpTo("live_tv") { inclusive = true }
                                        }
                                    },
                                    icon = { Icon(Icons.Default.LiveTv, contentDescription = "Live TV") },
                                    label = { Text("Live TV") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = arena.accentGreen,
                                        selectedTextColor = arena.accentGreen,
                                        indicatorColor = arena.accentGreen.copy(alpha = 0.14f),
                                        unselectedIconColor = arena.textSecondary,
                                        unselectedTextColor = arena.textSecondary
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentRoute == "scores",
                                    onClick = {
                                        navController.navigate("scores") {
                                            popUpTo("live_tv")
                                            launchSingleTop = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "Matches") },
                                    label = { Text("Matches") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = arena.accentGreen,
                                        selectedTextColor = arena.accentGreen,
                                        indicatorColor = arena.accentGreen.copy(alpha = 0.14f),
                                        unselectedIconColor = arena.textSecondary,
                                        unselectedTextColor = arena.textSecondary
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "live_tv",
                        modifier = Modifier.padding(innerPadding),
                        enterTransition = {
                            if (targetState.destination.route == "player" || targetState.destination.route == "settings") {
                                slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { it / 5 } + fadeIn(tween(220))
                            } else {
                                fadeIn(tween(200)) + scaleIn(initialScale = 0.985f, animationSpec = tween(200))
                            }
                        },
                        exitTransition = {
                            if (targetState.destination.route == "player" || targetState.destination.route == "settings") {
                                fadeOut(tween(180))
                            } else {
                                fadeOut(tween(160))
                            }
                        },
                        popEnterTransition = {
                            fadeIn(tween(220)) + scaleIn(initialScale = 0.985f, animationSpec = tween(220))
                        },
                        popExitTransition = {
                            if (initialState.destination.route == "player" || initialState.destination.route == "settings") {
                                slideOutHorizontally(tween(240, easing = FastOutSlowInEasing)) { it / 5 } + fadeOut(tween(180))
                            } else {
                                fadeOut(tween(160))
                            }
                        }
                    ) {
                        composable("live_tv") {
                            LiveChannelsScreen(
                                viewModel = sportsViewModel,
                                onPlayChannel = { channel ->
                                    sportsViewModel.openChannelInPlayer(channel)
                                    navController.navigate("player")
                                },
                                onOpenSettings = {
                                    navController.navigate("settings")
                                }
                            )
                        }
                        composable("scores") {
                            ScoreboardScreen(
                                viewModel = sportsViewModel,
                                onWatchEvent = { event ->
                                    sportsViewModel.openEventInPlayer(event)
                                    navController.navigate("player")
                                },
                                onOpenSettings = {
                                    navController.navigate("settings")
                                }
                            )
                        }
                        composable("settings") {
                            SourcesSettingsScreen(
                                viewModel = sportsViewModel,
                                onBack = { navController.popBackStack() },
                                onPlayCustomStream = {
                                    navController.navigate("player")
                                }
                            )
                        }
                        composable("player") {
                            PlayerScreen(
                                viewModel = sportsViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
