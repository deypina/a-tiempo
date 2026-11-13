package ec.atiempo.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ec.atiempo.app.domain.MedicationColors

/**
 * v1 → v2: adds `colorKey` to `medication`. Additive only: no row, table or column is dropped,
 * so medications, schedules, intakes and inventory survive the update untouched.
 * Existing medications get distinct colors in `id` order, cycling through the palette.
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        MIGRATION_1_2_SQL.forEach { db.execSQL(it) }
    }
}

/** Statements of [MIGRATION_1_2], in order. Kept apart so the exact SQL is easy to review. */
internal val MIGRATION_1_2_SQL: List<String> = listOf(
    "ALTER TABLE `medication` ADD COLUMN `colorKey` TEXT NOT NULL DEFAULT '${MedicationColors.DEFAULT}'",
    // Position of each row in id order (0, 1, 2…) is the number of rows with a smaller id.
    "UPDATE `medication` SET `colorKey` = CASE " +
        "((SELECT COUNT(*) FROM `medication` AS m2 WHERE m2.`id` < `medication`.`id`) % ${MedicationColors.KEYS.size}) " +
        MedicationColors.KEYS.mapIndexed { index, key -> "WHEN $index THEN '$key' " }.joinToString("") +
        "ELSE '${MedicationColors.DEFAULT}' END",
)
