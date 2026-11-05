# TASK_13 — Ghost Hunter

**baseline_sha:** `48bd28b` (main after TASK_12 docs)
**branch:** `codex/task-13-ghost-hunter`
**implementation_sha:** pending
**status:** IMPLEMENTED · LOCAL GATES PASS · DEVICE/VISUAL/CI PENDING

## Scope
Implements the Ghost Hunter service on the shared SAF foundation:

- Real read-only scan of the user-authorized tree. Candidates are limited to files the provider
  reports as exactly 0 bytes and directories that returned no children. Nothing is inferred from
  age, extension or an unfamiliar folder.
- Known sentinels (`.nomedia`, `.gitkeep`, `.gitignore`) are never offered; the count is disclosed.
- Every candidate carries a reason and confidence: high for a reported 0-byte size, medium for a
  childless directory because a provider may hide children.
- Confirmed cleanup: the user must select items and approve a dialog. Each candidate is re-read
  immediately before deletion; files that gained bytes and folders that gained children are
  skipped. A provider success is verified with a real absence check before it is counted.
- Receipt reports deleted / skipped / failed and verified-absent counts. Failed and cancelled
  operations are never presented as completed. A partial cleanup keeps the receipt and triggers a
  real rescan.
- Completed deletions append a `ghost_cleanup` entry (measured item count, zero bytes) to the
  Repair Lab journal. The UI states if journaling failed.
- Repair Lab now marks Ghost Hunter available and routes to this screen. The dashboard card count
  moves from 3/10 to 4/10; tests and the unavailable-demo route were updated to Duplicate DNA.

## Changed paths
- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/storage/StorageScan.kt` (document id)
- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/storage/SafStorageScanner.kt`
  (mutable source, document query, verified delete, write grant)
- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/repair/GhostHunter.kt` (new)
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/GhostHunterViewModel.kt` (new)
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/GhostHunterScreen.kt` (new)
- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/repair/RepairCatalog.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/{NativeClient,RepairLabScreen}.kt`
- `android/app/src/main/res/values{,-ar}/client_strings.xml`
- Tests: `GhostHunterTest`, `GhostHunterScreenTest`, `GhostHunterFlowTest`; updated
  `RepairCatalogTest`, `RepairLabScreenTest`, `RepairFlowTest`.

## Verification
- Local: `testDebugUnitTest` 129/129, 0 failures (7 new ghost tests + catalog updates);
  `compileDebugKotlin` PASS; `lintDebug` 0 errors; `assembleDebug` PASS;
  `compileDebugAndroidTestKotlin` PASS.
- JVM tests use a mutable fake tree to prove: sentinel exclusion, empty/non-empty directories,
  truncation, cancellation, unreadable branches, selective deletion, changed-item skipping,
  provider failure, vanished-success verification and partial cancellation receipts.
- Screen tests cover evidence, selection, confirmation, read-only regrant, empty/no-tree states,
  partial/cancelled/failed scans, receipt/journal failure and night Arabic large text.
- Device/visual captures pending the CI API26 run: planned `53`–`56`.

## Known limits (honest)
- The system folder picker is not automated, so the real `DocumentsContract.deleteDocument`
  provider call needs a manual user-picked tree for field proof; the same cleanup gate is tested
  against the mutable fake source. No deletion is claimed without a verified absence.
- Empty-directory confidence stays medium by design.
- Cleanup requires a write grant. A tree authorized read-only by Storage X-Ray shows a regrant CTA
  instead of pretending deletion is available.
- API35 renderer loss unchanged. No gateway.

## PR / CI / merge
- PR: pending
- merged_sha: pending
- remote_main_sha: pending
- next_task: TASK_14 — Duplicate DNA.
