package ec.atiempo.app.reminders

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ec.atiempo.app.ATiempoApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/** Runs [block] off the main thread and keeps the receiver alive until it ends (goAsync). */
private fun BroadcastReceiver.runAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    receiverScope.launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}

private val Context.reminders: ReminderManager
    get() = (applicationContext as ATiempoApp).container.reminderManager

/** Daily and repeat alarms. Not exported: only this app's PendingIntents reach it. */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_INTAKE_ALARM -> {
                val medicationId = intent.getLongExtra(AlarmScheduler.EXTRA_MEDICATION_ID, -1L)
                val scheduleId = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, -1L)
                val scheduledAt = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, -1L)
                if (medicationId <= 0 || scheduleId <= 0 || scheduledAt <= 0) return
                runAsync { context.reminders.onDailyAlarm(medicationId, scheduleId, scheduledAt) }
            }
            ACTION_REPEAT_ALARM -> {
                val eventId = intent.getStringExtra(AlarmScheduler.EXTRA_EVENT_ID) ?: return
                runAsync { context.reminders.onRepeatAlarm(eventId) }
            }
        }
    }

    companion object {
        const val ACTION_INTAKE_ALARM = "ec.atiempo.app.action.INTAKE_ALARM"
        const val ACTION_REPEAT_ALARM = "ec.atiempo.app.action.REPEAT_ALARM"
    }
}

/** Buttons inside the notification. Not exported; ids are checked against the database. */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra(AlarmScheduler.EXTRA_EVENT_ID) ?: return
        when (intent.action) {
            ACTION_TAKEN -> runAsync { context.reminders.onTaken(eventId) }
            ACTION_SNOOZE -> runAsync { context.reminders.onSnooze(eventId) }
        }
    }

    companion object {
        const val ACTION_TAKEN = "ec.atiempo.app.action.TAKEN"
        const val ACTION_SNOOZE = "ec.atiempo.app.action.SNOOZE"
    }
}

/**
 * Boot, clock or zone change, app update and exact-alarm permission change.
 * Exported because the system sends these; it only re-arms alarms and reads nothing from the Intent.
 */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            -> runAsync { context.reminders.rescheduleAll() }
        }
    }
}
