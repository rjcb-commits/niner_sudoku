# Niner — Play Store listing copy

All text below is ready to paste into the Play Console's "Main store listing" form. Character limits shown are the Play Store maximums.

---

## App name (30 chars)

```
Niner · the daily sudoku
```

(24 chars — leaves room for a localized variant later.)

Shorter fallback if needed:
```
Niner
```

---

## Short description (80 chars)

```
A clean, calm sudoku. One daily puzzle, five modes, zero ads, zero tracking.
```

(77 chars)

Alternate options:
- `Beautiful sudoku, made for focus. Daily puzzle, 5 modes, no ads, no tracking.` (78)
- `The daily sudoku, done right. 5 modes, 5 difficulties, offline, zero ads.` (74)

---

## Full description (4000 chars)

```
Niner is a beautiful, calm sudoku for people who love the puzzle — not the ads, pop-ups, and telemetry that clutter so many other apps.

Open the app. Solve today's puzzle. Close the app. That's the loop.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
WHAT'S INSIDE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

◆ One daily puzzle — same one for everyone, every day
◆ Five difficulties — Beginner, Easy, Medium, Hard, Expert
◆ Five modes — Classic, Strict, Coach, Speed, Killer
◆ 16 achievements, from your first win to "Cage Master"
◆ Daily-streak tracking with a 12-week activity heatmap
◆ Best-time tracking per difficulty + per mode
◆ Five colour themes, each with a hand-tuned dark variant
◆ Six celebration styles, including one minimal option for people who don't want confetti

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
GAME MODES
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Classic — the standard experience. Three mistakes, a handful of hints, all the time in the world.

Strict — one mistake and it's over. For players who want their accuracy to actually matter.

Coach — unlimited hints and mistakes. For learning, or for kids who are just starting out.

Speed — race yourself. Your time is the score, and every second counts.

Killer — cages with target sums instead of some given numbers. A fresh twist on classic sudoku for when you want something new.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
DESIGNED FOR FOCUS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

◆ No ads. None. Not even a "remove ads" upsell.
◆ No account. No login. No email address required.
◆ No internet permission. Niner literally cannot phone home.
◆ No third-party SDKs — no analytics, no trackers, no crash reporters.
◆ No dark patterns — no push notifications nagging you to come back.

Your progress lives on your device, in one tidy SharedPreferences file. Uninstall the app and it's gone — no cloud, no lingering data.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
THOUGHTFUL TOUCHES
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

◆ Wrong entries don't get stuck on the board — they shake and vanish. Your board always reflects what you know.
◆ Peer and same-digit highlighting make hard puzzles manageable without making easy ones boring.
◆ Colour-blind palette option for hints and errors.
◆ Large-text toggle for easier reading.
◆ Centered-notes toggle if you prefer a single-row pencil-mark style.
◆ The daily puzzle is deterministic — if you compare notes with a friend, they had the same puzzle you did.
◆ Adaptive launcher icon, proper dark mode, tasteful haptics.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
FOR EVERY LEVEL
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

New to sudoku? Start on Beginner with Coach mode — unlimited hints and mistakes, with explanations. Work up from there.

Returning player? Skip straight to Expert or try Speed mode to see how fast you can go.

Just want a daily ritual? The daily puzzle is a fixed Medium difficulty — one puzzle, a few minutes, no decisions.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
BUILT WITH CARE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Kotlin, Jetpack Compose, Material 3. Puzzle generator runs on-device with a proper uniqueness check, so every puzzle has exactly one solution. No pre-bundled puzzle pack.

Made by one developer, for people who love sudoku.
```

Rough length: ~2,950 chars. Under the 4,000 limit with room to breathe.

---

## What's new (500 chars — first release)

```
Niner 1.0 — the launch release.

• One shared daily puzzle
• 5 difficulties × 5 modes
• 16 achievements
• 5 colour themes with dark variants
• Colour-blind, large-text, and centered-notes accessibility options
• 100% offline — no ads, no accounts, no tracking

Thanks for trying the app. If you find a bug, email rayjack.cb@gmail.com.
```

(~380 chars)

---

## Categorisation

- **Category:** Games → Puzzle
- **Tags (choose up to 5 from Play's fixed list):** Puzzle, Brain, Casual, Offline, Logic
- **Content rating:** Everyone (no violence, no gambling, no user content)
- **Target audience:** Ages 13+ (safe for younger, but 13+ avoids COPPA paperwork)
- **Contains ads:** No
- **In-app purchases:** No
- **Data safety form:** "No data collected, no data shared" (matches `playstore/PRIVACY_POLICY.md`)
- **Privacy policy URL:** https://niner-privacy.pages.dev/privacy (live on Cloudflare Pages — deploy from `docs/` via `wrangler pages deploy docs --project-name=niner-privacy`).

---

## Graphic assets (specs)

| Asset                 | Size          | File/Path                                    | Notes |
|-----------------------|---------------|----------------------------------------------|-------|
| App icon              | 512×512 PNG   | derive from `app/src/main/res/mipmap-*/ic_launcher.png` or from `niner_hero.png` on a #00103D background | Required. No alpha — Play Store shows on varied backgrounds. |
| Feature graphic       | 1024×500 PNG  | create new: hero on left, tagline on right, gradient background | Required. Displayed above the install button on mobile. |
| Phone screenshots     | 1080×1920+ PNG, 2–8 images | `playstore/screenshots/*.png`              | Minimum 2, recommended 8. Portrait. |
| 7-inch tablet shots   | (optional)    | —                                            | Skip unless you specifically target tablets. |
| 10-inch tablet shots  | (optional)    | —                                            | Skip. |
| Promo video           | YouTube URL   | —                                            | Skip for v1. |

### Feature graphic layout suggestion

Landscape 1024×500. Left third: 360dp-tall Niner hero badge. Middle/right: app name in displayLarge weight (#FFFFFF), tagline "the daily sudoku" in titleMedium (#B8C4E0), optional mini 3×3 grid illustration in the bottom-right. Background: same #00103D as the launcher icon, with a subtle radial gradient toward the right.

### Screenshot hero-text overlay (optional but recommended)

If you add hero text overlays to the screenshots (Play Store allows this and they convert better):

- Screenshot 1: **"One puzzle. Every day."** — main menu with daily card
- Screenshot 2: **"Five modes, your pace."** — mode picker open
- Screenshot 3: **"Beautiful, calm sudoku."** — game screen mid-solve
- Screenshot 4: **"Make every win count."** — celebration screen
- Screenshot 5: **"Track what matters."** — stats screen with heatmap
- Screenshot 6: **"16 achievements to chase."** — achievements grid
- Screenshot 7: **"Five hand-tuned themes."** — settings/theme picker
- Screenshot 8: **"Private by design."** — about screen with "no account needed" copy

Text overlay style: Inter/SF Bold 72pt, white (#FFFFFF) on screenshots with dark bottom third; dark (#00103D) on screenshots with light bottom third. Leave ~180dp padding from the top for the status bar region.

---

## Release checklist (before first publish)

- [ ] Generate keystore (see `playstore/SIGNING.md`)
- [ ] Build signed `.aab`: `./gradlew bundleRelease`
- [ ] Install and smoke-test the signed release locally before upload (R8 can introduce bugs the debug build won't show)
- [x] Privacy policy hosted at https://niner-privacy.pages.dev/privacy — paste this URL into Play Console's privacy policy field
- [ ] Capture 8 phone screenshots (see `playstore/screenshots/`), 1080×1920 or larger, portrait
- [ ] Generate 1024×500 feature graphic
- [ ] Generate 512×512 app icon PNG (no alpha, no rounded corners — Play Store masks it)
- [ ] Fill in Data Safety form as "No data collected"
- [ ] Fill in Content Rating questionnaire: pick "Everyone"
- [ ] Set pricing: Free
- [ ] Pick countries: All, or start with your own country for a soft launch
- [ ] Internal testing track first (send to yourself + 1-2 friends), THEN production

---

## Marketing lines (for social / ASO / blog posts)

- "Sudoku, made calm."
- "One daily puzzle. Five modes. Zero ads."
- "The sudoku app that respects your attention."
- "Offline by design. Private by default."
- "Built for focus — not engagement."
