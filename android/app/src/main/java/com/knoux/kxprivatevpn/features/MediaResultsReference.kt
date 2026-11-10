package com.knoux.kxprivatevpn.features

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.designsystem.KxGlassIconTile
import com.knoux.kxprivatevpn.designsystem.KxGlassPanel
import com.knoux.kxprivatevpn.designsystem.KxGradientAction
import com.knoux.kxprivatevpn.designsystem.KxIconKind
import com.knoux.kxprivatevpn.designsystem.KxLineIcon
import com.knoux.kxprivatevpn.designsystem.kxCyan
import com.knoux.kxprivatevpn.designsystem.kxGreen
import com.knoux.kxprivatevpn.designsystem.kxInk
import com.knoux.kxprivatevpn.designsystem.kxMuted
import com.knoux.kxprivatevpn.designsystem.kxRose
import com.knoux.kxprivatevpn.designsystem.kxViolet
import com.knoux.kxprivatevpn.platform.media.CompressionOutcome
import com.knoux.kxprivatevpn.platform.media.MediaCandidate
import com.knoux.kxprivatevpn.platform.media.MediaKind
import com.knoux.kxprivatevpn.platform.media.MediaQuality
import com.knoux.kxprivatevpn.platform.media.MediaSlimScan

/** Local view-only filters. They never trigger a scan or alter the measured inventory. */
internal enum class MediaResultFilter { ALL, IMAGES, VIDEOS, UNSUPPORTED }

/** Local ordering of the already-measured rows. The default mirrors the scan's size ranking. */
internal enum class MediaResultSort { SIZE_DESC, NAME_ASC, KIND }

internal fun filterAndSortMedia(
    files: List<MediaCandidate>,
    query: String,
    filter: MediaResultFilter,
    sort: MediaResultSort,
): List<MediaCandidate> {
    val needle = query.trim()
    val filtered = files.filter { candidate ->
        val matchesQuery = needle.isEmpty() ||
            candidate.name.contains(needle, ignoreCase = true) ||
            candidate.relativePath.contains(needle, ignoreCase = true)
        val matchesFilter = when (filter) {
            MediaResultFilter.ALL -> true
            MediaResultFilter.IMAGES -> candidate.kind == MediaKind.IMAGE
            MediaResultFilter.VIDEOS -> candidate.kind == MediaKind.VIDEO
            MediaResultFilter.UNSUPPORTED -> candidate.kind == MediaKind.IMAGE && !candidate.compressible
        }
        matchesQuery && matchesFilter
    }
    return when (sort) {
        MediaResultSort.SIZE_DESC -> filtered.sortedByDescending { it.bytes }
        MediaResultSort.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
        MediaResultSort.KIND -> filtered.sortedWith(compareBy({ it.kind }, { it.name.lowercase() }))
    }
}

/**
 * Only verified outputs of a real compression run are counted. The scope is the current
 * selection when one exists; otherwise the verified results stand on their own.
 */
internal data class MediaMeasuredSavings(
    val beforeBytes: Long,
    val afterBytes: Long,
    val savedBytes: Long,
    val verifiedCount: Int,
) {
    val percent: Int?
        get() = if (beforeBytes > 0 && verifiedCount > 0) {
            (savedBytes * 100 / beforeBytes).toInt()
        } else null
}

internal fun measuredSavings(state: MediaSlimUiState): MediaMeasuredSavings {
    val scope = state.selectedSupported.map { it.documentId }.toSet()
    val outcomes = state.verified.filter { outcome ->
        scope.isEmpty() || outcome.value.candidate.documentId in scope
    }
    val before = outcomes.sumOf { it.value.candidate.bytes }
    val after = outcomes.sumOf { it.value.outputBytes }
    return MediaMeasuredSavings(
        beforeBytes = before,
        afterBytes = after,
        savedBytes = (before - after).coerceAtLeast(0),
        verifiedCount = outcomes.size,
    )
}

/**
 * Owner reference 17 results composition. Every number is either a provider-reported size or
 * the measured output of a verified compression copy. No "expected" or fabricated after-size
 * is rendered, and no delete/export/compare control exists because this build has no such
 * verified operation.
 */
@Composable
internal fun MediaReferenceResults(
    night: Boolean,
    state: MediaSlimUiState,
    onToggle: (MediaCandidate) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onQuality: (MediaQuality) -> Unit,
    onRequestCompression: () -> Unit,
) {
    val scan = state.scan ?: return
    MediaScopeStats(night, scan)
    if (scan.files.isEmpty()) {
        MediaEmptyCard(night)
        return
    }
    MediaQualityPanel(night, state, onSelectAll, onClear, onQuality)
    MediaSavingsPanel(night, state)
    MediaResultsList(night, state, onToggle)
    if (scan.videos.isNotEmpty()) {
        KxGlassPanel(
            night,
            modifier = Modifier.testTag("media-video-note"),
            radius = 18.dp,
            padding = PaddingValues(horizontal = 14.dp, vertical = 11.dp),
        ) {
            Text(
                stringResource(R.string.media_video_note),
                fontSize = 11.5.sp,
                lineHeight = 17.sp,
                color = kxMuted(night),
            )
        }
    }
    if (state.selectionError) {
        Text(
            stringResource(R.string.media_selection_empty),
            color = kxRose(night),
            fontSize = 13.sp,
            modifier = Modifier.testTag("media-selection-error"),
        )
    }
    if (state.selectedSupported.isNotEmpty()) MediaActionPanel(night, state, onRequestCompression)
    Text(
        stringResource(R.string.media_ref_results_note),
        fontSize = 11.sp,
        lineHeight = 16.sp,
        color = kxMuted(night),
        modifier = Modifier.fillMaxWidth().testTag("media-ref-results-note"),
    )
}

@Composable
private fun MediaScopeStats(night: Boolean, scan: MediaSlimScan) {
    val context = LocalContext.current
    KxGlassPanel(night, radius = 22.dp, padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Text(
            stringResource(
                R.string.media_scan_summary,
                scan.images.size,
                scan.videos.size,
                Formatter.formatFileSize(context, scan.imageBytes + scan.videoBytes),
            ),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = kxMuted(night),
            modifier = Modifier.testTag("media-scan-summary"),
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MediaScopeMetric(
                night = night,
                icon = KxIconKind.LAYERS,
                label = stringResource(R.string.media_ref_images),
                value = scan.images.size.toString(),
                detail = Formatter.formatFileSize(context, scan.imageBytes),
                modifier = Modifier.weight(1f),
                tag = "media-scope-images",
            )
            Box(Modifier.width(1.dp).height(44.dp).background(kxMuted(night).copy(alpha = .28f)))
            MediaScopeMetric(
                night = night,
                icon = KxIconKind.CLAPPER,
                label = stringResource(R.string.media_ref_videos),
                value = scan.videos.size.toString(),
                detail = Formatter.formatFileSize(context, scan.videoBytes),
                modifier = Modifier.weight(1f),
                tag = "media-scope-videos",
            )
        }
    }
}

@Composable
private fun MediaScopeMetric(
    night: Boolean,
    icon: KxIconKind,
    label: String,
    value: String,
    detail: String,
    modifier: Modifier,
    tag: String,
) {
    Row(
        modifier.testTag(tag).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        KxLineIcon(icon, kxCyan(night), Modifier.size(22.dp))
        Column {
            Text(label, color = kxMuted(night), fontSize = 10.5.sp, lineHeight = 14.sp)
            Text(value, color = kxInk(night), fontSize = 17.sp, lineHeight = 22.sp,
                fontWeight = FontWeight.Bold)
            Text(detail, color = kxMuted(night), fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun MediaQualityPanel(
    night: Boolean,
    state: MediaSlimUiState,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onQuality: (MediaQuality) -> Unit,
) {
    val context = LocalContext.current
    val enabled = !state.compressing
    KxGlassPanel(night, radius = 22.dp, padding = PaddingValues(13.dp)) {
        Text(
            stringResource(
                R.string.media_selected,
                state.selectedSupported.size,
                Formatter.formatFileSize(context, state.selectedBytes),
            ),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = kxInk(night),
            modifier = Modifier.testTag("media-selected-summary"),
        )
        Spacer(Modifier.height(7.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MediaTextAction(
                label = stringResource(R.string.media_select_all),
                enabled = enabled,
                tag = "media-select-all",
                modifier = Modifier.weight(1f),
                onClick = onSelectAll,
            )
            MediaTextAction(
                label = stringResource(R.string.media_clear),
                enabled = enabled,
                tag = "media-clear",
                modifier = Modifier.weight(1f),
                onClick = onClear,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.media_quality),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = kxInk(night),
        )
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            MediaQuality.entries.forEach { quality ->
                MediaQualityTile(
                    night = night,
                    quality = quality,
                    selected = state.quality == quality,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onClick = { onQuality(quality) },
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            stringResource(R.string.media_quality_note),
            fontSize = 10.5.sp,
            lineHeight = 15.sp,
            color = kxMuted(night),
        )
    }
}

@Composable
private fun MediaTextAction(
    label: String,
    enabled: Boolean,
    tag: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = .08f))
            .border(1.dp, Color.White.copy(alpha = .24f), RoundedCornerShape(50))
            .clickable(role = Role.Button, enabled = enabled, onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = Color.White.copy(alpha = if (enabled) .92f else .45f),
            fontSize = 11.sp,
            lineHeight = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun MediaQualityTile(
    night: Boolean,
    quality: MediaQuality,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val accent = when (quality) {
        MediaQuality.HIGH -> kxRose(night)
        MediaQuality.BALANCED -> kxViolet(night)
        MediaQuality.SMALL -> kxGreen(night)
    }
    val fill = if (selected) {
        Brush.linearGradient(listOf(accent.copy(alpha = .34f), accent.copy(alpha = .12f)))
    } else {
        Brush.linearGradient(listOf(Color.White.copy(alpha = if (night) .06f else .42f),
            accent.copy(alpha = .08f)))
    }
    Column(
        modifier.heightIn(min = 86.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(fill)
            .border(1.dp,
                if (selected) accent.copy(alpha = .88f) else Color.White.copy(alpha = .24f),
                RoundedCornerShape(16.dp))
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .testTag("media-quality-${quality.name.lowercase()}")
            .padding(horizontal = 7.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        KxLineIcon(
            when (quality) {
                MediaQuality.HIGH -> KxIconKind.SPARKLE
                MediaQuality.BALANCED -> KxIconKind.LAYERS
                MediaQuality.SMALL -> KxIconKind.FILE
            },
            accent,
            Modifier.size(19.dp),
        )
        Text(
            stringResource(when (quality) {
                MediaQuality.HIGH -> R.string.media_quality_high
                MediaQuality.BALANCED -> R.string.media_quality_balanced
                MediaQuality.SMALL -> R.string.media_quality_small
            }),
            fontSize = 10.5.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            color = kxInk(night),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            stringResource(when (quality) {
                MediaQuality.HIGH -> R.string.media_ref_quality_high_desc
                MediaQuality.BALANCED -> R.string.media_ref_quality_balanced_desc
                MediaQuality.SMALL -> R.string.media_ref_quality_small_desc
            }),
            fontSize = 9.sp,
            lineHeight = 12.sp,
            color = kxMuted(night),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MediaSavingsPanel(night: Boolean, state: MediaSlimUiState) {
    val context = LocalContext.current
    val savings = measuredSavings(state)
    KxGlassPanel(night, radius = 22.dp, padding = PaddingValues(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            KxGlassIconTile(night, KxIconKind.DRIVE_HEART, kxCyan(night), size = 38.dp)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.media_ref_savings_title),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = kxInk(night),
                )
                Text(
                    stringResource(R.string.media_ref_savings_scope, savings.verifiedCount),
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = kxMuted(night),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SavingsMetric(
                night, stringResource(R.string.media_ref_savings_before),
                Formatter.formatFileSize(context,
                    if (savings.verifiedCount > 0) savings.beforeBytes
                    else state.selectedBytes.takeIf { it > 0 } ?: 0L),
                Modifier.weight(1f), "media-savings-before",
            )
            SavingsMetric(
                night, stringResource(R.string.media_ref_savings_after),
                if (savings.verifiedCount > 0) Formatter.formatFileSize(context, savings.afterBytes)
                else stringResource(R.string.media_ref_not_measured),
                Modifier.weight(1f), "media-savings-after",
            )
            SavingsMetric(
                night, stringResource(R.string.media_ref_savings_saved),
                savings.percent?.let { stringResource(R.string.media_ref_saved_percent, it) }
                    ?: stringResource(R.string.media_ref_not_measured),
                Modifier.weight(1f), "media-savings-percent",
            )
        }
        Spacer(Modifier.height(9.dp))
        if (savings.percent != null) {
            LinearProgressIndicator(
                progress = { savings.savedBytes.toFloat() / savings.beforeBytes.toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                color = kxGreen(night),
                trackColor = Color.White.copy(alpha = if (night) .14f else .45f),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.media_ref_savings_note),
            fontSize = 10.sp,
            lineHeight = 15.sp,
            color = kxMuted(night),
            modifier = Modifier.testTag("media-savings-note"),
        )
    }
}

@Composable
private fun SavingsMetric(
    night: Boolean,
    label: String,
    value: String,
    modifier: Modifier,
    tag: String,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 9.5.sp, lineHeight = 13.sp, color = kxMuted(night),
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold,
            color = kxInk(night), maxLines = 2, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center, modifier = Modifier.testTag(tag))
    }
}

@Composable
private fun MediaResultsList(
    night: Boolean,
    state: MediaSlimUiState,
    onToggle: (MediaCandidate) -> Unit,
) {
    val scan = state.scan ?: return
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(MediaResultFilter.ALL) }
    var sort by remember { mutableStateOf(MediaResultSort.SIZE_DESC) }
    val visible = remember(state.visibleFiles, query, filter, sort) {
        filterAndSortMedia(state.visibleFiles, query, filter, sort)
    }
    val verified = remember(state.results) {
        state.verified.associateBy { it.value.candidate.documentId }
    }
    KxGlassPanel(night, radius = 24.dp, padding = PaddingValues(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.media_files_title),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = kxInk(night),
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier.clip(RoundedCornerShape(50))
                    .background(kxCyan(night).copy(alpha = .14f))
                    .border(1.dp, kxCyan(night).copy(alpha = .42f), RoundedCornerShape(50))
                    .padding(horizontal = 9.dp, vertical = 4.dp),
            ) {
                Text(
                    stringResource(R.string.media_ref_visible, visible.size, scan.files.size),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = kxInk(night),
                    modifier = Modifier.testTag("media-visible-count"),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        MediaSearchField(night, query) { query = it }
        Spacer(Modifier.height(7.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MediaResultFilter.entries.forEach { value ->
                val label = stringResource(when (value) {
                    MediaResultFilter.ALL -> R.string.media_ref_filter_all
                    MediaResultFilter.IMAGES -> R.string.media_ref_filter_images
                    MediaResultFilter.VIDEOS -> R.string.media_ref_filter_videos
                    MediaResultFilter.UNSUPPORTED -> R.string.media_ref_filter_unsupported
                })
                MediaChoiceChip(
                    label = label,
                    night = night,
                    selected = filter == value,
                    tag = "media-filter-${value.name.lowercase()}",
                ) { filter = value }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.media_ref_sort_label), fontSize = 10.sp,
                color = kxMuted(night))
            MediaResultSort.entries.forEach { value ->
                val label = stringResource(when (value) {
                    MediaResultSort.SIZE_DESC -> R.string.media_ref_sort_size
                    MediaResultSort.NAME_ASC -> R.string.media_ref_sort_name
                    MediaResultSort.KIND -> R.string.media_ref_sort_kind
                })
                MediaChoiceChip(
                    label = label,
                    night = night,
                    selected = sort == value,
                    tag = when (value) {
                        MediaResultSort.SIZE_DESC -> "media-sort-size"
                        MediaResultSort.NAME_ASC -> "media-sort-name"
                        MediaResultSort.KIND -> "media-sort-kind"
                    },
                ) { sort = value }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (visible.isEmpty()) {
            Text(
                stringResource(R.string.media_ref_results_empty),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = kxMuted(night),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    .testTag("media-results-empty"),
            )
        } else {
            visible.forEachIndexed { index, file ->
                if (index > 0) {
                    Box(Modifier.fillMaxWidth().height(1.dp)
                        .background(Color.White.copy(alpha = if (night) .10f else .40f)))
                }
                MediaFileRow(
                    night = night,
                    file = file,
                    selected = file.documentId in state.selected,
                    enabled = file.compressible && !state.compressing,
                    verified = verified[file.documentId],
                    onToggle = onToggle,
                )
            }
        }
        if (scan.files.size > state.visibleFiles.size) {
            Text(
                stringResource(R.string.media_files_limit, state.visibleFiles.size, scan.files.size),
                fontSize = 10.sp,
                lineHeight = 15.sp,
                color = kxMuted(night),
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun MediaSearchField(night: Boolean, value: String, onValueChange: (String) -> Unit) {
    val label = stringResource(R.string.media_ref_search)
    val focusManager = LocalFocusManager.current
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = kxInk(night), fontSize = 13.sp),
        cursorBrush = SolidColor(kxCyan(night)),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = if (night) .08f else .48f))
            .border(1.dp, Color.White.copy(alpha = if (night) .24f else .70f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 11.dp)
            .testTag("media-search")
            .semantics { contentDescription = label },
        decorationBox = { inner ->
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KxLineIcon(KxIconKind.SEARCH, kxMuted(night), Modifier.size(18.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(label, fontSize = 12.sp, color = kxMuted(night), maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                    inner()
                }
                if (value.isNotEmpty()) {
                    Text(
                        "×",
                        fontSize = 18.sp,
                        color = kxMuted(night),
                        modifier = Modifier.clickable(role = Role.Button) { onValueChange("") }
                            .testTag("media-search-clear")
                            .padding(horizontal = 4.dp),
                    )
                }
            }
        },
    )
}

@Composable
private fun MediaChoiceChip(
    label: String,
    night: Boolean,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    val accent = kxCyan(night)
    Box(
        Modifier.clip(RoundedCornerShape(50))
            .background(if (selected) accent.copy(alpha = .24f)
            else Color.White.copy(alpha = if (night) .06f else .36f))
            .border(1.dp,
                if (selected) accent.copy(alpha = .78f) else Color.White.copy(alpha = .22f),
                RoundedCornerShape(50))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .testTag(tag)
            .padding(horizontal = 11.dp, vertical = 6.dp),
    ) {
        Text(label, fontSize = 10.5.sp, lineHeight = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = kxInk(night), maxLines = 1)
    }
}

@Composable
private fun MediaFileRow(
    night: Boolean,
    file: MediaCandidate,
    selected: Boolean,
    enabled: Boolean,
    verified: CompressionOutcome.Verified?,
    onToggle: (MediaCandidate) -> Unit,
) {
    val context = LocalContext.current
    val accent = if (file.kind == MediaKind.IMAGE) kxCyan(night) else kxViolet(night)
    val savedPercent = verified?.value?.let { value ->
        if (value.candidate.bytes > 0) {
            (value.savedBytes * 100 / value.candidate.bytes).toInt()
        } else null
    }
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accent.copy(alpha = .13f) else Color.Transparent)
            .toggleable(
                value = selected,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = { if (enabled) onToggle(file) },
            )
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag("media-file-${file.documentId}")
            .semantics(mergeDescendants = true) { contentDescription = file.name },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Checkbox(checked = selected, onCheckedChange = null, enabled = enabled)
        KxGlassIconTile(
            night,
            if (file.kind == MediaKind.VIDEO) KxIconKind.CLAPPER else KxIconKind.LAYERS,
            accent,
            size = 38.dp,
        )
        Column(Modifier.weight(1f)) {
            Text(file.name, fontSize = 12.5.sp, lineHeight = 17.sp,
                fontWeight = FontWeight.SemiBold, color = kxInk(night),
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(file.relativePath, fontSize = 9.5.sp, lineHeight = 13.sp,
                color = kxMuted(night), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(when {
                    file.kind == MediaKind.VIDEO -> R.string.media_badge_video
                    file.compressible -> R.string.media_badge_image
                    else -> R.string.media_badge_unsupported
                }),
                fontSize = 9.5.sp,
                lineHeight = 13.sp,
                color = if (file.compressible) accent else kxMuted(night),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(Formatter.formatFileSize(context, file.bytes), fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold, color = kxInk(night),
                modifier = Modifier.testTag("media-row-before-${file.documentId}"))
            Text(
                verified?.value?.let { Formatter.formatFileSize(context, it.outputBytes) }
                    ?: stringResource(R.string.media_ref_not_measured),
                fontSize = 9.5.sp,
                lineHeight = 13.sp,
                color = if (verified != null) kxGreen(night) else kxMuted(night),
                modifier = Modifier.testTag("media-row-after-${file.documentId}"),
            )
            if (savedPercent != null) {
                Text(
                    stringResource(R.string.media_ref_saved_percent, savedPercent),
                    fontSize = 9.5.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = kxGreen(night),
                    modifier = Modifier.testTag("media-row-saved-${file.documentId}"),
                )
            }
        }
    }
}

@Composable
private fun MediaActionPanel(
    night: Boolean,
    state: MediaSlimUiState,
    onRequestCompression: () -> Unit,
) {
    val context = LocalContext.current
    KxGlassPanel(night, radius = 22.dp, padding = PaddingValues(12.dp)) {
        Text(
            stringResource(
                R.string.media_ref_action_summary,
                state.selectedSupported.size,
                Formatter.formatFileSize(context, state.selectedBytes),
            ),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = kxInk(night),
            modifier = Modifier.testTag("media-compress-summary"),
        )
        Spacer(Modifier.height(8.dp))
        KxGradientAction(
            label = stringResource(R.string.media_compress),
            night = night,
            enabled = !state.compressing,
            tag = "media-compress",
            icon = KxIconKind.WAND,
            onClick = onRequestCompression,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.media_ref_action_note),
            fontSize = 10.sp,
            lineHeight = 15.sp,
            color = kxMuted(night),
            modifier = Modifier.testTag("media-ref-action-note"),
        )
    }
}

@Composable
private fun MediaEmptyCard(night: Boolean) {
    KxGlassPanel(night, radius = 22.dp, padding = PaddingValues(14.dp)) {
        Text(
            stringResource(R.string.media_empty),
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = kxMuted(night),
            modifier = Modifier.testTag("media-empty"),
        )
    }
}
