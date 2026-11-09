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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.knoux.kxprivatevpn.designsystem.KxTitleBlock
import com.knoux.kxprivatevpn.designsystem.RepairGlyph
import com.knoux.kxprivatevpn.designsystem.kxCyan
import com.knoux.kxprivatevpn.designsystem.kxGreen
import com.knoux.kxprivatevpn.designsystem.kxInk
import com.knoux.kxprivatevpn.designsystem.kxMuted
import com.knoux.kxprivatevpn.designsystem.kxRose
import com.knoux.kxprivatevpn.designsystem.kxViolet
import com.knoux.kxprivatevpn.launch.rememberReducedMotion
import com.knoux.kxprivatevpn.platform.repair.DuplicateCleanupReceipt
import com.knoux.kxprivatevpn.platform.repair.DuplicateEntry
import com.knoux.kxprivatevpn.platform.repair.DuplicateGroup
import com.knoux.kxprivatevpn.platform.repair.DuplicateScan
import com.knoux.kxprivatevpn.platform.repair.RepairServiceId
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

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
 * Duplicate DNA screen, built from the owner-approved references 06 (hero) and 11 (results).
 * Only exact SHA-256 matches are shown; near-duplicates are never offered. Cleanup requires an
 * explicit selection, a re-verification of every selected file, and a confirmation dialog.
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
    val ink = kxInk(night)
    val scan = state.scan
    val selectedCount = state.selectedDocumentIds.size
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<DnaCategory?>(null) }

    val selectedBytes = remember(scan, state.selectedDocumentIds) {
        scan?.groups?.sumOf { group ->
            group.entries.filter { it.documentId in state.selectedDocumentIds }.sumOf { it.bytes }
        } ?: 0L
    }
    val filteredGroups = remember(scan, query, category) {
        scan?.let { filterGroups(it, query, category) }.orEmpty()
    }
    val showBar = scan != null && scan.groups.isNotEmpty()

    KxReferenceTheme(night) {
        Box(Modifier.fillMaxSize()) {
            KxReferenceAtmosphere(night)
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                // The selection bar owns real layout space at the bottom. It must never be an
                // overlay: Compose would scroll a row just under it and a real tap would land
                // on the bar instead of the row.
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp)
                        .padding(bottom = 20.dp)
                        .testTag("route-duplicate"),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    KxReferenceHeader(
                        night = night,
                        actionLabel = stringResource(R.string.back),
                        tag = "dna-ref-back",
                        icon = KxIconKind.BACK,
                        onClick = onBack,
                    )
                    KxTitleBlock(
                        title = stringResource(R.string.repair_service_duplicate),
                        subtitle = stringResource(
                            if (scan == null) R.string.dna_ref_subtitle else R.string.dna_ref_results_subtitle
                        ),
                        night = night,
                    )

                    if (scan == null) {
                        DnaHero(night, scanning = state.scanning)
                        IdleCategoryRow(night, category) { category = it }
                        DnaFolderCard(
                            night, state, ink, onChooseFolder, onScan, onCancelScan,
                        )
                        if (state.treeUri != null && !state.scanning) {
                            KxGradientAction(
                                label = stringResource(R.string.dna_ref_start_scan),
                                night = night,
                                enabled = true,
                                tag = "dna-scan",
                                icon = KxIconKind.SEARCH,
                                onClick = onScan,
                            )
                        }
                        IdleScanSummary(night, state, ink)
                    } else {
                        DnaFolderCard(
                            night, state, ink, onChooseFolder, onScan, onCancelScan,
                        )
                        DnaSummaryCard(night, scan, state, ink)
                        DnaFiltersCard(
                            night, ink, query, category,
                            onQuery = { query = it },
                            onCategory = { category = it },
                            visibleGroups = filteredGroups.size,
                        )
                        if (scan.groups.isEmpty()) {
                            KxGlassPanel(night, radius = 20.dp, padding = PaddingValues(14.dp)) {
                                Text(
                                    stringResource(
                                        if (scan.cancelled) R.string.dna_cancelled else R.string.dna_empty
                                    ),
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = ink.copy(alpha = .84f),
                                    modifier = Modifier.testTag("dna-empty"),
                                )
                            }
                        } else if (filteredGroups.isEmpty()) {
                            KxGlassPanel(night, radius = 20.dp, padding = PaddingValues(14.dp)) {
                                Text(
                                    stringResource(R.string.dna_ref_no_matches),
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = ink.copy(alpha = .84f),
                                    modifier = Modifier.testTag("dna-no-match"),
                                )
                            }
                        } else {
                            DnaGroupList(
                                night = night,
                                groups = filteredGroups,
                                state = state,
                                ink = ink,
                                onToggleEntry = onToggleEntry,
                                onSelectAll = onSelectAll,
                                onSelectGroup = onSelectGroup,
                                onClearSelection = onClearSelection,
                            )
                        }
                        DnaReceiptCard(night, state, ink)
                    }

                    DnaPrivacyPanel(night, ink)
                    Spacer(Modifier.height(4.dp))
                }

                if (showBar) {
                    SelectionBar(
                        night = night,
                        state = state,
                        selectedCount = selectedCount,
                        selectedBytes = selectedBytes,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        onRequestCleanup = onRequestCleanup,
                        onChooseFolder = onChooseFolder,
                        onCancelCleanup = onCancelCleanup,
                    )
                }
            }
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

// ---------------------------------------------------------------------------
// Hero
// ---------------------------------------------------------------------------

@Composable
private fun DnaHero(night: Boolean, scanning: Boolean) {
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
    Box(
        Modifier.fillMaxWidth().height(218.dp).testTag("dna-hero"),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height * .48f)
            val radius = min(size.width, size.height) * .34f
            drawCircle(
                Brush.radialGradient(
                    listOf(
                        Color(0xFF64EEFF).copy(alpha = .26f),
                        Color(0xFFAD71FF).copy(alpha = .16f),
                        Color.Transparent,
                    ),
                    c,
                    radius * 1.28f,
                ),
                radius * 1.28f,
                c,
            )
            repeat(3) { index ->
                drawCircle(
                    Color(0xFF64EEFF).copy(alpha = .22f - index * .05f),
                    radius * (.84f + index * .20f),
                    c,
                    style = Stroke(1.dp.toPx()),
                )
            }
            repeat(7) { index ->
                val angle = Math.toRadians((index * 51.4 + 14).toDouble())
                val orbit = radius * 1.18f
                drawCircle(
                    Color(0xFFAD71FF).copy(alpha = .72f),
                    1.7.dp.toPx(),
                    Offset(
                        c.x + cos(angle).toFloat() * orbit,
                        c.y + sin(angle).toFloat() * orbit * .64f,
                    ),
                )
            }
            drawArc(
                Brush.sweepGradient(
                    listOf(
                        Color.Transparent,
                        Color(0xFF64EEFF),
                        Color(0xFFAD71FF),
                        Color.Transparent,
                    ),
                    c,
                ),
                startAngle = sweep.value,
                sweepAngle = 112f,
                useCenter = false,
                topLeft = Offset(c.x - radius, c.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(148.dp), contentAlignment = Alignment.Center) {
                KxOrbBadge(148.dp, Modifier, night)
                RepairGlyph(RepairServiceId.DUPLICATE, Modifier.size(70.dp))
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(kxCyan(night).copy(alpha = .10f))
                    .border(1.dp, kxCyan(night).copy(alpha = .42f), RoundedCornerShape(50))
                    .padding(horizontal = 15.dp, vertical = 7.dp)
                    .testTag("dna-hero-hint"),
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
                        if (scanning) R.string.dna_ref_scanning_hint
                        else R.string.dna_ref_scan_hint
                    ),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = kxCyan(night),
                )
            }
        }
    }
}

@Composable
private fun IdleCategoryRow(
    night: Boolean,
    selected: DnaCategory?,
    onSelect: (DnaCategory?) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IDLE_CATEGORIES.forEach { item ->
            DnaCategoryChip(
                night = night,
                category = item,
                selected = selected == item,
                onClick = { onSelect(if (selected == item) null else item) },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Folder and scan setup
// ---------------------------------------------------------------------------

@Composable
private fun DnaFolderCard(
    night: Boolean,
    state: DuplicateDnaUiState,
    ink: Color,
    onChooseFolder: () -> Unit,
    onScan: () -> Unit,
    onCancelScan: () -> Unit,
) {
    KxGlassPanel(
        night,
        modifier = Modifier.testTag("dna-folder-card"),
        radius = 22.dp,
        padding = PaddingValues(13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KxGlassIconTile(night, KxIconKind.DATABASE, kxViolet(night), size = 40.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.dna_ref_folder),
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = kxMuted(night),
                )
                Text(
                    state.treeName ?: stringResource(R.string.dna_ref_no_folder),
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("dna-tree-name"),
                )
                if (state.treeUri != null) {
                    // Keep the access state under the folder name. A trailing status line can
                    // claim the whole row width and squeeze the name into a one-letter column,
                    // especially with the longer read-only sentence or large Arabic type.
                    Spacer(Modifier.height(3.dp))
                    Text(
                        stringResource(if (state.writable) R.string.ghost_writable else R.string.dna_ref_no_write),
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (state.writable) kxGreen(night) else kxRose(night),
                        modifier = Modifier.testTag(if (state.writable) "dna-writable" else "dna-read-only"),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        if (state.treeUri == null) {
            Text(
                stringResource(R.string.dna_tree_none),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = ink.copy(alpha = .82f),
            )
            Spacer(Modifier.height(8.dp))
            KxGradientAction(
                label = stringResource(R.string.dna_ref_choose_folder),
                night = night,
                enabled = true,
                tag = "dna-choose-folder",
                icon = KxIconKind.DATABASE,
                onClick = onChooseFolder,
            )
        } else {
            if (state.scanning) {
                Text(
                    stringResource(R.string.dna_scanning, state.scanned, state.hashed),
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    color = ink.copy(alpha = .82f),
                    modifier = Modifier.testTag("dna-scan-progress"),
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = onCancelScan,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("dna-scan-cancel"),
                ) { Text(stringResource(R.string.ghost_cancel)) }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.scan != null) {
                        KxGradientAction(
                            label = stringResource(R.string.dna_rescan),
                            night = night,
                            enabled = true,
                            tag = "dna-rescan",
                            icon = KxIconKind.SEARCH,
                            modifier = Modifier.weight(1f),
                            onClick = onScan,
                        )
                    }
                    Row(
                        Modifier
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, kxCyan(night).copy(alpha = .42f), RoundedCornerShape(50))
                            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.dna_ref_change_folder), onClick = onChooseFolder)
                            .padding(horizontal = 13.dp, vertical = 9.dp)
                            .testTag("dna-choose-again"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        KxLineIcon(KxIconKind.DATABASE, kxCyan(night), Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.dna_ref_change_folder),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ink,
                        )
                    }
                }
            }
            if (state.scanFailed) {
                Text(
                    stringResource(R.string.dna_scan_failed),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = kxRose(night),
                    modifier = Modifier.testTag("dna-scan-failed"),
                )
            }
            if (state.scanCancelled) {
                Text(
                    stringResource(R.string.dna_cancelled),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = ink.copy(alpha = .78f),
                    modifier = Modifier.testTag("dna-cancelled"),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.dna_ref_folder_scope),
            fontSize = 10.sp,
            lineHeight = 15.sp,
            color = kxMuted(night),
        )
    }
}

@Composable
private fun IdleScanSummary(night: Boolean, state: DuplicateDnaUiState, ink: Color) {
    KxGlassPanel(night, radius = 22.dp, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KxLineIcon(KxIconKind.CLOCK, kxViolet(night), Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text(
                stringResource(R.string.dna_ref_last_scan),
                fontSize = 16.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Bold,
                color = ink,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.dna_ref_no_scan),
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            color = kxMuted(night),
            modifier = Modifier.testTag("dna-no-scan"),
        )
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(kxCyan(night).copy(alpha = .08f))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KxLineIcon(KxIconKind.DATABASE, kxCyan(night), Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                state.treeName ?: stringResource(R.string.dna_ref_no_folder),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = ink.copy(alpha = .90f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Results
// ---------------------------------------------------------------------------

@Composable
private fun DnaSummaryCard(
    night: Boolean,
    scan: DuplicateScan,
    state: DuplicateDnaUiState,
    ink: Color,
) {
    val context = LocalContext.current
    KxGlassPanel(night, radius = 22.dp, padding = PaddingValues(13.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KxLineIcon(KxIconKind.LAYERS, kxCyan(night), Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text(
                stringResource(R.string.dna_results_title),
                fontSize = 16.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Bold,
                color = ink,
                modifier = Modifier.weight(1f),
            )
            if (scan.truncated) {
                Text(
                    stringResource(R.string.ghost_limited_badge),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = kxRose(night),
                    modifier = Modifier.testTag("dna-partial"),
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            stringResource(R.string.dna_scan_counts, scan.scannedFiles, scan.hashedFiles),
            fontSize = 10.5.sp,
            lineHeight = 15.sp,
            color = kxMuted(night),
            modifier = Modifier.testTag("dna-scan-counts"),
        )
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DnaSummaryTile(
                label = stringResource(R.string.dna_ref_duplicates),
                value = duplicateFileCount(scan).toString(),
                tag = "dna-duplicate-count",
                accent = kxCyan(night),
                modifier = Modifier.weight(1f),
                night = night,
                ink = ink,
            )
            DnaSummaryTile(
                label = stringResource(R.string.dna_ref_reclaimable),
                value = Formatter.formatFileSize(context, scan.totalReclaimableBytes),
                tag = "dna-reclaimable",
                accent = kxViolet(night),
                modifier = Modifier.weight(1f),
                night = night,
                ink = ink,
            )
            DnaSummaryTile(
                label = stringResource(R.string.dna_ref_groups),
                value = scan.duplicateGroupCount.toString(),
                tag = "dna-group-count",
                accent = kxRose(night),
                modifier = Modifier.weight(1f),
                night = night,
                ink = ink,
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            formatDnaDate(context, scan.atMillis),
            fontSize = 9.5.sp,
            color = kxMuted(night).copy(alpha = .72f),
        )
    }
}

@Composable
private fun DnaSummaryTile(
    label: String,
    value: String,
    tag: String,
    accent: Color,
    modifier: Modifier,
    night: Boolean,
    ink: Color,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = if (night) .12f else .10f))
            .border(1.dp, accent.copy(alpha = .24f), RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 9.dp),
    ) {
        Text(
            value,
            fontSize = 17.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag(tag),
        )
        Text(
            label,
            fontSize = 9.5.sp,
            lineHeight = 13.sp,
            color = ink.copy(alpha = .78f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DnaFiltersCard(
    night: Boolean,
    ink: Color,
    query: String,
    category: DnaCategory?,
    onQuery: (String) -> Unit,
    onCategory: (DnaCategory?) -> Unit,
    visibleGroups: Int,
) {
    KxGlassPanel(
        night,
        modifier = Modifier.testTag("dna-filters"),
        radius = 22.dp,
        padding = PaddingValues(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            KxLineIcon(KxIconKind.SEARCH, ink, Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text(
                stringResource(R.string.dna_ref_filters),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                color = ink,
                modifier = Modifier.weight(1f),
            )
            Text(
                pluralStringResource(R.plurals.dna_ref_group_count, visibleGroups, visibleGroups),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = kxMuted(night),
                modifier = Modifier.testTag("dna-visible-groups"),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(ink.copy(alpha = if (night) .07f else .05f))
                .border(1.dp, ink.copy(alpha = if (night) .16f else .10f), RoundedCornerShape(50))
                .padding(horizontal = 13.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KxLineIcon(KxIconKind.SEARCH, kxMuted(night), Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = TextStyle(color = ink, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(kxCyan(night)),
                modifier = Modifier.weight(1f).heightIn(min = 46.dp).testTag("dna-search"),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                stringResource(R.string.dna_ref_search_hint),
                                fontSize = 12.5.sp,
                                color = kxMuted(night),
                            )
                        }
                        inner()
                    }
                },
            )
            if (query.isNotEmpty()) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable(role = Role.Button, onClickLabel = stringResource(R.string.dna_ref_search_clear)) { onQuery("") }
                        .testTag("dna-search-clear"),
                    contentAlignment = Alignment.Center,
                ) {
                    KxLineIcon(KxIconKind.CHECK, kxRose(night), Modifier.size(15.dp))
                }
            }
        }
        Spacer(Modifier.height(9.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            DnaCategoryChip(
                night = night,
                label = stringResource(R.string.dna_ref_all),
                icon = KxIconKind.LAYERS,
                selected = category == null,
                tag = "dna-category-all",
                onClick = { onCategory(null) },
            )
            RESULT_CATEGORIES.forEach { item ->
                DnaCategoryChip(
                    night = night,
                    category = item,
                    selected = category == item,
                    onClick = { onCategory(if (category == item) null else item) },
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            stringResource(R.string.dna_ref_search_scope),
            fontSize = 10.sp,
            lineHeight = 15.sp,
            color = kxMuted(night),
        )
    }
}

@Composable
private fun DnaCategoryChip(
    night: Boolean,
    category: DnaCategory,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DnaCategoryChip(
        night = night,
        label = stringResource(category.label),
        icon = category.icon,
        selected = selected,
        tag = "dna-category-${category.key}",
        onClick = onClick,
    )
}

@Composable
private fun DnaCategoryChip(
    night: Boolean,
    label: String,
    icon: KxIconKind,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    val tone = if (selected) kxCyan(night) else kxInk(night)
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) {
                    Brush.horizontalGradient(
                        listOf(kxCyan(night).copy(alpha = if (night) .22f else .16f), kxViolet(night).copy(alpha = if (night) .18f else .12f))
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = if (night) .07f else .62f),
                            Color.White.copy(alpha = if (night) .04f else .42f),
                        )
                    )
                }
            )
            .border(1.dp, if (selected) kxCyan(night).copy(alpha = .58f) else kxInk(night).copy(alpha = if (night) .14f else .10f), RoundedCornerShape(50))
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KxLineIcon(icon, if (selected) tone else kxMuted(night), Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = tone,
            maxLines = 1,
        )
    }
}

@Composable
private fun DnaGroupList(
    night: Boolean,
    groups: List<DuplicateGroup>,
    state: DuplicateDnaUiState,
    ink: Color,
    onToggleEntry: (DuplicateEntry) -> Unit,
    onSelectAll: () -> Unit,
    onSelectGroup: (DuplicateGroup) -> Unit,
    onClearSelection: () -> Unit,
) {
    KxGlassPanel(night, radius = 22.dp, padding = PaddingValues(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            KxLineIcon(KxIconKind.LAYERS, kxCyan(night), Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text(
                stringResource(R.string.dna_review_title),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                color = ink,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.dna_selected, state.selectedDocumentIds.size),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = kxCyan(night),
                modifier = Modifier.testTag("dna-selected"),
            )
        }
        Spacer(Modifier.height(2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(
                onClick = onSelectAll,
                enabled = !state.scanning && !state.cleaning,
                modifier = Modifier.heightIn(min = 48.dp).testTag("dna-select-all"),
            ) { Text(stringResource(R.string.ghost_select_all), fontSize = 11.sp) }
            TextButton(
                onClick = onClearSelection,
                enabled = !state.scanning && !state.cleaning,
                modifier = Modifier.heightIn(min = 48.dp).testTag("dna-clear"),
            ) { Text(stringResource(R.string.ghost_clear_selection), fontSize = 11.sp) }
        }
        val displayed = groups.take(DuplicateDnaViewModel.MAX_DISPLAYED_GROUPS)
        displayed.forEachIndexed { index, group ->
            // Filtering must not renumber a group: the number is the group's position in the
            // measured scan, so test tags and spoken references stay stable across filters.
            val number = (state.scan?.groups?.indexOf(group) ?: index) + 1
            DnaGroupCard(
                number = number,
                group = group,
                state = state,
                night = night,
                ink = ink,
                onToggleEntry = onToggleEntry,
                onSelectGroup = onSelectGroup,
            )
            if (index != displayed.lastIndex) Spacer(Modifier.height(9.dp))
        }
        if (groups.size > DuplicateDnaViewModel.MAX_DISPLAYED_GROUPS) {
            Text(
                stringResource(R.string.dna_display_limit, DuplicateDnaViewModel.MAX_DISPLAYED_GROUPS),
                fontSize = 10.sp,
                lineHeight = 15.sp,
                color = kxMuted(night),
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
    ink: Color,
    onToggleEntry: (DuplicateEntry) -> Unit,
    onSelectGroup: (DuplicateGroup) -> Unit,
) {
    val context = LocalContext.current
    val category = group.category()
    val accent = when (category) {
        DnaCategory.IMAGE -> kxCyan(night)
        DnaCategory.VIDEO -> kxRose(night)
        DnaCategory.DOCUMENT -> kxViolet(night)
        DnaCategory.OTHER -> kxGreen(night)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(accent.copy(alpha = if (night) .07f else .055f))
            .border(1.dp, accent.copy(alpha = .22f), RoundedCornerShape(18.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KxGlassIconTile(night, category.icon, accent, size = 36.dp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(
                        R.string.dna_ref_group_title,
                        stringResource(category.label),
                        pluralStringResource(R.plurals.dna_ref_copies, group.entries.size, group.entries.size),
                    ),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("dna-group-$number"),
                )
                Text(
                    stringResource(
                        R.string.dna_ref_group_meta,
                        Formatter.formatFileSize(context, group.bytes),
                        Formatter.formatFileSize(context, group.reclaimableBytes),
                    ),
                    fontSize = 9.5.sp,
                    lineHeight = 14.sp,
                    color = kxMuted(night),
                    modifier = Modifier.testTag("dna-group-reclaimable-$number"),
                )
            }
            TextButton(
                onClick = { onSelectGroup(group) },
                enabled = !state.scanning && !state.cleaning,
                modifier = Modifier.heightIn(min = 44.dp).testTag("dna-select-group-$number"),
            ) {
                Text(
                    stringResource(R.string.dna_ref_select_copies),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Text(
            stringResource(R.string.dna_sha_prefix, group.sha256.take(16)),
            fontSize = 8.5.sp,
            lineHeight = 12.sp,
            color = kxMuted(night).copy(alpha = .72f),
            modifier = Modifier.testTag("dna-sha-$number"),
        )
        group.entries.forEach { entry ->
            DnaEntryRow(entry, category, state, night, ink, accent, onToggleEntry)
        }
    }
}

@Composable
private fun DnaEntryRow(
    entry: DuplicateEntry,
    category: DnaCategory,
    state: DuplicateDnaUiState,
    night: Boolean,
    ink: Color,
    accent: Color,
    onToggleEntry: (DuplicateEntry) -> Unit,
) {
    val context = LocalContext.current
    val checked = entry.documentId in state.selectedDocumentIds
    val selectable = !entry.keep
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(
                if (selectable) Modifier.toggleable(
                    value = checked,
                    enabled = !state.scanning && !state.cleaning,
                    role = Role.Checkbox,
                    onValueChange = { onToggleEntry(entry) },
                ) else Modifier
            )
            .padding(vertical = 3.dp)
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
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                KxLineIcon(KxIconKind.CHECK, kxGreen(night), Modifier.size(17.dp))
            }
        }
        Spacer(Modifier.width(5.dp))
        KxGlassIconTile(night, category.icon, accent, size = 34.dp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                fileNameOf(entry.relativePath),
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                fontWeight = if (entry.keep) FontWeight.Bold else FontWeight.SemiBold,
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                entry.relativePath,
                fontSize = 9.5.sp,
                lineHeight = 13.sp,
                color = kxMuted(night),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(
                    R.string.dna_ref_entry_meta,
                    Formatter.formatFileSize(context, entry.bytes),
                    formatDnaDate(context, entry.lastModified),
                ),
                fontSize = 9.sp,
                lineHeight = 13.sp,
                color = kxMuted(night).copy(alpha = .84f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (entry.keep) {
            Text(
                stringResource(R.string.dna_ref_keep),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = kxGreen(night),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(kxGreen(night).copy(alpha = .12f))
                    .border(1.dp, kxGreen(night).copy(alpha = .38f), RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("dna-keep-${entry.documentId}"),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Cleanup and receipt
// ---------------------------------------------------------------------------

@Composable
private fun SelectionBar(
    night: Boolean,
    state: DuplicateDnaUiState,
    selectedCount: Int,
    selectedBytes: Long,
    modifier: Modifier,
    onRequestCleanup: () -> Unit,
    onChooseFolder: () -> Unit,
    onCancelCleanup: () -> Unit,
) {
    val context = LocalContext.current
    val ink = kxInk(night)
    KxGlassPanel(night, modifier = modifier, radius = 24.dp, padding = PaddingValues(11.dp)) {
        when {
            !state.writable -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KxLineIcon(KxIconKind.LOCK, kxRose(night), Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        stringResource(R.string.dna_ref_read_only),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = ink.copy(alpha = .86f),
                        modifier = Modifier.weight(1f).testTag("dna-cleanup-read-only"),
                    )
                }
                Spacer(Modifier.height(6.dp))
                KxGradientAction(
                    label = stringResource(R.string.dna_ref_regrant),
                    night = night,
                    enabled = true,
                    tag = "dna-regrant",
                    icon = KxIconKind.DATABASE,
                    onClick = onChooseFolder,
                )
            }
            state.cleaning -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KxLineIcon(KxIconKind.CLOCK, kxGreen(night), Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        stringResource(R.string.dna_cleaning),
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ink,
                        modifier = Modifier.weight(1f).testTag("dna-cleaning"),
                    )
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                KxGradientAction(
                    label = stringResource(R.string.ghost_cancel),
                    night = night,
                    enabled = true,
                    tag = "dna-cleanup-cancel",
                    icon = KxIconKind.BACK,
                    onClick = onCancelCleanup,
                )
            }
            else -> {
                KxGradientAction(
                    label = stringResource(R.string.dna_ref_delete_selected, selectedCount),
                    night = night,
                    enabled = selectedCount > 0,
                    tag = "dna-cleanup",
                    icon = KxIconKind.BROOM,
                    onClick = onRequestCleanup,
                )
                if (selectedCount > 0) {
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.dna_selected, selectedCount),
                            fontSize = 10.5.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ink.copy(alpha = .88f),
                            modifier = Modifier.weight(1f).testTag("dna-bar-selected"),
                        )
                        Text(
                            stringResource(
                                R.string.dna_ref_selected_bytes,
                                Formatter.formatFileSize(context, selectedBytes),
                            ),
                            fontSize = 10.5.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = kxCyan(night),
                            modifier = Modifier.testTag("dna-bar-bytes"),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DnaReceiptCard(night: Boolean, state: DuplicateDnaUiState, ink: Color) {
    val context = LocalContext.current
    val receipt = state.receipt ?: return
    KxGlassPanel(night, radius = 22.dp, padding = PaddingValues(13.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KxLineIcon(KxIconKind.CHECK, kxGreen(night), Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text(
                stringResource(R.string.dna_receipt_title),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                color = ink,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(
                R.string.dna_receipt,
                receipt.deleted, receipt.skipped.size, receipt.failed.size,
            ),
            fontSize = 14.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.SemiBold,
            color = ink,
            modifier = Modifier.testTag("dna-receipt"),
        )
        Text(
            stringResource(
                R.string.dna_receipt_reclaimed,
                Formatter.formatFileSize(context, receipt.bytesReclaimed),
            ),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = kxGreen(night),
            modifier = Modifier.testTag("dna-receipt-reclaimed"),
        )
        Text(
            stringResource(R.string.ghost_receipt_verified, receipt.verifiedAbsent),
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = kxGreen(night),
        )
        if (!receipt.complete) {
            Text(
                stringResource(R.string.dna_cleanup_partial),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = kxRose(night),
                modifier = Modifier.testTag("dna-receipt-partial"),
            )
        }
        receipt.failed.take(5).forEach {
            Text(it, fontSize = 10.5.sp, color = kxRose(night))
        }
        receipt.skipped.take(5).forEach {
            Text(
                stringResource(R.string.ghost_skipped, it),
                fontSize = 10.5.sp,
                color = kxMuted(night),
            )
        }
        Text(
            stringResource(R.string.ghost_journal_note),
            fontSize = 10.sp,
            lineHeight = 15.sp,
            color = kxMuted(night),
        )
        if (state.journalFailed) {
            Text(
                stringResource(R.string.ghost_journal_failed),
                fontSize = 10.sp,
                lineHeight = 15.sp,
                color = kxRose(night),
                modifier = Modifier.testTag("dna-journal-failed"),
            )
        }
        if (state.cleanupFailed) {
            Text(
                stringResource(R.string.dna_cleanup_failed),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = kxRose(night),
                modifier = Modifier.testTag("dna-cleanup-failed"),
            )
        }
    }
}

@Composable
private fun DnaPrivacyPanel(night: Boolean, ink: Color) {
    KxGlassPanel(night, radius = 18.dp, padding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            KxLineIcon(KxIconKind.LOCK, kxGreen(night), Modifier.size(15.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.dna_privacy),
                fontSize = 9.5.sp,
                lineHeight = 14.sp,
                color = ink.copy(alpha = .74f),
                modifier = Modifier.testTag("dna-privacy"),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Categories and helpers
// ---------------------------------------------------------------------------

private enum class DnaCategory(
    val key: String,
    val label: Int,
    val icon: KxIconKind,
) {
    IMAGE("image", R.string.dna_ref_images, KxIconKind.SPARKLE),
    VIDEO("video", R.string.dna_ref_videos, KxIconKind.CLAPPER),
    DOCUMENT("document", R.string.dna_ref_documents, KxIconKind.FILE),
    OTHER("other", R.string.dna_ref_other, KxIconKind.GRID);

    fun matches(path: String): Boolean = when (this) {
        IMAGE -> extensionOf(path) in IMAGE_EXTENSIONS
        VIDEO -> extensionOf(path) in VIDEO_EXTENSIONS
        DOCUMENT -> extensionOf(path) in DOCUMENT_EXTENSIONS
        OTHER -> extensionOf(path) !in IMAGE_EXTENSIONS &&
            extensionOf(path) !in VIDEO_EXTENSIONS &&
            extensionOf(path) !in DOCUMENT_EXTENSIONS
    }

    companion object {
        fun of(path: String): DnaCategory =
            entries.firstOrNull { it != OTHER && it.matches(path) } ?: OTHER
    }
}

private val IDLE_CATEGORIES = listOf(DnaCategory.IMAGE, DnaCategory.VIDEO, DnaCategory.OTHER)
private val RESULT_CATEGORIES = listOf(
    DnaCategory.IMAGE, DnaCategory.VIDEO, DnaCategory.DOCUMENT, DnaCategory.OTHER,
)

private val IMAGE_EXTENSIONS = setOf(
    "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "avif", "tif", "tiff",
)
private val VIDEO_EXTENSIONS = setOf(
    "mp4", "mkv", "mov", "avi", "webm", "3gp", "m4v", "mpeg", "mpg", "ts",
)
private val DOCUMENT_EXTENSIONS = setOf(
    "pdf", "doc", "docx", "txt", "md", "rtf", "odt", "xls", "xlsx", "csv",
    "ppt", "pptx", "epub",
)

private fun DuplicateGroup.category(): DnaCategory =
    entries.groupingBy { DnaCategory.of(it.relativePath) }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
        ?: DnaCategory.OTHER

private fun filterGroups(
    scan: DuplicateScan,
    query: String,
    category: DnaCategory?,
): List<DuplicateGroup> {
    val trimmed = query.trim()
    return scan.groups.filter { group ->
        (category == null || group.category() == category) &&
            (trimmed.isEmpty() || group.entries.any {
                it.relativePath.contains(trimmed, ignoreCase = true)
            })
    }
}

private fun duplicateFileCount(scan: DuplicateScan): Int =
    scan.groups.sumOf { group -> group.entries.count { !it.keep } }

private fun fileNameOf(path: String): String = path.substringAfterLast('/')

private fun extensionOf(path: String): String =
    fileNameOf(path).substringAfterLast('.', "").lowercase(Locale.ROOT)

private fun formatDnaDate(context: android.content.Context, atMillis: Long): String {
    val locale = context.resources.configuration.locales.get(0) ?: Locale.getDefault()
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale)
        .format(Date(atMillis))
}
