package ec.atiempo.app.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Inventory rules from docs/ESPECIFICACION_TECNICA.md §3. Every function is pure. */
object InventoryRules {

    data class IntakeResult(
        val medication: Medication,
        val event: IntakeEvent,
        val movement: StockMovement,
        val alert: StockAlert?,
    )

    data class UndoResult(
        val medication: Medication,
        val event: IntakeEvent,
        val movement: StockMovement,
        val stopUrgentReminder: Boolean,
        val dismissPurchaseAlerts: Boolean,
    )

    data class PurchaseResult(
        val medication: Medication,
        val movement: StockMovement,
    )

    data class AdjustResult(
        val medication: Medication,
        val movement: StockMovement,
        val alert: StockAlert?,
        val stopUrgentReminder: Boolean,
        val dismissPurchaseAlerts: Boolean,
    )

    /** Unique id per scheduled intake: "{medId}-{scheduleId}-{yyyyMMdd}". Makes confirming idempotent. */
    fun intakeEventId(medicationId: Long, scheduleId: Long, date: LocalDate): String =
        "$medicationId-$scheduleId-${date.format(DateTimeFormatter.BASIC_ISO_DATE)}"

    /** Returns null when the intake was already taken, so confirming twice never subtracts twice. */
    fun confirmIntake(medication: Medication, event: IntakeEvent, now: Long): IntakeResult? {
        require(event.medicationId == medication.id) { "event belongs to another medication" }
        if (event.status == IntakeStatus.TAKEN) return null

        val previous = medication.stock
        val taken = min(medication.dosePerIntake, previous)
        val newStock = max(0.0, previous - medication.dosePerIntake)
        val alert = evaluateThresholds(previous, newStock, medication)

        return IntakeResult(
            medication = medication.copy(
                stock = newStock,
                restockPending = medication.restockPending || alert != null,
            ),
            event = event.copy(status = IntakeStatus.TAKEN, takenAt = now, quantity = taken),
            movement = movement(medication.id, MovementType.INTAKE, -taken, newStock, now),
            alert = alert,
        )
    }

    /** Gives back the amount taken. Returns null when the intake was not taken. */
    fun undoIntake(medication: Medication, event: IntakeEvent, now: Long): UndoResult? {
        require(event.medicationId == medication.id) { "event belongs to another medication" }
        if (event.status != IntakeStatus.TAKEN) return null

        val newStock = medication.stock + event.quantity
        val aboveWarn = newStock > medication.warnThreshold
        return UndoResult(
            medication = medication.copy(
                stock = newStock,
                restockPending = if (aboveWarn) false else medication.restockPending,
            ),
            // A fresh reminder cycle: the earlier unanswered repeats do not count anymore.
            event = event.copy(status = IntakeStatus.PENDING, snoozeCount = 0, takenAt = null, quantity = 0.0),
            movement = movement(medication.id, MovementType.UNDO, event.quantity, newStock, now),
            stopUrgentReminder = newStock > medication.urgentThreshold,
            dismissPurchaseAlerts = aboveWarn,
        )
    }

    /**
     * Registering a purchase always turns purchase reminders off (docs/PRD.md F5 and F6),
     * even if the new stock is still low. Reaching 0 later triggers the out-of-stock alert again.
     */
    fun registerPurchase(medication: Medication, quantity: Double, now: Long): PurchaseResult {
        require(quantity > 0) { "purchase quantity must be greater than 0" }
        // Capped so the count always stays editable (Quantity.MAX).
        val newStock = min(medication.stock + quantity, Quantity.MAX)
        return PurchaseResult(
            medication = medication.copy(stock = newStock, restockPending = false),
            movement = movement(medication.id, MovementType.PURCHASE, newStock - medication.stock, newStock, now),
        )
    }

    /** Sets the real count. Alerts only when it goes down across a threshold. Null if nothing changes. */
    fun adjustStock(medication: Medication, newValue: Double, now: Long): AdjustResult? {
        require(newValue >= 0) { "stock cannot be negative" }
        val previous = medication.stock
        if (newValue == previous) return null

        val alert = evaluateThresholds(previous, newValue, medication)
        val aboveWarn = newValue > medication.warnThreshold
        val restockPending = when {
            alert != null -> true
            aboveWarn -> false
            else -> medication.restockPending
        }
        return AdjustResult(
            medication = medication.copy(stock = newValue, restockPending = restockPending),
            movement = movement(medication.id, MovementType.ADJUSTMENT, newValue - previous, newValue, now),
            alert = alert,
            stopUrgentReminder = newValue > medication.urgentThreshold,
            dismissPurchaseAlerts = aboveWarn,
        )
    }

    /**
     * Alerts fire only when crossing a threshold going down. If one change crosses
     * several thresholds (e.g. 6 to 2), only the most serious alert is returned.
     */
    fun evaluateThresholds(previous: Double, current: Double, medication: Medication): StockAlert? {
        if (current >= previous) return null
        return when {
            current <= 0.0 && previous > 0.0 -> StockAlert.OUT
            current <= medication.urgentThreshold && previous > medication.urgentThreshold -> StockAlert.URGENT
            current <= medication.warnThreshold && previous > medication.warnThreshold -> StockAlert.LOW
            else -> null
        }
    }

    /** Whole days the stock lasts. Null when paused or without schedules (nothing to show). */
    fun daysLeft(medication: Medication, scheduleCount: Int): Int? {
        if (!medication.active || scheduleCount <= 0 || medication.dosePerIntake <= 0) return null
        return floor(medication.stock / (medication.dosePerIntake * scheduleCount)).toInt()
    }

    private fun movement(medicationId: Long, type: MovementType, delta: Double, resulting: Double, now: Long) =
        StockMovement(
            medicationId = medicationId,
            type = type,
            delta = delta,
            resultingStock = resulting,
            createdAt = now,
        )
}
