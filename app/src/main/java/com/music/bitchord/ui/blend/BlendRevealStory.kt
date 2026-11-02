package com.music.bitchord.ui.blend

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.data.blend.BlendedPlaylist
import com.music.bitchord.data.blend.TrackAttribution
import kotlin.math.roundToInt

/**
 * Spotify-style animated reveal story displaying the match percentage and mutual music chemistry.
 */
@Composable
fun BlendRevealStory(
    playlist: BlendedPlaylist,
    onOpenPlaylist: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentSlide by remember { mutableIntStateOf(0) }
    val totalSlides = 3

    val matchScoreAnim = remember { Animatable(0f) }

    LaunchedEffect(currentSlide) {
        if (currentSlide == 0) {
            matchScoreAnim.snapTo(0f)
            matchScoreAnim.animateTo(
                targetValue = playlist.matchPercentage.toFloat(),
                animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1E1430),
                        Color(0xFF0D0A14),
                        Color.Black,
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header Progress Segments
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(totalSlides) { idx ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (idx <= currentSlide) Color.White else Color.White.copy(alpha = 0.25f)
                            ),
                    )
                }
            }

            // Top Bar with Close
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${playlist.userAName} + ${playlist.userBName} Blend",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.7f),
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                    )
                }
            }

            Spacer(Modifier.weight(0.1f))

            // Slide Contents
            when (currentSlide) {
                0 -> {
                    // Slide 1: Dual Avatars & Match Percentage
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(0.8f),
                    ) {
                        // Overlapping dual avatars
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AvatarCircle(playlist.userAName, Color(0xFF6C5CE7))
                            Box(
                                modifier = Modifier
                                    .offset(x = (-24).dp)
                            ) {
                                AvatarCircle(playlist.userBName, Color(0xFF00CEC9))
                            }
                        }

                        Spacer(Modifier.height(32.dp))

                        Text(
                            text = "TASTE MATCH",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.W800,
                            letterSpacing = 3.sp,
                            color = Color(0xFFD4FF00),
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "${matchScoreAnim.value.roundToInt()}%",
                            style = MaterialTheme.typography.displayLarge,
                            fontSize = 88.sp,
                            fontWeight = FontWeight.W900,
                            color = Color.White,
                        )

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = when {
                                playlist.matchPercentage >= 80 -> "Musical Soulmates! Your listening habits are astonishingly aligned."
                                playlist.matchPercentage >= 60 -> "Strong Harmony. You share major favorites with room for cross-discovery."
                                else -> "Contrasting Explorers. Two distinct styles colliding into an eclectic mix."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    }
                }
                1 -> {
                    // Slide 2: Mutual Anthems
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(0.8f),
                    ) {
                        Text(
                            text = "SHARED ANTHEMS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00CEC9),
                            letterSpacing = 2.sp,
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "The music you both agree on",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(Modifier.height(28.dp))

                        val sharedTracks = playlist.tracks.filter { it.attribution == TrackAttribution.SHARED_FAVORITE }
                        if (sharedTracks.isNotEmpty()) {
                            sharedTracks.take(4).forEach { item ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .padding(16.dp),
                                ) {
                                    Column {
                                        Text(
                                            text = item.song.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                        )
                                        Text(
                                            text = item.song.artist,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.6f),
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Your blend is packed with brand new discoveries for each other!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                2 -> {
                    // Slide 3: Insights & CTA
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(0.8f),
                    ) {
                        Text(
                            text = "YOUR MIXTAPE IS READY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD4FF00),
                            letterSpacing = 2.sp,
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "${playlist.tracks.size} Handpicked Tracks",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.W800,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(Modifier.height(24.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            playlist.overlapInsights.forEach { insight ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("✦ ", color = Color(0xFFD4FF00), fontWeight = FontWeight.Bold)
                                    Text(
                                        text = insight,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.85f),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(36.dp))

                        // Large Play/Open Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF1DB954))
                                .clickable { onOpenPlaylist() }
                                .padding(vertical = 18.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Listen to Blended Mix",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Buttons
            if (currentSlide < totalSlides - 1) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable { currentSlide++ }
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = "Next →",
                            style = MaterialTheme.typography.labelLarge,
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
private fun AvatarCircle(name: String, color: Color) {
    Box(
        modifier = Modifier
            .size(92.dp)
            .clip(CircleShape)
            .background(color)
            .border(3.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.take(1).uppercase(),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.W900,
            color = Color.White,
        )
    }
}
