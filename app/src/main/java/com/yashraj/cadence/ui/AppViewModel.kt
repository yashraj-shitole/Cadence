package com.yashraj.cadence.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.yashraj.cadence.AppContainer
import com.yashraj.cadence.CadenceApp
import com.yashraj.cadence.data.BackdropStyle
import com.yashraj.cadence.data.HabitEntity
import com.yashraj.cadence.data.HabitLogEntity
import com.yashraj.cadence.data.NoteEntity
import com.yashraj.cadence.data.Settings
import com.yashraj.cadence.data.TaskEntity
import com.yashraj.cadence.data.ThemeMode
import com.yashraj.cadence.data.TimeOfDay
import com.yashraj.cadence.data.WeekStart
import com.yashraj.cadence.domain.Beat
import com.yashraj.cadence.domain.DayPlan
import com.yashraj.cadence.domain.Dates
import com.yashraj.cadence.domain.HabitView
import com.yashraj.cadence.domain.buildHabitViews
import com.yashraj.cadence.domain.habitKey
import com.yashraj.cadence.domain.planToday
import com.yashraj.cadence.domain.taskKey
import com.yashraj.cadence.feel.HapticSettings
import com.yashraj.cadence.reminder.ReminderScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The Undo toast. Completing and deleting are instant and reversible for five seconds. */
class ToastState(val token: Long, val message: String, val undo: (() -> Unit)?)

sealed interface UiEvent {
    /** The last beat of the day was just completed by the user. */
    data object DayComplete : UiEvent
}

const val ToastMillis = 5_000

/**
 * App-wide state and actions. Every list is a flow of what is saved; screens only draw it. Null means
 * "still loading", so a screen can wait instead of flashing an empty state.
 */
class AppViewModel(private val container: AppContainer) : ViewModel() {
    private val repo = container.repository
    private val settingsStore = container.settingsStore

    val settings: StateFlow<Settings?> = settingsStore.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val today: StateFlow<Long> = container.dayClock.today

    val habitViews: StateFlow<List<HabitView>?> = combine(repo.habits, repo.habitLogs, today, settingsStore.settings) { habits, logs, day, s ->
        buildHabitViews(habits, logs, day, s.weekendRest)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val tasks: StateFlow<List<TaskEntity>?> = repo.tasks.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val notes: StateFlow<List<NoteEntity>?> = repo.notes.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val plan: StateFlow<DayPlan?> = combine(habitViews, tasks, today) { habits, tasks, day ->
        if (habits == null || tasks == null) null else planToday(habits, tasks, day)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _deferred = MutableStateFlow<List<String>>(emptyList())

    /** Beats the user pushed to later this session, in the order they were pushed. */
    val deferred: StateFlow<List<String>> = _deferred.asStateFlow()

    private val _toast = MutableStateFlow<ToastState?>(null)
    val toast: StateFlow<ToastState?> = _toast.asStateFlow()
    private var toastJob: Job? = null
    private var toastCounter = 0L

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    init {
        // Keep the haptic engine in step with Settings > Feel without the UI having to ask.
        viewModelScope.launch {
            settingsStore.settings.collect { container.haptics.settings = HapticSettings(it.haptics, it.completionOnly) }
        }
        // The reminder is re-armed whenever its time changes.
        viewModelScope.launch {
            var last: Int? = -2
            settingsStore.settings.collect {
                if (it.reminderMinute != last) {
                    last = it.reminderMinute
                    ReminderScheduler.schedule(container.appContext, it.reminderMinute)
                }
            }
        }
    }

    fun onResume() = container.dayClock.refresh()

    // ---- toast ----

    fun showToast(message: String, undo: (() -> Unit)? = null) {
        toastCounter++
        _toast.value = ToastState(toastCounter, message, undo)
        toastJob?.cancel()
        toastJob = viewModelScope.launch {
            delay(ToastMillis.toLong())
            _toast.value = null
        }
    }

    fun dismissToast() {
        toastJob?.cancel()
        _toast.value = null
    }

    // ---- beats ----

    /** Completing a habit: instant, with an Undo toast. Un-checking it is silent. */
    fun toggleHabit(view: HabitView) {
        val id = view.habit.id
        val day = today.value
        if (view.loggedToday) {
            viewModelScope.launch { repo.setHabitLog(id, day, null) }
            dismissToast()
        } else {
            maybeCelebrate(habitKey(id))
            viewModelScope.launch { repo.setHabitLog(id, day, false) }
            showToast("${view.habit.title} done") { viewModelScope.launch { repo.setHabitLog(id, day, null) } }
        }
    }

    /** From Habit detail: [minimum] true logs the minimum version, false the full habit, and the same again clears it. */
    fun logHabit(view: HabitView, minimum: Boolean) {
        val id = view.habit.id
        val day = today.value
        val current = view.logs[day]
        if (current == minimum) {
            viewModelScope.launch { repo.setHabitLog(id, day, null) }
            dismissToast()
        } else {
            if (current == null) maybeCelebrate(habitKey(id))
            viewModelScope.launch { repo.setHabitLog(id, day, minimum) }
            showToast("${view.habit.title} done") { viewModelScope.launch { repo.setHabitLog(id, day, current) } }
        }
    }

    fun toggleTask(task: TaskEntity) {
        if (task.done) {
            viewModelScope.launch { repo.setTaskDone(task.id, false) }
            dismissToast()
        } else {
            maybeCelebrate(taskKey(task.id))
            viewModelScope.launch { repo.setTaskDone(task.id, true) }
            showToast("${task.title} done") { viewModelScope.launch { repo.setTaskDone(task.id, false) } }
        }
    }

    fun completeBeat(beat: Beat) {
        beat.habit?.let { toggleHabit(it) } ?: beat.task?.let { toggleTask(it) }
    }

    /** Pushes a beat to the end of Next up for this session. */
    fun later(beat: Beat) {
        _deferred.value = _deferred.value.filter { it != beat.key } + beat.key
    }

    private fun maybeCelebrate(key: String) {
        val p = plan.value ?: return
        val beat = p.beats.firstOrNull { it.key == key } ?: return
        if (beat.done || p.left != 1) return
        val day = today.value
        viewModelScope.launch {
            if (settingsStore.settings.first().lastCelebratedDay != day) {
                settingsStore.setLastCelebratedDay(day)
                _events.emit(UiEvent.DayComplete)
            }
        }
    }

    // ---- create and edit ----

    fun addTask(task: TaskEntity) {
        viewModelScope.launch {
            val id = repo.saveTask(task.copy(createdAt = Dates.nowMillis()))
            showToast("Added ${task.title}") { viewModelScope.launch { repo.task(id)?.let { repo.deleteTask(it) } } }
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch { repo.saveTask(task) }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repo.deleteTask(task)
            showToast("Task deleted") { viewModelScope.launch { repo.saveTask(task) } }
        }
    }

    fun addHabit(title: String, cue: String, minimum: String, timeOfDay: TimeOfDay) {
        viewModelScope.launch {
            val id = repo.addHabit(title, cue, minimum, timeOfDay, today.value)
            showToast("Added $title") {
                viewModelScope.launch { habitViews.value?.firstOrNull { it.habit.id == id }?.let { repo.deleteHabit(it.habit) } }
            }
        }
    }

    fun updateHabit(habit: HabitEntity) {
        viewModelScope.launch { repo.updateHabit(habit) }
    }

    fun deleteHabit(view: HabitView) {
        val logs = view.logs.map { (day, minimum) -> HabitLogEntity(view.habit.id, day, minimum) }
        viewModelScope.launch {
            repo.deleteHabit(view.habit)
            showToast("Habit deleted") { viewModelScope.launch { repo.restoreHabit(view.habit, logs) } }
        }
    }

    suspend fun saveNote(note: NoteEntity): Long = repo.saveNote(note)

    /** Saves without waiting, for the moment a screen is leaving. */
    fun saveNoteAsync(note: NoteEntity) {
        container.appScope.launch { repo.saveNote(note) }
    }

    suspend fun note(id: Long): NoteEntity? = repo.note(id)

    fun addNote(title: String, body: String) {
        viewModelScope.launch {
            val id = repo.saveNote(NoteEntity(title = title, body = body, updatedAt = Dates.nowMillis()))
            showToast("Saved ${title.ifBlank { "note" }}") { viewModelScope.launch { repo.note(id)?.let { repo.deleteNote(it) } } }
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repo.deleteNote(note)
            showToast("Note deleted") { viewModelScope.launch { repo.saveNote(note) } }
        }
    }

    fun setNotePinned(note: NoteEntity, pinned: Boolean) {
        viewModelScope.launch { repo.saveNote(note.copy(pinned = pinned, updatedAt = Dates.nowMillis())) }
    }

    // ---- settings ----

    fun setMotion(level: Int) = viewModelScope.launch { settingsStore.setMotion(level) }
    fun setHaptics(level: Int) = viewModelScope.launch { settingsStore.setHaptics(level) }
    fun setCompletionOnly(on: Boolean) = viewModelScope.launch { settingsStore.setCompletionOnly(on) }
    fun setPalette(id: String) = viewModelScope.launch { settingsStore.setPalette(id) }
    fun setTheme(mode: ThemeMode) = viewModelScope.launch { settingsStore.setTheme(mode) }
    fun setBackdrop(style: BackdropStyle) = viewModelScope.launch { settingsStore.setBackdrop(style) }
    fun setReduceTransparency(on: Boolean) = viewModelScope.launch { settingsStore.setReduceTransparency(on) }
    fun setHighContrast(on: Boolean) = viewModelScope.launch { settingsStore.setHighContrast(on) }
    fun setReminder(minute: Int?) = viewModelScope.launch { settingsStore.setReminderMinute(minute) }
    fun setWeekendRest(on: Boolean) = viewModelScope.launch { settingsStore.setWeekendRest(on) }
    fun setWeekStart(start: WeekStart) = viewModelScope.launch { settingsStore.setWeekStart(start) }
    fun setWalkthroughSeen(seen: Boolean) = viewModelScope.launch { settingsStore.setWalkthroughSeen(seen) }

    suspend fun exportJson(): String = repo.exportJson()

    /** Adds a Cadence export to the current data and says what happened in a toast. */
    suspend fun importJson(text: String) {
        val message = try {
            val result = repo.importJson(text)
            if (result.total == 0) {
                "Nothing new to import"
            } else {
                listOf(result.habits to "habit", result.tasks to "task", result.notes to "note")
                    .filter { it.first > 0 }
                    .joinToString(", ", prefix = "Imported ") { "${it.first} ${it.second}${if (it.first == 1) "" else "s"}" }
            }
        } catch (e: IllegalArgumentException) {
            e.message ?: "Couldn't import that file"
        }
        showToast(message)
    }

    fun eraseAll() {
        viewModelScope.launch {
            repo.eraseAll()
            _deferred.value = emptyList()
            dismissToast()
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CadenceApp
            return AppViewModel(app.container) as T
        }
    }
}
