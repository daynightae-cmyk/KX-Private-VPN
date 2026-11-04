# Native migration progress

| Task | Source base | Native implementation | Verification | Integration |
|---|---|---|---|---|
| TASK_00 | 37c5d8778ccdcd03dd78fca71ddcdc3d3b6df9d4 | Native cinematic identity | API26 verified; visual/performance unaccepted; API35 emulator loss | Draft PR #1, not merged |
| TASK_01 | TASK_00 branch | Welcome actions, education, persistence/navigation | Implemented; first-install/visual acceptance pending | Draft PR #2, dependent on #1 |
| TASK_02–03 | TASK_00 branch | Honest disconnected Home, real saved-profile selection/removal, real WireGuard transport behind Android consent (official `GoBackend`) | 24 JVM tests pass incl. 12 controller cases; lint 0 errors; API26 CI device gate green; external handshake deferred (no gateway) | Draft PR #3, stacked on #2 |
| TASK_04 | TASK_00 branch | Connected session screen with backend-confirmed counters/handshake and real `ConnectivityManager` VPN link | UI/RTL strings compile; no owned gateway for a real handshake yet | Draft PR #3 |
| TASK_05 | TASK_00 branch (main `4122b33`) | Truthful Security Center: real lockdown/always-on from `Settings.Secure` + `GoBackend.isLockdownEnabled`, tunnel DNS/IPv6 route evidence, disabled unimplemented rows, Android settings intents | 37 JVM tests (13 new policy); API26 CI 27/27 instrumentation; lint 0 errors; visual comparison vs `05-security-center_d0a1f09a.png` documented; no gateway so field-level "Fully protected" deferred | Merged PR #4 (`5ce8002` → `4122b33`), CI run `41873137461` |
| TASK_06 | TASK_05 (main `bae1bc6`) | Real split tunneling: `PackageManager` launcher discovery behind `<queries>`, persisted package-only selection, ONLY_VPN→allowed / BYPASS→disallowed lists, stale filtering, connected rebuild via explicit reconnect + real consent | 55 JVM tests (18 new); API26 CI 40/40 instrumentation incl. device flow + 1.6× Arabic large text; lint 0 errors; captures 25–29 compared to reference `06-split-tunneling_c9265fd7.png` | Merged PR #5 (head `5cc503e` → `bae1bc6`), CI run `41889911474` |
| TASK_07–08 | Pending | NOT IMPLEMENTED | Not run | Not merged |
| TASK_09 | TASK_00 branch | Keystore-encrypted file/manual/Clipboard/QR-image imports | API26 file/QR/manual/tamper/concurrency verified; camera/visual gaps | Draft PR #2 |
| TASK_10 | TASK_00 branch | Persisted locale/theme, local account state | Implemented; new interactive preference test pending | Draft PR #2 |
| TASK_11–12 | TASK_00 branch | Available repair navigation, basic StatFs/cache measurements | Full ten-tool dashboard and shared-media categories NOT IMPLEMENTED | Draft PR #2 |
| TASK_13–15 | Pending | NOT IMPLEMENTED | Not run | Not merged |
| TASK_16 | TASK_00 branch | Scoped cache preview/confirm/remove/recheck/journal | API26 64-byte target removed, sentinels retained; full visual/interruption acceptance pending | Draft PR #2 |
| TASK_17–19 | Pending | NOT IMPLEMENTED | Not run | Not merged |
| TASK_20 | TASK_00 branch | Android network snapshot + consented HTTPS timing | API26 platform and HTTP200/634ms verified; full offline/handover/visual scenarios pending | Draft PR #2 |
| TASK_21 | Pending | NOT IMPLEMENTED | Not run | Not merged |

No entire task is declared complete. See [native services evidence](evidence/NATIVE_SERVICES_20261103.md), [TASK_00 evidence](evidence/TASK_00.md) and [blockers](STATUS_BLOCKERS.md). React prototypes remain functional inventory only.
