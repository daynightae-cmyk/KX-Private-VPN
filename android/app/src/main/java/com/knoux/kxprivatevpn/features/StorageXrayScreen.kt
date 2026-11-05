package com.knoux.kxprivatevpn.features

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import com.knoux.kxprivatevpn.platform.storage.FolderSlice
import com.knoux.kxprivatevpn.platform.storage.MediaCategoryTotal
import com.knoux.kxprivatevpn.platform.storage.StorageCategory
import com.knoux.kxprivatevpn.platform.storage.StorageScan
import com.knoux.kxprivatevpn.platform.storage.StoragePermissions
import com.knoux.kxprivatevpn.platform.storage.reconcileStorage
import android.os.Build
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

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
    val foreground = if (night) Color(0xFFF7F3FF) else Color(0xFF171050)
    Box(Modifier.fillMaxSize()) {
        Atmosphere(night, 0f)
        Column(
            Modifier.fillMaxSize().safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .testTag("route-storage"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KxPageHeader(
                title = stringResource(R.string.storage_xray_title),
                night = night,
                backLabel = stringResource(R.string.back),
                onBack = onBack,
            )
            Text(
                stringResource(R.string.storage_xray_subtitle),
                color = foreground.copy(alpha = .82f), fontSize = 14.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            StorageCore(night, state, foreground)
            VolumeSummary(night, state, foreground)
            ReconciliationCard(night, state, foreground)
            MediaAccessCard(night, state, foreground, onRequestMedia, onOpenSettings)
            FolderCard(night, state, foreground, context, onChooseFolder, onScan, onCancel,
                onForget, onEnter, onUp)
            OutlinedButton(
                onClick = onRefresh,
                enabled = !state.measuring && !state.scanning,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("storage-refresh"),
            ) { Text(stringResource(R.string.storage_refresh)) }
            Text(
                stringResource(R.string.storage_scope_note),
                fontSize = 11.sp, lineHeight = 16.sp, color = foreground.copy(alpha = .72f),
            )
            Text(
                stringResource(R.string.storage_no_deletion),
                fontSize = 11.sp, lineHeight = 16.sp, color = foreground.copy(alpha = .72f),
                modifier = Modifier.testTag("storage-read-only"),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StorageCore(night: Boolean, state: StorageXrayUiState, foreground: Color) {
    val context = LocalContext.current
    val volume = state.volume
    val total = volume?.totalBytes
    val free = volume?.availableBytes
    val usedFraction = if (total != null && free != null && total > 0) {
        ((total - free).toFloat() / total).coerceIn(0f, 1f)
    } else 0f
    Box(Modifier.fillMaxWidth().height(252.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(236.dp)) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val radius = min(size.width, size.height) / 2f - 8.dp.toPx()
            drawCircle(Brush.radialGradient(listOf(Color(0xFFAD71FF).copy(alpha = .30f),
                Color(0xFF64EEFF).copy(alpha = .12f), Color.Transparent), c, radius), radius, c)
            drawCircle(Brush.linearGradient(listOf(Color.White.copy(alpha = .22f),
                Color(0xFF6D63E8).copy(alpha = .16f), Color(0xFF54D8FF).copy(alpha = .10f)),
                Offset(c.x - radius, c.y - radius), Offset(c.x + radius, c.y + radius)),
                radius * .72f, c)
            drawCircle(Color.White.copy(alpha = .38f), radius * .72f, c, style = Stroke(1.dp.toPx()))
            drawCircle(Color.White.copy(alpha = .16f), radius * .60f, c, style = Stroke(.8.dp.toPx()))
            drawCircle(Color.White.copy(alpha = .10f), radius * .86f, c, style = Stroke(.8.dp.toPx()))
            drawArc(Color.White.copy(alpha = .14f), startAngle = -90f, sweepAngle = 360f,
                useCenter = false, topLeft = Offset(c.x - radius * .93f, c.y - radius * .93f),
                size = Size(radius * 1.86f, radius * 1.86f), style = Stroke(6.dp.toPx()))
            drawArc(Brush.sweepGradient(listOf(Color(0xFF64EEFF), Color(0xFFAD71FF),
                Color(0xFFF9ECFF), Color(0xFF64EEFF)), c),
                startAngle = -90f, sweepAngle = 360f * usedFraction, useCenter = false,
                topLeft = Offset(c.x - radius * .93f, c.y - radius * .93f),
                size = Size(radius * 1.86f, radius * 1.86f),
                style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (total != null && free != null && total > 0) {
                Text("${(usedFraction * 100).toInt()}%", color = foreground,
                    fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.storage_used_label), color = foreground.copy(alpha = .75f),
                    fontSize = 12.sp)
                Text(Formatter.formatFileSize(context, free), color = foreground,
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.storage_free_label), color = foreground.copy(alpha = .7f),
                    fontSize = 11.sp)
            } else {
                Text(stringResource(if (state.measuring || state.loading)
                    R.string.storage_analyzing else R.string.storage_volume_unavailable),
                    color = foreground, fontSize = 13.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 30.dp))
            }
        }
    }
}

@Composable
private fun VolumeSummary(night: Boolean, state: StorageXrayUiState, foreground: Color) {
    val context = LocalContext.current
    val volume = state.volume
    GlassSurface(night) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.storage_volume_title), fontWeight = FontWeight.Bold,
                fontSize = 18.sp, color = foreground, modifier = Modifier.weight(1f))
            if (state.measuring) LinearProgressIndicator(Modifier.width(66.dp))
        }
        val total = volume?.totalBytes
        val available = volume?.availableBytes
        if (total != null && available != null) {
            Text(
                stringResource(R.string.storage_volume_used,
                    Formatter.formatFileSize(context, total - available),
                    Formatter.formatFileSize(context, total)),
                fontSize = 15.sp, color = foreground,
            )
            Text(
                stringResource(R.string.storage_measured_at, formatDate(context, volume.atMillis)),
                fontSize = 11.sp, color = foreground.copy(alpha = .7f),
                modifier = Modifier.testTag("storage-measured-at"),
            )
        } else {
            Text(stringResource(if (state.measuring || state.loading)
                R.string.storage_analyzing else R.string.storage_volume_unavailable),
                fontSize = 13.sp, color = foreground.copy(alpha = .82f))
        }
        Text(stringResource(R.string.storage_kx_owned), fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp, color = foreground.copy(alpha = .85f),
            modifier = Modifier.padding(top = 4.dp))
        if (state.kxBytes != null) {
            Text(
                stringResource(R.string.storage_kx_files, state.kxFiles,
                    Formatter.formatFileSize(context, state.kxBytes)),
                fontSize = 14.sp, color = foreground,
                modifier = Modifier.testTag("storage-kx"),
            )
            if (state.kxLimited) {
                Text(stringResource(R.string.storage_kx_limited), fontSize = 11.sp,
                    color = foreground.copy(alpha = .7f))
            }
        } else {
            Text(stringResource(R.string.storage_category_unavailable), fontSize = 12.sp,
                color = foreground.copy(alpha = .7f))
        }
    }
}

@Composable
private fun ReconciliationCard(night: Boolean, state: StorageXrayUiState, foreground: Color) {
    val context = LocalContext.current
    val mediaMeasured = state.media?.categories?.mapNotNull { it.bytes }
    val mediaTotal = if (mediaMeasured != null && mediaMeasured.isNotEmpty()) mediaMeasured.sum() else null
    val accessible = listOfNotNull(mediaTotal, state.kxBytes, state.scan?.totalBytes).sum()
    val reconciled = reconcileStorage(state.volume?.totalBytes, state.volume?.availableBytes, accessible)
    GlassSurface(night) {
        Text(stringResource(R.string.storage_reconcile_title), fontWeight = FontWeight.Bold,
            fontSize = 18.sp, color = foreground)
        if (reconciled == null) {
            Text(stringResource(if (state.measuring || state.loading) R.string.storage_analyzing
            else R.string.storage_reconcile_unavailable), fontSize = 12.sp,
                color = foreground.copy(alpha = .78f))
        } else {
            val totalUsed = reconciled.usedBytes.coerceAtLeast(1)
            Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = .18f))) {
                Box(Modifier.fillMaxWidth(reconciled.accessibleBytes.toFloat() / totalUsed)
                    .fillMaxHeight().background(Brush.horizontalGradient(
                        listOf(Color(0xFFAD71FF), Color(0xFF64EEFF)))))
            }
            Text(
                stringResource(R.string.storage_reconcile_accessible,
                    Formatter.formatFileSize(context, reconciled.accessibleBytes)),
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = foreground,
                modifier = Modifier.testTag("storage-accessible"),
            )
            Text(
                stringResource(R.string.storage_reconcile_outside,
                    Formatter.formatFileSize(context, reconciled.unexplainedBytes)),
                fontSize = 14.sp, color = foreground.copy(alpha = .82f),
                modifier = Modifier.testTag("storage-outside"),
            )
        }
        Text(stringResource(R.string.storage_reconcile_note), fontSize = 11.sp,
            lineHeight = 16.sp, color = foreground.copy(alpha = .7f))
    }
}

@Composable
private fun MediaAccessCard(
    night: Boolean,
    state: StorageXrayUiState,
    foreground: Color,
    onRequestMedia: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    GlassSurface(night) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.storage_media_title), fontWeight = FontWeight.Bold,
                fontSize = 18.sp, color = foreground, modifier = Modifier.weight(1f))
            if (state.mediaLoading) LinearProgressIndicator(Modifier.width(66.dp))
        }
        Text(stringResource(R.string.storage_media_scope), fontSize = 11.sp, lineHeight = 16.sp,
            color = foreground.copy(alpha = .72f))
        val media = state.media
        val missingPermission = media?.categories?.any {
            !it.permitted && it.category != StorageCategory.DOCUMENTS
        } == true
        if (media != null) {
            val current = LocalContext.current
            media.categories.forEach { category ->
                MediaCategoryRow(category, foreground, current)
            }
            if (media.truncated) {
                Text(stringResource(R.string.storage_media_truncated), fontSize = 11.sp,
                    color = foreground.copy(alpha = .7f))
            }
            if (media.partialAccess) {
                Text(stringResource(R.string.storage_media_partial), fontSize = 11.sp,
                    lineHeight = 16.sp, color = foreground.copy(alpha = .7f),
                    modifier = Modifier.testTag("storage-media-partial"))
            }
        }
        if (missingPermission) {
            Text(
                stringResource(if (state.mediaDenied) R.string.storage_media_permission_denied
                else R.string.storage_media_permission_needed),
                fontSize = 13.sp, color = foreground.copy(alpha = .82f),
                modifier = Modifier.testTag("storage-media-permission-text"),
            )
            OutlinedButton(onClick = onRequestMedia,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .testTag("storage-media-permission")) {
                Text(stringResource(R.string.storage_media_allow))
            }
            if (state.mediaDenied) {
                TextButton(onClick = onOpenSettings,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("storage-media-settings")) {
                    Text(stringResource(R.string.storage_open_settings))
                }
            }
        }
    }
}

@Composable
private fun MediaCategoryRow(
    category: MediaCategoryTotal,
    foreground: Color,
    context: android.content.Context,
) {
    val label = stringResource(categoryName(category.category))
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        StorageCategoryGlyph(category.category, Modifier.size(30.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = foreground)
            Text(
                when {
                    category.failed -> stringResource(R.string.storage_category_failed)
                    !category.permitted && category.category == StorageCategory.DOCUMENTS &&
                        !StoragePermissions.documentsAvailable(Build.VERSION.SDK_INT) ->
                        stringResource(R.string.storage_category_version_limited)
                    !category.permitted -> stringResource(R.string.storage_category_not_permitted)
                    category.bytes != null -> stringResource(R.string.storage_category_counts,
                        category.files)
                    else -> stringResource(R.string.storage_category_unavailable)
                },
                fontSize = 11.sp, color = foreground.copy(alpha = .72f),
                modifier = Modifier.testTag("storage-category-${category.category.name.lowercase()}"),
            )
        }
        if (category.bytes != null) {
            Text(Formatter.formatFileSize(context, category.bytes), fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold, color = foreground)
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
    foreground: Color,
    context: android.content.Context,
    onChooseFolder: () -> Unit,
    onScan: () -> Unit,
    onCancel: () -> Unit,
    onForget: () -> Unit,
    onEnter: (String) -> Unit,
    onUp: () -> Unit,
) {
    GlassSurface(night) {
        Text(stringResource(R.string.storage_folder_title), fontWeight = FontWeight.Bold,
            fontSize = 18.sp, color = foreground)
        if (state.treeUri == null) {
            Text(stringResource(R.string.storage_folder_none), fontSize = 13.sp,
                lineHeight = 18.sp, color = foreground.copy(alpha = .82f))
            OutlinedButton(onClick = onChooseFolder,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .testTag("storage-choose-folder")) {
                Text(stringResource(R.string.storage_folder_choose))
            }
        } else {
            Text(state.treeName ?: stringResource(R.string.storage_folder_title),
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = foreground,
                modifier = Modifier.testTag("storage-tree-name"))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (state.scanning) {
                    OutlinedButton(onClick = onCancel,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("storage-scan-cancel")) {
                        Text(stringResource(R.string.storage_folder_cancel))
                    }
                } else {
                    OutlinedButton(onClick = onScan,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("storage-scan")) {
                        Text(stringResource(if (state.scan == null) R.string.storage_folder_scan
                        else R.string.storage_folder_rescan))
                    }
                }
                TextButton(onClick = onForget,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("storage-forget")) {
                    Text(stringResource(R.string.storage_folder_forget))
                }
            }
            if (state.scanning) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(stringResource(R.string.storage_folder_scanning, state.scanProgress),
                    fontSize = 12.sp, color = foreground.copy(alpha = .8f),
                    modifier = Modifier.testTag("storage-scan-progress"))
            }
            if (state.scanFailed) {
                Text(stringResource(R.string.storage_folder_failed), fontSize = 13.sp,
                    color = Color(0xFFFFB4B4), modifier = Modifier.testTag("storage-scan-failed"))
            }
            if (state.scanCancelled) {
                Text(stringResource(R.string.storage_folder_cancelled), fontSize = 12.sp,
                    color = foreground.copy(alpha = .8f), modifier = Modifier.testTag("storage-scan-cancelled"))
            }
            state.scan?.let { scan ->
                if (scan.truncated) {
                    Text(stringResource(R.string.storage_folder_partial), fontSize = 11.sp,
                        lineHeight = 16.sp, color = foreground.copy(alpha = .72f),
                        modifier = Modifier.testTag("storage-scan-partial"))
                }
                Text(
                    stringResource(R.string.storage_folder_summary, scan.totalFiles,
                        Formatter.formatFileSize(context, scan.totalBytes)),
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = foreground,
                    modifier = Modifier.testTag("storage-scan-summary"),
                )
                Text(
                    stringResource(R.string.storage_folder_measured_at,
                        formatDate(context, scan.atMillis)),
                    fontSize = 11.sp, color = foreground.copy(alpha = .7f),
                )
                ScanCategories(scan, foreground, context)
                Box(Modifier.fillMaxWidth().height(1.dp)
                    .background(Color.White.copy(alpha = .18f)))
                PaneBrowser(state, scan, foreground, context, onEnter, onUp)
                Text(stringResource(R.string.storage_folder_overlap), fontSize = 11.sp,
                    lineHeight = 16.sp, color = foreground.copy(alpha = .7f))
            }
        }
        Text(stringResource(R.string.storage_folder_scope), fontSize = 11.sp, lineHeight = 16.sp,
            color = foreground.copy(alpha = .72f))
    }
}

@Composable
private fun ScanCategories(scan: StorageScan, foreground: Color, context: android.content.Context) {
    val totals = scan.categories()
    val total = totals.sumOf { it.bytes }.coerceAtLeast(1)
    totals.filter { it.files > 0 }.forEach { totalCategory ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(categoryName(totalCategory.category)), fontSize = 12.sp,
                color = foreground.copy(alpha = .82f), modifier = Modifier.width(84.dp))
            Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = .16f))) {
                Box(Modifier.fillMaxWidth(totalCategory.bytes.toFloat() / total)
                    .fillMaxHeight().background(Brush.horizontalGradient(
                        listOf(Color(0xFFAD71FF), Color(0xFF64EEFF)))))
            }
            Spacer(Modifier.width(8.dp))
            Text(Formatter.formatFileSize(context, totalCategory.bytes), fontSize = 11.sp,
                color = foreground, modifier = Modifier.width(76.dp),
                textAlign = TextAlign.End)
        }
    }
}

@Composable
private fun PaneBrowser(
    state: StorageXrayUiState,
    scan: StorageScan,
    foreground: Color,
    context: android.content.Context,
    onEnter: (String) -> Unit,
    onUp: () -> Unit,
) {
    val prefix = state.pathStack.joinToString("/")
    val children = scan.children(prefix)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (state.pathStack.isNotEmpty()) {
            TextButton(onClick = onUp,
                modifier = Modifier.heightIn(min = 48.dp).testTag("storage-pane-up")) {
                Text(stringResource(R.string.storage_folder_up))
            }
        }
        Text(
            stringResource(R.string.storage_folder_breadcrumb_root) +
                state.pathStack.joinToString("") { " / $it" },
            fontSize = 12.sp, color = foreground.copy(alpha = .8f),
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).testTag("storage-breadcrumb"),
        )
    }
    if (children.folders.isEmpty() && children.files.isEmpty()) {
        Text(stringResource(R.string.storage_folder_no_children), fontSize = 12.sp,
            color = foreground.copy(alpha = .72f), modifier = Modifier.testTag("storage-no-children"))
        return
    }
    if (children.folders.isNotEmpty()) {
        Text(stringResource(R.string.storage_folder_folders), fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold, color = foreground.copy(alpha = .82f))
        children.folders.take(30).forEach { folder -> FolderRow(folder, foreground, onEnter) }
    }
    if (children.files.isNotEmpty()) {
        Text(stringResource(R.string.storage_folder_files), fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold, color = foreground.copy(alpha = .82f),
            modifier = Modifier.padding(top = 4.dp))
        children.files.take(30).forEach { file ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(file.name, fontSize = 12.sp, color = foreground,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(Formatter.formatFileSize(context, file.bytes), fontSize = 11.sp,
                    color = foreground.copy(alpha = .8f))
            }
        }
    }
}

@Composable
private fun FolderRow(folder: FolderSlice, foreground: Color, onEnter: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .clickable(role = Role.Button,
                onClickLabel = folder.name, onClick = { onEnter(folder.path) })
            .testTag("storage-folder-${folder.name}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFAD71FF), Color(0xFF64EEFF)))))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(folder.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.storage_folder_counts, folder.files),
                fontSize = 10.sp, color = foreground.copy(alpha = .7f))
        }
        Text("›", fontSize = 20.sp, color = foreground)
    }
}

@Composable
private fun StorageCategoryGlyph(category: StorageCategory, modifier: Modifier) {
    val accent = when (category) {
        StorageCategory.IMAGES -> Color(0xFF64EEFF)
        StorageCategory.VIDEOS -> Color(0xFFAD71FF)
        StorageCategory.AUDIO -> Color(0xFFFF8AD8)
        StorageCategory.DOCUMENTS -> Color(0xFFB9E9FF)
        StorageCategory.OTHER -> Color(0xFF9A94C9)
    }
    Canvas(modifier) {
        val u = min(size.width, size.height)
        val c = Offset(size.width / 2f, size.height / 2f)
        drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = .45f), Color.Transparent),
            c, u * .5f), u * .5f, c)
        drawCircle(Brush.linearGradient(listOf(Color.White.copy(alpha = .35f),
            accent.copy(alpha = .30f))), u * .34f, c)
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
                drawLine(Color.White, Offset(c.x + u * .06f, c.y - u * .15f),
                    Offset(c.x + u * .06f, c.y + u * .10f), u * .055f, StrokeCap.Round)
                drawCircle(Color.White, u * .075f, Offset(c.x - u * .02f, c.y + u * .12f))
                drawLine(Color.White, Offset(c.x + u * .06f, c.y - u * .15f),
                    Offset(c.x + u * .16f, c.y - u * .10f), u * .055f, StrokeCap.Round)
            }
            StorageCategory.DOCUMENTS -> {
                drawRoundRect(Color.White.copy(alpha = .92f),
                    topLeft = Offset(c.x - u * .13f, c.y - u * .17f),
                    size = Size(u * .26f, u * .34f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(u * .03f))
                repeat(3) { i ->
                    drawLine(accent, Offset(c.x - u * .07f, c.y - u * .05f + i * u * .07f),
                        Offset(c.x + u * .07f, c.y - u * .05f + i * u * .07f), u * .018f)
                }
            }
            StorageCategory.OTHER -> {
                drawRoundRect(Color.White.copy(alpha = .9f),
                    topLeft = Offset(c.x - u * .24f, c.y - u * .02f),
                    size = Size(u * .48f, u * .17f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(u * .03f), style = stroke)
                drawRoundRect(Color.White.copy(alpha = .6f),
                    topLeft = Offset(c.x - u * .18f, c.y - u * .12f),
                    size = Size(u * .36f, u * .10f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(u * .03f), style = stroke)
            }
        }
    }
}
