package com.bases.pengingatpr

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, Modifier.width(110.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun RoundAction(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(nav: Nav, id: String) {
    val ctx = LocalContext.current
    val pr = Repo.get(id)
    var confirmDelete by remember { mutableStateOf(false) }

    if (pr == null) {
        LaunchedEffect(Unit) { nav.pop() }
        return
    }

    val dark = isDark()
    val cs = MaterialTheme.colorScheme
    val b = bucketOf(pr)
    val subsDone = pr.subtasks.count { it.done }

    ScreenFrame(
        title = "Detail PR",
        onBack = { nav.pop() },
        actions = {
            RoundAction(Icons.Rounded.Edit, "Ubah") { nav.push(Route.Editor(pr.id)) }
            Spacer(Modifier.width(8.dp))
            RoundAction(Icons.Rounded.Delete, "Hapus") { confirmDelete = true }
        },
    ) { inner ->
        Column(
            Modifier.padding(inner).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 48.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 48.dp, topEnd = 20.dp, bottomEnd = 48.dp, bottomStart = 20.dp))
                    .background(if (b == Bucket.Overdue) cs.errorContainer else subjectContainer(pr.subject, dark))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val fg = if (b == Bucket.Overdue) cs.onErrorContainer else subjectOn(pr.subject, dark)
                Text(pr.subject, style = MaterialTheme.typography.labelLarge, color = fg)
                Text(
                    pr.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = fg,
                    textDecoration = if (pr.done) TextDecoration.LineThrough else null,
                )
                Text(relativeDue(pr), style = MaterialTheme.typography.titleMedium, color = fg)
            }

            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(cs.surfaceContainerLow).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                InfoRow("Tenggat", fmtDue(pr.due))
                InfoRow("Prioritas", PRIORITY_LABELS[pr.priority.coerceIn(0, 2)])
                if (pr.estimateMin > 0) InfoRow("Perkiraan", "${pr.estimateMin} menit")
                if (pr.focusMin > 0) InfoRow("Waktu fokus", "${pr.focusMin} menit")
                if (pr.remindBefore.isNotEmpty()) InfoRow("Pengingat", pr.remindBefore.sorted().joinToString(", ") { reminderShort(it) })
                if (pr.note.isNotBlank()) InfoRow("Catatan", pr.note)
            }

            if (pr.subtasks.isNotEmpty()) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(cs.surfaceContainerLow).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Langkah pengerjaan • $subsDone/${pr.subtasks.size}", style = MaterialTheme.typography.titleMedium)
                    WavyProgress(subsDone.toFloat() / pr.subtasks.size)
                    pr.subtasks.forEach { st ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { Repo.toggleSub(pr.id, st.id) }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ExpCheck(st.done, { Repo.toggleSub(pr.id, st.id) })
                            Spacer(Modifier.width(12.dp))
                            Text(
                                st.text,
                                style = MaterialTheme.typography.bodyLarge,
                                textDecoration = if (st.done) TextDecoration.LineThrough else null,
                                color = if (st.done) cs.onSurfaceVariant else cs.onSurface,
                            )
                        }
                    }
                }
            }

            if (pr.done) {
                ExpButton(
                    "Batalkan selesai", { Repo.setDone(pr.id, false) }, Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.Undo,
                    container = cs.secondaryContainer, content = cs.onSecondaryContainer,
                )
            } else {
                ExpButton(
                    "Tandai selesai", { toggleDone(nav, pr) }, Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.CheckCircle,
                )
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExpTonalButton("Fokus", { nav.push(Route.Focus(pr.id)) }, icon = Icons.Rounded.Timer)
                ExpTonalButton("Tunda 1 hari", { Repo.snooze(pr.id, 1); nav.toast("Tenggat diundur 1 hari") }, icon = Icons.Rounded.Snooze)
                ExpTonalButton("Duplikat", { Repo.duplicate(pr.id); nav.toast("PR diduplikasi") }, icon = Icons.Rounded.ContentCopy)
                ExpTonalButton(
                    "Bagikan",
                    {
                        val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText(listOf(pr)))
                        ctx.startActivity(Intent.createChooser(i, "Bagikan PR"))
                    },
                    icon = Icons.Rounded.Share,
                )
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            "Hapus PR ini?",
            "\"${pr.title}\" akan dihapus.",
            "Hapus",
            {
                Repo.delete(pr.id)
                nav.pop()
                nav.toast("PR dihapus", "Urungkan") { Repo.restore(pr) }
            },
            { confirmDelete = false },
        )
    }
}
