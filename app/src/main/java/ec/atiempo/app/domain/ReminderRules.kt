package ec.atiempo.app.domain

import java.time.ZonedDateTime

/** Reminder rules from docs/ESPECIFICACION_TECNICA.md §4. Every function is pure. */
object ReminderRules {

    const val DEFAULT_SNOOZE_MINUTES = 15
    const val DEFAULT_MAX_REPEATS = 3

    /** Next time [time] happens strictly after [after]: later today, otherwise tomorrow. */
    fun nextOccurrence(time: DoseTime, after: ZonedDateTime): ZonedDateTime {
        val date = after.toLocalDate()
        val today = date.atTime(time.hour, time.minute).atZone(after.zone)
        if (today.isAfter(after)) return today
        return date.plusDays(1).atTime(time.hour, time.minute).atZone(after.zone)
    }

    /**
     * The daily alarm fired. Returns the event to show, or null when the intake was
     * already answered (taken or missed) and nothing should be shown.
     */
    fun openIntake(existing: IntakeEvent?, fresh: IntakeEvent): IntakeEvent? = when (existing?.status) {
        null -> fresh
        IntakeStatus.TAKEN, IntakeStatus.MISSED -> null
        IntakeStatus.PENDING, IntakeStatus.SNOOZED -> existing.copy(status = IntakeStatus.PENDING, snoozeCount = 0)
    }

    sealed interface RepeatDecision {
        /** Show the reminder again. [afterSnooze] is true when the person asked for it with "Recordar". */
        data class Remind(val event: IntakeEvent, val afterSnooze: Boolean) : RepeatDecision

        /** No answer after [maxRepeats] repeats: the intake stays "No confirmada" and nothing is subtracted. */
        data class Miss(val event: IntakeEvent) : RepeatDecision

        /** Already answered: do nothing. */
        data object Ignore : RepeatDecision
    }

    /**
     * The repeat alarm fired ("snoozeMinutes" after the last reminder).
     * Unanswered reminders repeat up to [maxRepeats] times; one interval after the last
     * repeat the intake becomes MISSED. A snooze starts a fresh cycle, because the person answered.
     */
    fun onRepeatDue(event: IntakeEvent, maxRepeats: Int): RepeatDecision = when (event.status) {
        IntakeStatus.TAKEN, IntakeStatus.MISSED -> RepeatDecision.Ignore
        IntakeStatus.SNOOZED -> RepeatDecision.Remind(
            event.copy(status = IntakeStatus.PENDING, snoozeCount = 0),
            afterSnooze = true,
        )
        IntakeStatus.PENDING -> if (event.snoozeCount < maxRepeats) {
            RepeatDecision.Remind(event.copy(snoozeCount = event.snoozeCount + 1), afterSnooze = false)
        } else {
            RepeatDecision.Miss(event.copy(status = IntakeStatus.MISSED))
        }
    }

    /** "Recordar en X min". Null when the intake was already answered. Never touches the stock. */
    fun snooze(event: IntakeEvent): IntakeEvent? = when (event.status) {
        IntakeStatus.TAKEN, IntakeStatus.MISSED -> null
        IntakeStatus.PENDING, IntakeStatus.SNOOZED -> event.copy(status = IntakeStatus.SNOOZED, snoozeCount = 0)
    }
}
