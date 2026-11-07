# KX Private VPN — final evidence audit (2026-11-07)

Canonical repo: `K:\KX-Private-VPN` / `https://github.com/daynightae-cmyk/KX-Private-VPN`.
App id `com.knoux.kxprivatevpn`. **main = `b1b0016`** (TASK_21 merged `9e09641` + evidence `d2a04ab`
+ cleanup record `b1b0016`). This audit records only measured results; anything not observed is
listed as pending/deferred, never as passed.

## 1. Task merge ledger (all verified by exact-SHA CI before merge)

| Task | PR | head SHA | merged SHA | CI run (build / device26 / device35) |
|---|---|---|---|---|
| TASK_16 Cache Surgeon | #21 | `a384c05` | `5746bc3` | `42268090643` all SUCCESS |
| device-suite robustness | #23 | `d696fda` | `a70d89d` | `42310987134` all SUCCESS |
| TASK_17 Media Slim | #22 | `b726fe7` | `bc590b5` | `42312482260` all SUCCESS |
| TASK_18 Download Rescue | #24 | `e4a4178` | `e47d8ee` | `42325382831` all SUCCESS |
| TASK_19 Session Shield | #25 | `1512ece` | `fcbf99e` | `42329089280` all SUCCESS |
| TASK_20 Network Doctor | on main | `95fa3d6` | (audit, pre-existing) | `ci-95fa3d6-api26` evidence |
| TASK_21 Repair Autopilot | #26 | `dfe535c` | `9e09641` | `42331246208` all SUCCESS |

Main-push CI on the TASK_21 evidence commit `d2a04ab`: run `42332627046` SUCCESS. Every merge was a
squash whose tree is byte-identical to the CI-tested head (verified by empty `git diff`). SonarCloud
quality gate OK on each PR (TASK_21: `new_reliability_rating = A`). No failing test was skipped,
disabled, weakened or deleted to reach green; the two documented API35-emulator-only skips are
reported SKIPPED, never PASS.

## 2. Test counts (measured)

- JVM at TASK_21/main code: **201 tests, 0 failures, 0 errors, 0 skipped** (24 suites), including
  `AutopilotTest` 9, `RepairCatalogTest` 6 (10 available services), `MediaSlimTest` 10,
  `DownloadRescueTest` 16, `SessionShieldTest`, `NetworkDoctorTest` 2.
- Full instrumentation suite on main: **196 `@Test` methods** across the androidTest source set.
  CI device(26) and device(35) run the suite green per the ledger above.
- TASK_18 full instrumented suite: **174 tests, 0 failures** on both API26 emulator and the physical
  API35 phone (at that SHA). TASK_21 connected subset: **22/22** (Autopilot 14 + RepairLab 7 +
  RepairFlow 1), including the two real read-back proofs (cache file really deleted, 4096 B freed;
  aged history really pruned, fresh kept).

## 3. Physical phone (itel S686LN, Android 15 / API 35, serial `1334825549012211`)

- Verified on-phone at their SHAs: TASK_16 (cache flow/settings/contract + client flow), TASK_17
  (Media Slim 14 tests OK), TASK_18 (full 174-test suite + real SAF classification of six seeded
  files). Captures committed under `docs/evidence/task16/`, `task17/phone/`, `task18/`.
- In-place updates only (`adb -s 1334825549012211 install -r`); first-install time preserved; no
  uninstall, no `pm clear`, no permission changes beyond the SAF grants the tests themselves make
  and release. Every phone command pinned the serial.
- **Pending:** TASK_19 and TASK_21 on-phone re-verification at the current main SHA. The phone's ADB
  is offline now (only `emulator-5554` attached); this needs a physical reconnect (see §8).

## 4. APK identity

- Current debug APK: 27,722,714 bytes, SHA-256
  `6e3414e1414753fc7b602f4c657c51cc72f4fb17a210833e35b52e013f489200`.
- Signing certificate SHA-256
  `c07168d455a86f8c792ef44ebf213ab63ec327226589692bcad69231e67c085e` (Android Debug) — the same
  certificate as the TASK_16 phone install, so updates are compatible in-place. This is a **debug**
  build; no production signing secret exists or is claimed.

## 5. Visual experience vs the 11 owner references

- All **11** approved references hash-verified (`docs/evidence/reference-sha256.json`, recomputed
  SHA-256 matches `SHA256_REFERENCES.txt`) and opened/inspected, not merely listed.
- The native app follows the owner KX orb/ribbon/glass identity; the bespoke cinematic glass-orb
  tap-to-enter splash is implemented (`KxLaunch`, `IntroTimeline`). Runtime Compose/phone captures
  (day/night, EN/AR, RTL, 360dp, 1.6x font) are committed per task, including TASK_21
  `48-autopilot-plan-day`, `49-autopilot-plan-night-ar-large`.
- Parity is **style-level, not pixel-level**: no per-service owner reference exists for the newer
  repair screens, and reference renders contain fictional fleet/latency/leak data KX does not
  invent. No reference image is used as a background or data source.

## 6. VPN honesty

- Real official WireGuard `GoBackend` (`com.wireguard.android:tunnel`) owns the tunnel; state,
  handshake, rx/tx, DNS and allowed IPs are read from the backend/`VpnService`. Consent is requested
  before start; a `specialUse` foreground `VpnSessionService` mirrors only a measured CONNECTED
  transition and restores nothing after process death.
- **No owned WireGuard gateway, provider credentials or peer exists here**, so no real public VPN
  exit is claimed or tested. Backend failure / missing authorization surface as explicit errors.
  No "Fully protected" claim exists in the app.

## 7. Pro / backend honesty

- No universal Pro password is hard-coded; no owner WhatsApp number, backend credential, payment
  configuration or production signing secret is invented. Pro activation / store release are
  **not configured and not claimed** (consolidation truth table: "does not claim … Pro service,
  store release … that was not observed").

## 8. Blockers / pending (reproducible continuation)

1. **Phone ADB offline.** Reconnect itel (`1334825549012211`), then re-run at main `b1b0016`:
   `adb -s 1334825549012211 install -r <apk>` and
   `adb -s 1334825549012211 shell am instrument -w -e class com.knoux.kxprivatevpn.SessionShieldScreenTest,com.knoux.kxprivatevpn.AutopilotScreenTest,com.knoux.kxprivatevpn.RepairFlowTest com.knoux.kxprivatevpn.test/androidx.test.runner.AndroidJUnitRunner`.
   Never target the emulator with phone scripts; never uninstall / `pm clear`.
2. **Video compression deferred** (TASK_17): videos measured/listed, never modified; MediaCodec/
   MediaMuxer work is future scope, not faked.
3. **No owned VPN gateway / Pro backend** (§6, §7): provider-independent honesty maintained.
4. Main CI on the cleanup-docs commit `b1b0016` was queued at audit time (docs-only; the code commit
   `d2a04ab` is already SUCCESS).

## 9. Unique-work reconciliation + cleanup (owner-approved)

- No unique work lost. TASK17/18/19/21 + device-suite branches are tree-identical to main squashes.
  The unmerged `kx-touch-to-enter`, `codex/task-00`, `qoder-freebuff-ui` branches are superseded or
  deliberate preservation snapshots (main already has tap-to-enter orb, `ServiceGlyph`,
  `RepairGlyph`, full bilingual copy); they are **preserved**, not deleted.
- Owner approved Section A of `CLEANUP_INVENTORY_20261107.md`; executed: 8 clean merged worktrees +
  their 8 branches (local+remote) removed, 2 traycer registrations pruned.
  **Actual recovered bytes: 2,548,306,944 (~2.37 GiB)** (summed measured `du` of removed dirs, all
  confirmed absent). Preserved untouched: canonical repo, active main worktree, dirty
  CONSOLIDATE/task07, unique ENTRY/freebuff-ui worktrees, `KX-PRESERVE`, owner
  `KX-QODER-VISUAL-HANDOFF` references, `KX-WireGuard-LAN-QA`.

## 10. Conclusion

TASK_16–21 are implemented as real Android services, merged to canonical main through green
exact-SHA CI, with JVM + instrumentation + physical-phone evidence where the device was reachable,
honest VPN/Pro boundaries, hash-verified owner references, and an owner-approved cleanup that
recovered ~2.37 GiB without losing unique work. Remaining gaps are explicitly pending (phone
reconnect for TASK_19/21 on-phone proof, video compression, owned VPN/Pro infrastructure), not
silently passed.