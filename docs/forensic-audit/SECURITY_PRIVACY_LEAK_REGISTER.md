# SECURITY & PRIVACY LEAK REGISTER
**Generated:** 2026-11-15 11:55:00 UAE  
**Scope:** Threat model of sensitive data, permissions, logs, network, supply chain  

---

## THREAT MODEL: ASSETS & ATTACK SURFACE

### High-Value Assets
| Asset | Location | Sensitivity | Current Protection |
|-------|----------|-------------|-------------------|
| WireGuard Private Keys | `ProfileStore` (DataStore) → `ConfigValidator.parse()` → `VpnController.read()` | **CRITICAL** | Zeroed in memory after parse (`bytes.fill(0)`) |
| WireGuard Peer Public Keys | Same as above | HIGH | Same |
| VPN Endpoint Host/Port | Same as above | HIGH | Same |
| Allowed IPs / Routes | Same as above | MEDIUM | Same |
| DNS Servers | Same as above | MEDIUM | Same |
| Session Traffic Counters | `UsageStatsStore` (DataStore) | LOW | Plaintext |
| User Preferences (locale, theme) | `UiPreferences` (DataStore) | LOW | Plaintext |
| Repair Scan Results | Various Repair `*Store` (DataStore) | MEDIUM | Plaintext |
| App Usage Statistics | `UsageStatsStore` (DataStore) | LOW | Plaintext |

---

## DATA FLOW ANALYSIS

### VPN Configuration Import → Tunnel Start
```
User Input (File/QR/Clipboard/Manual)
    ↓
ConfigInput.read() / ZXing decode / Manual parse
    ↓
ConfigValidator.parse(bytes) → WireGuard Config object
    ↓
ProfileStore.import(name, bytes) → DataStore (XML)
    ↓
ClientViewModel → VpnController.connect(profile)
    ↓
VpnController.read(profile.id) → loads bytes → parse → **bytes.fill(0)**
    ↓
SplitTunnelConfigurator.apply(config) → per-app routing
    ↓
AdvancedNetworkConfigurator.apply(config) → MTU/DNS/keepalive
    ↓
WireGuardVpnGateway.start(config) → GoBackend.setState(UP)
    ↓
WireGuard Library VpnService → TUN interface + routes
```

**Leak Points:**
1. **DataStore persistence** — Private keys stored in plaintext XML (no encryption verified)
2. **Memory during parse** — `bytes` array exists briefly before `fill(0)` ✅ mitigated
3. **Crash logs** — `hs_err_pid*.log` may contain heap dumps with keys ❌ **RISK**
4. **Backup/ADB** — `android:allowBackup="false"` + `fullBackupContent="false"` ✅ mitigated

---

## PERMISSION ANALYSIS

| Permission | Protection Level | Usage | Justified? | Risk |
|------------|------------------|-------|------------|------|
| `FOREGROUND_SERVICE` | Normal | `VpnSessionService` notification | ✅ Yes | LOW |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Normal (API 34+) | VPN session companion | ✅ Yes | LOW |
| `POST_NOTIFICATIONS` | Dangerous (API 33+) | VPN session notification | ✅ Yes | LOW |
| `READ_EXTERNAL_STORAGE` | Dangerous (≤API 32) | Storage X-Ray media scan | ✅ Yes (scoped) | MEDIUM |
| `READ_MEDIA_IMAGES` | Dangerous (API 33+) | Storage X-Ray / Media Slim | ✅ Yes (scoped) | MEDIUM |
| `READ_MEDIA_VIDEO` | Dangerous (API 33+) | Storage X-Ray / Media Slim | ✅ Yes (scoped) | MEDIUM |
| `READ_MEDIA_AUDIO` | Dangerous (API 33+) | Storage X-Ray | ✅ Yes (scoped) | MEDIUM |
| `READ_MEDIA_VISUAL_USER_SELECTED` | Dangerous (API 33+) | Media Slim / Download Rescue | ✅ Yes (user-selected) | LOW |
| `ACCESS_NETWORK_STATE` | Normal | Network Doctor / VPN status | ✅ Yes | LOW |
| `INTERNET` | Normal | Pro backend / config download | ✅ Yes | LOW |
| `REQUEST_DELETE_PACKAGES` | Normal | Cache Surgeon / App uninstall | ✅ Yes (user-confirmed) | MEDIUM |

**Package Visibility:** `<queries>` for LAUNCHER apps only ✅ Minimal

---

## NETWORK EXPOSURE

### Outbound Connections (App-Initiated)
| Destination | Purpose | Protocol | Auth | Data Sent |
|-------------|---------|----------|------|-----------|
| WireGuard Peer Endpoint | VPN Tunnel | UDP/51820 (config) | Pre-shared key | Encrypted tunnel traffic |
| Pro Backend (`HttpsProAuthority`) | License/Entitlement | HTTPS/TLS | Certificate pinning? | Device ID, receipt |
| Config Import (User-provided) | Download config | HTTPS/HTTP | None | User credential (if in URL) |
| Network Doctor Probes | Connectivity test | DNS/HTTPS | None | None (probe only) |

### Inbound Connections
| Port/Interface | Purpose | Exposure |
|----------------|---------|----------|
| None (no exported services) | — | ✅ None |
| `VpnSessionService` | `exported="false"` | ✅ Internal only |

### VPN Tunnel Traffic
- **All device traffic** (or per-app subset) routes through WireGuard tunnel
- **Encryption:** ChaCha20-Poly1305 (WireGuard standard) ✅
- **Key Rotation:** Handshake every 2-4 minutes (configurable keepalive) ✅
- **Leak Risk:** Split tunneling misconfiguration → traffic bypass ✅ Mitigated by `VpnSessionOwnership` locks

---

## LOGGING & TELEMETRY

### Application Logs (Logcat)
| Component | Log Level | Sensitive Data Risk |
|-----------|-----------|---------------------|
| `VpnController` | Minimal (state transitions only) | ✅ No keys |
| `WireGuardVpnGateway` | Errors only (backend exceptions) | ✅ No keys |
| `ClientViewModel` | Errors only (store failures) | ✅ No keys |
| Repair Services | Info (scan progress, counts) | ✅ No paths in logs |
| `UsageRecorder` | Checkpoint saves | ✅ Aggregated only |

### Crash Logs (JVM Crash)
| File | Location | Risk | Mitigation |
|------|----------|------|------------|
| `hs_err_pid*.log` | `android/` | **HIGH** — May contain heap with private keys | **Must exclude from any upload** |
| `replay_pid*.log` | `android/` | **HIGH** — JVM replay state | **Must exclude from any upload** |

**Action Required:** Ensure `hs_err_pid*.log` and `replay_pid*.log` are in `.gitignore` and never uploaded to Crashlytics/Play Console.

---

## STORAGE & BACKUP

### DataStore (Preferences DataStore)
| Store | File | Encrypted? | Backup? | Risk |
|-------|------|------------|---------|------|
| `ProfileStore` | `profile_store.pb` | ❌ **NO** (plaintext protobuf) | `allowBackup="false"` | **MEDIUM** — Keys in XML |
| `SplitTunnelStore` | `split_tunnel_store.pb` | ❌ NO | `allowBackup="false"` | LOW |
| `NetworkSettingsStore` | `network_settings_store.pb` | ❌ NO | `allowBackup="false"` | LOW |
| `UiPreferences` | `ui_preferences.pb` | ❌ NO | `allowBackup="false"` | LOW |
| `UsageStatsStore` | `usage_stats_store.pb` | ❌ NO | `allowBackup="false"` | LOW |
| `LaunchPreferences` | `launch_preferences.pb` | ❌ NO | `allowBackup="false"` | LOW |
| Repair Stores (Cache, Download, Ghost, etc.) | Various `.pb` | ❌ NO | `allowBackup="false"` | MEDIUM (file paths) |

**Critical Gap:** **No Android Keystore / EncryptedSharedPreferences** for private keys. `ProfileStore` stores WireGuard configs (including private keys) in plaintext DataStore.

---

## DEPENDENCY SUPPLY CHAIN

### Android Dependencies (Gradle Lockfile)
| Dependency | Version | Source | Verified? |
|------------|---------|--------|-----------|
| `com.android.application` (AGP) | 8.13.2 | Google Maven | ✅ |
| `org.jetbrains.kotlin.android` | 2.2.21 | JetBrains | ✅ |
| `org.jetbrains.kotlin.plugin.compose` | 2.2.21 | JetBrains | ✅ |
| `androidx.compose:compose-bom` | 2025.04.01 | Google Maven | ✅ |
| `androidx.activity:activity-compose` | 1.10.1 | Google Maven | ✅ |
| `androidx.core:core-splashscreen` | 1.0.1 | Google Maven | ✅ |
| `androidx.datastore:datastore-preferences` | 1.1.4 | Google Maven | ✅ |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.9.0 | Google Maven | ✅ |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.9.0 | Google Maven | ✅ |
| `com.wireguard.android:tunnel` | 1.0.20260102 | WireGuard Android | ✅ Official |
| `com.google.zxing:core` | 3.5.3 | Maven Central | ✅ |
| `org.json:json` | 20240303 | Maven Central | ✅ |
| `junit:junit` | 4.13.2 | Maven Central | ✅ |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | 1.10.2 | Maven Central | ✅ |
| `androidx.test.ext:junit` | 1.2.1 | Google Maven | ✅ |
| `androidx.test:runner` | 1.6.2 | Google Maven | ✅ |

**No dynamic versions, no SNAPSHOTs, all from trusted repositories.** ✅

### Web Prototype Dependencies (Not in APK)
| Dependency | Version | Risk |
|------------|---------|------|
| `@google/generative-ai` | ^0.x | **NOT IN ANDROID BUILD** — web prototype only |
| `react`, `vite`, `tailwind` | Various | **NOT IN ANDROID BUILD** |

---

## ANDROID KEYSTORE / HARDWARE-BACKED STORAGE

### Current State: **NOT IMPLEMENTED**
- No `KeyGenParameterSpec` for WireGuard private keys
- No `AndroidKeyStore` provider usage
- No `EncryptedSharedPreferences` for DataStore encryption
- No `BiometricPrompt` for key access authorization

### Required for Production:
```kotlin
// Example: Generate/store private key in Android Keystore
val keyGen = KeyGenerator.getInstance("ChaCha20", "AndroidKeyStore")
val spec = KeyGenParameterSpec.Builder("wireguard_private_key", 
    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
    .setUserAuthenticationRequired(true)
    .setInvalidatedByBiometricEnrollment(true)
    .build()
keyGen.init(spec)
val secretKey = keyGen.generateKey()
```

---

## CERTIFICATE PINNING

### Pro Backend (`HttpsProAuthority.kt`, `ProHttpsTransportTest.kt`)
| Check | Status | Evidence |
|-------|--------|----------|
| Custom `X509TrustManager` | **IMPLEMENTED** | `HttpsProAuthority.kt` |
| Certificate pinning (SHA-256) | **IMPLEMENTED** | `ProHttpsTransportTest.kt` verifies |
| Hostname verification | **IMPLEMENTED** | Standard OkHttp |
| Certificate transparency | Unknown | Not verified |

---

## DEEP LINKS / INTENTS

### Exported Activities
| Component | Exported | Intent Filter | Risk |
|-----------|----------|---------------|------|
| `MainActivity` | `true` | `MAIN` + `LAUNCHER` | ✅ Standard |

### Exported Services
| Component | Exported | Permission | Risk |
|-----------|----------|------------|------|
| `VpnSessionService` | `false` | — | ✅ Internal only |

### Implicit Intents Handled
| Action | Component | Validation |
|--------|-----------|------------|
| `VpnService.prepare()` result | `ClientViewModel.prepareConnect()` | System intent, validated |
| `ActivityResultContracts.OpenDocument()` | `ImportScreen` | SAF URI, validated |
| `ActivityResultContracts.StartActivityForResult()` | VPN Consent | System intent |

**No custom URL schemes, no deep links, no exported receivers.** ✅

---

## PRIVACY COMPLIANCE

### Data Minimization
| Feature | Data Collected | Minimized? | User Control |
|---------|----------------|------------|--------------|
| VPN Config | Private key, endpoint, allowed IPs | ✅ Only what's needed | ✅ User imports |
| Split Tunneling | Package names (user-selected) | ✅ Only selected apps | ✅ User toggles |
| Usage Stats | Session duration, bytes, handshake | ✅ Aggregated only | ✅ Clear history |
| Repair Scans | File metadata (size, path, hash) | ✅ Only permitted scope | ✅ User initiates |
| Network Doctor | DNS latency, connectivity | ✅ Probe only | ✅ User initiates |

### Data Retention
| Store | Retention Policy | User Clearable? |
|-------|------------------|-----------------|
| `ProfileStore` | Until user deletes profile | ✅ `remove(id)` |
| `UsageStatsStore` | Until user clears | ✅ `confirmClearStats()` |
| `SplitTunnelStore` | Until user changes | ✅ `setSplitMode()` |
| Repair Stores | Until user clears | ✅ Per-service clear |

### GDPR / Privacy Rights
- **Access:** User can view all stored data in-app ✅
- **Rectification:** User can edit/delete profiles ✅
- **Erasure:** `confirmClearStats()`, `ProfileStore.remove()`, Repair clear ✅
- **Portability:** Config export **NOT IMPLEMENTED** ❌

---

## SECURITY FINDINGS SUMMARY

| Severity | Finding | Location | Remediation |
|----------|---------|----------|-------------|
| **HIGH** | Private keys stored plaintext in DataStore | `ProfileStore` | Implement Android Keystore + EncryptedSharedPreferences |
| **HIGH** | Crash logs (`hs_err_pid*.log`) may contain heap with keys | `android/` | Add to `.gitignore`, configure Crashlytics to exclude |
| **MEDIUM** | No certificate pinning verification for WireGuard endpoints | N/A (pre-shared keys) | N/A — WireGuard uses PSK |
| **MEDIUM** | Repair scan results contain file paths (PII-adjacent) | Repair Stores | Ensure `allowBackup="false"`, user clearable |
| **LOW** | No config export (portability) | `ProfileStore` | Implement secure export (user-initiated) |
| **LOW** | `INTERNET` permission for Pro backend — domain not restricted | Manifest | Network Security Config with domain allowlist |

---

## REMEDIATION ROADMAP

| Priority | Task | Effort | Blocker |
|----------|------|--------|---------|
| 1 | Android Keystore integration for private keys | 2-3 days | Requires API 23+ (minSdk 26 ✅) |
| 2 | EncryptedSharedPreferences for DataStore | 1-2 days | Requires Jetpack Security crypto |
| 3 | Crash log exclusion (Crashlytics/Play Console) | 0.5 days | Configuration only |
| 4 | Network Security Config for Pro backend domain | 0.5 days | XML config |
| 5 | Config export (encrypted, user-initiated) | 1-2 days | New feature |

---

**REGISTER COMPLETE — 2 HIGH, 2 MEDIUM, 2 LOW FINDINGS. PRIVATE KEY STORAGE IS PRIMARY RISK.**