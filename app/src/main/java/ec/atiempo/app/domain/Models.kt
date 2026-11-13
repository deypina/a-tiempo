package ec.atiempo.app.domain

/** Pure domain models. No Android types here, so all rules can be unit tested. */

data class Medication(
    val id: Long = 0,
    val name: String,
    val dosePerIntake: Double,
    val stock: Double,
    val warnThreshold: Double = DEFAULT_WARN_THRESHOLD,
    val urgentThreshold: Double = DEFAULT_URGENT_THRESHOLD,
    val note: String? = null,
    val active: Boolean = true,
    val restockPending: Boolean = false,
    val createdAt: Long,
    /** Color key from [MedicationColors.KEYS], to recognize the medication at a glance. */
    val colorKey: String,
) {
    companion object {
        const val DEFAULT_WARN_THRESHOLD = 5.0
        const val DEFAULT_URGENT_THRESHOLD = 3.0
    }
}

data class DoseTime(val hour: Int, val minute: Int) : Comparable<DoseTime> {
    init {
        require(hour in 0..23) { "hour out of range: $hour" }
        require(minute in 0..59) { "minute out of range: $minute" }
    }

    override fun compareTo(other: DoseTime): Int =
        compareValuesBy(this, other, DoseTime::hour, DoseTime::minute)
}

data class Schedule(
    val id: Long = 0,
    val medicationId: Long,
    val time: DoseTime,
)

data class MedicationWithSchedules(
    val medication: Medication,
    val schedules: List<Schedule>,
)

enum class IntakeStatus { PENDING, TAKEN, SNOOZED, MISSED }

data class IntakeEvent(
    val id: String,
    val medicationId: Long,
    val scheduleId: Long,
    val scheduledAt: Long,
    val status: IntakeStatus = IntakeStatus.PENDING,
    /** Unanswered repeats sent since the last reminder (see ReminderRules.onRepeatDue). */
    val snoozeCount: Int = 0,
    val takenAt: Long? = null,
    /** Amount actually subtracted when taken, so it can be given back on undo. */
    val quantity: Double = 0.0,
)

enum class MovementType { INTAKE, PURCHASE, ADJUSTMENT, UNDO }

data class StockMovement(
    val id: Long = 0,
    val medicationId: Long,
    val type: MovementType,
    val delta: Double,
    val resultingStock: Double,
    val createdAt: Long,
)

/** Purchase reminder triggered when stock crosses a threshold going down. */
enum class StockAlert { LOW, URGENT, OUT }

/** "Tamaño de letra" in Ajustes. Multiplies the phone's own font size, so both add up. */
enum class FontSize(val scale: Float) {
    NORMAL(1.0f),
    LARGE(1.15f),
    XLARGE(1.3f),
}
