# Changelog

All notable changes to KX Private VPN are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added
- Adaptive launcher icon (API 26+) with deep-ink gradient background and KX glass-orb foreground
- Legacy mipmap fallbacks (mdpi → xxxhdpi) from owner-approved `kx_brand.png`
- Production build type: minify + shrink resources + ProGuard rules
- Optional release signing via `android/key.properties` (keystore never committed)
- Network security config: HTTPS only, cleartext blocked
- `android/key.properties.example` + gitignore for release keystore
- `README.md` with build/release reality contract
- `CHANGELOG.md` (this file)
- `docs/STATUS_BLOCKERS.md` for coordination tracking

### Changed
- AndroidManifest: adaptive icon references (`ic_launcher`, `ic_launcher_round`)
- AndroidManifest: network security config reference, cleartext disabled
- CI: added `release-compile` job (`:app:assembleRelease`)
- CI: removed obsolete `api35-focused-diagnostic` (PR #40 merged)
- CI: `build` job now runs `:app:assembleRelease`
- `build.gradle.kts`: release build type with minify + shrink + ProGuard
- `build.gradle.kts`: optional release signing via `key.properties`
- `.gitignore`: untrack build artifacts, rollback APKs, ignore `key.properties` + `*.jks`
- Removed 56 MB rollback APKs from `docs/evidence/task*/phone-rollback/`

### Removed
- 47 local branches consolidated into single `main`
- 39 remote branches deleted (work already in `main` via PR #49)
- 16 stale worktrees pruned

### Security
- ProGuard rules protect WireGuard/Compose/coroutines/ZXing from shrinking
- Network security config enforces HTTPS, blocks cleartext
- Release build falls back to debug key (never publish without owner keystore)

---

## [0.1.0] — 2026-11-18

### Added
- **Core VPN transport** — WireGuard via official `com.wireguard.android:tunnel:1.0.20260102`
  - `VpnController` state machine (IDLE → CONNECTING → CONNECTED/DISCONNECTING/ERROR)
  - `WireGuardVpnGateway` wraps `GoBackend` with honest UP/DOWN checks
  - `VpnSessionService` foreground companion with chronometer + disconnect action
  - Consent flow via `VpnService.prepare()` + `POST_NOTIFICATIONS` (API 33+)
  - Session restore on process death (only when backend confirms UP)
  - Private keys zeroed in memory after parse

- **Split tunneling** (per-app routing)
  - `SplitTunnelConfigurator` applies `includeApplications` / `excludeApplications`
  - Real `PackageManager` discovery, persisted selection, dirty state detection
  - Self-exclusion prevents routing loops

- **Advanced network settings**
  - MTU override, DNS mode (Profile/Custom/System), keepalive per-peer
  - Config rebuild chain: `split.apply → advanced.apply → backend`

- **Configuration import**
  - File (SAF), QR (ZXing), clipboard, manual text
  - `ConfigValidator` strict WireGuard config parsing
  - `ProfileStore` + `ConfigInput` with private key zeroing

- **Statistics & usage**
  - `UsageRecorder` samples session transitions, 15s checkpoints
  - `UsageAggregator` provides 7/30/90/365-day windows + sparklines
  - History clear with atomic checkpoint rebase

- **Security Center**
  - Kill switch/always-on from `Settings.Secure` + `GoBackend.isLockdownEnabled()`
  - DNS from tunnel or `ConnectivityManager`, IPv6 from real routes
  - Auto Reconnect / Tracker Blocking honestly **Unavailable**

- **Repair Lab** (10 tools, 3 available)
  - Storage X-Ray (StatFs + MediaStore + SAF)
  - Cache Surgeon (scoped KX cache + system settings shortcut)
  - Network Doctor (bounded probes, real DNS latency)
  - Ghost Hunter, Duplicate DNA, Heavyweight Finder, Media Slim, Download Rescue, Session Shield — honestly unavailable

- **UI / Visual**
  - Cinematic splash: photons → ribbons → glass orb → caustic sweep → wordmark
  - `KxOrb` Canvas formation with byte-preserved owner artwork
  - `CrystalIdentity` atmosphere: deep-ink nights, pearl days, ribbons, pulse
  - `KxReferenceUi`: glass cards, glyphs, bilingual (AR/EN), RTL
  - `GlassAction`, `CrystalPanel`, `ServiceGlyph`, `RepairGlyph`

- **Internationalization**
  - 1352 EN / 1350 AR strings (parity)
  - RTL layout, font scale 1.0/1.6, reduced motion
  - System locale or app-selected (EN/AR/System)

- **Testing & CI**
  - 391 unit tests (JVM)
  - 316 instrumented tests on API 26 (CI green)
  - 350 instrumented tests on API 35 (emulator instability blocks)
  - Lint 0 errors
  - SonarCloud fixes merged (3 PR #40 issues resolved)

- **Architecture**
  - Kotlin 2.2.21, Compose BOM 2025.04.01, AGP 8.13.2, JDK 17
  - minSdk 26, targetSdk 36, compileSdk 36
  - WireGuard 1.0.20260102, ZXing 3.5.3, DataStore 1.1.4
  - Kotlin Coroutines, StateFlow, ViewModel, Compose Navigation

---

## [0.0.1] — 2026-11-03

### Added
- Initial native Android project structure
- Cinematic splash (`KxOrb`, `IdentityExperience`, `IntroTimeline`)
- `MainActivity` + Compose navigation (`NativeClient`)
- `ProfileStore`, `ConfigValidator`, `SplitTunnelStore`, `UsageStatsStore`
- `VpnController` stub, `WireGuardVpnGateway` stub
- `RepairLabScreen` skeleton with 10 unavailable tools
- Owner visual references in `public/assets/aistudio/`
- Baseline CI workflow

---

## Legend

- **Added** for new features
- **Changed** for changes in existing functionality
- **Deprecated** for soon-to-be removed features
- **Removed** for now removed features
- **Fixed** for any bug fixes
- **Security** for vulnerability fixes