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
        Medication(id = id, name = name, dosePerIntake = 1.0, stock = 10.0, active = active, createdAt = 0, colorKey = "azul")

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

    // --- Next intake, AHORA / ATRASADA and "Recordar" from Hoy ---

    private fun event(medId: Long, scheduleId: Long, status: IntakeStatus) = IntakeEvent(
        id = InventoryRules.intakeEventId(medId, scheduleId, date),
        medicationId = medId, scheduleId = scheduleId, scheduledAt = 0, status = status,
    )

    private fun at(hour: Int, minute: Int) = TodayRules.scheduledAt(date, DoseTime(hour, minute), zone)

    @Test
    fun nextIntake_isTheEarliestNotTaken() {
        val taken = event(1, 11, IntakeStatus.TAKEN)
        val list = TodayRules.build(listOf(gaba, losartan), listOf(taken), date) { null }

        // 8:00 Gabapentina is taken; 8:00 Losartán is next, before 20:00 Gabapentina.
        assertEquals(20L, TodayRules.nextIntake(list)?.schedule?.id)
    }

    @Test
    fun nextIntake_countsSnoozedAndMissed_andIsNullWhenAllTaken() {
        val missed = event(1, 11, IntakeStatus.MISSED)
        val withMissed = TodayRules.build(listOf(gaba, losartan), listOf(missed), date) { null }
        assertEquals(11L, TodayRules.nextIntake(withMissed)?.schedule?.id)

        val snoozed = event(1, 11, IntakeStatus.SNOOZED)
        val withSnoozed = TodayRules.build(listOf(gaba), listOf(snoozed), date) { null }
        assertEquals(11L, TodayRules.nextIntake(withSnoozed)?.schedule?.id)

        val allTaken = listOf(event(1, 11, IntakeStatus.TAKEN), event(1, 10, IntakeStatus.TAKEN))
        assertNull(TodayRules.nextIntake(TodayRules.build(listOf(gaba), allTaken, date) { null }))
    }

    @Test
    fun isLate_onlyAfterItsScheduledTime() {
        val row = TodayRules.build(listOf(losartan), emptyList(), date) { null }.single()

        assertFalse(TodayRules.isLate(row, at(7, 59), zone))
        assertFalse(TodayRules.isLate(row, at(8, 0), zone))
        assertTrue(TodayRules.isLate(row, at(8, 0) + 1, zone))
    }

    @Test
    fun plan_putsNextFirst_restInTimeOrder() {
        val taken = event(1, 11, IntakeStatus.TAKEN)
        val list = TodayRules.build(listOf(gaba, losartan), listOf(taken), date) { null }
        val plan = TodayRules.plan(list.reversed(), at(9, 0), zone)

        assertEquals(20L, plan.next?.schedule?.id)
        assertTrue(plan.nextIsLate)
        assertEquals(listOf(11L, 10L), plan.rest.map { it.schedule.id })
    }

    @Test
    fun plan_beforeItsTime_isNowAndCannotSnooze() {
        val list = TodayRules.build(listOf(losartan), emptyList(), date) { null }
        val plan = TodayRules.plan(list, at(7, 30), zone)

        assertEquals(20L, plan.next?.schedule?.id)
        assertFalse(plan.nextIsLate)
        assertFalse(plan.canSnoozeNext)
        assertTrue(plan.rest.isEmpty())
    }

    @Test
    fun plan_allTaken_hasNoNext() {
        val list = TodayRules.build(listOf(losartan), listOf(event(2, 20, IntakeStatus.TAKEN)), date) { null }
        val plan = TodayRules.plan(list, at(9, 0), zone)

        assertNull(plan.next)
        assertFalse(plan.nextIsLate)
        assertFalse(plan.canSnoozeNext)
        assertEquals(listOf(20L), plan.rest.map { it.schedule.id })
    }

    @Test
    fun canSnoozeFromToday_pendingOrSnoozedAfterItsTime_neverMissed() {
        val after = at(8, 0) + 60_000
        fun row(status: IntakeStatus?) = TodayRules.build(
            listOf(losartan), listOfNotNull(status?.let { event(2, 20, it) }), date,
        ) { null }.single()

        assertTrue(TodayRules.canSnoozeFromToday(row(null), after, zone))
        assertTrue(TodayRules.canSnoozeFromToday(row(IntakeStatus.PENDING), after, zone))
        assertTrue(TodayRules.canSnoozeFromToday(row(IntakeStatus.SNOOZED), after, zone))
        assertFalse(TodayRules.canSnoozeFromToday(row(IntakeStatus.MISSED), after, zone))
        assertFalse(TodayRules.canSnoozeFromToday(row(IntakeStatus.TAKEN), after, zone))
        assertFalse(TodayRules.canSnoozeFromToday(row(IntakeStatus.PENDING), at(7, 0), zone))
    }

    // --- Stock level and the IMPORTANTE block ---

    @Test
    fun stockLevel_outLowOk() {
        val base = med(1, "A")
        assertEquals(StockLevel.OUT, TodayRules.stockLevel(base.copy(stock = 0.0)))
        assertEquals(StockLevel.LOW, TodayRules.stockLevel(base.copy(stock = 5.0, warnThreshold = 5.0)))
        assertEquals(StockLevel.LOW, TodayRules.stockLevel(base.copy(stock = 0.5)))
        assertEquals(StockLevel.OK, TodayRules.stockLevel(base.copy(stock = 5.5, warnThreshold = 5.0)))
    }

    @Test
    fun stockNotices_activeOnly_outFirst() {
        val low = item(med(1, "Aspirina").copy(stock = 2.0))
        val out = item(med(2, "Zinc").copy(stock = 0.0))
        val ok = item(med(3, "Calcio").copy(stock = 20.0))
        val pausedOut = item(med(4, "Pausada", active = false).copy(stock = 0.0))

        val notices = TodayRules.stockNotices(listOf(low, ok, pausedOut, out))

        assertEquals(listOf(2L, 1L), notices.map { it.medication.id })
        assertEquals(listOf(StockLevel.OUT, StockLevel.LOW), notices.map { it.level })
    }
}
