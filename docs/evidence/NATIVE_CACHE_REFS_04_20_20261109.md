# KX Private VPN — Dyad refs 04 + 20 native conversion

Date: 2026-11-09. Native baseline: `origin/main@33fe6cd8db7db0520fd35cc44af0cde24c506893`.

## References (untouched)
- 04 — Temporary Files Hero: `04-temporary-files-hero.png`, SHA-256 `25f3e016f1772250147a976ac5f51eeafd826f8485b687193a2fc355dddae8a2`.
- 20 — Temporary Files Results: `20-temporary-files-results.png`, SHA-256 `5d40559a2624b6ff12ed3bd6aa0103ecdf240dce59f607d1e53fe94b7ad4679f`.
- Authoritative originals remain in `K:\KX_DYAD_VISUAL_HANDOFF_20261108\visual-lab\public\references`. No screenshots used as Compose backgrounds.

## Native implementation
- `cache` opens a standalone Kotlin Jetpack Compose screen, localized English/Arabic, RTL-aware, Day/Night, authentic KX orb, four orbit icons, saturated violet/cyan glass, intrinsic scroll and back navigation.
- Ref 04 hero and Ref 20 scoped preview/results share the **existing** `CacheViewModel` + `CacheMaintenance` data path. All displayed item counts and sizes are measured from `CachePreview`. No synthetic reclaimed bytes, fake scan success or third-party cache access.
- Real file selection, existing `cache-selected-summary` and confirmation dialog, verified cleanup journal, interruption handling, Settings handoff and existing permission/privacy boundaries preserved.
- Existing device test contracts remain intact; two additional Compose UI tests guard absence of fake measurements before scanning and the disabled cleanup action until selection.
- Code is constructed with native Compose primitives and original brand asset. No WebView or screenshot wallpaper.

## Local verification
- On the exact isolated candidate using `D:\.gradle` and `D:\Android\sdk`:
  `gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --offline --no-daemon`
- **BUILD SUCCESSFUL**, Gradle process exit 0; 80 tasks (19 executed, 61 up-to-date).
- Inspected real JUnit XML: **383 unit tests / 42 suites / 0 failures / 0 errors / 0 skipped**.
- Debug Android APK and instrumented-test APK built; new UI tests **compile but were not executed locally on a connected device**.
- The first isolated run failed because the archive lacked `backend/pro/client_contract_fixture.py`. Restored the exact committed fixture; repeated the complete suite, with `ProBackendContractTest` now passing. No tests disabled.
- GitHub PR CI API 26 / API 35 and native visual comparison still pending at time of creating this note. No physical phone session, no Pixel-perfect approval, no Production/Release claim.

## Safe merge policy
Only reviewed exact paths are staged from the isolated candidate into a new PR based on the exact main SHA. Keep all agent worktrees, untracked screenshots, original Dyad project and unrelated `D:\KNOUX-Workspace\KX` untouched. Merge only after PR CI and review of runtime screenshot/RTL/permission behavior.
