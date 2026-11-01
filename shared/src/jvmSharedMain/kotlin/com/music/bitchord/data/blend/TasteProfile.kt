package com.music.bitchord.data.blend

import com.music.bitchord.data.model.Song
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
}

@Serializable
data class TasteArtist(
    val id: String,
    val name: String,
    val weight: Float = 1.0f,
    val plays: Int = 1,
)

@Serializable
data class TasteTrack(
    val videoId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String? = null,
    val playCount: Int = 1,
    val artistId: String? = null,
    val albumId: String? = null,
    val albumName: String? = null,
    val durationText: String? = null,
) {
    fun toSong(): Song = Song(
        videoId = videoId,
        title = title,
        artist = artist,
        thumbnailUrl = artworkUrl,
        artistId = artistId,
        albumId = albumId,
        albumName = albumName,
        durationText = durationText,
    )

    companion object {
        fun fromSong(song: Song, plays: Int = 1): TasteTrack = TasteTrack(
            videoId = song.videoId,
            title = song.title,
            artist = song.artist,
            artworkUrl = song.thumbnailUrl,
            playCount = plays,
            artistId = song.artistId,
            albumId = song.albumId,
            albumName = song.albumName,
            durationText = song.durationText,
        )
    }
}

@Serializable
data class TasteProfile(
    val userId: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val topArtists: List<TasteArtist> = emptyList(),
    val topTracks: List<TasteTrack> = emptyList(),
    val topGenres: List<String> = emptyList(),
    val totalMinutes: Long = 0L,
    val generatedAtMs: Long = System.currentTimeMillis(),
) {
    /**
     * Serializes this profile into a URL-safe, compressed Base64 string suitable for
     * deep links (`bitchord://blend?profile=...`) and compact QR codes.
     */
    fun toCompactPayload(): String {
        val jsonBytes = json.encodeToString(this).toByteArray(Charsets.UTF_8)
        val byteOut = ByteArrayOutputStream()
        GZIPOutputStream(byteOut).use { gzip ->
            gzip.write(jsonBytes)
        }
        val compressed = byteOut.toByteArray()
        return Base64.getUrlEncoder().withoutPadding().encodeToString(compressed)
    }

    companion object {
        /**
         * Decodes a profile exported by [toCompactPayload].
         */
        fun fromCompactPayload(payload: String): TasteProfile {
            val compressed = Base64.getUrlDecoder().decode(payload.trim())
            val byteIn = ByteArrayInputStream(compressed)
            val decompressedBytes = GZIPInputStream(byteIn).use { gzip ->
                gzip.readBytes()
            }
            val jsonString = String(decompressedBytes, Charsets.UTF_8)
            return json.decodeFromString<TasteProfile>(jsonString)
        }
    }
}

@Serializable
enum class TrackAttribution {
    USER_A,
    USER_B,
    SHARED_FAVORITE,
    DISCOVERY_BRIDGE,
}

data class BlendedTrack(
    val song: Song,
    val attribution: TrackAttribution,
    val reason: String? = null,
    val score: Float = 0f,
)

data class BlendedPlaylist(
    val id: String,
    val title: String,
    val userAName: String,
    val userBName: String,
    val userAAvatarUrl: String?,
    val userBAvatarUrl: String?,
    val matchPercentage: Int,
    val sharedArtists: List<String>,
    val sharedGenres: List<String>,
    val overlapInsights: List<String>,
    val tracks: List<BlendedTrack>,
    val createdAtMs: Long = System.currentTimeMillis(),
)
