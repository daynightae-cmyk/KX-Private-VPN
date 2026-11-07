# KX hardening — evidence corrections and privacy review

Baseline: `1149a63e95580e4a6ee0bafe9f4bd848f55a3a79`. This is a bounded audit and continuation
record, not a production release declaration. The original phone report's blanket visual/data
preservation claims are withdrawn. Source changes require their own exact-SHA gates.

## Corrections to previously published evidence

- `49-autopilot-plan-night-ar-large.png` has black filenames/history heading on violet glass.
  The earlier readable-contrast PASS was wrong. Fresh matched physical captures must be inspected.
- `04-welcome.png` is a callback-free `IdentityExperience` fixture with Continue controls, not
  the production entry configuration. Static fixture frames cannot prove real-time orb rotation
  or absence of a system-splash square flash.
- `11-vpn-session-connected.png` uses synthetic profile, backend version and counters from
  `VpnFlowTest`. The endpoint/DNS are RFC 5737 documentation values; the private tunnel address
  is a fixture too. The actual image and test source were checked to resolve a false-positive
  OCR reading of a routable endpoint. The Android link query is real; the displayed session
  does not prove a WireGuard handshake or protection.
- The committed raw log has 17,945 lines, not 104,104. A log scan covers only that retained log.
- Historical runner output reports 48 tests passed plus a separate HTTPS probe (HTTP 200,
  748 ms). Some old API 35 identity assertions were bypassed by an early return without a
  JUnit skip. Route walks prove navigation, not complete service behavior.
- Unchanged first-install time does not prove every private store survived. Previously selected
  tests mutate real preferences/history/profiles/cache. Aged-history tests can prune records
  unrelated to their fixture; journal appends can evict older entries at the retention limit.
  The earlier claim that only test-owned data was touched is not established.

## Bounded public-artifact assessment

Scope: 24 artifacts in the evidence publication (18 PNGs, five textual artifacts and rollback
APK). The read-only review included complete text, APK entries/DEX/resources, screenshot OCR,
and direct image/source verification of the disputed VPN fixture. OCR and static scans do not
prove absence of every encoded, encrypted, dynamic or native secret. Other history/releases
are outside this bounded review.

| Finding | Classification |
|---|---|
| Device serial in prior report | Confirmed device-identification exposure; not a credential |
| Raw debug log | Timestamps, process/thread/task IDs, paths, input/focus and notification lifecycle metadata unsuitable for minimal public evidence |
| Debug APK | Public debug distribution/attack-surface exposure, not release signing |
| Binder/window/activity token handles | Not bearer authentication secrets |
| Android Debug certificate / Go h1 strings | Public certificate metadata / dependency checksums, not private keys |
| WireGuard keys, passwords, bearer/JWT or provider credentials | No confirmed leak in the reviewed artifacts; not an exhaustive absence guarantee |
| Personal notification/message bodies | None confirmed; OCR limitations remain |
| Suspected routable endpoint | False-positive OCR; documentation-only fixture confirmed visually and in source |

**Public-artifact safety verdict: remediation required.** This forward report omits the serial.
The previous report, raw log and debug APK remain retrievable in public history. No exposure is
claimed resolved merely because a file is redacted in a later commit.

Original evidence was preserved outside Git using Windows CurrentUser DPAPI. The 61,419,520-byte
plaintext archive SHA-256 is `662bbc5abc4f922d8a57fffbe82dbf740ded3a11db03ed6dbed1fc206bb962cf`;
encrypt/decrypt equality was verified. The installed pre-fix APK and 40 regular app-private files
were separately preserved and verified. This is not an Android Keystore backup or a portable
restore guarantee. No originals were deleted and no new personal raw logs were uploaded.

Proposed containment: retain private originals; remove raw public log/debug rollback APK from
the current tree in an explicitly reviewed cleanup; publish minimal sanitized results only.
History rewriting, remote purge or force-push requires separate approval. No such operation was
performed. Rotate credentials if an actual credential is identified, not for token/checksum
false positives.

## Source findings and verification boundary

- Visual fix: component-owned Day/Night foreground, themed Autopilot controls, single labelled
  checkbox targets, wrapping mixed filenames, large-font action stacking and 48dp back target.
  Real source and synthetic regression tests exist; actual physical contrast/layout remains open.
- Entry: explicit gesture, lifecycle-resumed 680ms spin, saveable delivery latch, Home routing,
  no duplicate Continue alongside the active emblem. Original artwork is unchanged.
- History repair: frozen approved records; exact multiplicity-aware removal against the latest
  store; serialized journal/DataStore commits; readback/committed-result verification; rejection
  of corrupt mutation input; counts checkpointed after successful writes rather than before.
  Regression completion and exact-SHA CI must be recorded separately.
- Image compression is real WebP encoding to a new document, sampled decode/size verification;
  originals are read-only. Video compression remains explicitly unimplemented. Cleanup-failure
  reporting and stale source-size limitations are not claimed repaired by the visual work.
- VPN uses the actual WireGuard backend. Service UP is not a handshake. The owned LAN config's
  key pairs/limited routes validated, but live Windows WireGuard inspection was access-denied.
  Handshake state is UNKNOWN, not zero. No public VPN or DNS/IPv6/egress protection is proved.
- Pro has no configured server or owner WhatsApp contact. No master code, invented endpoint or
  unrelated backend deployment is permitted. An internal fail-closed policy is not activation.
- Profile-stop failure handling and every media/Shield lifecycle path have not received a full
  production verification. These limitations must not be hidden by green unit tests.

## Local hardening verification

The candidate source was built from the isolated hardening worktree on the baseline above with
JDK 21, running `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest`.
After focused review corrections, the final run completed successfully in 1m47s. XML reports contain 26 JVM suites / 213 tests,
zero failures, errors or skips. Lint completed with zero errors and 134 warnings; this is not a
warning-free verdict. Both application and instrumentation APKs compiled. Instrumentation
compilation does not establish native execution or visual acceptance.

- Candidate debug APK: 27,784,583 bytes, SHA-256
  `82ce094069d67e9d4f7acfad3fe93ccfe938f9cf57a784a3385a3f2a020b0680`.
- Candidate instrumentation APK: 1,652,079 bytes, SHA-256
  `dc764888c3bd061cfe14db5f27c6b6f914a4d73c0eff965845ec0a42e8a66711`.
- APK Signature Scheme v2 verification succeeded. The candidate Android Debug certificate
  SHA-256 is `c07168d455a86f8c792ef44ebf213ab63ec327226589692bcad69231e67c085e`, matching the
  separately preserved installed APK. The candidate has not been installed on the phone.
- No original artwork, private backup, raw phone log or APK is included in this hardening patch.
- Focused review corrected test sandbox ownership: instrumentation executes under the target UID.
  Tests now use injected UUID-owned files/directories under that UID, never production store paths.
  Capture roots must be non-null. No production DataStore is cleared for isolation.
- Journal mutations reject unpaired Unicode surrogates before any lossy UTF-8 rewrite. JVM
  regression passed; the native byte-preservation regression compiled but awaits execution.
- Real coroutine Job cancellation during and between real temporary-file DataStore commits
  verifies the shared production mutation boundary: exact committed counts, unapproved survivors
  and checkpoint preservation, with a cancelled/partial result. This does not exercise the entire
  Android ViewModel lifecycle or claim a native AtomicFile cancellation test.

Exact source commit and its remote CI results must be recorded after publication. Baseline CI
or these local results must not be inherited by a later source revision.

## Device execution boundary

The Mobile Use inventory and doctor found no live Canvas surface/target/session. No Canvas-open
host action was exposed. Therefore no fresh Canvas interaction, native-test receipt or visual
capture is claimed. Read-only serial-bound ADB backup is not a substitute for those gates. Do
not bypass the missing advertised device capability with raw UI/runner commands.

Safe future execution: bind the owner-authorized physical Canvas target; verify candidate and
installed certificates/hashes; preserve/compare stores; run reviewed non-destructive method
selectors only. Opt-in `VisualHardeningTest` captures synthetic state in unique test-owned paths.
`PhysicalEntryTest` runs the real Activity but not a true process-cold launch; it requires private
capture review and startup-state comparison. Never run the entire CI device suite on the phone.

## Independent release gates

| Gate | Current verdict |
|---|---|
| MAIN INTEGRATED | New hardening changes not yet merged |
| CI VERIFIED | Baseline CI only; new exact-SHA API 26/API 35 and Sonar required |
| PHONE VERIFIED | Fresh hardening build not verified; historical coverage is limited |
| VISUAL VERIFIED | NO — old PASS withdrawn; new physical comparison missing |
| PRIVACY REVIEWED | Bounded review complete; public exposure remains |
| VPN NETWORK VERIFIED | NO — live state unavailable, no protected egress evidence |
| PRO VERIFIED | NO — no authority/contact or end-to-end activation |
| RELEASE SIGNED | NO — Android Debug only |
| PRODUCTION RELEASED | NO |
