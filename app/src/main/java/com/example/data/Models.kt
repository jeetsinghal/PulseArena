package com.example.data

enum class SportCategory(
    val displayName: String,
    val keywords: List<String>
) {
    ALL("All", emptyList()),
    INDIAN_SPORTS(
        "Indian Sports",
        listOf(
            "india", "indian", "dd sports", "ddsports", "ipl", "wpl", "bcci",
            "isl", "indian super league", "i-league", "kabaddi", "pro kabaddi",
            "pkl", "kho kho", "ranji", "star sports", "sony sports", "sony ten",
            "sports18", "fancode"
        )
    ),
    CRICKET("Cricket", listOf("cricket", "ipl", "wpl", "willow", "cric", "t20", "icc", "bcci", "ranji")),
    SOCCER("Soccer", listOf("soccer", "football", "fifa", "fc", "liga", "premier", "uefa", "goal", "real madrid", "barca", "mutv", "isl")),
    BASKETBALL("NBA", listOf("nba", "basket", "hoop", "wnba", "fiba", "ncaa")),
    RACING("Motorsport", listOf("racing", "motor", "f1", "formula", "nascar", "rally", "drift", "speed", "auto", "red bull")),
    TENNIS("Tennis", listOf("tennis", "atp", "wta", "court", "grand slam", "padel")),
    COMBAT("Combat", listOf("ufc", "mma", "box", "fight", "wrestl", "karate", "glory", "kickbox", "impact", "hard knocks")),
    FOOTBALL("NFL", listOf("nfl", "gridiron", "cfb", "touchdown")),
    HOCKEY("NHL", listOf("nhl", "hockey", "ice")),
    BASEBALL("MLB", listOf("mlb", "baseball", "softball")),
    ACTION("Action", listOf("extreme", "surf", "skate", "snow", "ski", "outdoor", "fuel", "edge", "unbeaten", "trace", "olympic"))
}

enum class ChannelCategory(val displayName: String) {
    ALL("All Channels"),
    SPORTS("Sports"),
    NEWS("News"),
    MOVIES("Movies"),
    SONGS("Songs"),
    KIDS("Kids"),
    OTT("OTT Channels")
}

enum class ChannelFeedFilter(val displayName: String) {
    ALL("All"),
    INDIAN_SPORTS("India Sports"),
    HINDI("Hindi"),
    ENGLISH("English"),
    REGIONAL("Regional"),
    CRICKET("Cricket"),
    SOCCER("Football"),
    ALL_SPORTS("All Sports")
}

enum class EventStatusState {
    LIVE, UPCOMING, FINAL
}

data class LiveChannel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String?,
    val groupTitle: String,
    val sport: SportCategory,
    val userAgent: String? = null,
    val referrer: String? = null,
    val isHttps: Boolean = streamUrl.startsWith("https://", ignoreCase = true),
    val isGeoRestrictedHint: Boolean = false,
    val isBackupTestStream: Boolean = false,
    val qualityLabel: String = "LIVE",
    val isIndianOrigin: Boolean = false,
    val languageTag: String = "",
    val channelCategory: ChannelCategory = ChannelCategory.OTT
)

data class SportEvent(
    val id: String,
    val sport: SportCategory,
    val league: String,
    val title: String,
    val shortName: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeAbbr: String,
    val awayAbbr: String,
    val homeScore: String,
    val awayScore: String,
    val homeLogo: String?,
    val awayLogo: String?,
    val homeRecord: String,
    val awayRecord: String,
    val statusState: EventStatusState,
    val statusDetail: String,
    val clock: String,
    val venue: String,
    val broadcast: String,
    val lineScoresHome: List<String> = emptyList(),
    val lineScoresAway: List<String> = emptyList(),
    val headline: String? = null,
    val liveCaption: String? = null,
    val isIndianOrigin: Boolean = false
)

data class EventSummaryDetail(
    val eventId: String,
    val notes: String?,
    val leaders: List<String>,
    val recentPlays: List<String>
)

data class VideoQualityOption(
    val id: String,
    val label: String,
    val maxWidth: Int,
    val maxHeight: Int
)

data class AudioTrackOption(
    val id: String,
    val label: String,
    val languageCode: String,
    val groupIndex: Int,
    val trackIndex: Int,
    val isSelected: Boolean
)

data class SubtitleTrackOption(
    val id: String,
    val label: String,
    val languageCode: String,
    val groupIndex: Int,
    val trackIndex: Int,
    val isSelected: Boolean
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
