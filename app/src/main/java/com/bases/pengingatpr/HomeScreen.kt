package com.bases.pengingatpr

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalTime

fun toggleDone(nav: Nav, pr: Pr) {
    val becomeDone = !pr.done
    Repo.setDone(pr.id, becomeDone)
    if (becomeDone) nav.toast("PR ditandai selesai", "Urungkan") { Repo.setDone(pr.id, false) }
}

@Composable
fun HomeScreen(nav: Nav) {
    val prs = Repo.prs
    val now = System.currentTimeMillis()
    val active = prs.filter { !it.done }
    val overdue = active.filter { bucketOf(it, now) == Bucket.Overdue }
    val today = active.filter { bucketOf(it, now) == Bucket.Today }
    val tomorrow = active.filter { bucketOf(it, now) == Bucket.Tomorrow }
    val weekAhead = active.count { it.due < now + 7 * 86_400_000L }
    val doneWeek = prs.count { it.done && it.doneAt > now - 7 * 86_400_000L }
    val denom = weekAhead + doneWeek
    val progress = if (denom == 0) 1f else doneWeek.toFloat() / denom

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(greeting(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (active.isEmpty()) "Tidak ada PR. Santai dulu!" else "${active.size} PR menunggumu",
                        style = MaterialTheme.typography.headlineLarge,
                    )
                }
                Box(
                    Modifier.size(52.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { nav.push(Route.Settings) },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Settings, "Pengaturan") }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 52.dp, topEnd = 52.dp, bottomEnd = 52.dp, bottomStart = 18.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Progres 7 hari", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        "$doneWeek dari $denom PR selesai",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    WavyProgress(progress, color = MaterialTheme.colorScheme.primary, track = MaterialTheme.colorScheme.surface)
                }
                Spacer(Modifier.width(18.dp))
                Box(
                    Modifier.size(84.dp).clip(cookieShape(9, 0.09f)).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    overdue.size.toString(), "Terlambat", Modifier.weight(1f),
                    container = MaterialTheme.colorScheme.errorContainer,
                    content = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(topStart = 36.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 36.dp),
                )
                StatTile(
                    today.size.toString(), "Hari ini", Modifier.weight(1f),
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                    content = MaterialTheme.colorScheme.onTertiaryContainer,
                    shape = RoundedCornerShape(12.dp),
                )
                StatTile(
                    tomorrow.size.toString(), "Besok", Modifier.weight(1f),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 36.dp, bottomEnd = 36.dp, bottomStart = 12.dp),
                )
            }
        }

        item { LessonsToday(nav) }

        val urgent = overdue + today
        if (urgent.isNotEmpty()) {
            item { SectionTitle("Perlu perhatian", trailing = "Semua") { nav.selectTab(Tab.Tasks) } }
            items(urgent.take(5), key = { it.id }) { pr ->
                PrCard(pr, { nav.push(Route.Detail(pr.id)) }, { toggleDone(nav, pr) }, Modifier.animateItem())
            }
        }
        if (tomorrow.isNotEmpty()) {
            item { SectionTitle("Besok") }
            items(tomorrow.take(4), key = { it.id }) { pr ->
                PrCard(pr, { nav.push(Route.Detail(pr.id)) }, { toggleDone(nav, pr) }, Modifier.animateItem())
            }
        }
        if (urgent.isEmpty() && tomorrow.isEmpty()) {
            item {
                EmptyState(
                    if (active.isEmpty()) "Belum ada PR" else "Aman untuk sekarang",
                    if (active.isEmpty()) "Ketuk tombol + untuk mencatat PR pertamamu." else "Tidak ada PR yang jatuh tempo dalam waktu dekat.",
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExpTonalButton("Fokus", { nav.push(Route.Focus(null)) }, Modifier.weight(1f), Icons.Rounded.Timer)
                ExpTonalButton("Statistik", { nav.push(Route.Stats) }, Modifier.weight(1f), Icons.Rounded.BarChart)
            }
        }
    }
}

@Composable
private fun LessonsToday(nav: Nav) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val tt = Repo.timetable
        val dark = isDark()
        if (tt == null) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable { nav.push(Route.Import) }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.PictureAsPdf, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Impor jadwal pelajaran", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(
                        "Pilih berkas PDF dari aSc Timetables lalu pilih kelasmu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            return@Column
        }
        val dow = LocalDate.now().dayOfWeek.value
        val lessons = tt.lessons.filter { it.day == dow }.sortedBy { it.period }
        SectionTitle("Pelajaran hari ini • ${DAY_NAMES[dow - 1]}")
        if (lessons.isEmpty()) {
            Text("Tidak ada pelajaran hari ini.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
            return@Column
        }
        val nowT = LocalTime.now()
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(lessons, key = { it.period }) { l ->
                val s = parseTime(l.start)
                val e = parseTime(l.end)
                val current = s != null && e != null && !nowT.isBefore(s) && nowT.isBefore(e)
                val pending = Repo.prs.count { !it.done && it.subject.trim().equals(l.subject.trim(), ignoreCase = true) }
                Column(
                    Modifier.width(168.dp).clip(RoundedCornerShape(28.dp))
                        .background(if (current) MaterialTheme.colorScheme.primary else subjectContainer(l.subject, dark))
                        .clickable { nav.push(Route.Editor(null, l.subject)) }
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val fg = if (current) MaterialTheme.colorScheme.onPrimary else subjectOn(l.subject, dark)
                    Text(
                        if (l.start.isNotBlank()) "${l.start}–${l.end}" else "Jam ke-${l.period}",
                        style = MaterialTheme.typography.labelMedium, color = fg,
                    )
                    Text(l.subject, style = MaterialTheme.typography.titleMedium, color = fg, maxLines = 2)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (pending > 0) "$pending PR aktif" else "+ Tambah PR",
                        style = MaterialTheme.typography.labelSmall, color = fg,
                    )
                }
            }
        }

    }
}
