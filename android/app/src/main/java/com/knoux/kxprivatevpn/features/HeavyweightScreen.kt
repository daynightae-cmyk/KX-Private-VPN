package com.knoux.kxprivatevpn.features

import android.content.Intent
import android.text.format.Formatter
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.designsystem.KxChevron
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
import com.knoux.kxprivatevpn.designsystem.kxViolet
import com.knoux.kxprivatevpn.launch.rememberReducedMotion
import com.knoux.kxprivatevpn.platform.repair.HeavyweightScan
import com.knoux.kxprivatevpn.platform.repair.HeavyweightScanner
import com.knoux.kxprivatevpn.platform.repair.LargeFile
import com.knoux.kxprivatevpn.platform.repair.LargeFileCategory
import com.knoux.kxprivatevpn.platform.repair.RepairServiceId
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun HeavyweightRoute(night: Boolean, onBack: () -> Unit) {
    val model: HeavyweightViewModel = viewModel()
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val tree = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(model::attachTree)
    }
    HeavyweightScreen(
        night = night,
        state = state,
        onBack = onBack,
        onChooseFolder = { tree.launch(null) },
        onScan = model::scan,
        onCancelScan = model::cancelScan,
        onMinimumBytes = model::setMinimumBytes,
        onCategory = model::setCategory,
        onSelect = model::select,
        onOpen = { file ->
            model.focus(file)
            val uri = model.documentUri(file.documentId)
            val opened = uri != null && try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW)
                        .setDataAndType(uri, file.mime)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                )
                true
            } catch (_: Exception) {
                false
            }
            model.reportOpenResult(opened)
        },
    )
}

/**
 * Large Files ranks measured provider sizes inside the folder the user granted through Android's
 * system picker. It is read-only: there is no delete, rename or move action, and a file whose
 * size was not reported is excluded instead of being shown with an invented number.
 *
 * Visual references: 05-large-files-hero.png and 13-large-files-results.png. The setup row,
 * gradient rank action and compact measured-result panel follow the owner's approved composition.
 * Search and sort are local view state and never trigger another scan.
 */
@Composable
fun HeavyweightScreen(
    night: Boolean,
    state: HeavyweightUiState,
    onBack: () -> Unit,
    onChooseFolder: () -> Unit,
    onScan: () -> Unit,
    onCancelScan: () -> Unit,
    onMinimumBytes: (Long) -> Unit,
    onCategory: (LargeFileCategory?) -> Unit,
    onSelect: (LargeFile) -> Unit,
    onOpen: (LargeFile) -> Unit,
) {
    val ink = kxInk(night)
    val scan = state.scan
    val visible = state.visibleFiles
    var query by rememberSaveable { mutableStateOf("") }
    var sortMode by rememberSaveable { mutableStateOf(HeavySortMode.SIZE) }
    val shown = remember(visible, query, sortMode) {
        val needle = query.trim()
        sortFiles(
            visible.filter { file ->
                needle.isEmpty() ||
                    file.name.contains(needle, ignoreCase = true) ||
                    file.relativePath.contains(needle, ignoreCase = true)
            },
            sortMode,
        )
    }
    KxReferenceTheme(night) {
        Box(Modifier.fillMaxSize()) {
            KxReferenceAtmosphere(night)
            Column(
                Modifier.fillMaxSize().safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .testTag("route-heavyweight"),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                KxReferenceHeader(
                    night = night,
                    actionLabel = stringResource(R.string.back),
                    tag = "heavy-ref-back",
                    icon = KxIconKind.BACK,
                    onClick = onBack,
                )
                KxTitleBlock(
                    title = stringResource(R.string.repair_service_heavyweight),
                    subtitle = stringResource(R.string.heavy_subtitle),
                    night = night,
                )
                HeavyHero(
                    night = night,
                    scanning = state.scanning,
                    progress = state.progress,
                    compact = scan != null,
                )
                SetupRow(
                    night = night,
                    state = state,
                    onChooseFolder = onChooseFolder,
                    onMinimumBytes = onMinimumBytes,
                )
                ScanActionCard(
                    night = night,
                    state = state,
                    onScan = onScan,
                    onCancelScan = onCancelScan,
                )
                if (scan != null) {
                    SummaryCard(night, scan, ink)
                    FiltersCard(
                        night = night,
                        state = state,
                        visibleCount = shown.size,
                        query = query,
                        sortMode = sortMode,
                        onQuery = { query = it },
                        onSort = { sortMode = it },
                        onCategory = onCategory,
                    )
                    if (shown.isNotEmpty()) {
                        HeatmapCard(night, shown, ink, onSelect)
                        ResultList(night, state, shown, ink, onSelect, onOpen)
                    } else if (scan.files.isEmpty()) {
                        KxGlassPanel(night) {
                            Text(
                                stringResource(R.string.heavy_empty),
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = ink.copy(alpha = .84f),
                                modifier = Modifier.testTag("heavy-empty"),
                            )
                        }
                    } else {
                        KxGlassPanel(night) {
                            Text(
                                stringResource(R.string.heavy_no_match),
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = ink.copy(alpha = .84f),
                                modifier = Modifier.testTag("heavy-no-match"),
                            )
                        }
                    }
                    SelectedFileCard(night, state, ink, onOpen)
                }
                PrivacyPanel(night, ink)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private enum class HeavySortMode { SIZE, DATE, NAME }

/**
 * Local ordering only. [HeavySortMode.SIZE] keeps the provider-measured byte ranking; the other
 * modes are deterministic and tie-break on measured bytes, never on a guessed value.
 */
private fun sortFiles(files: List<LargeFile>, mode: HeavySortMode): List<LargeFile> = when (mode) {
    HeavySortMode.SIZE -> files.sortedWith(
        compareByDescending<LargeFile> { it.bytes }
            .thenBy { it.name.lowercase(Locale.ROOT) }
            .thenBy { it.documentId },
    )
    HeavySortMode.DATE -> files.sortedWith(
        compareByDescending<LargeFile> { it.lastModified }
            .thenByDescending { it.bytes }
            .thenBy { it.documentId },
    )
    HeavySortMode.NAME -> files.sortedWith(
        compareBy<LargeFile> { it.name.lowercase(Locale.ROOT) }
            .thenByDescending { it.bytes }
            .thenBy { it.documentId },
    )
}

@Composable
private fun HeavyHero(
    night: Boolean,
    scanning: Boolean,
    progress: Int,
    compact: Boolean,
) {
    val reduced = rememberReducedMotion()
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(scanning, reduced) {
        if (scanning && !reduced) {
            sweep.snapTo(0f)
            sweep.animateTo(360f, tween(1900, easing = LinearEasing))
        } else {
            sweep.snapTo(0f)
        }
    }
    val sceneHeight = if (compact) 172.dp else 220.dp
    val orbSize = if (compact) 116.dp else 150.dp
    val glyphSize = if (compact) 58.dp else 76.dp
    Box(
        Modifier.fillMaxWidth().height(sceneHeight).testTag("heavy-hero"),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height * .46f)
            val radius = min(size.width, size.height) * (if (compact) .30f else .34f)
            drawCircle(
                Brush.radialGradient(
                    listOf(
                        Color(0xFF64EEFF).copy(alpha = .28f),
                        Color(0xFFAD71FF).copy(alpha = .14f),
                        Color.Transparent,
                    ),
                    c,
                    radius * 1.22f,
                ),
                radius * 1.22f,
                c,
            )
            repeat(3) { index ->
                drawCircle(
                    Color(0xFF64EEFF).copy(alpha = .22f - index * .05f),
                    radius * (.86f + index * .20f),
                    c,
                    style = Stroke(1.dp.toPx()),
                )
            }
            repeat(6) { index ->
                val angle = Math.toRadians((index * 60 + 18).toDouble())
                val orbit = radius * 1.18f
                drawCircle(
                    Color(0xFFAD71FF).copy(alpha = .70f),
                    1.7.dp.toPx(),
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
                        Color(0xFF64EEFF),
                        Color(0xFFAD71FF),
                        Color.Transparent,
                    ),
                    c,
                ),
                startAngle = sweep.value,
                sweepAngle = 104f,
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
            Box(Modifier.size(orbSize), contentAlignment = Alignment.Center) {
                KxOrbBadge(orbSize, Modifier, night)
                RepairGlyph(RepairServiceId.HEAVYWEIGHT, Modifier.size(glyphSize))
            }
            if (!compact) {
                Spacer(Modifier.height(12.dp))
                HeroHintPill(
                    text = if (scanning) {
                        stringResource(R.string.heavy_scanning, progress)
                    } else {
                        stringResource(R.string.heavy_ref_scan_hint)
                    },
                    night = night,
                    scanning = scanning,
                )
            }
        }
    }
}

@Composable
private fun HeroHintPill(text: String, night: Boolean, scanning: Boolean) {
    val tone = if (scanning) kxGreen(night) else kxCyan(night)
    Row(
        Modifier.clip(RoundedCornerShape(50))
            .background(tone.copy(alpha = .10f))
            .border(1.dp, tone.copy(alpha = .42f), RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("heavy-hero-hint"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KxLineIcon(
            if (scanning) KxIconKind.CLOCK else KxIconKind.TARGET,
            tone,
            Modifier.size(15.dp),
        )
        Spacer(Modifier.width(7.dp))
        Text(text, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = tone)
    }
}

@Composable
private fun SetupRow(
    night: Boolean,
    state: HeavyweightUiState,
    onChooseFolder: () -> Unit,
    onMinimumBytes: (Long) -> Unit,
) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stack = maxWidth < 330.dp || fontScale > 1.2f
        if (stack) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FolderSetupCard(night, state, onChooseFolder, Modifier.fillMaxWidth())
                MinimumSetupCard(night, state, onMinimumBytes, Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FolderSetupCard(night, state, onChooseFolder, Modifier.weight(1.06f))
                MinimumSetupCard(night, state, onMinimumBytes, Modifier.weight(.94f))
            }
        }
    }
}

@Composable
private fun FolderSetupCard(
    night: Boolean,
    state: HeavyweightUiState,
    onChooseFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = kxInk(night)
    val actionLabel = stringResource(
        if (state.treeUri == null) R.string.heavy_choose_folder
        else R.string.heavy_choose_again,
    )
    KxGlassPanel(
        night,
        modifier = modifier.testTag("heavy-folder-card"),
        radius = 20.dp,
        padding = PaddingValues(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.heavy_ref_folder),
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp,
                    color = kxMuted(night),
                )
                if (state.treeName != null) {
                    Text(
                        state.treeName,
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(textDirection = TextDirection.Content),
                        modifier = Modifier.testTag("heavy-tree-name"),
                    )
                    if (state.scan != null) {
                        Text(
                            stringResource(R.string.heavy_ref_checked, state.scan.scannedFiles),
                            fontSize = 9.5.sp,
                            lineHeight = 13.sp,
                            color = kxMuted(night),
                            modifier = Modifier.testTag("heavy-tree-count"),
                        )
                    }
                } else {
                    Text(
                        stringResource(R.string.heavy_choose_folder),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ink,
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            SmallActionButton(
                icon = KxIconKind.FOLDER,
                label = actionLabel,
                tag = if (state.treeUri == null) "heavy-choose-folder" else "heavy-choose-again",
                night = night,
                accent = kxCyan(night),
                onClick = onChooseFolder,
            )
        }
        if (state.treeUri == null) {
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.heavy_tree_none),
                fontSize = 10.sp,
                lineHeight = 15.sp,
                color = kxMuted(night),
            )
        }
        Spacer(Modifier.height(6.dp))
        ReadOnlyPill(night)
    }
}

@Composable
private fun SmallActionButton(
    icon: KxIconKind,
    label: String,
    tag: String,
    night: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Box(
        Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = if (night) .16f else .10f))
            .border(1.dp, accent.copy(alpha = .38f), RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        KxLineIcon(icon, accent, Modifier.size(19.dp))
    }
}

@Composable
private fun ReadOnlyPill(night: Boolean) {
    val tone = kxGreen(night)
    Row(
        Modifier.clip(RoundedCornerShape(50))
            .background(tone.copy(alpha = .12f))
            .border(1.dp, tone.copy(alpha = .34f), RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 5.dp)
            .testTag("heavy-read-only"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KxLineIcon(KxIconKind.LOCK, tone, Modifier.size(12.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            stringResource(R.string.heavy_ref_read_only),
            fontSize = 9.5.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = tone,
        )
    }
}

@Composable
private fun MinimumSetupCard(
    night: Boolean,
    state: HeavyweightUiState,
    onMinimumBytes: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = kxInk(night)
    var expanded by remember { mutableStateOf(false) }
    KxGlassPanel(
        night,
        modifier = modifier.testTag("heavy-minimum-card"),
        radius = 20.dp,
        padding = PaddingValues(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KxGlassIconTile(night, KxIconKind.DATABASE, kxViolet(night), size = 38.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.heavy_ref_minimum),
                fontSize = 10.5.sp,
                lineHeight = 14.sp,
                color = kxMuted(night),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = if (night) .08f else .44f))
                    .border(1.dp, Color.White.copy(alpha = if (night) .26f else .62f),
                        RoundedCornerShape(50))
                    .clickable(role = Role.Button) { expanded = true }
                    .padding(horizontal = 12.dp)
                    .testTag("heavy-minimum-select"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    sizeFilterLabel(state.minimumBytes),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ink,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).testTag("heavy-minimum-value"),
                )
                KxChevron(
                    color = kxMuted(night),
                    modifier = Modifier.size(13.dp).graphicsLayer { rotationZ = 90f },
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                HeavyweightScanner.FILTER_STEPS.forEachIndexed { index, step ->
                    DropdownMenuItem(
                        text = { Text(sizeFilterLabel(step)) },
                        onClick = {
                            onMinimumBytes(step)
                            expanded = false
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                            .testTag(sizeFilterTag(step)),
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanActionCard(
    night: Boolean,
    state: HeavyweightUiState,
    onScan: () -> Unit,
    onCancelScan: () -> Unit,
) {
    val ink = kxInk(night)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.scanning) {
            OutlinedButton(
                onClick = onCancelScan,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    .testTag("heavy-scan-cancel"),
            ) { Text(stringResource(R.string.heavy_cancel), fontSize = 15.sp) }
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(
                stringResource(R.string.heavy_scanning, state.progress),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = kxMuted(night),
                modifier = Modifier.testTag("heavy-scan-progress"),
            )
        } else {
            KxGradientAction(
                label = stringResource(
                    if (state.scan == null) R.string.heavy_scan else R.string.heavy_rescan,
                ),
                night = night,
                enabled = state.treeUri != null,
                tag = if (state.scan == null) "heavy-scan" else "heavy-rescan",
                modifier = Modifier.fillMaxWidth(),
                icon = KxIconKind.TARGET,
                onClick = onScan,
            )
        }
        if (state.scanFailed && !state.scanning) {
            Text(
                stringResource(R.string.heavy_scan_failed),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = if (night) Color(0xFFFFB4B4) else Color(0xFF9B1C1C),
                modifier = Modifier.testTag("heavy-scan-failed"),
            )
        }
        Text(
            stringResource(R.string.heavy_ref_scan_scope),
            fontSize = 10.sp,
            lineHeight = 15.sp,
            color = kxMuted(night),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SummaryCard(night: Boolean, scan: HeavyweightScan, ink: Color) {
    val context = LocalContext.current
    KxGlassPanel(night, radius = 22.dp, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KxGlassIconTile(night, KxIconKind.BARS, kxCyan(night), size = 40.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.heavy_ref_latest),
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = ink,
                )
                Text(
                    stringResource(
                        R.string.heavy_scan_counts,
                        scan.scannedFiles,
                        scan.largeFileCount,
                        Formatter.formatFileSize(context, scan.minimumBytes),
                    ),
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                    color = kxMuted(night),
                    modifier = Modifier.testTag("heavy-scan-counts"),
                )
            }
            if (scan.truncated) {
                StatusTag(
                    label = stringResource(R.string.heavy_limited),
                    tone = if (night) Color(0xFFFFC98A) else Color(0xFF92400E),
                    tag = "heavy-partial",
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(
                label = stringResource(R.string.heavy_total_label),
                value = Formatter.formatFileSize(context, scan.totalLargeBytes),
                modifier = Modifier.weight(1f),
                night = night,
                accent = kxCyan(night),
                tag = "heavy-total-size",
                foreground = ink,
            )
            MetricTile(
                label = stringResource(R.string.heavy_shown_label),
                value = scan.files.size.toString(),
                modifier = Modifier.weight(1f),
                night = night,
                accent = kxViolet(night),
                tag = "heavy-shown",
                foreground = ink,
            )
        }
        if (scan.skippedUnknownSize > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.heavy_unknown, scan.skippedUnknownSize),
                fontSize = 10.5.sp,
                lineHeight = 15.sp,
                color = if (night) Color(0xFFFFC98A) else Color(0xFF92400E),
                modifier = Modifier.testTag("heavy-unknown"),
            )
        }
        if (scan.cancelled) {
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.heavy_cancelled),
                fontSize = 10.5.sp,
                lineHeight = 15.sp,
                color = ink.copy(alpha = .82f),
                modifier = Modifier.testTag("heavy-cancelled"),
            )
        }
        if (scan.failed) {
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.heavy_scan_failed),
                fontSize = 10.5.sp,
                lineHeight = 15.sp,
                color = if (night) Color(0xFFFFB4B4) else Color(0xFF9B1C1C),
                modifier = Modifier.testTag("heavy-summary-failed"),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.heavy_sort_note),
            fontSize = 9.5.sp,
            lineHeight = 14.sp,
            color = kxMuted(night),
        )
        Text(
            formatDate(context, scan.atMillis),
            fontSize = 9.5.sp,
            lineHeight = 14.sp,
            color = kxMuted(night),
        )
    }
}

@Composable
private fun StatusTag(label: String, tone: Color, tag: String) {
    Row(
        Modifier.clip(RoundedCornerShape(50))
            .background(tone.copy(alpha = .13f))
            .border(1.dp, tone.copy(alpha = .34f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(5.dp).clip(CircleShape).background(tone))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = tone, maxLines = 1)
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier,
    night: Boolean,
    accent: Color,
    tag: String,
    foreground: Color,
) {
    Column(
        modifier.background(accent.copy(alpha = if (night) .13f else .09f),
            RoundedCornerShape(16.dp))
            .border(1.dp, accent.copy(alpha = .28f), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Text(
            value,
            fontSize = 17.sp,
            lineHeight = 21.sp,
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
            color = foreground.copy(alpha = .78f),
            maxLines = 2,
        )
    }
}

@Composable
private fun FiltersCard(
    night: Boolean,
    state: HeavyweightUiState,
    visibleCount: Int,
    query: String,
    sortMode: HeavySortMode,
    onQuery: (String) -> Unit,
    onSort: (HeavySortMode) -> Unit,
    onCategory: (LargeFileCategory?) -> Unit,
) {
    val ink = kxInk(night)
    KxGlassPanel(
        night,
        modifier = Modifier.testTag("heavy-filters"),
        radius = 22.dp,
        padding = PaddingValues(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            KxLineIcon(KxIconKind.SEARCH, ink, Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text(
                stringResource(R.string.heavy_filters_title),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = ink,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.heavy_visible, visibleCount),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = kxCyan(night),
                modifier = Modifier.testTag("heavy-visible-count"),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterPill(
                label = stringResource(R.string.heavy_category_all),
                selected = state.category == null,
                night = night,
                ink = ink,
                tag = "heavy-filter-category-all",
                onClick = { onCategory(null) },
            )
            LargeFileCategory.entries.forEach { category ->
                FilterPill(
                    label = categoryLabel(category),
                    selected = state.category == category,
                    night = night,
                    ink = ink,
                    tag = "heavy-filter-category-${category.key}",
                    onClick = { onCategory(category) },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        SearchField(night, query, onQuery)
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.heavy_ref_sort),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = kxMuted(night),
            )
            HeavySortMode.entries.forEach { mode ->
                FilterPill(
                    label = sortLabel(mode),
                    selected = sortMode == mode,
                    night = night,
                    ink = ink,
                    tag = sortTag(mode),
                    onClick = { onSort(mode) },
                    modifier = Modifier.weight(1f),
                    icon = when (mode) {
                        HeavySortMode.SIZE -> KxIconKind.DATABASE
                        HeavySortMode.DATE -> KxIconKind.CLOCK
                        HeavySortMode.NAME -> KxIconKind.FILE
                    },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.heavy_ref_search_scope),
            fontSize = 9.5.sp,
            lineHeight = 14.sp,
            color = kxMuted(night),
        )
    }
}

@Composable
private fun SearchField(night: Boolean, query: String, onQuery: (String) -> Unit) {
    val ink = kxInk(night)
    val hint = stringResource(R.string.heavy_ref_search_hint)
    val clearLabel = stringResource(R.string.heavy_ref_search_clear)
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = if (night) .07f else .40f))
            .border(1.dp, Color.White.copy(alpha = if (night) .24f else .62f), shape)
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KxLineIcon(KxIconKind.SEARCH, kxMuted(night), Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            textStyle = TextStyle(color = ink, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            cursorBrush = SolidColor(kxCyan(night)),
            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                .semantics { contentDescription = hint }
                .testTag("heavy-search"),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(hint, fontSize = 13.sp, color = kxMuted(night), maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                    inner()
                }
            },
        )
        if (query.isNotEmpty()) {
            Box(
                Modifier.size(48.dp).clip(CircleShape)
                    .clickable(role = Role.Button, onClickLabel = clearLabel) { onQuery("") }
                    .semantics { contentDescription = clearLabel }
                    .testTag("heavy-search-clear"),
                contentAlignment = Alignment.Center,
            ) {
                Text("×", fontSize = 22.sp, lineHeight = 22.sp, color = ink)
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    night: Boolean,
    ink: Color,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: KxIconKind? = null,
) {
    val tone = kxCyan(night)
    val shape = RoundedCornerShape(50)
    Row(
        modifier.heightIn(min = 48.dp)
            .clip(shape)
            .background(
                if (selected) tone.copy(alpha = .20f)
                else Color.White.copy(alpha = if (night) .08f else .42f),
            )
            .border(
                1.dp,
                if (selected) tone.copy(alpha = .75f) else Color.White.copy(alpha = .32f),
                shape,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 13.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            KxLineIcon(icon, if (selected) tone else ink.copy(alpha = .72f), Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(
            label,
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) tone else ink,
            maxLines = 1,
        )
    }
}

@Composable
private fun sortLabel(mode: HeavySortMode): String = stringResource(
    when (mode) {
        HeavySortMode.SIZE -> R.string.heavy_ref_sort_size
        HeavySortMode.DATE -> R.string.heavy_ref_sort_date
        HeavySortMode.NAME -> R.string.heavy_ref_sort_name
    },
)

private fun sortTag(mode: HeavySortMode): String = when (mode) {
    HeavySortMode.SIZE -> "heavy-sort-size"
    HeavySortMode.DATE -> "heavy-sort-date"
    HeavySortMode.NAME -> "heavy-sort-name"
}

@Composable
private fun filesCountLabel(count: Int): String =
    pluralStringResource(R.plurals.heavy_ref_files_count, count, count)

@Composable
private fun sizeFilterLabel(step: Long): String = stringResource(
    when (step) {
        0L -> R.string.heavy_filter_any
        HeavyweightScanner.FILTER_STEPS[1] -> R.string.heavy_filter_10
        HeavyweightScanner.FILTER_STEPS[2] -> R.string.heavy_filter_50
        HeavyweightScanner.FILTER_STEPS[3] -> R.string.heavy_filter_100
        else -> R.string.heavy_filter_500
    },
)

private fun sizeFilterTag(step: Long): String {
    val index = HeavyweightScanner.FILTER_STEPS.indexOf(step)
    return "heavy-filter-size-${if (step == 0L) "any" else index}"
}

@Composable
private fun categoryLabel(category: LargeFileCategory): String = stringResource(
    when (category) {
        LargeFileCategory.IMAGE -> R.string.heavy_category_image
        LargeFileCategory.VIDEO -> R.string.heavy_category_video
        LargeFileCategory.AUDIO -> R.string.heavy_category_audio
        LargeFileCategory.DOCUMENT -> R.string.heavy_category_document
        LargeFileCategory.ARCHIVE -> R.string.heavy_category_archive
        LargeFileCategory.OTHER -> R.string.heavy_category_other
    },
)

private fun categoryIcon(category: LargeFileCategory): KxIconKind = when (category) {
    LargeFileCategory.IMAGE -> KxIconKind.FILE
    LargeFileCategory.VIDEO -> KxIconKind.CLAPPER
    LargeFileCategory.AUDIO -> KxIconKind.MIC
    LargeFileCategory.DOCUMENT -> KxIconKind.FILE
    LargeFileCategory.ARCHIVE -> KxIconKind.LAYERS
    LargeFileCategory.OTHER -> KxIconKind.FILE
}

@Composable
private fun HeatmapCard(
    night: Boolean,
    visible: List<LargeFile>,
    ink: Color,
    onSelect: (LargeFile) -> Unit,
) {
    val context = LocalContext.current
    val top = visible.take(8)
    KxGlassPanel(night, radius = 20.dp, padding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KxLineIcon(KxIconKind.BARS, kxCyan(night), Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text(
                stringResource(R.string.heavy_heatmap_title),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = ink,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.heavy_heatmap_body),
            fontSize = 10.sp,
            lineHeight = 14.sp,
            color = kxMuted(night),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().height(62.dp).testTag("heavy-heatmap"),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val largest = top.firstOrNull()?.bytes?.coerceAtLeast(1L) ?: 1L
            top.forEachIndexed { index, file ->
                val ratio = file.bytes.toFloat() / largest.toFloat()
                val weight = (0.18f + 0.82f * ratio).coerceIn(0.18f, 1f)
                val accent = categoryAccent(file.category, night)
                val description = "${file.name}, ${Formatter.formatFileSize(context, file.bytes)}"
                Box(
                    Modifier.weight(weight).fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    accent.copy(alpha = if (night) .62f else .48f),
                                    accent.copy(alpha = if (night) .22f else .16f),
                                ),
                            ),
                        )
                        .border(1.dp, accent.copy(alpha = .42f), RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button) { onSelect(file) }
                        .semantics { contentDescription = description }
                        .testTag("heavy-heat-$index"),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Text(
                        "${index + 1}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = .9f),
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultList(
    night: Boolean,
    state: HeavyweightUiState,
    visible: List<LargeFile>,
    ink: Color,
    onSelect: (LargeFile) -> Unit,
    onOpen: (LargeFile) -> Unit,
) {
    val context = LocalContext.current
    val displayed = visible.take(HeavyweightViewModel.MAX_DISPLAYED)
    KxGlassPanel(night, radius = 22.dp, padding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KxLineIcon(KxIconKind.FILE, kxCyan(night), Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text(
                stringResource(R.string.heavy_ref_top),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = ink,
                modifier = Modifier.weight(1f),
            )
            Text(
                filesCountLabel(visible.size),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = kxMuted(night),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.heavy_ref_open_only),
            fontSize = 9.5.sp,
            lineHeight = 14.sp,
            color = kxMuted(night),
        )
        Spacer(Modifier.height(8.dp))
        displayed.forEachIndexed { index, file ->
            LargeFileRow(
                index = index,
                file = file,
                selected = state.selectedDocumentId == file.documentId,
                night = night,
                ink = ink,
                context = context,
                onSelect = onSelect,
                onOpen = onOpen,
            )
            if (index != displayed.lastIndex) {
                Spacer(Modifier.height(6.dp))
            }
        }
        if (visible.size > HeavyweightViewModel.MAX_DISPLAYED) {
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.heavy_display_limit, HeavyweightViewModel.MAX_DISPLAYED),
                fontSize = 10.sp,
                color = kxMuted(night),
                modifier = Modifier.testTag("heavy-display-limit"),
            )
        }
    }
}

@Composable
private fun LargeFileRow(
    index: Int,
    file: LargeFile,
    selected: Boolean,
    night: Boolean,
    ink: Color,
    context: android.content.Context,
    onSelect: (LargeFile) -> Unit,
    onOpen: (LargeFile) -> Unit,
) {
    val accent = categoryAccent(file.category, night)
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) accent.copy(alpha = .15f)
                else Color.White.copy(alpha = if (night) .055f else .30f),
            )
            .border(
                1.dp,
                if (selected) accent.copy(alpha = .60f) else Color.White.copy(alpha = .22f),
                shape,
            )
            .clickable(role = Role.Button) { onSelect(file) }
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .testTag("heavy-file-$index"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(13.dp))
                .background(accent.copy(alpha = if (night) .20f else .14f))
                .border(1.dp, accent.copy(alpha = .34f), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center,
        ) {
            KxLineIcon(categoryIcon(file.category), accent, Modifier.size(21.dp))
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(
                file.name,
                fontSize = 12.5.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(textDirection = TextDirection.Content),
                modifier = Modifier.testTag("heavy-file-name-$index"),
            )
            Text(
                file.relativePath,
                fontSize = 9.5.sp,
                lineHeight = 13.sp,
                color = kxMuted(night),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(textDirection = TextDirection.Content),
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Formatter.formatFileSize(context, file.bytes),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent,
                )
                Text(" · ", fontSize = 9.sp, color = kxMuted(night))
                Text(
                    categoryLabel(file.category),
                    fontSize = 9.5.sp,
                    color = kxMuted(night),
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        OpenButton(
            file = file,
            index = index,
            accent = accent,
            night = night,
            onOpen = onOpen,
        )
    }
}

@Composable
private fun OpenButton(
    file: LargeFile,
    index: Int,
    accent: Color,
    night: Boolean,
    onOpen: (LargeFile) -> Unit,
) {
    val label = stringResource(R.string.heavy_open)
    Box(
        Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = if (night) .20f else .14f))
            .border(1.dp, accent.copy(alpha = .46f), RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClickLabel = label) { onOpen(file) }
            .semantics { contentDescription = "$label ${file.name}" }
            .testTag("heavy-open-$index"),
        contentAlignment = Alignment.Center,
    ) {
        KxLineIcon(KxIconKind.FORWARD, accent, Modifier.size(19.dp))
    }
}

@Composable
private fun SelectedFileCard(
    night: Boolean,
    state: HeavyweightUiState,
    ink: Color,
    onOpen: (LargeFile) -> Unit,
) {
    val selected = state.selectedFile ?: return
    val context = LocalContext.current
    val accent = categoryAccent(selected.category, night)
    KxGlassPanel(night, radius = 22.dp, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KxGlassIconTile(night, categoryIcon(selected.category), accent, size = 44.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.heavy_selected),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = ink,
                    modifier = Modifier.testTag("heavy-selected-card"),
                )
                Text(
                    selected.name,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(textDirection = TextDirection.Content),
                )
            }
            StatusTag(
                label = categoryLabel(selected.category),
                tone = accent,
                tag = "heavy-selected-category",
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.heavy_file_path, selected.relativePath),
            fontSize = 10.sp,
            lineHeight = 15.sp,
            color = kxMuted(night),
            style = TextStyle(textDirection = TextDirection.Content),
            modifier = Modifier.testTag("heavy-selected-path"),
        )
        Text(
            "${Formatter.formatFileSize(context, selected.bytes)} · ${selected.mime}",
            fontSize = 11.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
        )
        Text(
            stringResource(R.string.heavy_file_modified, formatDate(context, selected.lastModified)),
            fontSize = 10.sp,
            color = kxMuted(night),
        )
        Spacer(Modifier.height(10.dp))
        KxGradientAction(
            label = stringResource(R.string.heavy_open),
            night = night,
            enabled = true,
            tag = "heavy-selected-open",
            modifier = Modifier.fillMaxWidth(),
            icon = KxIconKind.FORWARD,
            onClick = { onOpen(selected) },
        )
        if (state.openFailed) {
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.heavy_open_failed),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = if (night) Color(0xFFFFB4B4) else Color(0xFF9B1C1C),
                modifier = Modifier.testTag("heavy-open-failed"),
            )
        }
    }
}

@Composable
private fun PrivacyPanel(night: Boolean, ink: Color) {
    KxGlassPanel(night, radius = 18.dp, padding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            KxLineIcon(KxIconKind.LOCK, kxGreen(night), Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.heavy_privacy),
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                    color = ink.copy(alpha = .74f),
                    modifier = Modifier.testTag("heavy-privacy"),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.heavy_floor_note),
                    fontSize = 9.5.sp,
                    lineHeight = 14.sp,
                    color = ink.copy(alpha = .62f),
                )
            }
        }
    }
}

private fun categoryAccent(category: LargeFileCategory, night: Boolean): Color = when (category) {
    LargeFileCategory.IMAGE -> kxCyan(night)
    LargeFileCategory.VIDEO -> kxViolet(night)
    LargeFileCategory.AUDIO -> if (night) Color(0xFFFF9AD5) else Color(0xFFB11E78)
    LargeFileCategory.DOCUMENT -> if (night) Color(0xFF8CA8FF) else Color(0xFF3348B8)
    LargeFileCategory.ARCHIVE -> if (night) Color(0xFFFFC98A) else Color(0xFF92400E)
    LargeFileCategory.OTHER -> if (night) Color(0xFF9BEFFF) else Color(0xFF0B7899)
}

private fun formatDate(context: android.content.Context, atMillis: Long): String {
    if (atMillis <= 0L) return "—"
    val locale = context.resources.configuration.locales.get(0) ?: Locale.getDefault()
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale)
        .format(Date(atMillis))
}
