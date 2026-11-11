package com.yashraj.cadence

import android.app.Application
import android.content.Context
import com.yashraj.cadence.data.CadenceDatabase
import com.yashraj.cadence.data.CadenceRepository
import com.yashraj.cadence.data.SettingsStore
import com.yashraj.cadence.domain.Dates
import com.yashraj.cadence.feel.HapticEngine
import com.yashraj.cadence.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Today's epoch day, kept current across midnight and resume. */
class DayClock(scope: CoroutineScope) {
    private val _today = MutableStateFlow(Dates.today())
    val today: StateFlow<Long> = _today

    init {
        scope.launch {
            while (true) {
                val now = LocalDateTime.now()
                val midnight = LocalDate.now().plusDays(1).atStartOfDay()
                val wait = java.time.Duration.between(now, midnight).toMillis() + 1_000
                delay(wait)
                refresh()
            }
        }
    }

    fun refresh() {
        _today.value = Dates.today()
    }
}

/** Hand-wired singletons: one database, one settings store, one haptic engine. No account, no network. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val database = CadenceDatabase.create(context)
    val repository = CadenceRepository(database)
    val settingsStore = SettingsStore(context)
    val haptics = HapticEngine(context)
    val dayClock = DayClock(appScope)
}

class CadenceApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.appScope.launch {
            ReminderScheduler.schedule(this@CadenceApp, container.settingsStore.settings.first().reminderMinute)
        }
    }
}
