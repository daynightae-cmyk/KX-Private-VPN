# TASK_06 — Per-app split tunneling

**baseline_sha:** `ca5f92e` (main after TASK_05 merge and docs)
**branch:** `codex/task-06-split-tunneling`
**implementation_sha:** `4fd25f3` (final head; `1cd28fd` feature, `b77225c` fixed scope notes, `24a7c62` swipes, `4fd25f3` tagged list gestures)
**status:** IMPLEMENTED · LOCAL GATES PASS · API26 CI VERIFIED (40/40) · API35 BLOCKED (SAME RENDERER) · VISUAL STRUCTURE VERIFIED

## Scope implemented

- New native `SplitTunnelScreen` (Compose) reached from Home via `open-split`, matching the owner
  reference `06-split-tunneling_c9265fd7.png`: KX header, glass hero with a two-ribbon routing
  emblem, Bypass / Only-through segmented control, search field, real app rows with PackageManager
  icons, and a persistent bottom summary.
- Real app discovery: `PackageManager.queryIntentActivities(ACTION_MAIN/CATEGORY_LAUNCHER)` with the
  Android 11+ `<queries>` launcher visibility declaration. Own package and non-launchable apps are
  excluded because Android never tunnels the VpnService owner.
- Two real routing modes mapped to WireGuard: `ONLY_VPN` → `includeApplications`
  (`allowedApplications`), `BYPASS` → `excludeApplications` (`disallowedApplications`). One list is
  sent at a time; imported `IncludedApplications`/`ExcludedApplications` are replaced, never merged.
- Empty allow-list is never sent: Android would route every app anyway, so the app keeps normal
  all-app routing and shows an explicit warning instead of claiming a selection is active.
  Toggling one row off in that state seeds an allow-list of every other installed app.
- Stale/uninstalled saved packages are filtered out before they reach `VpnService.Builder` and
  warned about in the UI.
- Persisted intent (mode + package names only) in private SharedPreferences; no labels, icons or
  traffic are stored. A failed commit surfaces as "Nothing was applied".
- Running-tunnel rebuild: changing rules while connected shows a "Reconnect now" banner with the
  **applied** counts only; reconnect reuses Android consent when still granted, otherwise launches
  the real consent intent, and never claims success before `GoBackend` reports up.
- Honest states: loading, PackageManager failure + retry, no launchable apps, no search match, save
  failure, stale-package warning, unapplied rules, disconnected note. Work-profile apps are
  documented as not visible.
- No new permission; no storage/media access; no traffic inspection.

## Changed paths

- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/apps/InstalledApps.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/vpn/SplitTunnelPolicy.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/vpn/SplitTunnelConfigurator.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/data/config/SplitTunnelStore.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/SplitTunnelScreen.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/ClientViewModel.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/NativeClient.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/vpn/VpnController.kt`
- `android/app/src/main/AndroidManifest.xml`
- `android/app/src/main/res/values/split_tunnel_strings.xml`
- `android/app/src/main/res/values-ar/split_tunnel_strings.xml`
- `android/app/src/test/java/com/knoux/kxprivatevpn/SplitTunnelPolicyTest.kt`
- `android/app/src/test/java/com/knoux/kxprivatevpn/VpnControllerTest.kt`
- `android/app/src/androidTest/java/com/knoux/kxprivatevpn/SplitTunnelScreenTest.kt`
- `android/app/src/androidTest/java/com/knoux/kxprivatevpn/SplitTunnelStoreTest.kt`
- `android/app/src/androidTest/java/com/knoux/kxprivatevpn/SplitTunnelFlowTest.kt`

## Local verification (Windows, 2026-11-04)

| Gate | Result |
|---|---|
| `:app:compileDebugKotlin` | PASS |
| `:app:testDebugUnitTest` | 55 tests, 0 failures (14 `SplitTunnelPolicyTest`, 16 `VpnControllerTest`, 25 existing) |
| `:app:lintDebug` | 0 errors |
| `:app:assembleDebug` | PASS |
| `:app:compileDebugAndroidTestKotlin` | PASS |

### New JVM tests

- `SplitTunnelPolicyTest` (14): exactly one Android list at a time; own/uninstalled/invalid packages
  never reach the tunnel; empty allow-list is not applied; counts follow mode; config round-trip
  preserves keys/DNS/MTU/peers; imported app rules are replaced; toggle seeding and inverse mapping.
- `VpnControllerTest` (+4): reconnect stops then starts with fresh rules; reconnect ignored unless
  connected; failed stop never claims a new tunnel; config adjuster seam is applied before backend.
  The test fake now returns a fresh config array because the controller zeroes the bytes it owns.

### New instrumentation tests

- `SplitTunnelScreenTest` (9): only-mode counts/status, bypass inverse mapping, empty allow-list
  warning plus truthful "Only through VPN" rows, search/no-match, toggle/mode callbacks, reconnect
  banner, applied banner, night Arabic RTL capture, 1.6× large-text Arabic reachability capture.
- `SplitTunnelStoreTest` (3): selection survives a new store instance; invalid persisted values fall
  back safely; missing store is the honest empty selection.
- `SplitTunnelFlowTest` (1): real Activity → Home → Split Tunneling, waits for PackageManager,
  asserts a non-zero real app count, asserts disconnected routing is not claimed, captures device.

## Visual evidence

Reference: `public/assets/aistudio/06-split-tunneling_c9265fd7.png`.
Expected captures from instrumentation (CI artifact): `25-split-tunnel-only-mode-light`,
`26-split-tunnel-bypass-mode-light`, `27-split-tunnel-night-rtl`,
`28-split-tunnel-device-light`, `29-split-tunnel-ar-large-text`.
Comparison notes are appended after the CI API26 artifact is downloaded.

### Visual comparison (CI API26 artifact, run `41888802503`)

| Aspect | Reference | Native capture | Verdict |
|---|---|---|---|
| Header | Shield + KX wordmark, back button | Roundel + KX, back button | Close; brand mark smaller (same gap as TASK_05) |
| Title | Large centered "Split Tunneling" | Same | Matches |
| Hero | Glass orb with split routing ribbons, shield + crossed-eye badges, description on the right | Glass orb with two routing ribbons, shield/bypass badges, description on the right | Structure matches |
| Protocol/mode | "Bypass VPN" / "Only through VPN" segmented control, selected pill violet | Same segmented control, same selected styling | Matches |
| Search | Rounded search field with glyph | Same | Matches |
| App rows | Icon, name, per-app status + switch | Real PackageManager icons (fallback letter tile offline), status + switch | Matches; reference shows 6 sample apps, capture shows Android's real list |
| Bottom summary | "6 apps · 3 through VPN · 3 bypassing" pill | Same three-part pill with measured counts | Matches |
| Disconnected honesty | Reference implies a working tunnel | Capture 28 states "No apps selected. Until you pick one, the tunnel routes every app." and never claims applied routing | Intentional truth difference |
| RTL/night | Not supplied | Capture 27: mirrored header, selector, switches, summary | Verified |
| Large text | Not supplied | Capture 29 at 1.6× Arabic: rows, notes and summary all reachable | Verified |
| Notes | Not in reference | Three scope notes fixed under the list (own addition, always visible) | Kept for honesty |

## Known limits (honest)

- No owned WireGuard gateway/peer, so end-to-end in/out package traffic cannot be verified here.
  The policy/config path is verified by JVM tests and the backend call is real at runtime.
- Android only exposes launchable apps to this app; non-launchable packages and work-profile apps
  cannot be listed, and selecting any allow-list necessarily excludes the invisible ones.
- API35 headless SwiftShader renderer remains unstable around dialog teardown (existing blocker);
  API26 is the device gate.
- No release signing / Play Console credentials.

## PR / CI / merge

- PR: #5 `https://github.com/daynightae-cmyk/KX-Private-VPN/pull/5`
- CI run: `41888802503` (head `4fd25f3`) — build PASS; API26 device PASS (40 tests, 0 failures, 36.392s); API35 FAIL only on `IdentityTest.arabicRtlLargeText360dp` dialog-teardown renderer loss (known blocker). Earlier runs on `1cd28fd`/`b77225c`/`24a7c62`/`24a7c62` documented the two device-found test defects and their fixes.
- Unit tests: 55/55 PASS (14 new policy tests, 4 new controller tests)
- Visual evidence: captures `25`–`29` downloaded from `native-device-api-26` artifact
- merged_sha: pending
- remote_main_sha: pending
- next_task: TASK_07 — advanced network configuration (MTU/DNS/keepalive), after merge.
