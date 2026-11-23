# LAN WireGuard E2E — VERIFIED (2026-11-23)

Real end-to-end proof: KX Private VPN app on the owner's physical phone
(Samsung SM-A025F, Android 12 / One UI, serial `[SERIAL]`, Wi-Fi 192.168.70.109)
connected through the real Android VPN consent dialog to the running Windows
WireGuard server `KX-Win-LAN-Server` (192.168.70.103:51820, tunnel IP 10.253.64.1).

Scope: **LAN-only tunnel** (client AllowedIPs = 10.253.64.1/32). Internet
full-tunnel is NOT-IMPLEMENTED by design and is NOT claimed here.

## Method (no secrets, no config changes)

- Existing paired configs only. No key generation/rotation for the tunnel, no
  server service restart, no firewall/NAT/route changes.
- Client profile delivered privately: `adb push` to `/sdcard/Download`, imported
  through the app's real SAF/QR import flow. QR image and all pushed copies were
  deleted from the phone afterwards (verified `ls` = No such file).
- All `wg show` captures taken elevated (read-only) on the server; the peer
  public key is replaced with `[REDACTED-PEER-PUBKEY]` in every committed file.
- App-side logcat filtered to the app PID, keys redacted (`[REDACTED-KEY]`),
  serial masked. GoBackend already logs peers by 8-char prefix only.

## Timeline (host clock UTC+4; epoch handshakes from `wg show`)

| Step | Evidence | Result |
|---|---|---|
| Baseline (before any client) | `wg-*-before.txt` | latest-handshake `0`, transfer `0 0`, endpoint `(none)` |
| Real consent dialog (`com.android.vpndialogs` «طلب الاتصال») confirmed on the actual system dialog — no appops/adb grant bypass | `logcat-e2e-redacted.txt` 22:46:51 | `Bringing tunnel kx-private UP` |
| First handshake | logcat 22:46:52.255 → 22:46:52.299 | initiation sent → **response received (44 ms)** |
| Server view t0 | `wg-hs-t0.txt`, `wg-tx-t0.txt` | handshake `1795459608`, transfer `1556 156` |
| Server view after keepalives (22:48:56) | `wg-*-after1.txt` | handshake `1795459735` (fresh rekey), transfer `1768 248`, endpoint `192.168.70.109:54476` |
| Tunnel-only traffic: TCP connect probes from phone to 10.253.64.1 ports 445/135/3389 via `nc` (all rc=0, real bidirectional sessions). ICMP through the tunnel is dropped by Windows firewall — per directive, TCP app-level probes are the proof | shell rc=0 ×3 + 8× payload probes | server RX grows |
| Server view 22:51:14 | `wg-*-after2.txt` | handshake `1795459856`, transfer **`14268 436`** (RX 1768 → 14268) |
| App connected screenshot | `connected-state.png` | «متصل», الخادم النشط KX-LAN-QA, المستلم ٢٤٨ بايت — matches server TX 248 at after1 |
| Disconnect via app button | `disconnected-state.png`, logcat 22:52:09.985 | `Bringing tunnel kx-private DOWN`, `tun0` gone (`Device "tun0" does not exist`), app «غير متصل», counters reset to — |
| Reconnect via app button | `reconnected-state.png`, logcat 22:52:38 | new tunnel UP, **new handshake** response 22:52:38.621 (Android remembers per-app consent; no bypass) |
| Server view 22:53:22 | `wg-*-after3.txt` | handshake `1795459954`, transfer `16432 592`, endpoint port changed `54476 → 48005` (new session socket) |
| Final disconnect + cleanup | shell | `tun0` gone, `svc power stayon false` restored, server services untouched (`WireGuardManager` + `WireGuardTunnel$KX-Win-LAN-Server` Running/Automatic) |

`wg transfer` columns are server-perspective `received sent` (RX from phone,
TX to phone). App «المستلم» (received) tracks server TX — consistent at both
checkpoints (248 B app ↔ 248 server TX).

## Installed build under test

- Package `com.knoux.kxprivatevpn` 0.1.0, installed from merged main `8128048`
  lineage (post-PR #55 animated glass build, lastUpdateTime 2026-11-22 22:48:58).
- Installed base.apk SHA256:
  `2256EF98DF44AE5AD51344BE4EFB2130B27AA90A5FD1AA029C4BD21D5339E60D`
  (debug-signed device build — NOT a production artifact).

## Honest limits

- LAN-only profile: no internet egress claim.
- ICMP inside the tunnel is firewalled on Windows; TCP application-level
  sessions are the traffic proof.
- The first VPN consent was confirmed by tapping the real system dialog
  (uiautomator-located «موافق»); `VpnService.prepare()` ran normally. Reconnect
  reuses Android's remembered per-app consent, which is platform behavior.