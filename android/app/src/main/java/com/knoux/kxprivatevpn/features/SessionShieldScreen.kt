package com.knoux.kxprivatevpn.features

import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
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
import com.knoux.kxprivatevpn.platform.security.ShieldReceiptCode

/**
 * KX Session Shield.
 *
 * The screen lists what KX itself holds. A profile that carries a credential says so and shows
 * nothing of the credential. Revoking and erasing both need an explicit approval, and the
 * receipt only reports what really happened. Every stored key is translated before it is shown.
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
        onRequestRevokeSelected = model::requestRevokeSelected,
        onRequestErase = model::requestErase,
        onDismissConfirmation = model::dismissConfirmation,
        onConfirmRevoke = model::confirmRevoke,
        onConfirmRevokeSelected = model::confirmRevokeSelected,
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
    onRequestRevokeSelected: () -> Unit,
    onRequestErase: () -> Unit,
    onDismissConfirmation: () -> Unit,
    onConfirmRevoke: () -> Unit,
    onConfirmRevokeSelected: () -> Unit,
    onConfirmErase: () -> Unit,
    onPrune: () -> Unit,
    onOpenVpnSettings: () -> Unit,
    onOpenAppInfo: () -> Unit,
) {
    val context = LocalContext.current
    val foreground = foreground(night)
    val selectedCount = state.selectedProfiles.size
    val canAct = !state.busy && !state.loading

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
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.shield_owned_body), fontSize = 13.sp,
                    color = foreground.copy(alpha = .84f))

                if (!state.loading && !state.inventory.failed) {
                    Spacer(Modifier.height(10.dp))
                    Text(pluralStringResource(R.plurals.shield_profiles_count,
                        state.inventory.profiles.size, state.inventory.profiles.size),
                        fontSize = 14.sp, modifier = Modifier.testTag("shield-profile-count"))
                    Text(pluralStringResource(R.plurals.shield_history_count,
                        state.inventory.history.size, state.inventory.history.size),
                        fontSize = 14.sp, modifier = Modifier.testTag("shield-history-count"))
                    Text(pluralStringResource(R.plurals.shield_secrets_count,
                        state.inventory.secretCount, state.inventory.secretCount),
                        fontSize = 14.sp, modifier = Modifier.testTag("shield-secret-count"))
                }

                when {
                    state.loading -> {
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.shield_loading), fontSize = 13.sp,
                            color = foreground.copy(alpha = .8f),
                            modifier = Modifier.testTag("shield-loading"))
                    }
                    state.inventory.failed -> {
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.shield_load_failed), fontSize = 13.sp,
                            color = if (night) Color(0xFFFFB4B4) else Color(0xFFB3261E),
                            modifier = Modifier.testTag("shield-failed"))
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onRefresh,
                            enabled = !state.busy,
                            colors = shieldButtonColors(night),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .testTag("shield-retry"),
                        ) { Text(stringResource(R.string.shield_retry)) }
                    }
                    state.inventory.entries.isEmpty() -> {
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.shield_empty_title), fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("shield-empty"))
                        Spacer(Modifier.height(3.dp))
                        Text(stringResource(R.string.shield_empty_body), fontSize = 13.sp,
                            color = foreground.copy(alpha = .8f))
                    }
                }

                // In a failed read the retry above is the only refresh action; two identical
                // buttons would just look like a bug.
                if (!state.inventory.failed) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onRefresh,
                        enabled = canAct,
                        colors = shieldButtonColors(night),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("shield-refresh"),
                    ) { Text(stringResource(R.string.shield_refresh)) }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onSelectAll,
                    enabled = canAct && state.inventory.profiles.isNotEmpty(),
                    colors = shieldButtonColors(night),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("shield-select-all"),
                ) { Text(stringResource(R.string.shield_select_all)) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onClearSelection,
                    enabled = canAct && state.selected.isNotEmpty(),
                    colors = shieldButtonColors(night),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("shield-clear"),
                ) { Text(stringResource(R.string.shield_clear_selection)) }
                if (selectedCount > 0) {
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.shield_selected_count, selectedCount),
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = if (night) Color(0xFF64EEFF) else Color(0xFF0E6C86),
                        modifier = Modifier.testTag("shield-selected-count"))
                }
                if (state.inventory.profiles.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onRequestRevokeSelected,
                        enabled = canAct && selectedCount > 0,
                        colors = shieldButtonColors(night),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("shield-revoke-selected"),
                    ) {
                        Text(stringResource(R.string.shield_revoke_selected, selectedCount))
                    }
                }
            }

            if (state.inventory.profiles.isNotEmpty()) {
                Text(stringResource(R.string.shield_profiles_title), fontWeight = FontWeight.Bold,
                    fontSize = 16.sp, color = foreground,
                    modifier = Modifier.padding(horizontal = 2.dp))
                state.inventory.profiles.forEachIndexed { index, entry ->
                    ShieldCard(
                        night = night,
                        index = index,
                        entry = entry,
                        selected = state.selected.contains(entry.id),
                        busy = state.busy || state.loading,
                        onToggle = { onToggle(entry.id) },
                        onRevoke = { onRequestRevoke(entry.id.substringAfter(':')) },
                    )
                }
            }

            if (state.inventory.history.isNotEmpty()) {
                GlassSurface(night) {
                    Text(stringResource(R.string.shield_history_title), fontWeight = FontWeight.Bold,
                        fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    state.inventory.history.take(MAX_SHOWN_HISTORY).forEachIndexed { index, entry ->
                        ShieldHistoryRow(night, index, entry)
                    }
                    if (state.inventory.history.size > MAX_SHOWN_HISTORY) {
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.shield_history_more,
                            state.inventory.history.size - MAX_SHOWN_HISTORY), fontSize = 11.sp,
                            color = foreground.copy(alpha = .7f))
                    }
                }
            }

            GlassSurface(night) {
                Text(stringResource(R.string.shield_actions_title), fontWeight = FontWeight.Bold,
                    fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onPrune,
                    enabled = canAct && state.inventory.history.isNotEmpty(),
                    colors = shieldButtonColors(night),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("shield-prune"),
                ) { Text(stringResource(R.string.shield_prune)) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRequestErase,
                    enabled = canAct && state.inventory.history.isNotEmpty(),
                    colors = shieldButtonColors(night),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("shield-erase"),
                ) { Text(stringResource(R.string.shield_erase)) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onOpenVpnSettings,
                    colors = shieldButtonColors(night),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("shield-vpn-settings"),
                ) { Text(stringResource(R.string.shield_vpn_settings)) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onOpenAppInfo,
                    colors = shieldButtonColors(night),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("shield-app-info"),
                ) { Text(stringResource(R.string.shield_app_info)) }
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.shield_android_guidance), fontSize = 12.sp,
                    color = foreground.copy(alpha = .8f),
                    modifier = Modifier.testTag("shield-guidance"))
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.shield_scope), fontSize = 11.sp,
                    color = foreground.copy(alpha = .72f),
                    modifier = Modifier.testTag("shield-scope"))
            }

            state.receipt?.let { receipt ->
                GlassSurface(night) {
                    Text(stringResource(R.string.shield_receipt_title), fontWeight = FontWeight.Bold,
                        fontSize = 16.sp, modifier = Modifier.testTag("shield-receipt"))
                    Spacer(Modifier.height(4.dp))
                    Text(receiptText(receipt.code), fontSize = 14.sp,
                        modifier = Modifier.testTag("shield-receipt-detail"))
                    Text(stringResource(R.string.shield_receipt_counts, receipt.removed,
                        receipt.pruned), fontSize = 13.sp,
                        modifier = Modifier.testTag("shield-receipt-counts"))
                    Text(
                        DateUtils.formatDateTime(
                            context,
                            receipt.atMillis,
                            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or
                                DateUtils.FORMAT_ABBREV_MONTH,
                        ),
                        fontSize = 11.sp, color = foreground.copy(alpha = .62f),
                    )
                }
            }
        }
    }

    if (state.confirming == ShieldAction.REVOKE_PROFILE) {
        val entry = state.inventory.profiles.firstOrNull {
            it.id.substringAfter(':') == state.pendingProfileId
        }
        // A dialog owns a separate Android window/context. Resolve its copy in this localized
        // composition; otherwise the dialog falls back to the Activity's previous resources.
        val dialogTitle = stringResource(R.string.shield_revoke_title)
        val dialogBody = stringResource(R.string.shield_revoke_body,
            entry?.title ?: stringResource(R.string.shield_unknown_profile))
        val dialogConfirm = stringResource(R.string.shield_revoke)
        val dialogCancel = stringResource(R.string.cancel)
        AlertDialog(
            onDismissRequest = onDismissConfirmation,
            title = { Text(dialogTitle) },
            text = { Text(dialogBody) },
            confirmButton = {
                TextButton(onClick = onConfirmRevoke,
                    modifier = Modifier.testTag("shield-revoke-confirm")) {
                    Text(dialogConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissConfirmation,
                    modifier = Modifier.testTag("shield-revoke-cancel")) {
                    Text(dialogCancel)
                }
            },
        )
    }

    if (state.confirming == ShieldAction.REVOKE_SELECTED) {
        val dialogTitle = stringResource(R.string.shield_revoke_selected_title)
        val dialogBody = pluralStringResource(R.plurals.shield_revoke_selected_body_count,
            selectedCount, selectedCount)
        val dialogConfirm = pluralStringResource(R.plurals.shield_revoke_selected_confirm_count,
            selectedCount, selectedCount)
        val dialogCancel = stringResource(R.string.cancel)
        AlertDialog(
            onDismissRequest = onDismissConfirmation,
            title = { Text(dialogTitle) },
            text = { Text(dialogBody) },
            confirmButton = {
                TextButton(onClick = onConfirmRevokeSelected,
                    modifier = Modifier.testTag("shield-revoke-selected-confirm")) {
                    Text(dialogConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissConfirmation,
                    modifier = Modifier.testTag("shield-revoke-selected-cancel")) {
                    Text(dialogCancel)
                }
            },
        )
    }

    if (state.confirming == ShieldAction.ERASE_HISTORY) {
        val dialogTitle = stringResource(R.string.shield_erase_title)
        val dialogBody = pluralStringResource(R.plurals.shield_erase_body_count,
            state.inventory.history.size, state.inventory.history.size)
        val dialogConfirm = stringResource(R.string.shield_erase)
        val dialogCancel = stringResource(R.string.cancel)
        AlertDialog(
            onDismissRequest = onDismissConfirmation,
            title = { Text(dialogTitle) },
            text = { Text(dialogBody) },
            confirmButton = {
                TextButton(onClick = onConfirmErase,
                    modifier = Modifier.testTag("shield-erase-confirm")) {
                    Text(dialogConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissConfirmation,
                    modifier = Modifier.testTag("shield-erase-cancel")) {
                    Text(dialogCancel)
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
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(
                    value = selected,
                    enabled = !busy,
                    role = Role.Checkbox,
                    onValueChange = { onToggle() },
                )
                .testTag("shield-select-row-$index"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = null,
                enabled = !busy,
                modifier = Modifier.testTag("shield-select-$index"),
            )
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
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onRevoke,
                enabled = !busy,
                colors = shieldButtonColors(night),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .testTag("shield-revoke-$index"),
            ) { Text(stringResource(R.string.shield_revoke)) }
        }
    }
}

@Composable
private fun ShieldHistoryRow(night: Boolean, index: Int, entry: ShieldEntry) {
    val context = LocalContext.current
    val title = when (entry.kind) {
        ShieldKind.REPAIR_HISTORY -> repairActionLabel(entry.actionKey)
        ShieldKind.USAGE_HISTORY -> entry.title.ifBlank {
            stringResource(R.string.shield_history_usage_fallback)
        }
        ShieldKind.SESSION_HINT -> stringResource(R.string.shield_hint_title)
        ShieldKind.PROFILE -> entry.title
    }
    val detail = when (entry.kind) {
        ShieldKind.REPAIR_HISTORY -> stringResource(
            R.string.shield_history_repair_detail,
            repairOutcomeLabel(entry.outcomeKey),
            pluralStringResource(R.plurals.shield_items_count, entry.items, entry.items),
            Formatter.formatFileSize(context, entry.bytes),
        )
        ShieldKind.USAGE_HISTORY -> {
            val endpoint = entry.detail.ifBlank {
                stringResource(R.string.shield_history_no_endpoint)
            }
            val duration = entry.durationMillis.takeIf { it > 0 }
                ?.let { DateUtils.formatElapsedTime(it / 1000) }
            val recovered = if (entry.recovered) {
                stringResource(R.string.shield_history_recovered)
            } else null
            listOfNotNull(endpoint, Formatter.formatFileSize(context, entry.bytes),
                duration, recovered).joinToString(" · ")
        }
        ShieldKind.SESSION_HINT -> stringResource(R.string.shield_hint_detail)
        ShieldKind.PROFILE -> entry.detail
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)
        .semantics(mergeDescendants = true) {}
        .testTag("shield-history-$index")) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2,
            color = foreground(night))
        Text(detail, fontSize = 11.sp, color = foreground(night).copy(alpha = .76f))
        if (entry.atMillis > 0) {
            Text(
                DateUtils.formatDateTime(
                    context,
                    entry.atMillis,
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or
                        DateUtils.FORMAT_ABBREV_MONTH,
                ),
                fontSize = 10.sp, color = foreground(night).copy(alpha = .58f),
            )
        }
    }
}

@Composable
private fun receiptText(code: ShieldReceiptCode): String = stringResource(
    when (code) {
        ShieldReceiptCode.SECRET_REMOVED -> R.string.shield_receipt_secret_removed
        ShieldReceiptCode.SECRET_STILL_PRESENT -> R.string.shield_receipt_secret_still_present
        ShieldReceiptCode.SECRET_UNVERIFIED -> R.string.shield_receipt_secret_unverified
        ShieldReceiptCode.SECRETS_REMOVED -> R.string.shield_receipt_secrets_removed
        ShieldReceiptCode.SECRETS_PARTIAL -> R.string.shield_receipt_secrets_partial
        ShieldReceiptCode.SECRETS_NONE_REMOVED -> R.string.shield_receipt_secrets_none_removed
        ShieldReceiptCode.SECRETS_UNVERIFIED -> R.string.shield_receipt_secrets_unverified
        ShieldReceiptCode.ERASED -> R.string.shield_receipt_erased
        ShieldReceiptCode.ERASE_INCOMPLETE -> R.string.shield_receipt_erase_incomplete
        ShieldReceiptCode.PRUNED -> R.string.shield_receipt_pruned
        ShieldReceiptCode.PRUNE_INCOMPLETE -> R.string.shield_receipt_prune_incomplete
    },
)

private const val MAX_SHOWN_HISTORY = 8

private fun foreground(night: Boolean): Color = if (night) Color(0xFFF7F3FF) else Color(0xFF171050)

/**
 * The stock outlined button inherits the theme primary, which is too dark on the night glass.
 * The explicit content colour keeps every enabled action readable in both themes; disabled
 * actions still dim through Material's own disabled alpha.
 */
@Composable
private fun shieldButtonColors(night: Boolean) = ButtonDefaults.outlinedButtonColors(
    contentColor = if (night) Color(0xFFEDE7FF) else Color(0xFF3F3390),
)
