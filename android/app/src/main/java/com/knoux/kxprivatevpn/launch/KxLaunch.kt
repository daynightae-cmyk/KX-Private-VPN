package com.knoux.kxprivatevpn.launch

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.designsystem.Atmosphere
import com.knoux.kxprivatevpn.designsystem.GlassAction
import com.knoux.kxprivatevpn.designsystem.KxOrb
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun KxLaunch() {
    val context = LocalContext.current
    val preferences = remember { LaunchPreferences(context.applicationContext) }
    var introFinished by rememberSaveable { mutableStateOf(false) }
    var initialRead by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        try {
            val seen = withTimeoutOrNull(1500) { preferences.hasSeenIntro() }
            if (seen == true) introFinished = true
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
    IdentityExperience(
        ready = initialRead, finished = introFinished, reducedMotion = rememberReducedMotion(),
        night = isSystemInDarkTheme(), saveError = error, onFinished = { introFinished = true },
    )
}

@Composable
fun IdentityExperience(
    ready: Boolean = true, finished: Boolean = false, reducedMotion: Boolean = false,
    night: Boolean = true, saveError: Boolean = false, onFinished: () -> Unit = {},
) {
    val progress = remember { Animatable(if (finished || reducedMotion) 1f else 0f) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var skipped by rememberSaveable { mutableStateOf(false) }
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
    val frame = IntroTimeline.frame(if (finished || reducedMotion || skipped) 1f else progress.value)
    val text = if (night) Color(0xFFF5F0FF) else Color(0xFF161050)
    MaterialTheme(colorScheme = if (night) darkColorScheme() else lightColorScheme()) {
        BoxWithConstraints(Modifier.fillMaxSize().testTag("identity")) {
            // Stable zero pulse lets Compose retain the background through photon/ribbon motion.
            Atmosphere(night, frame.pulse)
            val orb = minOf(maxWidth * .88f, maxHeight * .46f, 410.dp)
            // One shared orb across intro and Welcome: no image replacement or positional cut.
            Box(Modifier.fillMaxWidth().padding(top = maxHeight * .09f), contentAlignment = Alignment.TopCenter) {
                KxOrb(progress = if (!ready) 0f else progress.value, frame = frame,
                    modifier = Modifier.size(orb))
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
                if (frame.welcome > 0f) {
                    Column(Modifier.fillMaxWidth().graphicsLayer { alpha = frame.welcome },
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(28.dp))
                        GlassAction(stringResource(R.string.continue_label), Modifier.testTag("continue")) {
                            disclosure = true
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.preview_notice), color = text, fontSize = 12.sp)
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
                title = { Text(stringResource(R.string.welcome)) },
                text = { Text(stringResource(R.string.scope_notice),
                    modifier = Modifier.verticalScroll(rememberScrollState())) },
                confirmButton = { TextButton(onClick = { disclosure = false }) {
                    Text(stringResource(R.string.close))
                } },
            )
        }
    }
}
