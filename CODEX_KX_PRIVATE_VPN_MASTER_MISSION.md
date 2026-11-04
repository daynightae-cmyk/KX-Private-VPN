# KX PRIVATE VPN — CODEX AUTONOMOUS NATIVE ANDROID DELIVERY ORDER
## FINAL OWNER DIRECTIVE — VISUAL-REFERENCE SUPREMACY, REAL SERVICES, CONTINUOUS EXECUTION

**DO THE WORK, NOT JUST THE PLANNING.** This order is for Codex running in the active local coding environment, with authorized access to the user's own checkout. Execute directly inside the canonical repo, preserve unique work, and continue consecutive tasks without asking for routine decisions. You cannot promise unattended work past an actual ended session; leave a durable checkpoint if context/tool limits are reached.

### Canonical repo (DO NOT CONFUSE WITH OTHER KX REPO)
- New Gemini prototype repository: https://github.com/daynightae-cmyk/KX-Private-VPN
- Canonical Kotlin migration workspace: `K:\KX-Private-VPN`
- Remote: `origin`; integration branch: `main`.
- Last independently inspected baseline: `37c5d8778ccdcd03dd78fca71ddcdc3d3b6df9d4`; FETCH and record the current main before writing anything.
- An entirely different and concurrently active repository, `https://github.com/daynightae-cmyk/KX`, has its own Android agents at `D:\KNOUX-Workspace\KX`. **NEVER cd into it, cherry-pick from it, push to it, merge it, replace it, or move its work unless explicitly authorized in a later task.** Do not conflate product identities or commit history.

### First reads — required
1. `CODEX_BASELINE_AUDIT.md` (actual inspection findings)
2. `CODEX_REFERENCE_VISUAL_CONTRACT.md` (**strongest approved visual contract**)
3. `CODEX_TASK_QUEUE.md` (per-feature queue and gates)
4. `src/App.tsx`, `src/context/VPNContext.tsx`, `src/hooks/useRepairStats.ts`, `src/utils/repair-engine.ts`, `src/utils/config-parser.ts`, `src/i18n/translations.ts`, `src/components/screens/repair/RepairServiceModal.tsx`.
5. **OPEN EVERY ORIGINAL owner-supplied screenshot attached to this assignment.** If missing, use all eleven numbered `public/assets/aistudio/*.png` references that are already committed and log which ones were actually inspected. Owner-supplied images override Gemini artwork.
6. Inventory all 22 present React screens, navigation, state transitions, styling, assets, actions, file dialogs and imports.

## IMMEDIATE HARD OVERRIDE: DO NOT PRESERVE GEMINI VISUAL DESIGN
The owner has explicitly rejected treating Gemini's current appearance as the visual baseline. Replace/refine all Gemini screen styling in the **native app** to match the owner reference images as exactly as technically and accessibly possible.

Gemini TSX is valuable only as a provisional **feature catalogue, navigation model, bilingual content source and interaction inventory**. Gemini colors, components, layout, typography, card structure, iconography and animations are replaceable. Do not transfer them blindly to Compose or build another generic Android app.

**TOP DESIGN PRIORITY IS AN EXTRAORDINARY, ORIGINAL, CINEMATIC SPLASH** derived from exact KX glass-orb logo. Read `CODEX_REFERENCE_VISUAL_CONTRACT.md` for a full 8-beat light/ribbon/orb choreography, performance/reduced-motion, immediate system splash, real startup routing and visual QA. Deliver actual Kotlin Compose animations, not an image or non-interactive WebView. Give the splash special attention, then build pixel-faithful native screens section-by-section.

## REALITY CHECK — known independently executed baseline
The newly cloned Gemini app is React 19 / TypeScript 7 / Vite 8 with **11 VPN React screens + Repair Lab dashboard + 10 Repair React service screens + founder identity screen**. It is **not** an existing Kotlin Android app.

`bun install --frozen-lockfile` completed; `bun run lint` (tsc --noEmit) exit 0; `bun run build` exit 0, 1,723 modules transformed. These prove the web preview compiles, NOT VPN connectivity, Android behavior, data deletion, visual parity, or any native APK.

Observed production-blocking fakes requiring removal from SHIPPED native UI:
- `VPNContext.tsx`: fictitious seven-server fleet, country ping/load, seeded session history, sample imports/IPs; `toggleConnect` uses timeouts to report connected; fake speeds, enabled security flags, unverified WireGuard/OpenVPN.
- `useRepairStats.ts`: explicitly emulates StatFs and fabricates capacity, reserved bytes and scan results.
- `repair-engine.ts`: contains fixed invented trash, duplicates, cache, media, downloads, session/network data.
- `RepairServiceModal.tsx`: visual deletion of array items via timers falsely reports freed bytes.
- `Screen11RepairLab.tsx`: simulated scanning and synthetic storage savings.
- `Screen20NetworkDoctor.tsx`: random latency.
- `Screen21RepairAutopilot.tsx`: timer-driven fake execution/finish.

These must be classified as DEMO FIXTURES ONLY, with test fixtures isolated away from production. Genuine service results replace them, or the UI must show **NO DATA / PERMISSION REQUIRED / NOT CONFIGURED / FEATURE UNAVAILABLE**. No fake success.

## EXECUTION CONTROL — OWNER IS LEAVING THE COMPUTER
- Do NOT ask for repeated "Continue?" confirmations, routine style decisions, optional providers, SDK installation approval, or fake progress reports. You are authorized to choose sensible engineering defaults and continue implementing as far as the active agent can.
- Before every change verify baseline HEAD, origin/main SHA, git status, branches, worktrees, reflog, stashes, untracked assets and other active agents. Protect unique work. New repo's local files `CODEX_*.md` are owner handoff documents, NOT junk to discard.
- No reset --hard, git clean, force push, branch/worktree deletion, rewriting Git history, device file deletion or repository-wide refactor without proven need and authorization. No sudden migration into another repo.
- User wants rapid delivery and immediate integration: **build one small vertical slice, run the quickest relevant automated tests + live emulator/runtime + screenshot proof, push a task branch, verify CI and merge to main when PASS**. Start next slice immediately. Do not idle waiting for CI if independent preparation can continue safely; you MAY work on independently branched next-slice preparations, but do not mark a prior slice MERGED or branch from unverified code as authoritative. Never merge failing/unverified security or destructive-data functionality just to keep a task counter moving.
- The earlier general "no merge until owner returns" concerned parallel work on the separate KX repo; this mission explicitly authorizes sequential merges **within KX-Private-VPN** once objective gates pass. Never touch PRs/branches of the other repository.
- If a key, API, VPN server fleet, paid service, login provider, signing credential or Play Console access is absent: continue all provider-independent verified work and record the deferred external portion in `docs/STATUS_BLOCKERS.md`. Do not invent credentials, gateways or VPN traffic.
- If an essential proof is unavailable (no emulator/device/test gateway): preserve implementation in its branch and mark NOT_VERIFIED/BLOCKED, then continue safe independent work where possible; do not claim PASS. Keep a factual handoff at execution end.

## ANDROID NATIVE MIGRATION — REQUIRED ARCHITECTURE
- Keep React/Vite source and all original reference PNG files intact as historical functional/audit material and desktop design comparison. Do not rework the web app unless essential for reference capture and tests. They are NOT the Android runtime.
- Create an Android native Kotlin + Jetpack Compose project at `android/` (or another documented safe subfolder), Gradle Kotlin DSL + version catalog + checked Gradle wrapper, reviewed compatible AGP/Kotlin/Compose/JDK/SDK targets, tested CI.
- Standard modular architecture (modules or clean packages): `app/navigation`, `designsystem`, `domain/vpn`, `platform/vpn`, `data/config`, `features/vpn/*`, `domain/repair`, `platform/storage`, `features/repair/*`, `security`, `tests`. Avoid overengineering modules until integration needs demand.
- Jetpack Compose + StateFlow/ViewModel + Coroutines/Flow, navigation, AndroidX SplashScreen, persistence with DataStore/Room as needed, Android Keystore or vetted Android secret-storage primitive, WorkManager for cancellable eligible jobs. All app components actually wired to platform services.
- Package name and build signing verified; don't falsely publish to Play. Android Edge-to-Edge, version behavior, foreground service notification/lifecycle, networking and permission flows.
- `WireGuard` is first verified transport target: Android `VpnService`, platform consent, authentic tunnel backend, state machine, Android VPN foreground requirements, reconnect/disconnect, user-owned WireGuard configuration, real handshake and traffic proof. Do not write homebrew crypto. OpenVPN and other transport only if separately implemented/verified. No pretend global server network.
- Use Android official storage APIs (MediaStore, SAF, ContentResolver, DocumentFile, StatFs, proper app cache), scoped storage and user-mediated deletion. No cross-app private file scanning, root or fake global cache clear.
- Define a **typed capability/status contract**: supported + verified; supported but not configured; permission required; offline; unavailable/unsupported; deferred external; error. Make UI labels honest.

## FULL-STACK PER-SLICE INVARIANT
For each individual screen/service in `CODEX_TASK_QUEUE.md`, implement all relevant dimensions in the **same PR**:
1. Visual: match the approved single reference's exact composition, orb geometry/optics, translucent surfaces, typography, spacing, nav, state transitions and premium motion; custom themed Day/Night and AR/EN RTL/LTR, TalkBack 48dp targets, varied device sizes.
2. Domain: correct state machines, algorithms, validation, cancellation, progress grounded in work, error/retry flows, race protection, idempotency, offline/missing-permission behavior.
3. Platform: real Kotlin/Android APIs, VPN system consent, routing, storage grants, notifications, background lifecycle; no fake timers as evidence.
4. Persistence and privacy: encrypted/wrapped secrets, minimal metadata, user-controlled retention/clear, migrations where needed, no embedded credentials or sample IPs as real user data.
5. Tests: unit + selected instrumentation + emulator startup/interactions + screenshot comparisons. For network, validate controlled endpoint, handshake/routing/DNS; for cleaning, use synthetic permitted fixtures and prove only selected files affected.
6. CI + integration: Gradle build, lint, test, workflow run at exact SHA, safe PR review, merge, remote main SHA and post-merge check. Never silently hide a failing job.
7. Evidence: actual commands/exits, screenshot file names, Android API/device, CI URLs, PR, implementation SHA, merge SHA, status and blockers.
8. User experience: consistent empty/loading/permission/retry/partial/success/error screens, undo only where genuine; accessible clear controls and meaningful feedback.

## STRICT SCREEN ORDER — TOTAL 22 DELIVERABLES
### VPN identity + functional client (TASK_00–TASK_10)
- `TASK_00`: Legendary KX logo + Android System SplashScreen + premium cinematic native intro + launch/brand foundation (highest visual priority).
- `TASK_01`: Welcome/onboarding, first-run navigation, configuration entry, consent education.
- `TASK_02`: Disconnected Home + real VpnService/WireGuard session controller. Connection without config must stay disabled and explain why.
- `TASK_03`: Actual saved/authorized endpoint selection + favorites/search, honest ping/country labels.
- `TASK_04`: Genuine connected screen + session lifecycle, traffic counters, route/DNS status, disconnect/reconnect.
- `TASK_05`: Security Center; kill switch/Android lockdown explanations, actual DNS/IPv6/tunnel status, no fake fully protected.
- `TASK_06`: Split tunnel via Android VPN per-app allow or deny policy, discover eligible installed packages, verify traffic.
- `TASK_07`: Advanced Network; supported MTU/DNS/keepalive only. Disable unimplemented obfuscation/port hopping/OpenVPN.
- `TASK_08`: Real local usage statistics/history and transparent empty state, no synthetic privacy score.
- `TASK_09`: Secure WireGuard QR/file/clipboard/manual import, redaction, strict validation and private-key protection.
- `TASK_10`: Profile/settings, locale/theme/notifications/reset, truthful local account state and full app integration.

### Unique Repair Lab (TASK_11–TASK_21)
- `TASK_11`: **KX Repair Lab Dashboard**, extremely polished new native home, ten distinctive service icons/cards, scans and history, responsive night/day, accurate diagnostic summary.
- `TASK_12`: **Storage X-Ray** — actual total/free via StatFs, shared media categorization via MediaStore, SAF authorized areas, coverage disclosure, drill-down.
- `TASK_13`: **Ghost Hunter** — detect permitted empty directories, zero-byte and grounded abandoned temporary candidates; confidence reasons, review, user-mediated safe action.
- `TASK_14`: **Duplicate DNA** — streamed file-size buckets + SHA-256 exact duplicates, optional separate perceptual matches; preview, keep-one, explicit trash approval.
- `TASK_15`: **Heavyweight Finder** — sort/filter/preview accessible large files, actual storage/candidate details; safe selection.
- `TASK_16`: **Cache Surgeon** — app-owned cache cleaning, Android-supported settings shortcut for others, real before/after space; never bypass package sandbox.
- `TASK_17`: **Media Slim Studio** — real image/video optimization (only supported formats), comparison, output verification, original preservation, user-confirmed replacement.
- `TASK_18`: **Download Rescue** — permitted files, incomplete/zero-byte/known-format integrity checks, safe recovery/copy/rename/remove; no universal "repair" promises.
- `TASK_19`: **Session Shield** — KX-owned VPN/config/session records, retention and local revocation, official third-party settings guidance only; no other-app session access.
- `TASK_20`: **Network Doctor** — genuine ConnectivityManager status, bounded authorized DNS/latency probes, VPN route diagnostics, no random fake timing/IP.
- `TASK_21`: **Repair Autopilot** — actual end-to-end scan → analyze → explain → preview → user approve → execute → verify → report, robust partial-failure journal, actual reclaimed bytes, no silent destructive cleanup.

**Each of these 22 gets its own task document, a real standalone screen, code/service/tests/evidence, a branch/PR and a merge AFTER passing gates.** You may create task files incrementally within short initial planning, but do not let documentation replace coding.

## PHONE REPAIR UX — TEN SERVICE CARDS WITH REAL DIFFERENT EXPERIENCES
Repair Lab must be a highlighted but polished native navigation destination, not a gimmicky oversized nav item or web modal. Ten individual crystal/glass service icons with distinct metaphors (storage core, spectral scanner, DNA duplicate strands, gravity/size orbit, cache blade, media prism, download recovery, privacy shield, diagnostic globe, automated control orb). Keep visible copy precise and no "100% Safe" without safety proof.

Each tool is a genuinely complete **customer journey**: explicit capability/scope → what can be scanned → permission consent → scan real permitted data → visual results + confidence/risk → select/preview → confirm → execute actual Android operation → verify result → success/partial/failure with accurate evidence and review/undo only where possible. No random UI progress or invented scanned bytes.

Do not request unrestricted permissions by default: Android scoped storage / target SDK 36, user consent for deletions, SAF for selected directories, app-owned cache only, no invisible access to WhatsApp/Chrome private data, no fake deep-clean/kill-background/RAM boost.

## VISUAL DESIGN ACCEPTANCE — OWNER IMAGES, NOT GEMINI
- Pixel-level composition comparison with every screenshot supplied in this task and immutable original PNGs in `public/assets/aistudio`. Build reference-to-component measurements.
- Side-by-side and alpha overlays using emulator captures; store evidence locally under `docs/evidence/visual/TASK_XX`. Compare hero, orb/refraction, background, nav, app content hierarchy, card spacing, icons, typography and gradients.
- Do NOT paste a static whole-screen screenshot behind buttons. Native visual primitives and state-driven drawing.
- Achieve polished visual fidelity in both variants and both languages. One image/screen per captured file; no collage.
- Splash must be unusually cinematic and bespoke, with physically convincing glass lighting and coherent transitions, but startup must be responsive, skippable where appropriate and reduced-motion compatible.
- User-provided screenshots dominate all Gemini implementation styling choices. If screenshots use illustrative data, preserve layout and appearance but substitute truthfully grounded state.

## TESTING & MERGE POLICY — HIGH SPEED, ZERO FICTION
- Run fastest targeted tests immediately after each modification; do more extensive checks in CI concurrently where possible. Do not sit idle on optional background CI while useful independent preparation remains.
- Maintain `docs/PROGRESS_LEDGER.md`, `docs/STATUS_BLOCKERS.md`, `docs/evidence/TASK_XX.md`, source/reference asset hashes and exact screenshots. If you cannot run device tests, explicitly label DEVICE_UNVERIFIED; never infer production from web build.
- PRs to THIS REPOSITORY main only; no other KX repo mutations.
- For each passing verified slice: commit, push, PR, run checks, merge main, verify origin/main and SHA, continue next. Do not merge when relevant tests or critical visual/runtime checks fail. If CI cannot be activated, record the blocker and keep the implementation branch rather than faking.
- Never destroy unique work or delete device data automatically. Follow the latest explicit user instruction: continuous non-destructive coding without re-asking, but secure file deletion in the app always requires explicit end-user consent.
- External provider API keys, accounts, payment/signing, gateways, legal disclosures: document and defer, continue independent service slices and finish with blockers. No secret material in repository or screenshots.

## MANDATORY OUTPUT FROM THE ACTIVE CODEX SESSION
At each task boundary record:
TASK_ID | CURRENT_MAIN_SHA | SOURCE_BASE_SHA | IMPLEMENTATION_SHA | CHANGED_FILES | REFERENCE_IMAGE | NATIVE_SCREENSHOT | VISUAL_DELTA | DOMAIN_FUNCTIONALITY | REAL_ANDROID_API | TEST_COMMANDS+EXIT_STATUS | EMULATOR/API | VPN/GATEWAY_OR_STORAGE_PROOF | CI_RUN+PR | MERGE_SHA | BLOCKERS | NEXT_TASK.

If the session cannot finish all 22, maximize actual tested/merged slices and leave complete checkpoint/resumption commands. Avoid lengthy status narration; prioritize code and the user's approved reference visuals.

**START NOW. BEGIN WITH PRE-FLIGHT AND TASK_00'S LEGENDARY CINEMATIC SPLASH, THEN STRICTLY CONTINUE THE NATIVE FULL-STACK APP.**
