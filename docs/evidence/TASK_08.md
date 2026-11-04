# TASK_08 — Real measured usage statistics

**baseline_sha:** `0769523` (main after TASK_07 merge and docs)
**branch:** `codex/task-08-metrics`
**implementation_sha:** `9ab88bf` (feature `60f67a4`, dialog-capture fixes `fabc311`/`91a4b0a`,
in-screen modal `9ab88bf`; docs commit `34989fa` only)
**status:** IMPLEMENTED · LOCAL GATES PASS · API26 CI VERIFIED (79/79) · API35 BLOCKED (SAME
RENDERER) · VISUAL STRUCTURE VERIFIED · MERGED

## Scope implemented

- Measured-only statistics. `UsageRecord` persists WireGuard RX/TX counter deltas, monotonic session
  duration, profile/endpoint identity, the wall-clock start day, and a `recovered` marker. There is
  no ping, privacy score, synthetic download, interpolated traffic or invented history.
- `UsageRecorder` samples `VpnState.session` transitions. A 15 s checkpoint (record + monotonic
  uptime) is written while connected; process death recovers it once as `recovered`, and a
  zero-length checkpoint is discarded. Restored tunnels with no observed start are never recorded.
- Clearing history deletes records + checkpoint in one DataStore edit, then rebases the live
  session so pre-clear bytes/time can never be re-added. Retention keeps the newest 2,000 sessions.
- Pure `UsageAggregator`: 7/30/90/365-day windows, previous equal-window comparison, measured
  average rates (bytes ÷ measured tunnel time), per-server duration share, 7-day sparkline, and
  13 weekly/13 monthly buckets. A session crossing midnight counts on its start day (stated on
  screen).
- Native `StatisticsScreen` follows the owner reference structure: KX page header with overflow
  menu, total-data / average-speed / most-used-server / connection-time cards, Daily Usage area
  chart with real range chips, average-speed RX/TX chips, connection-time donut with measured
  share, Server History with per-server sparklines, honest measurement note, and the five-item
  glass bottom bar.
- Unsupported reference cards are replaced honestly: Average Ping and Privacy Score do not exist
  in this app, so the same slots show Sessions and measured Connection Time instead.
- Real controls: range chips recompute from stored records, overflow opens a confirmed clear
  dialog, retry reloads, bottom navigation routes to Home/Servers/Security/Profile.

## Changed paths

- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/usage/` — `UsageModels.kt`,
  `UsageCodec.kt`, `UsageAggregator.kt`, `UsageRecorder.kt`, `UsageHistory.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/data/stats/UsageStatsStore.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/StatisticsScreen.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/ClientViewModel.kt` (recorder wiring,
  checkpoint recovery, range/clear actions, save-failure surfacing)
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/NativeClient.kt` (`stats` route,
  `open-stats` home entry)
- `android/app/src/main/res/values/statistics_strings.xml` + `values-ar/statistics_strings.xml`
- Tests: `android/app/src/test/java/com/knoux/kxprivatevpn/UsageCodecTest.kt`,
  `UsageAggregatorTest.kt`, `UsageRecorderTest.kt`,
  `android/app/src/androidTest/java/com/knoux/kxprivatevpn/UsageStatsStoreTest.kt`,
  `StatisticsScreenTest.kt`, `StatisticsFlowTest.kt`

## Local verification (Windows, 2026-11-04)

| Gate | Result |
|---|---|
| `:app:compileDebugKotlin` | PASS |
| `:app:testDebugUnitTest` | 98 tests, 0 failures (30 new) |
| `:app:lintDebug` | 0 errors (27 warnings) |
| `:app:assembleDebug` | PASS |
| `:app:compileDebugAndroidTestKotlin` | PASS |

### New JVM tests

- `UsageCodecTest` (8): record/checkpoint round-trip, corruption rejection, retention ordering.
- `UsageAggregatorTest` (12): totals, windows, previous-window comparison, start-day attribution,
  buckets, server share/trend, empty-state honesty.
- `UsageRecorderTest` (10): checkpoint cadence, recovery once, zero-length discard, clear rebase,
  monotonic duration, save-failure propagation.

### New instrumentation tests

- `UsageStatsStoreTest` (7): restart persistence, ordering, checkpoint lifecycle, atomic clear,
  invalid input rejection, recovered flag.
- `StatisticsScreenTest` (10): empty state, measured totals/rates/server/history, range selection,
  confirmed clear dialog, save/clear errors, load failure/retry, loading state, bottom navigation,
  night RTL, Arabic 1.6× large text.
- `StatisticsFlowTest` (1): real Activity path from Home, app-private store read, no traffic claim
  on a fresh device, Home tab returns.

## Visual evidence

Reference: `public/assets/aistudio/08-statistics_a6c474b9.png`.
Captures (API26, artifact `native-device-api-26`, run `41924124861`), `device-screenshots/identity-evidence/`:

- `36-statistics-defaults-light.png`
- `37-statistics-populated-light.png`
- `38-statistics-clear-dialog.png`
- `39-statistics-night-rtl.png`
- `40-statistics-ar-large-text.png`
- `41-statistics-device-light.png`
- `42-statistics-30d-range.png`

### Visual comparison

| Aspect | Reference | Native capture | Verdict |
|---|---|---|---|
| Header | Back, KX lockup, overflow | Same header; overflow opens clear history | Matches |
| Metric cards | Total data / average speed / ping / privacy | Total data / average speed / most-used server / measured connection time | Structure matches; unsupported metrics replaced honestly |
| Daily Usage chart | Filled area chart + 7D/30D/3M/1Y chips | Same layout; empty history draws no fake line/dots, one `0 B` label and an explicit empty message | Matches; honesty difference |
| Speed card | Average speed + RX/TX chips | Same, computed from measured bytes ÷ measured tunnel time | Matches |
| Connection time | Donut + server legend | Same with measured share; empty state says `No data`, never `< 1 min` | Matches; honesty difference |
| Server History | Rows with sparkline | Same, real per-server measured trend | Matches |
| Bottom bar | 5 glass items | Same, all navigate | Matches |
| Night + RTL | Not supplied | Capture 39: mirrored layout, localized weekday labels with LRM marks | Verified |
| Arabic 1.6× | Not supplied | Capture 40: cards stack to one column, all sections reachable, labels wrap | Verified |

Capture review of run `41915740111` found and fixed: an empty chart drew a fake flat line with dots
and repeated `0 B` labels; an empty donut rendered `< 1 min`; captures were taken after scrolling
past the top of the screen; the Arabic capture lacked the localized `LocalConfiguration`. All were
fixed in `1f9a94d`/`91a4b0a` and re-captured in run `41924124861`. The clear dialog is rendered
in-screen (not a separate dialog window) so API 26 can capture it; `38-statistics-clear-dialog.png`
shows the real modal.

## Known limits (honest)

- No owned WireGuard gateway/peer exists, so external handshake/egress traffic still cannot be
  reconciled against real remote counters. What IS verified: counter/duration reconciliation from
  the controller's measured `VpnSession` values, persistence, recovery, aggregation and UI.
- Statistics only exist for sessions this app started and observed; profile imports or tunnels
  restored without a recorded start are not retroactively invented.
- API35 headless SwiftShader emulator keeps losing its renderer at
  `IdentityTest.arabicRtlLargeText360dp`; API26 is the merge gate. This is an environment blocker,
  not a product assertion failure, and is documented in `STATUS_BLOCKERS.md`.
- The KX brand orb/ribbons/glass optical depth are shared with the other merged screens and are
  tracked as a cross-cutting TASK_00 visual item; no statistics-specific regression was introduced.
- No release signing / Play Console credentials.

## Permission tests

No new permission. Usage history lives in app-private DataStore; no storage/media access, no
network calls, no analytics.

## PR / CI / merge

- PR: [#7](https://github.com/daynightae-cmyk/KX-Private-VPN/pull/7)
- CI run: `41924124861` (head `34989fa`) — build PASS; API26 device PASS (79 tests, 0 failures,
  0 errors, 0 skipped); API35 FAIL only on the documented `IdentityTest.arabicRtlLargeText360dp`
  dialog-teardown renderer loss.
- Unit tests: 98/98 PASS (30 new)
- Visual evidence: captures `36`–`42` downloaded from `native-device-api-26` and reviewed above
- merged_sha: `ecf98482dbae986d2a28582d85877078634b69bf` (PR #7 merge commit)
- remote_main_sha: `ecf9848` at merge
- next_task: TASK_09 — secure config import polish, after merge.
