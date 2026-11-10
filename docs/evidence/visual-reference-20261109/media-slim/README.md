# Media Slim results evidence (reference 17)

Runtime screenshots captured from the native Compose `MediaSlimScreen`, not from a mock-up
renderer. Results are produced by the real scan in `platform/media/MediaSlim.kt` and by
`ImageCompressor.kt`: sizes are provider metadata, and savings are counted only from copies
whose output was measured after a real re-encode.

## Reference

- `17-media-slim-results.png` - owner-approved results: measured summary, quality presets,
  measured savings, searchable rows, selected-image action panel and honest pending values.

## Capture setup

- Emulator: `kx_preview`, Android API 26.
- Test class: `com.knoux.kxprivatevpn.MediaSlimScreenTest` (14 tests).
- Final local run: `OK (14 tests)`.
- Test data is a deterministic three-row measured scan: one JPEG (`1.80 MB`, eligible), one MP4
  (`24.00 MB`, never compressed) and one GIF (`900 KB`, unsupported). The measured frame adds a
  verified `600 kB` WebP copy of the JPEG, which is `66%` smaller.
- The search frames close the platform IME with a real back key before capture, then scroll to
  the filtered row, so the screenshots show the full result composition instead of the keyboard.
- `90`, `91`, `92`, `92b` and `94` are light LTR frames; `93` is dark Arabic RTL at font
  scale 1.6.

## Files

| File | State shown |
| --- | --- |
| `90-media-ref17-unmeasured.png` | Reference 17 before any run: `After` and `Saved` read `Not measured yet` |
| `91-media-ref17-measured.png` | One verified copy: `600 kB`, `66% saved`, receipt and action panel |
| `92-media-ref17-search.png` | Local search `portrait`: `1 of 3` rows, no rescan |
| `92b-media-ref17-filtered.png` | `Videos` filter: `1 of 3` rows, image and GIF absent |
| `93-media-ref17-night-ar-large.png` | Dark Arabic RTL at font scale 1.6: measured rows and action stay reachable |
| `94-media-ref17-actions.png` | Selected-image action panel and the no-delete/no-export/no-compare note |

## Behaviour contract kept by the native screen

- A scan reads only the document tree the user authorised through Android's Storage Access
  Framework; the result rows are provider-measured files, not placeholders.
- Only still images accepted by the compressor can enter the batch. Videos and unsupported
  images remain visible but cannot be selected or re-encoded.
- `Selected images` and `Before` use measured source bytes. `After`, `Saved` and the percentage
  appear only when a verified output exists; otherwise they read `Not measured yet`.
- A verified row shows the measured output size and the measured saving of that exact copy.
- Search, type filters and size/name/type sorting only narrow the rows already measured in
  memory. They never rescan, re-read or modify a file.
- Compression still requires the existing confirmation dialog and creates new WebP copies in
  the same folder; originals are untouched.

## Intentional deviations from reference 17

Reference 17 suggests estimated after-sizes, delete/export/compare controls and per-file
resolution or duration details. This build renders none of them: the service can measure,
select, compress and verify image copies, but it cannot estimate an unrun output, delete an
original, export a result, compare before/after images, or report metadata the scanner does not
read. Those actions require new reviewed contracts and permissions; they are not simulated
here.

## Reproduce

```powershell
cd android
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
$adb = 'D:\Android\sdk\platform-tools\adb.exe'
& $adb install -r -t app\build\outputs\apk\debug\app-debug.apk
& $adb install -r -t app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
& $adb shell am instrument -w -e class com.knoux.kxprivatevpn.MediaSlimScreenTest `
    com.knoux.kxprivatevpn.test/androidx.test.runner.AndroidJUnitRunner
```

Pull the six frames from
`/sdcard/Android/data/com.knoux.kxprivatevpn/files/identity-evidence/` while the app is still
installed.
