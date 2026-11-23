# Release signing verification — 2026-11-23

First production signing identity was created **after** confirming that no pre-existing
keystore exists on this machine (repo tree, `AppData\Local\KX-Private-VPN`, user profile,
`D:\KNOUX-Workspace`). Only the standard Android SDK `debug.keystore` was present.

## Identity

- Keystore: `C:\Users\day night\AppData\Local\KX-Private-VPN\signing\kx-private-vpn-release.jks`
  (outside the repository, directory ACL: current user only, inheritance removed)
- Alias: `kx-private-vpn`
- Key: RSA **4096** bit, `SHA384withRSA`, validity **10950 days** (to 2056-11-15)
- Owner: `CN=KX Private VPN, OU=KNOUX, O=KNOUX, L=Cairo, C=EG`
- Passwords: 36-byte CSPRNG, stored only in the ACL-protected directory
  (`RECOVERY-README.txt`) and in `android/key.properties` (git-ignored, never committed)
- Private key algorithm: `PKCS12`-compatible JKS store, key password distinct from store password

## Public fingerprints

| Algorithm | Fingerprint |
|---|---|
| SHA-1 | `B2:95:1D:B9:81:91:0C:58:63:EE:8A:D4:DB:05:B5:92:72:14:7E:37` |
| SHA-256 | `2D:AF:CE:3D:FE:4A:54:DA:2F:93:0E:87:D6:F8:92:51:D1:6F:34:89:05:3D:3A:22:87:B0:9D:5B:1C:39:29:92` |

## Builds (this machine, branch `qa/lan-e2e-verified-20261123`)

```
.\gradlew.bat :app:assembleRelease :app:bundleRelease --console=plain -q
EXIT=0
```

| Artifact | Size | SHA-256 |
|---|---|---|
| `android/app/build/outputs/apk/release/app-release.apk` | 26,156,356 B | `d5f8dc49b2fbc1ecc7bd12721819dc6789f81d94edf765e4a34746eb4b968369` |
| `android/app/build/outputs/bundle/release/app-release.aab` | 21,353,000 B | `2889b777a475f3917c993d7663ffc347f93799e0f94edf1a0c859a9efed2dd0a` |

## apksigner verification

```
apksigner verify --print-certs --verbose app-release.apk
Verifies
Verified using v2 scheme (APK Signature Scheme v2): true
Number of signers: 1
Signer #1 certificate DN: CN=KX Private VPN, OU=KNOUX, O=KNOUX, L=Cairo, C=EG
Signer #1 certificate SHA-256 digest: 2dafce3dfe4a54da2f930e87d6f89251d16f3489053d3a2287b09d5b1c392992
```

`v1: false` is expected for `minSdk 26` with AGP 8 defaults (v2-only signing); it is
not a defect.

Bundletool `jarsigner -verify -strict` on the AAB reports `jar verified, with signer errors`
whose reasons are **self-signed leaf certificate + no PKIX trust anchor** — this is exactly
what an app-upload key looks like and is not a signing failure. Google Play is the trust
anchor for AAB uploads; the next step there is enrolling **Play App Signing** with this
upload key when the owner authorizes a first published release.

## Explicit non-claims

- Nothing was published or uploaded. Publishing/store submission needs separate explicit
  owner permission.
- This is not a statement that Play App Signing is configured; it is not (no Play Console
  release exists for this package yet).
- The debug-signed device build used for the LAN E2E
  (`2256EF98DF44AE5AD51344BE4EFB2130B27AA90A5FD1AA029C4BD21D5339E60D`) is **not** this
  production-keyed artifact and is not presented as a distribution build.
