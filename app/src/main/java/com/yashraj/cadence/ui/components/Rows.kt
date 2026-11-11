package com.yashraj.cadence.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yashraj.cadence.data.NoteEntity
import com.yashraj.cadence.data.TaskEntity
import com.yashraj.cadence.domain.Beat
import com.yashraj.cadence.domain.Dates
import com.yashraj.cadence.domain.DayState
import com.yashraj.cadence.domain.HabitView
import com.yashraj.cadence.domain.Markdown
import com.yashraj.cadence.domain.describeDays
import com.yashraj.cadence.ui.theme.CIcon
import com.yashraj.cadence.ui.theme.CIconSmall
import com.yashraj.cadence.ui.theme.CText
import com.yashraj.cadence.ui.theme.Cadence
import com.yashraj.cadence.ui.theme.CadenceIcons
import com.yashraj.cadence.ui.theme.CadenceType
import com.yashraj.cadence.ui.theme.Dur
import com.yashraj.cadence.ui.theme.Ease
import com.yashraj.cadence.ui.theme.Radius
import com.yashraj.cadence.ui.theme.Space

/** Lets a 48dp check sit in a row as if it were 28dp wide: the visible mark lines up with the row padding. */
fun Modifier.tuckHorizontal(by: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val tuck = by.roundToPx()
    layout(placeable.width - tuck * 2, placeable.height) { placeable.place(-tuck, 0) }
}

private val RowShape = RoundedCornerShape(Radius.card)

/** Title with the done treatment: muted and struck through with a 1px line. */
@Composable
private fun RowTitle(text: String, done: Boolean, modifier: Modifier = Modifier) {
    val colors = Cadence.colors
    val style = if (done) CadenceType.bodyStrong.copy(textDecoration = TextDecoration.LineThrough) else CadenceType.bodyStrong
    CText(text, style, modifier, color = if (done) colors.inkMuted else colors.ink, maxLines = 2)
}

/**
 * A habit row: check, title, cue, and the last seven days with the streak. [showMinimum] adds the
 * "Minimum: 1 page" line used on the Habits tab. Tapping the body opens the habit.
 */
@Composable
fun HabitRow(
    view: HabitView,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    showMinimum: Boolean = false,
) {
    val colors = Cadence.colors
    val habit = view.habit
    val state = when (view.todayState) {
        DayState.Done -> CheckState.Done
        DayState.Minimum -> CheckState.Minimum
        else -> CheckState.Off
    }
    Row(
        modifier
            .fillMaxWidth()
            .tappable(onOpen, onClickLabel = "Open ${habit.title}", focusShape = RowShape)
            .glass(RowShape)
            .heightIn(min = 72.dp)
            .padding(horizontal = Space.s4, vertical = Space.s3),
        horizontalArrangement = Arrangement.spacedBy(Space.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CCheck(state, "Log ${habit.title}", onToggle, Modifier.tuckHorizontal(10.dp))
        Column(Modifier.weight(1f)) {
            RowTitle(habit.title, view.loggedToday)
            if (habit.cue.isNotBlank()) CText(habit.cue, CadenceType.callout, color = colors.inkMuted, maxLines = 2)
            if (showMinimum && habit.minimum.isNotBlank()) {
                CText(view.minimumLine, CadenceType.caption, color = colors.inkMuted, modifier = Modifier.padding(top = 4.dp), maxLines = 1)
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            WeekDots(view.week, describeDays("Last 7 days", view.week))
            val streakText = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.ink)) { append(view.streak.toString()) }
                append(" day streak")
            }
            androidx.compose.foundation.text.BasicText(
                streakText,
                style = CadenceType.caption.copy(fontWeight = FontWeight(600), color = colors.inkMuted),
                maxLines = 1,
            )
        }
    }
}

/** A task row: square check, title, due time, duration and tag, with the priority flag at the right. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskRow(
    task: TaskEntity,
    today: Long,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Cadence.colors
    val due = when {
        task.dueMinute != null && (task.dueDay == null || task.dueDay == today) -> Dates.time(task.dueMinute)
        task.dueDay != null -> Dates.relativeDay(task.dueDay, today) + (task.dueMinute?.let { " " + Dates.time(it) } ?: "")
        else -> null
    }
    Row(
        modifier
            .fillMaxWidth()
            .tappable(onOpen, onClickLabel = "Edit ${task.title}", focusShape = RowShape)
            .glass(RowShape)
            .heightIn(min = 72.dp)
            .padding(horizontal = Space.s4, vertical = Space.s3),
        horizontalArrangement = Arrangement.spacedBy(Space.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CCheck(
            if (task.done) CheckState.Done else CheckState.Off,
            "Complete ${task.title}",
            onToggle,
            Modifier.tuckHorizontal(10.dp),
            shape = CheckShape.Square,
        )
        Column(Modifier.weight(1f)) {
            RowTitle(task.title, task.done)
            FlowRow(
                Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(Space.s2),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (due != null) MetaItem(CadenceIcons.Clock, due)
                if (task.durationMin != null) MetaItem(CadenceIcons.Timer, Dates.duration(task.durationMin))
                if (!task.tag.isNullOrBlank()) CTag(task.tag)
            }
        }
        if (task.priority) {
            CIconSmall(CadenceIcons.Flag, Modifier.semantics { contentDescription = "Priority" }, tint = colors.accent)
        }
    }
}

@Composable
private fun MetaItem(icon: com.yashraj.cadence.ui.theme.IconDef, text: String) {
    val colors = Cadence.colors
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        CIconSmall(icon, tint = colors.inkMuted)
        CText(text, CadenceType.caption, color = colors.inkMuted, maxLines = 1)
    }
}

/** A note card: title with a pin when pinned, a two-line preview, and the tag and date. */
@Composable
fun NoteCard(note: NoteEntity, today: Long, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Cadence.colors
    val preview = Markdown.preview(note.body)
    Column(
        modifier
            .fillMaxWidth()
            .tappable(onOpen, onClickLabel = "Open ${note.title.ifBlank { "note" }}", focusShape = RowShape)
            .glass(RowShape)
            .padding(Space.s4),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s2), verticalAlignment = Alignment.CenterVertically) {
            CText(note.title.ifBlank { "Untitled" }, CadenceType.bodyStrong, Modifier.weight(1f), maxLines = 1)
            if (note.pinned) CIconSmall(CadenceIcons.Pin, Modifier.semantics { contentDescription = "Pinned" }, tint = colors.accent)
        }
        if (preview.isNotEmpty()) CText(preview, CadenceType.callout, color = colors.inkMuted, maxLines = 2)
        Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(Space.s2), verticalAlignment = Alignment.CenterVertically) {
            if (!note.tag.isNullOrBlank()) CTag(note.tag)
            Spacer()
            CText(Dates.noteStamp(note.updatedAt, today), CadenceType.caption, color = colors.inkMuted, maxLines = 1)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Spacer() {
    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
}

/**
 * The single "Next up" card on Today: one beat, never a list. When the day is finished it reads
 * "That's today" and "8 of 8 beats done. Rest well." and nothing bigger.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NextUpCard(
    beat: Beat?,
    total: Int,
    onDone: () -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Cadence.colors
    val motion = Cadence.motion
    val shape = RoundedCornerShape(Radius.sheet)
    val density = androidx.compose.ui.platform.LocalDensity.current
    AnimatedContent(
        targetState = beat?.key ?: "done",
        modifier = modifier.fillMaxWidth().glass(shape, raised = true),
        transitionSpec = {
            val rise = with(density) { motion.travel(12f).dp.roundToPx() }
            val lift = with(density) { motion.travel(8f).dp.roundToPx() }
            (fadeIn(motion.fade(Dur.slow, Ease.easeOut)) + slideInVertically(motion.move(Dur.slow, Ease.easeOut)) { rise })
                .togetherWith(fadeOut(motion.fade(Dur.quick, Ease.easeIn)) + slideOutVertically(motion.move(Dur.quick, Ease.easeIn)) { -lift })
                .using(SizeTransform(clip = false))
        },
        label = "nextUp",
    ) {
        Column(Modifier.fillMaxWidth().padding(Space.s5)) {
            CText("NEXT UP", CadenceType.label, color = colors.inkMuted)
            if (beat != null) {
                CText(beat.title, CadenceType.title, Modifier.padding(top = Space.s1), maxLines = 2)
                if (beat.line.isNotBlank()) CText(beat.line, CadenceType.callout, color = colors.inkMuted, maxLines = 2)
                Row(Modifier.fillMaxWidth().padding(top = Space.s4), horizontalArrangement = Arrangement.spacedBy(Space.s2), verticalAlignment = Alignment.CenterVertically) {
                    CButton("Mark done", onDone, Modifier.weight(1f))
                    CButton("Later", onLater, kind = ButtonKind.Glass)
                }
            } else {
                CText("That's today", CadenceType.title, Modifier.padding(top = Space.s1))
                CText("$total of $total beats done. Rest well.", CadenceType.callout, color = colors.inkMuted)
            }
        }
    }
}

/** One sentence and one action, as every empty state is. */
@Composable
fun EmptyState(message: String, actionLabel: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Cadence.colors
    Column(
        modifier.fillMaxWidth().padding(top = Space.s8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.s4),
    ) {
        CText(message, CadenceType.body, color = colors.ink, textAlign = TextAlign.Center)
        CButton(actionLabel, onAction, kind = ButtonKind.Glass)
    }
}
