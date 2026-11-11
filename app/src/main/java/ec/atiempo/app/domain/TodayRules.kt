package ec.atiempo.app.domain

import java.time.LocalDate
import java.time.ZoneId

/** One scheduled intake of today, as shown in the "Hoy" screen (docs/PRD.md F4). */
data class TodayIntake(
    val medication: Medication,
    val scheduleCount: Int,
    val schedule: Schedule,
    val eventId: String,
    val status: IntakeStatus,
    val takenAt: Long?,
    /** When a snoozed intake reminds again. Null when unknown or not snoozed. */
    val snoozedUntil: Long?,
) {
    val canTake: Boolean get() = status != IntakeStatus.TAKEN
    val canUndo: Boolean get() = status == IntakeStatus.TAKEN
}

/** Builds the "Hoy" list. Every function is pure. */
object TodayRules {

    /**
     * Today's intakes of active medications in time order. An intake without an event yet
     * (its alarm has not fired) is pending. [snoozedUntil] gives the repeat time of a schedule.
     */
    fun build(
        medications: List<MedicationWithSchedules>,
        events: List<IntakeEvent>,
        date: LocalDate,
        snoozedUntil: (scheduleId: Long) -> Long?,
    ): List<TodayIntake> {
        val byId = events.associateBy { it.id }
        return medications
            .filter { it.medication.active }
            .flatMap { item ->
                item.schedules.map { schedule ->
                    val id = InventoryRules.intakeEventId(item.medication.id, schedule.id, date)
                    val event = byId[id]
                    val status = event?.status ?: IntakeStatus.PENDING
                    TodayIntake(
                        medication = item.medication,
                        scheduleCount = item.schedules.size,
                        schedule = schedule,
                        eventId = id,
                        status = status,
                        takenAt = event?.takenAt,
                        snoozedUntil = if (status == IntakeStatus.SNOOZED) snoozedUntil(schedule.id) else null,
                    )
                }
            }
            .sortedWith(compareBy<TodayIntake>({ it.schedule.time }, { it.medication.name.lowercase() }))
    }

    /** Epoch millis of [time] on [date], the same moment the daily alarm uses as scheduledAt. */
    fun scheduledAt(date: LocalDate, time: DoseTime, zone: ZoneId): Long =
        date.atTime(time.hour, time.minute).atZone(zone).toInstant().toEpochMilli()

    /** Start (inclusive) and end (exclusive) of [date] in epoch millis. */
    fun dayRange(date: LocalDate, zone: ZoneId): LongRange {
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start until end
    }
}
