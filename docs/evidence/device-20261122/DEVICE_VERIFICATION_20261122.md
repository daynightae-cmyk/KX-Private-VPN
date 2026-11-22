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

## 4b. Tools catalog (reference 18) and Repair Lab (reference 03)

- `05-tools-catalog-ar.png` — `لوحة الأدوات`: emblemed hero + search field + five filter chips
  (`الكل (9)` selected) + the nine owner-approved cards in the exact reference-18 RTL order
  (temporary, duplicates), (large, download), (media, smart repair), (apps, network), then the
  full-width storage-health card. Bottom nav highlights `الأدوات`.
- `06-repair-lab-ar.png` — `لوحة أدوات الإصلاح`: large emblemed orb with the orbital ring + tap hint
  (`اضغط على شعار KX لبدء عملية الفحص`), the eight service cards in reference-03 order, and the
  full-width `تشغيل إصلاح شامل` CTA. Bottom nav highlights `الأدوات`.

### Reference comparison (side-by-side, honest)

Composite comparisons are in `compare/` (reference on the left, runtime on the right, both scaled to
a common height):

- `compare/ref14-vs-runtime-home.png` — reference 14 vs `01-home-ar-disconnected.png`.
  **Same information architecture**: header lockup (`PRIVATE VPN` / `KX`), hero orb + status pill,
  primary CTA, three-metric session panel, four quick-action cards, recent-activity list, five-tab
  nav. The runtime frame is the honest `غير متصل` state; the connected fixtures in reference 14
  (Frankfurt 42 ms, 368.4 MB, 00:42:18) are deliberately **not** reproduced.
- `compare/ref18-vs-runtime-tools.png` — reference 18 vs `05-tools-catalog-ar.png`. Same header,
  search field, chip row, nine-card grid and card order. The runtime hero uses the violet KX glass
  emblem; the reference uses a blue globe variant.
- `compare/ref03-vs-runtime-repair.png` — reference 03 vs `06-repair-lab-ar.png`. Same eight-card
  order and comprehensive CTA. The runtime orb adds an orbit ring + satellites.

### Documented deviations (NOT claimed as 100 % parity)

1. **Aspect / density.** Reference frames are 1080×1920 (16:9); the phone renders at 411 dp × 914 dp
   (720×1600 @ 280 dpi, 20:9). The taller runtime viewport shows *more* vertical content, so
   per-screen density differs even when the element order matches.
2. **Iconography.** Runtime uses line icons inside circular wells; the references use solid
   rounded-square icon tiles. Same glyph semantics, different rendering.
3. **Bottom-tab conflict.** Reference 14 highlights `الخوادم`, reference 18 highlights `الأدوات`
   (the conflict flagged in `REFERENCE_MAP.md`). Runtime resolves it per screen: Home shows
   `الخوادم`, Tools/Repair show `الأدوات`. No route was collapsed or deleted.

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
| `05-tools-catalog-ar.png` | 1 121 742 | `4F7BA2119BF088616E46DADB98C074DA84E1B3843FF09ECD0DFD847D1FFAF36F` |
| `06-repair-lab-ar.png` | 1 211 675 | `B968EAA1979FA66FEC3CC7F713CB00AB58199F66F9773E02C3DBAE649576BC27` |
| `compare/ref14-vs-runtime-home.png` | 628 511 | `5779F98B644E624DFEC24D1DADEE0B24C55B9B9537D81FDB2A4E8FB7C7745407` |
| `compare/ref18-vs-runtime-tools.png` | 650 264 | `456559A5786851B5805C5B639A424FF563CAE4FB435D78A9CE13E95AC1F14744` |
| `compare/ref03-vs-runtime-repair.png` | 699 206 | `F377F1F3CF6E054982C44CC5E67DFC2212368618B22DE6DBC68523AFFE2469F4` |

All eight captures are distinct frames. These are runtime captures of the installed build; they are not
proof of VPN protection, and the connected-state reference (14) is deliberately **not** reproduced as
a live claim.

The comparison composites are generated from `K:\KX_DYAD_VISUAL_HANDOFF_20261108\visual-lab\public\references`
(reference) and the runtime frames above; both sides are downscaled to a common height, so the
composites are evidence of layout/order parity, not of pixel dimensions.
