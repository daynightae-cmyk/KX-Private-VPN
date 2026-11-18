# INTEGRATION STRATEGY
**Generated:** 2026-11-15 11:05:00 UAE  
**Purpose:** Safe, collision-free integration of all unique work into `main`  

---

## GUIDING PRINCIPLES

1. **NO DESTRUCTIVE OPERATIONS** — No `reset --hard`, `clean`, `push --force`, worktree deletion
2. **ACTIVE AGENT PROTECTION** — Never modify files owned by live agents (see ACTIVE_AGENT_OWNERSHIP_MATRIX.md)
3. **EVIDENCE-BASED MERGES** — Only merge after CI green + visual verification + runtime proof
4. **FAST-FORWARD PREFERRED** — Prefer FF merges; rebase feature branches onto updated main
5. **ONE SLICE AT A TIME** — Single PR → CI → merge → verify → next

---

## PHASE 0: BASELINE SYNCHRONIZATION (IMMEDIATE, SAFE)

### 0.1 Fast-Forward Main
```bash
cd K:\KX-Private-VPN
git checkout main
git pull --ff-only origin main
# Result: main at d4fda7e (PR #49), clean working tree
```

### 0.2 Push Local-Ahead Commits
```bash
# codex/task-02-vpn-transport has 2 local commits not on origin
git checkout codex/task-02-vpn-transport
git push origin codex/task-02-vpn-transport
```

### 0.3 Verify Origin/Main State
```bash
git fetch origin
git log --oneline -5 origin/main
# Confirm d4fda7e is latest
```

**Risk:** NONE — read-only operations on main, push to feature branch only.

---

## PHASE 1: CI & SAFETY FIXES (LOW CONFLICT, HIGH VALUE)

### 1.1 `fix/api35-emulator-stability` — **MERGED_VERIFIED (PR #18)**
- **Status:** Merged to main at `fa34b67` (PR #18)
- **Change:** Disable CI emulator IME that occluded taps
- **Files:** `android/scripts/verify-device.sh` (IME disable logic)
- **Test:** API 35 emulator suite completion
- **Conflict Risk:** NONE — touches only CI script
- **Evidence:** CI green on API 26/35, merged to main

### 1.2 `fix/production-visual-hardening-20261107` (PR Ready)
- **Change:** Verify visual clipping + block profile removal on failed VPN stop
- **Files:** `VpnScreens.kt`, `VpnController.kt` (STOP_FAILED handling)
- **Test:** Visual regression + VpnFlowTest.unresolvedStopOffersRetry
- **Conflict Risk:** LOW — touches VPN UI + controller (coordinate with Agent_A)

### 1.3 `fix/device-suite-robustness` (PR Ready)
- **Change:** Accept already authorized tree in Ghost Hunter flow
- **Files:** `GhostHunterScreenTest.kt`, `GhostHunterScreen.kt`
- **Test:** GhostHunterFlowTest
- **Conflict Risk:** LOW — isolated feature

### 1.4 `test(android): instrumented suite survival` — **MERGED_VERIFIED (PR #23)**
- **Status:** Merged to main at `a70d89d` (PR #23)
- **Change:** Suite survives real-device and renderer conditions
- **Files:** CI configuration, test bounds, emulator stability
- **Test:** Full instrumented suite completion on API 26/35
- **Conflict Risk:** LOW
- **Evidence:** CI green, merged to main

### 1.5 `Harden Autopilot contrast, emblem entry` — **MERGED_VERIFIED (PR #27)**
- **Status:** Merged to main at `29bb492` (PR #27)
- **Change:** Autopilot contrast + emblem entry + approved history writes
- **Files:** Autopilot UI, launch preferences, history writes
- **Test:** Visual regression + launch tests
- **Conflict Risk:** LOW
- **Evidence:** CI green, merged to main

---

## PHASE 2: VISUAL FIDELITY FIXES (COORDINATION REQUIRED)

### 2.1 `codex/task-07-advanced-network` — Glyph Size Fix
- **Change:** Pass size modifier to protocol card glyph
- **Files:** `AdvancedNetworkScreen.kt` (ServiceGlyph usage)
- **Visual Reference:** Ref 07 (`07-advanced-network_5b3214f4.png`)
- **Test:** AdvancedNetworkScreenTest + screenshot comparison
- **Coordination:** Agent_A may be editing `AdvancedNetworkScreen.kt`

### 2.2 `codex/task-05-security-center` — Compact Rows
- **Change:** Compact security rows to owner reference proportions
- **Files:** `SecurityScreens.kt`, `SecurityPolicy.kt`
- **Visual Reference:** Ref 05 (`05-security-center_d0a1f09a.png`)
- **Test:** SecurityCenterTest + screenshot comparison
- **Coordination:** Agent_A may be editing `SecurityScreens.kt`

### 2.3 `codex/task-11-repair-lab` — Day Mode Accents
- **Change:** Keep Repair Lab accents legible in day mode
- **Files:** `RepairLabScreen.kt`, `RepairGlyph.kt`
- **Visual Reference:** Ref 03 (`03-repair-lab-hub.png`)
- **Test:** RepairLabScreenTest + screenshot comparison
- **Coordination:** Agent_A may be editing `RepairLabScreen.kt`

### 2.4 `codex/task-12-storage-xray` — Media Permission
- **Change:** Grant media permission with waited shell output
- **Files:** `StorageXrayScreenTest.kt`, `StorageXrayScreen.kt`
- **Test:** StorageXrayDeviceTest (requires media permission)
- **Coordination:** LOW — test infrastructure only

### 2.5 `codex/task-13-ghost-hunter` — Dark Mode Tones
- **Change:** Darken Ghost status tones on light glass
- **Files:** `GhostHunterScreen.kt`, `CrystalIdentity.kt` (color tokens)
- **Test:** GhostHunterScreenTest + visual comparison
- **Coordination:** MEDIUM — touches `CrystalIdentity.kt` (core design system)

### 2.6 `codex/visual-consumer-ui` — Import Action Coloring
- **Change:** Color the import action + record final device pass
- **Files:** `ImportScreen` (in `NativeClient.kt`), `GlassAction` usage
- **Test:** Visual comparison + ImportScreen test
- **Coordination:** LOW — isolated UI change

---

## PHASE 3: CORE FEATURE INTEGRATION (REBASE REQUIRED)

### 3.1 `codex/task-02-vpn-transport` — Real WireGuard Transport
- **Status:** Local `fb8f8b3` is **ahead 2** of origin `f4cb157`
- **Action:** Push local commits → PR → CI → merge
- **Files:** `VpnController.kt`, `WireGuardVpnGateway.kt`, `VpnSessionService.kt`, `VpnModels.kt`
- **Test:** VpnFlowTest, VpnSessionServiceTest, VpnControllerTest, **real tunnel proof**
- **Blocker:** Requires authorized VPN gateway for `E2E_REAL_TUNNEL`
- **Deferral:** Mark `EXTERNAL_BLOCKED` for tunnel proof; merge code with honest UI state

### 3.2 `feature/kx-dyad-cache-04-20-native-20261109` — API 35 Full Suite
- **Action:** Rebase onto main (`d4fda7e`) → PR → CI
- **Files:** `.github/workflows/android.yml` (timeout), `verify-device.sh`
- **Test:** Full 350-test API 35 suite completion
- **Conflict Risk:** MEDIUM — CI config changes

### 3.3 `feature/kx-freebuff-service-completion-20261108` — 252-Test Suite
- **Action:** Rebase onto main → PR → CI
- **Files:** CI config, test bounds
- **Test:** 252-test device suite completion
- **Conflict Risk:** MEDIUM — CI config changes

### 3.4 `server-switch-truth` — Saved Server Switching
- **Action:** Rebase onto main → PR → CI
- **Files:** `ServerSelectionScreen.kt`, `ClientViewModel.select()`, `VpnController.reconnect()`
- **Test:** ServerSelectionScreenTest + integration test
- **Conflict Risk:** LOW — isolated feature

### 3.5 `settings-truth-premium` — Notification Truth + Premium
- **Action:** Rebase onto main → PR → CI
- **Files:** `SettingsScreen.kt`, `VpnSessionService.kt`, `ProScreen.kt`
- **Test:** SettingsScreenTest + ProScreenTest
- **Conflict Risk:** LOW — isolated feature

### 3.5 `feat/session-shield-customer-ux` — Session Shield Privacy
- **Action:** Rebase onto main → PR → CI
- **Files:** `SessionShieldScreen.kt`, `SessionShield.kt`, `SessionShieldViewModel.kt`
- **Test:** SessionShieldScreenTest + SessionShieldTest
- **Conflict Risk:** LOW — isolated feature

### 3.6 `feature/task21-repair-autopilot` — Repair Autopilot
- **Action:** Rebase onto main → PR → CI
- **Files:** `AutopilotScreen.kt`, `Autopilot.kt`, `RepairJournal.kt`, `RepairMeasurement.kt`
- **Test:** AutopilotScreenTest + AutopilotTest + RepairJournalDeviceTest
- **Conflict Risk:** MEDIUM — touches Repair Lab infrastructure

---

## PHASE 4: BLOCKED BY ACTIVE AGENTS (DO NOT PROCEED)

| Branch | Worktree | Agent | Required Action |
|--------|----------|-------|-----------------|
| `fix/download-export-safety-20261108` | `D:\KX-DOWNLOAD-SAFETY-20261108` | Agent_A | Wait for explicit handoff; 3 files untracked |
| `feature/kx-touch-to-enter-physical-20261106` | `D:\KNOUX-Workspace\KX-PRIVATE-ENTRY-20261106` | Agent_C (external repo) | Coordinate via GitHub issue; different repo |
| `integration/kx-consolidation-20261106` | `D:\KX-CONSOLIDATE-20261106` | Agent_A | Wait for completion |
| `feature/kx-guardian-20261108` | `K:\KX-GUARDIAN-20261108` | Agent_A | Rebase (12 behind) + coordinate |

---

## DEPENDENCY GRAPH

```
main (d4fda7e)
├── ✅ MERGED_VERIFIED (Phase 1 Complete)
│   ├── fix/api35-emulator-stability (PR #18) — `fa34b67`
│   ├── instrumented suite survival (PR #23) — `a70d89d`
│   ├── Harden Autopilot/entry (PR #27) — `29bb492`
│   └── (PR #49 Smart Repair) — `d4fda7e`
├── Phase 1 Remaining (CI/Safety) ──→ merge sequentially
│   ├── fix/production-visual-hardening-20261107
│   └── fix/device-suite-robustness
├── Phase 2 (Visual) ──→ merge after Phase 1
│   ├── codex/task-07-advanced-network
│   ├── codex/task-05-security-center
│   ├── codex/task-11-repair-lab
│   ├── codex/task-12-storage-xray
│   ├── codex/task-13-ghost-hunter
│   └── codex/visual-consumer-ui
├── Phase 3 (Core Features) ──→ merge after Phase 2
│   ├── codex/task-02-vpn-transport (push local first)
│   ├── feature/kx-dyad-cache-04-20-native-20261109
│   ├── feature/kx-freebuff-service-completion-20261108
│   ├── server-switch-truth
│   ├── settings-truth-premium
│   ├── feat/session-shield-customer-ux
│   └── feature/task21-repair-autopilot
└── Phase 4 (Blocked) ──→ wait for agent handoff
```

---

## MERGE GATES (PER PR)

| Gate | Command | Required | Evidence |
|------|---------|----------|----------|
| Format/Lint | `./gradlew :app:lintDebug` | Exit 0 | Report HTML |
| Unit Tests | `./gradlew :app:testDebugUnitTest` | Exit 0, count > 0 | Test report |
| Compile Android Tests | `./gradlew :app:assembleDebugAndroidTest` | Exit 0 | APK built |
| Instrumented API 26 | `./gradlew :app:connectedDebugAndroidTest` (API 26) | Exit 0, 316 tests | Test report + screenshots |
| Instrumented API 35 | `./gradlew :app:connectedDebugAndroidTest` (API 35) | Exit 0, 350 tests | Test report + screenshots |
| Visual Comparison | Manual + automated | Ref vs runtime ≤ 5% diff | Side-by-side captures |
| Real Tunnel Proof | `VpnPathVerification` | Handshake + traffic | `VpnPathEvidence` log |
| SonarCloud | PR analysis | Quality Gate PASS (A Rating) | SonarCloud URL |

**No merge if ANY gate fails.** Do not skip tests or weaken thresholds.

---

## ROLLBACK PROCEDURE

If a merge introduces regression:
1. `git revert <merge-commit>` on main
2. Push revert → CI verifies clean state
3. Document in `EXECUTION_LEDGER.md` with SHA, reason, impact
4. Re-open PR with fix

---

## NEXT SAFE ACTION FOR THIS SESSION

**READ-ONLY COMPLETE.** Implementation requires:
1. Agent_A coordination (Gradle daemons, core VPN files)
2. Fast-forward main to `d4fda7e` (safe, no conflicts)
3. Push local `codex/task-02-vpn-transport` commits
4. Then Phase 1 PRs can begin

**THIS SESSION STOPS AT DOCUMENTATION.** All deliverables in `docs/forensic-audit/`.