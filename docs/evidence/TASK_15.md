# TASK_15 — Large Files / Heavyweight Finder (read-only ranking)

**baseline_sha:** `1d2e499` (main after PR #19)
**branch:** `task15-heavyweight-finder`
**implementation_sha:** `6cc9d6d`
**merged_sha:** `c54a361`
**status:** MERGED — exact-SHA CI and post-merge `main` CI green

## Scope

- The fifth native Repair service: ranks the largest files in a folder the user authorises
  through Android's system SAF picker. It reuses the single shared tree grant and
  `StorageXrayStore` used by Storage X-Ray, Ghost Hunter and Duplicate DNA; no new permission.
- The walk is bounded and cancellable: 10,000 entries, depth 12, a 1 MiB floor, and only the
  largest 200 entries are retained in a min-heap. Every file at or above the floor is counted
  in the totals even when it is not retained, and truncation/cancellation is labelled.
- A provider entry without a reported size is counted in `skippedUnknownSize` and excluded
  from the ranking; no size is guessed. An unreadable branch marks the result partial.
- Size filters (any / 10 / 50 / 100 / 500 MB) and MIME-family filters (image, video, audio,
  document, archive, other) are pure view filters over the measured ranking; they never rescan
  and never invent metadata.
- Interactive relative-size heatmap: each tile is one file, width is driven by measured bytes,
  and tapping a tile selects the same file as the list row.
- Selection shows the measured path, reported MIME, modified date and size. The only file
  action is **Open** through a real `ACTION_VIEW` content URI built with
  `DocumentsContract.buildDocumentUriUsingTree`; an unhandled type is reported in the UI and
  the file is not touched.
- **No delete, rename, move or hide action exists in this screen.** The screen is read-only
  by design; the copy says so in both languages.
- Visual: original KX glass language (orb hero with scan sweep, glass cards, measured-data
  typography) in day/night, LTR/RTL, 360dp and 1.5x font scale, 48dp controls. There is no
  owner-supplied Repair/Large-Files reference image in the handoff set, so this composition
  derives from the owner's orb/glass identity rather than copying a Gemini screen.

## Changed paths

- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/repair/Heavyweight.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/HeavyweightViewModel.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/HeavyweightScreen.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/repair/RepairCatalog.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/NativeClient.kt`
- `android/app/src/main/res/values/heavyweight_strings.xml`
- `android/app/src/main/res/values-ar/heavyweight_strings.xml`
- `android/app/src/test/java/com/knoux/kxprivatevpn/HeavyweightTest.kt`
- `android/app/src/androidTest/java/com/knoux/kxprivatevpn/HeavyweightScreenTest.kt`
- `RepairCatalogTest`, `RepairLabScreenTest`, `RepairFlowTest` availability updates
  (6 of 10 tools available; the unavailable-route assertion now exercises Media).

## Verification

### Local (JVM + lint + build)

Command, run in `K:\KX-Private-VPN\android`:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

- `testDebugUnitTest`: **158 tests, 0 failures, 0 errors, 0 skipped** across 20 suites.
  `HeavyweightTest` contributes **8 tests**: byte floor and measured-only ranking, top-N
  retention with full counts, unknown-size exclusion, unreadable-branch partial result, entry
  budget truncation, cancellation, view filter by bytes/MIME family, and MIME mapping.
- `lintDebug`: **0 errors**; only warnings, 5 of them new `PluralsCandidate` suggestions in the
  compound measured-summary strings (the project already carries the same warning class).
- `assembleDebug` and `assembleDebugAndroidTest`: **pass**, so the instrumentation sources
  compile against the real app.

### Instrumentation (Compose)

`HeavyweightScreenTest` contains **11 tests** covering truthful ranking/summary, both filter
families without rescanning, selection details with Open as the only file action, heatmap
selection, empty and no-match states, partial/unknown/cancelled labelling, open failure with
no file mutation, no-grant state, cancellable scanning state, and night Arabic large text.

- Physical phone (itel S686LN, Android 15 / API 35, ar-EG, 360dp), in place with the matching
  debug certificate: **OK (11 tests) in 26.367s**, 0 failures. Exact command:

  ```powershell
  adb -s 1334825549012211 shell am instrument -w \
    -e class com.knoux.kxprivatevpn.HeavyweightScreenTest \
    com.knoux.kxprivatevpn.test/androidx.test.runner.AndroidJUnitRunner
  ```

- Installed APK: 29,316,678 bytes, SHA-256
  `5ebe66478567411efe111c3f28f9dd312f08ddd90c97df4d075901dddbe5eee6`; signing certificate
  SHA-256 `c07168d455a86f8c792ef44ebf213ab63ec327226589692bcad69231e67c085e` (the existing
  debug certificate; `firstInstallTime` unchanged at 2026-11-05 18:02:46). Test APK SHA-256
  `95bffb14e65397cc70f9f8c6da294f02a886fbddb2bfbe412e0508618017e7cf` (1,438,315 bytes).
- Five runtime captures pulled from
  `/sdcard/Android/data/com.knoux.kxprivatevpn/files/identity-evidence/` to
  `D:\KX-QODER-VISUAL-HANDOFF-20261105\DEVICE-EVIDENCE-20261106\task15-phone-20261106\identity-evidence\`:
  `59-heavy-ranking.png`, `59b-heavy-selected-card.png`, `59b-heavy-selected.png`,
  `59c-heavy-night-ar-large.png`, `59d-heavy-night-ar-results.png`.
### Exact-SHA CI

CI run [`42201616729`](https://github.com/daynightae-cmyk/KX-Private-VPN/actions/runs/42201616729)
at branch head `500ea6760a7f49100a1257f66c6c547b6418cf63` (implementation `6cc9d6d` + docs):

| Job | Result | Detail |
|---|---|---|
| build | PASS in 3m24s | 158 JVM tests, 0 failures; lint 0 errors; debug + androidTest APKs assembled |
| device API 26 | PASS in 6m22s | **149 tests, 0 failures, 0 skipped** |
| device API 35 | PASS in 8m00s | **149 tests, 0 failures, 2 skipped**; `display-contract.txt` = 720x1280 @ 280dpi = 411dp; IMEs disabled |

API 35 skips are the two documented emulator-only cases, reported as SKIPPED, never PASS:
`IdentityTest.arabicRtlLargeText360dp` and
`StorageXrayDeviceTest.mediaInventoryRespectsGrantedPermissionAndVersionMatrix`.
The CI artifacts contain all five `59*` captures; the API 35 English ranking capture was
reviewed and shows the measured summary, filters, heatmap and ranking at 411dp.

### Post-merge `main`

PR #20 merged into `main` as `c54a3610d9ca255cadd5baa3fefc5fc1fe430ad8`. The post-merge CI
run [`42204252025`](https://github.com/daynightae-cmyk/KX-Private-VPN/actions/runs/42204252025)
at that exact SHA completed with all jobs `success`:

| Job | Result | Duration |
|---|---|---|
| build | PASS | 3m09s |
| device API 26 | PASS | 6m59s |
| device API 35 | PASS | 9m14s |

## Permission tests

- No tree: the screen asks for a folder and offers no scan; it never falls back to broad
  storage access.
- Restored tree: the saved grant is only accepted when the persisted URI permission is still
  held with read access; otherwise the store is cleared and the empty state is shown.
- The grant is requested with `readWrite = false`; the walk uses read-only document APIs.
- JVM tests use a mutable fake `DocumentSource`; the real SAF picker is not automated, so a
  real provider tree still requires a manual user choice (documented limit).

## Visual before/after

There was no native Large Files screen before this task (the Repair Lab listed it as an
unavailable placeholder on `repair/heavyweight`). After: a real route at `heavyweight` with a
measured-data composition. Reviewed phone captures:

- `59-heavy-ranking.png` — Arabic RTL summary (14 files checked, 4 at/above the floor, total
  166 MB), filter chips, relative-size heatmap (4 measured tiles), and the ranked list with
  honest byte sizes and modified dates.
- `59b-heavy-selected-card.png` — selected `photo.jpg` row highlighted; details card shows the
  measured path, `image/jpeg`, size and date with the single **Open** action.
- `59b-heavy-selected.png` — list view with row-level Open actions and no destructive action.
- `59c-heavy-night-ar-large.png` — night Arabic at 1.6x font scale: orb hero, title, subtitle
  and authorised-folder card remain legible without overlap.
- `59d-heavy-night-ar-results.png` — night Arabic ranking/filters at 1.6x; filters and
  measured values remain readable.

The layout matches the owner's orb/glass identity in structure (orb hero, glass cards,
measured typography). It is not a pixel match to a Repair reference because no such reference
was supplied.

## Known limits

- The system SAF picker is manual; CI cannot grant a real provider tree.
- Files below 1 MiB and files with no provider-reported size are not ranked; both cases are
  counted and explained in the UI rather than estimated.
- No owner reference image exists specifically for this Repair screen; the design follows the
  approved KX orb/glass identity. Visual parity is therefore style-level, not pixel-level.
- No delete/cleanup path exists here by design; this service cannot reclaim storage.

## PR / CI / merge

- PR: #20
- CI: https://github.com/daynightae-cmyk/KX-Private-VPN/actions/runs/42201616729 (implementation)
- Docs-head CI: https://github.com/daynightae-cmyk/KX-Private-VPN/actions/runs/42202987779 (`b1dbcd1`, green)
- merged_sha: `c54a3610d9ca255cadd5baa3fefc5fc1fe430ad8`
- remote_main_sha: `c54a361` (verified after merge)
- next_task: TASK_16 Cache acceptance audit, then the remaining Repair services
