package com.knoux.kxprivatevpn.features

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.designsystem.*
import com.knoux.kxprivatevpn.platform.apps.InstalledApp
import com.knoux.kxprivatevpn.platform.apps.canRequestUninstall

/**
 * Installed apps, rebuilt on the owner-approved reference language (references 10 and 12): emblem
 * hero with the scan call to action, a quick summary built only from what the package manager
 * really reports, then the searchable, filterable management list with per-app handoff actions.
 *
 * Honesty rules stay absolute: per-app cache and usage sizes need a system grant KX does not ask
 * for, so the screen states that gap instead of inventing numbers, and every mutating action is a
 * handoff to Android with an explicit approval and a verified receipt.
 */
@Composable
internal fun InstalledAppsScreen(
    night: Boolean,
    state: AppInventoryState,
    ownPackage: String,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onCancel: () -> Unit,
    onQuery: (String) -> Unit,
    onFilter: (AppFilter) -> Unit,
    onSort: (AppSort) -> Unit,
    onRequestUninstall: (String) -> Unit,
    onDismissApproval: () -> Unit,
    onConfirmUninstall: () -> Unit,
    onAppSettings: (String) -> Unit,
    onAllSettings: () -> Unit,
    icon: @Composable (InstalledApp) -> Unit = {
        KxServiceGlyph("apps", glassForeground(night), Modifier.size(40.dp))
    },
) {
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    val shown = remember(state.apps, state.query, state.filter, state.sort) { state.shown }
    val scanning = state.phase == AppScanPhase.SCANNING
    KxReferenceTheme(night) {
        Box(Modifier.fillMaxSize()) {
            KxReferenceAtmosphere(night)
            LazyColumn(
                Modifier.fillMaxSize().safeDrawingPadding().testTag("route-apps"),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "header") {
                    KxReferenceHeader(
                        night = night,
                        actionLabel = stringResource(R.string.back),
                        tag = "apps-ref-back",
                        icon = KxIconKind.BACK,
                        onClick = onBack,
                    )
                }
                item(key = "hero") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        KxTitleBlock(
                            title = stringResource(R.string.app_inventory_title),
                            subtitle = stringResource(R.string.app_inventory_scope),
                            night = night,
                            announceTitleAsDescription = true,
                        )
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            KxOrbBadge(132.dp, Modifier.clearAndSetSemantics {}, night)
                        }
                        if (!scanning) {
                            KxGradientAction(
                                label = stringResource(if (state.phase == AppScanPhase.READY)
                                    R.string.apps_scan_action else R.string.apps_scan_again),
                                night = night,
                                enabled = !state.externalPending && state.approval == null,
                                tag = "apps-scan-orb",
                                icon = KxIconKind.SEARCH,
                                onClick = onScan,
                            )
                        }
                        AppScanStatus(
                            night = night,
                            state = state,
                            scanning = scanning,
                            onCancel = onCancel,
                            onAllSettings = onAllSettings,
                        )
                    }
                }
                item(key = "summary") {
                    AppSummaryCard(night = night, state = state, shownCount = shown.size)
                }
                state.receipt?.let { receipt ->
                    item(key = "receipt") { AppHandoffCard(night, receipt) }
                }
                if (state.phase == AppScanPhase.RESULTS) {
                    item(key = "results") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.apps_results), fontSize = 19.sp,
                                lineHeight = 26.sp, fontWeight = FontWeight.Bold,
                                color = kxInk(night), modifier = Modifier.semantics { heading() })
                            Text(stringResource(R.string.apps_visible_count, state.apps.size),
                                fontSize = 14.sp, lineHeight = 20.sp, color = kxMuted(night),
                                modifier = Modifier.testTag("apps-visible-count"))
                            Text(stringResource(R.string.app_inventory_counts,
                                state.apps.count { !it.system }, state.apps.count { it.system }),
                                fontSize = 12.5.sp, lineHeight = 18.sp, color = kxMuted(night))
                            Text(stringResource(R.string.apps_results_scope), fontSize = 11.5.sp,
                                lineHeight = 17.sp, color = kxMuted(night))
                        }
                    }
                    item(key = "controls") {
                        AppInventoryControls(state, shown.size, onQuery, onFilter, onSort)
                    }
                    if (shown.isEmpty()) {
                        item(key = "empty") {
                            KxGlassPanel(night, modifier = Modifier.testTag("apps-empty")) {
                                Text(stringResource(if (state.apps.isEmpty()) R.string.apps_empty_inventory
                                    else R.string.apps_no_matches), fontSize = 14.sp, lineHeight = 21.sp,
                                    color = kxInk(night))
                                if (state.query.isNotEmpty() || state.filter != AppFilter.ALL) {
                                    TextButton(onClick = { onQuery(""); onFilter(AppFilter.ALL) },
                                        modifier = Modifier.heightIn(min = 48.dp).testTag("apps-reset-filter")) {
                                        Text(stringResource(R.string.apps_reset_filter))
                                    }
                                }
                            }
                        }
                    }
                    items(shown, key = { "app:${it.packageName}" }) { app ->
                        AppInventoryCard(night, app, ownPackage, expanded == app.packageName,
                            state.canManage && state.approval == null,
                            onExpand = { expanded = if (expanded == app.packageName) null else app.packageName },
                            onInfo = { onAppSettings(app.packageName) },
                            onUninstall = { onRequestUninstall(app.packageName) }, icon = icon)
                    }
                }
            }
        }
        state.approval?.let { approved ->
            // Dialog windows resolve resources against the Activity context. Resolve the copy
            // here, in the localized screen composition, so Arabic never falls back to English.
            val dialogTitle = stringResource(R.string.app_inventory_confirm_title)
            val dialogBody = stringResource(R.string.app_inventory_confirm_body, approved.label)
            val dialogConfirm = stringResource(R.string.apps_open_confirmation)
            val dialogCancel = stringResource(R.string.cancel)
            AlertDialog(
                onDismissRequest = onDismissApproval,
                title = { Text(dialogTitle) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(approved.label, fontWeight = FontWeight.Bold)
                        Text(approved.packageName, modifier = Modifier.testTag("apps-approved-package"))
                        Text(dialogBody)
                    }
                },
                confirmButton = {
                    TextButton(onClick = onConfirmUninstall,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("apps-confirm-uninstall")) {
                        Text(dialogConfirm)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissApproval,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("apps-dismiss-uninstall")) {
                        Text(dialogCancel)
                    }
                },
            )
        }
    }
}

/** Scanning, failure and cancellation states plus the Android Settings handoff. */
@Composable
private fun AppScanStatus(
    night: Boolean,
    state: AppInventoryState,
    scanning: Boolean,
    onCancel: () -> Unit,
    onAllSettings: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (scanning) {
            LinearProgressIndicator(Modifier.fillMaxWidth().testTag("apps-progress"))
            Text(stringResource(R.string.apps_scanning), fontSize = 13.sp, lineHeight = 19.sp,
                color = kxMuted(night),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            OutlinedButton(onClick = onCancel,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("apps-cancel-scan")) {
                Text(stringResource(R.string.cancel))
            }
        } else {
            if (state.phase == AppScanPhase.FAILED || state.phase == AppScanPhase.CANCELLED) {
                Text(stringResource(if (state.phase == AppScanPhase.FAILED)
                    R.string.app_inventory_failure else R.string.apps_cancelled),
                    fontSize = 13.sp, lineHeight = 19.sp, color = kxRose(night),
                    modifier = Modifier.testTag("apps-scan-status").semantics { liveRegion = LiveRegionMode.Polite })
            }
        }
        OutlinedButton(onClick = onAllSettings,
            enabled = !scanning && !state.externalPending && state.approval == null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("apps-system-settings")) {
            Text(stringResource(R.string.app_inventory_all_settings))
        }
    }
}

/** Reference 10's quick summary, populated only from package-manager truth. */
@Composable
private fun AppSummaryCard(night: Boolean, state: AppInventoryState, shownCount: Int) {
    val results = state.phase == AppScanPhase.RESULTS
    val total = if (results) state.apps.size.toString() else stringResource(R.string.apps_not_scanned)
    val user = if (results) state.apps.count { !it.system }.toString() else "—"
    val system = if (results) state.apps.count { it.system }.toString() else "—"
    val shown = if (results) shownCount.toString() else "—"
    KxGlassPanel(night) {
        KxSectionHeaderRow(
            title = stringResource(R.string.apps_summary),
            icon = KxIconKind.LAYERS,
            night = night,
        )
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.border(1.dp, kxViolet(night).copy(alpha = .5f), RoundedCornerShape(18.dp))
                    .background(kxViolet(night).copy(alpha = .16f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(total, color = kxCyan(night), fontSize = 24.sp, lineHeight = 30.sp,
                        fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.apps_summary_total), color = kxMuted(night),
                        fontSize = 10.5.sp, lineHeight = 15.sp, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.apps_review_title), color = kxInk(night),
                fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppSummaryTile(night, user, stringResource(R.string.apps_summary_user),
                kxGreen(night), Modifier.weight(1f))
            AppSummaryTile(night, system, stringResource(R.string.apps_summary_system),
                kxRose(night), Modifier.weight(1f))
            AppSummaryTile(night, shown, stringResource(R.string.apps_summary_shown),
                kxCyan(night), Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.apps_storage_note), color = kxMuted(night),
            fontSize = 10.5.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun AppSummaryTile(night: Boolean, value: String, label: String, accent: Color, modifier: Modifier) {
    KxGlassPanel(night, modifier = modifier, radius = 16.dp,
        padding = PaddingValues(vertical = 10.dp, horizontal = 6.dp)) {
        Text(value, color = accent, fontSize = 16.sp, lineHeight = 22.sp,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth())
        Text(label, color = kxMuted(night), fontSize = 10.sp, lineHeight = 14.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppInventoryControls(
    state: AppInventoryState,
    shownCount: Int,
    onQuery: (String) -> Unit,
    onFilter: (AppFilter) -> Unit,
    onSort: (AppSort) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(state.query, onQuery, singleLine = true,
            label = { Text(stringResource(R.string.apps_search_label)) },
            leadingIcon = { KxLineIcon(KxIconKind.SEARCH, kxMuted(night = true), Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth().testTag("apps-search"))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppFilter.entries.forEach { filter ->
                FilterChip(selected = state.filter == filter, onClick = { onFilter(filter) },
                    label = { Text(stringResource(when (filter) {
                        AppFilter.ALL -> R.string.app_inventory_filter_all
                        AppFilter.USER -> R.string.app_inventory_filter_user
                        AppFilter.SYSTEM -> R.string.app_inventory_filter_system
                    })) }, modifier = Modifier.heightIn(min = 48.dp)
                        .testTag("apps-filter-${filter.name.lowercase(java.util.Locale.ROOT)}"))
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppSort.entries.forEach { sort ->
                FilterChip(selected = state.sort == sort, onClick = { onSort(sort) },
                    label = { Text(stringResource(if (sort == AppSort.NAME)
                        R.string.apps_sort_name else R.string.apps_sort_package)) },
                    modifier = Modifier.heightIn(min = 48.dp)
                        .testTag("apps-sort-${sort.name.lowercase(java.util.Locale.ROOT)}"))
            }
        }
        Text(stringResource(R.string.apps_shown_count, shownCount, state.apps.size), fontSize = 12.sp,
            lineHeight = 18.sp, color = kxMuted(night = true),
            modifier = Modifier.testTag("apps-shown-count").semantics { liveRegion = LiveRegionMode.Polite })
    }
}

@Composable
private fun AppInventoryCard(
    night: Boolean,
    app: InstalledApp,
    ownPackage: String,
    expanded: Boolean,
    canManage: Boolean,
    onExpand: () -> Unit,
    onInfo: () -> Unit,
    onUninstall: () -> Unit,
    icon: @Composable (InstalledApp) -> Unit,
) {
    val detailsLabel = stringResource(if (expanded) R.string.apps_hide_details else R.string.apps_show_details)
    // At large font scales the labelled action tiles would need to wrap inside a narrow pill, so
    // fall back to full-width labelled buttons; the tiles are the reference look at normal scale.
    val compact = LocalDensity.current.fontScale < 1.3f
    KxGlassPanel(night, radius = 20.dp, modifier = Modifier.testTag("apps-card-${app.packageName}")) {
        Column(Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClickLabel = detailsLabel, onClick = onExpand)
            .testTag("apps-details-${app.packageName}")) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(44.dp).clearAndSetSemantics {}) { icon(app) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(app.label, fontSize = 15.sp, lineHeight = 21.sp,
                        fontWeight = FontWeight.Bold, color = kxInk(night))
                    Text(app.packageName, fontSize = 11.5.sp, lineHeight = 17.sp, color = kxMuted(night))
                    Text(stringResource(when {
                        app.system -> R.string.app_inventory_system_protected
                        app.packageName == ownPackage -> R.string.apps_self_protected
                        else -> R.string.app_inventory_user_app
                    }), fontSize = 11.5.sp, lineHeight = 17.sp, color = kxMuted(night))
                }
                KxChevron(kxMuted(night), Modifier.size(16.dp), pointsToStart = expanded)
            }
            Text(detailsLabel, fontSize = 12.sp, lineHeight = 18.sp, color = kxCyan(night),
                modifier = Modifier.padding(top = 6.dp))
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Text(stringResource(R.string.apps_version, app.versionName ?: stringResource(R.string.unknown_value)),
                fontSize = 12.5.sp, lineHeight = 19.sp, color = kxInk(night))
            Text(stringResource(R.string.apps_target_sdk,
                app.targetSdk?.toString() ?: stringResource(R.string.unknown_value)),
                fontSize = 12.5.sp, lineHeight = 19.sp, color = kxInk(night))
            Text(stringResource(R.string.apps_storage_unknown), fontSize = 11.5.sp, lineHeight = 17.sp,
                color = kxMuted(night), modifier = Modifier.padding(vertical = 6.dp))
            if (compact) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppActionTile(
                        night = night,
                        label = stringResource(R.string.app_inventory_app_info),
                        icon = KxIconKind.GEAR,
                        accent = kxCyan(night),
                        enabled = canManage,
                        tag = "apps-info-${app.packageName}",
                        modifier = Modifier.weight(1f),
                        onClick = onInfo,
                    )
                    if (canRequestUninstall(app, ownPackage)) {
                        AppActionTile(
                            night = night,
                            label = stringResource(R.string.app_inventory_uninstall),
                            icon = KxIconKind.BROOM,
                            accent = kxRose(night),
                            enabled = canManage,
                            tag = "apps-uninstall-${app.packageName}",
                            modifier = Modifier.weight(1f),
                            onClick = onUninstall,
                        )
                    }
                }
            } else {
                OutlinedButton(onClick = onInfo, enabled = canManage,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("apps-info-${app.packageName}")) {
                    Text(stringResource(R.string.app_inventory_app_info))
                }
                if (canRequestUninstall(app, ownPackage)) {
                    TextButton(onClick = onUninstall, enabled = canManage,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("apps-uninstall-${app.packageName}")) {
                        Text(stringResource(R.string.app_inventory_uninstall))
                    }
                }
            }
        }
    }
}

/** Reference 12's labelled action tile; only used where the label can wrap safely. */
@Composable
private fun AppActionTile(
    night: Boolean,
    label: String,
    icon: KxIconKind,
    accent: Color,
    enabled: Boolean,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier.heightIn(min = 48.dp)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick)
            .border(1.dp, accent.copy(alpha = if (enabled) .5f else .25f), RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = if (enabled) .16f else .06f), RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        KxLineIcon(icon, if (enabled) accent else kxMuted(night), Modifier.size(18.dp))
        Text(label, color = if (enabled) kxInk(night) else kxMuted(night), fontSize = 10.5.sp,
            lineHeight = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AppHandoffCard(night: Boolean, receipt: AppManagementReceipt) {
    KxGlassPanel(night, modifier = Modifier.testTag("apps-handoff")) {
        KxSectionHeaderRow(
            title = stringResource(R.string.apps_handoff_title),
            icon = KxIconKind.FILE,
            night = night,
        )
        Text(stringResource(when (receipt.action) {
            AppManagementAction.DETAILS -> R.string.app_inventory_app_info
            AppManagementAction.UNINSTALL -> R.string.app_inventory_uninstall
            AppManagementAction.SETTINGS -> R.string.app_inventory_all_settings
        }), fontSize = 13.sp, lineHeight = 19.sp, color = kxMuted(night),
            modifier = Modifier.padding(top = 6.dp))
        receipt.target?.let { app ->
            Text(app.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                lineHeight = 20.sp, color = kxInk(night))
            Text(app.packageName, fontSize = 12.sp, lineHeight = 18.sp, color = kxMuted(night))
        }
        val targetName = receipt.target?.label ?: receipt.target?.packageName.orEmpty()
        val status = when (receipt.outcome) {
            AppManagementOutcome.OPENED -> stringResource(R.string.apps_handoff_opened)
            AppManagementOutcome.RETURNED -> stringResource(R.string.apps_handoff_returned)
            AppManagementOutcome.REMOVED ->
                stringResource(R.string.apps_handoff_removed, targetName)
            AppManagementOutcome.STILL_INSTALLED ->
                stringResource(R.string.apps_handoff_still_installed, targetName)
            AppManagementOutcome.VERIFY_FAILED ->
                stringResource(R.string.apps_handoff_verify_failed)
            AppManagementOutcome.UNAVAILABLE -> stringResource(R.string.apps_handoff_unavailable)
            AppManagementOutcome.BLOCKED -> stringResource(R.string.apps_handoff_blocked)
        }
        Text(status, fontSize = 13.sp, lineHeight = 19.sp, color = kxInk(night),
            modifier = Modifier.padding(top = 6.dp).testTag("apps-handoff-status")
                .semantics { liveRegion = LiveRegionMode.Polite })
    }
}
