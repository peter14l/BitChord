package com.music.bitchord.ui.blend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.music.bitchord.R
import com.music.bitchord.data.blend.BlendedPlaylist
import com.music.bitchord.data.blend.BlendedTrack
import com.music.bitchord.data.blend.TrackAttribution
import com.music.bitchord.data.model.ROW_ART_PX
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.data.model.durationMillis
import com.music.bitchord.data.model.playingRowMatcher
import com.music.bitchord.ui.components.PAGE_GUTTER
import com.music.bitchord.ui.components.ROW_DIVIDER_INSET
import com.music.bitchord.ui.components.SongRow
import com.music.bitchord.ui.components.liquidGlassRim
import com.music.bitchord.ui.components.thumbnailBorder
import com.music.bitchord.ui.components.topBarContentPadding
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.icons.BitChordIcons
import com.music.bitchord.ui.theme.ArtworkPalette
import com.music.bitchord.ui.theme.rememberArtworkPalette

private const val SLEEVE_FRACTION = 0.76f
private const val SLEEVE_MAX_HEIGHT = 0.5f
private val SLEEVE_SHAPE = RoundedCornerShape(12.dp)
private val SLEEVE_SHADOW = 18.dp

private val RELEASE_SIDE_BUTTON = 50.dp
private val RELEASE_ACTION_GAP = 14.dp
private val RELEASE_ACTIONS_MAX_WIDTH = 420.dp
private val HEADER_GUTTER = PAGE_GUTTER + 8.dp

private const val LIGHT_FILL_ALPHA = 0.12f
private const val TOP_RELEASE_EDGE_ALPHA = 0.18f

/**
 * Screen displaying the curated collaborative Blend playlist.
 *
 * Implements BitChord's standard release page design:
 * - Elevated framed sleeve with shadow and thumbnail border
 * - Centered title, credit, and metadata
 * - Standard liquid-glass Play and Shuffle pills plus Story circle button
 * - Standard [SongRow] items with playing indicator bars, swipe-to-queue, and long-press menu
 * - Subtle Blend attribution badge in the row trailing slot
 */
@Composable
fun BlendPlaylistScreen(
    playlist: BlendedPlaylist,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (List<Song>, Int) -> Unit,
    onSongLongPress: (Song) -> Unit,
    onSongSwipe: (Song) -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onOpenStory: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val seedThumbnail = remember(playlist) {
        playlist.tracks.firstOrNull { it.song.thumbnailUrl != null }?.song?.thumbnailUrl
    }
    val palette = rememberArtworkPalette(imageUrl = seedThumbnail)
    val songs = remember(playlist) { playlist.tracks.map { it.song } }
    val isCurrentRow = playingRowMatcher(songs, currentSong)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        val sleeveSize = (maxWidth * SLEEVE_FRACTION).coerceAtMost(maxHeight * SLEEVE_MAX_HEIGHT)

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
        ) {
            // Header Item
            item(key = "header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = topBarContentPadding(), bottom = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Elevated Sleeve Frame
                    Box(
                        modifier = Modifier
                            .size(sleeveSize)
                            .shadow(SLEEVE_SHADOW, SLEEVE_SHAPE)
                            .background(palette.elevated)
                            .thumbnailBorder(SLEEVE_SHAPE),
                    ) {
                        BlendSleeveArt(
                            playlist = playlist,
                            palette = palette,
                            modifier = Modifier.matchParentSize(),
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // Title
                    Text(
                        text = playlist.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.onBackground,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = HEADER_GUTTER),
                    )

                    Spacer(Modifier.height(2.dp))

                    // Subtitle / Credit
                    Text(
                        text = "${playlist.userAName} & ${playlist.userBName} • ${playlist.matchPercentage}% match",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 19.sp,
                        ),
                        color = palette.onBackgroundVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = HEADER_GUTTER),
                    )

                    // Meta: "Playlist · 50 tracks · 2h 45m"
                    val playtime = songs.playtimeText()
                    val meta = listOfNotNull(
                        stringResource(R.string.playlist),
                        pluralStringResource(R.plurals.track_count_plural, songs.size, songs.size),
                        playtime,
                    ).joinToString(" · ")

                    if (meta.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.onBackgroundVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = HEADER_GUTTER),
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    // Action Controls: Play, Shuffle, View Story
                    Row(
                        modifier = Modifier
                            .widthIn(max = RELEASE_ACTIONS_MAX_WIDTH)
                            .fillMaxWidth()
                            .padding(horizontal = HEADER_GUTTER),
                        horizontalArrangement = Arrangement.spacedBy(RELEASE_ACTION_GAP),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ActionPill(
                            icon = BitChordIcons.Play,
                            label = stringResource(R.string.play),
                            onClick = onPlayAll,
                            filled = true,
                            size = RELEASE_SIDE_BUTTON,
                            modifier = Modifier.weight(1f),
                        )
                        ActionPill(
                            icon = BitChordIcons.Shuffle,
                            label = stringResource(R.string.shuffle),
                            onClick = onShuffleAll,
                            filled = false,
                            size = RELEASE_SIDE_BUTTON,
                            modifier = Modifier.weight(1f),
                        )
                        CircleIconButton(
                            icon = Icons.Rounded.AutoAwesome,
                            contentDescription = "View Blend Story",
                            onClick = onOpenStory,
                            size = RELEASE_SIDE_BUTTON,
                        )
                    }
                }
            }

            // Playlist track rows
            itemsIndexed(
                items = playlist.tracks,
                key = { index, item -> "${item.song.videoId}_$index" },
            ) { index, trackItem ->
                val song = trackItem.song
                val isCurrent = isCurrentRow(song)

                SongRow(
                    song = song,
                    onClick = { onSongClick(songs, index) },
                    onLongPress = { onSongLongPress(song) },
                    onSwipeToQueue = { onSongSwipe(song) },
                    rowBackground = Color.Transparent,
                    subtitleColor = palette.onBackgroundVariant,
                    downloadedTint = palette.accent,
                    isCurrent = isCurrent,
                    isPlaying = isCurrent && isPlaying,
                    activeTint = palette.accent,
                    searchPlayingStyle = true,
                    trailing = {
                        BlendAttributionBadge(
                            track = trackItem,
                            userAName = playlist.userAName,
                            userBName = playlist.userBName,
                        )
                    },
                )

                if (index < playlist.tracks.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = ROW_DIVIDER_INSET),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

/**
 * Renders the sleeve artwork for the collaborative Blend playlist:
 * A 2x2 collage of track artworks from both listeners with overlaid dual avatars and match badge.
 */
@Composable
private fun BlendSleeveArt(
    playlist: BlendedPlaylist,
    palette: ArtworkPalette,
    modifier: Modifier = Modifier,
) {
    val sampleThumbnails = remember(playlist) {
        playlist.tracks
            .mapNotNull { it.song.thumbnailUrl }
            .distinct()
            .take(4)
    }

    Box(modifier = modifier) {
        if (sampleThumbnails.size >= 4) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.weight(1f)) {
                    AsyncImage(
                        model = sampleThumbnails[0].artworkAt(ROW_ART_PX),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    AsyncImage(
                        model = sampleThumbnails[1].artworkAt(ROW_ART_PX),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
                Row(Modifier.weight(1f)) {
                    AsyncImage(
                        model = sampleThumbnails[2].artworkAt(ROW_ART_PX),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    AsyncImage(
                        model = sampleThumbnails[3].artworkAt(ROW_ART_PX),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        } else if (sampleThumbnails.isNotEmpty()) {
            AsyncImage(
                model = sampleThumbnails[0].artworkAt(ROW_ART_PX),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF6C5CE7), Color(0xFF00CEC9))
                        )
                    )
            )
        }

        // Overlay with gradient vignette and centered avatars + match badge
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.65f),
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    SleeveAvatar(playlist.userAName, playlist.userAAvatarUrl, Color(0xFF6C5CE7))
                    Box(modifier = Modifier.offset(x = (-14).dp)) {
                        SleeveAvatar(playlist.userBName, playlist.userBAvatarUrl, Color(0xFF00CEC9))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Color(0xFF1DB954), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "${playlist.matchPercentage}% MATCH",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1DB954),
                        letterSpacing = 1.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SleeveAvatar(name: String, avatarUrl: String?, color: Color) {
    var imageFailed by remember(avatarUrl) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(color)
            .border(2.5.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (!avatarUrl.isNullOrBlank() && !imageFailed) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                onState = { imageFailed = it is AsyncImagePainter.State.Error },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = name.take(1).uppercase().ifBlank { "?" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun ActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    filled: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = RELEASE_SIDE_BUTTON,
) {
    val haptics = rememberHaptics()
    val content = if (filled) Color.Black else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .height(size)
            .clip(CircleShape)
            .then(
                if (filled) {
                    Modifier.background(Color.White)
                } else {
                    Modifier
                        .background(Color.White.copy(alpha = LIGHT_FILL_ALPHA), CircleShape)
                        .liquidGlassRim(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = TOP_RELEASE_EDGE_ALPHA), CircleShape)
                },
            )
            .clickable {
                haptics.play(Haptic.Resume)
                onClick()
            }
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = content,
            maxLines = 1,
        )
    }
}

@Composable
private fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp = RELEASE_SIDE_BUTTON,
    haptic: Haptic = Haptic.Tap,
) {
    val haptics = rememberHaptics()
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = LIGHT_FILL_ALPHA), CircleShape)
            .liquidGlassRim(CircleShape)
            .border(1.dp, Color.White.copy(alpha = TOP_RELEASE_EDGE_ALPHA), CircleShape)
            .clickable {
                haptics.play(haptic)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(size * 0.44f),
        )
    }
}

@Composable
private fun BlendAttributionBadge(
    track: BlendedTrack,
    userAName: String,
    userBName: String,
) {
    val (label, tint) = when (track.attribution) {
        TrackAttribution.USER_A -> userAName.take(1).uppercase() to Color(0xFF8C7AE6)
        TrackAttribution.USER_B -> userBName.take(1).uppercase() to Color(0xFF00D2D3)
        TrackAttribution.SHARED_FAVORITE -> "★" to Color(0xFFFFD32A)
        TrackAttribution.DISCOVERY_BRIDGE -> "✦" to Color(0xFFFF9F1A)
    }

    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.16f))
            .border(1.dp, tint.copy(alpha = 0.45f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = tint,
        )
    }
}

@Composable
private fun List<Song>.playtimeText(): String? {
    val minutes = sumOf { it.durationMillis() } / 60_000
    return when {
        minutes <= 0 -> null
        minutes < 60 -> stringResource(R.string.minutes_short, minutes.toInt())
        else -> stringResource(R.string.hours_minutes_short, (minutes / 60).toInt(), (minutes % 60).toInt())
    }
}
