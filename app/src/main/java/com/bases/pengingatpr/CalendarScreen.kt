package com.bases.pengingatpr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

@Composable
fun CalendarScreen(nav: Nav) {
    val today = LocalDate.now()
    var monthIdx by rememberSaveable { mutableStateOf(today.year * 12 + today.monthValue - 1) }
    var selectedEpoch by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    val ym = YearMonth.of(monthIdx / 12, monthIdx % 12 + 1)
    val selected = LocalDate.ofEpochDay(selectedEpoch)
    val byDate = Repo.prs.groupBy { it.due.toLdt().toLocalDate() }
    val offset = ym.atDay(1).dayOfWeek.value - 1
    val rows = (offset + ym.lengthOfMonth() + 6) / 7
    val cs = MaterialTheme.colorScheme

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    MONTHS[ym.monthValue - 1] + " " + ym.year,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(cs.surfaceContainerHigh).clickable { monthIdx-- },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.ChevronLeft, "Bulan sebelumnya") }
                Box(Modifier.size(8.dp))
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(cs.surfaceContainerHigh).clickable { monthIdx++ },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.ChevronRight, "Bulan berikutnya") }
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(36.dp)).background(cs.surfaceContainerLow).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(Modifier.fillMaxWidth()) {
                    DAY_SHORT.forEach {
                        Text(
                            it, Modifier.weight(1f), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant,
                        )
                    }
                }
                for (r in 0 until rows) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (c in 0 until 7) {
                            val dayNum = r * 7 + c - offset + 1
                            if (dayNum < 1 || dayNum > ym.lengthOfMonth()) {
                                Box(Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                val date = ym.atDay(dayNum)
                                val list = byDate[date] ?: emptyList()
                                val isSel = date == selected
                                val isToday = date == today
                                val hasOverdue = list.any { !it.done && it.due < System.currentTimeMillis() }
                                val hasActive = list.any { !it.done }
                                Box(
                                    Modifier.weight(1f).aspectRatio(1f)
                                        .clip(if (isSel) cookieShape(9, 0.08f) else CircleShape)
                                        .background(if (isSel) cs.primary else cs.surfaceContainerLow)
                                        .then(if (isToday && !isSel) Modifier.border(2.dp, cs.primary, CircleShape) else Modifier)
                                        .clickable { selectedEpoch = date.toEpochDay() },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            dayNum.toString(),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (isSel) cs.onPrimary else cs.onSurface,
                                        )
                                        if (list.isNotEmpty()) {
                                            val dot = when {
                                                isSel -> cs.onPrimary
                                                hasOverdue -> cs.error
                                                hasActive -> cs.tertiary
                                                else -> cs.outline
                                            }
                                            Box(Modifier.padding(top = 2.dp).size(6.dp).clip(CircleShape).background(dot))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(fmtDate(selected), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                ExpTonalButton(
                    "PR",
                    {
                        val due = LocalDateTime.of(selected, LocalTime.of(7, 0)).toMs()
                        nav.push(Route.Editor(null, null, due))
                    },
                    icon = Icons.Rounded.Add,
                )
            }
        }

        val dayPrs = (byDate[selected] ?: emptyList()).sortedBy { it.due }
        val lessons = Repo.timetable?.lessons?.filter { it.day == selected.dayOfWeek.value }?.sortedBy { it.period } ?: emptyList()
        if (lessons.isNotEmpty()) {
            item {
                Text(
                    "Pelajaran: " + lessons.joinToString(", ") { it.subject },
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                )
            }
        }
        if (dayPrs.isEmpty()) {
            item { EmptyState("Tidak ada PR", "Tidak ada PR dengan tenggat pada tanggal ini.") }
        } else {
            items(count = dayPrs.size, key = { dayPrs[it].id }) { i ->
                val pr = dayPrs[i]
                PrCard(pr, { nav.push(Route.Detail(pr.id)) }, { toggleDone(nav, pr) })
            }
        }
    }
}
