package com.knoux.kxprivatevpn.features

import android.app.Application
import android.content.Intent
import android.provider.Settings
import android.util.AtomicFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.designsystem.KxGlassPanel
import com.knoux.kxprivatevpn.designsystem.KxGradientAction
import com.knoux.kxprivatevpn.designsystem.KxIconKind
import com.knoux.kxprivatevpn.designsystem.KxLineIcon
import com.knoux.kxprivatevpn.designsystem.KxOrb
import com.knoux.kxprivatevpn.designsystem.KxReferenceAtmosphere
import com.knoux.kxprivatevpn.designsystem.KxReferenceHeader
import com.knoux.kxprivatevpn.designsystem.KxReferenceTheme
import com.knoux.kxprivatevpn.designsystem.KxTitleBlock
import com.knoux.kxprivatevpn.designsystem.kxCyan
import com.knoux.kxprivatevpn.designsystem.kxInk
import com.knoux.kxprivatevpn.designsystem.kxMuted
import com.knoux.kxprivatevpn.designsystem.kxViolet
import com.knoux.kxprivatevpn.launch.IntroTimeline
import com.knoux.kxprivatevpn.platform.repair.RepairJournal
import com.knoux.kxprivatevpn.platform.repair.RepairJournalEntry
import com.knoux.kxprivatevpn.platform.repair.RepairOutcome
import com.knoux.kxprivatevpn.platform.repair.RepairServiceId
import com.knoux.kxprivatevpn.platform.storage.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Android's own app list. It is the only supported route for another app's storage; KX never
 * reads or deletes third-party data even when this button is used.
 */
fun cacheSystemSettingsIntent(): Intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

data class CacheState(val busy: Boolean = false, val preview: CachePreview? = null,
    val receipt: CacheReceipt? = null, val interrupted: Boolean = false, val failed: Boolean = false)
class CacheViewModel(app: Application) : AndroidViewModel(app) {
    private val tool = CacheMaintenance(app.cacheDir)
    private val journal = AtomicFile(File(app.noBackupFilesDir, "cache-last-operation.json"))
    private val repairJournal = RepairJournal(File(app.noBackupFilesDir, "repair-journal.json"))
    private val mutable = MutableStateFlow(CacheState(busy = true))
    private companion object { val journalLock = Any() }
    val state = mutable.asStateFlow()
    init {
        viewModelScope.launch(Dispatchers.IO) {
            try { synchronized(journalLock) { if (journal.baseFile.exists() || File(journal.baseFile.path + ".bak").exists()) {
                val json = journal.openRead().use { JSONObject(it.readBytes().toString(Charsets.UTF_8)) }
                fun paths(key: String): List<String> = json.getJSONArray(key).let { a -> (0 until a.length()).map(a::getString) }
                val result = CacheReceipt(json.getInt("deleted"), json.getLong("bytes"), paths("skipped"), paths("failed"))
                mutable.update { it.copy(receipt = result, interrupted = !json.getBoolean("complete")) }
            } } } catch (_: Exception) { mutable.update { it.copy(failed = true) } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
    private fun write(result: CacheReceipt, complete: Boolean) = synchronized(journalLock) {
        val json = JSONObject().put("deleted", result.deleted).put("bytes", result.removedFileBytes)
            .put("skipped", JSONArray(result.skipped)).put("failed", JSONArray(result.failed)).put("complete", complete)
        val output = journal.startWrite()
        try { output.write(json.toString().toByteArray()); journal.finishWrite(output) }
        catch (e: Exception) { journal.failWrite(output); throw e }
    }
    fun scan() {
        if (mutable.value.busy) return
        mutable.update { it.copy(busy = true, failed = false, preview = null) }
        viewModelScope.launch {
            try { val value = withContext(Dispatchers.IO) { tool.preview() }; mutable.update { it.copy(preview = value) } }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutable.update { it.copy(failed = true) } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
    fun remove(paths: Set<String>) {
        val preview = mutable.value.preview ?: return
        if (mutable.value.busy || paths.isEmpty()) return
        mutable.update { it.copy(busy = true, failed = false, interrupted = true) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val result = tool.removeApproved(preview, paths, true) { value, complete ->
                        write(value, complete)
                        mutable.update { it.copy(receipt = value, interrupted = !complete) }
                    }
                    // A completed cleanup becomes a real journal entry. The journal is evidence,
                    // so a journal write failure never changes the cleanup result shown above.
                    try {
                        repairJournal.append(RepairJournalEntry(
                            atMillis = System.currentTimeMillis(),
                            serviceKey = RepairServiceId.CACHE.key,
                            action = "cache_remove",
                            outcome = when {
                                result.deleted == 0 && result.failed.isNotEmpty() -> RepairOutcome.FAILED
                                result.failed.isNotEmpty() || result.skipped.isNotEmpty() -> RepairOutcome.PARTIAL
                                else -> RepairOutcome.COMPLETE
                            },
                            items = result.deleted,
                            bytes = result.removedFileBytes,
                        ))
                    } catch (_: Exception) {
                        // The in-screen receipt still reports exactly what was removed.
                    }
                }
                mutable.update { it.copy(preview = null) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutable.update { it.copy(failed = true) } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
}

@Composable
fun CacheScreen(night: Boolean = false, onBack: () -> Unit = {}, onBusy: (Boolean) -> Unit) {
    val model: CacheViewModel = viewModel()
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var settingsFailed by remember { mutableStateOf(false) }
    KxReferenceTheme(night) {
        Box(Modifier.fillMaxSize()) {
            KxReferenceAtmosphere(night)
            Column(
                Modifier.fillMaxSize().safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .testTag("route-cache"),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                KxReferenceHeader(
                    night = night,
                    actionLabel = stringResource(R.string.back),
                    tag = "cache-back",
                    icon = KxIconKind.BACK,
                    onClick = { if (!state.busy) onBack() },
                )
                KxTitleBlock(
                    title = stringResource(R.string.cache_ref_title),
                    subtitle = stringResource(R.string.cache_ref_subtitle),
                    night = night,
                )
                CacheScreenContent(
                    state = state,
                    onBusy = onBusy,
                    onScan = model::scan,
                    onRemove = model::remove,
                    onOpenSettings = {
                        settingsFailed = !runCatching { context.startActivity(cacheSystemSettingsIntent()) }
                            .isSuccess
                    },
                    settingsFailed = settingsFailed,
                    night = night,
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Stateless composition so the confirmation gate, the measured selection and the bilingual
 * large-text layout can be exercised without a live cache or a real Settings launch.
 */
@Composable
internal fun CacheScreenContent(
    state: CacheState,
    onBusy: (Boolean) -> Unit,
    onScan: () -> Unit,
    onRemove: (Set<String>) -> Unit,
    onOpenSettings: () -> Unit,
    settingsFailed: Boolean = false,
    night: Boolean = false,
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var confirm by remember { mutableStateOf(false) }
    val selectedBytes = state.preview?.files?.filter { it.path in selected }?.sumOf { it.bytes } ?: 0L
    LaunchedEffect(state.busy) { onBusy(state.busy) }
    BackHandler(enabled = state.busy) { }
    val title = stringResource(R.string.cache_confirm)
    val body = stringResource(R.string.cache_approved_body)
    val remove = stringResource(R.string.cache_remove)
    val cancel = stringResource(R.string.cancel)
    // Resolve the dialog's measured summary in this localized composition, not in the dialog
    // window, so Arabic never falls back to English.
    val summary = stringResource(R.string.cache_selected_summary, selected.size,
        android.text.format.Formatter.formatFileSize(context, selectedBytes))
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CacheReferenceHero(night)
        KxGradientAction(
            label = stringResource(R.string.cache_scan),
            night = night,
            enabled = !state.busy,
            tag = "cache-scan",
            modifier = Modifier.heightIn(min = 52.dp),
            icon = KxIconKind.SPARKLE,
        ) { selected = emptySet(); onScan() }
        KxGlassPanel(night, radius = 23.dp) {
        Text(stringResource(R.string.cache_scope))
        Text(stringResource(R.string.cache_other_apps_body), fontSize = 13.sp,
            lineHeight = 18.sp)
        OutlinedButton(onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .testTag("cache-system-settings")) {
            Text(stringResource(R.string.cache_other_apps_action))
        }
        if (settingsFailed) Text(stringResource(R.string.cache_settings_failed))
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (state.failed) Text(stringResource(R.string.save_failed))
        if (state.interrupted && !state.busy) Text(stringResource(R.string.cache_interrupted))
        state.receipt?.let { value ->
            Text(stringResource(R.string.cache_receipt, value.deleted,
                android.text.format.Formatter.formatFileSize(context, value.removedFileBytes), value.skipped.size, value.failed.size))
            Text(stringResource(R.string.cache_bytes_note))
        }
        state.preview?.let { preview ->
            CacheMeasuredSummary(night, preview)
            if (preview.limited) Text(stringResource(R.string.cache_limit))
            if (preview.files.isEmpty()) Text(stringResource(R.string.cache_none))
            preview.files.forEach { item ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = item.path in selected,
                    enabled = !state.busy, role = Role.Checkbox, onValueChange = {
                        selected = if (it) selected + item.path else selected - item.path
                    })) {
                    Checkbox(checked = item.path in selected, enabled = !state.busy, onCheckedChange = null)
                    Column(Modifier.weight(1f).padding(top = 8.dp)) {
                        Text(item.path)
                        Text(android.text.format.Formatter.formatFileSize(context, item.bytes))
                    }
                }
            }
            if (selected.isNotEmpty()) {
                Text(stringResource(R.string.cache_selected_summary, selected.size,
                    android.text.format.Formatter.formatFileSize(context, selectedBytes)),
                    modifier = Modifier.testTag("cache-selected-summary"))
            }
            Button(onClick = { confirm = true }, enabled = !state.busy && selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("cache-remove")) { Text(remove) }
        }
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text(title) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(body)
            Spacer(Modifier.height(12.dp))
            Text(summary)
        } },
        confirmButton = { TextButton(onClick = { confirm = false; onRemove(selected) }, modifier = Modifier.testTag("cache-confirm")) { Text(remove) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(cancel) } })
}


/**
 * Reference 04 / Dyad: responsive native orb and four visual satellites.
 * The icons are decorative; only a user-triggered scan reads KX's own cache.
 */
@Composable
private fun CacheReferenceHero(night: Boolean) {
    Box(Modifier.fillMaxWidth().height(242.dp).testTag("cache-ref-hero"), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().clearAndSetSemantics {}) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val r = size.height * .42f
            drawCircle(kxViolet(night).copy(alpha = .32f), radius = r, center = center,
                style = Stroke(1.dp.toPx()))
            drawCircle(kxCyan(night).copy(alpha = .22f), radius = r * .82f, center = center,
                style = Stroke(2.dp.toPx()))
            drawArc(
                Brush.sweepGradient(listOf(Color.Transparent, kxCyan(night), kxViolet(night), Color.Transparent)),
                startAngle = -65f, sweepAngle = 145f, useCenter = false,
                topLeft = Offset(center.x - r, center.y - r),
                size = androidx.compose.ui.geometry.Size(r * 2f, r * 2f),
                style = Stroke(4.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        KxOrb(
            1f, IntroTimeline.frame(1f),
            Modifier.size(156.dp).clearAndSetSemantics {},
        )
        CacheSatellite(night, KxIconKind.BROOM, Modifier.align(Alignment.TopStart).padding(start = 18.dp, top = 16.dp))
        CacheSatellite(night, KxIconKind.LAYERS, Modifier.align(Alignment.TopEnd).padding(end = 18.dp, top = 16.dp))
        CacheSatellite(night, KxIconKind.CLOCK, Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 18.dp))
        CacheSatellite(night, KxIconKind.FILE, Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 18.dp))
    }
}

@Composable
private fun CacheSatellite(night: Boolean, icon: KxIconKind, modifier: Modifier = Modifier) {
    Box(
        modifier.size(54.dp).clip(CircleShape)
            .background(Brush.linearGradient(listOf(
                kxViolet(night).copy(alpha = if (night) .40f else .18f),
                kxCyan(night).copy(alpha = if (night) .28f else .12f),
            )))
            .border(1.dp, Color.White.copy(alpha = if (night) .65f else .85f), CircleShape)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        KxLineIcon(icon, if (night) Color(0xFFD3B6FF) else Color(0xFF4C2C8F), Modifier.size(28.dp))
    }
}

/** All metrics are derived from the real, KX-owned CachePreview. No fabricated savings. */
@Composable
private fun CacheMeasuredSummary(night: Boolean, preview: CachePreview) {
    val context = LocalContext.current
    val files = preview.files
    val total = files.sumOf { it.bytes.coerceAtLeast(0L) }
    Column(Modifier.fillMaxWidth().testTag("cache-ref-metrics"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.cache_ref_results_title),
            color = kxInk(night), fontSize = 17.sp, fontWeight = FontWeight.Bold,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            CacheMetric(night, stringResource(R.string.cache_ref_metric_count), files.size.toString(), Modifier.weight(1f))
            CacheMetric(night, stringResource(R.string.cache_ref_metric_bytes),
                android.text.format.Formatter.formatFileSize(context, total), Modifier.weight(1f))
            CacheMetric(night, stringResource(R.string.cache_ref_metric_scope),
                stringResource(R.string.cache_ref_scope_value), Modifier.weight(1f))
        }
        Text(
            stringResource(R.string.cache_ref_safety),
            color = kxMuted(night), fontSize = 11.sp, lineHeight = 17.sp,
        )
    }
}

@Composable
private fun CacheMetric(night: Boolean, label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp))
            .background(kxViolet(night).copy(alpha = if (night) .16f else .08f))
            .border(1.dp, kxCyan(night).copy(alpha = .35f), RoundedCornerShape(16.dp))
            .padding(horizontal = 5.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(label, fontSize = 10.sp, lineHeight = 14.sp, color = kxMuted(night),
            textAlign = TextAlign.Center)
        Text(value, fontSize = 13.sp, lineHeight = 18.sp, color = kxInk(night),
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}
