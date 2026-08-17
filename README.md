# DopaMind

A gamified, lifestyle-tracking Android app for cannabis/CBD, tobacco/IQOS, alcohol,
libido, dopamine/focus, recovery, and personal finance — **not** a medical or
clinical app. Dark, Whoop/Oura-inspired design system. All data stays on-device,
encrypted, with no cloud backend for the MVP.

## Status

This is a from-scratch MVP scaffold: every module described in the brief is
wired end-to-end (Room entity → repository → ViewModel → Compose screen →
navigation) with real, working logic — not placeholders. See
[Known limitations](#known-limitations) below for the one thing that could not
be done in this environment: an actual Gradle build run.

## Architecture

```
app/src/main/java/com/dopamind/app/
├── core/
│   ├── theme/            Dark design system: colors, typography, shapes
│   ├── designsystem/      Reusable Compose components (DMCard, ProgressRing, DMChip, SimpleBarChart, …)
│   ├── navigation/        Type-safe Navigation Compose routes (@Serializable) + NavHost
│   ├── database/          Encrypted Room database (SQLCipher)
│   ├── security/          Keystore-backed passphrase generation for the DB
│   ├── backup/            Local encrypted export/import of the DB file
│   ├── notifications/     Daily check-in reminder + craving-pattern alert WorkManager jobs
│   ├── i18n/              Per-app language override (SYSTEM/IT/EN)
│   ├── di/                Hand-rolled DI container (AppContainer) + ViewModel factory
│   ├── analytics/         Cross-module correlation engine (see below)
│   ├── ai/                The 4 AI modules (see below)
│   └── gamification/      Badge catalog + BadgeEngine
├── feature/
│   ├── cannabis/          Strain log, mood tracking, edibles calculator, T-break timer
│   ├── tobacco/           Puff/stick counter, trigger log, smart spacer
│   ├── alcohol/           Drink counter, BAC estimate (Widmark), can-I-drive test, hangover risk
│   ├── libido/             Solo/partner log, dopamine reset, monk-mode streak
│   ├── dopaminefocus/     Dopamine Debt display, health stacking, detox mode, why-prompt
│   ├── recovery/          Sleep log, Chill Coach SOS, munchies/junk-food tracker
│   ├── finance/           €/unit convenience calc, budget tracker, spend log
│   ├── dailyvibe/         The Daily Vibe Check-in (swipeable 3-card flow)
│   ├── weeklyrecap/       Weekly Recap + Annual Wrapped (Spotify-Wrapped style)
│   ├── dashboard/         Home screen (7 module cards)
│   ├── profile/           Onboarding, Splash routing, Profile/Settings screen
│   ├── badges/            Full badge gallery (locked/unlocked)
│   ├── history/           30-day trend charts per module
│   ├── nutrition/         Food diary + calorie/macro goals, water, weight, intermittent fasting
│   ├── voicelog/          Voice-to-Log hands-free entry point
│   └── scanner/           Shared camera screen for label/food scanning
└── res/values(-it)/strings.xml   All UI copy, English base + Italian
```

### What's new since the first MVP commit

- **Onboarding + Profile**: first launch collects a name/weight/sex/language/
  notification preference (all optional, all local); a `Splash` route decides
  whether to show onboarding or go straight to the dashboard. The saved
  weight/sex now feed the BAC and edibles calculators as defaults instead of
  hardcoded numbers, and a Profile screen (gear icon, top-right of the
  dashboard) lets you edit all of it later.
- **Notifications**: two WorkManager jobs — a daily reminder if you haven't
  checked in yet, and an hourly check against the Craving Prediction Engine
  that only fires if a predicted peak (from your own history) is coming up
  within the next hour. Both respect the notification toggle in onboarding/
  profile and Android 13+'s runtime permission.
- **Badge gallery**: every badge, locked or unlocked, with its unlock
  condition spelled out — reachable from the dashboard.
- **Module history**: a 30-day bar chart (Cannabis/Tobacco/Alcohol/Libido
  counts, Sleep hours) via a "View trend" button on each of those module
  screens, drawn with a small custom Canvas chart — no charting library
  dependency.
- **Local encrypted backup**: export/import the SQLCipher database file as-is
  from the Profile screen, via the system file picker. Important: this is a
  same-device backup only — the encryption key lives in this phone's hardware
  Keystore and never leaves it, so it can't restore onto a different phone or
  after an uninstall/reinstall. That's a deliberate, documented trade-off, not
  an oversight — see the in-app copy on the Profile screen.
- The Spot Map / saved-spots idea from the original brief was removed
  entirely from the Finance module per a later request, to keep it focused.
- **Nutrition & Fitness** (an 8th module, added on request — "bring in the
  Yazio-style features"): a food diary with per-meal calories/macros, a
  daily calorie/macro goal computed from the profile (Mifflin-St Jeor BMR ×
  activity level — same informational-only framing as the rest of the app's
  calculators, editable/overridable), water tracking, weight tracking with a
  30-day trend chart, and an intermittent-fasting timer (16:8-style, reusing
  the same start/target-hours/progress-ring pattern as the T-Break and Detox
  Mode timers elsewhere in the app). **No barcode scanner**: a real one needs
  a network call to an external food database (e.g. Open Food Facts), which
  would break the "no cloud" design that holds for every other module — food
  entry is manual-only by design, not a missing feature.
- **Goal-driven onboarding** (Yazio-style, added on request): onboarding now
  asks "what's your goal?" (lose / maintain / gain weight) right after the
  welcome step, with a target-weight follow-up when it's not "maintain". The
  answer feeds `NutritionGoalCalculator` directly — a ±500/+300 kcal/day
  deficit or surplus is applied on top of the Mifflin-St Jeor TDEE, floored
  at a 1200 kcal/day safety rail. Both the goal and target weight are
  editable anytime from the Profile screen, not just at onboarding.

Each `feature/<module>` package is split into `data` (Room entity/DAO/repository),
`domain` (pure Kotlin calculators — Widmark BAC, edibles dosing, €/unit convenience,
smart spacer, streaks — zero Android imports), and `ui` (ViewModel + Compose screen).
The `domain` layer and most of `core` (analytics, gamification catalog, AI parsers)
have no Android or Compose dependency, which is what makes a future Kotlin
Multiplatform port to iOS realistic: Room and the Compose UI are the two layers
that would need replacing, everything else travels as-is.

### core/analytics — the correlation engine

`CorrelationEngine` is the one place that reads across every module's
repository to build a day-by-day `DailyMetrics` picture. It feeds:

- **`DopamineDebtCalculator`** — a recency-weighted, playful "load score" across
  cannabis/tobacco/alcohol/libido/poor-sleep, paid down by completed detox sessions.
- **`HangoverRiskCalculator`** — BAC + hydration + food + sleep → a risk score
  and a set of actionable tips, used both retrospectively and live mid-session.
- **`SleepCorrelationAnalyzer`** — a real Pearson correlation between each
  module's daily usage and logged sleep quality.
- **`WeeklyRecapGenerator`** / **`AnnualWrappedGenerator`** — turn the above
  into the swipeable "Wrapped"-style cards.

### The 4 AI modules — honest, on-device scope

No cloud LLM, no network calls, nothing that needs an API key — everything
below runs locally on the phone:

1. **Pattern Recognition & Predictive Alerts** (`core/ai/craving`,
   `core/ai/hangover`) — real frequency analysis (hour-of-day / day-of-week
   clustering) over the user's own history, plus a live BAC/hydration
   recompute during a session. This is genuine, working heuristic pattern
   recognition — not a trained ML model, which would need a dataset and
   training infra out of MVP scope.
2. **Voice-to-Log** (`core/ai/voice`) — Android's on-device `SpeechRecognizer`
   captures audio locally, and a rule-based Italian/English keyword + number-word
   parser (`VoiceLogParser`) extracts structured log entries (e.g. *"due birre
   e una sigaretta"* → 2× alcohol, 1× tobacco). No audio or text leaves the device.
3. **Vision** (`core/ai/vision`) — ML Kit's on-device Text Recognition reads a
   drink label and regexes out ABV%/volume; ML Kit's on-device Image Labeling
   classifies a food photo against a junk-food keyword list for a "junk score".
4. **Chill Coach & SOS** (`core/ai/coach`) — a scripted, reviewed decision tree
   (not a free-form chat model — deliberately, for this use case) with a
   box-breathing guide and grounding exercise, lightly personalized by recent
   mood history.

## Design system

Dark-only, Whoop/Oura-inspired: `#0A0A0C`/`#121214` surfaces, a single accent
`#00D9A3`, 1px borders and no shadows, thin ring-based progress instead of flat
bars, line-style icons, emoji reserved for copy/microcopy (badges, recap cards,
check-in reactions) rather than UI chrome. See `core/theme/` and
`core/designsystem/`.

## Data & privacy

- Room database encrypted at rest with SQLCipher; the passphrase is generated
  once with `SecureRandom` and stored in `EncryptedSharedPreferences`, itself
  backed by a hardware Android Keystore key. Nothing is recoverable without
  the device.
- `allowBackup="false"` disables Android's auto-backup / device-transfer flows
  entirely — this data doesn't leave the device, full stop.
- No `INTERNET` permission is requested, because nothing in this app talks to
  the network.
- Camera and microphone permissions are requested only when the Vision Scanner
  or Voice Log screens are opened, and only used by on-device ML Kit /
  SpeechRecognizer — never uploaded anywhere.

## Building

```
./gradlew assembleDebug
```

Requires a local Android SDK (`local.properties` with `sdk.dir=...`, or the
`ANDROID_HOME` env var) and JDK 17+. minSdk 26, targetSdk/compileSdk 35.

### Build status

`./gradlew assembleDebug` has been run for real (JDK 17 + Android SDK
Platform 35 installed locally) and reaches **BUILD SUCCESSFUL**, producing
`app/build/outputs/apk/debug/app-debug.apk`. The codebase was originally
written in a sandboxed environment with no network access, so the first real
compile did surface a handful of small issues (a couple of bad imports, a
missing `dp` import, a stray import that shadowed `Modifier.weight()`, an
experimental-API opt-in) — all fixed. Only two harmless deprecation warnings
remain (`Icons.Outlined.ArrowBack`, `LocalLifecycleOwner`'s package move).

The real **Inter** variable font (SIL OFL 1.1, from the `google/fonts`
GitHub repo) is now bundled at `app/src/main/res/font/inter.ttf` and wired
per-weight via `FontVariation.Settings` in `core/theme/Type.kt` — no more
Roboto fallback.

(The Spot Map / saved-spots idea from the original brief has been dropped
entirely to keep the Finance module focused — see the module list above.)

### Before publishing to Google Play

Given the subject matter (drug/alcohol tracking), review Play's
[Restricted Content policy](https://support.google.com/googleplay/android-developer/answer/9878810)
and the Health Content policy before submitting — in particular, keep the
copy/tone clearly recreational-tracking and non-instructional (no dosing
"advice" framed as medical guidance, which this app already avoids by framing
every calculator as informational/harm-reduction, not clinical).

## Budget notes (~35€)

- Google Play one-time registration: ~23€
- No recurring API costs: the app is 100% on-device (no Maps billing, no LLM
  API, no cloud backend) — this was a deliberate architecture constraint, not
  just a cost-cutting afterthought, since it's also what makes the "no cloud
  for MVP" privacy promise real.
- A domain name is optional for an MVP with no web presence; skip it unless
  you want a marketing/privacy-policy page.
