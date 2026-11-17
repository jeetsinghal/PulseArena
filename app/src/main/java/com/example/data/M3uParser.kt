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

    private val kidsKeywords = listOf(
        "kids", "cartoon", "pogo", "nick", "nickelodeon", "disney", "hungama",
        "sonic", "chhota", "baby", "toon", "anim", "discovery kids", "sony yay",
        "yay!", "gubbare", "bal bharat", "chintu", "kushi", "kochu", "pbs kids",
        "moonbug", "cocomelon", "peppa", "boomerang", "cbeebies", "junior"
    )

    private val songsKeywords = listOf(
        "music", "song", "gaana", "9xm", "9x tashan", "9x jalwa", "9x jhakaas",
        "mtv", "vh1", "mastiii", "zing", "b4u music", "sangeet", "tadka", "pitaara",
        "yrf", "t-series", "sony mix", "zoom", "hits", "melody", "sun music",
        "gemini music", "udaya music", "raj music", "isai", "vevo", "stingray",
        "radio", "beats", "tunes", "ramdhenu"
    )

    private val newsKeywords = listOf(
        "news", "aaj tak", "abp", "ndtv", "tv9", "republic", "wion", "times now",
        "mirror now", "cnbc", "et now", "bbc", "al jazeera", "cnn", "news18",
        "zee news", "zee hindustan", "sansad", "dd news", "dd india", "samachar",
        "india tv", "good news today", "newsx", "news9", "india ahead", "polimer",
        "puthiya", "thanthi", "sathiyam", "sakshi", "hmtv", "10tv", "manorama",
        "mathrubhumi", "mediaone", "reporter", "janam", "public tv", "24 ghanta",
        "saam", "lokshahi", "sandesh", "otv", "kanak", "prameya", "dy365", "prag",
        "bloomberg", "euronews", "france 24", "dw ", "cna", "nhk"
    )

    private val movieKeywords = listOf(
        "movie", "cinema", "film", "goldmines", "b4u movies", "b4u kadak", "shemaroo",
        "filamchi", "zee cinema", "zee classic", "zee action", "zee bollywood",
        "star gold", "sony max", "sony wah", "utv movies", "utv action",
        "colors cineplex", "&pictures", "and pictures", "bollywood", "tollywood",
        "kollywood", "mollywood", "bhojpuri cinema", "dabangg", "enterr10",
        "fakt marathi", "tarang", "flix", "blockbuster", "box office"
    )

    private val ottPlatformMarkers = listOf(
        "ott", "pluto", "plex", "rakuten", "samsung", "distro", "xumo", "tubi",
        "wurl", "amagi", "jiotv", "mx player", "shemaroo me", "hungama play",
        "fast", "tv plus", "stream", "web", "entertainment", "general", "comedy",
        "drama", "lifestyle", "documentary", "discovery", "history", "nat geo",
        "nasa", "star plus", "star bharat", "zee tv", "colors", "sony sab",
        "sony pal", "dangal", "ishara", "nazara", "dd national", "dd bharati",
        "sun tv", "asianet", "etv", "gemini", "udaya", "ptc", "vijay", "surya",
        "mazhavil", "zee bangla", "zee marathi", "star jalsha", "star pravah"
    )

    private val unplayableUrlMarkers = listOf(
        "youtube.com", "youtu.be", "twitch.tv", "dailymotion.com", "vimeo.com",
        "facebook.com", ".html", ".htm", ".php", ".aspx"
    )

    private val indianChannelMarkers = listOf(
        "dd sports", "ddsports", "star sports", "sony sports", "sony ten",
        "sports18", "fancode", "india", "indian", "ipl", "wpl", "bcci", "kabaddi",
        "willow", "aaj tak", "abp", "zee", "colors", "ndtv", "republic", "times now",
        "mirror now", "wion", "dd national", "dd news", "dd india", "sansad", "sun tv",
        "asianet", "etv", "gemini", "udaya", "ptc", "tarang", "dangal", "shemaroo", "b4u",
        "9xm", "goldmines", "mastiii", "pogo", "hungama", "filamchi"
    )

    private val hindiMarkers = listOf(
        "hindi", "hin", "aaj tak", "abp news", "abp ganga", "zee news", "zee hindustan",
        "zee classic", "zee cinema", "zee tv", "star plus", "star gold", "star bharat",
        "sony sab", "sony max", "sony pal", "colors", "ndtv india", "news18 india",
        "republic bharat", "india tv", "good news today", "sansad", "dd national",
        "dd news", "dd bharati", "dd kisan", "dd retro", "b4u", "9xm", "shemaroo",
        "dangal", "ishara", "nazara", "sports18 khel", "goldmines", "enterr10", "mastiii"
    )

    private val indianEnglishMarkers = listOf(
        "wion", "ndtv 24x7", "ndtv profit", "republic tv", "republic world", "cnn-news18",
        "cnn news18", "india today", "times now", "mirror now", "et now", "cnbc-tv18",
        "cnbc tv18", "dd india", "news9", "newsx", "india ahead", "tv9", "english"
    )

    private val regionalLanguageRules: List<Pair<String, List<String>>> = listOf(
        "Tamil" to listOf("tamil", "tam", "sun tv", "sun news", "sun music", "vijay", "jaya", "kalaignar", "polimer", "puthiya", "thanthi", "raj tv", "dd podhigai", "dd tamil", "sathiyam", "news7 tamil"),
        "Telugu" to listOf("telugu", "tel", "etv", "gemini", "maa", "sakshi", "tv9 telugu", "ntv telugu", "v6", "t news", "dd yadagiri", "dd saptagiri", "abn", "hmtv", "10tv"),
        "Malayalam" to listOf("malayalam", "mal", "asianet", "mazhavil", "surya", "kairali", "amrita", "reporter", "mediaone", "manorama", "mathrubhumi", "jeevan", "kaumudy", "dd malayalam", "janam"),
        "Kannada" to listOf("kannada", "kan", "udaya", "suvarna", "tv9 kannada", "public tv", "kasturi", "dd chandana", "news18 kannada", "power tv", "sri sankara"),
        "Bengali" to listOf("bengali", "bangla", "ben", "zee bangla", "star jalsha", "abp ananda", "24 ghanta", "calcutta", "kolkata", "dd bangla", "sangeet bangla", "ruposhi", "akash aath", "ctvn"),
        "Marathi" to listOf("marathi", "mar", "zee marathi", "star pravah", "abp majha", "saam", "lokshahi", "dd sahyadri", "fakt marathi", "sangeet marathi", "news18 lokmat", "jai maharashtra"),
        "Punjabi" to listOf("punjabi", "pan", "ptc", "mh one", "pitaara", "tadka", "dd punjabi", "chardikla", "living india", "9x tashan"),
        "Gujarati" to listOf("gujarati", "guj", "tv9 gujarati", "sandesh", "vtv gujarati", "abp asmita", "dd girnar", "gstv", "mantavya"),
        "Odia" to listOf("odia", "ori", "oriya", "otv", "kanak", "tarang", "kalinga", "dd odia", "mbc tv", "prameya"),
        "Bhojpuri" to listOf("bhojpuri", "bho", "b4u bhojpuri", "dabangg", "filamchi", "zee ganga", "bhojpuri cinema"),
        "Assamese" to listOf("assam", "asm", "prag", "dy365", "protidin", "rang", "news live", "jonack", "ramdhenu"),
        "Urdu" to listOf("urdu", "urd", "dd kashir", "dd urdu", "gulistan", "e-tv urdu", "munsif")
    )

    private val foreignCountrySuffixes = listOf(
        ".es@", ".br@", ".fr@", ".de@", ".ar@", ".ru@", ".tr@", ".it@", ".pl@",
        ".nl@", ".pt@", ".jp@", ".kr@", ".cn@", ".tw@", ".th@", ".vn@", ".id@",
        ".sa@", ".eg@", ".ma@", ".ro@", ".gr@", ".se@", ".no@", ".dk@", ".fi@",
        ".cz@", ".hu@", ".bg@", ".ua@", ".kz@", ".ir@", ".mx@", ".co@", ".cl@",
        ".pe@", ".ve@", ".bo@"
    )

    private val foreignLanguageLabels = listOf(
        "[spanish]", "[portuguese]", "[french]", "[german]", "[arabic]", "[turkish]",
        "[italian]", "[russian]", "[polish]", "[dutch]", "[japanese]", "[korean]",
        "[chinese]", "[thai]", "[vietnamese]", "[persian]", "[romanian]", "[greek]",
        "español", "français", "deutsch", "português", "italiano", "türkçe"
    )

    private val relevantEnglishBroadKeywords = listOf(
        "sport", "cricket", "ipl", "soccer", "football", "fifa", "nba", "tennis",
        "ufc", "mma", "racing", "motorsport", "f1", "red bull", "fuel", "golf",
        "india", "indian", "wion", "ndtv", "republic", "news18", "times now",
        "mirror now", "bbc", "al jazeera", "dw ", "france 24", "cna", "bloomberg",
        "cnbc", "euronews", "nhk", "nasa", "nat geo", "discovery", "history",
        "music", "mtv", "vh1", "vevo", "stingray", "movie", "cinema", "classic",
        "kids", "cartoon", "moonbug", "pbs kids", "pluto", "plex", "rakuten",
        "samsung", "distro", "amagi", "wurl"
    )

    fun parse(
        m3uContent: String,
        filterSportsOnly: Boolean = false,
        markAllAsIndian: Boolean = false,
        defaultLanguageTag: String = "",
        keepOnlyIndianOrEnglishSports: Boolean = false,
        keepOnlyRelevantEnglish: Boolean = false,
        defaultChannelCategory: ChannelCategory? = null,
        excludeUnplayableOrGeoBlocked: Boolean = false
    ): List<LiveChannel> {
        val lines = m3uContent.lines()
        val channels = mutableListOf<LiveChannel>()
        val seenUrls = HashSet<String>()

        var currentName: String? = null
        var currentLogo: String? = null
        var currentGroup = "Live TV"
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
                    currentGroup = groupTitleRegex.find(line)?.groupValues?.getOrNull(1)?.ifBlank { "Live TV" } ?: "Live TV"
                    val commaIndex = line.lastIndexOf(',')
                    currentName = if (commaIndex != -1 && commaIndex < line.length - 1) {
                        line.substring(commaIndex + 1).trim()
                    } else {
                        "Live Channel"
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
                        val playableUrl = isPlayableDirectStreamUrl(line)

                        if (!excludeUnplayableOrGeoBlocked || (!isGeoBlocked && playableUrl)) {
                            val cleanName = rawName
                                .replace("[Geo-blocked]", "", ignoreCase = true)
                                .replace("[Not 24/7]", "", ignoreCase = true)
                                .trim()

                            val isIndian = markAllAsIndian || isIndianChannel(cleanName, currentGroup, currentId)
                            val detectedLanguage = detectLanguageTag(
                                name = cleanName,
                                group = currentGroup,
                                tvgId = currentId,
                                isIndian = isIndian,
                                defaultLanguageTag = defaultLanguageTag
                            )

                            val shouldInclude = when {
                                filterSportsOnly -> isSportsChannel(cleanName, currentGroup, currentId)
                                keepOnlyIndianOrEnglishSports -> isIndian || isIndianOrEnglishChannel(cleanName, currentGroup, currentId)
                                keepOnlyRelevantEnglish -> isIndian || isRelevantEnglishChannel(cleanName, currentGroup, currentId, line)
                                else -> true
                            }

                            if (shouldInclude) {
                                val quality = qualityRegex.find(cleanName)?.groupValues?.getOrNull(1) ?: "LIVE"
                                val sport = classifySport(cleanName, currentGroup, isIndian)
                                val category = classifyChannelCategory(
                                    name = cleanName,
                                    group = currentGroup,
                                    tvgId = currentId,
                                    streamUrl = line,
                                    sport = sport,
                                    fallbackCategory = defaultChannelCategory
                                )

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
                                        isIndianOrigin = isIndian,
                                        languageTag = detectedLanguage,
                                        channelCategory = category
                                    )
                                )
                            }
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

    fun isPlayableDirectStreamUrl(url: String): Boolean {
        val lower = url.trim().lowercase()
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) return false
        return unplayableUrlMarkers.none { lower.contains(it) }
    }

    fun classifyChannelCategory(
        name: String,
        group: String,
        tvgId: String = "",
        streamUrl: String = "",
        sport: SportCategory = SportCategory.ALL,
        fallbackCategory: ChannelCategory? = null
    ): ChannelCategory {
        val text = "$name $group $tvgId".lowercase()
        val urlLower = streamUrl.lowercase()

        if (kidsKeywords.any { text.contains(it) }) return ChannelCategory.KIDS
        if (songsKeywords.any { text.contains(it) }) return ChannelCategory.SONGS
        if (sport != SportCategory.ALL || isSportsChannel(name, group, tvgId)) return ChannelCategory.SPORTS
        if (newsKeywords.any { text.contains(it) }) return ChannelCategory.NEWS
        if (movieKeywords.any { text.contains(it) }) return ChannelCategory.MOVIES
        if (fallbackCategory != null && fallbackCategory != ChannelCategory.ALL) return fallbackCategory
        if (ottPlatformMarkers.any { text.contains(it) || urlLower.contains(it) }) return ChannelCategory.OTT
        return ChannelCategory.OTT
    }

    fun sortChannels(channels: List<LiveChannel>): List<LiveChannel> {
        return channels.sortedWith(
            compareBy<LiveChannel>(
                { it.isGeoRestrictedHint },
                { !it.isHttps },
                { !(it.isIndianOrigin && it.channelCategory == ChannelCategory.SPORTS) },
                { !it.isIndianOrigin },
                { !isPopularSports(it.name, it.isIndianOrigin) },
                { it.name.lowercase() }
            )
        )
    }

    fun isRegionalLanguage(languageTag: String): Boolean {
        return languageTag.isNotBlank() &&
            languageTag != "Hindi" &&
            languageTag != "English"
    }

    private fun isSportsChannel(name: String, group: String, tvgId: String): Boolean {
        val text = "$name $group $tvgId".lowercase()
        return sportsStreamKeywords.any { text.contains(it) }
    }

    private fun isIndianChannel(name: String, group: String, tvgId: String): Boolean {
        val lowerId = tvgId.lowercase()
        if (lowerId.endsWith(".in") || lowerId.contains(".in@")) return true
        val text = "$name $group".lowercase()
        return indianChannelMarkers.any { text.contains(it) }
    }

    private fun isForeignLanguageChannel(name: String, group: String, tvgId: String): Boolean {
        val idLower = tvgId.lowercase()
        if (foreignCountrySuffixes.any { idLower.contains(it) }) return true
        val textLower = "$name $group".lowercase()
        if (foreignLanguageLabels.any { textLower.contains(it) }) return true
        val hasUnsupportedScript = name.any { ch ->
            val code = ch.code
            (code in 0x0400..0x04FF) || // Cyrillic
                (code in 0x0600..0x06FF) || // Arabic
                (code in 0x4E00..0x9FFF) || // CJK
                (code in 0x0E00..0x0E7F)    // Thai
        }
        return hasUnsupportedScript
    }

    private fun isIndianOrEnglishChannel(name: String, group: String, tvgId: String): Boolean {
        if (isForeignLanguageChannel(name, group, tvgId)) return false
        val idLower = tvgId.lowercase()
        val text = "$name $group".lowercase()
        if (idLower.contains(".in@") || idLower.endsWith(".in")) return true
        if (idLower.contains(".uk@") || idLower.contains(".us@") || idLower.contains(".au@") || idLower.contains(".ca@") || idLower.contains(".nz@")) {
            return true
        }
        if (text.contains("[english]") || text.contains("english")) return true
        return isPopularSports(name, isIndian = false) ||
            text.contains("willow") ||
            text.contains("cricket") ||
            text.contains("nba") ||
            text.contains("nfl") ||
            text.contains("nhl") ||
            text.contains("ufc") ||
            text.contains("tennis") ||
            text.contains("fifa") ||
            text.contains("premier") ||
            text.contains("motorsport") ||
            text.contains("f1") ||
            text.contains("kids") ||
            text.contains("cartoon") ||
            text.contains("music") ||
            text.contains("movie") ||
            text.contains("pluto") ||
            text.contains("plex")
    }

    private fun isRelevantEnglishChannel(
        name: String,
        group: String,
        tvgId: String,
        streamUrl: String
    ): Boolean {
        if (isForeignLanguageChannel(name, group, tvgId)) return false
        val text = "$name $group $tvgId $streamUrl".lowercase()
        return relevantEnglishBroadKeywords.any { text.contains(it) }
    }

    private fun detectLanguageTag(
        name: String,
        group: String,
        tvgId: String,
        isIndian: Boolean,
        defaultLanguageTag: String
    ): String {
        val text = "$name $group $tvgId".lowercase()
        for ((lang, markers) in regionalLanguageRules) {
            if (markers.any { text.contains(it) }) return lang
        }
        if (hindiMarkers.any { text.contains(it) }) return "Hindi"
        if (indianEnglishMarkers.any { text.contains(it) } || defaultLanguageTag == "English") {
            return "English"
        }
        if (defaultLanguageTag.isNotBlank()) return defaultLanguageTag
        if (isIndian) {
            return if (text.contains("english")) "English" else "Hindi"
        }
        return "English"
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
        if (specific != null) return specific
        return if (isIndian && isSportsChannel(name, group, "")) {
            SportCategory.INDIAN_SPORTS
        } else {
            SportCategory.ALL
        }
    }
}
