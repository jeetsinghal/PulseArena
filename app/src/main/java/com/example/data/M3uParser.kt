package com.example.data

object M3uParser {

    private val tvgIdRegex = Regex("""tvg-id="([^"]*)"""")
    private val tvgLogoRegex = Regex("""tvg-logo="([^"]*)"""")
    private val groupTitleRegex = Regex("""group-title="([^"]*)"""")
    private val qualityRegex = Regex("""\((\d+p)\)""", RegexOption.IGNORE_CASE)

    private val sportsStreamKeywords = listOf(
        "sport", "cricket", "cric", "ipl", "wpl", "dd sports", "ddsports",
        "star sports", "sony ten", "sony sports", "sports18", "fancode", "eurosport",
        "willow", "soccer", "football", "fifa", "nba", "tennis", "ufc", "mma",
        "wrestl", "kabaddi", "hockey", "racing", "motorsport", "f1", "golf", "badminton"
    )

    private val indianChannelMarkers = listOf(
        "dd sports", "ddsports", "star sports", "sony sports", "sony ten",
        "sports18", "fancode", "india", "indian", "ipl", "wpl", "bcci", "kabaddi", "willow"
    )

    fun parse(
        m3uContent: String,
        filterSportsOnly: Boolean = false,
        markAllAsIndian: Boolean = false
    ): List<LiveChannel> {
        val lines = m3uContent.lines()
        val channels = mutableListOf<LiveChannel>()
        val seenUrls = HashSet<String>()

        var currentName: String? = null
        var currentLogo: String? = null
        var currentGroup = "Sports"
        var currentId = ""
        var currentUserAgent: String? = null
        var currentReferrer: String? = null

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            when {
                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    currentId = tvgIdRegex.find(line)?.groupValues?.getOrNull(1).orEmpty()
                    currentLogo = tvgLogoRegex.find(line)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
                    currentGroup = groupTitleRegex.find(line)?.groupValues?.getOrNull(1)?.ifBlank { "Sports" } ?: "Sports"
                    val commaIndex = line.lastIndexOf(',')
                    currentName = if (commaIndex != -1 && commaIndex < line.length - 1) {
                        line.substring(commaIndex + 1).trim()
                    } else {
                        "Live Sports Channel"
                    }
                    currentUserAgent = null
                    currentReferrer = null
                }

                line.startsWith("#EXTVLCOPT:http-user-agent=", ignoreCase = true) -> {
                    currentUserAgent = line.substringAfter("=").trim().takeIf { it.isNotEmpty() }
                }

                line.startsWith("#EXTVLCOPT:http-referrer=", ignoreCase = true) ||
                    line.startsWith("#EXTVLCOPT:http-referer=", ignoreCase = true) -> {
                    currentReferrer = line.substringAfter("=").trim().takeIf { it.isNotEmpty() }
                }

                line.startsWith("#") -> {
                    // Ignore other M3U directives
                }

                line.startsWith("http://", ignoreCase = true) || line.startsWith("https://", ignoreCase = true) -> {
                    val rawName = currentName
                    if (rawName != null && seenUrls.add(line)) {
                        val isGeoBlocked = rawName.contains("[Geo-blocked]", ignoreCase = true)
                        val cleanName = rawName
                            .replace("[Geo-blocked]", "", ignoreCase = true)
                            .replace("[Not 24/7]", "", ignoreCase = true)
                            .trim()

                        val shouldInclude = if (filterSportsOnly) {
                            isSportsChannel(cleanName, currentGroup, currentId)
                        } else {
                            true
                        }

                        if (shouldInclude) {
                            val quality = qualityRegex.find(cleanName)?.groupValues?.getOrNull(1) ?: "LIVE"
                            val isIndian = markAllAsIndian || isIndianSportsChannel(cleanName, currentGroup, currentId)
                            val sport = classifySport(cleanName, currentGroup, isIndian)

                            channels.add(
                                LiveChannel(
                                    id = currentId.ifBlank { "ch_${channels.size + 1}" } + "_${channels.size}",
                                    name = cleanName.ifBlank { rawName },
                                    streamUrl = line,
                                    logoUrl = currentLogo,
                                    groupTitle = currentGroup,
                                    sport = sport,
                                    userAgent = currentUserAgent,
                                    referrer = currentReferrer,
                                    isHttps = line.startsWith("https://", ignoreCase = true),
                                    isGeoRestrictedHint = isGeoBlocked,
                                    qualityLabel = quality,
                                    isIndianOrigin = isIndian
                                )
                            )
                        }
                    }
                    currentName = null
                    currentLogo = null
                    currentUserAgent = null
                    currentReferrer = null
                }
            }
        }

        return sortChannels(channels)
    }

    fun sortChannels(channels: List<LiveChannel>): List<LiveChannel> {
        return channels.sortedWith(
            compareBy<LiveChannel>(
                { it.isGeoRestrictedHint },
                { !it.isHttps },
                { !isPopularSports(it.name, it.isIndianOrigin) },
                { it.name.lowercase() }
            )
        )
    }

    private fun isSportsChannel(name: String, group: String, tvgId: String): Boolean {
        val text = "$name $group $tvgId".lowercase()
        return sportsStreamKeywords.any { text.contains(it) }
    }

    private fun isIndianSportsChannel(name: String, group: String, tvgId: String): Boolean {
        val lowerId = tvgId.lowercase()
        if (lowerId.endsWith(".in") || lowerId.contains(".in@")) return true
        val text = "$name $group".lowercase()
        return indianChannelMarkers.any { text.contains(it) }
    }

    private fun isPopularSports(name: String, isIndian: Boolean): Boolean {
        if (isIndian) return true
        val lower = name.lowercase()
        return lower.contains("red bull") ||
            lower.contains("fifa") ||
            lower.contains("olympic") ||
            lower.contains("racing") ||
            lower.contains("motorsport") ||
            lower.contains("tennis") ||
            lower.contains("fuel") ||
            lower.contains("unbeaten") ||
            lower.contains("trace sport") ||
            lower.contains("sports first") ||
            lower.contains("cricket") ||
            lower.contains("nba") ||
            lower.contains("ufc") ||
            lower.contains("hard knocks")
    }

    private fun classifySport(name: String, group: String, isIndian: Boolean): SportCategory {
        val text = "$name $group".lowercase()
        val specific = SportCategory.entries
            .filter { it != SportCategory.ALL && it != SportCategory.INDIAN_SPORTS }
            .firstOrNull { cat -> cat.keywords.any { kw -> text.contains(kw) } }
        return specific ?: if (isIndian) SportCategory.INDIAN_SPORTS else SportCategory.ALL
    }
}
