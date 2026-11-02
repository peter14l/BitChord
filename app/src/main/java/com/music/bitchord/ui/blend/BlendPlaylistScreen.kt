package com.music.bitchord.ui.blend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.bitchord.data.blend.BlendedPlaylist
import com.music.bitchord.data.blend.BlendedTrack
import com.music.bitchord.data.blend.TrackAttribution
import com.music.bitchord.data.model.Song

/**
 * Screen displaying the curated collaborative Blend playlist with track attribution chips and match stats.
 */
@Composable
fun BlendPlaylistScreen(
    playlist: BlendedPlaylist,
    onTrackClick: (Song) -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D11)),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
        ) {
            // Header Item
            item {
                BlendHeader(
                    playlist = playlist,
                    onBack = onBack,
                    onPlayAll = onPlayAll,
                    onShuffleAll = onShuffleAll,
                )
            }

            // Playlist track rows
            itemsIndexed(playlist.tracks, key = { index, item -> "${item.song.videoId}_$index" }) { index, trackItem ->
                BlendedTrackRow(
                    index = index + 1,
                    trackItem = trackItem,
                    userAName = playlist.userAName,
                    userBName = playlist.userBName,
                    onClick = { onTrackClick(trackItem.song) },
                )
            }
        }
    }
}

@Composable
private fun BlendHeader(
    playlist: BlendedPlaylist,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF281E40),
                        Color(0xFF130E1F),
                        Color(0xFF0D0D11),
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp),
    ) {
        Column {
            IconButton(onClick = onBack, modifier = Modifier.padding(top = 4.dp)) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }

            Spacer(Modifier.height(12.dp))

            // Overlapping Dual Avatars
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp),
            ) {
                SmallAvatar(playlist.userAName, Color(0xFF6C5CE7))
                Box(modifier = Modifier.offset(x = (-16).dp)) {
                    SmallAvatar(playlist.userBName, Color(0xFF00CEC9))
                }

                Spacer(Modifier.width(16.dp))

                // Match Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1DB954).copy(alpha = 0.20f))
                        .border(1.dp, Color(0xFF1DB954), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "${playlist.matchPercentage}% MATCH",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1DB954),
                        letterSpacing = 1.sp,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = playlist.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.W900,
                color = Color.White,
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Curated automatically from both of your listening habits · ${playlist.tracks.size} songs",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.65f),
            )

            Spacer(Modifier.height(20.dp))

            // Transport Actions: Play & Shuffle
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1DB954))
                        .clickable { onPlayAll() }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play All",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Play",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable { onShuffleAll() }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Shuffle",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BlendedTrackRow(
    index: Int,
    trackItem: BlendedTrack,
    userAName: String,
    userBName: String,
    onClick: () -> Unit,
) {
    val song = trackItem.song

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$index",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.width(28.dp),
        )

        // Thumbnail
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.08f)),
        ) {
            song.thumbnailUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        // Title, Artist, and Attribution Badge
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(4.dp))

            // Attribution chip
            val (badgeText, badgeColor) = when (trackItem.attribution) {
                TrackAttribution.USER_A -> "Added for $userAName" to Color(0xFF6C5CE7)
                TrackAttribution.USER_B -> "Added for $userBName" to Color(0xFF00CEC9)
                TrackAttribution.SHARED_FAVORITE -> "Shared Favorite ★" to Color(0xFF1DB954)
                TrackAttribution.DISCOVERY_BRIDGE -> "Discovery Match ✦" to Color(0xFFFFA502)
            }

            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = badgeColor,
            )
        }
    }
}

@Composable
private fun SmallAvatar(name: String, color: Color) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color)
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.take(1).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = Color.White,
        )
    }
}
