package com.music.bitchord.ui.blend

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.BuildConfig
import com.music.bitchord.data.blend.BlendCalculator
import com.music.bitchord.data.blend.BlendRepository
import com.music.bitchord.data.blend.BlendedPlaylist
import com.music.bitchord.data.blend.TasteArtist
import com.music.bitchord.data.blend.TasteProfile
import com.music.bitchord.data.blend.TasteTrack
import com.music.bitchord.data.stats.ListeningStats
import com.music.bitchord.data.stats.ReplayPeriod
import com.music.bitchord.ui.components.QrCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Screen where a user can invite a friend to Blend or join a friend's Blend room.
 */
@Composable
fun BlendInviteScreen(
    onClose: () -> Unit,
    onBlendReady: (BlendedPlaylist) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var myProfile by remember { mutableStateOf<TasteProfile?>(null) }
    var roomCode by remember { mutableStateOf<String?>(null) }
    var isCreating by remember { mutableStateOf(true) }
    var enteredCode by remember { mutableStateOf("") }
    var isJoining by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }

    val serverUrl = BuildConfig.LISTEN_TOGETHER_SERVER.ifBlank { "https://bitchord-listen-together.onrender.com" }

    // Load local listening profile and create room
    LaunchedEffect(Unit) {
        val summary = withContext(Dispatchers.IO) {
            ListeningStats.summary(ReplayPeriod.ALL_TIME)
        }
        val profile = TasteProfile(
            userId = "device_${System.currentTimeMillis().toString().takeLast(6)}",
            displayName = "You",
            topArtists = summary.artists.take(30).map { TasteArtist(it.browseId ?: it.title, it.title, weight = 1.0f, plays = it.plays) },
            topTracks = summary.songs.take(30).map { TasteTrack.fromSong(it.song, it.plays) },
            topGenres = summary.genres.take(10).map { it.title },
            totalMinutes = summary.minutes,
        )
        myProfile = profile

        // Automatically create a waiting room for sharing
        BlendRepository.createOnlineSession(serverUrl, profile)
            .onSuccess { code ->
                roomCode = code
                isCreating = false

                // Start waiting for friend to join in the background
                scope.launch {
                    BlendRepository.waitForPartner(serverUrl, code)
                        .onSuccess { partnerProfile ->
                            val playlist = BlendCalculator.curatePlaylist(profile, partnerProfile)
                            onBlendReady(playlist)
                        }
                        .onFailure {
                            // If timeout, session can simply be renewed
                        }
                }
            }
            .onFailure { err ->
                errorMessage = err.message
                isCreating = false
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0E))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Blend with a Friend",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Merge your musical tastes into a shared, dynamic playlist curated for both of you.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(28.dp))

        // Card 1: Your Room Code & QR
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "YOUR BLEND CODE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1DB954), // Spotify green
                    letterSpacing = 2.sp,
                )

                Spacer(Modifier.height(12.dp))

                if (isCreating) {
                    CircularProgressIndicator(
                        color = Color(0xFF1DB954),
                        modifier = Modifier.size(40.dp).padding(8.dp),
                    )
                } else if (roomCode != null) {
                    Text(
                        text = roomCode!!,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.W900,
                        color = Color.White,
                        letterSpacing = 6.sp,
                    )

                    Spacer(Modifier.height(16.dp))

                    // QR Code for easy scanning
                    val inviteLink = "bitchord://blend?code=${roomCode}"
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        QrCode(content = inviteLink, size = 136.dp)
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.10f))
                                .clickable {
                                    clipboard.setText(AnnotatedString(roomCode!!))
                                    copied = true
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (copied) "Copied!" else "Copy Code",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Waiting for your friend to enter this code or scan the QR...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Text(
                        text = errorMessage ?: "Failed to generate code. Try again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE53935),
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Divider
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.12f)))
            Text(
                text = "OR JOIN A FRIEND",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.12f)))
        }

        Spacer(Modifier.height(24.dp))

        // Card 2: Enter Partner's Code
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OutlinedTextField(
                value = enteredCode,
                onValueChange = { if (it.length <= 6) enteredCode = it.uppercase() },
                placeholder = { Text("Enter 6-char code", color = Color.White.copy(alpha = 0.3f)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Join,
                ),
                keyboardActions = KeyboardActions(
                    onJoin = {
                        if (enteredCode.length == 6 && myProfile != null) {
                            isJoining = true
                            errorMessage = null
                            scope.launch {
                                BlendRepository.joinOnlineSession(serverUrl, enteredCode, myProfile!!)
                                    .onSuccess { partnerProfile ->
                                        val playlist = BlendCalculator.curatePlaylist(partnerProfile, myProfile!!)
                                        onBlendReady(playlist)
                                    }
                                    .onFailure { err ->
                                        errorMessage = err.message ?: "Failed to join blend"
                                        isJoining = false
                                    }
                            }
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF1DB954),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            )

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (enteredCode.length == 6 && !isJoining) Color(0xFF1DB954) else Color.White.copy(alpha = 0.15f))
                    .clickable(enabled = enteredCode.length == 6 && !isJoining) {
                        if (myProfile != null) {
                            isJoining = true
                            errorMessage = null
                            scope.launch {
                                BlendRepository.joinOnlineSession(serverUrl, enteredCode, myProfile!!)
                                    .onSuccess { partnerProfile ->
                                        val playlist = BlendCalculator.curatePlaylist(partnerProfile, myProfile!!)
                                        onBlendReady(playlist)
                                    }
                                    .onFailure { err ->
                                        errorMessage = err.message ?: "Failed to join blend"
                                        isJoining = false
                                    }
                            }
                        }
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isJoining) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                } else {
                    Text(
                        text = "Blend Now",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (enteredCode.length == 6) Color.Black else Color.White.copy(alpha = 0.4f),
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE53935),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
