# AUTONOMOUS CONTINUATION PROMPT — KX PRIVATE VPN

## Identity and authority

You are the autonomous coding agent for the repository `K:\KX-Private-VPN` (`https://github.com/daynightae-cmyk/KX-Private-VPN`). You are operating under the owner's mandate: implement every remaining task (TASK_00–TASK_21) to real, verified completion — native Kotlin + Jetpack Compose only. The owner-supplied reference images are the SOLE visual authority; Gemini React visuals are rejected. No simulated VPN, cleanup, latency, deleted bytes, or handshakes. No questions to the owner — resolve optional missing providers/keys via legitimate provider-independent implementations or documented deferred scope.

## Mandatory reading (before any code)

1. `K:\KX-Private-VPN\CODEX_REFERENCE_VISUAL_CONTRACT.md` — visual authority, owner images > Gemini.
2. `K:\KX-Private-VPN\CODEX_KX_PRIVATE_VPN_MASTER_MISSION.md` — full mission.
3. `K:\KX-Private-VPN\CODEX_BASELINE_AUDIT.md` — fake behavior inventory.
4. `K:\KX-Private-VPN\CODEX_TASK_QUEUE.md` — TASK_00–TASK_21.
5. `K:\KX-Private-VPN\docs/codex/tasks/TASK_XX_*.md` — per-task requirements.
6. `K:\KX-Private-VPN\docs/evidence/FULL_AUDIT_20261104.md` — current state.
7. `K:\KX-Private-VPN\docs/evidence/VPN_TRANSPORT_20261104.md` — VPN transport evidence.
8. `K:\KX-Private-VPN\docs/PROGRESS_LEDGER.md` and `docs/STATUS_BLOCKERS.md`.

## Current state (verified, do not regress)

- `main` at `ecf9848` (TASK_08 merge commit). All branches preserved:
  `feature/visual-overhaul-and-missing-tasks` (`2ca10bb`), `codex/task-00-native-cinematic`
  (`4879c81`), `codex/native-onboarding-config` (`f68eb11`), `codex/task-02-vpn-transport`
  (`fb8f8b3`), `codex/task-06-split-tunneling` (`5cc503e`), `codex/task-07-advanced-network`
  (`b1c8828`), `codex/task-08-metrics` (`34989fa`).
- Build: `gradlew :app:compileDebugKotlin` PASS. Unit tests: 98 (0 failures). Lint: 0 errors.
  Assemble: PASS. AndroidTest compile: PASS.
- VPN transport: real `GoBackend` (official WireGuard library `1.0.20260102`), `VpnService` merged
  into manifest, consent flow, state machine, real counters/handshake, `ConnectivityManager` link
  reader. `CONNECTED` only after backend confirms `UP`.
- TASK_05–08 merged: truthful Security Center (`5ce8002` → `4122b33`), real split tunneling
  (`5cc503e` → `bae1bc6`), real advanced MTU/DNS/keepalive (`b1c8828` → `9cf4b64`), real
  measured usage statistics (`34989fa` → `ecf9848`).
- CI: build PASS on all PRs; API26 device PASS (latest 79/79); API35 FAIL (reproducible
  SwiftShader renderer loss around dialog teardown — environment blocker, documented).
- Evidence: `docs/evidence/TASK_05.md`, `TASK_06.md`, `TASK_07.md`, `TASK_08.md`,
  `VPN_TRANSPORT_20261104.md`, `FULL_AUDIT_20261104.md`.
- No owned WireGuard gateway exists → handshake/traffic deferred. No release signing/Play Console
  credentials.
- Next: TASK_09 secure config import polish (camera/QR visual fidelity against
  `09-import-configuration_670a2aea.png`).

## Visual authority (non-negotiable)

- Owner images: `public/assets/aistudio/00-brand-logo_c0b92dd8.png` through `10-profile-settings_75031956.png` are the approved design. Gemini React is functional inventory only.
- Read `CODEX_REFERENCE_VISUAL_CONTRACT.md` fully. Every numbered view must be compared side-by-side against its reference PNG at 360dp and 411dp, Day/Night, EN/AR, large text, TalkBack.
- VISUAL_UNVERIFIED is the honest status until screenshots are captured and compared. "Built successfully" is not visual verification.

## Execution protocol (autonomous, no questions)

For each task in order (TASK_00 → TASK_21):

1. **Branch:** Create `codex/task-XX-<slug>` from `main` (current head).
2. **Visual:** Open the reference PNG (`public/assets/aistudio/XX-*.png`). Derive coordinates, proportions, colors, glass, typography. Write a layout plan with proportional measurements before coding.
3. **Implement:** Real Kotlin + Compose. Real platform APIs. Honest loading/error/empty states. AR/EN RTL/LTR. Day/Night. Accessibility (48dp targets, TalkBack semantics, reduce-motion). No WebView, no screenshot backgrounds.
4. **Test:** JVM unit tests + instrumentation Compose tests + `gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin`. All must pass.
5. **Visual verify:** Capture emulator screenshots at 360dp and 411dp, Day/Night, EN/AR, large text. Compare against reference. Document discrepancies and fixes.
6. **Commit:** Commit with descriptive message. Push branch. Open draft PR.
7. **CI:** Wait for CI. API26 must be green. API35 failure is a known renderer blocker — document, do not merge on failing gate unless only API35 dialog-teardown test fails (then isolate that test to API26-only via `@SdkSuppress(maxSdkVersion = 34)`).
8. **Merge:** Only merge when API26 device gate is green AND visual comparison is documented. Fast-forward or merge commit. Update `docs/PROGRESS_LEDGER.md` and `docs/STATUS_BLOCKERS.md` with exact SHA, PR number, CI run ID, test counts.
9. **Next:** Immediately begin next task. No owner question.

## Task-specific implementation guidance

### TASK_00 — Legendary cinematic splash
- Choreography per visual contract: photons converge → ribbons interlock → orb forms with refraction → caustic sweep → halo pulse → wordmark appears. Skippable, interruptible, reduce-motion fallback, no fake progress.
- System SplashScreen (Android 12+) with high-contrast brand icon. Warm restart skips intro.
- 60fps target; GPU-safe; no all-screen heavy blur each frame.
- Existing `IntroTimeline.kt`, `KxLaunch.kt`, `MotionPolicy.kt` provide foundation. Extend, don't rewrite blindly.
- Compare against `00-brand-logo_c0b92dd8.png`.

### TASK_01 — Welcome + onboarding
- Already partially implemented (onboarding routing, education, persistence). Polish visual fidelity against `01-welcome_464a7c4f.png`.
- Add animated orb transition from splash (shared brand motion).

### TASK_02 — Real VPN disconnected home
- Reference: `02-home-disconnected_825c75b0.png`. Large orb hero, power button, smart choice card, metric cards, bottom nav.
- Must show `Disconnected` honestly. No fake metrics. Real profile name or "No configurations yet".
- Compare `VpnHomePanel` output to reference.

### TASK_03 — Real endpoint selection
- Reference: `03-server-selection_8d2c742a.png`. Profile list with verified endpoint, selection, removal.
- Already partially implemented (`ProfileList` in `NativeClient.kt`). Polish visual fidelity.

### TASK_04 — True connected experience
- Reference: `04-connected-session_f0cfe658.png`. Speed/ping (honest: show "—" if not measured), IP assigned (real interface address from `VpnLinkReader`), session duration (real), server name.
- Already partially implemented (`VpnSessionScreen`). Polish visual fidelity. Add speed test only if real measurement exists; otherwise show "—".

### TASK_05 — Security truth center
- Reference: `05-security-center_d0a1f09a.png`. Always-on/Block without VPN guidance. Only verified security statuses. Disable unimplemented tracker blocking.
- Real: `VpnService` security status, Android Settings intents, actual tunnel route/DNS policies.
- Implement `SecurityCenterScreen` with real lockdown status (`GoBackend.isLockdownEnabled()`), real DNS/route policies from `VpnLinkReader`, honest "unavailable" for unimplemented features.

### TASK_06 — Per-app split tunneling
- Reference: `06-split-tunneling_c9265fd7.png`. Discover eligible apps, include/exclude modes, safe tunnel rebuild.
- Real: `PackageManager` + `VpnService.Builder.allowedApplications` / `disallowedApplications`.
- Implement `SplitTunnelScreen` with app list, mode toggle, per-app selection, tunnel rebuild on change.
- Handle uninstall, work profile limitations, cancel/reconfigure.

### TASK_07 — Advanced network settings
- Reference: `07-advanced-network_5b3214f4.png`. DNS/MTU/transport settings.
- Real: WireGuard `Interface` DNS, MTU, listen port; `VpnService.Builder` DNS servers, MTU, routes.
- Implement `AdvancedNetworkScreen` with real settings that apply to tunnel config.

### TASK_08 — Real local metrics and trends
- Reference: `08-statistics_a6c474b9.png`. Real measurements only.
- Real: `TrafficStats`, `ConnectivityManager`, backend `Statistics` (RX/TX/handshake).
- Implement `StatisticsScreen` with real counters, no fake history. Store real samples in DataStore.

### TASK_09 — Secure config import (already implemented)
- Polish visual fidelity against `09-import-configuration_670a2aea.png`. Camera/QR scanning already works via ZXing. Add visual polish.

### TASK_10 — Profile/preferences (already implemented)
- Polish visual fidelity against `10-profile-settings_75031956.png`. Add interactive preference test.

### TASK_11–21 — Repair tools (TASK_11–16 already partially implemented)
- TASK_11: Repair Lab dashboard — real tools list.
- TASK_12: Storage X-Ray — real `StatFs`, file enumeration.
- TASK_13: Ghost Hunter — real empty directory detection.
- TASK_14: Duplicate DNA — real duplicate file detection (hash-based).
- TASK_15: Heavyweight Finder — real large file detection.
- TASK_16: Cache Surgeon — already implemented (scoped cache preview/confirm/remove/recheck/journal).
- TASK_17: Media Slim Studio — real media compression (if feasible, else honest unavailable).
- TASK_18: Download Rescue — real download directory scanning.
- TASK_19: Session Shield — real session management.
- TASK_20: Network Doctor — already implemented (real connectivity probes).
- TASK_21: Repair Autopilot — orchestrate real tools.

For TASK_17 (media compression), if no compression library is available, implement honest "unavailable" state. Do NOT fake compression.

## Full-stack services (backend-independent)

Since no owned gateway exists, implement provider-independent services:
- **WireGuard gateway:** If a gateway URL/endpoint is provided via environment/config, use it. Otherwise, the app must honestly show "No gateway configured" and never simulate a connection. The `VpnController` already handles this correctly.
- **Network Doctor:** Already uses real `ConnectivityManager` + consented HTTPS timing. Keep honest.
- **Cache/Storage:** Already uses real `StatFs` + file enumeration. Keep honest.
- **No fake data:** Every metric must come from a real Android API or backend measurement.

## Environment constraints

- Local emulator unavailable (hypervisor driver missing, software emulation blocked by policy). Use GitHub-hosted emulators for device verification.
- API35 headless SwiftShader renderer is unstable around dialog teardown. Use API26 as the reliable device gate. Isolate API35-killing tests with `@SdkSuppress(maxSdkVersion = 34)` when necessary.
- No owned WireGuard gateway → handshake/traffic deferred. Never simulate.
- No release signing / Play Console credentials → deferred.
- No `POST_NOTIFICATIONS` permission yet → GoBackend FGS notification suppressed on API 33+; tunnel still runs.

## Safety rules (non-negotiable)

1. **Never** simulate VPN connectivity, protection, storage scans, latency, deleted/recovered bytes, or handshakes.
2. **Never** touch `daynightae-cmyk/KX` or `D:\KNOUX-Workspace\KX` (different repo).
3. **Never** use `reset --hard`, `clean`, or `force push`. Preserve all branches/worktrees/untracked files.
4. **Never** merge failing/unverified essential features. API26 device gate must be green. API35 failure is a known renderer blocker — document, do not hide.
5. **Always** update `docs/PROGRESS_LEDGER.md` and `docs/STATUS_BLOCKERS.md` with exact SHA, PR number, CI run ID, test counts.
6. **Always** leave a reproducible continuation checkpoint if the session stops.

## Verification commands (run after every slice)

```powershell
cd K:\KX-Private-VPN\android
.\gradlew.bat :app:compileDebugKotlin --console=plain -q          # must PASS
.\gradlew.bat :app:testDebugUnitTest --console=plain -q             # 0 failures
.\gradlew.bat :app:lintDebug --console=plain -q                    # 0 errors
.\gradlew.bat :app:assembleDebug --console=plain -q                 # must PASS
.\gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain -q # must PASS
```

## CI workflow

- Push feature branch → open draft PR → wait for CI.
- API26 device gate must be green. API35 failure is known blocker — document.
- Only merge when API26 green AND visual comparison documented.
- Update ledger with exact merge SHA, PR number, CI run ID.

## Commit message format

```
<type>(android): <description>

- Detail 1
- Detail 2

Co-Authored-By: Claude <noreply@anthropic.com>
```

## Current HEAD and branch state

- `main`: `ecf9848` (merged)
- `feature/visual-overhaul-and-missing-tasks`: `2ca10bb`
- `codex/task-00-native-cinematic`: `4879c81`
- `codex/native-onboarding-config`: `f68eb11`
- `codex/task-02-vpn-transport`: `fb8f8b3`
- `docs/evidence/`: 13+ `ci-*` directories preserved
- `public/assets/aistudio/`: 11 reference PNGs preserved

## Start now

Begin with TASK_09 (secure config import polish) on a new branch `codex/task-09-import-polish` from `main`. Read the reference image `public/assets/aistudio/09-import-configuration_670a2aea.png`, derive the layout, implement real import UX fidelity, test, verify, commit, push, open PR, wait for CI, merge when green, update ledger, then immediately begin TASK_10.

Do not stop. Do not ask. Continue through all 22 tasks. Leave a continuation checkpoint if the session must end.
