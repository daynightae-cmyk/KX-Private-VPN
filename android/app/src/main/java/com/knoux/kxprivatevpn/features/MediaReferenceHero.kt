package com.knoux.kxprivatevpn.features

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.designsystem.KxGlassPanel
import com.knoux.kxprivatevpn.designsystem.KxGlassIconTile
import com.knoux.kxprivatevpn.designsystem.KxGradientAction
import com.knoux.kxprivatevpn.designsystem.KxIconKind
import com.knoux.kxprivatevpn.designsystem.KxLineIcon
import com.knoux.kxprivatevpn.designsystem.KxOrbBadge
import com.knoux.kxprivatevpn.designsystem.kxCyan
import com.knoux.kxprivatevpn.designsystem.kxInk
import com.knoux.kxprivatevpn.designsystem.kxMuted
import com.knoux.kxprivatevpn.designsystem.kxRose
import com.knoux.kxprivatevpn.designsystem.kxViolet
import com.knoux.kxprivatevpn.launch.rememberReducedMotion
import com.knoux.kxprivatevpn.platform.media.MediaQuality

/**
 * Owner reference 01 Media Studio's native, data-backed hero.
 * No screenshot bitmap, fictional inventory, invented quality percentage or unverified savings.
 */
@Composable
internal fun MediaReferenceHero(night: Boolean, processing: Boolean) {
    val reducedMotion = rememberReducedMotion()
    val rotation = if (processing && !reducedMotion) {
        val spin = rememberInfiniteTransition(label = "media-orbit")
        val value by spin.animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
            label = "media-orbit-rotation",
        )
        value
    } else 0f
    Box(Modifier.fillMaxWidth().height(244.dp).testTag("media-ref-hero"),
        contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(244.dp)) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val accent = if (night) Color(0xFF6CEAFF) else Color(0xFF287BD0)
            val rose = if (night) Color(0xFFE476FF) else Color(0xFF9C48DC)
            drawCircle(Brush.radialGradient(
                listOf(rose.copy(alpha = .28f), accent.copy(alpha = .12f), Color.Transparent),
                center = c, radius = size.minDimension * .49f), radius = size.minDimension * .49f)
            drawCircle(Brush.sweepGradient(
                listOf(rose, accent, Color.Transparent, rose.copy(alpha = .55f), rose),
                center = c),
                radius = size.minDimension * .46f,
                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round), alpha = .75f)
            drawCircle(accent.copy(alpha = .27f), radius = size.minDimension * .40f,
                style = Stroke(1.dp.toPx()))
            drawArc(Brush.sweepGradient(listOf(accent, rose, Color.Transparent, accent), c),
                startAngle = 200f + rotation, sweepAngle = 128f,
                useCenter = false,
                topLeft = Offset(size.width * .08f, size.height * .08f),
                size = androidx.compose.ui.geometry.Size(size.width * .84f, size.height * .84f),
                style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
            repeat(17) { index ->
                val angle = Math.toRadians((index * (360.0 / 17) + rotation * .2f))
                val radius = size.minDimension * .43f
                val dot = Offset(
                    c.x + kotlin.math.cos(angle).toFloat() * radius,
                    c.y + kotlin.math.sin(angle).toFloat() * radius,
                )
                drawCircle(if (index % 2 == 0) accent else rose,
                    radius = (if (index % 3 == 0) 2.3f else 1.2f).dp.toPx(),
                    center = dot, alpha = .7f)
            }
        }
        KxOrbBadge(184.dp, night = night,
            modifier = Modifier.graphicsLayer { rotationZ = if (processing) rotation else 0f })
        KxGlassIconTile(night, KxIconKind.LAYERS, kxRose(night),
            Modifier.align(Alignment.CenterStart).offset(y = (-42).dp), size = 54.dp)
        KxGlassIconTile(night, KxIconKind.CLAPPER, kxCyan(night),
            Modifier.align(Alignment.CenterEnd).offset(y = 66.dp), size = 54.dp)
    }
}

/** Summary is scoped to the chosen SAF folder and the most recent completed scan. */
@Composable
internal fun MediaReferenceMetrics(night: Boolean, state: MediaSlimUiState) {
    val scan = state.scan
    val images = scan?.images?.size?.toString() ?: "—"
    val videos = scan?.videos?.size?.toString() ?: "—"
    val quality = stringResource(when (state.quality) {
        MediaQuality.HIGH -> R.string.media_quality_high
        MediaQuality.BALANCED -> R.string.media_quality_balanced
        MediaQuality.SMALL -> R.string.media_quality_small
    })
    KxGlassPanel(night, modifier = Modifier.testTag("media-ref-metrics"),
        radius = 24.dp, padding = PaddingValues(vertical = 14.dp, horizontal = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MediaReferenceMetric(night, R.string.media_ref_images, images,
                KxIconKind.LAYERS, Modifier.weight(1f), "media-ref-images")
            Box(Modifier.width(1.dp).height(52.dp)
                .background(kxMuted(night).copy(alpha = .30f)))
            MediaReferenceMetric(night, R.string.media_ref_videos, videos,
                KxIconKind.CLAPPER, Modifier.weight(1f), "media-ref-videos")
            Box(Modifier.width(1.dp).height(52.dp)
                .background(kxMuted(night).copy(alpha = .30f)))
            MediaReferenceMetric(night, R.string.media_ref_quality, quality,
                KxIconKind.SPARKLE, Modifier.weight(1f), "media-ref-quality")
        }
        Text(stringResource(R.string.media_ref_scope), color = kxMuted(night),
            fontSize = 11.sp, lineHeight = 16.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
    }
}

@Composable
private fun MediaReferenceMetric(
    night: Boolean, label: Int, value: String, icon: KxIconKind,
    modifier: Modifier = Modifier, tag: String,
) {
    Column(modifier.testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)) {
        KxLineIcon(icon, kxCyan(night), Modifier.size(23.dp))
        Text(stringResource(label), color = kxMuted(night),
            fontSize = 11.sp, lineHeight = 16.sp, maxLines = 2,
            textAlign = TextAlign.Center)
        Text(value, color = kxInk(night), fontSize = 14.sp,
            lineHeight = 20.sp, fontWeight = FontWeight.Bold,
            maxLines = 2, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun MediaReferenceFolder(night: Boolean, state: MediaSlimUiState,
    onChooseFolder: () -> Unit,
) {
    KxGlassPanel(night,
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)
            .clip(RoundedCornerShape(27.dp))
            .clickable(
                role = Role.Button,
                enabled = !state.scanning && !state.compressing,
                onClickLabel = stringResource(R.string.media_choose_folder),
                onClick = onChooseFolder,
            ).testTag("media-ref-folder"),
        radius = 27.dp, padding = PaddingValues(horizontal = 16.dp, vertical = 13.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KxLineIcon(KxIconKind.DATABASE, kxCyan(night), Modifier.size(23.dp))
            Text(state.treeName ?: stringResource(R.string.media_choose_folder),
                modifier = Modifier.weight(1f), color = kxInk(night),
                fontSize = 15.sp, textAlign = TextAlign.Center,
                maxLines = 1)
        }
    }
}

/**
 * The reference's primary CTA must NEVER silently compress: before a selection,
 * it chooses/scans a folder; after the user selects eligible images it opens the
 * existing explicit compression confirmation dialog.
 */
@Composable
internal fun MediaReferenceAction(
    night: Boolean, state: MediaSlimUiState,
    onChooseFolder: () -> Unit, onScan: () -> Unit, onRequestCompression: () -> Unit,
) {
    val (label, enabled, action) = when {
        state.scanning -> Triple(stringResource(R.string.media_scanning), false, onScan)
        state.compressing -> Triple(stringResource(R.string.media_ref_processing), false, onScan)
        state.treeUri == null -> Triple(stringResource(R.string.media_choose_folder), true, onChooseFolder)
        state.scan == null -> Triple(stringResource(R.string.media_rescan), true, onScan)
        state.selectedSupported.isEmpty() -> Triple(
            stringResource(R.string.media_ref_select), false, onRequestCompression)
        else -> Triple(stringResource(R.string.media_compress), true, onRequestCompression)
    }
    KxGradientAction(label, night, enabled, "media-ref-primary-action",
        modifier = Modifier.heightIn(min = 52.dp), icon = KxIconKind.WAND, onClick = action)
}
