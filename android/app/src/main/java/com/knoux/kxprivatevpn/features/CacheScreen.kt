package com.knoux.kxprivatevpn.features

import android.app.Application
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
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.platform.storage.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class CacheState(val busy: Boolean = false, val preview: CachePreview? = null,
    val receipt: CacheReceipt? = null, val interrupted: Boolean = false, val failed: Boolean = false)
class CacheViewModel(app: Application) : AndroidViewModel(app) {
    private val tool = CacheMaintenance(app.cacheDir)
    private val journal = AtomicFile(File(app.noBackupFilesDir, "cache-last-operation.json"))
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
                    tool.removeApproved(preview, paths, true) { value, complete ->
                        write(value, complete)
                        mutable.update { it.copy(receipt = value, interrupted = !complete) }
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
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(state.busy) { onBusy(state.busy) }
    BackHandler(enabled = state.busy) { }
    val title = stringResource(R.string.cache_confirm)
    val body = stringResource(R.string.cache_approved_body)
    val remove = stringResource(R.string.cache_remove)
    val cancel = stringResource(R.string.cancel)
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
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (state.failed) Text(stringResource(R.string.save_failed))
        if (state.interrupted && !state.busy) Text(stringResource(R.string.cache_interrupted))
        state.receipt?.let { value ->
            Text(stringResource(R.string.cache_receipt, value.deleted,
                android.text.format.Formatter.formatFileSize(context, value.removedFileBytes), value.skipped.size, value.failed.size))
            Text(stringResource(R.string.cache_bytes_note))
        }
        OutlinedButton(onClick = { selected = emptySet(); model.scan() }, enabled = !state.busy,
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
            Button(onClick = { confirm = true }, enabled = !state.busy && selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("cache-remove")) { Text(remove) }
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text(title) },
        text = { Text(body, Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = { confirm = false; model.remove(selected) }, modifier = Modifier.testTag("cache-confirm")) { Text(remove) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(cancel) } })
}
