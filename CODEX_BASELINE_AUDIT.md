## Latest owner visual override — HIGHEST PRIORITY
The owner has explicitly REJECTED treating Gemini's React styling as the visual standard. The newly provided owner images and the numbered canonical PNG references are ABSOLUTE DESIGN AUTHORITY. Build the Kotlin Android screens to MATCH those images, not to preserve current Gemini layout or colors. Gemini is a functional/navigation inventory only. The brand Splash must be exceptional and cinematic, using native Compose glass/ribbon/orb motion. Read CODEX_REFERENCE_VISUAL_CONTRACT.md before changing any UI.
# KX PRIVATE VPN — ACTUAL INTAKE AUDIT
Date: 2026-11-03 (UAE). Evidence comes from inspecting actual files and running commands in the newly cloned workspace, **not** from the previous KX repo.

## Identity and baseline
- Canonical repo: https://github.com/daynightae-cmyk/KX-Private-VPN
- Local checkout: `K:\KX-Private-VPN`; clone created after verifying that the destination did not already exist.
- Remote: `origin https://github.com/daynightae-cmyk/KX-Private-VPN.git`
- Observed HEAD/main/origin/main: `37c5d8778ccdcd03dd78fca71ddcdc3d3b6df9d4`
- Last observed commits: `37c5d87 style: update AI studio asset thumbnails`, `4ed52b8 Initial commit`.
- Repository was clean after cloning and after running the web build. This is **not** `daynightae-cmyk/KX`; do not mutate or auto-merge that other active Android repo.

## Executed verification — exact results
- `bun install --frozen-lockfile`: successful, 180 packages installed, ~60.92 s. Dependencies were installed into the local checkout; no tracked changes.
- `bun run lint` -> `tsc --noEmit`: EXIT 0.
- `bun run build` -> `vite build`: EXIT 0, 1,723 modules transformed, ~1.48 s, `dist/index.html`, CSS 211.70 kB, JS 955.94 kB.
- Vite warned `vite.config.ts:11:27` uses `__dirname`, unsupported by future native configLoader; fix safely if web preview is retained.
- `git status --porcelain=v1 --branch`: `## main...origin/main`, no tracked/untracked source changes after building.
- These results prove **web typechecking and bundling only**. Android/Kotlin compile, actual APK, VPN tunnel, file deletion, emulator/device UI, network-leak tests, and pixel-level screenshot fidelity were NOT executed/verified.

## What exists (valuable assets to retain)
- Web stack: React 19, TypeScript 7, Vite 8, Tailwind 4, Motion, Lucide, QR import UI, Google GenAI dependency.
- 11 VPN visual/reference PNGs in `public/assets/aistudio/`, numbered 00..10, including circular KX orb brand.
- 11 React VPN screens in `src/components/screens/Screen00Splash.tsx` ... `Screen10ProfileSettings.tsx`.
- Phone Repair Lab dashboard `src/components/screens/repair/Screen11RepairLab.tsx` and 10 dedicated React service screens `Screen12StorageXRay.tsx` through `Screen21RepairAutopilot.tsx`; `RepairServiceModal.tsx` has service-specific controls.
- Supporting `src/App.tsx`, `src/components/common/AndroidBottomNav.tsx`, `src/components/common/KXGlassOrb.tsx`, `src/i18n/translations.ts`, `src/index.css`, typed VPN and repair models, `src/utils/config-parser.ts`, `src/utils/repair-engine.ts`, and identity page/assets.
- Repair is present as a top-level specially accented tab (six tabs total: Home, Servers, Repair, Security, Stats, Settings). Preserve its prominence, but solve tap-target/overflow concerns in native Compose.
- Source includes DeviceFrame, side-by-side reference compare and AuditLedgerModal — useful **design studio / QA tools**, not necessarily production mobile UI.
- The owner will additionally provide screenshot references to Codex. Never disregard either set; check hashes and content before reconstruction.

## Proven non-production / simulated behaviors — highest priority
1. `src/context/VPNContext.tsx`: `initialServers` contains seven hardcoded country endpoints/pings/load/IP addresses. `initialImports` and `initialHistory` are prefilled fictional; `initialSplitApps` has hardcoded Android apps not discovered from PackageManager.
2. Same file: `sessionStats` initialized to arbitrary durations/bytes, 92.4/46.8 Mbps, 28 ms and a sample assigned IP; `securitySettings` says kill switch/DNS/IPv6/tracker blocking/always-on are enabled without Android proof.
3. Same file: `toggleConnect` / `disconnectNow` change connected state after `setTimeout`. Connected speed is sinusoidally animated, not measured transport.
4. `src/hooks/useRepairStats.ts`: explicitly **emulated** StatFs/StorageStatsManager. 4 KB block size, ~9.8 GiB reserved and 8% availability discount are hardcoded; `refreshStats` replays same artificial storage data after 600ms.
5. `src/utils/repair-engine.ts`: `getInitialStorageStats` invents 128 GiB capacity, 84.6 GiB usage, category sizes and file counts. `getInitialGhostCandidates`, duplicate groups and other getInitial* data contain fabricated paths/amounts/scores. These are **design fixtures**, not filesystem inspection.
6. `src/components/screens/repair/RepairServiceModal.tsx`: Ghost, cache and duplicate cleanup and Repair Autopilot use timers, remove entries from in-memory arrays, calculate pretend reclaimed bytes and show success notices; no Android filesystem mutation is evidenced.
7. `src/components/screens/repair/Screen11RepairLab.tsx`: "Analyze My Phone" scan progress increments by timer; action completion subtracts fictional bytes from state and creates a log marked reversible without a real undo contract.
8. `src/components/screens/repair/Screen20NetworkDoctor.tsx`: probe latency uses `Math.random()`, not ConnectivityManager/real bounded probes.
9. `src/components/screens/repair/Screen21RepairAutopilot.tsx`: execution runs an interval, marks completion without real operation receipts.
10. Browser UI builds do not grant Android scoped-storage access, VpnService permission, per-app routing, Android Keystore or native lifecycle. No `android/`, Gradle wrapper, Kotlin sources or native APK were observed.

## Recommendation: migration, not demolition
Use this Git repo as the canonical project. Preserve original React/Vite sources as a functional/history inventory only, while treating the OWNER PNG REFERENCES (not Gemini styling) as the immutable visual design authority while creating a genuine Kotlin/Jetpack Compose Android client in a **new isolated `android/` subproject** (or reviewed native project arrangement) inside the same repo. Do not substitute an Android WebView shell and call it Kotlin native.

First anchor a screenshot/route/feature inventory of every React view and interactive state. REPLACE Gemini visual components; reproduce the owner reference orb/ribbon/glass geometry, layout, palette and motion in native Compose. Reuse only legitimate functional inventory, navigation ideas and vetted bilingual strings. The design references are target assets, not interactive screenshots to paste as background.

Migrate one complete tested vertical slice at a time, including real Android APIs, secure data, user-controlled permissions, cancellation/failures, locale/RTL, screenshots, target test and PR. Leave external-key/provider-dependent scope disabled and documented. Do not merge app changes into the **other** `daynightae-cmyk/KX` repository.

## Android repair boundaries
- `Storage X-Ray`: real StatFs + MediaStore + SAF with clearly identified coverage.
- `Ghost Hunter`: only permitted scope; no automatic "junk" classifier based on filename/age.
- `Duplicate DNA`: streaming SHA-256 exact matches; optional perceptual near-duplicate clearly separate.
- `Heavyweight Finder`: scanned accessible files; no private Android/data fishing.
- `Cache Surgeon`: own app cache and Android user-mediated management; cannot erase unrelated apps' private cache.
- `Media Slim`: actual transcoding, verify output and preserve originals by default.
- `Download Rescue`: validate supported formats, not generalized repair claims.
- `Session Shield`: KX-owned session/config data only; no third-party session termination.
- `Network Doctor`: connectivity state + real DNS/authorized test-endpoint latency; no random values.
- `Repair Autopilot`: scan → explain → preview → explicit approve → execute → verify → report. No timed pretend cleanup; honor cancellations and preserve user data.
- External provider keys/gateway/Play signing should be deferred with a real blocker ledger, not fabricated or used to pause independent work.
- Android privacy, restricted permissions and Google Play policies must be reviewed using current official documentation during coding.

## Status summary / next safe action
VERIFIED: repo identity, local clone, SHA, React source/routes, 11 PNGs, repair service designs, web TypeScript compile and Vite build.
NOT VERIFIED: pixel-perfect reference match on real running device, Android Kotlin build, VPN handshake/network protection, actual file scan/deletion, Google Play release.
NEXT: open `CODEX_KX_PRIVATE_VPN_MASTER_MISSION.md` and execute the first safe native baseline task. Track progress and tests honestly; do not rush unsafe merges.
