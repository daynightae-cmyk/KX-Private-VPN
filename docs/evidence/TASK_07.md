# TASK_07 — Advanced network configuration

**baseline_sha:** `e775abb` (main after TASK_06 merge and docs)
**branch:** `codex/task-07-advanced-network`
**implementation_sha:** `b1c8828` (final head; `51f5663` feature, `0b53657` first test fixes,
`881cedc` slider/DNS test fixes, `c8d6395` + `b1c8828` capture-review glyph/chevron fixes)
**status:** IMPLEMENTED · LOCAL GATES PASS · API26 CI VERIFIED (61/61) · API35 BLOCKED (SAME
RENDERER) · VISUAL STRUCTURE VERIFIED

## Scope implemented

- New native `AdvancedNetworkScreen` (Compose) reached from Home via `open-advanced`, matching the
  owner reference `07-advanced-network_5b3214f4.png`: KX header with the reference three-dot
  overflow, glass stack hero card, Protocol segmented cards, Advanced Controls cards (MTU slider,
  DNS mode, Obfuscation, Port hopping, Handshake keepalive slider), reset panel, honest notes.
- Real MTU tuning: slider 1280–1420 snapped onto a 10-byte grid, applied through
  `Interface.Builder.setMtu` before the backend starts. `null` = the profile's own `MTU` line
  passes through untouched (pill shows "Profile", never an invented number).
- Real DNS modes, honestly labelled:
  - **Profile DNS** — the servers written in the profile are applied (or nothing, when absent).
  - **Custom servers** — 1–3 numeric IPv4/IPv6 hosts validated by the WireGuard library's own
    `InetAddresses.parse`; a host name can never depend on a resolver lookup.
  - **System resolver** — no DNS line is emitted, so `GoBackend` leaves the device resolver alone.
    The description says explicitly that nothing is encrypted by KX (the reference's
    "Encrypted DNS" wording was rejected as a false claim).
- Real keepalive override: slider 0–120 s (5 s steps), rewritten per peer via a textual rebuild
  (`PersistentKeepalive`), because WireGuard's `Peer` has no public builder. WireGuard treats 0 as
  off, so a 0 override really stops periodic packets an imported profile asked for. `null` =
  profile value passes through. The running tunnel's measured keepalive is shown on the session
  (`VpnSession.keepaliveSeconds`), computed only when all peers agree.
- Protocol row truth: WireGuard is the only implemented protocol, so it is the selected card;
  OpenVPN and "Automatic" are visible, non-interactive, and captioned "Unavailable" instead of
  pretending a backend exists. The reference's pre-selected "Automatic" state is deliberately not
  reproduced.
- Obfuscation and Port hopping rows are rendered like the reference but with genuinely disabled
  switches (off, `disabled()` semantics) and "not implemented in this build" wording — no fake
  toggles that would change nothing.
- Applied/dirty tracking mirrors split tunneling: on the IDLE→CONNECTING transition the view model
  captures the settings the configurator read; while connected, changes show a "Reconnect now"
  banner, and a clean state shows what was applied (MTU/DNS/keepalive segments). Saves are
  immediate with a failure surfacing as "Nothing was applied"; reset (three-dot) puts MTU, DNS and
  keepalive back under profile control.
- Settings persistence (`NetworkSettingsStore`): MTU/DNS mode/custom hosts/keepalive only, in
  app-private SharedPreferences, sanitized on load (out-of-range values fall back to profile
  control), commit failure reported instead of faked success.
- `adjustConfig` composition completed in `ClientViewModel`:
  `advanced.apply(split.apply(config))` — the advanced rebuild carries split-tunnel application
  lists through via getters, then rewrites keepalive per peer. JVM test proves app lists survive
  the whole chain.
- Honest placeholders: profile baseline (selected profile's MTU/keepalive/DNS presence from the
  real parsed profile) drives pill previews; no profile selected is stated in the notes.

## Changed paths

- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/vpn/AdvancedNetworkSettings.kt` (modes, validator)
- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/vpn/AdvancedNetworkConfigurator.kt` (store reader + pure config rebuild)
- `android/app/src/main/java/com/knoux/kxprivatevpn/data/config/NetworkSettingsStore.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/AdvancedNetworkScreen.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/ClientViewModel.kt` (advanced state, applied/dirty, configurator chain)
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/NativeClient.kt` (route + home entry)
- `android/app/src/main/java/com/knoux/kxprivatevpn/designsystem/GlassComponents.kt` (back-chevron direction; shared with other page headers)
- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/vpn/VpnModels.kt` (+`VpnSession.keepaliveSeconds`)
- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/vpn/VpnController.kt` (measured keepalive in the session)
- `android/app/src/main/res/values/advanced_strings.xml` + `values-ar/advanced_strings.xml`
- Tests: `android/app/src/test/java/com/knoux/kxprivatevpn/AdvancedNetworkConfigTest.kt`,
  `android/app/src/androidTest/java/com/knoux/kxprivatevpn/NetworkSettingsStoreTest.kt`,
  `android/app/src/androidTest/java/com/knoux/kxprivatevpn/AdvancedNetworkScreenTest.kt`,
  `android/app/src/androidTest/java/com/knoux/kxprivatevpn/AdvancedNetworkFlowTest.kt`

## Local verification (Windows, 2026-11-04)

| Gate | Result |
|---|---|
| `:app:compileDebugKotlin` | PASS |
| `:app:testDebugUnitTest` | 68 tests, 0 failures (13 new) |
| `:app:lintDebug` | 0 errors (25 baseline warnings) |
| `:app:assembleDebug` | PASS |
| `:app:compileDebugAndroidTestKotlin` | PASS |

### New JVM tests

- `AdvancedNetworkConfigTest` (13): bounds (1280–1420 MTU, 0–65535 keepalive), numeric DNS hosts
  only (hostnames/`0.0.0.0`/`::`/duplicates/>3 rejected), MTU override vs profile, custom/system/
  profile DNS modes, custom-empty fallback, keepalive per-peer rewrite incl. zero=off and untouched
  profiles, peer fields preserved verbatim in the textual rebuild, and `advanced.after(split)`
  keeps `ExcludedApplications` while applying MTU+keepalive. IPv6 host comparison is
  address-based, not string-based.

### New instrumentation tests

- `NetworkSettingsStoreTest` (6): round-trip, empty default, garbage mounts sanitized (bad
  MTU/mode/hosts/keepalive), partial valid custom hosts survive, custom-without-usable-hosts
  falls back to PROFILE, save rejects out-of-range input instead of clamping.
- `AdvancedNetworkScreenTest` (14): protocol row states (WireGuard selected; others not selected +
  disabled semantics + "Unavailable" captions), DNS expansion incl. invalid-host error + disabled
  save + valid hosts saved, obfuscation/port-hopping switch disabled-ness, connected
  dirty→reconnect / clean→applied text, keepalive pill shows Profile/Off/N s (no invented number),
  reset panel flow, MTU slider commit through the Material3 accessibility `SetProgress` action
  snapping 1313 → 1310, night RTL and 1.6× Arabic captures.
- `AdvancedNetworkFlowTest` (1): real Activity path — stored settings load, honest "apply on next
  start" note, no applied/reconnect claims without a tunnel, disabled rows on device, runtime
  capture.

Compose-interaction facts verified against the Material3 1.3.2 bytecode: `SetProgress` invokes
`onValueChange` then `onValueChangeFinished`; a track tap only animates and a track drag starts no
gesture, so the earlier touch-based slider test could never commit. The DNS editor is below the
fold after expansion, so its field/error/save nodes are scrolled into view first.

## Visual evidence

Reference: `public/assets/aistudio/07-advanced-network_5b3214f4.png`.
Captures from CI run `41908841314` (API26), artifact `native-device-api-26`,
`device-screenshots/identity-evidence/`:

- `30-advanced-defaults-light.png`
- `31-advanced-dns-custom-expanded.png`
- `32-advanced-connected-dirty.png`
- `33-advanced-night-rtl.png`
- `34-advanced-ar-large-text.png`
- `35-advanced-device-light.png`

### Visual comparison (final artifact)

| Aspect | Reference | Native capture | Verdict |
|---|---|---|---|
| Header | Back `<`, KX lockup, clear vertical three-dot button | Same; chevron now `<` in LTR / `>` in RTL and the dots draw | Matches; both were broken and fixed in `c8d6395`/`b1c8828` |
| Hero | Glass stack emblem + glass card | Stack emblem now visible, same glass/rim | Matches |
| Protocol | Link/dome/infinity icons, gradient selected card | Same icons, WireGuard gradient selected; OpenVPN/Automatic honestly unavailable | Structure matches; honest state difference |
| Advanced Controls | Icons on every row, vibrant sliders/switches | Icons on every row, MTU/keepalive sliders, disabled switches for unimplemented rows | Matches; honesty difference |
| Custom DNS editor | Not present in reference | Numeric-host editor with inline validation and disabled Save | Added real behaviour |
| Night + RTL | Not supplied | Capture 33: mirrored header/rows, readable night glass | Verified |
| Arabic 1.6× | Not supplied | Capture 34: all rows reachable, glyphs visible, labels wrap | Verified |
| Connected dirty state | Not supplied | Capture 32: "Settings changed. Reconnect the tunnel…" + Reconnect now | Honest real-state addition |

Capture review found the initial artifact's glyph canvases were 0×0 (`CardGlyph` sized only the
outer Box) and the back chevron branches were inverted; both defects were fixed, re-tested, and
verified in the final artifact above. All 61 device tests passed with the fixes.

## Known limits (honest)

- No owned WireGuard gateway/peer exists, so MTU/DNS/keepalive cannot be verified end-to-end
  against real egress traffic this cycle; what IS verified: the exact bytes the backend would
  receive (JVM config rebuild tests) and the real on-device UI/store behaviour.
- OpenVPN/Automatic/obfuscation/port hopping have no backend in this build and stay honestly
  disabled.
- API35 headless SwiftShader emulator loses its renderer during
  `IdentityTest.arabicRtlLargeText360dp` dialog teardown (21/61 reached, then device lost). API26
  is the reliable device gate; this run reproduced the same blocker with no new assertion failures.
- No release signing / Play Console credentials.

## Permission tests

No new permission. No storage/media access. Store writes stay app-private.

## PR / CI / merge

- PR: [#6](https://github.com/daynightae-cmyk/KX-Private-VPN/pull/6)
- CI run: `41908841314` (head `b1c8828`) — build PASS; API26 device PASS (61 tests, 0 failures,
  0 errors, 0 skipped); API35 FAIL only on the documented `IdentityTest.arabicRtlLargeText360dp`
  dialog-teardown renderer loss.
- Unit tests: 68/68 PASS (13 new)
- Visual evidence: captures `30`–`35` downloaded from `native-device-api-26` and reviewed above
- merged_sha: `9cf4b6433d4d399bf87e03365103d7de7f20f8d1` (PR #6 merge commit)
- remote_main_sha: `9cf4b64` at merge; evidence/ledger docs commit follows on `main`
- next_task: TASK_08 — real local metrics/trends, after merge.
