package com.yashraj.cadence.ui

import android.app.TimePickerDialog
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.yashraj.cadence.data.HabitEntity
import com.yashraj.cadence.data.TaskEntity
import com.yashraj.cadence.data.TimeOfDay
import com.yashraj.cadence.domain.Dates
import com.yashraj.cadence.feel.HapticKind
import com.yashraj.cadence.ui.components.ButtonKind
import com.yashraj.cadence.ui.components.CButton
import com.yashraj.cadence.ui.components.CChip
import com.yashraj.cadence.ui.components.CField
import com.yashraj.cadence.ui.components.CSegmented
import com.yashraj.cadence.ui.components.LocalHaptics
import com.yashraj.cadence.ui.components.rememberGlassAdapt
import com.yashraj.cadence.ui.components.LocalOnGlass
import com.yashraj.cadence.ui.components.glass
import com.yashraj.cadence.ui.screens.bleedHorizontal
import com.yashraj.cadence.ui.theme.CText
import com.yashraj.cadence.ui.theme.Cadence
import com.yashraj.cadence.ui.theme.CadenceIcons
import com.yashraj.cadence.ui.theme.CadenceType
import com.yashraj.cadence.ui.theme.Dur
import com.yashraj.cadence.ui.theme.Ease
import com.yashraj.cadence.ui.theme.Radius
import com.yashraj.cadence.ui.theme.Space
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

enum class AddMode(val label: String, val placeholder: String, val action: String) {
    Task("Task", "What needs doing?", "Add task"),
    Habit("Habit", "What will you repeat?", "Add habit"),
    Note("Note", "Title", "Save note"),
}

/** What the sheet is doing: capturing something new, or editing a task or habit that already exists. */
sealed interface SheetTarget {
    data class New(val mode: AddMode) : SheetTarget
    data class EditTask(val task: TaskEntity) : SheetTarget
    data class EditHabit(val habit: HabitEntity) : SheetTarget
}

class SheetActions(
    val addTask: (TaskEntity) -> Unit,
    val updateTask: (TaskEntity) -> Unit,
    val deleteTask: (TaskEntity) -> Unit,
    val addHabit: (title: String, cue: String, minimum: String, TimeOfDay) -> Unit,
    val updateHabit: (HabitEntity) -> Unit,
    val deleteHabit: (HabitEntity) -> Unit,
    val addNote: (title: String, body: String) -> Unit,
)

private enum class Picker { None, Due, Duration, Tag }

private val DurationOptions = listOf(5, 10, 15, 25, 45, 60)
private val TimeOptions = listOf(9 * 60, 12 * 60, 17 * 60 + 30, 20 * 60)

/**
 * The Quick add sheet: opened from the centre tab button with the keyboard up. A title alone is a complete entry.
 * It rises over a scrim in 420ms, leaves in 75% of that, follows the finger, and can be reversed mid-way.
 */
@Composable
fun QuickAddOverlay(
    target: SheetTarget?,
    today: Long,
    tags: List<String>,
    actions: SheetActions,
    onDismiss: () -> Unit,
) {
    val colors = Cadence.colors
    val motion = Cadence.motion
    val density = LocalDensity.current
    val progress = remember { Animatable(0f) }
    var shown by remember { mutableStateOf<SheetTarget?>(null) }
    var sheetHeight by remember { mutableIntStateOf(1200) }
    val drag = remember { Animatable(0f) }

    LaunchedEffect(target) {
        if (target != null) {
            shown = target
            drag.snapTo(0f)
            progress.animateTo(1f, if (motion.off) motion.fade(Dur.sheet) else tween(motion.duration(Dur.sheet), easing = Ease.easeOut))
        } else {
            progress.animateTo(0f, if (motion.off) motion.fade(Dur.sheet) else tween((motion.duration(Dur.sheet) * 0.75f).toInt(), easing = Ease.easeIn))
            shown = null
        }
    }
    val current = shown ?: return
    val dismissDistance = with(density) { 120.dp.toPx() }
    val dragScope = androidx.compose.runtime.rememberCoroutineScope()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val maxSheet = maxHeight * 0.92f
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value }
                .background(colors.scrim)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClickLabel = "Close quick add") { onDismiss() }
                .semantics { contentDescription = "Close quick add" },
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .offset {
                    val travel = if (motion.off) 0f else (1f - progress.value) * sheetHeight
                    IntOffset(0, (travel + drag.value).roundToInt())
                }
                .graphicsLayer {
                    compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
                    alpha = if (motion.off) progress.value else 1f
                }
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta -> dragScope.launch { drag.snapTo((drag.value + delta).coerceAtLeast(0f)) } },
                    onDragStopped = { velocity ->
                        if (drag.value > dismissDistance || velocity > 1800f) onDismiss() else dragScope.launch { drag.animateTo(0f, tween(Dur.base, easing = Ease.easeOut)) }
                    },
                ),
        ) {
            CompositionLocalProvider(LocalOnGlass provides true) {
            SheetContent(
                target = current,
                today = today,
                tags = tags,
                actions = actions,
                onDismiss = onDismiss,
                maxHeight = maxSheet,
                modifier = Modifier.onSizeChanged { sheetHeight = it.height },
            )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SheetContent(
    target: SheetTarget,
    today: Long,
    tags: List<String>,
    actions: SheetActions,
    onDismiss: () -> Unit,
    maxHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val colors = Cadence.colors
    val haptics = LocalHaptics.current
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val titleFocus = remember { FocusRequester() }

    val editingTask = (target as? SheetTarget.EditTask)?.task
    val editingHabit = (target as? SheetTarget.EditHabit)?.habit
    var mode by remember(target) { mutableStateOf((target as? SheetTarget.New)?.mode ?: if (editingHabit != null) AddMode.Habit else AddMode.Task) }
    var title by remember(target) { mutableStateOf(editingTask?.title ?: editingHabit?.title ?: "") }

    // task
    var dueDay by remember(target) { mutableStateOf(if (editingTask != null) editingTask.dueDay else today) }
    var dueMinute by remember(target) { mutableStateOf(editingTask?.dueMinute) }
    var duration by remember(target) { mutableStateOf(editingTask?.durationMin) }
    var priority by remember(target) { mutableStateOf(editingTask?.priority ?: false) }
    var tag by remember(target) { mutableStateOf(editingTask?.tag) }
    var picker by remember(target) { mutableStateOf(Picker.None) }
    var newTag by remember(target) { mutableStateOf("") }
    var addingTag by remember(target) { mutableStateOf(false) }

    // habit
    var cue by remember(target) { mutableStateOf(editingHabit?.cue ?: "") }
    var minimum by remember(target) { mutableStateOf(editingHabit?.minimum ?: "") }
    var timeOfDay by remember(target) { mutableStateOf(editingHabit?.let { TimeOfDay.from(it.timeOfDay) } ?: TimeOfDay.Afternoon) }
    var restMask by remember(target) { mutableIntStateOf(editingHabit?.restDays ?: 0) }

    // note
    var body by remember(target) { mutableStateOf("") }

    LaunchedEffect(target) {
        titleFocus.requestFocus()
        keyboard?.show()
    }

    // Read the live state each time: a click handler must never act on a value captured by an earlier composition.
    fun canSubmitNow() = when (mode) {
        AddMode.Note -> title.isNotBlank() || body.isNotBlank()
        else -> title.isNotBlank()
    }
    val canSubmit = canSubmitNow()
    fun submit() {
        if (!canSubmitNow()) return
        haptics.fire(HapticKind.Medium)
        when {
            editingTask != null -> actions.updateTask(editingTask.copy(title = title.trim(), dueDay = dueDay, dueMinute = if (dueDay == null) null else dueMinute, durationMin = duration, priority = priority, tag = tag))
            editingHabit != null -> actions.updateHabit(editingHabit.copy(title = title.trim(), cue = cue.trim(), minimum = minimum.trim().removePrefix("Minimum:").trim(), timeOfDay = timeOfDay.ordinal, restDays = restMask))
            mode == AddMode.Task -> actions.addTask(TaskEntity(title = title.trim(), dueDay = dueDay, dueMinute = if (dueDay == null) null else dueMinute, durationMin = duration, priority = priority, tag = tag, createdAt = 0))
            mode == AddMode.Habit -> actions.addHabit(title, cue, minimum, timeOfDay)
            else -> actions.addNote(title.trim(), body.trim())
        }
        onDismiss()
    }

    val actionLabel = when {
        editingTask != null -> "Save task"
        editingHabit != null -> "Save habit"
        else -> mode.action
    }

    Column(
        modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .glass(RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet), raised = true, blurred = true, adapt = rememberGlassAdapt(canFlip = false, themeIsDark = colors.isDark))
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = Space.s5, end = Space.s5, top = Space.s3, bottom = Space.s6)
            .semantics { contentDescription = "Quick add" },
        verticalArrangement = Arrangement.spacedBy(Space.s4),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .width(40.dp)
                .height(5.dp)
                .background(colors.controlEdge, RoundedCornerShape(Radius.pill)),
        )
        if (target is SheetTarget.New) {
            CSegmented(AddMode.entries.map { it.label }, mode.ordinal, { mode = AddMode.entries[it] }, label = "Type")
        } else {
            CText(if (editingTask != null) "Edit task" else "Edit habit", CadenceType.title)
        }
        CField(
            value = title,
            onValueChange = { title = it },
            placeholder = if (editingTask != null || editingHabit != null) mode.placeholder else mode.placeholder,
            label = "Title",
            focusRequester = titleFocus,
            onImeAction = { if (mode != AddMode.Note) submit() },
        )

        when (mode) {
            AddMode.Task -> {
                Row(Modifier.bleedHorizontal(Space.s5).horizontalScroll(rememberScrollState()).padding(horizontal = Space.s5), horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
                    val dueText = when (dueDay) {
                        null -> "No date"
                        else -> Dates.relativeDay(dueDay!!, today) + (dueMinute?.let { " " + Dates.time(it) } ?: "")
                    }
                    CChip(dueText, selected = dueDay != null, onClick = { picker = if (picker == Picker.Due) Picker.None else Picker.Due }, icon = CadenceIcons.Clock)
                    CChip(duration?.let { Dates.duration(it) } ?: "Duration", selected = duration != null, onClick = { picker = if (picker == Picker.Duration) Picker.None else Picker.Duration }, icon = CadenceIcons.Timer)
                    CChip("Priority", selected = priority, onClick = { priority = !priority }, icon = CadenceIcons.Flag)
                    CChip(tag ?: "Tag", selected = tag != null, onClick = { picker = if (picker == Picker.Tag) Picker.None else Picker.Tag }, icon = CadenceIcons.Tag)
                }
                when (picker) {
                    Picker.Due -> Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
                            (0..6).forEach { offset ->
                                val day = today + offset
                                CChip(Dates.relativeDay(day, today), dueDay == day, { dueDay = day })
                            }
                            CChip("No date", dueDay == null, { dueDay = null; dueMinute = null })
                        }
                        if (dueDay != null) FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
                            CChip("Any time", dueMinute == null, { dueMinute = null })
                            TimeOptions.forEach { minute -> CChip(Dates.time(minute), dueMinute == minute, { dueMinute = minute }) }
                            CChip("Other time", dueMinute != null && dueMinute !in TimeOptions, {
                                val start = dueMinute ?: (9 * 60)
                                TimePickerDialog(context, { _, h, m -> dueMinute = h * 60 + m }, start / 60, start % 60, false).show()
                            })
                        }
                    }
                    Picker.Duration -> FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
                        DurationOptions.forEach { minutes -> CChip(Dates.duration(minutes), duration == minutes, { duration = minutes }) }
                        CChip("None", duration == null, { duration = null })
                    }
                    Picker.Tag -> Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
                            tags.forEach { name -> CChip(name, tag == name, { tag = name; addingTag = false }) }
                            CChip("New tag", addingTag, { addingTag = !addingTag })
                            CChip("None", tag == null, { tag = null; addingTag = false })
                        }
                        if (addingTag) CField(
                            value = newTag,
                            onValueChange = { newTag = it.take(20) },
                            placeholder = "Tag name",
                            label = "New tag",
                            minHeight = 48.dp,
                            textStyle = CadenceType.body,
                            onImeAction = {
                                if (newTag.isNotBlank()) {
                                    tag = newTag.trim()
                                    addingTag = false
                                    newTag = ""
                                }
                            },
                        )
                    }
                    Picker.None -> Unit
                }
            }
            AddMode.Habit -> Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
                CField(cue, { cue = it }, "After chai, on the balcony", "Cue", prefix = "Cue", minHeight = 48.dp, textStyle = CadenceType.body)
                CField(minimum, { minimum = it }, "The smallest version that counts", "Minimum version", prefix = "Minimum", minHeight = 48.dp, textStyle = CadenceType.body)
                Row(Modifier.bleedHorizontal(Space.s5).horizontalScroll(rememberScrollState()).padding(horizontal = Space.s5), horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
                    TimeOfDay.entries.forEach { t -> CChip(t.label, timeOfDay == t, { timeOfDay = t }) }
                }
                if (editingHabit != null) {
                    CText("Rest days keep the streak and stay off the day's count", CadenceType.callout, color = colors.inkMuted)
                    Row(Modifier.bleedHorizontal(Space.s5).horizontalScroll(rememberScrollState()).padding(horizontal = Space.s5), horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
                        (0..6).forEach { d ->
                            CChip(Dates.weekdayShort(d), (restMask and (1 shl d)) != 0, { restMask = restMask xor (1 shl d) })
                        }
                    }
                }
            }
            AddMode.Note -> CField(
                value = body,
                onValueChange = { body = it },
                placeholder = "Start writing…",
                label = "Note body",
                minHeight = 120.dp,
                textStyle = CadenceType.body,
                singleLine = false,
                verticalAlignment = Alignment.Top,
            )
        }

        CButton(actionLabel, { submit() }, Modifier.fillMaxWidth(), enabled = canSubmit)
        if (editingTask != null) CButton("Delete task", { actions.deleteTask(editingTask); onDismiss() }, Modifier.align(Alignment.CenterHorizontally), kind = ButtonKind.Danger)
        if (editingHabit != null) CButton("Delete habit", { actions.deleteHabit(editingHabit); onDismiss() }, Modifier.align(Alignment.CenterHorizontally), kind = ButtonKind.Danger)
    }
}
