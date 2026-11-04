# KX Private VPN — current blockers

## TASK_00 — PARTIALLY VERIFIED (2026-11-03)

- Local debug APK and Android test APK build; three JVM tests pass; lint passes with dependency/icon warnings.
- Local emulator initially failed because K: could not accommodate its userdata partition. A separate project-owned AVD was created under `D:/Android/kx-private-avd` without touching other AVDs.
- That AVD cannot boot with acceleration: emulator reports `Android Emulator hypervisor driver is not installed on this machine`. A software emulation launch attempt was rejected by automatic approval review (`blocked by policy`); it was not executed.
- API 35 image installation into K: failed for insufficient disk space. Existing SDK/system images and other project files were preserved.
- At `3f8d472f585eda6e799aecbc1b4e94601e2fb8a7`, all 7 API26 instrumentation tests and build CI passed. Arabic dialog capture inspected and correct after resolving strings before opening its separate window. API35 remains pending; earlier runs had the now-fixed Arabic failure.
- First real API26 Welcome PNG/video inspected at 6a15284: black orb fringe observed and corrected; full visual parity remains unaccepted. Default emulator was 320px wide; follow-up selects Nexus5 and records actual 360/411dp geometry.
- API26 software-GPU recording measured 87/87 janky frames (p50=77ms). 60fps is NOT VERIFIED and failed on that emulator. Background recomposition was reduced; new profiling and hardware evidence are needed. Do not merge this as completed TASK_00.
- Final orb uses the byte-preserved owner artwork with native Canvas formation, particles, ribbons, refraction gradients, satellites, caustic sweep and pulse. This is a hybrid brand reveal, not an all-vector recreation. Fidelity of the animated-to-original transition must be reviewed on device.
- Follow-up Draft PR #2 now implements Welcome/education routing, encrypted WireGuard profile import/management, locale/theme settings, basic StatFs, scoped confirmed KX-cache removal and real network diagnostics. At `95fa3d6`, build and all 15 API26 tests passed. These are narrower scopes, not completion of the 22 tasks. VPN transport, media inventory, most Repair tools and full visual acceptance remain NOT IMPLEMENTED/UNVERIFIED. Network permissions are declared for the explicit Network Doctor probe; no storage or VPN consent is requested by import.
- No owned WireGuard test gateway, provisioning provider, release signing or Play Console credentials have been verified. These do not block independent native UI work, but block transport/release claims.

## TASK_02/04 — REAL TRANSPORT IMPLEMENTED, EXTERNAL HANDSHAKE DEFERRED (2026-11-04)

- The official WireGuard tunnel library (`com.wireguard.android:tunnel:1.0.20260102`) now backs the
  app. `GoBackend$VpnService` is merged into the debug manifest with `BIND_VPN_SERVICE`; the old
  manifest `tools:node="remove"` was deleted. No VPN success is simulated.
- `VpnController` reports `CONNECTED` only after `GoBackend` confirms `UP`; consent, invalid config,
  unauthorized backend, start failure, unexpected stop and failed stop each have honest states.
  Counters and the latest handshake come from real backend `Statistics`; the session screen reads
  the Android VPN network from `ConnectivityManager`.
- Verified locally: 24 JVM tests (12 new controller tests), lint 0 errors, debug APK and androidTest
  APK build, merged manifest inspection, native `libwg-go.so` present for all ABIs. API26 CI device
  gate is green on the stacked PRs.
- **Still required:** an owned WireGuard gateway/peer to verify a real external handshake, egress and
  throughput. Without it, transport/release claims stay deferred. API35 headless rendering remains
  unstable around dialog teardown and is tracked separately.

## TASK_06 — SPLIT TUNNELING IMPLEMENTED, EGRESS NOT VERIFIED (2026-11-04)

- `SplitTunnelScreen` implements the owner reference structure: routing hero, Bypass/Only-through
  segmented control, search, real `PackageManager` app rows, and a measured bottom summary.
- Real routing: `ONLY_VPN` → `includingApplications` on the WireGuard interface, `BYPASS` →
  `excludeApplications`; imported per-app rules replaced; one Android list at a time; stale and
  invalid package names never reach `VpnService.Builder`; empty allow-list is not sent (Android
  would route every app) and the UI warns instead of claiming a selection.
- Persisted intent is package names only in app-private storage; no new permission (Android 11+
  `<queries>` launcher visibility declaration).
- Verified: 55 JVM tests, API26 CI device run 40/40 (real-device flow test counts 18 real
  launchable apps and asserts no connected routing is claimed), lint 0 errors, captures 25–29 vs
  reference (structure verified; honest disconnected wording differs by design).
- **Still required:** an owned WireGuard gateway/peer to prove end-to-end package traffic through
  and around the tunnel. API35 dialog-teardown renderer loss is unchanged.

## TASK_08 — REAL MEASURED STATISTICS IMPLEMENTED, EXTERNAL TRAFFIC RECONCILIATION DEFERRED (2026-11-05)

- `StatisticsScreen` and the usage pipeline are merged. Only measured values are stored or shown:
  WireGuard RX/TX counter deltas, monotonic session duration, profile/endpoint identity, and the
  session's start day. No ping, privacy score, synthetic download or interpolated traffic is
  produced. Unsupported reference cards are replaced by Sessions and measured Connection Time.
- `UsageRecorder` samples `VpnState.session` transitions, writes a 15 s checkpoint, recovers a
  process-death checkpoint once as `recovered`, discards zero-length checkpoints, and never records
  a restored tunnel whose start is unknown. Clearing history deletes records + checkpoint atomically
  and rebases the live session so pre-clear bytes/time cannot be re-added. Retention keeps the
  newest 2,000 sessions.
- Pure `UsageAggregator` provides 7/30/90/365-day windows, previous-window comparison, measured
  average rates, per-server share and 7-day sparklines, and 13 weekly/13 monthly buckets. A session
  crossing midnight counts on its start day (stated on screen).
- Verified: 98 JVM tests (30 new), API26 CI device run 79/79 (10 screen, 7 store, 1 flow), lint 0
  errors, captures `36`–`42` reviewed against `08-statistics_a6c474b9.png`. The clear dialog is
  rendered in-screen so API 26 can capture it.
- **Still required:** an owned WireGuard gateway/peer to reconcile recorded bytes/duration against
  real external egress. API35 headless SwiftShader renderer loss at
  `IdentityTest.arabicRtlLargeText360dp` is unchanged (21–22/79 reached). No release signing/Play
  Console credentials.

## TASK_07 — ADVANCED NETWORK IMPLEMENTED, MTU/DNS/KEEPALIVE FIELD PROOF DEFERRED (2026-11-04)

- `AdvancedNetworkScreen` implements the owner reference structure: header/overflow, glass stack
  hero, protocol cards, MTU slider, DNS mode (Profile/Custom/System), obfuscation and port-hopping
  rows, keepalive slider, reset panel and honest notes.
- Real config rebuild before `GoBackend.start`: `Interface.Builder.setMtu` for an override, custom
  DNS validated as 1–3 numeric IPv4/IPv6 hosts via the WireGuard library's `InetAddresses.parse`,
  SYSTEM mode emitting no DNS line, and a textual per-peer `PersistentKeepalive` rewrite (0 = off).
  JVM tests assert the exact rebuilt bytes and that split-tunnel application lists survive the
  `advanced.apply(split.apply(config))` chain.
- Unavailable capabilities stay honest: OpenVPN/Automatic show "Unavailable", obfuscation and port
  hopping are disabled switches with "not implemented in this build" copy — no fake toggles.
- Verified: 68 JVM tests, API26 CI device run 61/61 (14 advanced screen tests, 6 store tests, real
  device flow), lint 0 errors, captures 30–35 compared with reference
  `07-advanced-network_5b3214f4.png`. Capture review found and fixed two real rendering defects
  (zero-sized glyph canvases, inverted back chevron) and the final artifact re-verified them.
- **Still required:** an owned WireGuard gateway/peer to prove MTU/DNS/keepalive effects on real
  egress traffic. API35 dialog-teardown renderer loss is unchanged (same
  `IdentityTest.arabicRtlLargeText360dp` failure, 21/61 reached).

## TASK_05 — SECURITY CENTER IMPLEMENTED, FIELD VERIFICATION LIMITED (2026-11-04)

- `SecurityCenterScreen` implements all six owner-reference rows, but only with measured state:
  kill switch/always-on from `Settings.Secure` plus `GoBackend.isLockdownEnabled()`; DNS from the
  active tunnel or `ConnectivityManager`; IPv6 from real `::/0` tunnel routes. Auto Reconnect and
  Tracker Blocking are visibly **Unavailable** and disabled — no fake tracker list or reconnection.
- Verified: 37 JVM tests (13 new), API26 CI device gate 27/27 instrumentation tests, lint 0 errors,
  debug APK. Visual captures `20`–`24` match the owner reference structure (hero orb, six compact
  glass rows, status pills, switches); honest discrepancies are intentional (ours shows
  Unknown/Awaiting tunnel instead of false "On").
- **Still required:** an owned WireGuard gateway to reach a field "Fully protected" state; the
  verified state is exercised only in controlled UI tests. API35 dialog-teardown renderer loss is
  unchanged.

## Environment repair update

- D: SDK/build-tools/platforms verified; SDK licenses accepted by official sdkmanager; escaped ignored local SDK path fixed. Local builds/lint/JVM and instrumentation APK compilation pass. No unrelated SDK/cache removed.
- Hypervisor feature inspection requires elevation; local accelerated runtime still BLOCKED. Firmware virtualization is already enabled. No system-wide changes or bypass attempted.
- API35 current-emulator ColorBuffer/device loss is reproducible. Every ADB command is now bounded; failed tests propagate their exit status. Diagnostics did not establish OOM. CI is retrying official archived Emulator 36.4.10 Stable/build15004761 with both API26/35 and unchanged behavioral assertions. Do not call the repair verified before the run succeeds.
- Details, real screenshots, source SHA, operations and next steps: [native services evidence](evidence/NATIVE_SERVICES_20261103.md).
