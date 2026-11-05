# TASK_11 — KX Phone Repair Lab dashboard and architecture

**baseline_sha:** `424c718` (main after TASK_10 merge and docs)
**branch:** `codex/task-11-repair-lab`
**implementation_sha:** pending
**status:** IMPLEMENTED · LOCAL GATES PASS · DEVICE/VISUAL/CI PENDING

## Scope
Adds the native Repair Lab foundation and dashboard:

- A single service registry (`RepairCatalog`) for the ten owner-defined signature tools. Only
  `Storage X-Ray`, `Cache Surgeon` and `Network Doctor` are marked available because only those
  have real native implementations in this build; the other seven open honest "Unavailable"
  detail screens. No simulated results, progress or savings.
- Real measurement only: `StatFs` for the internal volume, a bounded walk of KX's own cache, and
  `CacheMaintenance.preview()` for reviewable KX-owned cache files. Null measurements stay null
  and are disclosed as partial; they are never replaced with zero.
- Dashboard UI in the owner KX glass identity: hero orb, ten-node diagnostic ring animated once
  per real measurement, ten frosted service cards with original Canvas glyphs, energy spine,
  measured storage summary, cleanup review card (navigates to Cache Surgeon; nothing is deleted
  from the dashboard), and a persisted repair journal.
- `RepairJournal` records completed Cache Surgeon removals with measured item/byte counts. It
  contains no paths, profile names or keys, is bounded to 50 entries and is app-private.
- All ten cards navigate; seven unavailable tools have real detail routes that state the missing
  implementation and the real scope/permission they will require.

## Changed paths
- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/repair/RepairCatalog.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/repair/RepairJournal.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/repair/RepairMeasurement.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/RepairLabViewModel.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/RepairLabScreen.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/designsystem/RepairGlyph.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/NativeClient.kt` (route + Home entry)
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/CacheScreen.kt` (journal append)
- `android/app/src/main/res/values{,-ar}/client_strings.xml` (Repair Lab copy)
- `android/app/build.gradle.kts`, `android/gradle/libs.versions.toml` (org.json for JVM codec tests)
- New tests: `RepairJournalCodecTest`, `RepairCatalogTest`, `RepairLabScreenTest`,
  `RepairFlowTest`, `RepairJournalDeviceTest`, `RepairMeasurementDeviceTest`; CacheFlowTest now
  asserts the real journal entry and that no cache path is written.

## Verification
- Local: `testDebugUnitTest` 109/109, 0 failures (11 new JVM tests); `lintDebug` 0 errors;
  `assembleDebug` PASS; `compileDebugAndroidTestKotlin` PASS.
- Device and visual evidence: pending CI API26 run (this environment cannot boot the local
  emulator). Planned captures: `43*`, `44*` (night Arabic large text), `45` (honest unavailable
  service), `46`/`47` (real Activity flow).
- The visual comparison against the owner VPN reference set will be recorded after the CI captures
  are reviewed. No Repair-specific owner artwork exists; this is an original native design in the
  same KX palette, as the visual contract allows.

## Known limits (honest)
- TASK_12–21 remain unimplemented; their cards say so. No fake scans.
- The dashboard itself requires no storage permission and therefore claims no permission state.
  Only KX cache and volume metadata are measured; other apps' private data is inaccessible.
- A cancelled cache cleanup is not journaled yet; only completed/partial/failed results are.
- API35 headless renderer loss is the same documented blocker; API26 is the device gate.

## PR / CI / merge
- PR: pending
- merged_sha: pending
- remote_main_sha: pending
- next_task: TASK_12 — Measured Storage X-Ray.
