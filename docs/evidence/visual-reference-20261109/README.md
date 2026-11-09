# Large Files reference evidence (references 05 and 13)

Duplicate DNA evidence for references 06 and 11 lives in
[`duplicate-dna/README.md`](duplicate-dna/README.md).

Runtime screenshots captured from the native Compose `HeavyweightScreen`, not from a mock-up
renderer. The screen reads real provider-reported sizes from Android `DocumentsContract`
metadata and exposes only the actions present in the implementation.

## References

- `05-large-files-hero.png` — owner-approved hero: title, orb, rank hint, allowed-folder and
  minimum-size cards, gradient rank action, measured scan summary.
- `13-large-files-results.png` — owner-approved results: filters, search, sort, size-spread
  heatmap, measured rows, selected-file detail and read-only privacy panel.

## Capture setup

- Emulator: `kx_preview`, Android API 26.
- Test class: `com.knoux.kxprivatevpn.HeavyweightScreenTest` (15 tests).
- Final local run: `OK (15 tests)`.
- Test data is a 14-entry scan with four measured large files; screenshots are taken from
  Compose semantics via `captureEvidence` after `waitForIdle()`.
- The search frame is captured after the platform IME is closed with `hideKeyboard()`; the
  screenshot therefore shows the full layout, not the keyboard overlay.

## Files

| File | State shown |
| --- | --- |
| `60e-heavy-reference-idle.png` | Idle reference 05: folder chosen, rank CTA enabled, read-only pill |
| `60-heavy-reference-hero.png` | Reference 05 composition after a measured scan |
| `60b-heavy-reference-results.png` | Reference 13 filters, search, sort and heatmap |
| `60c-heavy-reference-search.png` | Local search `backup`, 1 visible row, no rescan |
| `60d-heavy-reference-rows.png` | Measured result rows and size-spread tiles |
| `59-heavy-ranking.png` | Default measured ranking |
| `59b-heavy-selected-card.png` | Selected-file panel with Open action |
| `59b-heavy-selected.png` | Selected-file panel in context |
| `59c-heavy-night-ar-large.png` | Dark, Arabic RTL, large font scale |
| `59d-heavy-night-ar-results.png` | Dark, Arabic RTL results; right-first RTL heatmap |

## Intentional deviation from reference 13

Reference 13 shows delete, move, compress, share and multi-select actions. This implementation
is deliberately read-only because the MVP contract and the existing `HeavyweightViewModel`
scanner only authorize ranking and opening a file. No delete/move/compress/rename action is
rendered, and `readOnlyContractExposesOpenButNoDestructiveAction` asserts those controls do not
exist. Adding destructive actions requires a separate reviewed mutation flow, provider write
permissions and recovery handling; it is not simulated here.

## Reproduce

```powershell
cd android
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
$adb = 'D:\Android\sdk\platform-tools\adb.exe'
& $adb install -r -t app\build\outputs\apk\debug\app-debug.apk
& $adb install -r -t app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
& $adb shell am instrument -w -e class com.knoux.kxprivatevpn.HeavyweightScreenTest `
    com.knoux.kxprivatevpn.test/androidx.test.runner.AndroidJUnitRunner
```

Pull the frames from
`/sdcard/Android/data/com.knoux.kxprivatevpn/files/identity-evidence/` while the app is still
installed.
