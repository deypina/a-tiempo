package ec.atiempo.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuantityTest {

    @Test
    fun format_showsHalvesWithSymbolNeverDecimal() {
        assertEquals("½", Quantity.format(0.5))
        assertEquals("1", Quantity.format(1.0))
        assertEquals("1½", Quantity.format(1.5))
        assertEquals("2", Quantity.format(2.0))
        assertEquals("12½", Quantity.format(12.5))
        assertEquals("0", Quantity.format(0.0))
    }

    @Test
    fun parse_acceptsWholeHalfSymbolAndBothDecimalSeparators() {
        assertEquals(2.0, Quantity.parse("2")!!, 0.0)
        assertEquals(0.5, Quantity.parse("½")!!, 0.0)
        assertEquals(1.5, Quantity.parse("1½")!!, 0.0)
        assertEquals(1.5, Quantity.parse("1.5")!!, 0.0)
        assertEquals(1.5, Quantity.parse("1,5")!!, 0.0)
        assertEquals(10.0, Quantity.parse(" 10 ")!!, 0.0)
        assertEquals(0.0, Quantity.parse("0")!!, 0.0)
    }

    @Test
    fun parse_rejectsInvalidInput() {
        assertNull(Quantity.parse(""))
        assertNull(Quantity.parse("abc"))
        assertNull(Quantity.parse("-1"))
        assertNull(Quantity.parse("1.3"))
        assertNull(Quantity.parse("x½"))
        assertNull(Quantity.parse("10000"))
        assertNull(Quantity.parse("NaN"))
    }

    @Test
    fun formatThenParse_roundTrips() {
        listOf(0.5, 1.0, 1.5, 7.0, 10.5).forEach { value ->
            assertEquals(value, Quantity.parse(Quantity.format(value))!!, 0.0)
        }
    }

    @Test
    fun isSingular_onlyForHalfAndOne() {
        assertTrue(Quantity.isSingular(0.5))
        assertTrue(Quantity.isSingular(1.0))
        assertFalse(Quantity.isSingular(1.5))
        assertFalse(Quantity.isSingular(2.0))
        assertFalse(Quantity.isSingular(0.0))
    }
}
