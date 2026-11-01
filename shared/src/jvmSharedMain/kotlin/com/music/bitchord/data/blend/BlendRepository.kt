package com.music.bitchord.data.blend

import com.music.bitchord.data.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
}

private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

@Serializable
private data class BlendCreateRequest(val profile: String)

@Serializable
private data class BlendCreateResponse(val code: String, val expiresIn: Int = 900)

@Serializable
private data class BlendJoinResponse(val profile: String)

@Serializable
private data class BlendStatusResponse(val ready: Boolean)

object BlendRepository {

    private fun normalizeBaseUrl(url: String): String =
        url.trim().trimEnd('/')

    /**
     * Publishes User A's taste profile to the party server and receives a 6-character room code.
     */
    suspend fun createOnlineSession(serverUrl: String, profile: TasteProfile): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val base = normalizeBaseUrl(serverUrl)
                val payload = profile.toCompactPayload()
                val requestBody = json.encodeToString(BlendCreateRequest(payload))
                    .toRequestBody(JSON_MEDIA_TYPE)

                val request = Request.Builder()
                    .url("$base/api/blend")
                    .post(requestBody)
                    .build()

                Http.client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Failed to create blend session: HTTP ${response.code}")
                    }
                    val bodyString = response.body.string()
                    val parsed = json.decodeFromString<BlendCreateResponse>(bodyString)
                    parsed.code
                }
            }
        }

    /**
     * Waits for User B to join the session and returns User B's TasteProfile.
     * Long-polls for up to 45 seconds.
     */
    suspend fun waitForPartner(serverUrl: String, code: String): Result<TasteProfile> =
        withContext(Dispatchers.IO) {
            runCatching {
                val base = normalizeBaseUrl(serverUrl)
                val request = Request.Builder()
                    .url("$base/api/blend/$code/wait")
                    .get()
                    .build()

                Http.client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Failed waiting for partner: HTTP ${response.code}")
                    }
                    val bodyString = response.body.string()
                    val parsed = json.decodeFromString<BlendJoinResponse>(bodyString)
                    TasteProfile.fromCompactPayload(parsed.profile)
                }
            }
        }

    /**
     * User B joins an existing session by code, delivers their profile, and returns User A's TasteProfile.
     */
    suspend fun joinOnlineSession(serverUrl: String, code: String, profile: TasteProfile): Result<TasteProfile> =
        withContext(Dispatchers.IO) {
            runCatching {
                val base = normalizeBaseUrl(serverUrl)
                val payload = profile.toCompactPayload()
                val requestBody = json.encodeToString(BlendCreateRequest(payload))
                    .toRequestBody(JSON_MEDIA_TYPE)

                val request = Request.Builder()
                    .url("$base/api/blend/$code/join")
                    .post(requestBody)
                    .build()

                Http.client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Failed to join blend: HTTP ${response.code}")
                    }
                    val bodyString = response.body.string()
                    val parsed = json.decodeFromString<BlendJoinResponse>(bodyString)
                    TasteProfile.fromCompactPayload(parsed.profile)
                }
            }
        }

    /**
     * Checks if a blend code has been paired.
     */
    suspend fun checkStatus(serverUrl: String, code: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val base = normalizeBaseUrl(serverUrl)
                val request = Request.Builder()
                    .url("$base/api/blend/$code/status")
                    .get()
                    .build()

                Http.client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Failed checking status: HTTP ${response.code}")
                    }
                    val bodyString = response.body.string()
                    val parsed = json.decodeFromString<BlendStatusResponse>(bodyString)
                    parsed.ready
                }
            }
        }
}
