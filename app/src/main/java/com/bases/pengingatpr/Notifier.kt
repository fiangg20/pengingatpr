package com.bases.pengingatpr

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.time.LocalDateTime

object Notifier {
    const val CH_PR = "pr_pengingat"
    const val CH_DAILY = "pr_harian"
    const val ACTION_REMIND = "com.bases.pengingatpr.REMIND"
    const val ACTION_DAILY = "com.bases.pengingatpr.DAILY"
    const val ACTION_DONE = "com.bases.pengingatpr.DONE"
    const val ACTION_SNOOZE = "com.bases.pengingatpr.SNOOZE"
    const val EXTRA_ID = "pr_id"
    private const val DAILY_CODE = 7001
    private const val PREFS = "alarm_codes"

    fun createChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_PR, "Pengingat PR", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Pengingat mendekati tenggat PR"
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_DAILY, "Ringkasan harian", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Ringkasan PR yang perlu dikerjakan"
            },
        )
    }

    private fun alarmIntent(ctx: Context, action: String, id: String?): Intent =
        Intent(ctx, ReminderReceiver::class.java).setAction(action).apply {
            if (id != null) putExtra(EXTRA_ID, id)
        }

    private fun pending(ctx: Context, code: Int, intent: Intent, flags: Int = 0): PendingIntent? =
        PendingIntent.getBroadcast(ctx, code, intent, flags or PendingIntent.FLAG_IMMUTABLE)

    private fun setAlarm(ctx: Context, code: Int, at: Long, action: String, id: String?) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = pending(ctx, code, alarmIntent(ctx, action, id), PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        try {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (_: Exception) {
        }
    }

    fun rescheduleAll(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        // Batalkan alarm lama
        val old = sp.getStringSet("codes", emptySet()) ?: emptySet()
        for (c in old) {
            val code = c.toIntOrNull() ?: continue
            val pi = pending(ctx, code, alarmIntent(ctx, ACTION_REMIND, null), PendingIntent.FLAG_NO_CREATE)
            if (pi != null) am.cancel(pi)
        }

        val now = System.currentTimeMillis()
        val codes = HashSet<String>()
        for (pr in Repo.prs) {
            if (pr.done) continue
            for (m in pr.remindBefore.distinct()) {
                val at = pr.due - m * 60_000L
                if (at <= now) continue
                val code = (pr.id.hashCode() * 31 + m) and 0x3FFFFFFF
                setAlarm(ctx, code, at, ACTION_REMIND, pr.id)
                codes.add(code.toString())
            }
        }

        // Ringkasan harian
        val dailyOld = pending(ctx, DAILY_CODE, alarmIntent(ctx, ACTION_DAILY, null), PendingIntent.FLAG_NO_CREATE)
        if (dailyOld != null) am.cancel(dailyOld)
        val s = Repo.settings
        if (s.dailyOn) {
            var next = LocalDateTime.now().withHour(s.dailyHour).withMinute(s.dailyMinute).withSecond(0).withNano(0)
            if (!next.isAfter(LocalDateTime.now())) next = next.plusDays(1)
            setAlarm(ctx, DAILY_CODE, next.toMs(), ACTION_DAILY, null)
        }

        sp.edit().putStringSet("codes", codes).apply()
    }

    fun snoozeOneHour(ctx: Context, id: String) {
        val code = (id.hashCode() * 17 + 999) and 0x3FFFFFFF
        setAlarm(ctx, code, System.currentTimeMillis() + 3_600_000L, ACTION_REMIND, id)
    }

    private fun openAppIntent(ctx: Context, id: String?): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (id != null) putExtra(EXTRA_ID, id)
        }
        return PendingIntent.getActivity(
            ctx, (id ?: "app").hashCode() and 0x3FFFFFFF, i,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun post(ctx: Context, nid: Int, n: android.app.Notification) {
        if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return
        try {
            NotificationManagerCompat.from(ctx).notify(nid, n)
        } catch (_: SecurityException) {
        }
    }

    fun showReminder(ctx: Context, pr: Pr) {
        createChannels(ctx)
        val nid = pr.id.hashCode() and 0x3FFFFFFF
        val done = PendingIntent.getBroadcast(
            ctx, nid + 1, alarmIntent(ctx, ACTION_DONE, pr.id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val snooze = PendingIntent.getBroadcast(
            ctx, nid + 2, alarmIntent(ctx, ACTION_SNOOZE, pr.id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = "${pr.subject} • ${relativeDue(pr)}"
        val n = NotificationCompat.Builder(ctx, CH_PR)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(pr.title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text + "\nTenggat: " + fmtDue(pr.due)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(ctx, pr.id))
            .addAction(0, "Tandai selesai", done)
            .addAction(0, "Tunda 1 jam", snooze)
            .build()
        post(ctx, nid, n)
    }

    fun showDaily(ctx: Context) {
        createChannels(ctx)
        val active = Repo.prs.filter { !it.done }
        if (active.isEmpty()) return
        val overdue = active.count { bucketOf(it) == Bucket.Overdue }
        val today = active.count { bucketOf(it) == Bucket.Today }
        val tomorrow = active.count { bucketOf(it) == Bucket.Tomorrow }
        if (overdue + today + tomorrow == 0) return
        val parts = ArrayList<String>()
        if (overdue > 0) parts.add("$overdue terlambat")
        if (today > 0) parts.add("$today hari ini")
        if (tomorrow > 0) parts.add("$tomorrow besok")
        val n = NotificationCompat.Builder(ctx, CH_DAILY)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("Ringkasan PR")
            .setContentText(parts.joinToString(" • "))
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(ctx, null))
            .build()
        post(ctx, DAILY_CODE, n)
    }

    fun showFocusDone(ctx: Context, minutes: Int) {
        createChannels(ctx)
        val n = NotificationCompat.Builder(ctx, CH_PR)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("Sesi fokus selesai")
            .setContentText("Kerja bagus! $minutes menit fokus tercatat.")
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(ctx, null))
            .build()
        post(ctx, 7002, n)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Repo.init(context)
        val id = intent.getStringExtra(Notifier.EXTRA_ID)
        when (intent.action) {
            Notifier.ACTION_REMIND -> {
                val pr = id?.let { Repo.get(it) }
                if (pr != null && !pr.done) Notifier.showReminder(context, pr)
            }
            Notifier.ACTION_DAILY -> {
                Notifier.showDaily(context)
                Notifier.rescheduleAll(context)
            }
            Notifier.ACTION_DONE -> {
                if (id != null) {
                    Repo.setDone(id, true)
                    NotificationManagerCompat.from(context).cancel(id.hashCode() and 0x3FFFFFFF)
                }
            }
            Notifier.ACTION_SNOOZE -> {
                if (id != null) {
                    Notifier.snoozeOneHour(context, id)
                    NotificationManagerCompat.from(context).cancel(id.hashCode() and 0x3FFFFFFF)
                }
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Repo.init(context)
        Notifier.rescheduleAll(context)
    }
}
