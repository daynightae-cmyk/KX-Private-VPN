# KX Private VPN — current blockers

## TASK_00 — PARTIALLY VERIFIED (2026-11-03)

- Local debug APK and Android test APK build; three JVM tests pass; lint passes with dependency/icon warnings.
- Local emulator initially failed because K: could not accommodate its userdata partition. A separate project-owned AVD was created under `D:/Android/kx-private-avd` without touching other AVDs.
- That AVD cannot boot with acceleration: emulator reports `Android Emulator hypervisor driver is not installed on this machine`. A software emulation launch attempt was rejected by automatic approval review (`blocked by policy`); it was not executed.
- API 35 image installation into K: failed for insufficient disk space. Existing SDK/system images and other project files were preserved.
- At `3f8d472f585eda6e799aecbc1b4e94601e2fb8a7`, all 7 API26 instrumentation tests and build CI passed. Arabic dialog capture inspected and correct after resolving strings before opening its separate window. API35 remains pending; earlier runs had the now-fixed Arabic failure.
- First real API26 Welcome PNG/video inspected at 6a15284: black orb fringe observed and corrected; full visual parity remains unaccepted. Default emulator was 320px wide; follow-up selects Nexus5 and records actual 360/411dp geometry.
- API26 software-GPU recording measured 87/87 janky frames (p50=77ms). 60fps is NOT VERIFIED and failed on that emulator. Background recomposition was reduced; new profiling and hardware evidence are needed. Do not merge this as completed TASK_00.
- Final orb uses the byte-preserved owner artwork with native Canvas formation, particles, ribbons, refraction gradients, satellites, caustic sweep and pulse. This is a hybrid brand reveal, not an all-vector recreation. Fidelity of the animated-to-original transition must be reviewed on device.
- Follow-up Draft PR #2 now implements Welcome/education routing, encrypted WireGuard profile import/management, locale/theme settings, basic StatFs, scoped confirmed KX-cache removal and real network diagnostics. At `95fa3d6`, build and all 15 API26 tests passed. These are narrower scopes, not completion of the 22 tasks. VPN transport, media inventory, most Repair tools and full visual acceptance remain NOT IMPLEMENTED/UNVERIFIED. Network permissions are declared for the explicit Network Doctor probe; no storage or VPN consent is requested by import.
- No owned WireGuard test gateway, provisioning provider, release signing or Play Console credentials have been verified. These do not block independent native UI work, but block transport/release claims.

## Environment repair update

- D: SDK/build-tools/platforms verified; SDK licenses accepted by official sdkmanager; escaped ignored local SDK path fixed. Local builds/lint/JVM and instrumentation APK compilation pass. No unrelated SDK/cache removed.
- Hypervisor feature inspection requires elevation; local accelerated runtime still BLOCKED. Firmware virtualization is already enabled. No system-wide changes or bypass attempted.
- API35 current-emulator ColorBuffer/device loss is reproducible. Every ADB command is now bounded; failed tests propagate their exit status. Diagnostics did not establish OOM. CI is retrying official archived Emulator 36.4.10 Stable/build15004761 with both API26/35 and unchanged behavioral assertions. Do not call the repair verified before the run succeeds.
- Details, real screenshots, source SHA, operations and next steps: [native services evidence](evidence/NATIVE_SERVICES_20261103.md).
