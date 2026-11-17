package com.example

import com.example.data.ChannelCategory
import com.example.data.M3uParser
import com.example.data.SportCategory
import org.junit.Assert.*
import org.junit.Test

class M3uParserTest {

    @Test
    fun parseExtractsChannelsHeadersAndClassifiesSports() {
        val sampleM3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="GeoTest.uk" tvg-logo="https://example.com/geo.png" group-title="Sports",Geo Cricket [Geo-blocked]
            http://example.com/geo.m3u8
            #EXTINF:-1 tvg-id="RedBullTV.at" tvg-logo="https://example.com/rb.png" group-title="Sports",Red Bull TV (1080p)
            #EXTVLCOPT:http-user-agent=CustomUA/1.0
            #EXTVLCOPT:http-referrer=https://redbull.com
            https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8
            #EXTINF:-1 tvg-id="FIFAPlus.us" group-title="Sports",FIFA+ Live (720p)
            https://example.com/fifa.m3u8
        """.trimIndent()

        val channels = M3uParser.parse(sampleM3u)

        assertEquals(3, channels.size)
        assertEquals("FIFA+ Live (720p)", channels[0].name)
        assertEquals(SportCategory.SOCCER, channels[0].sport)
        assertEquals(ChannelCategory.SPORTS, channels[0].channelCategory)
        assertEquals("Red Bull TV (1080p)", channels[1].name)
        assertEquals(SportCategory.RACING, channels[1].sport)
        assertEquals("CustomUA/1.0", channels[1].userAgent)
        assertEquals("https://redbull.com", channels[1].referrer)
        assertEquals("1080p", channels[1].qualityLabel)

        assertEquals("Geo Cricket", channels[2].name)
        assertTrue(channels[2].isGeoRestrictedHint)
        assertEquals(SportCategory.CRICKET, channels[2].sport)
    }

    @Test
    fun parseFiltersIndianSportsChannelsAndMarksIndianOrigin() {
        val indiaM3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="DDNews.in" group-title="News",DD News
            https://example.com/ddnews.m3u8
            #EXTINF:-1 tvg-id="DDSports.in" group-title="Sports",DD Sports (720p)
            https://example.com/ddsports.m3u8
            #EXTINF:-1 tvg-id="StarSports1.in" group-title="Sports",Star Sports Cricket
            https://example.com/starsports.m3u8
        """.trimIndent()

        val indianSports = M3uParser.parse(
            m3uContent = indiaM3u,
            filterSportsOnly = true,
            markAllAsIndian = true
        )

        assertEquals(2, indianSports.size)
        assertTrue(indianSports.all { it.isIndianOrigin })
        assertTrue(indianSports.none { it.name.contains("DD News") })
        assertEquals("DD Sports (720p)", indianSports[0].name)
        assertEquals(SportCategory.CRICKET, indianSports[1].sport)
    }

    @Test
    fun parseClassifiesAllCategoriesAndExcludesUnplayableStreams() {
        val mixedM3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="GeoBlocked.in" group-title="Movies",Blocked Cinema [Geo-blocked]
            https://example.com/blocked.m3u8
            #EXTINF:-1 tvg-id="YouTubeFeed.in" group-title="News",Live News YT
            https://www.youtube.com/watch?v=12345
            #EXTINF:-1 tvg-id="AajTak.in" group-title="News",Aaj Tak
            https://example.com/aajtak.m3u8
            #EXTINF:-1 tvg-id="B4UMovies.in" group-title="Movies",B4U Movies
            https://example.com/b4umovies.m3u8
            #EXTINF:-1 tvg-id="9XM.in" group-title="Music",9XM
            https://example.com/9xm.m3u8
            #EXTINF:-1 tvg-id="Pogo.in" group-title="Kids",Pogo Kids
            https://example.com/pogo.m3u8
            #EXTINF:-1 tvg-id="PlutoAction.us" group-title="Entertainment",Pluto TV Entertainment
            https://service-stitcher.clusters.pluto.tv/stitch/hls/channel/master.m3u8
            #EXTINF:-1 tvg-id="DDSports.in" group-title="Sports",DD Sports
            https://example.com/ddsports.m3u8
        """.trimIndent()

        val playable = M3uParser.parse(
            m3uContent = mixedM3u,
            excludeUnplayableOrGeoBlocked = true
        )

        assertEquals(6, playable.size)
        assertTrue(playable.none { it.name.contains("Blocked") })
        assertTrue(playable.none { it.streamUrl.contains("youtube.com") })

        val byName = playable.associateBy { it.name }
        assertEquals(ChannelCategory.NEWS, byName["Aaj Tak"]?.channelCategory)
        assertEquals(ChannelCategory.MOVIES, byName["B4U Movies"]?.channelCategory)
        assertEquals(ChannelCategory.SONGS, byName["9XM"]?.channelCategory)
        assertEquals(ChannelCategory.KIDS, byName["Pogo Kids"]?.channelCategory)
        assertEquals(ChannelCategory.OTT, byName["Pluto TV Entertainment"]?.channelCategory)
        assertEquals(ChannelCategory.SPORTS, byName["DD Sports"]?.channelCategory)
    }
}
