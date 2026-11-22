# KX PRIVATE VPN — CONSOLIDATED ENGINEERING REPORT

**Session:** KNOUX MAX recovery / integration / verification — 2026-11-22
**Scope:** recover the three prior agent missions, reconcile PR #53, integrate verified work,
verify on the physical device, and report status honestly.

> Statuses used: `IMPLEMENTED AND VERIFIED` · `IMPLEMENTED BUT UNVERIFIED` · `UNIQUE NOT MERGED` ·
> `COMMITTED LOCALLY` · `PUSHED` · `MERGED` · `DEVICE VERIFIED` · `VPN E2E VERIFIED` · `BLOCKED` ·
> `RELEASED AND VERIFIED`. Nothing below claims a PASS without an executed command, a SHA, a run id
> or a capture.

---

## 1. Canonical repository and remote

| Item | Value |
|---|---|
| Canonical path | `K:\KX-Private-VPN` (`git rev-parse --show-toplevel` = `K:/KX-Private-VPN`) |
| Remote | `https://github.com/daynightae-cmyk/KX-Private-VPN` |
| Worktrees | exactly one (`K:/KX-Private-VPN`) |
| Stashes | none |
| Branch policy | single integration branch `main`; feature branches merged after verification |

The two different repos `daynightae-cmyk/KX` and `D:\KNOUX-Workspace\KX` were **not touched**.

## 2. Initial and final SHA

| Ref | At session start | Now |
|---|---|---|
| Branch HEAD (`codex/visual-elevation-and-cleanup-20261119`) | `6640da4` | `613a08a` (docs commit pushed) |
| `origin/codex/...` | `6640da4` | `613a08a` |
| `main` / `origin/main` | `096b260` | `096b260` (PR #53 merge pending CI) |

`git rev-list --left-right --count main...HEAD` = `0 2` (branch is 2 commits ahead of `main`).

## 3. Branch / worktree / PR matrix

| PR | Branch | State | Mergeable | CI on exact head |
|---|---|---|---|---|
| **#53** | `codex/visual-elevation-and-cleanup-20261119` | **OPEN** | MERGEABLE | ✅ `44484793803` green on `6640da4` (build 7m50s, release-compile 4m27s, device(26) 13m53s, device(35) 30m53s, SonarCloud gate OK); re-running as `45009718221` on `613a08a` |
| #52 | `codex/visual-fidelity-ref14-18-03` | MERGED → `096b260` | — | `44274718413` green |
| #51 | `codex/release-artifact-and-supply-chain` | MERGED → `e983498` | — | `44268994343` green |
| #50 | `codex/recover-audit-fix-api26` | MERGED → `377ffb3` | — | `44257528678` green |
| #42–#49 | service slices | MERGED | — | per-PR green |

CircleCI reports `fail` on every PR because **the repository has no `.circleci` config**
(`No configuration was found in your project`). Verified: only `.github/workflows/android.yml`
exists. It is not a real gate. There are no other local-only branches, no dangling worktrees, and no
reflog entries pointing to unmerged feature commits.

## 4. Recovered unique work (the three missions)

The three conversation exports were **not present in the repo**; they were located in
`C:\Users\day night\Downloads\` and parsed locally:
`kx-vpn.json` (4.8 MB, 9 user / 477 assistant), `kx-private-vpn-forensic-audit-and-recovery.json`
(11.4 MB, 25 user / 823 assistant), `kx-visual-references-01-10-reconstruction.json`
(2.9 MB, 3 user / 55 assistant). A fourth export
`kx-private-vpn-native-android-transformation(2).json` (11.1 MB) was also found.

- **MISSION A — functional integration.** App-uninstall verification and Download Rescue result
  verification are **already in `main`** (confirmed by the earlier forensic reconciliation: patch-id
  match / superset diff). Nothing to re-implement.
- **MISSION B — forensic recovery.** The 17 `docs/forensic-audit/*` deliverables are restored in
  `main` (PR #50). PR #53 is the outstanding unmerged branch and is being integrated now.
- **MISSION C — visual references (JS Visual Lab).** The Dyad lab is a JavaScript prototype, **not**
  the Android app. Its own acceptance report states **0/20 pixel-fidelity approvals** and **no native
  integration**. The transferable artefacts are the 20 per-screen specs
  (`K:\KX_DYAD_VISUAL_HANDOFF_20261108\dyad-native-handoff-b1da551\specs\`),
  `KX_COMPOSE_VISUAL_TOKENS.md` and the 20 byte-preserved references
  (`.../visual-lab/public/references/`). These were used as the measurement authority for the native
  Compose work; no JS prototype is treated as an Android deliverable.

## 5. Completed vs missing functionality

Classification of the real app sections (source inspection + device):

| Section | Route / class | Status |
|---|---|---|
| VPN Home | `features/HomeScreen.kt` (ref 14) | IMPLEMENTED AND VERIFIED (DEVICE VERIFIED, disconnected state) |
| Real WireGuard transport | `platform/vpn/VpnGateway.kt` (official `GoBackend`) + `VpnController.kt` | IMPLEMENTED AND VERIFIED (source + JVM/Debug); **VPN E2E BLOCKED** (no gateway) |
| VPN foreground surface | `platform/vpn/VpnSessionService.kt` | IMPLEMENTED AND VERIFIED (source + tests); no live run |
| Config import / Keystore | `data/config/ProfileStore.kt`, `ConfigValidator.kt` | IMPLEMENTED AND VERIFIED (JVM tests) |
| Server selection / Split tunnel | `ServerSelectionScreen.kt`, `SplitTunnelScreen.kt` | DEVICE VERIFIED (split tunnel renders real apps + honest banner) |
| Security / Statistics / Settings | `SecurityScreens.kt`, `StatisticsScreen.kt`, `SettingsScreen.kt` | IMPLEMENTED (device captures in `docs/evidence/visual-fidelity-20261119/`) |
| Repair Lab + 10 tools | `RepairLabScreen.kt`, `platform/repair/*` | IMPLEMENTED AND VERIFIED (measurement-backed, journal) |
| Network Doctor / Media Slim / Storage X-Ray / Cache / Large / Duplicates / Download Rescue / Installed Apps / Guardian / Autopilot | `platform/*`, `features/*` | IMPLEMENTED; per-slice suites merged (#39, #42–#49) |

No services were invented and none were removed.

## 6. WireGuard architecture status

- **Backend:** the official `com.wireguard.android.backend.GoBackend` runs its own `VpnService`; the
  app adds `VpnSessionService` as the foreground owner (the library never calls `startForeground`).
- **State machine (`VpnController`):** the only owner of tunnel state; `CONNECTED` is published
  **only after** `gateway.start(config)` returns UP, and traffic is read from
  `backend.getStatistics` (real RX/TX + latestHandshakeEpochMillis). A backend that reports DOWN
  mid-session sets `ERROR/BACKEND_STOPPED`. There is no code path that fabricates a handshake, an IP,
  a ping, uptime, a counter or a speed.
- **Consent:** start happens only after Android `VpnService` consent; denial → `CONSENT_DENIED`.
- **Secrets:** WireGuard profiles (incl. private key) are AES-256-GCM encrypted with an
  AndroidKeyStore key in `noBackupFilesDir` via `AtomicFile`; plaintext buffers are zeroed.
- **Honest UI:** `HomeScreen` derives every value from the live session, Android's formatter or the
  recorded history; unmeasured slots show `—`. Confirmed on device (`غير متصل`, `—`).

## 7. Real handshake and RX/TX evidence — or the exact blocker

**BLOCKED.** No authenticated handshake and no observable tunnel traffic were produced, because no
authorized WireGuard endpoint (server public key + endpoint host/port + a client profile) is
available to this session. The Windows interface `KX-Win-LAN-Server` (`WireGuardTunnel$KX-Win-LAN-Server`,
`10.253.64.0/24`) was **not** inspected with write access and its peer handshake was never verified;
it is **not** assumed to be an authorized KX Android test gateway. No private keys were read,
printed or changed. Exact requirement to unblock: an owner-authorized WireGuard gateway + a test
profile (`.conf` with client private key) imported on the phone.

## 8. Samsung installation and runtime evidence

See `docs/evidence/device-20261122/DEVICE_VERIFICATION_20261122.md`.
Device `R9ZR20HWHQY` (SM-A025F, API 31, ar-EG, 720x1600 @280).
Key results: package had to be `pm enable`d (device-side state, not a source defect); the entry gate
holds with no input at t≈16 s; after the emblem tap Home renders the reference-14 composition with an
honest disconnected state; split tunneling lists real apps. **DEVICE VERIFIED** for those flows.

## 9. Visual-reference acceptance (all 20)

References inspected at `K:\KX_DYAD_VISUAL_HANDOFF_20261108\visual-lab\public\references\`
(byte-preserved originals). Native routes exist for all 20. Acceptance here is **not** pixel-parity.

| Ref | Screen | Native route | Evidence | Result |
|---|---|---|---|---|
| 01 | Media Slim Hero | `MediaSlimScreen` / `MediaReferenceHero` | prior captures | IMPLEMENTED BUT UNVERIFIED (this session) |
| 02 | Network Doctor Hero | `NetworkScreen` / `NetworkReferenceHero` | prior captures | IMPLEMENTED BUT UNVERIFIED |
| 03 | Repair Lab Hub | `RepairLabScreen` | 2026-11-19 capture + PR #52 | IMPLEMENTED AND VERIFIED (visual) |
| 04 | Temporary Files Hero | `CacheScreen` | prior captures | IMPLEMENTED BUT UNVERIFIED |
| 05 | Large Files Hero | `HeavyweightScreen` | prior captures | IMPLEMENTED BUT UNVERIFIED |
| 06 | Duplicates Hero | `DuplicateDnaScreen` | prior captures | IMPLEMENTED BUT UNVERIFIED |
| 07 | Storage Health Hero | `StorageXrayScreen` | prior captures | IMPLEMENTED BUT UNVERIFIED |
| 08 | Download Rescue Hero | `DownloadRescueScreen` | prior captures | IMPLEMENTED BUT UNVERIFIED |
| 09 | Smart Repair Hero | `AutopilotScreen` | prior captures | IMPLEMENTED BUT UNVERIFIED |
| 10 | Installed Apps Hero | `InstalledAppsScreen` | prior captures | IMPLEMENTED BUT UNVERIFIED |
| 11 | Duplicates Results | `DuplicateDnaScreen` | prior captures | IMPLEMENTED BUT UNVERIFIED |
| 12 | App Mgmt Results | `InstalledAppsScreen` | prior captures | IMPLEMENTED BUT UNVERIFIED |
| 13 | Large Files Results | `HeavyweightScreen` | prior captures | IMPLEMENTED BUT UNVERIFIED |
| **14** | **Main VPN Home** | `HomeScreen` | **device 2026-11-22** | **DEVICE VERIFIED (disconnected)** |
| 15 | Smart Repair Plan | `AutopilotScreen` | prior captures | IMPLEMENTED BUT UNVERIFIED |
| 16 | Network Doctor Results | `NetworkScreen` | prior captures | IMPLEMENTED BUT UNVERIFIED |
| 17 | Media Slim Results | `MediaSlimScreen` | prior captures | IMPLEMENTED BUT UNVERIFIED |
| 18 | Tools Catalog | `ToolsCatalogScreen` | 2026-11-19 + PR #53 | IMPLEMENTED AND VERIFIED (visual) |
| 19 | Download Rescue Results | `DownloadRescueScreen` | prior captures | IMPLEMENTED BUT UNVERIFIED |
| 20 | Temporary Files Results | `CacheScreen` | prior captures | IMPLEMENTED BUT UNVERIFIED |

Known deviation: reference 14 shows a `الخوادم` bottom tab while 18 shows a `الأدوات` tab. The app
keeps 14's tab set (`الرئيسية/الخوادم/الأمان/الإحصاءات/الإعدادات`) and reaches Tools via "view all".
This is documented, not silently reconciled.

### 9.1 NEW evidence-integrity finding (2026-11-22) — the 2026-11-19 capture set was mislabelled

Independently opening every file in `docs/evidence/visual-fidelity-20261119/` showed that **5 of the 6
labels do not match the content**, and the set contains **no Tools Catalog and no Repair Lab capture**:

| File (as committed) | Actual content |
|---|---|
| `01-home-ar.png` | ✅ Home (disconnected) |
| `02-tools-ar.png` | Security Center |
| `03-repair-ar.png` | Statistics |
| `04-settings-ar.png` | Session Privacy |
| `05-security-ar.png` | Session Privacy |
| `06-statistics-ar.png` | Security Center |

This is **not** a product navigation bug — `NativeClient.kt` maps `tools → ToolsCatalogScreen`,
`security → SecurityRoute`, `stats → StatisticsRoute` correctly. It is a capture/labelling error.
Corrective action: the five files were **renamed to their true content** (bytes and SHA-256 preserved)
and the claim was retracted in
`docs/evidence/visual-fidelity-20261119/CORRECTION_20261122.md`. It also means the earlier
"6 distinct hashes" check validated bytes only — it never validated labels. Tools Catalog and Repair
Lab are therefore `VISUAL_UNVERIFIED` on device this session (their code exists and was touched in
PR #52/#53).

## 10. Before / after device screenshots

Before: `docs/evidence/device-20261118/` (6 captures) and the corrected set in
`docs/evidence/visual-fidelity-20261119/` (Home, Tools, Repair, Settings, Security, Statistics).
After (this session): `docs/evidence/device-20261122/` — intro, entry-gate-holds, Home, split tunnel.

## 11. Kotlin / Compose files changed in PR #53

`designsystem/KxReferenceUi.kt` (+240), `features/HomeScreen.kt` (+42),
`features/RepairLabScreen.kt` (+47), `features/ToolsCatalogScreen.kt` (+98),
`res/values/client_strings.xml` and `res/values-ar/client_strings.xml` (+1 each),
`docs/evidence/VISUAL_FIDELITY_20261118.md`. Plus this session's docs commit
(`docs/PROGRESS_LEDGER.md`, `docs/STATUS_BLOCKERS.md`).

## 12–14. Test / API / CI results

- CI (GitHub Actions `Native Android`) on PR #53 head: `build` ✅, `release-compile` ✅,
  `device (26)` ✅, `device (35)` ✅ (353 tests, 0 failed on the suite run), SonarCloud gate ✅.
  Run `44484793803` on `6640da4`; re-run `45009718221` on `613a08a` (this session's docs commit).
- Backend `python -m unittest discover -s backend/pro` → 52 tests OK (per ledger, PR #51).
- API compatibility: `minSdk 26`, `targetSdk 36`; CI device matrix runs API 26 and API 35.
- A previously green CI run is only cited for its own SHA — the historical `44244219616` is **not**
  reused as proof for `613a08a`.

## 15. Release signing status

**BLOCKED.** `android/key.properties` is absent, so release builds fall back to the debug key. No
owner keystore was provided this session. No AAB signed with debug credentials may be published.

## 16. Remaining blockers

| Blocker | Status | Needed |
|---|---|---|
| Real VPN E2E (handshake + traffic) | BLOCKED | Authorized WireGuard gateway + test profile |
| Owner release keystore | BLOCKED | `key.properties` / keystore |
| Automated visual-regression baseline | BLOCKED | Agreed diff thresholds / reference renders |
| Correctly-labelled Tools Catalog + Repair Lab device captures | BLOCKED (tooling) | Reliable screen-scoped capture; the 2026-11-19 set was mislabelled (see §9.1) |
| Full 20-screen re-verification this session | PARTIAL | Time; prior captures exist for most screens |

## 17. Exact next safe execution action

1. Let CI `45009718221` finish on `613a08a`; if green, merge **PR #53** into `main` (merge commit),
   verify the resulting `origin/main` SHA, then delete the merged branch — one integration state.
2. Open a small follow-up PR carrying this report + `docs/evidence/device-20261122/`, merge after CI.
3. To unblock VPN E2E: import an owner-authorized WireGuard test profile on `R9ZR20HWHQY`, tap
   Connect, accept the Android consent, and record the real handshake timestamp + RX/TX counters.
4. Provide the owner keystore to produce a release-signed AAB.
