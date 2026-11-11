package com.yashraj.cadence.data

import androidx.room.withTransaction
import com.yashraj.cadence.domain.Dates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class CadenceRepository(private val db: CadenceDatabase) {
    private val habitDao = db.habitDao()
    private val taskDao = db.taskDao()
    private val noteDao = db.noteDao()

    val habits: Flow<List<HabitEntity>> = habitDao.observeHabits()
    val habitLogs: Flow<List<HabitLogEntity>> = habitDao.observeLogs()
    val tasks: Flow<List<TaskEntity>> = taskDao.observeTasks()
    val notes: Flow<List<NoteEntity>> = noteDao.observeNotes()

    fun habit(id: Long): Flow<HabitEntity?> = habitDao.observeHabit(id)

    // ---- habits ----

    suspend fun addHabit(title: String, cue: String, minimum: String, timeOfDay: TimeOfDay, today: Long): Long =
        habitDao.insertHabit(
            HabitEntity(
                title = title.trim(),
                cue = cue.trim(),
                minimum = minimum.trim().removePrefix("Minimum:").trim(),
                timeOfDay = timeOfDay.ordinal,
                createdDay = today,
                sortOrder = habitDao.nextSortOrder(),
            ),
        )

    suspend fun updateHabit(habit: HabitEntity) = habitDao.updateHabit(habit)

    suspend fun deleteHabit(habit: HabitEntity) = habitDao.deleteHabit(habit)

    /** Restores a deleted habit together with its history (used by Undo). */
    suspend fun restoreHabit(habit: HabitEntity, logs: List<HabitLogEntity>) = db.withTransaction {
        habitDao.insertHabits(listOf(habit))
        habitDao.insertLogs(logs)
    }

    /** [minimum] null removes the log for that day; otherwise it records a done (false) or minimum (true) day. */
    suspend fun setHabitLog(habitId: Long, day: Long, minimum: Boolean?) {
        if (minimum == null) habitDao.deleteLog(habitId, day) else habitDao.upsertLog(HabitLogEntity(habitId, day, minimum))
    }

    // ---- tasks ----

    suspend fun saveTask(task: TaskEntity): Long = taskDao.upsert(task)

    suspend fun task(id: Long): TaskEntity? = taskDao.getTask(id)

    suspend fun setTaskDone(id: Long, done: Boolean) {
        val task = taskDao.getTask(id) ?: return
        taskDao.upsert(task.copy(done = done, doneAt = if (done) Dates.nowMillis() else null))
    }

    suspend fun deleteTask(task: TaskEntity) = taskDao.delete(task)

    // ---- notes ----

    suspend fun saveNote(note: NoteEntity): Long = noteDao.upsert(note)

    suspend fun note(id: Long): NoteEntity? = noteDao.getNote(id)

    suspend fun deleteNote(note: NoteEntity) = noteDao.delete(note)

    // ---- data ----

    /** Habits, tasks and notes as one readable JSON file. */
    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        fun <T> JSONArray.fill(items: List<T>, build: (T) -> JSONObject) = apply { items.forEach { put(build(it)) } }
        JSONObject()
            .put("app", "Cadence")
            .put("exportedAt", Dates.nowMillis())
            .put(
                "habits",
                JSONArray().fill(habitDao.getHabits()) {
                    JSONObject()
                        .put("id", it.id).put("title", it.title).put("cue", it.cue).put("minimum", it.minimum)
                        .put("timeOfDay", TimeOfDay.from(it.timeOfDay).label).put("restDays", it.restDays)
                        .put("createdDay", it.createdDay)
                },
            )
            .put(
                "habitLogs",
                JSONArray().fill(habitDao.getLogs()) {
                    JSONObject().put("habitId", it.habitId).put("day", it.day).put("minimum", it.minimum)
                },
            )
            .put(
                "tasks",
                JSONArray().fill(taskDao.getTasks()) {
                    JSONObject()
                        .put("id", it.id).put("title", it.title).put("dueDay", it.dueDay ?: JSONObject.NULL)
                        .put("dueMinute", it.dueMinute ?: JSONObject.NULL).put("durationMin", it.durationMin ?: JSONObject.NULL)
                        .put("priority", it.priority).put("tag", it.tag ?: JSONObject.NULL).put("done", it.done)
                },
            )
            .put(
                "notes",
                JSONArray().fill(noteDao.getNotes()) {
                    JSONObject()
                        .put("id", it.id).put("title", it.title).put("body", it.body)
                        .put("tag", it.tag ?: JSONObject.NULL).put("pinned", it.pinned).put("updatedAt", it.updatedAt)
                },
            )
            .toString(2)
    }

    /**
     * Reads a file written by [exportJson] and adds it to what is already here. Ids are remapped so habit logs stay
     * attached to their habit, and anything identical to an existing item is skipped, so importing the same file
     * twice changes nothing. Throws [IllegalArgumentException] when the file is not a Cadence export; nothing is
     * written in that case.
     */
    suspend fun importJson(text: String): ImportResult = withContext(Dispatchers.IO) {
        val root = try {
            JSONObject(text)
        } catch (e: org.json.JSONException) {
            throw IllegalArgumentException("That file is not a Cadence export.")
        }
        require(root.optString("app") == "Cadence") { "That file is not a Cadence export." }

        fun JSONObject.str(key: String): String? = if (isNull(key)) null else optString(key)
        fun JSONObject.long(key: String): Long? = if (isNull(key)) null else optLong(key)
        fun JSONObject.int(key: String): Int? = if (isNull(key)) null else optInt(key)
        fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

        db.withTransaction {
            val now = Dates.nowMillis()
            var habitsAdded = 0
            var tasksAdded = 0
            var notesAdded = 0

            val existingHabits = habitDao.getHabits().toMutableList()
            val habitIds = HashMap<Long, Long>()
            for (h in root.optJSONArray("habits").objects()) {
                val title = h.str("title")?.trim().orEmpty()
                if (title.isEmpty()) continue
                val timeOfDay = TimeOfDay.entries.firstOrNull { it.label == h.str("timeOfDay") }?.ordinal ?: TimeOfDay.Morning.ordinal
                val cue = h.str("cue").orEmpty()
                val minimum = h.str("minimum").orEmpty()
                val twin = existingHabits.firstOrNull { it.title == title && it.cue == cue && it.minimum == minimum && it.timeOfDay == timeOfDay }
                val newId = if (twin != null) {
                    twin.id
                } else {
                    habitsAdded++
                    val entity = HabitEntity(
                        title = title, cue = cue, minimum = minimum, timeOfDay = timeOfDay,
                        restDays = h.int("restDays") ?: 0,
                        createdDay = h.long("createdDay") ?: Dates.today(),
                        sortOrder = habitDao.nextSortOrder(),
                    )
                    val id = habitDao.insertHabit(entity)
                    existingHabits += entity.copy(id = id)
                    id
                }
                h.long("id")?.let { habitIds[it] = newId }
            }
            val logs = root.optJSONArray("habitLogs").objects().mapNotNull { l ->
                val habitId = habitIds[l.long("habitId") ?: return@mapNotNull null] ?: return@mapNotNull null
                HabitLogEntity(habitId, l.long("day") ?: return@mapNotNull null, l.optBoolean("minimum", false))
            }
            habitDao.insertLogs(logs)

            val existingTasks = taskDao.getTasks()
            for (t in root.optJSONArray("tasks").objects()) {
                val title = t.str("title")?.trim().orEmpty()
                if (title.isEmpty()) continue
                val dueDay = t.long("dueDay")
                val dueMinute = t.int("dueMinute")
                if (existingTasks.any { it.title == title && it.dueDay == dueDay && it.dueMinute == dueMinute }) continue
                val done = t.optBoolean("done", false)
                taskDao.upsert(
                    TaskEntity(
                        title = title, dueDay = dueDay, dueMinute = dueMinute, durationMin = t.int("durationMin"),
                        priority = t.optBoolean("priority", false), tag = t.str("tag"), done = done,
                        doneAt = if (done) now else null, createdAt = now,
                    ),
                )
                tasksAdded++
            }

            val existingNotes = noteDao.getNotes()
            for (n in root.optJSONArray("notes").objects()) {
                val title = n.str("title").orEmpty()
                val body = n.str("body").orEmpty()
                if (title.isBlank() && body.isBlank()) continue
                if (existingNotes.any { it.title == title && it.body == body }) continue
                noteDao.upsert(
                    NoteEntity(title = title, body = body, tag = n.str("tag"), pinned = n.optBoolean("pinned", false), updatedAt = n.long("updatedAt") ?: now),
                )
                notesAdded++
            }
            ImportResult(habitsAdded, tasksAdded, notesAdded)
        }
    }

    suspend fun eraseAll() = withContext(Dispatchers.IO) { db.clearAllTables() }
}

/** How many items an import added; duplicates are not counted. */
data class ImportResult(val habits: Int, val tasks: Int, val notes: Int) {
    val total get() = habits + tasks + notes
}
