package ec.atiempo.app.ui

import ec.atiempo.app.ui.common.formatTime12
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class FormattingTest {

    @Test
    fun formatTime12_midnight_isTwelveAm() {
        assertEquals("12:00 a. m.", formatTime12(LocalTime.of(0, 0)))
    }

    @Test
    fun formatTime12_noon_isTwelvePm() {
        assertEquals("12:00 p. m.", formatTime12(LocalTime.of(12, 0)))
    }

    @Test
    fun formatTime12_afternoon_hasNoLeadingZero() {
        assertEquals("1:25 p. m.", formatTime12(LocalTime.of(13, 25)))
    }

    @Test
    fun formatTime12_morning() {
        assertEquals("7:00 a. m.", formatTime12(LocalTime.of(7, 0)))
    }

    @Test
    fun formatTime12_lastMinuteOfDay() {
        assertEquals("11:59 p. m.", formatTime12(LocalTime.of(23, 59)))
    }
}
