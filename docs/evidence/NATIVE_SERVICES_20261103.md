# Native services continuation — actual evidence

## Reality / preservation

Resumed from `57d31f28c22412fa5d99096e86d0df8c8fe3c562`; fresh remote main remained `37c5d8778ccdcd03dd78fca71ddcdc3d3b6df9d4`. Original React/reference assets and Draft PR #1 retained. Historical downloaded CI reports remain untracked. No cleanup/reset/history rewrite or other-repository change.

Continuation: `codex/native-onboarding-config`, [Draft PR #2](https://github.com/daynightae-cmyk/KX-Private-VPN/pull/2), based on PR #1's branch. This is an explicit dependency, not an independent main merge or acceptance of the splash.

## Implemented scope

- `launch/KxLaunch.kt`, `LaunchPreferences.kt`: actual Welcome → education → Home navigation, onboarding and locale/theme persistence. Activity Result owner explicitly survives the localized context, fixing the device-tested picker registration crash. Per-route composition now resets the Import scroll offset before showing Home.
- `data/config/`: vetted WireGuard `Config.parse`, usable mobile-client checks, 64KiB stream limit, safe errors, bounded unique names, control/bidi rejection. All profile content and metadata use Android Keystore AES-256-GCM and atomic no-backup storage. Serialized operations across instances; corrupt/missing-key data preserved, never silently replaced.
- `features/NativeClient.kt`, `ClientViewModel.kt`: SAF file reader, explicit Clipboard read, masked in-memory manual entry, bounded selected-image QR decoding, real profile selection and confirmed removal. Secrets do not enter savedInstanceState/navigation/logs/backups; import applies FLAG_SECURE. Live camera not implemented. Invalid entry remains editable in memory. No invented server fleet.
- Native Home remains honestly disconnected. WireGuard tunnel service is removed from the manifest in this slice. Import/selection cannot activate protection. Account/subscription labels are truthful local-device state.
- Basic Storage screen uses actual StatFs volume total/available, cancellable KX cache-byte measurements and official Android settings navigation. Full shared-media categories are not implemented.
- `platform/storage/CacheMaintenance.kt`, `features/CacheScreen.kt`: KX-cache-only preview, empty initial selection, explicit confirmation, path/symlink boundary checks, size/mtime/SHA-256 recheck, exact selected-file deletion and absence verification. Atomic last-operation journal records verified progress and incomplete status; no auto-retry or fake undo. Reports file-content bytes, not a fabricated physical free-space increase. The 500-candidate limit is disclosed.
- `platform/network/NetworkDoctor.kt`, `NetworkScreen.kt`: real ConnectivityManager capabilities/transports/DNS callbacks. Android validation is distinct from protection. One user-consented HTTPS HEAD request, no credential/query URL, no redirects, socket timeouts, coroutine deadline and bounded worker/queue. Measures request duration, not ping or VPN speed. No automatic external probe.

New strings have EN/AR resources; native controls use RTL/Day/Night, 48dp+ targets, labelled radio/checkbox rows and scrolling. Manual TalkBack and complete owner visual acceptance remain unverified. These are narrower service slices, not completed TASK_00–21.

## Executed verification

Application SHA `95fa3d69bd53574eeebf1c1d15547d49be4c7bf3`, [CI 41671982562](https://github.com/daynightae-cmyk/KX-Private-VPN/actions/runs/41671982562): **build SUCCESS; API26 SUCCESS, all 15 instrumentation tests completed, zero skipped/failed. API35 FAILED after emulator display/device loss. Aggregate CI is not successful.**

- Device proof: ciphertext persistence and no plaintext key/name; duplicate/tamper no-overwrite; concurrent stores; file and QR-image import; missing input error; manual import UI and Activity recreation; truthful disconnected state; selected cache fixture deletion with sentinels retained; actual Android network capabilities; one approved HTTPS request; existing animation/RTL/large-font/reduced-motion cases.
- Actual HTTPS request to owner repository: **HTTP 200, 634ms**. [Raw result](ci-95fa3d6-api26/device-screenshots/identity-evidence/10-network-probe.txt). This is not a VPN/gateway or production latency claim.
- Cache proof: **one 64-byte fixture removed after confirmation**, unselected cache and private no-backup sentinels retained. [Native receipt](ci-95fa3d6-api26/device-screenshots/identity-evidence/09-cache-verified-receipt.png) inspected individually.
- [Native Home after import](ci-95fa3d6-api26/device-screenshots/identity-evidence/08-native-home-imported.png) inspected; its clipped header exposed cross-route scroll retention, subsequently corrected. Optical/reference fidelity remains unaccepted.
- Local `gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest`: exit 0 after route/glass changes, 80 tasks, 26s. **12 JVM tests passed**: timeline 3, config/input 4, cache safety 3, network consent/URL 2.
- A further interactive app-language/theme/recreation test and stronger receipt assertion were added after that CI result. Not counted in the 15; next device run should have 16 tests.

## Environment and CI repairs

- D: SDK inspected: platform 36, build-tools 35/36, platform-tools, emulator and API26 image present. Ignored local SDK configuration uses escaped `sdk.dir=D\:/Android/sdk`. Official `sdkmanager.bat --licenses`: **All SDK package licenses accepted**. XML-version warning from older command-line tools remains, but builds succeed. No unrelated SDK/cache removed.
- Firmware virtualization/SLAT true; `HypervisorPresent=False`; emulator reports missing acceleration driver. Windows Hypervisor Platform inspection returned **requires elevation**. No host feature/driver/BIOS/security change, reboot or software-emulation bypass. Local runtime remains BLOCKED; GitHub emulators are the authorized alternative.
- API35 lost ColorBuffers then ADB. Unbounded logcat caused hanging jobs. ADB/install/tests/recording now have deadlines and diagnostics; superseded runs cancelled. Failed tests retain nonzero exit.
- Host diagnostics: ~16GiB RAM, ~12GiB available, no kernel OOM entry. Memory exhaustion was not established. Gradle's lower memory/concurrency is a resource bound, not a claimed root-cause fix.
- API26 cannot capture dialog roots via PixelCopy below API28: use the supported full-window path there, dialog-root capture on newer APIs. Locale/visibility assertions unchanged. Current emulator still crashed on API35.
- CI now pins Google-archived **36.4.10 Stable, build 15004761**; both APIs/assertions retained. [Official archive](https://developer.android.com/studio/emulator_archive), Linux SHA256 `58bfdf0572f06ec007a7287154cdc6b2ebfea947d07a1e26a0f6b5e1a39e9191`. Pinned rerun pending; not claimed repaired.

## Delivery / continuation

Main unchanged; neither PR merged. No signed release, deployment, physical-phone installation or production tunnel. Preserve both Drafts until their gates pass. Latest exact branch head/CI must be rechecked before integration.

Remaining: splash visual/performance, full reference parity, API35 stability, live camera QR, first-install education UI acceptance, real WireGuard consent/foreground/handshake/routing/DNS/IPv6, media inventory and remaining repair tools, release/installed-device acceptance.

Next: inspect bounded pinned-emulator results and new captures, fix actual failures, isolate verified merge scope. Do not merge unfinished transport or label an entire task complete from these narrower results.
