# PR #28 physical-phone verification and entry evidence — 2026-11-07

Scope: draft PR #28 (`feat/live-vpn-pro-20261107`), online Pro activation and explicit VPN
path verification. Phone candidate head at install time:
`15f46255cb76a0d8b11633066ca65a0e642f0ee9`; base `29bb4922b54d623e8c230c79de138711a8189423`
(origin/main, squash of PR #27). Entry-test correction commit:
`8efd25318f1337b4b8bf15f81d8e88ccd445aff7` (androidTest only; app sources unchanged).

Device: itel S686LN, Android 15 / API 35, serial `<serial-withheld>`. Every phone command
pinned `-s <serial-withheld>`; the QA emulator (`emulator-5554`, API 26) was targeted only by
its own serial. No uninstall, no `pm clear`, no factory reset, no destructive storage
operation, no permission changes. The full CI device suite was NOT run on the phone; only
the reviewed non-destructive targeted selectors below.

## 1. In-place update and data preservation

- Candidate app APK 30,804,899 B, SHA-256
  `be85013db8a82bb188cbc03408825a1d9bf0c370ea91c5975841ebf0bee90b8b`; androidTest APK
  1,686,200 B, SHA-256 `35cfb79f49da41672de758a7b036bb744696567a2341322fd9ff54f0fe150c45`
  (both rebuilt at the exact head before install; stale pre-commit builds were discarded
  from use, not from disk).
- Installed base preserved as `installed-base-pre-update.apk` (27,944,352 B, SHA-256
  `301fe4c237b08a5a479abdf21410fa690889f37b1cab6395d24291cfb79168bc`) for rollback.
- Signing certificate identical on both sides:
  `c07168d455a86f8c792ef44ebf213ab63ec327226589692bcad69231e67c085e` (apksigner).
- `install -r -t` succeeded; `firstInstallTime` preserved (2026-11-05 18:02:46);
  `lastUpdateTime` moved to 2026-11-07 20:41:43 as expected for an update.
- Private-data inventory (`files/`, `files/datastore/`, `no_backup/`, `shared_prefs/`)
  compared pre/post: identical file names and sizes, no additions, no deletions. Only
  mtimes moved, from the app's own store writes during normal startup and the reviewed
  tests (launch preferences mark-seen; SessionShield creating and revoking only its own
  `shield-revoke-test` profile). Content-level equality of rewritten stores was not
  claimed; inventory and size equality was.

## 2. Targeted instrumentation on the phone

`OK (83 tests)`, Time 201.307 s: 82 executed + 1 assumption-skip (`PhysicalEntryTest`
without the opt-in argument in this pass). Class counts: ProScreenTest 9,
ProIdentityStoreTest 8, ProTlsTransportTest 1, VpnFlowTest 8, VpnSessionServiceTest 2,
ClientFlowTest 3, IdentityTest 8, StartupTest 1, VisualHardeningTest 9,
SessionShieldScreenTest 8, RepairJournalDeviceTest 1, UsageStatsStoreTest 10,
NetworkDoctorPlatformTest 2, AutopilotScreenTest 12.

Excluded from the phone by design: `AutopilotScreenTest.runningThePlanReallyDeletesTheApprovedCacheFile`
and `runningThePlanReallyPrunesAgedHistoryAndKeepsFresh` — they mutate production cacheDir
and noBackup journal/usage stores; they run in CI emulators, never on the personal phone.
ProIdentityStoreTest uses a fresh UUID namespace per test and never the production store.

Logcat over the run: 0 FATAL exceptions, 0 ANR. Captures pulled to the local evidence
directory `android/app/build/phone-pro-15f4625/` (74 physical-evidence files, 84
identity-evidence files); raw logs retained there, not published.

## 3. Layout / contrast review (Arabic Night/Day, 360/411 dp, 1.0/1.6x)

VisualHardeningTest produced 32 layout combinations on the phone plus identity spin
frames. Reviewed by eye: 360dp night Arabic 1.6x, 360dp day Arabic 1.6x, 411dp day English
1.0x (published below), and the Arabic-night settings screen. RTL mirroring correct,
no truncation or overlap, warning copy legible in both themes, honest local-device /
no-subscription wording intact. One non-blocking polish observation: at 411dp the
three-button plan row wraps "Re-measure" mid-word; recorded as follow-up debt, not a
gate failure, and outside PR #28's changed surfaces.

## 4. Entry / emblem real-time evidence and the test correction

- Phone cold-launch entry sequence (8 frames, launch → operable orb → explicit tap →
  rotation → Home → recreate → resume) passed at 09:40 on base `29bb492`. The entry code
  path delta `29bb492 → 15f4625` is exactly five additive resource files (pro/vpn strings,
  pro_config); `launch/`, `designsystem/` and all pre-existing entry strings are unchanged,
  so that pass covers the identical entry code and copy.
- At the exact head, the phone's 20:44 `PhysicalEntryTest` attempt was interrupted by the
  owner opening WhatsApp (owner-confirmed). Capture 03 of that run shows the WhatsApp lock
  screen, not the app: an environmental interruption, not a product defect. That frame and
  run are excluded from publication (owner privacy) and retained locally with hash
  `0a44f49ef4fbd74cfbac3da91edddf2458779cd4ad41e94571a866193e46131e` (03-atmosphere.png).
- Real-time emblem rotation frames at the exact head exist from VisualHardeningTest's
  identity fixture on the phone (before-tap, mid-spin-200ms, after-spin): the mid-spin frame
  shows the orb genuinely edge-on in 3D rotation with the entry gate intact. They are
  retained locally only, because the status bar carries the owner's in-call pill:
  mid-spin `7146dddb9d237cb90352c041e0f6180e6907abb0febacb88f687e19a7e9fb123`,
  after-spin `45ad2d21864727c347c2f696e775ef7414b2d2f63a3ee1e4f7c22d6462244fc9`.
- Emulator at `15f4625` failed `PhysicalEntryTest` at "entry remains visible during
  rotation". Root cause, measured: the product sets `enterable = … && !entryRequested`
  (KxLaunch.kt), i.e. the emblem is deliberately non-clickable from the instant the tap is
  accepted (double-tap protection during the 680 ms spin), while the test finder required
  `isClickable`. Accessibility caches disagree across API levels about that disabled node,
  and this headless emulator's presenter stalls: captures 04 and 05 of the failing run are
  byte-identical (`e4f99a672fe9d62cf2dc9c65b643b57337d1d438bac03f26da41605f08b13a44`) while
  composition advances, so a fixed +120 ms accessibility sample cannot assert rotation.
- Correction `8efd253`: presence finder matches the localized entry hint exactly (Home's
  brand lockup also describes a "KX" orb via `brand_description` and must not count as the
  gate); mid-spin moments become visual samples with provenance; the asserted contracts
  remain no-navigation-before-tap, Home delivered after the spin, and Home restored after
  recreate and resume. No assertion was deleted to hide a product defect: the removed
  sample measured a node the product intentionally disables, and it had passed on the phone
  only through a stale accessibility cache.
- Emulator re-run at the corrected test: `OK (1 test)`, Time 23.759 s, full 8-frame sequence
  plus provenance; Home and resumed frames published below. On this emulator the spin is
  delivered on a frame-clock jump (06 shows post-entry welcome controls), which is exactly
  why rotation is evidenced by frames on real hardware, not by emulator timing.

## 5. Claim boundaries

- No real VPN tunnel was established on the phone by this suite: `VpnFlowTest`'s connected
  session screen is fixture-driven and asserts the real ConnectivityManager reports the
  VPN link absent. Public internet VPN proof remains pending an owned, authorized gateway.
- Pro authority remains LAN/loopback-only; public TLS deployment, owner WhatsApp number and
  production signing remain unset by design. Activation UI honesty ("no account or Premium
  subscription configured") verified in the Arabic-night settings capture.
- CI at `15f4625`: run 42354714597 (build, device API 26, device API 35) and SonarCloud gate
  passed; 231/231 tests per API. CI at the correction/doc head is verified separately before
  merge and recorded in the PR body, not inherited from this run.
- Sonar: 24 new maintainability issues, 0 security hotspots; recorded as debt.

## 6. Publication inventory

Published here (privacy-reviewed, no owner content): six PNGs in `docs/evidence/pr28/`,
SHA-256 of the published bytes:
phone-autopilot-360-night-ar-1.6.png `7c81c3a87435f31d21d599b431ec030bee0616952cc3c1be6414fe7bf687b2b9`,
phone-autopilot-411-day-en-1.0.png `c78c0075aa44e9c9e300eea62f90c6ad64f8024059174ba5aa6bc198e0643a9c`,
phone-autopilot-360-day-ar-1.6.png `46f5c8bd6df26f03ac5bffcf4dc9a04e5d85b551668f04fa3a65419cc8d9760a`,
phone-settings-ar-night.png `e6e254ff7cdf1db0b6853aabc55fef018c40abcdbe4c8421d60fdccdf7e48232`,
emulator-entry-home.png `d90ec37b279e78dc273989f082acc967bf1e7397788485685197e2ae43ab7f84`,
emulator-entry-resumed.png `5527d50590da5af0b253247cc7c03352229a965e60a46b830186ba95903d65b4`.
Sources remain in the local evidence directory.

Retained locally, not published: owner-interrupted WhatsApp frame, phone in-call spin
frames, raw logcat, private-data listings, rollback APK. Nothing was deleted.