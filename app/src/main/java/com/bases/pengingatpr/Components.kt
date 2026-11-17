package com.bases.pengingatpr

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun isDark(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** Bentuk "cookie" bergelombang khas M3 Expressive. */
fun cookieShape(sides: Int = 9, amp: Float = 0.07f): Shape = GenericShape { size, _ ->
    val cx = size.width / 2f
    val cy = size.height / 2f
    val base = minOf(cx, cy) / (1f + amp)
    val steps = 140
    for (i in 0..steps) {
        val t = (i.toFloat() / steps) * 2f * PI.toFloat()
        val r = base * (1f + amp * cos(sides * t))
        val x = cx + r * cos(t)
        val y = cy + r * sin(t)
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** Tombol dengan bentuk yang berubah (morph) saat ditekan. */
@Composable
fun ExpButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    container: Color = MaterialTheme.colorScheme.primary,
    content: Color = MaterialTheme.colorScheme.onPrimary,
    height: Dp = 56.dp,
    enabled: Boolean = true,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val corner by animateDpAsState(
        if (pressed) 12.dp else height / 2,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "corner",
    )
    val bg = if (enabled) container else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val fg = if (enabled) content else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    Row(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(corner))
            .background(bg)
            .clickable(interactionSource = src, indication = LocalIndication.current, enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(22.dp))
            Box(Modifier.size(8.dp))
        }
        Text(text, color = fg, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ExpTonalButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    ExpButton(
        text, onClick, modifier, icon,
        container = MaterialTheme.colorScheme.secondaryContainer,
        content = MaterialTheme.colorScheme.onSecondaryContainer,
        height = 48.dp,
    )
}

/** Chip yang berubah bentuk menjadi kapsul saat dipilih. */
@Composable
fun ExpChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val corner by animateDpAsState(
        if (selected) 20.dp else 10.dp,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "chip",
    )
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "chipbg",
    )
    val fg = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(corner))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
            Box(Modifier.size(6.dp))
        }
        Text(label, color = fg, style = MaterialTheme.typography.labelLarge)
    }
}

/** Button group tersambung: item terpilih menjadi kapsul. */
@Composable
fun ExpSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            val corner by animateDpAsState(
                if (sel) 26.dp else 8.dp,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
                label = "seg",
            )
            val bg by animateColorAsState(
                if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                label = "segbg",
            )
            val fg = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(corner))
                    .background(bg)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
    }
}

/** Indikator progres bergelombang (wavy). */
@Composable
fun WavyProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    track: Color = MaterialTheme.colorScheme.primaryContainer,
) {
    val t = rememberInfiniteTransition(label = "wave")
    val phase by t.animateFloat(
        0f, (2f * PI).toFloat(),
        infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    Canvas(modifier.fillMaxWidth().height(20.dp)) {
        val w = size.width
        val cy = size.height / 2f
        val sw = 6.dp.toPx()
        val amp = 4.dp.toPx()
        val wl = 26.dp.toPx()
        val p = progress.coerceIn(0f, 1f)
        val end = w * p
        drawLine(track, Offset(end.coerceAtLeast(sw), cy), Offset(w - sw / 2f, cy), strokeWidth = sw, cap = StrokeCap.Round)
        if (end > 1f) {
            val path = Path()
            var x = sw / 2f
            val stop = end.coerceAtLeast(sw)
            path.moveTo(x, cy + amp * sin(phase + x / wl * 2f * PI.toFloat()))
            while (x < stop) {
                x += 2f
                path.lineTo(x, cy + amp * sin(phase + x / wl * 2f * PI.toFloat()))
            }
            drawPath(path, color, style = Stroke(width = sw, cap = StrokeCap.Round))
        }
    }
}

/** Indikator memuat: cookie berputar dan berdenyut. */
@Composable
fun LoadingCookie(modifier: Modifier = Modifier, size: Dp = 64.dp) {
    val t = rememberInfiniteTransition(label = "load")
    val rot by t.animateFloat(0f, 360f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "rot")
    val sc by t.animateFloat(0.85f, 1.1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "sc")
    Box(
        modifier
            .size(size)
            .graphicsLayer {
                rotationZ = rot
                scaleX = sc
                scaleY = sc
            }
            .clip(cookieShape(10, 0.12f))
            .background(MaterialTheme.colorScheme.primary),
    )
}

/** Lencana cookie berisi teks pendek atau ikon. */
@Composable
fun CookieBadge(
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    sides: Int = 8,
) {
    Box(
        modifier = modifier.size(size).clip(cookieShape(sides, 0.07f)).background(container),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = content, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
    }
}

/** Kotak centang yang berubah bentuk saat dicentang. */
@Composable
fun ExpCheck(checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val corner by animateDpAsState(
        if (checked) 16.dp else 9.dp,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "chk",
    )
    val bg by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "chkbg",
    )
    val borderCol = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(RoundedCornerShape(corner))
            .background(bg)
            .pointerInput(Unit) { detectTapGestures { onToggle() } },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(32.dp)) {
            if (!checked) {
                drawRoundRect(
                    borderCol,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
        if (checked) {
            Icon(
                Icons.Rounded.Check,
                null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
