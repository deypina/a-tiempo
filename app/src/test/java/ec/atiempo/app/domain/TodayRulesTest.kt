package ec.atiempo.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TodayRulesTest {

    private val date = LocalDate.of(2026, 11, 11)
    private val zone = ZoneId.of("America/Guayaquil")

    private fun med(id: Long, name: String, active: Boolean = true) =
        Medication(id = id, name = name, dosePerIntake = 1.0, stock = 10.0, active = active, createdAt = 0)

    private fun item(med: Medication, vararg times: Pair<Long, DoseTime>) =
        MedicationWithSchedules(med, times.map { (id, t) -> Schedule(id, med.id, t) })

    private val gaba = item(med(1, "Gabapentina"), 10L to DoseTime(20, 0), 11L to DoseTime(8, 0))
    private val losartan = item(med(2, "Losartán"), 20L to DoseTime(8, 0))

    @Test
    fun build_ordersByTimeThenName_andDefaultsToPending() {
        val list = TodayRules.build(listOf(gaba, losartan), emptyList(), date) { null }

        assertEquals(listOf(11L, 20L, 10L), list.map { it.schedule.id })
        assertTrue(list.all { it.status == IntakeStatus.PENDING })
        assertEquals("1-11-20261111", list.first().eventId)
        assertEquals(2, list.first().scheduleCount)
    }

    @Test
    fun build_usesTodaysEventStatus() {
        val taken = IntakeEvent(
            id = "1-11-20261111", medicationId = 1, scheduleId = 11, scheduledAt = 0,
            status = IntakeStatus.TAKEN, takenAt = 123L, quantity = 1.0,
        )
        val yesterday = taken.copy(id = "2-20-20261110")
        val list = TodayRules.build(listOf(gaba, losartan), listOf(taken, yesterday), date) { null }

        val first = list.first { it.schedule.id == 11L }
        assertEquals(IntakeStatus.TAKEN, first.status)
        assertEquals(123L, first.takenAt)
        assertTrue(first.canUndo)
        assertFalse(first.canTake)
        // An event from another day does not count for today.
        assertEquals(IntakeStatus.PENDING, list.first { it.schedule.id == 20L }.status)
    }

    @Test
    fun build_snoozedShowsRepeatTime_othersDoNot() {
        val snoozed = IntakeEvent(
            id = "1-11-20261111", medicationId = 1, scheduleId = 11, scheduledAt = 0, status = IntakeStatus.SNOOZED,
        )
        val list = TodayRules.build(listOf(gaba), listOf(snoozed), date) { 999L }

        assertEquals(999L, list.first { it.schedule.id == 11L }.snoozedUntil)
        assertNull(list.first { it.schedule.id == 10L }.snoozedUntil)
    }

    @Test
    fun build_missedCanStillBeTaken() {
        val missed = IntakeEvent(
            id = "2-20-20261111", medicationId = 2, scheduleId = 20, scheduledAt = 0, status = IntakeStatus.MISSED,
        )
        val row = TodayRules.build(listOf(losartan), listOf(missed), date) { null }.single()

        assertTrue(row.canTake)
        assertFalse(row.canUndo)
    }

    @Test
    fun build_skipsPausedMedications() {
        val paused = item(med(3, "Pausada", active = false), 30L to DoseTime(9, 0))
        val list = TodayRules.build(listOf(paused, losartan), emptyList(), date) { null }

        assertEquals(listOf(20L), list.map { it.schedule.id })
    }

    @Test
    fun scheduledAt_isInsideTheDayRange() {
        val at = TodayRules.scheduledAt(date, DoseTime(8, 0), zone)
        val range = TodayRules.dayRange(date, zone)

        assertTrue(at in range)
        assertEquals(24 * 60 * 60 * 1000L, range.last - range.first + 1)
    }
}
