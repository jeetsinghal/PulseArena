package com.example.data.sources

import com.example.data.EventStatusState
import com.example.data.EventSummaryDetail
import com.example.data.SportCategory
import com.example.data.SportEvent
import com.example.data.abstraction.ScoreboardDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class EspnScoreboardDataSource(
    private val httpClient: OkHttpClient
) : ScoreboardDataSource {

    private data class CachedScoreboard(
        val events: List<SportEvent>,
        val timestampMs: Long
    )

    private val scoreboardCache = ConcurrentHashMap<SportCategory, CachedScoreboard>()

    private data class EspnFeedConfig(
        val sport: SportCategory,
        val url: String,
        val defaultLeague: String,
        val isCricketScorepanel: Boolean = false,
        val isIndianLeague: Boolean = false
    )

    private val espnFeeds = listOf(
        EspnFeedConfig(
            sport = SportCategory.CRICKET,
            url = "https://site.web.api.espn.com/apis/site/v2/sports/cricket/scorepanel",
            defaultLeague = "International & Indian Cricket",
            isCricketScorepanel = true
        ),
        EspnFeedConfig(
            sport = SportCategory.SOCCER,
            url = "https://site.api.espn.com/apis/site/v2/sports/soccer/ind.1/scoreboard",
            defaultLeague = "Indian Super League",
            isIndianLeague = true
        ),
        EspnFeedConfig(
            sport = SportCategory.SOCCER,
            url = "https://site.api.espn.com/apis/site/v2/sports/soccer/ind.2/scoreboard",
            defaultLeague = "I-League India",
            isIndianLeague = true
        ),
        EspnFeedConfig(
            sport = SportCategory.SOCCER,
            url = "https://site.api.espn.com/apis/site/v2/sports/soccer/all/scoreboard",
            defaultLeague = "World Football"
        ),
        EspnFeedConfig(
            sport = SportCategory.BASKETBALL,
            url = "https://site.api.espn.com/apis/site/v2/sports/basketball/nba/scoreboard",
            defaultLeague = "NBA"
        ),
        EspnFeedConfig(
            sport = SportCategory.FOOTBALL,
            url = "https://site.api.espn.com/apis/site/v2/sports/football/nfl/scoreboard",
            defaultLeague = "NFL"
        ),
        EspnFeedConfig(
            sport = SportCategory.TENNIS,
            url = "https://site.api.espn.com/apis/site/v2/sports/tennis/atp/scoreboard",
            defaultLeague = "ATP Tour"
        ),
        EspnFeedConfig(
            sport = SportCategory.HOCKEY,
            url = "https://site.api.espn.com/apis/site/v2/sports/hockey/nhl/scoreboard",
            defaultLeague = "NHL"
        ),
        EspnFeedConfig(
            sport = SportCategory.BASEBALL,
            url = "https://site.api.espn.com/apis/site/v2/sports/baseball/mlb/scoreboard",
            defaultLeague = "MLB"
        ),
        EspnFeedConfig(
            sport = SportCategory.RACING,
            url = "https://site.api.espn.com/apis/site/v2/sports/racing/f1/scoreboard",
            defaultLeague = "Formula 1"
        ),
        EspnFeedConfig(
            sport = SportCategory.COMBAT,
            url = "https://site.api.espn.com/apis/site/v2/sports/mma/ufc/scoreboard",
            defaultLeague = "UFC"
        )
    )

    private val indianSportsMarkers = listOf(
        "india", "indian", "ipl", "wpl", "bcci", "ranji", "duleep", "syed mushtaq",
        "vijay hazare", "isl", "i-league", "mumbai", "chennai", "bengaluru", "kolkata",
        "delhi", "rajasthan", "hyderabad", "sunrisers", "punjab", "gujarat", "lucknow",
        "royal challengers", "mohun bagan", "east bengal", "kerala blasters", "fc goa",
        "odisha", "jamshedpur", "northeast united", "mohammedan", "nagal", "bopanna",
        "sindhu", "lakshya", "prannoy"
    )

    override fun getCachedEvents(category: SportCategory): List<SportEvent> {
        scoreboardCache[category]?.let { return it.events }
        val allCached = scoreboardCache[SportCategory.ALL]?.events.orEmpty()
        return when (category) {
            SportCategory.ALL, SportCategory.ACTION -> allCached
            SportCategory.INDIAN_SPORTS -> allCached.filter { it.isIndianOrigin || it.sport == SportCategory.CRICKET }
            else -> allCached.filter { it.sport == category }
        }
    }

    override suspend fun fetchEvents(
        category: SportCategory,
        forceRefresh: Boolean
    ): Result<List<SportEvent>> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cached = scoreboardCache[category]
        if (!forceRefresh && cached != null && (now - cached.timestampMs) < SCOREBOARD_TTL_MS) {
            return@withContext Result.success(cached.events)
        }

        runCatching {
            val targets = when (category) {
                SportCategory.ALL, SportCategory.ACTION -> espnFeeds
                SportCategory.INDIAN_SPORTS -> espnFeeds.filter {
                    it.isIndianLeague || it.isCricketScorepanel || it.sport == SportCategory.TENNIS || it.sport == SportCategory.SOCCER
                }
                else -> espnFeeds.filter { it.sport == category }
            }

            val allEvents = supervisorScope {
                targets.map { feed ->
                    async {
                        runCatching { fetchSingleEspnFeed(feed) }.getOrDefault(emptyList())
                    }
                }.awaitAll().flatten()
            }.distinctBy { "${it.sport.name}_${it.id}" }

            val filtered = if (category == SportCategory.INDIAN_SPORTS) {
                val indianMatches = allEvents.filter { it.isIndianOrigin }
                indianMatches.ifEmpty {
                    allEvents.filter { it.sport == SportCategory.CRICKET }
                }
            } else {
                allEvents
            }

            val sorted = filtered.sortedWith(
                compareBy<SportEvent>(
                    {
                        when (it.statusState) {
                            EventStatusState.LIVE -> 0
                            EventStatusState.UPCOMING -> 1
                            EventStatusState.FINAL -> 2
                        }
                    },
                    { !it.isIndianOrigin },
                    { it.sport.ordinal }
                )
            )
            scoreboardCache[category] = CachedScoreboard(sorted, System.currentTimeMillis())
            sorted
        }
    }

    private fun fetchSingleEspnFeed(feed: EspnFeedConfig): List<SportEvent> {
        val request = Request.Builder()
            .url(feed.url)
            .header("Accept", "application/json")
            .header("User-Agent", "PulseArena/1.0 (Android)")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            val root = JSONObject(body)

            return if (feed.isCricketScorepanel) {
                parseCricketScorepanel(root, feed)
            } else {
                parseStandardScoreboard(root, feed)
            }
        }
    }

    private fun parseCricketScorepanel(root: JSONObject, feed: EspnFeedConfig): List<SportEvent> {
        val results = mutableListOf<SportEvent>()
        val scoresArray = root.optJSONArray("scores")
        if (scoresArray != null) {
            for (i in 0 until scoresArray.length()) {
                val scoreGroup = scoresArray.optJSONObject(i) ?: continue
                val leagueName = scoreGroup.optJSONArray("leagues")
                    ?.optJSONObject(0)
                    ?.optString("name")
                    ?.takeIf { it.isNotBlank() }
                    ?: feed.defaultLeague
                val events = scoreGroup.optJSONArray("events") ?: continue
                for (j in 0 until events.length()) {
                    val eventObj = events.optJSONObject(j) ?: continue
                    parseEventObject(eventObj, SportCategory.CRICKET, leagueName, feed.isIndianLeague)
                        ?.let { results.add(it) }
                }
            }
        }
        val directEvents = root.optJSONArray("events")
        if (directEvents != null) {
            val leagueName = root.optJSONArray("leagues")
                ?.optJSONObject(0)
                ?.optString("name")
                ?.takeIf { it.isNotBlank() }
                ?: feed.defaultLeague
            for (i in 0 until directEvents.length()) {
                val eventObj = directEvents.optJSONObject(i) ?: continue
                parseEventObject(eventObj, SportCategory.CRICKET, leagueName, feed.isIndianLeague)
                    ?.let { results.add(it) }
            }
        }
        return results
    }

    private fun parseStandardScoreboard(root: JSONObject, feed: EspnFeedConfig): List<SportEvent> {
        val results = mutableListOf<SportEvent>()
        val rootLeagueName = root.optJSONArray("leagues")
            ?.optJSONObject(0)
            ?.optString("shortName")
            ?.takeIf { it.isNotBlank() }
            ?: root.optJSONArray("leagues")
                ?.optJSONObject(0)
                ?.optString("name")
                ?.takeIf { it.isNotBlank() }
            ?: feed.defaultLeague

        val events = root.optJSONArray("events") ?: return emptyList()
        for (i in 0 until events.length()) {
            val eventObj = events.optJSONObject(i) ?: continue
            val eventLeague = eventObj.optJSONObject("league")?.optString("shortName")?.takeIf { it.isNotBlank() }
                ?: eventObj.optJSONObject("season")?.optString("slug")?.takeIf { it.isNotBlank() }
                ?: rootLeagueName
            parseEventObject(eventObj, feed.sport, eventLeague, feed.isIndianLeague)
                ?.let { results.add(it) }
        }
        return results
    }

    private fun parseEventObject(
        eventObj: JSONObject,
        sport: SportCategory,
        fallbackLeague: String,
        forceIndianLeague: Boolean
    ): SportEvent? {
        val id = eventObj.optString("id").takeIf { it.isNotBlank() } ?: return null
        val title = eventObj.optString("name", "Live Event")
        val shortName = eventObj.optString("shortName", title)

        val competition = eventObj.optJSONArray("competitions")?.optJSONObject(0)
            ?: eventObj.optJSONArray("groupings")
                ?.optJSONObject(0)
                ?.optJSONArray("competitions")
                ?.optJSONObject(0)
            ?: return null

        val statusObj = competition.optJSONObject("status") ?: eventObj.optJSONObject("status")
        val statusType = statusObj?.optJSONObject("type")
        val stateStr = statusType?.optString("state", "pre")?.lowercase() ?: "pre"
        val statusState = when (stateStr) {
            "in" -> EventStatusState.LIVE
            "post" -> EventStatusState.FINAL
            else -> EventStatusState.UPCOMING
        }
        val statusDetail = statusType?.optString("shortDetail")
            ?.takeIf { it.isNotBlank() }
            ?: statusType?.optString("detail")
            ?.takeIf { it.isNotBlank() }
            ?: statusType?.optString("description")
            ?: "Scheduled"
        val clock = statusObj?.optString("displayClock", "").orEmpty()

        val venueObj = competition.optJSONObject("venue")
        val venueName = venueObj?.optString("fullName")?.takeIf { it.isNotBlank() } ?: fallbackLeague

        val broadcast = competition.optJSONArray("broadcasts")
            ?.optJSONObject(0)
            ?.optJSONArray("names")
            ?.optString(0)
            ?.takeIf { it.isNotBlank() }
            ?: competition.optString("broadcast", "")

        val headline = competition.optJSONArray("headlines")
            ?.optJSONObject(0)
            ?.optString("shortLinkText")
            ?.takeIf { it.isNotBlank() }

        val lastPlayText = competition.optJSONObject("situation")
            ?.optJSONObject("lastPlay")
            ?.optString("text")
            ?.takeIf { it.isNotBlank() }

        val liveCaption = lastPlayText
            ?: statusType?.optString("detail")?.takeIf { it.isNotBlank() }
            ?: headline

        val competitors = competition.optJSONArray("competitors") ?: JSONArray()
        if (competitors.length() == 0) {
            val isIndian = forceIndianLeague || matchesIndianSport("$title $fallbackLeague $venueName")
            return SportEvent(
                id = id,
                sport = sport,
                league = fallbackLeague,
                title = title,
                shortName = shortName,
                homeTeam = shortName,
                awayTeam = fallbackLeague,
                homeAbbr = sport.displayName.take(3).uppercase(),
                awayAbbr = "LIVE",
                homeScore = "-",
                awayScore = "-",
                homeLogo = null,
                awayLogo = null,
                homeRecord = "",
                awayRecord = "",
                statusState = statusState,
                statusDetail = statusDetail,
                clock = clock,
                venue = venueName,
                broadcast = broadcast,
                headline = headline,
                liveCaption = liveCaption,
                isIndianOrigin = isIndian
            )
        }

        var homeObj: JSONObject? = null
        var awayObj: JSONObject? = null
        for (k in 0 until competitors.length()) {
            val comp = competitors.optJSONObject(k) ?: continue
            val side = comp.optString("homeAway")
            if (side == "home" && homeObj == null) homeObj = comp
            else if (side == "away" && awayObj == null) awayObj = comp
        }
        if (homeObj == null) homeObj = competitors.optJSONObject(0)
        if (awayObj == null) awayObj = if (competitors.length() > 1) competitors.optJSONObject(1) else null

        val (hName, hAbbr, hLogo) = extractParticipantInfo(homeObj, "Home")
        val (aName, aAbbr, aLogo) = extractParticipantInfo(awayObj, "Away")
        val isIndian = forceIndianLeague ||
            hAbbr.equals("IND", ignoreCase = true) ||
            aAbbr.equals("IND", ignoreCase = true) ||
            matchesIndianSport("$title $fallbackLeague $hName $aName $venueName")

        return SportEvent(
            id = id,
            sport = sport,
            league = fallbackLeague,
            title = title,
            shortName = shortName,
            homeTeam = hName,
            awayTeam = aName,
            homeAbbr = hAbbr,
            awayAbbr = aAbbr,
            homeScore = extractScore(homeObj),
            awayScore = extractScore(awayObj),
            homeLogo = hLogo,
            awayLogo = aLogo,
            homeRecord = extractRecord(homeObj),
            awayRecord = extractRecord(awayObj),
            statusState = statusState,
            statusDetail = statusDetail,
            clock = clock,
            venue = venueName,
            broadcast = broadcast,
            lineScoresHome = extractLineScores(homeObj),
            lineScoresAway = extractLineScores(awayObj),
            headline = headline,
            liveCaption = liveCaption,
            isIndianOrigin = isIndian
        )
    }

    private fun matchesIndianSport(text: String): Boolean {
        val lower = text.lowercase()
        return indianSportsMarkers.any { lower.contains(it) }
    }

    private fun extractParticipantInfo(comp: JSONObject?, fallback: String): Triple<String, String, String?> {
        if (comp == null) return Triple(fallback, fallback.take(3).uppercase(), null)
        val team = comp.optJSONObject("team")
        if (team != null) {
            val name = team.optString("shortDisplayName")
                .takeIf { it.isNotBlank() }
                ?: team.optString("displayName")
                .takeIf { it.isNotBlank() }
                ?: team.optString("name", fallback)
            val abbr = team.optString("abbreviation")
                .takeIf { it.isNotBlank() }
                ?: name.take(3).uppercase()
            val logo = team.optString("logo").takeIf { it.isNotBlank() }
            return Triple(name, abbr, logo)
        }
        val athlete = comp.optJSONObject("athlete")
        if (athlete != null) {
            val name = athlete.optString("shortName")
                .takeIf { it.isNotBlank() }
                ?: athlete.optString("displayName", fallback)
            val abbr = name.split(" ").lastOrNull()?.take(3)?.uppercase() ?: fallback.take(3).uppercase()
            val logo = athlete.optJSONObject("flag")?.optString("href")?.takeIf { it.isNotBlank() }
            return Triple(name, abbr, logo)
        }
        return Triple(fallback, fallback.take(3).uppercase(), null)
    }

    private fun extractScore(comp: JSONObject?): String {
        if (comp == null) return "-"
        val raw = comp.opt("score")
        return when (raw) {
            is String -> raw.ifBlank { "-" }
            is Number -> raw.toString()
            is JSONObject -> raw.optString("displayValue", "-")
            else -> "-"
        }
    }

    private fun extractRecord(comp: JSONObject?): String {
        return comp?.optJSONArray("records")
            ?.optJSONObject(0)
            ?.optString("summary")
            .orEmpty()
    }

    private fun extractLineScores(comp: JSONObject?): List<String> {
        val arr = comp?.optJSONArray("linescores") ?: return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val valStr = item.optString("displayValue")
                .takeIf { it.isNotBlank() }
                ?: item.opt("value")?.toString()
                ?: continue
            list.add(valStr)
        }
        return list
    }

    override suspend fun fetchEventSummary(sport: SportCategory, eventId: String): EventSummaryDetail? =
        withContext(Dispatchers.IO) {
            val path = when (sport) {
                SportCategory.SOCCER -> "soccer/all"
                SportCategory.BASKETBALL -> "basketball/nba"
                SportCategory.CRICKET -> "cricket/8048"
                SportCategory.INDIAN_SPORTS -> "cricket/8048"
                SportCategory.FOOTBALL -> "football/nfl"
                SportCategory.TENNIS -> "tennis/atp"
                SportCategory.HOCKEY -> "hockey/nhl"
                SportCategory.BASEBALL -> "baseball/mlb"
                SportCategory.RACING -> "racing/f1"
                SportCategory.COMBAT -> "mma/ufc"
                else -> return@withContext null
            }
            val url = "https://site.api.espn.com/apis/site/v2/sports/$path/summary?event=$eventId"
            runCatching {
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .build()
                httpClient.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) return@runCatching null
                    val body = resp.body?.string() ?: return@runCatching null
                    val root = JSONObject(body)

                    val notes = root.optJSONObject("gameInfo")
                        ?.optJSONObject("venue")
                        ?.optString("fullName")
                        ?.takeIf { it.isNotBlank() }

                    val leadersList = mutableListOf<String>()
                    val leadersArr = root.optJSONArray("leaders")
                    if (leadersArr != null) {
                        for (i in 0 until minOf(leadersArr.length(), 2)) {
                            val teamLeader = leadersArr.optJSONObject(i) ?: continue
                            val teamName = teamLeader.optJSONObject("team")?.optString("abbreviation").orEmpty()
                            val cats = teamLeader.optJSONArray("leaders") ?: continue
                            for (j in 0 until minOf(cats.length(), 2)) {
                                val cat = cats.optJSONObject(j) ?: continue
                                val catName = cat.optString("shortDisplayName", cat.optString("name", ""))
                                val top = cat.optJSONArray("leaders")?.optJSONObject(0) ?: continue
                                val athlete = top.optJSONObject("athlete")?.optString("shortName").orEmpty()
                                val disp = top.optString("displayValue").orEmpty()
                                if (athlete.isNotBlank() && disp.isNotBlank()) {
                                    leadersList.add("$teamName • $catName: $athlete ($disp)")
                                }
                            }
                        }
                    }

                    val playsList = mutableListOf<String>()
                    val playsArr = root.optJSONArray("plays")
                    if (playsArr != null) {
                        val start = maxOf(0, playsArr.length() - 8)
                        for (i in playsArr.length() - 1 downTo start) {
                            val p = playsArr.optJSONObject(i) ?: continue
                            val text = p.optString("text").takeIf { it.isNotBlank() } ?: continue
                            val clock = p.optJSONObject("clock")?.optString("displayValue").orEmpty()
                            playsList.add(if (clock.isNotBlank()) "[$clock] $text" else text)
                        }
                    }

                    EventSummaryDetail(
                        eventId = eventId,
                        notes = notes,
                        leaders = leadersList,
                        recentPlays = playsList
                    )
                }
            }.getOrNull()
        }

    private companion object {
        const val SCOREBOARD_TTL_MS = 25_000L
    }
}
