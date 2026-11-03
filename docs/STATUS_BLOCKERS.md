# KX Private VPN — current blockers

## TASK_00 — PARTIALLY VERIFIED (2026-11-03)

- Local debug APK and Android test APK build; three JVM tests pass; lint passes with dependency/icon warnings.
- Local emulator initially failed because K: could not accommodate its userdata partition. A separate project-owned AVD was created under `D:/Android/kx-private-avd` without touching other AVDs.
- That AVD cannot boot with acceleration: emulator reports `Android Emulator hypervisor driver is not installed on this machine`. A software emulation launch attempt was rejected by automatic approval review (`blocked by policy`); it was not executed.
- API 35 image installation into K: failed for insufficient disk space. Existing SDK/system images and other project files were preserved.
- CI includes API 26/35 instrumented tests and native frame captures. Until these run and the frames are inspected, native runtime/visual acceptance remain UNVERIFIED; do not merge.
- Final orb uses the byte-preserved owner artwork with native Canvas formation, particles, ribbons, refraction gradients, satellites, caustic sweep and pulse. This is a hybrid brand reveal, not an all-vector recreation. Fidelity of the animated-to-original transition must be reviewed on device.
- Full Welcome onboarding (TASK_01), VPN transport/configuration and all Repair services (TASK_02–21) are NOT IMPLEMENTED in Android. The identity preview explicitly discloses this and requests no network/storage/VPN permissions.
- No owned WireGuard test gateway, provisioning provider, release signing or Play Console credentials have been verified. These do not block independent native UI work, but block transport/release claims.
