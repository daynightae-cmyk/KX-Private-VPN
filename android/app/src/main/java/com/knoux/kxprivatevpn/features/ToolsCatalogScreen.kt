package com.knoux.kxprivatevpn.features

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.designsystem.KxBottomNav
import com.knoux.kxprivatevpn.designsystem.KxGlassPanel
import com.knoux.kxprivatevpn.designsystem.KxIconKind
import com.knoux.kxprivatevpn.designsystem.KxLineIcon
import com.knoux.kxprivatevpn.designsystem.KxMiniOrb
import com.knoux.kxprivatevpn.designsystem.KxOrb
import com.knoux.kxprivatevpn.designsystem.KxReferenceAtmosphere
import com.knoux.kxprivatevpn.designsystem.KxReferenceHeader
import com.knoux.kxprivatevpn.designsystem.KxReferenceTheme
import com.knoux.kxprivatevpn.designsystem.KxTitleBlock
import com.knoux.kxprivatevpn.designsystem.glassForeground
import com.knoux.kxprivatevpn.designsystem.kxCyan
import com.knoux.kxprivatevpn.designsystem.kxInk
import com.knoux.kxprivatevpn.designsystem.kxMuted
import com.knoux.kxprivatevpn.designsystem.kxRose
import com.knoux.kxprivatevpn.designsystem.kxViolet
import com.knoux.kxprivatevpn.launch.IntroTimeline

enum class ToolFilter(val key: String, @StringRes val label: Int) {
    ALL("all", R.string.tools_filter_all),
    STORAGE("storage", R.string.tools_filter_storage),
    FILES("files", R.string.tools_filter_files),
    MEDIA("media", R.string.tools_filter_media),
    NETWORK("network", R.string.tools_filter_network),
}

data class ToolEntry(
    val key: String,
    val route: String,
    @StringRes val title: Int,
    @StringRes val body: Int,
    val icon: KxIconKind,
    val accentNight: Color,
    val accentDay: Color,
    val filters: Set<ToolFilter>,
    val fullWidth: Boolean = false,
)

/**
 * Nine owner-approved tools in reference order. The catalog is metadata only: every route
 * below is a real destination in this build and no entry invents a scan or measurement.
 */
object ToolsCatalog {
    // Reading order in RTL rows: (temporary, duplicates), (large, download),
    // (smart repair, media), (network, apps), then the full-width storage health card.
    val entries: List<ToolEntry> = listOf(
        ToolEntry("temporary", "cache", R.string.tools_temp_title, R.string.tools_temp_body,
            KxIconKind.SPARKLE, kxViolet(true), kxViolet(false),
            setOf(ToolFilter.STORAGE)),
        ToolEntry("duplicates", "duplicate", R.string.tools_duplicates_title, R.string.tools_duplicates_body,
            KxIconKind.COPY, kxCyan(true), kxCyan(false),
            setOf(ToolFilter.FILES)),
        ToolEntry("large", "heavyweight", R.string.tools_large_title, R.string.tools_large_body,
            KxIconKind.LAYERS, kxViolet(true), kxViolet(false),
            setOf(ToolFilter.STORAGE)),
        ToolEntry("download", "download", R.string.tools_download_title, R.string.tools_download_body,
            KxIconKind.DOWNLOAD, kxCyan(true), kxCyan(false),
            setOf(ToolFilter.FILES)),
        ToolEntry("smart", "autopilot", R.string.tools_smart_title, R.string.tools_smart_body,
            KxIconKind.WRENCH, kxRose(true), kxRose(false),
            setOf(ToolFilter.STORAGE, ToolFilter.FILES, ToolFilter.MEDIA, ToolFilter.NETWORK)),
        ToolEntry("media", "media", R.string.tools_media_title, R.string.tools_media_body,
            KxIconKind.CLAPPER, kxCyan(true), kxCyan(false),
            setOf(ToolFilter.MEDIA)),
        ToolEntry("network", "network", R.string.tools_network_title, R.string.tools_network_body,
            KxIconKind.WIFI, kxRose(true), kxRose(false),
            setOf(ToolFilter.NETWORK)),
        ToolEntry("apps", "apps", R.string.tools_apps_title, R.string.tools_apps_body,
            KxIconKind.ANDROID, kxCyan(true), kxCyan(false),
            setOf(ToolFilter.FILES, ToolFilter.STORAGE)),
        ToolEntry("storage-health", "storage", R.string.tools_storage_health_title,
            R.string.tools_storage_health_body, KxIconKind.DRIVE_HEART,
            kxViolet(true), kxViolet(false), setOf(ToolFilter.STORAGE), fullWidth = true),
    )

    val gridEntries: List<ToolEntry> = entries.filterNot { it.fullWidth }
    val fullWidthEntries: List<ToolEntry> = entries.filter { it.fullWidth }

    fun matches(entry: ToolEntry, filter: ToolFilter, query: String, title: String, body: String): Boolean {
        if (filter != ToolFilter.ALL && filter !in entry.filters) return false
        val needle = query.trim()
        if (needle.isEmpty()) return true
        return title.contains(needle, ignoreCase = true) || body.contains(needle, ignoreCase = true)
    }
}

@Composable
fun ToolsCatalogScreen(
    night: Boolean,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    KxReferenceTheme(night) {
        var query by rememberSaveable { mutableStateOf("") }
        var filterKey by rememberSaveable { mutableStateOf(ToolFilter.ALL.key) }
        val filter = ToolFilter.entries.firstOrNull { it.key == filterKey } ?: ToolFilter.ALL
        val foreground = kxInk(night)
        val filtered = ToolsCatalog.entries.filter { entry ->
            ToolsCatalog.matches(
                entry, filter, query,
                stringResource(entry.title), stringResource(entry.body),
            )
        }
        Box(Modifier.fillMaxSize()) {
            KxReferenceAtmosphere(night)
            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier.weight(1f).fillMaxWidth().safeDrawingPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .testTag("route-tools"),
                    verticalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    KxReferenceHeader(
                        night = night,
                        actionLabel = stringResource(R.string.back),
                        tag = "tools-back",
                        icon = KxIconKind.BACK,
                        onClick = onBack,
                    )
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        KxOrb(
                            progress = 1f,
                            frame = IntroTimeline.frame(1f),
                            modifier = Modifier.size(52.dp),
                            decorative = true,
                        )
                    }
                    KxTitleBlock(
                        title = stringResource(R.string.tools_title),
                        subtitle = stringResource(R.string.tools_subtitle),
                        night = night,
                    )
                    ToolSearchField(
                        value = query,
                        night = night,
                        onValueChange = { query = it },
                    )
                    FilterChips(
                        selected = filter,
                        night = night,
                        total = ToolsCatalog.entries.size,
                        onSelected = { filterKey = it.key },
                    )
                    val grid = filtered.filterNot { it.fullWidth }
                    // At accessibility font scales a two-column card cannot hold an Arabic
                    // title without breaking it mid-word, so the same cards stack in order.
                    if (LocalDensity.current.fontScale >= 1.3f) {
                        grid.forEach { entry ->
                            ToolCard(
                                entry = entry,
                                night = night,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onNavigate(entry.route) },
                            )
                        }
                    } else {
                        grid.chunked(2).forEach { row ->
                            Row(
                                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                row.forEach { entry ->
                                    ToolCard(
                                        entry = entry,
                                        night = night,
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        onClick = { onNavigate(entry.route) },
                                    )
                                }
                                repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                    filtered.filter { it.fullWidth }.forEach { entry ->
                        StorageHealthCard(entry, night) { onNavigate(entry.route) }
                    }
                    if (filtered.isEmpty()) {
                        KxGlassPanel(night, radius = 18.dp) {
                            Text(
                                stringResource(R.string.tools_empty),
                                color = foreground,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                modifier = Modifier.testTag("tools-empty"),
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                KxBottomNav(
                    night = night,
                    selected = "tools",
                    toolsTab = true,
                    modifier = Modifier.fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    onNavigate = onNavigate,
                )
            }
        }
    }
}

@Composable
private fun ToolSearchField(
    value: String,
    night: Boolean,
    onValueChange: (String) -> Unit,
) {
    val shape = RoundedCornerShape(50)
    val label = stringResource(R.string.tools_search_label)
    val hint = stringResource(R.string.tools_search_hint)
    val foreground = kxInk(night)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(shape)
            .background(if (night) Color(0xFF141A3C).copy(alpha = .72f) else Color.White.copy(alpha = .50f))
            .border(1.dp, Color.White.copy(alpha = if (night) .24f else .82f), shape)
            .semantics { contentDescription = label }
            .padding(horizontal = 15.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        KxLineIcon(KxIconKind.SEARCH, kxMuted(night), Modifier.size(18.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = foreground,
                fontSize = 13.sp,
                textAlign = TextAlign.Start,
            ),
            cursorBrush = SolidColor(kxCyan(night)),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.weight(1f).testTag("tools-search"),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            hint,
                            color = kxMuted(night),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        )
                    }
                    inner()
                }
            },
        )
        KxLineIcon(KxIconKind.MIC, kxMuted(night), Modifier.size(18.dp))
    }
}

@Composable
private fun FilterChips(
    selected: ToolFilter,
    night: Boolean,
    total: Int,
    onSelected: (ToolFilter) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        ToolFilter.entries.forEach { filter ->
            val active = filter == selected
            val label = if (filter == ToolFilter.ALL) {
                stringResource(filter.label, total)
            } else {
                stringResource(filter.label)
            }
            Row(
                Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(50))
                    .background(
                        if (active) Brush.horizontalGradient(
                            listOf(Color(0xFF8A52FF), Color(0xFF5B45F5), Color(0xFF1E9BE8)),
                        ) else Brush.horizontalGradient(
                            if (night) listOf(Color(0xFF1D1F4C).copy(alpha = .82f),
                                Color(0xFF141A3C).copy(alpha = .70f))
                            else listOf(Color.White.copy(alpha = .46f),
                                Color.White.copy(alpha = .30f)),
                        ),
                    )
                    .border(1.dp, Color.White.copy(alpha = if (active) .55f else if (night) .20f else .76f),
                        RoundedCornerShape(50))
                    .clickable(role = Role.Button, onClickLabel = label) { onSelected(filter) }
                    .padding(horizontal = 13.dp, vertical = 9.dp)
                    .testTag("tools-filter-${filter.key}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (filter != ToolFilter.ALL) {
                    KxLineIcon(
                        when (filter) {
                            ToolFilter.STORAGE -> KxIconKind.LAYERS
                            ToolFilter.FILES -> KxIconKind.FILE
                            ToolFilter.MEDIA -> KxIconKind.CLAPPER
                            ToolFilter.NETWORK -> KxIconKind.WIFI
                            ToolFilter.ALL -> KxIconKind.GRID
                        },
                        if (active) Color.White else kxMuted(night),
                        Modifier.size(14.dp),
                    )
                } else {
                    KxLineIcon(KxIconKind.GRID, if (active) Color.White else kxMuted(night), Modifier.size(14.dp))
                }
                Text(
                    label,
                    color = if (active) Color.White else kxInk(night),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun ToolCard(
    entry: ToolEntry,
    night: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val accent = if (night) entry.accentNight else entry.accentDay
    val title = stringResource(entry.title)
    val body = stringResource(entry.body)
    val shape = RoundedCornerShape(14.dp)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Row(
        modifier.clip(shape)
            .background(
                Brush.linearGradient(
                    if (night) listOf(
                        Color(0xFF2C2463).copy(alpha = .84f),
                        accent.copy(alpha = .14f),
                        Color(0xFF151B40).copy(alpha = .88f),
                    ) else listOf(
                        Color.White.copy(alpha = .54f),
                        accent.copy(alpha = .18f),
                        Color.Transparent,
                    ),
                ),
            )
            .border(1.dp, Brush.linearGradient(
                if (night) listOf(
                    Color.White.copy(alpha = .26f),
                    accent.copy(alpha = .30f),
                ) else listOf(
                    Color.White.copy(alpha = .88f),
                    accent.copy(alpha = .24f),
                ),
            ), shape)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.tools_open, title), onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp)
            .testTag("tool-card-${entry.key}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KxLineIcon(entry.icon, if (night) accent else accent.copy(alpha = .9f), Modifier.size(26.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                title,
                color = kxInk(night),
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
            )
            Text(
                body,
                color = kxMuted(night),
                fontSize = 10.sp,
                lineHeight = 14.sp,
                textAlign = TextAlign.Start,
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            KxMiniOrb(night, 26.dp)
            KxLineIcon(
                if (rtl) KxIconKind.BACK else KxIconKind.FORWARD,
                kxMuted(night),
                Modifier.size(10.dp),
            )
        }
    }
}

@Composable
private fun StorageHealthCard(
    entry: ToolEntry,
    night: Boolean,
    onClick: () -> Unit,
) {
    val accent = if (night) entry.accentNight else entry.accentDay
    val title = stringResource(entry.title)
    val body = stringResource(entry.body)
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(
                Brush.linearGradient(
                    if (night) listOf(
                        Color(0xFF2C2463).copy(alpha = .84f),
                        accent.copy(alpha = .16f),
                        kxCyan(night).copy(alpha = .10f),
                    ) else listOf(
                        Color.White.copy(alpha = .54f),
                        accent.copy(alpha = .18f),
                        kxCyan(night).copy(alpha = .12f),
                    ),
                ),
            )
            .border(1.dp, Brush.linearGradient(
                if (night) listOf(
                    Color.White.copy(alpha = .26f),
                    accent.copy(alpha = .34f),
                ) else listOf(
                    Color.White.copy(alpha = .88f),
                    accent.copy(alpha = .26f),
                ),
            ), shape)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.tools_open, title), onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp)
            .testTag("tools-storage-health"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        KxLineIcon(entry.icon, if (night) accent else accent.copy(alpha = .9f), Modifier.size(32.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = kxInk(night), fontSize = 15.sp, lineHeight = 20.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Start)
            Text(body, color = kxMuted(night), fontSize = 10.5.sp, lineHeight = 15.sp,
                textAlign = TextAlign.Start)
        }
        KxMiniOrb(night, 32.dp)
    }
}
