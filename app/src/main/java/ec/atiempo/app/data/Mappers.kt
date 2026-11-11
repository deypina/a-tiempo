package ec.atiempo.app.data

import ec.atiempo.app.domain.DoseTime
import ec.atiempo.app.domain.IntakeEvent
import ec.atiempo.app.domain.Medication
import ec.atiempo.app.domain.MedicationWithSchedules
import ec.atiempo.app.domain.Schedule
import ec.atiempo.app.domain.StockMovement

fun MedicationEntity.toDomain() = Medication(
    id = id,
    name = name,
    dosePerIntake = dosePerIntake,
    stock = stock,
    warnThreshold = warnThreshold,
    urgentThreshold = urgentThreshold,
    note = note,
    active = active,
    restockPending = restockPending,
    createdAt = createdAt,
)

fun Medication.toEntity() = MedicationEntity(
    id = id,
    name = name,
    dosePerIntake = dosePerIntake,
    stock = stock,
    warnThreshold = warnThreshold,
    urgentThreshold = urgentThreshold,
    note = note,
    active = active,
    restockPending = restockPending,
    createdAt = createdAt,
)

fun ScheduleEntity.toDomain() = Schedule(id = id, medicationId = medicationId, time = DoseTime(hour, minute))

fun MedicationWithSchedulesEntity.toDomain() = MedicationWithSchedules(
    medication = medication.toDomain(),
    schedules = schedules.map { it.toDomain() }.sortedBy { it.time },
)

fun StockMovement.toEntity() = StockMovementEntity(
    id = id,
    medicationId = medicationId,
    type = type,
    delta = delta,
    resultingStock = resultingStock,
    createdAt = createdAt,
)

fun StockMovementEntity.toDomain() = StockMovement(
    id = id,
    medicationId = medicationId,
    type = type,
    delta = delta,
    resultingStock = resultingStock,
    createdAt = createdAt,
)

fun IntakeEventEntity.toDomain() = IntakeEvent(
    id = id,
    medicationId = medicationId,
    scheduleId = scheduleId,
    scheduledAt = scheduledAt,
    status = status,
    snoozeCount = snoozeCount,
    takenAt = takenAt,
    quantity = quantity,
)

fun IntakeEvent.toEntity() = IntakeEventEntity(
    id = id,
    medicationId = medicationId,
    scheduleId = scheduleId,
    scheduledAt = scheduledAt,
    status = status,
    snoozeCount = snoozeCount,
    takenAt = takenAt,
    quantity = quantity,
)
