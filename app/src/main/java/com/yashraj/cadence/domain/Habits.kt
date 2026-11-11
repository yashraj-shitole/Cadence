package com.yashraj.cadence.domain

/** The shape of one habit day. Every state has a shape as well as a colour. */
enum class DayState(val word: String) {
    /** Filled dot. */
    Done("done"),

    /** Tinted ring. */
    Minimum("minimum"),

    /** Clay ring. */
    Rest("rest day"),

    /** Small quiet dot. Never red. */
    Missed("missed"),

    /** Dashed ring: today, not yet logged. */
    Today("today"),

    /** Empty dashed outline. */
    Future("not yet"),

    /** Before the habit existed: nothing to judge. */
    NotStarted("not started");

    val kept: Boolean get() = this == Done || this == Minimum
}

/**
 * What decides a habit's days: when it started and which weekdays rest (bit 0 = Monday).
 * [restMask] is the effective mask, already merged with the weekend-rest setting.
 */
class HabitRules(val createdDay: Long, val restMask: Int) {
    fun isRest(day: Long): Boolean = restMask and (1 shl Dates.weekdayIndex(day)) != 0

    /** [logs] maps an epoch day to true when only the minimum was done, false when the full habit was. */
    fun stateOf(day: Long, today: Long, logs: Map<Long, Boolean>): DayState {
        val minimumOnly = logs[day]
        if (minimumOnly != null) return if (minimumOnly) DayState.Minimum else DayState.Done
        return when {
            day < createdDay -> DayState.NotStarted
            day > today -> DayState.Future
            isRest(day) -> DayState.Rest
            day == today -> DayState.Today
            else -> DayState.Missed
        }
    }

    /**
     * Consecutive kept days ending today. A rest day keeps the streak without adding to it, today
     * not yet logged does not break it, and a minimum-version day counts as kept. Only a missed day ends it.
     */
    fun streak(today: Long, logs: Map<Long, Boolean>): Int {
        var count = 0
        var day = today
        while (day >= createdDay) {
            val state = stateOf(day, today, logs)
            if (state.kept) count++ else if (state == DayState.Missed) break
            day--
        }
        return count
    }

    /** The last seven days, oldest first, ending today. */
    fun week(today: Long, logs: Map<Long, Boolean>): List<DayState> =
        (today - 6..today).map { stateOf(it, today, logs) }

    /** Five whole weeks, row by row, with the current week last. */
    fun grid(today: Long, sundayFirst: Boolean, logs: Map<Long, Boolean>, weeks: Int = 5): List<DayState> {
        val start = Dates.weekStartOf(today, sundayFirst) - 7L * (weeks - 1)
        return (0 until weeks * 7).map { stateOf(start + it, today, logs) }
    }
}

/** Counts behind "19 of 21 active days": kept days against kept plus missed. Rest days and an unlogged today are not active. */
class ActiveDays(val kept: Int, val active: Int) {
    companion object {
        fun of(states: List<DayState>): ActiveDays {
            val kept = states.count { it.kept }
            val missed = states.count { it == DayState.Missed }
            return ActiveDays(kept, kept + missed)
        }
    }
}

/** Text alternative for a run of days, as the design requires for every chart-like element. */
fun describeDays(prefix: String, states: List<DayState>): String =
    prefix + ": " + states.joinToString(", ") { it.word }

fun describeCounts(prefix: String, states: List<DayState>): String {
    val done = states.count { it == DayState.Done }
    val minimum = states.count { it == DayState.Minimum }
    val rest = states.count { it == DayState.Rest }
    val missed = states.count { it == DayState.Missed }
    return "$prefix: $done done, $minimum minimum, $rest rest days, $missed missed"
}
