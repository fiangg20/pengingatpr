package com.bases.pengingatpr

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import java.time.LocalDateTime
import java.time.LocalTime

private val ESTIMATES = listOf(0, 15, 30, 60, 90, 120)
private val REMIND_OPTIONS = listOf(0, 10, 60, 180, 1440, 2880)

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(nav: Nav, r: Route.Editor) {
    val ctx = LocalContext.current
    val existing = remember(r.id) { r.id?.let { Repo.get(it) } }

    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var subject by rememberSaveable { mutableStateOf(existing?.subject ?: r.subject ?: "") }
    var note by rememberSaveable { mutableStateOf(existing?.note ?: "") }
    var dueMs by rememberSaveable {
        mutableStateOf(
            existing?.due
                ?: r.due
                ?: nextMeetings(Repo.timetable, r.subject ?: "", 1).firstOrNull()?.toMs()
                ?: LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(7, 0)).toMs(),
        )
    }
    var priority by rememberSaveable { mutableStateOf(existing?.priority ?: 1) }
    var estimate by rememberSaveable { mutableStateOf(existing?.estimateMin ?: 0) }
    var remind by remember { mutableStateOf(existing?.remindBefore ?: Repo.settings.defaultRemind) }
    var subs by remember { mutableStateOf(existing?.subtasks ?: emptyList()) }
    var newSub by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    val due = dueMs.toLdt()
    fun setDate(d: LocalDate) {
        dueMs = LocalDateTime.of(d, dueMs.toLdt().toLocalTime()).toMs()
    }

    fun addSub() {
        if (newSub.isNotBlank()) {
            subs = subs + SubTask(text = newSub.trim())
            newSub = ""
        }
    }

    fun save() {
        if (title.isBlank()) {
            showError = true
            return
        }
        val pr = (existing ?: Pr(title = "", subject = "", due = dueMs)).copy(
            title = title.trim(),
            subject = subject.trim().ifBlank { "Umum" },
            note = note.trim(),
            due = dueMs,
            priority = priority,
            estimateMin = estimate,
            remindBefore = remind.sorted(),
            subtasks = subs,
        )
        Repo.upsert(pr)
        nav.pop()
        nav.toast(if (existing == null) "PR disimpan" else "Perubahan disimpan")
    }

    ScreenFrame(
        title = if (existing == null) "PR baru" else "Ubah PR",
        onBack = { nav.pop() },
        floating = {
            ExtendedFloatingActionButton(
                onClick = { save() },
                icon = { Icon(Icons.Rounded.Check, null) },
                text = { Text("Simpan") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(24.dp),
            )
        },
    ) { inner ->
        Column(
            Modifier.padding(inner).imePadding().verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Judul PR") },
                placeholder = { Text("Contoh: Latihan soal bab 3") },
                isError = showError && title.isBlank(),
                supportingText = { if (showError && title.isBlank()) Text("Judul wajib diisi") },
                shape = RoundedCornerShape(24.dp),
            )

            Label("Mata pelajaran")
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nama mata pelajaran") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
            )
            val suggestions = Repo.subjects()
            if (suggestions.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestions.forEach { s ->
                        ExpChip(s, subject.trim().equals(s, ignoreCase = true), { subject = s })
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Catatan (opsional)") },
                minLines = 3,
                shape = RoundedCornerShape(24.dp),
            )

            Label("Tenggat")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExpTonalButton(
                    fmtDate(due.toLocalDate()),
                    { pickDate(ctx, due.toLocalDate()) { setDate(it) } },
                    Modifier.weight(1f), Icons.Rounded.CalendarMonth,
                )
                ExpTonalButton(
                    fmtTime(due.toLocalTime()),
                    { pickTime(ctx, due.hour, due.minute) { h, m -> dueMs = LocalDateTime.of(due.toLocalDate(), LocalTime.of(h, m)).toMs() } },
                    Modifier, Icons.Rounded.Schedule,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val today = LocalDate.now()
                ExpChip("Hari ini", due.toLocalDate() == today, { setDate(today) })
                ExpChip("Besok", due.toLocalDate() == today.plusDays(1), { setDate(today.plusDays(1)) })
                ExpChip("Lusa", due.toLocalDate() == today.plusDays(2), { setDate(today.plusDays(2)) })
                ExpChip("Minggu depan", due.toLocalDate() == today.plusDays(7), { setDate(today.plusDays(7)) })
            }
            val meetings = nextMeetings(Repo.timetable, subject, 3)
            if (meetings.isNotEmpty()) {
                Label("Pertemuan berikutnya (dari jadwal)")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    meetings.forEach { m ->
                        ExpChip(
                            fmtDateShort(m.toLocalDate()) + " " + fmtTime(m.toLocalTime()),
                            dueMs == m.toMs(),
                            { dueMs = m.toMs() },
                        )
                    }
                }
            }

            Label("Prioritas")
            ExpSegmented(PRIORITY_LABELS, priority, { priority = it })

            Label("Perkiraan waktu pengerjaan")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ESTIMATES.forEach { e ->
                    ExpChip(if (e == 0) "Tidak diatur" else "$e menit", estimate == e, { estimate = e })
                }
            }

            Label("Pengingat")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                REMIND_OPTIONS.forEach { m ->
                    val on = remind.contains(m)
                    ExpChip(reminderLabel(m), on, { remind = if (on) remind - m else remind + m })
                }
            }

            Label("Langkah pengerjaan")
            subs.forEach { st ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(st.text, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).clickable { subs = subs.filter { it.id != st.id } },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Close, "Hapus langkah") }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newSub,
                    onValueChange = { newSub = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Tambah langkah") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                )
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { addSub() },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Add, "Tambah", tint = MaterialTheme.colorScheme.onPrimaryContainer) }
            }
        }
    }
}
