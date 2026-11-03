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

Implementation commit, PR, CI and current main SHAs: append after actual delivery. No merge/release/deployment yet.

Reproduce from `android/`: `./gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest`; with an accelerated emulator: `./gradlew.bat :app:connectedDebugAndroidTest`.

Next: inspect API26/35 CI results, download/inspect each native capture; fix critical deviations, exercise cold/warm startup, AR/EN 360/411dp + large text, inspect frame timing before accepting TASK_00. Then implement full TASK_01 independently; do not move VPN or repair scope into the splash PR.

Official compatibility sources consulted: [AGP 8.13](https://developer.android.com/build/releases/agp-8-13-0-release-notes), [Compose compiler setup](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler), [system splash migration](https://developer.android.com/develop/ui/views/launch/splash-screen/migrate).
