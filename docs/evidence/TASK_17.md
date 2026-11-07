# TASK_17 — Media Slim Studio (real SAF image compression)

**baseline_sha:** `597342e` (main after TASK_16 / PR #21)
**branch:** `task17-media-slim`
**implementation_sha:** `b352fe0` (merged main `597342e` as `9ad106c`)
**merged_sha:** `bc590b5586b064c31cef21c6204b24f2a92e2ff5`
**status:** GREEN — merged to main; JVM, lint, API 26 emulator, physical API 35 phone and exact-SHA CI (run `42312482260`) all verified.

## Scope and truthfulness

The TASK_17 acceptance asks for original/compressed comparison, quality/codec choice,
batch progress, a safe output copy and verification. This slice implements that for **still
images** through the Storage Access Framework and the platform WebP encoder:

- A granted folder is persisted with read **and** write access. If write access is missing the
  scope is cleared and the screen says `media_permission_required`; compression is never
  offered against a read-only tree.
- Media files are measured by SAF metadata only. **No file content is read during a scan.**
- Selecting a file only adds its document id. The confirmation dialog states the exact
  supported count, the exact source bytes, and that originals are neither changed nor deleted.
- Compression writes a **new** WebP document in the same granted folder, then measures its
  provider-reported bytes, decodes the header bounds, and performs a bounded full decode.
  Only then is it a verified copy.
- A copy that is empty, unreadable, unverifiable or **not smaller** is deleted and reported as
  a failure. No receipt is fabricated, and the original document is never opened for write.
- Cancelling the batch cancels the active encode; the partial output is discarded in a
  `NonCancellable` cleanup, and completed copies remain honestly listed.
- Video is listed because the user needs truthful scope, but it is **not compressed** in this
  slice. The UI says so in English and Arabic. MediaCodec/MediaMuxer work is deferred rather
  than faked.

The MEDIA service is now available in the Repair Lab catalog (`7/10` available services) and
opens the real route `media`. Its previous route `repair/media` was an unavailable placeholder.

## Verification design

- `MediaSlimScanner` walks the user-granted tree breadth/depth bounded (10,000 entries, depth
  12, 32 KiB minimum), flags unreadable branches and truncation, and returns partial results on
  cancellation. Paths are display-only; document ids are the only handles.
- `ImageCompressor` downsamples to a 2560 px edge before re-encoding, bakes EXIF orientation
  into the copy, and verifies the output independently of the encoder's return value.
- `MediaSlimStore` keeps the folder memory in its own `media_slim` preferences, so choosing a
  photo folder cannot overwrite the Storage X-ray or another tool's SAF scope.
- `MediaSlimViewModel` exposes scan/selection/quality/confirmation/progress/receipt state and
  blocks duplicate jobs. All UI text is in `values/media_strings.xml` and
  `values-ar/media_strings.xml`.
- `MediaSlimScreen` is native Compose in the KX glass identity. Dialog strings are resolved
  before the dialog window opens, which keeps Arabic correct even when the dialog context is
  recreated.

## Results (executed locally)

### JVM / domain

Command (worktree `D:\KX-TASK17-20261106\android`, JDK/Gradle wrapper):

```
.\gradlew.bat :app:testDebugUnitTest --rerun-tasks --console=plain
```

Result: **BUILD SUCCESSFUL in 36s**, **169 tests, 0 failures, 0 errors, 0 skipped**
(21 suites). Log: `docs/evidence/task17/local-jvm-tests.txt`.

`MediaSlimTest` contributes 10 tests:

- scan classifies images/videos without reading content
- selection bytes and confirmation count only encodable images
- policy refuses empty/unsupported selections
- unknown size and minimum filter keep the scan honest
- entry limit and depth limit stop with a lower-bound flag
- an unreadable branch is flagged while measured files survive
- cancellation returns partial results flagged
- missing ids yield an empty selection
- MIME matching is case-insensitive

### Build and lint

Command:

```
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
```

Result: **BUILD SUCCESSFUL in 1m 24s**; debug APK and androidTest APK assembled.
Lint: **104 warnings, 0 errors** (`app/build/reports/lint-results-debug.xml`). The media
findings are the documented platform-`ExifInterface` recommendation and KTX suggestions; no
error-severity finding exists. Log: `docs/evidence/task17/local-verify-precommit.txt`.

### Instrumentation — API 26 emulator

`MediaSlimCompressorTest` + `MediaSlimScreenTest`: **13/13 passed**.
Adding `RepairFlowTest` (catalog/route integration): **14/14 passed**.
Real codec cases:

- real WebP copy is measured, verified and smaller
- EXIF rotation is baked into the copy
- a copy that is not smaller is deleted and reported
- failed write and failed create never leave a receipt
- unreadable and unsupported sources never create an output
- cancellation after copy creation discards the partial copy

These run real `Bitmap.compress`, real decode/verification and a byte-counting SAF seam; they do
not mock the encoder.

### Instrumentation — physical phone (API 35)

Device: `itel S686LN`, Android 15 / API 35, serial `1334825549012211`.
Installed in place with `adb -s 1334825549012211 install -r` (first install time preserved;
no uninstall, no `pm clear`).

Command:

```
adb -s 1334825549012211 shell am instrument -w \
  -e class com.knoux.kxprivatevpn.MediaSlimCompressorTest,\
com.knoux.kxprivatevpn.MediaSlimScreenTest,\
com.knoux.kxprivatevpn.RepairFlowTest \
  com.knoux.kxprivatevpn.test/androidx.test.runner.AndroidJUnitRunner
```

Result: **OK (14 tests)** in 35.789 s. Log: `docs/evidence/task17/phone-instrumentation.txt`.

### Permission and negative cases exercised

- missing persisted write grant → scope cleared, permission notice shown, no compression action
- read-only/unreadable image → failure, no output document
- create refused / write failed / empty output → no receipt
- output not smaller → output deleted, failure listed, original untouched
- cancellation → partial output deleted, cancelled flag shown
- unsupported MIME (GIF) and video → visible, not selectable, never sent to the encoder
- scan cancellation / unreadable branch / truncation → partial measured results, honest notice
- Arabic + night + 1.6× font on a phone → scope, selection summary, quality and confirmation all
  reachable, RTL layout intact

## Visual evidence

Phone captures were pulled from the app's external files directory after the test run and are
committed under `docs/evidence/task17/phone/`:

- `46-repair-lab-flow.png` — Repair Lab now reports 7/10 services and the MEDIA card is active
- `70a-media-day-top.png` — English day, folder scope, measured media, explicit quality
- `70-media-selected-confirmed.png` — selected image and confirmed batch boundary
- `71-media-receipt.png` — verified copy receipt with measured savings and an honest failure
- `72a-media-night-ar-large-top.png` — Arabic night, 1.6× text, RTL scope
- `72-media-night-ar-large-selected.png` — Arabic night selected summary at large text
- `73-media-night-ar-large-confirm.png` — Arabic confirmation dialog over the night screen

API 26 emulator captures (same UI contract, smaller viewport) are committed under
`docs/evidence/task17/emulator26/`:

`46-repair-lab-flow.png`, `70a-media-day-top.png`, `70-media-selected-confirmed.png`,
`71-media-receipt.png`, `72a-media-night-ar-large-top.png`,
`72-media-night-ar-large-selected.png`, `73-media-night-ar-large-confirm.png`.

**Before/after:** the TASK_16 baseline shows MEDIA as an unavailable Repair service
(`6/10`). This build changes the catalog count to `7/10`, opens the real Media Slim screen, and
removes the unavailable-route placeholder. The owner did not attach a dedicated Media reference
image, so the screen derives its composition from the existing owner-approved KX orb/ribbon/
glass identity rather than copying Gemini styling. Screenshots are runtime Compose/phone
captures, not design renders.

## Changed paths

- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/media/MediaSlim.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/media/ImageCompressor.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/media/MediaSlimStore.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/MediaSlimViewModel.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/MediaSlimScreen.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/NativeClient.kt`
- `android/app/src/main/java/com/knoux/kxprivatevpn/platform/repair/RepairCatalog.kt`
- `android/app/src/main/res/values/media_strings.xml`
- `android/app/src/main/res/values-ar/media_strings.xml`
- `android/app/src/test/java/com/knoux/kxprivatevpn/MediaSlimTest.kt`
- `android/app/src/test/java/com/knoux/kxprivatevpn/RepairCatalogTest.kt`
- `android/app/src/androidTest/java/com/knoux/kxprivatevpn/MediaSlimCompressorTest.kt`
- `android/app/src/androidTest/java/com/knoux/kxprivatevpn/MediaSlimScreenTest.kt`
- `android/app/src/androidTest/java/com/knoux/kxprivatevpn/RepairFlowTest.kt`
- `android/app/src/androidTest/java/com/knoux/kxprivatevpn/RepairLabScreenTest.kt`
- `docs/evidence/TASK_17.md` and `docs/evidence/task17/`

## Known limits and deferred scope

- **Video compression is not implemented.** The task's video path would require MediaCodec/
  MediaMuxer work, device codec variance and a much larger verification surface. Videos are
  measured, listed, clearly labeled, and never modified.
- **HEIC/HEIF decode depends on the platform/provider codec.** A decode failure is reported as
  unreadable; it is never counted as a successful compression.
- **EXIF orientation is baked into the copy; other metadata is not copied.** This is stated in
  the UI. A copy is a visual re-encode, not a metadata-preserving transcode. Color-profile
  preservation beyond what the platform decoder applies is not claimed.
- **Scan bounds:** 10,000 entries, 12 levels, 32 KiB minimum, first 200 entries displayed.
  Truncation is shown as a measured lower bound, never as a complete inventory.
- The system folder picker itself is not automated in tests; the SAF gateway is exercised with a
  real in-app tree in the codec tests. Missing write permission is simulated at state level and
  is also enforced by the real persisted-permission check in the ViewModel.
- Output names use a short time suffix; a provider that silently renames the document is
  displayed using the name the provider returned.
- Lint has 104 pre-existing/known warnings and 0 errors. The media `ExifInterface` warning is
  the documented recommendation to use `androidx.exifinterface`; the platform class is used
  deliberately in this slice and the warning is recorded, not suppressed globally.

## PR / CI / merge

- PR: https://github.com/daynightae-cmyk/KX-Private-VPN/pull/22
- head SHA: `b726fe7f06022dbbbf69321535d430330a4b3890`
- CI: run `42312482260` — build, device (26), device (35) all SUCCESS
- merged_sha: `bc590b5586b064c31cef21c6204b24f2a92e2ff5`
- next_task: TASK_18 (merged as `e47d8ee`, PR #24)
