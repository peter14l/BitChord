package com.music.bitchord.data.blend

import com.music.bitchord.data.model.Song
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class BlendMatchResult(
    val matchPercentage: Int,
    val sharedArtists: List<String>,
    val sharedGenres: List<String>,
    val sharedTrackIds: Set<String>,
    val insights: List<String>,
)

object BlendCalculator {

    private fun normalizeKey(name: String): String =
        name.trim().lowercase().replace(Regex("[^a-z0-9]"), "")

    /**
     * Calculates the musical compatibility score and overlap insights between two taste profiles.
     */
    fun calculateMatch(userA: TasteProfile, userB: TasteProfile): BlendMatchResult {
        if (userA.topArtists.isEmpty() && userA.topTracks.isEmpty()) {
            return fallbackEmptyResult(userA, userB)
        }
        if (userB.topArtists.isEmpty() && userB.topTracks.isEmpty()) {
            return fallbackEmptyResult(userA, userB)
        }

        // 1. Artist overlap calculation (Normalized Jaccard with min/max weights)
        val artistsA = userA.topArtists.associateBy({ normalizeKey(it.name) }, { it })
        val artistsB = userB.topArtists.associateBy({ normalizeKey(it.name) }, { it })

        val allArtistKeys = artistsA.keys + artistsB.keys
        val sharedArtistKeys = artistsA.keys.intersect(artistsB.keys)

        var intersectionWeightSum = 0f
        var unionWeightSum = 0f
        for (key in allArtistKeys) {
            val a = artistsA[key]?.weight ?: 0f
            val b = artistsB[key]?.weight ?: 0f
            intersectionWeightSum += min(a, b)
            unionWeightSum += max(a, b)
        }

        val artistScore = if (unionWeightSum > 0f) {
            intersectionWeightSum / unionWeightSum
        } else 0f

        // 2. Direct track overlap
        val trackIdsA = userA.topTracks.map { it.videoId }.toSet()
        val trackIdsB = userB.topTracks.map { it.videoId }.toSet()
        val sharedTrackIds = trackIdsA.intersect(trackIdsB)
        val minTrackCount = min(trackIdsA.size, trackIdsB.size)
        val trackScore = if (minTrackCount > 0) {
            sharedTrackIds.size.toFloat() / minTrackCount.toFloat()
        } else 0f

        // 3. Genre concordance
        val genresA = userA.topGenres.map { normalizeKey(it) }.toSet()
        val genresB = userB.topGenres.map { normalizeKey(it) }.toSet()
        val sharedGenresRaw = genresA.intersect(genresB)
        val unionGenres = genresA.union(genresB)
        val genreScore = if (unionGenres.isNotEmpty()) {
            sharedGenresRaw.size.toFloat() / unionGenres.size.toFloat()
        } else 0f

        // Combined weighted score: 60% Artist, 25% Track, 15% Genre
        val rawComposite = (artistScore * 0.60f) + (trackScore * 0.25f) + (genreScore * 0.15f)

        // Boost score slightly for non-empty overlap so friends get an engaging percentage (e.g. 50-95%)
        val scaled = if (sharedArtistKeys.isNotEmpty() || sharedTrackIds.isNotEmpty()) {
            35f + (rawComposite * 62f)
        } else {
            15f + (genreScore * 30f)
        }
        val matchPercentage = scaled.roundToInt().coerceIn(12, 98)

        // Identify display names for shared artists
        val sharedArtistNames = sharedArtistKeys.mapNotNull { key ->
            artistsA[key]?.name ?: artistsB[key]?.name
        }.sortedByDescending { key ->
            val norm = normalizeKey(key)
            (artistsA[norm]?.weight ?: 0f) + (artistsB[norm]?.weight ?: 0f)
        }

        val sharedGenreNames = userA.topGenres.filter { gA ->
            genresB.contains(normalizeKey(gA))
        }

        // Generate tailored social insights
        val insights = mutableListOf<String>()
        if (sharedArtistNames.isNotEmpty()) {
            val topShared = sharedArtistNames.take(2).joinToString(" & ")
            insights.add("You both have $topShared on repeat.")
        }
        if (sharedTrackIds.isNotEmpty()) {
            insights.add("You share ${sharedTrackIds.size} identical favorite song${if (sharedTrackIds.size > 1) "s" else ""}.")
        }
        if (sharedGenreNames.isNotEmpty()) {
            insights.add("Musical common ground: ${sharedGenreNames.first().replaceFirstChar { it.uppercase() }}.")
        }
        if (insights.isEmpty()) {
            insights.add("Opposites attract! A high-discovery blend of contrasting styles.")
        }

        return BlendMatchResult(
            matchPercentage = matchPercentage,
            sharedArtists = sharedArtistNames,
            sharedGenres = sharedGenreNames,
            sharedTrackIds = sharedTrackIds,
            insights = insights,
        )
    }

    /**
     * Curates a balanced, interleaved 40–50 track playlist combining both users' signatures and shared anthems.
     */
    fun curatePlaylist(
        userA: TasteProfile,
        userB: TasteProfile,
        bridgeTracks: List<Song> = emptyList(),
        targetCount: Int = 45,
    ): BlendedPlaylist {
        val match = calculateMatch(userA, userB)
        val sharedArtistNorms = match.sharedArtists.map { normalizeKey(it) }.toSet()

        val usedVideoIds = mutableSetOf<String>()

        // 1. Extract shared favorites
        val sharedList = mutableListOf<BlendedTrack>()
        val tracksA = userA.topTracks
        val tracksB = userB.topTracks

        // Exactly shared tracks first
        for (track in tracksA) {
            if (match.sharedTrackIds.contains(track.videoId) && usedVideoIds.add(track.videoId)) {
                sharedList.add(
                    BlendedTrack(
                        song = track.toSong(),
                        attribution = TrackAttribution.SHARED_FAVORITE,
                        reason = "Loved by both ${userA.displayName} and ${userB.displayName}",
                    )
                )
            }
        }

        // Tracks by shared artists
        for (track in (tracksA + tracksB)) {
            val artistNorm = normalizeKey(track.artist)
            if (sharedArtistNorms.contains(artistNorm) && usedVideoIds.add(track.videoId)) {
                sharedList.add(
                    BlendedTrack(
                        song = track.toSong(),
                        attribution = TrackAttribution.SHARED_FAVORITE,
                        reason = "Shared favorite artist: ${track.artist}",
                    )
                )
            }
        }

        // 2. User A unique favorites
        val userAList = mutableListOf<BlendedTrack>()
        for (track in tracksA) {
            if (usedVideoIds.add(track.videoId)) {
                userAList.add(
                    BlendedTrack(
                        song = track.toSong(),
                        attribution = TrackAttribution.USER_A,
                        reason = "From ${userA.displayName}'s heavy rotation",
                    )
                )
            }
        }

        // 3. User B unique favorites
        val userBList = mutableListOf<BlendedTrack>()
        for (track in tracksB) {
            if (usedVideoIds.add(track.videoId)) {
                userBList.add(
                    BlendedTrack(
                        song = track.toSong(),
                        attribution = TrackAttribution.USER_B,
                        reason = "From ${userB.displayName}'s heavy rotation",
                    )
                )
            }
        }

        // 4. Bridge / Discovery tracks
        val discoveryList = mutableListOf<BlendedTrack>()
        for (song in bridgeTracks) {
            if (usedVideoIds.add(song.videoId)) {
                discoveryList.add(
                    BlendedTrack(
                        song = song,
                        attribution = TrackAttribution.DISCOVERY_BRIDGE,
                        reason = "Discovery bridge: fits both your styles",
                    )
                )
            }
        }

        // 5. Calculate ideal proportions
        val targetShared = max(4, (targetCount * 0.25f).roundToInt())
        val targetPerUser = max(6, (targetCount * 0.35f).roundToInt())

        val selectedShared = sharedList.take(targetShared)
        val selectedA = userAList.take(targetPerUser)
        val selectedB = userBList.take(targetPerUser)

        // 6. Interleave tracks into an organic DJ mixtape flow
        // Pattern: Shared -> User A -> User B -> Shared/Discovery -> User A -> User B ...
        val finalTracks = mutableListOf<BlendedTrack>()
        var idxShared = 0
        var idxA = 0
        var idxB = 0
        var idxDisc = 0

        val maxIter = targetCount + 10
        var iter = 0

        while (finalTracks.size < targetCount && iter < maxIter) {
            iter++
            val startSize = finalTracks.size

            // Insert a shared track periodically
            if (idxShared < selectedShared.size) {
                finalTracks.add(selectedShared[idxShared++])
            } else if (idxDisc < discoveryList.size) {
                finalTracks.add(discoveryList[idxDisc++])
            }

            // Insert User A track
            if (idxA < selectedA.size && finalTracks.size < targetCount) {
                finalTracks.add(selectedA[idxA++])
            }

            // Insert User B track
            if (idxB < selectedB.size && finalTracks.size < targetCount) {
                finalTracks.add(selectedB[idxB++])
            }

            // If no tracks were added in this cycle, drain any remaining reserves
            if (finalTracks.size == startSize) {
                if (idxA < userAList.size) finalTracks.add(userAList[idxA++])
                else if (idxB < userBList.size) finalTracks.add(userBList[idxB++])
                else if (idxShared < sharedList.size) finalTracks.add(sharedList[idxShared++])
                else if (idxDisc < discoveryList.size) finalTracks.add(discoveryList[idxDisc++])
                else break
            }
        }

        val playlistTitle = "${userA.displayName} + ${userB.displayName} Blend"
        val playlistId = "blend_${userA.userId.take(6)}_${userB.userId.take(6)}"

        return BlendedPlaylist(
            id = playlistId,
            title = playlistTitle,
            userAName = userA.displayName,
            userBName = userB.displayName,
            userAAvatarUrl = userA.avatarUrl,
            userBAvatarUrl = userB.avatarUrl,
            matchPercentage = match.matchPercentage,
            sharedArtists = match.sharedArtists,
            sharedGenres = match.sharedGenres,
            overlapInsights = match.insights,
            tracks = finalTracks,
        )
    }

    private fun fallbackEmptyResult(userA: TasteProfile, userB: TasteProfile): BlendMatchResult {
        return BlendMatchResult(
            matchPercentage = 50,
            sharedArtists = emptyList(),
            sharedGenres = emptyList(),
            sharedTrackIds = emptySet(),
            insights = listOf("Start listening to build up your Blend percentage!"),
        )
    }
}
