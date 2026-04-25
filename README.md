# Niner — the daily sudoku

> A clean, calm sudoku game for Android. One daily puzzle, five modes, zero ads, zero tracking.

<p align="center">
  <img src="playstore/app_icon_512.png" width="160" alt="Niner app icon">
</p>

<p align="center">
  <img src="playstore/screenshots/01_main_menu.png" width="22%" alt="Main menu">
  <img src="playstore/screenshots/03_game.png" width="22%" alt="Game in progress">
  <img src="playstore/screenshots/04_stats.png" width="22%" alt="Statistics">
  <img src="playstore/screenshots/05_achievements.png" width="22%" alt="Achievements">
</p>

---

## Why Niner exists

Most sudoku apps in 2026 are a graveyard of full-screen interstitial ads, "remove ads for $4.99" upsells, push notifications nagging you to come back, and analytics SDKs reporting every cell tap to a third party.

Niner is what a sudoku app should be: a puzzle, a board, and the time it takes to solve it. **Nothing else.**

- **No ads.** Not even one. Not "remove ads" upsells.
- **No accounts.** No login, no email, no cloud.
- **No internet permission.** The `AndroidManifest.xml` declares zero `<uses-permission>` entries — Niner literally cannot phone home.
- **No third-party SDKs.** No analytics, no crash reporters, no ad networks, no telemetry of any kind.

Your puzzles, stats, and achievements live in a single SharedPreferences file on your device. Uninstall the app and they're gone — there's no server-side data because there's no server.

---

## Features

### Game

- **5 difficulties** — Beginner, Easy, Medium, Hard, Expert. Each has its own clue-count target and hint cap.
- **5 modes**
  - **Classic** — 3 mistakes, normal hint count
  - **Strict** — 1 mistake ends the game
  - **Coach** — unlimited hints + mistakes (great for learning)
  - **Speed** — countdown clock; every correct cell adds 4 seconds
  - **Killer** — cages with target sums layered on top of standard sudoku rules
- **Daily puzzle** — one shared puzzle for every player, every day, with a 12-week activity heatmap and streak tracking
- **Per-mode-per-difficulty best-time tracking** + average win time + win rate
- **16 achievements** with bundled badge artwork

### Polish

- **Five hand-tuned colour themes** × light/dark variants (Ember, Electric, Neon Night, Citrus Punch, Hyperdrive)
- **Six celebration styles** — Confetti, Fireworks, Bubbles, Cherry Blossoms, Emoji Rain, Minimal
- **Wrong entries don't stick to the board** — they shake and vanish, so the board always reflects what you know
- **Peer + same-digit highlighting** for quick visual scanning
- **Daily-streak heatmap** with empty-state preview grid
- **Animated mini-grid loader** during puzzle generation
- **Sequenced sound chimes** via Android's `ToneGenerator` (no bundled audio assets)
- **Loss-reason-aware celebration** — gave-up, mistake-limit, and Speed-timeout each get their own title/subtitle/emoji
- **Cage-aware hint engine in Killer** — the hint dialog explains in cage terms when cage constraints alone narrow a cell to one value

### Accessibility

- **Colour-blind palette** option (amber for hints, dark orange for errors, dot pattern in the heatmap)
- **Large-text toggle** for cell values + notes
- **Centered-notes** layout option for users who prefer single-row pencil marks
- **TalkBack semantic overlay** on the board canvas — every cell announces row, column, current value, and (in Killer) cage sum
- **Light + dark mode** controlled by system or pinned to either

---

## Tech stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose, Material 3 |
| Compose BOM | 2024.09.03 |
| Architecture | Single-activity, `AndroidViewModel` + `StateFlow` + `mutableStateOf` |
| Persistence | `SharedPreferences` + `org.json` (no Room, no DataStore) |
| Concurrency | Kotlin Coroutines |
| Build | AGP 8.7.0, Gradle KTS, R8 minification + resource shrinking |
| Min / Target SDK | 26 / 35 |

### Architecture overview

```
app/src/main/java/com/ninersudoku/
├── MainActivity.kt
├── achievements/      Achievement enum + AchievementManager (StateFlow + persistence)
├── daily/             DailyManager (deterministic per-date seed, streak tracking)
├── game/
│   ├── Board.kt       Immutable 9×9 board with conflict detection
│   ├── Cell.kt        Cell with value, given/hint flags, pencil notes
│   ├── Difficulty.kt  Per-level clue counts + hint caps
│   ├── GameMode.kt    Mode enum with display name + description
│   ├── Generator.kt   Classic puzzle generator with uniqueness check
│   ├── Solver.kt      Bitmask backtracking with MRV
│   ├── HintEngine.kt  Naked + hidden singles + cage-aware hints
│   └── Killer.kt      2- or 3-cell cage generator + Killer-aware solver
├── onboarding/        OnboardingManager (3-page first-run experience)
├── persistence/       SaveManager (in-progress game serialisation)
├── prefs/             DisplayPreferences (a11y + behaviour toggles)
├── sound/             SoundManager (ToneGenerator-based chimes)
├── stats/             StatsManager + per-mode best-time tracking
├── viewmodel/         GameViewModel — single source of state truth
└── ui/
    ├── BoardView.kt        Canvas-rendered grid + transparent semantics overlay for TalkBack
    ├── NumberPad.kt        Digit pad with remaining counts + tap/long-press
    ├── DifficultyScreen.kt Main menu (Daily, Continue, Mode picker, difficulty cards)
    ├── GameScreen.kt       Top bar + board + actions + pause + hint dialog + celebration
    ├── StatsScreen.kt      Hero card + summary + heatmap + per-difficulty cards + achievements
    ├── OnboardingScreen.kt 3-page intro
    ├── AboutScreen.kt
    ├── celebration/        Particle engine + style preview
    ├── components/         SettingsSheet (themes, modes, a11y)
    └── theme/              5 colour palettes × light + dark
```

### Notable design decisions

- **No external state library.** ViewModel + `StateFlow` + `mutableStateOf` is enough for a single-activity Compose app. Adding Hilt, Koin, or Room would have inflated the APK with no real benefit for this scope.
- **`org.json` over `kotlinx.serialization`.** The persistence surface is small (saved game, stats, achievements). `org.json` ships with Android and saves an entire serialisation plugin from being added to the build.
- **Wrong entries shake but don't stick.** A wrong digit increments the mistake counter and triggers a board shake, but the cell stays empty. Removes the awkward "I see a 7 here but I know it's wrong, do I leave it?" loop.
- **Killer cages are strictly 2 or 3 cells.** Single-cell cages would double as pre-filled givens and dilute the Killer identity. Larger cages slow the solve loop without adding interesting reasoning. Strict 2- or 3-cell cages keep deductions tractable. A topology-trapped orphan triggers a regen rather than a 4-cell exception.
- **Killer uses fewer starting clues than Classic** at the same difficulty (32 vs 50 at Beginner, 9 vs 24 at Expert) — cage sums carry part of the deduction load.
- **The Killer hint engine is cage-aware.** When the cage constraint alone narrows a cell to one digit, the hint dialog explains in cage terms ("This cage needs 14. With 9 already in, the last cell must be 5") instead of pretending it's a standard naked single.
- **"New puzzle" generates fresh, never restarts the same puzzle.** A "restart this puzzle" button would let players pre-scout, then run again to fake a best time or unlock Perfectionist/Flawless cheaply. The overflow menu always ships a different puzzle.
- **Process-death restore via `SharedPreferences`.** Every cell entry persists the full game state to a single key (`saved_game`). `am force-stop` + relaunch lands the player on the menu with a Continue card pointing at the exact board they left.
- **Adaptive launcher icon** with dedicated `mipmap-anydpi-v26` background colour matching the splash screen — no flicker on cold launch.

---

## Build

### Prerequisites

- Android Studio Ladybug or newer
- JDK 21 (Gradle requires it on AGP 8.7+)
- Android SDK with platform 35 + build-tools 35.0.0

### Debug build

```bash
export JAVA_HOME=$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Release build

You'll need your own keystore. Run once:

```bash
keytool -genkeypair -v \
  -keystore app/release.jks \
  -alias niner \
  -keyalg RSA -keysize 2048 \
  -validity 10000
```

Then add to `local.properties` (gitignored):

```properties
RELEASE_STORE_FILE=release.jks
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=niner
RELEASE_KEY_PASSWORD=...
```

Build:

```bash
./gradlew bundleRelease       # AAB for Play Store
./gradlew assembleRelease     # APK for sideloading
```

Without a keystore, the release build fails loudly with `SigningConfig "release" is missing required property "storeFile"` — that's intentional, it stops accidentally shipping unsigned artefacts.

---

## Privacy

The full privacy policy is hosted at **https://niner-privacy.pages.dev/privacy**.

The HTML source lives in [`docs/privacy.html`](docs/privacy.html). It's deployed to Cloudflare Pages with:

```bash
wrangler pages deploy docs --project-name=niner-privacy --branch=main
```

Short version: zero data collection, zero permissions, zero network calls. Everything stays on the device. Uninstalling the app removes all of it.

---

## Roadmap

Things deferred for v1.x:

- Smarter hint techniques (naked pair, X-Wing) for Hard / Expert
- Per-mode-per-difficulty win counts (the "Cage Master" achievement currently uses "won Killer at all 5 difficulties" as a proxy for "won 10 Killer puzzles")
- Tablet-optimised layout — the phone layout works on tablets but doesn't take advantage of the extra width
- Localisation — strings are inline; needs extraction to `strings.xml` first

---

## License

[MIT](LICENSE) — feel free to fork, learn from, or remix.

---

## Credits

Built solo by **Rayjack CB**. Made for people who love sudoku.
