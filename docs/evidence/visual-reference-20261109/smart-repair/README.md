# Smart Repair reference evidence (references 09 and 15)

Runtime screenshots captured from the native Compose `AutopilotScreen`, not from a mock-up
renderer. Every number comes from the app's own cache and aged history; nothing is estimated
or invented. The receipt reports only what was actually deleted or pruned, read back from the
operation.

## References

- `09-smart-repair-hero.png` — owner-approved emblem hero, measured stat trio (cache, thermal,
  app state), tap-to-measure orb, and gradient action.
- `15-smart-repair-plan.png` — owner-approved plan board with device checks, recommended repair
  card, selectable targets and progress journal.

## Capture setup

- Emulator: `kx_preview`, Android API 26.
- Test class: `com.knoux.kxprivatevpn.AutopilotScreenTest` (15 tests).
- Final local run: `OK (15 tests)`.
- Test data is a measured three-target plan: two cache files (`a.tmp` 1 KiB, `b.tmp` 2 KiB) and
  one aged history slice (1 journal + 2 usage entries). The real destructive tests seed a real
  cache file and aged journal/usage entries, approve them, and verify they are actually removed.
- Screenshots are captured from Compose semantics via `captureEvidence` after the behaviour in
  each test body has already been asserted.

## Files

| File | State shown |
| --- | --- |
| `87-smart-repair-hero-day.png` | Reference 09 hero: title, orb, three measured stats (cache, thermal gap, app healthy), hint and start action |
| `87b-smart-repair-plan-rows-day.png` | Plan board: summary, selected count, device checks (cache ✓, thermal gap, approval ✓), releasable space, recommended card with two switches |
| `87c-smart-repair-empty-day.png` | Honest empty state: no cache, no aged history, run button disabled |
| `87d-smart-repair-failure-day.png` | Measurement failure: no plan proposed, nothing deleted |
| `87e-smart-repair-plan-receipt-day.png` | Receipt after approved run: 2 deleted, 1 pruned, 3 KiB freed, complete |
| `88-smart-repair-hero-night-ar-large.png` | Dark, Arabic RTL, font scale 1.6: mirrored hero and three stats |
| `88b-smart-repair-plan-night-ar-large.png` | Dark Arabic plan board scrolled to targets, scope and receipt |

SHA-256 of the committed PNG bytes:

```text
0d9cbe0505d3a3fd855234c8febd64fe6913f776a29100025c2a976aee1ccf9e  87-smart-repair-hero-day.png
48ed5e4fb35c1deeb9734a102be1e74d341944d6d6568ca89db53b736e76e3d9  87b-smart-repair-plan-rows-day.png
5149d50bfa5420583f7a74d142b9016ee42920cea8efd6cc93b1d6a170134f07  87c-smart-repair-empty-day.png
fff7399a457ee3816e04083e2e992d3b6557e719c45244d0d53d01598556dbe4  87d-smart-repair-failure-day.png
5548af2082e9988250e63331b7ec6b29918ff2603558871b9d8b5d36b9a4162d  87e-smart-repair-plan-receipt-day.png
08ac5354aefb6c9977841442eb11815e9e3498af9dc38ee9de7ba7ab149bb13c  88-smart-repair-hero-night-ar-large.png
2b832bfdd7d4bafb7e8476ef2f8ad5f857833af0fb4830a7cccf124bc9cf366d  88b-smart-repair-plan-night-ar-large.png
```

## Intentional deviations from references 09 and 15

- Reference 09 shows thermal as "Normal" with a green check. On API 26 (and any Android < 29)
  the platform exposes no thermal signal. The screen honestly states **Thermal reporting
  unavailable on this Android version** instead of drawing a green check it cannot back. The
  fixture run confirms this gap is shown.
- Reference 15's three-button row (Re-measure, Select all, Clear) does not fit the 360 dp
  reference frame at the specified font sizes. The primary "Re-measure" action gets its own row
  and the two selection buttons share an equal row below it. This keeps every label on one
  readable line.
- The journal step "Repair" lights up while the run is busy. After the run finishes, the step
  stays lit because the repair actually happened; the "Complete" step lights only when the
  receipt is `complete == true`.
- Dates use the screen's locale (`localeOf(context)`) so Arabic stays Arabic; the process
  default locale is never used.
- No simulated progress bar, no invented percentage, no estimated savings. The receipt is
  built from `CacheMaintenance.removeApproved` and the retention prune, read back, and
  journaled.

## Behaviour contract kept by the native screen

- Every target is a real measured cache file or the real aged history slice. Nothing is invented.
- Cache files are deleted through `CacheMaintenance.removeApproved` which re-verifies size,
  mtime and SHA-256 before deleting; anything that changed is skipped, not deleted.
- Aged history is pruned through the same `ShieldRetention` rule the Session Shield uses.
  Fresh entries survive; the read-back proves it.
- The person must explicitly approve the destructive run. The first tap only asks; the second
  tap in the dialog confirms.
- The receipt reports exactly what happened: deleted count, pruned count, bytes freed, skipped
  items, failed items, and whether the run was complete, cancelled or partial. Every field is
  read back, never guessed.
- The run is journaled in `repair-journal.json` with the real outcome and byte count.

## Reproduce

```powershell
cd android
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
$adb = 'D:\Android\sdk\platform-tools\adb.exe'
& $adb install -r -t app\build\outputs\apk\debug\app-debug.apk
& $adb install -r -t app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
& $adb shell am instrument -w -e class com.knoux.kxprivatevpn.AutopilotScreenTest `
    com.knoux.kxprivatevpn.test/androidx.test.runner.AndroidJUnitRunner
```

Pull the seven frames from
`/sdcard/Android/data/com.knoux.kxprivatevpn/files/identity-evidence/` while the app is still
installed.