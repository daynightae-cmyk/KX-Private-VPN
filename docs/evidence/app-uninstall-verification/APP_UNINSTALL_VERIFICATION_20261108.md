# App Uninstall Verification - Evidence

Date: 2026-11-08
Repo: `K:\KX-Private-VPN` (canonical)
Base `main`: `4caa988334ad66d5704d2f7c4942c8816381194c` (merge of PR #37)
Slice parent: `87bdafc75e7f0d33f9dc03c48a9c224c237b0d9c`
Branch: `app-uninstall-verification`
Code commit: `8aced6adf01526382af36260040e17eea299f80c`

## Defect

The Apps screen could only report **Returned from Android** after the system uninstall
confirmation closed. A cancelled confirmation and a completed uninstall produced the same
receipt, so the customer could not tell whether the exact package was still installed.
Activity result codes and a missing launcher entry were already known not to be proof of
removal, but no direct package-state read existed.

## Fix

- `InstalledApps.isInstalled(packageName)` re-reads one exact package through
  `PackageManager.getApplicationInfo` on the IO dispatcher. `NameNotFoundException` is a
  measured absence (`false`); a failed or denied read is `null`, never a removal.
- `AppInventoryModel` receives that verifier and starts it only after an uninstall handoff
  returns as `RETURNED`. Details and Settings handoffs never run it.
- The three measured outcomes are `REMOVED`, `STILL_INSTALLED` and `VERIFY_FAILED`.
  `REMOVED` requires the package read to return `false`; no result is shown while the read
  is in flight.
- A late verification is discarded if a newer handoff receipt has replaced the one it was
  started for.
- English and Arabic receipt strings name the target. The removed copy explicitly does not
  claim freed storage.

## Local gates

| Gate | Result |
|---|---|
| JVM `:app:testDebugUnitTest` | **343 tests, 0 failures** |
| `AppInventoryModelTest` | **33 tests, 0 failures** - includes 6 new verification cases |
| Lint `:app:lintDebug` | **0 errors, 144 warnings** |
| APK compile | `:app:assembleDebug` + `:app:assembleDebugAndroidTest` successful |
| API26 instrumentation `InstalledAppsScreenTest` + `InstalledAppsInventoryTest` | **16/16 passed in 101.3s** |

The instrumentation suite includes the three new receipt outcomes, an Arabic RTL removal
receipt, and a real PackageManager presence/absence read.

## Runtime evidence (API26 emulator)

Device `emulator-5554`: Android SDK built for x86_64, API 26,
fingerprint `Android/sdk_gphone_x86_64/generic_x86_64:8.0.0/OSR1.180418.026/6741039:userdebug/dev-keys`.
KX `versionName=0.1.0`, `versionCode=1`, `minSdk=26`, `targetSdk=36`. No physical phone was
touched and no VPN/Pro/cleanup state was simulated.

A disposable probe package, `com.knoux.probe` (debug-signed, no user data), was installed so
that a real Android uninstall dialog could be exercised without touching a real customer app.

| Evidence | What it proves |
|---|---|
| `01-still-installed-after-cancel-en.png` | After **CANCEL** in Android's dialog, KX shows **Still installed: Android reports KX Probe as present. No removal is confirmed.** `pm list packages com.knoux.probe` still returns the package and the app count stays 25. |
| `02-verified-removed-after-confirm-en.png` | After **OK** in Android's dialog, KX shows **Verified removed: Android no longer reports KX Probe as installed. No freed storage is claimed.** The app count drops to 24 and `pm list packages com.knoux.probe` returns no output. |

The two receipts are the only new customer-visible outcomes exercised live. The
`VERIFY_FAILED` branch is covered by deterministic JVM tests (a `null` verifier result and
a thrown `SecurityException`), because inducing a PackageManager read failure on a healthy
emulator would not be a trustworthy customer-flow test.

## Honest limits

- Runtime verification here is API26 only. API35 is covered by CI, not by a local image.
- The package read observes the current Android user's visible package state. It does not
  inspect another work profile, a hidden package, or prove that any storage was freed.
- The probe was removed at the end of the run. `pm list packages -3` then listed only
  `com.knoux.kxprivatevpn` and its test package.
- No physical-phone, public WireGuard egress, release-signing, Pro-production or store
  publication claim is made by this slice.
