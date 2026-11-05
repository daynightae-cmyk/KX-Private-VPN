# TASK_14 — Duplicate DNA (exact SHA-256 duplicates, confirmed cleanup)

**baseline_sha:** `6c911a1` (main after TASK_13)
**branch:** `task14-duplicate-dna`
**implementation_sha:** `b885f28` + safety follow-up (pending)
**status:** REVIEW BEFORE MERGE · API26/build green on `b885f28` · API35 known renderer blocker

## Scope
- Two-pass SAF scan: size buckets first, then streaming SHA-256 over the real document stream.
  Zero-byte files are excluded (Ghost Hunter owns them); near-duplicates are never grouped.
- Group keeper = oldest lastModified, ties by path; only non-keep entries are selectable.
- Confirmed cleanup. **Safety follow-up on top of `b885f28`:**
  - the keeper is re-verified (exists, same size, re-hashed to the group digest) before any
    duplicate in that group is deleted;
  - every selected duplicate is re-hashed immediately before deletion, not just size-checked;
  - a same-size content mutation or a missing/changed keeper blocks deletion for that group.
- Receipt reports deleted/skipped/failed/verified-absent and measured reclaimed bytes.
- Reuses the shared SAF tree grant and app-private Repair Journal; no new permission.

## Changed paths
- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/repair/DuplicateDna.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/DuplicateDnaViewModel.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/DuplicateDnaScreen.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/repair/RepairCatalog.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/{NativeClient,RepairLabScreen}.kt`
- `android/app/src/main/res/values{,-ar}/client_strings.xml`
- `android/app/src/test/java/com/knoux/kxprivatevpn/DuplicateDnaTest.kt`
- `android/app/src/androidTest/java/com/knoux/kxprivatevpn/DuplicateDnaScreenTest.kt`
- `RepairCatalogTest`, `RepairLabScreenTest`, `RepairFlowTest` availability updates.

## Verification
- Local: `testDebugUnitTest` **148/148**, 0 failures (19 Duplicate DNA JVM tests, incl. new
  same-size mutation, changed keeper, missing keeper); lint 0 errors; assemble PASS; androidTest
  compile PASS.
- CI `41974550399` on `b885f28`: build PASS, Sonar PASS, API26 PASS, API35 known
  `IdentityTest.arabicRtlLargeText360dp` renderer loss.
- Device captures from the PR artifact: `60-dna-groups`, `61-dna-read-only`, `62-dna-receipt`,
  `63-dna-night-ar-large`.
- Safety follow-up must pass a fresh CI run before merge; record the exact final SHA.

## Known limits
- The system SAF picker is not automated; the real `DocumentsContract.deleteDocument` path still
  needs a manual user-chosen tree. The same cleanup gate is JVM-tested with a mutable fake source.
- No owned gateway; no VPN claim is involved in this repair tool.
- API35 is the documented emulator renderer blocker.
- Near-duplicate perceptual matching is not implemented; the UI only offers exact matches.

## PR / CI / merge
- PR: #13
- merged_sha: pending until the safety follow-up CI passes and the PR is merged
- next_task: visual transformation per owner handoff