# CI, SONARCLOUD & EMULATOR FAILURES
**Generated:** 2026-11-15 12:05:00 UAE  
**Source:** GitHub Actions runs, SonarCloud PR #40 analysis, `CI-BLOCKERS-20261108.md`  

---

## GITHUB ACTIONS WORKFLOW (`.github/workflows/android.yml`)

### Job: `build` (Ubuntu Latest, 20 min timeout)
| Step | Command | Status | Notes |
|------|---------|--------|-------|
| Checkout | `actions/checkout@v4` | ✅ PASS | |
| Setup Java | `actions/setup-java@v4` (Temurin 21) | ✅ PASS | |
| Setup Android | `android-actions/setup-android@v3` (API 36, Build Tools 35.0.0) | ✅ PASS | |
| Setup Gradle | `gradle/actions/setup-gradle@v4` | ✅ PASS | |
| Setup Python | `actions/setup-python@v5` (3.12) | ✅ PASS | For Pro backend tests |
| Verify Pro Authority | `python -m unittest discover -s backend/pro` | ✅ PASS | |
| Build & Test | `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest` | ✅ PASS | Working dir: `android` |
| Upload Artifacts | Reports + APKs | ✅ PASS | `native-build-evidence` |

**Status:** **CONSISTENTLY PASSING** on `origin/main` and feature branches.

---

### Job: `device` (Ubuntu Latest, 55 min timeout, Matrix API 26/35)

#### API 26 Configuration
| Parameter | Value |
|-----------|-------|
| Emulator Profile | Nexus 5 |
| Emulator Build | 15004761 (Emulator 36.4.10) |
| GPU Mode | `swiftshader_indirect` |
| Boot Timeout | 600s |
| Emulator Options | `-no-window -no-audio -no-boot-anim -no-snapshot -gpu swiftshader_indirect -skin 720x1280 -camera-back none -camera-front none` |
| Script | `bash scripts/verify-device.sh` |

**Results (Latest PASS):**
| CI Run | Tests | Status | Evidence |
|--------|-------|--------|----------|
| `ci-be93953-api26` | 316 | ✅ PASS | Screenshots + reports |
| `ci-f28291c-api26` | 316 | ✅ PASS | Screenshots + reports |
| `ci-initial-api26` | 316 | ✅ PASS | Screenshots + reports |

**Known Issue (Fixed):** `ToolsCatalogScreenTest.rtlReadingOrderMirrorsTheReferenceRows` failed at 3rd RTL row due to offscreen bounds inspection. **Fix:** Scroll each card into view before assertion. Reference PNG shows Media before Smart, Apps before Network in RTL.

---

#### API 35 Configuration
| Parameter | Value |
|-----------|-------|
| Emulator Profile | Nexus 5 |
| Emulator Build | 15004761 (Emulator 36.4.10) |
| GPU Mode | `swangle` (ANGLE/SwiftShader) |
| Boot Timeout | 600s |
| Emulator Options | `-no-window -no-audio -no-boot-anim -no-snapshot -gpu swangle -skin 720x1280 -camera-back none -camera-front none` |
| Script | `bash scripts/verify-device.sh` |

**Results (Latest FAIL):**
| CI Run | Tests Attempted | Tests Expected | Status | Failure |
|--------|-----------------|----------------|--------|---------|
| `ci-ef75300-api35` | 37 | 318 (later 350) | ❌ FAIL | Emulator lost |
| `ci-f376e4a-api35` | 0 | 316 | ❌ FAIL | Emulator lost |

**Failure Details (PR #40, Run 42484254384):**
```
ClientFlowTest.appearanceTapIsNotLostWhileTheLocaleWriteSettles
  → FAILED but XML failure node EMPTY
  → emulator-5554 disappeared from ADB
  → Cleanup: "Expected 318 tests, received 36"
  → No Java assertion stack trace
  → Host kernel: RenderThread SIGSEGV / QEMU signal 11
```

**Root Cause Analysis:**
1. **Emulator GPU Instability:** API 35 + `swiftshader_indirect` → RenderThread SIGSEGV (deprecated in Emulator 36.4.9)
2. **Mitigation Applied:** Switched to `swangle` (ANGLE) GPU mode for API 35 only
3. **Partial Success:** `swangle` reduces but **does not eliminate** emulator loss
4. **Timeout Pressure:** 350-test suite exceeds original 18-min budget; extended to 30-min + 15-min kill-after

**Current Status:** `swangle` mode allows more tests to run but emulator loss still occurs non-deterministically. **Not a proven app regression.**

---

### Job: `api35-focused-diagnostic` (PR #40 Only)
| Parameter | Value |
|-----------|-------|
| Trigger | `github.event_name == 'pull_request' && github.event.pull_request.number == 40` |
| Target Classes | `CacheFlowTest,ClientFlowTest` |
| Purpose | Isolate suspect tests in fresh emulator |

**Status:** Diagnostic only — does not gate merge.

---

## SONARCLOUD QUALITY GATE

### PR #40 Analysis (Run 128752581126)
| Metric | Threshold | Actual | Status |
|--------|-----------|--------|--------|
| Security Rating on New Code | A | **C** | ❌ FAIL |
| Reliability Rating | A | A | ✅ PASS |
| Maintainability Rating | A | A | ✅ PASS |
| Coverage | — | — | Not gated |
| Duplications | < 3% | — | Not gated |

### Issues Identified (Component: `daynightae-cmyk_KX-Private-VPN`, PR #40)
| Rule | File | Line | Description | Fix Applied |
|------|------|------|-------------|-------------|
| `pythonsecurity:S8707` | `android/scripts/prepare_brand_asset.py` | 33, 71 | CLI arbitrary path injection | ✅ Resolved to canonical paths only |
| `kotlin:S3776` | `android/app/src/main/java/com/knoux/kxprivatevpn/designsystem/CrystalIdentity.kt` | `KxOrb` | Cognitive complexity 58 > 45 | ✅ Extracted `drawKxOrbNativeShield` helper |
| `kotlin:S1479` | `android/app/src/main/java/com/knoux/kxprivatevpn/designsystem/KxReferenceUi.kt` | — | 31 `when` branches > 30 | ✅ Grouped mirrored glyphs |

### Local Verification (PR #40 Continuation)
```bash
# Python security test
python -m unittest discover -s android/scripts -p test_prepare_brand_asset_security.py -v
# Result: 4/4 passed

# Negative path injection attempts
python android/scripts/prepare_brand_asset.py --output K:\_KX_CI_PR40_20261108\should-not-exist.png
# Result: Exit 2, rejected

python android/scripts/prepare_brand_asset.py --master C:\Windows\win.ini
# Result: Exit 2, rejected
```

**Logo Integrity Verified:**
- Master SHA-256: `f336992dedba8f80580fc550735b0629c595f85d8c2a0587f08bf6f6fb2775e8`
- Runtime PNG SHA-256: `21d95dab9ecb6362319d4cc1ed49582c5562ae4dd28bad1a711b3172b4f07c70`

**SonarCloud Status:** **FIXES MERGED TO MAIN (d4fda7e)** — All three PR #40 issues resolved in current main:
1. `pythonsecurity:S8707` — Path validation in `prepare_brand_asset.py` ✅
2. `kotlin:S3776` — Cognitive complexity fix via `drawKxOrbNativeShield` helper ✅
3. `kotlin:S1479` — When branches grouped (SHIELD/SHIELD_CHECK, BACK/FORWARD) ✅

**Action Required:** Trigger new SonarCloud scan on `d4fda7e` (or latest main) to verify Quality Gate A Rating. No code changes needed — fixes are already in main.

---

## EMULATOR FAILURE FORENSICS

### Host-Level Evidence (from `verify-device.sh` heartbeat)
| Metric | Capture Method | Purpose |
|--------|----------------|---------|
| QEMU process state | `ps -eo pid,ppid,stat,rss,etimes,comm \| grep qemu` | Detect zombie/crashed emulator |
| QEMU wait status | `/proc/$pid/stat` field 52 | Identify SIGSEGV (11) / SIGABRT (6) |
| ADB device list | `adb devices -l` | Confirm emulator disappearance |
| cgroup memory events | `/sys/fs/cgroup/memory.events` | Detect OOM kills |
| Kernel logs | `dmesg` | RenderThread SIGSEGV, QEMU signal 11 |

### Emulator Loss Pattern
| Run | API | GPU Mode | Tests Run | Loss Point | Kernel Signal |
|-----|-----|----------|-----------|------------|---------------|
| 42484254384 (PR #40) | 35 | swiftshader_indirect | 37/318 | ClientFlowTest | RenderThread SIGSEGV |
| 42553240264 (later) | 35 | swangle | 267/324 | Timeout (18min) | — |
| Current `origin/main` | 35 | swangle | 0/350 | Immediate | QEMU signal 11 |

### Mitigations Attempted
| Mitigation | Result |
|------------|--------|
| Disable Vulkan | No effect |
| `swiftshader_indirect` → `swangle` (API 35 only) | Partial — reduces but not eliminates |
| Increase timeout (18min → 30min + 15min kill-after) | Allows more tests but loss still occurs |
| Disable animations | Not applied (kept for visual tests) |
| Disable IME (all emulators) | Applied — prevents tap occlusion |

### Remaining Options
1. **Physical Device Farm** — Firebase Test Lab / AWS Device Farm (real devices, no emulator GPU issues)
2. **Emulator 37.x with Fixed GPU** — Wait for Google to fix RenderThread in newer builds
3. **Headless CI Runner with GPU** — Self-hosted runner with proper GPU passthrough
4. **Test Sharding** — Split 350 tests across multiple emulator instances (parallel)

---

## BLOCKER SUMMARY FOR MERGE

| Blocker | Severity | Required for Merge | Status |
|---------|----------|-------------------|--------|
| API 35 Instrumented Tests PASS | **CRITICAL** | ✅ Required | ❌ BLOCKED (emulator loss) |
| SonarCloud Quality Gate (A Rating) | **CRITICAL** | ✅ Required | ⚠️ **FIXES IN MAIN** — Trigger re-scan on `d4fda7e` |
| API 26 Instrumented Tests PASS | **CRITICAL** | ✅ Required | ✅ PASS |
| Build + Lint + Unit Tests | **CRITICAL** | ✅ Required | ✅ PASS |
| Visual Regression Baseline | HIGH | Recommended | ⚠️ PARTIAL (phone evidence only) |
| VPN Tunnel E2E Test | HIGH | Required for release | ❌ EXTERNAL_BLOCKED |

---

## RECOMMENDED ACTIONS

### Immediate (This Sprint)
1. **Push Sonar fixes** to new branch → PR → SonarCloud re-scan → confirm A Rating
2. **File GitHub Issue** for API 35 emulator instability with Google (RenderThread SIGSEGV)
3. **Evaluate Firebase Test Lab** for API 35 physical device testing (bypass emulator)

### Short Term (Next Sprint)
1. **Migrate device tests to physical device farm** — eliminate emulator GPU dependency
2. **Implement test sharding** — parallel emulator instances for 350-test suite
3. **Add visual regression pipeline** — automated screenshot comparison

### Long Term
1. **Establish dedicated VPN test gateway** — unblock E2E tunnel tests
2. **Performance benchmark automation** — startup, reconnect, memory, battery
3. **Security test integration** — OWASP MASVS, dependency scanning in CI

---

**FOR NOW: NO MERGE TO MAIN UNTIL API 35 TESTS PASS AND SONARCLOUD GATE GREEN. API 26 IS STABLE.**