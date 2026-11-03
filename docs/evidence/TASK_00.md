# TASK_00 — native launch foundation

Status: PARTIALLY VERIFIED. Essential runtime and visual gates pending; no merge authorized by evidence yet.

## Reality and preservation

- Source base, freshly fetched origin/main and remote main: `37c5d8778ccdcd03dd78fca71ddcdc3d3b6df9d4`.
- Checkout: `K:/KX-Private-VPN`; task branch: `codex/task-00-native-cinematic`.
- Initial tracked tree clean; owner AGENTS/CODEX handoffs and 22 task documents were untracked and preserved unchanged. No stashes, alternate branches/worktrees or open PRs observed.
- Original React/source/reference files untouched; no access to the separate KX checkout was needed. All eleven canonical repository PNGs opened individually and inspected, matching the supplied visual references.

## Implementation

- Native Kotlin/Compose subproject, package `com.knoux.kxprivatevpn`, minSdk 26 / targetSdk 36, Gradle wrapper 8.14.6, AGP 8.13.2, Kotlin+Compose compiler 2.2.21, JDK 17 bytecode (built with JDK 21).
- Single Activity, AndroidX system splash, immediate Compose atmosphere; no network initialization.
- Bounded 3.6s native choreography: deterministic photon convergence, shield/ribbon formation, layered glass gradients, orbital satellites, caustic sweep, original brand reveal, wordmark and one final pulse. No infinite animation or simulated loading/security progress.
- Shared orb remains in place into Welcome preview. Skippable, lifecycle-aware animation; DataStore seen preference; saveable recreation state; settings observer for remove animations, battery saver and touch exploration. Preference failure is disclosed.
- AR/EN resources, Android layout direction, light/dark atmosphere, scrollable large-text content, 48dp+ controls and semantic logo/button descriptions.
- Preview CTA opens an honest scope disclosure. TASK_01 onboarding is not represented as complete.
- No Android permissions requested; backup and device transfer excluded. No credentials or personal data.

## Visual measurements before implementation

- Logo reference 1254×1254: orb approximately x=80..1165, y=88..1173; shield apex (627,212), base (627,726); crossing ribbons form the shield interior. Original bytes copied into drawable-nodpi; SHA-256 `c0b92dd84d60f6f0938bff177d7066a2f74a3eab59ba282438abb76da5f8681e`.
- Welcome 941×1672: hero orb ~x=164..778, y=156..766 (65% screen width); KX wordmark y~810..910; subtitle ~929; primary action y~1215..1356. Native intro uses a centered shared orb and full-bleed dark violet/cyan atmosphere; preview CTA intentionally discloses incomplete app scope.
- Known unverified differences: native ribbon formation has simplified optical geometry; raster settles over it; orb size/vertical composition, glass intensity and light/day appearance need real frame review. No claim of pixel parity or owner visual acceptance.

## Actual verification

- `gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`: first attempt built APK, then lint failed for API27-only windowLightNavigationBar in API26 theme; corrected by removing the attribute. Initial unit test task had NO-SOURCE before test files were added and does not count as a test pass.
- `gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest`: exit 0, BUILD SUCCESSFUL, 80 tasks, 38 seconds. Three JVM tests, zero failures/errors; bounded/ordered timeline, monotonic reveal, reduced-motion precedence.
- Local `connectedDebugAndroidTest`: exit 1, `No connected devices!`; emulator failures described in STATUS_BLOCKERS. Instrumentation compiled but has NOT yet run.
- Four device tests cover timed native frames/completion, immediate skip/disclosure, day/reduced-motion, DataStore persistence. Tests produce individual PNGs on the emulator; CI pulls these as artifacts. Device results, screenshot paths and animation profiling remain pending.

## Delivery and continuation

Implementation commit: `66ffc1097a33c22af6a3582f7036c3253edde4a9`; device test/CI follow-up: `f376e4af779fcb2aea6c34811b559d80e02bcca3`. Both pushed to the task branch. [Draft PR #1](https://github.com/daynightae-cmyk/KX-Private-VPN/pull/1). Main remains `37c5d8778ccdcd03dd78fca71ddcdc3d3b6df9d4`; no merge/release/deployment.

Initial real CI execution at `66ffc10`: [push run 41620943996](https://github.com/daynightae-cmyk/KX-Private-VPN/actions/runs/41620943996) and [PR run 41621059178](https://github.com/daynightae-cmyk/KX-Private-VPN/actions/runs/41621059178) FAILED. On API26 and API35, the timed choreography, reduced-motion/day render and preference tests passed; dialog visibility assertion failed because the test froze Compose's animation clock before Material dialog entrance. The fix resumes the test clock after the skip assertion. Build setup independently failed trying to install removed SDK package `tools`; explicit platform/build-tool packages replace that default. These failures are retained, not treated as passing acceptance.

Seven instrumented tests now cover the above plus Arabic RTL/1.5x font/360dp, a 411dp day layout, and real MainActivity recreation/background resume. Container width tests do not establish device-wide 360/411dp acceptance. Local compilation and lint passed after the test additions (exit 0, 57 tasks, 1m). [Follow-up run 41625979478](https://github.com/daynightae-cmyk/KX-Private-VPN/actions/runs/41625979478) is pending inspection.

CI capture script also records first-launch video, `am start -W` cold/repeat timings, native Welcome screenshot, and `dumpsys gfxinfo` frame data on fresh CI emulators. Do not claim these exist or establish 60fps until the corresponding run artifacts have been inspected.

Reproduce from `android/`: `./gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest`; with an accelerated emulator: `./gradlew.bat :app:connectedDebugAndroidTest`.

Next: inspect API26/35 CI results, download/inspect each native capture; fix critical deviations, exercise cold/warm startup, AR/EN 360/411dp + large text, inspect frame timing before accepting TASK_00. Then implement full TASK_01 independently; do not move VPN or repair scope into the splash PR.

## First captured runtime and corrective work

At `f376e4a`, [run 41625979478](https://github.com/daynightae-cmyk/KX-Private-VPN/actions/runs/41625979478): build succeeded; both API26/35 executed 7 tests with 1 failure each (Arabic large-font disclosure). The earlier dialog-clock failure was fixed; launcher recreation/resume passed. The large-text failure is a real UI accessibility gap: the disclosure body was not scrollable. Added native scrolling; acceptance awaits rerun.

At `6a1528461354bb02bd093e8d7bc52a2cc86bcbec`, [run 41626620376](https://github.com/daynightae-cmyk/KX-Private-VPN/actions/runs/41626620376), API26 produced actual [Welcome screenshot](ci-6a15284-api26/device-screenshots/launcher-welcome.png), [startup recording](ci-6a15284-api26/device-screenshots/kx-launch.mp4), [cold launch](ci-6a15284-api26/device-screenshots/cold-start.txt), [repeat launch](ci-6a15284-api26/device-screenshots/repeat-start.txt), and [frame timing](ci-6a15284-api26/device-screenshots/launch-frames.txt).

- Screenshot inspected individually: source brand silhouette recognizable; original artwork retained; observed black fringe around orb. Native mask was inset to remove that irregular source-background fringe. The day atmosphere and preview layout are NOT accepted as matching the supplied Welcome reference; full TASK_01 content/actions remain absent.
- Emulator default capture was only 320×640 pixels. It is not 360/411dp evidence. Follow-up CI selects Nexus 5 and records actual wm size/density before 360/411dp runs.
- Cold process launch `am start -W`: Status ok, TotalTime 1049ms, WaitTime 1055ms. Second force-stopped process launch: Status ok, TotalTime 1096ms, WaitTime 1123ms. This second measure is NOT a hot Activity resume or proof of animation smoothness.
- Software-GPU API26 recording: 87 total rendered frames, 87 janky (100%), p50=77ms, p90=150ms. **60fps gate FAILS on this measured emulator.** Recording and software GPU affect the result; no hardware performance conclusion is justified. Refactored background input to a stable pulse float so Compose can retain the full-screen background during photon/ribbon motion; profiling must be rerun to measure any improvement.
- Native test-frame PNGs were initially lost when Gradle uninstalled the app after instrumentation. CI now requests leaveApksInstalledAfterRun before pulling app-owned screenshots. The failed pull is documented, not counted as captured frame evidence.
- Latest local source check after scroll fix: testDebugUnitTest + lintDebug + assembleDebug + assembleDebugAndroidTest exit 0, 80 tasks, 1m40s. A later build attempt after the background-only optimization failed before compilation with Gradle UDP `BindException: Address already in use`; no host networking/processes were changed to force a pass. Exact current-source CI is required.

Correction commit: `be93953422347076b927b25796bbd5fd4829adb7`; subsequent background performance change remains subject to CI. No merge while visual/performance/large-text gates remain unresolved.

Official compatibility sources consulted: [AGP 8.13](https://developer.android.com/build/releases/agp-8-13-0-release-notes), [Compose compiler setup](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler), [system splash migration](https://developer.android.com/develop/ui/views/launch/splash-screen/migrate).
