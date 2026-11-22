# KX Private VPN — Physical Device Verification

**Date:** 2026-11-22
**Device:** Samsung SM-A025F (`a02q`), serial `R9ZR20HWHQY`
**OS:** Android 12 (API 31), ar-EG locale, 720x1600 @ 280 dpi
**App:** `com.knoux.kxprivatevpn`, versionName `0.1.0`, versionCode `1` (lastUpdateTime 2026-11-19)
**Branch under test:** `codex/visual-elevation-and-cleanup-20261119` @ `6640da4` (installed build predates this session)

Captures are real `adb exec-out screencap` frames on the physical phone. **No capture proves a
VPN connection, because no tunnel was established in this session.**

---

## 1. Package state finding (device-side, not an app defect)

`dumpsys package com.knoux.kxprivatevpn` reported `User 0: installed=true ... enabled=3 stopped=true`.
The launcher activity is declared (`Activity Resolver Table: com.knoux.kxprivatevpn/.MainActivity`,
`android.intent.action.MAIN` + `android.intent.category.LAUNCHER`), yet:

- `am start -n com.knoux.kxprivatevpn/.MainActivity` → `Error: Activity class ... does not exist`
- `cmd package resolve-activity -c android.intent.category.LAUNCHER com.knoux.kxprivatevpn` → `No activity found`
- `monkey -p com.knoux.kxprivatevpn -c android.intent.category.LAUNCHER 1` → `No activities found to run`

After `adb shell pm enable com.knoux.kxprivatevpn` (new state: **enabled**), the launcher activity
starts normally (`LaunchState: COLD`, `mCurrentFocus=.../com.knoux.kxprivatevpn.MainActivity`).

This is a device package state (e.g. Samsung unused-app auto-disable, or a prior test harness),
**not a source defect** — the manifest and resolver entry are intact. It should be re-checked on
the owner's daily-use device.

## 2. Entry gate — no automatic navigation (verified)

| Time (approx) | State | Evidence |
|---|---|---|
| t≈2 s | Branded intro: KX glass emblem + `PRIVATE KX` / `اضغط على شعار KX للدخول` + skip | `02-intro-emblem-t2.png` |
| t≈16 s, **no tap** | Same intro still displayed (uiautomator node texts unchanged) | `03-entry-gate-holds-t16.png` |

The Home screen does **not** appear without an explicit emblem tap, matching
`StartupTest.realLauncherKeepsHomeAbsentThroughoutNormalSpin` and `PhysicalEntryTest`
("no automatic navigation after the intro"). An earlier observation of Home appearing after a tap
was a delayed input event, not auto-entry — re-tested with no input and the gate held.

## 3. Home — reference 14 composition, honest disconnected state

After the emblem tap, Home renders (`01-home-ar-disconnected.png`):

- Header: gear (settings) + `PRIVATE VPN` / `KX` lockup with the emblemed orb.
- Title `الرئيسية` + subtitle `خصوصية بالتصميم. كل قيمة مقاسة على هذا الجهاز.`
- Hero: glass emblem with the OrbitalHalo ring; status pill **`غير متصل`** (not connected).
- Primary CTA `اتصال النفق` (Connect tunnel) — gradient, power glyph.
- Session panel: `الخادم المحدد = مفيش إعدادات محفوظة`, `المستلم = —`, `المدة = —`
  (em dashes = unmeasured, **nothing invented**).
- Quick actions: `اختيار الخادم`, `التوجيه الذكي`, `حماية التطبيقات`, `مفاتيح الاتصال`.
- Recent activity + 5-tab bottom nav: `الرئيسية / الخوادم / الأمان / الإحصاءات / الإعدادات`
  (matches reference 14's tab set; the reference-18 "Tools tab" conflict is resolved in favour of 14).

## 4. Split tunneling — real data, honest scope banner

`04-split-tunnel-ar.png`: lists real installed launcher apps with real icons (BluSecure VPN,
Browser, CamScanner, Chrome, …) and an honest empty-selection banner:
`مفيش تطبيقات مختارة. لحد ما تختار، النفق بيمرّر كل التطبيقات.`

## 5. What was NOT tested (blockers)

- **No VPN connect/consent/handshake/RX-TX run.** There is no authorized WireGuard endpoint with
  credentials available to this session, so `E2E_REAL_TUNNEL` stays `EXTERNAL_BLOCKED`.
- No network-change / reconnect test (requires a live tunnel).
- No release-signed artifact (no owner keystore).

## 6. Evidence integrity (SHA-256)

| File | Bytes | SHA-256 |
|---|---|---|
| `01-home-ar-disconnected.png` | 1 046 362 | `3E10B99611835FF0B963D1661CBC8EC09326888969B88B601ADAFF82840BFC3D` |
| `02-intro-emblem-t2.png` | 713 222 | `2335240610416A20F7EC103C18919287D3CA3351A6D0F757F02272CFFE210903` |
| `03-entry-gate-holds-t16.png` | 1 063 276 | `7435A9E560D3834CDA7E9B4F5497201603B617CC3BA2F40A0C19FC11D74CF2F7` |
| `04-split-tunnel-ar.png` | 1 121 185 | `9C6D6B7CE35030EAB04488B0006A811D3977E384C110E124C3282669A42B7AD0` |

All four are distinct frames. These are runtime captures of the installed build; they are not proof
of VPN protection, and the connected-state reference (14) is deliberately **not** reproduced as a
live claim.
