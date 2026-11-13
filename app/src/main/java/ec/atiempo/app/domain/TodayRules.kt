package ec.atiempo.app.domain

import java.time.LocalDate
import java.time.ZoneId

/** One scheduled intake of today, as shown in the "Hoy" screen (docs/PRD.md F4). */
data class TodayIntake(
    val medication: Medication,
    val scheduleCount: Int,
    val schedule: Schedule,
    /** Day of the list this intake belongs to. */
    val date: LocalDate,
    val eventId: String,
    val status: IntakeStatus,
    val takenAt: Long?,
    /** When a snoozed intake reminds again. Null when unknown or not snoozed. */
    val snoozedUntil: Long?,
) {
    val canTake: Boolean get() = status != IntakeStatus.TAKEN
    val canUndo: Boolean get() = status == IntakeStatus.TAKEN
}

/** How much of a medication is left, for the color of its stock line. Never shown by color alone. */
enum class StockLevel { OK, LOW, OUT }

/** A medication shown in the "IMPORTANTE" block of Hoy because it ran out or is running low. */
data class StockNotice(val medication: Medication, val level: StockLevel)

/**
 * Hoy in display order: the next intake first and large, then the rest in time order.
 * [nextIsLate] shows "ATRASADA" instead of "AHORA"; [canSnoozeNext] shows "Recordar en X minutos".
 */
data class TodayPlan(
    val next: TodayIntake?,
    val nextIsLate: Boolean,
    val canSnoozeNext: Boolean,
    val rest: List<TodayIntake>,
)

/** Builds the "Hoy" list. Every function is pure. */
object TodayRules {

    private val TIME_ORDER = compareBy<TodayIntake>({ it.schedule.time }, { it.medication.name.lowercase() })

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
                        date = date,
                        eventId = id,
                        status = status,
                        takenAt = event?.takenAt,
                        snoozedUntil = if (status == IntakeStatus.SNOOZED) snoozedUntil(schedule.id) else null,
                    )
                }
            }
            .sortedWith(TIME_ORDER)
    }

    /**
     * The intake shown first: the earliest one of the day not taken yet (pending, snoozed or
     * missed). Null when every intake is taken or there are none.
     */
    fun nextIntake(intakes: List<TodayIntake>): TodayIntake? =
        intakes.sortedWith(TIME_ORDER).firstOrNull { it.status != IntakeStatus.TAKEN }

    /** "ATRASADA": its scheduled time already passed. Otherwise it is "AHORA". */
    fun isLate(intake: TodayIntake, now: Long, zone: ZoneId): Boolean =
        now > scheduledAt(intake.date, intake.schedule.time, zone)

    /**
     * "Recordar en X minutos" in Hoy: only once the intake's time passed, and only while it still
     * waits for an answer, the same statuses ReminderRules.canSnooze accepts (never when missed).
     */
    fun canSnoozeFromToday(intake: TodayIntake, now: Long, zone: ZoneId): Boolean =
        (intake.status == IntakeStatus.PENDING || intake.status == IntakeStatus.SNOOZED) &&
            isLate(intake, now, zone)

    /** Splits the list for display: the next intake first, the rest in time order. */
    fun plan(intakes: List<TodayIntake>, now: Long, zone: ZoneId): TodayPlan {
        val ordered = intakes.sortedWith(TIME_ORDER)
        val next = nextIntake(ordered)
        return TodayPlan(
            next = next,
            nextIsLate = next != null && isLate(next, now, zone),
            canSnoozeNext = next != null && canSnoozeFromToday(next, now, zone),
            rest = ordered.filter { it.eventId != next?.eventId },
        )
    }

    /** Out when nothing is left, low at or under the warning threshold. */
    fun stockLevel(medication: Medication): StockLevel = when {
        medication.stock <= 0.0 -> StockLevel.OUT
        medication.stock <= medication.warnThreshold -> StockLevel.LOW
        else -> StockLevel.OK
    }

    /** Active medications that ran out (first) or run low, for the "IMPORTANTE" block. */
    fun stockNotices(medications: List<MedicationWithSchedules>): List<StockNotice> =
        medications
            .map { it.medication }
            .filter { it.active }
            .map { StockNotice(it, stockLevel(it)) }
            .filter { it.level != StockLevel.OK }
            .sortedWith(compareBy<StockNotice>({ it.level != StockLevel.OUT }, { it.medication.name.lowercase() }))

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
