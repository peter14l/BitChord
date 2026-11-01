package com.music.bitchord

import com.music.bitchord.data.blend.BlendCalculator
import com.music.bitchord.data.blend.TasteArtist
import com.music.bitchord.data.blend.TasteProfile
import com.music.bitchord.data.blend.TasteTrack
import com.music.bitchord.data.blend.TrackAttribution
import com.music.bitchord.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlendCalculatorTest {

    @Test
    fun testProfileCompactEncodingAndDecoding() {
        val original = TasteProfile(
            userId = "usr_12345",
            displayName = "Alex",
            avatarUrl = "https://example.com/avatar.jpg",
            topArtists = listOf(
                TasteArtist("art_1", "Radiohead", weight = 0.95f, plays = 42),
                TasteArtist("art_2", "Daft Punk", weight = 0.80f, plays = 30),
            ),
            topTracks = listOf(
                TasteTrack("vid_1", "Karma Police", "Radiohead", artworkUrl = "https://img/1.jpg", playCount = 25),
                TasteTrack("vid_2", "Get Lucky", "Daft Punk", artworkUrl = "https://img/2.jpg", playCount = 18),
            ),
            topGenres = listOf("Art Rock", "Electronic"),
            totalMinutes = 1250L,
        )

        val encoded = original.toCompactPayload()
        assertTrue("Encoded payload should not be empty", encoded.isNotEmpty())
        // URL-safe checks
        assertTrue("Must not contain plus", !encoded.contains('+'))
        assertTrue("Must not contain slash", !encoded.contains('/'))

        val decoded = TasteProfile.fromCompactPayload(encoded)
        assertEquals(original.userId, decoded.userId)
        assertEquals(original.displayName, decoded.displayName)
        assertEquals(original.avatarUrl, decoded.avatarUrl)
        assertEquals(original.topArtists.size, decoded.topArtists.size)
        assertEquals(original.topArtists[0].name, decoded.topArtists[0].name)
        assertEquals(original.topTracks.size, decoded.topTracks.size)
        assertEquals(original.topTracks[0].title, decoded.topTracks[0].title)
        assertEquals(original.topGenres, decoded.topGenres)
        assertEquals(original.totalMinutes, decoded.totalMinutes)
    }

    @Test
    fun testIdenticalTasteProfilesYieldHighMatch() {
        val artists = listOf(
            TasteArtist("1", "Kendrick Lamar", 1.0f, 100),
            TasteArtist("2", "SZA", 0.9f, 80),
            TasteArtist("3", "Tyler, The Creator", 0.8f, 70),
        )
        val tracks = listOf(
            TasteTrack("t1", "Not Like Us", "Kendrick Lamar", playCount = 50),
            TasteTrack("t2", "Snooze", "SZA", playCount = 45),
            TasteTrack("t3", "See You Again", "Tyler, The Creator", playCount = 40),
        )
        val genres = listOf("Hip-Hop", "R&B")

        val userA = TasteProfile("a", "Jordan", topArtists = artists, topTracks = tracks, topGenres = genres)
        val userB = TasteProfile("b", "Taylor", topArtists = artists, topTracks = tracks, topGenres = genres)

        val match = BlendCalculator.calculateMatch(userA, userB)
        assertTrue("Identical profiles should yield 90%+ match", match.matchPercentage >= 90)
        assertEquals(3, match.sharedArtists.size)
        assertEquals(3, match.sharedTrackIds.size)
        assertTrue("Insights should mention shared favorites", match.insights.isNotEmpty())
    }

    @Test
    fun testDisjointTasteProfilesYieldLowerMatch() {
        val userA = TasteProfile(
            "a", "Metalhead",
            topArtists = listOf(TasteArtist("m1", "Metallica", 1.0f), TasteArtist("m2", "Slayer", 0.9f)),
            topTracks = listOf(TasteTrack("tm1", "Master of Puppets", "Metallica")),
            topGenres = listOf("Heavy Metal", "Thrash"),
        )
        val userB = TasteProfile(
            "b", "PopLover",
            topArtists = listOf(TasteArtist("p1", "Dua Lipa", 1.0f), TasteArtist("p2", "Sabrina Carpenter", 0.9f)),
            topTracks = listOf(TasteTrack("tp1", "Levitating", "Dua Lipa")),
            topGenres = listOf("Dance-Pop", "Synthpop"),
        )

        val match = BlendCalculator.calculateMatch(userA, userB)
        assertTrue("Disjoint profiles should yield lower match percentage", match.matchPercentage in 12..35)
        assertEquals(0, match.sharedArtists.size)
        assertEquals(0, match.sharedTrackIds.size)
    }

    @Test
    fun testCuratePlaylistMaintainsInterleavingAndUniqueness() {
        val userA = TasteProfile(
            "a", "User A",
            topArtists = listOf(TasteArtist("a1", "Artist Common", 1.0f), TasteArtist("a2", "Artist A", 0.8f)),
            topTracks = listOf(
                TasteTrack("v_common", "Common Song", "Artist Common"),
                TasteTrack("v_a1", "Song A1", "Artist A"),
                TasteTrack("v_a2", "Song A2", "Artist A"),
                TasteTrack("v_a3", "Song A3", "Artist A"),
            ),
            topGenres = listOf("Indie"),
        )
        val userB = TasteProfile(
            "b", "User B",
            topArtists = listOf(TasteArtist("b1", "Artist Common", 1.0f), TasteArtist("b2", "Artist B", 0.8f)),
            topTracks = listOf(
                TasteTrack("v_common", "Common Song", "Artist Common"),
                TasteTrack("v_b1", "Song B1", "Artist B"),
                TasteTrack("v_b2", "Song B2", "Artist B"),
                TasteTrack("v_b3", "Song B3", "Artist B"),
            ),
            topGenres = listOf("Indie"),
        )

        val bridge = listOf(
            Song("v_bridge1", "Bridge Song 1", "Bridge Artist", thumbnailUrl = null),
        )

        val playlist = BlendCalculator.curatePlaylist(userA, userB, bridgeTracks = bridge, targetCount = 10)

        assertEquals("User A + User B Blend", playlist.title)
        assertTrue("Playlist must have tracks", playlist.tracks.isNotEmpty())

        // Check for uniqueness of video IDs
        val videoIds = playlist.tracks.map { it.song.videoId }
        assertEquals("All tracks in playlist must be unique", videoIds.size, videoIds.toSet().size)

        // Check for presence of different attributions
        val attributions = playlist.tracks.map { it.attribution }.toSet()
        assertTrue("Must include USER_A", attributions.contains(TrackAttribution.USER_A))
        assertTrue("Must include USER_B", attributions.contains(TrackAttribution.USER_B))
        assertTrue("Must include SHARED_FAVORITE", attributions.contains(TrackAttribution.SHARED_FAVORITE))
    }

    @Test
    fun testEmptyProfilesFallbackSafely() {
        val emptyA = TasteProfile("empty_a", "Newbie A")
        val emptyB = TasteProfile("empty_b", "Newbie B")

        val match = BlendCalculator.calculateMatch(emptyA, emptyB)
        assertNotNull(match)
        assertTrue("Should have non-negative match percentage", match.matchPercentage in 0..100)

        val playlist = BlendCalculator.curatePlaylist(emptyA, emptyB, targetCount = 5)
        assertNotNull(playlist)
    }
}
