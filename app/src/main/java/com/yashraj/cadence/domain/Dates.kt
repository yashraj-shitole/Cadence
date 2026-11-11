package com.yashraj.cadence.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Days are Long epoch days throughout; minutes of the day are Int. Weekday index: 0 = Monday ... 6 = Sunday. */
object Dates {
    private val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    private val weekdayShort = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val weekdayInitials = listOf("M", "T", "W", "T", "F", "S", "S")

    fun today(): Long = LocalDate.now().toEpochDay()

    fun nowMillis(): Long = System.currentTimeMillis()

    /** 1970-01-01 was a Thursday (index 3). */
    fun weekdayIndex(epochDay: Long): Int = Math.floorMod(epochDay + 3, 7L).toInt()

    fun weekdayShort(index: Int): String = weekdayShort[index]

    /** "Tue 10 Nov" */
    fun dateLine(epochDay: Long): String {
        val d = LocalDate.ofEpochDay(epochDay)
        return "${weekdayShort[weekdayIndex(epochDay)]} ${d.dayOfMonth} ${monthNames[d.monthValue - 1]}"
    }

    /** "Today", then "Tomorrow", then the weekday for the next six days, then "3 Nov". */
    fun relativeDay(epochDay: Long, today: Long): String {
        val delta = epochDay - today
        return when {
            delta == 0L -> "Today"
            delta == -1L -> "Yesterday"
            delta == 1L -> "Tomorrow"
            delta in 2..6 -> weekdayShort[weekdayIndex(epochDay)]
            else -> {
                val d = LocalDate.ofEpochDay(epochDay)
                "${d.dayOfMonth} ${monthNames[d.monthValue - 1]}"
            }
        }
    }

    /** Note timestamps: "Today", "Yesterday", weekday within the last week, then "3 Nov". */
    fun noteStamp(millis: Long, today: Long): String {
        val day = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), ZoneId.systemDefault()).toLocalDate().toEpochDay()
        val delta = day - today
        return when {
            delta == 0L -> "Today"
            delta == -1L -> "Yesterday"
            delta in -6..-2 -> weekdayShort[weekdayIndex(day)]
            else -> relativeDay(day, today)
        }
    }

    /** "5:30 pm" */
    fun time(minuteOfDay: Int): String {
        val h24 = minuteOfDay / 60
        val m = minuteOfDay % 60
        val suffix = if (h24 < 12) "am" else "pm"
        val h12 = when (val h = h24 % 12) { 0 -> 12; else -> h }
        return "$h12:${m.toString().padStart(2, '0')} $suffix"
    }

    /** "25 min", "1 hr", "1 hr 30 min" */
    fun duration(minutes: Int): String = when {
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0 -> "${minutes / 60} hr"
        else -> "${minutes / 60} hr ${minutes % 60} min"
    }

    /** Epoch day of the first day of the week containing [epochDay]. */
    fun weekStartOf(epochDay: Long, sundayFirst: Boolean): Long {
        val sinceMonday = weekdayIndex(epochDay)
        val back = if (sundayFirst) (sinceMonday + 1) % 7 else sinceMonday
        return epochDay - back
    }
}
