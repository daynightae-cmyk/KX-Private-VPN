# Real WireGuard transport slice — 2026-11-04

**Source:** PR #3 `codex/task-02-vpn-transport`, stacked on PR #2 (`codex/native-onboarding-config`,
head `65303bb`). Transport implementation commit `f4cb157`; verification-only follow-ups are listed
with the final head after CI.

## What is now real

- The official `com.wireguard.android:tunnel:1.0.20260102` library (`GoBackend`) is the tunnel
  backend. Its `VpnService` is no longer stripped: the merged debug manifest was inspected and
  contains `com.wireguard.android.backend.GoBackend$VpnService` with
  `android.permission.BIND_VPN_SERVICE` and the `android.net.VpnService` intent filter.
- `VpnController` reads the selected profile's Keystore-encrypted config, parses it with the
  existing `ConfigValidator`, and starts the backend. `CONNECTED` is emitted only after
  `backend.setState(tunnel, UP, config)` reports `UP`; a failed start, invalid/unreadable config,
  missing Android authorization or an unexpected backend stop becomes an explicit error instead of
  a connected claim.
- Android VPN consent is requested only when the user asks to connect: `VpnService.prepare()` is
  called from Home and the consent result starts the tunnel or records `CONSENT_DENIED`.
- While up, the UI reads real backend `Statistics` (RX/TX bytes, latest handshake) once per second.
  It never synthesizes latency or protection.
- A persisted session hint is used only to query the backend on the next process start. The UI shows
  `CONNECTED` only when `getState` confirms `UP`; otherwise it stays disconnected.
- The session screen reads the real Android VPN network from `ConnectivityManager`
  (`TRANSPORT_VPN`): interface name, addresses, DNS, route count and MTU (API 29+). When Android
  reports no VPN network it says so, instead of inventing one.
- English and Arabic strings cover every new state; no state uses fake success text.

## Verification executed locally

| Command | Result |
|---|---|
| `android> gradlew.bat :app:testDebugUnitTest` | 24 JVM tests, 0 failures (12 new `VpnControllerTest` cases) |
| `android> gradlew.bat :app:lintDebug` | 0 errors (17 warnings: dependency updates, plural candidate, unused string, launcher icon shape) |
| `android> gradlew.bat :app:assembleDebug` | success |
| `android> gradlew.bat :app:compileDebugAndroidTestKotlin` | success |
| merged debug manifest inspection | `GoBackend$VpnService` + `BIND_VPN_SERVICE` present |

New instrumentation coverage: `VpnFlowTest` proves the empty home disables Connect, `CONNECTED`
appears only with a session, and an error explains why nothing is protected.
`ProfileStoreTest.storedConfigIsReadableThroughDecryptionOnly` covers the exact decryption path the
backend uses.

## Not verified / deferred

- No owned WireGuard gateway, provider credentials or peer exists in this environment. A real
  external handshake, internet egress and throughput are therefore **not verified**. This slice must
  not be presented as proof that traffic is protected.
- CI emulators can exercise consent, backend failure states and UI honesty, but cannot reach a real
  peer without a gateway.
- Local accelerated emulation remains unavailable (hypervisor driver missing, software emulation
  blocked by policy). Device evidence comes from GitHub-hosted emulators.
- API35 headless software rendering is unstable around dialog teardown; API26 is the current green
  device gate. This is tracked in `docs/STATUS_BLOCKERS.md`.

## CI evidence

Appended after the first completed run for this PR, with run ID, head SHA and per-job results.
