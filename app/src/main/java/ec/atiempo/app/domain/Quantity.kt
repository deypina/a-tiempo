package ec.atiempo.app.domain

import kotlin.math.floor
import kotlin.math.roundToLong

/** Pill quantities move in steps of 0.5 and are shown as "½", "1", "1½"... never "0.5". */
object Quantity {
    const val MAX = 9999.0
    private const val HALF = "½"

    fun isHalfStep(value: Double): Boolean = value * 2 == floor(value * 2)

    fun format(value: Double): String {
        val halves = (value * 2).roundToLong()
        val whole = halves / 2
        val hasHalf = halves % 2 != 0L
        return when {
            !hasHalf -> whole.toString()
            whole == 0L -> HALF
            else -> "$whole$HALF"
        }
    }

    /**
     * Parses what a person types: "2", "1.5", "1,5", "½" or "1½".
     * Returns null for empty text, negatives, values above [MAX] or steps other than 0.5.
     */
    fun parse(text: String): Double? {
        val cleaned = text.trim().replace(',', '.').replace(" ", "")
        if (cleaned.isEmpty()) return null
        val value = if (cleaned.endsWith(HALF)) {
            val wholePart = cleaned.removeSuffix(HALF)
            if (wholePart.isEmpty()) 0.5 else (wholePart.toIntOrNull() ?: return null) + 0.5
        } else {
            cleaned.toDoubleOrNull() ?: return null
        }
        if (value.isNaN() || value < 0 || value > MAX || !isHalfStep(value)) return null
        return value
    }

    /** Spanish uses singular for ½ and 1: "Le queda ½ pastilla", "Le queda 1 pastilla". */
    fun isSingular(value: Double): Boolean = value > 0 && value <= 1.0
}
