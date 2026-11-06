package com.knoux.kxprivatevpn.features

import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.knoux.kxprivatevpn.platform.media.CompressionOutcome
import com.knoux.kxprivatevpn.platform.media.MediaCandidate
import com.knoux.kxprivatevpn.platform.media.MediaFailure
import com.knoux.kxprivatevpn.platform.media.MediaQuality
import com.knoux.kxprivatevpn.platform.media.MediaSlimScan
import com.knoux.kxprivatevpn.platform.repair.RepairServiceId

@Composable
fun MediaSlimRoute(night: Boolean, onBack: () -> Unit) {
    val model: MediaSlimViewModel = viewModel()
    val state by model.state.collectAsStateWithLifecycle()
    val tree = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(model::attachTree)
    }
    MediaSlimScreen(
        night = night,
        state = state,
        onBack = onBack,
        onChooseFolder = { tree.launch(null) },
        onScan = model::scan,
        onCancelScan = model::cancelScan,
        onToggle = model::toggle,
        onSelectAll = model::selectAllSupported,
        onClear = model::clearSelection,
        onQuality = model::chooseQuality,
        onRequestCompression = model::requestConfirmation,
        onDismissConfirmation = model::dismissConfirmation,
        onConfirmCompression = model::compressConfirmed,
        onCancelCompression = model::cancelCompression,
        onResetReceipt = model::resetReceipt,
    )
}

@Composable
fun MediaSlimScreen(
    night: Boolean,
    state: MediaSlimUiState,
    onBack: () -> Unit,
    onChooseFolder: () -> Unit,
    onScan: () -> Unit,
    onCancelScan: () -> Unit,
    onToggle: (MediaCandidate) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onQuality: (MediaQuality) -> Unit,
    onRequestCompression: () -> Unit,
    onDismissConfirmation: () -> Unit,
    onConfirmCompression: () -> Unit,
    onCancelCompression: () -> Unit,
    onResetReceipt: () -> Unit,
) {
    val context = LocalContext.current
    val foreground = if (night) Color(0xFFF7F3FF) else Color(0xFF171050)
    val scan = state.scan
    // Resolve confirmation copy in the screen's composition. Dialog windows can be recreated
    // with a different context; pre-resolved strings keep the chosen app language and RTL text.
    val confirmTitle = stringResource(R.string.media_confirm_title)
    val confirmBody = stringResource(R.string.media_confirm_body, state.selectedSupported.size)
    val confirmSize = stringResource(R.string.media_confirm_size,
        Formatter.formatFileSize(context, state.selectedBytes))
    val confirmQuality = stringResource(R.string.media_quality_note)
    val confirmStart = stringResource(R.string.media_start)
    val confirmCancel = stringResource(R.string.media_cancel)
    Box(Modifier.fillMaxSize()) {
        Atmosphere(night, 0f)
        Column(
            Modifier.fillMaxSize().safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .testTag("route-media"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KxPageHeader(
                title = stringResource(R.string.media_title),
                night = night,
                backLabel = stringResource(R.string.back),
                onBack = onBack,
            )
            Text(
                stringResource(R.string.media_subtitle),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = foreground.copy(alpha = .84f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            MediaHero()
            FolderCard(night, state, foreground, onChooseFolder, onScan, onCancelScan)
            if (scan != null) {
                ScanSummary(night, scan, foreground)
                if (scan.files.isNotEmpty()) {
                    SelectionSummary(night, state, foreground, onSelectAll, onClear, onQuality)
                    FileList(night, state, foreground, onToggle)
                } else {
                    GlassSurface(night) {
                        Text(
                            stringResource(R.string.media_empty),
                            fontSize = 14.sp, lineHeight = 20.sp,
                            color = foreground.copy(alpha = .84f),
                            modifier = Modifier.testTag("media-empty"),
                        )
                    }
                }
                if (scan.videos.isNotEmpty()) {
                    GlassSurface(night) {
                        Text(
                            stringResource(R.string.media_video_note),
                            fontSize = 12.sp, lineHeight = 17.sp,
                            color = foreground.copy(alpha = .78f),
                            modifier = Modifier.testTag("media-video-note"),
                        )
                    }
                }
                if (state.selectedSupported.isNotEmpty()) {
                    GlassAction(
                        label = stringResource(R.string.media_compress),
                        modifier = Modifier.testTag("media-compress"),
                        enabled = !state.compressing,
                    ) { onRequestCompression() }
                }
                if (state.selectionError) {
                    Text(
                        stringResource(R.string.media_selection_empty),
                        color = danger(night), fontSize = 13.sp,
                        modifier = Modifier.testTag("media-selection-error"),
                    )
                }
                if (state.compressing) {
                    CompressionProgress(night, state, foreground, onCancelCompression)
                }
                if (state.results.isNotEmpty()) {
                    ReceiptCard(night, state, foreground, onResetReceipt)
                }
            }
            Text(
                stringResource(R.string.media_originals_note),
                fontSize = 11.sp, lineHeight = 16.sp,
                color = foreground.copy(alpha = .72f),
                modifier = Modifier.testTag("media-originals-note"),
            )
            Text(
                stringResource(R.string.media_permission_note),
                fontSize = 11.sp, lineHeight = 16.sp,
                color = foreground.copy(alpha = .62f),
                modifier = Modifier.testTag("media-permission-note"),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
    if (state.confirming) {
        AlertDialog(
            onDismissRequest = onDismissConfirmation,
            containerColor = if (night) Color(0xFF1C1542) else Color(0xFFF7F3FF),
            titleContentColor = foreground,
            textContentColor = foreground.copy(alpha = .86f),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 0.dp,
            title = { Text(confirmTitle) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(confirmBody)
                    Spacer(Modifier.height(10.dp))
                    Text(confirmSize)
                    Spacer(Modifier.height(10.dp))
                    Text(confirmQuality, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirmCompression,
                    modifier = Modifier.testTag("media-confirm")) {
                    Text(confirmStart)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissConfirmation) {
                    Text(confirmCancel)
                }
            },
        )
    }
}

@Composable
private fun MediaHero() {
    Box(Modifier.fillMaxWidth().height(186.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(154.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(
                    Color(0xFFFFC2F0).copy(alpha = .34f),
                    Color(0xFF64EEFF).copy(alpha = .10f),
                    Color.Transparent,
                ))),
        )
        RepairGlyph(RepairServiceId.MEDIA, Modifier.size(138.dp))
    }
}

@Composable
private fun FolderCard(
    night: Boolean,
    state: MediaSlimUiState,
    foreground: Color,
    onChooseFolder: () -> Unit,
    onScan: () -> Unit,
    onCancelScan: () -> Unit,
) {
    GlassSurface(night) {
        Text(
            stringResource(R.string.media_scope),
            fontSize = 13.sp, lineHeight = 19.sp,
            color = foreground.copy(alpha = .84f),
        )
        if (state.permissionError) {
            Text(
                stringResource(R.string.media_permission_required),
                fontSize = 13.sp, lineHeight = 19.sp, color = danger(night),
                modifier = Modifier.testTag("media-permission-error"),
            )
        }
        state.treeName?.let {
            Text(
                stringResource(R.string.media_folder, it),
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = foreground, modifier = Modifier.testTag("media-folder-name"),
            )
        }
        OutlinedButton(
            onClick = onChooseFolder,
            enabled = !state.scanning && !state.compressing,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .testTag("media-folder"),
        ) { Text(stringResource(R.string.media_choose_folder)) }
        if (state.treeUri != null) {
            OutlinedButton(
                onClick = onScan,
                enabled = !state.scanning && !state.compressing,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .testTag("media-scan"),
            ) { Text(stringResource(R.string.media_rescan)) }
        }
        if (state.scanning) {
            Text(
                stringResource(R.string.media_scanning),
                fontSize = 13.sp, color = foreground,
                modifier = Modifier.testTag("media-scanning"),
            )
            Text(
                stringResource(R.string.media_scan_progress, state.progress),
                fontSize = 11.sp, color = foreground.copy(alpha = .7f),
            )
            LinearProgressIndicator(Modifier.fillMaxWidth())
            TextButton(onClick = onCancelScan,
                modifier = Modifier.heightIn(min = 48.dp).testTag("media-cancel-scan")) {
                Text(stringResource(R.string.media_cancel))
            }
        }
        state.scan?.let {
            if (it.cancelled) Text(
                stringResource(R.string.media_scan_cancelled),
                fontSize = 12.sp, color = warning(night),
                modifier = Modifier.testTag("media-scan-cancelled"),
            )
            if (it.failed) Text(
                stringResource(R.string.media_scan_failed),
                fontSize = 12.sp, color = danger(night),
                modifier = Modifier.testTag("media-scan-failed"),
            )
            if (it.truncated) Text(
                stringResource(R.string.media_scan_truncated),
                fontSize = 12.sp, color = foreground.copy(alpha = .72f),
                modifier = Modifier.testTag("media-scan-truncated"),
            )
        }
        if (state.scanFailed) Text(
            stringResource(R.string.media_scan_failed),
            fontSize = 12.sp, color = danger(night),
            modifier = Modifier.testTag("media-failed"),
        )
    }
}

@Composable
private fun ScanSummary(night: Boolean, scan: MediaSlimScan, foreground: Color) {
    val context = LocalContext.current
    GlassSurface(night) {
        Text(
            stringResource(R.string.media_scan_summary,
                scan.images.size, scan.videos.size,
                Formatter.formatFileSize(context, scan.imageBytes + scan.videoBytes)),
            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = foreground,
            modifier = Modifier.testTag("media-scan-summary"),
        )
        if (scan.skippedUnknownSize > 0) {
            Text(
                stringResource(R.string.media_files_limit, scan.files.size, scan.files.size),
                fontSize = 11.sp, color = foreground.copy(alpha = .7f),
            )
        }
    }
}

@Composable
private fun SelectionSummary(
    night: Boolean,
    state: MediaSlimUiState,
    foreground: Color,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onQuality: (MediaQuality) -> Unit,
) {
    val context = LocalContext.current
    val enabled = !state.compressing
    val accent = mediaAccent(night)
    GlassSurface(night) {
        Text(
            stringResource(R.string.media_selected, state.selectedSupported.size,
                Formatter.formatFileSize(context, state.selectedBytes)),
            fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = foreground,
            modifier = Modifier.testTag("media-selected-summary"),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onSelectAll, enabled = enabled,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    .testTag("media-select-all")) {
                Text(stringResource(R.string.media_select_all), fontSize = 11.sp,
                    textAlign = TextAlign.Center)
            }
            OutlinedButton(onClick = onClear, enabled = enabled,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    .testTag("media-clear")) {
                Text(stringResource(R.string.media_clear), fontSize = 11.sp,
                    textAlign = TextAlign.Center)
            }
        }
        Text(
            stringResource(R.string.media_quality),
            fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = foreground,
        )
        MediaQuality.entries.forEach { quality ->
            val label = stringResource(when (quality) {
                MediaQuality.HIGH -> R.string.media_quality_high
                MediaQuality.BALANCED -> R.string.media_quality_balanced
                MediaQuality.SMALL -> R.string.media_quality_small
            })
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .testTag("media-quality-${quality.name.lowercase()}")
                    .selectable(
                        selected = state.quality == quality,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { onQuality(quality) },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(20.dp).clip(CircleShape)
                        .border(2.dp,
                            if (state.quality == quality) accent
                            else foreground.copy(alpha = .45f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.quality == quality) {
                        Box(Modifier.size(10.dp).clip(CircleShape)
                            .background(accent))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(label, color = foreground, fontSize = 14.sp)
            }
        }
        Text(
            stringResource(R.string.media_quality_note),
            fontSize = 11.sp, lineHeight = 16.sp,
            color = foreground.copy(alpha = .68f),
        )
    }
}

@Composable
private fun FileList(
    night: Boolean,
    state: MediaSlimUiState,
    foreground: Color,
    onToggle: (MediaCandidate) -> Unit,
) {
    val context = LocalContext.current
    val scan = state.scan ?: return
    val accent = mediaAccent(night)
    GlassSurface(night) {
        Text(
            stringResource(R.string.media_files_title),
            fontWeight = FontWeight.Bold, fontSize = 17.sp, color = foreground,
        )
        state.visibleFiles.forEach { file ->
            val selected = file.documentId in state.selected
            val selectable = file.compressible && !state.compressing
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(if (selected) accent.copy(alpha = .13f)
                    else Color.Transparent)
                    .toggleable(
                        value = selected,
                        enabled = selectable,
                        role = Role.Checkbox,
                        onValueChange = { if (selectable) onToggle(file) },
                    )
                    .padding(vertical = 8.dp, horizontal = 4.dp)
                    .testTag("media-file-${file.documentId}")
                    .semantics(mergeDescendants = true) {
                        contentDescription = file.name
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = selected, onCheckedChange = null, enabled = selectable)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(file.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = foreground, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(file.relativePath, fontSize = 10.sp,
                        color = foreground.copy(alpha = .62f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        stringResource(when {
                            file.kind == com.knoux.kxprivatevpn.platform.media.MediaKind.VIDEO ->
                                R.string.media_badge_video
                            file.compressible -> R.string.media_badge_image
                            else -> R.string.media_badge_unsupported
                        }),
                        fontSize = 10.sp,
                        color = if (file.compressible) accent
                        else foreground.copy(alpha = .58f),
                    )
                }
                Text(Formatter.formatFileSize(context, file.bytes),
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = foreground)
            }
        }
        if (scan.files.size > state.visibleFiles.size) {
            Text(
                stringResource(R.string.media_files_limit,
                    state.visibleFiles.size, scan.files.size),
                fontSize = 11.sp, color = foreground.copy(alpha = .7f),
            )
        }
    }
}

@Composable
private fun CompressionProgress(
    night: Boolean,
    state: MediaSlimUiState,
    foreground: Color,
    onCancel: () -> Unit,
) {
    GlassSurface(night) {
        Text(
            stringResource(R.string.media_progress, state.currentIndex, state.total),
            fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = foreground,
            modifier = Modifier.testTag("media-progress"),
        )
        LinearProgressIndicator(
            progress = { if (state.total == 0) 0f else state.currentIndex.toFloat() / state.total },
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = onCancel,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .testTag("media-cancel-run")) {
            Text(stringResource(R.string.media_cancel_run))
        }
    }
}

@Composable
private fun ReceiptCard(
    night: Boolean,
    state: MediaSlimUiState,
    foreground: Color,
    onReset: () -> Unit,
) {
    val context = LocalContext.current
    val accent = mediaAccent(night)
    GlassSurface(night) {
        Text(
            stringResource(R.string.media_result),
            fontWeight = FontWeight.Bold, fontSize = 18.sp, color = foreground,
        )
        if (state.cancelled) {
            Text(
                stringResource(R.string.media_cancelled),
                fontSize = 12.sp, color = warning(night),
                modifier = Modifier.testTag("media-cancelled"),
            )
        }
        if (state.verified.isEmpty()) {
            Text(
                stringResource(R.string.media_result_none),
                fontSize = 13.sp, color = foreground.copy(alpha = .82f),
                modifier = Modifier.testTag("media-result-none"),
            )
        } else {
            Text(
                stringResource(R.string.media_result_summary,
                    state.verified.size, Formatter.formatFileSize(context, state.savedBytes)),
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = foreground,
                modifier = Modifier.testTag("media-result-summary"),
            )
            state.verified.take(20).forEach { outcome ->
                val value = outcome.value
                Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Text(value.candidate.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = foreground)
                    Text(value.outputName, fontSize = 10.sp,
                        color = foreground.copy(alpha = .64f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        stringResource(R.string.media_result_saved,
                            Formatter.formatFileSize(context, value.savedBytes),
                            Formatter.formatFileSize(context, value.candidate.bytes),
                            Formatter.formatFileSize(context, value.outputBytes)),
                        fontSize = 11.sp, color = accent,
                    )
                    Text(
                        stringResource(R.string.media_result_verified, value.width, value.height),
                        fontSize = 10.sp, color = foreground.copy(alpha = .68f),
                    )
                }
            }
        }
        if (state.failed.isNotEmpty()) {
            Text(
                stringResource(R.string.media_result_failed, state.failed.size),
                fontSize = 12.sp, color = warning(night),
                modifier = Modifier.testTag("media-result-failed"),
            )
            state.failed.take(10).forEach { failed ->
                Text(
                    "${failed.candidate.name}: ${failureText(failed.reason)}",
                    fontSize = 11.sp, lineHeight = 16.sp,
                    color = foreground.copy(alpha = .74f),
                )
            }
        }
        OutlinedButton(onClick = onReset, enabled = !state.compressing,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .testTag("media-new-run")) {
            Text(stringResource(R.string.media_new_run))
        }
    }
}

private fun mediaAccent(night: Boolean): Color =
    if (night) Color(0xFF64EEFF) else Color(0xFF00606E)

private fun danger(night: Boolean): Color =
    if (night) Color(0xFFFFB4B4) else Color(0xFF9B1C1C)

private fun warning(night: Boolean): Color =
    if (night) Color(0xFFFFC98A) else Color(0xFF7A4A00)

@Composable
private fun failureText(reason: MediaFailure): String = stringResource(when (reason) {
    MediaFailure.UNREADABLE -> R.string.media_failure_unreadable
    MediaFailure.CREATE_FAILED -> R.string.media_failure_create
    MediaFailure.WRITE_FAILED -> R.string.media_failure_write
    MediaFailure.EMPTY_OUTPUT -> R.string.media_failure_empty
    MediaFailure.VERIFY_FAILED -> R.string.media_failure_verify
    MediaFailure.NO_SAVING -> R.string.media_failure_no_saving
})
