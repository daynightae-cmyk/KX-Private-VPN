# KX PRIVATE VPN — VISUAL AUTHORITY CONTRACT
## OWNER OVERRIDE / ABSOLUTE VISUAL PRIORITY

**THE OWNER-SUPPLIED REFERENCE IMAGES ARE THE SINGLE SOURCE OF VISUAL TRUTH. GEMINI'S CURRENT REACT DESIGN IS NOT AN APPROVED DESIGN.**

The objective is NOT to preserve Gemini's existing look. The objective is to **replace/refine it until the native Kotlin/Compose app visibly matches the supplied reference screens**, while keeping only useful business concepts, interactions, content architecture, and functional coverage that do not conflict with those screenshots.

Treat every React layout, palette, font, glass surface, header, card, icon, spacing, decorative element and animation as provisional and replaceable. Do not be persuaded by the web app having a successful build. Do NOT create a generic Material 3 app. Do not settle for "similar", "inspired by", or "close enough" without screenshot comparisons and written discrepancies.

## Reference provenance and priority
1. **Highest priority**: the exact 10+ owner-provided images attached to Codex's task, verified visually by opening EACH INDIVIDUAL IMAGE. If images conflict with older ones, ask no design question: favor the latest explicit owner-provided screenshot and document the conflict.
2. **Repository immutable fallback**: eleven numbered canonical images already present inside this new repo at `public/assets/aistudio/`:
   - `00-brand-logo_c0b92dd8.png` — brand orb and iconic geometry
   - `01-welcome_464a7c4f.png` — welcome / landing
   - `02-home-disconnected_825c75b0.png` — idle VPN home
   - `03-server-selection_8d2c742a.png` — verified endpoint picker
   - `04-connected-session_f0cfe658.png` — connection detail (actual backend only)
   - `05-security-center_d0a1f09a.png` — privacy/security diagnostics
   - `06-split-tunneling_c9265fd7.png` — per-app routing
   - `07-advanced-network_5b3214f4.png` — DNS/MTU/transport settings
   - `08-statistics_a6c474b9.png` — real measurements
   - `09-import-configuration_670a2aea.png` — configuration import
   - `10-profile-settings_75031956.png` — personal/local app settings
3. React TSX files are SECONDARY FUNCTIONAL INVENTORY only. They are NOT a visual authority. Compare to them solely to avoid forgetting user-facing services or navigation.
4. Repair-specific screen references (TASK_11–21): use new owner-provided images if present; if absent, create fresh individual native design previews using the SAME visual system, NOT derivative Gemini styling. One screen = one independent reference / screenshot, no multi-panel collage.

## Required look and feel
- Advanced **3D circular crystalline/pearl glass orb** and fluid intertwined wing/shield/KX ribbon geometry, physically plausible reflections, layered thickness, violet/royal-blue/cyan refraction, delicate highlights.
- Rich deep-ink/navy nights, soft iridescent lavender/pearl days, purposeful volumetric glow and orbital particles. Avoid flat purple rectangles and plain generic cards.
- Distinct depth: near/far background planes, glass bevels, controlled soft caustics, gradient edge-lighting, premium shadows, varied pane opacity. Strong contrast for readable text over glass.
- Precisely reproduce hierarchy, hero scale, safe spaces, headers, navigation position, cards and illustrated values' *layout*, while using real runtime data or honest dashes in place of fictional metrics.
- The supplied **logo is binding**: do not invent an unrelated lettermark or flatten the glass emblem. Preserve supplied original reference images unchanged. Any native vector implementation should be compared to the visible original.
- Android full-bleed, edge-to-edge/insets done correctly, tablet/small-phone responsive; no web DeviceFrame or desktop designer toolbar in the shipping app.
- Fonts: geometric premium Latin and beautifully fitted Arabic; prevent Arabic text clipping and misaligned RTL icons. No fake hardcoded English text where AR mode should apply.
- UI controls are native interactive Compose elements, not full-screen pictures. Touch targets >=48dp; high contrast; TalkBack semantics; reduce-motion fallback; robust scrolling.
- Light/dark are intentional complete design variants, not an automatic palette reversal. No screen-specific theme forcibly overriding an explicit user choice.

## LEGENDARY CINEMATIC SPLASH — TASK_00 PRIORITY #1
The owner explicitly requests a non-traditional, breathtaking, flagship-grade Splash: it must be the unmistakable visual signature of KX, not a loading spinner or static icon.

**Choreography** (creative interpretation anchored on the exact owner logo):
1. Immediate Android 12+ system SplashScreen displays a high-contrast compliant brand icon (no black/blank flash); actual Android startup must never wait for network calls or simulated progress.
2. In the app's native Compose intro, darkness/pearl haze emerges. Tiny cyan/violet photons slowly converge toward an invisible glass sphere, with cinematic parallax depth.
3. Transparent ribbons sweep along curved paths, then interlock in the reference KX emblem. The orb forms via layered light/refraction: frosted center, liquid-glass rim, delicate orbit rings and soft transparent halo. The eyes/center of the icon (only if present in exact provided artwork) catch glints; don't add alien imagery.
4. A precise caustic light sweep travels across the icon, the outer halo pulses once, tiny satellites glide outward, then everything settles. Wordmark `KX PRIVATE VPN` appears elegantly with measured letter spacing and optional bilingual descriptor.
5. The same orb remains visually continuous when transitioning to Welcome/Home using a shared-brand motion language. Avoid a harsh cut or jarring logo jump.
6. Use classy sound ONLY if a genuine preference-controlled and user-allowed design requirement exists; no loud unrequested autoplay.
7. Keep the intro **brief, skippable, interruptible** and independent of VPN status. Native Android mandatory splash timing must remain appropriate; do not block accessibility/slow devices. On warm restart, restore navigation instead of replaying expensive intro every time.
8. Adaptive fidelity: 60fps target on supported devices, GPU-safe, no all-screen heavy blur each frame; fall back to simpler vector/canvas layered gradients on low-end devices. Support "Remove animations"/reduced motion, battery saver and accessibility.
9. State truth: never show a fake "100% secure", fake handshake, network progress or success on launch. Splash completion is VISUAL completion only.

Preferred native techniques (after feature support checks): Jetpack Compose Canvas paths, radial/linear gradients, custom vector paths, Animatable/Transition, graphicsLayer, lightweight layered alpha, system SplashScreen compat, RenderEffect/AGSL only when supported with fallback. No WebView and no GIF pasted as a fake native page.

**TASK_00 acceptance**:
- Cold and warm startup on Android emulator, no blank frame/crashes or endless loading.
- Distinct system splash + branded native intro; polished transition to the real next destination.
- Side-by-side comparison to supplied logo with shape/shine/orb/ribbon alignment documented.
- Multiple Android API versions, light/dark, AR/EN, RTL, 360dp/411dp, accessibility and reduced-motion captures.
- Animation profiling: no uncontrolled background timer or costly continuous animation.
- Only claim visual "legendary" when actual captured results meet visual review, not based on generated mockup or a green build.

## Exact screen visual review — TASK_00–TASK_10
For every numbered view:
- Capture reference at its intrinsic resolution and map safe-area, hero, cards, footers, iconography, transparency, typography, colors and gradient boundaries.
- Record a component/layout plan with on-image coordinates or proportional measurements before implementation; avoid repeating Gemini card layout automatically.
- Reproduce in Compose using real controls and actual data; use restrained animated accents only where genuinely helpful.
- Generate actual Android emulator screenshots at 360dp and 411dp minimum, Day/Night and EN/AR, plus large text and screen-reader check.
- Compare reference ↔ actual using aligned side-by-side and optional 50% overlay/difference view. The QA compare must be separate evidence, not a shipped UI.
- Fix every critical discrepancy (missing hero, incorrect logo, lost background atmosphere, cards out of place, overflowing navigation, missing iridescence). List minor intentional deviations with accessibility/technical reasons.
- Never trade away required interaction/accessibility or truthful state for a fake screenshot match.

## Visual gate and no fake QA
Visual review checklist per section:
A. Brand silhouette and logo geometry
B. Hero composition, orb size and relative placement
C. Gradient hues, day/night atmosphere and glass optical depth
D. Interactive card dimensions/gaps/edge rim, icons and typography
E. Animations, haptics where appropriate, and state continuity
F. RTL/EN, accessibility, responsive width and real navigation

Each criterion must be actually inspected and documented before calling the screen visually verified. "Built successfully" or "matches style" in an agent report is insufficient. If screenshot test cannot run, mark VISUAL_UNVERIFIED and preserve progress without falsely saying PASS.

## Priority summary
**APPROVED IMAGES > user's latest explicit direction > native UX/accessibility/security constraints > Gemini React look.**
When these priorities conflict, adapt within safety/UX constraints and document why. The result should look like the supplied imagery has come alive as a fully working premium Android application.
