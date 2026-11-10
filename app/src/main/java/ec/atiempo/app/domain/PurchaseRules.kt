package ec.atiempo.app.domain

/**
 * Purchase reminder rules from docs/PRD.md F5-F7 and docs/ESPECIFICACION_TECNICA.md §3-§4.
 * Thresholds are evaluated in [InventoryRules]; this decides what stays active afterwards.
 */
object PurchaseRules {

    /** Big shortcut buttons in "¿Cuántas pastillas compró?" (docs/PRD.md F6). */
    val PURCHASE_SHORTCUTS = listOf(5.0, 10.0, 14.0, 20.0, 28.0, 30.0)

    /** Default time of the daily urgent purchase reminder: 10:00 a. m. */
    val DEFAULT_URGENT_REMINDER_TIME = DoseTime(10, 0)

    /**
     * The daily reminder repeats while the stock is at or under the urgent threshold
     * and no purchase was registered. Paused medications do not remind.
     */
    fun needsDailyReminder(medication: Medication): Boolean =
        medication.active && medication.restockPending && medication.stock <= medication.urgentThreshold

    /** Which alert the daily reminder shows today. */
    fun dailyAlert(medication: Medication): StockAlert =
        if (medication.stock <= 0.0) StockAlert.OUT else StockAlert.URGENT

    /**
     * A purchase alert already shown stays only while a purchase is still pending and the
     * stock is at or under the warning threshold. Otherwise it is removed.
     */
    fun keepAlertVisible(medication: Medication): Boolean =
        medication.restockPending && medication.stock <= medication.warnThreshold

    /** Quantity typed in "¿Cuántas pastillas compró?". Null unless it is greater than 0. */
    fun parsePurchase(text: String): Double? = Quantity.parse(text)?.takeIf { it > 0.0 }

    /** Quantity typed in "Corregir cantidad". Null unless it is 0 or more. */
    fun parseAdjustment(text: String): Double? = Quantity.parse(text)
}
