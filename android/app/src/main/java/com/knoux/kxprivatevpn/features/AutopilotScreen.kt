package com.knoux.kxprivatevpn.features

import android.text.format.Formatter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.designsystem.KxGlassIconTile
import com.knoux.kxprivatevpn.designsystem.KxGlassPanel
import com.knoux.kxprivatevpn.designsystem.KxGradientAction
import com.knoux.kxprivatevpn.designsystem.KxIconKind
import com.knoux.kxprivatevpn.designsystem.KxLineIcon
import com.knoux.kxprivatevpn.designsystem.KxOrbBadge
import com.knoux.kxprivatevpn.designsystem.KxReferenceAtmosphere
import com.knoux.kxprivatevpn.designsystem.KxReferenceHeader
import com.knoux.kxprivatevpn.designsystem.KxReferenceTheme
import com.knoux.kxprivatevpn.designsystem.KxSectionHeaderRow
import com.knoux.kxprivatevpn.designsystem.KxTitleBlock
import com.knoux.kxprivatevpn.designsystem.kxCyan
import com.knoux.kxprivatevpn.designsystem.kxGreen
import com.knoux.kxprivatevpn.designsystem.kxInk
import com.knoux.kxprivatevpn.designsystem.kxMuted
import com.knoux.kxprivatevpn.designsystem.kxRose
import com.knoux.kxprivatevpn.designsystem.kxViolet
import com.knoux.kxprivatevpn.platform.repair.AutopilotKind
import com.knoux.kxprivatevpn.platform.repair.AutopilotPlan
import com.knoux.kxprivatevpn.platform.repair.AutopilotTarget
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * KX Repair Autopilot, rebuilt on the owner-approved reference language (references 09 and 15):
 * emblem hero with the measured stat trio, then the plan board with the real device checks, the
 * recommended repair card, the approved target rows and the honest progress journal.
 *
 * The screen shows a measured, deterministic plan: every cache file and the aged history slice with
 * its real side effect. The person can deselect any target, must approve the destructive run, and
 * sees a receipt that reports only what really happened. No simulated progress, no invented saving,
 * no estimated percentage is ever shown; where the platform reports nothing the screen says so.
 */
@Composable
fun AutopilotRoute(night: Boolean, onBack: () -> Unit) {
    val model: AutopilotViewModel = viewModel()
    val state by model.state.collectAsStateWithLifecycle()
    AutopilotScreen(
        night = night,
        state = state,
        onBack = onBack,
        onMeasure = model::measure,
        onToggle = model::toggle,
        onSelectAll = model::selectAll,
        onClearSelection = model::clearSelection,
        onRequestRun = model::requestRun,
        onDismissConfirmation = model::dismissConfirmation,
        onConfirmRun = model::confirmRun,
        onCancel = model::cancel,
    )
}

/** The stateless screen. Every value comes from [state]; every action is a callback. */
@Composable
fun AutopilotScreen(
    night: Boolean,
    state: AutopilotUiState,
    onBack: () -> Unit,
    onMeasure: () -> Unit,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onRequestRun: () -> Unit,
    onDismissConfirmation: () -> Unit,
    onConfirmRun: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val plan = state.plan
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    KxReferenceTheme(night) {
    Box(Modifier.fillMaxSize().testTag("route-autopilot")) {
        KxReferenceAtmosphere(night)
        Column(
            Modifier.fillMaxSize().safeDrawingPadding()
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KxReferenceHeader(
                night = night,
                actionLabel = stringResource(R.string.back),
                tag = "autopilot-ref-back",
                icon = KxIconKind.BACK,
                onClick = onBack,
            )

            // ---- Reference 09: emblem hero, tap-to-measure hint and the measured stat trio. ----
            KxTitleBlock(
                title = stringResource(R.string.autopilot_title),
                subtitle = stringResource(R.string.autopilot_body),
                night = night,
                announceTitleAsDescription = true,
            )
            val measureLabel = stringResource(R.string.autopilot_hero_hint)
            Box(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                KxOrbBadge(
                    148.dp,
                    Modifier.clickable(
                        enabled = state.canEdit,
                        role = Role.Button,
                        onClickLabel = measureLabel,
                        onClick = onMeasure,
                    ).semantics { contentDescription = measureLabel }
                        .testTag("autopilot-orb"),
                    night,
                )
            }
            Row(
                Modifier.wrapContentWidth(Alignment.CenterHorizontally)
                    .border(1.dp, kxCyan(night).copy(alpha = .45f), RoundedCornerShape(50))
                    .background(kxCyan(night).copy(alpha = if (night) .12f else .08f), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("autopilot-hero-hint"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                KxLineIcon(KxIconKind.WRENCH, kxCyan(night), Modifier.size(15.dp))
                Text(measureLabel, color = kxInk(night), fontSize = 12.sp, lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold)
            }
            KxGradientAction(
                label = stringResource(R.string.autopilot_start),
                night = night,
                enabled = state.canEdit,
                tag = "autopilot-measure",
                icon = KxIconKind.WRENCH,
                onClick = onMeasure,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KxStatCard(
                    night = night,
                    icon = KxIconKind.DATABASE,
                    accent = kxViolet(night),
                    label = stringResource(R.string.autopilot_stat_cache),
                    value = plan?.let { Formatter.formatFileSize(context, it.totalBytes) } ?: "—",
                    modifier = Modifier.weight(1f),
                )
                KxStatCard(
                    night = night,
                    icon = KxIconKind.BOLT,
                    accent = kxRose(night),
                    label = stringResource(R.string.autopilot_stat_thermal),
                    value = stringResource(when (state.thermal) {
                        KxThermal.NONE -> R.string.autopilot_thermal_none
                        KxThermal.LIGHT -> R.string.autopilot_thermal_light
                        KxThermal.MODERATE -> R.string.autopilot_thermal_moderate
                        KxThermal.SEVERE -> R.string.autopilot_thermal_severe
                        KxThermal.UNKNOWN -> R.string.autopilot_thermal_unavailable
                    }),
                    modifier = Modifier.weight(1f),
                )
                KxStatCard(
                    night = night,
                    icon = KxIconKind.SHIELD_CHECK,
                    accent = kxCyan(night),
                    label = stringResource(R.string.autopilot_stat_app),
                    value = stringResource(when {
                        state.failure -> R.string.autopilot_app_failed
                        state.measuring || state.loading -> R.string.autopilot_app_measuring
                        else -> R.string.autopilot_app_healthy
                    }),
                    modifier = Modifier.weight(1f),
                )
            }

            // ---- Reference 15: the plan board. ----
            KxGlassPanel(night) {
                KxSectionHeaderRow(
                    title = stringResource(R.string.autopilot_plan_title),
                    icon = KxIconKind.TARGET,
                    night = night,
                    // The hardening matrix measures the real plan title text, not the panel box.
                    titleModifier = Modifier.testTag("autopilot-plan"),
                )
                if (state.measuring || state.loading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().testTag("autopilot-progress"))
                }
                when {
                    state.failure -> Text(stringResource(R.string.autopilot_failure), fontSize = 13.sp,
                        lineHeight = 19.sp, color = kxRose(night),
                        modifier = Modifier.testTag("autopilot-failure"))
                    plan == null -> Text(stringResource(R.string.autopilot_failure), fontSize = 13.sp,
                        lineHeight = 19.sp, color = kxMuted(night))
                    plan.isEmpty -> Text(stringResource(R.string.autopilot_empty), fontSize = 13.sp,
                        lineHeight = 19.sp, color = kxInk(night),
                        modifier = Modifier.testTag("autopilot-empty"))
                    else -> {
                        Text(
                            stringResource(R.string.autopilot_plan_summary, plan.targets.size,
                                Formatter.formatFileSize(context, plan.totalBytes)),
                            fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold,
                            color = kxInk(night),
                            modifier = Modifier.testTag("autopilot-plan-summary"),
                        )
                        Text(
                            stringResource(R.string.autopilot_selected, state.selected.size,
                                Formatter.formatFileSize(context, plan.selectedBytes(state.selected))),
                            fontSize = 13.sp, lineHeight = 19.sp, color = kxMuted(night),
                            modifier = Modifier.testTag("autopilot-selected"),
                        )
                        if (!plan.cacheMeasured) {
                            Text(stringResource(R.string.autopilot_cache_unavailable), fontSize = 12.sp,
                                lineHeight = 18.sp, color = kxRose(night),
                                modifier = Modifier.testTag("autopilot-cache-unavailable"))
                        }
                        if (plan.cacheLimited) {
                            Text(stringResource(R.string.autopilot_cache_limited), fontSize = 11.sp,
                                lineHeight = 16.sp, color = kxMuted(night))
                        }
                        Text(
                            stringResource(R.string.autopilot_measured_at,
                                dateFormat(context, DateFormat.MEDIUM)
                                    .format(Date(plan.measuredAtMillis))),
                            fontSize = 11.sp, lineHeight = 16.sp, color = kxMuted(night),
                            modifier = Modifier.testTag("autopilot-measured-at"),
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                AutopilotDeviceStatus(night = night, state = state, plan = plan, context = context)

                if (plan != null && !plan.isEmpty) {
                    Spacer(Modifier.height(10.dp))
                    AutopilotRecommendedCard(
                        night = night,
                        plan = plan,
                        selected = state.selected,
                        enabled = state.canEdit,
                        onSetKind = { ids, on ->
                            ids.forEach { id -> if ((id in state.selected) != on) onToggle(id) }
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                    // Three weighted buttons do not fit the 360dp reference frame: the primary
                    // label broke mid-word. The primary action gets its own row and the two
                    // selection buttons share an equal row below it.
                    KxGradientAction(
                        label = stringResource(R.string.autopilot_measure),
                        night = night,
                        enabled = state.canEdit,
                        tag = "autopilot-measure-again",
                        icon = KxIconKind.SEARCH,
                        onClick = onMeasure,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onSelectAll,
                            enabled = state.canEdit,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                .testTag("autopilot-select-all"),
                        ) { Text(stringResource(R.string.autopilot_select_all), maxLines = 1) }
                        OutlinedButton(
                            onClick = onClearSelection,
                            enabled = state.canEdit,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                .testTag("autopilot-clear"),
                        ) { Text(stringResource(R.string.autopilot_clear), maxLines = 1) }
                    }
                }
            }

            if (plan != null && !plan.isEmpty) {
                plan.targets.forEachIndexed { index, target ->
                    AutopilotTargetCard(
                        night = night,
                        index = index,
                        target = target,
                        plan = plan,
                        selected = target.id in state.selected,
                        busy = !state.canEdit,
                        onToggle = { onToggle(target.id) },
                    )
                }
                AutopilotJournalCard(
                    night = night,
                    state = state,
                    plan = plan,
                    context = context,
                    onReview = { scope.launch { scroll.animateScrollTo(scroll.maxValue) } },
                )
            }

            state.message?.let {
                Text(stringResource(R.string.autopilot_no_selection), fontSize = 12.sp,
                    lineHeight = 18.sp, color = kxRose(night),
                    modifier = Modifier.testTag("autopilot-message"))
            }

            if (state.busy) {
                KxGlassPanel(night, modifier = Modifier.testTag("autopilot-busy")) {
                    Text(stringResource(R.string.autopilot_busy), fontSize = 14.sp, lineHeight = 20.sp,
                        color = kxInk(night), fontWeight = FontWeight.SemiBold)
                    LinearProgressIndicator(Modifier.fillMaxWidth().testTag("autopilot-run-progress"))
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("autopilot-cancel"),
                    ) { Text(stringResource(R.string.autopilot_cancel)) }
                }
            } else {
                KxGradientAction(
                    label = stringResource(R.string.autopilot_run),
                    night = night,
                    enabled = state.canEdit && plan != null && !plan.isEmpty && state.selected.isNotEmpty(),
                    tag = "autopilot-run",
                    icon = KxIconKind.BOLT,
                ) { onRequestRun() }
            }

            Text(stringResource(R.string.autopilot_scope), fontSize = 11.sp, lineHeight = 16.sp,
                color = kxMuted(night), modifier = Modifier.testTag("autopilot-scope"))

            state.receipt?.let { receipt ->
                KxGlassPanel(night, modifier = Modifier.testTag("autopilot-receipt")) {
                    KxSectionHeaderRow(
                        title = stringResource(R.string.autopilot_receipt_title),
                        icon = KxIconKind.FILE,
                        night = night,
                    )
                    Text(
                        stringResource(when {
                            receipt.cancelled -> R.string.autopilot_receipt_cancelled
                            receipt.complete -> R.string.autopilot_receipt_complete
                            else -> R.string.autopilot_receipt_partial
                        }),
                        fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold,
                        color = kxInk(night),
                        modifier = Modifier.testTag("autopilot-receipt-detail"),
                    )
                    Text(
                        stringResource(R.string.autopilot_receipt_counts, receipt.deleted, receipt.pruned,
                            Formatter.formatFileSize(context, receipt.bytesFreed)),
                        fontSize = 13.sp, lineHeight = 19.sp, color = kxMuted(night),
                        modifier = Modifier.testTag("autopilot-receipt-counts"),
                    )
                    if (receipt.skipped.isNotEmpty()) {
                        Text(stringResource(R.string.autopilot_receipt_skipped, receipt.skipped.size),
                            fontSize = 12.sp, lineHeight = 18.sp, color = kxMuted(night),
                            modifier = Modifier.testTag("autopilot-receipt-skipped"))
                    }
                    if (receipt.failed.isNotEmpty()) {
                        Text(stringResource(R.string.autopilot_receipt_failed, receipt.failed.size),
                            fontSize = 12.sp, lineHeight = 18.sp, color = kxRose(night),
                            modifier = Modifier.testTag("autopilot-receipt-failed"))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (state.confirming && plan != null) {
        // Dialog windows resolve resources against the Activity context. Resolve the copy here,
        // in the localized screen composition, so Arabic never falls back to English.
        val dialogTitle = stringResource(R.string.autopilot_confirm_title)
        val dialogBody = stringResource(R.string.autopilot_confirm_body, state.selected.size,
            Formatter.formatFileSize(context, plan.selectedBytes(state.selected)))
        val dialogConfirm = stringResource(R.string.autopilot_confirm)
        val dialogCancel = stringResource(R.string.cancel)
        AlertDialog(
            onDismissRequest = onDismissConfirmation,
            title = { Text(dialogTitle) },
            text = { Text(dialogBody) },
            confirmButton = {
                TextButton(onClick = onConfirmRun, modifier = Modifier.testTag("autopilot-confirm")) {
                    Text(dialogConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissConfirmation,
                    modifier = Modifier.testTag("autopilot-confirm-cancel")) {
                    Text(dialogCancel)
                }
            },
        )
    }
    }
}

/** One measured hero stat card from reference 09. The value is always a real reading or a gap. */
@Composable
private fun KxStatCard(
    night: Boolean,
    icon: KxIconKind,
    accent: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    KxGlassPanel(
        night = night,
        modifier = modifier,
        radius = 18.dp,
        padding = PaddingValues(vertical = 12.dp, horizontal = 8.dp),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            KxLineIcon(icon, accent, Modifier.size(20.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = kxInk(night), fontSize = 11.sp, lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth())
        Text(value, color = accent, fontSize = 13.sp, lineHeight = 18.sp,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth())
    }
}

/**
 * Reference 15's device status card. Every check is a real reading: the cache measurement result,
 * the platform thermal report and the approval gate that the implementation actually enforces.
 */
@Composable
private fun AutopilotDeviceStatus(
    night: Boolean,
    state: AutopilotUiState,
    plan: AutopilotPlan?,
    context: android.content.Context,
) {
    val ring = Brush.sweepGradient(
        listOf(kxViolet(night), kxCyan(night), kxViolet(night)),
    )
    val status = stringResource(when {
        state.failure -> R.string.autopilot_app_failed
        state.measuring || state.loading -> R.string.autopilot_app_measuring
        else -> R.string.autopilot_ready
    })
    KxGlassPanel(night, radius = 20.dp, padding = PaddingValues(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(84.dp).border(BorderStroke(5.dp, ring), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(status, color = kxInk(night), fontSize = 11.sp, lineHeight = 15.sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("autopilot-device-ring"))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AutopilotCheck(
                    night = night,
                    ok = plan?.cacheMeasured == true,
                    label = stringResource(R.string.autopilot_check_cache),
                )
                AutopilotCheck(
                    night = night,
                    ok = state.thermal == KxThermal.NONE || state.thermal == KxThermal.LIGHT,
                    unavailable = state.thermal == KxThermal.UNKNOWN,
                    label = stringResource(
                        if (state.thermal == KxThermal.UNKNOWN) R.string.autopilot_check_thermal_unavailable
                        else R.string.autopilot_check_thermal,
                    ),
                )
                AutopilotCheck(
                    night = night,
                    ok = true,
                    label = stringResource(R.string.autopilot_check_approval),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                KxLineIcon(KxIconKind.DATABASE, kxInk(night), Modifier.size(18.dp))
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.autopilot_releasable), color = kxMuted(night),
                    fontSize = 10.sp, lineHeight = 14.sp, textAlign = TextAlign.End)
                Text(
                    plan?.let { Formatter.formatFileSize(context, it.totalBytes) } ?: "—",
                    color = kxInk(night), fontSize = 17.sp, lineHeight = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.testTag("autopilot-releasable"),
                )
            }
        }
    }
}

@Composable
private fun AutopilotCheck(night: Boolean, ok: Boolean, label: String, unavailable: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(
            Modifier.size(16.dp).border(1.dp, if (ok) kxGreen(night) else kxMuted(night), CircleShape)
                .background(if (ok) kxGreen(night).copy(alpha = .22f) else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (ok) KxLineIcon(KxIconKind.CHECK, kxGreen(night), Modifier.size(10.dp))
        }
        Text(label, color = if (ok) kxInk(night) else kxMuted(night), fontSize = 11.5.sp,
            lineHeight = 16.sp, fontWeight = if (ok) FontWeight.SemiBold else FontWeight.Normal,
            modifier = if (unavailable) Modifier.testTag("autopilot-thermal-gap") else Modifier)
    }
}

/**
 * The recommended repair card. Its two switches are real selection controls: they include or
 * exclude the measured cache files and the aged history slice from the approved set.
 */
@Composable
private fun AutopilotRecommendedCard(
    night: Boolean,
    plan: AutopilotPlan,
    selected: Set<String>,
    enabled: Boolean,
    onSetKind: (List<String>, Boolean) -> Unit,
) {
    val cacheIds = plan.cacheTargets.map { it.id }
    val historyIds = listOfNotNull(plan.historyTarget?.id)
    val cacheOn = cacheIds.any { it in selected }
    val historyOn = historyIds.any { it in selected }
    KxGlassPanel(
        night = night,
        radius = 20.dp,
        padding = PaddingValues(14.dp),
        modifier = Modifier.border(1.dp, kxCyan(night).copy(alpha = .55f), RoundedCornerShape(20.dp)),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    KxLineIcon(KxIconKind.SPARKLE, kxCyan(night), Modifier.size(16.dp))
                    Text(stringResource(R.string.autopilot_recommended), color = kxInk(night),
                        fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.autopilot_recommended_body), color = kxMuted(night),
                    fontSize = 11.5.sp, lineHeight = 17.sp)
            }
            Box(
                Modifier.border(1.dp, kxCyan(night).copy(alpha = .6f), RoundedCornerShape(50))
                    .background(kxCyan(night).copy(alpha = .16f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(stringResource(R.string.autopilot_recommended_badge), color = kxCyan(night),
                    fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AutopilotKindSwitch(
                night = night,
                label = stringResource(R.string.autopilot_include_cache),
                icon = KxIconKind.BROOM,
                checked = cacheOn,
                enabled = enabled && cacheIds.isNotEmpty(),
                tag = "autopilot-include-cache",
                modifier = Modifier.weight(1f),
                onChange = { onSetKind(cacheIds, it) },
            )
            AutopilotKindSwitch(
                night = night,
                label = stringResource(R.string.autopilot_include_history),
                icon = KxIconKind.CLOCK,
                checked = historyOn,
                enabled = enabled && historyIds.isNotEmpty(),
                tag = "autopilot-include-history",
                modifier = Modifier.weight(1f),
                onChange = { onSetKind(historyIds, it) },
            )
        }
    }
}

@Composable
private fun AutopilotKindSwitch(
    night: Boolean,
    label: String,
    icon: KxIconKind,
    checked: Boolean,
    enabled: Boolean,
    tag: String,
    modifier: Modifier = Modifier,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier.fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = if (night) .18f else .55f), RoundedCornerShape(50))
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = kxCyan(night),
                checkedThumbColor = Color.White,
                uncheckedTrackColor = kxMuted(night).copy(alpha = .35f),
                uncheckedThumbColor = Color.White.copy(alpha = .8f),
            ),
            modifier = Modifier.size(width = 34.dp, height = 20.dp),
        )
        KxLineIcon(icon, if (checked) kxCyan(night) else kxMuted(night), Modifier.size(14.dp))
        Text(label, color = if (checked) kxInk(night) else kxMuted(night), fontSize = 11.sp,
            lineHeight = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
    }
}

@Composable
private fun AutopilotTargetCard(
    night: Boolean,
    index: Int,
    target: AutopilotTarget,
    plan: AutopilotPlan,
    selected: Boolean,
    busy: Boolean,
    onToggle: () -> Unit,
) {
    val context = LocalContext.current
    val isHistory = target.kind == AutopilotKind.HISTORY
    val title = if (isHistory) stringResource(R.string.autopilot_history_title) else target.title
    val detail = if (isHistory) {
        stringResource(R.string.autopilot_history_detail, plan.agedJournal, plan.agedUsage, plan.retentionDays)
    } else {
        stringResource(R.string.autopilot_cache_detail, Formatter.formatFileSize(context, target.bytes))
    }
    val chip = if (isHistory) {
        (plan.agedJournal + plan.agedUsage).toString()
    } else {
        Formatter.formatFileSize(context, target.bytes)
    }
    KxGlassPanel(
        night = night,
        radius = 18.dp,
        padding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        modifier = Modifier.testTag("autopilot-target-$index"),
    ) {
        // One labelled checkbox target, not an unlabelled tiny checkbox beside inert text.
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .toggleable(value = selected, enabled = !busy, role = Role.Checkbox) { onToggle() }
                .testTag("autopilot-select-$index"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = null,
                enabled = !busy,
                colors = CheckboxDefaults.colors(
                    checkedColor = kxViolet(night),
                    checkmarkColor = Color.White,
                    uncheckedColor = kxMuted(night),
                ),
                modifier = Modifier.size(26.dp),
            )
            KxGlassIconTile(
                night = night,
                icon = if (isHistory) KxIconKind.CLOCK else KxIconKind.BROOM,
                accent = if (isHistory) kxCyan(night) else kxViolet(night),
                size = 40.dp,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = kxInk(night), fontWeight = FontWeight.Bold, fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    style = TextStyle(textDirection = if (isHistory) TextDirection.Content else TextDirection.ContentOrLtr),
                    modifier = Modifier.testTag("autopilot-target-title-$index"))
                Text(detail, fontSize = 11.5.sp, lineHeight = 17.sp, color = kxMuted(night),
                    modifier = Modifier.testTag("autopilot-target-detail-$index"))
                if (target.destructive) {
                    Text(stringResource(R.string.autopilot_destructive), fontSize = 10.5.sp,
                        lineHeight = 15.sp, color = kxRose(night),
                        modifier = Modifier.testTag("autopilot-approval-$index"))
                }
            }
            Box(
                Modifier.border(1.dp, Color.White.copy(alpha = if (night) .22f else .6f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(chip, color = kxInk(night), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Dates follow the screen's locale, not the process default, so Arabic stays Arabic. */
private fun localeOf(context: android.content.Context): Locale =
    context.resources.configuration.locales[0]

private fun dateFormat(context: android.content.Context, style: Int): DateFormat =
    DateFormat.getDateTimeInstance(style, DateFormat.SHORT, localeOf(context))

/**
 * Reference 15's progress journal. The four steps advance only on real state transitions and the
 * bar fills by completed steps; no percentage is ever invented.
 */
@Composable
private fun AutopilotJournalCard(
    night: Boolean,
    state: AutopilotUiState,
    plan: AutopilotPlan,
    context: android.content.Context,
    onReview: () -> Unit,
) {
    val measured = plan.measuredAtMillis > 0 && !state.measuring
    val analyzed = plan.targets.isNotEmpty()
    val repairing = state.busy
    // A finished run has already repaired, even though the spinner is gone.
    val repaired = state.busy || state.receipt != null
    val done = state.receipt?.complete == true
    val steps = listOf(
        Triple(R.string.autopilot_step_scan, measured, false),
        Triple(R.string.autopilot_step_analyze, analyzed, false),
        Triple(R.string.autopilot_step_repair, repaired, repairing),
        Triple(R.string.autopilot_step_done, done, false),
    )
    val completed = steps.count { it.second }
    KxGlassPanel(night, radius = 20.dp, padding = PaddingValues(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1.35f)) {
                KxSectionHeaderRow(
                    title = stringResource(R.string.autopilot_journal_title),
                    icon = KxIconKind.FILE,
                    night = night,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    steps.forEachIndexed { index, step ->
                        Column(
                            Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(
                                Modifier.size(26.dp)
                                    .then(
                                        if (step.second) Modifier.background(
                                            Brush.horizontalGradient(listOf(kxViolet(night), kxCyan(night))),
                                            CircleShape,
                                        ) else Modifier.border(
                                            1.dp, kxMuted(night).copy(alpha = .6f), CircleShape,
                                        ),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (step.third) {
                                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp,
                                        color = Color.White)
                                } else if (step.second) {
                                    KxLineIcon(KxIconKind.CHECK, Color.White, Modifier.size(13.dp))
                                }
                            }
                            Text(stringResource(step.first), color = if (step.second) kxInk(night) else kxMuted(night),
                                fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag("autopilot-step-$index"))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth().height(6.dp)
                        .background(kxMuted(night).copy(alpha = .25f), RoundedCornerShape(50)),
                ) {
                    Box(
                        Modifier.fillMaxWidth(completed.toFloat() / steps.size.toFloat())
                            .height(6.dp)
                            .background(
                                Brush.horizontalGradient(listOf(kxViolet(night), kxRose(night))),
                                RoundedCornerShape(50),
                            )
                            .testTag("autopilot-journal-bar"),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (state.busy) stringResource(R.string.autopilot_busy)
                    else stringResource(R.string.autopilot_measured_at,
                        dateFormat(context, DateFormat.MEDIUM).format(Date(plan.measuredAtMillis))),
                    color = kxMuted(night), fontSize = 10.5.sp, lineHeight = 15.sp,
                    modifier = Modifier.testTag("autopilot-journal-status"),
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier.width(1.dp).heightIn(min = 70.dp)
                    .background(Color.White.copy(alpha = if (night) .16f else .5f)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    KxLineIcon(KxIconKind.CLOCK, kxMuted(night), Modifier.size(14.dp))
                    Text(stringResource(R.string.autopilot_last_scan), color = kxInk(night),
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    DateFormat.getTimeInstance(DateFormat.SHORT, localeOf(context))
                        .format(Date(plan.measuredAtMillis)),
                    color = kxMuted(night), fontSize = 11.sp,
                    modifier = Modifier.testTag("autopilot-last-scan"),
                )
                if (state.receipt != null) {
                    OutlinedButton(
                        onClick = onReview,
                        modifier = Modifier.heightIn(min = 40.dp).testTag("autopilot-review"),
                    ) { Text(stringResource(R.string.autopilot_review), fontSize = 12.sp) }
                }
            }
        }
    }
}
