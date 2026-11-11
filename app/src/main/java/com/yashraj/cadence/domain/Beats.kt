package com.yashraj.cadence.domain

import com.yashraj.cadence.data.HabitEntity
import com.yashraj.cadence.data.HabitLogEntity
import com.yashraj.cadence.data.TaskEntity
import com.yashraj.cadence.data.TimeOfDay

/** Saturday and Sunday as bits of a rest-day mask (bit 0 = Monday). */
const val WeekendMask = (1 shl 5) or (1 shl 6)

/** A habit with everything the screens need to draw it. */
class HabitView(
    val habit: HabitEntity,
    val rules: HabitRules,
    val logs: Map<Long, Boolean>,
    val today: Long,
) {
    val timeOfDay: TimeOfDay = TimeOfDay.from(habit.timeOfDay)
    val todayState: DayState = rules.stateOf(today, today, logs)
    val loggedToday: Boolean get() = todayState.kept
    val week: List<DayState> = rules.week(today, logs)
    val streak: Int = rules.streak(today, logs)

    /** Rest day today with nothing logged: not part of today's beats. */
    val restingToday: Boolean get() = todayState == DayState.Rest

    /** "After chai, on the balcony · Minimum: 1 page" as used on Next up. */
    val line: String
        get() = listOf(habit.cue, minimumLine).filter { it.isNotBlank() }.joinToString(" · ")

    val minimumLine: String get() = if (habit.minimum.isBlank()) "" else "Minimum: ${habit.minimum}"
}

fun buildHabitViews(habits: List<HabitEntity>, logs: List<HabitLogEntity>, today: Long, weekendRest: Boolean): List<HabitView> {
    val byHabit = logs.groupBy { it.habitId }
    return habits.map { habit ->
        val mask = habit.restDays or (if (weekendRest) WeekendMask else 0)
        val map = byHabit[habit.id].orEmpty().associate { it.day to it.minimum }
        HabitView(habit, HabitRules(habit.createdDay, mask), map, today)
    }
}

/** One thing due today: a habit to log or a task to finish. */
class Beat(
    val key: String,
    val habit: HabitView?,
    val task: TaskEntity?,
    val minuteOfDay: Int,
) {
    val title: String get() = habit?.habit?.title ?: task!!.title
    val done: Boolean get() = habit?.loggedToday ?: task!!.done

    /** The second line on Next up. */
    val line: String
        get() = habit?.line ?: task!!.let { t ->
            listOfNotNull(
                t.dueMinute?.let { Dates.time(it) } ?: "Today",
                t.durationMin?.let { Dates.duration(it) },
            ).joinToString(" · ")
        }
}

class DayPlan(val beats: List<Beat>) {
    val total: Int get() = beats.size
    val doneCount: Int get() = beats.count { it.done }
    val allDone: Boolean get() = total > 0 && doneCount == total
    val left: Int get() = total - doneCount

    /** The first undone beat in time order, skipping any the user pushed to later. */
    fun nextUp(deferred: List<String> = emptyList()): Beat? {
        val open = beats.filter { !it.done }.sortedBy { it.minuteOfDay }
        val pushed = deferred.mapNotNull { key -> open.firstOrNull { it.key == key } }
        return (open.filter { it !in pushed } + pushed).firstOrNull()
    }
}

private const val UntimedTaskMinute = 12 * 60

fun habitKey(id: Long) = "h$id"
fun taskKey(id: Long) = "t$id"

/** Everything due today: habits not resting today (or already logged), and tasks dated today. */
fun planToday(habits: List<HabitView>, tasks: List<TaskEntity>, today: Long): DayPlan {
    val habitBeats = habits
        .filter { !it.restingToday }
        .map { Beat(habitKey(it.habit.id), it, null, it.timeOfDay.minuteOfDay) }
    val taskBeats = tasks
        .filter { it.dueDay == today }
        .map { Beat(taskKey(it.id), null, it, it.dueMinute ?: UntimedTaskMinute) }
    return DayPlan((habitBeats + taskBeats).sortedBy { it.minuteOfDay })
}
