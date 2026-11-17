package com.bases.pengingatpr

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private val FOCUS_CHOICES = listOf(15, 25, 45, 60)
private val BREAK_CHOICES = listOf(5, 10, 15)

@Composable
fun FocusScreen(nav: Nav, prId: String?) {
    val ctx = LocalContext.current
    val view = LocalView.current
    var focusMin by remember { mutableStateOf(25) }
    var breakMin by remember { mutableStateOf(5) }
    var isBreak by remember { mutableStateOf(false) }
    var remaining by remember { mutableStateOf(25 * 60) }
    var running by remember { mutableStateOf(false) }
    var selectedPr by remember { mutableStateOf(prId ?: "") }

    val total = (if (isBreak) breakMin else focusMin) * 60
    val cs = MaterialTheme.colorScheme

    DisposableEffect(running) {
        view.keepScreenOn = running
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(running, isBreak) {
        while (running && remaining > 0) {
            delay(1000)
            remaining -= 1
        }
        if (running && remaining <= 0) {
            if (!isBreak) {
                Repo.addFocus(FocusSession(System.currentTimeMillis(), focusMin, selectedPr))
                Notifier.showFocusDone(ctx, focusMin)
                nav.toast("Sesi fokus selesai • $focusMin menit tercatat")
                isBreak = true
                remaining = breakMin * 60
            } else {
                isBreak = false
                remaining = focusMin * 60
            }
            running = false
        }
    }

    val t = rememberInfiniteTransition(label = "focus")
    val rot by t.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "rot")

    val todayStart = java.time.LocalDate.now().atStartOfDay().toMs()
    val todayMin = Repo.focusLog.filter { it.at >= todayStart }.sumOf { it.minutes }
    val active = Repo.prs.filter { !it.done }.sortedBy { it.due }

    ScreenFrame(title = if (isBreak) "Istirahat" else "Mode fokus", onBack = { nav.pop() }) { inner ->
        Column(
            Modifier.padding(inner).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            val progress = if (total == 0) 0f else 1f - remaining.toFloat() / total
            Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
                val track = cs.surfaceContainerHighest
                val arc = if (isBreak) cs.tertiary else cs.primary
                Canvas(Modifier.size(300.dp)) {
                    val sw = 16.dp.toPx()
                    val inset = sw / 2f
                    val sz = Size(size.width - sw, size.height - sw)
                    drawArc(track, -90f, 360f, false, Offset(inset, inset), sz, style = Stroke(sw, cap = StrokeCap.Round))
                    drawArc(arc, -90f, 360f * progress, false, Offset(inset, inset), sz, style = Stroke(sw, cap = StrokeCap.Round))
                }
                Box(
                    Modifier.size(224.dp)
                        .graphicsLayer { rotationZ = if (running) rot else 0f }
                        .clip(cookieShape(12, 0.06f))
                        .background(if (isBreak) cs.tertiaryContainer else cs.primaryContainer),
                )
                Text(
                    "%02d:%02d".format(remaining / 60, remaining % 60),
                    style = MaterialTheme.typography.displayMedium,
                    color = if (isBreak) cs.onTertiaryContainer else cs.onPrimaryContainer,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ExpButton(
                    if (running) "Jeda" else "Mulai",
                    { running = !running },
                    Modifier.weight(1f),
                    icon = if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    height = 64.dp,
                )
                ExpTonalButton(
                    "Atur ulang",
                    { running = false; isBreak = false; remaining = focusMin * 60 },
                    Modifier,
                    Icons.Rounded.Replay,
                )
            }

            if (!running && !isBreak) {
                Text("Durasi fokus", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(FOCUS_CHOICES) { m ->
                        ExpChip("$m menit", focusMin == m, { focusMin = m; remaining = m * 60 })
                    }
                }
                Text("Durasi istirahat", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(BREAK_CHOICES) { m -> ExpChip("$m menit", breakMin == m, { breakMin = m }) }
                }
            }

            if (!isBreak) {
                Text("Mengerjakan", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    item { ExpChip("Tanpa PR", selectedPr.isBlank(), { selectedPr = "" }) }
                    items(active, key = { it.id }) { p ->
                        ExpChip(p.title.take(22), selectedPr == p.id, { selectedPr = p.id })
                    }
                }
            }

            Text(
                "Total fokus hari ini: $todayMin menit. Layar tetap menyala saat timer berjalan; tetap buka halaman ini sampai sesi selesai.",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
            )
        }
    }
}
