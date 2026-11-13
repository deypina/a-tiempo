package ec.atiempo.app.domain

/**
 * Colors a person can give a medication to recognize it (docs/ESPECIFICACION_TECNICA.md
 * "Primer color libre"). Only the keys live here; the UI maps them to actual colors.
 */
object MedicationColors {
    /** Palette order. The database migration v1→v2 assigns colors in this same order. */
    val KEYS: List<String> = listOf("azul", "rosa", "mostaza", "oliva", "ciruela", "blanco")

    val DEFAULT: String = KEYS.first()

    /** First palette color no other medication uses; the first one when all are taken. */
    fun firstFreeColor(used: Collection<String>): String =
        KEYS.firstOrNull { it !in used } ?: DEFAULT

    /** A known key as is; anything else (null, empty, unknown) falls back to the first color. */
    fun normalize(key: String?): String =
        if (key != null && key in KEYS) key else DEFAULT
}
