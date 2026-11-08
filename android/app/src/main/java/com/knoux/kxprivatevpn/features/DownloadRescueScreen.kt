package com.knoux.kxprivatevpn.features

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.designsystem.Atmosphere
import com.knoux.kxprivatevpn.designsystem.GlassSurface
import com.knoux.kxprivatevpn.designsystem.KxPageHeader
import com.knoux.kxprivatevpn.platform.repair.DownloadAction
import com.knoux.kxprivatevpn.platform.repair.DownloadCandidate
import com.knoux.kxprivatevpn.platform.repair.DownloadHealth
import com.knoux.kxprivatevpn.platform.storage.formatBytes

/**
 * Download Rescue.
 *
 * The screen reports measured states only. Every file was read from the folder the user
 * authorised and judged by a structural validator, and a removal only happens after the user
 * confirms it.
 */
@Composable
fun DownloadRescueRoute(night: Boolean, onBack: () -> Unit) {
    val model: DownloadRescueViewModel = viewModel()
    val state by model.state.collectAsStateWithLifecycle()
    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let { model.attachTree(it) } }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri -> model.exportTo(uri) }

    DownloadRescueScreen(
        night = night,
        state = state,
        onBack = onBack,
        onChooseFolder = { folderLauncher.launch(null) },
        onScan = model::scan,
        onForget = model::forgetTree,
        onToggle = model::toggle,
        onSelectAttention = model::selectAttention,
        onClearSelection = model::clearSelection,
        onRequestRemove = model::requestRemove,
        onDismissConfirmation = model::dismissConfirmation,
        onConfirmRemove = model::confirmRemove,
        onExport = {
            if (model.prepareExport()) {
                try {
                    exportLauncher.launch("kx-rescued-copy.bin")
                } catch (_: RuntimeException) {
                    model.exportTo(null)
                }
            }
        },
        onRename = model::renameSelected,
    )
}

/** The stateless screen. Every value it shows comes from [state]; every action is a callback. */
@Composable
fun DownloadRescueScreen(
    night: Boolean,
    state: DownloadRescueUiState,
    onBack: () -> Unit,
    onChooseFolder: () -> Unit,
    onScan: () -> Unit,
    onForget: () -> Unit,
    onToggle: (String) -> Unit,
    onSelectAttention: () -> Unit,
    onClearSelection: () -> Unit,
    onRequestRemove: () -> Unit,
    onDismissConfirmation: () -> Unit,
    onConfirmRemove: () -> Unit,
    onExport: () -> Unit,
    onRename: (String) -> Unit,
) {
    var renaming by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().testTag("route-download")) {
        Atmosphere(night, 0f)
        Column(
            Modifier.fillMaxSize().safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KxPageHeader(
                title = stringResource(R.string.download_title),
                night = night,
                backLabel = stringResource(R.string.back),
                onBack = onBack,
            )

            GlassSurface(night) {
                Text(stringResource(R.string.download_scope_title), fontWeight = FontWeight.Bold,
                    fontSize = 18.sp)
                val treeName = state.treeName
                if (treeName == null) {
                    Text(stringResource(R.string.download_scope_none), fontSize = 13.sp,
                        color = foreground(night).copy(alpha = .84f))
                } else {
                    Text(stringResource(R.string.download_scope_folder, treeName), fontSize = 14.sp,
                        modifier = Modifier.testTag("download-scope"))
                    if (state.mediaStoreUnavailable) {
                        Text(stringResource(R.string.download_scope_media_unavailable), fontSize = 12.sp,
                            color = foreground(night).copy(alpha = .78f),
                            modifier = Modifier.testTag("download-media-unavailable"))
                    } else {
                        Text(stringResource(R.string.download_scope_media, state.mediaStoreRows),
                            fontSize = 12.sp, color = foreground(night).copy(alpha = .78f))
                    }
                }
                Text(stringResource(R.string.download_inspected, state.inspected), fontSize = 14.sp,
                    modifier = Modifier.testTag("download-count"))
                Text(stringResource(R.string.download_attention, state.attention), fontSize = 14.sp,
                    color = foreground(night).copy(alpha = .84f),
                    modifier = Modifier.testTag("download-attention"))
                if (state.truncated) {
                    Text(stringResource(R.string.download_truncated), fontSize = 12.sp,
                        color = foreground(night).copy(alpha = .78f))
                }
                if (state.scanFailed) {
                    Text(stringResource(R.string.download_scan_failed), fontSize = 12.sp,
                        color = Color(0xFFFFC98A), modifier = Modifier.testTag("download-failed"))
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onChooseFolder,
                    enabled = !state.actionsBlocked,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("download-choose-folder"),
                ) { Text(stringResource(R.string.download_choose_folder)) }
                if (treeName != null) {
                    OutlinedButton(
                        onClick = onScan,
                        enabled = !state.actionsBlocked,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("download-scan"),
                    ) { Text(stringResource(if (state.candidates.isEmpty()) R.string.download_scan else R.string.download_rescan)) }
                    OutlinedButton(
                        onClick = onForget,
                        enabled = !state.actionsBlocked,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("download-forget"),
                    ) { Text(stringResource(R.string.download_forget)) }
                }
            }

            if (state.candidates.isNotEmpty()) {
                GlassSurface(night) {
                    Text(stringResource(R.string.download_selected, state.selected.size,
                        formatBytes(state.selectedBytes)), fontSize = 14.sp,
                        modifier = Modifier.testTag("download-selected"))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onSelectAttention,
                            enabled = !state.actionsBlocked,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("download-select-attention"),
                        ) { Text(stringResource(R.string.download_select_attention)) }
                        OutlinedButton(
                            onClick = onClearSelection,
                            enabled = !state.actionsBlocked,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("download-clear"),
                        ) { Text(stringResource(R.string.download_clear)) }
                    }
                }
            }

            if (state.candidates.isEmpty() && state.treeName != null && !state.scanning && !state.scanFailed) {
                GlassSurface(night) {
                    Text(stringResource(R.string.download_empty), fontWeight = FontWeight.Bold,
                        fontSize = 15.sp, modifier = Modifier.testTag("download-empty"))
                    Text(stringResource(R.string.download_empty_hint), fontSize = 12.sp,
                        color = foreground(night).copy(alpha = .8f))
                }
            }

            state.candidates.forEachIndexed { index, candidate ->
                DownloadCard(night, index, candidate, state.selected.contains(candidate.documentId),
                    enabled = !state.actionsBlocked) {
                    onToggle(candidate.documentId)
                }
            }

            if (state.candidates.isNotEmpty()) {
                GlassSurface(night) {
                    OutlinedButton(
                        onClick = onRequestRemove,
                        enabled = !state.actionsBlocked && state.selectedCandidates.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("download-remove"),
                    ) { Text(stringResource(R.string.download_remove)) }
                    OutlinedButton(
                        onClick = onExport,
                        enabled = state.canExport,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("download-export"),
                    ) { Text(stringResource(R.string.download_export)) }
                    Text(stringResource(R.string.download_export_hint), fontSize = 12.sp,
                        modifier = Modifier.testTag("download-export-hint"))
                    OutlinedButton(
                        onClick = { renaming = true },
                        enabled = !state.actionsBlocked && state.selectedCandidates.size == 1,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("download-rename"),
                    ) { Text(stringResource(R.string.download_rename)) }
                }
            }

            state.receipt?.let { receipt ->
                GlassSurface(night) {
                    Text(stringResource(R.string.download_receipt_title), fontWeight = FontWeight.Bold,
                        fontSize = 16.sp, modifier = Modifier.testTag("download-receipt"))
                    Text(
                        when (receipt.action) {
                            DownloadAction.REMOVE -> stringResource(R.string.download_receipt_removed,
                                receipt.succeeded, formatBytes(receipt.bytesRemoved), receipt.failed)
                            DownloadAction.EXPORT -> stringResource(R.string.download_receipt_exported,
                                receipt.succeeded, formatBytes(receipt.bytesCopied), receipt.failed)
                            DownloadAction.RENAME -> stringResource(R.string.download_receipt_renamed,
                                receipt.succeeded, receipt.failed)
                        },
                        fontSize = 14.sp, modifier = Modifier.testTag("download-receipt-summary"),
                    )
                    receipt.results.forEach { result ->
                        Text(stringResource(R.string.download_receipt_line, result.displayName,
                            result.message), fontSize = 12.sp,
                            color = foreground(night).copy(alpha = .82f))
                    }
                }
            }

            GlassSurface(night) {
                Text(stringResource(R.string.download_no_repair), fontSize = 12.sp,
                    color = foreground(night).copy(alpha = .82f),
                    modifier = Modifier.testTag("download-no-repair"))
                Text(stringResource(R.string.download_privacy), fontSize = 11.sp,
                    color = foreground(night).copy(alpha = .74f),
                    modifier = Modifier.testTag("download-privacy"))
            }
        }
    }

    if (state.confirming) {
        // Dialog windows resolve resources against the Activity context. Resolve the copy here,
        // in the localized screen composition, so Arabic never falls back to English.
        val dialogTitle = stringResource(R.string.download_confirm_title)
        val dialogBody = stringResource(R.string.download_confirm_body,
            state.removalTargets.size,
            formatBytes(state.removalTargets.sumOf { it.candidate.bytes }))
        val dialogConfirm = stringResource(R.string.download_confirm)
        val dialogCancel = stringResource(R.string.download_cancel)
        AlertDialog(
            onDismissRequest = onDismissConfirmation,
            title = { Text(dialogTitle) },
            text = { Text(dialogBody) },
            confirmButton = {
                TextButton(onClick = onConfirmRemove,
                    enabled = state.canConfirmRemove,
                    modifier = Modifier.testTag("download-confirm")) {
                    Text(dialogConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissConfirmation,
                    modifier = Modifier.testTag("download-cancel")) {
                    Text(dialogCancel)
                }
            },
        )
    }

    if (renaming) {
        val dialogTitle = stringResource(R.string.download_rename_title)
        val dialogHint = stringResource(R.string.download_rename_hint)
        val dialogLabel = stringResource(R.string.download_rename_value)
        val dialogConfirm = stringResource(R.string.download_rename_confirm)
        val dialogCancel = stringResource(R.string.download_cancel)
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text(dialogTitle) },
            text = {
                Column {
                    Text(dialogHint, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { if (it.length <= 255) newName = it },
                        label = { Text(dialogLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("download-rename-field"),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    renaming = false
                    onRename(newName)
                }, modifier = Modifier.testTag("download-rename-confirm")) {
                    Text(dialogConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { renaming = false },
                    modifier = Modifier.testTag("download-rename-cancel")) {
                    Text(dialogCancel)
                }
            },
        )
    }
}

@Composable
private fun DownloadCard(
    night: Boolean,
    index: Int,
    candidate: DownloadCandidate,
    selected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    val rim = if (selected) Color(0xFF79E7FF).copy(alpha = .85f) else Color.White.copy(alpha = if (night) .28f else .8f)
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
            .testTag("download-card-$index"),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // The selection control is its own node so every measured value stays addressable
            // for the screen reader and for the test.
            Checkbox(
                checked = selected,
                enabled = enabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("download-select-$index"),
            )
            Column(Modifier.weight(1f)) {
                Text(candidate.displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    maxLines = 2, modifier = Modifier.testTag("download-name-$index"))
                Text(formatBytes(candidate.bytes), fontSize = 12.sp,
                    color = foreground(night).copy(alpha = .78f))
                Text(healthLabel(candidate.health), fontSize = 13.sp,
                    color = healthColor(candidate.health, night),
                    modifier = Modifier.testTag("download-health-$index"))
                Text(reasonLabel(candidate.reason), fontSize = 11.sp,
                    color = foreground(night).copy(alpha = .74f),
                    modifier = Modifier.testTag("download-reason-$index"))
            }
        }
    }
}

@Composable
private fun healthLabel(health: DownloadHealth): String = stringResource(
    when (health) {
        DownloadHealth.INTACT -> R.string.download_health_intact
        DownloadHealth.TRUNCATED -> R.string.download_health_truncated
        DownloadHealth.CORRUPTED -> R.string.download_health_corrupted
        DownloadHealth.ZERO_BYTE -> R.string.download_health_zero_byte
        DownloadHealth.UNSUPPORTED -> R.string.download_health_unsupported
        DownloadHealth.UNREADABLE -> R.string.download_health_unreadable
    },
)

@Composable
private fun reasonLabel(reason: String): String = stringResource(
    when (reason) {
        "no_bytes" -> R.string.download_reason_no_bytes
        "no_validator" -> R.string.download_reason_no_validator
        "unreadable" -> R.string.download_reason_unreadable
        "bad_header" -> R.string.download_reason_bad_header
        "missing_tail" -> R.string.download_reason_missing_tail
        else -> R.string.download_reason_verified
    },
)

private fun healthColor(health: DownloadHealth, night: Boolean): Color = when (health) {
    DownloadHealth.INTACT -> if (night) Color(0xFF64EEFF) else Color(0xFF0E6C86)
    DownloadHealth.TRUNCATED -> Color(0xFFFFC98A)
    DownloadHealth.CORRUPTED -> Color(0xFFFF9A9A)
    DownloadHealth.ZERO_BYTE -> Color(0xFFFF9A9A)
    DownloadHealth.UNSUPPORTED -> if (night) Color(0xFFC9A6FF) else Color(0xFF5B3FB8)
    DownloadHealth.UNREADABLE -> Color(0xFFFFC98A)
}

private fun foreground(night: Boolean): Color = if (night) Color(0xFFF7F3FF) else Color(0xFF171050)
