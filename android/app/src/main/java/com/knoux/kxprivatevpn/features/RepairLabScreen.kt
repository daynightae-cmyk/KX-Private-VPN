package com.knoux.kxprivatevpn.features

import android.content.Context
import android.text.format.Formatter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.compose.ui.platform.LocalConfiguration
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
import com.knoux.kxprivatevpn.designsystem.KxOrb
import com.knoux.kxprivatevpn.designsystem.KxPageHeader
import com.knoux.kxprivatevpn.designsystem.RepairGlyph
import com.knoux.kxprivatevpn.designsystem.repairAccent
import com.knoux.kxprivatevpn.designsystem.repairAccentPair
import com.knoux.kxprivatevpn.launch.IntroTimeline
import com.knoux.kxprivatevpn.launch.rememberReducedMotion
import com.knoux.kxprivatevpn.platform.repair.RepairCatalog
import com.knoux.kxprivatevpn.platform.repair.RepairJournalEntry
import com.knoux.kxprivatevpn.platform.repair.RepairMeasurement
import com.knoux.kxprivatevpn.platform.repair.RepairOutcome
import com.knoux.kxprivatevpn.platform.repair.RepairService
import com.knoux.kxprivatevpn.platform.repair.RepairServiceId
import java.text.DateFormat
import java.util.Date
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun RepairLabRoute(
    night: Boolean,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val model: RepairLabViewModel = viewModel()
    val state by model.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { model.refreshJournal() }
    RepairLabScreen(
        night = night,
        state = state,
        onBack = onBack,
        onAnalyze = model::measure,
        onNavigate = onNavigate,
    )
}

/**
 * The Repair Lab dashboard renders only measured state. Cards for tools that are not built
 * in this slice stay visible but open an honest unavailable screen; they never invent scans,
 * savings or progress.
 */
@Composable
fun RepairLabScreen(
    night: Boolean,
    state: RepairUiState,
    onBack: () -> Unit,
    onAnalyze: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val foreground = if (night) Color(0xFFF7F3FF) else Color(0xFF171050)
    val reducedMotion = rememberReducedMotion()
    Box(Modifier.fillMaxSize()) {
        Atmosphere(night, 0f)
        Column(
            Modifier.fillMaxSize().safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .testTag("repair-lab"),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            KxPageHeader(
                title = stringResource(R.string.repair_title),
                night = night,
                backLabel = stringResource(R.string.back),
                onBack = onBack,
            )
            RepairHero(night, state.measuring, reducedMotion, foreground)
            SnapshotCard(night, state, onAnalyze)
            state.measurement?.let { measurement ->
                if (!state.loading) CleanupReview(night, measurement, onNavigate)
            }
            ServiceGrid(night, state.measurement, foreground, onNavigate)
            JournalCard(night, state.journal, foreground)
            Text(
                stringResource(R.string.repair_privacy),
                color = foreground.copy(alpha = .72f),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.testTag("repair-privacy"),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RepairHero(night: Boolean, measuring: Boolean, reducedMotion: Boolean, foreground: Color) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.fillMaxWidth().height(254.dp), contentAlignment = Alignment.Center) {
            DiagnosticRing(measuring, reducedMotion, Modifier.size(244.dp))
            KxOrb(1f, IntroTimeline.frame(1f), Modifier.size(172.dp))
        }
        Text(
            stringResource(R.string.repair_lab),
            color = if (night) Color(0xFF9BEFFF) else Color(0xFF3B2AA8),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.4.sp,
        )
        Text(
            stringResource(R.string.repair_tagline),
            color = foreground.copy(alpha = .84f),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Ten nodes, one per signature service. The arc sweeps once when a real measurement starts;
 * it is not a looping decoration, and reduced-motion users see the static ring.
 */
@Composable
private fun DiagnosticRing(measuring: Boolean, reducedMotion: Boolean, modifier: Modifier) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(measuring, reducedMotion) {
        if (measuring && !reducedMotion) {
            rotation.snapTo(0f)
            rotation.animateTo(360f, tween(1900, easing = LinearEasing))
        } else {
            rotation.snapTo(0f)
        }
    }
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val radius = min(size.width, size.height) / 2f - 4.dp.toPx()
        drawCircle(
            Brush.sweepGradient(listOf(Color.Transparent, Color.White.copy(alpha = .30f),
                Color(0xFF64EEFF).copy(alpha = .55f), Color(0xFFAD71FF).copy(alpha = .55f),
                Color.Transparent), c),
            radius, c, style = Stroke(1.2.dp.toPx()),
        )
        drawCircle(Color.White.copy(alpha = .14f), radius * .80f, c,
            style = Stroke(.8.dp.toPx()))
        val services = RepairCatalog.services
        services.forEachIndexed { index, service ->
            val angle = Math.toRadians(-90.0 + index * 360.0 / services.size)
            val point = Offset(c.x + cos(angle).toFloat() * radius,
                c.y + sin(angle).toFloat() * radius)
            val accent = repairAccent(service.id)
            drawCircle(accent.copy(alpha = if (service.id.available) .85f else .32f),
                4.5.dp.toPx(), point)
            drawCircle(Color.White.copy(alpha = if (service.id.available) .9f else .35f),
                1.6.dp.toPx(), point)
            val nextAngle = Math.toRadians(-90.0 + (index + 1) * 360.0 / services.size)
            val next = Offset(c.x + cos(nextAngle).toFloat() * radius,
                c.y + sin(nextAngle).toFloat() * radius)
            drawLine(accent.copy(alpha = .18f), point, next, .8.dp.toPx())
        }
        if (rotation.value > 0f) {
            drawArc(
                Brush.sweepGradient(listOf(Color.Transparent, Color(0xFF64EEFF),
                    Color(0xFFAD71FF), Color.Transparent), c),
                startAngle = rotation.value, sweepAngle = 86f, useCenter = false,
                topLeft = Offset(c.x - radius, c.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun SnapshotCard(night: Boolean, state: RepairUiState, onAnalyze: () -> Unit) {
    val context = LocalContext.current
    val foreground = if (night) Color(0xFFF7F3FF) else Color(0xFF171050)
    val measurement = state.measurement
    GlassSurface(night) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.repair_snapshot), fontWeight = FontWeight.Bold,
                fontSize = 19.sp, color = foreground, modifier = Modifier.weight(1f))
            StatusPill(
                label = stringResource(when {
                    state.loading || state.measuring -> R.string.repair_analyzing
                    measurement != null -> R.string.repair_status_measured
                    else -> R.string.repair_never_measured
                }),
                accent = Color(0xFF64EEFF),
                night = night,
            )
        }
        if (state.loading || state.measuring) {
            LinearProgressIndicator(Modifier.fillMaxWidth().testTag("repair-progress"))
        }
        if (measurement == null) {
            Text(
                stringResource(if (state.failure) R.string.repair_measure_failed
                else R.string.repair_never_measured),
                color = foreground.copy(alpha = .82f), fontSize = 13.sp,
                modifier = Modifier.testTag("repair-no-measurement"),
            )
        } else {
            if (measurement.volumeMeasured) {
                val total = measurement.totalBytes!!.coerceAtLeast(0)
                val free = measurement.availableBytes!!.coerceAtLeast(0)
                val used = (total - free).coerceIn(0, total)
                val fraction = if (total > 0) used.toFloat() / total else 0f
                Text(
                    stringResource(R.string.repair_storage_free,
                        Formatter.formatFileSize(context, free),
                        Formatter.formatFileSize(context, total)),
                    color = foreground, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                    modifier = Modifier.testTag("repair-volume"),
                )
                Box(Modifier.fillMaxWidth().height(9.dp)
                    .clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .18f))) {
                    Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(
                        Brush.horizontalGradient(listOf(Color(0xFFAD71FF), Color(0xFF64EEFF)))))
                }
                Text(
                    stringResource(R.string.repair_storage_used,
                        Formatter.formatFileSize(context, used)),
                    color = foreground.copy(alpha = .7f), fontSize = 11.sp,
                )
            } else {
                Text(stringResource(R.string.repair_volume_missing), fontSize = 13.sp)
            }
            Text(stringResource(R.string.repair_kx_cache_heading), fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp, color = foreground.copy(alpha = .82f),
                modifier = Modifier.padding(top = 4.dp))
            if (measurement.cacheMeasured) {
                Text(
                    stringResource(R.string.repair_kx_cache_files, measurement.cacheFileCount,
                        Formatter.formatFileSize(context, measurement.cacheBytes!!)),
                    color = foreground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("repair-cache"),
                )
                if (measurement.cacheWalkLimited) {
                    Text(stringResource(R.string.repair_cache_preview_limited),
                        fontSize = 11.sp, color = foreground.copy(alpha = .7f))
                }
            } else {
                Text(stringResource(R.string.repair_cache_missing), fontSize = 13.sp)
            }
            val issues = buildList {
                if (!measurement.volumeMeasured) add(context.getString(R.string.repair_volume_missing))
                if (!measurement.cacheMeasured) add(context.getString(R.string.repair_cache_missing))
                if (measurement.previewFailed) add(context.getString(R.string.repair_preview_missing))
            }
            if (issues.isNotEmpty()) {
                Text(
                    stringResource(R.string.repair_measure_partial, issues.joinToString(" · ")),
                    fontSize = 11.sp, color = Color(0xFFFFB4B4),
                    modifier = Modifier.testTag("repair-partial"),
                )
            }
            Text(
                stringResource(R.string.repair_measured_at,
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(Date(measurement.atMillis))),
                fontSize = 11.sp, color = foreground.copy(alpha = .7f),
                modifier = Modifier.testTag("repair-measured-at"),
            )
        }
        GlassAction(
            label = stringResource(R.string.repair_analyze),
            modifier = Modifier.testTag("repair-analyze"),
            enabled = !state.measuring,
        ) { onAnalyze() }
        Text(
            stringResource(R.string.repair_measure_scope),
            fontSize = 11.sp, lineHeight = 16.sp, color = foreground.copy(alpha = .72f),
        )
        Text(
            stringResource(R.string.repair_permission_note),
            fontSize = 11.sp, lineHeight = 16.sp, color = foreground.copy(alpha = .72f),
            modifier = Modifier.testTag("repair-permission-note"),
        )
    }
}

@Composable
private fun CleanupReview(night: Boolean, measurement: RepairMeasurement, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val foreground = if (night) Color(0xFFF7F3FF) else Color(0xFF171050)
    val preview = measurement.preview
    GlassSurface(night) {
        Text(stringResource(R.string.repair_cleanup_title), fontWeight = FontWeight.Bold,
            fontSize = 18.sp, color = foreground)
        when {
            preview == null -> Text(
                stringResource(if (measurement.previewFailed) R.string.repair_cleanup_unavailable
                else R.string.repair_cleanup_empty),
                fontSize = 13.sp, color = foreground.copy(alpha = .8f),
            )
            preview.files.isEmpty() -> Text(stringResource(R.string.repair_cleanup_empty),
                fontSize = 13.sp, color = foreground.copy(alpha = .8f))
            else -> {
                Text(
                    stringResource(R.string.repair_cleanup_body, preview.files.size,
                        Formatter.formatFileSize(context, preview.files.sumOf { it.bytes })),
                    fontSize = 14.sp, color = foreground, modifier = Modifier.testTag("repair-cleanup"),
                )
                if (preview.limited) {
                    Text(stringResource(R.string.repair_cache_preview_limited),
                        fontSize = 11.sp, color = foreground.copy(alpha = .7f))
                }
                OutlinedButton(
                    onClick = { onNavigate(RepairServiceId.CACHE.route) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("repair-review-cache"),
                ) { Text(stringResource(R.string.repair_review_cache)) }
            }
        }
    }
}

@Composable
private fun ServiceGrid(
    night: Boolean,
    measurement: RepairMeasurement?,
    foreground: Color,
    onNavigate: (String) -> Unit,
) {
    val context = LocalContext.current
    val columns = if (LocalConfiguration.current.screenWidthDp >= 600) 3 else 2
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.repair_services), fontWeight = FontWeight.Bold,
            fontSize = 21.sp, color = foreground)
        Text(
            stringResource(R.string.repair_services_count,
                RepairCatalog.available(), RepairCatalog.services.size),
            fontSize = 12.sp, color = foreground.copy(alpha = .74f),
            modifier = Modifier.testTag("repair-services-count"),
        )
        Box(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Canvas(Modifier.matchParentSize()) {
                val spine = Path().apply {
                    moveTo(size.width * .5f, 0f)
                    cubicTo(size.width * .42f, size.height * .28f, size.width * .60f,
                        size.height * .62f, size.width * .5f, size.height)
                }
                drawPath(spine, Brush.linearGradient(listOf(
                    Color(0xFFAD71FF).copy(alpha = .38f),
                    Color(0xFF64EEFF).copy(alpha = .55f),
                    Color(0xFFF9ECFF).copy(alpha = .30f))),
                    style = Stroke(1.2.dp.toPx()))
                for (i in 1..6) {
                    val y = size.height * i / 7f
                    drawCircle(Color.White.copy(alpha = .30f), 2.dp.toPx(),
                        Offset(size.width * .5f, y))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RepairCatalog.services.chunked(columns).forEach { row ->
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { service ->
                            ServiceCard(
                                service = service,
                                measurement = measurement,
                                night = night,
                                foreground = foreground,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                onClick = { onNavigate(service.id.route) },
                                context = context,
                            )
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceCard(
    service: RepairService,
    measurement: RepairMeasurement?,
    night: Boolean,
    foreground: Color,
    modifier: Modifier,
    onClick: () -> Unit,
    context: Context,
) {
    val (accent, second) = repairAccentPair(service.id)
    val shape = RoundedCornerShape(22.dp)
    val available = service.id.available
    val status = if (available) stringResource(R.string.repair_status_available)
    else stringResource(R.string.repair_status_unavailable)
    val detail = when {
        !available -> stringResource(R.string.repair_status_unavailable)
        service.id == RepairServiceId.STORAGE && measurement?.volumeMeasured == true ->
            stringResource(R.string.repair_status_measured)
        service.id == RepairServiceId.CACHE && measurement?.cacheMeasured == true ->
            stringResource(R.string.repair_status_measured)
        else -> stringResource(R.string.repair_status_ready)
    }
    val title = stringResource(service.titleRes)
    Column(
        modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(
                Color.White.copy(alpha = if (night) .10f else .52f),
                accent.copy(alpha = if (night) .15f else .20f),
                second.copy(alpha = if (night) .08f else .14f))))
            .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = .46f),
                accent.copy(alpha = .42f), second.copy(alpha = .28f))), shape)
            .clickable(role = Role.Button,
                onClickLabel = context.getString(R.string.repair_open_service, title, status),
                onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "$title. $status. $detail" }
            .padding(13.dp)
            .testTag("repair-card-${service.id.key}"),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            RepairGlyph(service.id, Modifier.size(46.dp))
            Spacer(Modifier.weight(1f))
            StatusPill(
                label = if (available) stringResource(R.string.repair_status_available)
                else stringResource(R.string.repair_status_unavailable),
                accent = if (available) accent else Color(0xFF9A94C9).copy(alpha = .8f),
                night = night,
            )
        }
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 19.sp,
            color = foreground)
        Text(
            stringResource(service.descriptionRes),
            fontSize = 11.sp, lineHeight = 15.sp, color = foreground.copy(alpha = .74f),
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        Text(detail, fontSize = 10.sp, lineHeight = 14.sp,
            color = if (available) accent else foreground.copy(alpha = .6f),
            fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatusPill(label: String, accent: Color, night: Boolean) {
    Box(
        Modifier.clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = if (night) .18f else .14f))
            .border(.8.dp, accent.copy(alpha = .55f), RoundedCornerShape(50))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(label, fontSize = 8.sp, lineHeight = 10.sp, color = accent,
            fontWeight = FontWeight.SemiBold, maxLines = 2, textAlign = TextAlign.Center)
    }
}

@Composable
private fun JournalCard(night: Boolean, entries: List<RepairJournalEntry>, foreground: Color) {
    val context = LocalContext.current
    GlassSurface(night) {
        Text(stringResource(R.string.repair_journal), fontWeight = FontWeight.Bold,
            fontSize = 18.sp, color = foreground)
        if (entries.isEmpty()) {
            Text(stringResource(R.string.repair_journal_empty), fontSize = 12.sp,
                lineHeight = 17.sp, color = foreground.copy(alpha = .74f),
                modifier = Modifier.testTag("repair-journal-empty"))
        } else {
            entries.take(4).forEach { entry ->
                JournalRow(entry, night, foreground, context)
            }
        }
    }
}

@Composable
private fun JournalRow(
    entry: RepairJournalEntry,
    night: Boolean,
    foreground: Color,
    context: Context,
) {
    val service = RepairCatalog.service(entry.serviceKey)
    val accent = service?.let { repairAccent(it.id) } ?: Color(0xFF64EEFF)
    val outcome = when (entry.outcome) {
        RepairOutcome.COMPLETE -> stringResource(R.string.repair_journal_complete)
        RepairOutcome.PARTIAL -> stringResource(R.string.repair_journal_partial)
        RepairOutcome.FAILED -> stringResource(R.string.repair_journal_failed)
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 5.dp).size(7.dp).background(accent, CircleShape))
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(service?.let { stringResource(it.titleRes) } ?: stringResource(R.string.repair_lab),
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = foreground)
            Text(
                stringResource(if (entry.action == ACTION_CACHE_REMOVE)
                    R.string.repair_action_cache_remove else R.string.repair_action_other),
                fontSize = 11.sp, color = foreground.copy(alpha = .72f),
            )
            Text(
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(Date(entry.atMillis)),
                fontSize = 10.sp, color = foreground.copy(alpha = .58f),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(outcome, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                color = if (entry.outcome == RepairOutcome.COMPLETE) accent else Color(0xFFFFC98A))
            Text(
                stringResource(R.string.repair_journal_measure, entry.items,
                    Formatter.formatFileSize(context, entry.bytes)),
                fontSize = 11.sp, color = foreground.copy(alpha = .8f),
            )
        }
    }
}

/**
 * Honest detail route for services that are registered but not implemented in this build.
 * When a later slice makes a service real, its catalog entry points at a working route instead.
 */
@Composable
fun RepairServiceRoute(route: String, night: Boolean, onBack: () -> Unit) {
    val service = RepairCatalog.byRoute(route)
    val foreground = if (night) Color(0xFFF7F3FF) else Color(0xFF171050)
    Box(Modifier.fillMaxSize()) {
        Atmosphere(night, 0f)
        Column(
            Modifier.fillMaxSize().safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KxPageHeader(
                title = service?.let { stringResource(it.titleRes) }
                    ?: stringResource(R.string.repair_title),
                night = night,
                backLabel = stringResource(R.string.back),
                onBack = onBack,
            )
            if (service == null) {
                GlassSurface(night) {
                    Text(stringResource(R.string.repair_unknown_service), fontSize = 15.sp)
                }
            } else {
                Box(Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center) {
                    RepairGlyph(service.id, Modifier.size(148.dp))
                }
                Text(
                    stringResource(service.descriptionRes),
                    fontSize = 16.sp, lineHeight = 23.sp, color = foreground,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
                GlassSurface(night) {
                    Text(stringResource(R.string.repair_unavailable), fontWeight = FontWeight.Bold,
                        fontSize = 18.sp, color = foreground)
                    Text(stringResource(R.string.repair_unavailable_body), fontSize = 13.sp,
                        lineHeight = 19.sp, color = foreground.copy(alpha = .8f))
                }
                GlassSurface(night) {
                    Text(stringResource(R.string.repair_scope,
                        stringResource(service.scopeRes)), fontSize = 13.sp,
                        lineHeight = 19.sp, color = foreground)
                    Text(stringResource(R.string.repair_requires,
                        stringResource(service.requiresRes)), fontSize = 13.sp,
                        lineHeight = 19.sp, color = foreground.copy(alpha = .8f))
                }
                Text(stringResource(R.string.repair_privacy), fontSize = 11.sp,
                    lineHeight = 16.sp, color = foreground.copy(alpha = .72f))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private const val ACTION_CACHE_REMOVE = "cache_remove"
