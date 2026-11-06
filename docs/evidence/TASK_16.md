# TASK_16 — Truthful cache management (acceptance audit)

**baseline_sha:** `c54a361` (main after PR #20 / TASK_15)
**branch:** `task16-cache-audit`
**implementation_sha:** `b56a05e` (plus `1da1d32`, both on top of the `origin/main` merge)
**merged_sha:** pending
**status:** LOCAL GREEN — exact-SHA CI and merge pending

## Scope

The native cache service already existed; this task audited it against the TASK_16 acceptance
criteria and closed the two real gaps:

1. **Official Settings guidance for other apps.** KX cannot read or delete another app's
   storage on modern Android. The screen now states that explicitly in English and Arabic and
   offers a 48dp **Open Android app settings** button. It launches
   `Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS` with `FLAG_ACTIVITY_NEW_TASK`; a failure is
   reported in-screen instead of claiming success.
2. **Measured selection summary.** The exact selected count and measured file-content bytes
   are shown before the confirmation dialog and again inside it, using the same byte values
   that the remover will verify.

The audited deletion path is unchanged in its safety model:

- `CacheMaintenance` accepts only `Context.cacheDir`; `safe()` rejects blank, absolute and
  canonical paths outside that root, so traversal cannot escape the KX cache.
- Preview records each candidate's path, size, modified time and SHA-256.
- Removal requires an explicit approval flag and a non-empty explicit path set.
- Immediately before deletion every file is re-read and its SHA-256, size and timestamp are
  compared with the preview; changed files are skipped.
- A file is counted as removed only when the delete call succeeds **and** the file is verified
  absent; `removedFileBytes` is the sum of the previewed sizes of those verified removals.
- The receipt is persisted with `AtomicFile` for interrupted/last-operation display; no path
  outside the KX cache is ever written or deleted.
- The delete call is injectable only for tests, so permission-denied deletion is exercised
  without weakening the production filesystem checks.

## Changed paths

- `android/app/src/main/java/com/knoux/kxprivatevpn/features/CacheScreen.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/storage/CacheMaintenance.kt`
- `android/app/src/main/res/values/cache_strings.xml`
- `android/app/src/main/res/values-ar/cache_strings.xml`
- `android/app/src/test/java/com/knoux/kxprivatevpn/CacheMaintenanceTest.kt`
- `android/app/src/androidTest/java/com/knoux/kxprivatevpn/CacheFlowTest.kt`
- `android/app/src/androidTest/java/com/knoux/kxprivatevpn/CacheSettingsIntentTest.kt`
- `android/app/src/androidTest/java/com/knoux/kxprivatevpn/CacheScreenContractTest.kt`
- `docs/evidence/TASK_15.md`, `docs/evidence/CONSOLIDATION_TRUTH_20261106.md` (merge state)

## Acceptance audit

| Requirement | Real implementation and evidence |
|---|---|
| Clean own temporary files | `CacheMaintenance(Context.cacheDir)` only; no third-party, external-cache or files-dir walk. Preview lists real files from the KX cache. |
| Exact size measurements | Preview stores measured `bytes` and SHA-256 per file. Receipt reports `removedFileBytes` only from files verified absent after deletion. Selected summary shows the same measured total. |
| Confirm before deletion | Selecting files enables Remove; the dialog shows the count/bytes and requires the confirm button. `CacheFlowTest` asserts the target file still exists while the dialog is open and is gone only after confirm. |
| Guide to official Android storage settings | New button/copy in EN/AR plus `CacheSettingsIntentTest`, which resolves the official app-list intent and checks `FLAG_ACTIVITY_NEW_TASK`. |
| No third-party deletion | No broad-storage permission or third-party path exists. JVM tests keep a sentinel outside the cache root and a `noBackupFilesDir` sentinel intact; the flow test keeps its own cache sentinel. The UI states KX never reads/deletes other apps' data. |
| Permission errors | New JVM case injects a denied delete: 0 deleted, 0 bytes, path listed as failed, file still exists. |
| Changed files | Same-size/same-timestamp mutation is detected by SHA-256 and skipped; traversal is rejected. |
| Empty/no-op | Empty selection is an explicit no-op with 0 deleted and 0 bytes. |
| EN/AR, day/night, RTL, 360dp, font scaling | Real phone captures at 360dp in English day and Arabic night; a Compose contract test renders Arabic night at 1.6x font scale with 48dp actions. |

## Verification

### Local build and tests

```powershell
cd K:\KX-Private-VPN\android
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
```

Result: **BUILD SUCCESSFUL**; **159 JVM tests, 0 failures, 0 errors, 0 skipped**
(`CacheMaintenanceTest` = 4/4); lint **0 errors / 99 warnings**; debug APK 29,326,627 bytes;
androidTest APK 1,449,844 bytes.

### Physical phone — itel S686LN, Android 15 / API 35, ar-EG, 360dp

```powershell
adb -s 1334825549012211 shell am instrument -w \
  -e class com.knoux.kxprivatevpn.CacheSettingsIntentTest,\
com.knoux.kxprivatevpn.CacheFlowTest,\
com.knoux.kxprivatevpn.CacheScreenContractTest \
  com.knoux.kxprivatevpn.test/androidx.test.runner.AndroidJUnitRunner
```

Result: **OK (5 tests) in 25.474s**, 0 failures.

- Installed APK: 29,326,627 bytes, SHA-256
  `9a206bdfbd62e371add4b8fbef7de2cf7f33bb2a184a7ec9f342b038bbbc1978`.
- Signing certificate SHA-256:
  `c07168d455a86f8c792ef44ebf213ab63ec327226589692bcad69231e67c085e` (existing debug
  certificate; installed in place).
- `firstInstallTime` unchanged at 2026-11-05 18:02:46; `lastUpdateTime` 2026-11-06 14:48:26.

## Visual before / after

Before this task the route showed the KX-only scope paragraph, scan results and receipt, but no
route for another app's cache and no selected-total summary. After:

- `09-cache-verified-receipt.png` — English day: scope, official-settings guidance, real
  `Verified removed: 1 files · 64 B of file contents. Skipped: 0 · Failed: 0.` receipt and the
  file-content-bytes explanation.
- `60-cache-night-ar-scope.png` — Arabic night at 360dp: RTL scope, explicit third-party
  exclusion, official-settings button and scan action.
- `60b-cache-night-ar-selected.png` — measured `128 B` row selected, `المختار: 1 ملف · 128 B`
  summary and the Remove action.
- `60c-cache-night-ar-confirm.png` — confirmation dialog with the same measured summary and
  Cancel/Remove actions over the real screen.
- `61-cache-night-ar-large-scope.png` — Arabic night at 1.6x font scale: scope and both
  actions stay visible and readable.
- `61b-cache-night-ar-large-selected.png` — Arabic night at 1.6x: selected row, measured
  summary and Remove action remain visible.

Files are under
`D:\KX-QODER-VISUAL-HANDOFF-20261105\DEVICE-EVIDENCE-20261106\task16-phone-20261106\`.

The composition follows the owner KX orb/glass identity already used across the native app.
No cache-specific owner reference image exists, so parity is style-level, not pixel-level.

## Known limits

- Other apps' caches cannot be cleared by KX; Android provides no supported API for that. The
  only real action is the official app list, which the user drives.
- The service intentionally touches `Context.cacheDir` only; KX `filesDir`,
  `noBackupFilesDir` (saved profiles/journal) and external caches are outside its scope.
- File-content bytes are exact for removed files; physical free-space change can differ due to
  Android filesystem accounting, and the UI says so.
- The system Settings window itself is not automated by the test; only intent resolution and
  flags are asserted. Launch failure is surfaced by the UI.
- No owner cache-specific reference image; visual parity is style-level.
- API 35 emulator CI has the two documented emulator-only skips; they are reported SKIPPED,
  never PASS.

## PR / CI / merge

- PR: pending
- CI: pending (exact-SHA API 26 / API 35 + build required before merge)
- merged_sha: pending
- remote_main_sha: pending
- next_task: TASK_17 Media Slim Studio
