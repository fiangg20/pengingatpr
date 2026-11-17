package com.bases.pengingatpr

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

private val SORTS = listOf("Tenggat", "Prioritas", "Mapel", "Terbaru")

@Composable
fun TasksScreen(nav: Nav) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(0) }
    var sort by rememberSaveable { mutableStateOf(0) }
    var subjectFilter by rememberSaveable { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }

    val all = Repo.prs
    val now = System.currentTimeMillis()
    val activeCount = all.count { !it.done }
    val overdueCount = all.count { bucketOf(it, now) == Bucket.Overdue }
    val doneCount = all.count { it.done }

    var list = when (filter) {
        0 -> all.filter { !it.done }
        1 -> all.filter { bucketOf(it, now) == Bucket.Overdue }
        2 -> all.filter { it.done }
        else -> all
    }
    if (subjectFilter.isNotBlank()) list = list.filter { it.subject.trim().equals(subjectFilter, ignoreCase = true) }
    if (query.isNotBlank()) {
        val q = query.trim().lowercase()
        list = list.filter {
            it.title.lowercase().contains(q) || it.subject.lowercase().contains(q) || it.note.lowercase().contains(q)
        }
    }
    list = when (sort) {
        0 -> list.sortedBy { it.due }
        1 -> list.sortedWith(compareByDescending<Pr> { it.priority }.thenBy { it.due })
        2 -> list.sortedWith(compareBy<Pr> { it.subject.lowercase() }.thenBy { it.due })
        else -> list.sortedByDescending { it.createdAt }
    }

    val groups: List<Pair<String, List<Pr>>> = if (sort == 0 && filter != 2) {
        Bucket.entries.mapNotNull { b ->
            val items = list.filter { bucketOf(it, now) == b }
            if (items.isEmpty()) null else b.label to items
        }
    } else if (sort == 2) {
        list.groupBy { it.subject.trim().ifBlank { "Tanpa mapel" } }.entries.map { it.key to it.value }
    } else {
        listOf("" to list)
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Daftar PR", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
                if (doneCount > 0) {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { confirmClear = true },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.DeleteSweep, "Hapus yang selesai") }
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cari PR, mapel, atau catatan") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        Icon(Icons.Rounded.Close, "Hapus", Modifier.clip(CircleShape).clickable { query = "" }.padding(4.dp))
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
            )
        }
        item {
            ExpSegmented(
                listOf("Aktif $activeCount", "Telat $overdueCount", "Selesai $doneCount", "Semua"),
                filter,
                { filter = it },
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SORTS.size) { i -> ExpChip(SORTS[i], sort == i, { sort = i }) }
            }
        }
        val subjects = Repo.subjects()
        if (subjects.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { ExpChip("Semua mapel", subjectFilter.isBlank(), { subjectFilter = "" }) }
                    items(subjects) { s -> ExpChip(s, subjectFilter.equals(s, ignoreCase = true), { subjectFilter = s }) }
                }
            }
        }

        if (list.isEmpty()) {
            item {
                EmptyState(
                    if (query.isNotBlank()) "Tidak ditemukan" else "Daftar kosong",
                    if (query.isNotBlank()) "Coba kata kunci lain." else "Tidak ada PR pada filter ini.",
                )
            }
        }
        groups.forEach { (label, groupItems) ->
            if (label.isNotBlank()) {
                item(key = "h_$label") {
                    SectionTitle(label + " • " + groupItems.size, Modifier.animateItem())
                }
            }
            items(groupItems, key = { it.id }) { pr ->
                PrCard(pr, { nav.push(Route.Detail(pr.id)) }, { toggleDone(nav, pr) }, Modifier.animateItem())
            }
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            "Hapus PR selesai?",
            "$doneCount PR yang sudah selesai akan dihapus permanen.",
            "Hapus",
            { Repo.clearDone(); nav.toast("PR selesai dihapus") },
            { confirmClear = false },
        )
    }
}
