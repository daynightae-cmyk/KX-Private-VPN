# Full Repository Audit — 2026-11-04 (post-merge)

## 1. Merge status

- **Merged branch:** `codex/task-02-vpn-transport` (head `fb8f8b3`) into `main` via merge commit `481ecf0`.
- **Base main SHA before merge:** `37c5d87` (style: update AI studio asset thumbnails).
- **Branches preserved (no deletion):** `main` (`481ecf0`), `feature/visual-overhaul-and-missing-tasks` (`2ca10bb`), `codex/task-00-native-cinematic` (`4879c81`), `codex/native-onboarding-config` (`f68eb11`), `codex/task-02-vpn-transport` (`fb8f8b3`).
- **No destructive git operation performed:** no `reset --hard`, no `clean`, no `force push`. All untracked `docs/evidence/ci-*` directories preserved.

## 2. Build / compile / test / lint audit

| Gate | Command | Result |
|---|---|---|
| Kotlin compile | `gradlew :app:compileDebugKotlin` | PASS (exit 0) |
| Unit tests | `gradlew :app:testDebugUnitTest` | 24 tests, 0 failures, 0 skipped |
| Lint | `gradlew :app:lintDebug` | 0 errors (17 warnings: dependency updates, plural candidate, unused string, launcher icon shape) |
| Assemble | `gradlew :app:assembleDebug` | PASS; APK at `android/app/build/outputs/apk/debug/app-debug.apk` |
| AndroidTest compile | `gradlew :app:compileDebugAndroidTestKotlin` | PASS |

### Unit test suites (24 total)

| Suite | Tests | Failures |
|---|---|---|
| `CacheMaintenanceTest` | 3 | 0 |
| `ConfigValidatorTest` | 4 | 0 |
| `IntroTimelineTest` | 3 | 0 |
| `NetworkDoctorTest` | 2 | 0 |
| `VpnControllerTest` | 12 | 0 |

## 3. CI audit (GitHub Actions — `android.yml`)

| PR | Branch | Run ID | Build | API26 device | API35 device |
|---|---|---|---|---|---|
| #1 | `codex/task-00-native-cinematic` | `41676071493` | PASS | PASS | FAIL (emulator renderer loss) |
| #1 | `codex/task-00-native-cinematic` | `41714494223` | PASS | PASS | FAIL (same blocker) |
| #1 | `codex/task-00-native-cinematic` | `41718225204` | PASS | PASS | FAIL (same blocker) |
| #2 | `codex/native-onboarding-config` | `41677500831` | PASS | PASS | FAIL (same blocker) |
| #2 | `codex/native-onboarding-config` | `41714440885` | PASS | PASS | FAIL (same blocker) |
| #2 | `codex/native-onboarding-config` | `41718056011` | PASS | PASS | FAIL (same blocker) |
| #3 | `codex/task-02-vpn-transport` | `41718273756` | PASS | PASS (19/19) | FAIL (same blocker) |
| #2 | `codex/native-onboarding-config` | `41719440729` | PASS | PASS | FAIL (same blocker) |

**API35 blocker (documented, not hidden):** Headless SwiftShader renderer loses its ColorBuffer when a separate dialog window is created/destroyed (`arabicRtlLargeText360dp`). API26 is the reliable device gate. The test `captureWindow` skips dialog capture on SDK ≥35 and captures the Arabic screen before the dialog opens. The emulator death is reproducible across 8 runs. This is an environment/renderer limitation, not an app bug — all 6 IdentityTest cases pass on API26 and all VPN tests pass on API26.

## 4. Manifest / native library audit

- `AndroidManifest.xml` (merged): contains `com.wireguard.android.backend.GoBackend$VpnService` with `android.permission.BIND_VPN_SERVICE` and `android.net.VpnService` intent filter. The old `tools:node="remove"` was deleted.
- APK native libs (verified by jar listing): `libwg-go.so`, `libwg-quick.so`, `libwg.so` present for `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`.
- Permissions declared: `ACCESS_NETWORK_STATE`, `INTERNET`. No `POST_NOTIFICATIONS` yet (deferred — GoBackend FGS notification suppressed on API 33+ without it, but tunnel still runs).

## 5. Code inventory (all Kotlin, no WebView, no simulated VPN)

### Main source (35 files)

| Package | Files | Purpose |
|---|---|---|
| `data.config` | `ConfigInput`, `ConfigValidator`, `ProfileStore` | Keystore-encrypted profile storage, WireGuard config parsing |
| `designsystem` | `CrystalIdentity` | Glass orb, `GlassAction`, brand components |
| `features` | `NativeClient`, `ClientViewModel`, `CacheScreen`, `NetworkScreen`, `VpnScreens` | All Compose screens, state management |
| `launch` | `KxLaunch`, `IntroTimeline`, `LaunchPreferences`, `MotionPolicy` | Cinematic splash, onboarding, persistence |
| `platform.network` | `NetworkDoctor` | Real connectivity probes |
| `platform.storage` | `CacheMaintenance` | Scoped cache preview/confirm/remove |
| `platform.vpn` | `VpnController`, `VpnGateway` (`WireGuardVpnGateway`), `VpnLinkReader`, `VpnModels` | Real WireGuard transport via official `GoBackend` |

### Test source (17 files)

| Suite | Tests | Focus |
|---|---|---|
| JVM (5 suites) | 24 | Config validation, cache, intro timeline, network doctor, VPN controller state machine |
| Instrumentation (10 suites) | ~20 | Identity, client flow, config import, profile store, network doctor, startup, cache, VPN flow |

### VPN transport (TASK_02/04) — real implementation, no simulation

- `VpnController`: emits `IDLE`/`CONNECTING`/`CONNECTED`/`DISCONNECTING`/`ERROR`. `CONNECTED` only after `GoBackend.setState(UP)` returns `UP`. Errors: `NO_PROFILE`, `CONSENT_DENIED`, `CONFIG_INVALID`, `CONFIG_UNREADABLE`, `AUTHORIZATION`, `START_FAILED`, `STOP_FAILED`, `BACKEND_STOPPED`.
- `WireGuardVpnGateway`: wraps `GoBackend`; reads real `Statistics` (RX/TX, latest handshake); `isUp()` confirms backend state; `stop()` waits for backend.
- `VpnLinkReader`: reads real `ConnectivityManager` VPN transport (`TRANSPORT_VPN`) — interface, addresses, DNS, routes, MTU.
- `ClientViewModel`: consent flow via `VpnService.prepare()`; session hint persisted in `DataStore`; restore only when backend confirms `UP`.
- `VpnScreens`: `VpnHomePanel` (empty/connected/loading/error), `VpnSessionScreen` (session details + Android link + config scope); all state uses `testTag`.
- `VpnFlowTest`: 4 UI truth tests (empty home disables connect, CONNECTED requires session, error explains, session screen shows measured values + absent link).
- `VpnControllerTest`: 12 unit tests with fake gateway (connect session, invalid/unreadable config, authorization failure, start failure, counters, unexpected stop, disconnect, failed stop, restore, no-profile/denied, duplicate guard).

## 6. Task completion status

| Task | Status | Evidence |
|---|---|---|
| TASK_00 | Partially verified | API26 7/7 pass; visual unverified; API35 renderer blocker |
| TASK_01 | Implemented | Welcome/onboarding routing; API26 verified |
| TASK_02 | Implemented (transport real) | `VpnController` + `WireGuardVpnGateway`; 12 unit tests + 4 UI tests |
| TASK_03 | Implemented | Profile selection/removal; `VpnFlowTest` connected state |
| TASK_04 | Implemented | `VpnSessionScreen` with real counters/handshake/link; 1 UI test |
| TASK_05–08 | NOT IMPLEMENTED | Pending |
| TASK_09 | Implemented | Keystore-encrypted import (file/QR/manual/clipboard); API26 verified |
| TASK_10 | Implemented | Locale/theme persistence; API26 verified |
| TASK_11–12 | Partially implemented | Basic StatFs/cache; full dashboard NOT IMPLEMENTED |
| TASK_13–15 | NOT IMPLEMENTED | Pending |
| TASK_16 | Implemented | Scoped cache preview/confirm/remove/recheck/journal; API26 verified |
| TASK_17–19 | NOT IMPLEMENTED | Pending |
| TASK_20 | Implemented | Android network snapshot + consented HTTPS timing; API26 verified |
| TASK_21 | NOT IMPLEMENTED | Pending |

## 7. Known blockers (honest, documented)

1. **No owned WireGuard gateway/server/peer** — actual handshake, egress, throughput NOT verified. `VpnController` never emits `CONNECTED` falsely.
2. **API35 headless SwiftShader renderer loss** — separate dialog window creation/teardown kills emulator (`arabicRtlLargeText360dp`). API26 is the reliable device gate. 8 runs reproduced.
3. **Visual comparison** — no side-by-side comparison against `public/assets/aistudio/02-home-disconnected_*.png` or `04-connected-session_*.png` performed. VISUAL_UNVERIFIED per contract.
4. **No release signing / Play Console credentials** — deferred.
5. **No `POST_NOTIFICATIONS` permission** — GoBackend FGS notification suppressed on API 33+; tunnel still runs.

## 8. Protected state (no destructive operations performed)

- `main`: `481ecf0` (merged)
- `feature/visual-overhaul-and-missing-tasks`: `2ca10bb`
- `codex/task-00-native-cinematic`: `4879c81`
- `codex/native-onboarding-config`: `f68eb11`
- `codex/task-02-vpn-transport`: `fb8f8b3`
- `docs/evidence/ci-*`: 13 directories preserved
- `public/assets/aistudio/`: 11 reference PNGs preserved
- No `reset --hard`, no `clean`, no `force push` performed

## 9. Reproducible verification commands

```powershell
cd K:\KX-Private-VPN\android
.\gradlew.bat :app:compileDebugKotlin --console=plain -q          # PASS
.\gradlew.bat :app:testDebugUnitTest --console=plain -q             # 24 tests, 0 failures
.\gradlew.bat :app:lintDebug --console=plain -q                    # 0 errors
.\gradlew.bat :app:assembleDebug --console=plain -q                 # PASS
.\gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain -q # PASS
```
