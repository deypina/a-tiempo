package ec.atiempo.app.reminders

import ec.atiempo.app.data.IntakeRepository
import ec.atiempo.app.data.MedicationRepository
import ec.atiempo.app.data.SettingsRepository
import ec.atiempo.app.domain.ReminderRules
import ec.atiempo.app.domain.ReminderRules.RepeatDecision
import ec.atiempo.app.domain.Schedule
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.ZonedDateTime

/**
 * Glue between alarms, notifications and data. Receivers and screens call this;
 * the decisions themselves live in [ReminderRules].
 *
 * Every entry point runs under one lock, so an alarm and a button tap arriving
 * together are handled one after the other and never show a reminder already answered.
 */
class ReminderManager(
    private val medications: MedicationRepository,
    private val intakes: IntakeRepository,
    private val settings: SettingsRepository,
    private val scheduler: AlarmScheduler,
    private val notifier: ReminderNotifier,
    private val clock: Clock,
) {
    private val lock = Mutex()

    /** Boot, time or zone change, app update, permission granted, app opened. */
    suspend fun rescheduleAll() = lock.withLock {
        medications.activeSchedules().forEach { arm(it) }
        restoreRepeats()
    }

    /**
     * After saving, pausing or deleting a medication. [previousScheduleIds] are the schedules
     * it had before the change, so alarms of removed times are cancelled.
     */
    suspend fun refreshMedication(medicationId: Long, previousScheduleIds: Collection<Long>) = lock.withLock {
        val item = medications.getMedication(medicationId)
        val current = if (item?.medication?.active == true) item.schedules else emptyList()
        val keep = current.map { it.id }.toSet()
        val known = previousScheduleIds + item?.schedules.orEmpty().map { it.id }
        known.filter { it !in keep }.distinct().forEach { scheduleId ->
            scheduler.cancelAll(scheduleId)
            notifier.cancelAllFor(scheduleId)
        }
        current.forEach { arm(it) }
    }

    suspend fun onDailyAlarm(medicationId: Long, scheduleId: Long, scheduledAt: Long) = lock.withLock {
        // Already handled by a catch-up, or replaced by a newer alarm.
        if (ReminderRules.isStaleAlarm(scheduler.armedAt(scheduleId), scheduledAt)) return@withLock
        handleDailyAlarm(medicationId, scheduleId, scheduledAt)
    }

    suspend fun onRepeatAlarm(eventId: String) = lock.withLock {
        val event = intakes.getEvent(eventId) ?: return@withLock
        val target = intakes.reminderTarget(event.medicationId, event.scheduleId) ?: return@withLock
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
            RepeatDecision.Ignore -> notifier.cancelIntake(event.scheduleId)
        }
    }

    /** "Ya la tomé". Confirming twice subtracts once (the second call finds it TAKEN). */
    suspend fun onTaken(eventId: String) = lock.withLock {
        val event = intakes.getEvent(eventId) ?: return@withLock
        val result = intakes.confirmIntake(eventId)
        scheduler.cancelRepeat(event.scheduleId)
        notifier.cancelIntake(event.scheduleId)
        result?.let { notifier.showTakenToast(it.medication) }
    }

    /** "Recordar en X min". Never changes the stock. */
    suspend fun onSnooze(eventId: String) = lock.withLock {
        val event = intakes.getEvent(eventId) ?: return@withLock
        notifier.cancelIntake(event.scheduleId)
        val snoozed = intakes.snooze(eventId) ?: return@withLock
        scheduler.scheduleRepeat(snoozed, settings.current().snoozeMinutes)
    }

    /**
     * Arms the next daily alarm. If the one already armed was due and never fired
     * (phone off, force stop, late alarm), reminds now instead of silently skipping today.
     */
    private suspend fun arm(schedule: Schedule) {
        val armedAt = scheduler.armedAt(schedule.id)
        if (armedAt != null && ReminderRules.shouldCatchUp(armedAt, clock.millis())) {
            handleDailyAlarm(schedule.medicationId, schedule.id, armedAt)
        } else {
            scheduler.scheduleNext(schedule)
        }
    }

    private suspend fun handleDailyAlarm(medicationId: Long, scheduleId: Long, scheduledAt: Long) {
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

    /** A reboot or force stop deletes waiting repeats; re-arm them so the intake still ends answered or missed. */
    private suspend fun restoreRepeats() {
        val since = clock.millis() - RESTORE_WINDOW_MS
        val minutes = settings.current().snoozeMinutes
        intakes.unansweredSince(since)
            .filter { !scheduler.hasRepeat(it.scheduleId) }
            .forEach { scheduler.scheduleRepeat(it, minutes) }
    }

    private companion object {
        /** Longer than a full cycle (30 min × 4) with margin; older ones are left as they are. */
        const val RESTORE_WINDOW_MS = 6 * 60 * 60 * 1000L
    }
}
