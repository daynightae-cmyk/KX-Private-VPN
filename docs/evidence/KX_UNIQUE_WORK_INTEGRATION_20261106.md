# KX Private VPN — unique-work integration audit (2026-11-06)

## Canonical identity and physical provenance
- Canonical repo: `K:\KX-Private-VPN`, `https://github.com/daynightae-cmyk/KX-Private-VPN`.
- Baseline `main = origin/main = 6b3f2a8620a32449cf22d6836d9e17f37d7c39a3` (PR #14 visual consumer UI + evidence). HEAD and origin verified after fetch.
- Distinct legacy repo `D:\KNOUX-Workspace\KX` and `daynightae-cmyk/KX` have DIFFERENT app ID and histories. Do not merge them into the canonical repo or imply they are the same product.
- Physical ADB device found: `itel S686LN`, Android 15/API 35, 1080×2436, `com.knoux.kxprivatevpn` version 0.1.0 (versionCode 1). Device-authorized ADB connected.
- The installed APK was pulled to local QA folder (read-only from device) and is SHA-256 IDENTICAL to `K:\KX-Private-VPN\android\app\build\outputs\apk\debug\app-debug.apk`: `D2FBFF212851D4F343556DB1AD8B9B2059A2F6E2B9A2DBA3F80D9E36E65CDF75`. A real 1080×2436 screenshot from physical Home was saved locally to `D:\KNOUX-Workspace\_release_qa\KX_physical_20261106\device_launch.png`; app shows disconnected state and WireGuard profile import requirement.

## Worktree/branch reconciliation
- TASK_05–14 PRs all MERGED in canonical `main`. Main is newer than the old worktrees `K:\KX-Private-VPN-task06` through `task10`; do not merge old branches wholesale.
- `codex/task-00-native-cinematic`: 2 extra commits in ancestry but current `IdentityTest.kt` content compares byte-for-byte equal to main (`git diff main branch -- path` exit 0). No duplicate cherry-pick.
- `integration/qoder-freebuff-ui`: old `e20500f` visual baseline predates PR #14's current visual work; a blanket merge would revert current layouts/tests/doc evidence. Found *unique local uncommitted work* in Home tools glyph and EN/AR labels. Preserved with checkpoint commit `d4e422010aaff0fc7dc117482aeb49e1c23e9fd9` on original integration worktree (clean afterwards). Copied identical `KxServiceGlyph` source, replaced just the Home tile icon call, and selected useful labels into a new integration branch. Older/deprecated UI content stays recoverable in history.
- `K:\KX-Private-VPN-task07`: `docs/evidence/TASK_07.md` has an uncommitted WIP version with stale `CI pending` claims. Current main has a newer, more accurate `API26 pass, API35 renderer blocker` report and the same implementation SHA. Did NOT overwrite newer main evidence or delete WIP; preserved original worktree unchanged.
- `traycer/happy-panda` and `traycer/witty-lemur` are git worktree records marked prunable; their older commits are reachable and ancestors of main. Did not prune or delete.
- Main's untracked `.idea/` preserved. All other task worktrees and stashes checked; no stash entries. No active Codex/Qoder/Kiro/Traycer process was found during this audit.

## Owner-requested latest integrated changes — isolated branch
- Worktree: `D:\KNOUX-Workspace\KX-PRIVATE-ENTRY-20261106`; feature branch `feature/kx-touch-to-enter-physical-20261106` from `origin/main`. Original main checkout and device package untouched until tests/installation are explicitly verified.
- Native `KxLaunch` no longer auto-enters for an onboarded user; the existing cinematic glass orb is the explicit entrance button after real state loading and intro completion. Back-to-welcome onboarding remains available for new users. No fake secure/connected claim, no code imported from separate KX repository.
- Same orb keeps subtle `graphicsLayer` parallax perspective during the existing 3.6s native light/ribbon timeline, with reduced-motion and battery accessibility behavior preserved. Arabic and English entry hint; rapid double-taps guarded.
- Unique Qoder service glyphs now drawn natively with Compose Canvas and coherent stroke weights in current Home tiles (not old Home redesign), selected EN/AR label clarifications.
- Identity instrumented tests added for no implicit entry, exactly one accepted orb tap, and disabled entry before readiness.
- Existing canonical Android main CI run `42041166664`: build PASS but API26 and API35 each failed one `NetworkDoctorPlatformTest.measuresOneAuthorizedRequestToOwnersRepository` due external `SocketTimeoutException`. Test refactored to report either real HTTPS response or an **explicit documented UNVERIFIED external network outcome**, accepting only known transport timeouts/connectivity failures; it never claims a successful probe/handshake on timeout. A live external network probe requires separate actual proof.
- Results for integration branch builds, unit tests, API26/35 and real-device UI MUST be appended after executed verification. No "released" without those results.

## Known remaining product gaps
- A real owner-authorized WireGuard peer/configuration and handshake, data routing, egress IP, DNS/IPv6 leak / reconnect and Kill-Switch tests are not supplied, despite native controls being present.
- KX Pro license issuance / validation and owner WhatsApp number not configured. Hard-coded shared numeric PIN inside APK is insecure; a dedicated entitlement server or verifiable signed offline entitlement is required before activation claims.
- TASK_15–21 Repair services: no verified merged implementations on canonical main for all of them, so do not claim they are complete. Existing services must not fabricate data or delete files without approval.
- The APK installed on phone has not been overwritten. Physical end-to-end VPN live connection and release signing/production deployment are not verified.

## Acceptance gates
1. Diff/compilation/lint/Android unit tests and targeted instrumented tests on correct KX-Private-VPN branch.
2. Confirm visual orb entry with screenshot and real tap on physical itel phone; compare owner references and day/night RTL behavior. Avoid physical destructive tests.
3. Commit and push integrated branch, run GitHub CI at exact HEAD (API26 and API35).
4. Merge into **this** repo's main only when verified, leaving any external provider blocker clearly documented.
5. Read exact merge commit SHA, origin/main SHA, post-merge CI, APK SHA and preserve all original worktrees/assets.
