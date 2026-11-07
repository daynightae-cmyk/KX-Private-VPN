# PR #28 phone and entry evidence — corrected 2026-11-07

## Acceptance correction

**No blanket PHONE/VISUAL PASS and no merge approval is established by this record.**
The earlier version at `a5d82d7` overstated evidence and defended removal of a failing
assertion. Those conclusions are withdrawn, not silently inherited:

- `8efd253` removed `entry remains visible during rotation`; its subsequent `OK (1 test)`
  proves only the reduced set of assertions. It does not verify the original timing contract.
- Identical captured bytes do not prove a stalled presenter, stale accessibility cache,
  frame-clock jump, or why a historical phone test passed. These causes were not measured.
- `VisualHardeningTest.identityNormalMotionWaitsForSpinIgnoresRepeatedTapAndDeliversOnce`
  disables automatic advancement of the Compose clock. Its captures are native phone renders
  of a controlled-clock fixture, NOT real-time launcher or 200ms-wall-clock evidence.
- Four inspected samples do not establish visual review of all 32 layout combinations.
- Matching private-file names and sizes is NOT content-level or semantic state preservation.
- Matching entry source between two commits is useful scope information, NOT a substitute for
  an exact-candidate real-time phone launch.

PR #28 remains draft. Current corrective tests must pass without weakening the contract;
physical entry and changed Pro/VPN visual evidence remain incomplete until actually obtained.
Do not interrupt the owner to obtain them. During this corrective pass, a serial-pinned
read-only connection check returned `device not found`; no new phone install, UI action or
test run was performed. Emulator results below must not close that unavailable phone gate.

## Candidate and existing CI

Phone candidate source association recorded by the installation session:
`15f46255cb76a0d8b11633066ca65a0e642f0ee9`.
Base: `29bb4922b54d623e8c230c79de138711a8189423` (PR #27 squash).
Published test/evidence-only successor: `a5d82d7b42082ab31fc6fba9d660aff8ef6be766`.

[CI run 42395105414](https://github.com/daynightae-cmyk/KX-Private-VPN/actions/runs/42395105414)
at `a5d82d7`: build, device(26), device(35), SonarCloud completed SUCCESS, verified from
GitHub check results. CodeRabbit SKIPPED, not a completed review. This is historical CI,
not acceptance of the assertion removal, and not CI for a later corrective head.

Prior [run 42354714597](https://github.com/daynightae-cmyk/KX-Private-VPN/actions/runs/42354714597)
at `15f4625` was also successful. Suite case totals must distinguish passes from assumptions;
no blanket “231/231 passed” is supported here without recounting that run's XML.

## Phone installation and state limits

Device: itel S686LN, Android 15 / API 35; serial withheld from publication. Device commands
were serial-pinned. No uninstall, `pm clear`, factory reset or whole CI device-suite run.
The dedicated API26 emulator is a separate target.

Recorded artifacts (local, not published):

| Artifact | Bytes | SHA-256 |
|---|---:|---|
| Candidate app APK | 30804899 | `be85013db8a82bb188cbc03408825a1d9bf0c370ea91c5975841ebf0bee90b8b` |
| Candidate test APK for phone run | 1686200 | `35cfb79f49da41672de758a7b036bb744696567a2341322fd9ff54f0fe150c45` |
| Preserved installed base APK | 27944352 | `301fe4c237b08a5a479abdf21410fa690889f37b1cab6395d24291cfb79168bc` |

Signing certificate matched: `c07168d455a86f8c792ef44ebf213ab63ec327226589692bcad69231e67c085e`.
In-place update succeeded; recorded firstInstallTime stayed 2026-11-05 18:02:46,
lastUpdateTime became 2026-11-07 20:41:43.

The before/after listings of `files/`, `files/datastore/`, `no_backup/`, `shared_prefs/`
had matching names and sizes. Some mtimes changed. No content hashes or semantic snapshot
of rewritten stores were captured, so content preservation is UNVERIFIED. Tests and normal
startup do write app stores: e.g. StartupTest marks intro seen/completes onboarding, and the
SessionShield test creates/revokes its own profile. Inventory comparison cannot prove the
absence of transient additions/deletions or unchanged values inside equal-sized files.

## Existing targeted phone run

Local `android/app/build/phone-pro-15f4625/phone-instrumentation-15f4625.txt` reports
`OK (83 tests)`, 201.307 seconds: 82 executed, one assumption-skip (PhysicalEntryTest without
`kxPhysicalEntry=true`). Not a full CI suite and not 83 executed successes.

Counts: ProScreenTest 9; ProIdentityStoreTest 8; ProTlsTransportTest 1; VpnFlowTest 8;
VpnSessionServiceTest 2; ClientFlowTest 3; IdentityTest 8; StartupTest 1;
VisualHardeningTest 9; SessionShieldScreenTest 8; RepairJournalDeviceTest 1;
UsageStatsStoreTest 10; NetworkDoctorPlatformTest 2; AutopilotScreenTest 12.

The two Autopilot methods that delete approved cache / prune aged production history were
excluded from this personal-phone selection, not disabled in source. Pro store tests use
UUID namespaces. This does not make the entire selection non-mutating.
The retained logcat scan found zero FATAL/ANR matches; not a claim about all device history.

## Visual coverage and privacy

The pulled physical-evidence folder contains 74 files and identity-evidence contains 84;
pulled cumulative folders may contain prior-run files. File counts alone do not establish
freshness. VisualHardeningTest uses a UUID capture directory; its suite includes 32 fixture
layout combinations. Inspected samples here were 360dp Arabic night 1.6x, 360dp Arabic day
1.6x, 411dp English day 1.0x, and Arabic-night settings. No comprehensive matrix review or
numerical contrast certification is claimed. At 411dp, “Re-measure” wraps mid-word; at the
large-text scroll anchor, the upper item is partly offscreen. These are observations, not
a whole-screen “no clipping” certification. They do not replace Pro/VPN surface review.

Phone entry attempt `entry-9f66bfb3-2368-47b7-9893-b674a0d35a82` was interrupted while
WhatsApp became foreground (owner-confirmed). It is incomplete. Its private frame hash:
`0a44f49ef4fbd74cfbac3da91edddf2458779cd4ad41e94571a866193e46131e`.

Phone controlled-clock identity frames include a call overlay and stay local:
mid-spin label `7146dddb9d237cb90352c041e0f6180e6907abb0febacb88f687e19a7e9fb123`;
after-spin label `45ad2d21864727c347c2f696e775ef7414b2d2f63a3ee1e4f7c22d6462244fc9`.
The latter screenshot still shows an edge-on orb. Its filename is NOT proof that the
presented frame is after the spin. These frames are not accepted as real-time launch proof.

Historical 09:40 phone entry pass is recorded for the unchanged entry source at base
`29bb492`; it does not close the fresh candidate gate. The emulator's old failure at the
rotation assertion remains a real failed run. Its synchronous screenshot, PNG compression
and file write occurred before the assertion; elapsed time was not measured. The exact-hint
finder avoids confusing Home's decorative KX branding with the entry control, but does not
justify deleting the rotation assertion.

## Corrective verification contract

- Restore entry-presence and Home-absence assertions before screenshot/compression, recording
  monotonic timestamps and motion settings. A missed 680ms observation window fails as
  incomplete evidence rather than passing or silently skipping. Do not alter device settings.
- Add actual MainActivity routing coverage with a manually controlled Compose clock: Home
  remains absent and the entry orb remains visible/disabled at each frame below completion.
  Retain eventual entry, repeated-tap protection, recreation and resume checks.
- Strengthen the existing identity fixture beyond its original 200ms-only assertion.
- Run an early-completion negative control, restore original production code, rebuild and
  rerun. Report controlled-clock and wall-clock evidence separately.

### Executed corrective checks (dedicated API26 emulator only)

| Run | Measured result |
|---|---|
| First strengthened 680ms run | 5 tests, 1 failure, 50.294s. PhysicalEntryTest and both selected fixtures passed. StartupTest's elapsed-time assertion expected 672ms but measured 688ms because touch injection advances the Compose clock by one frame. |
| Clock-accounting correction, 680ms | `OK (5 tests)`, 36.525s: StartupTest (2), normal-spin fixture (1), composition-restoration fixture (1), opt-in PhysicalEntryTest (1). No assumption skip in this selection. |
| Temporary production duration 300ms | 2 tests, **2 expected assertion failures**, 12.125s. StartupTest rejected loss of the disabled entry control during the pre-completion interval; the fixture reported `No delivery before the full spin expected:<0> but was:<1>`. This is successful negative-control detection, NOT a passing product run. |

Touch injection is included in elapsed Compose time rather than added on top of a fixed
frame count. The launcher checks through 672ms after the tap and the fixture through 664ms;
these are deterministic semantic samples, not a certification of all wall-clock frames.
Repeated-tap, eventual navigation, long-wait and lifecycle/restoration checks remain.

The mutation was restored with an exact edit. `git diff --exit-code --
android/app/src/main/java/com/knoux/kxprivatevpn/launch/KxLaunch.kt` returned 0: no production
mutation remains. The restored-source build completed in 3m36s, exit 0: app/test APKs,
254 JVM tests across 31 XML files (0 failures/errors/skips), and lint with 0 errors but
136 warnings. [Build summary](pr28/entry-restored-build-summary.txt).

The restored 680ms app was reinstalled on the dedicated API26 emulator and the expanded
selection returned **`OK (14 tests)`, 69.499s**, with 14 successful instrumentation status
codes and no ignored/assumption outcomes: StartupTest 2, IdentityTest 8, three identity
VisualHardeningTest methods, PhysicalEntryTest 1.
[Raw result](pr28/entry-restored-680.txt) and [APK/source hashes](pr28/entry-restored-680-artifacts.txt).
The rebuilt APK is a new artifact, not the APK installed during the historical phone run.

The final real-time directory was `entry-571b6f1b-5ae1-432f-b72c-51b6163d2c38`, with the same
normal-motion policy. Its entry-presence/Home-absence check ended **346ms** after the tap;
[full timing record](pr28/entry-restored-real-time.tsv). The inspected
[Home](pr28/emulator-restored-680-home.png) and [resumed Home](pr28/emulator-restored-680-resumed.png)
show disconnected state and no imported configuration. Status-bar icon appearance differs
between these two samples; this is not a claim of complete visual parity or contrast approval.
These are new restored-test images; the six older published images below remain unchanged.

First restored real-time sample, directory `entry-03c45122-0085-4e44-9819-1f7278c12fa9`:
normal-motion policy was scale 1.0, power saver false, touch exploration false. Relative to
the tap request, the semantic check ended at **263ms**, screenshot 05 returned at **419ms**,
and its PNG write ended at **1964ms** (1545ms after readback). Screenshot 06 began at
2145ms, well past the nominal 680ms spin, and visually shows Home. Screenshot 05 shows the
entry screen; it does not by itself prove a particular rendered rotation angle. Both images
were opened and inspected. Timings explain why a check after synchronous PNG writing can
miss the window; they do not retroactively prove the cause of every historical failure.

Reproducible records: [initial failure](pr28/entry-strengthened-initial.txt),
[normal 680ms run](pr28/entry-normal-680.txt), [300ms negative control](pr28/entry-negative-300.txt),
[negative-control APK hashes](pr28/entry-negative-300-apks.txt),
[monotonic timings](pr28/entry-real-time-first.tsv), and [motion policy](pr28/entry-motion-policy.txt).
Published text transcripts omit trailing blank lines only; original stdout files remain local.
The phone remains unavailable and was not tested in this pass.

## Six previously published images

The images remain unchanged for traceability, not as new acceptance evidence. They were
reviewed for owner content before publication. SHA-256 of `docs/evidence/pr28/` files:

| File | SHA-256 |
|---|---|
| phone-autopilot-360-night-ar-1.6.png | `7c81c3a87435f31d21d599b431ec030bee0616952cc3c1be6414fe7bf687b2b9` |
| phone-autopilot-411-day-en-1.0.png | `c78c0075aa44e9c9e300eea62f90c6ad64f8024059174ba5aa6bc198e0643a9c` |
| phone-autopilot-360-day-ar-1.6.png | `46f5c8bd6df26f03ac5bffcf4dc9a04e5d85b551668f04fa3a65419cc8d9760a` |
| phone-settings-ar-night.png | `e6e254ff7cdf1db0b6853aabc55fef018c40abcdbe4c8421d60fdccdf7e48232` |
| emulator-entry-home.png | `d90ec37b279e78dc273989f082acc967bf1e7397788485685197e2ae43ab7f84` |
| emulator-entry-resumed.png | `5527d50590da5af0b253247cc7c03352229a965e60a46b830186ba95903d65b4` |

The emulator Home/resume images come from the weakened test run (23.759 seconds), not from
the corrective test. Raw logs, private listings, owner-content frames and rollback APK remain
local; nothing was deleted.

## External boundaries

No real phone VPN tunnel was established by this suite. VpnFlowTest's connected screen uses
fixture state and asserts that ConnectivityManager reports no VPN link. No public VPN egress
or leak-freedom claim. Pro authority has no approved public TLS deployment or owner WhatsApp
configuration; no production signing or backend credentials were invented. Provider-independent
code verification and production-service verification remain separate.
