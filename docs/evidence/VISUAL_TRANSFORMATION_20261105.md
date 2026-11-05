# VISUAL TRANSFORMATION — owner handoff `KX-QODER-VISUAL-HANDOFF-20261105`

**baseline_sha:** `a1d7c08` (main after TASK_14 merge `8b0d716`)
**branch:** `codex/visual-consumer-ui`
**acceptance device:** itel S686LN, Android 15 (API 35), `1334825549012211`, 1080×2436 @ 480 dpi
(360 dp width), system locale `ar-EG`
**status:** PENDING — PR / CI / merge recorded at the bottom of this file

## 1. Reference set verified by hash and by eye

All eleven approved references were opened and inspected, not just listed. Recomputed SHA-256
matches `SHA256_REFERENCES.txt` byte for byte:

| File | SHA-256 |
| --- | --- |
| `00-brand-logo.png` | `C0B92DD84D60F6F0938BFF177D7066A2F74A3EAB59BA282438ABB76DA5F8681E` |
| `01-welcome.png` | `464A7C4F7FF3BDB0C703578CB886CF942CDD09E6D09FB6DAD149BDD9996458C6` |
| `02-home-disconnected.png` | `825C75B082FDB1E2C4528AD3F62C5B1B35B003B069D55773DF39C7A995F3A28E` |
| `03-server-selection.png` | `8D2C742A10C9CDF0B2792C143479874891C1A28F930B323A6B0F6A572538C587` |
| `04-connected-session.png` | `F0CFE658E5F66DC9C6DBCDCD371FFF0B825B2F30A32E9C089D5C811A32FFEF30` |
| `05-security-center.png` | `D0A1F09AF2DD856B2B4FEB891167BF9994FA20C861960D5EC65D0506E104F218` |
| `06-split-tunneling.png` | `C9265FD7BE8AE14D03296E32C3910158F6D4395B95C3BF6E7CB3241FEABC9B3B` |
| `07-advanced-network.png` | `5B3214F408BD841409FC1A75AD01239A73C40F459EE5D2EAFAC55AA8A2D72022` |
| `08-statistics.png` | `A6C474B99A3C55BB9E4B6DF265FC6CE3C89CA7B265302E53A0CB6B256B9852D0` |
| `09-import-configuration.png` | `670A2AEA5230FD66B05D533F5F78A04D4200E520B483C9847DADAD35497192CE` |
| `10-profile-settings.png` | `750319564B5965A0F71EF963F2EB4C3709B0EDFBFB2A8EC7B947C7C406E57BE6` |

What was adopted: the glass orb identity, the single dominant Connect action, the card grid for
care services, the icon-plus-short-label-plus-tiny-status card anatomy, the bottom navigation
bar, the search-first server list, and the dark-indigo/violet-cyan palette.

What was deliberately rejected: the invented countries, `23 ms` ping values, `Load 32%` bars,
starred "Recommended" rows, the `10.13.1.7` address and the fully-protected shield. Those are
fictional content in the reference render, and KX has no server fleet, no latency probe and no
leak test to back them. No reference image is used as a background or as a data source.

## 2. Screens built

- `features/HomeScreen.kt` — new consumer Home: brand lockup + settings gear, 176 dp `KxOrb`,
  one status pill, one `GlassAction` Connect CTA (disabled until a real profile exists), the
  selected-server card that opens the real profile list, a 2×2 metric grid that only ever shows
  measured session values or `—`, the KX Care summary with the honest `5 of 10 ready` count,
  a 4×2 tool grid, and a five-tab bottom bar that lives **outside** the scroll container.
- `features/ServerSelectionScreen.kt` — new Server Selection: search over imported profiles only,
  one card per real endpoint, select/remove, explicit `Latency not measured` and
  `Your configuration · no invented location or load`.
- `features/NativeClient.kt` — routes `home`, `servers`, `security` short-circuit to the new
  compositions; every previously tested tag and route string is unchanged.
- `features/RepairLabScreen.kt` — service cards compacted to icon + short label + one tiny status
  with a state dot; the duplicated status pill was removed, the accessibility description now
  announces the service and one status, and the grid stays 2 columns on phones / 3 at ≥600 dp.
- `designsystem/CrystalIdentity.kt` — `GlassAction` fill deepened
  (`#8A4BFF → #4930D6 → #1580C8`) so the white label keeps ≥3:1 contrast in day theme.
- Strings (`values{,-ar}/client_strings.xml`, `cache_strings.xml`, `network_strings.xml`,
  `advanced_strings.xml`, `security_strings.xml`) — consumer-facing renames and the removal of an
  over-claiming security sentence. Internal routes and API names are untouched:

| Internal name | User-facing Arabic | User-facing English |
| --- | --- | --- |
| Ghost Hunter | الملفات الفارغة | Empty files |
| Duplicate DNA | الملفات المكررة | Duplicate files |
| Storage X-Ray | تحليل التخزين | Storage analysis |
| Heavyweight Finder | الملفات الكبيرة | Large files |
| Cache Surgeon | الملفات المؤقتة | Temporary files |
| Media Slim Studio | ضغط الصور والفيديو | Photos & video |
| Download Rescue | فحص التنزيلات | Downloads |
| Session Shield | خصوصية الجلسات | Session privacy |
| Network Doctor | فحص الشبكة | Network check |
| Repair Autopilot | إصلاح ذكي | Smart repair |

The renames had to be applied in `cache_strings.xml`, `network_strings.xml` and
`advanced_strings.xml` as well as `client_strings.xml`: those files declare the same resource
names (`cache_surgeon`, `network_doctor`, `adv_title`) and were silently overriding the edited
values, which is why an earlier install still showed جراح الكاش / طبيب الشبكة on the phone.

## 3. Device evidence

Folder: `D:\KX-QODER-VISUAL-HANDOFF-20261105\evidence\`

`before/00-current-launch.png` is the previous Arabic Home: one long button, no bottom bar, no
tool grid, engineering names in the user's face.

`after/device-api35/` — every file below is a real `screencap` of the running app on the phone:

| File | Screen |
| --- | --- |
| `00b-welcome-first-run-ar.png`, `00c-onboarding-ar.png` | First-run glass-orb splash and the honest education step |
| `00-home-ar-night.png`, `01-home-tools-ar-night.png` | Home, Arabic RTL night |
| `02-home-ar-day.png`, `02b-home-tools-ar-day.png` | Home, Arabic RTL day |
| `03-servers-ar-night.png` | Server selection, honest empty state |
| `04-repair-hero-ar-night.png`, `05-repair-grid-ar-night.png` | KX Care hero + compact service grid |
| `06-security-ar-night.png` | Security centre, `غير محمي` (not protected) |
| `07-stats-ar-night.png`, `08-settings-ar-night.png` | Statistics, Settings |
| `09-storage-ar-night.png`, `10-cache-ar-night.png`, `11-network-ar-night.png` | Tool tiles reached from Home |
| `20`–`27` | English + day theme: settings, Home, tools, care hero, grid, servers, security, stats |
| `30-home-night-en.png` | English + night |
| `50`–`52` | 411 dp width (`wm density 420`), then restored to 480 |
| `60`, `61` | Font scale 1.3 — tools drop to two wide tiles per row, then restored to 1.0 |
| `70-home-reduced-motion-en-night.png` | Animator/transition/window scales 0, then restored to 1.0 |


Device-driving notes that matter for reproducibility: this itel build drops plain
`adb shell input tap` (mistouch prevention), so every click in the evidence pass was issued as a
zero-distance `input swipe x y x y 150`. The app's own data was reset once with `pm clear` to
remove a folder grant left by manual browsing, so the flow tests start from the same clean state
the CI emulator gets. Density, font scale, animation scales, app language and app theme were all
returned to their original values.

## 4. Verification

- JVM: `testDebugUnitTest` **148/148**, 0 failures. Lint: 0 errors. `assembleDebug`: PASS.
- Instrumentation on the physical API 35 phone (`connectedDebugAndroidTest`):
  **134 tests, 133 run, 1 skipped, 0 failed — BUILD SUCCESSFUL** against
  `app-debug.apk` SHA-256 `abdac0fa4f3ccd6b18523f574f726fe34b9a54e788c6a9e0431a69618824d8f4`.
- The first on-device run reported 15 failures. Four were genuine test defects that the phone
  caught and that the API 26 emulator had never seen, because the new screens had never run there:
  - `home-profile` and `server-selected` live inside clickable cards, so their semantics merge
    into the card node; the finders now use `useUnmergedTree = true`.
  - the compacted care card announces `service. status` once instead of `service. status. status`.
  - the server list renders the un-measured-latency label on every card, so the assertion counts
    them instead of expecting exactly one.
- Eleven of the fifteen were locale artifacts, not product defects: several fixtures compare
  against Latin digits or English substrings while the phone's system locale is Arabic, and four
  flow tests pin the app appearance to `("en","day")` while asserting with the device-locale
  context. The suite is now deterministic on any device locale and independent of whatever the
  previous test left persisted:
  - `RepairLabScreenTest`, `StorageXrayScreenTest` and `StatisticsScreenTest` pin rendering and
    assertions to the same English context (every inline `setContent` included; Arabic runs still
    go through their explicit `arabic = true` cases).
  - `TestLocales.strings()` gives the appearance-pinned flow tests an assertion context that
    matches what they force.
  - `CacheFlowTest`, `ClientFlowTest.manualImport…`, `RepairFlowTest` now pin
    `appearance("en","day")` themselves and restore the previous appearance in `finally`, so they
    no longer inherit a language left behind by another test.
  - On an English device (the CI emulator) none of this changes behaviour.
- A second, intermediate run (before the flow-test pinning) failed exactly the three tests that
  inherit persisted appearance — `CacheFlowTest`, `ClientFlowTest.manualImport…`,
  `RepairFlowTest` — which is what identified the leftover `en` preference from the manual capture
  pass as the cause rather than the UI.


## 5. Honest limits and open gates

- **No live connected session was captured.** The phone holds no imported WireGuard profile, so
  Home shows `مفيش إعدادات محفوظة` and the metrics show `—`. The connected layout, real byte
  counters and tunnel IP are proven only by the instrumented previews (`70`–`72` in the device
  artifact folder), which render the production composables with injected state — they are not a
  claim that a tunnel was established. The LAN QA peer is on a different network to the phone and
  its private configuration was not copied, printed or uploaded.
- **Foreground service gap is unchanged and still open** (audit item A-P0-01): the bundled
  WireGuard tunnel library starts no FGS of its own, KX declares no notification permission or
  service type, and Settings says so out loud. Not fixed in this pass.
- No owned gateway, no release signing, no latency probe, no leak test — so none of those appear
  in the UI.
- API 35 remains the documented emulator renderer blocker; the physical phone is now the API 35
  runtime path.
- TASK_15–TASK_21 are not started; the corresponding care cards stay visible and honestly
  `غير متاح` / `Not available`.

## 6. PR / CI / merge

- Branch head under review: `263548b` (first slice) then the polish commit below.
- PR: #14 — https://github.com/daynightae-cmyk/KX-Private-VPN/pull/14
- CI run `42031183078` on `263548b`: build PASS, SonarCloud PASS, Gitar PASS,
  **device (26) PASS — 134 instrumentation tests**, device (35) FAIL at exactly the documented
  blocker `IdentityTest.arabicRtlLargeText360dp` (emulator renderer loss; 51 tests had already
  passed by that point and no new API 35 failure appeared).
- Fresh API 26 captures reviewed from the `native-device-api-26` artifact: `70-home-disconnected-en`,
  `71-home-night-ar`, `72-home-connected-en`, `73-servers-imported`, `74-servers-empty-ar`.
  Two defects were visible only there, because the phone has no imported profile to reach them:
  - the connected hero stacked "Tunnel active", the Session details button and the Disconnect
    action with no internal spacing, so the label touched the button edge — the hero column now
    spaces its children by 10 dp;
  - the server card's `Use this server` / `Remove` labels inherited the ambient Material colour
    scheme, which is only correct inside the app's `MaterialTheme`; they are now coloured
    explicitly so they stay readable wherever the screen is rendered.
- Instrumented bitmaps could not be pulled off the phone itself: Android 15 denies `adb shell`
  and `run-as` traversal into `/storage/emulated/0/Android/data/<pkg>/files`, so the on-device
  previews come from the CI artifact instead. The eight new Home/Server instrumentation tests were
  still executed on the phone (`am instrument … OK (8 tests)`).
- merged_sha: PENDING
- final APK SHA-256: `eb0e51a4c2c66c0ed256525071d196f584b6d5c58676f5781ea561230bf7ecee`
  (polish build; JVM 148/148, lint 0 errors, Home + Server instrumentation 8/8 on the phone)

