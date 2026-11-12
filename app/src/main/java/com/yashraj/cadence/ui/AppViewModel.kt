package com.yashraj.cadence.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.yashraj.cadence.AppContainer
import android.net.Uri
import com.yashraj.cadence.BuildConfig
import com.yashraj.cadence.CadenceApp
import com.yashraj.cadence.data.BackdropStyle
import com.yashraj.cadence.data.BackupFolder
import com.yashraj.cadence.data.BackupFormat
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
import com.yashraj.cadence.ui.components.BackgroundStore
import com.yashraj.cadence.ui.components.LoadedBackground
import com.yashraj.cadence.update.CheckResult
import com.yashraj.cadence.update.ReleaseInfo
import com.yashraj.cadence.update.UpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

/** The Undo toast. Completing and deleting are instant and reversible for five seconds. */
class ToastState(val token: Long, val message: String, val undo: (() -> Unit)?)

sealed interface UiEvent {
    /** The last beat of the day was just completed by the user. */
    data object DayComplete : UiEvent
}

const val ToastMillis = 5_000

/** A change is written to the backup folder this long after the last edit, and again whenever the app is left. */
private const val BackupDebounceMs = 20_000L
private const val DayMs = 24 * 60 * 60 * 1000L

/** Where the "Check for updates" row in Settings is. */
sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus
    data object UpToDate : UpdateStatus
    data object Failed : UpdateStatus
    data class Available(val release: ReleaseInfo) : UpdateStatus
}

/**
 * App-wide state and actions. Every list is a flow of what is saved; screens only draw it. Null means
 * "still loading", so a screen can wait instead of flashing an empty state.
 */
@OptIn(FlowPreview::class)
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

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    private val _updatePrompt = MutableStateFlow<ReleaseInfo?>(null)

    /** The release to offer in the update popup, or null when there is nothing to show. */
    val updatePrompt: StateFlow<ReleaseInfo?> = _updatePrompt.asStateFlow()
    val appVersion: String = BuildConfig.VERSION_NAME

    /** The chosen background photo, loaded whenever it changes; null shows the flowing gradient. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val background: StateFlow<LoadedBackground?> = settingsStore.settings
        .map { it.backgroundImage }
        .distinctUntilChanged()
        .mapLatest { stamp -> if (stamp == 0L) null else BackgroundStore.load(container.appContext) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun chooseBackground(uri: Uri) {
        viewModelScope.launch {
            if (BackgroundStore.save(container.appContext, uri)) settingsStore.setBackgroundImage(Dates.nowMillis()) else showToast("Couldn't use that image")
        }
    }

    fun removeBackground() {
        viewModelScope.launch {
            BackgroundStore.clear(container.appContext)
            settingsStore.setBackgroundImage(0)
        }
    }

    private val backupMutex = Mutex()
    private var backupDirty = false
    private val _backupProblem = MutableStateFlow<String?>(null)

    /** Why the last automatic backup failed, or null when it is fine. */
    val backupProblem: StateFlow<String?> = _backupProblem.asStateFlow()
    private val _backupBusy = MutableStateFlow(false)
    val backupBusy: StateFlow<Boolean> = _backupBusy.asStateFlow()
    private val _restoreDeclined = MutableStateFlow(false)

    /**
     * True on a fresh install with nothing in it, where a restore from a backup file is worth offering before the
     * walkthrough. Null while the first data is still loading.
     */
    val restoreOffer: StateFlow<Boolean?> = combine(settings, habitViews, tasks, notes, _restoreDeclined) { s, h, t, n, declined ->
        if (s == null || h == null || t == null || n == null) null else !declined && !s.walkthroughSeen && h.isEmpty() && t.isEmpty() && n.isEmpty()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // One quiet check at start-up. It only speaks up when there is a newer release.
        checkForUpdates(manual = false)
        // Automatic backup: any change to the data or the portable settings schedules a write to the backup folder.
        val portable = combine(repo.habits, repo.habitLogs, repo.tasks, repo.notes, settingsStore.settings.map { BackupFormat.settingsToJson(it).toString() }.distinctUntilChanged()) { _, _, _, _, _ -> Unit }
        viewModelScope.launch { portable.drop(1).collect { backupDirty = true } }
        viewModelScope.launch { portable.drop(1).debounce(BackupDebounceMs).collect { runBackup(manual = false) } }
        // And at least once a day, even if nothing changed.
        viewModelScope.launch {
            val s = settingsStore.settings.first()
            if (s.backupFolder != null && Dates.nowMillis() - s.lastBackupAt > DayMs) runBackup(manual = false)
        }
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

    /** Leaving the app is the moment to save any change not yet backed up; it outlives the screen. */
    fun onStop() {
        if (backupDirty) container.appScope.launch { runBackup(manual = false) }
    }

    // ---- updates ----

    /** The start-up check stays silent unless it finds something; a tap in Settings always answers. */
    fun checkForUpdates(manual: Boolean) {
        if (_updateStatus.value == UpdateStatus.Checking) return
        val previous = _updateStatus.value
        _updateStatus.value = UpdateStatus.Checking
        viewModelScope.launch {
            when (val result = UpdateChecker.check(appVersion)) {
                is CheckResult.Available -> {
                    _updateStatus.value = UpdateStatus.Available(result.release)
                    _updatePrompt.value = result.release
                }
                CheckResult.UpToDate -> _updateStatus.value = UpdateStatus.UpToDate
                CheckResult.Failed -> {
                    _updateStatus.value = if (manual) UpdateStatus.Failed else (previous as? UpdateStatus.Available ?: UpdateStatus.Idle)
                    if (manual) showToast("Couldn't check for updates")
                }
            }
        }
    }

    fun showUpdatePrompt(release: ReleaseInfo) { _updatePrompt.value = release }
    fun dismissUpdatePrompt() { _updatePrompt.value = null }

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

    suspend fun exportJson(): String = repo.exportJson(settings.value)

    /**
     * Reads a Cadence backup and says what happened in a toast. [replace] erases what is here first and also restores
     * the settings in the file; otherwise the file is only added to the current data.
     */
    suspend fun importJson(text: String, replace: Boolean = false) {
        val message = try {
            val result = repo.importJson(text, replace)
            if (replace) result.settings?.let { settingsStore.restore(it) }
            val parts = listOf(result.habits to "habit", result.tasks to "task", result.notes to "note")
                .filter { it.first > 0 }
                .joinToString(", ") { "${it.first} ${it.second}${if (it.first == 1) "" else "s"}" }
            when {
                replace && parts.isEmpty() -> "Backup restored"
                replace -> "Restored $parts"
                parts.isEmpty() -> "Nothing new to import"
                else -> "Imported $parts"
            }
        } catch (e: IllegalArgumentException) {
            e.message ?: "Couldn't import that file"
        }
        showToast(message)
    }

    /** "Start fresh" on the first-run restore offer. */
    fun declineRestore() { _restoreDeclined.value = true }

    // ---- automatic backup ----

    /** Turns automatic backup on, writing to [folder] (a document tree the user just picked), and writes one right away. */
    fun chooseBackupFolder(folder: Uri) {
        viewModelScope.launch {
            val old = settingsStore.settings.first().backupFolder
            try {
                BackupFolder.remember(container.appContext, folder)
            } catch (e: SecurityException) {
                showToast("Couldn't get access to that folder")
                return@launch
            }
            if (old != null && old != folder.toString()) BackupFolder.forget(container.appContext, old)
            settingsStore.setBackupFolder(folder.toString())
            settingsStore.setLastBackupAt(0)
            _backupProblem.value = null
            runBackup(manual = true)
        }
    }

    fun turnOffBackup() {
        viewModelScope.launch {
            settingsStore.settings.first().backupFolder?.let { BackupFolder.forget(container.appContext, it) }
            settingsStore.setBackupFolder(null)
            _backupProblem.value = null
            backupDirty = false
            showToast("Automatic backup is off. The last backup stays in its folder")
        }
    }

    fun backupNow() {
        viewModelScope.launch { runBackup(manual = true) }
    }

    /** Writes the backup file. An empty database is never written, so a fresh install cannot overwrite a good backup. */
    private suspend fun runBackup(manual: Boolean) {
        val s = settingsStore.settings.first()
        val folder = s.backupFolder ?: return
        if (manual) _backupBusy.value = true
        try {
            backupMutex.withLock {
                if (repo.isEmpty()) {
                    if (manual) showToast("Nothing to back up yet")
                    return
                }
                try {
                    val json = repo.exportJson(s)
                    withContext(Dispatchers.IO) { BackupFolder.write(container.appContext, folder, json) }
                    settingsStore.setLastBackupAt(Dates.nowMillis())
                    backupDirty = false
                    _backupProblem.value = null
                    if (manual) showToast("Backed up")
                } catch (e: IOException) {
                    _backupProblem.value = "Couldn't write to the backup folder. Choose it again."
                    if (manual) showToast("Couldn't back up. Choose the folder again")
                }
            }
        } finally {
            if (manual) _backupBusy.value = false
        }
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
