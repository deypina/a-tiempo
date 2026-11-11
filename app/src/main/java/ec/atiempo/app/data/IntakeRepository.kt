package ec.atiempo.app.data

import androidx.room.withTransaction
import ec.atiempo.app.domain.IntakeEvent
import ec.atiempo.app.domain.InventoryRules
import ec.atiempo.app.domain.Medication
import ec.atiempo.app.domain.ReminderRules
import ec.atiempo.app.domain.ReminderRules.RepeatDecision
import ec.atiempo.app.domain.Schedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant

/**
 * Intake events for reminders. Every read-modify-write runs in one transaction, so a
 * "Ya la tomé" tap and a repeat alarm arriving together can never undo each other.
 */
class IntakeRepository(
    private val db: ATiempoDatabase,
    private val clock: Clock,
) {
    private val medicationDao = db.medicationDao()
    private val scheduleDao = db.scheduleDao()
    private val intakeEventDao = db.intakeEventDao()
    private val movementDao = db.stockMovementDao()

    data class ReminderTarget(val medication: Medication, val schedule: Schedule)

    /** Medication and schedule behind an alarm, only if both still exist and it is not paused. */
    suspend fun reminderTarget(medicationId: Long, scheduleId: Long): ReminderTarget? {
        val medication = medicationDao.get(medicationId)?.toDomain() ?: return null
        if (!medication.active) return null
        val schedule = scheduleDao.get(scheduleId)?.toDomain() ?: return null
        if (schedule.medicationId != medicationId) return null
        return ReminderTarget(medication, schedule)
    }

    suspend fun getEvent(id: String): IntakeEvent? = intakeEventDao.get(id)?.toDomain()

    suspend fun unansweredSince(since: Long): List<IntakeEvent> =
        intakeEventDao.unansweredSince(since).map { it.toDomain() }

    /** Intakes scheduled in [range] (one day), kept up to date. */
    fun observeBetween(range: LongRange): Flow<List<IntakeEvent>> =
        intakeEventDao.observeBetween(range.first, range.last + 1).map { list -> list.map { it.toDomain() } }

    fun observeMissed(medicationId: Long): Flow<List<IntakeEvent>> =
        intakeEventDao.observeMissed(medicationId).map { list -> list.map { it.toDomain() } }

    /**
     * Id of the intake of [target] at [scheduledAt], creating it as pending when its alarm has
     * not fired yet ("Ya la tomé" from Hoy before the reminder). An existing event is left as it is.
     */
    suspend fun ensureEvent(target: ReminderTarget, scheduledAt: Long): String = db.withTransaction {
        val date = Instant.ofEpochMilli(scheduledAt).atZone(clock.zone).toLocalDate()
        val id = InventoryRules.intakeEventId(target.medication.id, target.schedule.id, date)
        if (intakeEventDao.get(id) == null) {
            val fresh = IntakeEvent(
                id = id,
                medicationId = target.medication.id,
                scheduleId = target.schedule.id,
                scheduledAt = scheduledAt,
            )
            intakeEventDao.upsert(fresh.toEntity())
        }
        id
    }

    /** "Deshacer": gives the amount back once. Null when the event is unknown or not taken. */
    suspend fun undoIntake(eventId: String): InventoryRules.UndoResult? = db.withTransaction {
        val event = intakeEventDao.get(eventId)?.toDomain() ?: return@withTransaction null
        val medication = medicationDao.get(event.medicationId)?.toDomain() ?: return@withTransaction null
        val result = InventoryRules.undoIntake(medication, event, clock.millis()) ?: return@withTransaction null
        medicationDao.update(result.medication.toEntity())
        intakeEventDao.upsert(result.event.toEntity())
        movementDao.insert(result.movement.toEntity())
        result
    }

    /** Creates or reopens the event of the day. Null when it was already answered. */
    suspend fun openIntake(target: ReminderTarget, scheduledAt: Long): IntakeEvent? = db.withTransaction {
        val date = Instant.ofEpochMilli(scheduledAt).atZone(clock.zone).toLocalDate()
        val id = InventoryRules.intakeEventId(target.medication.id, target.schedule.id, date)
        val fresh = IntakeEvent(
            id = id,
            medicationId = target.medication.id,
            scheduleId = target.schedule.id,
            scheduledAt = scheduledAt,
        )
        val event = ReminderRules.openIntake(intakeEventDao.get(id)?.toDomain(), fresh)
        event?.let { intakeEventDao.upsert(it.toEntity()) }
        event
    }

    suspend fun applyRepeat(eventId: String, maxRepeats: Int): RepeatDecision = db.withTransaction {
        val event = intakeEventDao.get(eventId)?.toDomain() ?: return@withTransaction RepeatDecision.Ignore
        val decision = ReminderRules.onRepeatDue(event, maxRepeats)
        when (decision) {
            is RepeatDecision.Remind -> intakeEventDao.upsert(decision.event.toEntity())
            is RepeatDecision.Miss -> intakeEventDao.upsert(decision.event.toEntity())
            RepeatDecision.Ignore -> Unit
        }
        decision
    }

    suspend fun snooze(eventId: String): IntakeEvent? = db.withTransaction {
        val event = intakeEventDao.get(eventId)?.toDomain() ?: return@withTransaction null
        val snoozed = ReminderRules.snooze(event)
        snoozed?.let { intakeEventDao.upsert(it.toEntity()) }
        snoozed
    }

    /** Subtracts the dose once. Null when the event is unknown or was already taken. */
    suspend fun confirmIntake(eventId: String): InventoryRules.IntakeResult? = db.withTransaction {
        val event = intakeEventDao.get(eventId)?.toDomain() ?: return@withTransaction null
        val medication = medicationDao.get(event.medicationId)?.toDomain() ?: return@withTransaction null
        val result = InventoryRules.confirmIntake(medication, event, clock.millis()) ?: return@withTransaction null
        medicationDao.update(result.medication.toEntity())
        intakeEventDao.upsert(result.event.toEntity())
        movementDao.insert(result.movement.toEntity())
        result
    }
}
