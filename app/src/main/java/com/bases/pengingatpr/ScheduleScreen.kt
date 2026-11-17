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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun ScheduleScreen(nav: Nav) {
    val tt = Repo.timetable
    val todayDow = LocalDate.now().dayOfWeek.value
    var day by rememberSaveable { mutableStateOf(if (todayDow > 6) 1 else todayDow) }
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Lesson?>(null) }
    val dark = isDark()

    val lessons = tt?.lessons?.filter { it.day == day }?.sortedBy { it.period } ?: emptyList()
    val nowT = LocalTime.now()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Jadwal", style = MaterialTheme.typography.headlineLarge)
                    if (tt != null) {
                        Text(
                            tt.name + " • " + tt.lessons.size + " pelajaran • " + tt.lessons.map { it.subject }.distinct().size + " mapel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { nav.push(Route.Import) },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.PictureAsPdf, "Impor PDF", tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { showAdd = true },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Add, "Tambah pelajaran") }
            }
        }
        item { ExpSegmented(DAY_SHORT, day - 1, { day = it + 1 }) }

        if (lessons.isEmpty()) {
            item {
                EmptyState(
                    if (tt == null) "Jadwal belum ada" else "Tidak ada pelajaran",
                    if (tt == null) "Impor dari PDF aSc Timetables atau tambahkan pelajaran secara manual."
                    else "Hari ${DAY_NAMES[day - 1]} tidak memiliki pelajaran.",
                    icon = Icons.Rounded.Schedule,
                )
            }
            if (tt == null) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ExpButton("Impor PDF", { nav.push(Route.Import) }, Modifier.weight(1f), Icons.Rounded.PictureAsPdf)
                        ExpTonalButton("Manual", { showAdd = true }, Modifier.weight(1f), Icons.Rounded.Add)
                    }
                }
            }
        }

        items(count = lessons.size) { idx ->
            val l = lessons[idx]
            val s = parseTime(l.start)
            val e = parseTime(l.end)
            val current = day == todayDow && s != null && e != null && !nowT.isBefore(s) && nowT.isBefore(e)
            val pending = Repo.prs.count { !it.done && it.subject.trim().equals(l.subject.trim(), ignoreCase = true) }
            val bg = if (current) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 14.dp, bottomEnd = 32.dp, bottomStart = 14.dp))
                    .background(bg)
                    .clickable { nav.push(Route.Editor(null, l.subject)) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CookieBadge(
                    l.period.toString(),
                    subjectContainer(l.subject, dark), subjectOn(l.subject, dark),
                    size = 52.dp,
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(l.subject, style = MaterialTheme.typography.titleMedium)
                    val sub = buildString {
                        if (l.start.isNotBlank()) append(l.start + "–" + l.end) else append("Jam ke-${l.period}")
                        if (l.detail.isNotBlank()) append(" • " + l.detail)
                    }
                    Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (current) Text("Sedang berlangsung", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    if (pending > 0) Text("$pending PR aktif", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                }
                Box(
                    Modifier.size(40.dp).clip(CircleShape).clickable { toDelete = l },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Delete, "Hapus", tint = MaterialTheme.colorScheme.outline) }
            }
        }
    }

    if (showAdd) AddLessonDialog(day, { showAdd = false })

    val del = toDelete
    if (del != null) {
        ConfirmDialog(
            "Hapus pelajaran?",
            "${del.subject} (${DAY_NAMES[del.day - 1]}, jam ke-${del.period}) akan dihapus dari jadwal.",
            "Hapus",
            { Repo.removeLesson(del) },
            { toDelete = null },
        )
    }
}

@Composable
private fun AddLessonDialog(initialDay: Int, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var day by remember { mutableStateOf(initialDay) }
    var period by remember { mutableStateOf("") }
    var startH by remember { mutableStateOf(7) }
    var startM by remember { mutableStateOf(0) }
    var endH by remember { mutableStateOf(7) }
    var endM by remember { mutableStateOf(45) }
    var subject by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(32.dp),
        title = { Text("Tambah pelajaran") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExpSegmented(DAY_SHORT, day - 1, { day = it + 1 })
                OutlinedTextField(
                    value = subject, onValueChange = { subject = it },
                    label = { Text("Mata pelajaran") }, singleLine = true,
                    isError = error && subject.isBlank(), shape = RoundedCornerShape(20.dp),
                )
                OutlinedTextField(
                    value = period, onValueChange = { period = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Jam ke-") }, singleLine = true,
                    isError = error && period.toIntOrNull() == null, shape = RoundedCornerShape(20.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExpTonalButton("Mulai ${fmtHm(startH, startM)}", { pickTime(ctx, startH, startM) { h, m -> startH = h; startM = m } }, Modifier.weight(1f))
                    ExpTonalButton("Selesai ${fmtHm(endH, endM)}", { pickTime(ctx, endH, endM) { h, m -> endH = h; endM = m } }, Modifier.weight(1f))
                }
                OutlinedTextField(
                    value = detail, onValueChange = { detail = it },
                    label = { Text("Guru / ruang (opsional)") }, singleLine = true, shape = RoundedCornerShape(20.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val p = period.toIntOrNull()
                if (subject.isBlank() || p == null) {
                    error = true
                } else {
                    Repo.addLesson(Lesson(day, p, fmtHm(startH, startM), fmtHm(endH, endM), subject.trim(), detail.trim()))
                    onDismiss()
                }
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
