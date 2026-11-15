# FULL VISUAL GAP MATRIX
**Generated:** 2026-11-15 11:35:00 UAE  
**Source:** `CONTINUATION-REVIEW.md`, `CODEX_REFERENCE_VISUAL_CONTRACT.md`, CI evidence screenshots, phone captures  

---

## REFERENCE IMAGE INVENTORY

### Owner-Approved References (Highest Authority)
| Ref | File | Screen | Source |
|-----|------|--------|--------|
| 00 | `00-brand-logo_c0b92dd8.png` | Brand Orb / Splash | `public/assets/aistudio/` |
| 01 | `01-welcome_464a7c4f.png` | Welcome / Landing | `public/assets/aistudio/` |
| 02 | `02-home-disconnected_825c75b0.png` | Home Disconnected | `public/assets/aistudio/` |
| 03 | `03-repair-lab-hub.png` | Repair Lab Hub | Owner provided (20 refs) |
| 04 | `04-connected-session_f0cfe658.png` | Connected Session | `public/assets/aistudio/` |
| 05 | `05-security-center_d0a1f09a.png` | Security Center | `public/assets/aistudio/` |
| 06 | `06-split-tunneling_c9265fd7.png` | Split Tunneling | `public/assets/aistudio/` |
| 07 | `07-advanced-network_5b3214f4.png` | Advanced Network | `public/assets/aistudio/` |
| 08 | `08-statistics_a6c474b9.png` | Statistics | `public/assets/aistudio/` |
| 09 | `09-import-configuration_670a2aea.png` | Import Configuration | `public/assets/aistudio/` |
| 10 | `10-profile-settings_75031956.png` | Profile Settings | `public/assets/aistudio/` |
| 11-20 | Various | Repair services (11-21) | Owner provided (20 refs) |

### Runtime Captures (Evidence)
| Location | Contents |
|----------|----------|
| `docs/evidence/ci-*-api*/device-screenshots/identity-evidence/` | Emulator cold start frames (photons, ribbons, reveal, welcome) |
| `docs/evidence/task16/phone-evidence/` | Physical device captures (360dp/411dp, day/night, AR/EN) |
| `docs/evidence/task17/phone/` | Media Slim captures |
| `docs/evidence/task18/` | Download Rescue captures |
| `docs/evidence/task21/phone-evidence/` | Repair Autopilot captures |
| `docs/evidence/visual-reference-20261108/runtime/identity-evidence/` | Side-by-side comparison set |
| `docs/evidence/visual-reference-20261109/` | Additional reference comparisons |

---

## VISUAL FIDELITY ASSESSMENT (per CONTINUATION-REVIEW.md)

### Ref 14 — Home Screen (Maps to Ref 02)
| Aspect | Reference | Runtime | Gap | Severity |
|--------|-----------|---------|-----|----------|
| Header / Status / Session | ✅ Match | ✅ Match | None | — |
| Actions / Activity / Nav | ✅ Match | ✅ Match | None | — |
| Atmosphere (neon ribbons/glow) | Rich, layered | **Flatter, weaker** | Missing depth/bloom | **HIGH** |
| Glass depth (orb/cards) | Deep, refractive | **Shallower** | Less volumetric glow | **HIGH** |
| Content height | Reference proportions | **Different** | Layout spacing off | **MEDIUM** |
| Feature illumination | Vivid accents | **Subdued** | Less orbital glow | **MEDIUM** |

**Verdict:** `PARTIAL` — Structural PASS, Visual FAIL on atmosphere/glass

### Ref 18 — Tools Catalog (Maps to Ref 18 in owner refs)
| Aspect | Reference | Runtime | Gap | Severity |
|--------|-----------|---------|-----|----------|
| RTL card order | ✅ Match | ✅ Match | None | — |
| Search / Filter | ✅ Match | ✅ Match | None | — |
| Nine destinations | ✅ Match | ✅ Match | None | — |
| Card height | Taller | **Shorter** | Compressed | **HIGH** |
| Luminous edges | Strong | **Weaker** | Dimmer glow | **HIGH** |
| Icon lighting | Rich | **Subdued** | Less vibrant | **MEDIUM** |
| Titles / Copy | Spacious | **Compressed** | Typography scaling | **MEDIUM** |

**Verdict:** `PARTIAL` — Functional PASS, Visual FAIL on card treatment

### Ref 03 — Repair Lab (Maps to Ref 03 in owner refs)
| Aspect | Reference | Runtime | Gap | Severity |
|--------|-----------|---------|-----|----------|
| Order / Ring orb / Hint / CTA | ✅ Match | ✅ Match | None | — |
| Glowing platform/portal under orb | **Present** | **Absent** | Missing floor glow | **CRITICAL** |
| Neon card rims | Vivid | **Subdued/Absent** | Weak card treatment | **HIGH** |
| Card heights | Reference proportions | **Different** | Layout mismatch | **MEDIUM** |
| Typography | Reference | **Different** | Font weight/size | **MEDIUM** |

**Verdict:** `PARTIAL` — Missing signature glow platform, weak cards

---

## SCREEN-BY-SCREEN GAP ANALYSIS

### TASK_00 — Legendary Cinematic Splash (Ref 00)
| Component | Reference | Current Implementation | Gap |
|-----------|-----------|------------------------|-----|
| System SplashScreen | Android 12+ compliant icon | `MainActivity.kt:12` `installSplashScreen()` | ✅ Exists |
| Native Compose Intro | 8-beat choreography | `KxLaunch.kt` + `IdentityExperience` + `KxOrb` | **PARTIAL** — Photon/ribbon/orb implemented |
| Photon convergence | Cyan/violet → sphere | `CrystalIdentity.kt:111-120` | ✅ Implemented |
| Ribbon interlock | KX emblem formation | `CrystalIdentity.kt:161-207` (`drawKxOrbNativeShield`) | ✅ Implemented |
| Orb formation | Frosted center, liquid rim, orbit rings | `KxOrb.kt` + `CrystalIdentity.kt` | ✅ Implemented |
| Caustic sweep | Light sweep across icon | `CrystalIdentity.kt:144-151` | ✅ Implemented |
| Halo pulse + satellites | Pulse + outward glide | `CrystalIdentity.kt:137-143, 152-155` | ✅ Implemented |
| Wordmark reveal | "KX PRIVATE VPN" | `KxLaunch.kt:237-240` | ✅ Implemented |
| Shared orb continuity | Same orb → Welcome/Home | `KxLaunch.kt:196-228` (shared `KxOrb`) | ✅ Implemented |
| Reduced motion | Bypass animation | `KxLaunch.kt:136, 149-153` | ✅ Implemented |
| Warm restart optimization | Skip intro if seen | `LaunchPreferences.hasSeenIntro()` | ✅ Implemented |

**Gap:** Visual fidelity vs Ref 00 not yet measured (no screenshot comparison evidence)

---

### TASK_01 — Welcome / Onboarding (Ref 01)
| Component | Reference | Current Implementation | Gap |
|-----------|-----------|------------------------|-----|
| Dark opening frame | Logo only, then scene | `IdentityExperience.kt:191-195` (`appearance` alpha) | ✅ Implemented |
| Orb entry animation | Fly in from distance | `IdentityExperience.kt:206-216` (scale/translation/rotation) | ✅ Implemented |
| Tap-to-enter | Orb as button | `IdentityExperience.kt:203, 218-222` | ✅ Implemented |
| Spin on tap | 360° rotation | `IdentityExperience.kt:149-154` (`tapSpin.animateTo(360f)`) | ✅ Implemented |
| Welcome controls | After entry only | `IdentityExperience.kt:253-266` (Get Started / Import) | ✅ Implemented |
| Skip button | Top-right | `IdentityExperience.kt:269-273` | ✅ Implemented |
| Disclosure dialog | Scope notice | `IdentityExperience.kt:275-283` | ✅ Implemented |

**Gap:** Visual comparison vs Ref 01 not documented

---

### TASK_02 — Home Disconnected (Ref 02 / Ref 14)
| Component | Reference | Current Implementation | Gap |
|-----------|-----------|------------------------|-----|
| Profile name + endpoint | ✅ | `VpnHomePanel.kt:48-53` | ✅ |
| "No profiles" state | ✅ | `VpnHomePanel.kt:49, 94-97` | ✅ |
| Connect button (disabled if no profile) | ✅ | `VpnHomePanel.kt:90-93` | ✅ |
| Connect button (enabled with profile) | ✅ | `VpnHomePanel.kt:90-93` | ✅ |
| Atmosphere/background | Rich neon/glass | `Atmosphere` + `CrystalPanel` | **GAP** — Flatter per Ref 14 |
| Orb/Button styling | Signature KX orb | `KxOrb` decorative in `NativeClient.kt:251` | **GAP** — Weak glow per Ref 14 |
| Action cards (Security, Split, Advanced, Stats, Servers, Storage, Cache, Settings) | Vertical cards | `NativeClient.kt:266-275` `OutlinedButton` list | **GAP** — Card treatment per Ref 14/18 |

---

### TASK_03 — Server Selection (Ref 03 in owner refs = Server Selection)
| Component | Reference | Current Implementation | Gap |
|-----------|-----------|------------------------|-----|
| Profile list | Vertical cards | `ProfileList` in `NativeClient.kt:338-363` | ✅ |
| Select/Remove actions | ✅ | `OutlinedButton` / `TextButton` | ✅ |
| Active profile banner | ✅ | `ServerSelectionRoute` (not fully audited) | **UNVERIFIED** |
| Import action | ✅ | `NativeClient.kt:120` navigate to import | ✅ |

---

### TASK_04 — Connected Session (Ref 04)
| Component | Reference | Current Implementation | Gap |
|-----------|-----------|------------------------|-----|
| Session profile + endpoint | ✅ | `VpnSessionScreen.kt:131-134` | ✅ |
| Connected since / duration | ✅ | `VpnSessionScreen.kt:135-140` | ✅ |
| Traffic counters (RX/TX) | ✅ | `VpnSessionScreen.kt:141-149` | ✅ |
| Handshake timestamp | ✅ | `VpnSessionScreen.kt:151-156` | ✅ |
| Backend version | ✅ | `VpnSessionScreen.kt:157-159` | ✅ |
| Config details (addresses, routes, DNS, MTU) | ✅ | `VpnSessionScreen.kt:161-167` | ✅ |
| Android link verification | ✅ | `VpnSessionScreen.kt:168-184` + `VpnLinkReader` | ✅ |
| Path verification panel | ✅ | `VpnPathPanel.kt` (conditional) | ✅ |
| Disconnect button | ✅ | `VpnSessionScreen.kt:189-195` | ✅ |

**Gap:** Visual fidelity vs Ref 04 not measured

---

### TASK_05 — Security Center (Ref 05)
| Component | Reference | Current Implementation | Gap |
|-----------|-----------|------------------------|-----|
| Kill switch / Lockdown status | ✅ | `SecurityScreens.kt` + `ClientViewModel.lockdownEnabled()` | ✅ |
| DNS / IPv6 / Tunnel status | ✅ | `SecurityDiagnostics.kt` | ✅ |
| Compact rows (per task-05) | ✅ | `codex/task-05-security-center` branch | **PENDING MERGE** |
| No fake "fully protected" | ✅ | Honest "unknown" / "not configured" | ✅ |

---

### TASK_06 — Split Tunneling (Ref 06)
| Component | Reference | Current Implementation | Gap |
|-----------|-----------|------------------------|-----|
| Mode selector (Include/Exclude) | ✅ | `SplitTunnelScreen.kt` + `SplitTunnelMode` | ✅ |
| App list with toggles | ✅ | `InstalledApps` + `SplitTunnelPolicy.toggle()` | ✅ |
| Applied vs pending diff | ✅ | `ClientViewModel.syncSplitState()` | ✅ |
| Reconnect banner | ✅ | `SplitTunnelScreen.kt` dirty state | ✅ |

---

### TASK_07 — Advanced Network (Ref 07)
| Component | Reference | Current Implementation | Gap |
|-----------|-----------|------------------------|-----|
| MTU override | ✅ | `AdvancedNetworkSettings.mtuOverride` | ✅ |
| DNS mode (Profile/Custom/System) | ✅ | `AdvancedNetworkSettings.dnsMode` | ✅ |
| Custom DNS hosts | ✅ | `AdvancedNetworkSettings.customDns` | ✅ |
| Keepalive override | ✅ | `AdvancedNetworkSettings.keepaliveSeconds` | ✅ |
| Profile baseline display | ✅ | `ClientViewModel.profileBaseline()` | ✅ |
| Glyph size fix | ✅ | `codex/task-07-advanced-network` branch | **PENDING MERGE** |

---

### TASK_08 — Statistics (Ref 08)
| Component | Reference | Current Implementation | Gap |
|-----------|-----------|------------------------|-----|
| Session history list | ✅ | `StatisticsScreen.kt` + `UsageAggregator` | ✅ |
| Range selector (7d/30d/All) | ✅ | `StatisticsUiState.range` + `selectStatsRange()` | ✅ |
| Summary cards (total time, bytes, sessions) | ✅ | `StatisticsUiState.summary` | ✅ |
| Clear history (with confirmation) | ✅ | `confirmClearStats()` + `UsageStore.clear()` | ✅ |

---

### TASK_09 — Import Configuration (Ref 09)
| Component | Reference | Current Implementation | Gap |
|-----------|-----------|------------------------|-----|
| Profile name field | ✅ | `ImportScreen.kt:306-308` | ✅ |
| File import (SAF) | ✅ | `ImportScreen.kt:299-301` | ✅ |
| QR import (camera/gallery) | ✅ | `ImportScreen.kt:302-304` | ✅ |
| Clipboard paste | ✅ | `ImportScreen.kt:314-323` | ✅ |
| Manual text entry | ✅ | `ImportScreen.kt:326-332` | ✅ |
| FLAG_SECURE on import screen | ✅ | `ImportScreen.kt:294-298` | ✅ |

---

### TASK_10 — Profile Settings (Ref 10)
| Component | Reference | Current Implementation | Gap |
|-----------|-----------|------------------------|-----|
| Language selector (EN/AR/System) | ✅ | `SettingsRoute.kt` + `UiPreferences` | ✅ |
| Theme selector (Day/Night/System) | ✅ | `SettingsRoute.kt` + `UiPreferences` | ✅ |
| Notifications toggle | ✅ | `SettingsRoute.kt` (delegates to system) | ✅ |
| Reset to defaults | ✅ | `ClientViewModel.appearance("system", "system")` | ✅ |
| Pro upsell navigation | ✅ | `SettingsRoute.kt:202` | ✅ |

---

### TASK_11-21 — Repair Lab Services
| Service | Reference | Implementation | Visual Gap |
|---------|-----------|----------------|------------|
| Storage X-Ray (12) | Ref 07 | `StorageXrayScreen.kt` + `StorageScan.kt` | **UNVERIFIED** |
| Ghost Hunter (13) | Ref 04 (temp) | `GhostHunterScreen.kt` + `GhostHunter.kt` | Dark mode tones fixed (task-13) |
| Duplicate DNA (14) | Ref 06 | `DuplicateDnaScreen.kt` + `DuplicateDna.kt` | **UNVERIFIED** |
| Heavyweight Finder (15) | Ref 05 (large) | `HeavyweightScreen.kt` + `Heavyweight.kt` | **UNVERIFIED** |
| Cache Surgeon (16) | Ref ? | `CacheScreen.kt` + `CacheMaintenance.kt` | **UNVERIFIED** |
| Media Slim (17) | Ref 17 | `MediaSlimScreen.kt` + `MediaSlim.kt` | **UNVERIFIED** |
| Download Rescue (18) | Ref 08 | `DownloadRescueScreen.kt` + `DownloadRescue.kt` | **ACTIVE_AGENT** |
| Session Shield (19) | Ref ? | `SessionShieldScreen.kt` + `SessionShield.kt` | **UNVERIFIED** |
| Network Doctor (20) | Ref 02 (net) | `NetworkDoctorScreen.kt` + `NetworkDoctor.kt` | **UNVERIFIED** |
| Repair Autopilot (21) | Ref 09, 15 | `AutopilotScreen.kt` + `Autopilot.kt` | **UNVERIFIED** |

---

## DEVICE/LOCALE MATRIX (Visual QA Requirements)

| Screen | API 26 | API 35 | 360dp | 411dp | Day | Night | EN (LTR) | AR (RTL) | Font 1.0 | Font 1.6 | Reduced Motion |
|--------|--------|--------|-------|-------|-----|-------|----------|----------|----------|----------|----------------|
| Splash | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Welcome | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Home Disconnected | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Home Connected | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Server Selection | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Session Detail | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Security Center | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Split Tunneling | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Advanced Network | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Statistics | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Import Config | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Settings | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Repair Lab | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| All Repair Services | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

**Evidence Status:** API 26 ✅ (CI passes), API 35 ❌ (emulator loss), Phone captures ✅ (task16/17/18/21)

---

## PRIORITIZED VISUAL FIXES

| Priority | Screen | Fix | Branch | Effort |
|----------|--------|-----|--------|--------|
| 1 | Home (Ref 14/02) | Increase atmosphere bloom, strengthen glass depth, fix card rim lighting | — | HIGH |
| 2 | Tools Catalog (Ref 18) | Increase card height/padding, strengthen luminous edges, scale tool tiles | — | HIGH |
| 3 | Repair Lab (Ref 03) | Add glowing platform/portal under orb, strengthen neon card rims | — | HIGH |
| 4 | Advanced Network (Ref 07) | Glyph size fix (merge `codex/task-07-advanced-network`) | `codex/task-07-advanced-network` | LOW |
| 5 | Security Center (Ref 05) | Compact rows to reference proportions (merge `codex/task-05-security-center`) | `codex/task-05-security-center` | LOW |
| 6 | Repair Lab Day Mode (Ref 03) | Accents legible in day (merge `codex/task-11-repair-lab`) | `codex/task-11-repair-lab` | LOW |

---

**MATRIX COMPLETE — 22 SCREENS MAPPED, 3 CRITICAL VISUAL GAPS IDENTIFIED, 3 PENDING MERGES FOR QUICK FIXES**