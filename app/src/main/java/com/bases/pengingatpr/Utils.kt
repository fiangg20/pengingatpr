package com.bases.pengingatpr

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

val DAY_NAMES = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
val DAY_SHORT = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
val MONTHS = listOf(
    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
    "Juli", "Agustus", "September", "Oktober", "November", "Desember",
)
val MONTHS_SHORT = listOf(
    "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des",
)
val PRIORITY_LABELS = listOf("Rendah", "Sedang", "Tinggi")

fun zone(): ZoneId = ZoneId.systemDefault()

fun Long.toLdt(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), zone())

fun LocalDateTime.toMs(): Long = atZone(zone()).toInstant().toEpochMilli()

fun fmtTime(t: LocalTime): String = "%02d.%02d".format(t.hour, t.minute)

fun fmtHm(h: Int, m: Int): String = "%02d.%02d".format(h, m)

fun fmtDate(d: LocalDate): String =
    "${DAY_NAMES[d.dayOfWeek.value - 1]}, ${d.dayOfMonth} ${MONTHS_SHORT[d.monthValue - 1]} ${d.year}"

fun fmtDateShort(d: LocalDate): String =
    "${DAY_SHORT[d.dayOfWeek.value - 1]}, ${d.dayOfMonth} ${MONTHS_SHORT[d.monthValue - 1]}"

fun fmtDue(ms: Long): String {
    val t = ms.toLdt()
    return fmtDate(t.toLocalDate()) + " • " + fmtTime(t.toLocalTime())
}

fun reminderLabel(min: Int): String = when {
    min == 0 -> "Tepat waktu"
    min < 60 -> "$min menit sebelumnya"
    min < 1440 -> "${min / 60} jam sebelumnya"
    else -> "${min / 1440} hari sebelumnya"
}

fun reminderShort(min: Int): String = when {
    min == 0 -> "Tepat waktu"
    min < 60 -> "$min mnt"
    min < 1440 -> "${min / 60} jam"
    else -> "${min / 1440} hari"
}

enum class Bucket(val label: String) {
    Overdue("Terlambat"),
    Today("Hari ini"),
    Tomorrow("Besok"),
    Week("Minggu ini"),
    Later("Nanti"),
    Done("Selesai"),
}

fun bucketOf(pr: Pr, now: Long = System.currentTimeMillis()): Bucket {
    if (pr.done) return Bucket.Done
    if (pr.due < now) return Bucket.Overdue
    val today = now.toLdt().toLocalDate()
    val d = ChronoUnit.DAYS.between(today, pr.due.toLdt().toLocalDate())
    return when {
        d <= 0L -> Bucket.Today
        d == 1L -> Bucket.Tomorrow
        d <= 7L -> Bucket.Week
        else -> Bucket.Later
    }
}

fun relativeDue(pr: Pr, now: Long = System.currentTimeMillis()): String {
    val due = pr.due.toLdt()
    val today = now.toLdt().toLocalDate()
    val days = ChronoUnit.DAYS.between(today, due.toLocalDate())
    val time = fmtTime(due.toLocalTime())
    if (pr.done) return "Selesai"
    if (pr.due < now) {
        val hours = (now - pr.due) / 3_600_000L
        return when {
            hours < 1L -> "Terlambat beberapa menit"
            hours < 24L -> "Terlambat $hours jam"
            else -> "Terlambat ${hours / 24L} hari"
        }
    }
    return when (days) {
        0L -> "Hari ini, $time"
        1L -> "Besok, $time"
        2L -> "Lusa, $time"
        else -> if (days <= 7L) "$days hari lagi, $time" else fmtDateShort(due.toLocalDate()) + ", " + time
    }
}

/** Tanggal pertemuan berikutnya untuk sebuah mata pelajaran, berdasarkan jadwal. */
fun nextMeetings(tt: Timetable?, subject: String, count: Int = 3): List<LocalDateTime> {
    if (tt == null || subject.isBlank()) return emptyList()
    val key = subject.trim().lowercase()
    val lessons = tt.lessons.filter { it.subject.trim().lowercase() == key }
    if (lessons.isEmpty()) return emptyList()
    val now = LocalDateTime.now()
    val out = ArrayList<LocalDateTime>()
    var d = now.toLocalDate()
    var guard = 0
    while (out.size < count && guard < 60) {
        val dow = d.dayOfWeek.value
        lessons.filter { it.day == dow }.sortedBy { it.period }.take(1).forEach { l ->
            val time = parseTime(l.start) ?: LocalTime.of(7, 0)
            val dt = LocalDateTime.of(d, time)
            if (dt.isAfter(now)) out.add(dt)
        }
        d = d.plusDays(1)
        guard++
    }
    return out
}

fun parseTime(s: String): LocalTime? {
    val m = Regex("(\\d{1,2})[:.](\\d{2})").find(s) ?: return null
    val h = m.groupValues[1].toInt()
    val mi = m.groupValues[2].toInt()
    if (h > 23 || mi > 59) return null
    return LocalTime.of(h, mi)
}

fun greeting(): String {
    val h = LocalTime.now().hour
    return when {
        h < 11 -> "Selamat pagi"
        h < 15 -> "Selamat siang"
        h < 18 -> "Selamat sore"
        else -> "Selamat malam"
    }
}

fun shareText(list: List<Pr>): String {
    val sb = StringBuilder("Daftar PR\n")
    list.sortedBy { it.due }.forEach {
        sb.append("• ${it.title} (${it.subject}) — ${fmtDue(it.due)}\n")
    }
    return sb.toString().trim()
}
