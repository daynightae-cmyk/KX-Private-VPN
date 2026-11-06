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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
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
import com.knoux.kxprivatevpn.designsystem.GlassSurface
import com.knoux.kxprivatevpn.designsystem.KxPageHeader
import com.knoux.kxprivatevpn.designsystem.RepairGlyph
import com.knoux.kxprivatevpn.platform.repair.HeavyweightScan
import com.knoux.kxprivatevpn.platform.repair.HeavyweightScanner
import com.knoux.kxprivatevpn.platform.repair.LargeFile
import com.knoux.kxprivatevpn.platform.repair.LargeFileCategory
import com.knoux.kxprivatevpn.platform.repair.RepairServiceId
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

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
    val foreground = if (night) Color(0xFFF7F3FF) else Color(0xFF171050)
    val scan = state.scan
    val visible = state.visibleFiles
    Box(Modifier.fillMaxSize()) {
        Atmosphere(night, 0f)
        Column(
            Modifier.fillMaxSize().safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .testTag("route-heavyweight"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KxPageHeader(
                title = stringResource(R.string.repair_service_heavyweight),
                night = night,
                backLabel = stringResource(R.string.back),
                onBack = onBack,
            )
            Text(
                stringResource(R.string.heavy_subtitle),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = foreground.copy(alpha = .82f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            HeavyHero(night, state.scanning)
            TreeCard(night, state, foreground, onChooseFolder, onScan, onCancelScan)
            if (scan != null) {
                SummaryCard(night, scan, foreground)
                FiltersCard(night, state, visible.size, foreground, onMinimumBytes, onCategory)
                if (visible.isNotEmpty()) {
                    HeatmapCard(night, visible, foreground, onSelect)
                    ResultList(night, state, visible, foreground, onSelect, onOpen)
                } else if (scan.files.isEmpty()) {
                    GlassSurface(night) {
                        Text(
                            stringResource(R.string.heavy_empty),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = foreground.copy(alpha = .84f),
                            modifier = Modifier.testTag("heavy-empty"),
                        )
                    }
                } else {
                    GlassSurface(night) {
                        Text(
                            stringResource(R.string.heavy_no_match),
                            fontSize = 14.sp,
                            color = foreground.copy(alpha = .84f),
                            modifier = Modifier.testTag("heavy-no-match"),
                        )
                    }
                }
                SelectedFileCard(night, state, foreground, onOpen)
            }
            Text(
                stringResource(R.string.heavy_privacy),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = foreground.copy(alpha = .72f),
                modifier = Modifier.testTag("heavy-privacy"),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeavyHero(night: Boolean, scanning: Boolean) {
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(scanning) {
        if (scanning) {
            sweep.snapTo(0f)
            sweep.animateTo(360f, tween(1900, easing = LinearEasing))
        } else {
            sweep.snapTo(0f)
        }
    }
    Box(Modifier.fillMaxWidth().height(238.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(216.dp)) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val radius = min(size.width, size.height) / 2f - 4.dp.toPx()
            drawCircle(
                Brush.radialGradient(
                    listOf(
                        Color(0xFF64EEFF).copy(alpha = .26f),
                        Color(0xFFAD71FF).copy(alpha = .12f),
                        Color.Transparent,
                    ),
                    c,
                    radius,
                ),
                radius,
                c,
            )
            repeat(3) { index ->
                drawCircle(
                    Color(0xFF64EEFF).copy(alpha = .18f - index * .04f),
                    radius * (.55f + index * .18f),
                    c,
                    style = Stroke(1.dp.toPx()),
                )
            }
            drawArc(
                Brush.sweepGradient(
                    listOf(Color.Transparent, Color(0xFF64EEFF), Color(0xFFAD71FF),
                        Color.Transparent),
                    c,
                ),
                startAngle = sweep.value,
                sweepAngle = 96f,
                useCenter = false,
                topLeft = Offset(c.x - radius, c.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round),
            )
            if (!scanning) {
                repeat(5) { index ->
                    val width = radius * (1.35f - index * .18f)
                    val y = c.y + radius * .55f - index * 8.dp.toPx()
                    drawLine(
                        Color(0xFF64EEFF).copy(alpha = .28f + index * .08f),
                        Offset(c.x - width / 2f, y),
                        Offset(c.x + width / 2f, y),
                        2.2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
        RepairGlyph(RepairServiceId.HEAVYWEIGHT, Modifier.size(132.dp))
    }
}

@Composable
private fun TreeCard(
    night: Boolean,
    state: HeavyweightUiState,
    foreground: Color,
    onChooseFolder: () -> Unit,
    onScan: () -> Unit,
    onCancelScan: () -> Unit,
) {
    GlassSurface(night) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.heavy_folder_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = foreground,
                modifier = Modifier.weight(1f),
            )
            if (state.scanning) LinearProgressIndicator(Modifier.width(66.dp))
        }
        if (state.treeUri == null) {
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.heavy_tree_none),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = foreground.copy(alpha = .82f),
            )
            OutlinedButton(
                onClick = onChooseFolder,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .testTag("heavy-choose-folder"),
            ) { Text(stringResource(R.string.heavy_choose_folder)) }
        } else {
            Spacer(Modifier.height(4.dp))
            Text(
                state.treeName ?: stringResource(R.string.heavy_folder_title),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = foreground,
                modifier = Modifier.testTag("heavy-tree-name"),
            )
            Text(
                stringResource(R.string.heavy_read_only),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = accentTone(Color(0xFF64EEFF), night),
                modifier = Modifier.testTag("heavy-read-only"),
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (state.scanning) {
                    OutlinedButton(
                        onClick = onCancelScan,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("heavy-scan-cancel"),
                    ) { Text(stringResource(R.string.heavy_cancel)) }
                } else {
                    OutlinedButton(
                        onClick = onScan,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag(if (state.scan == null) "heavy-scan" else "heavy-rescan"),
                    ) {
                        Text(stringResource(
                            if (state.scan == null) R.string.heavy_scan else R.string.heavy_rescan
                        ))
                    }
                }
                TextButton(
                    onClick = onChooseFolder,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("heavy-choose-again"),
                ) { Text(stringResource(R.string.heavy_choose_again)) }
            }
            if (state.scanning) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(
                    stringResource(R.string.heavy_scanning, state.progress),
                    fontSize = 12.sp,
                    color = foreground.copy(alpha = .8f),
                    modifier = Modifier.testTag("heavy-scan-progress"),
                )
            }
            if (state.scanFailed && !state.scanning) {
                Text(
                    stringResource(R.string.heavy_scan_failed),
                    fontSize = 13.sp,
                    color = accentTone(Color(0xFFFFB4B4), night),
                    modifier = Modifier.testTag("heavy-scan-failed"),
                )
            }
        }
        Text(
            stringResource(R.string.heavy_floor_note),
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = foreground.copy(alpha = .72f),
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun SummaryCard(night: Boolean, scan: HeavyweightScan, foreground: Color) {
    val context = LocalContext.current
    GlassSurface(night) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.heavy_summary_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = foreground,
                modifier = Modifier.weight(1f),
            )
            if (scan.truncated) {
                Text(
                    stringResource(R.string.heavy_limited),
                    fontSize = 9.sp,
                    color = accentTone(Color(0xFFFFC98A), night),
                    modifier = Modifier.testTag("heavy-partial"),
                )
            }
        }
        Text(
            stringResource(
                R.string.heavy_scan_counts,
                scan.scannedFiles,
                scan.largeFileCount,
                Formatter.formatFileSize(context, scan.minimumBytes),
            ),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = foreground.copy(alpha = .78f),
            modifier = Modifier.testTag("heavy-scan-counts").padding(vertical = 4.dp),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(
                label = stringResource(R.string.heavy_total_label),
                value = Formatter.formatFileSize(context, scan.totalLargeBytes),
                modifier = Modifier.weight(1f),
                night = night,
                accent = Color(0xFF64EEFF),
                tag = "heavy-total-size",
                foreground = foreground,
            )
            MetricTile(
                label = stringResource(R.string.heavy_shown_label),
                value = scan.files.size.toString(),
                modifier = Modifier.weight(1f),
                night = night,
                accent = Color(0xFFAD71FF),
                tag = "heavy-shown",
                foreground = foreground,
            )
        }
        if (scan.skippedUnknownSize > 0) {
            Text(
                stringResource(R.string.heavy_unknown, scan.skippedUnknownSize),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = accentTone(Color(0xFFFFC98A), night),
                modifier = Modifier.testTag("heavy-unknown"),
            )
        }
        if (scan.cancelled) {
            Text(
                stringResource(R.string.heavy_cancelled),
                fontSize = 12.sp,
                color = foreground.copy(alpha = .82f),
                modifier = Modifier.testTag("heavy-cancelled"),
            )
        }
        if (scan.failed) {
            Text(
                stringResource(R.string.heavy_scan_failed),
                fontSize = 12.sp,
                color = accentTone(Color(0xFFFFB4B4), night),
                modifier = Modifier.testTag("heavy-summary-failed"),
            )
        }
        Text(
            stringResource(R.string.heavy_sort_note),
            fontSize = 11.sp,
            color = foreground.copy(alpha = .7f),
        )
        Text(
            formatDate(context, scan.atMillis),
            fontSize = 10.sp,
            color = foreground.copy(alpha = .6f),
        )
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
    val tone = accentTone(accent, night)
    Column(
        modifier.background(tone.copy(alpha = .12f), RoundedCornerShape(16.dp))
            .border(1.dp, tone.copy(alpha = .28f), RoundedCornerShape(16.dp))
            .padding(10.dp),
    ) {
        Text(
            value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = tone,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag(tag),
        )
        Text(label, fontSize = 10.sp, lineHeight = 13.sp, color = foreground.copy(alpha = .8f))
    }
}

@Composable
private fun FiltersCard(
    night: Boolean,
    state: HeavyweightUiState,
    visibleCount: Int,
    foreground: Color,
    onMinimumBytes: (Long) -> Unit,
    onCategory: (LargeFileCategory?) -> Unit,
) {
    GlassSurface(night, modifier = Modifier.testTag("heavy-filters")) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.heavy_filters_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = foreground,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.heavy_visible, visibleCount),
                fontSize = 11.sp,
                color = foreground.copy(alpha = .76f),
                modifier = Modifier.testTag("heavy-visible-count"),
            )
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HeavyweightScanner.FILTER_STEPS.forEachIndexed { index, step ->
                FilterPill(
                    label = sizeFilterLabel(step),
                    selected = state.minimumBytes == step,
                    night = night,
                    foreground = foreground,
                    tag = "heavy-filter-size-${if (step == 0L) "any" else index}",
                    onClick = { onMinimumBytes(step) },
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterPill(
                label = stringResource(R.string.heavy_category_all),
                selected = state.category == null,
                night = night,
                foreground = foreground,
                tag = "heavy-filter-category-all",
                onClick = { onCategory(null) },
            )
            LargeFileCategory.entries.forEach { category ->
                FilterPill(
                    label = categoryLabel(category),
                    selected = state.category == category,
                    night = night,
                    foreground = foreground,
                    tag = "heavy-filter-category-${category.key}",
                    onClick = { onCategory(category) },
                )
            }
        }
    }
}

@Composable
private fun sizeFilterLabel(step: Long): String = when (step) {
    0L -> stringResource(R.string.heavy_filter_any)
    HeavyweightScanner.FILTER_STEPS[1] -> stringResource(R.string.heavy_filter_10)
    HeavyweightScanner.FILTER_STEPS[2] -> stringResource(R.string.heavy_filter_50)
    HeavyweightScanner.FILTER_STEPS[3] -> stringResource(R.string.heavy_filter_100)
    else -> stringResource(R.string.heavy_filter_500)
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
    }
)

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    night: Boolean,
    foreground: Color,
    tag: String,
    onClick: () -> Unit,
) {
    val tone = accentTone(Color(0xFF64EEFF), night)
    val shape = RoundedCornerShape(50)
    Box(
        Modifier.heightIn(min = 48.dp)
            .clip(shape)
            .background(
                if (selected) tone.copy(alpha = .20f)
                else Color.White.copy(alpha = if (night) .08f else .42f)
            )
            .border(
                1.dp,
                if (selected) tone.copy(alpha = .75f) else Color.White.copy(alpha = .32f),
                shape,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) tone else foreground,
            maxLines = 1,
        )
    }
}

@Composable
private fun HeatmapCard(
    night: Boolean,
    visible: List<LargeFile>,
    foreground: Color,
    onSelect: (LargeFile) -> Unit,
) {
    val context = LocalContext.current
    val top = visible.take(8)
    GlassSurface(night) {
        Text(
            stringResource(R.string.heavy_heatmap_title),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = foreground,
        )
        Text(
            stringResource(R.string.heavy_heatmap_body),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = foreground.copy(alpha = .78f),
        )
        Row(
            Modifier.fillMaxWidth().height(78.dp).testTag("heavy-heatmap"),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val largest = top.firstOrNull()?.bytes?.coerceAtLeast(1L) ?: 1L
            top.forEachIndexed { index, file ->
                val ratio = file.bytes.toFloat() / largest.toFloat()
                val weight = (0.18f + 0.82f * ratio).coerceIn(0.18f, 1f)
                val accent = categoryAccent(file.category)
                val description = "${file.name}, ${Formatter.formatFileSize(context, file.bytes)}"
                Box(
                    Modifier.weight(weight).fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.verticalGradient(listOf(
                                accent.copy(alpha = if (night) .62f else .48f),
                                accent.copy(alpha = if (night) .22f else .16f),
                            ))
                        )
                        .border(1.dp, accent.copy(alpha = .42f), RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button) { onSelect(file) }
                        .semantics { contentDescription = description }
                        .testTag("heavy-heat-$index"),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Text(
                        "${index + 1}",
                        fontSize = 10.sp,
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
    foreground: Color,
    onSelect: (LargeFile) -> Unit,
    onOpen: (LargeFile) -> Unit,
) {
    val context = LocalContext.current
    GlassSurface(night) {
        Text(
            stringResource(R.string.heavy_results_title),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = foreground,
        )
        Text(
            stringResource(R.string.heavy_select_hint),
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = foreground.copy(alpha = .72f),
        )
        visible.take(HeavyweightViewModel.MAX_DISPLAYED).forEachIndexed { index, file ->
            LargeFileRow(
                index = index,
                file = file,
                selected = state.selectedDocumentId == file.documentId,
                night = night,
                foreground = foreground,
                context = context,
                onSelect = onSelect,
                onOpen = onOpen,
            )
        }
        if (visible.size > HeavyweightViewModel.MAX_DISPLAYED) {
            Text(
                stringResource(R.string.heavy_display_limit, HeavyweightViewModel.MAX_DISPLAYED),
                fontSize = 11.sp,
                color = foreground.copy(alpha = .7f),
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
    foreground: Color,
    context: android.content.Context,
    onSelect: (LargeFile) -> Unit,
    onOpen: (LargeFile) -> Unit,
) {
    val accent = accentTone(categoryAccent(file.category), night)
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) accent.copy(alpha = .14f)
                else Color.White.copy(alpha = if (night) .05f else .28f)
            )
            .border(
                1.dp,
                if (selected) accent.copy(alpha = .55f) else Color.White.copy(alpha = .22f),
                shape,
            )
            .clickable(role = Role.Button) { onSelect(file) }
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("heavy-file-$index"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(accent.copy(alpha = .16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accent)
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(
                file.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = foreground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("heavy-file-name-$index"),
            )
            Text(
                file.relativePath,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                color = foreground.copy(alpha = .68f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Formatter.formatFileSize(context, file.bytes),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
                Text(" · ", fontSize = 10.sp, color = foreground.copy(alpha = .6f))
                Text(
                    categoryLabel(file.category),
                    fontSize = 10.sp,
                    color = foreground.copy(alpha = .74f),
                )
            }
        }
        TextButton(
            onClick = { onOpen(file) },
            modifier = Modifier.heightIn(min = 48.dp).testTag("heavy-open-$index"),
        ) { Text(stringResource(R.string.heavy_open), fontSize = 12.sp) }
    }
}

@Composable
private fun SelectedFileCard(
    night: Boolean,
    state: HeavyweightUiState,
    foreground: Color,
    onOpen: (LargeFile) -> Unit,
) {
    val selected = state.selectedFile ?: return
    val context = LocalContext.current
    val accent = accentTone(categoryAccent(selected.category), night)
    GlassSurface(night) {
        Text(
            stringResource(R.string.heavy_selected),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = foreground,
            modifier = Modifier.testTag("heavy-selected-card"),
        )
        Text(
            selected.name,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = foreground,
        )
        Text(
            stringResource(R.string.heavy_file_path, selected.relativePath),
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = foreground.copy(alpha = .72f),
            modifier = Modifier.testTag("heavy-selected-path"),
        )
        Text(
            "${Formatter.formatFileSize(context, selected.bytes)} · ${categoryLabel(selected.category)}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
        )
        Text(
            stringResource(R.string.heavy_file_mime, selected.mime),
            fontSize = 11.sp,
            color = foreground.copy(alpha = .76f),
        )
        Text(
            stringResource(R.string.heavy_file_modified, formatDate(context, selected.lastModified)),
            fontSize = 11.sp,
            color = foreground.copy(alpha = .76f),
        )
        OutlinedButton(
            onClick = { onOpen(selected) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .testTag("heavy-selected-open"),
        ) { Text(stringResource(R.string.heavy_open)) }
        if (state.openFailed) {
            Text(
                stringResource(R.string.heavy_open_failed),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = accentTone(Color(0xFFFFB4B4), night),
                modifier = Modifier.testTag("heavy-open-failed"),
            )
        }
    }
}

private fun categoryAccent(category: LargeFileCategory): Color = when (category) {
    LargeFileCategory.IMAGE -> Color(0xFF64EEFF)
    LargeFileCategory.VIDEO -> Color(0xFFAD71FF)
    LargeFileCategory.AUDIO -> Color(0xFFFF9AD5)
    LargeFileCategory.DOCUMENT -> Color(0xFF8CA8FF)
    LargeFileCategory.ARCHIVE -> Color(0xFFFFC98A)
    LargeFileCategory.OTHER -> Color(0xFF9BEFFF)
}

private fun accentTone(color: Color, night: Boolean): Color =
    if (night) color else lerp(color, Color(0xFF171050), .55f)

private fun formatDate(context: android.content.Context, atMillis: Long): String {
    if (atMillis <= 0L) return "—"
    val locale = context.resources.configuration.locales.get(0) ?: Locale.getDefault()
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale)
        .format(Date(atMillis))
}
