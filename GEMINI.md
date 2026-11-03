# GEMINI.md — BitChord Development Guide & System Reference

> **Context Guide for AI Pair Programmers, Agents, and Contributors**  
> *Last Updated: November 2026*  
> *Target Branch: `feat/blend-and-wrapped` / `main`*

---

## 1. Project Overview & Philosophy

**BitChord** is a high-performance, privacy-respecting, and aesthetic YouTube Music and multi-source audio client for Android, Wear OS, Android TV, and Web, with visual design inspired by modern Apple Music and Telegram frosted-glass aesthetics.

### Core Tenets
- **Ad-Free & Privacy-First:** No advertisements, no user tracking, and zero telemetry dispatched to third parties without explicit user consent.
- **Audiophile Fidelity:** True gapless playback, configurable crossfade (0–12s), DJ Automix transitions, and bit-perfect USB exclusive DAC output with source sample-rate preservation.
- **Multi-Source Hybrid Architecture:** Primary catalogue sourced from YouTube Music via reverse-engineered Innertube endpoints, augmented with pluggable external lossless audio addons (FLAC/ALAC), JioSaavn, and Spotify library synchronization.
- **Fluid & Dynamic UI:** Translucent frosted-glass headers/bars via Haze, dynamically generated Material 3 palettes derived from real-time album artwork, and word-by-word synced animated lyric canvases.

---

## 2. Repository & Module Architecture

The codebase is organized as a multi-module Kotlin Multiplatform (KMP) and Android project, alongside a Go microservices backend and a React/TypeScript web client.

```
BitChord/
├── app/                  # Main Android application (Phone/Tablet)
│   ├── src/main/java/    # Android-specific UI, MainActivity, PlaybackService, ViewModels
│   ├── src/main/cpp/     # Native C++ NDK audio DSP engine (bit-perfect USB DAC)
│   └── src/test/         # Unit and regression tests
├── shared/               # Kotlin Multiplatform Core Logic & Data Layer
│   ├── src/commonMain/   # KMP common models, enums, math utilities
│   ├── src/jvmSharedMain/# Innertube client, parsers, lyrics, sources, stats, Spotify importer
│   └── src/jvmTest/      # JVM unit tests for network, parsing, and algorithms
├── sharedUi/             # Compose Multiplatform UI Library
│   └── src/jvmSharedMain/# Reusable Compose UI screens, pills, dialogs, list items
├── wearos/               # Companion Wear OS smartwatch application
├── tv/                   # Android TV application with D-pad navigation
├── backend/              # Go microservice backends (Party & Blend rooms)
│   ├── party/            # Listen Together synchronized party room server
│   ├── blend/            # Ephemeral 6-char code room exchange for Spotify Blend
│   ├── hub/              # WebSocket connection multiplexer
│   └── clock/            # NTP-like monotonic playback clock sync
├── web/                  # Web player client (TypeScript, React, Vite)
└── docs/                 # Extended technical and architectural documentation
```

### Module Responsibilities

| Module | Technologies | Key Packages & Classes |
| :--- | :--- | :--- |
| **`:shared`** | KMP, Kotlin Coroutines, Ktor/OkHttp, Kotlinx Serialization | `YtMusicRepository`, `InnertubeParser`, `TrackMatcher`, `SpotifyImporter`, `MusicSource`, `BlendCalculator`, `LyricsHelper` |
| **`:sharedUi`** | Compose Multiplatform, Haze (frosted glass) | `SearchScreen`, `PillSegmentedControl`, `LyricsView`, `QueueView`, `BrowseView` |
| **`:app`** | AndroidX, Jetpack Compose, Media3 (ExoPlayer), NDK | `MainActivity`, `PlaybackService`, `MainViewModel`, `AudioEngine`, `BlendRevealStory`, `SpotifyLibraryScreen` |
| **`backend/`** | Go 1.22+, Gorilla WebSocket | `main.go`, `party.go`, `blend.go`, `clock.go`, `hub.go` |

---

## 3. Key Subsystems & Implementation Details

### 3.1. Innertube Engine & Search (`:shared`)
* **Endpoint Client:** Reverse-engineers YouTube Music's `WEB_REMIX` client (`Innertube.kt`). All requests route through `YtMusicRepository.call()`.
* **Search Filtering:** Supported filters via `SearchFilter`:
  * `ALL`: Mixed search page. **Must include both official catalogue tracks and video-first music tracks**, deduplicated by `v:${videoId}`.
  * `SONGS`: Filters strictly for official distributor / Topic audio releases.
  * `VIDEOS`: Filters strictly for video music uploads and live/channel versions.
  * `ALBUMS`, `ARTISTS`, `PLAYLISTS`: Filters browseable entities.
* **Track Matching (`TrackMatcher.kt`):** Heuristic title normalization, duration tolerance matching, and artist scoring to resolve cross-catalogue tracks between Spotify, YouTube, and Lossless Addons without false-positive cover matches.

### 3.2. Audio Playback & Media3 Architecture (`:app`)
* **Service:** `PlaybackService.kt` runs as a foreground media service integrating AndroidX Media3 `MediaSession`.
* **Audio Cache (`AudioCache.kt`):** Pre-fetches and streams chunks ahead of the playback cursor to guarantee gapless playback on weak connections.
* **Bit-Perfect DAC Output:** Utilizes custom Android NDK C++ audio pipelines (`app/src/main/cpp`) to query hardware sample rates and stream un-resampled PCM audio directly to external USB DACs.
* **Automix Engine:** Beat-matches consecutive queue items with transient tempo shifting and cross-fading based on BPM metadata and energy transitions.

### 3.3. Blend & Wrapped Systems (`:shared`, `:app`, `backend/blend/`)
* **Taste Profile (`TasteProfile.kt`):** Extracts top 50 listening history tracks, frequent artists, and mapped genres from user playback databases.
* **Blend Algorithm (`BlendCalculator.kt`):**
  * Multi-factor scoring: Artist overlap (40%), Track overlap (35%), Genre affinity (25%).
  * Generates a 40–50 track curated blended playlist with round-robin interleaving between contributors.
* **Ephemeral Backend Exchange (`backend/blend/blend.go`):**
  * In-memory room broker where User A opens a room with a 6-character code (15-minute TTL).
  * User B joins via code or QR link (`https://bitchord.app/blend?code=...`).
  * Once both users submit their encrypted `TasteProfile`, the backend computes the match, delivers the combined payload, and flushes the room.
* **Blend UI:**
  * `BlendInviteScreen.kt`: Shareable code and QR visualizer.
  * `BlendRevealStory.kt`: Fullscreen animated story breakdown (Match %, Shared Artists, Contrasts, Song Previews).
  * `BlendPlaylistScreen.kt`: Matches BitChord's default playlist UI (Taste banner, member badges, play/shuffle/save controls).

### 3.4. Spotify Importer & Library Sync (`:shared`)
* **Importer (`SpotifyImporter.kt`):**
  * Parses public Spotify playlist URLs (`https://open.spotify.com/playlist/...`) via web embed tokens and Spotify's Pathfinder GraphQL endpoint.
  * Matches Spotify metadata against YouTube Music: searches `SearchFilter.SONGS` first; if unmatched (e.g. for indie, video-first, or folk artists like Farya Faraji), **automatically falls back to `SearchFilter.VIDEOS`**.

### 3.5. Lyrics Engine & Translation (`:shared`, `:sharedUi`)
* Multi-source waterfall query: **LrcLib -> KuGou -> Musixmatch -> PaxSenix -> BiniLyrics -> Genius**.
* Word-by-word synced timing animation with dynamic syllable interpolation.
* In-line multi-lingual lyrics translation using neural translation backends with phonetic romaji/pinyin projection.

---

## 4. Development Environment & Prerequisites

Ensure the following tools are available in your development environment:

| Tool | Version / Specification | Notes |
| :--- | :--- | :--- |
| **JDK** | Java 17 (Eclipse Temurin 17) or JDK 21 | JDK 17 is standard for Android Gradle builds; JDK 21 is supported for JVM tests. |
| **Android SDK** | `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26` | Build Tools 35.0.0+ |
| **CMake & NDK** | CMake 3.22.1+, Android NDK 26+ | Required for compiling native audio DSP components in `:app`. |
| **Go** | Go 1.22+ | Required for developing and testing `backend/`. |
| **Node.js & pnpm** | Node 20+, pnpm 9+ | Required for the `web/` module. |

---

## 5. Build, Test, and CI Commands

All commands should be executed from the repository root.

### Android Application Builds
```bash
# Assemble Development Debug APK
./gradlew assembleDevDebug

# Assemble Production Signed Release APK (used for GitHub Releases)
./gradlew assembleProdRelease

# Assemble Wear OS Companion App
./gradlew :wearos:assembleProdRelease
```

### Testing Suites
```bash
# Run JVM tests for shared core and shared UI modules
./gradlew :shared:jvmTest :sharedUi:jvmTest

# Run unit tests on Android app module (requires Android SDK)
./gradlew testDevDebugUnitTest

# Run a specific unit test
./gradlew :shared:jvmTest --tests "com.music.bitchord.data.innertube.*"
```

### Go Backend (Party & Blend Servers)
```bash
cd backend

# Run all backend unit and concurrency tests
go test -v ./...

# Run the local party & blend server on :8080
go run main.go
```

### Sandboxed / Headless Environment Considerations
When compiling or testing inside a sandbox or Codespace without host Android SDK paths:
```bash
# Run JVM tests headlessly with tmp directories to prevent read-only lockouts
mkdir -p /tmp/android
ANDROID_USER_HOME=/tmp/android JAVA_HOME=/usr/local/sdkman/candidates/java/21.0.13.0+2-ms ./gradlew :shared:jvmTest :sharedUi:jvmTest
```

---

## 6. Git, Branching, and CI Workflow Rules

### Branch Strategy
- **`upstream/main` (`kushagrasinghx/BitChord`):** Production release baseline. **Never push directly to upstream main.**
- **`origin/feat/blend-and-wrapped` (`peter14l/BitChord`):** Active feature branch for Blend, Wrapped, and UI enhancements.
- Keep commits atomic, well-scoped, and prefixed using Conventional Commits (`feat:`, `fix:`, `refactor:`, `perf:`, `docs:`, `ci:`).

### GitHub Actions Workflows (`.github/workflows/`)
- **`android.yml` (Android Release APK):**
  - Triggers on push to `main` and `feat/blend-and-wrapped`.
  - Automatically handles keystore signing (uses self-signed fallback key for fork builds so APKs are immediately installable).
  - Uploads `bitchord-prod-release-signed` and `bitchord-wear-prod-release-signed` artifacts.
- **`check.yml` (CI & Validation):**
  - Runs Go backend test matrix and Android/JVM linting and tests.
- **Troubleshooting Fork Workflows:**
  - If a push does not trigger CI, check repository settings: GitHub frequently disables Actions on forks after periods of inactivity. Ensure Actions are enabled under **Settings -> Actions -> General**.

---

## 7. Code Quality & Architectural Guidelines

1. **Maintain Documentation & Comments:**
   - Preserve existing architectural notes, comments, and docstrings throughout the codebase. Every complex regex, API protocol, or state transition must be properly documented.
2. **State Management in Jetpack Compose:**
   - Hoist state to ViewModels (`MainViewModel.kt`).
   - Use `StateFlow` and Compose `collectAsStateWithLifecycle()` or `rememberSaveable`.
   - Avoid instantiating costly objects (e.g. `Json` serializers, regexes, paint brushes) inside composables; retain them via `remember` or companion objects.
3. **Audio Telemetry vs. Physical Reality:**
   - As outlined in `CONTRIBUTING.md`: Application telemetry must never be presented as proof of physical hardware behavior unless verified with external hardware meters or DAC readout. Clearly differentiate between decoder output, AudioFlinger HAL states, and hardware endpoint capabilities.
4. **Licensing Integrity (GPLv3):**
   - BitChord is licensed under **GNU General Public License v3.0**.
   - No proprietary, closed-source, or GPLv3-incompatible libraries may be introduced. Evaluate any prospective dependency for binary footprint, performance impact, and license compatibility before adoption.
