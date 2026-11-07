# TASK 18 — Download Rescue: real download integrity assistance

**Status:** implemented, verified on the emulator and on the physical phone.
**Branch:** `feature/task18-download-rescue` | **PR:** #24
**Baseline SHA:** `597342e` (main, TASK 16 merged) → merged with main `bc590b55` (TASK 17 merged)
**Implementation SHA:** `31fc5aa`

---

## What the task actually delivers

Download Rescue was an honest "not available yet" card. It now performs the real Android work:

1. The person opens a folder with the system document picker (`OpenDocumentTree`). KX reads
   **only** that folder. No other application data is reachable from this screen.
2. Every file in the folder is read and judged by a structural validator that compares the bytes
   the provider actually returned. Each format has its own head marker and its own end marker.
3. The result card states the measured condition of every file and the reason for it.
4. A format that has no validator is reported as **unsupported**. KX never calls an unknown
   format damaged.
5. KX never rewrites a damaged download. The offered actions are export, rename and removal.
   Removal only runs after the person confirms it in a dialog that names the files and their size.
6. Every action produces a receipt built from what the platform returned: how many succeeded,
   how many failed, the bytes actually copied or freed.

### Measured states

| State | Meaning | Example reason text |
| --- | --- | --- |
| Intact | the structure was checked and matches the format | "the file's structure was checked and matches its format" |
| Incomplete | the beginning is valid but the end is missing | "the beginning is right but the end is missing: an interrupted download" |
| Damaged | the beginning does not match the format | "the beginning of the file does not match its format" |
| Empty file | the file holds no bytes | "the file holds no bytes" |
| Unsupported | there is no validator for this format | "KX has no structural validator for this format, so it makes no claim about it" |
| Unreadable | the provider returned no readable bytes | "the provider did not return readable bytes" |

---

## Changed paths

```
android/app/src/main/java/com/knoux/kxprivatevpn/platform/repair/DownloadRescue.kt      (new)
android/app/src/main/java/com/knoux/kxprivatevpn/platform/storage/Downloads.kt          (new)
android/app/src/main/java/com/knoux/kxprivatevpn/features/DownloadRescueViewModel.kt   (new)
android/app/src/main/java/com/knoux/kxprivatevpn/features/DownloadRescueScreen.kt      (new)
android/app/src/main/res/values/download_strings.xml                                  (new)
android/app/src/main/res/values-ar/download_strings.xml                                (new)
android/app/src/test/java/com/knoux/kxprivatevpn/DownloadRescueTest.kt                 (new)
android/app/src/androidTest/java/com/knoux/kxprivatevpn/DownloadRescueScreenTest.kt    (new)
android/app/src/main/java/com/knoux/kxprivatevpn/features/NativeClient.kt               (route)
android/app/src/main/java/com/knoux/kxprivatevpn/platform/repair/RepairCatalog.kt       (availability)
android/app/src/test/java/com/knoux/kxprivatevpn/RepairCatalogTest.kt                  (count)
android/app/src/androidTest/java/com/knoux/kxprivatevpn/RepairLabScreenTest.kt         (subject)
android/app/src/androidTest/java/com/knoux/kxprivatevpn/RepairFlowTest.kt              (route walk)
```

---

## Real results

| Check | Command / device | Result |
| --- | --- | --- |
| JVM unit tests | `./gradlew :app:testDebugUnitTest` | BUILD SUCCESSFUL, 0 failures (incl. 16 `DownloadRescueTest` cases) |
| Lint | `./gradlew :app:lintDebug --rerun-tasks` | BUILD SUCCESSFUL, 0 errors |
| Instrumented suite, API 26 | `emulator-5554`, full `am instrument` | **174 tests, 0 failures** |
| Instrumented suite, API 35 | itel S686LN `1334825549012211`, full `am instrument` | **174 tests, 0 failures** |
| Screen contract | `DownloadRescueScreenTest` | 7 tests, 0 failures on both devices |
| Physical device, real SAF flow | itel S686LN | folder granted through the system picker, real files classified |

### Physical device classification run

Six test-owned files were pushed into the folder the person authorised and judged by the app on
the real phone (`docs/evidence/task18/download-test-files/`):

| File | Size | Reported state |
| --- | --- | --- |
| `archive-sample.zip` | 154 B | **Intact** — "the file's structure was checked and matches its format" |
| `valid-image.png` | 76 B | **Intact** |
| `interrupted-image.png` | 30 B | **Incomplete** — "the beginning is right but the end is missing: an interrupted download" |
| `damaged-file.pdf` | 126 B | **Damaged** — "the beginning of the file does not match its format" |
| `empty-download.bin` | 0 B | **Empty file** — "the file holds no bytes" |
| `unknown-payload.dat` | 256 B | **Unsupported** — "KX has no structural validator for this format" |

Header: 6 files inspected, 4 need attention. The files were deleted again and the folder grant
was released through the app's own "forget this folder" action, so the phone was left exactly as
it was found.

---

## Two real defects the device run found

### 1. A healthy archive was reported as interrupted

The first physical scan reported `archive-sample.zip` as **incomplete**. The validator read a
16 byte tail window, but a ZIP end of central directory record is 22 bytes plus an optional
comment, so the signature was never inside the window. A perfectly intact archive was being
called an interrupted download.

Fixed by making the tail window follow the format (a ZIP gets the whole possible end record),
and by never reading past the end of the file. `DownloadRescueTest` now builds a real archive
and a real PNG and proves that a 16 byte window misses the end record while the format window
finds it.

### 2. The shared download collection crashed below Android 10

Lint reported `MediaStore.Downloads#EXTERNAL_CONTENT_URI` requires API 29 while the app
supports Android 8, so the query would have crashed on API 26 to 28. Android before 10 has no
shared download collection at all: it is now reported as unavailable, the screen says so, and
the authorised folder stays the only source. The version rule is a pure function covered by a
JVM test.

---

## Visual evidence (physical itel S686LN, Arabic, right to left)

| File | What it shows |
| --- | --- |
| `docs/evidence/task18/phone-repair-lab.png` | the repair lab with real measured values (191 GB free of 244 GB, 52.12 GB used, KX cache 6 files / 17.90 KB, measured at 07/11/2026 4:17 PM) |
| `docs/evidence/task18/phone-download-rescue.png` | Download Rescue before a folder is authorised: scope, inspected count 0, attention 0, and the honest empty state |
| `docs/evidence/task18/phone-download-granted-empty.png` | the same screen after the folder was granted through the system picker: "Folder: QwenDownloads", scan and forget actions present |
| `docs/evidence/task18/phone-download-classified.png` | the six classified files with their measured state and reason |
| `docs/evidence/task18/phone-home-tools.png` | the tool grid on the phone in Arabic, right to left, with the KX glass orbs |

The screen was also verified in English, in day and night mode, and at a large font scale by the
instrumented suite.

---

## Honest limits

- KX judges only the five formats that have a real structural validator (PDF, PNG, JPEG, MP4,
  ZIP). Everything else is reported as unsupported instead of being guessed at.
- MP4 is judged from its file type box only. A container cannot be declared complete from a
  fixed window, so no completeness claim is made for it.
- The app never claims to repair a damaged file. It can export a copy, rename the file or
  remove it after approval.
- The shared download collection is only reachable from Android 10. On Android 8 and 9 the
  authorised folder is the only source, and the screen states that.
- Android itself refuses to hand out a tree grant for the top level Download folder on some
  devices; a sub folder has to be chosen. That is the platform's decision, not a KX limit.

---

## PR and CI

- PR: <https://github.com/daynightae-cmyk/KX-Private-VPN/pull/24>
- head SHA: `e4a41783c0729d0e5f44d40c944c8136ef5810fc`
- CI: run `42325382831` — build, device (26), device (35) all SUCCESS.
- merged_sha: `e47d8ee5418217f275809188c8da7ae1471af077`

## Next task

TASK 19 — KX session privacy center (`RepairServiceId.SESSION`): the real listing of KX owned
sessions, configurations and history, a safe revoke, an erase with a receipt, a retention rule
and the honest Android guidance. It must never touch another application's session.
