# KX Private VPN — historical phone run (claims corrected 2026-11-07)

**The blanket PHONE/VISUAL PASS conclusions below are withdrawn.** The original record is
retained for audit, not current acceptance. See [hardening audit corrections](HARDENING_AUDIT_20261107.md)
for fixture provenance, incomplete coverage, data-safety findings and public privacy exposure.
No later fix inherits this run's PASS status.

Device: itel S686LN, Android 15 / API 35, serial `<serial-withheld>`, package
`com.knoux.kxprivatevpn`. Every device command pinned `-s <serial-withheld>`; the emulator was
never targeted. No uninstall, no `pm clear`, no destructive storage operation. This report
records measured results only.

## A. Git / build

- Starting SHA: `4210430f5927a001cc7a343d15f1776eb028d5d6` (main; all TASK_16–21 evidence merged).
- APKs rebuilt at that checkout (`:app:assembleDebug :app:assembleDebugAndroidTest`,
  BUILD SUCCESSFUL in 32 s). No code change was needed — this is verification only, so no new PR.
- Candidate app APK: 27,722,714 bytes, SHA-256
  `6e3414e1414753fc7b602f4c657c51cc72f4fb17a210833e35b52e013f489200`.

## B. Signature, rollback and in-place install

- Installed certificate SHA-256: `c07168d455a86f8c792ef44ebf213ab63ec327226589692bcad69231e67c085e`
  (Android Debug) — reverified from the pulled installed base APK, not assumed.
- Candidate certificate SHA-256: identical → signature-compatible in-place update allowed.
- Rollback copy of the previously installed base APK preserved at
  `docs/evidence/task21/phone-rollback/installed-base.apk` (29,266,030 bytes, SHA-256
  `833c91fb584c3e7a1a4ec7a0a216eb3d0630e5c3a3b73f87e179a07a8aaa4019`).
- `install -r` (app) and `install -r -t` (androidTest): Success.
- Data preserved: `firstInstallTime` unchanged at `2026-11-05 18:02:46`; `lastUpdateTime` now
  `2026-11-07 07:37:53`. App data pre/post inspection shows only KX-owned QA artifacts
  (test profiles, repair journal, cache), nothing personal touched.

## C. Tests (physical phone, exact SHA)

```
adb -s <serial-withheld> shell am instrument -w -e class
com.knoux.kxprivatevpn.StartupTest,IdentityTest,ClientFlowTest,VpnFlowTest,
VpnSessionServiceTest,SessionShieldScreenTest,AutopilotScreenTest,RepairFlowTest,
RepairLabScreenTest com.knoux.kxprivatevpn.test/androidx.test.runner.AndroidJUnitRunner
```

**OK (48 tests) in 99.498 s, 0 failures, 0 skips**:

| Class | Tests | Covers |
|---|---:|---|
| StartupTest | 1 | real launcher restores dashboard across recreation/resume |
| IdentityTest | 8 | cinematic splash choreography, tap-to-enter gate, reduced motion, RTL Arabic layout, EN day |
| ClientFlowTest | 3 | language/theme (Arabic/English, day/night) persistence across recreation |
| VpnFlowTest | 4 | honest disconnected VPN lifecycle, consent, no simulated connect |
| VpnSessionServiceTest | 2 | manifest FGS type/permissions; ongoing `kx_vpn_session` notification while foreground, removed after stop |
| SessionShieldScreenTest | 8 | TASK_19: inventory, revoke/erase/prune approval, honest receipts, real encrypted-store read-back |
| AutopilotScreenTest | 14 | TASK_21: measured plan, selection, approval, partial/cancel honesty, real cache delete (4096 B freed) and real aged-history prune read-backs |
| RepairFlowTest | 1 | all repair routes navigate to their real screens |
| RepairLabScreenTest | 7 | 10/10 available catalog, honest unknown-service panel |

Fresh on-phone network probe (same SHA): `NetworkDoctorPlatformTest#reportsRealProbeOutcomeWithoutAssumingExternalInternet`
→ **OK (1 test)**; evidence `10-network-probe.txt` = `LIVE_HTTP=200`, `HTTPS_HEAD_elapsedMs=748`,
`No VPN claim`.

Runtime logs: `phone-logcat-4210430.txt` (104,104 lines) contains **0** `FATAL EXCEPTION` / `ANR`.
It records the real foreground-service start (channel `kx_vpn_session`, `ONGOING_EVENT`) and
its removal. Full runner output: `phone-instrumentation-main-4210430.txt`.

## D. UI — runtime captures and visual gate

Captures pulled from the phone after the run (`docs/evidence/task21/phone-evidence/captures/`):

- Cinematic splash: `01-photons` (KX glass orb directly on deep navy, no square-logo flash),
  `02-ribbons`, `03-reveal`, `04-welcome` (entry-ready), `05-day-reduced` (reduced motion).
  `diag-entry-gate.txt` shows the single `entry-orb` button (1080×2436 physical display),
  "Tap the KX emblem to enter" hint, `orb-count=1` — no automatic Home navigation, tap required,
  duplicate-tap protection asserted by the passing gate tests.
- VPN: `11-vpn-session-connected` — real GoBackend 1.0.20260102 session card that states
  "no handshake recorded from WireGuard", "these values are not a protection claim" and the
  stopped-tunnel Android notice; Disconnect action present.
- Home/settings: `08-native-home-imported` (Home with imported profile), `11-settings-ar-night`
  (Arabic night settings).
- Repair lab: `43/43b` day (10/10 catalog), `44/44b` Arabic night large text, `45-repair-unknown-route`
  (honest unknown panel), `46-repair-lab-flow`, `47-repair-duplicate-flow`.
- Autopilot: `48-autopilot-plan-day`, `49-autopilot-plan-night-ar-large` — **Arabic RTL at 1.6x
  font renders without clipping on the physical device** (cards, approval-required labels, run
  button and scope text all fully visible); the brief's clipping concern is resolved: no device
  clipping observed.

Visual gate vs the 11 owner references (`D:\KX-QODER-VISUAL-HANDOFF-20261105\references`, all
hash-verified previously): the phone runtime preserves the owner orb/ribbon/glass identity —
glass cards, orb as primary action, RTL-shaped Arabic, sufficient spacing and contrast.
Parity is style-level (no per-service owner reference exists); no reference image is used as
background or data source.

## E. Services — runtime-verified this run

Verified on-phone at `4210430`: Cinematic entry (splash + gate), Home, VPN lifecycle + foreground
service, Server/import surfaces, Repair Lab (catalog 10/10), Session Shield (TASK_19), Repair
Autopilot (TASK_21), Network Doctor (real HTTPS 200 probe), Duplicate DNA / Ghost Hunter /
Heavyweight / Cache routes (route walk). TASK_15–18 screen-level phone proof at their merged SHAs
is recorded in `TASK_16.md` / `TASK_17.md` / `TASK_18.md`.

## F. VPN network status: NETWORK NOT VERIFIED

`D:\KX-WireGuard-LAN-QA` shows a LAN-only Windows WireGuard server configured
(`WG_LISTEN_PORT=51820`, 1 peer) but `WG_PEERS_WITH_NONZERO_HANDSHAKE=0` — **no handshake has
ever occurred and no public egress exists**. Missing infrastructure for a verified tunnel:
an owned, reachable WireGuard gateway with a routable endpoint, provisioned peer credentials,
and a NAT/firewall path to the internet. No server is invented; the app-side honesty (real
GoBackend, no simulated connect, explicit "no handshake" reporting) is what was verified on the
phone. Full VPN protection is NOT claimed from UI/service tests.

## G. Pro / WhatsApp activation: UNAVAILABLE (honest)

No entitlement backend, activation codes, expiry/revocation, rate limiting or owner WhatsApp
contact exists anywhere in the codebase (grep over `src/main`: zero matches for
whatsapp/payment/entitlement/license). No Pro screen or route exists; no universal password is
embedded. Activation stays unavailable until the owner supplies a backend/contact; nothing
decorative is labelled as working activation, and no essential VPN security is paywalled.

## H. Cleanup / preservation

Preserved set confirmed intact after this session's work: canonical `K:\KX-Private-VPN`, active
main worktree, dirty CONSOLIDATE + task07, unique ENTRY + freebuff-ui worktrees, codex/task-00
branch, `KX-PRESERVE`, owner visual handoff, `KX-WireGuard-LAN-QA`, and
`docs/evidence/evidence.zip` (present, provenance/review per brief). No new deletion candidates;
any further removal would need a new inventory + explicit owner approval.

## I. Release verdict (distinct states)

| State | Verdict |
|---|---|
| MAIN INTEGRATED | YES — `4210430` carries TASK_16–21 + evidence; remote == local |
| CI VERIFIED | YES — run `42333821653` (build, device 26, device 35 all SUCCESS) on `4210430` |
| PHONE VERIFIED | YES — 48/48 targeted + 1/1 live probe on itel S686LN at `4210430`, 0 crashes |
| VPN NETWORK VERIFIED | NO — no owned gateway handshake/egress ever observed (LAN-only, 0 handshakes) |
| PRODUCTION RELEASED | NO — debug signing only; no store release; VPN gateway, Pro backend and video compression remain missing/deferred |
