package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.EventStatusState
import com.example.data.LiveChannel
import com.example.data.SportEvent
import com.example.ui.theme.arenaColors

@Composable
fun LiveChannelCard(
    channel: LiveChannel,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val arena = MaterialTheme.arenaColors
    val accent = sportAccentColor(channel.sport)
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = arena.surfaceElevated,
        border = BorderStroke(1.dp, arena.cardBorder),
        shadowElevation = if (arena.isDark) 0.dp else 1.5.dp,
        modifier = modifier
            .fillMaxWidth()
            .pressableCardClick(onPlay)
            .testTag("channel_card_${channel.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(arena.surfaceHighlight),
                contentAlignment = Alignment.Center
            ) {
                if (channel.logoUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(channel.logoUrl)
                            .crossfade(180)
                            .build(),
                        contentDescription = channel.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    SportVisualBadge(category = channel.sport, size = 50.dp)
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = arena.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LiveSignalBars(color = accent, modifier = Modifier.size(width = 12.dp, height = 10.dp))
                    Surface(
                        color = accent.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text(
                            text = channel.sport.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (channel.isIndianOrigin) {
                        Surface(
                            color = androidx.compose.ui.graphics.Color(0xFFFF8F00).copy(alpha = 0.16f),
                            shape = RoundedCornerShape(5.dp)
                        ) {
                            Text(
                                text = "INDIA",
                                style = MaterialTheme.typography.labelSmall,
                                color = androidx.compose.ui.graphics.Color(0xFFFF8F00),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = buildString {
                            append(channel.qualityLabel)
                            if (channel.isGeoRestrictedHint) append(" • Regional")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = arena.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = arena.accentGreen.copy(alpha = 0.14f),
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onPlay)
                    .minimumInteractiveComponentSize()
                    .testTag("play_channel_${channel.id}")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Watch ${channel.name}",
                        tint = arena.accentGreen
                    )
                }
            }
        }
    }
}

@Composable
fun SportEventCard(
    event: SportEvent,
    onWatchLive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val arena = MaterialTheme.arenaColors
    val sportColor = sportAccentColor(event.sport)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = arena.surfaceElevated,
        border = BorderStroke(1.dp, arena.cardBorder),
        shadowElevation = if (arena.isDark) 0.dp else 1.5.dp,
        modifier = modifier
            .fillMaxWidth()
            .pressableCardClick(onWatchLive)
            .testTag("event_card_${event.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    SportVisualBadge(category = event.sport, size = 28.dp)
                    Text(
                        text = "${event.sport.displayName} • ${event.league}",
                        style = MaterialTheme.typography.labelMedium,
                        color = arena.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (event.isIndianOrigin) {
                        Surface(
                            color = androidx.compose.ui.graphics.Color(0xFFFF8F00).copy(alpha = 0.16f),
                            shape = RoundedCornerShape(5.dp)
                        ) {
                            Text(
                                text = "INDIA",
                                style = MaterialTheme.typography.labelSmall,
                                color = androidx.compose.ui.graphics.Color(0xFFFF8F00),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (event.statusState == EventStatusState.LIVE) {
                        arena.liveRed.copy(alpha = 0.14f)
                    } else {
                        arena.surfaceHighlight
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (event.statusState == EventStatusState.LIVE) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(arena.liveRed, CircleShape)
                            )
                        }
                        Text(
                            text = if (event.statusState == EventStatusState.LIVE) {
                                "LIVE ${event.statusDetail}"
                            } else {
                                event.statusDetail
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (event.statusState == EventStatusState.LIVE) arena.liveRed else arena.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TeamScoreRow(
                name = event.awayTeam,
                abbr = event.awayAbbr,
                logoUrl = event.awayLogo,
                record = event.awayRecord,
                score = event.awayScore,
                isLive = event.statusState == EventStatusState.LIVE
            )

            Spacer(modifier = Modifier.height(8.dp))

            TeamScoreRow(
                name = event.homeTeam,
                abbr = event.homeAbbr,
                logoUrl = event.homeLogo,
                record = event.homeRecord,
                score = event.homeScore,
                isLive = event.statusState == EventStatusState.LIVE
            )

            if (event.statusState == EventStatusState.LIVE || event.statusState == EventStatusState.FINAL) {
                Spacer(modifier = Modifier.height(10.dp))
                ScoreComparisonBar(
                    awayScoreRaw = event.awayScore,
                    homeScoreRaw = event.homeScore,
                    awayColor = sportColor,
                    homeColor = arena.accentGreen
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = arena.cardBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (event.broadcast.isNotBlank()) "${event.venue} • ${event.broadcast}" else event.venue,
                    style = MaterialTheme.typography.bodySmall,
                    color = arena.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalButton(
                    onClick = onWatchLive,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = arena.accentGreen.copy(alpha = 0.14f),
                        contentColor = arena.accentGreen
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (event.statusState == EventStatusState.LIVE) "Watch Live" else "Watch",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamScoreRow(
    name: String,
    abbr: String,
    logoUrl: String?,
    record: String,
    score: String,
    isLive: Boolean
) {
    val arena = MaterialTheme.arenaColors
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(arena.surfaceHighlight),
                contentAlignment = Alignment.Center
            ) {
                if (logoUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(logoUrl)
                            .crossfade(180)
                            .build(),
                        contentDescription = name,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(3.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = abbr.take(3),
                        style = MaterialTheme.typography.labelSmall,
                        color = arena.textPrimary
                    )
                }
            }
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = arena.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (record.isNotBlank()) {
                    Text(
                        text = record,
                        style = MaterialTheme.typography.labelSmall,
                        color = arena.textSecondary
                    )
                }
            }
        }

        AnimatedContent(
            targetState = score,
            transitionSpec = {
                (slideInVertically { -it / 2 } + fadeIn()) togetherWith
                    (slideOutVertically { it / 2 } + fadeOut())
            },
            label = "score_change"
        ) { animatedScore ->
            Text(
                text = animatedScore,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isLive) arena.textPrimary else arena.textSecondary
            )
        }
    }
}
