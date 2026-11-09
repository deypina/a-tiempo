package ec.atiempo.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicationValidatorTest {

    private val validForm = MedicationForm(
        name = "  Gabapentina ",
        dose = 1.0,
        times = listOf(DoseTime(21, 0), DoseTime(8, 0), DoseTime(21, 0)),
        stock = 8.0,
        warnThreshold = 5.0,
        urgentThreshold = 3.0,
        note = "  ",
    )

    private fun errorsOf(form: MedicationForm): Set<MedicationError> =
        (MedicationValidator.validate(form) as ValidationResult.Invalid).errors

    @Test
    fun validForm_isTrimmedSortedAndDeduplicated() {
        val input = (MedicationValidator.validate(validForm) as ValidationResult.Valid).input

        assertEquals("Gabapentina", input.name)
        assertEquals(listOf(DoseTime(8, 0), DoseTime(21, 0)), input.times)
        assertEquals(null, input.note)
    }

    @Test
    fun missingName_isRejected() {
        assertEquals(setOf(MedicationError.NAME_MISSING), errorsOf(validForm.copy(name = "   ")))
    }

    @Test
    fun missingTime_isRejected() {
        assertEquals(setOf(MedicationError.TIME_MISSING), errorsOf(validForm.copy(times = emptyList())))
    }

    @Test
    fun missingNameAndTime_reportsBoth() {
        assertEquals(
            setOf(MedicationError.NAME_MISSING, MedicationError.TIME_MISSING),
            errorsOf(validForm.copy(name = "", times = emptyList())),
        )
    }

    @Test
    fun urgentGreaterOrEqualThanWarn_isRejected() {
        assertEquals(setOf(MedicationError.THRESHOLDS_INVALID), errorsOf(validForm.copy(urgentThreshold = 6.0)))
        assertEquals(setOf(MedicationError.THRESHOLDS_INVALID), errorsOf(validForm.copy(urgentThreshold = 5.0)))
    }

    @Test
    fun invalidNumbers_areRejected() {
        val errors = errorsOf(validForm.copy(dose = 0.0, stock = null, warnThreshold = null, urgentThreshold = null))

        assertTrue(MedicationError.DOSE_INVALID in errors)
        assertTrue(MedicationError.STOCK_INVALID in errors)
        assertTrue(MedicationError.WARN_INVALID in errors)
        assertTrue(MedicationError.URGENT_INVALID in errors)
    }
}
