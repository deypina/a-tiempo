package ec.atiempo.app.domain

import kotlin.math.abs

enum class HistoryKind { TAKEN, MISSED, PURCHASE, ADJUSTMENT, UNDO }

/** One line of "Historial de {medicamento}" (docs/PRD.md F8). */
data class HistoryEntry(
    val at: Long,
    val kind: HistoryKind,
    /** Pills taken, bought or given back; the new count for a correction; 0 for a missed intake. */
    val amount: Double,
)

/** Builds the history of one medication. Every function is pure. */
object HistoryRules {

    /**
     * Intakes, purchases, corrections and undos come from stock movements, so an undone intake
     * still shows. Missed intakes come from intake events. Most recent first.
     * [dose] replaces an intake that subtracted nothing because the count was already 0.
     */
    fun build(movements: List<StockMovement>, missed: List<IntakeEvent>, dose: Double): List<HistoryEntry> {
        val fromMovements = movements.map { movement ->
            when (movement.type) {
                MovementType.INTAKE -> HistoryEntry(
                    movement.createdAt,
                    HistoryKind.TAKEN,
                    abs(movement.delta).takeIf { it > 0.0 } ?: dose,
                )
                MovementType.PURCHASE -> HistoryEntry(movement.createdAt, HistoryKind.PURCHASE, movement.delta)
                MovementType.ADJUSTMENT -> HistoryEntry(movement.createdAt, HistoryKind.ADJUSTMENT, movement.resultingStock)
                MovementType.UNDO -> HistoryEntry(movement.createdAt, HistoryKind.UNDO, movement.delta)
            }
        }
        val fromMissed = missed
            .filter { it.status == IntakeStatus.MISSED }
            .map { HistoryEntry(it.scheduledAt, HistoryKind.MISSED, 0.0) }
        // Stable sort: movements already come newest first, so ties keep that order.
        return (fromMovements + fromMissed).sortedByDescending { it.at }
    }
}
