package com.music.bitchord.ui.replay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Interactive Spotify Wrapped-style quiz card.
 *
 * Prompts the listener to guess their top song or top artist before revealing the truth.
 */
@Composable
fun ReplayQuizCard(
    question: String,
    correctAnswer: String,
    options: List<String>,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current

    val isAnswered = selectedIndex != null
    val isCorrect = selectedIndex != null && options.getOrNull(selectedIndex!!) == correctAnswer

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "TRIVIA TIME",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFD4FF00), // Spotify neon yellow/lime
            letterSpacing = 2.sp,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = question,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.W800,
            color = Color.White,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(20.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            options.forEachIndexed { index, option ->
                val isThisOptionSelected = selectedIndex == index
                val isThisOptionCorrect = option == correctAnswer

                val backgroundColor by animateColorAsState(
                    targetValue = when {
                        !isAnswered -> Color.White.copy(alpha = 0.06f)
                        isThisOptionCorrect -> Color(0xFF1DB954).copy(alpha = 0.35f) // Spotify green
                        isThisOptionSelected -> Color(0xFFE53935).copy(alpha = 0.35f) // Red
                        else -> Color.White.copy(alpha = 0.03f)
                    },
                    animationSpec = tween(300),
                    label = "quiz_bg",
                )

                val borderColor by animateColorAsState(
                    targetValue = when {
                        !isAnswered -> Color.White.copy(alpha = 0.18f)
                        isThisOptionCorrect -> Color(0xFF1DB954)
                        isThisOptionSelected -> Color(0xFFE53935)
                        else -> Color.White.copy(alpha = 0.08f)
                    },
                    animationSpec = tween(300),
                    label = "quiz_border",
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(backgroundColor)
                        .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                        .clickable(enabled = !isAnswered) {
                            selectedIndex = index
                            haptic.performHapticFeedback(
                                if (isThisOptionCorrect) HapticFeedbackType.LongPress
                                else HapticFeedbackType.TextHandleMove
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isThisOptionSelected || (isAnswered && isThisOptionCorrect)) FontWeight.Bold else FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )

                    if (isAnswered) {
                        if (isThisOptionCorrect) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Correct",
                                tint = Color(0xFF1DB954),
                                modifier = Modifier.size(20.dp),
                            )
                        } else if (isThisOptionSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Incorrect",
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        AnimatedVisibility(
            visible = isAnswered,
            enter = fadeIn() + scaleIn(),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isCorrect) "Spot on! You know your music." else "Surprise! The answer was $correctAnswer.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCorrect) Color(0xFF1DB954) else Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .clickable { onComplete() }
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = "Show Full Leaderboard →",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                    )
                }
            }
        }

        if (!isAnswered) {
            Text(
                text = "Skip Quiz",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.45f),
                modifier = Modifier
                    .clickable { onComplete() }
                    .padding(8.dp),
            )
        }
    }
}
