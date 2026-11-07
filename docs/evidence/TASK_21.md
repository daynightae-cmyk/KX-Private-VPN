# TASK_21 — Repair Autopilot (a real measured plan with approved execution)

**branch:** `feature/task21-repair-autopilot`
**head_sha:** `dfe535cb13d8c306344047edc2f969dde2e4a9e1`
**merged_sha:** `9e09641eb115dd981db3368ec606607771573dd6`
**PR:** https://github.com/daynightae-cmyk/KX-Private-VPN/pull/26
**CI run:** https://github.com/daynightae-cmyk/KX-Private-VPN/actions/runs/42331246208
**status:** GREEN — merged to `main`; build, device (26), device (35) and SonarCloud all SUCCESS.
The squash-merge tree is byte-identical to the CI-tested head (`git diff dfe535c 9e09641` is empty).

## Scope

The autopilot is the last repair service. It aggregates **only what the preceding tools really
measured** — the app's own cache files (SHA-256 + size + mtime from `CachePreview`) and the KX
journal/usage history that fell out of the retention window — into a deterministic, individually
selectable plan. Every target shows its real side effect, can be deselected, and is acted on only
after an explicit approval. Execution reuses the verified cache-removal and retention-prune paths;
the receipt is read back from the real operation. There is **no interval-based or simulated
completion**: nothing reports success on a timer.

## Real implementation

- `platform/repair/Autopilot.kt` — pure, JVM-testable domain:
  - `AutopilotKind` (CACHE_FILE / HISTORY), `AutopilotTarget` (id, kind, title, detail, measured
    `bytes`, `destructive`), `AutopilotPlan`, `AutopilotReceipt`.
  - `AutopilotPlanner.build(cache, journal, usage, nowMillis, retentionDays)` is a **pure function**:
    cache files sorted largest-first then by path (stable tie-break), a single HISTORY target only
    when `agedJournal + agedUsage > 0` (counted via `ShieldRetention.isExpired`). History reports
    `bytes = 0` because pruning a journal deletes no measurable file. `cacheMeasured = false` when
    the preview could not be read, so the screen states the gap instead of claiming an empty cache.
    The same measurement always proposes the same targets in the same order.
- `features/AutopilotViewModel.kt` — `AndroidViewModel` over `CacheMaintenance(app.cacheDir)`,
  `RepairJournal(noBackupFilesDir/repair-journal.json)` and `UsageStatsStore`:
  - `measure()` reads the real cache/journal/usage on IO and builds the plan; a failed cache read
    yields `cache = null` (honest gap), a total failure sets `failure` and invents no plan.
  - Selection (`toggle` / `selectAll` / `clearSelection`); `requestRun()` opens an approval dialog
    (or reports `no_selection`); the request itself deletes nothing.
  - `confirmRun()` executes exactly the approved targets: cache files through
    `CacheMaintenance.removeApproved` (re-verifies size/mtime/SHA-256, skips changed files, streams a
    checkpoint so partial progress survives cancellation); aged history through
    `journal.replace(kept)` + `usage.saveSurvivors(survivors)` using the same retention rule as the
    Session Shield. Per-phase failures are tracked; `CancellationException` is rethrown and labelled
    cancelled. The receipt + journal entry are written in a `NonCancellable` block, then the plan is
    re-measured. `complete` is true only when nothing was cancelled, failed or skipped.
- `features/AutopilotScreen.kt` — stateless screen, `route-autopilot` tag, `RepairGlyph(AUTOPILOT)`,
  plan summary, per-target selectable glass cards, approval `AlertDialog`, busy card with cancel,
  honest receipt (complete / partial / cancelled, skipped + failed lists), empty / failure /
  cache-unavailable / cache-limited states and an explicit scope paragraph.
- `NativeClient.kt` / `RepairCatalog.kt` — `autopilot` route wired; `AUTOPILOT.available = true`
  (all 10 catalog services now real).
- EN/AR `autopilot_strings.xml` (35 strings each), including the scope text stating KX never reads
  or deletes another app's files, never ends another app's session and never invents a result.

## Acceptance audit (TASK_21 spec)

| Requirement | Real implementation and evidence |
|---|---|
| Plan from real results of preceding tools | `AutopilotPlanner.build` consumes only `CachePreview` (measured bytes/SHA-256/mtime) and the real journal/usage stores; no invented finding. |
| Deterministic safe plan | Pure function; JVM `theSameMeasurementAlwaysProposesTheSamePlan` pins equal-input ⇒ equal-plan and stable ordering. |
| User sees every target + side effects | Per-target cards show title, measured bytes / aged counts and "Deletes data. Approval required."; `aMeasuredPlanListsEveryTargetWithItsSideEffect`. |
| Can deselect | `toggle` / `selectAll` / `clearSelection`; `togglingATargetInvokesTheCallbackWithItsId`; run disabled on empty selection. |
| Approves destructive steps | `requestRun` opens a dialog; `confirmRun` is separate; `runningAsksFirstAndTheRequestItselfDeletesNothing` and `theApprovalDialogSeparatesTheApprovalFromTheRequest`. |
| Executes supported Android operations | Cache deletion via verified `CacheMaintenance.removeApproved`; history prune via journal/usage retention rewrite. No unsupported/3rd-party operation. |
| Verifies each receipt | `runningThePlanReallyDeletesTheApprovedCacheFile` asserts the file is really gone and `bytesFreed == 4096`; `runningThePlanReallyPrunesAgedHistoryAndKeepsFresh` reads both stores back (aged gone, fresh kept). |
| Partial failure and cancel | Per-phase `cacheFailed`/`historyFailed`, `skipped`/`failed` lists, `cancelled` label; `aPartialReceiptSurfacesSkippedAndFailed`, `aCancelledRunIsLabelledCancelledNotCompleted`, busy-state cancel. |
| Reports actual reclaimed bytes | `bytesFreed` = sum of verified removed file bytes from the maintenance receipt; history contributes 0 (no measurable file). Never estimated. |
| NO fake interval-based completion | No timer/`delay`-driven success anywhere; the receipt is produced only in the `finally` of the real operation and read back. |

## Tests

- JVM `AutopilotTest.kt` — **9/9** (determinism, largest-first ordering, aged-only history target,
  fresh-history no-op, honest unmeasured-cache gap, selected-byte accounting, limited/retention
  carry-through, stable namespaced ids). `RepairCatalogTest` — **6/6** (10 available services).
  Full JVM suite at this head: **201 tests, 0 failures, 0 errors, 0 skipped** (24 suites).
- Instrumentation `AutopilotScreenTest.kt` — **14 tests** (12 UI/state/approval/receipt tests plus the
  two real read-back proofs above). Combined connected run: **22 tests, 0 failures, 0 errors,
  0 skipped** = AutopilotScreenTest 14 + RepairLabScreenTest 7 + RepairFlowTest 1 (autopilot nav).
  Real-test timings: cache delete 2.155 s, history prune 1.085 s.
- `RepairLabScreenTest.unavailableDetailStatesNoSimulation` was adapted (not weakened): with all 10
  services now available it renders the unknown-service route and asserts the honest
  `repair_unknown_service` panel, preserving the original no-simulation assertion path.

## SonarCloud gate

Quality gate **OK** on PR #26: `new_reliability_rating = 1 (A)`, `new_security_rating = 1 (A)`,
`new_maintainability_rating = 1 (A)`, `new_duplicated_lines_density = 0.0`,
`new_security_hotspots_reviewed = 100`. No test was skipped, disabled or weakened.

## Visual evidence (runtime, API 26 emulator)

Captured by the asserted test bodies and pulled from the device before any uninstall, under
`docs/evidence/task21/emulator-evidence/`:

- `48-autopilot-plan-day.png` — English day: scope paragraph, "Proposed plan · 3 targets · 3.07 kB
  measured", selected total, Re-measure/Select all/Clear, three selectable target cards with
  measured bytes and approval-required labels, "Run the approved plan".
- `49-autopilot-plan-night-ar-large.png` — Arabic night at 1.6x font scale, RTL: target cards,
  aged-history counts and the honest scope text remain visible and readable.
- `45-repair-unknown-route.png` — the honest unknown-service panel (no simulation claim).

The composition follows the owner KX orb/glass identity used across the native app. No
autopilot-specific owner reference image exists, so parity is style-level, not pixel-level.

## Honest limits

- Physical-phone (itel S686LN, serial `1334825549012211`) re-verification at this SHA is **pending**
  (ADB offline); TASK_21 is verified on the API 26 / API 35 CI emulators and the local API 26
  emulator. See the final audit for device status.
- Scope is KX's own `cacheDir` + KX journal/usage only. Other apps' files, sessions and storage are
  never read, listed or deleted; the screen says so and the planner has no such target kind.
- History pruning frees no measurable file bytes (reported as 0); only cache removal reports bytes.
- A stale cache preview can only cause a skip (re-verification), never a wrong deletion.

## PR / CI / merge

- PR: https://github.com/daynightae-cmyk/KX-Private-VPN/pull/26
- head SHA: `dfe535cb13d8c306344047edc2f969dde2e4a9e1`
- CI: run `42331246208` — build, device (26), device (35), SonarCloud all SUCCESS (Gitar SUCCESS,
  CodeRabbit SKIPPED).
- merged_at: `2026-11-07T03:06:11Z`
- merged_sha: `9e09641eb115dd981db3368ec606607771573dd6`
- Branch preserved (no force-push, no branch deletion).
- next: unique-work consolidation + safe cleanup (owner approval), physical-phone re-verification,
  final evidence audit.