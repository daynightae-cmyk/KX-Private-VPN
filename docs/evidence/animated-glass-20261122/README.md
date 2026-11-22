# Animated glass UI — reference identity verification (2026-11-22)

## What was supplied

The owner supplied files named `ChatGPT Image Nov 22, 2026, 03_*.png` in
`C:\Users\day night\Downloads` (20 files, two download batches at 03:37–03:38 and
03:40–03:41) as the animated-glassmorphism splendor baseline.

## Deduplication result (executed, SHA-256)

`Get-FileHash -Algorithm SHA256` over all 20 files yields **10 unique images**; each
appears exactly twice (the two batches are byte-identical). Every unique hash matches
the canonical approved aistudio references already recorded in
`docs/evidence/reference-sha256.json`:

| Downloads file (first batch) | SHA-256 prefix | Canonical reference |
|---|---|---|
| 03_37_42 PM-1.png | `464a7c4f7ff3` | 01-welcome |
| 03_37_44 PM-2.png | `825c75b082fd` | 02-home-disconnected |
| 03_37_47 PM-3.png | `8d2c742a10c9` | 03-server-selection |
| 03_37_49 PM-4.png | `f0cfe658e5f6` | 04-connected-session |
| 03_37_51 PM-5.png | `d0a1f09af2dd` | 05-security-center |
| 03_37_53 PM-6.png | `c9265fd7be8a` | 06-split-tunneling |
| 03_38_02 PM-7.png | `5b3214f408bd` | 07-advanced-network |
| 03_38_06 PM-8.png | `a6c474b99a3c` | 08-statistics |
| 03_38_12 PM-9.png | `670a2aea5230` | 09-import-configuration |
| 03_38_14 PM-10.png | `750319564b59` | 10-profile-settings |

**Conclusion:** the approved visual identity is unchanged. The task is to bring the
existing native screens up to the animated glass splendor of these same canonical
screens — not to adopt a new composition. Original bytes were never modified.

## Implementation (branch `codex/animated-glass-ui-20261122`, PR #55)

- `designsystem/KxMotion.kt` (new): one shared ambient clock (`KxAmbient` /
  `LocalKxAmbient`) with five `State<Float>` channels read inside draw/graphicsLayer
  phases (redraw-only animation, no 60fps recomposition); `KxAmbient.Still` for
  reduced motion; `kxEntrance` staggered rise+fade; `kxSheen`, `kxBreathe`,
  `kxLivePulse`, `kxFloat`, `cubicPoint`.
- Ambient wired at app root (`KxLaunch`), `IdentityExperience`, `KxReferenceTheme`,
  `KxGlassTheme`; atmospheres, glass panels, CTAs, status pills, title underlines and
  the KX orb idle life all animate.
- Entrance stagger added to every feature screen (Home, Tools, Security, Servers,
  Split, Advanced, Statistics, Settings, Shield, Guardian, Ghost, Apps, Pro, Repair,
  Autopilot, Cache, Download, DNA, Heavyweight, Media, Network, Storage, native
  fallback screens). All testTags/semantics/data paths unchanged.

## Motion engineering hardening (added after first CI run)

CI run `45029892114` (`97957e3`) failed **device (26)** only: the full 350-test
instrumented suite hung inside `VpnSessionServiceTest#sessionNotificationUsesTheAppChosenLanguage`
after opening the notification shade (gradle exit 124, `test-timeout.txt`); no test
assertion failed anywhere, and device (35) passed the whole suite. Root cause: the
shared ambient `rememberInfiniteTransition` kept requesting Choreographer frames even
while the shade/another activity covered the app, starving the API26 software-rendered
emulator. Two fixes landed:

- `e38fe1a` — `kxEntrance` reads its `Animatable` inside the `graphicsLayer` lambda
  (layer phase only, no per-frame recomposition) and latches `settled` so the layer
  stops invalidating permanently once the entrance completes.
- `8e0c5a3` — `rememberKxAmbient` returns `KxAmbient.Still` (transition disposed,
  zero frame requests) unless the window is focused AND the lifecycle is RESUMED.
  Covered/background app now costs nothing; the clock relights on foreground return.
  Consumers already render a stable mid-animation scene from `Still`.
- `c5a964e` — run `45035675592` (on `8e0c5a3`) still hung device (26) at the same
  test with **zero app-side frames after the shade opened** — proving the hang is a
  wedged UiAutomation binder call in the API26 SystemUI bridge, not ambient frames.
  `VpnSessionServiceTest` shade probes (`shadeIsOpen`, `nodeBounds`, `shell`) now run
  on bounded daemon watchdogs (2s/3s/15s) with a window-manager fallback. Real
  observations and assertions unchanged; only a hung transport is bypassed.

## Verification status

- `:app:compileDebugKotlin`, `:app:testDebugUnitTest`,
  `:app:compileDebugAndroidTestKotlin`, `:app:assembleDebug`,
  `:app:assembleDebugAndroidTest` — BUILD SUCCESSFUL (executed locally, `8e0c5a3`).
- Physical SM-A025F (Android 12, 3.5GB RAM): the two originally failing tests pass
  individually on-device (`autopilotSampleSurvives...` OK 82s,
  `navigationAndToolsActuallyInvokeTargets` OK 24s). Full-suite local runs showed
  `No compose hierarchies found` flakes; the device event log proves a **concurrent
  agent** (`D:\Temp\opencode\knoux_home_ref\reverify_1to5.py` → `verify_hub3.py`,
  working the unrelated KNOUX AI app) repeatedly launched `com.knoux.ai.debug` via
  adb shell **during** our instrumented runs (18:47–19:02), stealing window focus and
  destroying test activities mid-assertion. Not a motion regression.
- **Clean local run (after the concurrent agent finished): `OK (20 tests)`** —
  VisualHardeningTest 9/9, HomeScreenTest 7/7, ToolsCatalogScreenTest 4/4 on
  `8e0c5a3`, SM-A025F Android 12, animation scales 1, screen awake
  (`device/local-suite-20tests-8e0c5a3.txt`, 355s).
  CI emulators are isolated and remain the authoritative device gate.
- CI: run on `8e0c5a3` recorded in the PR and ledger when complete.
- Physical-device captures and side-by-side comparisons: see `device/` (added after
  the capture pass on SM-A025F R9ZR20HWHQY).
- `04-connected-session` cannot be reproduced honestly without an authorized
  WireGuard endpoint (standing blocker); no connected-state fabrication.

## Round 3 — CI run 45042088536 (`1a0921d`) diagnosis and fixes

Both device jobs failed with exit 124, but the artifacts prove **zero assertion
failures** (API35: 352/350 completed, 3 skipped, 0 failed; API26: 346/350, 0 failed):

- **API35 logcat**: between `MainActivity RESUMED` (16:36:41.372) and the next test
  statement (FGS start 16:38:36.601) there are **115 seconds** of continuous
  ~150ms EGL frames (`app_time_stats avg=148ms`). The per-vsync ambient stream kept
  the main looper permanently busy, starving `ActivityScenario.launch`'s idle wait.
  The suite then ran out of the 30m budget mid-`ongoingNotification...`.
- **API26 logcat**: last app activity 16:21:49 (shade gesture + SystemUI deopt),
  then silence until the budget kill — the wedge is inside the shade-capture
  transport (`takeScreenshot` was the one un-watchdogged binder call).

Fixes (this round):

- `8cf1374` — **frame-paced ambient clock**: one `withFrameNanos` request per
  update followed by a real `delay`, so between updates there are zero pending
  frame awaiters and the looper genuinely idles. ~30fps cap, adaptive backoff to
  ~2x observed frame cost (33..500ms) on slow renderers, absolute-phase channels
  preserving every original period/waveform. `KxOrbitalHalo` drops its private
  per-vsync infinite transition (which also recomposed per frame) and rides the
  shared clock in the draw phase. Real battery/responsiveness win on the owner's
  SM-A025F, not a test hack.
- `2f83d5f` — shade capture: the expand-affordance picker now selects the chevron
  **nearest** the session title (field probe `shade-dump-scroll1.json`: the USB
  row's chevron at y=1103 fell inside the old ±40dp window of the KX title at
  y=1199 and came first in tree order, so the tap opened USB options).
  `takeScreenshot` joins the daemon-watchdog pattern (10s bound, explicit honest
  failure message instead of a suite-wide hang).

Local `VpnSessionServiceTest` on the physical phone reached 4/6 before these
fixes; the two remaining failures were the chevron mis-tap above. After the
phone was re-attached, the full class was re-run on the merged build
(`54f1e14`, installed 22:48/22:53 per `dumpsys package`): **`OK (6 tests)` in
48.2s** — `device/local-vpn-service-6tests-54f1e14.txt`. The nearest-chevron
picker and screenshot watchdog are confirmed on real One UI, not just the
emulators.

## Final gate — CI run 45053268703 on `8a8d489`: ALL GREEN

| Job | Result | Duration |
|---|---|---|
| build | success | 8m02s |
| release-compile | success | 4m45s |
| device (26) | **success** (previously exit-124 wedge) | **12m24s** |
| device (35) | success | 30m35s |
| SonarCloud Code Analysis | pass (after `358b8ee` kotlin:S899 fix) | 1m13s |

CircleCI reports "fail" because the external service has no configuration in this
repo; ignored per standing policy. PR #55 merged into `main` as `e8dad11`;
feature branch deleted local + remote. The frame-paced clock resolved the
API26 looper-starvation wedge for real: the full 350-test suite now completes
well inside the budget on the software-rendered emulator.

## Post-merge physical-phone confirmation (build `54f1e14`, 2026-11-22 ~23:05)

Phone re-attached; merged build installed (`dumpsys package` lastUpdateTime
22:48:58 app / 22:53:26 test). Every capture below was taken only after the
accessibility tree confirmed the actual screen (an earlier blind-tap attempt
landed on the splash and was discarded, not committed):

- `device/final-01-home-54f1e14.png` — Home: orbital halo + satellites around
  the hero orb, luminous ribbons, honest `غير متصل` pill and measured-or-dash
  counters (SHA-256 prefix `7db440f1878744a7`).
- `device/final-02-tools-54f1e14.png` — `لوحة الأدوات` (ref 18): haloed emblem,
  search field, filter chips `الكل (٩)`, glass tool cards (`732908201100d10a`).
- `device/final-03-repair-54f1e14.png` — `الإصلاح الذكي` (ref 03): measured
  plan (١٢٨ بايت cache, طبيعية temperature, سليم app state), no fabricated
  progress (`557edd2121f2074b`).
- `device/local-vpn-service-6tests-54f1e14.txt` — **`OK (6 tests)`** in 48.2s.

`svc power stayon` restored to `false`; shade collapsed after the run.
