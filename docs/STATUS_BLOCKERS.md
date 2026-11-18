# KX Private VPN — CURRENT BLOCKERS & COORDINATION LOG

**Last updated:** 2026-11-18 12:15 UAE
**Base main SHA:** `e59a010` (pushed, `origin/main`)
**Working branch:** `codex/recover-audit-fix-api26`
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

## 🔴 P0 — SONARCLOUD QUALITY GATE — ROOT CAUSE FOUND, FIXED IN THIS BRANCH, RE-SCAN PENDING

**Gate state before this branch** (SonarCloud API, analysis of `e59a010` at `2026-11-18T05:26:18Z`):

| Condition | Status | Value |
|-----------|--------|-------|
| `new_reliability_rating` | OK | A (1) |
| **`new_security_rating`** | **ERROR** | **E (5)** — threshold is A |
| `new_maintainability_rating` | OK | A (1) |
| `new_duplicated_lines_density` | OK | 0.8 % |
| `new_security_hotspots_reviewed` | OK | 100 % |

New-code period = `previous_version` of `2026-11-04T17:56:30Z`.

**Exactly four vulnerabilities sit on new code** (`sinceLeakPeriod=true`):

| Rule | Severity | Location | Fix in this branch |
|------|----------|----------|--------------------|
| `pythonsecurity:S5131` | **BLOCKER** | `backend/pro/pro_authority.py` — reflected request-body data written to the HTTP response | New `echo_token()` helper: the echoed nonce is re-validated as strict hex64 and HTML-escaped before it can enter a response body. A valid hex nonce is byte-identical, so the Android client contract (`fields["nonce"] == nonce`) is unchanged. New regression test `test_response_never_reflects_unescaped_client_input`. |
| `githubactions:S7637` ×3 | MAJOR | `.github/workflows/android.yml` — actions referenced by mutable tag | Every action is now pinned to a full 40-character commit SHA with the version in a comment (checkout v4.4.0, setup-java v4.9.1, setup-python v5.6.0, setup-gradle v4.4.4, setup-android v3.2.2, upload-artifact v4.6.2, android-emulator-runner v2.38.0). |

Because the E rating was caused by the single BLOCKER, closing it plus the MAJOR/MINOR new-code
findings is what moves `new_security_rating` to A. The re-scan is automatic on push; the gate
must be re-read after the new analysis lands.

**Old-code findings (outside the current gate condition, real but not blocking):**

| Rule | Count | Note |
|------|-------|------|
| `githubactions:S7637` | 5 | Closed by the same SHA pinning. |
| `kotlin:S7204` | 1 | "Obfuscation should be enabled for release builds" on `android/app/build.gradle.kts`, although `release { isMinifyEnabled = true; isShrinkResources = true }` is set and the `release-compile` job builds the minified APK successfully. Treated as a likely analyzer false positive; re-check after the next analysis. |
| `text:S8569` | 1 | No Gradle dependency lock file / verification metadata. Supply-chain hardening deferred to its own slice (adding `gradle/verification-metadata.xml` changes dependency resolution and must not be mixed into the gate fix). |
| `xml:S5332` | 1 | `usesCleartextTraffic` + `networkSecurityConfig` interaction; cleartext is already denied globally by `network_security_config.xml` and `minSdk` is 26. |
| `css:S4656` | 1 | Duplicate `margin` property in `docs/evidence/visual/TASK_00/review.html` — cosmetic evidence page. |
| `kotlin:S3923` | 1 | Identical branches in `SecurityCenterTest.kt`. |
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

## 🟡 PLANNED — ANDROID KEYSTORE ENCRYPTION FOR STORED SECRETS (owner option C)

| Item | Current | Target |
|------|---------|--------|
| WireGuard configuration (contains the private key) | Stored in app-private storage as plain bytes | AES-GCM key held in the Android Keystore; only ciphertext leaves the Keystore-backed key holder |
| Implementation plan | — | New `platform/security/SecretBox` (Keystore-backed AES-GCM) + injectable cipher for JVM tests, wrapped around `ProfileStore`; no new third-party dependency (platform API only) |
| Migration | — | Read legacy plaintext once, re-encrypt on write, wipe the plaintext buffer; failure must surface as an honest error, never a silent downgrade |

This is the next code slice after the current branch merges. It is provider-independent and fully
unit-testable, so it does not wait on any external dependency.

---

## 📋 NEXT SEQUENCE

1. CI on `codex/recover-audit-fix-api26` → API 26 + API 35 device jobs, build, release-compile.
2. Merge to `main` only after those jobs are green, then delete the feature branch (single `main`).
3. Re-read the SonarCloud gate after the new analysis: the requirement is `new_security_rating = A`.
4. Implement the Android Keystore secret box slice with unit tests.
5. Agent_A coordination window → visual fidelity pass for Ref 14 / 18 / 03 with screenshot evidence.
6. Real tunnel proof on the physical phone with an authorized gateway, then owner-signed AAB and release.

---

## 📍 COORDINATION LOG

| Date | Agent | Action | Result |
|------|-------|--------|--------|
| 2026-11-15 | Forensic | 17 audit deliverables committed on `codex/task-02-vpn-transport` | ✅ Later lost by consolidation, recovered 2026-11-18 |
| 2026-11-17 | Consolidation | 47 local / 39 remote branches deleted, worktrees pruned | ⚠️ Audit deliverables dropped with them — recovered 2026-11-18 |
| 2026-11-18 08:40 | Consolidation | Adaptive icon, release build config, CI release gate, README, changelog, editorconfig pushed to `main` | ✅ `build`/`release-compile` green, `device (26)` red |
| 2026-11-18 11:05 | Forensic | Audit recovered by cherry-pick; API 26 root cause found; SonarCloud P0 root cause found | ✅ Fixes in `codex/recover-audit-fix-api26` |
| 2026-11-18 12:10 | Forensic | Local proof: Gradle debug build + unit tests, 52 backend tests | ✅ All green |
| — | Agent_A | Visual fidelity coordination | ⏳ Awaiting response |

---

**Update this file whenever a blocker changes. Never claim a PASS that is not backed by a run, a
SHA or an executed command.**
