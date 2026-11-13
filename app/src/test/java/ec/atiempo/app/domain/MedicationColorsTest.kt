package ec.atiempo.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MedicationColorsTest {

    @Test
    fun paletteOrder_isTheOneInTheSpec() {
        assertEquals(listOf("azul", "rosa", "mostaza", "oliva", "ciruela", "blanco"), MedicationColors.KEYS)
    }

    @Test
    fun firstFreeColor_nothingUsed_isAzul() {
        assertEquals("azul", MedicationColors.firstFreeColor(emptyList()))
    }

    @Test
    fun firstFreeColor_someUsed_isTheFirstFreeInPaletteOrder() {
        assertEquals("rosa", MedicationColors.firstFreeColor(listOf("azul")))
        assertEquals("mostaza", MedicationColors.firstFreeColor(listOf("rosa", "azul", "oliva")))
        assertEquals("azul", MedicationColors.firstFreeColor(listOf("rosa", "mostaza")))
    }

    @Test
    fun firstFreeColor_repeatedAndUnknownKeysDoNotCount() {
        assertEquals("mostaza", MedicationColors.firstFreeColor(listOf("azul", "azul", "rosa", "verde")))
    }

    @Test
    fun firstFreeColor_allSixUsed_isAzul() {
        val all = listOf("blanco", "ciruela", "oliva", "mostaza", "rosa", "azul")
        assertEquals("azul", MedicationColors.firstFreeColor(all))
    }

    @Test
    fun normalize_keepsKnownKeys() {
        MedicationColors.KEYS.forEach { assertEquals(it, MedicationColors.normalize(it)) }
    }

    @Test
    fun normalize_unknownEmptyOrNull_fallsBackToAzul() {
        assertEquals("azul", MedicationColors.normalize("verde"))
        assertEquals("azul", MedicationColors.normalize(""))
        assertEquals("azul", MedicationColors.normalize(null))
        assertEquals("azul", MedicationColors.normalize("Rosa"))
    }
}
