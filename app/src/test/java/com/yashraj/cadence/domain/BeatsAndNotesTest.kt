package com.yashraj.cadence.domain

import com.yashraj.cadence.data.HabitEntity
import com.yashraj.cadence.data.HabitLogEntity
import com.yashraj.cadence.data.TaskEntity
import com.yashraj.cadence.data.TimeOfDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BeatsAndNotesTest {
    private val today = LocalDate.of(2026, 11, 10).toEpochDay() // Tuesday

    private fun habit(id: Long, tod: TimeOfDay, rest: Int = 0) =
        HabitEntity(id = id, title = "Habit $id", timeOfDay = tod.ordinal, restDays = rest, createdDay = today - 40)

    private fun task(id: Long, dueDay: Long?, minute: Int? = null, done: Boolean = false) =
        TaskEntity(id = id, title = "Task $id", dueDay = dueDay, dueMinute = minute, done = done, createdAt = 0)

    private fun views(habits: List<HabitEntity>, logs: List<HabitLogEntity> = emptyList(), weekendRest: Boolean = true) =
        buildHabitViews(habits, logs, today, weekendRest)

    @Test
    fun todayHoldsHabitsAndTasksDueToday() {
        val plan = planToday(
            views(listOf(habit(1, TimeOfDay.Morning), habit(2, TimeOfDay.Evening))),
            listOf(task(1, today, 17 * 60 + 30), task(2, today + 1), task(3, null)),
            today,
        )
        assertEquals(listOf("h1", "t1", "h2"), plan.beats.map { it.key })
        assertEquals(3, plan.total)
    }

    @Test
    fun aHabitThatRestsTodayIsOffTheCount() {
        val tuesdayRest = 1 shl 1
        val plan = planToday(views(listOf(habit(1, TimeOfDay.Morning, rest = tuesdayRest), habit(2, TimeOfDay.Morning))), emptyList(), today)
        assertEquals(listOf("h2"), plan.beats.map { it.key })
    }

    @Test
    fun aRestDayHabitAlreadyLoggedStillShowsAsDone() {
        val tuesdayRest = 1 shl 1
        val logs = listOf(HabitLogEntity(1, today, minimum = false))
        val plan = planToday(views(listOf(habit(1, TimeOfDay.Morning, rest = tuesdayRest)), logs), emptyList(), today)
        assertEquals(1, plan.total)
        assertTrue(plan.beats.single().done)
    }

    @Test
    fun weekendRestSettingMakesSaturdayAndSundayRestDays() {
        val saturday = today + 4
        val withRest = buildHabitViews(listOf(habit(1, TimeOfDay.Morning)), emptyList(), saturday, weekendRest = true)
        val without = buildHabitViews(listOf(habit(1, TimeOfDay.Morning)), emptyList(), saturday, weekendRest = false)
        assertTrue(withRest.single().restingToday)
        assertFalse(without.single().restingToday)
    }

    @Test
    fun nextUpIsTheEarliestOpenBeatAndLaterPushesItToTheEnd() {
        val plan = planToday(
            views(listOf(habit(1, TimeOfDay.Morning), habit(2, TimeOfDay.Afternoon))),
            listOf(task(1, today, 20 * 60)),
            today,
        )
        assertEquals("h1", plan.nextUp()?.key)
        assertEquals("h2", plan.nextUp(deferred = listOf("h1"))?.key)
        assertEquals("t1", plan.nextUp(deferred = listOf("t1", "h2", "h1"))?.key)
        assertEquals("t1", plan.nextUp(deferred = listOf("h2", "h1"))?.key)
    }

    @Test
    fun completedBeatsAreSkippedAndAllDoneHasNoNextUp() {
        val logs = listOf(HabitLogEntity(1, today, false))
        val plan = planToday(views(listOf(habit(1, TimeOfDay.Morning)), logs), listOf(task(1, today, done = true)), today)
        assertTrue(plan.allDone)
        assertNull(plan.nextUp())
        assertEquals(2, plan.doneCount)
        assertEquals(0, plan.left)
    }

    @Test
    fun nextUpLineReadsLikeTheMockups() {
        val h = habit(1, TimeOfDay.Afternoon).copy(cue = "After chai, on the balcony", minimum = "1 page")
        val plan = planToday(views(listOf(h)), listOf(task(1, today, 17 * 60 + 30).copy(durationMin = 25)), today)
        assertEquals("After chai, on the balcony · Minimum: 1 page", plan.beats.first { it.key == "h1" }.line)
        assertEquals("5:30 pm · 25 min", plan.beats.first { it.key == "t1" }.line)
    }

    @Test
    fun timesAndDurationsFollowTheHouseStyle() {
        assertEquals("5:30 pm", Dates.time(17 * 60 + 30))
        assertEquals("12:00 am", Dates.time(0))
        assertEquals("12:05 pm", Dates.time(12 * 60 + 5))
        assertEquals("8:00 am", Dates.time(8 * 60))
        assertEquals("25 min", Dates.duration(25))
        assertEquals("1 hr", Dates.duration(60))
        assertEquals("Tue 10 Nov", Dates.dateLine(today))
        assertEquals("Today", Dates.relativeDay(today, today))
        assertEquals("Thu", Dates.relativeDay(today + 2, today))
        assertEquals("17 Nov", Dates.relativeDay(today + 7, today))
    }
}
