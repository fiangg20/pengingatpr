package com.bases.pengingatpr

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate

/** Kerangka layar sekunder: judul besar + tombol kembali bergaya tonal. */
@Composable
fun ScreenFrame(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    floating: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = floating,
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Kembali")
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                actions()
            }
        },
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: String? = null, onTrailing: () -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (trailing != null) {
            TextButton(onClick = onTrailing) { Text(trailing) }
        }
    }
}

@Composable
fun EmptyState(title: String, subtitle: String, modifier: Modifier = Modifier, icon: ImageVector = Icons.Rounded.Inbox) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(96.dp).clip(cookieShape(8, 0.09f)).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun PrCard(pr: Pr, onClick: () -> Unit, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val dark = isDark()
    val b = bucketOf(pr)
    val cs = MaterialTheme.colorScheme
    val container = when (b) {
        Bucket.Overdue -> cs.errorContainer
        Bucket.Done -> cs.surfaceContainerLow
        else -> cs.surfaceContainerHigh
    }
    val dueColor = if (b == Bucket.Overdue) cs.error else cs.onSurfaceVariant
    val subsDone = pr.subtasks.count { it.done }
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 14.dp, bottomEnd = 32.dp, bottomStart = 14.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CookieBadge(
            text = pr.subject.trim().take(1).uppercase().ifBlank { "?" },
            container = subjectContainer(pr.subject, dark),
            content = subjectOn(pr.subject, dark),
            size = 52.dp,
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                pr.title,
                style = MaterialTheme.typography.titleMedium,
                textDecoration = if (pr.done) TextDecoration.LineThrough else null,
                color = if (pr.done) cs.onSurfaceVariant else cs.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                pr.subject + " • " + relativeDue(pr),
                style = MaterialTheme.typography.bodySmall,
                color = dueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val extras = ArrayList<String>()
            if (!pr.done && pr.priority == 2) extras.add("Prioritas tinggi")
            if (pr.subtasks.isNotEmpty()) extras.add("$subsDone/${pr.subtasks.size} langkah")
            if (pr.estimateMin > 0) extras.add("${pr.estimateMin} mnt")
            if (extras.isNotEmpty()) {
                Text(
                    extras.joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (pr.priority == 2 && !pr.done) cs.tertiary else cs.outline,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        ExpCheck(pr.done, onToggle)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
        shape = RoundedCornerShape(32.dp),
    )
}

fun pickDate(ctx: Context, initial: LocalDate, onPick: (LocalDate) -> Unit) {
    DatePickerDialog(
        ctx,
        { _, y, m, d -> onPick(LocalDate.of(y, m + 1, d)) },
        initial.year, initial.monthValue - 1, initial.dayOfMonth,
    ).show()
}

fun pickTime(ctx: Context, hour: Int, minute: Int, onPick: (Int, Int) -> Unit) {
    TimePickerDialog(ctx, { _, h, m -> onPick(h, m) }, hour, minute, true).show()
}

@Composable
fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    container: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondaryContainer,
    content: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSecondaryContainer,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(28.dp),
) {
    Column(
        modifier.clip(shape).background(container).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, style = MaterialTheme.typography.headlineLarge, color = content)
        Text(label, style = MaterialTheme.typography.labelLarge, color = content, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun fillScreen(): Modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
