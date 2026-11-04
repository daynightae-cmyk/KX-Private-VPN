# TASK_08 — Statistics native Android evidence and unresolved acceptance

Updated: 2026-11-05 (Asia/Dubai)

## Reality / integration
- Repository: daynightae-cmyk/KX-Private-VPN (not the separate KX project).
- Source baseline: main `07695239201e74492e247275a56a11338cbd65de`.
- Feature branch: `codex/task-08-metrics`, PR #7.
- Implementation baseline before CI troubleshooting: `9ab88bf3cf56ce6041d2df8dae3c66f84814ffb8`.
- Trial fix `fe97100` avoided API35 identity screenshot readback, but did NOT resolve CI. Reverted non-destructively by `fefca9941236f61c746a6442a4775437d65e1961`. Original test assertions and capture logic restored.
- Status: **IMPLEMENTED, PARTIALLY VERIFIED, NOT MERGED / NOT RELEASED**. Do not close until exact-SHA CI and visual/field gates are satisfied.

## Actual implementation
- `UsageRecorder` obtains connected-session counters from real `VpnState` (WireGuard backend), measures duration using monotonic elapsed time and checkpoints in storage; recovered sessions are labeled, not silently invented.
- `UsageStatsStore`, `UsageRecord`, and `UsageAggregator` provide private persistence, rollups and filter periods. Clear-history behavior rebases a live measurement, rather than replaying already-cleared bytes.
- `StatisticsScreen` is a real Compose view with stats cards, range selection, chart, time/history, clear confirmation, loading/error/empty states, navigation, Arabic/English, light/dark, and accessibility tests. No random generated traffic, latency, or privacy score is presented as observed data.
- Files changed from source baseline: 17 Android files, including `StatisticsScreen.kt`, `UsageRecorder.kt`, `UsageAggregator.kt`, `UsageStatsStore.kt`, three JVM test classes, and instrumentation tests.

## Test evidence
- Local: `gradlew.bat :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest --offline --no-daemon --console=plain` succeeded on `fe97100`; 98 JVM test cases, zero failures/errors in XML reports. Compilation alone is not device behavior.
- CI run 41921214902, SHA `9ab88bf`: build SUCCESS, API26 SUCCESS, API35 FAILURE. API35 reported `IdentityTest.arabicRtlLargeText360dp` and emulator disconnected after 22/79 test starts.
- CI run 41922942888, SHA `fe97100`: build SUCCESS, API26 SUCCESS, API35 FAILURE. API35 also reported `AdvancedNetworkScreenTest.nightArabicLayoutCapturesRtlEvidence` Compose timeout; identity test then failed and the emulator disappeared. **The trial fix was ineffective and was reverted.**
- Latest pushed `fefca99` is semantically restored to the pre-trial test code; its own CI result must be checked separately. Never mark API35 PASS by inference.
- Explicit CI links: https://github.com/daynightae-cmyk/KX-Private-VPN/actions/runs/41921214902 and https://github.com/daynightae-cmyk/KX-Private-VPN/actions/runs/41922942888
- PR: https://github.com/daynightae-cmyk/KX-Private-VPN/pull/7

## Visual evidence and limitations
- Canonical owner/repository reference: `public/assets/aistudio/08-statistics_a6c474b9.png` (SHA256 `A6C474B99A3C55BB9E4B6DF265FC6CE3C89CA7B265302E53A0CB6B256B9852D0`).
- API26 emulator captures under `D:\Temp\task08-run3-26\device-screenshots\identity-evidence\`: `36-statistics-defaults-light.png` through `42-statistics-30d-range.png`, including clear confirmation, dark Arabic and large text. These are LOCAL paths, not published artifacts in the repository.
- Visual comparison: native screen preserves core chart/card/navigation hierarchy, but the current header orb/wordmark size, glass optical depth, multicolor refraction and luminous ornaments do **not** yet match the premium visual reference. Sample values in reference are NOT shown as live metrics.
- `VISUAL_UNVERIFIED` on API35 real hardware; the headless CI emulator is unstable around Compose UI/readback/dialogs. Do not suppress assertions or omit visual proof and then declare full acceptance.

## External/field gates
- An owned WireGuard gateway, controlled handshake and known traffic generation remain necessary to verify recorded bytes/duration against real egress; backend-derived counters are implemented, not field-proven here.
- API35 requires stable emulator/hardware or separately proven renderer remedy; identify test vs emulator failure without weakening assertions. Verify RTL, 360/411dp, large fonts, reduced motion on supported hardware.
- Confirm that screenshot comparisons meet the actual owner-approved references before merging. The critical visual discrepancies remain open.
- After CI and visual/field checks are genuinely green, update this evidence with final SHA, CI URLs, PR/merge SHA, and `origin/main` verification. Until then keep PR #7 open.
- Safe next work: isolate the API35 renderer failure and independently inspect the reference-vs-runtime visual delta while preserving the working API26 functionality and all unique branches/worktrees.
