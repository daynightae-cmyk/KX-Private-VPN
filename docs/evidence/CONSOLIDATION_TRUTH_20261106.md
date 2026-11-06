# KX Private VPN — consolidation truth table (2026-11-06)

Scope: PR #17 + PR #16 consolidation, PR #18 API 35 CI stabilisation, the API 35 display fix,
the VPN foreground-service gap, physical-device validation, owner-reference comparison, and
the cleanup audit.
This document records only measured results. It does not claim a working public VPN exit,
Pro service, store release, or visual parity that was not observed.

## 1. Canonical state

| Item | Value |
|---|---|
| Repository | `daynightae-cmyk/KX-Private-VPN` |
| Canonical worktree | `K:\KX-Private-VPN` |
| `main` before consolidation | `6b3f2a8620a32449cf22d6836d9e17f37d7c39a3` |
| Verified integration head (PR #17) | `6e575c89d9876c8c73c4a1b6880a967e149ed163` |
| PR #17 merge commit on `main` | `ae8b7b47a1d8a083ca07fe05a4bb8f4ac8273b4b` |
| Source tree equality | `ae8b7b4^{tree}` = `6e575c8^{tree}` = `e3f3da103e1f7d9749da1351e39ed110dd001a19` |
| PR #16 | MERGED via the same integration history (head `d2aa4e0e936993e0cda057964637554874cad0f4` is an ancestor of `main`); its API 35 job had failed on the 240dp emulator display |
| PR #15 | CLOSED as superseded; unique glyph file is byte-identical in `main`, patch preserved |
| PR #18 | MERGED as `fa34b671c5975c1e54f0af4ef40d8d5286efd147` (parents `ae8b7b4` + `d4123d4`); verified by run `42146411681`; contains no production-source change |
| PR #18 tree equality | `fa34b67^{tree}` = `d4123d4^{tree}` = `bea2138c7be1dfee36f6f4710d501d352cf1e5da` |
| PR #19 | MERGED as `1d2e49948aa121c5c4e82a705958a8d313cdeb69` (parents `fa34b67` + `a73d71d`); head `a73d71d` verified by run `42192806914`; post-merge run `42197489233` all green; CI budget + docs only, no app or test source change |
| Stashes | none |
| Unreachable commits | none (two unreachable trees only) |

The installed APK was built from tree `e3f3da1`, i.e. exactly the PR #17 merged source tree.
No re-install was needed after that merge. PR #18 changed only test sources and the CI
verification script, so the installed app APK bytes are unchanged by it.

### Branch disposition (measured with `git rev-list --count origin/main..<branch>`)

| Branch | Tip | Ahead of `origin/main` | Pushed | Disposition |
|---|---|---|---|---|
| `main` | `fa34b67` | 0 | yes | canonical |
| `fix/api35-emulator-stability` | `d4123d4` | 0 | yes | merged via PR #18 |
| `codex/task-00-native-cinematic` | `4879c81` | 2 | yes | superseded by PR #17; patch preserved |
| `feature/kx-touch-to-enter-physical-20261106` | `ae5861e` | 1 | yes | PR #15 closed as superseded; glyph identical; patch preserved |
| `integration/qoder-freebuff-ui` | `d4e4220` | 2 | **no** | superseded; patches preserved locally |
| `feature/kx-orb-sequenced-launch-20261106` | `6e575c8` | 0 | yes | merged via PR #17 |
| `integration/kx-consolidation-20261106` | `d2aa4e0` | 0 | yes | merged via PR #16/#17 |
| all other `codex/*`, `task14-*`, `traycer/*` | — | 0 | mostly yes | contained in `origin/main`; no unique commits |

Every branch ahead of `origin/main` is either merged or preserved as a patch under
`D:\KX-QODER-VISUAL-HANDOFF-20261105\DEVICE-EVIDENCE-20261106\unique-work\` with a
SHA-256 in the cleanup inventory. Nothing is unaccounted for.

## 2. Verification at exact SHAs

### PR #17 head `6e575c8` — CI run `42135553590` (all jobs green)

| Job | Result |
|---|---|
| build | PASS in 3m26s: 150 JVM tests, 0 failures, 0 skipped; `lintDebug` 0 errors / 93 warnings; debug + androidTest APKs |
| device API 26 | PASS in 5m38s: 138 tests, 0 failures, 0 errors, 0 skipped |
| device API 35 | PASS in 8m25s: 138 tests, 0 failures, 0 errors, 1 skipped |

API 35 skip: `StorageXrayDeviceTest.mediaInventoryRespectsGrantedPermissionAndVersionMatrix`
— documented API ≤ 32-only test, reported as SKIPPED, not PASS.

API 35 `device-screenshots/display-contract.txt`:
`width=720 height=1280 density=280 logical_width=411dp`.

### `main` `ae8b7b4` — CI run `42136921527`

| Job | Result |
|---|---|
| build | PASS |
| device API 26 | PASS |
| device API 35 | **FAIL** in 6m1s — emulator infrastructure loss, not a test assertion |

API 35 detail (from the downloaded artifact and job log): the run stopped after 53 of 138
tests (52 reported complete). The recorded failure is `IdentityTest.arabicRtlLargeText360dp`
with an **empty** `<failure>` element, followed by
`Exception thrown during onAfterAll ... device 'emulator-5554' not found`,
`Test run failed to complete. Expected 138 tests, received 52` and
`could not connect to TCP port 5554: Connection refused`. The emulator process disappeared
mid-suite; no assertion text was ever produced. A rerun of the failed job reproduced the
identical loss at the same test. The same tree had passed all 138 tests in PR #17 CI minutes
earlier, so this is emulator instability under `-gpu software`.
The same renderer loss at this test was already recorded earlier in
`docs/evidence/TASK_07.md`: the API35 headless SwiftShader emulator lost its renderer during
`IdentityTest.arabicRtlLargeText360dp` after 21 of 61 tests. It is a recurring hosted-emulator
defect, not a new regression.
Remediation is tracked in PR #18 (host SwiftShader, an explicit `emulator-loss.txt` artifact,
and an `assumeTrue` API35-emulator skip for this single composition after it was proven to pass
on the physical phone). No production source is changed by that fix.

### PR #18 (`fix/api35-emulator-stability`) — measured run history

| Run | Head | build | API 26 | API 35 | Detail |
|---|---|---|---|---|---|
| `42139780055` | `a95fd0b` | PASS | PASS | **FAIL** | Died at `IdentityTest.arabicRtlLargeText360dp` again; host SwiftShader did not stop the renderer loss |
| `42141963135` | `90514e3` | PASS | PASS | **FAIL** | 138 reported, 2 skipped, 1 failure: `SplitTunnelScreenTest.searchFiltersByLabelAndReportsNoMatch` (`split-no-match` not found) |
| `42143462462` | `c6ac8f3` | PASS | PASS | **FAIL** | 138 reported, 2 skipped, 2 failures: the same split test plus `ClientFlowTest.manualImportPersistsAndNeverClaimsConnection` (`config-text` not found); per-test logcat shows `ImeTracker ... onShown` immediately before both |
| `42146411681` | `d4123d4` | PASS in 2m42s: 150 JVM tests, 0 failures, 0 ignored; lint 0 errors / 94 warnings | PASS in 6m52s: 138 tests, 0 failures, 0 skipped | **PASS** in 6m41s: 138 tests, 0 failures, 2 skipped | `ime-disabled.txt` shows all IMEs disabled; `display-contract.txt` = 720x1280 @ 280dpi = 411dp |

Run `42141963135` is the proof that the skip works without weakening the gate: all 138 tests
were reported, the emulator survived the full suite, and only one non-skipped test failed.
Run `42143462462` then showed both failures were soft-keyboard occlusion on the 411dp AVD,
not product assertions; those tests pass on API26 and on the physical phone. Run
`42146411681` is the green gate for PR #18, with the same 138-test suite and the two
documented skips reported as SKIPPED, never PASS.

### Post-merge `main` `fa34b67` — CI run `42186391691`

`fa34b67` is the PR #18 merge commit. It was verified twice because the first API 26 attempt
was cancelled by the job budget, not by a test.

| Job | Attempt | Result |
|---|---|---|
| build | 1 | PASS in 3m29s: 150 JVM tests, 0 failures, 0 ignored; lint 0 errors / 94 warnings |
| device API 35 | 1 | PASS in 8m38s: 138 tests, 0 failures, 2 skipped; `display-contract.txt` = 411dp; `ime-disabled.txt` shows all IMEs disabled |
| device API 26 | 1 | **CANCELLED at the 20m job timeout** with no test XML, no artifact and no job log (`BlobNotFound`) |
| device API 26 | 2 (rerun) | PASS in 6m49s: 138 tests, 0 failures, 0 skipped |
| run overall | 2 | **PASS** only after the rerun; attempt 1 is recorded as failed/cancelled, never as PASS |

The cancellation is explained by the workflow, not by the code: the device job allowed 20m
while `verify-device.sh` can legitimately spend up to 600s boot + 6m install + 8m tests = 24m.
A slow boot therefore cancels the job before it can finish or upload evidence, and GitHub
does not retain logs for the cancelled job. The rerun of the identical commit completed in
6m49s. This PR raises the budget to 30m so the existing inner bounds stay enforceable.

### PR #19 and final gate

PR #19 (`fix/device-job-budget-20261106`) raised the device job timeout from 20m to 30m and
published this truth table. Its head `a73d71d` passed run `42192806914` (build, device 26,
device 35, SonarCloud). It was merged as `1d2e499`; the merge commit changes only the CI
workflow and docs, not app or test sources.

The post-merge CI run `42197489233` at exact SHA `1d2e49948aa121c5c4e82a705958a8d313cdeb69`
completed on 2026-11-06 and is the final `main` gate:

| Job | Result | Duration |
|---|---|---|
| build | PASS | 2m25s |
| device API 26 | PASS | 6m07s |
| device API 35 | PASS | 9m25s |

All three jobs reported `success`; no job was cancelled, rerun or skipped as a whole. This is
the verified application state before TASK_15 starts. TASK_15 is recorded separately in
`docs/evidence/TASK_15.md` and does not alter any earlier result in this table.

### Physical device — itel S686LN, Android 15 / API 35

| Item | Value |
|---|---|
| ADB serial | `1334825549012211` |
| Panel | 1080x2436 @ 480dpi = 360dp, locale `ar-EG` |
| App update | in place; `firstInstallTime` unchanged at 2026-11-05 18:02:46, `lastUpdateTime` 2026-11-06 04:12:19 |
| Signing certificate SHA-256 | `c07168d455a86f8c792ef44ebf213ab63ec327226589692bcad69231e67c085e` (matches installed app) |
| Installed APK SHA-256 | `b835eaa0df13e76c143d4e129719ed5376db14074d0e06efbd76327bbd4eca31` (28,836,922 bytes) |
| Display/flow suite | 68/68 pass (Advanced Network, Home, Identity, Repair Lab, Server Selection, Split Tunnel, Statistics flow + screen, Client flow, VPN flow, Security Center) |
| Arabic 360dp / 1.5x focused rerun | Outside the 70 above: `IdentityTest.arabicRtlLargeText360dp` **OK (1 test)** in 1.454s against test APK SHA-256 `82ff0d6491604e1567140bb7eb7a8a5ce5da083c089a37baa55a6aa48e41d440`; transcript `phone-arabic-rtl-test-20261106.txt` |
| Split-filter focused rerun | Outside the 70 above: `SplitTunnelScreenTest.searchFiltersByLabelAndReportsNoMatch` **OK (1 test)** in 3.699s against the final test APK SHA-256 `a62a233c88c75b7918141a335fb94d91662a3ac4170fbab3cd4fd6dc6d5fd4e8`; an earlier 3.348s run used `2704666a…`; transcript `phone-split-filter-test-20261106.txt` |
| New session-service tests | `VpnSessionServiceTest` OK (2 tests) |
| Total device tests run in this pass | 70, 0 failures |

`VpnSessionServiceTest` evidence (`session-evidence.txt`, pulled from the device):
`notification_id=19288`, `channel_id=kx_vpn_session`, `ongoing=true`,
`only_alert_once=true`, `visibility=0` (private), `category=service`,
title in Arabic (`جلسة KX VPN نشطة`) and text `KX device test`, posted while the activity was
visible. `dumpsys activity services com.knoux.kxprivatevpn` was empty after stop, and the
test asserts the notification is removed.

## 3. VPN truth

What is real in the merged code:

- Android VPN consent is requested before the backend starts; no auto-connect.
- The official WireGuard `GoBackend` owns the tunnel; state, handshake time, rx/tx counters,
  DNS servers and allowed IPs are read from the backend and Android `VpnService`.
- A companion `VpnSessionService` (foreground, `specialUse`, subtype
  `active_wireguard_vpn_session`) mirrors a measured `CONNECTED` transition only. The
  WireGuard library never calls `startForeground` itself (verified in its bytecode).
- The service is `START_NOT_STICKY` and restores nothing after process death, so a
  notification can never outlive its tunnel. It has no fake stop action; it opens the app
  where the real disconnect lives.
- Notification permission is requested once per process before consent. Denial does not
  block the tunnel; the session simply has no visible notification.
- Security Center reports `Tunnel verified` only when connected + real handshake + Android
  link + DNS + IPv6 routes are measured, and its body explicitly says traffic and leak
  protection are not verified. No "Fully protected" claim exists in the app.
- Kill Switch / Always-On rows reflect measured Android state and open Android VPN settings;
  KX does not implement enforcement itself. Unsupported rows are disabled.

What is not proven:

- No owned public WireGuard gateway, public egress, DNS-leak or IPv6-leak proof. A LAN-only
  QA gateway exists (`192.168.70.103:51820`, config protected outside the repo) but has not
  been used to claim public VPN availability.
- Kill switch and always-on enforcement is delegated to Android Settings, not implemented in KX.
- No Pro backend, WhatsApp number, entitlement keys, release keystore, or store Data Safety
  submission. No Pro feature is configured or claimed.

## 4. Owner references vs phone runtime

All 11 owner reference PNGs are byte-verified against `SHA256_REFERENCES.txt` (11/11, 0
mismatches). Runtime captures are real instrumentation screenshots from the itel at 360dp,
pulled to `D:\KX-QODER-VISUAL-HANDOFF-20261105\DEVICE-EVIDENCE-20261106\phone-360dp-20261106`.

| Reference | Runtime capture | Verdict |
|---|---|---|
| `00-brand-logo.png` | `03-reveal.png`, `04-welcome.png` | PASS — approved orb art is used unchanged |
| `01-welcome.png` | `07-en-411-day.png` | PARTIAL — same orb/brand/tagline, but one Continue action and no Import Config CTA; capture is Arabic despite the file name |
| `02-home-disconnected.png` | `70-home-disconnected-en.png` | PARTIAL — orb, status pill, connect action, 4 metric tiles, 5-item nav; missing bell/overflow, "Best Location", Ping/Download/Upload speed tiles. Metrics show honest dashes/real counters instead of invented values |
| `03-server-selection.png` | `73-servers-imported.png` | INTENTIONAL DIVERGENCE — imported WireGuard profiles only; no country catalogue, flags, latency/load or favorites. Latency says "not measured" |
| `04-connected-session.png` | `72-home-connected-en.png` | PARTIAL — no map, route flags, speed cards or duration dial; shows real profile, handshake time, rx/tx and tunnel address |
| `05-security-center.png` | `22-security-center-verified-light.png` | HIGH — same hero + shield rows + switches; copy says "Tunnel verified" and names unverified protections; unsupported rows disabled |
| `06-split-tunneling.png` | `25-split-tunnel-only-mode-light.png` | HIGH — same hero, mode switch, search, app rows and 3-part summary; uses real installed apps |
| `07-advanced-network.png` | `30-advanced-defaults-light.png` | HIGH structure — protocol cards and controls match; OpenVPN/Automatic and unsupported controls are disabled and labelled unavailable |
| `08-statistics.png` | `36-statistics-defaults-light.png` | HIGH structure — 4 summary cards, 7D/30D/3M/1Y range, honest empty chart. No privacy score or server-history list |
| `09-import-configuration.png` | none | UNVERIFIED — no runtime screenshot in this pass; import flow tests exist |
| `10-profile-settings.png` | only `11-settings-ar-night.png` (app settings) | UNVERIFIED — the reference's profile/settings composition was not captured |

Evidence caveat: several files named `*-en*` render Arabic because `IdentityTest` and
`HomeScreenTest` do not force a localized context on this `ar-EG` phone, so the file name is
not proof of English. English visual coverage must be re-captured with a localized context
(or on an en-US emulator) before claiming EN visual parity. The layout and behavior are real;
the language label in those file names is not.

## 5. Cleanup audit (nothing deleted)

Full inventory: `D:\KX-QODER-VISUAL-HANDOFF-20261105\DEVICE-EVIDENCE-20261106\cleanup-inventory.md`.

- Bytes deleted in this session: **0**.
- Deletion candidates (9 worktrees + 2 stale metadata entries, awaiting owner approval):
  **2,317,556,037 bytes ≈ 2.16 GiB**, re-measured after PR #17 merged. The earlier audit
  figure of 1.90 GiB excluded the PR #17 worktree, which was still required then.
- Unique-work patches preserved and hashed (6 files) plus 3 convergence screenshots.
- Fully merged branches (0 commits ahead of `origin/main`) are safe to delete without losing
  commits; unique branches are preserved as patches first.
- No `reset --hard`, `git clean`, force push, reflog expiry, uninstall, or app-data clear was
  run. The phone was updated in place with the matching debug certificate.

## 6. Reproduction

```powershell
# Build + JVM tests + lint + test APK
cd K:\KX-Private-VPN\android
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest

# Physical device, in place (matching debug certificate)
adb -s 1334825549012211 install -r app\build\outputs\apk\debug\app-debug.apk
adb -s 1334825549012211 install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
adb -s 1334825549012211 shell am instrument -w \
  -e class com.knoux.kxprivatevpn.VpnSessionServiceTest \
  com.knoux.kxprivatevpn.test/androidx.test.runner.AndroidJUnitRunner

# CI-equivalent device job (emulator only; it resizes the display and disables IMEs,
# so never point it at the owner's physical phone)
bash scripts/verify-device.sh
```

## 7. Remaining blockers

1. Public WireGuard exit/handshake/leak proof — missing (LAN-only QA gateway).
2. Pro backend, entitlement keys, WhatsApp support number — not configured.
3. Release signing, Play policy and Data Safety — not prepared.
4. Visual gaps listed above (home/connected/server catalogue; import and profile captures).
5. EN/AR capture-label defect in two test classes.
6. Cleanup of 2.16 GiB of stale worktrees/build outputs — requires owner approval.
