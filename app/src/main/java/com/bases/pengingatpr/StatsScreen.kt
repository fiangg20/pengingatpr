package com.bases.pengingatpr

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun StatsScreen(nav: Nav) {
    val prs = Repo.prs
    val now = System.currentTimeMillis()
    val cs = MaterialTheme.colorScheme
    val done = prs.filter { it.done }
    val active = prs.filter { !it.done }
    val overdue = active.count { it.due < now }
    val onTime = done.count { it.doneAt <= it.due }
    val onTimePct = if (done.isEmpty()) 0 else onTime * 100 / done.size

    val today = LocalDate.now()
    val doneDays = done.filter { it.doneAt > 0 }.map { it.doneAt.toLdt().toLocalDate() }.toSet()
    var streak = 0
    var d = if (doneDays.contains(today)) today else today.minusDays(1)
    while (doneDays.contains(d)) {
        streak++
        d = d.minusDays(1)
    }

    val last7 = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val counts = last7.map { day -> done.count { it.doneAt > 0 && it.doneAt.toLdt().toLocalDate() == day } }
    val maxCount = (counts.maxOrNull() ?: 0).coerceAtLeast(1)

    val weekStart = today.minusDays(6).atStartOfDay().toMs()
    val focusWeek = Repo.focusLog.filter { it.at >= weekStart }.sumOf { it.minutes }
    val focusAll = Repo.focusLog.sumOf { it.minutes }

    val bySubject = prs.groupBy { it.subject.trim().ifBlank { "Umum" } }
        .map { Triple(it.key, it.value.count { p -> p.done }, it.value.size) }
        .sortedByDescending { it.third }

    ScreenFrame(title = "Statistik", onBack = { nav.pop() }) { inner ->
        Column(
            Modifier.padding(inner).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 40.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    "$streak", "Hari beruntun", Modifier.weight(1f),
                    container = cs.primaryContainer, content = cs.onPrimaryContainer,
                    shape = RoundedCornerShape(topStart = 44.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp),
                )
                StatTile(
                    "${done.size}", "PR selesai", Modifier.weight(1f),
                    shape = RoundedCornerShape(topStart = 14.dp, topEnd = 44.dp, bottomEnd = 14.dp, bottomStart = 14.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    "$onTimePct%", "Tepat waktu", Modifier.weight(1f),
                    container = cs.tertiaryContainer, content = cs.onTertiaryContainer,
                    shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 44.dp),
                )
                StatTile(
                    "$overdue", "Terlambat", Modifier.weight(1f),
                    container = cs.errorContainer, content = cs.onErrorContainer,
                    shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomEnd = 44.dp, bottomStart = 14.dp),
                )
            }

            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(cs.surfaceContainerLow).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("PR selesai, 7 hari terakhir", style = MaterialTheme.typography.titleMedium)
                Row(
                    Modifier.fillMaxWidth().height(150.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    last7.forEachIndexed { i, day ->
                        val h by animateDpAsState(
                            (14 + 100 * counts[i] / maxCount).dp,
                            animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
                            label = "bar",
                        )
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${counts[i]}", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                            Box(
                                Modifier.fillMaxWidth().height(h)
                                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
                                    .background(if (day == today) cs.primary else cs.primary.copy(alpha = 0.45f)),
                            )
                            Text(
                                DAY_SHORT[day.dayOfWeek.value - 1],
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(cs.surfaceContainerLow).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Waktu fokus", style = MaterialTheme.typography.titleMedium)
                Text("$focusWeek menit dalam 7 hari terakhir", style = MaterialTheme.typography.bodyLarge)
                Text("$focusAll menit secara keseluruhan", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            }

            if (bySubject.isNotEmpty()) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(cs.surfaceContainerLow).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("Per mata pelajaran", style = MaterialTheme.typography.titleMedium)
                    bySubject.forEach { (name, doneCount, all) ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row {
                                Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                                Text("$doneCount/$all", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
                            }
                            Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(cs.surfaceContainerHighest)) {
                                Box(
                                    Modifier.fillMaxHeight()
                                        .fillMaxWidth(if (all == 0) 0f else doneCount.toFloat() / all)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(cs.primary),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
