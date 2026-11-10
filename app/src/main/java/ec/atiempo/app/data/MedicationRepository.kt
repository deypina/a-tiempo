package ec.atiempo.app.data

import androidx.room.withTransaction
import ec.atiempo.app.domain.InventoryRules
import ec.atiempo.app.domain.Medication
import ec.atiempo.app.domain.MedicationInput
import ec.atiempo.app.domain.MedicationWithSchedules
import ec.atiempo.app.domain.Schedule
import ec.atiempo.app.domain.StockAlert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock

/** The only entry point that changes medication data. UI and receivers go through here. */
class MedicationRepository(
    private val db: ATiempoDatabase,
    private val clock: Clock,
) {
    private val medicationDao = db.medicationDao()
    private val scheduleDao = db.scheduleDao()
    private val intakeEventDao = db.intakeEventDao()
    private val movementDao = db.stockMovementDao()

    fun observeMedications(): Flow<List<MedicationWithSchedules>> =
        medicationDao.observeAllWithSchedules().map { list -> list.map { it.toDomain() } }

    suspend fun getMedication(id: Long): MedicationWithSchedules? =
        medicationDao.getWithSchedules(id)?.toDomain()

    suspend fun allMedications(): List<Medication> = medicationDao.getAll().map { it.toDomain() }

    /** [alert] is set when a corrected count crossed a purchase threshold going down. */
    data class SaveResult(val id: Long, val alert: StockAlert?)

    /**
     * Creates or updates a medication. With [keepStock] the count in the database is kept,
     * so a purchase registered while the edit screen was open is not overwritten.
     */
    suspend fun saveMedication(id: Long?, input: MedicationInput, keepStock: Boolean = false): SaveResult = db.withTransaction {
        val now = clock.millis()
        val existing = id?.let { medicationDao.get(it) }?.toDomain()

        var alert: StockAlert? = null
        val medicationId = if (existing == null) {
            medicationDao.insert(
                Medication(
                    name = input.name,
                    dosePerIntake = input.dose,
                    stock = input.stock,
                    warnThreshold = input.warnThreshold,
                    urgentThreshold = input.urgentThreshold,
                    note = input.note,
                    createdAt = now,
                ).toEntity(),
            )
        } else {
            // A different count typed in the edit screen is recorded as a correction.
            val adjusted = if (keepStock) null else InventoryRules.adjustStock(existing, input.stock, now)
            adjusted?.let { movementDao.insert(it.movement.toEntity()) }
            alert = adjusted?.alert
            val base = adjusted?.medication ?: existing
            medicationDao.update(
                base.copy(
                    name = input.name,
                    dosePerIntake = input.dose,
                    warnThreshold = input.warnThreshold,
                    urgentThreshold = input.urgentThreshold,
                    note = input.note,
                ).toEntity(),
            )
            existing.id
        }

        replaceSchedules(medicationId, input)
        SaveResult(medicationId, alert)
    }

    /** "Ya compré": adds the quantity and turns purchase reminders off. Null if the medication is gone. */
    suspend fun registerPurchase(id: Long, quantity: Double): InventoryRules.PurchaseResult? = db.withTransaction {
        val medication = medicationDao.get(id)?.toDomain() ?: return@withTransaction null
        val result = InventoryRules.registerPurchase(medication, quantity, clock.millis())
        medicationDao.update(result.medication.toEntity())
        movementDao.insert(result.movement.toEntity())
        result
    }

    /** "Corregir cantidad". Null if the medication is gone or the count did not change. */
    suspend fun adjustStock(id: Long, newValue: Double): InventoryRules.AdjustResult? = db.withTransaction {
        val medication = medicationDao.get(id)?.toDomain() ?: return@withTransaction null
        val result = InventoryRules.adjustStock(medication, newValue, clock.millis()) ?: return@withTransaction null
        medicationDao.update(result.medication.toEntity())
        movementDao.insert(result.movement.toEntity())
        result
    }

    suspend fun scheduleIds(medicationId: Long): List<Long> =
        scheduleDao.forMedication(medicationId).map { it.id }

    suspend fun activeSchedules(): List<Schedule> = scheduleDao.activeSchedules().map { it.toDomain() }

    suspend fun setActive(id: Long, active: Boolean) = medicationDao.setActive(id, active)

    /** Deletes the medication with its schedules and history (docs/TEXTOS_UI.md med_delete_confirm). */
    suspend fun deleteMedication(id: Long) = db.withTransaction {
        movementDao.deleteForMedication(id)
        intakeEventDao.deleteForMedication(id)
        scheduleDao.deleteForMedication(id)
        medicationDao.delete(id)
    }

    /** Keeps unchanged times (same id, so today's intake keeps its id) and replaces the rest. */
    private suspend fun replaceSchedules(medicationId: Long, input: MedicationInput) {
        val current = scheduleDao.forMedication(medicationId)
        val wanted = input.times.toSet()
        val toDelete = current.filter { it.toDomain().time !in wanted }.map { it.id }
        val kept = current.map { it.toDomain().time }.toSet()
        val toInsert = input.times.filter { it !in kept }.map {
            ScheduleEntity(medicationId = medicationId, hour = it.hour, minute = it.minute)
        }
        if (toDelete.isNotEmpty()) scheduleDao.deleteByIds(toDelete)
        if (toInsert.isNotEmpty()) scheduleDao.insertAll(toInsert)
    }
}
