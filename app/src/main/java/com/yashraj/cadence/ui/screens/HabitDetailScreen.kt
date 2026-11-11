package com.yashraj.cadence.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yashraj.cadence.data.HabitEntity
import com.yashraj.cadence.domain.ActiveDays
import com.yashraj.cadence.domain.Dates
import com.yashraj.cadence.domain.HabitView
import com.yashraj.cadence.domain.WeekendMask
import com.yashraj.cadence.domain.describeCounts
import com.yashraj.cadence.data.WeekStart
import com.yashraj.cadence.ui.AppViewModel
import com.yashraj.cadence.ui.components.ActionMenu
import com.yashraj.cadence.ui.components.ButtonKind
import com.yashraj.cadence.ui.components.CButton
import com.yashraj.cadence.ui.components.CIconButton
import com.yashraj.cadence.ui.components.ConsistencyGrid
import com.yashraj.cadence.ui.components.DayLegend
import com.yashraj.cadence.ui.components.GroupDivider
import com.yashraj.cadence.ui.components.Glows
import com.yashraj.cadence.ui.components.MenuItem
import com.yashraj.cadence.ui.components.SceneryHost
import com.yashraj.cadence.ui.components.SectionHeading
import com.yashraj.cadence.ui.components.SettingRow
import com.yashraj.cadence.ui.components.SettingsGroup
import com.yashraj.cadence.ui.components.glass
import com.yashraj.cadence.ui.theme.CText
import com.yashraj.cadence.ui.theme.Cadence
import com.yashraj.cadence.ui.theme.CadenceIcons
import com.yashraj.cadence.ui.theme.CadenceType
import com.yashraj.cadence.ui.theme.Radius
import com.yashraj.cadence.ui.theme.Space

/**
 * One habit without judgement: title and cue, the streak, a five-week grid, and the rhythm it runs on.
 * The two buttons log today in full or at the minimum; pressing a logged one clears it.
 */
@Composable
fun HabitDetailScreen(
    vm: AppViewModel,
    habitId: Long,
    onBack: () -> Unit,
    onEdit: (HabitEntity) -> Unit,
) {
    val colors = Cadence.colors
    val views by vm.habitViews.collectAsState()
    val settings by vm.settings.collectAsState()
    val today by vm.today.collectAsState()
    val view: HabitView? = views?.firstOrNull { it.habit.id == habitId }
    var menuOpen by remember { mutableStateOf(false) }

    // The habit was deleted (from the menu, or by Undo being skipped): leave quietly.
    LaunchedEffect(views != null && view == null) { if (views != null && view == null) onBack() }

    SceneryHost(Glows.Detail) {
        if (view == null) return@SceneryHost
        val sundayFirst = settings?.weekStart == WeekStart.Sunday
        val grid = view.rules.grid(today, sundayFirst, view.logs)
        val active = ActiveDays.of(grid)
        val habit = view.habit
        val loggedDay = view.logs[today]

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = Space.s4, start = Space.s5, end = Space.s5),
        ) {
            Row(Modifier.fillMaxWidth().padding(bottom = Space.s3), horizontalArrangement = Arrangement.SpaceBetween) {
                CIconButton(CadenceIcons.Back, "Back to habits", onBack)
                Box {
                    CIconButton(CadenceIcons.More, "More options", { menuOpen = true })
                    ActionMenu(
                        expanded = menuOpen,
                        onDismiss = { menuOpen = false },
                        items = listOf(
                            MenuItem("Edit habit", CadenceIcons.Edit) { onEdit(habit) },
                            MenuItem("Delete habit", CadenceIcons.Trash, danger = true) { vm.deleteHabit(view) },
                        ),
                    )
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 150.dp),
            ) {
                CText(habit.title, CadenceType.display, color = colors.ink, maxLines = 3)
                if (habit.cue.isNotBlank()) {
                    CText(habit.cue, EyebrowStyle, Modifier.padding(top = 4.dp, bottom = Space.s5), color = colors.ink)
                } else {
                    Box(Modifier.padding(bottom = Space.s5))
                }

                Column(
                    Modifier
                        .fillMaxWidth()
                        .glass(RoundedCornerShape(Radius.card))
                        .padding(start = Space.s4, end = Space.s4, top = Space.s5, bottom = Space.s4),
                ) {
                    Row(Modifier.fillMaxWidth().padding(bottom = Space.s4), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Space.s2), verticalAlignment = Alignment.Bottom) {
                            CText(view.streak.toString(), CadenceType.numeral, color = colors.ink, maxLines = 1)
                            CText("day streak", CadenceType.bodyStrong, Modifier.padding(bottom = 2.dp), color = colors.ink, maxLines = 1)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            CText("${active.kept} of ${active.active}", CadenceType.bodyStrong, color = colors.ink, maxLines = 1)
                            CText("active days", CadenceType.callout, color = colors.inkMuted, maxLines = 1)
                        }
                    }
                    ConsistencyGrid(grid, sundayFirst, describeCounts("Last five weeks", grid))
                    DayLegend(Modifier.padding(top = Space.s3))
                }

                SectionHeading("Rhythm")
                SettingsGroup {
                    SettingRow(
                        title = "Minimum version",
                        sub = "Counts on a low-energy day",
                        value = habit.minimum.ifBlank { "Not set" },
                        chevron = true,
                        onClick = { onEdit(habit) },
                    )
                    GroupDivider()
                    SettingRow(title = "Time of day", value = view.timeOfDay.label, chevron = true, onClick = { onEdit(habit) })
                    GroupDivider()
                    val mask = habit.restDays or (if (settings?.weekendRest == true) WeekendMask else 0)
                    val restNames = (0..6).filter { mask and (1 shl it) != 0 }.joinToString(", ") { Dates.weekdayShort(it) }
                    SettingRow(
                        title = "Rest days",
                        sub = "Keep the streak, off the day's count",
                        value = restNames.ifEmpty { "None" },
                        chevron = true,
                        onClick = { onEdit(habit) },
                    )
                }
            }
        }

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = Space.s4, end = Space.s4, bottom = TabBarBottomGap)
                .fillMaxWidth()
                .glass(RoundedCornerShape(Radius.sheet), raised = true, blurred = false)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(Space.s2),
        ) {
            CButton(
                if (loggedDay == false) "Logged today" else "Log today",
                { vm.logHabit(view, minimum = false) },
                Modifier.weight(1f),
            )
            if (habit.minimum.isNotBlank() || loggedDay == true) {
                CButton(
                    if (loggedDay == true) "Minimum logged" else "Did the minimum",
                    { vm.logHabit(view, minimum = true) },
                    kind = ButtonKind.Glass,
                )
            }
        }
    }
}
