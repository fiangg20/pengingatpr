package com.bases.pengingatpr

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val REMIND_CHOICES = listOf(0, 10, 60, 180, 1440, 2880)

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(nav: Nav) {
    val ctx = LocalContext.current
    val s = Repo.settings
    val cs = MaterialTheme.colorScheme
    var confirmWipe by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        nav.toast(if (ok) "Notifikasi diizinkan" else "Notifikasi belum diizinkan")
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { u: Uri? ->
        if (u != null) {
            try {
                ctx.contentResolver.openOutputStream(u)?.use { it.write(Repo.exportJson().toByteArray()) }
                nav.toast("Cadangan tersimpan")
            } catch (e: Exception) {
                nav.toast("Gagal menyimpan cadangan")
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u: Uri? ->
        if (u != null) {
            try {
                val text = ctx.contentResolver.openInputStream(u)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
                nav.toast(if (Repo.importJson(text)) "Cadangan dipulihkan" else "Berkas cadangan tidak valid")
            } catch (e: Exception) {
                nav.toast("Gagal membaca cadangan")
            }
        }
    }

    ScreenFrame(title = "Pengaturan", onBack = { nav.pop() }) { inner ->
        Column(
            Modifier.padding(inner).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 40.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Group("Tampilan") {
                ExpSegmented(listOf("Sistem", "Terang", "Gelap"), s.themeMode, { Repo.updateSettings(s.copy(themeMode = it)) })
                Text("Warna aksen", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ACCENT_HUES.forEachIndexed { i, h ->
                        val sel = s.accent == i
                        Box(
                            Modifier.size(52.dp)
                                .clip(if (sel) cookieShape(9, 0.08f) else CircleShape)
                                .background(hsl(h, 0.55f, 0.45f))
                                .clickable { Repo.updateSettings(s.copy(accent = i)) },
                            contentAlignment = Alignment.Center,
                        ) { if (sel) Icon(Icons.Rounded.Check, ACCENT_NAMES[i], tint = Color.White) }
                    }
                    if (Build.VERSION.SDK_INT >= 31) {
                        val sel = s.accent >= ACCENT_HUES.size
                        Box(
                            Modifier.size(52.dp)
                                .clip(if (sel) cookieShape(9, 0.08f) else CircleShape)
                                .background(cs.secondaryContainer)
                                .border(2.dp, cs.outline, if (sel) cookieShape(9, 0.08f) else CircleShape)
                                .clickable { Repo.updateSettings(s.copy(accent = ACCENT_HUES.size)) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (sel) Icon(Icons.Rounded.Check, "Warna sistem", tint = cs.onSecondaryContainer)
                            else Text("Sis", style = MaterialTheme.typography.labelMedium, color = cs.onSecondaryContainer)
                        }
                    }
                }
                Text(
                    if (s.accent >= ACCENT_HUES.size) "Warna sistem (dinamis)" else ACCENT_NAMES[s.accent.coerceIn(0, ACCENT_NAMES.size - 1)],
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }

            Group("Pengingat") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Ringkasan harian", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Notifikasi daftar PR yang perlu dikerjakan",
                            style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant,
                        )
                    }
                    Switch(checked = s.dailyOn, onCheckedChange = { Repo.updateSettings(s.copy(dailyOn = it)) })
                }
                if (s.dailyOn) {
                    ExpTonalButton(
                        "Pukul " + fmtHm(s.dailyHour, s.dailyMinute),
                        { pickTime(ctx, s.dailyHour, s.dailyMinute) { h, m -> Repo.updateSettings(s.copy(dailyHour = h, dailyMinute = m)) } },
                    )
                }
                Text("Pengingat bawaan untuk PR baru", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    REMIND_CHOICES.forEach { m ->
                        val on = s.defaultRemind.contains(m)
                        ExpChip(
                            reminderLabel(m), on,
                            { Repo.updateSettings(s.copy(defaultRemind = if (on) s.defaultRemind - m else s.defaultRemind + m)) },
                        )
                    }
                }
                ExpTonalButton(
                    "Izin notifikasi",
                    {
                        if (Build.VERSION.SDK_INT >= 33) {
                            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            val i = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                            ctx.startActivity(i)
                        }
                    },
                    icon = Icons.Rounded.Notifications,
                )
            }

            Group("Cadangan data") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExpTonalButton("Ekspor", { exportLauncher.launch("pengingat-pr-cadangan.json") }, Modifier.weight(1f), Icons.Rounded.FileDownload)
                    ExpTonalButton("Impor", { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }, Modifier.weight(1f), Icons.Rounded.FileUpload)
                }
                ExpTonalButton(
                    "Bagikan semua PR aktif",
                    {
                        val list = Repo.prs.filter { !it.done }
                        if (list.isEmpty()) {
                            nav.toast("Tidak ada PR aktif")
                        } else {
                            val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText(list))
                            ctx.startActivity(Intent.createChooser(i, "Bagikan daftar PR"))
                        }
                    },
                    Modifier.fillMaxWidth(), Icons.Rounded.Share,
                )
            }

            Group("Hapus data") {
                ExpTonalButton("Hapus PR yang selesai", { confirmClear = true }, Modifier.fillMaxWidth(), Icons.Rounded.DeleteSweep)
                ExpButton(
                    "Hapus semua data", { confirmWipe = true }, Modifier.fillMaxWidth(),
                    container = cs.errorContainer, content = cs.onErrorContainer, height = 48.dp,
                )
            }

            Text(
                "Pengingat PR 1.0 • Tema Material 3 Expressive. Semua data disimpan di perangkat ini.",
                style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            "Hapus PR selesai?", "Semua PR yang sudah selesai akan dihapus permanen.", "Hapus",
            { Repo.clearDone(); nav.toast("PR selesai dihapus") }, { confirmClear = false },
        )
    }
    if (confirmWipe) {
        ConfirmDialog(
            "Hapus semua data?", "PR, jadwal, riwayat fokus, dan pengaturan akan dihapus. Tindakan ini tidak bisa dibatalkan.", "Hapus semua",
            { Repo.wipe(); nav.toast("Semua data dihapus") }, { confirmWipe = false },
        )
    }
}
