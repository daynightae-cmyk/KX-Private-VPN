# Storage health reference evidence (reference 07)

Runtime screenshots captured from the native Compose `StorageXrayScreen`, not from a mock-up
renderer. Every number comes from `StorageXrayViewModel`: Android `StatFs` volume data,
`MediaStore` query rows the platform returned, and documents inside a tree the user explicitly
authorized through the Storage Access Framework (SAF).

## Reference

- `07-storage-health-hero.png` — owner-approved composition: brand header, title/subtitle,
  preserved KX orb with orbit accents, gradient analysis action, and the status/score panel with
  four category rows.

## Capture setup

- Emulator: `kx_preview`, Android API 26.
- Test class: `com.knoux.kxprivatevpn.StorageXrayScreenTest` (13 tests).
- Final local run: `OK (13 tests)`.
- Pure model: `StorageHealthModelTest` (8 JVM tests) covers unavailable, measuring, partial,
  lower-bound, failed-query and measured-zero rules without an emulator.
- Fixture volume: 64 GiB total with 20 GiB free (reported by the screen as 68.72 GB / 18.63 GB
  using the platform formatter).
- Fixture measurements: 5 MiB KX-owned data, and MediaStore images 2 GiB + videos 1 GiB +
  audio 500 MiB, summed to a 3.5 GiB measured media subtotal.
- On API 26 the `MediaStore` documents collection is unavailable, so the documents row honestly
  reports the version limitation in the light frame. The Arabic frame uses an authorized-folder
  scan (3,000 B documents, 500 B other) so all four rows carry measured values.
- Screenshots are captured from Compose semantics via `captureEvidence` after the behaviour in
  each test body has already been asserted. `68` and `70b` scroll the last health row into view
  before capture so the panel, not the screen top, is reviewed.

## Files

| File | State shown |
| --- | --- |
| `67-storage-health-hero.png` | Reference 07 hero: title, orb, analysis action and panel header |
| `68-storage-health-measured.png` | Scrolled panel: KX, media, documents and other rows with measured bytes, volume share and dashed unavailable rings |
| `69-storage-health-unavailable.png` | No volume or media measurements: dashed rings, `—`, no invented zeroes |
| `70-storage-health-night-ar-large.png` | Dark, Arabic RTL and font scale 1.6: mirrored header, title, CTA and panel header |
| `70b-storage-health-night-ar-panel.png` | Dark Arabic panel scrolled to the four measured rows |

SHA-256 of the committed PNG bytes:

```text
f416d0182a87fb52356a906754fc4d45c5f804db2d217f510fe387a577c2eb4e  67-storage-health-hero.png
c2ae21e76de3d37c3dfba4e028ab3f140b90a9417feccd2f5787570623b654b8  68-storage-health-measured.png
bd70a07f8030c3f8826577f4fa4ebbfaa1cef148c4d5a3f5bdb3a3d031aa06ac  69-storage-health-unavailable.png
50f4d95055e524f0598d0858810d5ded4bcacee12c287f8f3224c64cb33a73b4  70-storage-health-night-ar-large.png
3c959964429d236d3b2c07348ff8f294711b8eba0b7425d4078b02c6fc3dd797  70b-storage-health-night-ar-panel.png
```

## Intentional deviations from reference 07

- Reference 07 shows a health score of `A5 / 100`. The repository has no approved storage-health
  formula, so the score is always rendered as **Not available** / **غير متاحة**. No grade, score
  or percentage is synthesized; `StorageHealthModelTest` asserts that unavailable measurements
  stay null instead of becoming zero.
- The headline status is measurement coverage, not device health:
  `Measuring`, `Measured`, `Partial` or `Unavailable`. It never claims the device is healthy or
  unhealthy.
- Row percentages are the measured bytes divided by the real internal volume, clamped to 100%.
  They are not a quality rating, and categories can overlap (for example a media file inside an
  authorized folder is counted in both media and folder rows). The panel states this explicitly.
- SAF-sourced documents and other rows are labeled **Authorized folder only**; KX cannot see
  files outside the granted tree. A truncated KX or SAF measurement is labeled **Lower bound**
  and **Partial measurement**.
- No cleanup, deletion or "free space" action exists on this screen. It is a read-only analysis,
  as asserted by `storage-read-only`.
- The legacy measured detail cards (volume, reconciliation, MediaStore access, SAF folder scan
  and privacy) remain below the reference panel. Their functional tags and honest failure states
  are unchanged.

## Behaviour contract kept by the native screen

- Missing permission, a failed query or an unavailable platform collection is shown as
  `Not permitted`, `Failed`, `Not available on this Android version` or `Not measured`; never as
  `0 B`.
- MediaStore media subtotals sum only categories that returned measured bytes and become
  `Partial` when one of images/videos/audio is missing.
- Documents prefer the measured `MediaStore` row; on versions without it, a measured authorized
  tree is used and labeled as folder-only. Otherwise the limitation/failure is named.
- The start action triggers the real `StorageXrayViewModel.refresh()`; the instrumentation test
  asserts exactly one callback invocation.

## Reproduce

```powershell
cd android
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
$adb = 'D:\Android\sdk\platform-tools\adb.exe'
& $adb install -r -t app\build\outputs\apk\debug\app-debug.apk
& $adb install -r -t app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
& $adb shell am instrument -w -e class com.knoux.kxprivatevpn.StorageXrayScreenTest `
    com.knoux.kxprivatevpn.test/androidx.test.runner.AndroidJUnitRunner
```

Pull the frames from
`/sdcard/Android/data/com.knoux.kxprivatevpn/files/identity-evidence/` while the app is still
installed.
