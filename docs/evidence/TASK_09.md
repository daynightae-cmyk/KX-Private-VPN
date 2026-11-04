# TASK_09 — Secure configuration import (polish)

**baseline_sha:** `ecf9848` (main after TASK_08 merge and docs)
**branch:** `codex/task-09-import-polish`
**implementation_sha:** `cd6d5c7` (test-tag exposure only)
**status:** FUNCTIONALLY COMPLETE AND VERIFIED · VISUAL FIDELITY UNVERIFIED (image tooling
broken this cycle) · MERGED

## Scope
The secure import stack was already implemented and verified in earlier slices (Keystore AES-GCM
ProfileStore in noBackupFilesDir, strict ConfigValidator, SAF file, QR image via ZXing, clipboard,
manual text, FLAG_SECURE, privacy redaction). This task exposed the missing `import-qr` test tag so
the QR path can be exercised by automation, and ran the full gate.

## Changed paths
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/NativeClient.kt` (+1 testTag)

## Verification
- Local gates: compileDebugKotlin PASS, testDebugUnitTest 98/98, lint 0 errors, assembleDebug PASS,
  compileDebugAndroidTestKotlin PASS.
- CI run `41932130083` (head `cd6d5c7`): build PASS; API26 device PASS (79/79, 0 failures);
  API35 FAIL only on documented `IdentityTest.arabicRtlLargeText360dp` renderer loss.
- Existing coverage: `ConfigImportTest` (file + QR + missing-input rejection), `ClientFlowTest`
  (manual import persists across recreate, no connection claim, capture 08-native-home-imported.png).

## Known limits (honest)
- Visual fidelity against `09-import-configuration_670a2aea.png` was NOT re-verified this cycle:
  image-reading tooling is broken in this session, so no reference comparison was performed and no
  speculative visual changes were made. The import screen retains its existing `CrystalPanel` layout
  and test tags. Re-verify when image tooling is restored.
- Live-camera QR scanning is not implemented; import uses a picked image decoded by ZXing. This is
  documented, not faked.
- No owned WireGuard gateway; imported profiles are stored, not connected.

## Permission tests
No new permission. Profile ciphertext stays in app-private noBackupFilesDir with a Keystore key;
configs are never logged or backed up.

## PR / CI / merge
- PR: #8
- CI run: 41932130083 (head cd6d5c7) — build PASS; API26 device PASS (79/79).
- merged_sha: d34a08306df27205b35d8254f6eb93f4df332857
- remote_main_sha: d34a083
- next_task: TASK_10 — profile/preferences.
