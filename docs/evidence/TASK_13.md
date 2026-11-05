# TASK_13 — Ghost Hunter

**baseline_sha:** `48bd28b` (main after TASK_12 docs)
**branch:** `codex/task-13-ghost-hunter`
**implementation_sha:** `f5865bc` + `71afadb` + `71a7176` + `bd0a343` (head)
**merged_sha:** `df3b396`
**status:** MERGED · API26 DEVICE 111/111 · BUILD AND SONAR GREEN · API35 KNOWN RENDERER BLOCKER

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
  `lintDebug` 0 errors; `assembleDebug` PASS; `compileDebugAndroidTestKotlin` PASS. All five gates
  re-ran PASS on the merged `main` tree (`df3b396`).
- JVM tests use a mutable fake tree to prove: sentinel exclusion, empty/non-empty directories,
  truncation, cancellation, unreadable branches, selective deletion, changed-item skipping,
  provider failure, vanished-success verification and partial cancellation receipts.
- CI run `41954025923` (PR #12, head `bd0a343`): build PASS, SonarCloud PASS, API26 device PASS
  111/111 (0 failures, 0 skipped), API35 FAIL only on the documented
  `IdentityTest.arabicRtlLargeText360dp` renderer loss.
- Captures (local CI artifact `native-device-api-26`, under
  `device-screenshots/identity-evidence/`): `53-ghost-evidence.png`,
  `54-ghost-read-only.png`, `55-ghost-night-ar-large.png`, `56-ghost-flow.png`.
- Visual review found and fixed real day-mode defects: pale confidence/summary/warning tones were
  darkened in `71a7176` and `bd0a343`; the sentinel sentence was replaced with a count label. The
  dialog capture was removed from the test because API26 cannot capture a separate dialog window;
  the confirmation assertion remains. The spectral hero, evidence rows and receipt hierarchy match
  the KX identity.

## Known limits (honest)
- The system folder picker is not automated, so the real `DocumentsContract.deleteDocument`
  provider call needs a manual user-picked tree for field proof; the same cleanup gate is tested
  against the mutable fake source. No deletion is claimed without a verified absence.
- Empty-directory confidence stays medium by design.
- Cleanup requires a write grant. A tree authorized read-only by Storage X-Ray shows a regrant CTA
  instead of pretending deletion is available.
- API35 renderer loss unchanged. No gateway.

## PR / CI / merge
- PR: #12
- CI: https://github.com/daynightae-cmyk/KX-Private-VPN/actions/runs/41954025923
- merged_sha: `df3b3965db36a475af23c38d43f4a7ef3007da17`
- remote_main_sha: `df3b396` (fast-forward verified locally after merge)
- next_task: TASK_14 — Duplicate DNA.
