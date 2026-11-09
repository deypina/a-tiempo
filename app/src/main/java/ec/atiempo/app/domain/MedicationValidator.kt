package ec.atiempo.app.domain

/** What the edit screen collected. Null numbers mean the text could not be parsed. */
data class MedicationForm(
    val name: String,
    val dose: Double?,
    val times: List<DoseTime>,
    val stock: Double?,
    val warnThreshold: Double?,
    val urgentThreshold: Double?,
    val note: String,
)

/** A form that passed validation, ready to be saved. */
data class MedicationInput(
    val name: String,
    val dose: Double,
    val times: List<DoseTime>,
    val stock: Double,
    val warnThreshold: Double,
    val urgentThreshold: Double,
    val note: String?,
)

enum class MedicationError {
    NAME_MISSING,
    DOSE_INVALID,
    TIME_MISSING,
    STOCK_INVALID,
    WARN_INVALID,
    URGENT_INVALID,
    THRESHOLDS_INVALID,
}

sealed interface ValidationResult {
    data class Valid(val input: MedicationInput) : ValidationResult
    data class Invalid(val errors: Set<MedicationError>) : ValidationResult
}

object MedicationValidator {
    const val MAX_NAME_LENGTH = 60
    const val MAX_NOTE_LENGTH = 120

    fun validate(form: MedicationForm): ValidationResult {
        val errors = mutableSetOf<MedicationError>()
        val name = form.name.trim()

        if (name.isEmpty()) errors += MedicationError.NAME_MISSING
        if (form.dose == null || form.dose <= 0) errors += MedicationError.DOSE_INVALID
        if (form.times.isEmpty()) errors += MedicationError.TIME_MISSING
        if (form.stock == null) errors += MedicationError.STOCK_INVALID
        if (form.warnThreshold == null) errors += MedicationError.WARN_INVALID
        if (form.urgentThreshold == null) errors += MedicationError.URGENT_INVALID
        if (form.warnThreshold != null && form.urgentThreshold != null &&
            form.urgentThreshold >= form.warnThreshold
        ) {
            errors += MedicationError.THRESHOLDS_INVALID
        }

        if (errors.isNotEmpty()) return ValidationResult.Invalid(errors)

        return ValidationResult.Valid(
            MedicationInput(
                name = name.take(MAX_NAME_LENGTH),
                dose = form.dose!!,
                times = form.times.distinct().sorted(),
                stock = form.stock!!,
                warnThreshold = form.warnThreshold!!,
                urgentThreshold = form.urgentThreshold!!,
                note = form.note.trim().take(MAX_NOTE_LENGTH).ifEmpty { null },
            ),
        )
    }
}
