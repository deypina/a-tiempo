package ec.atiempo.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Transaction
    @Query("SELECT * FROM medication ORDER BY name COLLATE NOCASE")
    fun observeAllWithSchedules(): Flow<List<MedicationWithSchedulesEntity>>

    @Transaction
    @Query("SELECT * FROM medication WHERE id = :id")
    suspend fun getWithSchedules(id: Long): MedicationWithSchedulesEntity?

    @Query("SELECT * FROM medication WHERE id = :id")
    suspend fun get(id: Long): MedicationEntity?

    @Insert
    suspend fun insert(medication: MedicationEntity): Long

    @Update
    suspend fun update(medication: MedicationEntity)

    @Query("UPDATE medication SET active = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    @Query("DELETE FROM medication WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule WHERE medicationId = :medicationId")
    suspend fun forMedication(medicationId: Long): List<ScheduleEntity>

    @Insert
    suspend fun insertAll(schedules: List<ScheduleEntity>)

    @Query("DELETE FROM schedule WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM schedule WHERE medicationId = :medicationId")
    suspend fun deleteForMedication(medicationId: Long)
}

@Dao
interface IntakeEventDao {
    @Query("SELECT * FROM intake_event WHERE id = :id")
    suspend fun get(id: String): IntakeEventEntity?

    @Upsert
    suspend fun upsert(event: IntakeEventEntity)

    @Query("DELETE FROM intake_event WHERE medicationId = :medicationId")
    suspend fun deleteForMedication(medicationId: Long)
}

@Dao
interface StockMovementDao {
    @Insert
    suspend fun insert(movement: StockMovementEntity)

    @Query("SELECT * FROM stock_movement WHERE medicationId = :medicationId ORDER BY createdAt DESC, id DESC")
    fun observeForMedication(medicationId: Long): Flow<List<StockMovementEntity>>

    @Query("DELETE FROM stock_movement WHERE medicationId = :medicationId")
    suspend fun deleteForMedication(medicationId: Long)
}
