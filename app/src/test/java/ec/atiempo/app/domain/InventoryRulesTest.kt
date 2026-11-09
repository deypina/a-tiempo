package ec.atiempo.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InventoryRulesTest {

    private val now = 1_000L

    private fun med(stock: Double, dose: Double = 1.0, warn: Double = 5.0, urgent: Double = 3.0, restock: Boolean = false) =
        Medication(
            id = 7,
            name = "Gabapentina",
            dosePerIntake = dose,
            stock = stock,
            warnThreshold = warn,
            urgentThreshold = urgent,
            restockPending = restock,
            createdAt = 0,
        )

    private fun event(status: IntakeStatus = IntakeStatus.PENDING, quantity: Double = 0.0) =
        IntakeEvent(id = "7-1-20261109", medicationId = 7, scheduleId = 1, scheduledAt = 0, status = status, quantity = quantity)

    // --- Confirm intake ---

    @Test
    fun confirmIntake_subtractsDoseMarksTakenAndRecordsMovement() {
        val result = InventoryRules.confirmIntake(med(stock = 8.0), event(), now)!!

        assertEquals(7.0, result.medication.stock, 0.0)
        assertEquals(IntakeStatus.TAKEN, result.event.status)
        assertEquals(now, result.event.takenAt)
        assertEquals(1.0, result.event.quantity, 0.0)
        assertEquals(MovementType.INTAKE, result.movement.type)
        assertEquals(-1.0, result.movement.delta, 0.0)
        assertEquals(7.0, result.movement.resultingStock, 0.0)
        assertNull(result.alert)
    }

    @Test
    fun confirmIntake_twice_doesNotSubtractTwice() {
        val first = InventoryRules.confirmIntake(med(stock = 8.0), event(), now)!!

        val second = InventoryRules.confirmIntake(first.medication, first.event, now + 1)

        assertNull(second)
        assertEquals(7.0, first.medication.stock, 0.0)
    }

    @Test
    fun confirmIntake_fromSnoozedOrMissed_isAllowed() {
        assertEquals(7.0, InventoryRules.confirmIntake(med(8.0), event(IntakeStatus.SNOOZED), now)!!.medication.stock, 0.0)
        assertEquals(7.0, InventoryRules.confirmIntake(med(8.0), event(IntakeStatus.MISSED), now)!!.medication.stock, 0.0)
    }

    @Test
    fun confirmIntake_withHalfPill() {
        val result = InventoryRules.confirmIntake(med(stock = 10.0, dose = 0.5), event(), now)!!

        assertEquals(9.5, result.medication.stock, 0.0)
        assertEquals(0.5, result.event.quantity, 0.0)
    }

    @Test
    fun confirmIntake_neverGoesBelowZero_andRecordsOnlyWhatWasLeft() {
        val result = InventoryRules.confirmIntake(med(stock = 0.5, dose = 1.0), event(), now)!!

        assertEquals(0.0, result.medication.stock, 0.0)
        assertEquals(0.5, result.event.quantity, 0.0)
        assertEquals(-0.5, result.movement.delta, 0.0)
        assertEquals(StockAlert.OUT, result.alert)
    }

    @Test
    fun confirmIntake_withZeroStock_staysAtZeroWithoutNewAlert() {
        val result = InventoryRules.confirmIntake(med(stock = 0.0), event(), now)!!

        assertEquals(0.0, result.medication.stock, 0.0)
        assertEquals(0.0, result.event.quantity, 0.0)
        assertNull(result.alert)
    }

    @Test
    fun confirmIntake_crossingWarn_setsRestockPending() {
        val result = InventoryRules.confirmIntake(med(stock = 6.0), event(), now)!!

        assertEquals(StockAlert.LOW, result.alert)
        assertTrue(result.medication.restockPending)
    }

    // --- Thresholds (C-15, C-16, C-18, C-21) ---

    @Test
    fun thresholds_crossingWarn_isLow() {
        assertEquals(StockAlert.LOW, InventoryRules.evaluateThresholds(6.0, 5.0, med(6.0)))
    }

    @Test
    fun thresholds_crossingUrgent_isUrgent() {
        assertEquals(StockAlert.URGENT, InventoryRules.evaluateThresholds(4.0, 3.0, med(4.0)))
    }

    @Test
    fun thresholds_reachingZero_isOut() {
        assertEquals(StockAlert.OUT, InventoryRules.evaluateThresholds(1.0, 0.0, med(1.0)))
    }

    @Test
    fun thresholds_doubleCrossing_returnsOnlyTheMostSerious() {
        assertEquals(StockAlert.URGENT, InventoryRules.evaluateThresholds(6.0, 2.0, med(6.0)))
        assertEquals(StockAlert.OUT, InventoryRules.evaluateThresholds(6.0, 0.0, med(6.0)))
    }

    @Test
    fun thresholds_doseOfFourFromSix_firesOnlyUrgent() {
        val result = InventoryRules.confirmIntake(med(stock = 6.0, dose = 4.0), event(), now)!!

        assertEquals(StockAlert.URGENT, result.alert)
    }

    @Test
    fun thresholds_alreadyBelow_doesNotRepeat() {
        assertNull(InventoryRules.evaluateThresholds(5.0, 4.0, med(5.0)))
        assertNull(InventoryRules.evaluateThresholds(3.0, 2.0, med(3.0)))
    }

    @Test
    fun thresholds_withHalfPills() {
        assertEquals(StockAlert.LOW, InventoryRules.evaluateThresholds(5.5, 5.0, med(5.5)))
        assertEquals(StockAlert.URGENT, InventoryRules.evaluateThresholds(3.5, 3.0, med(3.5)))
        assertEquals(StockAlert.URGENT, InventoryRules.evaluateThresholds(4.0, 2.5, med(4.0)))
        assertEquals(StockAlert.OUT, InventoryRules.evaluateThresholds(0.5, 0.0, med(0.5)))
        assertNull(InventoryRules.evaluateThresholds(5.0, 4.5, med(5.0)))
    }

    @Test
    fun thresholds_goingUp_neverAlerts() {
        assertNull(InventoryRules.evaluateThresholds(2.0, 10.0, med(2.0)))
    }

    // --- Undo ---

    @Test
    fun undoIntake_givesBackQuantityAndReturnsToPending() {
        val taken = InventoryRules.confirmIntake(med(stock = 8.0), event(), now)!!

        val undo = InventoryRules.undoIntake(taken.medication, taken.event, now + 1)!!

        assertEquals(8.0, undo.medication.stock, 0.0)
        assertEquals(IntakeStatus.PENDING, undo.event.status)
        assertNull(undo.event.takenAt)
        assertEquals(MovementType.UNDO, undo.movement.type)
        assertEquals(1.0, undo.movement.delta, 0.0)
    }

    @Test
    fun undoIntake_backAboveWarn_clearsRestockAndStopsAlerts() {
        val taken = InventoryRules.confirmIntake(med(stock = 6.0), event(), now)!!
        assertTrue(taken.medication.restockPending)

        val undo = InventoryRules.undoIntake(taken.medication, taken.event, now + 1)!!

        assertFalse(undo.medication.restockPending)
        assertTrue(undo.dismissPurchaseAlerts)
        assertTrue(undo.stopUrgentReminder)
    }

    @Test
    fun undoIntake_stillLow_keepsRestockPending() {
        val taken = InventoryRules.confirmIntake(med(stock = 3.0, restock = true), event(), now)!!

        val undo = InventoryRules.undoIntake(taken.medication, taken.event, now + 1)!!

        assertTrue(undo.medication.restockPending)
        assertFalse(undo.dismissPurchaseAlerts)
        assertFalse(undo.stopUrgentReminder)
    }

    @Test
    fun undoIntake_whenNotTaken_doesNothing() {
        assertNull(InventoryRules.undoIntake(med(8.0), event(IntakeStatus.PENDING), now))
    }

    // --- Purchase ---

    @Test
    fun registerPurchase_addsStockAndClearsRestock() {
        val result = InventoryRules.registerPurchase(med(stock = 3.0, restock = true), 10.0, now)

        assertEquals(13.0, result.medication.stock, 0.0)
        assertFalse(result.medication.restockPending)
        assertEquals(MovementType.PURCHASE, result.movement.type)
        assertEquals(10.0, result.movement.delta, 0.0)
        assertEquals(13.0, result.movement.resultingStock, 0.0)
    }

    @Test
    fun registerPurchase_smallAmount_stillTurnsRemindersOff() {
        val result = InventoryRules.registerPurchase(med(stock = 1.0, restock = true), 1.0, now)

        assertEquals(2.0, result.medication.stock, 0.0)
        assertFalse(result.medication.restockPending)
    }

    @Test(expected = IllegalArgumentException::class)
    fun registerPurchase_zero_isRejected() {
        InventoryRules.registerPurchase(med(3.0), 0.0, now)
    }

    // --- Adjust ---

    @Test
    fun adjustStock_down_recordsAdjustmentAndAlertsOnCrossing() {
        val result = InventoryRules.adjustStock(med(stock = 8.0), 4.0, now)!!

        assertEquals(4.0, result.medication.stock, 0.0)
        assertEquals(MovementType.ADJUSTMENT, result.movement.type)
        assertEquals(-4.0, result.movement.delta, 0.0)
        assertEquals(StockAlert.LOW, result.alert)
        assertTrue(result.medication.restockPending)
    }

    @Test
    fun adjustStock_up_doesNotAlertAndClearsRestockAboveWarn() {
        val result = InventoryRules.adjustStock(med(stock = 2.0, restock = true), 12.0, now)!!

        assertNull(result.alert)
        assertFalse(result.medication.restockPending)
        assertTrue(result.dismissPurchaseAlerts)
        assertTrue(result.stopUrgentReminder)
    }

    @Test
    fun adjustStock_sameValue_returnsNull() {
        assertNull(InventoryRules.adjustStock(med(stock = 4.0), 4.0, now))
    }

    @Test(expected = IllegalArgumentException::class)
    fun adjustStock_negative_isRejected() {
        InventoryRules.adjustStock(med(4.0), -1.0, now)
    }

    // --- Days left (C-01, C-02, C-03, C-06) ---

    @Test
    fun daysLeft_onePillOnceADay() {
        assertEquals(8, InventoryRules.daysLeft(med(stock = 8.0), scheduleCount = 1))
    }

    @Test
    fun daysLeft_halfPillOnceADay() {
        assertEquals(20, InventoryRules.daysLeft(med(stock = 10.0, dose = 0.5), scheduleCount = 1))
    }

    @Test
    fun daysLeft_twiceADay() {
        assertEquals(5, InventoryRules.daysLeft(med(stock = 10.0), scheduleCount = 2))
    }

    @Test
    fun daysLeft_roundsDown() {
        assertEquals(3, InventoryRules.daysLeft(med(stock = 7.0), scheduleCount = 2))
        assertEquals(0, InventoryRules.daysLeft(med(stock = 0.5), scheduleCount = 1))
    }

    @Test
    fun daysLeft_pausedOrWithoutSchedules_isNull() {
        assertNull(InventoryRules.daysLeft(med(stock = 8.0).copy(active = false), scheduleCount = 1))
        assertNull(InventoryRules.daysLeft(med(stock = 8.0), scheduleCount = 0))
    }

    // --- Event id ---

    @Test
    fun intakeEventId_combinesMedicationScheduleAndDate() {
        assertEquals("7-1-20261109", InventoryRules.intakeEventId(7, 1, LocalDate.of(2026, 11, 9)))
    }
}
