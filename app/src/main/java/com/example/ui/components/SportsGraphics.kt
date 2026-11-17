package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ChannelCategory
import com.example.data.LiveChannel
import com.example.data.SportCategory
import com.example.ui.theme.arenaColors

fun channelCategoryIconFor(category: ChannelCategory): ImageVector = when (category) {
    ChannelCategory.ALL -> Icons.Default.LiveTv
    ChannelCategory.SPORTS -> Icons.Default.SportsCricket
    ChannelCategory.NEWS -> Icons.Default.Newspaper
    ChannelCategory.MOVIES -> Icons.Default.Movie
    ChannelCategory.SONGS -> Icons.Default.MusicNote
    ChannelCategory.KIDS -> Icons.Default.ChildCare
    ChannelCategory.OTT -> Icons.Default.SmartDisplay
}

@Composable
fun channelCategoryAccentColor(category: ChannelCategory): Color {
    val arena = MaterialTheme.arenaColors
    return when (category) {
        ChannelCategory.ALL -> arena.accentGreen
        ChannelCategory.SPORTS -> arena.accentGreen
        ChannelCategory.NEWS -> arena.sportBlue
        ChannelCategory.MOVIES -> Color(0xFFF43F5E)
        ChannelCategory.SONGS -> Color(0xFFA855F7)
        ChannelCategory.KIDS -> arena.goldAccent
        ChannelCategory.OTT -> arena.secondaryAccent
    }
}

fun sportIconFor(category: SportCategory): ImageVector = when (category) {
    SportCategory.ALL -> Icons.Default.LiveTv
    SportCategory.INDIAN_SPORTS -> Icons.Default.Stadium
    SportCategory.CRICKET -> Icons.Default.SportsCricket
    SportCategory.SOCCER -> Icons.Default.SportsSoccer
    SportCategory.BASKETBALL -> Icons.Default.SportsBasketball
    SportCategory.RACING -> Icons.Default.DirectionsCar
    SportCategory.TENNIS -> Icons.Default.SportsTennis
    SportCategory.COMBAT -> Icons.Default.SportsMma
    SportCategory.FOOTBALL -> Icons.Default.SportsFootball
    SportCategory.HOCKEY -> Icons.Default.SportsHockey
    SportCategory.BASEBALL -> Icons.Default.SportsBaseball
    SportCategory.ACTION -> Icons.Default.Bolt
}

@Composable
fun sportAccentColor(category: SportCategory): Color {
    val arena = MaterialTheme.arenaColors
    return when (category) {
        SportCategory.INDIAN_SPORTS -> Color(0xFFFF8F00)
        SportCategory.CRICKET -> arena.accentGreen
        SportCategory.SOCCER -> arena.sportBlue
        SportCategory.BASKETBALL -> arena.goldAccent
        SportCategory.RACING -> arena.liveRed
        SportCategory.TENNIS -> Color(0xFF10B981)
        SportCategory.COMBAT -> Color(0xFFF43F5E)
        SportCategory.FOOTBALL -> Color(0xFF8B5CF6)
        SportCategory.HOCKEY -> Color(0xFF06B6D4)
        SportCategory.BASEBALL -> Color(0xFF3B82F6)
        SportCategory.ACTION -> Color(0xFFF97316)
        SportCategory.ALL -> arena.accentGreen
    }
}

fun Modifier.pressableCardClick(onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "press_scale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            onClick = onClick
        )
}

@Composable
fun rememberShimmerBrush(): Brush {
    val arena = MaterialTheme.arenaColors
    val base = arena.surfaceHighlight
    val highlight = if (arena.isDark) Color(0xFF323B4E) else Color(0xFFFFFFFF)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = -300f,
        targetValue = 1100f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_x"
    )
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(translate, 0f),
        end = Offset(translate + 320f, 180f)
    )
}

@Composable
fun ChannelSkeletonList() {
    val arena = MaterialTheme.arenaColors
    val shimmer = rememberShimmerBrush()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(shimmer)
        )
        repeat(5) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = arena.surfaceElevated,
                border = BorderStroke(1.dp, arena.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(shimmer)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.68f)
                                .height(15.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(shimmer)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.42f)
                                .height(11.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(shimmer)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(shimmer)
                    )
                }
            }
        }
    }
}

@Composable
fun MatchSkeletonList() {
    val arena = MaterialTheme.arenaColors
    val shimmer = rememberShimmerBrush()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(shimmer)
        )
        repeat(4) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = arena.surfaceElevated,
                border = BorderStroke(1.dp, arena.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(13.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(shimmer)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmer)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmer)
                    )
                }
            }
        }
    }
}

@Composable
fun SportVisualBadge(
    category: SportCategory,
    size: Dp = 36.dp
) {
    val tint = sportAccentColor(category)
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = sportIconFor(category),
            contentDescription = category.displayName,
            tint = tint,
            modifier = Modifier.size(size * 0.56f)
        )
    }
}

@Composable
fun LiveSignalBars(
    color: Color = MaterialTheme.arenaColors.accentGreen,
    modifier: Modifier = Modifier.size(width = 16.dp, height = 14.dp)
) {
    val transition = rememberInfiniteTransition(label = "live_bars")
    val p1 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar1"
    )
    val p2 by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(340, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar2"
    )
    val p3 by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar3"
    )

    Canvas(modifier = modifier) {
        val barWidth = size.width / 5f
        val gap = barWidth
        val heights = listOf(p1, p2, p3)
        heights.forEachIndexed { index, ratio ->
            val h = size.height * ratio
            val x = index * (barWidth + gap)
            drawRoundRect(
                color = color,
                topLeft = Offset(x, size.height - h),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
fun FeaturedArenaHeroCard(
    featuredChannel: LiveChannel?,
    totalChannelCount: Int,
    liveMatchCount: Int,
    onWatchFeatured: (LiveChannel) -> Unit
) {
    val arena = MaterialTheme.arenaColors
    val pulseTransition = rememberInfiniteTransition(label = "on_air_pulse")
    val dotAlpha by pulseTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(750, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dot_alpha"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = 1.2.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    arena.gradientStart.copy(alpha = 0.85f),
                    arena.gradientMid.copy(alpha = 0.75f),
                    arena.gradientEnd.copy(alpha = 0.85f)
                )
            )
        ),
        shadowElevation = if (arena.isDark) 0.dp else 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(172.dp)
            .then(
                if (featuredChannel != null) {
                    Modifier.pressableCardClick { onWatchFeatured(featuredChannel) }
                } else {
                    Modifier
                }
            )
            .testTag("featured_arena_hero")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.img_arena_hero),
                contentDescription = "Live broadcast arena",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                arena.gradientStart.copy(alpha = 0.82f),
                                arena.gradientMid.copy(alpha = 0.62f),
                                Color(0xCC070B14)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = arena.liveRed,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color.White.copy(alpha = dotAlpha), CircleShape)
                            )
                            Text(
                                text = "ON AIR",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                    ) {
                        Text(
                            text = "$totalChannelCount Feeds • $liveMatchCount Live Matches",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = featuredChannel?.name ?: "PulseArena Multi-Sport Live",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = featuredChannel?.let {
                                val categoryTag = if (it.sport != SportCategory.ALL) {
                                    it.sport.displayName
                                } else {
                                    it.channelCategory.displayName
                                }
                                "$categoryTag • ${it.qualityLabel} Live Stream"
                            } ?: "Indian & Global Sports, News, Movies, Kids & OTT",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.86f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (featuredChannel != null) {
                        Button(
                            onClick = { onWatchFeatured(featuredChannel) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = arena.accentGreen,
                                contentColor = arena.onAccentGreen
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("watch_featured_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Watch", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreSpotlightBanner(
    liveCount: Int,
    upcomingCount: Int,
    finalCount: Int,
    sportTitle: String
) {
    val arena = MaterialTheme.arenaColors
    Surface(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = 1.2.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    arena.gradientStart.copy(alpha = 0.75f),
                    arena.gradientMid.copy(alpha = 0.7f),
                    arena.gradientEnd.copy(alpha = 0.75f)
                )
            )
        ),
        shadowElevation = if (arena.isDark) 0.dp else 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(114.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.img_match_pitch_bg),
                contentDescription = "Scoreboard tactical pitch backdrop",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                arena.gradientStart.copy(alpha = 0.84f),
                                arena.gradientMid.copy(alpha = 0.78f),
                                Color(0xE0070B14)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LiveSignalBars(color = arena.accentGreen)
                        Text(
                            text = "$sportTitle Scorecenter",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Tap any match to open stream + live captions",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatMiniPill(count = liveCount, label = "LIVE", color = arena.liveRed)
                    StatMiniPill(count = upcomingCount, label = "NEXT", color = arena.sportBlue)
                    StatMiniPill(count = finalCount, label = "DONE", color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

@Composable
private fun StatMiniPill(
    count: Int,
    label: String,
    color: Color
) {
    Surface(
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ScoreComparisonBar(
    awayScoreRaw: String,
    homeScoreRaw: String,
    awayColor: Color = MaterialTheme.arenaColors.sportBlue,
    homeColor: Color = MaterialTheme.arenaColors.accentGreen
) {
    val awayVal = awayScoreRaw.substringBefore("/").substringBefore("-").trim().toFloatOrNull() ?: 0f
    val homeVal = homeScoreRaw.substringBefore("/").substringBefore("-").trim().toFloatOrNull() ?: 0f
    val total = awayVal + homeVal
    val targetRatio = if (total > 0f) (awayVal / total).coerceIn(0.12f, 0.88f) else 0.5f
    val awayRatio by animateFloatAsState(
        targetValue = targetRatio,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "score_ratio"
    )
    val trackColor = MaterialTheme.arenaColors.cardBorder

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp))
    ) {
        drawRect(color = trackColor)
        val splitX = size.width * awayRatio
        drawRect(
            color = awayColor,
            topLeft = Offset.Zero,
            size = Size((splitX - 2.dp.toPx()).coerceAtLeast(0f), size.height)
        )
        drawRect(
            color = homeColor,
            topLeft = Offset(splitX + 2.dp.toPx(), 0f),
            size = Size((size.width - splitX - 2.dp.toPx()).coerceAtLeast(0f), size.height)
        )
    }
}

@Composable
fun EmptyBroadcastView(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val arena = MaterialTheme.arenaColors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_broadcast),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(116.dp)
                .clip(RoundedCornerShape(24.dp))
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = arena.textPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = arena.textSecondary
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = arena.accentGreen,
                    contentColor = arena.onAccentGreen
                )
            ) {
                Text(actionLabel, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
