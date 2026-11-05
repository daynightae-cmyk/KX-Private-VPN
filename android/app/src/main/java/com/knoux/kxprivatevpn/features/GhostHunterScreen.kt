package com.knoux.kxprivatevpn.features

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.knoux.kxprivatevpn.platform.repair.GhostCandidate
import com.knoux.kxprivatevpn.platform.repair.GhostConfidence
import com.knoux.kxprivatevpn.platform.repair.GhostKind
import com.knoux.kxprivatevpn.platform.repair.GhostReason
import com.knoux.kxprivatevpn.platform.repair.GhostScan
import com.knoux.kxprivatevpn.platform.repair.RepairServiceId
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

@Composable
fun GhostHunterRoute(night: Boolean, onBack: () -> Unit) {
    val model: GhostHunterViewModel = viewModel()
    val state by model.state.collectAsStateWithLifecycle()
    val tree = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(model::attachTree)
    }
    BackHandler(enabled = state.cleaning) { }
    GhostHunterScreen(
        night = night,
        state = state,
        onBack = onBack,
        onChooseFolder = { tree.launch(null) },
        onScan = model::scan,
        onCancelScan = model::cancelScan,
        onToggle = model::toggle,
        onSelectAll = model::selectAll,
        onClearSelection = model::clearSelection,
        onRequestCleanup = model::requestCleanup,
        onDismissCleanup = model::dismissCleanup,
        onConfirmCleanup = model::confirmCleanup,
        onCancelCleanup = model::cancelCleanup,
    )
}

/**
 * Ghost Hunter only offers evidence-backed candidates from the user-authorized tree: a reported
 * 0-byte size or a directory that returned no children. Cleanup is always selected, confirmed and
 * re-verified; a provider failure is reported, never hidden.
 */
@Composable
fun GhostHunterScreen(
    night: Boolean,
    state: GhostUiState,
    onBack: () -> Unit,
    onChooseFolder: () -> Unit,
    onScan: () -> Unit,
    onCancelScan: () -> Unit,
    onToggle: (GhostCandidate) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onRequestCleanup: () -> Unit,
    onDismissCleanup: () -> Unit,
    onConfirmCleanup: () -> Unit,
    onCancelCleanup: () -> Unit,
) {
    val context = LocalContext.current
    val foreground = if (night) Color(0xFFF7F3FF) else Color(0xFF171050)
    val scan = state.scan
    val selectedCount = state.selected.size
    Box(Modifier.fillMaxSize()) {
        Atmosphere(night, 0f)
        Column(
            Modifier.fillMaxSize().safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .testTag("route-ghost"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KxPageHeader(
                title = stringResource(R.string.repair_service_ghost),
                night = night,
                backLabel = stringResource(R.string.back),
                onBack = onBack,
            )
            Text(stringResource(R.string.ghost_subtitle), fontSize = 14.sp, lineHeight = 20.sp,
                color = foreground.copy(alpha = .82f), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
            GhostHero(night, state.scanning, foreground)
            TreeCard(night, state, foreground, onChooseFolder, onScan, onCancelScan)
            if (scan != null) {
                ScanSummary(night, scan, state, foreground)
                if (scan.candidates.isEmpty()) {
                    GlassSurface(night) {
                        Text(stringResource(if (scan.cancelled) R.string.ghost_cancelled
                        else R.string.ghost_empty), fontSize = 14.sp,
                            color = foreground.copy(alpha = .84f),
                            modifier = Modifier.testTag("ghost-empty"))
                    }
                } else {
                    CandidateCard(night, scan, state, foreground, onToggle, onSelectAll,
                        onClearSelection)
                    CleanupActions(night, state, selectedCount, foreground, onRequestCleanup,
                        onChooseFolder, onCancelCleanup)
                }
            }
            ReceiptCard(night, state, foreground)
            Text(stringResource(R.string.ghost_privacy), fontSize = 11.sp, lineHeight = 16.sp,
                color = foreground.copy(alpha = .72f), modifier = Modifier.testTag("ghost-privacy"))
            Spacer(Modifier.height(24.dp))
        }
    }
    if (state.confirming) AlertDialog(
        onDismissRequest = onDismissCleanup,
        title = { Text(stringResource(R.string.ghost_dialog_title)) },
        text = {
            Text(stringResource(R.string.ghost_dialog_body, selectedCount),
                modifier = Modifier.verticalScroll(rememberScrollState()))
        },
        confirmButton = {
            TextButton(onClick = onConfirmCleanup,
                modifier = Modifier.heightIn(min = 48.dp).testTag("ghost-confirm")) {
                Text(stringResource(R.string.ghost_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissCleanup,
                modifier = Modifier.heightIn(min = 48.dp).testTag("ghost-keep")) {
                Text(stringResource(R.string.ghost_cleanup_cancel))
            }
        },
    )
}

@Composable
private fun GhostHero(night: Boolean, scanning: Boolean, foreground: Color) {
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(scanning) {
        if (scanning) {
            sweep.snapTo(0f)
            sweep.animateTo(360f, tween(1800, easing = LinearEasing))
        } else sweep.snapTo(0f)
    }
    Box(Modifier.fillMaxWidth().height(238.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(216.dp)) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val radius = min(size.width, size.height) / 2f - 4.dp.toPx()
            drawCircle(Brush.radialGradient(listOf(Color(0xFFAD71FF).copy(alpha = .30f),
                Color(0xFF64EEFF).copy(alpha = .10f), Color.Transparent), c, radius), radius, c)
            repeat(3) { i ->
                drawCircle(Color(0xFFAD71FF).copy(alpha = .20f - i * .04f),
                    radius * (.55f + i * .18f), c, style = Stroke(1.dp.toPx()))
            }
            drawArc(Brush.sweepGradient(listOf(Color.Transparent, Color(0xFF64EEFF),
                Color(0xFFAD71FF), Color.Transparent), c),
                startAngle = sweep.value, sweepAngle = 96f, useCenter = false,
                topLeft = Offset(c.x - radius, c.y - radius), size = Size(radius * 2, radius * 2),
                style = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round))
            repeat(6) { i ->
                val angle = Math.toRadians(-90.0 + i * 60.0)
                val point = Offset(c.x + kotlin.math.cos(angle).toFloat() * radius * .82f,
                    c.y + kotlin.math.sin(angle).toFloat() * radius * .82f)
                drawCircle(if (i % 2 == 0) Color(0xFFAD71FF) else Color(0xFF64EEFF),
                    3.dp.toPx(), point, alpha = if (scanning) .9f else .45f)
            }
        }
        RepairGlyph(RepairServiceId.GHOST, Modifier.size(132.dp))
    }
}

@Composable
private fun TreeCard(
    night: Boolean,
    state: GhostUiState,
    foreground: Color,
    onChooseFolder: () -> Unit,
    onScan: () -> Unit,
    onCancelScan: () -> Unit,
) {
    GlassSurface(night) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.ghost_folder_title), fontWeight = FontWeight.Bold,
                fontSize = 18.sp, color = foreground, modifier = Modifier.weight(1f))
            if (state.scanning) LinearProgressIndicator(Modifier.width(66.dp))
        }
        if (state.treeUri == null) {
            Text(stringResource(R.string.ghost_tree_none), fontSize = 13.sp, lineHeight = 18.sp,
                color = foreground.copy(alpha = .82f))
            OutlinedButton(onClick = onChooseFolder,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .testTag("ghost-choose-folder")) {
                Text(stringResource(R.string.ghost_choose_folder))
            }
        } else {
            Text(state.treeName ?: stringResource(R.string.ghost_folder_title),
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = foreground,
                modifier = Modifier.testTag("ghost-tree-name"))
            Text(
                stringResource(if (state.writable) R.string.ghost_writable else R.string.ghost_read_only),
                fontSize = 11.sp, lineHeight = 16.sp,
                color = if (state.writable) Color(0xFF64EEFF) else Color(0xFFFFC98A),
                modifier = Modifier.testTag(if (state.writable) "ghost-writable" else "ghost-read-only"),
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (state.scanning) {
                    OutlinedButton(onClick = onCancelScan,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("ghost-scan-cancel")) {
                        Text(stringResource(R.string.ghost_cancel))
                    }
                } else {
                    OutlinedButton(onClick = onScan,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("ghost-scan")) {
                        Text(stringResource(if (state.scan == null) R.string.ghost_scan
                        else R.string.ghost_rescan))
                    }
                }
                TextButton(onClick = onChooseFolder,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("ghost-choose-again")) {
                    Text(stringResource(R.string.ghost_choose_again))
                }
            }
            if (state.scanning) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(stringResource(R.string.ghost_scanning, state.progress), fontSize = 12.sp,
                    color = foreground.copy(alpha = .8f),
                    modifier = Modifier.testTag("ghost-scan-progress"))
            }
            if (state.scanFailed) {
                Text(stringResource(R.string.ghost_scan_failed), fontSize = 13.sp,
                    color = Color(0xFFFFB4B4), modifier = Modifier.testTag("ghost-scan-failed"))
            }
            if (state.scanCancelled) {
                Text(stringResource(R.string.ghost_cancelled), fontSize = 12.sp,
                    color = foreground.copy(alpha = .8f),
                    modifier = Modifier.testTag("ghost-cancelled"))
            }
        }
        Text(stringResource(R.string.ghost_scope), fontSize = 11.sp, lineHeight = 16.sp,
            color = foreground.copy(alpha = .72f))
    }
}

@Composable
private fun ScanSummary(night: Boolean, scan: GhostScan, state: GhostUiState, foreground: Color) {
    val context = LocalContext.current
    GlassSurface(night) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.ghost_results_title), fontWeight = FontWeight.Bold,
                fontSize = 18.sp, color = foreground, modifier = Modifier.weight(1f))
            if (scan.truncated) {
                Text(stringResource(R.string.ghost_limited_badge), fontSize = 9.sp,
                    color = Color(0xFFFFC98A), modifier = Modifier.testTag("ghost-partial"))
            }
        }
        Text(stringResource(R.string.ghost_scan_counts, scan.scannedFiles, scan.scannedDirectories),
            fontSize = 12.sp, color = foreground.copy(alpha = .78f),
            modifier = Modifier.testTag("ghost-scan-counts"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryTile(stringResource(R.string.ghost_zero_title), scan.zeroByteCount,
                Modifier.weight(1f), foreground, night, Color(0xFF64EEFF), "ghost-zero-count")
            SummaryTile(stringResource(R.string.ghost_dirs_title), scan.emptyDirectoryCount,
                Modifier.weight(1f), foreground, night, Color(0xFFAD71FF), "ghost-dir-count")
        }
        if (scan.excludedSentinels > 0) {
            Text(stringResource(R.string.ghost_excluded, scan.excludedSentinels), fontSize = 11.sp,
                color = foreground.copy(alpha = .7f), modifier = Modifier.testTag("ghost-excluded"))
        }
        if (state.scanning) {
            Text(stringResource(R.string.ghost_scanning, state.progress), fontSize = 11.sp,
                color = foreground.copy(alpha = .7f))
        }
        Text(formatDate(context, scan.atMillis), fontSize = 10.sp,
            color = foreground.copy(alpha = .6f))
    }
}

@Composable
private fun SummaryTile(
    label: String,
    value: Int,
    modifier: Modifier,
    foreground: Color,
    night: Boolean,
    accent: Color,
    tag: String,
) {
    val tone = if (night) accent else lerp(accent, Color(0xFF171050), .55f)
    Column(modifier.background(tone.copy(alpha = .12f), RoundedCornerShape(16.dp))
        .padding(10.dp)) {
        Text(value.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = tone,
            modifier = Modifier.testTag(tag))
        Text(label, fontSize = 11.sp, color = foreground.copy(alpha = .8f))
    }
}

@Composable
private fun CandidateCard(
    night: Boolean,
    scan: GhostScan,
    state: GhostUiState,
    foreground: Color,
    onToggle: (GhostCandidate) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
) {
    val context = LocalContext.current
    GlassSurface(night) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.ghost_review_title), fontWeight = FontWeight.Bold,
                fontSize = 18.sp, color = foreground, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.ghost_selected, state.selected.size), fontSize = 11.sp,
                color = foreground.copy(alpha = .78f), modifier = Modifier.testTag("ghost-selected"))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onSelectAll, enabled = !state.scanning && !state.cleaning,
                modifier = Modifier.heightIn(min = 48.dp).testTag("ghost-select-all")) {
                Text(stringResource(R.string.ghost_select_all))
            }
            TextButton(onClick = onClearSelection, enabled = !state.scanning && !state.cleaning,
                modifier = Modifier.heightIn(min = 48.dp).testTag("ghost-clear")) {
                Text(stringResource(R.string.ghost_clear_selection))
            }
        }
        val candidates = scan.candidates.take(GhostHunterViewModel.MAX_DISPLAYED)
        val zero = candidates.filter { it.kind == GhostKind.ZERO_BYTE_FILE }
        val directories = candidates.filter { it.kind == GhostKind.EMPTY_DIRECTORY }
        if (zero.isNotEmpty()) {
            Text(stringResource(R.string.ghost_zero_title), fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold, color = foreground.copy(alpha = .85f),
                modifier = Modifier.padding(top = 4.dp))
            zero.forEach { CandidateRow(it, state, night, foreground, context, onToggle) }
        }
        if (directories.isNotEmpty()) {
            Text(stringResource(R.string.ghost_dirs_title), fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold, color = foreground.copy(alpha = .85f),
                modifier = Modifier.padding(top = 4.dp))
            directories.forEach { CandidateRow(it, state, night, foreground, context, onToggle) }
        }
        if (scan.candidates.size > GhostHunterViewModel.MAX_DISPLAYED) {
            Text(stringResource(R.string.ghost_display_limit, GhostHunterViewModel.MAX_DISPLAYED),
                fontSize = 11.sp, color = foreground.copy(alpha = .7f),
                modifier = Modifier.testTag("ghost-display-limit"))
        }
    }
}

@Composable
private fun CandidateRow(
    candidate: GhostCandidate,
    state: GhostUiState,
    night: Boolean,
    foreground: Color,
    context: android.content.Context,
    onToggle: (GhostCandidate) -> Unit,
) {
    val checked = candidate.documentId in state.selected
    val confidence = when (candidate.confidence) {
        GhostConfidence.HIGH -> stringResource(R.string.ghost_confidence_high)
        GhostConfidence.MEDIUM -> stringResource(R.string.ghost_confidence_medium)
    }
    val reason = when (candidate.reason) {
        GhostReason.REPORTED_ZERO_BYTES -> stringResource(R.string.ghost_reason_zero)
        GhostReason.NO_CHILDREN -> stringResource(R.string.ghost_reason_empty)
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .toggleable(value = checked, enabled = !state.scanning && !state.cleaning,
                role = Role.Checkbox, onValueChange = { onToggle(candidate) })
            .testTag("ghost-candidate-${candidate.documentId}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, enabled = !state.scanning && !state.cleaning,
            onCheckedChange = null)
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(candidate.relativePath, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = foreground, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(reason, fontSize = 10.sp, color = foreground.copy(alpha = .7f))
            val confidenceAccent = if (candidate.confidence == GhostConfidence.HIGH)
                Color(0xFF64EEFF) else Color(0xFFFFC98A)
            Text(confidence, fontSize = 10.sp,
                color = if (night) confidenceAccent
                else lerp(confidenceAccent, Color(0xFF171050), .55f))
        }
        if (candidate.kind == GhostKind.ZERO_BYTE_FILE) {
            Text(Formatter.formatFileSize(context, 0), fontSize = 11.sp,
                color = foreground.copy(alpha = .6f))
        }
    }
}

@Composable
private fun CleanupActions(
    night: Boolean,
    state: GhostUiState,
    selectedCount: Int,
    foreground: Color,
    onRequestCleanup: () -> Unit,
    onChooseFolder: () -> Unit,
    onCancelCleanup: () -> Unit,
) {
    GlassSurface(night) {
        Text(stringResource(R.string.ghost_cleanup_title), fontWeight = FontWeight.Bold,
            fontSize = 18.sp, color = foreground)
        Text(stringResource(R.string.ghost_cleanup_body), fontSize = 12.sp, lineHeight = 17.sp,
            color = foreground.copy(alpha = .8f))
        if (!state.writable) {
            Text(stringResource(R.string.ghost_not_deletable), fontSize = 12.sp,
                color = Color(0xFFFFC98A), modifier = Modifier.testTag("ghost-cleanup-read-only"))
            OutlinedButton(onClick = onChooseFolder,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .testTag("ghost-regrant")) {
                Text(stringResource(R.string.ghost_choose_again))
            }
        } else if (state.cleaning) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(stringResource(R.string.ghost_cleaning), fontSize = 13.sp,
                modifier = Modifier.testTag("ghost-cleaning"))
            OutlinedButton(onClick = onCancelCleanup,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .testTag("ghost-cleanup-cancel")) {
                Text(stringResource(R.string.ghost_cancel))
            }
        } else {
            GlassAction(
                label = stringResource(if (selectedCount == 0) R.string.ghost_no_selection
                else R.string.ghost_review_cleanup),
                modifier = Modifier.testTag("ghost-cleanup"),
                enabled = selectedCount > 0,
            ) { onRequestCleanup() }
        }
    }
}

@Composable
private fun ReceiptCard(night: Boolean, state: GhostUiState, foreground: Color) {
    val receipt = state.receipt ?: return
    GlassSurface(night) {
        Text(stringResource(R.string.ghost_receipt_title), fontWeight = FontWeight.Bold,
            fontSize = 18.sp, color = foreground)
        Text(
            stringResource(R.string.ghost_receipt, receipt.deleted, receipt.skipped.size,
                receipt.failed.size),
            fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = foreground,
            modifier = Modifier.testTag("ghost-receipt"),
        )
        Text(stringResource(R.string.ghost_receipt_verified, receipt.verifiedAbsent),
            fontSize = 12.sp, color = Color(0xFF64EEFF))
        if (!receipt.complete) {
            Text(stringResource(R.string.ghost_cleanup_partial), fontSize = 12.sp,
                color = Color(0xFFFFC98A), modifier = Modifier.testTag("ghost-receipt-partial"))
        }
        receipt.failed.take(5).forEach {
            Text(it, fontSize = 11.sp, color = Color(0xFFFFB4B4))
        }
        receipt.skipped.take(5).forEach {
            Text(stringResource(R.string.ghost_skipped, it), fontSize = 11.sp,
                color = foreground.copy(alpha = .72f))
        }
        Text(stringResource(R.string.ghost_journal_note), fontSize = 11.sp,
            color = foreground.copy(alpha = .7f))
        if (state.journalFailed) {
            Text(stringResource(R.string.ghost_journal_failed), fontSize = 11.sp,
                color = Color(0xFFFFC98A), modifier = Modifier.testTag("ghost-journal-failed"))
        }
    }
}

private fun formatDate(context: android.content.Context, atMillis: Long): String {
    val locale = context.resources.configuration.locales.get(0) ?: Locale.getDefault()
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale)
        .format(Date(atMillis))
}
