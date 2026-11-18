# Physical device verification — KX Private VPN

**Date:** 2026-11-18
**Branch at capture time:** `codex/release-artifact-and-supply-chain` (`495acdb`, based on `codex/recover-audit-fix-api26`)
**Purpose:** prove that the native Compose application starts, renders and navigates on a real
Android phone, and capture the runtime evidence that the API 26/API 35 emulator matrix cannot
produce (real glass rendering, real RTL layout, real system bars).

## Device

| Property | Value |
|----------|-------|
| Serial | `R9ZR20HWHQY` |
| Model | Samsung SM-A025F (`a02qnnxx`) |
| Android | 12 (`ro.build.version.release=12`, `ro.build.version.sdk=31`) |
| Display | 720x1600 physical, density 280 (360x800 dp) |
| Locale | `ar-EG,en-GB` — the phone runs the app in **Arabic RTL** by default |
| Pre-existing apps | The phone did **not** have `com.knoux.kxprivatevpn` installed; no other package was touched |

> Note: earlier notes in this repository called this device an "API 35 acceptance phone". The
> real value is **API 31 (Android 12)**; the API 35 coverage comes from the CI emulator matrix.

## Build under test

| Property | Value |
|----------|-------|
| Artifact | `android/app/build/outputs/apk/debug/app-debug.apk` (35,862,401 bytes) |
| SHA-256 | `3274E4C0D4B1750CCC8F289B63EC44429BEEECD077E8107708592AD0FB3CA01A` |
| Build type | `debug` (unsigned release artefacts are never installed on a device) |
| Local proof | `:app:assembleDebug :app:testDebugUnitTest` → BUILD SUCCESSFUL (9m42s) |

## Commands executed

```text
adb install -r -g android/app/build/outputs/apk/debug/app-debug.apk     # Success
adb shell am start -n com.knoux.kxprivatevpn/.MainActivity             # Starting: Intent { cmp=com.knoux.kxprivatevpn/.MainActivity }
adb exec-out screencap -p > <capture>.png
```

No application data was deleted, no other package was modified and no device file was removed.

## Captures

| File | What it shows |
|------|---------------|
| `phone-01s.png`, `phone-03s.png`, `phone-06s.png`, `phone-12s.png` | The first-run identity screen: the native glass orb with the owner KX artwork, the ribbons, the `KX / PRIVATE VPN` wordmark and the honest prompt «اضغط على شعار KX للدخول» / *tap the KX logo to enter*. The screen is stable across the capture window, so the cinematic intro hands over to the identity screen instead of flickering. |
| `phone-after-orb-tap.png` | **Disconnected Home** (TASK_02) in Arabic RTL: settings entry, brand lockup, orb with the «غير متصل» / *not connected* pill, the honest empty state «استورد إعداد WireGuard للتوصيل.» / *import a WireGuard config to connect*, the single gradient tunnel action, the status card whose three measured values are all `—` (never invented), the four quick-action cards, the empty recent-activity panel and the five-tab bottom bar. |
| `phone-tools.png` | **Statistics** (TASK_08): measured session counts, connection time, the usage chart, quick stats and the recent-activity list — all values come from the real usage store, the empty case is stated. |
| `phone-tools-catalog.png` | **Settings** (TASK_10) in Arabic RTL: the Pro panel that states plainly that no profile is stored and that KX Pro is optional, the language chooser (system / English / العربية) and the theme chooser (system / night / day). No fake account, no fake entitlement. |

## What this proves — and what it does not

**Proves**

- The native Compose app installs, starts and navigates on a physical Android phone.
- The splash/identity experience renders the owner glass-orb artwork with the real compositor.
- Arabic RTL layout, theming and the bottom navigation work on a real device.
- The disconnected state is honest: no profile means no connection and no invented measurements.

**Does not prove**

- No WireGuard profile was imported, therefore **no tunnel, no handshake and no traffic** are
  proven by this capture. `E2E_REAL_TUNNEL` stays `EXTERNAL_BLOCKED` until an authorized gateway
  is provisioned.
- No Pro entitlement was granted; the Pro surface only shows its honest "not activated" state.
- This is a `debug` artifact. The release keystore and the signed AAB remain owner-gated.
