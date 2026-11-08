package com.knoux.kxprivatevpn.features

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
import com.knoux.kxprivatevpn.platform.repair.DuplicateCleanupReceipt
import com.knoux.kxprivatevpn.platform.repair.DuplicateEntry
import com.knoux.kxprivatevpn.platform.repair.DuplicateGroup
import com.knoux.kxprivatevpn.platform.repair.DuplicateScan
import com.knoux.kxprivatevpn.platform.repair.RepairServiceId
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DuplicateDnaRoute(night: Boolean, onBack: () -> Unit) {
    val model: DuplicateDnaViewModel = viewModel()
    val state by model.state.collectAsStateWithLifecycle()
    val treeLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let(model::attachTree)
        }
    BackHandler(enabled = state.cleaning) { }
    DuplicateDnaScreen(
        night = night,
        state = state,
        onBack = onBack,
        onChooseFolder = { treeLauncher.launch(null) },
        onScan = model::scan,
        onCancelScan = model::cancelScan,
        onToggleEntry = model::toggleEntry,
        onSelectAll = model::selectAllDuplicates,
        onSelectGroup = model::selectGroup,
        onClearSelection = model::clearSelection,
        onRequestCleanup = model::requestCleanup,
        onDismissCleanup = model::dismissCleanup,
        onConfirmCleanup = model::confirmCleanup,
        onCancelCleanup = model::cancelCleanup,
    )
}

/**
 * Duplicate DNA screen. Only confirms exact SHA-256 matches; near-duplicates are not offered.
 * Every cleanup action requires explicit selection and a confirmation dialog.
 */
@Composable
fun DuplicateDnaScreen(
    night: Boolean,
    state: DuplicateDnaUiState,
    onBack: () -> Unit,
    onChooseFolder: () -> Unit,
    onScan: () -> Unit,
    onCancelScan: () -> Unit,
    onToggleEntry: (DuplicateEntry) -> Unit,
    onSelectAll: () -> Unit,
    onSelectGroup: (DuplicateGroup) -> Unit,
    onClearSelection: () -> Unit,
    onRequestCleanup: () -> Unit,
    onDismissCleanup: () -> Unit,
    onConfirmCleanup: () -> Unit,
    onCancelCleanup: () -> Unit,
) {
    val context = LocalContext.current
    val foreground = if (night) Color(0xFFF7F3FF) else Color(0xFF171050)
    val scan = state.scan
    val selectedCount = state.selectedDocumentIds.size

    Box(Modifier.fillMaxSize()) {
        Atmosphere(night, 0f)
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .testTag("route-duplicate"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KxPageHeader(
                title = stringResource(R.string.repair_service_duplicate),
                night = night,
                backLabel = stringResource(R.string.back),
                onBack = onBack,
            )
            Text(
                stringResource(R.string.dna_subtitle),
                fontSize = 14.sp, lineHeight = 20.sp,
                color = foreground.copy(alpha = .82f), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            DnaHero(night)
            DnaFolderCard(night, state, foreground, onChooseFolder, onScan, onCancelScan)

            if (scan != null) {
                DnaScanSummary(night, scan, state, foreground)
                if (scan.groups.isEmpty()) {
                    GlassSurface(night) {
                        Text(
                            stringResource(
                                if (scan.cancelled) R.string.dna_cancelled
                                else R.string.dna_empty
                            ),
                            fontSize = 14.sp, color = foreground.copy(alpha = .84f),
                            modifier = Modifier.testTag("dna-empty"),
                        )
                    }
                } else {
                    DnaGroupList(
                        night, scan, state, foreground,
                        onToggleEntry, onSelectAll, onSelectGroup, onClearSelection,
                    )
                    DnaCleanupActions(
                        night, state, selectedCount, foreground,
                        onRequestCleanup, onChooseFolder, onCancelCleanup,
                    )
                }
            }
            DnaReceiptCard(night, state, foreground)
            Text(
                stringResource(R.string.dna_privacy),
                fontSize = 11.sp, lineHeight = 16.sp,
                color = foreground.copy(alpha = .72f),
                modifier = Modifier.testTag("dna-privacy"),
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (state.confirming) {
        // Dialog windows resolve resources against the Activity context. Resolve the copy here,
        // in the localized screen composition, so Arabic never falls back to English.
        val dialogTitle = stringResource(R.string.dna_dialog_title)
        val dialogBody = stringResource(R.string.dna_dialog_body, selectedCount)
        val dialogConfirm = stringResource(R.string.dna_confirm)
        val dialogCancel = stringResource(R.string.dna_cleanup_cancel)
        AlertDialog(
            onDismissRequest = onDismissCleanup,
            title = { Text(dialogTitle) },
            text = {
                Text(dialogBody, modifier = Modifier.verticalScroll(rememberScrollState()))
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmCleanup,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("dna-confirm"),
                ) { Text(dialogConfirm) }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissCleanup,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("dna-dismiss"),
                ) { Text(dialogCancel) }
            },
        )
    }
}

@Composable
private fun DnaHero(night: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center,
    ) {
        RepairGlyph(RepairServiceId.DUPLICATE, Modifier.size(128.dp))
    }
}

@Composable
private fun DnaFolderCard(
    night: Boolean,
    state: DuplicateDnaUiState,
    foreground: Color,
    onChooseFolder: () -> Unit,
    onScan: () -> Unit,
    onCancelScan: () -> Unit,
) {
    GlassSurface(night) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.ghost_folder_title),
                fontWeight = FontWeight.Bold, fontSize = 18.sp,
                color = foreground, modifier = Modifier.weight(1f),
            )
            if (state.scanning) LinearProgressIndicator(Modifier.width(66.dp))
        }
        if (state.treeUri == null) {
            Text(
                stringResource(R.string.dna_tree_none),
                fontSize = 13.sp, lineHeight = 18.sp, color = foreground.copy(alpha = .82f),
            )
            OutlinedButton(
                onClick = onChooseFolder,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .testTag("dna-choose-folder"),
            ) { Text(stringResource(R.string.ghost_choose_folder)) }
        } else {
            Text(
                state.treeName ?: stringResource(R.string.ghost_folder_title),
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = foreground,
                modifier = Modifier.testTag("dna-tree-name"),
            )
            Text(
                stringResource(if (state.writable) R.string.ghost_writable else R.string.ghost_read_only),
                fontSize = 11.sp, lineHeight = 16.sp,
                color = dnaStatusTone(
                    if (state.writable) Color(0xFF64EEFF) else Color(0xFFFFC98A), night
                ),
                modifier = Modifier.testTag(if (state.writable) "dna-writable" else "dna-read-only"),
            )
            if (state.scanning) {
                Text(
                    stringResource(R.string.dna_scanning, state.scanned, state.hashed),
                    fontSize = 12.sp, color = foreground.copy(alpha = .8f),
                    modifier = Modifier.testTag("dna-scan-progress"),
                )
                LinearProgressIndicator(Modifier.fillMaxWidth())
                OutlinedButton(
                    onClick = onCancelScan,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("dna-scan-cancel"),
                ) { Text(stringResource(R.string.ghost_cancel)) }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onScan,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("dna-scan"),
                    ) {
                        Text(stringResource(if (state.scan == null) R.string.dna_scan else R.string.dna_rescan))
                    }
                    TextButton(
                        onClick = onChooseFolder,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("dna-choose-again"),
                    ) { Text(stringResource(R.string.ghost_choose_again)) }
                }
            }
            if (state.scanFailed) {
                Text(
                    stringResource(R.string.dna_scan_failed),
                    fontSize = 13.sp, color = dnaStatusTone(Color(0xFFFFB4B4), night),
                    modifier = Modifier.testTag("dna-scan-failed"),
                )
            }
            if (state.scanCancelled) {
                Text(
                    stringResource(R.string.dna_cancelled),
                    fontSize = 12.sp, color = foreground.copy(alpha = .8f),
                    modifier = Modifier.testTag("dna-cancelled"),
                )
            }
        }
        Text(
            stringResource(R.string.dna_scope),
            fontSize = 11.sp, lineHeight = 16.sp, color = foreground.copy(alpha = .72f),
        )
    }
}

@Composable
private fun DnaScanSummary(
    night: Boolean,
    scan: DuplicateScan,
    state: DuplicateDnaUiState,
    foreground: Color,
) {
    val context = LocalContext.current
    GlassSurface(night) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.dna_results_title),
                fontWeight = FontWeight.Bold, fontSize = 18.sp,
                color = foreground, modifier = Modifier.weight(1f),
            )
            if (scan.truncated) {
                Text(
                    stringResource(R.string.ghost_limited_badge),
                    fontSize = 9.sp, color = dnaStatusTone(Color(0xFFFFC98A), night),
                    modifier = Modifier.testTag("dna-partial"),
                )
            }
        }
        Text(
            stringResource(R.string.dna_scan_counts, scan.scannedFiles, scan.hashedFiles),
            fontSize = 12.sp, color = foreground.copy(alpha = .78f),
            modifier = Modifier.testTag("dna-scan-counts"),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DnaSummaryTile(
                stringResource(R.string.dna_groups_label), scan.duplicateGroupCount,
                Modifier.weight(1f), foreground, night, Color(0xFF64EEFF), "dna-group-count",
            )
            DnaSummaryTile(
                stringResource(R.string.dna_reclaimable_label),
                Formatter.formatFileSize(context, scan.totalReclaimableBytes),
                Modifier.weight(1f), foreground, night, Color(0xFFAD71FF), "dna-reclaimable",
            )
        }
        Text(
            formatDnaDate(context, scan.atMillis),
            fontSize = 10.sp, color = foreground.copy(alpha = .6f),
        )
    }
}

@Composable
private fun DnaSummaryTile(
    label: String,
    value: Any,
    modifier: Modifier,
    foreground: Color,
    night: Boolean,
    accent: Color,
    tag: String,
) {
    val tone = if (night) accent else lerp(accent, Color(0xFF171050), .55f)
    Column(
        modifier
            .background(tone.copy(alpha = .12f), RoundedCornerShape(16.dp))
            .padding(10.dp),
    ) {
        Text(
            value.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold,
            color = tone, modifier = Modifier.testTag(tag),
        )
        Text(label, fontSize = 11.sp, color = foreground.copy(alpha = .8f))
    }
}

@Composable
private fun DnaGroupList(
    night: Boolean,
    scan: DuplicateScan,
    state: DuplicateDnaUiState,
    foreground: Color,
    onToggleEntry: (DuplicateEntry) -> Unit,
    onSelectAll: () -> Unit,
    onSelectGroup: (DuplicateGroup) -> Unit,
    onClearSelection: () -> Unit,
) {
    val context = LocalContext.current
    GlassSurface(night) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.dna_review_title),
                fontWeight = FontWeight.Bold, fontSize = 18.sp,
                color = foreground, modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.dna_selected, state.selectedDocumentIds.size),
                fontSize = 11.sp, color = foreground.copy(alpha = .78f),
                modifier = Modifier.testTag("dna-selected"),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = onSelectAll,
                enabled = !state.scanning && !state.cleaning,
                modifier = Modifier.heightIn(min = 48.dp).testTag("dna-select-all"),
            ) { Text(stringResource(R.string.ghost_select_all)) }
            TextButton(
                onClick = onClearSelection,
                enabled = !state.scanning && !state.cleaning,
                modifier = Modifier.heightIn(min = 48.dp).testTag("dna-clear"),
            ) { Text(stringResource(R.string.ghost_clear_selection)) }
        }
        val groups = scan.groups.take(DuplicateDnaViewModel.MAX_DISPLAYED_GROUPS)
        groups.forEachIndexed { idx, group ->
            DnaGroupCard(
                idx + 1, group, state, night, foreground, context,
                onToggleEntry, onSelectGroup,
            )
        }
        if (scan.groups.size > DuplicateDnaViewModel.MAX_DISPLAYED_GROUPS) {
            Text(
                stringResource(
                    R.string.dna_display_limit, DuplicateDnaViewModel.MAX_DISPLAYED_GROUPS
                ),
                fontSize = 11.sp, color = foreground.copy(alpha = .7f),
                modifier = Modifier.testTag("dna-display-limit"),
            )
        }
    }
}

@Composable
private fun DnaGroupCard(
    number: Int,
    group: DuplicateGroup,
    state: DuplicateDnaUiState,
    night: Boolean,
    foreground: Color,
    context: android.content.Context,
    onToggleEntry: (DuplicateEntry) -> Unit,
    onSelectGroup: (DuplicateGroup) -> Unit,
) {
    val accent = Color(0xFF64EEFF)
    val tone = if (night) accent else lerp(accent, Color(0xFF171050), .55f)
    Column(
        Modifier
            .fillMaxWidth()
            .background(tone.copy(alpha = .08f), RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.dna_group_label, number),
                fontSize = 12.sp, fontWeight = FontWeight.Bold,
                color = tone, modifier = Modifier.weight(1f)
                    .testTag("dna-group-$number"),
            )
            Text(
                Formatter.formatFileSize(context, group.bytes),
                fontSize = 11.sp, color = foreground.copy(alpha = .78f),
            )
            Text(
                stringResource(R.string.dna_reclaimable_bytes,
                    Formatter.formatFileSize(context, group.reclaimableBytes)),
                fontSize = 10.sp, color = tone,
                modifier = Modifier.testTag("dna-group-reclaimable-$number"),
            )
            TextButton(
                onClick = { onSelectGroup(group) },
                enabled = !state.scanning && !state.cleaning,
                modifier = Modifier.heightIn(min = 40.dp).testTag("dna-select-group-$number"),
            ) { Text(stringResource(R.string.dna_select_group), fontSize = 11.sp) }
        }
        // Show SHA-256 prefix for transparency.
        Text(
            stringResource(R.string.dna_sha_prefix, group.sha256.take(16)),
            fontSize = 9.sp, color = foreground.copy(alpha = .50f),
            modifier = Modifier.testTag("dna-sha-$number"),
        )
        group.entries.forEach { entry ->
            DnaEntryRow(entry, state, night, foreground, onToggleEntry)
        }
    }
}

@Composable
private fun DnaEntryRow(
    entry: DuplicateEntry,
    state: DuplicateDnaUiState,
    night: Boolean,
    foreground: Color,
    onToggleEntry: (DuplicateEntry) -> Unit,
) {
    val checked = entry.documentId in state.selectedDocumentIds
    val selectable = !entry.keep
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .then(
                if (selectable) Modifier.toggleable(
                    value = checked,
                    enabled = !state.scanning && !state.cleaning,
                    role = Role.Checkbox,
                    onValueChange = { onToggleEntry(entry) },
                ) else Modifier
            )
            .testTag("dna-entry-${entry.documentId}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectable) {
            Checkbox(
                checked = checked,
                enabled = !state.scanning && !state.cleaning,
                onCheckedChange = null,
            )
        } else {
            Spacer(Modifier.width(40.dp))
        }
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.relativePath, fontSize = 13.sp,
                fontWeight = if (entry.keep) FontWeight.Bold else FontWeight.Normal,
                color = foreground, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            if (entry.keep) {
                Text(
                    stringResource(R.string.dna_keep_label),
                    fontSize = 10.sp,
                    color = dnaStatusTone(Color(0xFF64EEFF), night),
                    modifier = Modifier.testTag("dna-keep-${entry.documentId}"),
                )
            }
        }
    }
}

@Composable
private fun DnaCleanupActions(
    night: Boolean,
    state: DuplicateDnaUiState,
    selectedCount: Int,
    foreground: Color,
    onRequestCleanup: () -> Unit,
    onChooseFolder: () -> Unit,
    onCancelCleanup: () -> Unit,
) {
    GlassSurface(night) {
        Text(
            stringResource(R.string.dna_cleanup_title),
            fontWeight = FontWeight.Bold, fontSize = 18.sp, color = foreground,
        )
        Text(
            stringResource(R.string.dna_cleanup_body),
            fontSize = 12.sp, lineHeight = 17.sp, color = foreground.copy(alpha = .8f),
        )
        when {
            !state.writable -> {
                Text(
                    stringResource(R.string.ghost_not_deletable),
                    fontSize = 12.sp,
                    color = dnaStatusTone(Color(0xFFFFC98A), night),
                    modifier = Modifier.testTag("dna-cleanup-read-only"),
                )
                OutlinedButton(
                    onClick = onChooseFolder,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("dna-regrant"),
                ) { Text(stringResource(R.string.ghost_choose_again)) }
            }
            state.cleaning -> {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(
                    stringResource(R.string.dna_cleaning),
                    fontSize = 13.sp, modifier = Modifier.testTag("dna-cleaning"),
                )
                OutlinedButton(
                    onClick = onCancelCleanup,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("dna-cleanup-cancel"),
                ) { Text(stringResource(R.string.ghost_cancel)) }
            }
            else -> {
                GlassAction(
                    label = stringResource(
                        if (selectedCount == 0) R.string.dna_no_selection
                        else R.string.dna_review_cleanup
                    ),
                    modifier = Modifier.testTag("dna-cleanup"),
                    enabled = selectedCount > 0,
                ) { onRequestCleanup() }
            }
        }
    }
}

@Composable
private fun DnaReceiptCard(
    night: Boolean,
    state: DuplicateDnaUiState,
    foreground: Color,
) {
    val context = LocalContext.current
    val receipt = state.receipt ?: return
    GlassSurface(night) {
        Text(
            stringResource(R.string.dna_receipt_title),
            fontWeight = FontWeight.Bold, fontSize = 18.sp, color = foreground,
        )
        Text(
            stringResource(
                R.string.dna_receipt,
                receipt.deleted, receipt.skipped.size, receipt.failed.size,
            ),
            fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = foreground,
            modifier = Modifier.testTag("dna-receipt"),
        )
        Text(
            stringResource(R.string.dna_receipt_reclaimed,
                Formatter.formatFileSize(context, receipt.bytesReclaimed)),
            fontSize = 13.sp, color = dnaStatusTone(Color(0xFF64EEFF), night),
            modifier = Modifier.testTag("dna-receipt-reclaimed"),
        )
        Text(
            stringResource(R.string.ghost_receipt_verified, receipt.verifiedAbsent),
            fontSize = 12.sp, color = dnaStatusTone(Color(0xFF64EEFF), night),
        )
        if (!receipt.complete) {
            Text(
                stringResource(R.string.dna_cleanup_partial),
                fontSize = 12.sp, color = dnaStatusTone(Color(0xFFFFC98A), night),
                modifier = Modifier.testTag("dna-receipt-partial"),
            )
        }
        receipt.failed.take(5).forEach {
            Text(it, fontSize = 11.sp, color = dnaStatusTone(Color(0xFFFFB4B4), night))
        }
        receipt.skipped.take(5).forEach {
            Text(
                stringResource(R.string.ghost_skipped, it),
                fontSize = 11.sp, color = foreground.copy(alpha = .72f),
            )
        }
        Text(
            stringResource(R.string.ghost_journal_note),
            fontSize = 11.sp, color = foreground.copy(alpha = .7f),
        )
        if (state.journalFailed) {
            Text(
                stringResource(R.string.ghost_journal_failed),
                fontSize = 11.sp, color = dnaStatusTone(Color(0xFFFFC98A), night),
                modifier = Modifier.testTag("dna-journal-failed"),
            )
        }
        if (state.cleanupFailed) {
            Text(
                stringResource(R.string.dna_cleanup_failed),
                fontSize = 12.sp, color = dnaStatusTone(Color(0xFFFFB4B4), night),
                modifier = Modifier.testTag("dna-cleanup-failed"),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun dnaStatusTone(color: Color, night: Boolean): Color =
    if (night) color else lerp(color, Color(0xFF171050), .55f)

private fun formatDnaDate(context: android.content.Context, atMillis: Long): String {
    val locale = context.resources.configuration.locales.get(0) ?: Locale.getDefault()
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale)
        .format(Date(atMillis))
}
