# TASK_07 — Advanced network configuration

**baseline_sha:** `e775abb` (main after TASK_06 merge and docs)
**branch:** `codex/task-07-advanced-network`
**implementation_sha:** `b1c8828` (`51f5663` feature, `0b53657` first test fixes, `881cedc` slider/DNS test fixes, `c8d6395` + `b1c8828` capture-review glyph/chevron fixes)
**status:** IMPLEMENTED · LOCAL GATES PASS (compile/unit 68 ✓/lint 0 errors/assemble) · CI/visual verification in progress on `b1c8828`

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

## api_and_real_results

- Local gates on `b1c8828` (from `android/`): `:app:compileDebugKotlin` PASS;
  `:app:testDebugUnitTest` 68 tests, 0 failures (13 new: MTU/DNS/keepalive/validator,
  IPv6-correct DNS comparison, split→advanced composition);
  `:app:compileDebugAndroidTestKotlin` PASS; `:app:lintDebug` 0 errors (baseline warnings);
  `:app:assembleDebug` PASS.
- CI `41906598533` on `881cedc`: build PASS; **API26 device PASS 61/61 (0 failures, 0 errors,
  0 skipped)**; API35 stopped at the documented `IdentityTest.arabicRtlLargeText360dp` renderer
  loss (no new assertion failures).
- CI on `b1c8828`: pending — final visual pass after the capture-review fixes.

## unit_ui_instrumentation_results

- JVM `AdvancedNetworkConfigTest`: 13 tests — bounds (1280–1420 MTU, 0–65535 keepalive), numeric
  DNS hosts only (hostnames/`0.0.0.0`/`::`/duplicates/>3 rejected), MTU override vs profile,
  custom/system/profile DNS modes, custom-empty fallback, keepalive per-peer rewrite incl. zero=off
  and untouched profiles, peer fields preserved verbatim in the textual rebuild, and
  `advanced.after(split)` keeps `ExcludedApplications` while applying MTU+keepalive.
- Instrumented `NetworkSettingsStoreTest`: 6 tests — round-trip, empty default, garbage mounts
  sanitized (bad MTU/mode/hosts/keepalive), partial valid custom hosts survive, custom-without-
  usable-hosts falls back to PROFILE, save rejects out-of-range input instead of clamping.
- Instrumented `AdvancedNetworkScreenTest`: 14 tests — protocol row states (WireGuard selected;
  others not selected + disabled semantics + "Unavailable" captions), DNS expansion incl.
  invalid-host error + disabled save + valid hosts saved, obfuscation/port-hopping switch
  disabled-ness, connected dirty→reconnect / clean→applied text, keepalive pill shows Profile/Off/N s
  (no invented number), reset panel flow, MTU slider commit through the Material3 accessibility
  `SetProgress` action snapping 1313 → 1310, night RTL and 1.6× Arabic captures.
- Instrumented `AdvancedNetworkFlowTest`: real Activity path — stored settings load, honest
  "apply on next start" note, no applied/reconnect claims without a tunnel, disabled rows on
  device, runtime capture.
- Compose interaction fixes verified against Material3 1.3.2 bytecode: the slider commit path is
  `SetProgress` (calls `onValueChange` then `onValueChangeFinished`); a track tap only animates and
  a track drag starts no gesture, so the old touch test could never commit.

## visual_before_after

Reference: `public/assets/aistudio/07-advanced-network_5b3214f4.png`.

| Reference element | Device capture | Verdict |
| --- | --- | --- |
| KX back target + centered brand + three-dot overflow | 30/33 | Structure matches; capture review found the three-dot glyph and every card glyph rendered 0×0. `CardGlyph` now forwards `fillMaxSize`, so dots/glyphs draw. |
| Glass stack hero | 30 | Gradient glass, rim, overlapping glass panes reproduced. |
| Protocol cards (WireGuard / OpenVPN / Automatic) | 30 | Faithful layout; only WireGuard interactive, others honestly "Unavailable". |
| Advanced Controls (MTU / DNS / Obfuscation / Port hopping / keepalive) | 30–32, 34 | Layout, labels, sliders, switches and pills match; unsupported rows disabled with honest copy instead of the reference's "On". |
| Custom DNS editor | 31 | Reference has no editor state; the app adds numeric-host validation, inline error and a disabled Save. |
| Night + RTL | 33 | Gradient/readability and mirrored layout hold; back chevron direction fixed in `c8d6395` (was inverted in both directions). |
| Arabic 1.6× large text | 34 | Labels wrap without clipping; controls stay reachable. |
| Connected dirty banner | 32 | Honest reconnect banner replaces any fake applied state. |

`c8d6395` + `b1c8828` capture-review fixes (require the final CI artifact): `CardGlyph` size
forwarding at all four call sites (three-dot, protocol cards, advanced-control panels) and
`BackChevron` LTR/RTL swap.

## screenshot_files

From CI run `41906598533` (API26), artifact `native-device-api-26`,
`device-screenshots/identity-evidence/`:

- `30-advanced-defaults-light.png`
- `31-advanced-dns-custom-expanded.png`
- `32-advanced-connected-dirty.png`
- `33-advanced-night-rtl.png`
- `34-advanced-ar-large-text.png`
- `35-advanced-device-light.png`

Final `b1c8828` captures are downloaded from the follow-up API26 artifact and reviewed before
merge; evidence paths remain the same names.

## known_errors

- API35 headless emulator loses its renderer during `IdentityTest.arabicRtlLargeText360dp`
  dialog teardown (documented blocker; API26 is the reliable device gate).
- No owned WireGuard gateway/peer exists, so MTU/DNS/keepalive cannot be verified end-to-end
  against real egress traffic this cycle; what IS verified: the exact bytes the backend would
  receive (JVM config rebuild tests) and the real on-device UI/store behaviour.
- OpenVPN/Automatic/obfuscation/port hopping have no backend in this build and stay honestly
  disabled.

## permission_tests

No new permission. No storage/media access. Store writes stay app-private.

## PR / CI / merge

- PR: [#6](https://github.com/daynightae-cmyk/KX-Private-VPN/pull/6)
- CI run: `41906598533` (`881cedc`: build PASS, API26 61/61 PASS, API35 documented renderer
  blocker); follow-up run on `b1c8828`: pending
- merged_sha: pending
- remote_main_sha: pending
- next_task: TASK_08 — real local metrics/trends, after merge.
