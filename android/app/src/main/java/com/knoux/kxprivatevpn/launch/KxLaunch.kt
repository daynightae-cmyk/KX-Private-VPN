package com.knoux.kxprivatevpn.launch

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivityResultRegistryOwner
import android.content.res.Configuration
import java.util.Locale
import com.knoux.kxprivatevpn.features.ClientViewModel
import com.knoux.kxprivatevpn.features.NativeClient
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.designsystem.Atmosphere
import com.knoux.kxprivatevpn.designsystem.GlassAction
import com.knoux.kxprivatevpn.designsystem.KxOrb
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch

@Composable
fun KxLaunch() {
    val context = LocalContext.current
    val activityResults = checkNotNull(LocalActivityResultRegistryOwner.current)
    val preferences = remember { LaunchPreferences(context.applicationContext) }
    val client: ClientViewModel = viewModel()
    val state by client.state.collectAsStateWithLifecycle()
    var stack by rememberSaveable { mutableStateOf(listOf("welcome")) }
    var entryAccepted by rememberSaveable { mutableStateOf(false) }
    var routingInitialized by rememberSaveable { mutableStateOf(false) }
    // Do not send returning users directly to Home: the person must tap the KX emblem.
    LaunchedEffect(state.loaded, entryAccepted) {
        if (state.loaded && entryAccepted && !routingInitialized) {
            if (state.settings.onboarded || state.snapshot.profiles.isNotEmpty()) stack = listOf("home")
            routingInitialized = true
        }
    }
    LaunchedEffect(state.settings.onboarded) {
        if (state.settings.onboarded && stack.last() == "education") stack = listOf("home")
    }
    LaunchedEffect(state.importedId) {
        if (state.importedId != null) { stack = listOf("home"); client.consumeImport() }
    }
    var introFinished by rememberSaveable { mutableStateOf(false) }
    var initialRead by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        try {
            val seen = withTimeoutOrNull(1500) { preferences.hasSeenIntro() }
            // A previous successful launch must not bypass the owner's entry screen.
            // Keep the bounded read for honest storage-error reporting only.
            if (seen == null) error = true
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { error = true }
        initialRead = true
    }
    LaunchedEffect(introFinished) {
        if (introFinished) {
            try { preferences.markSeen() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { error = true }
        }
    }
    val systemConfiguration = LocalConfiguration.current
    val configuration = remember(systemConfiguration, state.settings.language) {
        Configuration(systemConfiguration).apply {
            if (state.settings.language != "system") {
                val locale = Locale.forLanguageTag(state.settings.language)
                setLocale(locale); setLayoutDirection(locale)
            }
        }
    }
    val localized = remember(context, configuration) { context.createConfigurationContext(configuration) }
    val night = when (state.settings.theme) { "day" -> false; "night" -> true; else -> isSystemInDarkTheme() }
    CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides configuration,
        LocalActivityResultRegistryOwner provides activityResults,
        LocalLayoutDirection provides if (configuration.layoutDirection == 1) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        BackHandler(enabled = stack.size > 1 || state.busy) {
            if (!state.busy && stack.size > 1) stack = stack.dropLast(1)
        }
        if (!entryAccepted || stack.last() == "welcome") IdentityExperience(
            ready = initialRead && state.loaded,
            finished = entryAccepted || introFinished,
            reducedMotion = rememberReducedMotion(),
            night = night, saveError = error, onFinished = { introFinished = true },
            onEnter = if (!entryAccepted) ({ entryAccepted = true }) else null,
            showWelcomeControls = entryAccepted,
            onGetStarted = { stack = stack + "education" }, onImport = { stack = stack + "import" },
        ) else key(stack.last()) { NativeClient(stack.last(), state, client, night,
            navigate = { stack = stack + it }, back = {
                if (!state.busy) stack = if (stack.size > 1) stack.dropLast(1) else listOf("welcome")
            }) }
    }
}

@Composable
fun IdentityExperience(
    ready: Boolean = true, finished: Boolean = false, reducedMotion: Boolean = false,
    night: Boolean = true, saveError: Boolean = false, onFinished: () -> Unit = {},
    onGetStarted: (() -> Unit)? = null, onImport: (() -> Unit)? = null,
    onEnter: (() -> Unit)? = null, showWelcomeControls: Boolean = true,
) {
    val progress = remember { Animatable(if (finished || reducedMotion) 1f else 0f) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var skipped by rememberSaveable { mutableStateOf(false) }
    var entryFired by remember { mutableStateOf(false) }
    val tapSpin = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var disclosure by rememberSaveable { mutableStateOf(false) }
    val currentFinished by rememberUpdatedState(onFinished)
    LaunchedEffect(ready, finished, reducedMotion, skipped, lifecycle) {
        if (!ready) return@LaunchedEffect
        if (finished || reducedMotion || skipped) {
            progress.snapTo(1f)
            if (!finished) currentFinished()
        } else {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (progress.value < 1f) {
                    progress.animateTo(1f, tween(
                        (IntroTimeline.DURATION_MS * (1f - progress.value)).toInt().coerceAtLeast(1),
                        easing = LinearEasing,
                    ))
                    currentFinished()
                }
            }
        }
    }
    val stage = if (finished || reducedMotion || skipped) 1f else progress.value
    val frame = IntroTimeline.frame(stage)
    // Dark opening frame: show only the actual emblem first, then reveal the page.
    val appearance = ((stage - .16f) / .66f).coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }
    val text = if (night) Color(0xFFF5F0FF) else Color(0xFF161050)
    // Dialog owns a separate Android window/context. Resolve copy in the selected locale here,
    // so that its portal cannot fall back to the underlying Activity's previous resources.
    val disclosureTitle = stringResource(R.string.welcome)
    val disclosureBody = stringResource(R.string.scope_notice)
    val disclosureClose = stringResource(R.string.close)
    val entryHint = stringResource(R.string.tap_kx_orb_to_enter)
    MaterialTheme(colorScheme = if (night) darkColorScheme() else lightColorScheme()) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF080B28)).testTag("identity")) {
            // Only the logo exists on the first Compose frame; the approved scene gradually appears.
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = appearance }) {
                Atmosphere(night, frame.pulse)
            }
            val orb = minOf(maxWidth * .88f, maxHeight * .46f, 410.dp)
            val travel = maxHeight * .18f
            // One shared orb across intro and Welcome: no image replacement or positional cut.
            Box(Modifier.fillMaxWidth().padding(top = maxHeight * .09f), contentAlignment = Alignment.TopCenter) {
                // The same native glass orb is the entry button, not an extra generic CTA.
                // Visual choreography stays 3D; a reduced-motion setting bypasses animation,
                // never the explicit user click.
                val enterable = onEnter != null && ready && progress.value >= 1f && !entryFired
                val emblemModifier = Modifier.size(orb)
                    .testTag("entry-orb")
                    .graphicsLayer {
                        val distance = 1f - stage
                        // Fly in from the distant center before the background becomes visible.
                        scaleX = .33f + .67f * appearance
                        scaleY = .33f + .67f * appearance
                        translationY = travel.value * density * distance
                        alpha = (.35f + .65f * (stage * 4f).coerceIn(0f, 1f))
                        rotationY = -38f * distance + tapSpin.value
                        rotationX = 17f * distance
                        cameraDistance = 1500f * density
                    }
                    .then(
                        if (onEnter != null) Modifier
                            .semantics { contentDescription = entryHint }
                            .clickable(enabled = enterable, role = Role.Button) {
                                if (!entryFired) {
                                    entryFired = true
                                    if (reducedMotion) {
                                        onEnter()
                                    } else {
                                        scope.launch {
                                            tapSpin.animateTo(360f, tween(680, easing = FastOutSlowInEasing))
                                            onEnter()
                                        }
                                    }
                                }
                            }
                        else Modifier
                    )
                // Byte-preserved owner KX artwork is visible even before the ribbon formation.
                // Unlike the legacy system icon, KxOrb crops the outside square corners.
                val formedFrame = frame.copy(artwork = maxOf(frame.artwork, .65f))
                KxOrb(progress = stage, frame = formedFrame, modifier = emblemModifier)
            }
            Column(
                Modifier.fillMaxWidth().padding(top = maxHeight * .53f)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 30.dp).verticalScroll(rememberScrollState())
                    .graphicsLayer { alpha = frame.wordmark },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("KX", color = text, fontSize = 52.sp, fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp)
                Text("PRIVATE VPN", color = text, fontSize = 20.sp, letterSpacing = 5.sp,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.tagline), color = text, fontSize = 18.sp,
                    fontStyle = FontStyle.Italic, textAlign = TextAlign.Center)
                if (onEnter != null && frame.welcome > 0f) {
                    Spacer(Modifier.height(22.dp))
                    Text(entryHint, color = text, fontSize = 17.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("entry-hint")
                            .graphicsLayer { alpha = frame.welcome })
                }
                if (showWelcomeControls && frame.welcome > 0f) {
                    Column(Modifier.fillMaxWidth().graphicsLayer { alpha = frame.welcome },
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(28.dp))
                        GlassAction(stringResource(if (onGetStarted == null) R.string.continue_label else R.string.get_started), Modifier.testTag("continue")) {
                            if (onGetStarted != null) onGetStarted() else disclosure = true
                        }
                        Spacer(Modifier.height(12.dp))
                        if (onImport != null) GlassAction(stringResource(R.string.import_config), Modifier.testTag("welcome-import"), onClick = onImport)
                        else Text(stringResource(R.string.preview_notice), color = text, fontSize = 12.sp)
                        if (saveError) Text(stringResource(R.string.save_error), color = text,
                            textAlign = TextAlign.Center, fontSize = 12.sp)
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
            if (ready && frame.welcome < 1f) {
                TextButton(onClick = { skipped = true }, modifier = Modifier.align(Alignment.TopEnd)
                    .statusBarsPadding().padding(8.dp).heightIn(min = 48.dp).testTag("skip")) {
                    Text(stringResource(R.string.skip_intro), color = text)
                }
            }
            if (disclosure) AlertDialog(
                onDismissRequest = { disclosure = false },
                title = { Text(disclosureTitle) },
                text = { Text(disclosureBody,
                    modifier = Modifier.verticalScroll(rememberScrollState())) },
                confirmButton = { TextButton(onClick = { disclosure = false }) {
                    Text(disclosureClose)
                } },
            )
        }
    }
}
