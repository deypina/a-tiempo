package ec.atiempo.app.reminders

import ec.atiempo.app.data.IntakeRepository
import ec.atiempo.app.data.MedicationRepository
import ec.atiempo.app.data.SettingsRepository
import ec.atiempo.app.domain.ReminderRules.RepeatDecision
import java.time.Clock
import java.time.ZonedDateTime

/**
 * Glue between alarms, notifications and data. Receivers and screens call this;
 * the decisions themselves live in [ec.atiempo.app.domain.ReminderRules].
 */
class ReminderManager(
    private val medications: MedicationRepository,
    private val intakes: IntakeRepository,
    private val settings: SettingsRepository,
    private val scheduler: AlarmScheduler,
    private val notifier: ReminderNotifier,
    private val clock: Clock,
) {

    /** Boot, time or zone change, app update, permission granted, app opened. */
    suspend fun rescheduleAll() {
        medications.activeSchedules().forEach { scheduler.scheduleNext(it) }
    }

    /**
     * After saving, pausing or deleting a medication. [previousScheduleIds] are the schedules
     * it had before the change, so alarms of removed times are cancelled.
     */
    suspend fun refreshMedication(medicationId: Long, previousScheduleIds: Collection<Long>) {
        val item = medications.getMedication(medicationId)
        val current = if (item?.medication?.active == true) item.schedules else emptyList()
        val keep = current.map { it.id }.toSet()
        val known = previousScheduleIds + item?.schedules.orEmpty().map { it.id }
        known.filter { it !in keep }.distinct().forEach { scheduleId ->
            scheduler.cancelAll(scheduleId)
            notifier.cancelAllFor(scheduleId)
        }
        current.forEach { scheduler.scheduleNext(it) }
    }

    suspend fun onDailyAlarm(medicationId: Long, scheduleId: Long, scheduledAt: Long) {
        // Unknown, removed or paused: drop the alarm and do not arm it again.
        val target = intakes.reminderTarget(medicationId, scheduleId) ?: return

        // Arm tomorrow first, so nothing below can stop future reminders.
        val now = ZonedDateTime.now(clock)
        val scheduled = AlarmScheduler.zoned(scheduledAt, clock)
        val after = if (scheduled.isAfter(now)) scheduled else now
        scheduler.scheduleNext(target.schedule, after)

        val event = intakes.openIntake(target, scheduledAt) ?: return
        val prefs = settings.current()
        notifier.showIntake(target.medication, event, prefs.userName, prefs.snoozeMinutes, isRepeat = false)
        scheduler.scheduleRepeat(event, prefs.snoozeMinutes)
    }

    suspend fun onRepeatAlarm(eventId: String) {
        val event = intakes.getEvent(eventId) ?: return
        val target = intakes.reminderTarget(event.medicationId, event.scheduleId) ?: return
        val prefs = settings.current()
        when (val decision = intakes.applyRepeat(eventId, prefs.maxRepeats)) {
            is RepeatDecision.Remind -> {
                notifier.showIntake(
                    target.medication,
                    decision.event,
                    prefs.userName,
                    prefs.snoozeMinutes,
                    isRepeat = !decision.afterSnooze,
                )
                scheduler.scheduleRepeat(decision.event, prefs.snoozeMinutes)
            }
            is RepeatDecision.Miss -> {
                notifier.cancelIntake(event.scheduleId)
                notifier.showMissed(target.medication, decision.event)
            }
            RepeatDecision.Ignore -> Unit
        }
    }

    /** "Ya la tomé". Confirming twice subtracts once (the second call finds it TAKEN). */
    suspend fun onTaken(eventId: String) {
        val event = intakes.getEvent(eventId) ?: return
        val result = intakes.confirmIntake(eventId)
        scheduler.cancelRepeat(event.scheduleId)
        notifier.cancelIntake(event.scheduleId)
        result?.let { notifier.showTakenToast(it.medication) }
    }

    /** "Recordar en X min". Never changes the stock. */
    suspend fun onSnooze(eventId: String) {
        val event = intakes.getEvent(eventId) ?: return
        notifier.cancelIntake(event.scheduleId)
        val snoozed = intakes.snooze(eventId) ?: return
        scheduler.scheduleRepeat(snoozed, settings.current().snoozeMinutes)
    }
}
