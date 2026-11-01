package com.music.bitchord.ui.replay

/**
 * Visual themes available when rendering the Replay / Wrapped share poster.
 */
enum class ReplayPosterTheme(val displayName: String) {
    AURORA_GLASS("Aurora Glass"),
    NEO_BRUTALIST("Neo-Brutalist"),
    THERMAL_RECEIPT("Receipt"),
    ANALOG_CASSETTE("Cassette");

    companion object {
        val all: List<ReplayPosterTheme> = entries
    }
}
