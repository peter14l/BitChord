package com.music.bitchord.data.blend

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URI

data class ParsedBlendInvite(
    val code: String? = null,
    val payload: String? = null,
)

/**
 * Handles deep links for Spotify Blend invitations (both https:// and bitchord:// schemes).
 */
object BlendInviteLink {
    private const val EXTRA_CONSUMED = "bitchord.blendInviteConsumed"
    private val _pending = MutableStateFlow<ParsedBlendInvite?>(null)
    val pending: StateFlow<ParsedBlendInvite?> = _pending.asStateFlow()

    fun consume(intent: Intent?): Boolean {
        if (
            intent == null ||
            intent.action != Intent.ACTION_VIEW ||
            intent.getBooleanExtra(EXTRA_CONSUMED, false)
        ) return false

        val dataStr = intent.dataString ?: return false
        val uri = runCatching { URI(dataStr) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase()
        val host = uri.host?.lowercase()
        val path = uri.path?.lowercase().orEmpty()

        val isBlendUri = (scheme == "bitchord" && host == "blend") ||
            ((scheme == "https" || scheme == "http") && (host == "bitchord.kushagrasingh.in" || host == "bitchord.app") && path.startsWith("/blend"))
        if (!isBlendUri) return false

        val query = uri.rawQuery.orEmpty()
        val payload = if (query.contains("p=")) query.substringAfter("p=").substringBefore("&").trim() else null
        val code = if (query.contains("code=")) query.substringAfter("code=").substringBefore("&").trim() else null

        if (payload != null || code != null) {
            intent.putExtra(EXTRA_CONSUMED, true)
            _pending.value = ParsedBlendInvite(code = code, payload = payload)
            return true
        }
        return false
    }

    fun handled() {
        _pending.value = null
    }
}
