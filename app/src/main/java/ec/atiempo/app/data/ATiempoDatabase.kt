package ec.atiempo.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Schema changes must ship a Migration: dropping the database would erase the user's history.
 * Exported schemas live in app/schemas/ and are committed.
 */
@Database(
    entities = [
        MedicationEntity::class,
        ScheduleEntity::class,
        IntakeEventEntity::class,
        StockMovementEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ATiempoDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun intakeEventDao(): IntakeEventDao
    abstract fun stockMovementDao(): StockMovementDao

    companion object {
        private const val NAME = "a_tiempo.db"

        fun build(context: Context): ATiempoDatabase =
            Room.databaseBuilder(context, ATiempoDatabase::class.java, NAME).build()
    }
}
