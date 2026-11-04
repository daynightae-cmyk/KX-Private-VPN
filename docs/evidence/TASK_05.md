# TASK_05 — Security truth center

**baseline_sha:** `481ecf0` (main after TASK_00–04 merge)
**branch:** `codex/task-05-security-center`
**implementation_sha:** pending first push
**status:** IMPLEMENTED · LOCAL GATES PASS · CI/visual verification pending

## Scope implemented

- New native `SecurityCenterScreen` (Compose) reached from Home via `open-security`.
- Six reference rows kept visually faithful (Kill Switch, DNS Leak Protection, IPv6
  Protection, Auto Reconnect, Tracker Blocking, Always-On VPN) but every state is real:
  - Kill Switch / Always-On: `Settings.Secure` `always_on_vpn_app` and
    `always_on_vpn_lockdown`, cross-checked with the WireGuard backend
    `GoBackend.isLockdownEnabled`. A disagreement between the two sources is rendered as
    **Unknown**, never guessed.
  - DNS: `VpnSession.dnsServers` from the active tunnel (falling back to the real
    `ConnectivityManager` VPN link DNS). No tunnel means **Awaiting tunnel**.
  - IPv6: derives from the active tunnel `AllowedIPs` (`::/0` required). IPv4-only routes
    render **Not covered** and block any verified-protection claim.
  - Auto Reconnect and Tracker Blocking: explicitly **Unavailable**, switches disabled.
    No fake tracker list, no fake reconnection.
  - Protection headline is **Fully protected** only when tunnel + handshake + Android VPN
    link + tunnel DNS + `::/0` are all measured. Otherwise **Not protected** or
    **Tunnel up · verification pending** with the missing checks listed.
- Diagnostics card exposes measured values (state, Android link, DNS, routes, handshake,
  system lockdown, always-on package, VPN consent, measurement time) and opens the real
  Android VPN settings / security settings intents.
- No new permission. No new storage. No system setting is changed by the app.

## Changed paths

- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/security/SecurityPolicy.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/security/SecurityDiagnostics.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/SecurityScreens.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/designsystem/GlassComponents.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/NativeClient.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/ClientViewModel.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/vpn/VpnGateway.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/vpn/VpnController.kt`
- `android/app/src/main/res/values/security_strings.xml`
- `android/app/src/main/res/values-ar/security_strings.xml`
- `android/app/src/test/java/com/knoux/kxprivatevpn/SecurityPolicyTest.kt`
- `android/app/src/androidTest/java/com/knoux/kxprivatevpn/SecurityCenterTest.kt`
- `android/app/src/androidTest/java/com/knoux/kxprivatevpn/SecurityFlowTest.kt`

## Local verification (Windows, 2026-11-04)

| Gate | Result |
|---|---|
| `:app:compileDebugKotlin` | PASS |
| `:app:testDebugUnitTest` | 37 tests, 0 failures (24 existing + 13 new `SecurityPolicyTest`) |
| `:app:lintDebug` | 0 errors, 21 warnings (dependency updates / existing icon warnings) |
| `:app:assembleDebug` | PASS |
| `:app:compileDebugAndroidTestKotlin` | PASS |

## New tests

- `SecurityPolicyTest` (13): disconnected never verified; handshake/link/DNS/IPv6 barriers;
  `::/0` parsing; kill-switch requires lockdown + this app; unreadable settings stay
  unknown; lockdown source disagreement; consent does not imply protection.
- `SecurityCenterTest` (5): disconnected hero is Not protected; connected without handshake
  never shows Fully protected; verified state requires all measured evidence; tracker/auto
  remain disabled; night Arabic RTL capture.
- `SecurityFlowTest` (1): opens Security Center through the real Activity, reads actual
  Android settings/VpnService/ConnectivityManager and WireGuard backend on the emulator,
  asserts Not protected, captures a device screenshot.

## Visual evidence

Reference: `public/assets/aistudio/05-security-center_d0a1f09a.png`.
Screenshots produced by instrumentation to `identity-evidence/` and collected by
`android/scripts/verify-device.sh`:

- `20-security-center-disconnected-light.png`
- `21-security-center-connected-unverified-light.png`
- `22-security-center-verified-light.png`
- `23-security-center-verified-night-rtl.png`
- `24-security-center-device-light.png`

**VISUAL_UNVERIFIED locally** because no local emulator is usable (hypervisor policy
block). The CI API26 artifact is the screenshot gate; comparison notes are appended after
download.

## Known limits (honest)

- No owned WireGuard gateway/peer: a real external handshake still cannot be produced, so
  the app will not claim “Fully protected” in the field. The verified visual state exists
  only in controlled UI tests.
- Auto Reconnect and Tracker Blocking are intentionally not implemented and visibly
  disabled.
- API35 headless SwiftShader renderer remains unstable around dialog teardown (existing,
  documented blocker); API26 is the device gate.
- No release signing/Play Console credentials.

## PR / CI / merge

- PR: pending
- CI run: pending
- merged_sha: pending
- remote_main_sha: pending
