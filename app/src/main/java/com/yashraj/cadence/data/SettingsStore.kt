package com.yashraj.cadence.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yashraj.cadence.ui.theme.DefaultPaletteId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode(val label: String) { Light("Light"), Dark("Dark"), Auto("Auto") }
enum class BackdropStyle(val label: String) { Glow("Glow"), Soft("Soft"), Plain("Plain") }
enum class WeekStart(val label: String) { Monday("Monday"), Sunday("Sunday") }

/** Four-stop intensity used by both Motion and Haptics: 0 Off, 1 Subtle/Soft, 2 Standard/Balanced, 3 Expressive/Firm. */
const val LevelOff = 0
const val LevelDefault = 2

data class Settings(
    val motion: Int = LevelDefault,
    val haptics: Int = LevelDefault,
    val completionOnly: Boolean = false,
    val palette: String = DefaultPaletteId,
    val theme: ThemeMode = ThemeMode.Auto,
    val backdrop: BackdropStyle = BackdropStyle.Glow,
    val reduceTransparency: Boolean = false,
    val highContrast: Boolean = false,
    /** Minutes after midnight for the one daily reminder; null when it is off. */
    val reminderMinute: Int? = null,
    val weekendRest: Boolean = true,
    val weekStart: WeekStart = WeekStart.Monday,
    /** Epoch day the day-complete sweep last played, so it plays once a day at most. */
    val lastCelebratedDay: Long = -1,
    /** Whether the first-run walkthrough has been finished or skipped. */
    val walkthroughSeen: Boolean = false,
    /** Folder (a document-tree URI) the automatic backup writes to; null when automatic backup is off. */
    val backupFolder: String? = null,
    /** When the last backup was written, in epoch milliseconds; 0 if never. */
    val lastBackupAt: Long = 0,
    /** When the background photo was chosen (epoch millis), or 0 for the flowing gradient. Stays on the device. */
    val backgroundImage: Long = 0,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cadence_settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val motion = intPreferencesKey("motion")
        val haptics = intPreferencesKey("haptics")
        val completionOnly = booleanPreferencesKey("completion_only")
        val palette = stringPreferencesKey("palette")
        val theme = stringPreferencesKey("theme")
        val backdrop = stringPreferencesKey("backdrop")
        val reduceTransparency = booleanPreferencesKey("reduce_transparency")
        val highContrast = booleanPreferencesKey("high_contrast")
        val reminderMinute = intPreferencesKey("reminder_minute")
        val weekendRest = booleanPreferencesKey("weekend_rest")
        val weekStart = stringPreferencesKey("week_start")
        val lastCelebratedDay = longPreferencesKey("last_celebrated_day")
        val walkthroughSeen = booleanPreferencesKey("walkthrough_seen")
        val backupFolder = stringPreferencesKey("backup_folder")
        val lastBackupAt = longPreferencesKey("last_backup_at")
        val backgroundImage = longPreferencesKey("background_image")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        val d = Settings()
        Settings(
            motion = p[Keys.motion]?.coerceIn(0, 3) ?: d.motion,
            haptics = p[Keys.haptics]?.coerceIn(0, 3) ?: d.haptics,
            completionOnly = p[Keys.completionOnly] ?: d.completionOnly,
            palette = p[Keys.palette] ?: d.palette,
            theme = p[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: d.theme,
            backdrop = p[Keys.backdrop]?.let { runCatching { BackdropStyle.valueOf(it) }.getOrNull() } ?: d.backdrop,
            reduceTransparency = p[Keys.reduceTransparency] ?: d.reduceTransparency,
            highContrast = p[Keys.highContrast] ?: d.highContrast,
            reminderMinute = p[Keys.reminderMinute]?.takeIf { it >= 0 },
            weekendRest = p[Keys.weekendRest] ?: d.weekendRest,
            weekStart = p[Keys.weekStart]?.let { runCatching { WeekStart.valueOf(it) }.getOrNull() } ?: d.weekStart,
            lastCelebratedDay = p[Keys.lastCelebratedDay] ?: d.lastCelebratedDay,
            walkthroughSeen = p[Keys.walkthroughSeen] ?: d.walkthroughSeen,
            backupFolder = p[Keys.backupFolder]?.takeIf { it.isNotEmpty() },
            lastBackupAt = p[Keys.lastBackupAt] ?: d.lastBackupAt,
            backgroundImage = p[Keys.backgroundImage] ?: d.backgroundImage,
        )
    }

    suspend fun setMotion(level: Int) = edit { it[Keys.motion] = level }
    suspend fun setHaptics(level: Int) = edit { it[Keys.haptics] = level }
    suspend fun setCompletionOnly(on: Boolean) = edit { it[Keys.completionOnly] = on }
    suspend fun setPalette(id: String) = edit { it[Keys.palette] = id }
    suspend fun setTheme(mode: ThemeMode) = edit { it[Keys.theme] = mode.name }
    suspend fun setBackdrop(style: BackdropStyle) = edit { it[Keys.backdrop] = style.name }
    suspend fun setReduceTransparency(on: Boolean) = edit { it[Keys.reduceTransparency] = on }
    suspend fun setHighContrast(on: Boolean) = edit { it[Keys.highContrast] = on }
    suspend fun setReminderMinute(minute: Int?) = edit { it[Keys.reminderMinute] = minute ?: -1 }
    suspend fun setWeekendRest(on: Boolean) = edit { it[Keys.weekendRest] = on }
    suspend fun setWeekStart(start: WeekStart) = edit { it[Keys.weekStart] = start.name }
    suspend fun setLastCelebratedDay(day: Long) = edit { it[Keys.lastCelebratedDay] = day }
    suspend fun setWalkthroughSeen(seen: Boolean) = edit { it[Keys.walkthroughSeen] = seen }

    suspend fun setBackupFolder(uri: String?) = edit { it[Keys.backupFolder] = uri.orEmpty() }
    suspend fun setLastBackupAt(millis: Long) = edit { it[Keys.lastBackupAt] = millis }
    suspend fun setBackgroundImage(stamp: Long) = edit { it[Keys.backgroundImage] = stamp }

    /** Applies the settings from a restored backup in one write. Device-only values (backup folder, last backup) are kept. */
    suspend fun restore(s: Settings) = edit {
        it[Keys.motion] = s.motion
        it[Keys.haptics] = s.haptics
        it[Keys.completionOnly] = s.completionOnly
        it[Keys.palette] = s.palette
        it[Keys.theme] = s.theme.name
        it[Keys.backdrop] = s.backdrop.name
        it[Keys.reduceTransparency] = s.reduceTransparency
        it[Keys.highContrast] = s.highContrast
        it[Keys.reminderMinute] = s.reminderMinute ?: -1
        it[Keys.weekendRest] = s.weekendRest
        it[Keys.weekStart] = s.weekStart.name
        it[Keys.walkthroughSeen] = true
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }
}
