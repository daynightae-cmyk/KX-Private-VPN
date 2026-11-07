package com.knoux.kxprivatevpn.features

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.designsystem.Atmosphere
import com.knoux.kxprivatevpn.designsystem.GlassSurface
import com.knoux.kxprivatevpn.designsystem.KxGlassTheme
import com.knoux.kxprivatevpn.designsystem.KxPageHeader
import com.knoux.kxprivatevpn.platform.security.ProConfiguration
import com.knoux.kxprivatevpn.platform.security.ProIdentityStore
import com.knoux.kxprivatevpn.platform.security.ProRepository
import com.knoux.kxprivatevpn.platform.security.isProHex
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
internal fun ProRoute(night: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val url = stringResource(R.string.pro_authority_url)
    val contact = stringResource(R.string.pro_owner_whatsapp_e164)
    val configuration = remember(url, contact) { ProConfiguration(url, contact) }
    val presenter = remember(configuration) {
        ProActivationState(ProRepository(configuration, ProIdentityStore(context.applicationContext)))
    }
    val state by presenter.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val request = stringResource(R.string.pro_contact_request)
    var contactError by remember { mutableStateOf(false) }
    ProScreen(night, state, onBack,
        onRefresh = { scope.launch { presenter.refresh() } },
        onRedeem = { code -> scope.launch { presenter.redeem(code) } },
        onContact = {
            configuration.contactUrl(request)?.let { target ->
                contactError = false
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
                } catch (_: ActivityNotFoundException) { contactError = true }
                catch (_: SecurityException) { contactError = true }
            }
        }, contactError = contactError)
}

/** Stateless license status/callbacks; the secret input lives only in this composition. */
@Composable
internal fun ProScreen(
    night: Boolean,
    state: ProUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRedeem: (String) -> Unit,
    onContact: () -> Unit,
    contactError: Boolean = false,
) {
    var code by remember { mutableStateOf("") } // Never rememberSaveable, prefs, arguments or logs.
    val activity = generateSequence(LocalView.current.context) { (it as? ContextWrapper)?.baseContext }
        .filterIsInstance<Activity>().firstOrNull()
    DisposableEffect(activity) {
        val window = activity?.window
        val alreadySecure = ((window?.attributes?.flags ?: 0) and WindowManager.LayoutParams.FLAG_SECURE) != 0
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { if (!alreadySecure) window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
    val locale = LocalConfiguration.current.locales[0]
    KxGlassTheme(night) {
        Box(Modifier.fillMaxSize().testTag("pro-screen")) {
            Atmosphere(night, 0f)
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                KxPageHeader(stringResource(R.string.pro_title), night, stringResource(R.string.back), onBack)
                Text(stringResource(R.string.pro_scope), modifier = Modifier.testTag("pro-scope"))
                GlassSurface(night) {
                    Text(stringResource(statusText(state.status)),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.testTag("pro-status").semantics { liveRegion = LiveRegionMode.Polite })
                    state.expiresAtMillis?.let { expiry ->
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.pro_expiry,
                            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(expiry))),
                            modifier = Modifier.testTag("pro-expiry"))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.pro_online_only), modifier = Modifier.testTag("pro-online"))
                    if (state.busy) {
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(Modifier.fillMaxWidth().testTag("pro-busy"))
                        Text(stringResource(R.string.pro_checking))
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { if (state.configured && !state.busy) onRefresh() },
                        enabled = state.configured && !state.busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("pro-refresh")) {
                        Text(stringResource(R.string.pro_refresh))
                    }
                }
                OutlinedTextField(code, { if (it.length <= 64) code = it },
                    enabled = state.configured && !state.busy, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.Password),
                    label = { Text(stringResource(R.string.pro_code)) },
                    supportingText = { Text(stringResource(R.string.pro_code_hint)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("pro-code"))
                Button(onClick = {
                    if (state.configured && !state.busy && isProHex(code)) {
                        val submitted = code
                        code = "" // Clear before invoking a callback, including failure/cancellation.
                        onRedeem(submitted)
                    }
                }, enabled = state.configured && !state.busy && isProHex(code),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("pro-redeem")) {
                    Text(stringResource(R.string.pro_redeem))
                }
                Text(stringResource(R.string.pro_code_privacy))
                OutlinedButton(onClick = { if (state.contactAvailable && !state.busy) onContact() },
                    enabled = state.contactAvailable && !state.busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("pro-contact")) {
                    Text(stringResource(R.string.pro_contact))
                }
                Text(stringResource(if (state.contactAvailable) R.string.pro_contact_hint else R.string.pro_contact_absent),
                    modifier = Modifier.testTag("pro-contact-note"))
                if (contactError) Text(stringResource(R.string.pro_contact_error), modifier = Modifier.testTag("pro-contact-error"))
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

private fun statusText(status: ProStatus): Int = when (status) {
    ProStatus.NOT_CHECKED -> R.string.pro_not_checked
    ProStatus.UNCONFIGURED -> R.string.pro_unconfigured
    ProStatus.UNAVAILABLE -> R.string.pro_unavailable
    ProStatus.INACTIVE -> R.string.pro_inactive
    ProStatus.INVALID -> R.string.pro_invalid
    ProStatus.RATE_LIMITED -> R.string.pro_rate_limited
    ProStatus.IDENTITY_UNAVAILABLE -> R.string.pro_identity_unavailable
    ProStatus.EXPIRED -> R.string.pro_expired
    ProStatus.REVOKED -> R.string.pro_revoked
    ProStatus.ACTIVE_SNAPSHOT -> R.string.pro_active_snapshot
}
