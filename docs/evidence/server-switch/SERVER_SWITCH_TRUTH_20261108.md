# Server Switch Truth — Evidence

Date: 2026-11-08
Repo: `K:\KX-Private-VPN` (canonical)
Base `main`: `8cb076a669639b6c18e7bb3e59b0a32047134af1`
Branch: `server-switch-truth`

## Defect reproduced before the fix

With a live WireGuard session on **Alpha**, selecting **Beta** in Servers only rewrote the
saved preference. Home then showed `Connected` + `SELECTED SERVER Beta`, while Session details
still reported the active session **Alpha**. The tunnel was never switched, so the UI claimed a
server that the running tunnel was not using.

Evidence from the live API26 audit:
`D:\Temp\opencode\home-beta-selected.png`, `ui-home-beta-selected.xml`, `ui-session.xml`.

## Fix

A pure `VpnSessionOwnership` policy now defines when a session may be presented as the live
server and when selection must be frozen:

| Phase / issue | Owns tunnel | Active server | Selection locked |
|---|---|---|---|
| `IDLE` | no | none | no |
| `CONNECTING` | yes (session present) | session profile | **yes** |
| `CONNECTED` | yes | session profile | no (different server requires a confirmed stop) |
| `DISCONNECTING` | yes | session profile | **yes** |
| `ERROR + STOP_FAILED` | yes | session profile | **yes** |
| any other `ERROR` | no | none | no |

Behavior:

- Home labels the live session server as **Active server** while a tunnel is owned and shows an
  em dash for counters that no owned backend produced.
- Servers shows an **Active tunnel: name** banner, an **Active** pill on the real session
  profile, and never presents the stored selection as the active one.
- Choosing a different server while `CONNECTED` opens a confirmation that states the current
  tunnel will be disconnected and the chosen server becomes selected for the next connection.
  Only after confirmation does `ClientViewModel` call `VpnController.stopNow()` and then persist
  the new selection.
- If the stop fails, the saved server is not changed and the unresolved `STOP_FAILED` state
  offers a real retry; selection stays locked.
- During `CONNECTING`/`DISCONNECTING` the cards and removal actions are disabled with explicit
  copy explaining why.
- Removing the active profile warns that the tunnel will be disconnected first.
- A final guard refuses to persist a different server if a concurrent notification action leaves
  any tunnel owning the backend.

## Local gates

| Gate | Result |
|---|---|
| JVM `:app:testDebugUnitTest` | **337 tests, 0 failures** — includes 10 new `VpnSessionOwnershipTest` cases |
| Lint `:app:lintDebug` | **0 errors, 144 warnings** |
| API26 instrumentation `HomeScreenTest` + `ServerSelectionScreenTest` | **23/23 passed in 155.8s** |
| APK compile | `:app:assembleDebug` + `:app:assembleDebugAndroidTest` successful |

## Runtime evidence (API26 emulator, dummy localhost profiles)

All captures use the owner-approved app build and test profiles **Alpha** (`127.0.0.1:51820`)
and **Beta** (`127.0.0.1:51821`). No physical phone was touched.

| Evidence | What it proves |
|---|---|
| `01-home-active-server-en.png` | Home shows **ACTIVE SERVER Beta** while connected; the saved selection is not presented as active. |
| `02-servers-active-banner-en.png` | Servers shows **Active tunnel: Beta**, an **Active** pill on Beta, and a switch action on Alpha. |
| `03-switch-confirm-en.png` | The confirmation states that Beta will be disconnected and Alpha becomes selected for the next connection. |
| `04-home-after-switch-en.png` | After confirming, the tunnel is really stopped (no `VpnSessionService`) and Home shows **Disconnected** with **SELECTED SERVER Alpha**. |
| `05-servers-active-banner-ar.png` | Arabic RTL active banner and active pill render with the same truth. |
| `06-switch-confirm-ar.png` | Arabic RTL confirmation keeps both profile names readable and the consequences explicit. |

The English live flow verified confirm: Beta → stop → Alpha selected → disconnected.
The Arabic live flow verified confirm: Alpha → stop → Beta selected → disconnected.
The cancel path (no selection change) is covered by
`ServerSelectionScreenTest.connectedTunnelShowsActiveServerAndConfirmsSwitch`.

`VpnSessionService` was checked through `dumpsys activity services` before and after; it is absent
after the switch. The tunnel uses a localhost dummy profile, so this proves real backend ownership
and shutdown, not public WireGuard handshake or internet egress.

## Emulator state restored

- App language/theme: **System/System**
- Density: **320**
- Font scale: **1.0**
- No VPN session service left running
