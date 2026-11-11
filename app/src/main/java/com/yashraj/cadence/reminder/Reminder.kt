package com.yashraj.cadence.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.yashraj.cadence.CadenceApp
import com.yashraj.cadence.MainActivity
import com.yashraj.cadence.R
import com.yashraj.cadence.domain.Dates
import com.yashraj.cadence.domain.buildHabitViews
import com.yashraj.cadence.domain.planToday
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/** The one daily reminder. Inexact, repeating, and never more than one a day. */
object ReminderScheduler {
    private const val ChannelId = "daily"
    private const val RequestCode = 4001

    fun schedule(context: Context, minuteOfDay: Int?) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val pending = pendingIntent(context)
        alarms.cancel(pending)
        if (minuteOfDay == null) return
        val first = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
            set(Calendar.MINUTE, minuteOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        alarms.setInexactRepeating(AlarmManager.RTC, first.timeInMillis, AlarmManager.INTERVAL_DAY, pending)
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        RequestCode,
        Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun canNotify(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun post(context: Context, text: String) {
        if (!canNotify(context)) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(ChannelId, "Daily reminder", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "One reminder a day, at the time you choose"
                setShowBadge(false)
            },
        )
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        manager.notify(
            1,
            NotificationCompat.Builder(context, ChannelId)
                .setSmallIcon(R.drawable.ic_stat_cadence)
                .setContentTitle("Today")
                .setContentText(text)
                .setContentIntent(open)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .build(),
        )
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val container = (context.applicationContext as CadenceApp).container
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val today = Dates.today()
                val settings = container.settingsStore.settings.first()
                val views = buildHabitViews(container.repository.habits.first(), container.repository.habitLogs.first(), today, settings.weekendRest)
                val plan = planToday(views, container.repository.tasks.first(), today)
                if (plan.total > 0 && !plan.allDone) {
                    val next = plan.nextUp()
                    val noun = if (plan.total == 1) "beat" else "beats"
                    ReminderScheduler.post(context, "${plan.total} $noun today. Next up: ${next?.title}.")
                }
            } finally {
                pending.finish()
            }
        }
    }
}

/** Re-arms the reminder after a reboot, since alarms do not survive one. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        val container = (context.applicationContext as CadenceApp).container
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                ReminderScheduler.schedule(context, container.settingsStore.settings.first().reminderMinute)
            } finally {
                pending.finish()
            }
        }
    }
}
