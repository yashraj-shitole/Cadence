package com.yashraj.cadence.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitsTest {
    // Tuesday 10 Nov 2026.
    private val today = LocalDate.of(2026, 11, 10).toEpochDay()
    private val noRest = 0

    private fun logs(vararg entries: Pair<Long, Boolean>) = mapOf(*entries)
    private fun doneOn(vararg offsets: Int) = offsets.associate { (today + it) to false }

    @Test
    fun weekdayIndexIsMondayBased() {
        assertEquals(1, Dates.weekdayIndex(today)) // Tuesday
        assertEquals(0, Dates.weekdayIndex(today - 1)) // Monday
        assertEquals(6, Dates.weekdayIndex(today + 5)) // Sunday
        assertEquals(3, Dates.weekdayIndex(0)) // 1 Jan 1970 was a Thursday
    }

    @Test
    fun streakCountsConsecutiveDoneDaysEndingToday() {
        val rules = HabitRules(createdDay = today - 30, restMask = noRest)
        assertEquals(4, rules.streak(today, doneOn(0, -1, -2, -3)))
    }

    @Test
    fun todayNotYetLoggedDoesNotBreakTheStreak() {
        val rules = HabitRules(createdDay = today - 30, restMask = noRest)
        assertEquals(3, rules.streak(today, doneOn(-1, -2, -3)))
    }

    @Test
    fun aMissedDayEndsTheStreak() {
        val rules = HabitRules(createdDay = today - 30, restMask = noRest)
        // Done today and yesterday, missed 2 days ago, done before that.
        assertEquals(2, rules.streak(today, doneOn(0, -1, -3, -4)))
    }

    @Test
    fun aMinimumDayKeepsTheStreakAndCounts() {
        val rules = HabitRules(createdDay = today - 30, restMask = noRest)
        val history = logs(today to false, (today - 1) to true, (today - 2) to false)
        assertEquals(3, rules.streak(today, history))
        assertEquals(DayState.Minimum, rules.stateOf(today - 1, today, history))
    }

    @Test
    fun aRestDayKeepsTheStreakWithoutAddingToIt() {
        // Rest on Monday (bit 0): yesterday was Monday.
        val rules = HabitRules(createdDay = today - 30, restMask = 1)
        val history = doneOn(0, -2, -3) // Tue, Sun, Sat; Monday (-1) rests
        assertEquals(DayState.Rest, rules.stateOf(today - 1, today, history))
        assertEquals(3, rules.streak(today, history))
    }

    @Test
    fun aMissedDayIsNeverTheRestColourOrStateWhenItIsARestDay() {
        val rules = HabitRules(createdDay = today - 30, restMask = 1 shl 0)
        assertEquals(DayState.Rest, rules.stateOf(today - 1, today, emptyMap()))
        assertEquals(DayState.Missed, rules.stateOf(today - 2, today, emptyMap()))
    }

    @Test
    fun daysBeforeTheHabitExistedAreNotMissed() {
        val rules = HabitRules(createdDay = today - 2, restMask = noRest)
        assertEquals(DayState.NotStarted, rules.stateOf(today - 3, today, emptyMap()))
        assertEquals(DayState.Missed, rules.stateOf(today - 1, today, emptyMap()))
        assertEquals(0, rules.streak(today, emptyMap()).coerceAtLeast(0))
    }

    @Test
    fun todayAndFutureStates() {
        val rules = HabitRules(createdDay = today - 5, restMask = noRest)
        assertEquals(DayState.Today, rules.stateOf(today, today, emptyMap()))
        assertEquals(DayState.Future, rules.stateOf(today + 1, today, emptyMap()))
        assertEquals(DayState.Done, rules.stateOf(today, today, doneOn(0)))
    }

    @Test
    fun loggingOnARestDayShowsDoneAndCounts() {
        val rules = HabitRules(createdDay = today - 30, restMask = 1 shl Dates.weekdayIndex(today))
        assertEquals(DayState.Rest, rules.stateOf(today, today, emptyMap()))
        assertEquals(DayState.Done, rules.stateOf(today, today, doneOn(0)))
        assertEquals(1, rules.streak(today, doneOn(0)))
    }

    @Test
    fun weekHasSevenDaysEndingToday() {
        val rules = HabitRules(createdDay = today - 30, restMask = noRest)
        val week = rules.week(today, doneOn(0, -1))
        assertEquals(7, week.size)
        assertEquals(DayState.Done, week.last())
        assertEquals(DayState.Missed, week[0])
    }

    @Test
    fun gridIsFiveWeeksWithTheCurrentWeekLast() {
        val rules = HabitRules(createdDay = today - 60, restMask = noRest)
        val monday = rules.grid(today, sundayFirst = false, logs = doneOn(0))
        assertEquals(35, monday.size)
        // Tuesday is the second column of the last row when weeks start on Monday.
        assertEquals(DayState.Done, monday[28 + 1])
        assertEquals(DayState.Future, monday[28 + 2])
        val sunday = rules.grid(today, sundayFirst = true, logs = doneOn(0))
        // Tuesday is the third column when weeks start on Sunday.
        assertEquals(DayState.Done, sunday[28 + 2])
    }

    @Test
    fun activeDaysCountKeptAgainstKeptPlusMissed() {
        val states = listOf(DayState.Done, DayState.Minimum, DayState.Rest, DayState.Missed, DayState.Today, DayState.Future, DayState.NotStarted)
        val active = ActiveDays.of(states)
        assertEquals(2, active.kept)
        assertEquals(3, active.active)
    }

    @Test
    fun textAlternativeNamesEveryDay() {
        val text = describeDays("Last 7 days", listOf(DayState.Done, DayState.Missed, DayState.Rest))
        assertEquals("Last 7 days: done, missed, rest day", text)
        assertTrue(describeCounts("Five weeks", listOf(DayState.Done, DayState.Done, DayState.Minimum)).contains("2 done, 1 minimum"))
    }
}
