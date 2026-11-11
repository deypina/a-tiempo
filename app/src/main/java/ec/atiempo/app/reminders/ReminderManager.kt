package ec.atiempo.app.reminders

import ec.atiempo.app.data.IntakeRepository
import ec.atiempo.app.data.MedicationRepository
import ec.atiempo.app.data.Settings
import ec.atiempo.app.data.SettingsRepository
import ec.atiempo.app.domain.Medication
import ec.atiempo.app.domain.PurchaseRules
import ec.atiempo.app.domain.ReminderRules
import ec.atiempo.app.domain.ReminderRules.RepeatDecision
import ec.atiempo.app.domain.Schedule
import ec.atiempo.app.domain.StockAlert
import ec.atiempo.app.domain.TodayRules
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
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
        val prefs = settings.current()
        medications.allMedications().forEach { syncPurchase(it, prefs) }
    }

    /**
     * After saving, pausing or deleting a medication. [previousScheduleIds] are the schedules
     * it had before the change, so alarms of removed times are cancelled. [alert] comes from a
     * corrected count in the edit screen that crossed a purchase threshold going down.
     */
    suspend fun refreshMedication(
        medicationId: Long,
        previousScheduleIds: Collection<Long>,
        alert: StockAlert? = null,
    ) = lock.withLock {
        val item = medications.getMedication(medicationId)
        val current = if (item?.medication?.active == true) item.schedules else emptyList()
        val keep = current.map { it.id }.toSet()
        val known = previousScheduleIds + item?.schedules.orEmpty().map { it.id }
        known.filter { it !in keep }.distinct().forEach { scheduleId ->
            scheduler.cancelAll(scheduleId)
            notifier.cancelAllFor(scheduleId)
        }
        current.forEach { arm(it) }

        val medication = item?.medication
        if (medication == null) {
            scheduler.cancelPurchase(medicationId)
            notifier.cancelPurchase(medicationId)
        } else {
            val prefs = settings.current()
            showOrRefreshPurchase(medication, alert, prefs)
            syncPurchase(medication, prefs, justAlerted = alert != null)
        }
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
    suspend fun onTaken(eventId: String) = lock.withLock { take(eventId) }

    /**
     * "Ya la tomé" from Hoy (C-22). Works the same as from the notification, also before the
     * reminder rang: the intake of today is created first, and the alarm later finds it taken.
     */
    suspend fun onTakenToday(medicationId: Long, scheduleId: Long, date: LocalDate) = lock.withLock {
        val target = intakes.reminderTarget(medicationId, scheduleId) ?: return@withLock
        // The day of the list on screen, so a list left open past midnight never marks the wrong day.
        val eventId = intakes.ensureEvent(target, TodayRules.scheduledAt(date, target.schedule.time, clock.zone))
        // A late confirmation also clears the "No confirmó su toma" notice.
        notifier.cancelAllFor(scheduleId)
        take(eventId)
    }

    /**
     * "Deshacer" (C-23): gives the dose back and leaves the intake pending. If its time already
     * passed, the reminder starts again after the snooze interval, as for any unanswered intake.
     */
    suspend fun onUndo(eventId: String) = lock.withLock {
        val result = intakes.undoIntake(eventId) ?: return@withLock
        val prefs = settings.current()
        if (result.event.scheduledAt <= clock.millis()) {
            scheduler.scheduleRepeat(result.event, prefs.snoozeMinutes)
        }
        showOrRefreshPurchase(result.medication, null, prefs)
        syncPurchase(result.medication, prefs)
    }

    /** When a snoozed intake of this schedule reminds again. */
    fun snoozedUntil(scheduleId: Long): Long? = scheduler.repeatAt(scheduleId)

    private suspend fun take(eventId: String) {
        val event = intakes.getEvent(eventId) ?: return
        val result = intakes.confirmIntake(eventId)
        scheduler.cancelRepeat(event.scheduleId)
        notifier.cancelIntake(event.scheduleId)
        result ?: return
        notifier.showTakenToast(result.medication)
        val prefs = settings.current()
        // Crossing two thresholds at once (6 to 2) returns only the most serious alert (C-21).
        showOrRefreshPurchase(result.medication, result.alert, prefs)
        syncPurchase(result.medication, prefs, justAlerted = result.alert != null)
    }

    /** Daily urgent purchase reminder (C-17). */
    suspend fun onPurchaseAlarm(medicationId: Long, scheduledAt: Long) = lock.withLock {
        if (ReminderRules.isStaleAlarm(scheduler.purchaseArmedAt(medicationId), scheduledAt)) return@withLock
        handlePurchaseAlarm(medicationId, scheduledAt)
    }

    /** "Ya compré": adds [quantity] and turns purchase reminders off. Null if the medication is gone. */
    suspend fun onPurchase(medicationId: Long, quantity: Double): Medication? = lock.withLock {
        val result = medications.registerPurchase(medicationId, quantity) ?: return@withLock null
        scheduler.cancelPurchase(medicationId)
        notifier.cancelPurchase(medicationId)
        notifier.showPurchaseDoneToast(result.medication)
        result.medication
    }

    /** "Corregir cantidad". Alerts only when the count goes down across a threshold. */
    suspend fun onAdjust(medicationId: Long, newValue: Double) = lock.withLock {
        val result = medications.adjustStock(medicationId, newValue) ?: return@withLock
        val prefs = settings.current()
        showOrRefreshPurchase(result.medication, result.alert, prefs)
        syncPurchase(result.medication, prefs, justAlerted = result.alert != null)
    }

    /**
     * "Recordar en X min". Never changes the stock. The alarm (and its time) is stored before the
     * status changes, so Hoy already knows the new time when it sees the intake snoozed.
     */
    suspend fun onSnooze(eventId: String) = lock.withLock {
        val event = intakes.getEvent(eventId) ?: return@withLock
        notifier.cancelIntake(event.scheduleId)
        if (!ReminderRules.canSnooze(event)) return@withLock
        scheduler.scheduleRepeat(event, settings.current().snoozeMinutes)
        if (intakes.snooze(eventId) == null) scheduler.cancelRepeat(event.scheduleId)
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

    /** Shows a new alert, or keeps the count of the one already on screen up to date. */
    private fun showOrRefreshPurchase(medication: Medication, alert: StockAlert?, prefs: Settings) {
        if (!medication.active) return
        when {
            alert != null -> notifier.showPurchase(medication, alert, prefs.userName)
            PurchaseRules.keepAlertVisible(medication) ->
                notifier.refreshPurchase(medication, PurchaseRules.currentLevel(medication), prefs.userName)
        }
    }

    private suspend fun handlePurchaseAlarm(medicationId: Long, scheduledAt: Long) {
        val medication = medications.getMedication(medicationId)?.medication
        if (medication == null || !PurchaseRules.needsDailyReminder(medication)) {
            scheduler.cancelPurchase(medicationId)
            return
        }
        val prefs = settings.current()
        val now = ZonedDateTime.now(clock)
        val scheduled = AlarmScheduler.zoned(scheduledAt, clock)
        scheduler.schedulePurchase(medicationId, prefs.urgentReminderTime, if (scheduled.isAfter(now)) scheduled else now)
        notifier.showPurchase(medication, PurchaseRules.dailyAlert(medication), prefs.userName)
    }

    /**
     * Keeps purchase alerts in line with the stock: removes a stale alert and arms or cancels the
     * daily urgent reminder. When an alert was [justAlerted], the first repeat is tomorrow.
     */
    private suspend fun syncPurchase(medication: Medication, prefs: Settings, justAlerted: Boolean = false) {
        val id = medication.id
        if (!medication.active || !PurchaseRules.keepAlertVisible(medication)) notifier.cancelPurchase(id)
        if (!PurchaseRules.needsDailyReminder(medication)) {
            scheduler.cancelPurchase(id)
            return
        }
        val nowMillis = clock.millis()
        val armedAt = scheduler.purchaseArmedAt(id)
        val time = prefs.urgentReminderTime
        when {
            justAlerted -> scheduler.schedulePurchase(id, time, ZonedDateTime.now(clock).with(LocalTime.MAX))
            armedAt == null -> scheduler.schedulePurchase(id, time)
            // Due but never fired (phone off, force stop): remind now if it is recent.
            armedAt <= nowMillis -> {
                if (ReminderRules.shouldCatchUp(armedAt, nowMillis)) {
                    handlePurchaseAlarm(id, armedAt)
                } else {
                    scheduler.schedulePurchase(id, time)
                }
            }
            // Still ahead at the configured time: re-set the same moment (a reboot deletes alarms).
            AlarmScheduler.zoned(armedAt, clock).let { it.hour == time.hour && it.minute == time.minute } ->
                scheduler.schedulePurchaseAt(id, armedAt)
            else -> scheduler.schedulePurchase(id, time)
        }
    }

    /** A reboot or force stop deletes waiting repeats; re-arm them so the intake still ends answered or missed. */
    private suspend fun restoreRepeats() {
        val now = clock.millis()
        val minutes = settings.current().snoozeMinutes
        intakes.unansweredSince(now - RESTORE_WINDOW_MS)
            // A future intake (taken early in Hoy, then undone) waits for its own daily alarm.
            .filter { it.scheduledAt <= now && !scheduler.hasRepeat(it.scheduleId) }
            .forEach { scheduler.scheduleRepeat(it, minutes) }
    }

    private companion object {
        /** Longer than a full cycle (30 min × 4) with margin; older ones are left as they are. */
        const val RESTORE_WINDOW_MS = 6 * 60 * 60 * 1000L
    }
}
