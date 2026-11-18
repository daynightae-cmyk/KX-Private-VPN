# KX PRIVATE VPN — SECOND-GENERATION MASTER EXECUTION PROMPT
**Generated:** 2026-11-15 12:50:00 UAE  
**Source:** Forensic audit of `K:\KX-Private-VPN` at `ccda186` (HEAD) / `d4fda7e` (origin/main)  
**Status:** **DRAFT / NOT_INTEGRATED** — Created in `docs/forensic-audit/` to avoid active agent collision  

---

## ⚠️ CRITICAL SAFETY PREAMBLE

**READ THIS FIRST BEFORE ANY ACTION:**

1. **ACTIVE AGENT COLLISION:** Gradle daemons running at PID 8852 (1.6 GB) and 16188. **DO NOT RUN GRADLE COMMANDS.** Agent_A owns the build environment.
2. **17 ACTIVE WORKTREES:** All catalogued in `BRANCH_WORKTREE_MATRIX.md`. **DO NOT MODIFY, DELETE, OR REBASE ANY WORKTREE.**
3. **3 UNTRACKED ACTIVE FILES** in `D:\KX-DOWNLOAD-SAFETY-20261108` (Download Rescue). **DO NOT TOUCH.**
4. **CORE FILES OWNED BY AGENT_A:** `CrystalIdentity.kt`, `KxOrb.kt`, `NativeClient.kt`, `ClientViewModel.kt`, `VpnController.kt`, `WireGuardVpnGateway.kt`. **COORDINATE BEFORE EDITING.**
5. **PHYSICAL DEVICE ATTACHED:** R9ZR20HWHQY via ADB. Coordinate before `adb install`.
6. **ORIGIN/MAIN AHEAD:** Current HEAD `ccda186` is 1 commit behind `d4fda7e`. **FAST-FORWARD FIRST.**

**THIS PROMPT IS FOR THE NEXT IMPLEMENTATION AGENT. EXECUTE IN ORDER. NO SHORTCUTS.**

---

## 1. VERIFIED REALITY (Exact Paths, SHAs, Versions)

### Repository
| Property | Value |
|----------|-------|
| Canonical Root | `K:\KX-Private-VPN` |
| Git Dir | `K:\KX-Private-VPN\.git` |
| Remote | `https://github.com/daynightae-cmyk/KX-Private-VPN.git` |
| Current HEAD | `ccda1860a46efbb85f206c6fe0bd6e742ac7d311` |
| Current Branch | `feature/kx-reference-09-15-smart-repair-20261110` |
| origin/main | `d4fda7e` (Merge PR #49) |
| Behind main | 1 commit (fast-forward) |

### Android Module
| Property | Value |
|----------|-------|
| Project Root | `K:\KX-Private-VPN\android` |
| App Module | `K:\KX-Private-VPN\android\app` |
| Application ID | `com.knoux.kxprivatevpn` |
| Namespace | `com.knoux.kxprivatevpn` |
| minSdk | 26 |
| compileSdk / targetSdk | 36 |
| AGP | 8.13.2 |
| Kotlin | 2.2.21 |
| Compose BOM | 2025.04.01 |
| JDK | 17 |
| WireGuard | `com.wireguard.android:tunnel:1.0.20260102` |

### Key Source Files (Exact Paths)
```
VPN Core:
  android/app/src/main/java/com/knoux/kxprivatevpn/platform/vpn/VpnController.kt
  android/app/src/main/java/com/knoux/kxprivatevpn/platform/vpn/WireGuardVpnGateway.kt
  android/app/src/main/java/com/knoux/kxprivatevpn/platform/vpn/VpnModels.kt
  android/app/src/main/java/com/knoux/kxprivatevpn/platform/vpn/VpnSessionService.kt
  android/app/src/main/java/com/knoux/kxprivatevpn/platform/vpn/VpnSessionControl.kt
  android/app/src/main/java/com/knoux/kxprivatevpn/platform/vpn/VpnScreens.kt
  android/app/src/main/java/com/knoux/kxprivatevpn/platform/vpn/VpnPathVerification.kt

UI/Navigation:
  android/app/src/main/java/com/knoux/kxprivatevpn/MainActivity.kt
  android/app/src/main/java/com/knoux/kxprivatevpn/launch/KxLaunch.kt
  android/app/src/main/java/com/knoux/kxprivatevpn/launch/IdentityExperience.kt
  android/app/src/main/java/com/knoux/kxprivatevpn/launch/IntroTimeline.kt
  android/app/src/main/java/com/knoux/kxprivatevpn/features/NativeClient.kt
  android/app/src/main/java/com/knoux/kxprivatevpn/features/ClientViewModel.kt

Design System:
  android/app/src/main/java/com/knoux/kxprivatevpn/designsystem/CrystalIdentity.kt
  android/app/src/main/java/com/knoux/kxprivatevpn/designsystem/KxOrb.kt
  android/app/src/main/java/com/knoux/kxprivatevpn/designsystem/GlassComponents.kt
  android/app/src/main/java/com/knoux/kxprivatevpn/designsystem/KxReferenceUi.kt

Config/Storage:
  android/app/src/main/java/com/knoux/kxprivatevpn/data/config/ProfileStore.kt
  android/app/src/main/java/com/knoux/kxprivatevpn/data/config/ConfigValidator.kt
  android/app/src/main/java/com/knoux/kxprivatevpn/data/config/ConfigInput.kt

Manifest:
  android/app/src/main/AndroidManifest.xml

CI/CD:
  .github/workflows/android.yml
  android/scripts/verify-device.sh
  android/gradle/libs.versions.toml
```

### Visual References (Immutable — Owner Authority)
```
public/assets/aistudio/00-brand-logo_c0b92dd8.png      # Brand orb (Ref 00)
public/assets/aistudio/01-welcome_464a7c4f.png        # Welcome (Ref 01)
public/assets/aistudio/02-home-disconnected_825c75b0.png  # Home disconnected (Ref 02)
public/assets/aistudio/03-server-selection_8d2c742a.png   # Server selection (Ref 03)
public/assets/aistudio/04-connected-session_f0cfe658.png  # Connected session (Ref 04)
public/assets/aistudio/05-security-center_d0a1f09a.png    # Security center (Ref 05)
public/assets/aistudio/06-split-tunneling_c9265fd7.png    # Split tunneling (Ref 06)
public/assets/aistudio/07-advanced-network_5b3214f4.png   # Advanced network (Ref 07)
public/assets/aistudio/08-statistics_a6c474b9.png         # Statistics (Ref 08)
public/assets/aistudio/09-import-configuration_670a2aea.png # Import config (Ref 09)
public/assets/aistudio/10-profile-settings_75031956.png   # Profile settings (Ref 10)
```
**Owner 20-ref set:** `C:\Users\day night\Downloads\KX_VISUAL_REFERENCE_MASTER_20\references\` (if accessible)

### Evidence Locations
```
CI Captures:     docs/evidence/ci-*-api*/device-screenshots/
Phone Captures:  docs/evidence/task16/phone-evidence/, task17/phone/, task18/, task21/phone-evidence/
Visual Compare:  docs/evidence/visual-reference-20261108/runtime/identity-evidence/
                 docs/evidence/visual-reference-20261109/
```

---

## 2. TOP RISKS & BLOCKERS (Must Respect)

| # | Risk | Impact | Mitigation |
|---|------|--------|------------|
| 1 | **Agent_A owns Gradle + core files** | Any Gradle command kills their build; file edits cause conflicts | **Coordinate via `docs/STATUS_BLOCKERS.md` before ANY Gradle/file write** |
| 2 | **API 35 emulator loss** | CI gate fails, blocks merge | Use Firebase Test Lab / sharding / self-hosted GPU runner |
| 3 | **SonarCloud C Rating** | Quality gate fails | Push local fixes (already in `ccda186`) → new scan |
| 4 | **No VPN test gateway** | `E2E_REAL_TUNNEL` impossible | Provision WireGuard server; mark EXTERNAL_BLOCKED until then |
| 5 | **Visual fidelity gaps (Ref 14, 18, 03)** | Owner rejects Gemini visuals | Coordinate with Agent_A on `CrystalIdentity.kt`/`KxOrb.kt` |
| 6 | **Release signing not configured** | Cannot produce signed AAB | Owner provides keystore; defer in `STATUS_BLOCKERS.md` |
| 7 | **Private keys plaintext in DataStore** | Security HIGH | Implement Android Keystore after baseline sync |

---

## 3. EXECUTION ROADMAP (Sequential, Evidence-Gated)

### PHASE 0: BASELINE SYNC (Immediate, Safe, No Coordination)
```bash
cd K:\KX-Private-VPN
git checkout main
git pull --ff-only origin main          # → d4fda7e
git checkout codex/task-02-vpn-transport
git push origin codex/task-02-vpn-transport  # Push 2 local commits
```
**Gates:** Git operations only. No Gradle. No file edits.
**Evidence:** `git log --oneline -1` shows `d4fda7e` on main.

---

### PHASE 1: CI & SAFETY FIXES (Low Conflict, High Value)
Create PRs for each, run CI, merge sequentially:

| PR | Branch | Key Files | Gate |
|----|--------|-----------|------|
| 1 | `fix/api35-emulator-stability` | `android/scripts/verify-device.sh` (IME disable) | CI green |
| 2 | `fix/production-visual-hardening-20261107` | `VpnScreens.kt`, `VpnController.kt` (STOP_FAILED) | CI + Visual |
| 3 | `fix/device-suite-robustness` | `GhostHunterScreenTest.kt`, `GhostHunterScreen.kt` | CI green |

**Coordination:** PR 2 touches `VpnController.kt` (Agent_A). Comment on PR requesting review.

---

### PHASE 2: VISUAL FIDELITY FIXES (Requires Agent_A Coordination)
**DO NOT START until Agent_A confirms `CrystalIdentity.kt`/`KxOrb.kt` available.**

| PR | Branch | Target Ref | Key Files | Visual Gate |
|----|--------|------------|-----------|-------------|
| 4 | `codex/task-07-advanced-network` | Ref 07 | `AdvancedNetworkScreen.kt` (ServiceGlyph size) | Ref 07 match |
| 5 | `codex/task-05-security-center` | Ref 05 | `SecurityScreens.kt` (compact rows) | Ref 05 match |
| 6 | `codex/task-11-repair-lab` | Ref 03 | `RepairLabScreen.kt`, `RepairGlyph.kt` (day accents) | Ref 03 match |
| 7 | `codex/task-12-storage-xray` | — | `StorageXrayScreenTest.kt` (permission wait) | CI green |
| 8 | `codex/task-13-ghost-hunter` | — | `GhostHunterScreen.kt` (dark tones) | Visual |
| 9 | `codex/visual-consumer-ui` | Ref 09 | `NativeClient.kt` (ImportScreen GlassAction color) | Ref 09 match |

**Visual Verification:** Each PR must include side-by-side screenshots (360dp/411dp, Day/Night, EN/AR, Font 1.0/1.6) vs owner reference.

---

### PHASE 3: CORE FEATURE INTEGRATION (Rebase Required)
Rebase each onto updated `main` (`d4fda7e`), then PR:

| Feature | Branch | Key Files | Blockers |
|---------|--------|-----------|----------|
| Real WireGuard Transport | `codex/task-02-vpn-transport` | `VpnController.kt`, `WireGuardVpnGateway.kt`, `VpnSessionService.kt` | **EXTERNAL_BLOCKED** (no gateway) — merge code, mark tunnel proof deferred |
| API 35 Full Suite | `feature/kx-dyad-cache-04-20-native-20261109` | CI config | Rebase conflicts |
| 252-Test Suite | `feature/kx-freebuff-service-completion-20261108` | CI config | Rebase conflicts |
| Server Switch Truth | `server-switch-truth` | `ServerSelectionScreen.kt`, `ClientViewModel.select()` | Rebase |
| Settings Truth Premium | `settings-truth-premium` | `SettingsScreen.kt`, `VpnSessionService.kt` | Rebase |
| Session Shield | `feat/session-shield-customer-ux` | `SessionShieldScreen.kt`, `SessionShield.kt` | Rebase |
| Repair Autopilot | `feature/task21-repair-autopilot` | `AutopilotScreen.kt`, `Autopilot.kt` | Rebase |

---

### PHASE 4: BLOCKED (Wait for Handoff)
| Item | Worktree | Agent | Action |
|------|----------|-------|--------|
| Download Rescue | `D:\KX-DOWNLOAD-SAFETY-20261108` | Agent_A | Wait for explicit handoff |
| KX Touch Entry | `D:\KNOUX-Workspace\KX-PRIVATE-ENTRY-20261106` | Agent_C (external) | GitHub issue coordination |
| Consolidation | `D:\KX-CONSOLIDATE-20261106` | Agent_A | Wait for completion |
| Guardian | `K:\KX-GUARDIAN-20261108` | Agent_A | Rebase (12 behind) + coordinate |

---

## 4. PER-TASK EXECUTION TEMPLATE (Follow for EVERY Task)

### For Each Task:
```
1. CONFIRM BASELINE
   - git fetch origin
   - git status (clean?)
   - Verify no active Gradle daemons (jps -l) OR coordinate with Agent_A
   - Record current SHA in task evidence

2. SELECT UNOWNED GAP
   - Pick next task from PHASE order above
   - Verify no active agent owns target files (check ACTIVE_AGENT_OWNERSHIP_MATRIX.md)
   - Create feature branch from main (d4fda7e)

3. IMPLEMENT REAL FIX
   - Write code in feature branch
   - Follow existing patterns (VpnController, ClientViewModel, etc.)
   - NO mocks, NO fake states, NO random data
   - Honest UI: "UNAVAILABLE" / "PERMISSION REQUIRED" / "NOT CONFIGURED"

4. RUN RELEVANT TESTS
   - Unit: ./gradlew :app:testDebugUnitTest (coordinate Gradle!)
   - Lint: ./gradlew :app:lintDebug
   - Compile: ./gradlew :app:assembleDebugAndroidTest
   - Instrumented: ONLY if device/emulator free + coordinated

5. EXERCISE ANDROID RUNTIME
   - Install debug APK on physical device (R9ZR20HWHQY)
   - Manual smoke test: launch → navigate to feature → verify flow
   - Capture screenshots (adb exec-out screencap)

6. COMPARE VISUALS
   - Open owner reference PNG side-by-side with runtime screenshot
   - Check: 360dp/411dp, Day/Night, EN/AR, Font 1.0/1.6, Reduced motion
   - Document diffs ≤ 5% structural, 0% critical elements

7. RECORD SHA/ARTIFACTS
   - Commit with conventional message: `feat(scope): description`
   - Push feature branch
   - Record: implementation SHA, test counts, screenshot paths, visual diff %

8. REVIEW CI
   - Open PR → wait for GitHub Actions (build + device API 26 + device API 35)
   - SonarCloud scan → must be A Rating
   - NO MERGE if ANY gate fails

9. SAFELY INTEGRATE
   - Merge PR (squash or FF)
   - Verify origin/main updated
   - Post-merge: ./gradlew :app:testDebugUnitTest :app:lintDebug (verify clean)

10. NEXT
    - Update TASK_INDEX.md status
    - Move to next task in PHASE order
```

---

## 5. COMMANDS REFERENCE (Exact, Verified)

### Git
```bash
# Baseline sync
cd K:\KX-Private-VPN
git fetch origin
git checkout main
git pull --ff-only origin main

# Push local commits
git checkout codex/task-02-vpn-transport
git push origin codex/task-02-vpn-transport

# Create feature branch
git checkout -b feat/my-task main

# Check worktrees
git worktree list --porcelain
```

### Gradle (COORDINATE FIRST — Agent_A owns daemons)
```bash
cd K:\KX-Private-VPN\android

# Unit + Lint + Build (safe-ish if coordinated)
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest

# Instrumented (requires device/emulator coordination)
./gradlew :app:connectedDebugAndroidTest

# Focused test class
KX_TEST_CLASS="com.knoux.kxprivatevpn.VpnFlowTest" ./gradlew :app:connectedDebugAndroidTest

# Release build (when keystore available)
./gradlew :app:assembleRelease
```

### ADB / Device
```bash
# Device status
adb devices
adb shell getprop ro.build.version.sdk

# Install
adb install -r android/app/build/outputs/apk/debug/app-debug.apk

# Screenshot
adb exec-out screencap -p > screenshot.png

# Logcat
adb logcat -d > logcat.txt

# VPN consent intent (for testing)
adb shell am start -n com.knoux.kxprivatevpn/.MainActivity
```

### Visual Comparison
```bash
# Reference images
public/assets/aistudio/00-brand-logo_c0b92dd8.png
public/assets/aistudio/01-welcome_464a7c4f.png
# ... etc.

# Runtime captures (from CI/device)
docs/evidence/ci-*/device-screenshots/identity-evidence/*.png
docs/evidence/task16/phone-evidence/*.png
```

---

## 6. ACCEPTANCE CRITERIA PER GATE (No Exceptions)

| Gate | Command | Required Result | Evidence File |
|------|---------|-----------------|---------------|
| Format/Lint | `./gradlew :app:lintDebug` | Exit 0, 0 errors | `lint-results-debug.html` |
| Unit Tests | `./gradlew :app:testDebugUnitTest` | Exit 0, >150 tests, 0 failures | `testDebugUnitTest/index.html` |
| Compile Android Test | `./gradlew :app:assembleDebugAndroidTest` | Exit 0 | APKs in `outputs/apk/` |
| Instrumented API 26 | CI device job | Exit 0, 316 tests, 0 failures | `native-device-api-26` artifact |
| Instrumented API 35 | CI device job | Exit 0, 350 tests, 0 failures | `native-device-api-35` artifact |
| SonarCloud | PR analysis | Quality Gate PASS (A Rating) | SonarCloud URL |
| Visual Regression | Manual + automated | ≤5% pixel diff vs reference | Side-by-side + diff images |
| E2E Real Tunnel | `VpnPathVerification` + packet capture | Handshake + traffic > 0 + encrypted | `VpnPathEvidence` JSON + pcap |

**NO MERGE IF ANY GATE FAILS. NO SKIPPING TESTS. NO WEAKENING THRESHOLDS.**

---

## 7. VISUAL FIDELITY SPECIFICS (Owner References Are Law)

### Reference 00 — Brand Logo / Splash (Highest Priority)
- **File:** `public/assets/aistudio/00-brand-logo_c0b92dd8.png`
- **Implementation:** `KxOrb.kt` + `CrystalIdentity.kt` + `IdentityExperience.kt` + `IntroTimeline.kt`
- **8-Beat Choreography:** Photons → Ribbons → Orb Formation → Caustic Sweep → Halo Pulse → Satellites → Wordmark → Continuity
- **Reduced Motion:** Bypass animation, keep orb visible
- **Warm Restart:** Skip intro if `LaunchPreferences.hasSeenIntro()`

### Reference 14 / 02 — Home Disconnected
- **Files:** `public/assets/aistudio/02-home-disconnected_825c75b0.png` + Owner Ref 14
- **Gaps:** Flatter atmosphere, weaker neon ribbons/glow, shallower glass, compressed content height
- **Fix Target:** `CrystalIdentity.kt` (Atmosphere), `KxOrb.kt` (orb hero), `NativeClient.kt` (card layout)

### Reference 18 — Tools Catalog
- **File:** Owner Ref 18 (Tools Catalog nine destinations)
- **Gaps:** Cards shorter/dimmer, weaker luminous edges, compressed titles
- **Fix Target:** `KxReferenceUi.kt` (card rendering), `CrystalIdentity.kt` (glow)

### Reference 03 — Repair Lab
- **File:** Owner Ref 03 (Repair Lab hub)
- **Gaps:** Missing glowing platform/portal under orb, subdued neon card rims
- **Fix Target:** `RepairLabScreen.kt`, `RepairGlyph.kt`, `CrystalIdentity.kt` (floor glow)

### All Screens — Device/Locale Matrix
| Variant | Required |
|---------|----------|
| 360dp Day EN | ✅ |
| 360dp Night EN | ✅ |
| 360dp Day AR (RTL) | ✅ |
| 360dp Night AR (RTL) | ✅ |
| 411dp Day EN | ✅ |
| 411dp Night EN | ✅ |
| 411dp Day AR (RTL) | ✅ |
| 411dp Night AR (RTL) | ✅ |
| Font Scale 1.0 | ✅ |
| Font Scale 1.6 | ✅ |
| Reduced Motion | ✅ |

---

## 8. SECURITY HARDENING CHECKLIST (Post-Baseline)

| Item | File | Status | Action |
|------|------|--------|--------|
| Android Keystore for private keys | `ProfileStore.kt`, new `KeyManager.kt` | ❌ NOT DONE | Implement after TASK_SYNC_01 |
| EncryptedSharedPreferences for DataStore | All `*Store.kt` | ❌ NOT DONE | Jetpack Security crypto |
| Crash log exclusion (hs_err_pid*) | `.gitignore`, Crashlytics config | ❌ NOT DONE | Add to ignore + filter |
| Network Security Config (Pro backend) | `res/xml/network_security_config.xml` | ❌ NOT DONE | Domain allowlist + pinning |
| Config export (user portability) | `ProfileStore.kt` | ❌ NOT DONE | Encrypted export flow |

---

## 9. STATUS TRACKING FILES (Maintain These)

| File | Purpose | Update Frequency |
|------|---------|------------------|
| `docs/STATUS_BLOCKERS.md` | External blockers, agent coordination | Per blocker change |
| `docs/PROGRESS_LEDGER.md` | Task completion evidence | Per task merge |
| `docs/evidence/TASK_XX.md` | Per-task detailed evidence | Per task completion |
| `docs/forensic-audit/TASK_INDEX.md` | Master task status | Per task status change |

---

## 10. NEXT AGENT STARTUP CHECKLIST

```
☐ Read this prompt completely
☐ Read FORENSIC_BASELINE.md, ACTIVE_AGENT_OWNERSHIP_MATRIX.md, INTEGRATION_STRATEGY.md
☐ Verify git status: clean, on main at d4fda7e
☐ Verify no active Gradle daemons (jps -l) OR coordinate with Agent_A
☐ Verify ADB device free (adb devices)
☐ Open owner reference PNGs (public/assets/aistudio/*.png)
☐ Start with PHASE 0: Fast-forward main + push VPN transport commits
☐ Create PRs for PHASE 1 fixes
☐ WAIT for Agent_A coordination before PHASE 2 visual fixes
☐ Document every step in PROGRESS_LEDGER.md
☐ NO MERGE without all gates green
```

---

## 11. ESCALATION CONTACTS

| Issue | Contact Method |
|-------|----------------|
| Agent_A coordination | `docs/STATUS_BLOCKERS.md` + GitHub issue on repo |
| Owner visual approval | Owner-supplied reference images (this prompt + `CODEX_REFERENCE_VISUAL_CONTRACT.md`) |
| Infrastructure (emulator, device farm, gateway) | GitHub issue + owner notification |
| Security review | `SECURITY_PRIVACY_LEAK_REGISTER.md` findings |

---

**THIS PROMPT IS THE SINGLE SOURCE OF TRUTH FOR NEXT EXECUTION. ALL PATHS, SHAs, FILES, AND GATES ARE VERIFIED AGAINST ACTUAL REPOSITORY STATE AT `ccda186` / `d4fda7e`. DEVIATION REQUIRES EVIDENCE.**

**EXECUTE PHASE 0 NOW. NO QUESTIONS. EVIDENCE OR IT DIDN'T HAPPEN.**