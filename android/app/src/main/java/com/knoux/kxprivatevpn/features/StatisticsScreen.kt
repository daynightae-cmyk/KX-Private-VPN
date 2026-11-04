package com.knoux.kxprivatevpn.features

import android.content.Context
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.designsystem.Atmosphere
import com.knoux.kxprivatevpn.designsystem.GlassSurface
import com.knoux.kxprivatevpn.designsystem.KxPageHeader
import com.knoux.kxprivatevpn.platform.usage.DailyUsage
import com.knoux.kxprivatevpn.platform.usage.ServerUsage
import com.knoux.kxprivatevpn.platform.usage.StatsRange
import com.knoux.kxprivatevpn.platform.usage.UsageAggregator
import com.knoux.kxprivatevpn.platform.usage.UsageRecord
import com.knoux.kxprivatevpn.platform.usage.UsageSummary
import java.text.DateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class StatisticsUiState(
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val saveFailed: Boolean = false,
    val clearFailed: Boolean = false,
    val clearing: Boolean = false,
    val confirmClear: Boolean = false,
    val range: StatsRange = StatsRange.WEEK,
    val records: List<UsageRecord> = emptyList(),
    val summary: UsageSummary = UsageAggregator.summarize(
        emptyList(), StatsRange.WEEK, System.currentTimeMillis(),
    ),
)

@Composable
fun StatisticsRoute(
    model: ClientViewModel,
    night: Boolean,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val state by model.statsState.collectAsStateWithLifecycle()
    StatisticsScreen(
        night = night,
        state = state,
        onBack = onBack,
        onRefresh = model::refreshStats,
        onRangeChange = model::selectStatsRange,
        onOpenClear = model::openClearStats,
        onDismissClear = model::dismissClearStats,
        onConfirmClear = model::confirmClearStats,
        onNavigate = onNavigate,
    )
}

/**
 * Only measured values reach this screen: WireGuard RX/TX counters, monotonic session duration,
 * and the calendar day each session started. Unsupported reference cards (ping, privacy score)
 * are not reproduced — the same slots show sessions and connection time instead.
 */
@Composable
fun StatisticsScreen(
    night: Boolean,
    state: StatisticsUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRangeChange: (StatsRange) -> Unit,
    onOpenClear: () -> Unit,
    onDismissClear: () -> Unit,
    onConfirmClear: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val muted = if (night) Color(0xFFC8C5E9) else Color(0xFF4B477F)
    val titleColor = if (night) Color(0xFFF6F1FF) else Color(0xFF171050)
    val accent = if (night) Color(0xFF79E7FF) else Color(0xFF5B3AE8)

    Box(Modifier.fillMaxSize().testTag("stats-screen")) {
        Atmosphere(night, 0f)
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .testTag("stats-scroll"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    KxPageHeader(
                        title = stringResource(R.string.stats_title),
                        night = night,
                        backLabel = stringResource(R.string.back),
                        onBack = onBack,
                    )
                    MenuButton(
                        night = night,
                        muted = muted,
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp)
                            .testTag("stats-menu"),
                        onClick = onOpenClear,
                    )
                }
                when {
                    state.loading -> LoadingPanel(muted)
                    state.loadFailed -> LoadFailedPanel(night, muted, onRefresh)
                    else -> {
                        if (state.saveFailed) Banner(
                            night,
                            stringResource(R.string.stats_save_failed),
                            Modifier.testTag("stats-save-error"),
                        )
                        if (state.clearFailed) Banner(
                            night,
                            stringResource(R.string.stats_clear_failed),
                            Modifier.testTag("stats-clear-error"),
                        )
                        StatGrid(state, night, muted, titleColor)
                        UsageChartCard(state, night, muted, titleColor, accent, onRangeChange)
                        SpeedAndTimeRow(state, night, muted, titleColor, accent)
                        ServerHistoryCard(state, night, muted, titleColor)
                        Text(
                            stringResource(R.string.stats_note),
                            color = muted.copy(alpha = .8f),
                            fontSize = 10.5.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(horizontal = 24.dp)
                                .testTag("stats-note"),
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
            StatsBottomBar(night, muted, titleColor, accent, onNavigate)
        }
        if (state.confirmClear) {
            ClearHistoryDialog(night, state, onDismissClear, onConfirmClear)
        }
    }
}

@Composable
private fun StatGrid(
    state: StatisticsUiState,
    night: Boolean,
    muted: Color,
    titleColor: Color,
) {
    val summary = state.summary
    val context = LocalContext.current
    val noSessions = stringResource(R.string.stats_no_sessions)
    val range = rangeLabel(state.range)
    val cards = listOf(
        StatSpec(
            GlyphKind.DATABASE,
            label = stringResource(R.string.stats_total_data),
            value = if (summary.hasAnyRecords) bytesText(context, summary.totalBytes) else noSessions,
            hint = if (summary.hasAnyRecords) comparisonText(context, summary) else range,
            tag = "stats-total", valueTag = "stats-total-value",
        ),
        StatSpec(
            GlyphKind.SPEED,
            label = stringResource(R.string.stats_average_speed),
            value = rateText(context, summary.averageBytesPerSecond),
            hint = if (summary.sessions > 0)
                stringResource(R.string.stats_speed_hint, summary.sessions) else noSessions,
            tag = "stats-speed", valueTag = "stats-speed-value",
        ),
        StatSpec(
            GlyphKind.SERVER,
            label = stringResource(R.string.stats_most_used),
            value = summary.mostUsed?.label ?: stringResource(R.string.stats_no_data),
            hint = summary.mostUsed?.let {
                stringResource(R.string.stats_server_hint, (it.shareOfDuration * 100).roundToInt())
            } ?: noSessions,
            tag = "stats-server", valueTag = "stats-server-value",
        ),
        StatSpec(
            GlyphKind.CLOCK,
            label = stringResource(R.string.stats_connection_time),
            value = if (summary.sessions > 0) durationText(context, summary.durationMillis)
            else stringResource(R.string.stats_no_data),
            hint = if (summary.sessions > 0) stringResource(R.string.stats_time_hint) else noSessions,
            tag = "stats-time", valueTag = "stats-time-value",
        ),
    )
    // At large font scales a half-width card cannot fit a full value, so stack the cards.
    val columns = if (LocalDensity.current.fontScale >= 1.3f) 1 else 2
    Column(
        Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        cards.chunked(columns).forEach { rowCards ->
            Row(
                Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowCards.forEach { card ->
                    StatCard(
                        night, card.glyph,
                        label = card.label, value = card.value, hint = card.hint,
                        tag = card.tag, valueTag = card.valueTag,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

private data class StatSpec(
    val glyph: GlyphKind,
    val label: String,
    val value: String,
    val hint: String,
    val tag: String,
    val valueTag: String,
)

@Composable
private fun StatCard(
    night: Boolean,
    glyph: GlyphKind,
    label: String,
    value: String,
    hint: String,
    tag: String,
    valueTag: String,
    modifier: Modifier = Modifier,
) {
    val muted = if (night) Color(0xFFC8C5E9) else Color(0xFF4B477F)
    val titleColor = if (night) Color(0xFFF6F1FF) else Color(0xFF171050)
    GlassSurface(
        night = night,
        modifier = modifier.testTag(tag),
        radius = 22.dp,
        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphPanel(night, glyph, 36.dp)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = muted, fontSize = 10.5.sp, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
                Text(value, color = titleColor, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag(valueTag))
                Text(hint, color = muted.copy(alpha = .82f), fontSize = 9.sp, lineHeight = 12.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun UsageChartCard(
    state: StatisticsUiState,
    night: Boolean,
    muted: Color,
    titleColor: Color,
    accent: Color,
    onRangeChange: (StatsRange) -> Unit,
) {
    GlassSurface(
        night, Modifier.padding(horizontal = 20.dp).testTag("stats-chart-card"),
        radius = 24.dp, contentPadding = PaddingValues(13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatsGlyph(GlyphKind.CHART, titleColor, Modifier.size(19.dp))
            Spacer(Modifier.width(7.dp))
            Text(stringResource(R.string.stats_daily_usage), color = titleColor, fontSize = 14.sp,
                fontWeight = FontWeight.Bold)
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.End),
        ) {
            StatsRange.entries.forEach { range ->
                RangeChip(range, state.range == range, muted, accent) { onRangeChange(range) }
            }
        }
        val chartDescription = stringResource(
            R.string.stats_chart_desc,
            bytesText(LocalContext.current, state.summary.totalBytes),
            state.summary.sessions,
        )
        Box(
            Modifier.fillMaxWidth().height(172.dp).padding(top = 4.dp)
                .semantics { contentDescription = chartDescription },
        ) {
            UsageChart(state.summary, night, muted, accent, Modifier.fillMaxSize().testTag("stats-chart"))
            if (!state.summary.hasRangeData) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.stats_chart_empty),
                        color = muted, fontSize = 11.5.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 28.dp).testTag("stats-chart-empty"),
                    )
                }
            }
        }
    }
}

@Composable
private fun RangeChip(
    range: StatsRange,
    selected: Boolean,
    muted: Color,
    accent: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier.sizeIn(minWidth = 44.dp, minHeight = 40.dp).clip(shape)
            .background(if (selected) accent else Color.White.copy(alpha = .22f), shape)
            .border(1.dp, if (selected) accent else Color.White.copy(alpha = .5f), shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .testTag("stats-range-${range.days}"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            rangeLabel(range),
            color = if (selected) if (selectedColorIsDark(accent)) Color.White else Color(0xFF171050)
            else muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun selectedColorIsDark(color: Color): Boolean =
    (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue) < .6

@Composable
private fun UsageChart(
    summary: UsageSummary,
    night: Boolean,
    muted: Color,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val labelStyle = TextStyle(color = muted, fontSize = 9.sp)
    Canvas(modifier) {
        val left = 44.dp.toPx()
        val right = 4.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = 20.dp.toPx()
        val chartWidth = size.width - left - right
        val chartHeight = size.height - top - bottom
        if (chartWidth <= 0 || chartHeight <= 0) return@Canvas

        val hasData = summary.hasRangeData
        val values = summary.buckets.map { it.bytes.toFloat() }
        val maxValue = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val gridColor = muted.copy(alpha = if (night) .18f else .22f)

        repeat(4) { index ->
            val fraction = index / 3f
            val y = top + chartHeight * (1f - fraction)
            drawLine(gridColor, Offset(left, y), Offset(left + chartWidth, y),
                strokeWidth = 1.dp.toPx())
            // An empty chart gets one zero label; measured charts get real axis values.
            if (hasData && (index == 0 || index == 3 || maxValue >= 3f)) {
                // A leading LTR mark keeps "4 MB" number-first inside an RTL paragraph.
                val label = "\u200E" + bytesShort((maxValue * fraction).toLong())
                val layout = measurer.measure(AnnotatedString(label), style = labelStyle)
                drawText(layout, topLeft = Offset(0f, y - layout.size.height / 2f))
            } else if (!hasData && index == 0) {
                val layout = measurer.measure(AnnotatedString("\u200E0 B"), style = labelStyle)
                drawText(layout, topLeft = Offset(0f, y - layout.size.height / 2f))
            }
        }

        // No line, area, or dots may imply measurement on an empty history.
        if (!hasData) return@Canvas
        if (values.isEmpty()) return@Canvas
        val step = if (values.size == 1) 0f else chartWidth / (values.size - 1)
        fun point(index: Int): Offset {
            val rawX = left + step * index
            val x = rawX.coerceIn(left, left + chartWidth)
            val y = top + chartHeight * (1f - values[index] / maxValue)
            return Offset(x, y)
        }

        if (values.size > 1) {
            val area = Path().apply {
                val first = point(0)
                moveTo(first.x, top + chartHeight)
                lineTo(first.x, first.y)
                for (index in 1 until values.size) {
                    val p = point(index)
                    lineTo(p.x, p.y)
                }
                val last = point(values.lastIndex)
                lineTo(last.x, top + chartHeight)
                close()
            }
            drawPath(
                area,
                Brush.verticalGradient(
                    listOf(accent.copy(alpha = .42f), accent.copy(alpha = .03f)),
                    startY = top, endY = top + chartHeight,
                ),
            )
            val line = Path().apply {
                val first = point(0)
                moveTo(first.x, first.y)
                for (index in 1 until values.size) {
                    val p = point(index)
                    lineTo(p.x, p.y)
                }
            }
            drawPath(line, accent, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round))
        }
        if (values.size <= 14) {
            values.indices.forEach { index ->
                val p = point(index)
                drawCircle(if (night) Color(0xFFF6F1FF) else Color.White, 3.2.dp.toPx(), p)
                drawCircle(accent, 2.dp.toPx(), p)
            }
        }

        val labelEvery = when {
            values.size <= 7 -> 1
            values.size <= 13 -> 2
            else -> 5
        }
        values.indices.filter { it % labelEvery == 0 || it == values.lastIndex }.forEach { index ->
            val bucket = summary.buckets[index]
            val date = LocalDate.ofEpochDay(bucket.startEpochDay)
            val text = when {
                summary.range == StatsRange.WEEK ->
                    DateTimeFormatter.ofPattern("EEE", locale).format(date)
                summary.range == StatsRange.MONTH ->
                    DateTimeFormatter.ofPattern("d", locale).format(date)
                else -> DateTimeFormatter.ofPattern("MMM", locale).format(date)
            }
            val layout = measurer.measure(AnnotatedString(text), style = labelStyle)
            val p = point(index)
            val x = (p.x - layout.size.width / 2f).coerceIn(left, left + chartWidth - layout.size.width)
            drawText(layout, topLeft = Offset(x, top + chartHeight + 5.dp.toPx()))
        }
    }
}

@Composable
private fun SpeedAndTimeRow(
    state: StatisticsUiState,
    night: Boolean,
    muted: Color,
    titleColor: Color,
    accent: Color,
) {
    val context = LocalContext.current
    val summary = state.summary
    Column(
        Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GlassSurface(night, Modifier.testTag("stats-speed-card"), radius = 24.dp,
            contentPadding = PaddingValues(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatsGlyph(GlyphKind.SPEED, titleColor, Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text(stringResource(R.string.stats_average_speed), color = titleColor,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                rateText(context, summary.averageBytesPerSecond),
                color = titleColor, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.testTag("stats-speed-total"),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RateChip("↓", rateText(context, summary.averageRxBytesPerSecond), muted,
                    Modifier.testTag("stats-speed-rx"))
                RateChip("↑", rateText(context, summary.averageTxBytesPerSecond), muted,
                    Modifier.testTag("stats-speed-tx"))
            }
            Text(stringResource(R.string.stats_speed_note), color = muted.copy(alpha = .8f),
                fontSize = 9.5.sp, lineHeight = 13.sp)
        }
        GlassSurface(night, Modifier.testTag("stats-donut-card"), radius = 24.dp,
            contentPadding = PaddingValues(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatsGlyph(GlyphKind.CLOCK, titleColor, Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text(stringResource(R.string.stats_connection_time), color = titleColor,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val top = summary.mostUsed
                Box(Modifier.size(96.dp).testTag("stats-donut")
                    .semantics {
                        contentDescription = context.getString(
                            R.string.stats_donut_desc,
                            top?.label ?: context.getString(R.string.stats_no_data),
                        )
                    },
                    contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = 11.dp.toPx()
                        val inset = stroke / 2f
                        drawArc(
                            muted.copy(alpha = .22f), 0f, 360f, false,
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - stroke, size.height - stroke),
                            style = Stroke(stroke, cap = StrokeCap.Round),
                        )
                        if (summary.durationMillis > 0 && top != null) {
                            drawArc(
                                accent, -90f, (top.shareOfDuration * 360f).toFloat().coerceIn(0f, 360f),
                                false,
                                topLeft = Offset(inset, inset),
                                size = Size(size.width - stroke, size.height - stroke),
                                style = Stroke(stroke, cap = StrokeCap.Round),
                            )
                        }
                    }
                    if (top != null) {
                        Text(
                            "${(top.shareOfDuration * 100).roundToInt()}%",
                            color = titleColor, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        if (summary.sessions > 0) durationText(context, summary.durationMillis)
                        else stringResource(R.string.stats_no_data),
                        color = titleColor,
                        fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.testTag("stats-time-total"),
                    )
                    if (top != null) {
                        LegendRow(accent, top.label, durationText(context, top.durationMillis), muted)
                        LegendRow(
                            muted.copy(alpha = .5f),
                            stringResource(R.string.stats_other_servers),
                            durationText(
                                context,
                                (summary.durationMillis - top.durationMillis).coerceAtLeast(0),
                            ),
                            muted,
                        )
                    } else {
                        Text(stringResource(R.string.stats_no_sessions), color = muted,
                            fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RateChip(prefix: String, text: String, muted: Color, modifier: Modifier = Modifier) {
    Row(
        modifier.background(Color.White.copy(alpha = .22f), RoundedCornerShape(50))
            .border(1.dp, Color.White.copy(alpha = .45f), RoundedCornerShape(50))
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$prefix ", color = muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(text, color = muted, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun LegendRow(dot: Color, label: String, value: String, muted: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(5.dp))
        Text(label, color = muted, fontSize = 9.5.sp, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(value, color = muted, fontSize = 9.5.sp, maxLines = 1)
    }
}

@Composable
private fun ServerHistoryCard(
    state: StatisticsUiState,
    night: Boolean,
    muted: Color,
    titleColor: Color,
) {
    val context = LocalContext.current
    GlassSurface(night, Modifier.padding(horizontal = 20.dp).testTag("stats-server-history"),
        radius = 24.dp, contentPadding = PaddingValues(13.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatsGlyph(GlyphKind.SERVER, titleColor, Modifier.size(19.dp))
            Spacer(Modifier.width(7.dp))
            Text(stringResource(R.string.stats_server_history), color = titleColor,
                fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(rangeLabel(state.range), color = muted, fontSize = 10.sp,
                modifier = Modifier.background(Color.White.copy(alpha = .22f), RoundedCornerShape(50))
                    .padding(horizontal = 9.dp, vertical = 3.dp))
        }
        if (state.summary.servers.isEmpty()) {
            Text(stringResource(R.string.stats_no_servers), color = muted, fontSize = 11.sp,
                modifier = Modifier.padding(vertical = 6.dp).testTag("stats-no-servers"))
        } else {
            state.summary.servers.take(5).forEach { server ->
                ServerRow(server, muted, titleColor, context)
            }
        }
    }
}

@Composable
private fun ServerRow(
    server: ServerUsage,
    muted: Color,
    titleColor: Color,
    context: Context,
) {
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp)
            .testTag("stats-server-row-${server.key}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(32.dp).clip(CircleShape)
                .background(Color.White.copy(alpha = .28f))
                .border(1.dp, Color.White.copy(alpha = .55f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { StatsGlyph(GlyphKind.SERVER, titleColor, Modifier.size(16.dp)) }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(server.label, color = titleColor, fontSize = 12.5.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(
                    R.string.stats_last_used,
                    DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
                        .format(Date(server.lastStartedAtMillis)),
                ),
                color = muted.copy(alpha = .85f), fontSize = 9.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(durationText(context, server.durationMillis), color = titleColor,
                fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
            Text(bytesText(context, server.bytes), color = muted, fontSize = 9.sp)
        }
        Spacer(Modifier.width(7.dp))
        Sparkline(server.trend, muted, Modifier.size(width = 44.dp, height = 22.dp))
    }
}

@Composable
private fun Sparkline(trend: List<Long>, muted: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (trend.isEmpty()) return@Canvas
        val max = trend.maxOrNull()?.coerceAtLeast(1L) ?: 1L
        val step = if (trend.size == 1) 0f else size.width / (trend.size - 1)
        val path = Path()
        trend.forEachIndexed { index, value ->
            val x = step * index
            val y = size.height - (value.toFloat() / max) * (size.height - 2.dp.toPx()) - 1.dp.toPx()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, muted.copy(alpha = .8f),
            style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun StatsBottomBar(
    night: Boolean,
    muted: Color,
    titleColor: Color,
    accent: Color,
    onNavigate: (String) -> Unit,
) {
    GlassSurface(
        night,
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp).testTag("stats-nav"),
        radius = 26.dp,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            NavItem(GlyphKind.HOME, stringResource(R.string.stats_nav_home), false, muted,
                titleColor, accent, Modifier.weight(1f).testTag("stats-nav-home")) {
                onNavigate("home")
            }
            NavItem(GlyphKind.SERVER, stringResource(R.string.stats_nav_servers), false, muted,
                titleColor, accent, Modifier.weight(1f).testTag("stats-nav-servers")) {
                onNavigate("servers")
            }
            NavItem(GlyphKind.CHART, stringResource(R.string.stats_nav_statistics), true, muted,
                titleColor, accent, Modifier.weight(1f).testTag("stats-nav-stats")) {}
            NavItem(GlyphKind.SHIELD, stringResource(R.string.stats_nav_security), false, muted,
                titleColor, accent, Modifier.weight(1f).testTag("stats-nav-security")) {
                onNavigate("security")
            }
            NavItem(GlyphKind.PROFILE, stringResource(R.string.stats_nav_profile), false, muted,
                titleColor, accent, Modifier.weight(1f).testTag("stats-nav-profile")) {
                onNavigate("settings")
            }
        }
    }
}

@Composable
private fun NavItem(
    glyph: GlyphKind,
    label: String,
    selected: Boolean,
    muted: Color,
    titleColor: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(18.dp))
            .then(if (selected) Modifier.background(accent.copy(alpha = .22f)) else Modifier)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StatsGlyph(glyph, if (selected) titleColor else muted, Modifier.size(18.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, color = if (selected) titleColor else muted, fontSize = 8.5.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MenuButton(
    night: Boolean,
    muted: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val label = stringResource(R.string.stats_clear_menu)
    val rim = Color.White.copy(alpha = if (night) .38f else .85f)
    Box(
        modifier.size(44.dp).clip(CircleShape)
            .background(
                Brush.linearGradient(
                    if (night) listOf(Color.White.copy(alpha = .14f),
                        Color(0xFF6B62E8).copy(alpha = .22f))
                    else listOf(Color.White.copy(alpha = .62f),
                        Color(0xFFB6C9FF).copy(alpha = .34f)),
                ),
            )
            .border(1.dp, rim, CircleShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(18.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outer = size.minDimension * .30f
            val inner = size.minDimension * .12f
            drawCircle(muted, outer, center, style = Stroke(1.8.dp.toPx()))
            drawCircle(muted, inner, center, style = Stroke(1.8.dp.toPx()))
            repeat(8) { index ->
                rotate(index * 45f, center) {
                    drawLine(muted, Offset(center.x, center.y - outer),
                        Offset(center.x, center.y - outer - size.minDimension * .16f),
                        strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                }
            }
        }
    }
}

@Composable
private fun GlyphPanel(night: Boolean, glyph: GlyphKind, size: androidx.compose.ui.unit.Dp) {
    val titleColor = if (night) Color(0xFFF6F1FF) else Color(0xFF171050)
    Box(
        Modifier.size(size).clip(CircleShape)
            .background(
                Brush.linearGradient(
                    if (night) listOf(Color(0xFF6B62E8).copy(alpha = .3f),
                        Color(0xFF54D8FF).copy(alpha = .18f))
                    else listOf(Color(0xFFE7D9FF).copy(alpha = .9f),
                        Color(0xFFC6CFFF).copy(alpha = .7f)),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = .55f), CircleShape),
        contentAlignment = Alignment.Center,
    ) { StatsGlyph(glyph, titleColor, Modifier.size(size * .52f)) }
}

private enum class GlyphKind { DATABASE, SPEED, SERVER, CLOCK, CHART, SHIELD, HOME, PROFILE }

@Composable
private fun StatsGlyph(kind: GlyphKind, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { contentDescription = "" }) {
        val stroke = size.minDimension * .10f
        when (kind) {
            GlyphKind.DATABASE -> {
                drawArc(color, 180f, 180f, false,
                    topLeft = Offset(size.width * .22f, size.height * .06f),
                    size = Size(size.width * .56f, size.height * .3f),
                    style = Stroke(stroke))
                drawLine(color, Offset(size.width * .22f, size.height * .21f),
                    Offset(size.width * .22f, size.height * .79f), stroke, StrokeCap.Round)
                drawLine(color, Offset(size.width * .78f, size.height * .21f),
                    Offset(size.width * .78f, size.height * .79f), stroke, StrokeCap.Round)
                drawArc(color, 20f, 140f, false,
                    topLeft = Offset(size.width * .22f, size.height * .64f),
                    size = Size(size.width * .56f, size.height * .3f),
                    style = Stroke(stroke))
                drawArc(color, 30f, 120f, false,
                    topLeft = Offset(size.width * .22f, size.height * .45f),
                    size = Size(size.width * .56f, size.height * .3f),
                    style = Stroke(stroke))
            }
            GlyphKind.SPEED -> {
                drawArc(color, 160f, 220f, false,
                    topLeft = Offset(size.width * .08f, size.height * .12f),
                    size = Size(size.width * .84f, size.height * .84f),
                    style = Stroke(stroke * 1.2f, cap = StrokeCap.Round))
                val center = Offset(size.width * .5f, size.height * .55f)
                drawLine(color, center, Offset(size.width * .72f, size.height * .32f),
                    stroke * 1.3f, StrokeCap.Round)
                drawCircle(color, size.width * .09f, center)
            }
            GlyphKind.SERVER -> {
                listOf(.18f, .56f).forEach { y ->
                    drawRoundRect(color, Offset(size.width * .12f, size.height * y),
                        Size(size.width * .76f, size.height * .26f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * .08f),
                        style = Stroke(stroke))
                    drawCircle(color, size.width * .04f,
                        Offset(size.width * .27f, size.height * (y + .13f)))
                    drawLine(color, Offset(size.width * .38f, size.height * (y + .13f)),
                        Offset(size.width * .72f, size.height * (y + .13f)),
                        stroke, StrokeCap.Round)
                }
            }
            GlyphKind.CLOCK -> {
                drawCircle(color, size.minDimension * .38f,
                    Offset(size.width * .5f, size.height * .5f), style = Stroke(stroke))
                drawLine(color, Offset(size.width * .5f, size.height * .5f),
                    Offset(size.width * .5f, size.height * .28f), stroke, StrokeCap.Round)
                drawLine(color, Offset(size.width * .5f, size.height * .5f),
                    Offset(size.width * .68f, size.height * .58f), stroke, StrokeCap.Round)
            }
            GlyphKind.CHART -> {
                val base = size.height * .86f
                listOf(.22f to .46f, .43f to .30f, .64f to .54f, .84f to .18f).forEach { (x, h) ->
                    drawLine(color, Offset(size.width * x, base),
                        Offset(size.width * x, size.height * h),
                        stroke * 1.5f, StrokeCap.Round)
                }
            }
            GlyphKind.SHIELD -> {
                drawArc(color, 200f, 140f, false,
                    topLeft = Offset(size.width * .16f, size.height * .10f),
                    size = Size(size.width * .68f, size.height * .68f),
                    style = Stroke(stroke))
                drawLine(color, Offset(size.width * .16f, size.height * .48f),
                    Offset(size.width * .5f, size.height * .90f), stroke, StrokeCap.Round)
                drawLine(color, Offset(size.width * .84f, size.height * .48f),
                    Offset(size.width * .5f, size.height * .90f), stroke, StrokeCap.Round)
            }
            GlyphKind.HOME -> {
                drawLine(color, Offset(size.width * .12f, size.height * .48f),
                    Offset(size.width * .5f, size.height * .12f), stroke, StrokeCap.Round)
                drawLine(color, Offset(size.width * .5f, size.height * .12f),
                    Offset(size.width * .88f, size.height * .48f), stroke, StrokeCap.Round)
                drawLine(color, Offset(size.width * .22f, size.height * .44f),
                    Offset(size.width * .22f, size.height * .88f), stroke, StrokeCap.Round)
                drawLine(color, Offset(size.width * .78f, size.height * .44f),
                    Offset(size.width * .78f, size.height * .88f), stroke, StrokeCap.Round)
                drawLine(color, Offset(size.width * .22f, size.height * .88f),
                    Offset(size.width * .78f, size.height * .88f), stroke, StrokeCap.Round)
            }
            GlyphKind.PROFILE -> {
                drawCircle(color, size.minDimension * .17f,
                    Offset(size.width * .5f, size.height * .30f), style = Stroke(stroke))
                drawArc(color, 200f, 140f, false,
                    topLeft = Offset(size.width * .20f, size.height * .48f),
                    size = Size(size.width * .60f, size.height * .42f),
                    style = Stroke(stroke))
            }
        }
    }
}

@Composable
private fun LoadingPanel(muted: Color) {
    Column(Modifier.padding(horizontal = 20.dp).testTag("stats-loading")) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.stats_loading), color = muted, fontSize = 12.sp)
    }
}

@Composable
private fun LoadFailedPanel(night: Boolean, muted: Color, onRefresh: () -> Unit) {
    GlassSurface(night, Modifier.padding(horizontal = 20.dp).testTag("stats-error"), radius = 22.dp) {
        Text(stringResource(R.string.stats_load_failed), color = muted, fontSize = 12.sp)
        OutlinedButton(onClick = onRefresh,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("stats-retry")) {
            Text(stringResource(R.string.stats_retry))
        }
    }
}

@Composable
private fun Banner(night: Boolean, text: String, modifier: Modifier = Modifier) {
    GlassSurface(
        night = night,
        modifier = modifier.padding(horizontal = 20.dp),
        radius = 18.dp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Text(text, color = if (night) Color(0xFFFF9DB4) else Color(0xFFC23A5E), fontSize = 11.5.sp)
    }
}

@Composable
private fun ClearHistoryDialog(
    night: Boolean,
    state: StatisticsUiState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val title = stringResource(R.string.stats_clear_title)
    // Rendered inside the screen instead of a separate dialog window: Compose cannot capture
    // dialog windows below API 28, and the in-screen modal keeps dialog evidence capturable.
    BackHandler(enabled = !state.clearing) { onDismiss() }
    Box(
        Modifier.fillMaxSize().background(Color(0x66000000))
            .semantics { paneTitle = title }
            .testTag("stats-clear-dialog"),
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(
            night, Modifier.padding(horizontal = 28.dp), radius = 28.dp,
            contentPadding = PaddingValues(22.dp),
        ) {
            Text(title, color = if (night) Color(0xFFF6F1FF) else Color(0xFF171050),
                fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.stats_clear_body),
                color = if (night) Color(0xFFC8C5E9) else Color(0xFF4B477F),
                fontSize = 13.sp, lineHeight = 19.sp,
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss, enabled = !state.clearing,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("stats-clear-cancel")) {
                    Text(stringResource(R.string.stats_clear_cancel))
                }
                TextButton(onClick = onConfirm, enabled = !state.clearing,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("stats-clear-confirm")) {
                    Text(stringResource(
                        if (state.clearing) R.string.stats_clearing else R.string.stats_clear_confirm,
                    ))
                }
            }
        }
    }
}

@Composable
private fun rangeLabel(range: StatsRange): String = stringResource(
    when (range) {
        StatsRange.WEEK -> R.string.stats_range_7d
        StatsRange.MONTH -> R.string.stats_range_30d
        StatsRange.QUARTER -> R.string.stats_range_3m
        StatsRange.YEAR -> R.string.stats_range_1y
    },
)

private fun bytesText(context: Context, bytes: Long): String =
    Formatter.formatFileSize(context, bytes.coerceAtLeast(0))

private fun bytesShort(bytes: Long): String {
    val value = bytes.coerceAtLeast(0).toDouble()
    // One decimal below 10 units keeps adjacent axis labels distinguishable (2.5 vs 1.7 MB).
    fun scaled(divisor: Double, unit: String): String {
        val scaled = value / divisor
        return if (scaled < 10) String.format(Locale.US, "%.1f %s", scaled, unit)
        else String.format(Locale.US, "%.0f %s", scaled, unit)
    }
    return when {
        value >= 1_073_741_824 -> scaled(1_073_741_824.0, "GB")
        value >= 1_048_576 -> scaled(1_048_576.0, "MB")
        value >= 1_024 -> String.format(Locale.US, "%.0f KB", value / 1_024)
        else -> "${bytes.coerceAtLeast(0)} B"
    }
}

private fun durationText(context: Context, millis: Long): String {
    val minutes = millis.coerceAtLeast(0) / 60_000
    return when {
        minutes >= 60 -> context.getString(
            R.string.stats_duration_hm, minutes / 60, (minutes % 60).toInt(),
        )
        minutes >= 1 -> context.getString(R.string.stats_duration_m, minutes.toInt())
        else -> context.getString(R.string.stats_under_minute)
    }
}

private fun rateText(context: Context, bytesPerSecond: Long?): String {
    if (bytesPerSecond == null) return context.getString(R.string.stats_no_data)
    val bits = bytesPerSecond.coerceAtLeast(0) * 8.0
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    return when {
        bits >= 1_000_000 -> context.getString(
            R.string.stats_rate_mbps, String.format(locale, "%.1f", bits / 1_000_000),
        )
        bits >= 1_000 -> context.getString(
            R.string.stats_rate_kbps, String.format(locale, "%.0f", bits / 1_000),
        )
        else -> context.getString(R.string.stats_rate_bps, String.format(locale, "%.0f", bits))
    }
}

private fun comparisonText(context: Context, summary: UsageSummary): String {
    val previous = summary.previousTotalBytes
        ?: return context.getString(R.string.stats_compare_none)
    if (previous <= 0) {
        return when {
            summary.previousSessions > 0 ->
                context.getString(R.string.stats_compare_previous_empty)
            summary.totalBytes > 0 -> context.getString(R.string.stats_compare_new)
            else -> context.getString(R.string.stats_compare_same)
        }
    }
    val delta = ((summary.totalBytes - previous) * 100.0 / previous).roundToInt()
    return when {
        delta > 0 -> context.getString(R.string.stats_compare_up, delta)
        delta < 0 -> context.getString(R.string.stats_compare_down, -delta)
        else -> context.getString(R.string.stats_compare_same)
    }
}
