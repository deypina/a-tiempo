package ec.atiempo.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchaseRulesTest {

    private fun med(stock: Double, pending: Boolean = true, active: Boolean = true) = Medication(
        id = 1,
        name = "Gabapentina",
        dosePerIntake = 1.0,
        stock = stock,
        warnThreshold = 5.0,
        urgentThreshold = 3.0,
        active = active,
        restockPending = pending,
        createdAt = 0,
    )

    // --- Daily urgent reminder (C-17) ---

    @Test
    fun needsDailyReminder_atUrgentWithoutPurchase_repeats() {
        assertTrue(PurchaseRules.needsDailyReminder(med(3.0)))
        assertTrue(PurchaseRules.needsDailyReminder(med(0.0)))
    }

    @Test
    fun needsDailyReminder_onlyLow_doesNotRepeat() {
        assertFalse(PurchaseRules.needsDailyReminder(med(4.0)))
    }

    @Test
    fun needsDailyReminder_afterPurchase_stops() {
        // A purchase turns reminders off even if the stock is still low (spec §3).
        assertFalse(PurchaseRules.needsDailyReminder(med(2.0, pending = false)))
    }

    @Test
    fun needsDailyReminder_paused_stops() {
        assertFalse(PurchaseRules.needsDailyReminder(med(2.0, active = false)))
    }

    @Test
    fun dailyAlert_isOutAtZeroAndUrgentOtherwise() {
        assertEquals(StockAlert.OUT, PurchaseRules.dailyAlert(med(0.0)))
        assertEquals(StockAlert.URGENT, PurchaseRules.dailyAlert(med(0.5)))
    }

    // --- Visible alert ---

    @Test
    fun keepAlertVisible_onlyWhilePendingAndLow() {
        assertTrue(PurchaseRules.keepAlertVisible(med(5.0)))
        assertFalse(PurchaseRules.keepAlertVisible(med(5.5)))
        assertFalse(PurchaseRules.keepAlertVisible(med(2.0, pending = false)))
    }

    @Test
    fun currentLevel_followsStock() {
        assertEquals(StockAlert.OUT, PurchaseRules.currentLevel(med(0.0)))
        assertEquals(StockAlert.URGENT, PurchaseRules.currentLevel(med(3.0)))
        assertEquals(StockAlert.LOW, PurchaseRules.currentLevel(med(4.0)))
    }

    @Test
    fun purchase_neverGoesAboveMax() {
        val bought = InventoryRules.registerPurchase(med(9995.0), 30.0, 1L)
        assertEquals(Quantity.MAX, bought.medication.stock, 0.0)
        assertEquals(4.0, bought.movement.delta, 0.0)
    }

    // --- Typed quantities ---

    @Test
    fun parsePurchase_acceptsPositiveHalfSteps() {
        assertEquals(10.0, PurchaseRules.parsePurchase("10")!!, 0.0)
        assertEquals(14.5, PurchaseRules.parsePurchase("14½")!!, 0.0)
    }

    @Test
    fun parsePurchase_rejectsZeroEmptyAndInvalid() {
        assertNull(PurchaseRules.parsePurchase("0"))
        assertNull(PurchaseRules.parsePurchase(""))
        assertNull(PurchaseRules.parsePurchase("-3"))
        assertNull(PurchaseRules.parsePurchase("abc"))
    }

    @Test
    fun parseAdjustment_acceptsZero() {
        assertEquals(0.0, PurchaseRules.parseAdjustment("0")!!, 0.0)
        assertEquals(4.0, PurchaseRules.parseAdjustment("4")!!, 0.0)
        assertNull(PurchaseRules.parseAdjustment("-1"))
    }

    // --- End to end with InventoryRules ---

    @Test
    fun purchase_afterUrgent_stopsEverything() {
        val urgent = InventoryRules.confirmIntake(med(4.0, pending = true), intake(), 1L)!!
        assertEquals(StockAlert.URGENT, urgent.alert)
        assertTrue(PurchaseRules.needsDailyReminder(urgent.medication))

        val bought = InventoryRules.registerPurchase(urgent.medication, 10.0, 2L)
        assertEquals(13.0, bought.medication.stock, 0.0)
        assertFalse(PurchaseRules.needsDailyReminder(bought.medication))
        assertFalse(PurchaseRules.keepAlertVisible(bought.medication))
    }

    @Test
    fun adjust_toFour_keepsOnlyLowAlertAndNoDailyReminder() {
        val adjusted = InventoryRules.adjustStock(med(10.0, pending = false), 4.0, 1L)!!
        assertEquals(StockAlert.LOW, adjusted.alert)
        assertEquals(4.0, adjusted.medication.stock, 0.0)
        assertEquals(-6.0, adjusted.movement.delta, 0.0)
        assertTrue(PurchaseRules.keepAlertVisible(adjusted.medication))
        assertFalse(PurchaseRules.needsDailyReminder(adjusted.medication))
    }

    private fun intake() = IntakeEvent(id = "1-1-20261109", medicationId = 1, scheduleId = 1, scheduledAt = 0)
}
