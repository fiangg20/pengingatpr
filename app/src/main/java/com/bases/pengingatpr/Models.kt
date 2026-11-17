package com.bases.pengingatpr

import java.util.UUID

fun newId(): String = UUID.randomUUID().toString().take(12)

data class SubTask(
    val id: String = newId(),
    val text: String,
    val done: Boolean = false,
)

data class Pr(
    val id: String = newId(),
    val title: String,
    val subject: String,
    val note: String = "",
    val due: Long,
    val priority: Int = 1,
    val estimateMin: Int = 0,
    val done: Boolean = false,
    val doneAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val remindBefore: List<Int> = listOf(60, 1440),
    val subtasks: List<SubTask> = emptyList(),
    val focusMin: Int = 0,
)

data class Lesson(
    val day: Int,
    val period: Int,
    val start: String,
    val end: String,
    val subject: String,
    val detail: String = "",
)

data class Timetable(
    val name: String,
    val source: String,
    val lessons: List<Lesson>,
)

data class FocusSession(
    val at: Long,
    val minutes: Int,
    val prId: String,
)

data class Settings(
    val themeMode: Int = 0,
    val accent: Int = 0,
    val dailyOn: Boolean = true,
    val dailyHour: Int = 18,
    val dailyMinute: Int = 0,
    val defaultRemind: List<Int> = listOf(60, 1440),
)

sealed interface Route {
    data class Editor(val id: String?, val subject: String? = null, val due: Long? = null) : Route
    data class Detail(val id: String) : Route
    data class Focus(val prId: String?) : Route
    data object Import : Route
    data object Stats : Route
    data object Settings : Route
}
