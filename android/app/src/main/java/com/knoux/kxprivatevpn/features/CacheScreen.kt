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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
fun CacheScreen(onBusy: (Boolean) -> Unit) {
    val model: CacheViewModel = viewModel()
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var settingsFailed by remember { mutableStateOf(false) }
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
    )
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
    CrystalPanel {
        Canvas(Modifier.size(72.dp)) {
            val path = Path().apply {
                moveTo(size.width * .18f, size.height * .72f)
                lineTo(size.width * .65f, size.height * .13f)
                lineTo(size.width * .86f, size.height * .32f)
                lineTo(size.width * .38f, size.height * .85f); close()
            }
            drawCircle(Brush.radialGradient(listOf(Color(0xFFAF77FF).copy(alpha = .5f), Color.Transparent)))
            drawPath(path, Brush.linearGradient(listOf(Color.White, Color(0xFF69EFFF), Color(0xFF7330EE)), Offset.Zero, Offset(size.width, size.height)))
            drawPath(path, Color.White.copy(alpha = .7f), style = Stroke(1.dp.toPx()))
            drawLine(Color(0xFFF7EFFF), Offset(size.width * .2f, size.height * .70f),
                Offset(size.width * .65f, size.height * .15f), 2.dp.toPx(), StrokeCap.Round)
        }
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
        OutlinedButton(onClick = { selected = emptySet(); onScan() }, enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("cache-scan")) { Text(stringResource(R.string.cache_scan)) }
        state.preview?.let { preview ->
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
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text(title) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(body)
            Spacer(Modifier.height(12.dp))
            Text(summary)
        } },
        confirmButton = { TextButton(onClick = { confirm = false; onRemove(selected) }, modifier = Modifier.testTag("cache-confirm")) { Text(remove) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(cancel) } })
}
