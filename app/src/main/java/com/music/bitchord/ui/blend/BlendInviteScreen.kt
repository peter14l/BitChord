package com.music.bitchord.ui.blend

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.QrCodeScanner
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
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.stats.ListeningStats
import com.music.bitchord.data.stats.ReplayPeriod
import com.music.bitchord.ui.components.QrCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Screen where a user can invite a friend to Blend or join a friend's Blend room.
 * Supports both online room server exchange and 100% offline/serverless direct link/QR exchange.
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
    var compactPayload by remember { mutableStateOf<String?>(null) }
    var roomCode by remember { mutableStateOf<String?>(null) }
    var serverOnline by remember { mutableStateOf(false) }
    var enteredCode by remember { mutableStateOf("") }
    var isJoining by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }

    val serverUrl = BuildConfig.LISTEN_TOGETHER_SERVER.ifBlank { "https://bitchord-listen-together.onrender.com" }

    // Load local listening profile and prepare direct invite payload
    LaunchedEffect(Unit) {
        val summary = withContext(Dispatchers.IO) {
            ListeningStats.summary(ReplayPeriod.ALL_TIME)
        }

        // Build taste profile, with starter favorites fallback if device has 0 play history
        val artists = if (summary.artists.isNotEmpty()) {
            summary.artists.take(25).map { TasteArtist(it.browseId ?: it.title, it.title, weight = 1.0f, plays = it.plays) }
        } else {
            listOf(
                TasteArtist("The Weeknd", "The Weeknd", 1.0f, 10),
                TasteArtist("Daft Punk", "Daft Punk", 1.0f, 8),
                TasteArtist("Taylor Swift", "Taylor Swift", 1.0f, 6),
                TasteArtist("Coldplay", "Coldplay", 1.0f, 5),
            )
        }

        val tracks = if (summary.songs.isNotEmpty()) {
            summary.songs.take(25).map { TasteTrack.fromSong(it.song, it.plays) }
        } else {
            listOf(
                TasteTrack("4NRXx6U8ABQ", "Blinding Lights", "The Weeknd", 10),
                TasteTrack("fJ9rUzIMcZQ", "Bohemian Rhapsody", "Queen", 8),
                TasteTrack("kJQP7kiw5Fk", "Despacito", "Luis Fonsi", 6),
                TasteTrack("JGwWNGJdvx8", "Shape of You", "Ed Sheeran", 5),
            )
        }

        val genres = if (summary.genres.isNotEmpty()) {
            summary.genres.take(8).map { it.title }
        } else {
            listOf("Pop", "Synthwave", "Rock", "R&B")
        }

        val profile = TasteProfile(
            userId = "device_${System.currentTimeMillis().toString().takeLast(6)}",
            displayName = "You",
            topArtists = artists,
            topTracks = tracks,
            topGenres = genres,
            totalMinutes = summary.minutes.coerceAtLeast(42L),
        )
        myProfile = profile
        compactPayload = profile.toCompactPayload()

        // Attempt online room session in background (non-blocking)
        BlendRepository.createOnlineSession(serverUrl, profile)
            .onSuccess { code ->
                roomCode = code
                serverOnline = true

                // Wait for partner on room server
                scope.launch {
                    BlendRepository.waitForPartner(serverUrl, code)
                        .onSuccess { partnerProfile ->
                            val playlist = BlendCalculator.curatePlaylist(profile, partnerProfile)
                            onBlendReady(playlist)
                        }
                }
            }
            .onFailure {
                // Server 404 or offline: direct QR & link invite mode remains 100% active
                serverOnline = false
            }
    }

    val directInviteLink = compactPayload?.let { "bitchord://blend?p=$it" } ?: "bitchord://blend"

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

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Merge your musical tastes into a shared playlist with match percentage and dual attributions.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(24.dp))

        // Card 1: Your Invite QR & Code
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (serverOnline) Color(0xFF1DB954) else Color(0xFF388E3C)),
                    )
                    Text(
                        text = if (serverOnline && roomCode != null) "ONLINE ROOM CODE" else "DIRECT BLEND INVITE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1DB954),
                        letterSpacing = 2.sp,
                    )
                }

                Spacer(Modifier.height(14.dp))

                if (serverOnline && roomCode != null) {
                    Text(
                        text = roomCode!!,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.W900,
                        color = Color.White,
                        letterSpacing = 6.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // QR Code (always visible for instant zero-server pairing)
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val qrContent = if (serverOnline && roomCode != null) {
                        "bitchord://blend?code=$roomCode"
                    } else {
                        directInviteLink
                    }
                    QrCode(content = qrContent, size = 146.dp)
                }

                Spacer(Modifier.height(16.dp))

                // Action buttons: Share & Copy
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Share Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1DB954))
                            .clickable {
                                val shareText = if (serverOnline && roomCode != null) {
                                    "Let's blend our music on BitChord! Use room code: $roomCode or tap: $directInviteLink"
                                } else {
                                    "Let's blend our music tastes on BitChord! Tap to blend: $directInviteLink"
                                }
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Blend Invite"))
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Share",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Share Invite",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                            )
                        }
                    }

                    // Copy Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable {
                                val toCopy = roomCode ?: directInviteLink
                                clipboard.setText(AnnotatedString(toCopy))
                                copied = true
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                contentDescription = "Copy",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (copied) "Copied!" else "Copy Link",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = if (serverOnline && roomCode != null) {
                        "Waiting for friend to enter code or scan QR..."
                    } else {
                        "Have your friend scan this QR or paste your invite link to blend instantly!"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // Solo Test / Demo Blend Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF6C5CE7).copy(alpha = 0.15f))
                .border(1.dp, Color(0xFF6C5CE7).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .clickable {
                    val partner = TasteProfile(
                        userId = "demo_luna",
                        displayName = "Luna",
                        topArtists = listOf(
                            TasteArtist("Billie Eilish", "Billie Eilish", 1.0f, 32),
                            TasteArtist("The Weeknd", "The Weeknd", 1.0f, 28),
                            TasteArtist("Lana Del Rey", "Lana Del Rey", 1.0f, 22),
                            TasteArtist("Daft Punk", "Daft Punk", 1.0f, 18),
                            TasteArtist("Olivia Rodrigo", "Olivia Rodrigo", 1.0f, 15),
                        ),
                        topTracks = listOf(
                            TasteTrack("4NRXx6U8ABQ", "Blinding Lights", "The Weeknd", 28),
                            TasteTrack("V1Pl8CzNzCw", "bad guy", "Billie Eilish", 25),
                            TasteTrack("fJ9rUzIMcZQ", "Bohemian Rhapsody", "Queen", 18),
                            TasteTrack("oHg5SJYRHA0", "drivers license", "Olivia Rodrigo", 16),
                            TasteTrack("TUVcZfQe-Kw", "Levitating", "Dua Lipa", 14),
                        ),
                        topGenres = listOf("Indie Pop", "Synthwave", "Alternative", "Pop"),
                        totalMinutes = 2450L,
                    )
                    val playlist = BlendCalculator.curatePlaylist(myProfile ?: partner, partner)
                    onBlendReady(playlist)
                }
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFA29BFE),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Try Demo Blend (Test Solo with Luna)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFA29BFE),
                )
            }
        }

        Spacer(Modifier.height(26.dp))

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

        Spacer(Modifier.height(20.dp))

        // Card 2: Enter Partner's Code or Link
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OutlinedTextField(
                value = enteredCode,
                onValueChange = { enteredCode = it.trim() },
                placeholder = {
                    Text(
                        "Paste friend's invite link or 6-char code",
                        color = Color.White.copy(alpha = 0.35f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        processJoin(
                            rawInput = enteredCode,
                            myProfile = myProfile,
                            serverUrl = serverUrl,
                            onJoining = { isJoining = it },
                            onError = { errorMessage = it },
                            onSuccess = onBlendReady,
                            scope = scope,
                        )
                    }
                ),
                trailingIcon = {
                    IconButton(onClick = {
                        val clip = clipboard.getText()?.text
                        if (!clip.isNullOrBlank()) {
                            enteredCode = clip.trim()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.ContentPaste,
                            contentDescription = "Paste",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF1DB954),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            )

            Spacer(Modifier.height(14.dp))

            val canJoin = enteredCode.isNotBlank() && !isJoining
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (canJoin) Color(0xFF1DB954) else Color.White.copy(alpha = 0.15f))
                    .clickable(enabled = canJoin) {
                        processJoin(
                            rawInput = enteredCode,
                            myProfile = myProfile,
                            serverUrl = serverUrl,
                            onJoining = { isJoining = it },
                            onError = { errorMessage = it },
                            onSuccess = onBlendReady,
                            scope = scope,
                        )
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
                        color = if (canJoin) Color.Black else Color.White.copy(alpha = 0.4f),
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
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

private fun processJoin(
    rawInput: String,
    myProfile: TasteProfile?,
    serverUrl: String,
    onJoining: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    onSuccess: (BlendedPlaylist) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val input = rawInput.trim()
    if (input.isBlank() || myProfile == null) return

    onJoining(true)
    onError(null)

    // Check if input is a direct compact payload or deep link
    val payload = when {
        input.contains("p=") -> input.substringAfter("p=").substringBefore("&").trim()
        input.length > 10 -> input
        else -> null
    }

    if (payload != null) {
        // Direct local decode — zero network call needed!
        runCatching {
            TasteProfile.fromCompactPayload(payload)
        }.onSuccess { partnerProfile ->
            onJoining(false)
            val playlist = BlendCalculator.curatePlaylist(myProfile, partnerProfile)
            onSuccess(playlist)
        }.onFailure { err ->
            onJoining(false)
            onError("Invalid blend link or profile data. Please try again.")
        }
    } else {
        // 6-character room code on server
        scope.launch {
            BlendRepository.joinOnlineSession(serverUrl, input.uppercase(), myProfile)
                .onSuccess { partnerProfile ->
                    onJoining(false)
                    val playlist = BlendCalculator.curatePlaylist(partnerProfile, myProfile)
                    onSuccess(playlist)
                }
                .onFailure { err ->
                    onJoining(false)
                    val msg = err.message.orEmpty()
                    if (msg.contains("404")) {
                        onError("Room server is currently offline. Please scan your friend's QR code or paste their invite link instead!")
                    } else {
                        onError(msg.ifBlank { "Could not join blend room. Try scanning QR instead." })
                    }
                }
        }
    }
}
