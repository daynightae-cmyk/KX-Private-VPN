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

# Play/F-Droid bundle (same build type, same rules)
./gradlew :app:bundleRelease
```

The release build type runs R8 (`isMinifyEnabled` + `isShrinkResources`) and is verified by the
`release-compile` CI job, which uploads the APK and the AAB together with their SHA-256 sums.

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
| Instrumented | `:app:connectedDebugAndroidTest` | API 26 + API 35 in CI |
| Release compile | `:app:assembleRelease :app:bundleRelease` | minify + shrink + real app bundle, with SHA-256 checksums |
| Dependency integrity | Gradle dependency verification | `android/gradle/verification-metadata.xml` pins every artifact checksum |
| CI | `.github/workflows/android.yml` | build + release-compile + device(26, 35), every action pinned to a commit SHA |
| SonarCloud | automatic analysis of `main` | Quality Gate **OK** (`new_security_rating = A`) on `377ffb3` |

The API 35 emulator is pinned to Google Emulator build `15004761` with the `swangle` GPU
backend; with that combination the full API 35 suite runs to completion (353 tests, 0 failed on
CI run `44257528678`). A lost emulator is still **not** a product assertion failure, but it is
also no longer the normal outcome.

---

## Security posture

- WireGuard private keys are zeroed from memory after parsing.
- Stored secrets are encrypted with an **Android Keystore** AES-256-GCM key:
  `ProfileStore` holds the imported WireGuard configurations (including the private key) and
  `ProIdentityStore` holds the installation credential. Only ciphertext is written to the
  app-private, backup-excluded directory; a missing key with an existing file is a hard error,
  never a silent re-key.
- `allowBackup=false`, full backup excluded, network security config blocks cleartext.
- Release shrinking + ProGuard rules keep the tunnel and Compose intact.
- Every Gradle dependency is checksum-verified, and every CI action is pinned to a full commit
  SHA, so neither the build inputs nor the pipeline can be moved silently.

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