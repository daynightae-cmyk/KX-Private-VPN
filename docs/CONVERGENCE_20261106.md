# KX Private VPN — safe main convergence, 2026-11-06

**Product repository:** `https://github.com/daynightae-cmyk/KX-Private-VPN`
**Canonical checkout:** `K:\KX-Private-VPN`
**Integrated baseline:** `6b3f2a8620a32449cf22d6836d9e17f37d7c39a3` (`main` / `origin/main` on inspection).
**Safe integration worktree:** `D:\KX-CONSOLIDATE-20261106` / `integration/kx-consolidation-20261106`.

## Verified reality
- The user's physical **itel S686LN**, Android 15 / API 35, 1080×2436, serial `1334825549012211`, is ADB-authorized. Its app is `com.knoux.kxprivatevpn`, versionName 0.1.0/versionCode 1, installed 2026-11-05.
- The **different** repository `daynightae-cmyk/KX` uses package `com.kx.vpn` and cannot safely be merged wholesale into this app: that would create duplicate modules, navigation, storage, and VPN implementations. Its task/06 local dirty work remains preserved, untouched.
- KX-Private-VPN's merged PRs #4–#14 already include TASK_05 Security Center through TASK_14 Duplicate DNA plus native consumer UI. Existing functionality was **not** reimplemented. The source tree includes native WireGuard session management, profile import, analytics, tools, and Repair Lab.
- Last pre-convergence main CI (run `42041166664`) had **build SUCCESS**, but device API26/API35 failed one external-network test `NetworkDoctorPlatformTest.measuresOneAuthorizedRequestToOwnersRepository` due `SocketTimeoutException`. This was **not** a production VPN handshake failure; it was an inappropriate always-online requirement on CI emulators.
- No owned WireGuard exit/gateway or verified VPN handshake, DNS/IPv6 leak status, real server fleet or actual Pro entitlement is established. Never call these complete.

## Unique work audited and selected
| Source | Unique value | Safe action |
| --- | --- | --- |
| `D:\KX-INTEGRATION-20261105\wt`, `integration/qoder-freebuff-ui` | New `ServiceGlyph.kt` native icon family and refined EN/AR tool copy, preservation commit `d4e4220` | Cherry-picked cleanly on latest main as `267565d`, without reintroducing older, superseded Qoder UI tree |
| `D:\KNOUX-Workspace\KX-PRIVATE-ENTRY-20261106`, uncommitted | Owner-requested glass orb tap-to-enter, 3D intro, bilingual hint and entry test | Surgically ported without changing original working set |
| Same entry worktree | Measured Network Doctor probe outcome; no fabricated network success | Ported; when outbound HTTPS times out, write honest diagnostic and mark external evidence SKIPPED, not PASS |
| Same entry worktree | More exact privacy scope for Storage X-Ray and honest missing-service descriptions | Ported relevant AR/EN descriptions, kept explicit ready count |
| `K:\KX-Private-VPN-task07`, dirty `docs/evidence/TASK_07.md` | Older implementation SHA/status wording | Protected in patch; **do not** overwrite already merged main ledger with obsolete 'merge pending' text |
| `codex/task-00-native-cinematic` | Older dialog screenshot test changes | Compared against current main; latest version contains the material behavior, no mass replay |
| `traycer/*` removed worktree paths | Worktrees are prunable but refs/history still exist | Neither pruned nor deleted; preservation rule |
| `daynightae-cmyk/KX` task/06 dirty | Independent implementation under different package | Preserve separately, no destructive repo-to-repo merge |

## Preservation
Read-only snapshots: `D:\KX-PRESERVE-20261106\entry.patch`, `entry-latest.patch`, `alt-kx.patch`, `task07.patch`, `QODER-ServiceGlyph.kt`.
The phone's installed, debug-signed APK was pulled safely to `D:\KX-PRESERVE-20261106\phone-kxprivatevpn-before.apk`, SHA-256 `D2FBFF212851D4F343556DB1AD8B9B2059A2F6E2B9A2DBA3F80D9E36E65CDF75`. No app data, branches, stash, worktrees, or files were wiped.

Installed APK **signing certificate SHA-256** `c07168d455a86f8c792ef44ebf213ab63ec327226589692bcad69231e67c085e`.
Convergence build APK same signing certificate SHA-256 verified. Package ID identical `com.knoux.kxprivatevpn`, enabling an in-place update **without uninstall or data wipe** when device smoke is authorized.

## Verification ledger (do not overclaim)
- Initial convergence before test-suite gate updates: `gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin` => `BUILD SUCCESSFUL` (7m06s), JVM **148/148, 0 failures, 0 skipped**; run log `D:\KX-PRESERVE-20261106\consolidation-build.log`.
- Full-activity instrumentation tests initially assumed Home opened automatically. Added a human-like tap to all **11** MainActivity launch sites, plus `KxEntryTestGate.kt` asserting Home does not appear before a tap. **These edits require their own exact-SHA CI compile/execution.**
- A subsequent isolated local incremental compilation encountered a **Gradle UDP FileLockContentionHandler `BindException: Address already in use`** before compilation; not a code compile result. Do not count it as PASS or code failure.
- PR [#16](https://github.com/daynightae-cmyk/KX-Private-VPN/pull/16) first CI on `9bbaf53`: build/lint/units PASS (run `42116165059`); API26 started **136 tests**, finished with **one actual failure** in `RepairLabScreenTest.journalShowsOnlyRecordedOperationsAndAnHonestEmptyState`, because the new honest label "Cache cleanup" appears in three semantics nodes and an unscoped text selector demanded exactly one. Corrected by tagging and asserting the journal action itself. API35 failed **before tests** with `No connected devices` (emulator offline). Neither result is misreported as PASS; CI on the corrected exact SHA is still required.
- Physical in-place install: `adb install -r` returned `Success` on itel S686LN / Android 15 API35. APK package unchanged, signing certificate SHA-256 matched exactly, no uninstall or clear-data command used. The built APK hash was `E48241730266579892326884336C0BEF9D1B7E84F9898689E53DBF84F3A65895`. ADB `am start -W` reported `LaunchState: COLD`.
- Real-device human gate: screenshot after **6 seconds without touching the screen** still showed the KX glass orb and "اضغط على شعار KX للدخول", not Home. ADB `input tap 540 650` then displayed the original Arabic disconnected Home. Checked sampled crash buffer with no KX crash lines. Images: [before Home](evidence/convergence-20261106-public/01-before-home.png), [cold splash gate](evidence/convergence-20261106-public/02-after-splash-gate.png), [Home after tap](evidence/convergence-20261106-public/03-after-orb-entry-home.png).
- Physical device **not** used for the full instrumentation suite (could modify user-owned profiles and settings). VPN live handshake, DNS/leaks, automated reconnect and Pro activation remain unverified and blocked.

## Remaining release gates
1. Green CI on integrated exact SHA: Android build/lint/units, emulator API26 and API35. Any external-network checks must show authentic pass or explicit SKIPPED evidence.
2. In-place update and cold splash/manual entry/Home smoke were observed on the actual phone. Remaining physical checks: full service navigation, AR/EN/RTL, day/night and detailed crash/performance telemetry. Never run the destructive whole test suite on the personal phone.
3. Final integrate only after checks to main through a reviewed PR, and verify the new `origin/main` SHA and post-merge CI.
4. Core VPN release remains network-unverified until an owner-controlled WireGuard endpoint supplies real handshake, routing, DNS, leak and reconnect evidence.
5. Pro activation by WhatsApp still requires the **owner's WhatsApp number** and a separate authenticated licensing/entitlement backend. No global shared PIN in APK and no claims of active Pro services.

**Evidence semantics:** COMMITTED ≠ CI PASS ≠ DEVICE VERIFIED ≠ VPN NETWORK VERIFIED ≠ RELEASED.
