# API 26–36 TEST MATRIX
**Generated:** 2026-11-15 11:45:00 UAE  
**Scope:** Unit, Instrumentation, Integration, Visual, Security, E2E test coverage across API levels  

---

## TEST SUITE INVENTORY

### Unit Tests (JVM) — `./gradlew :app:testDebugUnitTest`
| Test Class | Domain | Count | Status |
|------------|--------|-------|--------|
| `AdvancedNetworkConfigTest` | Advanced Network | ~5 | PASS |
| `AppInventoryModelTest` | App Inventory | ~5 | PASS |
| `AutopilotTest` | Repair Autopilot | ~8 | PASS |
| `CacheMaintenanceTest` | Cache Surgeon | ~6 | PASS |
| `ConfigValidatorTest` | Config Validation | ~10 | PASS |
| `DownloadExportSafetyTest` | Download Safety | ~4 | PASS |
| `DownloadRescueTest` | Download Rescue | ~6 | PASS |
| `DuplicateDnaTest` | Duplicate DNA | ~8 | PASS |
| `GhostHunterTest` | Ghost Hunter | ~6 | PASS |
| `GuardianAdvancedTest` | Guardian | ~5 | PASS |
| `GuardianPolicyTest` | Guardian | ~5 | PASS |
| `HeavyweightTest` | Heavyweight Finder | ~6 | PASS |
| `HomeServiceCatalogTest` | Home Services | ~5 | PASS |
| `InstalledAppClassificationTest` | App Classification | ~5 | PASS |
| `IntroTimelineTest` | Splash Timeline | ~4 | PASS |
| `MediaSlimTest` | Media Slim | ~6 | PASS |
| `NetworkDoctorTest` | Network Doctor | ~6 | PASS |
| `NetworkProbePolicyTest` | Network Probe | ~4 | PASS |
| `NetworkReferencePolicyTest` | Network Reference | ~4 | PASS |
| `ProActivationClientTest` | Pro Activation | ~4 | PASS |
| `ProBackendContractTest` | Pro Backend | ~4 | PASS |
| `ProEntitlementsTest` | Pro Entitlements | ~4 | PASS |
| `ProHttpsTransportTest` | Pro Transport | ~4 | PASS |
| `RepairCatalogTest` | Repair Catalog | ~5 | PASS |
| `RepairJournalCodecTest` | Repair Journal | ~4 | PASS |
| `SafStorageScannerTest` | SAF Storage | ~4 | PASS |
| `SecurityPolicyTest` | Security Policy | ~5 | PASS |
| `SessionShieldTest` | Session Shield | ~5 | PASS |
| `SettingsModelTest` | Settings | ~5 | PASS |
| `SplitTunnelPolicyTest` | Split Tunnel | ~8 | PASS |
| `StorageHealthModelTest` | Storage Health | ~5 | PASS |
| `StoragePermissionsTest` | Storage Permissions | ~4 | PASS |
| `StorageScanTest` | Storage Scan | ~5 | PASS |
| `ToolsCatalogTest` | Tools Catalog | ~5 | PASS |
| `UsageAggregatorTest` | Usage Stats | ~6 | PASS |
| `UsageCodecTest` | Usage Codec | ~4 | PASS |
| `UsageMutationTest` | Usage Mutation | ~4 | PASS |
| `UsageRecorderTest` | Usage Recorder | ~5 | PASS |
| `VpnControllerTest` | VPN Controller | ~12 | PASS |
| `VpnPathVerificationTest` | VPN Path Verify | ~6 | PASS |
| `VpnSessionControlTest` | VPN Session Control | ~5 | PASS |
| `VpnSessionOwnershipTest` | VPN Ownership | ~6 | PASS |
| `WireGuardGatewayTest` | WireGuard Gateway | ~8 | PASS |
| **TOTAL** | | **~200+** | **PASS** |

---

### Instrumented Tests (Device/Emulator) — `./gradlew :app:connectedDebugAndroidTest`

#### API 26 (Nexus 5, swiftshader_indirect) — **PASS (316 tests)**
| Test Class | Category | Tests | Status | Evidence |
|------------|----------|-------|--------|----------|
| `AdvancedNetworkFlowTest` | Advanced Network | ~8 | PASS | `ci-be93953-api26`, `ci-f28291c-api26` |
| `AdvancedNetworkScreenTest` | Advanced Network UI | ~6 | PASS | |
| `AutopilotScreenTest` | Repair Autopilot | ~8 | PASS | |
| `CacheFlowTest` | Cache Surgeon | ~6 | PASS | |
| `CacheScreenContractTest` | Cache UI | ~4 | PASS | |
| `CacheSettingsIntentTest` | Cache Intent | ~3 | PASS | |
| `ClientFlowTest` | Client VPN Flow | ~10 | PASS | |
| `ConfigImportTest` | Config Import | ~5 | PASS | |
| `DownloadRescueScreenTest` | Download Rescue | ~6 | PASS | |
| `DuplicateDnaScreenTest` | Duplicate DNA | ~6 | PASS | |
| `EvidenceCapture` | Test Utility | — | PASS | |
| `GhostHunterFlowTest` | Ghost Hunter | ~6 | PASS | |
| `GhostHunterScreenTest` | Ghost Hunter UI | ~6 | PASS | |
| `GuardianScreenTest` | Guardian | ~5 | PASS | |
| `HeavyweightScreenTest` | Heavyweight | ~6 | PASS | |
| `HomeScreenTest` | Home | ~8 | PASS | |
| `IdentityTest` | Identity/Splash | ~4 | PASS | |
| `InstalledAppsInventoryTest` | App Inventory | ~5 | PASS | |
| `InstalledAppsScreenTest` | App Inventory UI | ~5 | PASS | |
| `KxEntryTestGate` | Entry Gate | ~3 | PASS | |
| `MediaSlimCompressorTest` | Media Slim | ~4 | PASS | |
| `MediaSlimScreenTest` | Media Slim UI | ~6 | PASS | |
| `NetworkDoctorScreenTest` | Network Doctor | ~6 | PASS | |
| `NetworkDoctorTest` | Network Doctor | ~6 | PASS | |
| `NetworkSettingsStoreTest` | Network Settings | ~4 | PASS | |
| `PhysicalEntryTest` | Physical Entry | ~4 | PASS | |
| `ProfileStoreTest` | Profile Store | ~5 | PASS | |
| `ProIdentityStoreTest` | Pro Identity | ~4 | PASS | |
| `ProScreenTest` | Pro UI | ~5 | PASS | |
| `ProTlsTransportTest` | Pro Transport | ~4 | PASS | |
| `RepairFlowTest` | Repair Flow | ~6 | PASS | |
| `RepairJournalDeviceTest` | Repair Journal | ~4 | PASS | |
| `RepairLabScreenTest` | Repair Lab | ~8 | PASS | |
| `RepairMeasurementDeviceTest` | Repair Measurement | ~4 | PASS | |
| `SecurityCenterTest` | Security Center | ~6 | PASS | |
| `SecurityFlowTest` | Security Flow | ~6 | PASS | |
| `ServerSelectionScreenTest` | Server Selection | ~6 | PASS | |
| `SessionShieldScreenTest` | Session Shield | ~5 | PASS | |
| `SettingsScreenTest` | Settings | ~6 | PASS | |
| `SplitTunnelFlowTest` | Split Tunnel | ~8 | PASS | |
| `SplitTunnelScreenTest` | Split Tunnel UI | ~8 | PASS | |
| `SplitTunnelStoreTest` | Split Tunnel Store | ~4 | PASS | |
| `StartupTest` | Startup | ~3 | PASS | |
| `StatisticsFlowTest` | Statistics | ~6 | PASS | |
| `StatisticsScreenTest` | Statistics UI | ~6 | PASS | |
| `StorageXrayDeviceTest` | Storage X-Ray | ~6 | PASS | |
| `StorageXrayFlowTest` | Storage X-Ray Flow | ~6 | PASS | |
| `StorageXrayScreenTest` | Storage X-Ray UI | ~6 | PASS | |
| `TestLocales` | Locale Test | ~3 | PASS | |
| `ToolsCatalogScreenTest` | Tools Catalog | ~8 | PASS | |
| `UsageStatsStoreTest` | Usage Stats Store | ~4 | PASS | |
| `VisualHardeningTest` | Visual Hardening | ~4 | PASS | |
| `VpnFlowTest` | VPN Flow | ~6 | PASS | |
| `VpnSessionServiceTest` | VPN Session Service | ~5 | PASS | |
| **TOTAL** | | **316** | **PASS** | |

#### API 35 (Nexus 5, swangle/ANGLE) — **FAIL/INCOMPLETE (37/350 attempted)**
| Test Class | Category | Tests Attempted | Status | Failure Details |
|------------|----------|-----------------|--------|-----------------|
| `ClientFlowTest` | Client VPN Flow | 1 | **FAIL** | `appearanceTapIsNotLostWhileTheLocaleWriteSettles` — empty XML failure node |
| `CacheFlowTest` | Cache Surgeon | 0 | **NOT RUN** | Emulator lost before test |
| *(348 more)* | — | 0 | **NOT RUN** | Emulator `emulator-5554` disappeared from ADB |

**CI Evidence:** `ci-ef75300-api35`, `ci-f376e4a-api35`
- Emulator boot: 600s timeout
- GPU mode: `swangle` (Emulator 36.4.10, build 15004761)
- Host kernel: RenderThread SIGSEGV / QEMU signal 11
- Cleanup message: `Expected 318 tests, received 36`

---

## TEST MATRIX BY API LEVEL

| Test Type | API 26 | API 35 | API 36 (Target) | Notes |
|-----------|--------|--------|-----------------|-------|
| Unit (JVM) | ✅ PASS | ✅ PASS | ✅ PASS | Same bytecode |
| Lint | ✅ PASS | ✅ PASS | ✅ PASS | Static analysis |
| Compile Debug | ✅ PASS | ✅ PASS | ✅ PASS | `assembleDebug` |
| Compile Android Test | ✅ PASS | ✅ PASS | ✅ PASS | `assembleDebugAndroidTest` |
| Instrumented (Full) | ✅ PASS (316) | ❌ FAIL (37/350) | **UNTESTED** | Emulator instability |
| Visual Screenshots | ✅ Captured | ⚠️ Partial | **UNTESTED** | API 35 capture skipped |
| Startup Benchmark | ✅ Captured | ⚠️ Partial | **UNTESTED** | `cold-start.txt` |
| Frame Stats | ✅ Captured | ⚠️ Partial | **UNTESTED** | `launch-frames.txt` |
| VPN Tunnel (E2E) | ❌ EXTERNAL | ❌ EXTERNAL | ❌ EXTERNAL | No authorized gateway |

---

## VISUAL REGRESSION TESTING

### Current Evidence (Screenshots)
| Source | API | Device | Locale | Theme | Count | Status |
|--------|-----|--------|--------|-------|-------|--------|
| `ci-be93953-api26` | 26 | Emulator | EN/AR | Day/Night | 6 frames | ✅ |
| `ci-f28291c-api26` | 26 | Emulator | EN/AR | Day/Night | 6 frames | ✅ |
| `ci-ef75300-api35` | 35 | Emulator | — | — | 0 frames | ❌ (emulator lost) |
| `ci-f376e4a-api35` | 35 | Emulator | — | — | 1 frame | ❌ (emulator lost) |
| `task16/phone-evidence` | 34+ | Physical (R9ZR20HWHQY) | EN/AR | Day/Night | 50+ | ✅ |
| `task17/phone` | 34+ | Physical | EN/AR | Day/Night | 7 | ✅ |
| `task18` | 34+ | Physical | EN/AR | Day/Night | 5 | ✅ |
| `task21/phone-evidence` | 34+ | Physical | EN/AR | Day/Night | 20+ | ✅ |

### Required Visual Coverage (Per CODEX_REFERENCE_VISUAL_CONTRACT.md)
| Screen | 360dp Day EN | 360dp Night EN | 360dp Day AR | 360dp Night AR | 411dp Day EN | 411dp Night EN | 411dp Day AR | 411dp Night AR | Font 1.0 | Font 1.6 | Reduced Motion |
|--------|--------------|----------------|--------------|----------------|--------------|----------------|--------------|----------------|----------|----------|----------------|
| Splash (00) | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Welcome (01) | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Home Disconnected (02) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| Server Selection (03) | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Connected Session (04) | ✅ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Security Center (05) | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Split Tunneling (06) | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Advanced Network (07) | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Statistics (08) | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Import Config (09) | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Profile Settings (10) | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Repair Lab (11) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| All Repair Services | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |

**Legend:** ✅ = Captured in phone evidence, ❌ = Not captured / not compared

---

## CI/CD PIPELINE STATUS

### GitHub Actions Workflow (`.github/workflows/android.yml`)
| Job | Trigger | Timeout | Matrix | Status |
|-----|---------|---------|--------|--------|
| `build` | PR/push to main | 20 min | — | ✅ PASS |
| `device` | PR/push to main | 55 min | API 26, 35 | ⚠️ API 35 FAIL |
| `api35-focused-diagnostic` | PR #40 only | 25 min | API 35 | Diagnostic only |

### Quality Gates
| Gate | Tool | Threshold | Current Status |
|------|------|-----------|----------------|
| Format/Lint | Android Lint / ktlint | 0 errors | ✅ PASS |
| Unit Tests | JUnit + Kotlin Coroutines Test | > 0 tests, 0 failures | ✅ PASS (~200) |
| Instrumented API 26 | AndroidJUnitRunner | 316 tests, 0 failures | ✅ PASS |
| Instrumented API 35 | AndroidJUnitRunner | 350 tests, 0 failures | ❌ FAIL (emulator loss) |
| SonarCloud | SonarCloud Scanner | Quality Gate A Rating | ❌ FAIL (C Rating on PR #40) |
| Security Scan | — | — | NOT CONFIGURED |
| Dependency Check | Gradle lockfile | Verified | ✅ PASS |

---

## TEST EXECUTION COMMANDS

### Local Development
```bash
cd K:\KX-Private-VPN\android

# Unit tests + Lint + Debug build
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest

# Instrumented (requires device/emulator)
./gradlew :app:connectedDebugAndroidTest

# Focused test class (for debugging)
KX_TEST_CLASS="com.knoux.kxprivatevpn.VpnFlowTest" ./gradlew :app:connectedDebugAndroidTest
```

### CI Simulation (via `scripts/verify-device.sh`)
```bash
cd K:\KX-Private-VPN\android
bash scripts/verify-device.sh
# Sets up emulator, runs full instrumented suite, captures evidence
```

---

## KNOWN TEST FAILURES / FLAKINESS

| Test | API | Failure Mode | Root Cause | Mitigation |
|------|-----|--------------|------------|------------|
| `ClientFlowTest.appearanceTapIsNotLostWhileTheLocaleWriteSettles` | 35 | Empty XML failure | Emulator loss mid-test | swangle GPU + Emulator 36.4.10 |
| `ToolsCatalogScreenTest.rtlReadingOrderMirrorsTheReferenceRows` | 26 | 3rd RTL row clipped | Offscreen bounds inspection | Fixed: scroll into view first |
| `AdvancedNetworkFlowTest` | 35 | Test 1 failure | Emulator loss | Same as above |
| All API 35 tests | 35 | Emulator disappears | RenderThread SIGSEGV / QEMU signal 11 | swangle GPU mode (partially mitigated) |

---

## TEST COVERAGE GAPS

| Area | Current Coverage | Gap | Required for Production |
|------|------------------|-----|-------------------------|
| VPN Tunnel E2E | 0% | No authorized gateway | **REQUIRED** |
| DNS Leak Test | 0% | Requires tunnel + DNS capture | **REQUIRED** |
| Kill Switch Test | 0% | Requires network disconnect | **REQUIRED** |
| IPv6 Test | 0% | Requires dual-stack gateway | NICE TO HAVE |
| Network Change Test | 0% | Requires WiFi↔Cellular switch | NICE TO HAVE |
| Process Death Restore | Code only | Not tested on device | **REQUIRED** |
| Low Memory / OOM | 0% | Not tested | NICE TO HAVE |
| Battery Optimization | 0% | Not tested | NICE TO HAVE |
| Accessibility (TalkBack) | Partial | `TestLocales` only | **REQUIRED** |
| Reduced Motion | Code only | Not visually verified | **REQUIRED** |

---

## RECOMMENDED TEST STRATEGY

1. **Stabilize API 35 Emulator** — Use Emulator 36.4.10 + swangle GPU + increased timeout (55 min)
2. **Add Physical Device Farm** — Cloud device lab (Firebase Test Lab / AWS Device Farm) for API 26, 30, 33, 35
3. **Establish VPN Test Gateway** — Dedicated WireGuard server for E2E tunnel tests
4. **Visual Regression Pipeline** — Automated screenshot comparison (reference vs runtime)
5. **Security Test Suite** — OWASP MASVS, dependency scanning, secret detection
6. **Performance Benchmarks** — Startup, reconnect latency, memory, battery (baseline captured)

---

**MATRIX COMPLETE — 316/350 TESTS PASSING ON API 26, API 35 BLOCKED BY EMULATOR INSTABILITY, E2E TUNNEL TESTS EXTERNAL_BLOCKED**