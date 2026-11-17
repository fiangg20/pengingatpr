package com.bases.pengingatpr

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun displayName(ctx: Context, uri: Uri): String {
    try {
        ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (i >= 0 && c.moveToFirst()) return c.getString(i)
        }
    } catch (_: Exception) {
    }
    return "jadwal.pdf"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImportScreen(nav: Nav) {
    val ctx = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var pages by remember { mutableStateOf<List<PageInfo>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf(-1) }
    var query by remember { mutableStateOf("") }
    var day by remember { mutableStateOf(1) }
    var renames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var renameTarget by remember { mutableStateOf<String?>(null) }
    val dark = isDark()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) {
            uri = u
            fileName = displayName(ctx, u)
            pages = null
            selected = -1
            error = null
        }
    }

    LaunchedEffect(uri) {
        val u = uri ?: return@LaunchedEffect
        loading = true
        try {
            pages = withContext(Dispatchers.Default) { TimetableParser.parse(ctx, u) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            error = "Gagal membaca PDF: " + (e.message ?: "berkas tidak dikenali")
        } finally {
            loading = false
        }
    }

    LaunchedEffect(selected) { renames = emptyMap() }

    val allPages = pages
    val chosen = allPages?.firstOrNull { it.index == selected }
    val mapped = chosen?.lessons?.map { it.copy(subject = renames[it.subject] ?: it.subject) } ?: emptyList()
    LaunchedEffect(selected) {
        val first = chosen?.lessons?.minByOrNull { it.day }?.day
        if (first != null) day = first
    }

    ScreenFrame(title = "Impor jadwal", onBack = { nav.pop() }) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 48.dp, topEnd = 20.dp, bottomEnd = 48.dp, bottomStart = 20.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Rounded.PictureAsPdf, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text(
                        if (uri == null) "Pilih berkas PDF jadwal" else fileName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Text(
                        "Gunakan PDF hasil ekspor aSc Timetables. Bila jadwal diatur per kelas, satu halaman PDF berisi satu kelas, " +
                            "lalu kamu bisa memilih halaman yang sesuai.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    ExpButton(
                        if (uri == null) "Pilih berkas PDF" else "Ganti berkas",
                        { picker.launch(arrayOf("application/pdf")) },
                        icon = Icons.Rounded.FileUpload,
                    )
                }
            }

            if (loading) {
                item {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LoadingCookie()
                        Text("Membaca halaman PDF...", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            val err = error
            if (err != null) {
                item {
                    Text(
                        err,
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.errorContainer).padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            if (allPages != null) {
                item {
                    Text(
                        "Pilih halaman (kelas) • ${allPages.size} halaman",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                if (allPages.size > 6) {
                    item {
                        OutlinedTextField(
                            value = query, onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Cari nama kelas") },
                            leadingIcon = { Icon(Icons.Rounded.Search, null) },
                            singleLine = true, shape = RoundedCornerShape(28.dp),
                        )
                    }
                }
                val shown = allPages.filter {
                    query.isBlank() || it.title.contains(query.trim(), ignoreCase = true) || it.index.toString() == query.trim()
                }
                items(shown, key = { it.index }) { p ->
                    val sel = p.index == selected
                    val usable = p.lessons.isNotEmpty()
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(if (sel) 32.dp else 18.dp))
                            .background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { selected = p.index }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CookieBadge(
                            p.index.toString(),
                            if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                            size = 48.dp,
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (usable) "${p.lessons.size} pelajaran • ${p.lessons.map { it.subject }.distinct().size} mapel" else p.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (usable) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                            )
                        }
                        if (sel) Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (chosen != null) {
                item { Text("Pratinjau • ${chosen.title}", style = MaterialTheme.typography.titleLarge) }
                if (mapped.isEmpty()) {
                    item {
                        Text(
                            "Halaman ini tidak berisi pelajaran yang bisa dibaca. Pilih halaman lain, atau tambahkan jadwal secara manual di tab Jadwal.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    item { ExpSegmented(DAY_SHORT, day - 1, { day = it + 1 }) }
                    val dayLessons = mapped.filter { it.day == day }.sortedBy { it.period }
                    if (dayLessons.isEmpty()) {
                        item { Text("Tidak ada pelajaran pada hari ini.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    items(count = dayLessons.size) { idx ->
                        val l = dayLessons[idx]
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                                .background(subjectContainer(l.subject, dark)).padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                l.period.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                color = subjectOn(l.subject, dark),
                                modifier = Modifier.width(34.dp),
                            )
                            Column {
                                Text(l.subject, style = MaterialTheme.typography.titleMedium, color = subjectOn(l.subject, dark))
                                val sub = (if (l.start.isNotBlank()) l.start + "–" + l.end else "") +
                                    (if (l.detail.isNotBlank()) (if (l.start.isNotBlank()) " • " else "") + l.detail else "")
                                if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = subjectOn(l.subject, dark))
                            }
                        }
                    }
                    item { Text("Ubah nama mapel (opsional)", style = MaterialTheme.typography.titleMedium) }
                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            chosen.lessons.map { it.subject }.distinct().forEach { orig ->
                                ExpChip(renames[orig] ?: orig, false, { renameTarget = orig }, icon = Icons.Rounded.Edit)
                            }
                        }
                    }
                    item {
                        ExpButton(
                            "Gunakan jadwal ini",
                            {
                                Repo.setTimetable(Timetable(chosen.title, fileName, mapped))
                                nav.pop()
                                nav.toast("Jadwal ${chosen.title} disimpan")
                            },
                            Modifier.fillMaxWidth(),
                            icon = Icons.Rounded.CheckCircle,
                        )
                    }
                }
            }
        }
    }

    val target = renameTarget
    if (target != null) {
        var text by remember(target) { mutableStateOf(renames[target] ?: target) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            shape = RoundedCornerShape(32.dp),
            title = { Text("Ubah nama mapel") },
            text = {
                OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true, shape = RoundedCornerShape(20.dp))
            },
            confirmButton = {
                TextButton(onClick = {
                    if (text.isNotBlank()) renames = renames + (target to text.trim())
                    renameTarget = null
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Batal") } },
        )
    }
}
