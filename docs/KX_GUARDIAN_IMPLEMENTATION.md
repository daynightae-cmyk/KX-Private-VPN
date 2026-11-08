# KX Guardian — implementation and handoff (2026-11-08)

**Isolated baseline:** `origin/main` @ `b9fd0777b46a4c0dbd82d96d32eeabdb782ee725`.
**Branch/worktree:** `feature/kx-guardian-20261108` at `K:\KX-GUARDIAN-20261108`.
**Do not merge into main while `visual-reference-batch-1` is actively edited.** The ongoing visual agent also modifies `HomeScreen.kt` and `NativeClient.kt`; preserve both unique changes and reconcile the tiny route/card additions after visual work is stable.

## Real end-to-end behavior
- Home `open-guardian` routes to `GuardianRoute`.
- Screen `route-guardian` displays an interactive KX orb that rotates during scan; scan state and all evidence values are real and nullable.
- `GuardianViewModel` reads `NetworkDoctor.read()`, `AndroidRepairMeasurer.measure()` and `InstalledApps.inventory()` locally; it makes **no external requests** and **no mutations**. Network capability callbacks refresh network data only while the screen is visible.
- VPN status comes from the existing live `VpnState` and never equates backend tunnel state with verified internet connectivity.
- `GuardianPolicy.recommendations` ranks observed problems and unknowns; each recommended action navigates only to a **real existing review screen**, not an automatic deletion/repair action.
- Storage metric is Android's internal data volume through `StatFs`; cache is strictly KX's own cache; apps list is visible launchable apps only. Neither means full phone inventory/total reclaimable space.
- AR/EN resources included, day/night through `KxGlassTheme`, scrollable responsive cards, role/test tags.
- Approved execution remains in KX's already existing services. Destructive operations are never performed by Guardian.

## Evidence and limits
- `GuardianPolicyTest`: **13/13 PASS**, including unknown network, offline/captive, missing DNS, true storage threshold, no silent cache deletion, permission-scoped apps and valid routes.
- Full `:app:testDebugUnitTest` result: **356 tests, 0 failures, 0 errors, 0 skipped, across 39 XML reports** (local Gradle, offline).
- `:app:assembleDebug`: **PASS**; generated `android/app/build/outputs/apk/debug/app-debug.apk` (28,090,033 bytes), SHA-256 `2DB27C41E80B7AE3A3DB562D7CF3C24122B8485530FE5038BA06D7DC1066872A`.
- `GuardianScreenTest`: 2 instrumented Compose cases; `:app:compileDebugAndroidTestKotlin` **PASS**. **Compilation is NOT execution**; do not mark device runtime PASS unless tested.
- No cloud AI, antivirus engine, DNS/IP leakage guarantee, zero-knowledge provider, server fleet or silent system-wide cleaner has been implemented.
- No billing required, no API key, no new manifest permissions.
- Physical phones MUST NOT be modified without the owner's explicit approval. The existing emulator can be in use by the visual agent, so it must not be overwritten during parallel work.

## Validation before merge
1. `cd android; ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`
2. Run Compose instrumentation in an **isolated emulator** and capture Arabic/English plus day/night screenshots.
3. Compare against approved KX brand/logo references, verify accessibility at 1.3x/1.5x, RTL direction and tall screen.
4. Reconcile any conflicts with the visual agent's unique work; never discard its edits or ours.
5. Verify tunnel/no-profile/error/captive/low-space and failed-scan behavior on a test device. Confirm Android consent remains part of every actual change.
6. Only then propose merging to main and verify CI and runtime separately.

## Guardian 2.0 — prioritized evidence and diagnostic passport

New implementation (feature branch only, no main merge):
- `GuardianPolicy` distinguishes critical (<=5%) vs low (<=10%) internal free storage. This is a StatFs threshold only, **not** a whole-device cleaner or risk score.
- Independent timestamps for network, storage and launchable-app observations. Network readings expire after 2m, storage 10m, apps 15m; future/missing timestamps are stale. A network callback cannot refresh old storage/app observations.
- WireGuard CONNECTED vs Android default-network VPN transport discrepancy triggers a **review**, not an IP/DNS leak verdict.
- `Trust Passport` shows counts of actionable/review/informational findings and navigates to the highest-priority existing review destination. Navigation never authorizes a mutation.
- On explicit tap, `GuardianPassport.render` produces a *sanitized local diagnostic report* copied to the Android clipboard. No VPN profile names, private keys, endpoints, DNS/IP addresses, app/package names, file paths or personal identities.
- The KX orb animates only during scanning, respecting Reduce Motion preferences.
- Android services are still read-only during Guardian scans. Other KX tools perform user-confirmed actions.
- Translations in `guardian_advanced_strings.xml` for EN/AR; stale readings are rendered as stale, not as current values.
- `GuardianAdvancedTest` adds 11 JVM cases: freshness/unknown, source isolation, critical storage, transport discrepancy, priority routes and sanitized export.
- Required release checks: full JVM test suite, APK assemble, Compose test compilation, **separate** Android emulator runtime/visual QA after the visual agent is done. Device pass has not been asserted.

Note: the guardian worktree follows committed origin/main baseline b9fd077; the other agent's reference-screen work is a different worktree and may conflict in the Home/NativeClient navigation touch points. Preserve all unique changes during eventual integration.
