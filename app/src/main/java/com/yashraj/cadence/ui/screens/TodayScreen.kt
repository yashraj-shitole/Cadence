package com.yashraj.cadence.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yashraj.cadence.data.TaskEntity
import com.yashraj.cadence.domain.Beat
import com.yashraj.cadence.domain.Dates
import com.yashraj.cadence.ui.AddMode
import com.yashraj.cadence.ui.AppViewModel
import com.yashraj.cadence.ui.UiEvent
import com.yashraj.cadence.ui.components.BeatBar
import com.yashraj.cadence.ui.components.CIconButton
import com.yashraj.cadence.ui.components.EmptyState
import com.yashraj.cadence.ui.components.HabitRow
import com.yashraj.cadence.ui.components.NextUpCard
import com.yashraj.cadence.ui.components.SectionHeading
import com.yashraj.cadence.ui.components.TaskRow
import com.yashraj.cadence.ui.components.WalkTarget
import com.yashraj.cadence.ui.components.walkTarget
import com.yashraj.cadence.ui.theme.CText
import com.yashraj.cadence.ui.theme.Cadence
import com.yashraj.cadence.ui.theme.CadenceIcons
import com.yashraj.cadence.ui.theme.CadenceType
import com.yashraj.cadence.ui.theme.Dur
import com.yashraj.cadence.ui.theme.Space
import kotlinx.coroutines.delay

/**
 * Today: one number over a segmented bar, one Next up card, then what is due today by time of day.
 * Completed rows stay in place; the list never reorders under the user's thumb.
 */
@Composable
fun TodayScreen(
    vm: AppViewModel,
    onOpenSettings: () -> Unit,
    onOpenHabit: (Long) -> Unit,
    onEditTask: (TaskEntity) -> Unit,
    onAdd: (AddMode) -> Unit,
) {
    val colors = Cadence.colors
    val motion = Cadence.motion
    val plan by vm.plan.collectAsState()
    val deferred by vm.deferred.collectAsState()
    val today by vm.today.collectAsState()
    val habitViews by vm.habitViews.collectAsState()
    val tasks by vm.tasks.collectAsState()

    var sweepKey by remember { mutableIntStateOf(0) }
    val lift = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            if (event is UiEvent.DayComplete) {
                // Let the segment fill land first, then sweep the bar. Expressive also lifts the number by 4dp.
                delay(Dur.base.toLong())
                sweepKey++
                if (motion.level == 3) {
                    lift.animateTo(-4f, tween(motion.duration(Dur.celebrate / 2)))
                    lift.animateTo(0f, tween(motion.duration(Dur.celebrate / 2)))
                }
            }
        }
    }
    var intro by remember { mutableStateOf(true) }
    LaunchedEffect(plan != null) {
        if (plan != null) {
            delay(700)
            intro = false
        }
    }

    ScreenFrame {
        ScreenHeader(
            title = "Today",
            eyebrow = Dates.dateLine(today),
            trailing = { CIconButton(CadenceIcons.Settings, "Settings", onOpenSettings, Modifier.walkTarget(WalkTarget.Settings)) },
        )
        val p = plan
        if (p == null) return@ScreenFrame
        val totalHabits = habitViews?.size ?: 0
        val totalTasks = tasks?.size ?: 0

        if (p.total > 0) {
            Column(Modifier.padding(bottom = Space.s5)) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = Space.s3),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Row(Modifier.offset(y = lift.value.dp), verticalAlignment = Alignment.Bottom) {
                        CText("${p.doneCount}/${p.total}", CadenceType.numeral, color = colors.ink, maxLines = 1)
                        CText("beats done", CadenceType.heading, Modifier.padding(start = Space.s2, bottom = 2.dp), color = colors.ink, maxLines = 1)
                    }
                    CText(
                        if (p.allDone) "All done" else "${p.left} to go",
                        CadenceType.caption.copy(fontWeight = FontWeight(600)),
                        Modifier.padding(bottom = 4.dp),
                        color = colors.ink,
                    )
                }
                BeatBar(p.total, p.doneCount, sweepKey = sweepKey)
            }
            val next = p.nextUp(deferred)
            NextUpCard(next, p.total, onDone = { next?.let(vm::completeBeat) }, onLater = { next?.let(vm::later) })
        }

        val habitBeats = p.beats.filter { it.habit != null }
        val taskBeats = p.beats.filter { it.task != null }
        val listState = rememberLazyListState()

        if (p.total == 0) {
            if (totalHabits == 0 && totalTasks == 0) {
                EmptyState("No habits yet. Add one you can do in two minutes.", "Add habit", { onAdd(AddMode.Habit) })
            } else {
                EmptyState("Nothing is due today.", "Add task", { onAdd(AddMode.Task) })
            }
        } else {
            LazyColumn(
                Modifier.weight(1f).padding(top = Space.s1),
                state = listState,
                contentPadding = listPadding(),
                verticalArrangement = Arrangement.spacedBy(Space.s2),
            ) {
                if (habitBeats.isNotEmpty()) {
                    item(key = "habits-heading") {
                        SectionHeading("Habits", trailing = "${habitBeats.count { it.done }} of ${habitBeats.size}")
                    }
                    items(habitBeats, key = { it.key }) { beat ->
                        val view = beat.habit!!
                        HabitRow(
                            view,
                            onToggle = { vm.toggleHabit(view) },
                            onOpen = { onOpenHabit(view.habit.id) },
                            modifier = Modifier.riseIn(habitBeats.indexOf(beat), intro),
                        )
                    }
                }
                if (taskBeats.isNotEmpty()) {
                    item(key = "tasks-heading") {
                        SectionHeading("Tasks", trailing = "${taskBeats.count { it.done }} of ${taskBeats.size}")
                    }
                    items(taskBeats, key = { it.key }) { beat ->
                        val task = beat.task!!
                        TaskRow(
                            task,
                            today,
                            onToggle = { vm.toggleTask(task) },
                            onOpen = { onEditTask(task) },
                            modifier = Modifier.riseIn(habitBeats.size + taskBeats.indexOf(beat), intro),
                        )
                    }
                }
            }
        }
    }
}
