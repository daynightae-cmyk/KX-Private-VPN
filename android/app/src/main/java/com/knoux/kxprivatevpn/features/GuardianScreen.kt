package com.knoux.kxprivatevpn.features

import android.content.ClipData
import android.content.ClipboardManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.text.format.Formatter
import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.designsystem.Atmosphere
import com.knoux.kxprivatevpn.designsystem.GlassSurface
import com.knoux.kxprivatevpn.designsystem.KxGlassTheme
import com.knoux.kxprivatevpn.designsystem.KxOrb
import com.knoux.kxprivatevpn.designsystem.KxPageHeader
import com.knoux.kxprivatevpn.designsystem.KxSectionHeader
import com.knoux.kxprivatevpn.designsystem.glassForeground
import com.knoux.kxprivatevpn.launch.IntroTimeline
import com.knoux.kxprivatevpn.launch.rememberReducedMotion
import com.knoux.kxprivatevpn.platform.guardian.GuardianCode
import com.knoux.kxprivatevpn.platform.guardian.GuardianEvidence
import com.knoux.kxprivatevpn.platform.guardian.GuardianPolicy
import com.knoux.kxprivatevpn.platform.guardian.GuardianPassport
import com.knoux.kxprivatevpn.platform.guardian.GuardianPriority
import com.knoux.kxprivatevpn.platform.guardian.GuardianRecommendation
import com.knoux.kxprivatevpn.platform.vpn.VpnPhase
import com.knoux.kxprivatevpn.platform.vpn.VpnState
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.delay

@Composable
fun GuardianRoute(
    night: Boolean,
    vpn: VpnState,
    hasProfile: Boolean,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val model: GuardianViewModel = viewModel()
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(Unit) { model.scan() }
    // Observe network changes only while this screen is visible. No polling, no HTTP requests.
    DisposableEffect(context, model) {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = model.refreshNetwork()
            override fun onLost(network: Network) = model.refreshNetwork()
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) =
                model.refreshNetwork()
        }
        val registered = try {
            manager.registerDefaultNetworkCallback(callback)
            true
        } catch (_: Exception) {
            false
        }
        onDispose {
            if (registered) try { manager.unregisterNetworkCallback(callback) } catch (_: Exception) { }
        }
    }
    GuardianScreen(night, vpn, hasProfile, state, onBack, onNavigate, model::scan)
}

/** A true on-device dashboard: all data is supplied by OS or existing KX service measurements. */
@Composable
fun GuardianScreen(
    night: Boolean,
    vpn: VpnState,
    hasProfile: Boolean,
    state: GuardianUiState,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onScan: () -> Unit,
) {
    val foreground = glassForeground(night)
    val muted = foreground.copy(alpha = .78f)
    val context = LocalContext.current
    val now by produceState(initialValue = System.currentTimeMillis(), state.scanned) {
        value = System.currentTimeMillis()
        if (state.scanned) while (true) {
            delay(30_000)
            value = System.currentTimeMillis()
        }
    }
    val evidence = remember(vpn, hasProfile, state, now) {
        GuardianEvidence(
            vpn = vpn,
            hasProfile = hasProfile,
            network = state.network,
            storage = state.storage,
            visibleApps = state.visibleApps,
            nowMillis = now,
            networkReadAtMillis = state.networkReadAtMillis,
            storageReadAtMillis = state.storageReadAtMillis,
            appsReadAtMillis = state.appsReadAtMillis,
        )
    }
    val findings = remember(evidence, state.scanned) {
        if (!state.scanned) emptyList()
        else GuardianPolicy.recommendations(evidence)
    }
    val summary = remember(findings) { GuardianPolicy.summary(findings) }
    val reducedMotion = rememberReducedMotion()
    var passportCopied by remember(state.measuredAtMillis) { mutableStateOf(false) }
    var passportFailed by remember(state.measuredAtMillis) { mutableStateOf(false) }
    KxGlassTheme(night) {
        Box(Modifier.fillMaxSize()) {
            Atmosphere(night, 0f)
            Column(
                Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("route-guardian"),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                KxPageHeader(
                    title = stringResource(R.string.guardian_title),
                    night = night,
                    backLabel = stringResource(R.string.back),
                    onBack = onBack,
                )
                Text(
                    stringResource(R.string.guardian_subtitle), color = muted,
                    fontSize = 14.sp, lineHeight = 21.sp,
                )
                GlassSurface(night, radius = 28.dp) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        val rotation = if (state.scanning && !reducedMotion) {
                            val transition = rememberInfiniteTransition(label = "guardian-orbit")
                            val animated by transition.animateFloat(
                                initialValue = 0f, targetValue = 360f,
                                animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
                                label = "guardian-orb-rotation",
                            )
                            animated
                        } else 0f
                        Box(
                            Modifier.size(176.dp)
                                .clickable(
                                    enabled = !state.scanning,
                                    role = Role.Button,
                                    onClickLabel = stringResource(R.string.guardian_scan),
                                    onClick = onScan,
                                )
                                .testTag("guardian-orb"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Canvas(Modifier.fillMaxSize()) {
                                val line = if (night) Color(0xFF6DEBFF) else Color(0xFF6035CA)
                                drawCircle(line.copy(alpha = .22f), radius = size.minDimension * .48f,
                                    style = Stroke(2.dp.toPx()))
                                if (state.scanning) {
                                    drawArc(
                                        color = line, startAngle = rotation - 90f, sweepAngle = 94f,
                                        useCenter = false, topLeft = Offset(6.dp.toPx(), 6.dp.toPx()),
                                        size = androidx.compose.ui.geometry.Size(
                                            size.width - 12.dp.toPx(), size.height - 12.dp.toPx(),
                                        ),
                                        style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
                                    )
                                }
                            }
                            KxOrb(
                                progress = 1f, frame = IntroTimeline.frame(1f),
                                modifier = Modifier.size(128.dp)
                                    .graphicsLayer { rotationZ = if (state.scanning) rotation else 0f },
                            )
                        }
                        Text(
                            stringResource(
                                when {
                                    state.scanning -> R.string.guardian_scanning
                                    state.scanned -> R.string.guardian_scan_complete
                                    else -> R.string.guardian_not_scanned
                                },
                            ),
                            color = foreground, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        )
                        if (state.scanning) LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().testTag("guardian-scan-progress")
                        )
                        OutlinedButton(
                            onClick = onScan, enabled = !state.scanning,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .testTag("guardian-scan"),
                        ) { Text(stringResource(R.string.guardian_scan)) }
                        Text(
                            stringResource(R.string.guardian_privacy_scope),
                            color = muted, fontSize = 12.sp, lineHeight = 17.sp,
                        )
                    }
                }
                if (state.scanned) {
                    GlassSurface(
                        night, modifier = Modifier.testTag("guardian-trust-passport"),
                        radius = 22.dp,
                    ) {
                        Text(
                            stringResource(R.string.guardian_passport_title),
                            fontSize = 19.sp, fontWeight = FontWeight.Bold,
                            color = foreground,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.guardian_passport_counts,
                                summary.actions, summary.reviews, summary.information),
                            color = muted, fontSize = 14.sp,
                            modifier = Modifier.testTag("guardian-priority-counts"),
                        )
                        Text(
                            stringResource(R.string.guardian_passport_provenance),
                            color = muted, fontSize = 12.sp, lineHeight = 18.sp,
                        )
                        summary.highestPriorityRoute?.let { route ->
                            Spacer(Modifier.height(9.dp))
                            OutlinedButton(
                                onClick = { onNavigate(route) },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    .testTag("guardian-first-action"),
                            ) { Text(stringResource(R.string.guardian_priority_action)) }
                        }
                        Spacer(Modifier.height(6.dp))
                        TextButton(
                            onClick = {
                                try {
                                    val clipboard = requireNotNull(
                                        context.getSystemService(ClipboardManager::class.java)
                                    )
                                    clipboard.setPrimaryClip(ClipData.newPlainText(
                                        "KX Guardian diagnostic",
                                        GuardianPassport.render(evidence, findings),
                                    ))
                                    passportCopied = true
                                    passportFailed = false
                                } catch (_: Exception) {
                                    passportCopied = false
                                    passportFailed = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .testTag("guardian-copy-passport"),
                        ) { Text(stringResource(R.string.guardian_copy_passport)) }
                        if (passportCopied || passportFailed) {
                            Text(
                                stringResource(if (passportCopied)
                                    R.string.guardian_copied else R.string.guardian_copy_failed),
                                color = muted, fontSize = 12.sp,
                                modifier = Modifier.testTag("guardian-copy-result"),
                            )
                        }
                    }
                }
                KxSectionHeader(stringResource(R.string.guardian_evidence), night)
                val vpnStatus = when (vpn.phase) {
                    VpnPhase.CONNECTED -> if (vpn.session != null) R.string.guardian_vpn_tunnel
                        else R.string.guardian_unknown
                    VpnPhase.IDLE -> R.string.guardian_vpn_disconnected
                    VpnPhase.CONNECTING, VpnPhase.DISCONNECTING -> R.string.guardian_vpn_transition
                    VpnPhase.ERROR -> R.string.guardian_vpn_error
                }
                val handshake = vpn.session?.latestHandshakeMillis?.takeIf { it > 0 }?.let {
                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it))
                } ?: stringResource(R.string.guardian_unknown)
                GuardianEvidenceCard(
                    night, stringResource(R.string.guardian_vpn), stringResource(vpnStatus),
                    stringResource(R.string.guardian_handshake, handshake),
                    "guardian-evidence-vpn", { onNavigate("security") },
                )
                val network = state.network
                val networkFresh = GuardianPolicy.fresh(
                    now, state.networkReadAtMillis, GuardianPolicy.NETWORK_MAX_AGE_MILLIS
                )
                val networkStatus = when {
                    !state.scanned -> R.string.guardian_unknown
                    network == null -> R.string.guardian_unknown
                    !networkFresh -> R.string.guardian_stale
                    !network.present -> R.string.guardian_network_offline
                    network.captive -> R.string.guardian_network_captive
                    network.validated -> R.string.guardian_network_validated
                    else -> R.string.guardian_network_unverified
                }
                val networkDetail = if (network == null) {
                    stringResource(R.string.guardian_network_unknown_scope)
                } else if (!networkFresh) {
                    stringResource(R.string.guardian_stale_scope)
                } else {
                    stringResource(
                        R.string.guardian_network_details,
                        network.transports.joinToString().ifEmpty { "—" },
                        network.dns.size,
                    )
                }
                GuardianEvidenceCard(
                    night, stringResource(R.string.guardian_network),
                    stringResource(networkStatus), networkDetail,
                    "guardian-evidence-network", { onNavigate("network") },
                )
                val storageFresh = GuardianPolicy.fresh(
                    now, state.storageReadAtMillis, GuardianPolicy.STORAGE_MAX_AGE_MILLIS
                )
                val total = state.storage?.totalBytes
                val free = state.storage?.availableBytes
                val storageStatus = if (!storageFresh && state.storage != null) {
                    stringResource(R.string.guardian_stale)
                } else if (storageFresh && total != null && free != null && total > 0 && free in 0..total) {
                    val percent = (free.toDouble() * 100.0 / total.toDouble()).toInt()
                    stringResource(R.string.guardian_storage_free,
                        Formatter.formatFileSize(context, free), percent)
                } else stringResource(R.string.guardian_unknown)
                val cache = state.storage?.cacheBytes
                val storageDetail = if (!storageFresh && state.storage != null) {
                    stringResource(R.string.guardian_stale_scope)
                } else if (cache == null) stringResource(R.string.guardian_storage_scope)
                    else stringResource(R.string.guardian_cache_measured, Formatter.formatFileSize(context, cache))
                GuardianEvidenceCard(
                    night, stringResource(R.string.guardian_storage), storageStatus, storageDetail,
                    "guardian-evidence-storage", { onNavigate("storage") },
                )
                GuardianEvidenceCard(
                    night, stringResource(R.string.guardian_apps),
                    if (state.visibleApps != null && !GuardianPolicy.fresh(
                        now, state.appsReadAtMillis, GuardianPolicy.APPS_MAX_AGE_MILLIS
                    )) stringResource(R.string.guardian_stale)
                    else state.visibleApps?.let {
                        stringResource(R.string.guardian_visible_apps, it)
                    } ?: stringResource(R.string.guardian_unknown),
                    stringResource(R.string.guardian_apps_scope),
                    "guardian-evidence-apps", { onNavigate("apps") },
                )
                if (state.scanned) {
                    state.measuredAtMillis?.let {
                        Text(
                            stringResource(
                                R.string.guardian_last_check,
                                DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it)),
                            ),
                            color = muted, fontSize = 12.sp,
                        )
                    }
                    KxSectionHeader(stringResource(R.string.guardian_recommendations), night)
                    findings.forEach { recommendation ->
                        GuardianRecommendationCard(
                            recommendation, night,
                            onNavigate = onNavigate,
                        )
                    }
                }
                Text(
                    stringResource(R.string.guardian_disclaimer), color = muted,
                    fontSize = 12.sp, lineHeight = 18.sp,
                )
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun GuardianEvidenceCard(
    night: Boolean, title: String, value: String, detail: String,
    tag: String, onClick: () -> Unit,
) {
    val color = glassForeground(night)
    GlassSurface(
        night, modifier = Modifier.clickable(
            role = Role.Button, onClickLabel = title, onClick = onClick
        ).testTag(tag), radius = 20.dp,
    ) {
        Text(title, color = color.copy(alpha = .76f), fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = color, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text(detail, color = color.copy(alpha = .78f), fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun GuardianRecommendationCard(
    finding: GuardianRecommendation, night: Boolean, onNavigate: (String) -> Unit,
) {
    val (titleRes, detailRes) = guardianCopy(finding.code)
    val color = glassForeground(night)
    val accent = when (finding.priority) {
        GuardianPriority.ACTION -> if (night) Color(0xFFFFB7A8) else Color(0xFF8E302F)
        GuardianPriority.REVIEW -> if (night) Color(0xFFFFD18A) else Color(0xFF70411C)
        GuardianPriority.INFO -> if (night) Color(0xFF8DEBFF) else Color(0xFF3921A1)
    }
    GlassSurface(night, modifier = Modifier.testTag("guardian-finding-${finding.code.name.lowercase()}"),
        radius = 20.dp) {
        Text(stringResource(titleRes), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accent)
        Spacer(Modifier.height(5.dp))
        Text(stringResource(detailRes), fontSize = 13.sp, lineHeight = 19.sp, color = color)
        finding.route?.let { route ->
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = { onNavigate(route) },
                modifier = Modifier.heightIn(min = 48.dp).testTag("guardian-open-${finding.code.name.lowercase()}")) {
                Text(stringResource(R.string.guardian_open_service))
            }
        }
    }
}

private fun guardianCopy(code: GuardianCode): Pair<Int, Int> = when (code) {
    GuardianCode.IMPORT_PROFILE -> R.string.guardian_import_title to R.string.guardian_import_body
    GuardianCode.START_VPN -> R.string.guardian_start_title to R.string.guardian_start_body
    GuardianCode.VPN_ISSUE -> R.string.guardian_vpn_issue_title to R.string.guardian_vpn_issue_body
    GuardianCode.VPN_TRANSITION -> R.string.guardian_transition_title to R.string.guardian_transition_body
    GuardianCode.VERIFY_VPN_PATH -> R.string.guardian_verify_title to R.string.guardian_verify_body
    GuardianCode.VPN_TRANSPORT_NOT_REPORTED -> R.string.guardian_transport_title to R.string.guardian_transport_body
    GuardianCode.NETWORK_UNAVAILABLE -> R.string.guardian_offline_title to R.string.guardian_offline_body
    GuardianCode.CAPTIVE_PORTAL -> R.string.guardian_captive_title to R.string.guardian_captive_body
    GuardianCode.NETWORK_UNVERIFIED -> R.string.guardian_net_unverified_title to R.string.guardian_net_unverified_body
    GuardianCode.NETWORK_UNKNOWN -> R.string.guardian_net_unknown_title to R.string.guardian_net_unknown_body
    GuardianCode.NETWORK_STALE -> R.string.guardian_net_stale_title to R.string.guardian_net_stale_body
    GuardianCode.DNS_NOT_REPORTED -> R.string.guardian_dns_title to R.string.guardian_dns_body
    GuardianCode.STORAGE_LOW -> R.string.guardian_low_storage_title to R.string.guardian_low_storage_body
    GuardianCode.STORAGE_CRITICAL -> R.string.guardian_critical_storage_title to R.string.guardian_critical_storage_body
    GuardianCode.STORAGE_STALE -> R.string.guardian_storage_stale_title to R.string.guardian_storage_stale_body
    GuardianCode.STORAGE_UNKNOWN -> R.string.guardian_storage_unknown_title to R.string.guardian_storage_unknown_body
    GuardianCode.REVIEW_KX_CACHE -> R.string.guardian_cache_title to R.string.guardian_cache_body
    GuardianCode.APPS_UNKNOWN -> R.string.guardian_apps_unknown_title to R.string.guardian_apps_unknown_body
    GuardianCode.APPS_STALE -> R.string.guardian_apps_stale_title to R.string.guardian_apps_stale_body
    GuardianCode.REVIEW_APPS -> R.string.guardian_apps_review_title to R.string.guardian_apps_review_body
    GuardianCode.NO_ACTIONS -> R.string.guardian_no_actions_title to R.string.guardian_no_actions_body
}
