package ec.atiempo.app.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import ec.atiempo.app.domain.IntakeStatus
import ec.atiempo.app.domain.MovementType

@Entity(tableName = "medication")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dosePerIntake: Double,
    val stock: Double,
    val warnThreshold: Double,
    val urgentThreshold: Double,
    val note: String?,
    val active: Boolean,
    val restockPending: Boolean,
    val createdAt: Long,
    // Added in database version 2 (MIGRATION_1_2). The default must match the migration's SQL.
    @ColumnInfo(defaultValue = "azul") val colorKey: String,
)

@Entity(
    tableName = "schedule",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("medicationId")],
)
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val hour: Int,
    val minute: Int,
)

/** No foreign key to schedule: past intakes stay in the history when a time is removed. */
@Entity(
    tableName = "intake_event",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("medicationId"), Index("scheduledAt")],
)
data class IntakeEventEntity(
    @PrimaryKey val id: String,
    val medicationId: Long,
    val scheduleId: Long,
    val scheduledAt: Long,
    val status: IntakeStatus,
    val snoozeCount: Int,
    val takenAt: Long?,
    val quantity: Double,
)

@Entity(
    tableName = "stock_movement",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("medicationId")],
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val type: MovementType,
    val delta: Double,
    val resultingStock: Double,
    val createdAt: Long,
)

data class MedicationWithSchedulesEntity(
    @Embedded val medication: MedicationEntity,
    @Relation(parentColumn = "id", entityColumn = "medicationId")
    val schedules: List<ScheduleEntity>,
)
