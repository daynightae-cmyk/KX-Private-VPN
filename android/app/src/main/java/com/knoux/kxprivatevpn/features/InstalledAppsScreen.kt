package com.knoux.kxprivatevpn.features

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.designsystem.*
import com.knoux.kxprivatevpn.launch.IntroTimeline
import com.knoux.kxprivatevpn.platform.apps.InstalledApp
import com.knoux.kxprivatevpn.platform.apps.canRequestUninstall

/** Stateless inventory UI. Tests supply fixtures; only the route can hand off to Android. */
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
    KxGlassTheme(night) {
        Box(Modifier.fillMaxSize()) {
            Atmosphere(night, 0f)
            LazyColumn(
                Modifier.fillMaxSize().safeDrawingPadding().testTag("route-apps"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "header") {
                    KxPageHeader(stringResource(R.string.app_inventory_title), night,
                        stringResource(R.string.back), onBack)
                }
                item(key = "scope") {
                    AppScanCard(night, state, onScan, onCancel, onAllSettings)
                }
                state.receipt?.let { receipt ->
                    item(key = "receipt") { AppHandoffCard(night, receipt) }
                }
                if (state.phase == AppScanPhase.RESULTS) {
                    item(key = "results") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.apps_results), fontSize = 20.sp,
                                fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                            Text(stringResource(R.string.apps_visible_count, state.apps.size),
                                fontSize = 16.sp, modifier = Modifier.testTag("apps-visible-count"))
                            Text(stringResource(R.string.app_inventory_counts,
                                state.apps.count { !it.system }, state.apps.count { it.system }),
                                fontSize = 14.sp, lineHeight = 20.sp)
                            Text(stringResource(R.string.apps_results_scope), fontSize = 13.sp,
                                lineHeight = 19.sp)
                        }
                    }
                    item(key = "controls") {
                        AppInventoryControls(state, shown.size, onQuery, onFilter, onSort)
                    }
                    if (shown.isEmpty()) {
                        item(key = "empty") {
                            GlassSurface(night, Modifier.testTag("apps-empty")) {
                                Text(stringResource(if (state.apps.isEmpty()) R.string.apps_empty_inventory
                                    else R.string.apps_no_matches), fontSize = 15.sp, lineHeight = 22.sp)
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

@Composable
private fun AppScanCard(
    night: Boolean,
    state: AppInventoryState,
    onScan: () -> Unit,
    onCancel: () -> Unit,
    onAllSettings: () -> Unit,
) {
    val scanning = state.phase == AppScanPhase.SCANNING
    GlassSurface(night) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KxOrb(1f, IntroTimeline.frame(1f), Modifier.size(64.dp).clearAndSetSemantics {})
            Text(stringResource(R.string.apps_review_title), fontSize = 20.sp,
                lineHeight = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.app_inventory_scope), fontSize = 14.sp, lineHeight = 21.sp)
        Spacer(Modifier.height(12.dp))
        if (scanning) {
            LinearProgressIndicator(Modifier.fillMaxWidth().testTag("apps-progress"))
            Text(stringResource(R.string.apps_scanning), fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp).semantics { liveRegion = LiveRegionMode.Polite })
            OutlinedButton(onClick = onCancel,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("apps-cancel-scan")) {
                Text(stringResource(R.string.cancel))
            }
        } else {
            if (state.phase == AppScanPhase.FAILED || state.phase == AppScanPhase.CANCELLED) {
                Text(stringResource(if (state.phase == AppScanPhase.FAILED)
                    R.string.app_inventory_failure else R.string.apps_cancelled),
                    fontSize = 14.sp, lineHeight = 21.sp,
                    modifier = Modifier.testTag("apps-scan-status").semantics { liveRegion = LiveRegionMode.Polite })
            }
            Button(onClick = onScan, enabled = !state.externalPending && state.approval == null,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("apps-scan-orb")) {
                Text(stringResource(if (state.phase == AppScanPhase.READY) R.string.apps_scan_action
                    else R.string.apps_scan_again))
            }
        }
        TextButton(onClick = onAllSettings,
            enabled = !scanning && !state.externalPending && state.approval == null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("apps-system-settings")) {
            Text(stringResource(R.string.app_inventory_all_settings))
        }
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
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(state.query, onQuery, singleLine = true,
            label = { Text(stringResource(R.string.apps_search_label)) },
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
        Text(stringResource(R.string.apps_shown_count, shownCount, state.apps.size), fontSize = 13.sp,
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
    GlassSurface(night, Modifier.testTag("apps-card-${app.packageName}")) {
        Column(Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClickLabel = detailsLabel, onClick = onExpand)
            .testTag("apps-details-${app.packageName}")) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(40.dp).clearAndSetSemantics {}) { icon(app) }
                Text(app.label, fontSize = 17.sp, lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            Text(app.packageName, fontSize = 13.sp, lineHeight = 19.sp,
                modifier = Modifier.padding(top = 8.dp))
            Text(stringResource(when {
                app.system -> R.string.app_inventory_system_protected
                app.packageName == ownPackage -> R.string.apps_self_protected
                else -> R.string.app_inventory_user_app
            }), fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 4.dp))
            Text(detailsLabel, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp))
        }
        if (expanded) {
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(stringResource(R.string.apps_version, app.versionName ?: stringResource(R.string.unknown_value)),
                fontSize = 14.sp, lineHeight = 21.sp)
            Text(stringResource(R.string.apps_target_sdk,
                app.targetSdk?.toString() ?: stringResource(R.string.unknown_value)),
                fontSize = 14.sp, lineHeight = 21.sp)
            Text(stringResource(R.string.apps_storage_unknown), fontSize = 13.sp, lineHeight = 19.sp,
                modifier = Modifier.padding(vertical = 8.dp))
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

@Composable
private fun AppHandoffCard(night: Boolean, receipt: AppManagementReceipt) {
    GlassSurface(night, Modifier.testTag("apps-handoff")) {
        Text(stringResource(R.string.apps_handoff_title), fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(when (receipt.action) {
            AppManagementAction.DETAILS -> R.string.app_inventory_app_info
            AppManagementAction.UNINSTALL -> R.string.app_inventory_uninstall
            AppManagementAction.SETTINGS -> R.string.app_inventory_all_settings
        }), fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 8.dp))
        receipt.target?.let { app ->
            Text(app.label, fontWeight = FontWeight.SemiBold)
            Text(app.packageName, fontSize = 13.sp, lineHeight = 19.sp)
        }
        Text(stringResource(when (receipt.outcome) {
            AppManagementOutcome.OPENED -> R.string.apps_handoff_opened
            AppManagementOutcome.RETURNED -> R.string.apps_handoff_returned
            AppManagementOutcome.UNAVAILABLE -> R.string.apps_handoff_unavailable
            AppManagementOutcome.BLOCKED -> R.string.apps_handoff_blocked
        }), fontSize = 14.sp, lineHeight = 21.sp,
            modifier = Modifier.padding(top = 8.dp).testTag("apps-handoff-status")
                .semantics { liveRegion = LiveRegionMode.Polite })
    }
}
