package com.yashraj.cadence.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Time of day a habit belongs to. Stored as its ordinal. */
enum class TimeOfDay(val label: String, val minuteOfDay: Int) {
    Morning("Morning", 8 * 60),
    Afternoon("Afternoon", 14 * 60),
    Evening("Evening", 20 * 60);

    companion object {
        fun from(ordinal: Int) = entries.getOrElse(ordinal) { Morning }
    }
}

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** "After chai, on the balcony" */
    val cue: String = "",
    /** The smallest version that counts, without the "Minimum:" prefix ("1 page"). */
    val minimum: String = "",
    val timeOfDay: Int = TimeOfDay.Morning.ordinal,
    /** Days of the week this habit rests on, bit 0 = Monday ... bit 6 = Sunday. */
    val restDays: Int = 0,
    /** Epoch day the habit was created; earlier days are never counted as missed. */
    val createdDay: Long,
    val sortOrder: Int = 0,
)

/** One day a habit was kept. A row exists only for done or minimum days. */
@Entity(
    tableName = "habit_logs",
    primaryKeys = ["habitId", "day"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("habitId")],
)
data class HabitLogEntity(
    val habitId: Long,
    val day: Long,
    /** True when only the minimum version was done. */
    val minimum: Boolean = false,
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** Epoch day, or null when the task has no due date. */
    val dueDay: Long? = null,
    /** Minutes after midnight, or null. */
    val dueMinute: Int? = null,
    val durationMin: Int? = null,
    val priority: Boolean = false,
    val tag: String? = null,
    val done: Boolean = false,
    val doneAt: Long? = null,
    val createdAt: Long,
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** Plain text. Lines starting with "- [ ] " or "- [x] " are checklist lines. */
    val body: String = "",
    val tag: String? = null,
    val pinned: Boolean = false,
    val updatedAt: Long,
)
