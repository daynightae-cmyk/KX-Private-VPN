package com.knoux.kxprivatevpn.features

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.designsystem.KxDivider
import com.knoux.kxprivatevpn.designsystem.KxGlassIconTile
import com.knoux.kxprivatevpn.designsystem.KxGlassPanel
import com.knoux.kxprivatevpn.designsystem.KxGradientAction
import com.knoux.kxprivatevpn.designsystem.KxIconKind
import com.knoux.kxprivatevpn.designsystem.KxLineIcon
import com.knoux.kxprivatevpn.designsystem.KxOrbBadge
import com.knoux.kxprivatevpn.designsystem.KxReferenceAtmosphere
import com.knoux.kxprivatevpn.designsystem.KxReferenceHeader
import com.knoux.kxprivatevpn.designsystem.KxReferenceTheme
import com.knoux.kxprivatevpn.designsystem.KxTitleBlock
import com.knoux.kxprivatevpn.designsystem.RepairGlyph
import com.knoux.kxprivatevpn.designsystem.kxCyan
import com.knoux.kxprivatevpn.designsystem.kxGreen
import com.knoux.kxprivatevpn.designsystem.kxInk
import com.knoux.kxprivatevpn.designsystem.kxMuted
import com.knoux.kxprivatevpn.designsystem.kxRose
import com.knoux.kxprivatevpn.designsystem.kxViolet
import com.knoux.kxprivatevpn.launch.rememberReducedMotion
import com.knoux.kxprivatevpn.platform.repair.DownloadAction
import com.knoux.kxprivatevpn.platform.repair.DownloadCandidate
import com.knoux.kxprivatevpn.platform.repair.DownloadHealth
import com.knoux.kxprivatevpn.platform.repair.RepairServiceId
import com.knoux.kxprivatevpn.platform.storage.formatBytes
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

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

/** Which measured health states are visible. Filtering never rescans and never changes a file. */
private enum class DownloadFilter(val label: Int) {
    ALL(R.string.download_ref_filter_all),
    INTACT(R.string.download_ref_filter_intact),
    INCOMPLETE(R.string.download_ref_filter_incomplete),
    FAILED(R.string.download_ref_filter_failed),
    UNVERIFIED(R.string.download_ref_filter_unverified),
    ;

    fun matches(candidate: DownloadCandidate): Boolean = when (this) {
        ALL -> true
        INTACT -> candidate.health == DownloadHealth.INTACT
        INCOMPLETE -> candidate.health == DownloadHealth.TRUNCATED
        FAILED -> candidate.health == DownloadHealth.CORRUPTED ||
            candidate.health == DownloadHealth.ZERO_BYTE ||
            candidate.health == DownloadHealth.UNREADABLE
        UNVERIFIED -> candidate.health == DownloadHealth.UNSUPPORTED
    }
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
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(DownloadFilter.ALL) }

    // The list is filtered from the measured scan already in memory. A query or a chip must
    // never trigger a new provider read, so this is a pure derivation of [state.candidates].
    val visible = remember(state.candidates, query, filter) {
        state.candidates.withIndex().filter { (_, candidate) ->
            filter.matches(candidate) &&
                (query.isBlank() || candidate.displayName.contains(query.trim(), ignoreCase = true))
        }
    }
    val ink = kxInk(night)
    val hasCandidates = state.candidates.isNotEmpty()
    val selectedCount = state.selected.size
    val showBottomBar = hasCandidates

    KxReferenceTheme(night) {
        Box(Modifier.fillMaxSize()) {
            KxReferenceAtmosphere(night)
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp)
                        .padding(bottom = 20.dp)
                        .testTag("route-download"),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    KxReferenceHeader(
                        night = night,
                        actionLabel = stringResource(R.string.back),
                        tag = "download-ref-back",
                        icon = KxIconKind.BACK,
                        onClick = onBack,
                    )

                    if (hasCandidates) {
                        ResultsOrb(night)
                        KxTitleBlock(
                            title = stringResource(R.string.download_title),
                            subtitle = stringResource(R.string.download_ref_results_subtitle),
                            night = night,
                        )
                        DownloadSummaryCard(night, state)
                        FolderScopeCard(
                            night, state, compact = true,
                            onChooseFolder = onChooseFolder,
                            onScan = onScan,
                            onForget = onForget,
                        )
                        DownloadFilterCard(
                            night = night,
                            state = state,
                            query = query,
                            filter = filter,
                            visibleCount = visible.size,
                            onQuery = { query = it },
                            onFilter = { filter = it },
                            onSelectAttention = onSelectAttention,
                            onClearSelection = onClearSelection,
                        )
                        if (state.scanning) {
                            InlineNotice(
                                text = stringResource(
                                    R.string.download_ref_scanning_progress, state.scanProgress
                                ),
                                night = night,
                                tag = "download-scanning",
                            )
                        }
                        if (visible.isEmpty()) {
                            KxGlassPanel(night, radius = 20.dp, padding = PaddingValues(14.dp)) {
                                Text(
                                    stringResource(R.string.download_ref_no_match),
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    color = kxMuted(night),
                                    modifier = Modifier.testTag("download-no-match"),
                                )
                            }
                        } else {
                            visible.forEach { entry ->
                                DownloadRow(
                                    night = night,
                                    index = entry.index,
                                    candidate = entry.value,
                                    selected = state.selected.contains(entry.value.documentId),
                                    enabled = !state.actionsBlocked,
                                    onToggle = { onToggle(entry.value.documentId) },
                                )
                            }
                        }
                        DownloadActionPanel(
                            night = night,
                            state = state,
                            onSelectAttention = onSelectAttention,
                            onClearSelection = onClearSelection,
                            onRequestRemove = onRequestRemove,
                            onExport = onExport,
                            onRename = { renaming = true },
                        )
                        state.receipt?.let { DownloadReceiptCard(night, it) }
                        DownloadHonestyPanel(night)
                    } else {
                        KxTitleBlock(
                            title = stringResource(R.string.download_title),
                            subtitle = stringResource(R.string.download_ref_subtitle),
                            night = night,
                        )
                        DownloadHero(night, scanning = state.scanning, state = state)
                        if (state.treeUri == null) {
                            KxGradientAction(
                                label = stringResource(R.string.download_ref_choose_action),
                                night = night,
                                enabled = !state.actionsBlocked,
                                tag = "download-choose-folder",
                                icon = KxIconKind.FOLDER,
                                onClick = onChooseFolder,
                            )
                        } else {
                            KxGradientAction(
                                label = stringResource(
                                    if (state.scanning) R.string.download_ref_scanning_action
                                    else R.string.download_scan
                                ),
                                night = night,
                                enabled = !state.actionsBlocked,
                                tag = "download-scan",
                                icon = KxIconKind.SEARCH,
                                onClick = onScan,
                            )
                        }
                        FolderScopeCard(
                            night, state, compact = false,
                            onChooseFolder = onChooseFolder,
                            onScan = onScan,
                            onForget = onForget,
                        )
                        LastScanCard(night, state)
                        if (state.candidates.isEmpty() && state.treeUri != null &&
                            !state.scanning && !state.scanFailed
                        ) {
                            KxGlassPanel(night, radius = 20.dp, padding = PaddingValues(14.dp)) {
                                Text(
                                    stringResource(R.string.download_empty),
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ink,
                                    modifier = Modifier.testTag("download-empty"),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    stringResource(R.string.download_empty_hint),
                                    fontSize = 11.5.sp,
                                    lineHeight = 17.sp,
                                    color = kxMuted(night),
                                )
                            }
                        }
                        if (state.scanFailed) {
                            InlineNotice(
                                text = stringResource(R.string.download_scan_failed),
                                night = night,
                                tag = "download-failed",
                                accent = kxRose(night),
                            )
                        }
                        state.receipt?.let { DownloadReceiptCard(night, it) }
                        DownloadHonestyPanel(night)
                    }
                    Spacer(Modifier.height(4.dp))
                }

                if (showBottomBar) {
                    DownloadSelectionBar(
                        night = night,
                        selectedCount = selectedCount,
                        selectedBytes = state.selectedBytes,
                        canRemove = !state.actionsBlocked && state.selectedCandidates.isNotEmpty(),
                        onRemove = onRequestRemove,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }

    if (state.confirming) {
        // Dialog windows resolve resources against the Activity context. Resolve the copy here,
        // in the localized screen composition, so Arabic never falls back to English.
        val dialogTitle = stringResource(R.string.download_confirm_title)
        val dialogBody = stringResource(
            R.string.download_confirm_body,
            state.removalTargets.size,
            formatBytes(state.removalTargets.sumOf { it.candidate.bytes }),
        )
        val dialogConfirm = stringResource(R.string.download_confirm)
        val dialogCancel = stringResource(R.string.download_cancel)
        AlertDialog(
            onDismissRequest = onDismissConfirmation,
            title = { Text(dialogTitle) },
            text = { Text(dialogBody) },
            confirmButton = {
                TextButton(
                    onClick = onConfirmRemove,
                    enabled = state.canConfirmRemove,
                    modifier = Modifier.testTag("download-confirm"),
                ) { Text(dialogConfirm) }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissConfirmation,
                    modifier = Modifier.testTag("download-cancel"),
                ) { Text(dialogCancel) }
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
                TextButton(
                    onClick = {
                        renaming = false
                        onRename(newName)
                    },
                    modifier = Modifier.testTag("download-rename-confirm"),
                ) { Text(dialogConfirm) }
            },
            dismissButton = {
                TextButton(
                    onClick = { renaming = false },
                    modifier = Modifier.testTag("download-rename-cancel"),
                ) { Text(dialogCancel) }
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Hero
// ---------------------------------------------------------------------------

@Composable
private fun ResultsOrb(night: Boolean) {
    Box(
        Modifier.fillMaxWidth().height(96.dp).testTag("download-results-orb"),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
            KxOrbBadge(84.dp, Modifier, night)
            RepairGlyph(RepairServiceId.DOWNLOAD, Modifier.size(40.dp))
        }
    }
}

@Composable
private fun DownloadHero(night: Boolean, scanning: Boolean, state: DownloadRescueUiState) {
    val reduced = rememberReducedMotion()
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(scanning, reduced) {
        if (scanning && !reduced) {
            sweep.snapTo(0f)
            sweep.animateTo(360f, tween(2100, easing = LinearEasing))
        } else {
            sweep.snapTo(0f)
        }
    }
    val fontScale = LocalDensity.current.fontScale
    Box(
        Modifier.fillMaxWidth().height(if (fontScale > 1.3f) 232.dp else 244.dp)
            .testTag("download-hero"),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().clearAndSetSemantics {}) {
            val c = Offset(size.width / 2f, size.height * .45f)
            val radius = min(size.width, size.height) * .32f
            drawCircle(
                Brush.radialGradient(
                    listOf(
                        kxCyan(night).copy(alpha = .24f),
                        kxViolet(night).copy(alpha = .16f),
                        Color.Transparent,
                    ),
                    c,
                    radius * 1.30f,
                ),
                radius * 1.30f,
                c,
            )
            repeat(3) { index ->
                drawCircle(
                    kxCyan(night).copy(alpha = .20f - index * .05f),
                    radius * (.86f + index * .18f),
                    c,
                    style = Stroke(1.dp.toPx()),
                )
            }
            repeat(8) { index ->
                val angle = Math.toRadians((index * 47.0 + 12.0))
                val orbit = radius * 1.20f
                drawCircle(
                    kxViolet(night).copy(alpha = .70f),
                    1.6.dp.toPx(),
                    Offset(
                        c.x + cos(angle).toFloat() * orbit,
                        c.y + sin(angle).toFloat() * orbit * .62f,
                    ),
                )
            }
            drawArc(
                Brush.sweepGradient(
                    listOf(
                        Color.Transparent,
                        kxCyan(night),
                        kxViolet(night),
                        Color.Transparent,
                    ),
                    c,
                ),
                startAngle = sweep.value,
                sweepAngle = 118f,
                useCenter = false,
                topLeft = Offset(c.x - radius, c.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(2.6.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        // Floating format tiles echo the reference composition. They are decorative and are
        // omitted at large font scale so they can never crowd the hint pill.
        if (fontScale <= 1.3f) {
            FloatingFormatTile(
                night, KxIconKind.DOWNLOAD, kxViolet(night), Modifier
                    .align(Alignment.TopStart).padding(start = 20.dp, top = 18.dp),
            )
            FloatingFormatTile(
                night, KxIconKind.FILE, kxCyan(night), Modifier
                    .align(Alignment.TopEnd).padding(end = 16.dp, top = 30.dp),
            )
            FloatingFormatTile(
                night, KxIconKind.COPY, kxRose(night), Modifier
                    .align(Alignment.BottomStart).padding(start = 30.dp, bottom = 38.dp),
            )
            FloatingFormatTile(
                night, KxIconKind.FOLDER, kxGreen(night), Modifier
                    .align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 34.dp),
            )
        }

        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(142.dp), contentAlignment = Alignment.Center) {
                KxOrbBadge(142.dp, Modifier, night)
                RepairGlyph(RepairServiceId.DOWNLOAD, Modifier.size(68.dp))
            }
            Spacer(Modifier.height(9.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(kxCyan(night).copy(alpha = .10f))
                    .border(1.dp, kxCyan(night).copy(alpha = .42f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("download-hero-hint"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KxLineIcon(
                    if (scanning) KxIconKind.CLOCK else KxIconKind.SHIELD_CHECK,
                    kxCyan(night),
                    Modifier.size(15.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    stringResource(
                        when {
                            scanning && state.scanProgress > 0 ->
                                R.string.download_ref_scanning_progress
                            scanning -> R.string.download_ref_scanning_hint
                            else -> R.string.download_ref_hero_hint
                        },
                        state.scanProgress,
                    ),
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = kxCyan(night),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun FloatingFormatTile(
    night: Boolean,
    icon: KxIconKind,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.size(46.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(
                Brush.linearGradient(
                    if (night) listOf(Color.White.copy(alpha = .12f), accent.copy(alpha = .18f))
                    else listOf(Color.White.copy(alpha = .82f), accent.copy(alpha = .14f)),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = if (night) .30f else .82f), RoundedCornerShape(15.dp))
            .padding(10.dp)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        KxLineIcon(icon, accent, Modifier.fillMaxSize())
    }
}

// ---------------------------------------------------------------------------
// Folder scope
// ---------------------------------------------------------------------------

@Composable
private fun FolderScopeCard(
    night: Boolean,
    state: DownloadRescueUiState,
    compact: Boolean,
    onChooseFolder: () -> Unit,
    onScan: () -> Unit,
    onForget: () -> Unit,
) {
    val ink = kxInk(night)
    KxGlassPanel(
        night,
        modifier = Modifier.testTag(if (compact) "download-scope" else "download-scope-card"),
        radius = 22.dp,
        padding = PaddingValues(if (compact) 12.dp else 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KxGlassIconTile(
                night, KxIconKind.FOLDER, kxViolet(night),
                size = if (compact) 42.dp else 46.dp,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.download_ref_folder),
                    fontSize = 9.5.sp,
                    lineHeight = 13.sp,
                    color = kxMuted(night),
                )
                val treeName = state.treeName
                if (treeName != null) {
                    Text(
                        stringResource(R.string.download_scope_folder, treeName),
                        fontSize = if (compact) 14.sp else 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("download-tree-name"),
                    )
                } else {
                    Text(
                        stringResource(R.string.download_ref_no_folder),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ink,
                    )
                }
                if (state.treeUri == null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.download_scope_none),
                        fontSize = 10.5.sp,
                        lineHeight = 15.sp,
                        color = kxMuted(night),
                    )
                }
            }
        }

        if (state.treeUri != null) {
            Spacer(Modifier.height(8.dp))
            if (state.mediaStoreUnavailable) {
                Text(
                    stringResource(R.string.download_scope_media_unavailable),
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                    color = kxMuted(night),
                    modifier = Modifier.testTag("download-media-unavailable"),
                )
            } else {
                Text(
                    stringResource(R.string.download_ref_media, state.mediaStoreRows),
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                    color = kxMuted(night),
                )
            }
            if (state.truncated) {
                Text(
                    stringResource(R.string.download_truncated),
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                    color = kxRose(night),
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.download_inspected, state.inspected),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ink,
                    modifier = Modifier.testTag("download-count"),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.download_attention, state.attention),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (state.attention > 0) kxRose(night) else kxGreen(night),
                    textAlign = TextAlign.End,
                    modifier = Modifier.testTag("download-attention"),
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        if (compact) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DownloadPillAction(
                    label = stringResource(
                        if (state.treeUri == null) R.string.download_ref_choose_action
                        else R.string.download_ref_change_folder
                    ),
                    night = night,
                    enabled = !state.actionsBlocked,
                    tag = "download-choose-folder",
                    onClick = onChooseFolder,
                )
                if (state.treeUri != null) {
                    DownloadPillAction(
                        label = stringResource(R.string.download_ref_rescan),
                        night = night,
                        enabled = !state.actionsBlocked,
                        tag = "download-scan",
                        onClick = onScan,
                    )
                    DownloadPillAction(
                        label = stringResource(R.string.download_forget),
                        night = night,
                        enabled = !state.actionsBlocked,
                        tag = "download-forget",
                        onClick = onForget,
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.treeUri != null) {
                    DownloadGlassButton(
                        label = stringResource(R.string.download_ref_change_folder),
                        night = night,
                        enabled = !state.actionsBlocked,
                        tag = "download-choose-folder",
                        icon = KxIconKind.FOLDER,
                        onClick = onChooseFolder,
                    )
                    DownloadGlassButton(
                        label = stringResource(R.string.download_forget),
                        night = night,
                        enabled = !state.actionsBlocked,
                        tag = "download-forget",
                        onClick = onForget,
                    )
                }
            }
        }
    }
}

@Composable
private fun LastScanCard(night: Boolean, state: DownloadRescueUiState) {
    KxGlassPanel(
        night,
        modifier = Modifier.testTag("download-last-scan"),
        radius = 22.dp,
        padding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KxGlassIconTile(night, KxIconKind.CLOCK, kxCyan(night), size = 40.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.download_ref_last_scan),
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = kxInk(night),
                )
                Text(
                    stringResource(R.string.download_ref_no_scan),
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                    color = kxMuted(night),
                    modifier = Modifier.testTag("download-no-scan"),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Results summary
// ---------------------------------------------------------------------------

@Composable
private fun DownloadSummaryCard(night: Boolean, state: DownloadRescueUiState) {
    val candidates = state.candidates
    val incomplete = candidates.count { it.health == DownloadHealth.TRUNCATED }
    val failed = candidates.count { it.health == DownloadHealth.CORRUPTED }
    val zero = candidates.count { it.health == DownloadHealth.ZERO_BYTE }
    val unreadable = candidates.count { it.health == DownloadHealth.UNREADABLE }
    KxGlassPanel(
        night,
        modifier = Modifier.testTag("download-summary"),
        radius = 22.dp,
        padding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KxGlassIconTile(night, KxIconKind.DOWNLOAD, kxViolet(night), size = 52.dp)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.download_ref_summary_title),
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = kxInk(night),
                )
                Text(
                    stringResource(R.string.download_ref_summary_note),
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = kxMuted(night),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SummaryMetric(
                value = candidates.size,
                label = stringResource(R.string.download_ref_total),
                accent = kxInk(night),
                tag = "download-summary-total",
                modifier = Modifier.weight(1f),
            )
            KxDivider(night, Modifier.height(34.dp))
            SummaryMetric(
                value = incomplete,
                label = stringResource(R.string.download_ref_incomplete),
                accent = kxCyan(night),
                tag = "download-summary-incomplete",
                modifier = Modifier.weight(1f),
            )
            KxDivider(night, Modifier.height(34.dp))
            SummaryMetric(
                value = failed + zero + unreadable,
                label = stringResource(R.string.download_ref_failed),
                accent = kxRose(night),
                tag = "download-summary-failed",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SummaryMetric(
    value: Int,
    label: String,
    accent: Color,
    tag: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value.toString(),
            fontSize = 21.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = accent,
            modifier = Modifier.testTag(tag),
        )
        Text(
            label,
            fontSize = 9.5.sp,
            lineHeight = 13.sp,
            color = accent.copy(alpha = .84f),
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

// ---------------------------------------------------------------------------
// Search and measured filters
// ---------------------------------------------------------------------------

@Composable
private fun DownloadFilterCard(
    night: Boolean,
    state: DownloadRescueUiState,
    query: String,
    filter: DownloadFilter,
    visibleCount: Int,
    onQuery: (String) -> Unit,
    onFilter: (DownloadFilter) -> Unit,
    onSelectAttention: () -> Unit,
    onClearSelection: () -> Unit,
) {
    val ink = kxInk(night)
    KxGlassPanel(
        night,
        modifier = Modifier.testTag("download-filters"),
        radius = 20.dp,
        padding = PaddingValues(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 46.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = if (night) .07f else .46f))
                .border(1.dp, Color.White.copy(alpha = if (night) .20f else .72f), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KxLineIcon(KxIconKind.SEARCH, kxMuted(night), Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = TextStyle(
                    color = ink,
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                cursorBrush = SolidColor(kxCyan(night)),
                modifier = Modifier.weight(1f).testTag("download-search"),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                stringResource(R.string.download_ref_search_hint),
                                fontSize = 13.sp,
                                color = kxMuted(night),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        inner()
                    }
                },
            )
            if (query.isNotEmpty()) {
                Box(
                    Modifier.size(32.dp).clip(CircleShape)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = stringResource(R.string.download_ref_search_clear),
                        ) { onQuery("") }
                        .testTag("download-search-clear"),
                    contentAlignment = Alignment.Center,
                ) {
                    KxLineIcon(KxIconKind.CHECK, kxCyan(night), Modifier.size(14.dp))
                }
            }
        }
        Spacer(Modifier.height(9.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            DownloadFilter.entries.forEach { item ->
                val count = state.candidates.count { item.matches(it) }
                FilterChip(
                    label = stringResource(
                        R.string.download_ref_filter_count,
                        stringResource(item.label),
                        count,
                    ),
                    night = night,
                    selected = filter == item,
                    tag = "download-filter-${item.name.lowercase()}",
                    onClick = { onFilter(item) },
                )
            }
        }
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(
                    R.string.download_ref_visible, visibleCount, state.candidates.size
                ),
                fontSize = 10.sp,
                lineHeight = 14.sp,
                color = kxMuted(night),
                modifier = Modifier.weight(1f).testTag("download-visible-count"),
            )
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(kxViolet(night).copy(alpha = .16f))
                    .border(1.dp, kxViolet(night).copy(alpha = .52f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("download-selected-chip"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.download_ref_selected_chip, state.selected.size),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ink,
                )
            }
        }
        Spacer(Modifier.height(9.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DownloadPillAction(
                label = stringResource(R.string.download_select_attention),
                night = night,
                enabled = !state.actionsBlocked,
                tag = "download-select-attention",
                onClick = onSelectAttention,
            )
            DownloadPillAction(
                label = stringResource(R.string.download_clear),
                night = night,
                enabled = !state.actionsBlocked && state.selected.isNotEmpty(),
                tag = "download-clear",
                onClick = onClearSelection,
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    night: Boolean,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.heightIn(min = 38.dp)
            .clip(shape)
            .background(
                if (selected) {
                    Brush.horizontalGradient(
                        listOf(
                            kxViolet(night).copy(alpha = .82f),
                            Color(0xFF5B45F5).copy(alpha = .78f),
                            kxCyan(night).copy(alpha = .72f),
                        ),
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = if (night) .07f else .46f),
                            kxMuted(night).copy(alpha = .08f),
                        ),
                    )
                },
            )
            .border(
                1.dp,
                if (selected) Color.White.copy(alpha = .72f)
                else Color.White.copy(alpha = if (night) .20f else .66f),
                shape,
            )
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else kxInk(night),
            maxLines = 1,
        )
    }
}

// ---------------------------------------------------------------------------
// Measured rows
// ---------------------------------------------------------------------------

@Composable
private fun DownloadRow(
    night: Boolean,
    index: Int,
    candidate: DownloadCandidate,
    selected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val accent = healthAccent(candidate.health, night)
    Column(
        Modifier.fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    if (night) {
                        listOf(Color.White.copy(alpha = .07f), kxViolet(night).copy(alpha = .12f))
                    } else {
                        listOf(Color.White.copy(alpha = .62f), kxCyan(night).copy(alpha = .10f))
                    },
                ),
            )
            .border(
                1.dp,
                if (selected) kxCyan(night).copy(alpha = .86f)
                else Color.White.copy(alpha = if (night) .22f else .78f),
                shape,
            )
            .padding(11.dp)
            .testTag("download-card-$index"),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = selected,
                enabled = enabled,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = kxCyan(night),
                    uncheckedColor = kxMuted(night),
                    checkmarkColor = Color(0xFF082033),
                ),
                modifier = Modifier.testTag("download-select-$index"),
            )
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    candidate.displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    lineHeight = 19.sp,
                    color = kxInk(night),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("download-name-$index"),
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(accent.copy(alpha = .16f))
                            .border(1.dp, accent.copy(alpha = .62f), RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            healthLabel(candidate.health),
                            fontSize = 9.5.sp,
                            lineHeight = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            modifier = Modifier.testTag("download-health-$index"),
                        )
                    }
                    Spacer(Modifier.width(7.dp))
                    Text(
                        stringResource(R.string.download_ref_size, formatBytes(candidate.bytes)),
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = kxMuted(night),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    reasonLabel(candidate.reason),
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = kxMuted(night),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("download-reason-$index"),
                )
            }
            Spacer(Modifier.width(8.dp))
            FormatBadge(night, candidate)
        }
    }
}

@Composable
private fun FormatBadge(night: Boolean, candidate: DownloadCandidate) {
    val label = candidate.format?.extension?.uppercase() ?: "FILE"
    val accent = when (candidate.format?.extension) {
        "pdf" -> kxRose(night)
        "png", "jpg" -> kxGreen(night)
        "mp4" -> kxViolet(night)
        "zip" -> kxCyan(night)
        else -> kxMuted(night)
    }
    Box(
        Modifier.size(42.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(accent.copy(alpha = .14f))
            .border(1.dp, accent.copy(alpha = .48f), RoundedCornerShape(13.dp))
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 8.5.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = accent,
            maxLines = 1,
        )
    }
}

// ---------------------------------------------------------------------------
// Actions
// ---------------------------------------------------------------------------

@Composable
private fun DownloadActionPanel(
    night: Boolean,
    state: DownloadRescueUiState,
    onSelectAttention: () -> Unit,
    onClearSelection: () -> Unit,
    onRequestRemove: () -> Unit,
    onExport: () -> Unit,
    onRename: () -> Unit,
) {
    KxGlassPanel(
        night,
        modifier = Modifier.testTag("download-actions"),
        radius = 20.dp,
        padding = PaddingValues(13.dp),
    ) {
        Text(
            stringResource(R.string.download_ref_actions),
            fontSize = 10.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            color = kxMuted(night),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(
                R.string.download_selected,
                state.selected.size,
                formatBytes(state.selectedBytes),
            ),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = kxInk(night),
            modifier = Modifier.testTag("download-selected"),
        )
        Spacer(Modifier.height(9.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DownloadPillAction(
                label = stringResource(R.string.download_select_attention),
                night = night,
                enabled = !state.actionsBlocked,
                tag = "download-select-attention-actions",
                onClick = onSelectAttention,
            )
            DownloadPillAction(
                label = stringResource(R.string.download_clear),
                night = night,
                enabled = !state.actionsBlocked && state.selected.isNotEmpty(),
                tag = "download-clear-actions",
                onClick = onClearSelection,
            )
        }
        Spacer(Modifier.height(9.dp))
        DownloadGlassButton(
            label = stringResource(R.string.download_remove),
            night = night,
            enabled = !state.actionsBlocked && state.selectedCandidates.isNotEmpty(),
            tag = "download-remove",
            icon = KxIconKind.BROOM,
            onClick = onRequestRemove,
        )
        Spacer(Modifier.height(8.dp))
        DownloadGlassButton(
            label = stringResource(R.string.download_export),
            night = night,
            enabled = state.canExport,
            tag = "download-export",
            icon = KxIconKind.COPY,
            onClick = onExport,
        )
        Text(
            stringResource(R.string.download_export_hint),
            fontSize = 10.sp,
            lineHeight = 14.sp,
            color = kxMuted(night),
            modifier = Modifier.padding(top = 4.dp).testTag("download-export-hint"),
        )
        Spacer(Modifier.height(8.dp))
        DownloadGlassButton(
            label = stringResource(R.string.download_rename),
            night = night,
            enabled = !state.actionsBlocked && state.selectedCandidates.size == 1,
            tag = "download-rename",
            icon = KxIconKind.FILE,
            onClick = onRename,
        )
    }
}

@Composable
private fun DownloadSelectionBar(
    night: Boolean,
    selectedCount: Int,
    selectedBytes: Long,
    canRemove: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KxGlassPanel(
        night,
        modifier = modifier.testTag("download-selection-bar"),
        radius = 26.dp,
        padding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.download_ref_process_note, selectedCount, formatBytes(selectedBytes)),
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = kxInk(night),
                    modifier = Modifier.testTag("download-process-note"),
                )
                Text(
                    stringResource(
                        if (selectedCount == 0) R.string.download_ref_process_none
                        else R.string.download_ref_confirmed
                    ),
                    fontSize = 9.sp,
                    lineHeight = 12.sp,
                    color = kxMuted(night),
                )
            }
            Spacer(Modifier.width(10.dp))
            KxGradientAction(
                label = stringResource(R.string.download_ref_process),
                night = night,
                enabled = canRemove,
                tag = "download-process",
                icon = KxIconKind.CHECK,
                onClick = onRemove,
                modifier = Modifier.weight(1.15f),
            )
        }
    }
}

@Composable
private fun DownloadGlassButton(
    label: String,
    night: Boolean,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: KxIconKind? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    val ink = if (enabled) kxInk(night) else kxMuted(night).copy(alpha = .55f)
    Row(
        modifier.fillMaxWidth().heightIn(min = 44.dp)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    if (night) {
                        listOf(Color.White.copy(alpha = .09f), kxViolet(night).copy(alpha = .10f))
                    } else {
                        listOf(Color.White.copy(alpha = .60f), kxViolet(night).copy(alpha = .06f))
                    },
                ),
            )
            .border(1.dp, Color.White.copy(alpha = if (night) .24f else .76f), shape)
            .then(
                if (enabled) {
                    Modifier.clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .semantics { if (!enabled) disabled() }
            .padding(horizontal = 13.dp, vertical = 9.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            KxLineIcon(icon, ink, Modifier.size(16.dp))
            Spacer(Modifier.width(7.dp))
        }
        Text(
            label,
            fontSize = 12.5.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = ink,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DownloadPillAction(
    label: String,
    night: Boolean,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.heightIn(min = 38.dp)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    if (night) {
                        listOf(Color.White.copy(alpha = .08f), kxMuted(night).copy(alpha = .08f))
                    } else {
                        listOf(Color.White.copy(alpha = .52f), kxMuted(night).copy(alpha = .06f))
                    },
                ),
            )
            .border(1.dp, Color.White.copy(alpha = if (night) .20f else .68f), shape)
            .then(
                if (enabled) {
                    Modifier.clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .semantics { if (!enabled) disabled() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontSize = 10.5.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) kxInk(night) else kxMuted(night).copy(alpha = .55f),
            maxLines = 1,
        )
    }
}

// ---------------------------------------------------------------------------
// Receipt and honesty
// ---------------------------------------------------------------------------

@Composable
private fun DownloadReceiptCard(night: Boolean, receipt: DownloadReceipt) {
    KxGlassPanel(
        night,
        modifier = Modifier.testTag("download-receipt-card"),
        radius = 20.dp,
        padding = PaddingValues(13.dp),
    ) {
        Text(
            stringResource(R.string.download_receipt_title),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = kxInk(night),
            modifier = Modifier.testTag("download-receipt"),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            when (receipt.action) {
                DownloadAction.REMOVE -> stringResource(
                    R.string.download_receipt_removed,
                    receipt.succeeded,
                    formatBytes(receipt.bytesRemoved),
                    receipt.failed,
                )
                DownloadAction.EXPORT -> stringResource(
                    R.string.download_receipt_exported,
                    receipt.succeeded,
                    formatBytes(receipt.bytesCopied),
                    receipt.failed,
                )
                DownloadAction.RENAME -> stringResource(
                    R.string.download_receipt_renamed,
                    receipt.succeeded,
                    receipt.failed,
                )
            },
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = kxInk(night),
            modifier = Modifier.testTag("download-receipt-summary"),
        )
        Spacer(Modifier.height(4.dp))
        receipt.results.forEach { result ->
            Text(
                stringResource(R.string.download_receipt_line, result.displayName, result.message),
                fontSize = 10.5.sp,
                lineHeight = 15.sp,
                color = kxMuted(night),
            )
        }
    }
}

@Composable
private fun DownloadHonestyPanel(night: Boolean) {
    KxGlassPanel(night, radius = 20.dp, padding = PaddingValues(13.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KxGlassIconTile(night, KxIconKind.SHIELD_CHECK, kxGreen(night), size = 36.dp)
            Spacer(Modifier.width(9.dp))
            Text(
                stringResource(R.string.download_ref_panel_title),
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Bold,
                color = kxInk(night),
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            stringResource(R.string.download_no_repair),
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = kxMuted(night),
            modifier = Modifier.testTag("download-no-repair"),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            stringResource(R.string.download_privacy),
            fontSize = 10.sp,
            lineHeight = 15.sp,
            color = kxMuted(night),
            modifier = Modifier.testTag("download-privacy"),
        )
    }
}

@Composable
private fun InlineNotice(
    text: String,
    night: Boolean,
    tag: String,
    accent: Color = kxCyan(night),
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = .10f))
            .border(1.dp, accent.copy(alpha = .42f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KxLineIcon(KxIconKind.CLOCK, accent, Modifier.size(15.dp))
        Spacer(Modifier.width(7.dp))
        Text(
            text,
            fontSize = 10.5.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = accent,
            modifier = Modifier.weight(1f),
        )
    }
}

// ---------------------------------------------------------------------------
// Localisation helpers
// ---------------------------------------------------------------------------

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

private fun healthAccent(health: DownloadHealth, night: Boolean): Color = when (health) {
    DownloadHealth.INTACT -> kxCyan(night)
    DownloadHealth.TRUNCATED -> Color(0xFFFFC98A)
    DownloadHealth.CORRUPTED -> kxRose(night)
    DownloadHealth.ZERO_BYTE -> kxRose(night)
    DownloadHealth.UNSUPPORTED -> kxViolet(night)
    DownloadHealth.UNREADABLE -> Color(0xFFFFC98A)
}
