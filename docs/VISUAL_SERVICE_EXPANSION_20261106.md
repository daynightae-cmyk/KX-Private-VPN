# KX Private VPN — sequenced launch and service scope, 2026-11-06

Status: **Implemented on isolated feature branch; not merged to main or released.**

## Owner reference
- Supplied screenshot `42260.jpg`: Android 15 mandatory OS launch splash currently shows a square PNG KX icon with black corners against the dark launch background.
- Requested progression: no initial square graphic; visible KX orb grows from a distant 3D pose; background and typography appear progressively; only a user-initiated tap enters; orb rotates before navigation.
- Existing visual identity preserved: `kx_brand.png`, Compose `KxOrb`, colors, background choreography and bilingual copy.

## Android platform constraints
- API 31+ requires a system splash window; cannot remove it entirely. `Theme.KX.Starting` now supplies a transparent vector as system icon to eliminate the extra square-logo appearance, retaining the navy handoff background. The visual KX logo is drawn by Compose in the next first frame.
- Native Compose sequence includes a depth-scaled flying orb and gradual atmosphere reveal; the orb spins by 360° for 680ms before `onEnter`. Reduced motion bypasses animation but still requires explicit tap. The click is one-shot.
- Existing KX VPN Home orb is now an accessible secondary connect/disconnect control only when a real profile/session permits it. The existing labeled button and permission flow are unchanged. No simulated tunnel.
- Home service tiles use wider two-column cards and one KX orb image per card, with the whole card as a tap target.

## New service: Installed Apps
- New `InstalledAppsRoute` reachable from Home **Apps** tile. Tap KX orb to query *launchable apps actually visible* through existing `ACTION_MAIN/CATEGORY_LAUNCHER` manifest visibility. It **does not** claim to discover all hidden or unlaunchable packages.
- Shows real labels and package names, separates user-installed from `FLAG_SYSTEM` / `FLAG_UPDATED_SYSTEM_APP`. System apps are **never offered an inline uninstall button**.
- For regular user apps only, removal requires (1) an explicit in-app confirmation and then (2) Android's own `ACTION_DELETE` system confirmation; no silent uninstall or accessibility automation. Android Settings provides full installed-app management when a broader inventory is desired.
- No `QUERY_ALL_PACKAGES`, `MANAGE_EXTERNAL_STORAGE`, administrator privilege, or delete-everything permission was added. Google Play policies for sensitive installed-app inventory and all-files permission would require separate eligibility and review.

## Existing services preserved
- `Duplicate DNA` already performs exact SHA-256 on file bytes, size filtering, keeper selection and re-verification prior to user-approved deletion. Its SAF scope is the user-authorized document tree; it does not circumvent private Android directories.
- `Cache Surgeon` only inspects/removes KX-owned cache with explicit preview, approval, checksum and safe-path validation. Android does not authorize one ordinary app to silently clean other apps' private caches.
- `Storage X-Ray` measures Android/MediaStore data within available permissions. Previous installed build's real-device Home/Security/Storage screens and entry gate were verified; this new feature branch's device runtime has **not** yet been inspected.
- The remaining repair tools retain their previously implemented/honest unavailable states; this branch does not claim a completed all-device cleaner.

## Verification matrix
- Baseline: `83d6a7264c175074fa28ecc97a0bf62cc954c015` from PR #16, already merged feature tasks through TASK 14 but **PR #16 not merged** at worktree creation.
- Local Gradle: `:app:compileDebugKotlin :app:assembleDebug :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest` => **BUILD SUCCESSFUL** in 1m35s; 150 JVM tests / 0 failures / 0 skipped; log `D:\KX-PRESERVE-20261106\visual-services-build-2.log`.
- Old initial Kotlin graphics-layer error (unqualified `maxHeight`) corrected before successful build. QA test changes to Home's VPN orb made after that build require an updated compile/CI check.
- APK SHA-256: `4E003FBB63D245981B4891211C00FD9A4E1466931FF8753F2D55A75C912F2B75`; signing certificate SHA-256 `c07168d455a86f8c792ef44ebf213ab63ec327226589692bcad69231e67c085e`, matching previously installed phone package.
- Physical itel device was disconnected from ADB during this feature build. **Do not report updated splash or app inventory runtime as phone verified yet.**
- API26/API35 CI on this exact feature SHA: **PENDING**. PR #16 previous baseline's API35 CI remained red due hosted emulator disappearing; do not merge either PR without an explicit evidence decision.

## Next gates, not completed
1. Verify incremental Android instrumentation compile and exact-SHA GitHub CI; remedy verified failures.
2. Reconnect physical itel, in-place install only if package/signature match and app data backup remains available; capture OS splash start, near/distant orb, tap-spin completion, Home, app inventory; verify the transparent vector avoids square branding.
3. Cover accessibility and RTL on actual phone, click targets at font scale 1.5x, no runtime crashes.
4. Finish persistent user-consented multi-tree SAF / scoped shared media duplicate scan only after clear permission design, full-disk performance limits and privacy validation. Do not claim access to `Android/data`, other app private caches or installed apps hidden by package visibility.
5. Merge feature branch into the canonical `daynightae-cmyk/KX-Private-VPN` main after new PR and required CI/review, not the separate `daynightae-cmyk/KX` repository.
