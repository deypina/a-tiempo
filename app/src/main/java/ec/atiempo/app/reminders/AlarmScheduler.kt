package ec.atiempo.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import ec.atiempo.app.domain.IntakeEvent
import ec.atiempo.app.domain.ReminderRules
import ec.atiempo.app.domain.Schedule
import java.time.Clock
import java.time.Instant
import java.time.ZonedDateTime

/**
 * Exact alarms with AlarmManager (docs/ESPECIFICACION_TECNICA.md §4).
 * Each schedule owns two alarm slots: the daily one and the repeat/snooze one.
 */
class AlarmScheduler(
    private val context: Context,
    private val clock: Clock,
) {
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    /** Arms the next daily alarm of [schedule], strictly after [after]. Replaces any previous one. */
    fun scheduleNext(schedule: Schedule, after: ZonedDateTime = ZonedDateTime.now(clock)) {
        val triggerAt = ReminderRules.nextOccurrence(schedule.time, after).toInstant().toEpochMilli()
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
            .setAction(ReminderAlarmReceiver.ACTION_INTAKE_ALARM)
            .putExtra(EXTRA_MEDICATION_ID, schedule.medicationId)
            .putExtra(EXTRA_SCHEDULE_ID, schedule.id)
            .putExtra(EXTRA_SCHEDULED_AT, triggerAt)
        setAlarm(triggerAt, broadcast(dailyCode(schedule.id), intent))
    }

    /** Arms the repeat (or snooze) alarm of [event] in [minutes]. */
    fun scheduleRepeat(event: IntakeEvent, minutes: Int) {
        val triggerAt = clock.millis() + minutes * 60_000L
        setAlarm(triggerAt, broadcast(repeatCode(event.scheduleId), repeatIntent().putExtra(EXTRA_EVENT_ID, event.id)))
    }

    fun cancelRepeat(scheduleId: Long) = cancel(repeatCode(scheduleId), repeatIntent())

    fun cancelAll(scheduleId: Long) {
        cancel(dailyCode(scheduleId), Intent(context, ReminderAlarmReceiver::class.java).setAction(ReminderAlarmReceiver.ACTION_INTAKE_ALARM))
        cancelRepeat(scheduleId)
    }

    private fun repeatIntent() = Intent(context, ReminderAlarmReceiver::class.java)
        .setAction(ReminderAlarmReceiver.ACTION_REPEAT_ALARM)

    /** Without the exact-alarm permission the reminder may arrive a few minutes late, but it still arrives. */
    private fun setAlarm(triggerAt: Long, operation: PendingIntent) {
        if (ReminderPermissions.exactAlarmsAllowed(context)) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
                return
            } catch (_: SecurityException) {
                // The permission was revoked in the meantime: fall through to the inexact alarm.
            }
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
    }

    private fun cancel(requestCode: Int, intent: Intent) {
        val existing = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        ) ?: return
        alarmManager.cancel(existing)
        existing.cancel()
    }

    private fun broadcast(requestCode: Int, intent: Intent): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        const val EXTRA_MEDICATION_ID = "ec.atiempo.app.extra.MEDICATION_ID"
        const val EXTRA_SCHEDULE_ID = "ec.atiempo.app.extra.SCHEDULE_ID"
        const val EXTRA_SCHEDULED_AT = "ec.atiempo.app.extra.SCHEDULED_AT"
        const val EXTRA_EVENT_ID = "ec.atiempo.app.extra.EVENT_ID"

        /** Stable request code per schedule; the repeat slot uses a separate range. */
        fun dailyCode(scheduleId: Long): Int = scheduleId.toInt()
        fun repeatCode(scheduleId: Long): Int = REPEAT_CODE_OFFSET + scheduleId.toInt()

        private const val REPEAT_CODE_OFFSET = 100_000

        /** Epoch millis to the zone-aware time used to arm the next day. */
        fun zoned(millis: Long, clock: Clock): ZonedDateTime = Instant.ofEpochMilli(millis).atZone(clock.zone)
    }
}
