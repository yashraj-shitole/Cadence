package com.yashraj.cadence.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yashraj.cadence.data.TaskEntity
import com.yashraj.cadence.ui.AddMode
import com.yashraj.cadence.ui.AppViewModel
import com.yashraj.cadence.ui.components.CChip
import com.yashraj.cadence.ui.components.CIconButton
import com.yashraj.cadence.ui.components.EmptyState
import com.yashraj.cadence.ui.components.Glows
import com.yashraj.cadence.ui.components.SearchField
import com.yashraj.cadence.ui.components.SectionHeading
import com.yashraj.cadence.ui.components.TaskRow
import com.yashraj.cadence.ui.theme.CadenceIcons
import com.yashraj.cadence.ui.theme.Space
import kotlinx.coroutines.delay

private class TaskGroup(val name: String, val rows: List<TaskEntity>, val trailing: String)

/**
 * Tasks: filter chips, then Today, This week, Done. A task you complete stays where it is until you next open
 * the screen, so the list never moves under your thumb.
 */
@Composable
fun TasksScreen(vm: AppViewModel, onEdit: (TaskEntity) -> Unit, onNew: (AddMode) -> Unit) {
    val tasks by vm.tasks.collectAsState()
    val today by vm.today.collectAsState()
    var filter by remember { mutableStateOf<String?>(null) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    // Ids that were already done when this screen opened: only those are shown in Done.
    var settled by remember { mutableStateOf<Set<Long>?>(null) }
    var intro by remember { mutableStateOf(true) }
    LaunchedEffect(tasks != null) {
        val list = tasks
        if (list != null) {
            if (settled == null) settled = list.filter { it.done }.map { it.id }.toSet()
            delay(700)
            intro = false
        }
    }

    ScreenFrame(Glows.Tasks) {
        val list = tasks
        val open = list?.count { !it.done } ?: 0
        val done = list?.count { it.done } ?: 0
        ScreenHeader(
            title = "Tasks",
            eyebrow = if (list == null) null else "$open open · $done done",
            trailing = {
                CIconButton(CadenceIcons.Search, "Search tasks", {
                    searching = !searching
                    if (!searching) query = ""
                }, selected = searching)
            },
        )
        if (list == null) return@ScreenFrame
        if (list.isEmpty()) {
            EmptyState("No tasks yet. Add one and give it a time.", "Add task", { onNew(AddMode.Task) })
            return@ScreenFrame
        }
        if (searching) SearchField(query, { query = it }, "Search tasks", Modifier.padding(bottom = Space.s2))

        val tags = list.mapNotNull { it.tag }.distinct().sorted()
        if (filter != null && filter !in tags) filter = null
        if (tags.isNotEmpty()) {
            Row(
                Modifier
                    .bleedHorizontal(Space.s5)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Space.s5, vertical = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(Space.s2),
            ) {
                CChip("All", filter == null, { filter = null })
                tags.forEach { tag -> CChip(tag, filter == tag, { filter = tag }) }
            }
        }

        val q = query.trim()
        val shown = list.filter { t ->
            (filter == null || t.tag == filter) && (q.isEmpty() || t.title.contains(q, ignoreCase = true))
        }
        val settledIds = settled.orEmpty()
        fun isSettledDone(t: TaskEntity) = t.done && t.id in settledIds

        val todayRows = shown.filter { !isSettledDone(it) && (it.dueDay != null && it.dueDay <= today) }
        val weekRows = shown.filter { !isSettledDone(it) && it.dueDay != null && it.dueDay > today && it.dueDay <= today + 6 }
        val laterRows = shown.filter { !isSettledDone(it) && (it.dueDay == null || it.dueDay > today + 6) }
        val doneRows = shown.filter { isSettledDone(it) }
        val groups = listOf(
            TaskGroup("Today", todayRows, "${todayRows.count { !it.done }} open"),
            TaskGroup("This week", weekRows, "${weekRows.count { !it.done }} open"),
            TaskGroup("Later", laterRows, "${laterRows.count { !it.done }} open"),
            TaskGroup("Done", doneRows, doneRows.size.toString()),
        ).filter { it.rows.isNotEmpty() }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = listPadding(),
            verticalArrangement = Arrangement.spacedBy(Space.s2),
        ) {
            var rowIndex = 0
            groups.forEach { group ->
                item(key = "heading-${group.name}") { SectionHeading(group.name, trailing = group.trailing) }
                items(group.rows, key = { it.id }) { task ->
                    TaskRow(
                        task,
                        today,
                        onToggle = { vm.toggleTask(task) },
                        onOpen = { onEdit(task) },
                        modifier = Modifier.riseIn(shown.indexOf(task), intro),
                    )
                }
            }
        }
    }
}
