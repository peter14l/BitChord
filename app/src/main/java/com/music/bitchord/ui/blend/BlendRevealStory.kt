package com.music.bitchord.ui.blend

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.data.blend.BlendedPlaylist
import com.music.bitchord.data.blend.TrackAttribution
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Spotify-style fullscreen animated reveal story displaying the match percentage and mutual music chemistry.
 * Supports:
 * - Auto-advancing timed progress bar (6s per slide)
 * - Tap right to advance, tap left to go back
 * - Tap & hold to pause the story
 * - Explicit Next / Back buttons
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
    val scope = rememberCoroutineScope()

    var isPaused by remember { mutableStateOf(false) }
    val slideProgress = remember { Animatable(0f) }
    val matchScoreAnim = remember { Animatable(0f) }

    fun goToSlide(target: Int) {
        if (target in 0 until totalSlides) {
            scope.launch { slideProgress.snapTo(0f) }
            currentSlide = target
        } else if (target >= totalSlides) {
            onOpenPlaylist()
        }
    }

    // Auto-advance progress timer (6 seconds per slide)
    LaunchedEffect(currentSlide, isPaused) {
        if (isPaused) return@LaunchedEffect
        val remainingFactor = (1f - slideProgress.value).coerceIn(0f, 1f)
        val durationMs = (remainingFactor * 6000f).roundToInt().coerceAtLeast(100)
        slideProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMs, easing = LinearEasing),
        )
        if (slideProgress.value >= 1f) {
            if (currentSlide < totalSlides - 1) {
                slideProgress.snapTo(0f)
                currentSlide++
            } else {
                onOpenPlaylist()
            }
        }
    }

    // Number roll animation on first slide
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
            .navigationBarsPadding()
            .pointerInput(currentSlide) {
                detectTapGestures(
                    onPress = {
                        isPaused = true
                        tryAwaitRelease()
                        isPaused = false
                    },
                    onTap = { offset ->
                        // Tap right 65% advances, tap left 35% goes back
                        if (offset.x >= size.width * 0.35f) {
                            goToSlide(currentSlide + 1)
                        } else {
                            goToSlide(currentSlide - 1)
                        }
                    },
                )
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header Progress Segments (Animated fill)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(totalSlides) { idx ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.25f)),
                    ) {
                        val fillFraction = when {
                            idx < currentSlide -> 1f
                            idx == currentSlide -> slideProgress.value
                            else -> 0f
                        }
                        if (fillFraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fillFraction)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White),
                            )
                        }
                    }
                }
            }

            // Top Bar with Title and Close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${playlist.userAName} + ${playlist.userBName} Blend",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.75f),
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

            // Slide Contents with Animated Transition
            AnimatedContent(
                targetState = currentSlide,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                modifier = Modifier.weight(0.82f),
                label = "StorySlide",
            ) { slide ->
                when (slide) {
                    0 -> {
                        // Slide 1: Dual Avatars & Match Percentage
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            // Overlapping dual avatars
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AvatarCircle(playlist.userAName, Color(0xFF6C5CE7))
                                Box(modifier = Modifier.offset(x = (-24).dp)) {
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

                            Spacer(Modifier.height(24.dp))

                            Text(
                                text = "Tap right to continue →",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.45f),
                            )
                        }
                    }
                    1 -> {
                        // Slide 2: Mutual Anthems
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize(),
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
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Your blend is packed with brand new discoveries for each other!",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        // Slide 3: Insights & Mixtape Ready CTA
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize(),
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
            }

            // Bottom Navigation Buttons (Explicit controls)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (currentSlide > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable { goToSlide(currentSlide - 1) }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Back",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                if (currentSlide < totalSlides - 1) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.18f))
                            .clickable { goToSlide(currentSlide + 1) }
                            .padding(horizontal = 22.dp, vertical = 10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Next",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Rounded.ArrowForward,
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
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
