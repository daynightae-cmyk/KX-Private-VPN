# Download Rescue reference evidence (references 08 and 19)

Runtime screenshots captured from the native Compose `DownloadRescueScreen`, not from a mock-up
renderer. The screen is backed by the real structural validator in
`platform/repair/DownloadRescue.kt` and by the Storage Access Framework scanner in
`platform/storage/Downloads.kt`: only files inside the folder the user authorised are read, and
the format is judged from its real header/tail bytes.

## References

- `08-download-rescue-hero.png` - owner-approved hero: title, orb, local-check pill, floating
  format tiles, gradient folder action, allowed-folder card and measured counters.
- `19-download-rescue-results.png` - owner-approved results: measured summary, folder scope,
  local search/health filters, measured rows, selected-file actions and a bottom selection bar.

## Capture setup

- Emulator: `kx_preview`, Android API 26.
- Test class: `com.knoux.kxprivatevpn.DownloadRescueScreenTest` (24 tests).
- Final local run: `OK (24 tests)`.
- Test data is a deterministic seven-row measured scan: three incomplete ZIP streams, one
  bad-header ZIP, one intact PNG, one zero-byte ZIP and one format without a validator. Counts
  and bytes in the frames come from that fixture, not from reference placeholders.
- The search frame is captured after the platform IME is closed by a window-token
  `hideSoftInputFromWindow` call and after the vertical list is scrolled to the filter row, so
  the screenshot shows the full layout rather than the keyboard.
- `80`, `81`, `82` and `83` are light LTR frames; `84` is dark LTR; `85` is light Arabic RTL;
  `86` is light LTR at font scale 2.0.

## Files

| File | State shown |
| --- | --- |
| `80-download-rescue-hero.png` | Idle reference 08: orb, local-check promise, folder action, empty scope |
| `81-download-rescue-results-top.png` | Reference 19 summary, folder scope and selected-file bar |
| `82-download-rescue-results-rows.png` | Measured rows and the confirmed-removal action panel |
| `83-download-rescue-filtered.png` | `Failed` health filter, 2 of 7 shown, no rescan |
| `84-download-rescue-results-night.png` | Dark results frame with measured contrast |
| `85-download-rescue-results-ar.png` | Arabic RTL: right-first metrics, mirrored text alignment |
| `86-download-rescue-large-font.png` | Font scale 2.0: rows and confirmed removal remain reachable |

## Behaviour contract kept by the native screen

- Scan reads only the authorised document tree and the shared Downloads collection exposed by
  the platform; classification is local and nothing is uploaded.
- Health labels mean measured structure only: `Intact`, `Incomplete` (valid head, missing tail),
  `Damaged` (bad header), `Empty file`, `No validator for this format`, `Could not be read`.
- Removal requires a selection, a confirmation dialog that freezes the measured targets, a
  provider delete of each target and a measured receipt. A failed delete is reported as failed.
- Export copies exactly one selected file to a destination the user picks and reads the copy
  back to verify it; the original is untouched.
- Rename renames exactly one selected file through the provider and records the result.
- Search and health filters operate on the measured rows already in memory and never rescan.
- The bottom action bar owns layout space; it never overlays a row a user is trying to tap.

## Fixture honesty

The seven-row fixture uses APK-style display names but models them as ZIP containers, because the
production validator classifies ZIP headers/tails and does not verify APK signatures or install
payloads. This is test data for the visual layer only; the app does not claim APK validation.

## Intentional deviations from reference 19

Reference 19 shows per-row move, retry, resume and percentage progress. This implementation
renders none of those controls: the existing service can scan, verify, export, rename and remove
files, but it cannot resume an interrupted transfer or move a document between providers. No
progress percentage is shown because no transfer is running. Adding those actions requires a
real downloader/queue contract and recovery handling; they are not simulated here.

## Reproduce

```powershell
cd android
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
$adb = 'D:\Android\sdk\platform-tools\adb.exe'
& $adb install -r -t app\build\outputs\apk\debug\app-debug.apk
& $adb install -r -t app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
& $adb shell am instrument -w -e class com.knoux.kxprivatevpn.DownloadRescueScreenTest `
    com.knoux.kxprivatevpn.test/androidx.test.runner.AndroidJUnitRunner
```

Pull the seven frames from
`/sdcard/Android/data/com.knoux.kxprivatevpn/files/identity-evidence/` while the app is still
installed.
