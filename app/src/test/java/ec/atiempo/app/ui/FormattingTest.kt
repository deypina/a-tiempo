package ec.atiempo.app.ui

import ec.atiempo.app.ui.common.formatTime12
import ec.atiempo.app.ui.common.splitTime12
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class FormattingTest {

    @Test
    fun formatTime12_midnight_isTwelveAm() {
        assertEquals("12:00\u00A0a.\u00A0m.", formatTime12(LocalTime.of(0, 0)))
    }

    @Test
    fun formatTime12_noon_isTwelvePm() {
        assertEquals("12:00\u00A0p.\u00A0m.", formatTime12(LocalTime.of(12, 0)))
    }

    @Test
    fun formatTime12_afternoon_hasNoLeadingZero() {
        assertEquals("1:25\u00A0p.\u00A0m.", formatTime12(LocalTime.of(13, 25)))
    }

    @Test
    fun formatTime12_morning() {
        assertEquals("7:00\u00A0a.\u00A0m.", formatTime12(LocalTime.of(7, 0)))
    }

    @Test
    fun formatTime12_lastMinuteOfDay() {
        assertEquals("11:59\u00A0p.\u00A0m.", formatTime12(LocalTime.of(23, 59)))
    }

    @Test
    fun splitTime12_separatesHourAndSuffix() {
        assertEquals("3:30" to "p.\u00A0m.", splitTime12(LocalTime.of(15, 30)))
    }
}
