package com.yashraj.cadence.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.yashraj.cadence.data.TimeOfDay
import com.yashraj.cadence.ui.AddMode
import com.yashraj.cadence.ui.AppViewModel
import com.yashraj.cadence.ui.components.CIconButton
import com.yashraj.cadence.ui.components.DayLegend
import com.yashraj.cadence.ui.components.EmptyState
import com.yashraj.cadence.ui.components.Glows
import com.yashraj.cadence.ui.components.HabitRow
import com.yashraj.cadence.ui.components.SectionHeading
import com.yashraj.cadence.ui.components.glass
import com.yashraj.cadence.ui.theme.CadenceIcons
import com.yashraj.cadence.ui.theme.Radius
import com.yashraj.cadence.ui.theme.Space
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.dp

/** Habits: every habit and its week, grouped Morning, Afternoon, Evening. */
@Composable
fun HabitsScreen(vm: AppViewModel, onOpenHabit: (Long) -> Unit, onNew: (AddMode) -> Unit) {
    val habits by vm.habitViews.collectAsState()
    var intro by remember { mutableStateOf(true) }
    LaunchedEffect(habits != null) {
        if (habits != null) {
            delay(700)
            intro = false
        }
    }
    ScreenFrame(Glows.Habits) {
        val list = habits
        val logged = list?.count { it.loggedToday } ?: 0
        ScreenHeader(
            title = "Habits",
            eyebrow = if (list == null) null else "$logged of ${list.size} logged today",
            trailing = { CIconButton(CadenceIcons.Plus, "New habit", { onNew(AddMode.Habit) }) },
        )
        if (list == null) return@ScreenFrame
        if (list.isEmpty()) {
            EmptyState("No habits yet. Add one you can do in two minutes.", "Add habit", { onNew(AddMode.Habit) })
            return@ScreenFrame
        }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = listPadding(),
            verticalArrangement = Arrangement.spacedBy(Space.s2),
        ) {
            var index = 0
            TimeOfDay.entries.forEach { slot ->
                val rows = list.filter { it.timeOfDay == slot }
                if (rows.isEmpty()) return@forEach
                item(key = "heading-${slot.name}") { SectionHeading(slot.label, trailing = "${rows.count { it.loggedToday }} of ${rows.size}") }
                items(rows, key = { it.habit.id }) { view ->
                    HabitRow(
                        view,
                        onToggle = { vm.toggleHabit(view) },
                        onOpen = { onOpenHabit(view.habit.id) },
                        showMinimum = true,
                        modifier = Modifier.riseIn(list.indexOf(view), intro),
                    )
                }
            }
            item(key = "legend") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = Space.s6)
                        .glass(RoundedCornerShape(Radius.card))
                        .padding(horizontal = Space.s4, vertical = 14.dp),
                ) { DayLegend() }
            }
        }
    }
}
