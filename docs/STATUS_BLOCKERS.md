# KX Private VPN — current blockers

## TASK_00 — PARTIALLY VERIFIED (2026-11-03)

- Local debug APK and Android test APK build; three JVM tests pass; lint passes with dependency/icon warnings.
- Local emulator initially failed because K: could not accommodate its userdata partition. A separate project-owned AVD was created under `D:/Android/kx-private-avd` without touching other AVDs.
- That AVD cannot boot with acceleration: emulator reports `Android Emulator hypervisor driver is not installed on this machine`. A software emulation launch attempt was rejected by automatic approval review (`blocked by policy`); it was not executed.
- API 35 image installation into K: failed for insufficient disk space. Existing SDK/system images and other project files were preserved.
- CI ran real API26/35 tests. At f376e4a six of seven passed on each API; Arabic large-font disclosure failed. Added scrolling and awaiting rerun. Startup/recreation and earlier skip-clock checks passed.
- First real API26 Welcome PNG/video inspected at 6a15284: black orb fringe observed and corrected; full visual parity remains unaccepted. Default emulator was 320px wide; follow-up selects Nexus5 and records actual 360/411dp geometry.
- API26 software-GPU recording measured 87/87 janky frames (p50=77ms). 60fps is NOT VERIFIED and failed on that emulator. Background recomposition was reduced; new profiling and hardware evidence are needed. Do not merge this as completed TASK_00.
- Final orb uses the byte-preserved owner artwork with native Canvas formation, particles, ribbons, refraction gradients, satellites, caustic sweep and pulse. This is a hybrid brand reveal, not an all-vector recreation. Fidelity of the animated-to-original transition must be reviewed on device.
- Full Welcome onboarding (TASK_01), VPN transport/configuration and all Repair services (TASK_02–21) are NOT IMPLEMENTED in Android. The identity preview explicitly discloses this and requests no network/storage/VPN permissions.
- No owned WireGuard test gateway, provisioning provider, release signing or Play Console credentials have been verified. These do not block independent native UI work, but block transport/release claims.
