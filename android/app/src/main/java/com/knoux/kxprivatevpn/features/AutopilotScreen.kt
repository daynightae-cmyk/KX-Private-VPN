package com.knoux.kxprivatevpn.features

import android.text.format.Formatter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.designsystem.Atmosphere
import com.knoux.kxprivatevpn.designsystem.GlassAction
import com.knoux.kxprivatevpn.designsystem.GlassSurface
import com.knoux.kxprivatevpn.designsystem.KxPageHeader
import com.knoux.kxprivatevpn.designsystem.RepairGlyph
import com.knoux.kxprivatevpn.platform.repair.AutopilotKind
import com.knoux.kxprivatevpn.platform.repair.AutopilotPlan
import com.knoux.kxprivatevpn.platform.repair.AutopilotTarget
import com.knoux.kxprivatevpn.platform.repair.RepairServiceId
import java.text.DateFormat
import java.util.Date

/**
 * KX Repair Autopilot.
 *
 * The screen shows a measured, deterministic plan: every cache file and the aged history slice with
 * its real side effect. The person can deselect any target, must approve the destructive run, and
 * sees a receipt that reports only what really happened. No simulated progress or invented saving
 * is ever shown.
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
    val foreground = if (night) Color(0xFFF7F3FF) else Color(0xFF171050)
    val plan = state.plan
    Box(Modifier.fillMaxSize().testTag("route-autopilot")) {
        Atmosphere(night, 0f)
        Column(
            Modifier.fillMaxSize().safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KxPageHeader(
                title = stringResource(R.string.autopilot_title),
                night = night,
                backLabel = stringResource(R.string.back),
                onBack = onBack,
            )
            Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                RepairGlyph(RepairServiceId.AUTOPILOT, Modifier.size(132.dp))
            }
            Text(
                stringResource(R.string.autopilot_body),
                fontSize = 14.sp, lineHeight = 20.sp, color = foreground.copy(alpha = .84f),
            )

            GlassSurface(night) {
                Text(stringResource(R.string.autopilot_plan_title), fontWeight = FontWeight.Bold,
                    fontSize = 18.sp, modifier = Modifier.testTag("autopilot-plan"))
                if (state.measuring || state.loading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().testTag("autopilot-progress"))
                }
                when {
                    state.failure -> Text(stringResource(R.string.autopilot_failure), fontSize = 13.sp,
                        color = Color(0xFFFFB4B4), modifier = Modifier.testTag("autopilot-failure"))
                    plan == null -> Text(stringResource(R.string.autopilot_failure), fontSize = 13.sp)
                    plan.isEmpty -> Text(stringResource(R.string.autopilot_empty), fontSize = 13.sp,
                        color = foreground.copy(alpha = .82f), modifier = Modifier.testTag("autopilot-empty"))
                    else -> {
                        Text(
                            stringResource(R.string.autopilot_plan_summary, plan.targets.size,
                                Formatter.formatFileSize(context, plan.totalBytes)),
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("autopilot-plan-summary"),
                        )
                        Text(
                            stringResource(R.string.autopilot_selected, state.selected.size,
                                Formatter.formatFileSize(context, plan.selectedBytes(state.selected))),
                            fontSize = 13.sp, color = foreground.copy(alpha = .8f),
                            modifier = Modifier.testTag("autopilot-selected"),
                        )
                        if (!plan.cacheMeasured) {
                            Text(stringResource(R.string.autopilot_cache_unavailable), fontSize = 12.sp,
                                color = Color(0xFFFFC98A), modifier = Modifier.testTag("autopilot-cache-unavailable"))
                        }
                        if (plan.cacheLimited) {
                            Text(stringResource(R.string.autopilot_cache_limited), fontSize = 11.sp,
                                color = foreground.copy(alpha = .7f))
                        }
                        Text(
                            stringResource(R.string.autopilot_measured_at,
                                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                    .format(Date(plan.measuredAtMillis))),
                            fontSize = 11.sp, color = foreground.copy(alpha = .7f),
                            modifier = Modifier.testTag("autopilot-measured-at"),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onMeasure,
                        enabled = !state.busy && !state.measuring,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("autopilot-measure"),
                    ) { Text(stringResource(R.string.autopilot_measure)) }
                    OutlinedButton(
                        onClick = onSelectAll,
                        enabled = !state.busy && plan != null && !plan.isEmpty,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("autopilot-select-all"),
                    ) { Text(stringResource(R.string.autopilot_select_all)) }
                    OutlinedButton(
                        onClick = onClearSelection,
                        enabled = !state.busy,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("autopilot-clear"),
                    ) { Text(stringResource(R.string.autopilot_clear)) }
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
                        busy = state.busy,
                        onToggle = { onToggle(target.id) },
                    )
                }
            }

            state.message?.let {
                Text(stringResource(R.string.autopilot_no_selection), fontSize = 12.sp,
                    color = Color(0xFFFFC98A), modifier = Modifier.testTag("autopilot-message"))
            }

            if (state.busy) {
                GlassSurface(night) {
                    Text(stringResource(R.string.autopilot_busy), fontSize = 14.sp,
                        modifier = Modifier.testTag("autopilot-busy"))
                    LinearProgressIndicator(Modifier.fillMaxWidth().testTag("autopilot-run-progress"))
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("autopilot-cancel"),
                    ) { Text(stringResource(R.string.autopilot_cancel)) }
                }
            } else {
                GlassAction(
                    label = stringResource(R.string.autopilot_run),
                    modifier = Modifier.testTag("autopilot-run"),
                    enabled = plan != null && !plan.isEmpty && state.selected.isNotEmpty(),
                ) { onRequestRun() }
            }

            state.receipt?.let { receipt ->
                GlassSurface(night) {
                    Text(stringResource(R.string.autopilot_receipt_title), fontWeight = FontWeight.Bold,
                        fontSize = 16.sp, modifier = Modifier.testTag("autopilot-receipt"))
                    Text(
                        stringResource(when {
                            receipt.cancelled -> R.string.autopilot_receipt_cancelled
                            receipt.complete -> R.string.autopilot_receipt_complete
                            else -> R.string.autopilot_receipt_partial
                        }),
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("autopilot-receipt-detail"),
                    )
                    Text(
                        stringResource(R.string.autopilot_receipt_counts, receipt.deleted, receipt.pruned,
                            Formatter.formatFileSize(context, receipt.bytesFreed)),
                        fontSize = 13.sp, modifier = Modifier.testTag("autopilot-receipt-counts"),
                    )
                    if (receipt.skipped.isNotEmpty()) {
                        Text(stringResource(R.string.autopilot_receipt_skipped, receipt.skipped.size),
                            fontSize = 12.sp, color = foreground.copy(alpha = .8f),
                            modifier = Modifier.testTag("autopilot-receipt-skipped"))
                    }
                    if (receipt.failed.isNotEmpty()) {
                        Text(stringResource(R.string.autopilot_receipt_failed, receipt.failed.size),
                            fontSize = 12.sp, color = Color(0xFFFFB4B4),
                            modifier = Modifier.testTag("autopilot-receipt-failed"))
                    }
                }
            }

            Text(stringResource(R.string.autopilot_scope), fontSize = 11.sp, lineHeight = 16.sp,
                color = foreground.copy(alpha = .72f), modifier = Modifier.testTag("autopilot-scope"))
            Spacer(Modifier.height(24.dp))
        }
    }

    if (state.confirming && plan != null) {
        AlertDialog(
            onDismissRequest = onDismissConfirmation,
            title = { Text(stringResource(R.string.autopilot_confirm_title)) },
            text = {
                Text(stringResource(R.string.autopilot_confirm_body, state.selected.size,
                    Formatter.formatFileSize(context, plan.selectedBytes(state.selected))))
            },
            confirmButton = {
                TextButton(onClick = onConfirmRun, modifier = Modifier.testTag("autopilot-confirm")) {
                    Text(stringResource(R.string.autopilot_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissConfirmation,
                    modifier = Modifier.testTag("autopilot-confirm-cancel")) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
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
    val foreground = if (night) Color(0xFFF7F3FF) else Color(0xFF171050)
    val isHistory = target.kind == AutopilotKind.HISTORY
    val title = if (isHistory) stringResource(R.string.autopilot_history_title) else target.title
    val detail = if (isHistory) {
        stringResource(R.string.autopilot_history_detail, plan.agedJournal, plan.agedUsage, plan.retentionDays)
    } else {
        stringResource(R.string.autopilot_cache_detail, Formatter.formatFileSize(context, target.bytes))
    }
    GlassSurface(night, modifier = Modifier.testTag("autopilot-target-$index")) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggle() },
                enabled = !busy,
                modifier = Modifier.testTag("autopilot-select-$index"),
            )
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2,
                    modifier = Modifier.testTag("autopilot-target-title-$index"))
                Text(detail, fontSize = 12.sp, color = foreground.copy(alpha = .78f),
                    modifier = Modifier.testTag("autopilot-target-detail-$index"))
                if (target.destructive) {
                    Text(stringResource(R.string.autopilot_destructive), fontSize = 11.sp,
                        color = if (night) Color(0xFFFFC98A) else Color(0xFF8A5A00))
                }
            }
        }
    }
}