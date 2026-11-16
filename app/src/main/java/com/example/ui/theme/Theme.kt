package com.example.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.abstraction.ThemeMode

@Immutable
data class ArenaColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceHighlight: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val cardBorder: Color,
    val accentGreen: Color,
    val onAccentGreen: Color,
    val accentGreenVariant: Color,
    val liveRed: Color,
    val sportBlue: Color,
    val goldAccent: Color
)

private val DarkArenaColors = ArenaColors(
    isDark = true,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceElevated = DarkSurfaceElevated,
    surfaceHighlight = DarkSurfaceHighlight,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    cardBorder = DarkCardBorder,
    accentGreen = NeonGreen,
    onAccentGreen = Color(0xFF052E16),
    accentGreenVariant = NeonGreenVariant,
    liveRed = LiveRed,
    sportBlue = VividBlueDark,
    goldAccent = GoldAccentDark
)

private val LightArenaColors = ArenaColors(
    isDark = false,
    background = LightBackground,
    surface = LightSurface,
    surfaceElevated = LightSurfaceElevated,
    surfaceHighlight = LightSurfaceHighlight,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    cardBorder = LightCardBorder,
    accentGreen = StadiumGreenLight,
    onAccentGreen = Color.White,
    accentGreenVariant = Color(0xFFDCFCE7),
    liveRed = LiveRedLight,
    sportBlue = VividBlue,
    goldAccent = GoldAccent
)

val LocalArenaColors = staticCompositionLocalOf { DarkArenaColors }

val MaterialTheme.arenaColors: ArenaColors
    @Composable
    get() = LocalArenaColors.current

private val DarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = Color(0xFF052E16),
    primaryContainer = NeonGreenVariant,
    onPrimaryContainer = DarkTextPrimary,
    secondary = VividBlueDark,
    onSecondary = DarkBackground,
    tertiary = GoldAccentDark,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkCardBorder,
    error = LiveRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = StadiumGreenLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = VividBlue,
    onSecondary = Color.White,
    tertiary = GoldAccent,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceHighlight,
    onSurfaceVariant = LightTextSecondary,
    outline = LightCardBorder,
    error = LiveRedLight,
    onError = Color.White
)

@Composable
fun PulseArenaTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val targetArena = if (useDarkTheme) DarkArenaColors else LightArenaColors
    val animSpec = tween<Color>(durationMillis = 260)

    val bg by animateColorAsState(targetArena.background, animSpec, label = "theme_bg")
    val surface by animateColorAsState(targetArena.surface, animSpec, label = "theme_surface")
    val elevated by animateColorAsState(targetArena.surfaceElevated, animSpec, label = "theme_elevated")
    val highlight by animateColorAsState(targetArena.surfaceHighlight, animSpec, label = "theme_highlight")
    val textPrimary by animateColorAsState(targetArena.textPrimary, animSpec, label = "theme_text_primary")
    val textSecondary by animateColorAsState(targetArena.textSecondary, animSpec, label = "theme_text_secondary")
    val border by animateColorAsState(targetArena.cardBorder, animSpec, label = "theme_border")
    val accent by animateColorAsState(targetArena.accentGreen, animSpec, label = "theme_accent")

    val animatedArena = targetArena.copy(
        background = bg,
        surface = surface,
        surfaceElevated = elevated,
        surfaceHighlight = highlight,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        cardBorder = border,
        accentGreen = accent
    )

    val baseScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme
    val colorScheme = baseScheme.copy(
        primary = accent,
        background = bg,
        surface = surface,
        surfaceVariant = elevated,
        onBackground = textPrimary,
        onSurface = textPrimary,
        onSurfaceVariant = textSecondary,
        outline = border
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()
            if (activity != null) {
                val controller = WindowCompat.getInsetsController(activity.window, view)
                controller.isAppearanceLightStatusBars = !useDarkTheme
                controller.isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }

    CompositionLocalProvider(LocalArenaColors provides animatedArena) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
