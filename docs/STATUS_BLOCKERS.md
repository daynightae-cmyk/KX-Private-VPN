# KX Private VPN — CURRENT BLOCKERS & COORDINATION LOG

**Last updated:** 2026-11-22 UAE
**Main SHA:** `096b260` (PR #52 merged — visual fidelity Ref 14/18/03, CI run `44280765817` green, SonarCloud gate OK)
**Working branch:** `codex/visual-elevation-and-cleanup-20261119` (PR #53, CI green on `6640da4`, awaiting merge)
**Branch policy:** exactly one integration branch (`main`); feature branches are merged and deleted after verification.

Every status below is backed by an executed command, a CI run URL/SHA or a SonarCloud API
response. Nothing in this file is an estimate or an intention.

---

## 🔴 P0 — RED CI GATE (API 26 instrumentation) — FIXED IN THIS BRANCH, AWAITING CI

| Item | Fact |
|------|------|
| Failing run | `44244219616` (`e59a010`), job `device (26)`, job id `134599246216` |
| Failing test | `com.knoux.kxprivatevpn.ToolsCatalogScreenTest.nineToolsRenderWithWorkingSearchFiltersAndNavigation` |
| Observed failure | `java.lang.AssertionError: expected:<1> but was:<0>` → the `onBack` callback never ran |
| Root cause | The reference header (and with it the `tools-back` circle) scrolls with the page. After the grid was scrolled, the tap was injected outside the window and never reached the button. The app was never broken; the test was not scroll-aware. |
| Fix | `compose.onNodeWithTag("tools-back").performScrollTo().performClick()` — same pattern already used for the filter chips and tool cards in the same test. |
| Verification | Local `:app:assembleDebug :app:testDebugUnitTest` → BUILD SUCCESSFUL (9m42s). The instrumented re-run is owned by CI (API 26 emulator). |

---

## 🟢 SONARCLOUD QUALITY GATE — **OK (VERIFIED)**

**Gate state on `main` after PR #50** (SonarCloud API, analysis of `377ffb3` at `2026-11-18T11:47:03Z`):

| Condition | Status | Value |
|-----------|--------|-------|
| `new_reliability_rating` | OK | A (1) |
| `new_security_rating` | **OK** | **A (1)** — was E (5) |
| `new_maintainability_rating` | OK | A (1) |
| `new_duplicated_lines_density` | OK | 0.8 % |
| `new_security_hotspots_reviewed` | OK | 100 % |
| **Quality gate** | **OK** | `projectStatus.status = OK` |

All four new-code vulnerabilities are **CLOSED** (`sinceLeakPeriod=true`): the BLOCKER
`pythonsecurity:S5131` and the three `githubactions:S7637` findings. The gate is no longer a
blocker for this repository.

**What the P0 actually was (previous notes were wrong):** the earlier ledger claimed that the
three fixes from PR #40 were already in `main` and that only a re-scan was missing. The gate
actually failed because of a **BLOCKER** in the Pro authority plus three unpinned actions. Both
are fixed and the gate now reports `OK`.

**Remaining old-code findings** (outside the gate condition, tracked, not blocking):

| Rule | Count | Note |
|------|-------|------|
| `githubactions:S7637` | 5 | Closed by the same SHA pinning. |
| `kotlin:S7204` | 1 | "Obfuscation should be enabled for release builds" on `android/app/build.gradle.kts`, although `release { isMinifyEnabled = true; isShrinkResources = true }` is set and the release build runs `minifyReleaseWithR8` successfully. Treated as a likely analyzer false positive. |
| `text:S8569` | 1 | Closed in PR #51 with `android/gradle/verification-metadata.xml` (503 components, SHA-256). |
| `xml:S5332` | 1 | `usesCleartextTraffic` + `networkSecurityConfig` interaction; cleartext is already denied globally by `network_security_config.xml` and `minSdk` is 26. |
| `css:S4656` | 1 | Closed in PR #51 (duplicate `margin` in the TASK_00 evidence page). |
| `kotlin:S3923` | 1 | Closed in PR #51 (identical conditional branches in `SecurityCenterTest`). |
| `typescript:S1082` | 18 | Legacy React prototype under `src/`, kept as historical material by the mission. Not the Android runtime. |

---

## 🟢 VERIFIED GREEN — API 35 FULL SUITE (correction of an earlier false blocker)

| Item | Fact |
|------|------|
| Run | `44244219616`, job `device (35)` (id `134599245744`) |
| Result | ✅ PASSED in 29m16s — `Finished 353 tests on emulator-5554 - 15`, 3 skipped, **0 failed** |
| Emulator | Google Emulator 36.4.10 build `15004761`, GPU `swangle`, skin 720x1280 |

The earlier note that "API 35 is blocked by an emulator RenderThread SIGSEGV" is **no longer true**.
The current CI configuration completes the whole API 35 matrix. API 35 must be re-confirmed on
the merge SHA of this branch, because a green API 35 run does not cover a changed test file.

---

## 🟢 VERIFIED GREEN — BUILD, UNIT TESTS, LINT, RELEASE COMPILE

| Job | Run `44244219616` | Note |
|-----|-------------------|------|
| `build` (Pro authority tests, JVM unit tests, lint, debug APK, androidTest APK, release APK) | ✅ 7m26s | includes `python -m unittest discover -s backend/pro` (52 tests OK locally) |
| `release-compile` (`:app:assembleRelease`, minify + shrink) | ✅ 4m33s | release artifact uploads as evidence |

Local proof on this machine: `:app:assembleDebug :app:testDebugUnitTest` → BUILD SUCCESSFUL in
9m42s; `python -B -W error::ResourceWarning -m unittest discover -s backend/pro` → 52 tests OK.

---

## 🟢 FORENSIC AUDIT — RESTORED (17 deliverables)

`docs/forensic-audit/` was empty after the branch consolidation: the audit had been committed on
`codex/task-02-vpn-transport` (`034fa98`, `01beaad`, `d92dc1c`), and that branch was deleted before
its content was merged. All objects survived locally, so the deliverables were recovered with a
clean cherry-pick. The tree now holds the **17 audited files** plus the restored ledger entry:

`ACTIVE_AGENT_OWNERSHIP_MATRIX.md`, `ANDROID_WIREGUARD_ARCHITECTURE_AUDIT.md`,
`API_26_36_TEST_MATRIX.md`, `BRANCH_WORKTREE_MATRIX.md`, `CI_SONAR_AND_EMULATOR_FAILURES.md`,
`EXECUTION_LEDGER.md`, `FORENSIC_BASELINE.md`, `FULL_VISUAL_GAP_MATRIX.md`,
`INTEGRATION_STRATEGY.md`, `KX_PRIVATE_VPN_SECOND_GENERATION_MASTER_EXECUTION_PROMPT.md`,
`PRODUCTION_RELEASE_GATES.md`, `SECURITY_PRIVACY_LEAK_REGISTER.md`, `STORAGE_CLEANUP_REGISTER.md`,
`TASK_000.md`, `TASK_INDEX.md`, `UNIQUE_WORK_REGISTER.md`, `VPN_SERVICE_REALITY_MATRIX.md`.

---

## 🟢 UNIQUE-WORK RECONCILIATION — NOTHING WAS LOST

Every commit that was reachable only through the deleted branches was compared against `main` by
stable patch-id and by per-file blob identity. Result: **33 dangling commits, 0 unique feature
implementations missing from `main`.**

| Group | Count | Disposition |
|-------|-------|-------------|
| Patch-id identical to a merged `main` commit | 9 | WireGuard transport (`f4cb157`), Repair Autopilot (#26), Settings (#34), Session Shield (#33), dialog localization (#35), package-state fix, Download Rescue refs 08+19, Arabic capture fix, export safety (#32) |
| Already applied verbatim in `main` | 3 | ghost-hunter authorized tree, app-uninstall evidence, clipping detection |
| Superseded by newer `main` code (file-by-file diff shows `main` is a superset) | 18 | app inventory, Network Doctor, app-inventory UI, CI timeouts, visual hardening, journal Unicode, profile-removal guard, evidence capture, … |
| Restored by this session | 3 | forensic audit (17 files) + ledger + `docs/STATUS_BLOCKERS.md` refresh |

PR reconciliation corrections:

| PR | Claimed earlier | Verified now |
|----|------------------|--------------|
| #48 | "not found in any branch" | **Exists and is MERGED** (`chore/docs: preserve unique local work before worktree cleanup`) |
| #41 | "download export safety lost" | **Merged through PR #32** (`8cdb835`), `DownloadExportSafetyTest.kt` is present in `main` |
| #18, #23, #27 | Phase 1 | Ancestors of `main` (`fa34b67`, `a70d89d`, `29bb492`), classified `MERGED_VERIFIED` |

---

## 🟡 EXTERNAL BLOCKERS — NEED OWNER OR INFRASTRUCTURE

| Blocker | Status | What is needed |
|---------|--------|----------------|
| **Real VPN tunnel proof** | ❌ Never proven on any device. `VpnPathVerification` cannot complete a handshake without an endpoint. | An authorized WireGuard gateway + a test profile. Until then `E2E_REAL_TUNNEL` stays `EXTERNAL_BLOCKED` and no release claim may be made. |
| **Release signing** | ❌ `android/key.properties` absent. Release builds fall back to the debug key and must never be published. | The owner keystore (`key.properties.example` documents the format). |
| **Physical-device tunnel run** | ❌ The API 35 phone `R9ZR20HWHQY` is attached over ADB, but no gateway exists to test against. | Same gateway as above. |
| **Firebase Test Lab** | Optional. The hosted API 26/35 emulator matrix is green, so Test Lab is now a robustness option, not a requirement. | A Firebase project with billing if the owner wants physical-device coverage in CI. |
| **Owner visual review (Ref 14 / 18 / 03)** | ⏳ Awaiting Agent_A. The current screens are native and functional; the gap is atmosphere/glow/glass fidelity. | Agent_A coordination window over `CrystalIdentity.kt`, `KxOrb.kt`, `KxReferenceUi.kt`, `RepairLabScreen.kt`. |

---

## 🟢 SECRETS ON DISK — ALREADY KEYSTORE-ENCRYPTED (correction of an earlier false gap)

The earlier ledger claimed that WireGuard private keys sit in plain DataStore. That was wrong:
both secret stores already use an Android Keystore held AES-256-GCM key and never write plaintext.

| Store | Content | Protection (verified in source) |
|-------|---------|--------------------------------|
| `data/config/ProfileStore.kt` | Imported WireGuard configurations, including the private key | `AndroidKeyStore` AES-256-GCM (`KeyGenParameterSpec`, GCM, no padding, randomised IV required), ciphertext written to `noBackupFilesDir` through `AtomicFile`; the plaintext buffer is zeroed right after use; a missing key with an existing file is a hard error, never a silent re-key |
| `platform/security/ProIdentityStore.kt` | The installation credential | Separate Keystore alias, own AES-256-GCM ciphertext file, no backup participation, the secret is redacted in `toString()` |
| `data/stats/UsageStatsStore.kt`, `launch/LaunchPreferences.kt` | Usage history and UI preferences | Plain DataStore, but no secret material is stored there |

No third-party crypto library is used and none is needed: the platform Keystore is the primitive.


---

## 📋 NEXT SEQUENCE

1. CI on PR #51 (`codex/release-artifact-and-supply-chain`) → build, release-compile (now with the real AAB), API 26 + API 35 device jobs, with Gradle dependency verification enforced.
2. Merge PR #51 into `main` only after those jobs are green, then delete the branch (single `main`).
3. Visual fidelity pass for Ref 14 / 18 / 03 with screenshot evidence from the physical phone.
4. Real tunnel proof on the physical phone with an authorized gateway, then owner-signed AAB and release.

---

## 📍 COORDINATION LOG

| Date | Agent | Action | Result |
|------|-------|--------|--------|
| 2026-11-15 | Forensic | 17 audit deliverables committed on `codex/task-02-vpn-transport` | ✅ Later lost by consolidation, recovered 2026-11-18 |
| 2026-11-17 | Consolidation | 47 local / 39 remote branches deleted, worktrees pruned | ⚠️ Audit deliverables dropped with them — recovered 2026-11-18 |
| 2026-11-18 08:40 | Consolidation | Adaptive icon, release build config, CI release gate, README, changelog, editorconfig pushed to `main` | ✅ `build`/`release-compile` green, `device (26)` red |
| 2026-11-18 11:05 | Forensic | Audit recovered by cherry-pick; API 26 root cause found; SonarCloud P0 root cause found | ✅ Fixes in `codex/recover-audit-fix-api26` |
| 2026-11-18 12:10 | Forensic | Local proof: Gradle debug build + unit tests, 52 backend tests | ✅ All green |
| 2026-11-18 11:46 | Forensic | PR #50 merged into `main` as `377ffb3` | ✅ CI run `44257528678` green on all four jobs (build, release-compile, API 26, API 35) |
| 2026-11-18 11:47 | Forensic | SonarCloud re-analysis of `377ffb3` | ✅ **Quality gate OK**, `new_security_rating = A`, all four new-code vulnerabilities CLOSED |
| 2026-11-18 12:30 | Forensic | Physical device proof on `R9ZR20HWHQY` (Android 12 / API 31, Arabic RTL) | ✅ 6 captures + report in `docs/evidence/device-20261118/` |
| 2026-11-18 12:40 | Forensic | PR #51: dependency verification, real AAB release gate, checksums, two old-code Sonar findings | ✅ Merged PR #51 → `e983498`, CI `44268994343` green |
| 2026-11-18 12:50 | Forensic | PR #52: visual fidelity Ref 14/18/03 (atmosphere sweeps, glass bevel, orb aura, Repair platform) | ✅ Merged PR #52 → `096b260`, CI `44274718413` green |
| 2026-11-18 13:10 | Forensic | Main push CI `44280765817` green on `096b260`; SonarCloud gate OK (`new_security_rating = A`) | ✅ Verified |
| 2026-11-19 15:30 | Forensic | PR #53: visual elevation + animation (KxOrbitalHalo, CTA bloom/press/haptic, Tools clear-search, Repair press scale), evidence correction (voided false 2026-11-18 claims), disk cleanup (648 MB freed), 6 new truthful captures | ✅ CI `44484793803` green on `6640da4` (build 7m50s, release-compile 4m27s, device(26) 13m53s, device(35) 30m53s, SonarCloud gate OK) |
| 2026-11-22 | Recovery | Repo/deep recovery session: three mission conversation exports located in `Downloads`; PR #53 re-verified (MERGEABLE, CI green); device `R9ZR20HWHQY` re-checked | ✅ App was in `stopped`/disabled package state on the phone — `pm enable com.knoux.kxprivatevpn` restored launch; native entry gate confirmed (no auto-navigation after the intro; Home appears only after the emblem tap); disconnected Home renders honest measured-or-dash state |

---

**Update this file whenever a blocker changes. Never claim a PASS that is not backed by a run, a
SHA or an executed command.**
