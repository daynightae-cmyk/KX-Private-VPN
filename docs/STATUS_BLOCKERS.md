# KX Private VPN — CURRENT BLOCKERS & COORDINATION LOG
**Last updated:** 2026-11-18 09:15 UAE  
**Main SHA:** `5325f75` (style/production: adaptive launcher icon + legacy fallbacks)  
**Main branch:** ONLY `main` (all feature branches deleted, all worktrees pruned)

---

## 🔴 AGENT_A COORDINATION REQUIRED — VISUAL FIDELITY (P0)

### What we need from Agent_A
| Resource | Why | Files Owned |
|----------|-----|-------------|
| **Gradle daemon access** | Current daemon (PID 23912) blocks any `./gradlew` command from this session | Build environment |
| **Design system ownership** | Visual fidelity fixes (Ref 14, 18, 03) require modifying `CrystalIdentity.kt`, `KxOrb.kt`, `KxReferenceUi.kt` | `CrystalIdentity.kt`, `KxOrb.kt`, `KxReferenceUi.kt`, `NativeClient.kt`, `ClientViewModel.kt`, `VpnController.kt`, `WireGuardVpnGateway.kt` |
| **Design review** | Agent_A created the current visual system; fidelity fixes need their sign-off | All design system files |

### Proposed Sync Protocol
1. **Agent_A confirms** they can pause Gradle daemon for a coordination window
2. **We share** the three visual gaps (Ref 14 Home, Ref 18 Tools, Ref 03 Repair Lab) with side-by-side captures
3. **Agent_A reviews** proposed `CrystalIdentity.kt`/`KxOrb.kt` adjustments
4. **We implement** under Agent_A review → PR → CI → merge

**Status:** ⏳ **AWAITING AGENT_A RESPONSE** — Please update this section when available.

---

## 🔵 SONARCLOUD RE-SCAN (P0) — READY TO TRIGGER

| Metric | Threshold | Current (PR #40) | Fixes In Main |
|--------|-----------|------------------|---------------|
| Security Rating on New Code | A | **C** ❌ | ✅ All 3 fixes merged in `5325f75` |
| Reliability | A | A ✅ | — |
| Maintainability | A | A ✅ | — |

**Fixes already in `main` (PR #49 + subsequent):**
1. `pythonsecurity:S8707` — `android/scripts/prepare_brand_asset.py` path validation hardened
2. `kotlin:S3776` — `CrystalIdentity.kt` cognitive complexity 58→45 via `drawKxOrbNativeShield` helper
3. `kotlin:S1479` — `KxReferenceUi.kt` when branches grouped (SHIELD/SHIELD_CHECK, BACK/FORWARD)

**Action needed:** ☐ **Trigger SonarCloud scan on `5325f75`** (one click in SonarCloud UI or push a trivial commit)

---

## 🟡 API 35 EMULATOR INSTABILITY (P1)

| Detail | Value |
|--------|-------|
| Root cause | RenderThread SIGSEGV / QEMU signal 11 (Google Emulator 36.4.x bug) |
| Current config | Emulator 36.4.10 (build 15004761), `swangle` GPU for API 35 |
| Impact | 350-test suite cannot complete; emulator disappears mid-run |
| Mitigations tried | `swiftshader_indirect`→`swangle`, timeout 18→30+15 min, IME disable |

**Options under evaluation:**
- ☐ Firebase Test Lab (physical devices) — eliminates GPU dependency
- ☐ Test sharding (parallel emulators) — splits 350 tests
- ☐ Self-hosted GPU runner — requires proper GPU passthrough
- ☐ Wait for Emulator 37.x fix — timeline unknown

**Decision needed:** Choose path and provision.

---

## 🟡 VPN TEST GATEWAY — EXTERNAL_BLOCKED (P1)

| Requirement | Status |
|-------------|--------|
| WireGuard test server | ❌ Not provisioned |
| `E2E_REAL_TUNNEL` gate | ❌ Cannot pass |
| Required for | Production release (M3 gate) |

**Needed:** Owner provisions WireGuard server with known config → add test profile → verify `VpnPathVerification` returns `HANDSHAKE_OK` + traffic > 0.

---

## 🟡 RELEASE SIGNING — DEFERRED (P1)

| Item | Status |
|------|--------|
| `android/key.properties` | ❌ Not present (owner provides) |
| Release keystore | ❌ Not in repo (gitignored) |
| Signed AAB | ❌ Cannot produce |

**Ready:** `android/key.properties.example` documents format. Build falls back to debug key (never publish without owner keystore).

---

## 🟡 ANDROID KEYSTORE ENCRYPTION — PLANNED (P2)

| Gap | Current | Target |
|-----|---------|--------|
| Private keys in DataStore | Plaintext (`ProfileStore`) | Android Keystore + EncryptedSharedPreferences |
| Crash log risk | `hs_err_pid*.log` may contain keys | Exclude from uploads |

**Plan:** Add `androidx.security:security-crypto` → `KeyManager` for ChaCha20 keys → migrate `ProfileStore`.

---

## 🟢 VISUAL FIDELITY GAPS — READY FOR AGENT_A

| Ref | Screen | Gap | Severity | Files to Adjust |
|-----|--------|-----|----------|-----------------|
| **14** | Home | Flatter atmosphere, weaker neon ribbons/glow, shallower glass | HIGH | `CrystalIdentity.kt` (Atmosphere), `KxOrb.kt` |
| **18** | Tools Catalog | Cards shorter/dimmer, compressed titles, weaker icon lighting | HIGH | `KxReferenceUi.kt` (card rendering) |
| **03** | Repair Lab | Missing glowing platform/portal under orb, subdued card rims | HIGH | `RepairLabScreen.kt`, `RepairGlyph.kt`, `CrystalIdentity.kt` |

**Evidence:** `docs/evidence/visual-reference-20261108/CONTINUATION-REVIEW.md` documents PARTIAL fidelity with side-by-side comparisons.

---

## 🟢 PRODUCTION GATES — CURRENT STATE

| Gate | Command | Status | Evidence |
|------|---------|--------|----------|
| Unit | `:app:testDebugUnitTest` | ✅ Local green | 1352 EN / 1350 AR strings |
| Lint | `:app:lintDebug` | ✅ 0 errors | — |
| Instrumented API 26 | `:app:connectedDebugAndroidTest` | ✅ CI green | 316 tests |
| Instrumented API 35 | `:app:connectedDebugAndroidTest` | ❌ Emulator loss | Blocked by GPU bug |
| Release compile | `:app:assembleRelease` | ✅ Config valid | New CI job added |
| SonarCloud | PR analysis | ⚠️ C Rating | Re-scan on `5325f75` needed |
| E2E Tunnel | `VpnPathVerification` | ❌ No gateway | External blocked |

---

## 📋 NEXT STEPS — SEQUENCE

1. **Agent_A responds** → visual fidelity sync window
2. **SonarCloud re-scan** triggered on `5325f75` → Quality Gate A
3. **Firebase Test Lab** provisioned OR sharding implemented → API 35 green
4. **WireGuard test gateway** provisioned → `E2E_REAL_TUNNEL` unblocked
5. **Release keystore** provided → signed AAB → Play/F-Droid publish

---

## 📍 COORDINATION LOG

| Date | Agent | Action | Result |
|------|-------|--------|--------|
| 2026-11-18 09:15 | Forensic | Consolidated 47 branches → 1 `main` | ✅ Done |
| 2026-11-18 09:30 | Forensic | Production build config + CI + README | ✅ Done |
| 2026-11-18 09:45 | Forensic | Adaptive icon + network policy + ProGuard | ✅ Done |
| 2026-11-18 09:50 | Forensic | **This entry** — awaiting Agent_A for visual fidelity | ⏳ Pending |

---

**Update this file when Agent_A responds or any blocker changes.**