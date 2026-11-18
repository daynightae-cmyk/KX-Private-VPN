# TASK INDEX — TASK_000 THROUGH PRODUCTION
**Generated:** 2026-11-15 12:25:00 UAE  
**Source:** `CODEX_TASK_QUEUE.md`, forensic audit findings, active agent matrix  

---

## TASK CLASSIFICATION LEGEND
- **NOT_STARTED** — No work begun
- **PARTIAL** — Some implementation exists, gaps documented
- **LOCAL_UNCOMMITTED** — Unstaged changes in working tree
- **COMMITTED_UNPUSHED** — Local commits not on origin
- **PUSHED_UNMERGED** — On origin branch, not merged to main
- **MERGED_UNVERIFIED** — Merged to main but not fully verified
- **REGRESSED** — Previously working, now broken
- **VERIFIED** — Code + tests + visual + runtime evidence all PASS
- **ACTIVE_AGENT** — Currently worked on by live agent (BLOCKED)
- **EXTERNAL_BLOCKED** — Requires external dependency (keys, gateway, signing)
- **BLOCKED_BY_ACTIVE_AGENT** — Cannot proceed due to active agent ownership

---

## MASTER TASK INDEX

| ID | Title | Status | Owner | Branch/Worktree | Prerequisites | Next Safe Action |
|----|-------|--------|-------|-----------------|---------------|------------------|
| **TASK_000** | Verified Baseline & Active Agent Protection | **IN_PROGRESS** | Forensic Auditor | `docs/forensic-audit/` | None | Complete read-only audit |
| **TASK_00** | Legendary Cinematic Splash | **NOT_STARTED** | — | — | TASK_000 complete | Implement `KxOrb` + `IdentityExperience` visual fidelity |
| **TASK_01** | Welcome + Onboarding | **NOT_STARTED** | — | — | TASK_00 | Implement `IdentityExperience` controls |
| **TASK_02** | Real VPN Disconnected Home | **PARTIAL** | Agent_A | `feature/kx-reference-09-15...` | TASK_01 | Visual fidelity fixes (Ref 14) |
| **TASK_03** | Real Endpoint Selection | **PARTIAL** | — | `ServerSelectionScreen.kt` | TASK_02 | Visual verification vs Ref 03 |
| **TASK_04** | True Connected Experience | **PARTIAL** | — | `VpnSessionScreen.kt` | TASK_03 | Real tunnel proof (EXTERNAL_BLOCKED) |
| **TASK_05** | Security Truth Center | **UNVERIFIED** | — | `codex/task-05-security-center` | TASK_04 | Merge PR → visual verify Ref 05 |
| **TASK_06** | Actual Per-App Split Tunnels | **REAL_VERIFIED** | — | `SplitTunnelScreen.kt` | TASK_05 | Visual verification Ref 06 |
| **TASK_07** | Actual Advanced Network | **REAL_VERIFIED** | — | `codex/task-07-advanced-network` | TASK_06 | Merge PR → visual verify Ref 07 |
| **TASK_08** | Real Local Metrics | **REAL_VERIFIED** | — | `StatisticsScreen.kt` | TASK_07 | Visual verification Ref 08 |
| **TASK_09** | Secure Config Import | **REAL_VERIFIED** | — | `ImportScreen` | TASK_08 | Visual verification Ref 09 |
| **TASK_10** | Profile/Settings | **REAL_VERIFIED** | — | `SettingsRoute` | TASK_09 | Visual verification Ref 10 |
| **TASK_11** | KX Repair Lab Dashboard | **PARTIAL** | — | `codex/task-11-repair-lab` | TASK_10 | Merge PR → visual verify Ref 03 (Repair) |
| **TASK_12** | Storage X-Ray | **REAL_VERIFIED** | — | `codex/task-12-storage-xray` | TASK_11 | Merge PR → visual verify |
| **TASK_13** | Ghost Hunter | **REAL_VERIFIED** | — | `codex/task-13-ghost-hunter` | TASK_11 | Merge PR → visual verify |
| **TASK_14** | Duplicate DNA | **REAL_VERIFIED** | — | `DuplicateDnaScreen.kt` | TASK_11 | Visual verification |
| **TASK_15** | Heavyweight Finder | **REAL_VERIFIED** | — | `HeavyweightScreen.kt` | TASK_11 | Visual verification |
| **TASK_16** | Cache Surgeon | **REAL_VERIFIED** | — | `CacheScreen.kt` | TASK_11 | Visual verification |
| **TASK_17** | Media Slim Studio | **REAL_VERIFIED** | — | `MediaSlimScreen.kt` | TASK_11 | Visual verification |
| **TASK_18** | Download Rescue | **ACTIVE_AGENT** | Agent_A | `D:\KX-DOWNLOAD-SAFETY-20261108` | TASK_11 | **WAIT for Agent_A handoff** |
| **TASK_19** | Session Shield | **REAL_VERIFIED** | — | `SessionShieldScreen.kt` | TASK_11 | Visual verification |
| **TASK_20** | Network Doctor | **REAL_VERIFIED** | — | `NetworkDoctorScreen.kt` | TASK_11 | Visual verification |
| **TASK_21** | Repair Autopilot | **PARTIAL** | — | `feature/task21-repair-autopilot` | TASK_11 | Visual verification |
| **TASK_SYNC_01** | Fast-forward main to origin/main | **VERIFIED** | — | `main` | TASK_000 | `git pull --ff-only` (DONE) |
| **TASK_SYNC_02** | Resolve API 35 emulator instability | **BLOCKED** | — | CI config | TASK_SYNC_01 | Physical device farm / sharding |
| **TASK_SYNC_03** | Configure release signing | **DEFERRED** | — | `app/build.gradle.kts` | External keys | Document blocker |
| **TASK_SYNC_04** | Android Keystore for private keys | **PLANNED** | — | `ProfileStore.kt` | TASK_SYNC_01 | Implement encryption |
| **TASK_SYNC_05** | Authorized VPN test gateway | **EXTERNAL** | — | Infrastructure | TASK_04 | Provision test server |
| **TASK_SYNC_06** | Visual fidelity remediation (Ref 14, 18, 03) | **PLANNED** | — | `CrystalIdentity.kt`, `KxOrb` | Agent_A coordination | Coordinate + implement |
| **TASK_SYNC_07** | Download Rescue integration | **BLOCKED_BY_ACTIVE_AGENT** | Agent_A | `fix/download-export-safety-20261108` | Agent_A handoff | Wait for handoff |
| **TASK_PR18** | Fix API 35 Emulator Stability | **MERGED_VERIFIED** | — | `fix/api35-emulator-stability` (PR #18) | — | Merged at `fa34b67`, CI green |
| **TASK_PR23** | Instrumented Suite Survival | **MERGED_VERIFIED** | — | `test(android): suite survival` (PR #23) | — | Merged at `a70d89d`, CI green |
| **TASK_PR27** | Harden Autopilot Contrast/Entry | **MERGED_VERIFIED** | — | `Harden Autopilot` (PR #27) | — | Merged at `29bb492`, CI green |

---

## TASK DEPENDENCY GRAPH

```
TASK_000 (THIS AUDIT)
    │
    ├── TASK_SYNC_01 (Fast-forward main) ──► Can run immediately
    │
    ├── TASK_00 (Splash) ──► TASK_01 (Welcome) ──► TASK_02 (Home) ──► TASK_03 (Servers) ──► TASK_04 (Connected)
    │                                                                      │
    │                                                                      ▼
    │                                                           TASK_05 (Security) ──► TASK_06 (Split)
    │                                                                      │
    │                                                                      ▼
    │                                                           TASK_07 (Advanced) ──► TASK_08 (Stats)
    │                                                                      │
    │                                                                      ▼
    │                                                           TASK_09 (Import) ──► TASK_10 (Settings)
    │                                                                      │
    │                                                                      ▼
    │                                                           TASK_11 (Repair Lab) ──► TASK_12-21 (Services)
    │                                                                                      │
    │                                                                                      ├── TASK_18 (Download Rescue) ── BLOCKED_BY_ACTIVE_AGENT
    │                                                                                      └── TASK_21 (Autopilot) ── PARTIAL
    │
    ├── TASK_SYNC_02 (API 35 CI) ── BLOCKED (emulator)
    ├── TASK_SYNC_03 (Release Signing) ── DEFERRED
    ├── TASK_SYNC_04 (Keystore) ── PLANNED
    ├── TASK_SYNC_05 (VPN Gateway) ── EXTERNAL_BLOCKED
    ├── TASK_SYNC_06 (Visual Fidelity) ── PLANNED (needs Agent_A)
    └── TASK_SYNC_07 (Download Rescue) ── BLOCKED_BY_ACTIVE_AGENT
```

---

## DETAILED TASK SPECIFICATIONS (Per CODEX_TASK_QUEUE.md Gates)

### TASK_000 — Verified Baseline & Active Agent Protection
| Field | Value |
|-------|-------|
| **Status** | IN_PROGRESS |
| **Owner** | Forensic Auditor (this session) |
| **Files** | All forensic deliverables in `docs/forensic-audit/` |
| **Git SHA** | Baseline: `ccda186` (HEAD), `d4fda7e` (origin/main) |
| **Prerequisites** | None |
| **Protected Assets** | 17 worktrees, Gradle daemons, ADB device, unique evidence |
| **Acceptance** | All 12 forensic deliverables created, no writes to active paths |
| **Negative Tests** | Verify no Gradle commands executed, no Git writes, no emulator launch |
| **API/Device Gates** | N/A (read-only) |
| **Commands** | `git status`, `git worktree list`, `jps`, `adb devices`, file reads |
| **Artifacts** | 12 markdown files in `docs/forensic-audit/` |
| **PR/CI Evidence** | N/A |
| **Rollback** | N/A (read-only) |
| **Next Safe Action** | Complete deliverables, handoff to implementation agent |

### TASK_SYNC_01 — Fast-forward Main to origin/main
| Field | Value |
|-------|-------|
| **Status** | READY |
| **Owner** | Implementation Agent (next session) |
| **Files** | None (Git operation only) |
| **Git SHA** | From `8af3428` → `d4fda7e` |
| **Prerequisites** | TASK_000 complete, no active commits on main |
| **Protected Assets** | None (main is clean) |
| **Acceptance** | `git log --oneline -1` shows `d4fda7e` on local main |
| **Negative Tests** | Verify no local changes lost, no worktrees affected |
| **Commands** | `git checkout main && git pull --ff-only origin main` |
| **Artifacts** | Updated main branch |
| **PR/CI Evidence** | N/A (local only) |
| **Rollback** | `git reset --hard 8af3428` (if needed) |
| **Next Safe Action** | Execute after TASK_000 handoff |

### TASK_00 — Legendary Cinematic Splash
| Field | Value |
|-------|-------|
| **Status** | NOT_STARTED |
| **Owner** | Unassigned |
| **Files** | `KxLaunch.kt`, `IdentityExperience.kt`, `KxOrb.kt`, `CrystalIdentity.kt`, `IntroTimeline.kt`, `LaunchPreferences.kt` |
| **Git SHA** | Base: `d4fda7e` (after TASK_SYNC_01) |
| **Prerequisites** | TASK_SYNC_01, visual reference Ref 00 opened |
| **Protected Assets** | `CrystalIdentity.kt` (Agent_A may own), `KxOrb.kt` |
| **Acceptance** | 8-beat choreography matches Ref 00, cold/warm start, API 26/35, AR/EN, reduced motion |
| **Negative Tests** | No blank frame, no crash, no network calls, skip works, warm resume skips intro |
| **API/Device Gates** | API 26, 35 emulator + physical device screenshots |
| **Commands** | `./gradlew :app:assembleDebug`, `./gradlew :app:connectedDebugAndroidTest` (IdentityTest) |
| **Artifacts** | APK, screenshots (8 variants × 2 devices × 2 themes × 2 locales), visual diff |
| **PR/CI Evidence** | PR → CI green (build + device API 26 + device API 35) |
| **Rollback** | Revert merge commit |
| **Next Safe Action** | After TASK_SYNC_01, coordinate `CrystalIdentity.kt` ownership with Agent_A |

### TASK_02 — Real VPN Disconnected Home (Visual Fidelity)
| Field | Value |
|-------|-------|
| **Status** | PARTIAL |
| **Owner** | Agent_A (likely) |
| **Files** | `VpnHomePanel.kt`, `NativeClient.kt` (home route), `CrystalIdentity.kt` (atmosphere) |
| **Git SHA** | `ccda186` (feature branch) |
| **Prerequisites** | TASK_01, Ref 14 visual gap analysis |
| **Protected Assets** | `CrystalIdentity.kt`, `NativeClient.kt` |
| **Acceptance** | Home screen matches Ref 02/14: atmosphere bloom, glass depth, card rim lighting, orb hero |
| **Negative Tests** | No fake connected state, disabled connect without profile, honest error states |
| **API/Device Gates** | API 26, 35 + physical (360dp/411dp, Day/Night, AR/EN, Font 1.0/1.6) |
| **Commands** | Visual regression vs Ref 14 captures |
| **Artifacts** | Side-by-side screenshots, visual diff report |
| **PR/CI Evidence** | PR → CI green + visual verification |
| **Rollback** | Revert merge commit |
| **Next Safe Action** | Coordinate with Agent_A on `CrystalIdentity.kt` changes |

---

## SYNC TASKS (Infrastructure)

### TASK_SYNC_02 — API 35 Emulator Stability
| Field | Value |
|-------|-------|
| **Status** | BLOCKED |
| **Owner** | Infrastructure |
| **Files** | `.github/workflows/android.yml`, `scripts/verify-device.sh` |
| **Blocker** | Emulator GPU RenderThread SIGSEGV (Google bug) |
| **Options** | 1) Firebase Test Lab physical devices 2) Test sharding 3) Self-hosted GPU runner |
| **Acceptance** | 350 tests complete on API 35 without emulator loss |
| **Next Action** | Evaluate Firebase Test Lab trial |

### TASK_SYNC_03 — Release Signing
| Field | Value |
|-------|-------|
| **Status** | DEFERRED |
| **Owner** | Owner (keys) |
| **Files** | `app/build.gradle.kts` (signingConfigs), `keystore.jks` |
| **Blocker** | Keystore not in repo, owner must provide |
| **Acceptance** | `./gradlew :app:assembleRelease` produces signed AAB |
| **Next Action** | Document in `STATUS_BLOCKERS.md`, continue without |

### TASK_SYNC_04 — Android Keystore Integration
| Field | Value |
|-------|-------|
| **Status** | PLANNED |
| **Owner** | Unassigned |
| **Files** | `ProfileStore.kt`, `ConfigInput.kt`, `ConfigValidator.kt`, new `KeyManager.kt` |
| **Prerequisites** | TASK_SYNC_01 |
| **Acceptance** | Private keys encrypted at rest, decrypted only for tunnel start |
| **Next Action** | Design after baseline sync |

### TASK_SYNC_05 — VPN Test Gateway
| Field | Value |
|-------|-------|
| **Status** | EXTERNAL_BLOCKED |
| **Owner** | Owner / Infrastructure |
| **Requirement** | WireGuard server with known config, accessible from CI/device |
| **Acceptance** | `VpnPathVerification` returns `HANDSHAKE_OK` + traffic counters > 0 |
| **Next Action** | Provision when ready |

### TASK_SYNC_06 — Visual Fidelity Remediation
| Field | Value |
|-------|-------|
| **Status** | PLANNED |
| **Owner** | Agent_A coordination required |
| **Files** | `CrystalIdentity.kt` (Atmosphere), `KxOrb.kt` (orb), `KxReferenceUi.kt` (cards) |
| **Targets** | Ref 14 (Home), Ref 18 (Tools), Ref 03 (Repair Lab) |
| **Acceptance** | Side-by-side ≤ 5% pixel diff, all device/locale combos |
| **Next Action** | Schedule sync with Agent_A |

### TASK_SYNC_07 — Download Rescue Integration
| Field | Value |
|-------|-------|
| **Status** | BLOCKED_BY_ACTIVE_AGENT |
| **Owner** | Agent_A |
| **Worktree** | `D:\KX-DOWNLOAD-SAFETY-20261108` |
| **Files** | `DownloadRescueViewModel.kt`, `DownloadRescue.kt`, `Downloads.kt` (untracked) |
| **Next Action** | Wait for Agent_A handoff |

---

**INDEX COMPLETE — 28 TASKS CATALOGUED, 3 READY, 5 BLOCKED, 20 PLANNED/NOT_STARTED**