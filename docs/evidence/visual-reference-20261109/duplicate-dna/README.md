# Duplicate DNA reference evidence (references 06 and 11)

Runtime screenshots captured from the native Compose `DuplicateDnaScreen`, not from a mock-up
renderer. The screen is backed by the real SHA-256 scanner in
`platform/repair/DuplicateDna.kt`: only exact fingerprints are grouped, the oldest entry in each
group is the retained copy, and deletion is offered only for explicitly selected surplus copies.

## References

- `06-duplicates-hero.png` — owner-approved hero: title, orb, local-scan hint, category chips,
  allowed folder, start-scan action and last-scan summary.
- `11-duplicates-results.png` — owner-approved results: measured summary, local filter/search,
  category chips, grouped duplicate rows, selection bar and receipt.

## Capture setup

- Emulator: `kx_preview`, Android API 26.
- Test class: `com.knoux.kxprivatevpn.DuplicateDnaScreenTest` (21 tests).
- Final local run: `OK (21 tests)`.
- Test data is a deterministic two-group scan (three duplicate copies, 600 kB reclaimable) with
  realistic file timestamps; screenshots are taken from Compose semantics via `captureEvidence`
  after the behaviour in each test body has already been asserted.
- The search frame is captured after the platform IME is closed with a window-token
  `hideSoftInputFromWindow` call, so the screenshot shows the full layout rather than the keyboard.
- `64-dna-hero.png` and `66-dna-selection.png` are light LTR frames; `63-dna-night-ar-large.png`
  is the dark Arabic RTL frame at font scale 1.6.

## Files

| File | State shown |
| --- | --- |
| `64-dna-hero.png` | Idle reference 06: folder chosen, category chips, start-scan action |
| `60-dna-groups.png` | Measured groups, SHA-256 prefixes, retained copies and row selection |
| `65-dna-search.png` | Local search `dup2`, 1 visible group, no rescan, IME closed |
| `66-dna-selection.png` | Two selected copies, measured reclaimable bytes and delete action |
| `61-dna-read-only.png` | Read-only tree: cleanup disabled, regrant action visible |
| `62-dna-receipt.png` | Cleanup receipt with partial result, verified-absent count and journal warning |
| `63-dna-night-ar-large.png` | Dark, Arabic RTL, large font scale |

## Behaviour contract kept by the native screen

- Scan reads only the authorized document tree; hashing and comparison are local.
- Groups require an exact SHA-256 match. Near-duplicates are never offered.
- The retained copy is the oldest entry, tie-broken by relative path. It is labelled **Keep**, not
  "best", because the scanner has no quality ranking.
- Cleanup requires a non-empty selection, a confirmation dialog, provider write access, a
  re-verification of every selected file and a measured receipt.
- A read-only tree renders no delete action; it offers a regrant path instead.
- Search and category filters operate on the measured groups already in memory and never rescan.

## Intentional deviations from reference 11

Reference 11 shows per-row move, delete and "keep best" actions plus a star rating. This
implementation exposes only the mutation the existing scanner and provider contract can verify:
deleting selected surplus copies. It does not render move, compress, share or "keep best"
controls, because those operations are not implemented by the real service and must not be
simulated. The retained copy is still shown explicitly, and all header rows use measured counts,
not reference placeholder numbers.

## Reproduce

```powershell
cd android
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
$adb = 'D:\Android\sdk\platform-tools\adb.exe'
& $adb install -r -t app\build\outputs\apk\debug\app-debug.apk
& $adb install -r -t app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
& $adb shell am instrument -w -e class com.knoux.kxprivatevpn.DuplicateDnaScreenTest `
    com.knoux.kxprivatevpn.test/androidx.test.runner.AndroidJUnitRunner
```

Pull the frames from
`/sdcard/Android/data/com.knoux.kxprivatevpn/files/identity-evidence/` while the app is still
installed.
