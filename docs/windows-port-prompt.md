# Alvand Player — Windows Desktop Port: Master Prompt

> **How to use:** paste everything inside the `=== PROMPT BEGIN ===` / `=== PROMPT END ===`
> fence below as a single instruction to a coding agent. It is written to be self-contained.
> Every hard number in it was extracted from the shipped Android source (v1.6.9) — treat the
> Android repository as the source of truth and re-verify any value against the code before
> implementing it.

=== PROMPT BEGIN ===

## 1. Role and mission

You are a senior desktop engineer. Build a **native Windows desktop port of "Alvand Player"**,
an existing shipped Android music player. You are porting a *finished, released product* — not
designing a new one. The Android app is the specification.

**Source of truth (read it before writing any code):**
- Repository: `Alvandcode/Alvand-Player` (MIT licensed, owned by the requester)
- Android version being ported: **1.6.9** (`versionCode 16`), tag `v1.6.9`
- Key paths: `app/src/main/java/com/alvand/player/**`, `app/src/main/res/values/strings.xml`
  and `values-fa/strings.xml`, `app/schemas/**`, `app/src/test/**`, `CHANGELOG.md`, `README.md`
- If the repository is not available locally, clone it. Do not proceed on assumptions when
  source is reachable.

**Deliverable:** a complete, buildable, signed-ready Windows desktop application that reproduces
the Android app's behaviour and visual identity, including its glass design system, the curved
scrub arc, the dynamic-artwork accent, the 4 accent themes, the sleep-timer fade, the lyrics
engine, and the audio DSP chain. Plus tests, an installer, and documentation.

**Success criterion:** a user who knows the Android build should not feel they switched products,
except where Windows genuinely differs (and every such difference must be listed in
`PORT-DIFFERENCES.md` with a justification).

---

## 2. Target product definition

A minimal, glass-morphism **local music player** for Windows 10 (1809+) and Windows 11, x64 and
ARM64. Single-window desktop app. It plays the user's own audio files from disk, plus HTTPS
direct links. It has no account, no cloud, no telemetry, and no advertising. The only permitted
network call is an optional, user-opt-in lyrics lookup against LRCLIB.

Non-goals: streaming-service integrations, downloads, transcoding UI, video, podcasts,
multi-user profiles, and any form of background telemetry or crash reporting to a server.

---

## 3. Mandatory technology decision

**Use Kotlin + Compose Multiplatform (desktop, JVM target) with a JVM-native audio backend.**

Rationale — this is the whole point of the port:
- The Android app's entire design system (`AlvandPalette`, `AccentTheme`, `DynamicAccent`,
  `GlassCard`, `CoverHalo`, `ProgressArc`, `BottomArcHandle`, `MiniBars`, `ControlsRow`) is
  Jetpack Compose. Porting to WinUI 3/WPF means rewriting all of it and losing fidelity.
- The lyrics engine, LRC parser, library sort/filter/grouping logic, direct-link validation,
  and the equalizer preset math are plain Kotlin and can be moved almost verbatim.
- One language, one design token source, one test suite.

Required stack:
- Kotlin 2.0.21, Compose Multiplatform desktop, Material 3
- Gradle 8.9+ with a version catalog (mirror the Android repo's `gradle/libs.versions.toml`)
- Coroutines 1.9.x for the same structured-concurrency patterns
- JVM persistence: SQLite via **JetBrains Exposed** (preferred) or plain JDBC + Flyway-style
  versioned migrations. Room is Android-only; do **not** try to use Room on desktop.
- Tag/metadata reading: **jaudiotagger** (pure JVM, covers ID3v1/ID3v2, MP4/M4A, Vorbis, FLAC,
  APE, Ogg, and embedded cover art)
- HTTP: **OkHttp 4.12** (same library as Android) with the same timeouts
- Image loading: **Coil 3** (or Coil 2) for disk/HTTP artwork
- Packaging: **WiX v4** toolset (MSI + EXE bootstrapper) plus a portable ZIP.
  MSIX only if the requester asks for Store distribution.

**Explicitly rejected** (do not choose these): WinUI 3 / WPF / Avalonia / Electron /
.NET MAUI / Tauri. Each forces a full rewrite of the design system or the runtime. If you
believe the stack decision is wrong, stop and argue it *before* writing code — do not silently
switch.

---

## 4. Non-negotiable parity rules

1. **No invented features.** Port what exists. If you believe a feature is missing from Android,
   do not add it. Record it in `PORT-DIFFERENCES.md` as a proposal, separately.
2. **No silent redesign.** Every colour, radius, duration, easing curve, and layout dimension
   listed in this prompt is a contract. Deviations require a written justification.
3. **Behaviour before pixels.** Functional parity (queue semantics, error handling, persistence,
   DSP correctness) outranks visual polish.
4. **Offline-first.** Every feature except the opt-in lyrics lookup must work with no network.
5. **HTTPS only.** Direct links must be `https://`. No cleartext HTTP. No exceptions.
6. **No telemetry, no analytics, no crash upload.** Crash handling is local-only (see §17).
7. **Ship tests with the feature.** §21 is not optional; a port without parity tests is
   incomplete.
8. **Read before you write.** Every constant in this prompt has a counterpart in the Android
   source. Open the file, confirm, then port. If this prompt and the source disagree, **the
   source wins**, and you must note the discrepancy in `PORT-DIFFERENCES.md`.

---

## 5. Repository layout for the Windows app

Create a new repository `Alvandcode/Alvand-Player-Windows` (or a `windows/` module if staying
in one repo — your call, state it in the README). Suggested Gradle module split:

```
Alvand-Player-Windows/
├── shared/                 (Kotlin Multiplatform: pure logic, no UI, no Windows APIs)
│   └── src/commonMain/kotlin/com/alvand/player/
│       ├── model/          Song, Album, Artist, Playlist, PlayHistory, AudioSettings,
│       │                    LyricLine, LyricsResult, SleepTimerState
│       ├── library/        SUPPORTED_EXTENSIONS, isSupportedPath, fromDirectLink,
│       │                    LibrarySort, LibraryFilter.filterAndSort, groupByAlbum,
│       │                    groupByArtist, indexOf
│       ├── lyrics/         LyricsManager (pure parts), LrcParser, Id3UsltScanner,
│       │                    LrclibClient, cachePathFor, sanitizeFileName, decodeText
│       ├── audio/          DspSpec, presetCurves, interpCurve, applyNoiseReductionMath,
│       │                    SleepTimerEngine (with injected clock)
│       └── theme/          AlvandPalette, AccentTheme, AccentThemes, sanitizeAccent,
│                            dynamicBackgroundBrush, tinting
├── desktopApp/             (Compose Multiplatform UI, Windows integration)
│   └── src/jvmMain/kotlin/com/alvand/player/
│       ├── ui/screens/     WelcomeScreen, PlayerScreen, AboutScreen
│       ├── ui/components/  GlassCard, PlayerBackground, AmbientBlob, CoverHalo,
│       │                    ProgressArc, MiniBars, ControlsRow, ArtImage, BottomArcHandle,
│       │                    MenuSheet, EqSheet, LyricsSheet, SleepTimerDialog,
│       │                    ThemeDialog, BackgroundDialog, LanguageDialog, CrashDialog
│       ├── player/         PlayerEngine (interface), engine/*, PlaybackCoordinator
│       ├── library/        FolderScanner, DirectoryWatcher, TagReader, ArtworkCache,
│       │                    DominantColorExtractor
│       ├── data/           Database (SQLite), DAOs, SettingsStore
│       ├── system/         SmtcIntegration, MediaKeys, TrayIcon, FileAssociations,
│       │                    SingleInstance, StartupEntry, DeepLinks, CrashLog
│       └── Main.kt
├── shared/src/commonTest/  (ported unit tests)
├── installer/              (WiX v4 sources, icon, upgrade code)
└── docs/                   PORT-DIFFERENCES.md, BUILD.md, PUBLISH.md, SHORTCUTS.md
```

The `shared`/`desktopApp` split is a hard requirement: it is what makes the logic testable
without a display and what keeps the Android-portable code free of Windows API calls.

---

## 6. Feature parity matrix

Every Android feature and its required Windows counterpart. Implement in this order.

| # | Android feature (source) | Windows implementation | Parity notes |
|---|---|---|---|
| 1 | All local formats via ExoPlayer/Media3 (`README` "فرمت‌ها") | Backend with equivalent decoder coverage | Must handle: `mp3, wav, flac, ogg, oga, opus, m4a, aac, wma, alac, aiff, aif, amr, mid, midi, mp4, mka, mkv, ts, m3u8, mpd` |
| 2 | HLS / DASH streaming | Same engine, streaming enabled | `.m3u8` → HLS, `.mpd` → DASH |
| 3 | Background playback (foreground service, `PlaybackService`) | Audio runs on a non-UI thread; closing the window hides to tray, never stops audio | Tray icon is the persistent surface |
| 4 | MediaSession + notification + lock screen controls | **SMTC** (System Media Transport Controls) | Title/artist/album/artwork + play/pause/next/prev/seek/shuffle/repeat |
| 5 | Android Auto browse tree (`PlaybackService.onGetLibraryRoot/onGetChildren`, `ROOT_ID`, `MAX_BROWSE_PAGE=100`) | SMTC is display-only; expose the full queue to OS media keys and the OS volume-flyout | Document as a platform-inherent difference |
| 6 | Home-screen widget (`PlayerWidgetProvider`, 3 actions, live title/artist/state) | **Tray icon** with the same 3 actions and live state | Parity target |
| 7 | Media button receiver (`MediaButtonReceiver`) | Global media-key handling (`WM_APPCOMMAND` / `RegisterHotKey`) + SMTC | Parity target |
| 8 | Audio focus + `setHandleAudioBecomingNoisy(true)` | **WASAPI session notifications**: pause on device unplug / exclusive-mode loss; cooperate with other apps via `IAudioSessionManager` session grouping | Parity target; must not fight other players |
| 9 | Wake lock (`WAKE_MODE_LOCAL`) | Not applicable; keep the display awake only if a fullscreen visualizer is added | Document |
| 10 | Runtime audio permission + graceful denial | No permission model; the "Allow access" CTA becomes "Choose music folder" | Content-preserving, not parity |
| 11 | MediaStore library scan (`SongRepository.loadLocalSongs`, `IS_MUSIC != 0 AND DURATION > 30000`, `DATE_ADDED DESC`, limit 8000) | Folder-based recursive scan (see §13) | Content-parity, mechanism differs |
| 12 | Manual "Scan" button + "N new songs" toast | Rescan button + "N added" notification | Parity |
| 13 | First-run demo playlist when scan is empty (`demoPlaylist()`, 4 remote tracks) | Same, verbatim, including the `Dreamy Night` metadata | Parity |
| 14 | Direct link (paste HTTPS URL, `VIEW`/`SEND` intent filters) | Paste dialog + `alvand://` protocol handler + `.m3u8`/`.mpd` file association | Validation rules identical (§9) |
| 15 | File import via SAF (`pickAudio`, `audio/*`) | Native open dialog + **drag & drop** of files/folders onto the window | Drag & drop is a free Windows win; keep it |
| 16 | Lyrics: 4 sources, LRCLIB, manual entry, cache | Identical engine, plus a "search next to the file" that works on Windows | Algorithm-exact (§12) |
| 17 | 5-band EQ + presets + BassBoost + Reverb + LoudnessEnhancer + noise reduction (`EqualizerManager`) | Engine-side DSP graph (see §11) | Math and ranges identical; presets are now ours, not the device's |
| 18 | Sleep timer: 7 presets + end-of-track, sqrt perceptual fade | Identical `SleepTimerEngine` with injected clock | Algorithm-exact (§10) |
| 19 | Custom background photo (`SettingsRepo.backgroundUri`, SAF) | Native open dialog, persisted path, "Remove" | Parity |
| 20 | Light / Dark / System (`theme_mode`) | Follow `AppsUseLightTheme` registry/`SystemParametersInfo`, overridable | Parity |
| 21 | 4 accent themes (`accent_theme`, §8) | Identical | Parity, pixel-exact |
| 22 | In-app language switch, `en` + `fa` only (`AppLocale`) | In-app language switch, `en` + `fa` only | Parity; do **not** add locales |
| 23 | Library sort (5 modes) + search + favourites + album/artist grouping (`LibrarySort`, `LibraryFilter`) | Identical pure functions + the UI that renders them | Android has the logic but **no UI for sort/search/favourites** — see §5 note below |
| 24 | Playlists: create/rename/delete/add/dedupe/remove (`PlaylistRepository`, `LibraryDao`) | Identical schema and DAO logic; **wire the existing `PlaylistSheets` UI**, which Android never renders | Android has unwired UI — reuse it |
| 25 | Play history with `playCount` and 90-day prune | Identical | Parity |
| 26 | Favourites in DataStore string set (`liked_ids`) | Identical (settings store) | Parity |
| 27 | Onboarding/welcome screen (`onboarding_seen`) | Identical | Parity |
| 28 | About screen: GitHub, website, Telegram, TON donation + copy | Identical, links open in the default browser | Parity |
| 29 | Local crash log + report dialog (`CrashLog`, §17) | Identical, file-based | Parity |
| 30 | Dynamic accent from artwork (`DynamicAccent`, HSL sanitize) | Identical math; replace the Android palette extractor | Algorithm-exact (§8) |
| 31 | Toast/snackbar error surfacing (`play_error`) | In-app snackbar | Parity |
| 32 | 2 Hz position pump (`MusicPlayerManager.startProgress`, 500 ms) | Same cadence | Parity; consider 4 Hz only behind a measurement (§18) |

**Note on 23/24:** the Android app has playlist, history, search, sort, and favourites logic plus
ready-made `PlaylistSheets.kt` UI, but nothing renders them. The port must ship that UI. This is
the one place where the port is *ahead* of Android; it is still parity-with-the-codebase, and it
must be listed in `PORT-DIFFERENCES.md`.

---

## 7. Design system port — exact tokens

These values are contracts. Copy them into a single `theme/Tokens.kt` and do not scatter magic
numbers through the UI.

### 7.1 `AlvandPalette` (12 fields)

```kotlin
@Immutable data class AlvandPalette(
    val bg: Color, val ink: Color, val sub: Color, val line: Color, val track: Color,
    val card: Color, val sheet: Color, val glass: Color, val glassBorder: Color,
    val sheen: Color, val scrim: Color, val isDark: Boolean
)
```

| Field | Light | Dark |
|---|---|---|
| `bg` | `#F4F4F6` | `#0C0C10` |
| `ink` | `#111114` | `#F2F2F5` |
| `sub` | `#5E5E66` | `#B4B4BE` |
| `line` | `#DFDFE5` | `#33FFFFFF` |
| `track` | `#CFCFD6` | `#44FFFFFF` |
| `card` | `#FFFFFF` | `#17171D` |
| `sheet` | `#FFFFFF` @ α 0.97 | `#17171D` @ α 0.97 |
| `glass` | `#FFFFFF` @ α 0.72 | `#FFFFFF` @ α 0.20 |
| `glassBorder` | `#FFFFFF` @ α 0.85 | `#FFFFFF` @ α 0.25 |
| `sheen` | `#FFFFFF` @ α 0.55 | `#FFFFFF` @ α 0.18 |
| `scrim` | `#000000` @ α 0.38 | `#000000` @ α 0.60 |
| `isDark` | `false` | `true` |

Support colours: `AlvandDeep #1B1040`, `AlvandPurple #6C4CF1`, `AlvandLavender #B9A7FF`,
`AlvandPink #F3A8FF`, `AlvandPeach #FFC9A8`, `AlvandCream #FFF6E9`, `MonoInk #111114`,
`MonoBg #F4F4F6`, `MonoSub #6E6E73`, `MonoLine #E2E2E6`, `MonoTrack #D8D8DC`,
`ArtDark1 #0E1620`, `ArtDark2 #2A3A52`.

### 7.2 `AccentTheme` — the 4 themes

```kotlin
@Immutable data class AccentTheme(
    val id: Int, val light: Color, val dark: Color,
    val lightDeep: Color, val darkDeep: Color
) {
    fun accent(isDark: Boolean) = if (isDark) dark else light
    fun deep(isDark: Boolean)   = if (isDark) darkDeep else lightDeep
    val isColored get() = id != AccentThemeMode.MONO
}
```

| id | Name | light | dark | lightDeep | darkDeep |
|---|---|---|---|---|---|
| 0 | Minimal (`accent_mono`) | `#111114` | `#B9A7FF` | `#111114` | `#2A3A52` |
| 1 | Royal (`accent_royal`) | `#6C4CF1` | `#B9A7FF` | `#1B1040` | `#160C33` |
| 2 | Sunset (`accent_sunset`) | `#E8590C` | `#FFB088` | `#3A1206` | `#2E0E04` |
| 3 | Ocean (`accent_ocean`) | `#0E7490` | `#67E8F9` | `#04222C` | `#031B23` |

`AccentThemeMode` = `{ MONO=0, ROYAL=1, SUNSET=2, OCEAN=3, COUNT=4 }`.
`accentThemeAt(id)` must fall back to element 0 for any unknown id (port `AccentThemeTest`).

### 7.3 Palette tinting — `AlvandPalette.tintedWith(accent, isDark)`

- For `MONO`: **return `this` unchanged** (identity — a test asserts this).
- Otherwise `lerp` each field toward `accent.accent(isDark)` with these exact factors:

| Field | Factor |
|---|---|
| `bg` | 0.05 |
| `card` | 0.09 |
| `sheet` | 0.06 |
| `glass` | 0.18 |
| `glassBorder` | 0.38 |
| `sheen` | 0.28 |

`ink`, `sub`, `line`, `track`, `scrim`, `isDark` are **never** tinted.

### 7.4 Dynamic accent from artwork

Android: `Palette` (androidx) → dominant/vibrant/dark-vibrant colours → HSL sanitisation.
Desktop: `androidx.palette` is unavailable, so implement a `DominantColorExtractor` (median-cut or
OKLab k-means over a 64×64 downsample) and **feed it into the identical HSL pipeline** so the
result looks the same.

```kotlin
fun sanitizeAccent(raw: Color, dark: Boolean): Color {
    colorToHSL(raw)
    hsl[1] = hsl[1].coerceIn(0.45f, 0.95f)                       // saturation
    hsl[2] = if (dark) hsl[2].coerceIn(0.55f, 0.80f)            // lightness
             else          hsl[2].coerceIn(0.28f, 0.55f)
    return HSLToColor(hsl)
}
```

For the deep/background variant: take the dark-vibrant colour and coerce **lightness into
`0.12..0.30`**, leaving saturation untouched.

`DynamicAccent(accent, accentDark, fromArtwork)`. Fallback chain (port exactly):
1. If the active accent theme `isColored` → `DynamicAccent(accentTheme.accent(dark), accentTheme.deep(dark), fromArtwork = false)`
2. else if `dark` → `DynamicAccent(#B9A7FF, #2A3A52, false)`
3. else → `DynamicAccent(MonoInk #111114, ArtDark2 #2A3A52, false)`

The `LaunchedEffect`/`derivedState` keys in the Android source are `(song?.id, dark, accentTheme.id)`.
Port the key set, not just the values. Animate both colours with `animateColorAsState`.

### 7.5 Background composition

`dynamicBackgroundBrush(accent, base)`: vertical gradient
`accentDark @ α0.28 → accent @ α0.10 → base`.

`PlayerBackground` is **three mutually exclusive layers, then one ambient blob**:

1. No custom background **and** the track has embedded art:
   full-bleed cover at `ContentScale.Crop`, **blurred 70 dp**, α 0.38 (dark) / 0.30 (light),
   then a vertical scrim `bg @0.42 → bg @0.72 → bg`.
   *(Android only blurs on API ≥ 31; on Windows, always blur.)*
2. Custom background image set:
   `AsyncImage` of the chosen file, `ContentScale.Crop`, crossfade, then scrim
   `scrim → scrim @ (scrim.alpha * 0.55) → bg @0.88`. The blurred cover is **not** drawn in this branch.
3. Neither: vertical gradient `accentDark @0.55 (dark) | 0.26 (light) → accent @0.20 (dark) | 0.09 (light) → bg`.

Then `AmbientBlob(color = accent.accent, center = Offset(0.5, 0.08), radiusFraction = 0.55,
alpha = 0.14 if a cover or custom photo exists else 0.22)`, blurred 90 dp.
*(The Android port skipped this layer below API 31; on Windows it is always available — use it.)*

### 7.6 Glass surfaces

- `GlassCard`: `RoundedCornerShape(24.dp)`, `color = pal.glass`, 1 dp border `pal.glassBorder`,
  shadow elevation 10 dp, plus a top sheen overlay (vertical gradient over the first 320 px).
- Modal sheets: `ModalBottomSheet(containerColor = pal.sheet)`, sheet corner radius 28 dp
  (player bottom sheet), no drag handle on the player sheet.
- `ControlsRow` **must** be wrapped in `CompositionLocalProvider(LocalLayoutDirection provides Ltr)`
  even in the Persian UI — this is deliberate, so the transport controls never mirror.

---

## 8. Player screen port — layout and interaction

These are the highest-risk fidelity items. Get them right.

### 8.1 Layout

- Root: bottom-sheet scaffold, `sheetPeekHeight = 0.dp`, sheet shape
  `RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)`, `sheetContainerColor = pal.sheet`,
  no drag handle. Background composes behind everything, full-bleed.
- `BoxWithConstraints` → `wide = maxWidth > 600.dp`
  - Cover diameter = `wide ? 360.dp : maxWidth * 0.80f` clamped to `240..400.dp`
  - Content column = `width(560.dp)` centred when `wide`, else `fillMaxSize()`; wrapped in `verticalScroll`
  - Horizontal padding: 20 dp narrow, 60 dp wide
- Top row: circular menu `IconButton` 44 dp, background `pal.card @0.92`, `CircleShape`, leading side.
  Trailing side: the sleep chip, visible **only** while a timer is active.
- Cover: `CoverHalo` behind `ArtImage`, `shadow(28.dp, CircleShape)`, halo diameter = `cover + 72.dp`.
- Title 20.sp ExtraBold, `letterSpacing 0.2.sp`; artist 13.sp `letterSpacing 0.4.sp`; both centred,
  1 line, ellipsis, 28 dp horizontal padding.
- Queue block: tappable collapse row (chevron-down 28 dp tinted `dyn.accent`),
  header `"<playlist> (<count>)"`, then a `LazyColumn` with `heightIn(max = 460.dp)`, rows keyed by
  song id: 44 dp artwork at 12 dp radius, title, artist, duration, and `MiniBars` on the
  active+playing row. Active row background `pal.glass` at 16 dp radius.
- Below the queue: the lyrics preview card, then a second compact `ControlsRow(big = false)`.
- Bottom: `BottomArcHandle` — a bottom bar with a **centre bump** (`bumpW = 150.dp`,
  `bumpH = 30.dp`, 26 dp smooth corners, two cubic curves), 64 dp tall, clipped to that shape,
  containing an up-chevron and the label `"Playlist • <count>"`.

### 8.2 `CoverHalo` (Canvas)

- While playing: infinite pulse `0.35 ↔ 1.0` over **2200 ms**, `FastOutSlowInEasing`, `Reverse`
- Two stroked glow rings: radius offset 26 dp at `α = 0.10 * pulse`; 12 dp at `α = 0.28 * pulse`
- Edge ring: 3 dp stroke, round cap, `α = 0.85` playing / `0.30` paused
- Edge radius = `minDimension / 2 - 30.dp`
- No orbiting dot and no light bar — these were deliberately removed in v1.6.8. Do not re-add.

### 8.3 `ProgressArc` — the curved scrub bar (port this exactly)

Geometry:
- Container height **86 dp**, full width, 28 dp horizontal padding
- The path is a **downward-curving "smile" arc**, not a ring: quadratic Bezier from
  `(0, h * 0.16)` with control point `(w / 2, 2 * (h * 0.88) - h * 0.16)` to `(w, h * 0.16)`
- Track: full path, `pal.track`, 6 dp stroke, round cap
- Progress: `PathMeasure.getSegment(0, length * progress, segment, true)` in the accent colour
- Knob: white filled circle `r = 13.dp` + accent stroke `r = 13.dp` width `3.5.dp` + inner dot `r = 4.dp`,
  positioned with `PathMeasure.getPosTan(length * progress)`
- When `progress <= 0.001`, draw a **smaller start knob** (`r = 11 / 11 / 3.5 dp`) so the control
  is discoverable at position zero
- Cache the path and rebuild it only when width or height changes
- Time labels are forced LTR: `"<mm:ss> / <mm:ss>"`; position is clamped to
  `min(position, duration)` when `duration > 1 ms`

Touch model (this is the signature interaction — implement all three cases):
- Single `pointerInput(duration)` using `awaitEachGesture`; require movement beyond
  `viewConfiguration.touchSlop` before a drag begins
- Hit-test by sampling the `PathMeasure` at **120 points** and taking the nearest fraction,
  accepted only if the squared distance to the bar is `<= (64 dp)^2`
- **Tap on the bar** → **absolute** seek to that position
- **Drag that stays within 64 dp of the bar** → **absolute** seek
- **Drag beyond 64 dp (off the bar)** → **relative** horizontal seek:
  `dxFraction = (x - previousX) / width`, `newFraction = (lastFraction + dxFraction).coerceIn(0, 1)`
- All seeks go through `rememberUpdatedState(onSeekMs)`

### 8.4 Transport controls

Order is fixed: **Shuffle · Previous · Play/Pause · Next · Repeat**.

- `big` (player): play button 76 dp circle filled with `dyn.accent`, white icon; side icons 30 dp
- `small` (queue sheet): play 58 dp, side icons 26 dp
- Previous/Next glyphs are a fixed 34 dp
- Off-state tint is `pal.sub`
- Repeat icon is `Repeat` when off/all and `RepeatOne` when repeat-one
- Repeat cycles OFF → ALL → ONE → OFF
- There are **no** ±15 s buttons, no speed control, and no favourite button on the player
  screen in Android. Do not add them to the transport row. (A favourite action may live in the
  queue-row overflow, if you add row overflow at all — note it as a difference.)

### 8.5 `MiniBars` (4-bar equalizer animation)

4 bars, 3.5 dp wide, 2.5 dp spacing. Idle height 4 dp. Playing: infinite
`animateFloat(4f, 8f + (i * 7 % 10), tween(350 + (i * 53 % 300) ms, FastOutSlowInEasing), Reverse)`
per bar with per-index delay.

### 8.6 Empty and error states

- No permission concept on Windows. Empty state: "no music" + a "Choose music folder" action.
  While a scan is running, disable the action and show progress.
- Playback error: in-app snackbar with the localized `play_error` string, then clear the error
  state (the Android build shows a Toast and calls `clearError()`).
- Scan result: snackbar with `scan_new` (N) / `scan_none`.

---

## 9. Direct-link validation (port exactly)

`https://` only. No `http://`. These rules are unit-tested in Android — port the tests.

```kotlin
val SUPPORTED_EXTENSIONS = setOf(
    "mp3", "wav", "flac", "ogg", "oga", "opus", "m4a",
    "aac", "wma", "alac", "aiff", "aif", "amr", "mid",
    "midi", "mp4", "mka", "mkv", "ts", "m3u8", "mpd"
)
```

`isSupportedPath(path)`:
1. trim, and take a lowercase copy for the scheme test
2. `withoutQuery = p.substringBefore('?').substringBefore('#')`;
   `fileName = withoutQuery.substringAfterLast('/')`;
   `ext = fileName.substringAfterLast('.', "").lowercase()`
3. if `ext` is non-empty **and** in `SUPPORTED_EXTENSIONS` → `true` (this also applies to local paths)
4. else if the original was `https://` → `true` **only** if the lowercase URL contains
   `.m3u8` or `.mpd`
5. else `false` — so plain `http://` and extension-less HTTPS URLs are rejected

`fromDirectLink(url, titleFallback = "Stream")`:
1. trim; must start with the literal lowercase `"https://"` (case-sensitive) → else `null`
2. must satisfy `isSupportedPath` → else `null`
3. title = last path segment before `?`; blank → `titleFallback`
4. `URLDecoder.decode(title, "UTF-8")` inside `runCatching`
5. if the extension is in `SUPPORTED_EXTENSIONS`, strip it
6. `_` → space, collapse whitespace runs to a single space, trim; blank → fallback
7. `artist = "Direct link"`, `isRemote = true`, `durationMs = 0`,
   `mimeHint = AUDIO_MIME_BY_EXT[ext]` (may be `null` — that is expected for
   `wma, alac, aiff, aif, amr, mkv`)

MIME map (15 entries): `mp3→audio/mpeg`, `wav→audio/wav`, `flac→audio/flac`,
`ogg|oga→audio/ogg`, `opus→audio/opus`, `m4a→audio/mp4`, `aac→audio/aac`, `mid|midi→audio/midi`,
`m3u8→application/x-mpegURL`, `mpd→application/dash+xml`, `mp4→audio/mp4`,
`mka→audio/x-matroska`, `ts→video/mp2t`.

### Song ID scheme (three disjoint spaces — keep it disjoint)

- MediaStore items: `id >= 0`
- Direct links: `id = -((hashCode & 0x7FFFFFFF) + 1_000_000)` → always `< -999_999`
- Locally picked files: `id = -((uriString.hashCode & 0x7FFFFFFF) + 2_000_000)`

Windows has no MediaStore `_ID`, so define a third, stable local identity:
`id = (SHA-256(normalised absolute path).take(56 bits) & Long.MAX_VALUE)`, where "normalised" means
lowercased with `\` → `/`. It must be stable across rescans and must never collide with a remote
id. Document this substitution in `PORT-DIFFERENCES.md` and test that rescanning a folder does
not change any id.

---

## 10. Sleep timer (port the algorithm, not the code)

`SleepTimerState(active, remainingMs, totalMs, fading, justFinished)` plus
`progress = (remainingMs / totalMs).coerceIn(0, 1)`.

Constants: `MAX_MS = 12 h`, `MIN_MS = 5_000`.

`defaultFade(totalMs)`: if `totalMs <= MIN_MS` → `totalMs / 2`; else
`min(30_000, max(5_000, totalMs / 4)).coerceAtMost(totalMs)`.

`formatRemaining(ms)`: `"%d:%02d"`, or `"%d:%02d:%02d"` when hours > 0.

Constructor takes injected collaborators — this is what makes it testable, keep it:
`scope`, `setVolume: (Float) -> Unit`, `getVolume: () -> Float = { 1f }`,
`onExpire: () -> Unit`, `elapsedRealtime: () -> Long = { System.nanoTime() / 1_000_000 }`.

`start(totalMs, fadeMs = defaultFade(totalMs))`:
- `@Synchronized`; reject `totalMs < 5_000`; clamp to `MAX_MS`; `fade.coerceIn(0, totalMs)`
- cancel any previous job, **restoring volume only if the previous run was fading**
- capture `baseVolume = getVolume()` validated into `0f..1f`, else `1f`
- increment an `AtomicLong` generation

Loop:
- `deadline = elapsedRealtime() + totalMs`; tick every **1000 ms**
- `remaining = (deadline - elapsedRealtime()).coerceAtLeast(0)`
- while fading: `linear = remaining / fadeMs`, **`perceptual = sqrt(linear)`**,
  `setVolume(baseVolume * perceptual)`
- publish state each tick
- on expiry: call `onExpire()` **only if the generation still matches**, then `justFinished = true`
- in `finally`: restore `startVolume` and reset state, again only for the current generation

Integration:
- volume is the engine's master volume; `onExpire` = pause
- the UI clamps the preset to **1..720 minutes**
- end-of-track: `remain = (duration - position).coerceAtLeast(5_000)`; if duration is unknown, do
  nothing; `fadeMs = min(15_000, remain / 2)`
- UI presets: **5, 10, 15, 30, 45, 60, 90** minutes as a 3-per-row chip grid, plus an
  "End of current song" action; while active show remaining time, a progress bar
  (`1f - progress`), and a Cancel action. The chip is forced LTR, formatted `"☾ %1$s"`.

`SleepTimerTest` must be ported and must still cover: fade curve monotonicity, generation
guard, volume restore, clamping, and end-of-track.

---

## 11. Audio engine and DSP — the hardest part

### 11.1 Engine abstraction

Define a narrow interface and keep every UI/controller class engine-agnostic:

```kotlin
interface PlayerEngine {
    val state: StateFlow<EngineState>   // current, isPlaying, position, duration, queue,
                                        // shuffle, repeatMode, error
    suspend fun setQueue(items: List<PlayableItem>, startIndex: Int, autoPlay: Boolean)
    suspend fun play(); suspend fun pause(); suspend fun toggle()
    suspend fun next(); suspend fun previous()
    suspend fun seekTo(ms: Long)
    suspend fun setVolume(v: Float)
    fun setShuffle(enabled: Boolean)
    fun cycleRepeat()
    fun setDsp(spec: DspSpec)
    fun release()
}
```

Requirements for any engine implementation:
- Decoder coverage matching §6 row 1, including HLS and DASH
- Gapless playback across queue transitions
- Accurate `position`/`duration` reporting, including `C.TIME_UNSET`-style "unknown duration" for
  live streams (a live stream must not be seekable)
- `previous()` rule: if `position > 3000 ms` seek to 0, else go to the previous item, else seek to 0
- `next()` at the end of the queue with repeat off: **do nothing** (do not stop, do not wrap)
- All public methods safe to call from any thread; all failures reported through `state.error`,
  never thrown into the UI
- A `pendingQueue` slot for the window between construction and engine readiness, applied when
  the engine reports ready — port the "last-wins" semantics

### 11.2 Error and skip policy (from `PlaybackService.widgetListener`)

- On a playback error: log, notify the UI, and **auto-skip to the next item** only while
  `consecutiveErrors < 3` **and** a next item exists; then `seekToNext → prepare → play`
- Reset `consecutiveErrors = 0` on every `onIsPlayingChanged(true)`
- This is the "skip a corrupt file and keep going" behaviour. Preserve the cap.

### 11.3 EQ attach/retry analogue

Android attaches platform audio effects to an `audioSessionId` that may be `0` for the first few
hundred ms after playback starts, so it retries up to **8** times at **500 ms** intervals, then
gives up and releases. On Windows the DSP graph is owned by the engine, so this problem
disappears — **but the re-apply problem does not**: DSP settings must be re-applied on
(a) stream start, (b) output-device change, (c) sample-rate/format change, (d) engine restart.
Implement a `DspApplier` that listens to those events and re-applies the last `DspSpec`. Keep the
Android retry semantics documented in a comment so future maintainers know why it exists.

### 11.4 DSP mapping — Android effect → Windows implementation

| Android | Windows target | Mapping |
|---|---|---|
| `android.media.audiofx.Equalizer` | 10-band biquad/FFT equalizer in the audio graph | Band count is now **ours**: fix it at **10** (5 low/mid + 5 high/mid) or make it configurable, but it can no longer depend on the device. Keep the millibel UI range **−1500..+1500 mB**. |
| Device presets | Our preset set | Windows exposes no system presets, so the **11 anchored curves become canonical**: `rock, pop, jazz, classical, dance, bass, vocal, treble, latin, party, piano`. Display the first 8 as chips, exactly as Android does. |
| `BassBoost(0..1000 strength)` | Low-shelf biquad at 100–150 Hz | Map `strength` linearly to **0..+10 dB**; enable only when `strength > 0`. |
| `PresetReverb` | `aecho`/`reverb` or a generated small-room impulse | Expose **Off + 3 room sizes** (small / medium / large). Android's `AudioSettings.reverbPreset` has **no UI**; ship the UI or omit the field — your call, document it. |
| `LoudnessEnhancer(0..+10 dB)` | Pre-volume gain **+ brickwall limiter** | `targetGain = dB * 100`, same scale. The limiter is mandatory: on Windows, boosting without limiting WILL clip, because Android's `LoudnessEnhancer` is itself a limiter. **This is a correctness fix, not a feature — implement it and note it.** |
| "Noise reduction" (EQ-based hiss/hum) | Same EQ math, engine-side | Port verbatim (§11.5). It is a band-gain approximation, not a real NR algorithm — keep that honest, and optionally expose a true spectral NR as a clearly separate, clearly labelled extra. |
| `AudioAttributes(USAGE_MEDIA, CONTENT_TYPE_MUSIC, handleAudioFocus=true)` | `IAudioClient3` stream category `AudioMediaGame`/`AudioMedia`, exclusive off | Same intent: share the device politely, pause on focus loss and on becoming-noisy (device unplug). |

### 11.5 EQ preset math (port verbatim)

Fallback band centres: **60, 230, 910, 3600, 14000 Hz**.
Preset curves in dB at those 5 anchors:

| Key | Curve |
|---|---|
| `rock` | `[4, 3, -1, 2, 4]` |
| `pop` | `[-2, -1, 1, 2, 1]` |
| `jazz` | `[3, 2, 0, 2, 3]` |
| `classical` | `[3, 2, -1, 1, 2]` |
| `dance` | `[5, 3, 0, 1, 2]` |
| `bass` | `[5, 4, 1, 0, 0]` |
| `vocal` | `[0, 1, 3, 3, 1]` |
| `treble` | `[-2, -1, 1, 3, 5]` |
| `latin` | `[3, 2, 0, 2, 3]` |
| `party` | `[3, 2, 0, 2, 3]` |
| `piano` | `[2, 1, 0, 2, 1]` |

Presets are matched by `presetName.lowercase().contains(key)`. Interpolate **linearly in
log10(frequency)** and clamp at both ends. `applyNamedCurve` is the fallback when a preset
cannot be applied directly.

Noise reduction, per band, with `k = level / 100f` added on top of the user's base levels:

| Condition | Cut |
|---|---|
| `f < 120 Hz` | `-600 · k` (hum) |
| `1000 Hz ≤ f ≤ 4000 Hz` | `+150 · k` (vocal clarity) |
| `f > 8000 Hz` | `-1200 · k` (hiss) |
| `f > 5000 Hz` | `-600 · k` |
| otherwise | `0` |

Final band level = `base + cut`, clamped to the band range.

`AudioSettings` shape to preserve:
`eqEnabled: Boolean = true`, `preset: Int = 0` (**−1 means manual**),
`bandLevels: List<Int> = [0,0,0,0,0]` in millibel, `bassStrength: Int = 500` (0..1000),
`volumeBoostDb: Int = 0` (0..10), `noiseReduction: Boolean = false`, `noiseLevel: Int = 50`
(0..100), `reverbPreset: Int = 0` (0 = off).

EQ sheet UI contract: enable switch; a "play a song first" hint when the engine has no active
stream; preset chips (first 8); one slider per band labelled `"${hz}Hz"` for `1..999` Hz and
`"${hz/1000}k"` above that, with a 44 dp label column; Bass Boost 0..1000; Volume Boost 0..10
with `steps = 9`; Denoise switch + strength 0..100. Moving any band slider sets `preset = -1`.

**Persistence is a deliberate fix, not parity:** in Android these settings live only in memory
and are lost on restart. On Windows, persist `AudioSettings` in the settings store and restore it
at startup. Note the divergence in `PORT-DIFFERENCES.md`.

---

## 12. Lyrics engine (port exactly — this is fully portable)

Data types: `LyricLine(timeMs, text)`, `LyricsResult(lines, plainText, source)` where `source ∈
{"embedded", "lrc-file", "lyrics-cache", "online", "manual", "none"}`.

HTTP: OkHttp with `callTimeout 15 s`, `connectTimeout 10 s`, `readTimeout 15 s`.
Size caps: `MAX_READ_BYTES = 400_000`, `MAX_LRC_FILE_BYTES = 300_000`, `MAX_ID3_SCAN_BYTES = 300_000`.

### 12.1 Source priority (automatic path)

1. **App cache** → `source = "lyrics-cache"`
2. **Embedded ID3v2 USLT** → `source = "embedded"`
3. **Sidecar `.lrc`** next to the audio file → `source = "lrc-file"`
4. otherwise `LyricsResult(emptyList(), "", "none")`

On Windows, step 3 is strictly more capable than on Android (Android cannot read a sibling file
for `content://` URIs under scoped storage). Keep the same order and the same `source` labels.

Sidecar path: same base name, `.lrc` extension, in the same directory.

### 12.2 LRC parsing

```kotlin
Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
```

- seconds must be `0..59`; otherwise skip the whole match
- fraction: empty → 0; length 2 → ×10 (centiseconds); length 3 → milliseconds; otherwise pad to 3 and take 3
- skip lines with no timestamp, blank text, or starting with `ti:` / `ar:`
- emit **one `LyricLine` per timestamp** found on a line
- return lines sorted by `timeMs`

### 12.3 Embedded USLT extraction

- Local: stream the file, bounded to the first **400 KB**
- Remote: HTTP `Range: bytes=0-300000`
- Require `size > 3` and a literal `"ID3"` header at offset 0 — **never** blind-search for the tag
- decode the tag body as **ISO-8859-1** (that is how ID3 text frames with a Latin-1 encoding byte work)
- find `"USLT"`, take a 20 000-char chunk, strip control chars
  `[\x00-\x08\x0B\x0C\x0E-\x1F]`, remove the language tag when it matches
  `(eng|fas|per|ara|fre|ger|spa|tur|und)`, truncate to 12 000 chars
- if the chunk contains LRC timestamps → `parseLrc`; else split on 2+ spaces or newlines, keep
  fragments of length 2..200 that contain a letter, and assign `i * 4000 ms`

### 12.4 Online — LRCLIB (opt-in only)

- Endpoint: `GET https://lrclib.net/api/get?artist_name={enc}&track_name={enc}`, plus
  `&duration={sec}` when the duration is known
- Bail out with `"none"` if the title is blank, `"Stream"`, or `"Direct link"`
- non-2xx or empty body → `"none"`; any exception, including OOM → `"none"`
- prefer `syncedLyrics` → `parseLrc`; if that yields no lines, fall through to `plainLyrics`
- plain text → pseudo-timed lines: `step = durationSec * 1000 / nonBlankLineCount` clamped to
  **2000..8000 ms**, or **4000 ms** when the duration is unknown; line `i` gets `i * step`
- gate: `shouldFetchOnline(hasLocalLyrics, onlineEnabled) = !hasLocalLyrics && onlineEnabled`.
  **Online lyrics stay opt-in, default off, and must remain behind a visible switch.**
- a "Fetch lyrics" button bypasses the gate for the current track only

### 12.5 Caching

- Directory preference: `<appData>/lyrics` → `%LOCALAPPDATA%` equivalent → temp
- Filename: `"${songId}_" + sanitise("${artist}-${title}").take(80).ifBlank { "lyrics" } + ".lrc"`
- `sanitiseFileName` strips ``[\\/:*?"<>|]`` and control chars, trims, caps at 80 chars, and
  **preserves Unicode** (Persian titles must survive)
- Lookup: `name.startsWith("${songId}_") && name.endsWith(".lrc")`, checked in every candidate dir
- `cacheOnline` writes **only if no cache file exists yet**, and only if the text is
  `<= 300_000` bytes. Never silently overwrite a manual or sidecar lyric.

### 12.6 Manual entry and encoding

- `saveManual(song, raw)`: if the text already contains an LRC timestamp, store it verbatim;
  otherwise stamp each non-blank line `[MM:SS.00]` at `i * 4 s`
- Manual text is written to the same cache, so it loads as `"lyrics-cache"` next time
- Provide an explicit **Clear lyrics** action. Android has no such API (you must delete the
  file); on Windows, ship the button. Note as a difference.

`decodeText(bytes)` fallback chain — port exactly:
1. strip a UTF-8 BOM (`EF BB BF`)
2. strict UTF-8 decode with `CodingErrorAction.REPORT`
3. on failure → **`windows-1256`**

The Windows port can now also try the active ANSI code page first for legacy Persian `.lrc`
files, but **`windows-1256` must remain the final fallback** so files that work today keep working.

### 12.7 Lyrics UI

- `LyricsSheet`: title `"Lyrics • <source>"`, a "Fetch lyrics" button, a progress indicator while
  loading, the online-lyrics switch **with its description**, then either a "no lyrics" state or a
  `LazyColumn` with `heightIn(max = 340.dp)`, 8 dp spacing. Active line: `pal.ink`, bold, 16.sp,
  centred, 1 line, infinite marquee at 16 dp spacing. Inactive: `pal.sub @ 0.6`, 14.sp.
  Manual entry field + Save (clears the field after saving).
- `LyricsPreviewCard` in the queue sheet: header + open-in-new icon; active line in a marquee at
  15.sp bold, plus the next line as a 1-line 12.sp preview in `pal.sub`.
- Active line is found by **binary search** on `timeMs` inside a `derivedStateOf` that observes
  **only** `state.positionMs` — so the sheet does not recompose at 2 Hz. Port that optimisation.
- **Neither lyrics surface auto-scrolls on Android.** Decide explicitly whether to keep that or to
  add smooth scroll-to-line; if you add it, put it in `PORT-DIFFERENCES.md` and behind a setting.

---

## 13. Library scanning on Windows

Android scans MediaStore with `IS_MUSIC != 0 AND DURATION > 30000`, sorted `DATE_ADDED DESC`,
capped at 8000 rows, skipping `duration <= 0`, defaulting blank title to `"Unknown"` and blank
artist to `"Unknown Artist"`, and **rethrowing `SecurityException`** so the UI can prompt.

Windows has no system media index with comparable semantics, so implement a folder model:

1. **Roots**: the user picks one or more music folders (persisted). Ship sensible defaults from
   the Known Folder API (`Music`, plus any folder containing files they imported).
2. **Scan**: recursive walk, matched against `SUPPORTED_EXTENSIONS` (case-insensitive), skipping
   hidden/system files and the app's own directories, with a hard cap (keep 8000) and cancellation.
3. **Metadata**: jaudiotager reads tags. Fall back to the filename for a missing title
   (`"01 - Artist - Title.mp3"` → title/artist heuristics), `Unknown` / `Unknown Artist` otherwise.
4. **Duration filter**: Android's `DURATION > 30000` cannot be applied without opening every file.
   Default behaviour: keep files whose *tag* duration is `> 30 s` when cheaply available, keep
   everything else, and expose a user setting **"Hide clips shorter than 30 seconds"** that
   measures on demand. Document this in `PORT-DIFFERENCES.md`; it is a genuine behavioural
   difference forced by the platform.
5. **Sorting**: default to newest-first by file last-write time, matching `DATE_ADDED DESC`.
6. **Watching**: `ReadDirectoryChangesW` (or `WatchService`) for incremental updates, debounced
   ~2 s, so the library stays live without a manual rescan. Manual rescan button always available.
7. **First-run demo**: if the first scan finds nothing, offer the same 4-track demo playlist that
   Android ships (same URLs, same `Dreamy Night` metadata).
8. **Drag & drop**: dropping files or folders adds them (and persists new folder roots).

---

## 14. Data layer

### 14.1 Schema (port the Room entities 1:1; verify field names against
`app/schemas/com.alvand.player.data.local.AlvandDatabase/1.json`)

- `playlists`: `id INTEGER PK AUTOINCREMENT`, `name TEXT NOT NULL`,
  `created_at INTEGER NOT NULL DEFAULT (now)`
- `playlist_songs`: `id INTEGER PK AUTOINCREMENT`,
  `playlist_id INTEGER NOT NULL REFERENCES playlists(id) ON DELETE CASCADE`,
  `song_id INTEGER NOT NULL`, `uri TEXT NOT NULL`, `title TEXT NOT NULL`, `artist TEXT NOT NULL`,
  `album TEXT NOT NULL DEFAULT ''`, `duration_ms INTEGER NOT NULL DEFAULT 0`,
  `is_remote INTEGER NOT NULL DEFAULT 0`, `position INTEGER NOT NULL DEFAULT 0`,
  `added_at INTEGER NOT NULL DEFAULT (now)`;
  indices on `playlist_id` and `song_id`
- `play_history`: `id INTEGER PK AUTOINCREMENT`, `song_id INTEGER NOT NULL`, `title TEXT NOT NULL`,
  `artist TEXT NOT NULL`, `played_at INTEGER NOT NULL DEFAULT (now)`,
  `play_count INTEGER NOT NULL DEFAULT 1`; indices on `song_id` and `played_at`

Keep the deliberate design choice: `playlist_songs` **caches** uri/title/artist/album/duration so a
playlist survives the underlying file being deleted.

### 14.2 Query semantics (port every one)

- `observePlaylists` / `getPlaylists`: `ORDER BY created_at DESC`
- `observePlaylistSongs`: `ORDER BY position ASC, added_at ASC`
- `countInPlaylist`: `SELECT COUNT(*)` — used for de-duplication
- `nextPosition`: `SELECT COALESCE(MAX(position), -1) + 1` — order is **append-only**
- `observeRecent(limit = 50)`: `ORDER BY played_at DESC LIMIT ?`
- `historyFor(songId)`: `LIMIT 1`
- `upsertHistory`: replace-by-row-id while preserving `play_count = previous + 1` and
  `played_at = now`
- `pruneHistory(before)`: delete older than 90 days
- `clearHistory`

Playlist operations: `createPlaylist` trims the name, caps it at **60 chars**, and rejects blank;
`renamePlaylist` trims and caps at 60; `deletePlaylist` cascades; `addToPlaylist` returns `false`
if the song is already present. (Android has **no** reorder/move DAO method and does not dedupe
names — decide whether to add reordering on Windows; if you do, it is a difference, not parity.)

### 14.3 Migration policy

Android is at schema version 1 with `exportSchema = true` and **no migrations**. On Windows:

- Write **versioned, forward-only SQL migrations** from day one. Ship `001_initial.sql`.
- Every migration must be applied inside a transaction and verified by a test that migrates an
  older database file forward.
- Never use destructive fallback. A corrupt settings file resets to defaults (that is exactly what
  Android's `ReplaceFileCorruptionHandler { emptyPreferences() }` does) — a corrupt **database**
  must surface an error, not silently wipe the user's playlists.

### 14.4 Settings store (1:1 with `SettingsRepo`)

| Android DataStore key | Type | Default | Clamp | Meaning |
|---|---|---|---|---|
| `theme_mode` | Int | `0` | `0..2` | system / light / dark |
| `accent_theme` | Int | `0` | `0..3` | mono / royal / sunset / ocean |
| `background_uri` | String? | absent | — | custom background image path |
| `onboarding_seen` | Boolean | `false` | — | welcome screen gate |
| `online_lyrics_enabled` | Boolean | `false` | — | LRCLIB opt-in |
| `liked_ids` | String set | empty | — | favourites (store ids as strings) |
| `library_sort` | Int | `0` | `0..4` | see §14.5 |

Store the settings file in `%APPDATA%\Alvand\settings.json` (or an equivalent single file) and
**write it atomically** (temp file + rename) so a crash mid-write cannot corrupt it. Ship the
corruption-recovery behaviour from §14.3.

### 14.5 Library sort / filter / grouping (port verbatim, these are pure functions)

`LibrarySort`: `DEFAULT=0` (scan order), `TITLE_AZ=1`, `ARTIST_AZ=2`, `DURATION_DESC=3`,
`DURATION_ASC=4`.

`filterAndSort(songs, query, sortMode, liked, favouritesOnly)` applies, in this exact order:
1. **favourites filter** — keep only `id in liked`
2. **search** — lowercase substring against title **OR** artist **OR** album
3. **sort** — title (lowercase, then artist lowercase); artist (lowercase, then title lowercase);
   duration descending; duration ascending; otherwise preserve input order

`groupByAlbum`: key = album trimmed or `"Unknown Album"`; artist = the artist with the most songs
in the group; artwork representative = the first song; groups sorted by lowercase name.
`groupByArtist`: key = artist trimmed or `"Unknown Artist"`; album count = distinct albums;
sorted by lowercase name.

---

## 15. Windows system integration checklist

Every item is a parity requirement unless marked otherwise.

- [ ] **Single instance**: named mutex; a second launch forwards its arguments to the running
      instance and activates that window.
- [ ] **SMTC**: title, artist, album, artwork, and play/pause/next/previous/seek/shuffle/repeat
      all work from the OS (volume flyout, lock screen, Bluetooth/headset buttons, keyboard media
      keys). Also expose the **position and duration** so the OS scrubber works.
- [ ] **Global media keys** independent of focus.
- [ ] **Tray icon**: show/hide, play/pause, next, previous, quit. Tooltip shows the track.
- [ ] **Close-to-tray** with a user setting for "minimise to tray on close"; **playback must never
      stop when the window is closed** (parity with the Android foreground service).
- [ ] **Window state persistence**: size, position, maximised state.
- [ ] **File associations** for `SUPPORTED_EXTENSIONS` + `m3u8` + `mpd` via WiX, and
      **"Open with Alvand Player"** plus **"Play in Alvand Player"** context-menu entries.
- [ ] **Protocol handler** `alvand://` (e.g. `alvand://play?url=…` for direct links).
- [ ] **Drag & drop** of files and folders onto the window.
- [ ] **"Show in Explorer"** for the current track (if it is a local file).
- [ ] **Start with Windows** (HKCU `Run`), off by default.
- [ ] **High-DPI**: correct behaviour at 125/150/175/200% scaling, including the glass blur
      (Windows blurs natively — no API-level gating needed).
- [ ] **Multiple monitors** and DPI changes at runtime.
- [ ] **Theme integration**: follow the OS light/dark preference, and also respond to the
      `AppsUseLightTheme` registry value. Optional: accent colour from the OS (`SystemUsesLightTheme`
      / `HKCU\Software\Microsoft\Windows\DWM`), offered as a 5th "System" accent theme.
- [ ] **Taskbar progress** and jump-list entries are nice-to-haves; list them as proposals.

---

## 16. Internationalisation and RTL

- Ship **exactly** `en` and `fa`. Do not add locales. Android deliberately restricted the shipped
  locales to these two.
- 128 strings in the Android build; port the full set and keep the same string **names** so
  translators and the diff history stay meaningful.
- In-app language switch, applied without a restart (`en` ⇄ `فارسی`), persisted separately from
  the OS locale. `normalize(code)`: lowercase, take the base before `-`/`_`, and fall back to
  `en` if unsupported. `currentTag()`: read the current list and return the first supported base
  tag, else `en`.
- **Full RTL for `fa`**: mirrored layout direction, right-to-left text flow, mirrored icons where
  semantically directional (open/share/chevrons), Persian digits optional, correct
  `start`/`end` padding everywhere. Explicitly **LTR-locked**: the transport controls, the scrub
  arc, the time labels, the sleep-timer chip, and any URL/TON address.
- **Bidirectional safety**: never break an LTR run (URL, TON address, file path) inside an RTL
  paragraph — use Unicode isolates. The Android About screen does this with U+2066/U+2069; port it.
- Format `frozenDateTime`-free: no date/time pickers are in scope.

---

## 17. Crash reporting (local only)

Port `CrashLog` faithfully:
- install a default uncaught-exception handler that chains to the previous one, then terminates
- write `crashes/crash-<epochMillis>.txt` next to the app data
- header: app version, package/build, OS build, device, thread name
- include the stack trace and up to **3** levels of `Caused by`
- cap the file at **60 000** characters
- keep at most **5** crash files, pruning by last-modified
- track a "seen" timestamp in a small preferences store; `unseenCrash()` compares
  `lastModified > seen`
- UI: an About-screen "Report a problem" card with **Copy**, **Share** (opens the mail composer
  with the report attached as text — user-initiated only) and **Delete**, plus a first-launch
  dialog for an unread crash showing headline, time, and the first **8 000** characters in a
  selectable monospace block

**Never** upload anything. State this in the privacy section of the README.

---

## 18. Quality, performance and tooling

- **State flow cadence**: Android pushes full player state every **500 ms** (2 Hz) from a single
  coroutine on the main dispatcher. Port it for fidelity, then measure. If a spot-check shows
  visible stutter in the scrub arc, raise the cadence to 4 Hz and drive only the arc from a
  dedicated `derivedStateOf` — never by pushing the whole state faster.
- **Cold start to first frame**: < 900 ms on a mid-range laptop.
- **Scrub latency**: the arc must track the pointer with no visible lag. Never read disk, decode
  art, or run the DB from the UI thread; never `delay` the position pump synchronously.
- **Memory**: artwork cache is byte-bounded at `min(maxMemory / 8, 32 MB)`; a separate colour
  cache holds 120 entries; add a negative cache with a 10-minute TTL so a missing cover is not
  re-decoded on every scroll.
- **Library with 8 000 tracks**: the queue list must stay at 60 fps while scrolling, and a full
  rescan must not block the UI. Virtualise the list; batch DB writes in one transaction.
- **Threading discipline**: engine work off the UI thread, `StateFlow` for state, structured
  concurrency, no `GlobalScope`, cancellation wired to component disposal.
- **No blocking calls on the UI thread** — this is a hard requirement and the single most common
  defect in desktop ports.
- **Stability**: no crash and no memory growth over a 4-hour continuous-playback soak.

---

## 19. Security

- No network activity except the opt-in LRCLIB request, over TLS, with certificate validation on.
- Reject `http://` direct links (§9).
- Parse untrusted input defensively: tag fields, `.lrc` text, ID3 frames, and playlist names are
  all attacker-controlled. Enforce the byte caps in §12 and cap the line count of a parsed LRC.
- No code execution from metadata, no shell-outs with interpolated filenames, no path traversal
  when building cache filenames (`sanitiseFileName` is the guard — test it with `../` and `CON`
  style inputs).
- Do not log lyrics, file paths, or URLs at info level; keep debug logging behind a flag.
- No secrets in the binary, no analytics, no crash upload.

---

## 20. Toolchain

| Component | Version | Notes |
|---|---|---|
| Kotlin | 2.0.21 | match the Android build |
| Gradle | 8.9 or newer | version catalog, like the Android repo |
| JDK | 17 | Temurin 17 in CI |
| Compose Multiplatform | current stable | Material 3 |
| OkHttp | 4.12.0 | same as Android |
| Coroutines | 1.9.x | |
| jaudiotagger | current | pure JVM |
| Coil | 2.7 or Coil 3 | |
| Test stack | JUnit 4.13, kotlin-test, turbine, mockk | mirror the Android test deps |
| Packaging | WiX v4 | MSI + bootstrapper + portable ZIP |
| CI | GitHub Actions, `windows-latest` | run on `windows-latest`, cache Gradle, upload the ZIP |

CI gates, mirroring the Android workflow's intent:
`build → unit tests (blocking) → lint/detekt (blocking) → package (MSI + ZIP) → checksum`.
The signed build must fail closed if the signing certificate or its expected SHA-256 thumbprint
is missing — port the Android fail-closed release gate, do not ship a silently unsigned build.

---

## 21. Testing requirements

Port every existing Android test and keep it green in the `shared` module. Android has 6 test
classes / 33 passing tests; the port must have **at least** equivalent coverage plus the new
Windows-specific behaviour.

| Android test | Port coverage |
|---|---|
| `SongTest` | all 21 extensions, extension-less rejection, `http://` rejection, `.m3u8`/`.mpd` acceptance, title derivation and decoding, `_`→space, blank fallback, ID-space disjointness |
| `LyricsTest` | LRC regex (1/2/3-digit fractions, invalid seconds, `ti:`/`ar:` skipping, multi-timestamp lines, sorting), USLT extraction and control-char stripping, plain-text pseudo-timing and clamping, cache naming/sanitisation, `decodeText` UTF-8 → windows-1256 fallback |
| `SleepTimerTest` | fade curve, `sqrt` perceptual shape, generation guard, volume restore, clamping, end-of-track |
| `AppLocaleTest` | `normalize`, unsupported → `en`, `currentTag` selection |
| `LibraryFilterTest` | ordering (favourites → search → sort), all 5 sort modes, `groupByAlbum`, `groupByArtist`, empty/null album and artist fallbacks |
| `AccentThemeTest` | id lookup and out-of-range fallback, one theme per mode, mono identity (no tinting), tint direction and per-field factors, light/dark accent divergence |

Add new tests for Windows-specific logic:
- folder scanner: extension filtering, hidden-file skipping, cancellation, the 8 000 cap
- song id stability across rescans
- settings store: atomic write, corruption recovery
- `DspSpec` → engine graph translation, including the limiter being engaged on boost
- path sanitisation against traversal and reserved device names
- `PlayerEngine` queue semantics: pending-queue-until-ready, previous-at-3 s boundary,
  next-at-end-does-nothing, seek clamping, live-stream non-seekability

Prefer pure, injectable, clock-driven units over UI tests. No test may require a display.

---

## 22. Documentation deliverables

- `README.md` — what it is, screenshots, feature parity table, build and run instructions,
  privacy statement ("no telemetry, no analytics, the only network request is an opt-in lyrics
  lookup")
- `BUILD.md` — prerequisites, toolchain, Gradle commands, packaging
- `PUBLISH.md` — signing (WiX/Authenticode), versioning, checksum generation, GitHub Release flow
- `PORT-DIFFERENCES.md` — **mandatory**: every place the Windows build differs from Android, with
  the reason. Expected entries at minimum: library indexing, EQ band count and presets, the
  loudness limiter, EQ persistence, drag & drop, the unwired playlist/search/sort UI, clear
  lyrics, crash-report file location, and the song-id scheme.
- `CHANGELOG.md` — Keep a Changelog format, matching the Android project's style
- Update the Android `README.md` feature table to point at the Windows build

---

## 23. Suggested milestones

Deliver in this order; each milestone must build, pass tests, and be demoable.

1. **Skeleton + tokens** — Gradle project, window, `AlvandPalette`/`AccentTheme`, theme switching,
   tray icon, single instance. Nothing else.
2. **Library** — folder picker, scanner, tag reading, database, sort/search/filter/grouping,
   library UI.
3. **Playback core** — `PlayerEngine` + one real implementation, queue semantics, transport
   controls, SMTC, media keys, error/skip policy.
4. **Player screen** — full layout, `CoverHalo`, `ProgressArc` with the three-mode scrub,
   `MiniBars`, `BottomArcHandle`, background composition, `AmbientBlob`.
5. **DSP** — equalizer, presets, bass, boost + limiter, reverb, noise reduction, settings
   persistence, EQ sheet.
6. **Lyrics** — LRC, USLT, sidecar, cache, LRCLIB opt-in, manual entry, both lyrics surfaces.
7. **Sleep timer** — engine, fade, presets, end-of-track, chip and dialog.
8. **System integration & polish** — file associations, protocol handler, context menu,
   drag & drop, Start with Windows, DPI, localisation and full RTL.
9. **Packaging & release** — WiX installer, portable ZIP, code signing, CI, checksums, docs.

---

## 24. Definition of done

- [ ] Builds from a clean clone on `windows-latest` with one documented command
- [ ] All ported + new tests pass; lint/analysis clean
- [ ] Every row of the §6 parity matrix is implemented, or explicitly listed in
      `PORT-DIFFERENCES.md` with a justification
- [ ] The design system matches §7 token-for-token
- [ ] The scrub arc matches §8.3, including all three touch modes
- [ ] Sleep-timer fade and lyrics pipeline are algorithm-identical to Android
- [ ] No network traffic other than opt-in LRCLIB; verified with a packet capture or proxy log
- [ ] No UI-thread blocking; 8 000-track library scrolls at 60 fps
- [ ] 4-hour playback soak: no crash, no leak
- [ ] Persian UI is fully RTL with LTR-locked controls, verified by screenshots
- [ ] Installer and portable ZIP both build; the signed artifact verifies with `signtool verify /pa`
- [ ] `README.md`, `BUILD.md`, `PUBLISH.md`, `PORT-DIFFERENCES.md`, `CHANGELOG.md` complete

---

## 25. Reporting

Work in visible increments. After each milestone, report: what was built, what was verified and
how (command output, not assertion), what is stubbed, and what is knowingly incomplete. When you
hit a genuine ambiguity in the Android source, **read the code and decide**, then record the
decision in `PORT-DIFFERENCES.md` — do not stop and ask unless the decision would change
user-visible behaviour in a way Android does not already answer.

Start now: read the Android source listed in §1, then produce milestone 1.

=== PROMPT END ===

---

## Notes for the requester

- The prompt is deliberately written in **English** because it is meant to be handed to a coding
  agent, and every identifier it references matches the English identifiers in the source.
- Every hard number in it (hex values, fade curve, LRC regex, EQ tables, schema, preference keys,
  scrub geometry) was read out of the shipped v1.6.9 source, not invented.
- It instructs the agent to treat the Android repository as the source of truth and to log every
  deliberate divergence in `PORT-DIFFERENCES.md` — which is how you keep "professional" honest
  rather than aspirational.
