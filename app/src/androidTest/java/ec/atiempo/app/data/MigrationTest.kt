package ec.atiempo.app.data

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Updating the app must never lose the person's data (docs/ESPECIFICACION_TECNICA.md,
 * "Versiones de la base de datos y migraciones"). Builds a real version 1 database from
 * app/schemas/.../1.json, then opens it with the current [ATiempoDatabase], which runs
 * [MIGRATION_1_2] and makes Room validate the result against the version 2 entities.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ATiempoDatabase::class.java,
    )

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private var migrated: ATiempoDatabase? = null

    @After
    fun tearDown() {
        migrated?.close()
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun migrate1To2_keepsAllDataAndGivesDistinctColorsInIdOrder() {
        // Ids out of insertion order and with gaps: colors follow id order, not insertion order.
        val medications = listOf(
            OldMedication(id = 9, name = "Losartán", stock = 12.5, note = null),
            OldMedication(id = 2, name = "Gabapentina", stock = 30.0, note = "Con comida"),
            OldMedication(id = 5, name = "Metformina", stock = 0.0, note = null),
            OldMedication(id = 3, name = "Atorvastatina", stock = 7.5, note = null),
            OldMedication(id = 11, name = "Omeprazol", stock = 14.0, note = null),
            OldMedication(id = 8, name = "Vitamina D", stock = 60.0, note = null),
            OldMedication(id = 10, name = "Aspirina", stock = 1.5, note = null),
        )

        helper.createDatabase(DB_NAME, 1).apply {
            medications.forEach { insertOldMedication(it) }
            execSQL("INSERT INTO `schedule` (`id`, `medicationId`, `hour`, `minute`) VALUES (1, 2, 8, 0)")
            execSQL("INSERT INTO `schedule` (`id`, `medicationId`, `hour`, `minute`) VALUES (2, 2, 20, 30)")
            execSQL(
                "INSERT INTO `intake_event` (`id`, `medicationId`, `scheduleId`, `scheduledAt`, `status`, " +
                    "`snoozeCount`, `takenAt`, `quantity`) VALUES ('2-1-20261111', 2, 1, 1000, 'TAKEN', 0, 1100, 1.0)",
            )
            execSQL(
                "INSERT INTO `stock_movement` (`id`, `medicationId`, `type`, `delta`, `resultingStock`, `createdAt`) " +
                    "VALUES (1, 2, 'INTAKE', -1.0, 30.0, 1100)",
            )
            close()
        }

        val db = Room.databaseBuilder(context, ATiempoDatabase::class.java, DB_NAME)
            .addMigrations(MIGRATION_1_2)
            .build()
            .also { migrated = it }
        // Opening runs the migration and Room's schema validation; it throws if either fails.
        val sql = db.openHelper.writableDatabase

        val rows = sql.queryRows(
            "SELECT `id`, `name`, `stock`, `note`, `colorKey` FROM `medication` ORDER BY `id`",
        ) { listOf(getLong(0), getString(1), getDouble(2), if (isNull(3)) null else getString(3), getString(4)) }

        val expected = medications.sortedBy { it.id }
            .zip(listOf("azul", "rosa", "mostaza", "oliva", "ciruela", "blanco", "azul"))
            .map { (m, color) -> listOf(m.id, m.name, m.stock, m.note, color) }
        assertEquals(expected, rows)

        assertEquals(
            listOf(listOf(1L, 2L, 8L, 0L), listOf(2L, 2L, 20L, 30L)),
            sql.queryRows("SELECT `id`, `medicationId`, `hour`, `minute` FROM `schedule` ORDER BY `id`") {
                listOf(getLong(0), getLong(1), getLong(2), getLong(3))
            },
        )
        assertEquals(
            listOf(listOf("2-1-20261111", "TAKEN", 1100L, 1.0)),
            sql.queryRows("SELECT `id`, `status`, `takenAt`, `quantity` FROM `intake_event`") {
                listOf(getString(0), getString(1), getLong(2), getDouble(3))
            },
        )
        assertEquals(
            listOf(listOf("INTAKE", -1.0, 30.0)),
            sql.queryRows("SELECT `type`, `delta`, `resultingStock` FROM `stock_movement`") {
                listOf(getString(0), getDouble(1), getDouble(2))
            },
        )

        // The migrated rows read back through the app's own DAO and mapper.
        val gabapentina = runBlocking { db.medicationDao().getWithSchedules(2) }!!.toDomain()
        assertEquals("rosa", gabapentina.medication.colorKey)
        assertEquals(30.0, gabapentina.medication.stock, 0.0)
        assertEquals(2, gabapentina.schedules.size)
    }

    @Test
    fun migrate1To2_emptyDatabase_opens() {
        helper.createDatabase(DB_NAME, 1).close()

        val db = Room.databaseBuilder(context, ATiempoDatabase::class.java, DB_NAME)
            .addMigrations(MIGRATION_1_2)
            .build()
            .also { migrated = it }

        assertEquals(
            listOf(0L),
            db.openHelper.writableDatabase.queryRows("SELECT COUNT(*) FROM `medication`") { getLong(0) },
        )
    }

    private data class OldMedication(val id: Long, val name: String, val stock: Double, val note: String?)

    /** Raw insert with exactly the version 1 columns (app/schemas/.../1.json). */
    private fun SupportSQLiteDatabase.insertOldMedication(m: OldMedication) {
        execSQL(
            "INSERT INTO `medication` (`id`, `name`, `dosePerIntake`, `stock`, `warnThreshold`, " +
                "`urgentThreshold`, `note`, `active`, `restockPending`, `createdAt`) " +
                "VALUES (?, ?, 1.0, ?, 5.0, 3.0, ?, 1, 0, 500)",
            arrayOf<Any?>(m.id, m.name, m.stock, m.note),
        )
    }

    private fun <T> SupportSQLiteDatabase.queryRows(sql: String, read: android.database.Cursor.() -> T): List<T> =
        query(sql).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.read()) }
        }

    private companion object {
        /** Not the app's real name, so the test never touches a real database. */
        const val DB_NAME = "migration-test.db"
    }
}
