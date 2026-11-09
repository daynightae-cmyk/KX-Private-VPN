# KX Owner References — independent visual continuation review, 2026-11-08

## Verified repository context
- Approved source set: `C:\Users\day night\Downloads\KX_VISUAL_REFERENCE_MASTER_20\references\`, exactly **20** PNGs.
- Visual agent's committed/pushed baseline: `visual-reference-batch-1` at `5e29eb4a572af68abb328f284d185a2ba4595720`.
- Base `origin/main`: `b9fd0777b46a4c0dbd82d96d32eeabdb782ee725`.
- Other agent's modified `DownloadRescueViewModel.kt`, `DownloadRescue.kt`, `Downloads.kt` and untracked task16/18 evidence are **not in scope** and must never be staged or overwritten.
- Compare using **actual runtime screenshot** and the approved source image at a common viewport. UI tests/CI establish behavior, not visual equality.

## Independent side-by-side inspection (not the visual agent's PASS assertions)

| Ref | Screen | Structural inspection | Visual fidelity | Next corrections |
| --- | --- | --- | --- | --- |
| 14 | Home | Header, status, session, actions, activity and nav largely correspond. | **PARTIAL**: runtime notably flatter, weaker atmospheric neon ribbons/glow and shallower glass than master; differences in content height and feature illumination. | Increase layered atmosphere and bloom *without screenshots/backgrounds*, refine orb sizing/hero, card rim lighting and CTA; preserve truthful disconnected/connected values. |
| 18 | Tools Catalog | RTL card order, search/filter, nine destinations and nav present. | **PARTIAL**: reference cards taller/brighter with stronger luminous edges and richer icon lighting; runtime titles/copy more compressed. | Card padding/radius/glow and tool tile scale; preserve keyboard/search/RTL/fontScale behavior. |
| 03 | Repair Lab | Reference order, ring orb, hint and primary CTA present. | **PARTIAL**: glowing platform/portal under orb and vivid neon card rims absent/subdued; runtime card heights and typography differ from reference. | Native Compose layered ring + floor glow, strengthen card treatments, maintain actual scan tap and real repair routes. |

Visual QA source pairs (unmodified):
- Ref 14: `14-main-home-vpn.png` vs `runtime/identity-evidence/71-home-night-ar.png`
- Ref 18: `18-tools-catalog-nine.png` vs `runtime/identity-evidence/44c-tools-catalog-arabic-night.png`
- Ref 03: `03-repair-lab-hub.png` vs `runtime/identity-evidence/44a-repair-lab-night-ar-large-header.png`
- Read-only review composites outside repo: `K:\_KX_VISUAL_AUDIT_20261108\ref14-vs-runtime.jpg`, `ref18-vs-runtime.jpg`, `ref03-vs-runtime.jpg`.

**Verdict:** the agent's `PASS` matrix can support structural and functional coverage, but does **not** establish pixel/lighting/spacing parity. Do not close P0 visual fidelity until comparable post-fix runtime captures are inspected.

## Continued implementation — owner reference 01, Media Slim Studio
- Scope: `MediaSlimScreen.kt` and new `MediaReferenceHero.kt` only, plus bilingual string resources and focused instrumentation assertions. Existing SAF folder grant, scan, file candidate selection, quality, explicit compression confirmation, cancel, journal/receipt behavior remain unchanged.
- Native Compose: premium violet/cyan reference atmosphere, shared KX header/title, big preserved KX RGBA orb with ring/orbit accents and media icon tiles, three real measurement tiles, direct folder picker and context-sensitive gradient CTA.
- Images/videos **from the actual selected-folder scan only**; before scan values are `—`. The source reference's sample 196 images, 5 videos, and 65% are intentionally **not fabricated**. Quality shows chosen encoder preset, not a fake “optimization percentage”.
- CTA: `choose folder` when no URI, `scan` when URI available without scan, `select supported images` disabled until user selects them, then opens existing confirmation. No silent modifications, no video conversion claim.
- Native images use original KX logo through `KxOrbBadge`, not a reference screenshot as UI background.
- Kept existing `route-media`, `media-folder`, `media-scan`, `media-compress`, selection and confirmation test tags below the fold.
- Required evidence gates: `:app:testDebugUnitTest`, `:app:compileDebugAndroidTestKotlin`, `:app:assembleDebug`, `:app:lintDebug`, emulator-run MediaSlimScreenTest, screenshots EN/AR day/night at 360/411dp and 1.0/1.6 font scales. Until a new screenshot from the exact APK is captured, **visual runtime verdict remains NOT VERIFIED**.

## Remaining reference matrix — do not assume any is completed
- `02-network-doctor-hero`
- `04-temporary-files-hero`
- `05-large-files-hero`
- `06-duplicates-hero`
- `07-storage-health-hero`
- `08-download-rescue-hero` (**other agent active; wait/coordinate**)
- `09-smart-repair-hero`
- `10-installed-apps-hero`
- `11-duplicates-results`
- `12-apps-management-results`
- `13-large-files-results`
- `15-smart-repair-plan`
- `16-network-doctor-results`
- `17-media-slim-results`
- `19-download-rescue-results` (**other agent active; wait/coordinate**)
- `20-temporary-files-results`

Safety boundaries: Do not claim Android scans every package/entire device, falsely present estimated speed/latency/savings or silently delete originals. Preserve unique Git work from all agents. Do not reset, clean, rebase, force-push, delete worktrees/files or install on the owner's physical phone.

## Exact-head CI failure discovered for the visual agent baseline
- PR #40: `https://github.com/daynightae-cmyk/KX-Private-VPN/pull/40`
- Workflow run `42484254384`, head `5e29eb4`: **build SUCCESS, device API 26 FAILURE, device API 35 FAILURE**.
- API 26: 316 planned instrumented tests, a later `ToolsCatalogScreenTest.rtlReadingOrderMirrorsTheReferenceRows` failed at the third RTL row. This is a real device-test failure, not proof of production readiness. The original test directly inspected potentially clipped offscreen bounds; revised to scroll each card into view first. The reference PNG also shows **Media before Smart** and **Apps before Network** from right to left, contrary to the initial agent handoff; the catalog and matching tests are updated to the actual reference.
- API 35: `AdvancedNetworkFlowTest` was marked failed at test 1, followed by `device emulator-5554 not found` and `Expected 316 tests, received 0` in teardown. Treat as **emulator loss / incomplete run**, not a proven app regression or PASS. CI needs exact-head rerun and log correlation.
- These follow-up fixes have no CI PASS claim until a new pushed SHA is exercised. Local Gradle builds and Compose test compilation are necessary but not sufficient.
