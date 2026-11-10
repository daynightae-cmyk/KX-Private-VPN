package com.knoux.kxprivatevpn.features

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
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
import com.knoux.kxprivatevpn.designsystem.kxCyan
import com.knoux.kxprivatevpn.designsystem.kxGreen
import com.knoux.kxprivatevpn.designsystem.kxInk
import com.knoux.kxprivatevpn.designsystem.kxMuted
import com.knoux.kxprivatevpn.designsystem.kxRose
import com.knoux.kxprivatevpn.designsystem.kxViolet
import com.knoux.kxprivatevpn.platform.storage.FolderSlice
import com.knoux.kxprivatevpn.platform.storage.MediaCategoryTotal
import com.knoux.kxprivatevpn.platform.storage.StorageCategory
import com.knoux.kxprivatevpn.platform.storage.StorageScan
import com.knoux.kxprivatevpn.platform.storage.StoragePermissions
import com.knoux.kxprivatevpn.platform.storage.reconcileStorage
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun StorageXrayRoute(night: Boolean, onBack: () -> Unit) {
    val model: StorageXrayViewModel = viewModel()
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result -> model.onMediaPermissions(result) }
    val tree = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let(model::attachTree) }
    BackHandler(enabled = state.pathStack.isNotEmpty()) { model.upFolder() }
    StorageXrayScreen(
        night = night,
        state = state,
        onBack = onBack,
        onRefresh = model::refresh,
        onRequestMedia = { permissions.launch(model.requiredMediaPermissions()) },
        onOpenSettings = {
            try {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) {
                // The button stays a no-op rather than claiming settings were opened.
            }
        },
        onChooseFolder = { tree.launch(null) },
        onScan = model::scanTree,
        onCancel = model::cancelScan,
        onForget = model::forgetTree,
        onEnter = model::enterFolder,
        onUp = model::upFolder,
    )
}

/**
 * Storage X-Ray renders only StatFs volume data, MediaStore rows the platform returned, and the
 * documents inside a user-authorized SAF tree. Missing permission or a failed query is shown as
 * such — never as 0 MB and never as a fabricated "clean" result. This screen is read-only.
 *
 * The hero and summary panel follow owner reference 07. The reference's "A5 / 100" health score
 * is not reproduced because no approved health model exists; the score is shown as unavailable
 * while the four rows carry real measured bytes only.
 */
@Composable
fun StorageXrayScreen(
    night: Boolean,
    state: StorageXrayUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRequestMedia: () -> Unit,
    onOpenSettings: () -> Unit,
    onChooseFolder: () -> Unit,
    onScan: () -> Unit,
    onCancel: () -> Unit,
    onForget: () -> Unit,
    onEnter: (String) -> Unit,
    onUp: () -> Unit,
) {
    val context = LocalContext.current
    val ink = kxInk(night)
    val health = remember(state) {
        storageHealth(state, StoragePermissions.documentsAvailable(Build.VERSION.SDK_INT))
    }
    val busy = state.loading || state.measuring

    KxReferenceTheme(night) {
        Box(Modifier.fillMaxSize()) {
            KxReferenceAtmosphere(night)
            Column(
                Modifier.fillMaxSize().safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 26.dp)
                    .testTag("route-storage"),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                KxReferenceHeader(
                    night = night,
                    actionLabel = stringResource(R.string.back),
                    tag = "storage-ref-back",
                    icon = KxIconKind.BACK,
                    onClick = onBack,
                )
                KxTitleBlock(
                    title = stringResource(R.string.storage_xray_title),
                    subtitle = stringResource(R.string.storage_xray_subtitle),
                    night = night,
                )
                StorageHealthHero(night)
                KxGradientAction(
                    label = stringResource(
                        if (busy) R.string.storage_ref_analyzing
                        else R.string.storage_ref_start_analysis
                    ),
                    night = night,
                    enabled = !busy && !state.scanning,
                    tag = "storage-refresh",
                    icon = KxIconKind.SEARCH,
                    onClick = onRefresh,
                )
                if (busy) {
                    LinearProgressIndicator(
                        Modifier.fillMaxWidth().testTag("storage-health-measuring"),
                    )
                }
                StorageHealthPanel(night, health)
                VolumeSummary(night, state)
                ReconciliationCard(night, state)
                MediaAccessCard(night, state, onRequestMedia, onOpenSettings)
                FolderCard(
                    night = night,
                    state = state,
                    context = context,
                    onChooseFolder = onChooseFolder,
                    onScan = onScan,
                    onCancel = onCancel,
                    onForget = onForget,
                    onEnter = onEnter,
                    onUp = onUp,
                )
                StoragePrivacyPanel(night, ink)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Reference 07 hero and measured status panel
// ---------------------------------------------------------------------------

@Composable
private fun StorageHealthHero(night: Boolean) {
    Box(
        Modifier.fillMaxWidth().height(226.dp).testTag("storage-health-hero"),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * .50f)
            val radius = min(size.width, size.height) * .355f
            drawCircle(
                Brush.radialGradient(
                    listOf(
                        Color(0xFF64EEFF).copy(alpha = .26f),
                        Color(0xFFAD71FF).copy(alpha = .18f),
                        Color.Transparent,
                    ),
                    center,
                    radius * 1.62f,
                ),
                radius * 1.62f,
                center,
            )
            rotate(-14f, center) {
                // Dashed orbits around the owner's preserved KX orb.
                repeat(3) { index ->
                    val scale = 1.00f + index * .16f
                    drawOval(
                        Color.White.copy(alpha = .16f - index * .03f),
                        topLeft = Offset(center.x - radius * scale, center.y - radius * scale * .64f),
                        size = Size(radius * 2f * scale, radius * 2f * scale * .64f),
                        style = Stroke(
                            (1f - index * .12f) * 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(2.2.dp.toPx(), 6.4.dp.toPx()),
                            ),
                        ),
                    )
                }
                drawArc(
                    Brush.sweepGradient(
                        listOf(Color.Transparent, kxCyan(night), Color.Transparent),
                        center,
                    ),
                    startAngle = 198f,
                    sweepAngle = 66f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius * 1.22f, center.y - radius * 1.22f),
                    size = Size(radius * 2.44f, radius * 2.44f),
                    style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round),
                )
                orbitArrowHead(center, radius * 1.22f, 264f, kxCyan(night))
                drawArc(
                    Brush.sweepGradient(
                        listOf(Color.Transparent, kxCyan(night), Color.Transparent),
                        center,
                    ),
                    startAngle = 16f,
                    sweepAngle = 62f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius * 1.22f, center.y - radius * 1.22f),
                    size = Size(radius * 2.44f, radius * 2.44f),
                    style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round),
                )
                orbitArrowHead(center, radius * 1.22f, 78f, kxCyan(night))
            }
            repeat(14) { index ->
                val angle = Math.toRadians((index * 27.3 + 8).toDouble())
                val orbit = radius * (1.34f + (index % 3) * .05f)
                drawCircle(
                    if (index % 3 == 0) kxRose(night).copy(alpha = .62f)
                    else kxCyan(night).copy(alpha = .48f),
                    (1.2 + (index % 2) * .7).dp.toPx(),
                    Offset(
                        center.x + cos(angle).toFloat() * orbit,
                        center.y + sin(angle).toFloat() * orbit * .66f,
                    ),
                )
            }
        }
        KxOrbBadge(164.dp, Modifier, night)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.orbitArrowHead(
    center: Offset,
    radius: Float,
    angleDeg: Float,
    color: Color,
) {
    val angle = Math.toRadians(angleDeg.toDouble())
    val tip = Offset(
        center.x + cos(angle).toFloat() * radius,
        center.y + sin(angle).toFloat() * radius,
    )
    val tangent = Math.toRadians(angleDeg.toDouble() + 90.0)
    val length = 7.dp.toPx()
    listOf(-0.52, 0.52).forEach { spread ->
        val direction = tangent + Math.PI + spread
        val point = Offset(
            tip.x + cos(direction).toFloat() * length,
            tip.y + sin(direction).toFloat() * length,
        )
        drawLine(color, tip, point, 2.2.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
private fun StorageHealthPanel(night: Boolean, health: StorageHealthSnapshot) {
    val ink = kxInk(night)
    KxGlassPanel(
        night,
        modifier = Modifier.testTag("storage-health-panel"),
        radius = 23.dp,
        padding = PaddingValues(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.storage_ref_status),
                        fontSize = 10.5.sp,
                        color = kxMuted(night),
                    )
                    Spacer(Modifier.width(4.dp))
                    KxLineIcon(KxIconKind.SPARKLE, kxViolet(night), Modifier.size(13.dp))
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    stringResource(storageStatusLabel(health.status)),
                    fontSize = 21.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = storageStatusColor(health.status, night),
                    modifier = Modifier.testTag("storage-health-status"),
                )
            }
            KxDivider(night, Modifier.padding(horizontal = 10.dp))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.storage_ref_health_score),
                        fontSize = 10.5.sp,
                        color = kxMuted(night),
                    )
                    Spacer(Modifier.width(4.dp))
                    KxLineIcon(KxIconKind.SHIELD_CHECK, kxCyan(night), Modifier.size(13.dp))
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    stringResource(R.string.storage_ref_score_unavailable),
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = ink.copy(alpha = .78f),
                    modifier = Modifier.testTag("storage-health-score"),
                )
            }
        }
        Text(
            stringResource(R.string.storage_ref_score_note),
            fontSize = 9.5.sp,
            lineHeight = 13.sp,
            color = kxMuted(night),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        )
        Spacer(Modifier.height(4.dp))
        health.categories.forEach { category ->
            HealthCategoryRow(night, health, category)
            Spacer(Modifier.height(7.dp))
        }
        Text(
            stringResource(R.string.storage_ref_health_note),
            fontSize = 9.5.sp,
            lineHeight = 13.sp,
            color = kxMuted(night),
        )
        Text(
            stringResource(R.string.storage_ref_panel_source_note),
            fontSize = 9.5.sp,
            lineHeight = 13.sp,
            color = kxMuted(night),
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun HealthCategoryRow(
    night: Boolean,
    health: StorageHealthSnapshot,
    category: StorageHealthCategory,
) {
    val ink = kxInk(night)
    val accent = when (category.kind) {
        StorageHealthKind.KX -> kxViolet(night)
        StorageHealthKind.MEDIA -> kxCyan(night)
        StorageHealthKind.DOCUMENTS -> kxRose(night)
        StorageHealthKind.OTHER -> kxGreen(night)
    }
    val icon = when (category.kind) {
        StorageHealthKind.KX -> KxIconKind.LAYERS
        StorageHealthKind.MEDIA -> KxIconKind.CLAPPER
        StorageHealthKind.DOCUMENTS -> KxIconKind.FILE
        StorageHealthKind.OTHER -> KxIconKind.FOLDER
    }
    val label = stringResource(
        when (category.kind) {
            StorageHealthKind.KX -> R.string.storage_kx_owned
            StorageHealthKind.MEDIA -> R.string.storage_media_title
            StorageHealthKind.DOCUMENTS -> R.string.storage_category_documents
            StorageHealthKind.OTHER -> R.string.storage_category_other
        },
    )
    val suffix = healthCategoryNote(category)
    val measured = category.bytes != null
    val fraction = health.fractionOf(category)
    val percent = health.percentOf(category)
    val percentLabel = when {
        !measured || percent == null -> null
        (category.bytes ?: 0L) > 0L && percent == 0 ->
            stringResource(R.string.storage_ref_less_than_one)
        else -> "$percent%"
    }
    val value = if (measured) {
        val bytes = Formatter.formatFileSize(LocalContext.current, category.bytes!!)
        val base = if (health.totalBytes != null) {
            stringResource(
                R.string.storage_ref_of_total,
                bytes,
                Formatter.formatFileSize(LocalContext.current, health.totalBytes),
            )
        } else {
            bytes
        }
        if (suffix == null) base else stringResource(R.string.storage_ref_join, base, suffix)
    } else {
        stringResource(storageAvailabilityLabel(category.availability))
    }

    Row(
        Modifier.fillMaxWidth()
            .testTag("storage-health-row-${category.kind.name.lowercase()}")
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (night) Color(0xFF2C2768).copy(alpha = .58f)
                else Color.White.copy(alpha = .58f),
            )
            .border(
                1.dp,
                if (night) Color.White.copy(alpha = .14f)
                else Color.White.copy(alpha = .82f),
                RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 9.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1.22f), verticalAlignment = Alignment.CenterVertically) {
            KxGlassIconTile(night, icon, accent, size = 30.dp)
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Bold,
                color = ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(Modifier.weight(1.38f).padding(horizontal = 6.dp)) {
            Text(
                value,
                fontSize = 9.5.sp,
                lineHeight = 12.sp,
                color = if (measured) ink.copy(alpha = .84f) else kxMuted(night),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("storage-health-value-${category.kind.name.lowercase()}"),
            )
            Spacer(Modifier.height(4.dp))
            HealthTrack(night, accent, fraction)
        }
        HealthPercentRing(
            night = night,
            accent = accent,
            fraction = fraction,
            label = percentLabel,
            tag = "storage-health-percent-${category.kind.name.lowercase()}",
        )
    }
}

@Composable
private fun HealthTrack(night: Boolean, accent: Color, fraction: Float?) {
    val track = if (night) Color.White.copy(alpha = .16f) else Color.Black.copy(alpha = .10f)
    Box(
        Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(track),
    ) {
        if (fraction != null && fraction > 0f) {
            Box(
                Modifier.fillMaxWidth(fraction).fillMaxHeight().background(
                    Brush.horizontalGradient(
                        listOf(accent.copy(alpha = .68f), accent),
                    ),
                ),
            )
        }
    }
}

@Composable
private fun HealthPercentRing(
    night: Boolean,
    accent: Color,
    fraction: Float?,
    label: String?,
    tag: String,
) {
    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 2.8.dp.toPx()
            val radius = size.minDimension / 2f - stroke
            val center = Offset(size.width / 2f, size.height / 2f)
            val track = if (night) Color.White.copy(alpha = .18f) else Color.Black.copy(alpha = .13f)
            drawCircle(
                track,
                radius,
                center,
                style = Stroke(
                    stroke,
                    pathEffect = if (fraction == null) {
                        PathEffect.dashPathEffect(floatArrayOf(2.4.dp.toPx(), 3.2.dp.toPx()))
                    } else null,
                ),
            )
            drawCircle(accent.copy(alpha = .08f), radius - stroke * 1.5f, center)
            if (fraction != null && fraction > 0f) {
                drawArc(
                    Brush.sweepGradient(
                        listOf(accent.copy(alpha = .42f), accent, Color.White),
                        center,
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(
                label ?: "—",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (fraction != null) kxInk(night) else kxMuted(night),
                maxLines = 1,
                modifier = Modifier.testTag(tag),
            )
        }
    }
}

private fun storageStatusLabel(status: StorageHealthStatus): Int = when (status) {
    StorageHealthStatus.MEASURING -> R.string.storage_ref_status_measuring
    StorageHealthStatus.MEASURED -> R.string.storage_ref_status_measured
    StorageHealthStatus.PARTIAL -> R.string.storage_ref_status_partial
    StorageHealthStatus.UNAVAILABLE -> R.string.storage_ref_status_unavailable
}

private fun storageStatusColor(status: StorageHealthStatus, night: Boolean): Color = when (status) {
    StorageHealthStatus.MEASURING -> kxViolet(night)
    StorageHealthStatus.MEASURED -> kxCyan(night)
    StorageHealthStatus.PARTIAL -> kxRose(night)
    StorageHealthStatus.UNAVAILABLE -> kxMuted(night)
}

private fun storageAvailabilityLabel(availability: StorageHealthAvailability): Int = when (availability) {
    StorageHealthAvailability.MEASURING -> R.string.storage_analyzing
    StorageHealthAvailability.PERMISSION_REQUIRED -> R.string.storage_category_not_permitted
    StorageHealthAvailability.VERSION_LIMITED -> R.string.storage_category_version_limited
    StorageHealthAvailability.QUERY_FAILED -> R.string.storage_category_failed
    StorageHealthAvailability.MEASURED, StorageHealthAvailability.PARTIAL,
    StorageHealthAvailability.NOT_MEASURED -> R.string.storage_category_unavailable
}

@Composable
private fun healthCategoryNote(category: StorageHealthCategory): String? {
    val partial = if (category.availability == StorageHealthAvailability.PARTIAL) {
        stringResource(R.string.storage_ref_partial_note)
    } else null
    val lowerBound = if (category.lowerBound) {
        stringResource(R.string.storage_ref_lower_bound)
    } else null
    val folderOnly = if (category.source == StorageHealthSource.AUTHORIZED_FOLDER &&
        category.bytes != null
    ) {
        stringResource(R.string.storage_ref_folder_only)
    } else null
    return listOfNotNull(partial, lowerBound, folderOnly)
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" · ")
}

// ---------------------------------------------------------------------------
// Existing measured detail cards, restyled with the reference glass vocabulary
// ---------------------------------------------------------------------------

@Composable
private fun VolumeSummary(night: Boolean, state: StorageXrayUiState) {
    val context = LocalContext.current
    val ink = kxInk(night)
    val volume = state.volume
    KxGlassPanel(night, radius = 22.dp, padding = PaddingValues(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            KxGlassIconTile(night, KxIconKind.DRIVE_HEART, kxViolet(night), size = 34.dp)
            Spacer(Modifier.width(9.dp))
            Text(
                stringResource(R.string.storage_volume_title),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = ink,
                modifier = Modifier.weight(1f),
            )
            if (state.measuring) LinearProgressIndicator(Modifier.width(56.dp))
        }
        Spacer(Modifier.height(7.dp))
        val total = volume?.totalBytes
        val available = volume?.availableBytes
        if (total != null && available != null) {
            Text(
                stringResource(
                    R.string.storage_volume_used,
                    Formatter.formatFileSize(context, total - available),
                    Formatter.formatFileSize(context, total),
                ),
                fontSize = 14.sp,
                color = ink,
                modifier = Modifier.testTag("storage-volume-used"),
            )
            Text(
                stringResource(R.string.storage_measured_at, formatDate(context, volume.atMillis)),
                fontSize = 10.5.sp,
                color = kxMuted(night),
                modifier = Modifier.testTag("storage-measured-at"),
            )
        } else {
            Text(
                stringResource(
                    if (state.measuring || state.loading) R.string.storage_analyzing
                    else R.string.storage_volume_unavailable,
                ),
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                color = kxMuted(night),
            )
        }
        Spacer(Modifier.height(8.dp))
        KxDivider(night, vertical = false)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.storage_kx_owned),
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp,
            color = ink.copy(alpha = .85f),
        )
        if (state.kxBytes != null) {
            Text(
                stringResource(
                    R.string.storage_kx_files,
                    state.kxFiles,
                    Formatter.formatFileSize(context, state.kxBytes),
                ),
                fontSize = 13.5.sp,
                color = ink,
                modifier = Modifier.testTag("storage-kx"),
            )
            if (state.kxLimited) {
                Text(
                    stringResource(R.string.storage_kx_limited),
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp,
                    color = kxMuted(night),
                )
            }
        } else {
            Text(
                stringResource(R.string.storage_category_unavailable),
                fontSize = 11.5.sp,
                color = kxMuted(night),
            )
        }
    }
}

@Composable
private fun ReconciliationCard(night: Boolean, state: StorageXrayUiState) {
    val context = LocalContext.current
    val ink = kxInk(night)
    val mediaMeasured = state.media?.categories?.mapNotNull { it.bytes }
    val mediaTotal = if (mediaMeasured != null && mediaMeasured.isNotEmpty()) mediaMeasured.sum() else null
    val accessible = listOfNotNull(mediaTotal, state.kxBytes, state.scan?.totalBytes).sum()
    val reconciled = reconcileStorage(state.volume?.totalBytes, state.volume?.availableBytes, accessible)
    KxGlassPanel(night, radius = 22.dp, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KxGlassIconTile(night, KxIconKind.BARS, kxCyan(night), size = 34.dp)
            Spacer(Modifier.width(9.dp))
            Text(
                stringResource(R.string.storage_reconcile_title),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = ink,
            )
        }
        Spacer(Modifier.height(6.dp))
        if (reconciled == null) {
            Text(
                stringResource(
                    if (state.measuring || state.loading) R.string.storage_analyzing
                    else R.string.storage_reconcile_unavailable,
                ),
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                color = kxMuted(night),
            )
        } else {
            val totalUsed = reconciled.usedBytes.coerceAtLeast(1)
            Box(
                Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = .16f)),
            ) {
                Box(
                    Modifier.fillMaxWidth(reconciled.accessibleBytes.toFloat() / totalUsed)
                        .fillMaxHeight().background(
                            Brush.horizontalGradient(
                                listOf(kxViolet(night), kxCyan(night)),
                            ),
                        ),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(
                    R.string.storage_reconcile_accessible,
                    Formatter.formatFileSize(context, reconciled.accessibleBytes),
                ),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = ink,
                modifier = Modifier.testTag("storage-accessible"),
            )
            Text(
                stringResource(
                    R.string.storage_reconcile_outside,
                    Formatter.formatFileSize(context, reconciled.unexplainedBytes),
                ),
                fontSize = 13.5.sp,
                color = ink.copy(alpha = .82f),
                modifier = Modifier.testTag("storage-outside"),
            )
        }
        Text(
            stringResource(R.string.storage_reconcile_note),
            fontSize = 10.sp,
            lineHeight = 14.sp,
            color = kxMuted(night),
            modifier = Modifier.padding(top = 5.dp),
        )
    }
}

@Composable
private fun MediaAccessCard(
    night: Boolean,
    state: StorageXrayUiState,
    onRequestMedia: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val ink = kxInk(night)
    KxGlassPanel(night, radius = 22.dp, padding = PaddingValues(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            KxGlassIconTile(night, KxIconKind.CLAPPER, kxRose(night), size = 34.dp)
            Spacer(Modifier.width(9.dp))
            Text(
                stringResource(R.string.storage_media_title),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = ink,
                modifier = Modifier.weight(1f),
            )
            if (state.mediaLoading) LinearProgressIndicator(Modifier.width(56.dp))
        }
        Text(
            stringResource(R.string.storage_media_scope),
            fontSize = 10.sp,
            lineHeight = 14.sp,
            color = kxMuted(night),
            modifier = Modifier.padding(top = 4.dp),
        )
        val media = state.media
        val missingPermission = media?.categories?.any {
            !it.permitted && it.category != StorageCategory.DOCUMENTS
        } == true
        if (media != null) {
            val current = LocalContext.current
            Spacer(Modifier.height(4.dp))
            media.categories.forEach { category ->
                MediaCategoryRow(category, ink, night, current)
            }
            if (media.truncated) {
                Text(
                    stringResource(R.string.storage_media_truncated),
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = kxMuted(night),
                )
            }
            if (media.partialAccess) {
                Text(
                    stringResource(R.string.storage_media_partial),
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = kxMuted(night),
                    modifier = Modifier.testTag("storage-media-partial"),
                )
            }
        }
        if (missingPermission) {
            Text(
                stringResource(
                    if (state.mediaDenied) R.string.storage_media_permission_denied
                    else R.string.storage_media_permission_needed,
                ),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = ink.copy(alpha = .82f),
                modifier = Modifier.testTag("storage-media-permission-text"),
            )
            OutlinedButton(
                onClick = onRequestMedia,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .testTag("storage-media-permission"),
            ) {
                Text(stringResource(R.string.storage_media_allow))
            }
            if (state.mediaDenied) {
                TextButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("storage-media-settings"),
                ) {
                    Text(stringResource(R.string.storage_open_settings))
                }
            }
        }
    }
}

@Composable
private fun MediaCategoryRow(
    category: MediaCategoryTotal,
    ink: Color,
    night: Boolean,
    context: android.content.Context,
) {
    val label = stringResource(categoryName(category.category))
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StorageCategoryGlyph(category.category, Modifier.size(30.dp))
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = ink)
            Text(
                when {
                    category.failed -> stringResource(R.string.storage_category_failed)
                    !category.permitted && category.category == StorageCategory.DOCUMENTS &&
                        !StoragePermissions.documentsAvailable(Build.VERSION.SDK_INT) ->
                        stringResource(R.string.storage_category_version_limited)
                    !category.permitted -> stringResource(R.string.storage_category_not_permitted)
                    category.bytes != null -> stringResource(
                        R.string.storage_category_counts,
                        category.files,
                    )
                    else -> stringResource(R.string.storage_category_unavailable)
                },
                fontSize = 10.5.sp,
                color = kxMuted(night),
                modifier = Modifier.testTag("storage-category-${category.category.name.lowercase()}"),
            )
        }
        if (category.bytes != null) {
            Text(
                Formatter.formatFileSize(context, category.bytes),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = ink,
            )
        }
    }
}

private fun formatDate(context: android.content.Context, atMillis: Long): String {
    val locale = context.resources.configuration.locales.get(0) ?: Locale.getDefault()
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
        .format(Date(atMillis))
}

private fun categoryName(category: StorageCategory): Int = when (category) {
    StorageCategory.IMAGES -> R.string.storage_category_images
    StorageCategory.VIDEOS -> R.string.storage_category_videos
    StorageCategory.AUDIO -> R.string.storage_category_audio
    StorageCategory.DOCUMENTS -> R.string.storage_category_documents
    StorageCategory.OTHER -> R.string.storage_category_other
}

@Composable
private fun FolderCard(
    night: Boolean,
    state: StorageXrayUiState,
    context: android.content.Context,
    onChooseFolder: () -> Unit,
    onScan: () -> Unit,
    onCancel: () -> Unit,
    onForget: () -> Unit,
    onEnter: (String) -> Unit,
    onUp: () -> Unit,
) {
    val ink = kxInk(night)
    KxGlassPanel(night, radius = 22.dp, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KxGlassIconTile(night, KxIconKind.FOLDER, kxGreen(night), size = 34.dp)
            Spacer(Modifier.width(9.dp))
            Text(
                stringResource(R.string.storage_folder_title),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = ink,
            )
        }
        Spacer(Modifier.height(5.dp))
        if (state.treeUri == null) {
            Text(
                stringResource(R.string.storage_folder_none),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = ink.copy(alpha = .82f),
            )
            OutlinedButton(
                onClick = onChooseFolder,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .testTag("storage-choose-folder"),
            ) {
                Text(stringResource(R.string.storage_folder_choose))
            }
        } else {
            Text(
                state.treeName ?: stringResource(R.string.storage_folder_title),
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = ink,
                modifier = Modifier.testTag("storage-tree-name"),
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (state.scanning) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("storage-scan-cancel"),
                    ) {
                        Text(stringResource(R.string.storage_folder_cancel))
                    }
                } else {
                    OutlinedButton(
                        onClick = onScan,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("storage-scan"),
                    ) {
                        Text(
                            stringResource(
                                if (state.scan == null) R.string.storage_folder_scan
                                else R.string.storage_folder_rescan,
                            ),
                        )
                    }
                }
                TextButton(
                    onClick = onForget,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("storage-forget"),
                ) {
                    Text(stringResource(R.string.storage_folder_forget))
                }
            }
            if (state.scanning) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(
                    stringResource(R.string.storage_folder_scanning, state.scanProgress),
                    fontSize = 11.sp,
                    color = ink.copy(alpha = .8f),
                    modifier = Modifier.testTag("storage-scan-progress"),
                )
            }
            if (state.scanFailed) {
                Text(
                    stringResource(R.string.storage_folder_failed),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = kxRose(night),
                    modifier = Modifier.testTag("storage-scan-failed"),
                )
            }
            if (state.scanCancelled) {
                Text(
                    stringResource(R.string.storage_folder_cancelled),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = ink.copy(alpha = .8f),
                    modifier = Modifier.testTag("storage-scan-cancelled"),
                )
            }
            state.scan?.let { scan ->
                if (scan.truncated) {
                    Text(
                        stringResource(R.string.storage_folder_partial),
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        color = kxMuted(night),
                        modifier = Modifier.testTag("storage-scan-partial"),
                    )
                }
                Text(
                    stringResource(
                        R.string.storage_folder_summary,
                        scan.totalFiles,
                        Formatter.formatFileSize(context, scan.totalBytes),
                    ),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ink,
                    modifier = Modifier.testTag("storage-scan-summary"),
                )
                Text(
                    stringResource(
                        R.string.storage_folder_measured_at,
                        formatDate(context, scan.atMillis),
                    ),
                    fontSize = 10.sp,
                    color = kxMuted(night),
                )
                ScanCategories(scan, ink, context)
                Box(
                    Modifier.fillMaxWidth().height(1.dp)
                        .background(Color.White.copy(alpha = .18f)),
                )
                PaneBrowser(state, scan, ink, context, onEnter, onUp)
                Text(
                    stringResource(R.string.storage_folder_overlap),
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = kxMuted(night),
                )
            }
        }
        Text(
            stringResource(R.string.storage_folder_scope),
            fontSize = 10.sp,
            lineHeight = 14.sp,
            color = kxMuted(night),
        )
    }
}

@Composable
private fun ScanCategories(scan: StorageScan, ink: Color, context: android.content.Context) {
    val totals = scan.categories()
    val total = totals.sumOf { it.bytes }.coerceAtLeast(1)
    totals.filter { it.files > 0 }.forEach { totalCategory ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(categoryName(totalCategory.category)),
                fontSize = 11.sp,
                color = ink.copy(alpha = .82f),
                modifier = Modifier.width(84.dp),
            )
            Box(
                Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = .16f)),
            ) {
                Box(
                    Modifier.fillMaxWidth(totalCategory.bytes.toFloat() / total)
                        .fillMaxHeight().background(
                            Brush.horizontalGradient(listOf(kxViolet(true), kxCyan(true))),
                        ),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                Formatter.formatFileSize(context, totalCategory.bytes),
                fontSize = 10.5.sp,
                color = ink,
                modifier = Modifier.width(76.dp),
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun PaneBrowser(
    state: StorageXrayUiState,
    scan: StorageScan,
    ink: Color,
    context: android.content.Context,
    onEnter: (String) -> Unit,
    onUp: () -> Unit,
) {
    val prefix = state.pathStack.joinToString("/")
    val children = scan.children(prefix)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (state.pathStack.isNotEmpty()) {
            TextButton(
                onClick = onUp,
                modifier = Modifier.heightIn(min = 48.dp).testTag("storage-pane-up"),
            ) {
                Text(stringResource(R.string.storage_folder_up))
            }
        }
        Text(
            stringResource(R.string.storage_folder_breadcrumb_root) +
                state.pathStack.joinToString("") { " / $it" },
            fontSize = 11.sp,
            color = ink.copy(alpha = .8f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).testTag("storage-breadcrumb"),
        )
    }
    if (children.folders.isEmpty() && children.files.isEmpty()) {
        Text(
            stringResource(R.string.storage_folder_no_children),
            fontSize = 11.sp,
            color = ink.copy(alpha = .72f),
            modifier = Modifier.testTag("storage-no-children"),
        )
        return
    }
    if (children.folders.isNotEmpty()) {
        Text(
            stringResource(R.string.storage_folder_folders),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = ink.copy(alpha = .82f),
        )
        children.folders.take(30).forEach { folder ->
            FolderRow(folder, ink, onEnter)
        }
    }
    if (children.files.isNotEmpty()) {
        Text(
            stringResource(R.string.storage_folder_files),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = ink.copy(alpha = .82f),
            modifier = Modifier.padding(top = 4.dp),
        )
        children.files.take(30).forEach { file ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    file.name,
                    fontSize = 11.sp,
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    Formatter.formatFileSize(context, file.bytes),
                    fontSize = 10.sp,
                    color = ink.copy(alpha = .8f),
                )
            }
        }
    }
}

@Composable
private fun FolderRow(folder: FolderSlice, ink: Color, onEnter: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .clickable(
                role = Role.Button,
                onClickLabel = folder.name,
                onClick = { onEnter(folder.path) },
            )
            .testTag("storage-folder-${folder.name}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(14.dp).clip(RoundedCornerShape(4.dp))
                .background(Brush.linearGradient(listOf(kxViolet(true), kxCyan(true)))),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                folder.name,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.storage_folder_counts, folder.files),
                fontSize = 9.5.sp,
                color = ink.copy(alpha = .7f),
            )
        }
        Text("›", fontSize = 19.sp, color = ink)
    }
}

@Composable
private fun StoragePrivacyPanel(night: Boolean, ink: Color) {
    KxGlassPanel(night, radius = 22.dp, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KxLineIcon(KxIconKind.SHIELD_CHECK, kxGreen(night), Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text(
                stringResource(R.string.storage_no_deletion),
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = ink,
                modifier = Modifier.testTag("storage-read-only"),
            )
        }
        Text(
            stringResource(R.string.storage_scope_note),
            fontSize = 10.sp,
            lineHeight = 14.sp,
            color = kxMuted(night),
            modifier = Modifier.padding(top = 5.dp),
        )
    }
}

@Composable
private fun StorageCategoryGlyph(category: StorageCategory, modifier: Modifier) {
    val accent = when (category) {
        StorageCategory.IMAGES -> kxCyan(true)
        StorageCategory.VIDEOS -> kxViolet(true)
        StorageCategory.AUDIO -> kxRose(true)
        StorageCategory.DOCUMENTS -> Color(0xFFB9E9FF)
        StorageCategory.OTHER -> Color(0xFF9A94C9)
    }
    Canvas(modifier) {
        val u = min(size.width, size.height)
        val c = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            Brush.radialGradient(
                listOf(accent.copy(alpha = .45f), Color.Transparent),
                c,
                u * .5f,
            ),
            u * .5f,
            c,
        )
        drawCircle(
            Brush.linearGradient(
                listOf(Color.White.copy(alpha = .35f), accent.copy(alpha = .30f)),
            ),
            u * .34f,
            c,
        )
        drawCircle(Color.White.copy(alpha = .4f), u * .34f, c, style = Stroke(u * .03f))
        val stroke = Stroke(u * .055f, cap = StrokeCap.Round)
        when (category) {
            StorageCategory.IMAGES -> {
                val path = Path().apply {
                    moveTo(c.x - u * .16f, c.y + u * .10f)
                    lineTo(c.x - u * .04f, c.y - u * .03f)
                    lineTo(c.x + u * .04f, c.y + u * .05f)
                    lineTo(c.x + u * .12f, c.y - u * .04f)
                    lineTo(c.x + u * .18f, c.y + u * .10f)
                    close()
                }
                drawPath(path, Color.White.copy(alpha = .9f))
                drawCircle(Color.White, u * .035f, Offset(c.x + u * .10f, c.y - u * .13f))
            }
            StorageCategory.VIDEOS -> {
                val play = Path().apply {
                    moveTo(c.x - u * .08f, c.y - u * .13f)
                    lineTo(c.x + u * .14f, c.y)
                    lineTo(c.x - u * .08f, c.y + u * .13f)
                    close()
                }
                drawPath(play, Color.White.copy(alpha = .92f))
            }
            StorageCategory.AUDIO -> {
                drawLine(
                    Color.White,
                    Offset(c.x + u * .06f, c.y - u * .15f),
                    Offset(c.x + u * .06f, c.y + u * .10f),
                    u * .055f,
                    StrokeCap.Round,
                )
                drawCircle(Color.White, u * .075f, Offset(c.x - u * .02f, c.y + u * .12f))
                drawLine(
                    Color.White,
                    Offset(c.x + u * .06f, c.y - u * .15f),
                    Offset(c.x + u * .16f, c.y - u * .10f),
                    u * .055f,
                    StrokeCap.Round,
                )
            }
            StorageCategory.DOCUMENTS -> {
                drawRoundRect(
                    Color.White.copy(alpha = .92f),
                    topLeft = Offset(c.x - u * .13f, c.y - u * .17f),
                    size = Size(u * .26f, u * .34f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(u * .03f),
                )
                repeat(3) { i ->
                    drawLine(
                        accent,
                        Offset(c.x - u * .07f, c.y - u * .05f + i * u * .07f),
                        Offset(c.x + u * .07f, c.y - u * .05f + i * u * .07f),
                        u * .018f,
                    )
                }
            }
            StorageCategory.OTHER -> {
                drawRoundRect(
                    Color.White.copy(alpha = .9f),
                    topLeft = Offset(c.x - u * .24f, c.y - u * .02f),
                    size = Size(u * .48f, u * .17f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(u * .03f),
                    style = stroke,
                )
                drawRoundRect(
                    Color.White.copy(alpha = .6f),
                    topLeft = Offset(c.x - u * .18f, c.y - u * .12f),
                    size = Size(u * .36f, u * .10f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(u * .03f),
                    style = stroke,
                )
            }
        }
    }
}
