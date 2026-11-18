# KX Private VPN — Native Android

Consumer-facing native Android VPN client built with Kotlin + Jetpack Compose.
The only transport is **WireGuard** via the official `com.wireguard.android:tunnel`
backend. No OpenVPN, no homemade crypto, no simulated tunnel.

**Canonical repository:** https://github.com/daynightae-cmyk/KX-Private-VPN
**Integration branch:** `main` (single source of truth)

---

## Reality contract (read before claiming anything)

| Claim | Required evidence |
|-------|-------------------|
| "Builds" | `./gradlew :app:assembleDebug` exit 0 at a recorded SHA |
| "Tests pass" | `:app:testDebugUnitTest` + `:app:connectedDebugAndroidTest` counts and exit codes |
| "VPN works" | Live WireGuard handshake (`latestHandshakeMillis > 0`) **and** traffic counters > 0 |
| "Released" | GitHub Release tag + signed AAB SHA-256 + store listing URL |

A successful compile is **not** a VPN connection. A "CONNECTED" label is **not**
proof of an encrypted tunnel. A merged PR is **not** a published APK.

---

## Project layout

```
android/                 Gradle project (AGP 8.13.2, Kotlin 2.2.21, Compose BOM 2025.04.01)
  app/src/main/java/com/knoux/kxprivatevpn/
    MainActivity.kt      Single Activity + Compose entry
    launch/              Cinematic splash + launch preferences
    designsystem/        KxOrb, Atmosphere, glass components, glyphs
    features/            All customer screens (Home, Servers, Repair Lab, Security, ...)
    platform/vpn/        VpnController, WireGuardVpnGateway, VpnSessionService
    platform/repair/     Real repair services (storage, duplicates, cache, ...)
    platform/security/   Pro authority, entitlements, session shield
    data/config/         ProfileStore, ConfigValidator, SplitTunnelStore
    platform/storage/    Scoped storage, SAF, MediaStore
docs/forensic-audit/     MISSION 01 forensic audit deliverables
docs/evidence/           CI and device evidence (screenshots, reports, captures)
public/assets/aistudio/  Owner-approved visual references (immutable)
```

---

## Build

```bash
cd android

# Debug (installable, debug-signed)
./gradlew :app:assembleDebug

# Unit + lint + test APK
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest

# Instrumented suite (requires a device or emulator)
./gradlew :app:connectedDebugAndroidTest

# Release (requires android/key.properties — see below)
./gradlew :app:assembleRelease
```

### Release signing

Release signing is **not** committed. Copy `android/key.properties.example` to
`android/key.properties`, fill in the owner keystore, and build. Without it the
release build falls back to the debug key and must not be published.

`android/key.properties`, `*.jks`, and `*.keystore` are gitignored.

---

## Configuration (owner-supplied, never invented)

| Item | Location | Default |
|------|----------|---------|
| PRO authority URL | `res/values/pro_config.xml` `pro_authority_url` | empty (no requests) |
| PRO contact | `res/values/pro_config.xml` `pro_owner_whatsapp_e164` | empty |
| WireGuard configs | Imported by the user (file / QR / clipboard / manual) | none |
| Release keystore | `android/key.properties` | absent |

Empty PRO config means the app performs **no** network requests and exposes **no**
invented contact. No server fleet, ping table, or fake endpoint is shipped.

---

## Test matrix

| Gate | Command | Notes |
|------|---------|-------|
| Unit | `:app:testDebugUnitTest` | JVM |
| Lint | `:app:lintDebug` | 0 errors |
| Instrumented | `:app:connectedDebugAndroidTest` | API 26 + API 35 |
| CI | `.github/workflows/android.yml` | build + device(26, 35) |
| SonarCloud | PR analysis | Quality Gate must be A |

API 35 emulator runs are subject to a known host GPU/RenderThread instability;
a lost emulator is **not** a product assertion failure. See
`docs/forensic-audit/CI_SONAR_AND_EMULATOR_FAILURES.md`.

---

## Security posture

- WireGuard private keys are zeroed from memory after parsing.
- No Android Keystore integration yet (tracked as a production hardening task).
- `allowBackup=false`, full backup excluded, network security config blocks cleartext.
- Release shrinking + ProGuard rules keep the tunnel and Compose intact.

Full threat model: `docs/forensic-audit/SECURITY_PRIVACY_LEAK_REGISTER.md`.

---

## Owner visual authority

The owner-supplied reference images are the **single source of visual truth**.
Gemini's React prototype is a functional inventory only — never the design target.
See `CODEX_REFERENCE_VISUAL_CONTRACT.md`.

---

## Agent rules

See `AGENTS.md`. Never touch the separate `daynightae-cmyk/KX` repository.
Never fake VPN/cleanup success. Never merge failing or unverified essential features.
Protect all branches, worktrees, and unique evidence.