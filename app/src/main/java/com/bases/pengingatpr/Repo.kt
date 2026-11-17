package com.bases.pengingatpr

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object Repo {
    var prs by mutableStateOf(listOf<Pr>())
        private set
    var timetable by mutableStateOf<Timetable?>(null)
        private set
    var settings by mutableStateOf(Settings())
        private set
    var focusLog by mutableStateOf(listOf<FocusSession>())
        private set

    /** Id PR yang diminta dibuka dari notifikasi. */
    var pendingOpen by mutableStateOf<String?>(null)

    private var file: File? = null
    private var appCtx: Context? = null

    fun init(ctx: Context) {
        if (file != null) return
        appCtx = ctx.applicationContext
        file = File(ctx.applicationContext.filesDir, "data.json")
        try {
            val f = file
            if (f != null && f.exists()) parse(f.readText())
        } catch (_: Exception) {
        }
    }

    private fun save() {
        try {
            file?.writeText(toJson().toString())
        } catch (_: Exception) {
        }
        appCtx?.let { Notifier.rescheduleAll(it) }
    }

    // ---------- PR ----------

    fun get(id: String): Pr? = prs.firstOrNull { it.id == id }

    fun upsert(pr: Pr) {
        prs = if (prs.any { it.id == pr.id }) prs.map { if (it.id == pr.id) pr else it } else prs + pr
        save()
    }

    fun delete(id: String) {
        prs = prs.filter { it.id != id }
        save()
    }

    fun restore(pr: Pr) = upsert(pr)

    fun setDone(id: String, done: Boolean) {
        val p = get(id) ?: return
        upsert(p.copy(done = done, doneAt = if (done) System.currentTimeMillis() else 0L))
    }

    fun toggleSub(prId: String, subId: String) {
        val p = get(prId) ?: return
        upsert(p.copy(subtasks = p.subtasks.map { if (it.id == subId) it.copy(done = !it.done) else it }))
    }

    fun snooze(id: String, days: Int) {
        val p = get(id) ?: return
        upsert(p.copy(due = p.due + days * 86_400_000L, done = false, doneAt = 0L))
    }

    fun duplicate(id: String) {
        val p = get(id) ?: return
        upsert(
            p.copy(
                id = newId(),
                title = p.title + " (salinan)",
                done = false,
                doneAt = 0L,
                createdAt = System.currentTimeMillis(),
                subtasks = p.subtasks.map { it.copy(id = newId(), done = false) },
                focusMin = 0,
            ),
        )
    }

    fun clearDone() {
        prs = prs.filter { !it.done }
        save()
    }

    fun subjects(): List<String> {
        val set = LinkedHashSet<String>()
        timetable?.lessons?.forEach { if (it.subject.isNotBlank()) set.add(it.subject.trim()) }
        prs.forEach { if (it.subject.isNotBlank()) set.add(it.subject.trim()) }
        return set.toList().sortedBy { it.lowercase() }
    }

    // ---------- Jadwal ----------

    fun replaceTimetable(tt: Timetable?) {
        timetable = tt
        save()
    }

    fun addLesson(l: Lesson) {
        val t = timetable ?: Timetable("Jadwal manual", "Manual", emptyList())
        timetable = t.copy(lessons = t.lessons + l)
        save()
    }

    fun removeLesson(l: Lesson) {
        val t = timetable ?: return
        timetable = t.copy(lessons = t.lessons.filter { it != l })
        save()
    }

    // ---------- Pengaturan & fokus ----------

    fun updateSettings(s: Settings) {
        settings = s
        save()
    }

    fun addFocus(s: FocusSession) {
        focusLog = focusLog + s
        val p = get(s.prId)
        if (p != null) {
            prs = prs.map { if (it.id == p.id) it.copy(focusMin = it.focusMin + s.minutes) else it }
        }
        save()
    }

    fun wipe() {
        prs = emptyList()
        timetable = null
        focusLog = emptyList()
        settings = Settings()
        save()
    }

    // ---------- JSON ----------

    fun exportJson(): String = toJson().toString(2)

    fun importJson(text: String): Boolean {
        return try {
            parse(text)
            save()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("versi", 1)
        o.put("pr", JSONArray().also { a -> prs.forEach { a.put(prJson(it)) } })
        val tt = timetable
        if (tt != null) {
            o.put(
                "jadwal",
                JSONObject()
                    .put("nama", tt.name)
                    .put("sumber", tt.source)
                    .put(
                        "pelajaran",
                        JSONArray().also { a ->
                            tt.lessons.forEach { l ->
                                a.put(
                                    JSONObject().put("hari", l.day).put("jam", l.period)
                                        .put("mulai", l.start).put("selesai", l.end)
                                        .put("mapel", l.subject).put("detail", l.detail),
                                )
                            }
                        },
                    ),
            )
        }
        o.put(
            "fokus",
            JSONArray().also { a ->
                focusLog.forEach {
                    a.put(JSONObject().put("at", it.at).put("menit", it.minutes).put("pr", it.prId))
                }
            },
        )
        val s = settings
        o.put(
            "pengaturan",
            JSONObject().put("tema", s.themeMode).put("aksen", s.accent)
                .put("harianOn", s.dailyOn).put("harianJam", s.dailyHour).put("harianMenit", s.dailyMinute)
                .put("pengingat", JSONArray(s.defaultRemind)),
        )
        return o
    }

    private fun prJson(p: Pr): JSONObject = JSONObject()
        .put("id", p.id).put("judul", p.title).put("mapel", p.subject).put("catatan", p.note)
        .put("tenggat", p.due).put("prioritas", p.priority).put("estimasi", p.estimateMin)
        .put("selesai", p.done).put("selesaiPada", p.doneAt).put("dibuat", p.createdAt)
        .put("fokusMenit", p.focusMin)
        .put("pengingat", JSONArray(p.remindBefore))
        .put(
            "subtugas",
            JSONArray().also { a ->
                p.subtasks.forEach { a.put(JSONObject().put("id", it.id).put("teks", it.text).put("selesai", it.done)) }
            },
        )

    private fun ints(a: JSONArray?): List<Int> {
        if (a == null) return emptyList()
        return (0 until a.length()).map { a.getInt(it) }
    }

    private fun parse(text: String) {
        val o = JSONObject(text)
        val pa = o.optJSONArray("pr") ?: JSONArray()
        val list = ArrayList<Pr>()
        for (i in 0 until pa.length()) {
            val j = pa.getJSONObject(i)
            val sa = j.optJSONArray("subtugas") ?: JSONArray()
            val subs = (0 until sa.length()).map {
                val s = sa.getJSONObject(it)
                SubTask(s.optString("id", newId()), s.optString("teks"), s.optBoolean("selesai"))
            }
            list.add(
                Pr(
                    id = j.optString("id", newId()),
                    title = j.optString("judul"),
                    subject = j.optString("mapel"),
                    note = j.optString("catatan"),
                    due = j.optLong("tenggat"),
                    priority = j.optInt("prioritas", 1),
                    estimateMin = j.optInt("estimasi"),
                    done = j.optBoolean("selesai"),
                    doneAt = j.optLong("selesaiPada"),
                    createdAt = j.optLong("dibuat"),
                    remindBefore = ints(j.optJSONArray("pengingat")),
                    subtasks = subs,
                    focusMin = j.optInt("fokusMenit"),
                ),
            )
        }
        prs = list

        val jt = o.optJSONObject("jadwal")
        timetable = if (jt == null) null else {
            val la = jt.optJSONArray("pelajaran") ?: JSONArray()
            val lessons = (0 until la.length()).map {
                val l = la.getJSONObject(it)
                Lesson(
                    l.optInt("hari"), l.optInt("jam"), l.optString("mulai"), l.optString("selesai"),
                    l.optString("mapel"), l.optString("detail"),
                )
            }
            Timetable(jt.optString("nama"), jt.optString("sumber"), lessons)
        }

        val fa = o.optJSONArray("fokus") ?: JSONArray()
        focusLog = (0 until fa.length()).map {
            val f = fa.getJSONObject(it)
            FocusSession(f.optLong("at"), f.optInt("menit"), f.optString("pr"))
        }

        val js = o.optJSONObject("pengaturan")
        if (js != null) {
            settings = Settings(
                themeMode = js.optInt("tema", 0),
                accent = js.optInt("aksen", 0),
                dailyOn = js.optBoolean("harianOn", true),
                dailyHour = js.optInt("harianJam", 18),
                dailyMinute = js.optInt("harianMenit", 0),
                defaultRemind = ints(js.optJSONArray("pengingat")).ifEmpty { listOf(60, 1440) },
            )
        }
    }
}
