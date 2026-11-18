# PRODUCTION RELEASE GATES
**Generated:** 2026-11-15 12:45:00 UAE  
**Purpose:** Measurable, evidence-based criteria for each release milestone  

---

## GATE CLASSIFICATION

| Gate | Code | Description | Verification Method |
|------|------|-------------|---------------------|
| **Format/Lint** | `FORMAT/LINT` | Code style, Android Lint, ktlint | `./gradlew :app:lintDebug` exit 0 |
| **Unit Tests** | `UNIT` | JVM unit tests (JUnit, Coroutines Test) | `./gradlew :app:testDebugUnitTest` exit 0, count > 0 |
| **Integration Tests** | `INTEGRATION` | Compile + assemble Android tests | `./gradlew :app:assembleDebugAndroidTest` exit 0 |
| **Security Scan** | `SECURITY` | Dependency check, secret scan, OWASP MASVS | Manual + automated tools |
| **Visual Regression** | `VISUAL` | Screenshot comparison vs owner references | Automated diff + manual review |
| **Instrumented API 26** | `INSTRUMENTATION_26` | Full suite on API 26 emulator/device | 316 tests PASS, screenshots captured |
| **Instrumented API 35** | `INSTRUMENTATION_35` | Full suite on API 35 emulator/device | 350 tests PASS, screenshots captured |
| **E2E Real Tunnel** | `E2E_REAL_TUNNEL` | Actual WireGuard handshake + traffic | Authorized gateway + packet capture |
| **API Matrix** | `API_MATRIX` | Tested on API 26, 30, 33, 35, 36 | Physical device farm or cloud |
| **CI/Sonar** | `CI/SONAR` | GitHub Actions green + SonarCloud Quality Gate A | PR checks PASS |
| **Signed APK/AAB** | `SIGNED_APK` | Release build with production keystore | `./gradlew :app:assembleRelease` → signed AAB |
| **Installable Runtime** | `INSTALLABLE_RUNTIME` | APK installs, launches, basic flow works | `adb install` + manual smoke test |
| **GitHub Release** | `GITHUB_RELEASE` | Tagged release with artifacts + notes | GitHub Releases page |
| **F-Droid/Play Publish** | `PUBLISH` | Published to store with verified metadata | Store listing live |

---

## RELEASE MILESTONES & REQUIRED GATES

### M1: Development Merge Gate (Per PR to main)
| Gate | Required | Evidence |
|------|----------|----------|
| `FORMAT/LINT` | ✅ | Lint HTML report |
| `UNIT` | ✅ | Test report (count, time) |
| `INTEGRATION` | ✅ | APK + test APK built |
| `INSTRUMENTATION_26` | ✅ | Test report + screenshots |
| `INSTRUMENTATION_35` | ✅ | Test report + screenshots |
| `CI/SONAR` | ✅ | GitHub Checks + SonarCloud URL |
| **Visual Review** | ⚠️ Recommended | Side-by-side captures (if UI changed) |

**Policy:** NO MERGE if ANY required gate fails. No skipping tests.

---

### M2: Release Candidate (Tagged RC Build)
| Gate | Required | Evidence |
|------|----------|----------|
| All M1 gates | ✅ | Previous PR evidence |
| `VISUAL` | ✅ | All 22 screens vs references (8 variants each) |
| `SECURITY` | ✅ | Dependency scan report, secret scan clean |
| `API_MATRIX` | ⚠️ Minimum API 26, 35 | Device farm results |
| `E2E_REAL_TUNNEL` | ⚠️ At least 1 successful handshake | Gateway log + `VpnPathEvidence` |
| `SIGNED_APK` | ✅ | Signed AAB + SHA-256 |
| `INSTALLABLE_RUNTIME` | ✅ | Physical device install + smoke test |

**Policy:** RC tag only if all ✅ gates PASS. ⚠️ gates documented as known limitations.

---

### M3: Production Release (GitHub + Store)
| Gate | Required | Evidence |
|------|----------|----------|
| All M2 gates | ✅ | RC evidence |
| `E2E_REAL_TUNNEL` | ✅ | **MANDATORY** — Real tunnel proof |
| `API_MATRIX` | ✅ | API 26, 30, 33, 35, 36 all PASS |
| `GITHUB_RELEASE` | ✅ | Tag + AAB + Release Notes + SHA-256 |
| `PUBLISH` | ✅ | Store listing URL + version code |
| **Rollback Plan** | ✅ | Documented in release notes |

**Policy:** NO PRODUCTION RELEASE without `E2E_REAL_TUNNEL` PASS and full `API_MATRIX`.

---

## EVIDENCE REQUIREMENTS PER GATE

### `FORMAT/LINT`
```bash
./gradlew :app:lintDebug
# Artifact: android/app/build/reports/lint-results-debug.html
# Exit code: 0
# Zero errors, warnings documented
```

### `UNIT`
```bash
./gradlew :app:testDebugUnitTest
# Artifact: android/app/build/reports/tests/testDebugUnitTest/index.html
# Exit code: 0
# Test count: > 150 (currently ~200)
# Zero failures
```

### `INSTRUMENTATION_26`
```bash
# Via CI: .github/workflows/android.yml (device job, API 26)
# Artifact: native-device-api-26 (reports + screenshots)
# Exit code: 0
# Test count: 316
# Zero failures
# Screenshots: cold-start, launch-frames, identity-evidence (6 frames)
```

### `INSTRUMENTATION_35`
```bash
# Via CI: .github/workflows/android.yml (device job, API 35)
# Artifact: native-device-api-35 (reports + screenshots)
# Exit code: 0
# Test count: 350
# Zero failures
# Screenshots: identity-evidence (6 frames) + test-specific captures
# NOTE: Currently BLOCKED by emulator instability
```

### `VISUAL`
```bash
# Manual + automated comparison
# Reference: public/assets/aistudio/*.png (11 refs) + owner 20 refs
# Runtime: Emulator (360dp/411dp, Day/Night, EN/AR, Font 1.0/1.6) + Physical
# Tool: Side-by-side + alpha overlay diff
# Threshold: ≤ 5% pixel difference (structural), 0% critical element diff
# Evidence: Comparison images + diff report per screen
```

### `E2E_REAL_TUNNEL`
```bash
# Requirements:
# 1. Authorized WireGuard gateway (config in ProfileStore)
# 2. Physical device or emulator with network access
# 3. VpnPathVerification executed
# 4. Handshake observed (latestHandshakeMillis > 0)
# 5. Traffic counters increment (rxBytes/txBytes > 0 after data)
# 5. Packet capture confirms encryption (Wireshark/tcpdump)
# Evidence: Gateway logs + VpnPathEvidence JSON + pcap file + screenshot
```

### `API_MATRIX`
```bash
# Device farm (Firebase Test Lab / AWS Device Farm / Local devices)
# APIs: 26, 30, 33, 35, 36
# Per API: Install + Launch + Instrumented subset (smoke) + Visual (1 screen)
# Evidence: Per-API test report + screenshot
```

### `SECURITY`
```bash
# Dependency scan: OWASP Dependency Check / GitHub Dependabot
# Secret scan: TruffleHog / GitLeaks / manual grep
# MASVS: Manual review against MASVS L1/L2
# Evidence: Scan reports + remediation log
```

### `SIGNED_APK`
```bash
./gradlew :app:assembleRelease
# Artifact: android/app/build/outputs/bundle/release/app-release.aab
# Signed: jarsigner -verify -verbose -certs app-release.aab
# SHA-256: sha256sum app-release.aab
# Version Code/Name: Verified in bundle
```

### `INSTALLABLE_RUNTIME`
```bash
adb install -r app-release.apk
# Launch: adb shell am start -n com.knoux.kxprivatevpn/.MainActivity
# Smoke: Splash → Welcome → Home → Import config → Connect (if gateway)
# Evidence: adb logcat (no crashes) + screenshot of each step
```

---

## GATE STATUS TRACKER (Current)

| Gate | Status | Evidence | Blocker |
|------|--------|----------|---------|
| `FORMAT/LINT` | ✅ PASS | CI build job | — |
| `UNIT` | ✅ PASS | CI build job (~200 tests) | — |
| `INTEGRATION` | ✅ PASS | CI build job | — |
| `INSTRUMENTATION_26` | ✅ PASS | 316 tests, screenshots | — |
| `INSTRUMENTATION_35` | ❌ FAIL | Emulator loss (37/350) | Emulator GPU bug |
| `VISUAL` | ⚠️ PARTIAL | Phone captures (50+), 3 critical gaps | Ref 14, 18, 03 fidelity |
| `SECURITY` | ⚠️ PARTIAL | 2 HIGH findings (keys, crash logs) | Keystore not implemented |
| `E2E_REAL_TUNNEL` | ❌ EXTERNAL_BLOCKED | No authorized gateway | Provision test server |
| `API_MATRIX` | ⚠️ PARTIAL | API 26 only | API 35 emulator, no device farm |
| `CI/SONAR` | ❌ FAIL | C Rating (fixes local only) | Push fixes → re-scan |
| `SIGNED_APK` | ❌ NOT_CONFIGURED | No release keystore | Owner provides |
| `INSTALLABLE_RUNTIME` | ✅ PASS (Debug) | Debug APK installs + launches | Release not tested |
| `GITHUB_RELEASE` | ❌ NOT_DONE | No tags/releases | — |
| `PUBLISH` | ❌ NOT_DONE | No store listing | — |

---

## RELEASE BLOCKER REGISTER

| Blocker | Gate Impacted | Severity | Owner | Resolution |
|---------|---------------|----------|-------|------------|
| API 35 Emulator Instability | `INSTRUMENTATION_35`, `API_MATRIX`, `VISUAL` (API 35) | CRITICAL | Infrastructure | Firebase Test Lab / Sharding |
| SonarCloud C Rating | `CI/SONAR` | CRITICAL | Developer | Push fixes → re-scan |
| No VPN Test Gateway | `E2E_REAL_TUNNEL`, `INSTRUMENTATION_35` (tunnel tests) | CRITICAL | Owner/Infra | Provision WireGuard server |
| Release Keystore Missing | `SIGNED_APK`, `PUBLISH` | HIGH | Owner | Provide keystore / defer |
| Android Keystore Not Implemented | `SECURITY` | HIGH | Developer | Implement post-baseline |
| Visual Fidelity Gaps (3 refs) | `VISUAL` | HIGH | Agent_A + Designer | Coordinate + implement |
| No Device Farm Access | `API_MATRIX`, `VISUAL` (device) | MEDIUM | Infrastructure | Firebase Test Lab trial |

---

## RELEASE CHECKLIST (Per Version)

### Pre-Release (RC)
- [ ] All M1 gates PASS on `main` at target SHA
- [ ] `VISUAL` gate: 22 screens × 8 variants captured + diff ≤ 5%
- [ ] `SECURITY` gate: Dependency scan clean, secrets clean, MASVS reviewed
- [ ] `E2E_REAL_TUNNEL`: At least 1 successful handshake + traffic proof
- [ ] `SIGNED_APK`: Release AAB built, signed, SHA-256 recorded
- [ ] `INSTALLABLE_RUNTIME`: Physical device smoke test PASS
- [ ] Release Notes drafted (features, fixes, known issues, upgrade notes)
- [ ] Rollback plan documented (previous version tag, migration steps)

### Release (Production)
- [ ] All RC gates PASS
- [ ] `E2E_REAL_TUNNEL`: Full tunnel validation (handshake + traffic + leak test)
- [ ] `API_MATRIX`: All 5 API levels PASS on device farm
- [ ] GitHub Release: Tag `v{X.Y.Z}`, attach AAB + SHA-256, publish notes
- [ ] Store Publish: Upload AAB to Play Console / F-Droid, fill metadata
- [ ] Post-Release: Monitor crash rate (Play Console), user feedback, CI health

---

## VERSIONING POLICY

| Component | Scheme | Example |
|-----------|--------|---------|
| `versionCode` | Monotonic integer | 1, 2, 3... |
| `versionName` | SemVer + build metadata | `1.0.0`, `1.0.1-beta.2+abc123` |
| Git Tag | `v{versionName}` | `v1.0.0`, `v1.0.1-beta.2` |
| Release Branch | `release/{versionName}` | `release/1.0.0` |

---

**GATES DEFINED — NO RELEASE WITHOUT EVIDENCE. E2E_REAL_TUNNEL IS MANDATORY FOR PRODUCTION.**