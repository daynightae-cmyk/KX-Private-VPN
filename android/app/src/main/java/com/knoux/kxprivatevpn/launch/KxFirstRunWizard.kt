package com.knoux.kxprivatevpn.launch

import android.app.Activity
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.designsystem.KxGlassIconTile
import com.knoux.kxprivatevpn.designsystem.KxGlassPanel
import com.knoux.kxprivatevpn.designsystem.KxGradientAction
import com.knoux.kxprivatevpn.designsystem.KxIconKind
import com.knoux.kxprivatevpn.designsystem.KxLineIcon
import com.knoux.kxprivatevpn.designsystem.KxMiniOrb
import com.knoux.kxprivatevpn.designsystem.KxOrb
import com.knoux.kxprivatevpn.designsystem.KxOrbAura
import com.knoux.kxprivatevpn.designsystem.KxOrbitalHalo
import com.knoux.kxprivatevpn.designsystem.KxReferenceAtmosphere
import com.knoux.kxprivatevpn.designsystem.KxReferenceTheme
import com.knoux.kxprivatevpn.designsystem.kxCyan
import com.knoux.kxprivatevpn.designsystem.kxEntrance
import com.knoux.kxprivatevpn.designsystem.kxInk
import com.knoux.kxprivatevpn.designsystem.kxMuted
import com.knoux.kxprivatevpn.designsystem.kxPressScale
import com.knoux.kxprivatevpn.designsystem.kxViolet
import com.knoux.kxprivatevpn.designsystem.rememberKxTapHaptic
import com.knoux.kxprivatevpn.platform.security.ProConfiguration

/**
 * First-run experience reproduced from the owner-approved reference: a brand header with the
 * mini orb, a step indicator, a hero emblem inside an orbital halo, feature cards and
 * Previous/Next glass actions. The final step is the sign-in screen.
 *
 * This is an in-app Android wizard, not a Windows NSIS installer. It never claims a real
 * account backend: the email/password form reports the honest unconfigured state, while
 * "guest" and the Pro activation route remain the real, working entry points.
 */
@Composable
fun KxFirstRunWizard(
    night: Boolean,
    onGuest: () -> Unit,
    onImport: () -> Unit,
    onOpenPro: () -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current
    val authorityUrl = stringResource(R.string.pro_authority_url)
    val whatsapp = stringResource(R.string.pro_owner_whatsapp_e164)
    val proConfig = remember(authorityUrl, whatsapp) { ProConfiguration(authorityUrl, whatsapp) }

    // The password field must never appear in screenshots or recents on the sign-in step.
    val activity = generateSequence(LocalView.current.context) { (it as? ContextWrapper)?.baseContext }
        .filterIsInstance<Activity>().firstOrNull()
    val secureLogin = step == 3
    DisposableEffect(secureLogin, activity) {
        val window = activity?.window
        val alreadySecure = ((window?.attributes?.flags ?: 0) and WindowManager.LayoutParams.FLAG_SECURE) != 0
        if (secureLogin) window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { if (secureLogin && !alreadySecure) window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }

    BackHandler(enabled = step > 0) { step -= 1 }

    KxReferenceTheme(night) {
        BoxWithConstraints(
            Modifier.fillMaxSize()
                .background(if (night) Color(0xFF080B28) else Color(0xFFEFEBFF))
                .testTag("kx-first-run-wizard"),
        ) {
            KxReferenceAtmosphere(night)
            val compact = maxHeight < 740.dp
            Column(
                Modifier.fillMaxSize().safeDrawingPadding().imePadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                WizardHeader(night, step, Modifier.kxEntrance(index = 0))
                // The reference login screen carries no step chrome, only the brand header.
                if (step < 3) {
                    Spacer(Modifier.height(if (compact) 4.dp else 10.dp))
                    WizardDots(night, step, Modifier.kxEntrance(index = 1))
                }
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                        .padding(top = if (compact) 6.dp else 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
                ) {
                    when (step) {
                        0 -> StepIntro(night, compact)
                        1 -> StepPrivacy(night, compact)
                        2 -> StepAccess(night, compact, proConfig,
                            onOpenSignIn = { step = 3 }, onGuest = onGuest, onImport = onImport)
                        else -> StepLogin(night, compact, proConfig,
                            onGuest = onGuest, onOpenPro = onOpenPro)
                    }
                    Spacer(Modifier.height(6.dp))
                }
                WizardFooter(
                    night = night,
                    step = step,
                    compact = compact,
                    onBack = {
                        if (step == 0) step = 2 else step -= 1
                    },
                    onNext = { if (step < 3) step += 1 else Unit },
                )
            }
        }
    }
}

@Composable
private fun WizardHeader(night: Boolean, step: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
        KxMiniOrb(night, 24.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            "KX Private VPN",
            color = kxInk(night), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.weight(1f))
        if (step < 3) Text(
            "0${step + 1} / 04",
            color = kxCyan(night), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
            modifier = Modifier.testTag("wizard-step-counter"),
        )
    }
}

@Composable
private fun WizardDots(night: Boolean, active: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        repeat(4) { index ->
            val isActive = index == active
            val idle = Color.White.copy(alpha = if (night) .22f else .30f)
            var dot = Modifier.padding(horizontal = 4.dp)
                .width(if (isActive) 26.dp else 7.dp).height(7.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    if (isActive) Brush.horizontalGradient(listOf(kxCyan(night), kxViolet(night)))
                    else Brush.horizontalGradient(listOf(idle, idle)),
                )
            if (isActive) dot = dot.testTag("wizard-dot-active")
            Box(dot)
        }
    }
}

@Composable
private fun WizardHero(
    night: Boolean,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier.size(size + 56.dp),
        contentAlignment = Alignment.Center,
    ) {
        KxOrbitalHalo(night, Modifier.size(size + 56.dp), animated = !rememberReducedMotion())
        KxOrbAura(night, Modifier.size(size + 30.dp), strength = 1f)
        Box(
            Modifier.size(size).clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        if (night) listOf(Color.White.copy(alpha = .14f), Color(0xFF3A2C74).copy(alpha = .55f), Color.Transparent)
                        else listOf(Color.White.copy(alpha = .85f), Color(0xFFC9B8FF).copy(alpha = .45f), Color.Transparent),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = if (night) .38f else .9f), CircleShape),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

@Composable
private fun StepIntro(night: Boolean, compact: Boolean) {
    WizardHero(night, if (compact) 108.dp else 132.dp, Modifier.kxEntrance(index = 2)) {
        KxLineIcon(KxIconKind.SHIELD, kxViolet(night), Modifier.size(if (compact) 52.dp else 64.dp))
        Text(
            "KX", color = kxInk(night), fontSize = (if (compact) 15 else 18).sp,
            fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp,
            modifier = Modifier.offset(y = (if (compact) 16 else 20).dp),
        )
    }
    WizardTitle(night, stringResource(R.string.onboard_step1_title), stringResource(R.string.onboard_step1_sub), compact)
    FeatureCard(night, KxIconKind.LINK, kxCyan(night),
        stringResource(R.string.onboard_step1_f1), stringResource(R.string.onboard_step1_f1d), 3)
    FeatureCard(night, KxIconKind.BOLT, kxViolet(night),
        stringResource(R.string.onboard_step1_f2), stringResource(R.string.onboard_step1_f2d), 4)
    FeatureCard(night, KxIconKind.LAYERS, kxCyan(night),
        stringResource(R.string.onboard_step1_f3), stringResource(R.string.onboard_step1_f3d), 5)
}

@Composable
private fun StepPrivacy(night: Boolean, compact: Boolean) {
    WizardHero(night, if (compact) 108.dp else 132.dp, Modifier.kxEntrance(index = 2)) {
        KxLineIcon(KxIconKind.LOCK, kxViolet(night), Modifier.size(if (compact) 52.dp else 64.dp))
    }
    WizardTitle(night, stringResource(R.string.onboard_step2_title), stringResource(R.string.onboard_step2_sub), compact)
    FeatureCard(night, KxIconKind.SHIELD_CHECK, kxCyan(night),
        stringResource(R.string.onboard_step2_f1), stringResource(R.string.onboard_step2_f1d), 3)
    FeatureCard(night, KxIconKind.SEARCH, kxViolet(night),
        stringResource(R.string.onboard_step2_f2), stringResource(R.string.onboard_step2_f2d), 4)
    FeatureCard(night, KxIconKind.SPLIT, kxCyan(night),
        stringResource(R.string.onboard_step2_f3), stringResource(R.string.onboard_step2_f3d), 5)
}

@Composable
private fun StepAccess(
    night: Boolean,
    compact: Boolean,
    proConfig: ProConfiguration,
    onOpenSignIn: () -> Unit,
    onGuest: () -> Unit,
    onImport: () -> Unit,
) {
    Box(Modifier.kxEntrance(index = 2), contentAlignment = Alignment.Center) {
        KxOrbitalHalo(night, Modifier.size(if (compact) 150.dp else 180.dp), animated = !rememberReducedMotion())
        KxOrb(1f, IntroTimeline.frame(1f), Modifier.size(if (compact) 84.dp else 104.dp))
    }
    WizardTitle(night, stringResource(R.string.onboard_step3_title), stringResource(R.string.onboard_step3_sub), compact)
    OptionCard(
        night = night, icon = KxIconKind.POWER, accent = kxViolet(night),
        title = stringResource(R.string.onboard_access_signin),
        description = stringResource(R.string.onboard_access_signin_d),
        tag = "wizard-signin-option", index = 3, onClick = onOpenSignIn,
    )
    OptionCard(
        night = night, icon = KxIconKind.BOLT, accent = kxCyan(night),
        title = stringResource(R.string.onboard_access_guest),
        description = stringResource(R.string.onboard_access_guest_d),
        tag = "wizard-guest-option", index = 4, onClick = onGuest,
    )
    if (proConfig.configured) {
        Text(
            stringResource(R.string.onboard_access_note),
            color = kxMuted(night), fontSize = 11.sp, lineHeight = 16.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).kxEntrance(index = 5),
        )
    }
    WizardGhostButton(
        label = stringResource(R.string.onboard_access_import),
        tag = "wizard-import-option", night = night, icon = KxIconKind.FILE,
        modifier = Modifier.fillMaxWidth().kxEntrance(index = 6),
        onClick = onImport,
    )
}

@Composable
private fun StepLogin(
    night: Boolean,
    compact: Boolean,
    proConfig: ProConfiguration,
    onGuest: () -> Unit,
    onOpenPro: () -> Unit,
) {
    var email by remember { mutableStateOf("") }   // never rememberSaveable, prefs or logs
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val haptic = rememberKxTapHaptic()
    // Resolved in composition; the click lambda below is not a @Composable scope.
    val emptyMessage = stringResource(R.string.onboard_login_empty)
    val unavailableMessage = stringResource(R.string.onboard_login_unavailable)

    Box(Modifier.testTag("wizard-login").kxEntrance(index = 2), contentAlignment = Alignment.Center) {
        KxOrbitalHalo(night, Modifier.size(if (compact) 128.dp else 150.dp), animated = !rememberReducedMotion())
        KxOrb(1f, IntroTimeline.frame(1f), Modifier.size(if (compact) 66.dp else 80.dp))
    }
    WizardTitle(night, stringResource(R.string.onboard_login_title), stringResource(R.string.onboard_login_sub), compact)

    KxGlassPanel(night, radius = 18.dp, padding = PaddingValues(14.dp), modifier = Modifier.kxEntrance(index = 3)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KxGlassIconTile(night, KxIconKind.SHIELD_CHECK, kxViolet(night), size = 40.dp)
            Column {
                Text(stringResource(R.string.onboard_login_account), color = kxInk(night),
                    fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.onboard_login_account_d), color = kxMuted(night),
                    fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
    }

    LoginField(
        value = email, onValueChange = { email = it; status = null },
        placeholder = stringResource(R.string.onboard_email),
        night = night, tag = "wizard-email", icon = KxIconKind.FILE,
        keyboardEmail = true, modifier = Modifier.kxEntrance(index = 4),
    )
    LoginField(
        value = password, onValueChange = { password = it; status = null },
        placeholder = stringResource(R.string.onboard_password),
        night = night, tag = "wizard-password", icon = KxIconKind.LOCK,
        password = true, showPassword = showPassword, onToggleShow = { showPassword = !showPassword },
        modifier = Modifier.kxEntrance(index = 5),
    )

    Row(
        Modifier.fillMaxWidth().kxEntrance(index = 6),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(50))
                .clickable(role = Role.Checkbox, onClickLabel = stringResource(R.string.onboard_remember)) {
                    rememberMe = !rememberMe
                }
                .testTag("wizard-remember"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                Modifier.size(18.dp).clip(RoundedCornerShape(6.dp))
                    .background(if (rememberMe) kxViolet(night) else Color.Transparent)
                    .border(1.dp, if (rememberMe) kxViolet(night) else Color.White.copy(alpha = .4f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (rememberMe) KxLineIcon(KxIconKind.CHECK, Color.White, Modifier.size(11.dp))
            }
            Text(stringResource(R.string.onboard_remember), color = kxMuted(night), fontSize = 11.5.sp)
        }
        Spacer(Modifier.weight(1f))
        Text(
            stringResource(R.string.onboard_forgot), color = kxCyan(night), fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("wizard-forgot"),
        )
    }

    KxGradientAction(
        label = stringResource(R.string.onboard_signin),
        night = night,
        enabled = true,
        tag = "wizard-signin",
        icon = KxIconKind.FORWARD,
        modifier = Modifier.fillMaxWidth().kxEntrance(index = 7),
    ) {
        haptic()
        val submittedEmail = email
        val submittedPassword = password
        email = ""; password = ""   // clear before any callback, like ProScreen
        // Honest behavior: no account provider is configured in this build, so the
        // email/password path can never report a real sign-in success.
        status = when {
            submittedEmail.isBlank() || submittedPassword.isBlank() -> emptyMessage
            !proConfig.configured -> unavailableMessage
            else -> unavailableMessage
        }
    }
    status?.let {
        Text(
            it, color = kxCyan(night), fontSize = 11.sp, lineHeight = 16.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().testTag("wizard-login-status").kxEntrance(index = 8),
        )
    }

    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp).kxEntrance(index = 8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = if (night) .14f else .3f)))
        Text(
            stringResource(R.string.onboard_or_continue), color = kxMuted(night), fontSize = 10.5.sp,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
        Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = if (night) .14f else .3f)))
    }
    Row(
        Modifier.fillMaxWidth().kxEntrance(index = 9),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(4) { index ->
            Box(
                Modifier.padding(horizontal = 7.dp).size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = if (night) .07f else .6f))
                    .border(1.dp, Color.White.copy(alpha = if (night) .22f else .8f), RoundedCornerShape(14.dp))
                    .semantics(mergeDescendants = true) { },
                contentAlignment = Alignment.Center,
            ) {
                SocialGlyph(index, kxInk(night))
            }
        }
    }
    Text(
        stringResource(R.string.onboard_social_unavailable),
        color = kxMuted(night), fontSize = 10.sp, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().testTag("wizard-social-note").kxEntrance(index = 10),
    )

    Row(
        Modifier.fillMaxWidth().kxEntrance(index = 11),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.onboard_no_account), color = kxMuted(night), fontSize = 11.5.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(R.string.onboard_create_account), color = kxCyan(night),
            fontSize = 11.5.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.heightIn(min = 44.dp).testTag("wizard-create-account")
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.onboard_create_account)) {
                    onOpenPro()
                },
        )
    }
    WizardGhostButton(
        label = stringResource(R.string.onboard_guest),
        tag = "wizard-login-guest", night = night, icon = KxIconKind.BACK,
        modifier = Modifier.fillMaxWidth().kxEntrance(index = 12),
        onClick = onGuest,
    )
}

@Composable
private fun WizardTitle(night: Boolean, title: String, subtitle: String, compact: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().kxEntrance(index = 2), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            title, color = kxInk(night), fontSize = if (compact) 22.sp else 25.sp,
            fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = (if (compact) 28 else 32).sp,
            modifier = Modifier.fillMaxWidth().testTag("wizard-step-title"),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle, color = kxMuted(night), fontSize = 12.5.sp, lineHeight = 18.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
        )
    }
}

@Composable
private fun FeatureCard(
    night: Boolean,
    icon: KxIconKind,
    accent: Color,
    title: String,
    description: String,
    entranceIndex: Int,
) {
    KxGlassPanel(night, radius = 18.dp, padding = PaddingValues(13.dp), modifier = Modifier.kxEntrance(index = entranceIndex)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KxGlassIconTile(night, icon, accent, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(title, color = kxInk(night), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(description, color = kxMuted(night), fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
    }
}

@Composable
private fun OptionCard(
    night: Boolean,
    icon: KxIconKind,
    accent: Color,
    title: String,
    description: String,
    tag: String,
    index: Int,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val haptic = rememberKxTapHaptic()
    KxGlassPanel(
        night, radius = 18.dp, padding = PaddingValues(13.dp),
        modifier = Modifier.kxEntrance(index = index)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = {
                haptic(); onClick()
            })
            .kxPressScale(interaction, true)
            .testTag(tag),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KxGlassIconTile(night, icon, accent, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(title, color = kxInk(night), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(description, color = kxMuted(night), fontSize = 11.sp, lineHeight = 15.sp)
            }
            KxLineIcon(KxIconKind.FORWARD, kxMuted(night), Modifier.size(16.dp))
        }
    }
}

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    night: Boolean,
    tag: String,
    icon: KxIconKind,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    showPassword: Boolean = false,
    onToggleShow: (() -> Unit)? = null,
    keyboardEmail: Boolean = false,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = 52.dp).clip(shape)
            .background(Color.White.copy(alpha = if (night) .07f else .65f))
            .border(1.dp, Color.White.copy(alpha = if (night) .22f else .85f), shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = kxInk(night), fontSize = 13.sp),
            visualTransformation = if (password && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = if (keyboardEmail) androidx.compose.ui.text.input.KeyboardType.Email
                else if (password) androidx.compose.ui.text.input.KeyboardType.Password
                else androidx.compose.ui.text.input.KeyboardType.Text,
                autoCorrectEnabled = !keyboardEmail,
            ),
            cursorBrush = SolidColor(kxCyan(night)),
            modifier = Modifier.weight(1f).testTag(tag),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth()) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = kxMuted(night), fontSize = 13.sp)
                    }
                    inner()
                }
            },
        )
        Spacer(Modifier.width(10.dp))
        if (password && onToggleShow != null) {
            Box(
                Modifier.size(34.dp).clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onToggleShow)
                    .testTag("wizard-toggle-password"),
                contentAlignment = Alignment.Center,
            ) {
                KxLineIcon(KxIconKind.SEARCH, kxMuted(night), Modifier.size(17.dp))
            }
        } else {
            KxLineIcon(icon, kxMuted(night), Modifier.size(18.dp))
        }
    }
}

@Composable
private fun WizardGhostButton(
    label: String,
    tag: String,
    night: Boolean,
    icon: KxIconKind,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val haptic = rememberKxTapHaptic()
    Row(
        modifier.heightIn(min = 50.dp).clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = if (night) .07f else .5f))
            .border(1.dp, Color.White.copy(alpha = if (night) .30f else .9f), RoundedCornerShape(50))
            .kxPressScale(interaction, true)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button,
                onClickLabel = label, onClick = { haptic(); onClick() })
            .padding(horizontal = 16.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        KxLineIcon(icon, kxInk(night), Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = kxInk(night), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WizardFooter(
    night: Boolean,
    step: Int,
    compact: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    if (step == 3) {
        WizardGhostButton(
            label = stringResource(R.string.onboard_back),
            tag = "wizard-back", night = night, icon = KxIconKind.BACK,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            onClick = onBack,
        )
        return
    }
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.weight(1f)) {
            WizardGhostButton(
                label = stringResource(if (step == 0) R.string.onboard_skip else R.string.onboard_back),
                tag = "wizard-back-or-skip", night = night, icon = KxIconKind.BACK,
                modifier = Modifier.fillMaxWidth(), onClick = onBack,
            )
        }
        Box(Modifier.weight(1.35f)) {
            KxGradientAction(
                label = stringResource(if (step == 2) R.string.onboard_continue else R.string.onboard_next),
                night = night,
                enabled = true,
                tag = "wizard-next",
                icon = KxIconKind.FORWARD,
                modifier = Modifier.fillMaxWidth(),
                onClick = onNext,
            )
        }
    }
}

/** Decorative social marks. They are not auth providers in this build and never trigger one. */
@Composable
private fun SocialGlyph(index: Int, tint: Color) {
    when (index) {
        0 -> Text("G", color = tint, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        1 -> Text("f", color = tint, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        2 -> Canvas(Modifier.size(20.dp)) {
            drawCircle(tint, size.minDimension * .32f, Offset(size.width / 2f, size.height * .42f))
            drawLine(tint, Offset(size.width * .18f, size.height * .62f), Offset(size.width * .82f, size.height * .62f), size.minDimension * .09f, StrokeCap.Round)
            drawLine(tint, Offset(size.width * .3f, size.height * .62f), Offset(size.width * .3f, size.height * .88f), size.minDimension * .09f, StrokeCap.Round)
            drawLine(tint, Offset(size.width * .7f, size.height * .62f), Offset(size.width * .7f, size.height * .88f), size.minDimension * .09f, StrokeCap.Round)
        }
        else -> Canvas(Modifier.size(20.dp)) {
            val s = size.minDimension
            drawCircle(tint, s * .30f, Offset(s * .5f, s * .58f))
            drawCircle(Color.Transparent, s * .13f, Offset(s * .68f, s * .5f))
            drawOval(tint, Offset(s * .46f, s * .08f), androidx.compose.ui.geometry.Size(s * .16f, s * .2f))
        }
    }
}
