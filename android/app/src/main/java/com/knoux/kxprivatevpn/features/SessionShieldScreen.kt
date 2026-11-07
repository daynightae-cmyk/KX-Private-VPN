package com.knoux.kxprivatevpn.features

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.designsystem.Atmosphere
import com.knoux.kxprivatevpn.designsystem.GlassSurface
import com.knoux.kxprivatevpn.designsystem.KxPageHeader
import com.knoux.kxprivatevpn.platform.security.ShieldAction
import com.knoux.kxprivatevpn.platform.security.ShieldEntry
import com.knoux.kxprivatevpn.platform.security.ShieldKind

/**
 * KX Session Shield.
 *
 * The screen lists what KX itself holds. A profile that carries a credential says so and shows
 * nothing of the credential. Revoking and erasing both need an explicit approval, and the
 * receipt only reports what really happened.
 */
@Composable
fun SessionShieldRoute(night: Boolean, onBack: () -> Unit) {
    val model: SessionShieldViewModel = viewModel()
    val state by model.state.collectAsStateWithLifecycle()
    SessionShieldScreen(
        night = night,
        state = state,
        onBack = onBack,
        onRefresh = model::refresh,
        onToggle = model::toggle,
        onSelectAll = model::selectAll,
        onClearSelection = model::clearSelection,
        onRequestRevoke = model::requestRevoke,
        onRequestErase = model::requestErase,
        onDismissConfirmation = model::dismissConfirmation,
        onConfirmRevoke = model::confirmRevoke,
        onConfirmErase = model::confirmErase,
        onPrune = { model.pruneOld() },
        onOpenVpnSettings = { model.vpnSettingsIntent() },
        onOpenAppInfo = { model.appInfoIntent() },
    )
}

/** The stateless screen. Every value comes from [state]; every action is a callback. */
@Composable
fun SessionShieldScreen(
    night: Boolean,
    state: SessionShieldUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onRequestRevoke: (String) -> Unit,
    onRequestErase: () -> Unit,
    onDismissConfirmation: () -> Unit,
    onConfirmRevoke: () -> Unit,
    onConfirmErase: () -> Unit,
    onPrune: () -> Unit,
    onOpenVpnSettings: () -> Unit,
    onOpenAppInfo: () -> Unit,
) {
    Box(Modifier.fillMaxSize().testTag("route-shield")) {
        Atmosphere(night, 0f)
        Column(
            Modifier.fillMaxSize().safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KxPageHeader(
                title = stringResource(R.string.shield_title),
                night = night,
                backLabel = stringResource(R.string.back),
                onBack = onBack,
            )

            GlassSurface(night) {
                Text(stringResource(R.string.shield_owned_title), fontWeight = FontWeight.Bold,
                    fontSize = 18.sp)
                Text(stringResource(R.string.shield_owned_body), fontSize = 13.sp,
                    color = foreground(night).copy(alpha = .84f))
                Text(stringResource(R.string.shield_profiles, state.inventory.profiles.size),
                    fontSize = 14.sp, modifier = Modifier.testTag("shield-profile-count"))
                Text(stringResource(R.string.shield_history, state.inventory.history.size),
                    fontSize = 14.sp, modifier = Modifier.testTag("shield-history-count"))
                Text(stringResource(R.string.shield_secrets, state.inventory.secretCount),
                    fontSize = 14.sp, modifier = Modifier.testTag("shield-secret-count"))
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onRefresh,
                        enabled = !state.busy,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("shield-refresh"),
                    ) { Text(stringResource(R.string.shield_refresh)) }
                    OutlinedButton(
                        onClick = onSelectAll,
                        enabled = !state.busy,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("shield-select-all"),
                    ) { Text(stringResource(R.string.shield_select_all)) }
                }
            }

            state.inventory.profiles.forEachIndexed { index, entry ->
                ShieldCard(night, index, entry, state.selected.contains(entry.id),
                    busy = state.busy,
                    onToggle = { onToggle(entry.id) },
                    onRevoke = { onRequestRevoke(entry.id.substringAfter(':')) })
            }

            if (state.inventory.history.isNotEmpty()) {
                GlassSurface(night) {
                    Text(stringResource(R.string.shield_history_title), fontWeight = FontWeight.Bold,
                        fontSize = 16.sp)
                    state.inventory.history.take(MAX_SHOWN_HISTORY).forEachIndexed { index, entry ->
                        Text("${entry.title} · ${entry.detail}", fontSize = 12.sp,
                            color = foreground(night).copy(alpha = .8f),
                            modifier = Modifier.testTag("shield-history-$index"))
                    }
                    if (state.inventory.history.size > MAX_SHOWN_HISTORY) {
                        Text(stringResource(R.string.shield_history_more,
                            state.inventory.history.size - MAX_SHOWN_HISTORY), fontSize = 11.sp,
                            color = foreground(night).copy(alpha = .7f))
                    }
                }
            }

            GlassSurface(night) {
                Text(stringResource(R.string.shield_actions_title), fontWeight = FontWeight.Bold,
                    fontSize = 16.sp)
                OutlinedButton(
                    onClick = onPrune,
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("shield-prune"),
                ) { Text(stringResource(R.string.shield_prune)) }
                OutlinedButton(
                    onClick = onRequestErase,
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("shield-erase"),
                ) { Text(stringResource(R.string.shield_erase)) }
                OutlinedButton(
                    onClick = onOpenVpnSettings,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("shield-vpn-settings"),
                ) { Text(stringResource(R.string.shield_vpn_settings)) }
                OutlinedButton(
                    onClick = onOpenAppInfo,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("shield-app-info"),
                ) { Text(stringResource(R.string.shield_app_info)) }
                Text(stringResource(R.string.shield_android_guidance), fontSize = 12.sp,
                    color = foreground(night).copy(alpha = .8f),
                    modifier = Modifier.testTag("shield-guidance"))
                Text(stringResource(R.string.shield_scope), fontSize = 11.sp,
                    color = foreground(night).copy(alpha = .72f),
                    modifier = Modifier.testTag("shield-scope"))
            }

            state.receipt?.let { receipt ->
                GlassSurface(night) {
                    Text(stringResource(R.string.shield_receipt_title), fontWeight = FontWeight.Bold,
                        fontSize = 16.sp, modifier = Modifier.testTag("shield-receipt"))
                    Text(receipt.detail, fontSize = 14.sp,
                        modifier = Modifier.testTag("shield-receipt-detail"))
                    Text(stringResource(R.string.shield_receipt_counts, receipt.removed,
                        receipt.pruned), fontSize = 13.sp,
                        modifier = Modifier.testTag("shield-receipt-counts"))
                }
            }
        }
    }

    if (state.confirming == ShieldAction.REVOKE_PROFILE) {
        val entry = state.inventory.profiles.firstOrNull {
            it.id.substringAfter(':') == state.pendingProfileId
        }
        AlertDialog(
            onDismissRequest = onDismissConfirmation,
            title = { Text(stringResource(R.string.shield_revoke_title)) },
            text = { Text(stringResource(R.string.shield_revoke_body, entry?.title ?: "")) },
            confirmButton = {
                TextButton(onClick = onConfirmRevoke,
                    modifier = Modifier.testTag("shield-revoke-confirm")) {
                    Text(stringResource(R.string.shield_revoke))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissConfirmation,
                    modifier = Modifier.testTag("shield-revoke-cancel")) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (state.confirming == ShieldAction.ERASE_HISTORY) {
        AlertDialog(
            onDismissRequest = onDismissConfirmation,
            title = { Text(stringResource(R.string.shield_erase_title)) },
            text = { Text(stringResource(R.string.shield_erase_body, state.inventory.history.size)) },
            confirmButton = {
                TextButton(onClick = onConfirmErase,
                    modifier = Modifier.testTag("shield-erase-confirm")) {
                    Text(stringResource(R.string.shield_erase))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissConfirmation,
                    modifier = Modifier.testTag("shield-erase-cancel")) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ShieldCard(
    night: Boolean,
    index: Int,
    entry: ShieldEntry,
    selected: Boolean,
    busy: Boolean,
    onToggle: () -> Unit,
    onRevoke: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    val rim = if (selected) Color(0xFF79E7FF).copy(alpha = .85f)
    else Color.White.copy(alpha = if (night) .28f else .8f)
    Column(
        Modifier.fillMaxWidth()
            .background(
                Brush.linearGradient(
                    if (night) listOf(Color.White.copy(alpha = .07f), Color(0xFF6D63E8).copy(alpha = .14f))
                    else listOf(Color.White.copy(alpha = .48f), Color(0xFFCFF8FF).copy(alpha = .24f)),
                ), shape,
            )
            .border(1.dp, rim, shape)
            .padding(14.dp)
            .testTag("shield-card-$index"),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = selected, onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("shield-select-$index"))
            Column(Modifier.weight(1f)) {
                Text(entry.title, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    maxLines = 2, modifier = Modifier.testTag("shield-title-$index"))
                Text(entry.detail, fontSize = 12.sp,
                    color = foreground(night).copy(alpha = .78f),
                    modifier = Modifier.testTag("shield-detail-$index"))
                if (entry.holdsSecret) {
                    Text(stringResource(R.string.shield_holds_secret), fontSize = 11.sp,
                        color = if (night) Color(0xFFFFC98A) else Color(0xFF8A5A00),
                        modifier = Modifier.testTag("shield-secret-$index"))
                }
                if (entry.selected) {
                    Text(stringResource(R.string.shield_selected_profile), fontSize = 11.sp,
                        color = if (night) Color(0xFF64EEFF) else Color(0xFF0E6C86),
                        modifier = Modifier.testTag("shield-selected-$index"))
                }
            }
        }
        if (entry.kind == ShieldKind.PROFILE) {
            OutlinedButton(
                onClick = onRevoke,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("shield-revoke-$index"),
            ) { Text(stringResource(R.string.shield_revoke)) }
        }
    }
}

private const val MAX_SHOWN_HISTORY = 8

private fun foreground(night: Boolean): Color = if (night) Color(0xFFF7F3FF) else Color(0xFF171050)
