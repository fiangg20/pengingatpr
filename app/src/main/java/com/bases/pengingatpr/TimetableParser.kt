package com.bases.pengingatpr

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlin.math.abs

/** Hasil pembacaan satu halaman PDF (satu kelas bila jadwal diatur per kelas). */
data class PageInfo(
    val index: Int,
    val title: String,
    val lessons: List<Lesson>,
    val note: String = "",
)

private class Word(val text: String, val x0: Float, val x1: Float, val yc: Float, val h: Float)

private class Collector : PDFTextStripper() {
    val words = ArrayList<Word>()

    override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
        if (textPositions.isEmpty() || text.isBlank()) return
        val first = textPositions.first()
        val last = textPositions.last()
        val h = first.heightDir
        words.add(Word(text.trim(), first.xDirAdj, last.xDirAdj + last.widthDirAdj, first.yDirAdj - h / 2f, h))
    }
}

object TimetableParser {
    private val DAYS = mapOf(
        "senin" to 1, "sen" to 1, "monday" to 1, "mon" to 1,
        "selasa" to 2, "sel" to 2, "tuesday" to 2, "tue" to 2,
        "rabu" to 3, "rab" to 3, "wednesday" to 3, "wed" to 3,
        "kamis" to 4, "kam" to 4, "thursday" to 4, "thu" to 4,
        "jumat" to 5, "jum" to 5, "friday" to 5, "fri" to 5,
        "sabtu" to 6, "sab" to 6, "saturday" to 6, "sat" to 6,
        "minggu" to 7, "ahad" to 7, "sunday" to 7, "sun" to 7,
    )
    private val TIME = Regex("(\\d{1,2})[:.](\\d{2})")
    private val NUM = Regex("^\\d{1,2}$")

    fun parse(ctx: Context, uri: Uri): List<PageInfo> {
        PDFBoxResourceLoader.init(ctx.applicationContext)
        val stream = ctx.contentResolver.openInputStream(uri) ?: throw IllegalStateException("Berkas tidak dapat dibuka")
        val doc = stream.use { PDDocument.load(it) }
        try {
            val out = ArrayList<PageInfo>()
            for (p in 1..doc.numberOfPages) {
                val c = Collector()
                c.sortByPosition = true
                c.startPage = p
                c.endPage = p
                c.getText(doc)
                out.add(analyze(p, c.words))
            }
            return out
        } finally {
            doc.close()
        }
    }

    private fun norm(s: String): String = s.lowercase().filter { it.isLetter() }

    private fun median(v: List<Float>): Float {
        if (v.isEmpty()) return 0f
        val s = v.sorted()
        return s[s.size / 2]
    }

    private fun analyze(page: Int, words: List<Word>): PageInfo {
        val fallbackTitle = "Halaman $page"
        if (words.isEmpty()) return PageInfo(page, fallbackTitle, emptyList(), "Halaman tanpa teks")

        // 1. Cari label hari
        val dayWords = words.filter { DAYS.containsKey(norm(it.text)) }
        val byDay = dayWords.groupBy { DAYS.getValue(norm(it.text)) }
        if (byDay.size < 2) return PageInfo(page, fallbackTitle, emptyList(), "Label hari tidak ditemukan")

        // 2. Orientasi: hari tersusun ke bawah (baris) atau ke samping (kolom)
        val firstPerDay = byDay.mapValues { e -> e.value.minByOrNull { it.x0 + it.yc }!! }
        val xs = firstPerDay.values.map { (it.x0 + it.x1) / 2f }
        val ys = firstPerDay.values.map { it.yc }
        val rows = (ys.max() - ys.min()) >= (xs.max() - xs.min())

        fun u(w: Word): Float = if (rows) w.yc else (w.x0 + w.x1) / 2f
        fun v(w: Word): Float = if (rows) (w.x0 + w.x1) / 2f else w.yc

        val labels = firstPerDay.entries.map { Triple(it.key, u(it.value), it.value) }.sortedBy { it.second }
        val us = labels.map { it.second }
        val spacing = median(us.zipWithNext { a, b -> b - a })
        if (spacing <= 0f) return PageInfo(page, fallbackTitle, emptyList(), "Susunan hari tidak dikenali")
        val lo0 = us.first() - spacing / 2f
        val hiN = us.last() + spacing / 2f
        val bounds = labels.indices.map { i ->
            val lo = if (i == 0) lo0 else (us[i - 1] + us[i]) / 2f
            val hi = if (i == labels.lastIndex) hiN else (us[i] + us[i + 1]) / 2f
            lo to hi
        }

        // 3. Header jam pelajaran (angka) di sisi sebelum baris/kolom hari pertama
        val dayWordSet = dayWords.toSet()
        val numWords = words.filter { NUM.matches(it.text) && u(it) < lo0 }
        if (numWords.isEmpty()) return PageInfo(page, fallbackTitle, emptyList(), "Header jam pelajaran tidak ditemukan")
        val periods = numWords.groupBy { it.text.toInt() }
            .mapValues { e -> e.value.minByOrNull { u(it) }!! }
            .entries.map { Triple(it.key, v(it.value), it.value) }
            .sortedBy { it.second }
        val pv = periods.map { it.second }
        val pSpacing = if (pv.size >= 2) median(pv.zipWithNext { a, b -> b - a }) else 120f
        val minV = pv.first() - pSpacing / 2f
        val maxV = pv.last() + pSpacing / 2f

        // Waktu tiap jam pelajaran
        val timeWords = words.filter { u(it) < lo0 && TIME.containsMatchIn(it.text) }
        val times = HashMap<Int, List<String>>()
        for (tw in timeWords) {
            val idx = pv.indices.minByOrNull { abs(pv[it] - v(tw)) } ?: continue
            val key = periods[idx].first
            val found = TIME.findAll(tw.text).map { fmtHm(it.groupValues[1].toInt(), it.groupValues[2].toInt()) }.toList()
            times[key] = (times[key] ?: emptyList()) + found
        }

        // 4. Isi sel
        val cells = HashMap<Pair<Int, Int>, MutableList<Word>>()
        for (w in words) {
            if (w in dayWordSet) continue
            val uu = u(w)
            val vv = v(w)
            if (uu < lo0 || uu > hiN || vv < minV || vv > maxV) continue
            val di = bounds.indexOfFirst { uu >= it.first && uu < it.second }
            if (di < 0) continue
            val pi = pv.indices.minByOrNull { abs(pv[it] - vv) } ?: continue
            cells.getOrPut(labels[di].first to periods[pi].first) { ArrayList() }.add(w)
        }

        val lessons = ArrayList<Lesson>()
        for ((key, ws) in cells) {
            val lines = toLines(ws)
            if (lines.isEmpty()) continue
            val subject = lines.first()
            if (subject.length < 2 && !subject.any { it.isLetter() }) continue
            val t = times[key.second] ?: emptyList()
            lessons.add(
                Lesson(
                    day = key.first,
                    period = key.second,
                    start = t.getOrNull(0) ?: "",
                    end = t.getOrNull(1) ?: "",
                    subject = subject,
                    detail = lines.drop(1).joinToString(" · "),
                ),
            )
        }
        lessons.sortWith(compareBy<Lesson>({ it.day }, { it.period }))

        // 5. Judul halaman (nama kelas) = teks paling atas di luar tabel
        val topY = (numWords.map { it.yc } + dayWords.map { it.yc }).min()
        val titleWords = words.filter { it.yc < topY - 2f && !TIME.containsMatchIn(it.text) }
        val titleLines = toLines(titleWords)
        val title = titleLines.take(2).joinToString(" • ").take(60).ifBlank { fallbackTitle }

        val note = if (lessons.isEmpty()) "Tidak ada pelajaran terbaca" else ""
        return PageInfo(page, title, lessons, note)
    }

    /** Gabungkan kata menjadi baris-baris teks (urut dari atas ke bawah, kiri ke kanan). */
    private fun toLines(ws: List<Word>): List<String> {
        if (ws.isEmpty()) return emptyList()
        val sorted = ws.sortedBy { it.yc }
        val lines = ArrayList<MutableList<Word>>()
        for (w in sorted) {
            val cur = lines.lastOrNull()
            if (cur != null && abs(cur.first().yc - w.yc) < maxOf(2f, w.h * 0.6f)) cur.add(w) else lines.add(mutableListOf(w))
        }
        return lines.map { l -> l.sortedBy { it.x0 }.joinToString(" ") { it.text }.trim() }.filter { it.isNotBlank() }
    }
}
