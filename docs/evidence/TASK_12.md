# TASK_12 — Measured Storage X-Ray

**baseline_sha:** `490c65c` (main after TASK_11 docs)
**branch:** `codex/task-12-storage-xray`
**implementation_sha:** `d9c5008` + `ef252bf` + `542c380` + `0fa9dad` + `f5ae806` (head)
**merged_sha:** `5e1ec69`
**status:** MERGED · API26 DEVICE 100/100 · BUILD AND SONAR GREEN · API35 KNOWN RENDERER BLOCKER

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
  `assembleDebug` PASS; `compileDebugAndroidTestKotlin` PASS. All five gates re-ran PASS on the
  merged `main` tree (`5e1ec69`).
- CI run `41947508625` (PR #11, head `f5ae806`): build PASS, SonarCloud PASS, API26 device PASS
  100/100 (0 failures, 0 skipped), API35 FAIL only on the documented
  `IdentityTest.arabicRtlLargeText360dp` renderer loss.
- Device tests grant the real media permission through a waited shell command and prove the
  version matrix; each unpermitted category stays null. SAF traversal is covered with a
  deterministic fake tree (depth, entry cap, cancellation, unreadable branch, cycle guard).
- Captures (local CI artifact `native-device-api-26`, under
  `device-screenshots/identity-evidence/`): `48-storage-xray-day.png`,
  `48b-storage-xray-folder-empty.png`, `49-storage-xray-permission.png`,
  `50-storage-xray-folder-scan.png`, `51-storage-xray-night-ar-large.png`,
  `52-storage-xray-flow.png`.
- Visual review found and fixed two real defects: duplicate byte figures per MediaStore category
  and English timestamps inside the Arabic screen. `0fa9dad` localizes timestamps through the
  active configuration locale and keeps one size + one count per category; re-captured `48`/`50`/
  `51` confirm both fixes. The glass storage core, category hierarchy and sandbox copy match the
  KX identity; no Repair-specific owner art exists.

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
- PR: #11
- CI: https://github.com/daynightae-cmyk/KX-Private-VPN/actions/runs/41947508625
- merged_sha: `5e1ec69a3c05b3503d2974ab333aba6bff6f782b`
- remote_main_sha: `5e1ec69` (fast-forward verified locally after merge)
- next_task: TASK_13 — Ghost Hunter.
