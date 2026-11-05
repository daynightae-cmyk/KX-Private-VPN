# TASK_12 — Measured Storage X-Ray

**baseline_sha:** `490c65c` (main after TASK_11 docs)
**branch:** `codex/task-12-storage-xray`
**implementation_sha:** pending
**status:** IMPLEMENTED · LOCAL GATES PASS · DEVICE/VISUAL/CI PENDING

## Scope
Replaces the basic StatFs card with a real Storage X-Ray:

- `StatFs` internal volume plus a bounded walk of KX-owned directories (`filesDir`, `cacheDir`,
  `noBackupFilesDir`). Failures stay null and render as "Not measured"/"unavailable".
- Real MediaStore aggregation for images, videos and audio after an explicit runtime permission
  request. Android 13+ uses granular media permissions; Android 14+ also declares and accepts
  partial visual access, labeled honestly. Documents via MediaStore are queried only on
  Android 10–12 where that permission model actually exposes them; otherwise SAF is the path.
  No row is invented and a denied/failed category never becomes 0 MB.
- SAF folder scan through `DocumentsContract`, user-selected tree only, depth/entry bounded,
  cancellable with partial results, persisted URI permission, and a real breadcrumb drill-down
  with folder aggregation, file list and sizes.
- Reconciliation card compares used volume against measured MediaStore + KX + SAF bytes and
  labels the remainder "Outside KX access", with an overlap disclosure.
- Read-only screen. No deletion, no move, no WRITE permission, no media modification.
- Native KX glass design: concentric storage core with measured used arc, category glyphs,
  frosted cards, EN/AR, Day/Night, large text, 48dp targets and test tags.

## Changed paths
- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/storage/StorageScan.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/storage/MediaInventory.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/storage/SafStorageScanner.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/StorageXrayViewModel.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/StorageXrayScreen.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/NativeClient.kt` (route swap)
- `android/app/src/main/AndroidManifest.xml` (read-only media permissions, legacy maxSdk 32)
- `android/app/src/main/res/values{,-ar}/client_strings.xml`
- New tests: `StorageScanTest`, `SafStorageScannerTest`, `StoragePermissionsTest`,
  `StorageXrayScreenTest`, `StorageXrayFlowTest`, `StorageXrayDeviceTest`.

## Verification
- Local: `testDebugUnitTest` 122/122, 0 failures (13 new JVM tests); `lintDebug` 0 errors;
  `assembleDebug` PASS; `compileDebugAndroidTestKotlin` PASS.
- Device tests cover the real MediaStore permission matrix with `pm grant`/`pm revoke` on API 26
  and assert denied categories stay null. SAF traversal is covered with a deterministic fake tree
  (depth, entry cap, cancellation, unreadable branch, cycle guard).
- Device and visual captures are pending the CI API26 run: planned `48*`, `49`, `50`, `51`,
  `52`. The new screen will be compared against the KX reference identity, not Gemini.

## Known limits (honest)
- The real system folder picker is not automated; SAF traversal correctness is proven on the
  JVM with the same scanner code, but the DocumentsContract reader itself needs a manual
  user-picked tree for field proof.
- MediaStore totals and SAF totals can overlap; the UI states this instead of pretending a
  precise partition.
- Android 13+ cannot expose documents through media permissions; the screen says so and directs
  to SAF. Android 8–9 documents are SAF-only in this build.
- API35 headless renderer loss is unchanged. No gateway; unrelated to storage.

## PR / CI / merge
- PR: pending
- merged_sha: pending
- remote_main_sha: pending
- next_task: TASK_13 — Ghost Hunter.
