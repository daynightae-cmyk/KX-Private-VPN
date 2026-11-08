# Visual reference batch 1 — Home (14), Tools (18), Repair (03)

Status: **runtime PASS with documented honesty deviations** on `emulator-5554`
(`kx_preview`, Android 8.0.0 / API 26). Root base: `b9fd0777b46a4c0dbd82d96d32eeabdb782ee725`.
Work branch: `visual-reference-batch-1`.

The owner-approved screenshots under
`KX_VISUAL_REFERENCE_MASTER_20/references/` remain the only visual authority.
The app draws live Compose UI; no reference screenshot is used as a background.
All values shown at runtime come from the running app or from an explicit
test fixture passed into a screen; no VPN, scan, cleanup, speed, country, flag,
latency or success value is fabricated.

## 1. Logo edge (P0)

| Item | Value |
| --- | --- |
| Preserved master | `docs/brand/KX_MASTER_LOGO_ORIGINAL_RGBA.png` |
| Master SHA-256 | `f336992dedba8f80580fc550735b0629c595f85d8c2a0587f08bf6f6fb2775e8` |
| Runtime asset | `android/app/src/main/res/drawable-nodpi/kx_brand.png` |
| Runtime SHA-256 | `21d95dab9ecb6362319d4cc1ed49582c5562ae4dd28bad1a711b3172b4f07c70` |
| Derivation | `android/scripts/prepare_brand_asset.py` |
| Allowed change | radial alpha feather only, centre `538.5`, radius `526.0 → 531.5` on the 1078 px master |
| Guarantee | every pixel inside radius `< 526` is byte-copied; alpha is `0` at radius `≥ 532` |
| Runtime draw | `KxOrb` draws the clean RGBA asset full-square, no crop, mask, dark backplate or overlay |

Evidence: `logo/logo-before-checkerboard.png`,
`logo/logo-after-checkerboard.png`, `logo/logo-before-right-edge-4x.png`,
`logo/logo-after-right-edge-4x.png`, `logo/logo-black-diagnostic.png`, and
`logo/home-orb-edge-runtime-4x.png` (a 4× crop of the Home orb captured from
this exact emulator run). The runtime crop shows no black rim, halo or
backplate on the light/dark orb edges.

## 2. Verdict per reference

| Ref | Screen | Runtime evidence | Verdict |
| --- | --- | --- | --- |
| 14 | Home | `runtime/identity-evidence/70-home-disconnected-en.png`, `71-home-night-ar.png`, `72-home-connected-en.png`, `76-home-connected-active-server.png` | **PASS** (with honesty deviations below) |
| 18 | Tools hub | `runtime/identity-evidence/44-tools-catalog-day-light.png`, `44c-tools-catalog-arabic-night.png`, `44d-tools-catalog-night-font160.png`, `44e-tools-catalog-360-night-ar-font160.png` | **PASS** (with honesty deviations below) |
| 03 | Repair tools hub | `runtime/identity-evidence/43a-repair-lab-day-header.png`, `43-repair-lab-day-light.png`, `44a-repair-lab-night-ar-large-header.png`, `44f-repair-lab-360-night-ar-font160.png` | **PASS** (with honesty deviations below) |

BEFORE captures: `baseline/home-before.png` and `baseline/home-before.xml` are
the pre-batch Home runtime and hierarchy. The old Tools/Repair surfaces were
captured by the separate task-16 workstream
(`docs/evidence/task16/phone-home-tools.png`,
`docs/evidence/task16/phone-repair-lab.png`); those files are intentionally
left untracked and unstaged because they belong to another agent. Ref 18 is a
new top-level route in this batch; the old “additional tools” grid was the
closest previous surface.

## 3. Reference 14 — Home

| Element | Result | Evidence / note |
| --- | --- | --- |
| Header gear + `PRIVATE VPN KX` lockup + orb disc | PASS | `71-home-night-ar.png`; gear start-side, lockup end-side |
| Centred title, subtitle, gradient underline | PASS | exact hierarchy and underline; subtitle copy is an honesty deviation |
| Hero orb, no black rim | PASS | 122 dp, clean RGBA, runtime 4× crop |
| Status pill | PASS | connected = green shield + dot; disconnected = honest grey state |
| Gradient power CTA | PASS | violet→cyan, pearl rim, real connect/disconnect action |
| Three-part session panel | PASS | server / received / duration; em dash when unmeasured; chevron follows reading direction |
| Four quick actions | PASS | import, shield, split, servers; reference order and icons |
| Recent activity | PASS | live `VpnSession` plus recorded history; honest empty state |
| Bottom navigation | PASS | home, servers, security, stats, settings; contextual Servers slot |
| Existing catalog / care / telemetry | PASS | preserved below the reference fold, no test tag removed |

Honesty deviations: the reference subtitle promises a fast connection and the
server card promises the fastest automatic server; the app instead says
“Private by design. Every value measured on this device.” and lets the user
choose an endpoint. The reference flag and 42 ms latency are omitted because
this build has no measured country/latency source. The reference activity feed
contains example sessions; the app shows the real live session or an honest
empty state.

## 4. Reference 18 — Tools hub

| Element | Result | Evidence / note |
| --- | --- | --- |
| Header back disc + brand lockup | PASS | `44-tools-catalog-day-light.png`, `44c-tools-catalog-arabic-night.png` |
| Centred orb + title/subtitle/underline | PASS | orb added at the reference position; Cairo weights |
| Functional search | PASS | filters by localised title/body; `tools-search` |
| Filter chips | PASS | All (9), Storage, Files, Media, Network; count only on All |
| RTL card order | REVISED in continuation | `(temporary, duplicates)`, `(large, download)`, `(media, smart repair)`, `(apps, network)` read right-to-left from the approved PNG; rerun device QA required |
| Full-width storage health | PASS | spans the content width in both directions |
| Nine real destinations | PASS | cache, duplicate, heavyweight, download, autopilot, media, network, apps, storage |
| Bottom nav | PASS | Tools slot selected in place of Servers; Home/Security/Stats/Settings present |
| Empty search result | PASS | honest `tools-empty` panel, no invented fallback |
| 360 dp + 1.6 font, Arabic/night | PASS | cards stack to one column and keep every title readable |

Honesty deviations: “Smart repair” describes an approval-based plan instead
of promising automatic repair; storage health offers a review of safe
improvements rather than a guaranteed optimisation. Body copy is concise and
localised, not a fabricated measurement.

## 5. Reference 03 — Repair tools hub

| Element | Result | Evidence / note |
| --- | --- | --- |
| Bespoke header | PASS | back at start, centred `KX PRIVATE VPN`, settings gear at end; RTL back arrow points right |
| Title, subtitle, underline | PASS | reference hierarchy; subtitle is an honesty deviation |
| Ringed hero orb | PASS | 146 dp orb inside 204 dp orbit ring; clean asset |
| Tap hint | PASS | real tap target starts the measurement flow |
| Eight-card RTL grid | PASS | `(temporary, duplicates)`, `(download, large)`, `(media, storage)`, `(network, apps)` |
| Comprehensive-repair CTA | PASS | full-width gradient action opens the real `autopilot` route |
| No bottom navigation | PASS | reference layout ends at the CTA |
| Preserved Ghost/Session/Autopilot | PASS | 10/10 services, snapshot, journal and cleanup remain below the fold |
| Installed apps | PASS | direct real `apps` destination, not a duplicate catalog service |
| 360 dp + 1.6 font, Arabic/night | PASS | header/hero/grid remain complete and scrollable |

Honesty deviations: the reference subtitle promises cleaning and performance
improvement; the app describes accessible storage, files and connectivity
instead. The CTA is labelled as starting the real approval-based repair flow,
not a simulated one-tap fix. Journal and snapshot values are only shown after
a real measurement.

## 6. Cross-cutting verification

- Locales: Arabic RTL first-class, English LTR, day/night.
- Widths: 360 dp and 411 dp fixtures (Home captures in
  `runtime/identity-evidence/home-layout-fixture-*`).
- Font scales: 1.0 and 1.6; bottom-nav labels are capped to one physical
  line to prevent mid-word Arabic breaks, while primary content still scales.
- Accessibility: semantic tags preserved for existing tests; status orb and
  controls expose button roles and labels; decorative artwork is excluded
  from semantics.
- Typeface: Cairo variable weights 400/600/700/800, OFL licence at
  `android/app/src/main/assets/licenses/Cairo-OFL.txt`.
- Emulator only: no physical-device capture was performed for this batch.

## 7. Test evidence

Executed on the final branch tree:

```
gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r -t app-debug.apk
adb install -r -t app-debug-androidTest.apk
adb shell am instrument -w -e class \
  "com.knoux.kxprivatevpn.HomeScreenTest,\
   com.knoux.kxprivatevpn.RepairLabScreenTest,\
   com.knoux.kxprivatevpn.ToolsCatalogScreenTest,\
   com.knoux.kxprivatevpn.VpnFlowTest" \
  com.knoux.kxprivatevpn.test/androidx.test.runner.AndroidJUnitRunner
```

Result: **350/350 JVM tests pass**, **`:app:lintDebug` succeeds**, both APKs
assemble, and **27/27 focused device tests pass** (Home 7, Repair 8, Tools 4,
VpnFlow 8); `ToolsCatalogTest` contributes 7 of the JVM tests. The Home
connected fixture starts its synthetic session 42 minutes before capture so the
measured duration renders realistically; the screen still computes it from the
provided `startedAtMillis` and never invents a value. CI must be re-run on the
exact pushed head before merge; local evidence alone is not a merge gate.

## 8. Remaining references

This batch covers references 14, 18 and 03 only. The other 17 approved
references remain queued for subsequent small batches; no claim is made here
about their visual state.
