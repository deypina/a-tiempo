package ec.atiempo.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryRulesTest {

    private fun movement(type: MovementType, delta: Double, resulting: Double, at: Long) =
        StockMovement(medicationId = 1, type = type, delta = delta, resultingStock = resulting, createdAt = at)

    @Test
    fun build_mergesEverything_newestFirst() {
        val movements = listOf(
            movement(MovementType.ADJUSTMENT, -6.0, 4.0, 400),
            movement(MovementType.PURCHASE, 10.0, 10.0, 300),
            movement(MovementType.UNDO, 1.0, 1.0, 200),
            movement(MovementType.INTAKE, -1.0, 0.0, 100),
        )
        val missed = listOf(
            IntakeEvent(id = "1-1-x", medicationId = 1, scheduleId = 1, scheduledAt = 250, status = IntakeStatus.MISSED),
        )

        val history = HistoryRules.build(movements, missed, dose = 1.0)

        assertEquals(
            listOf(HistoryKind.ADJUSTMENT, HistoryKind.PURCHASE, HistoryKind.MISSED, HistoryKind.UNDO, HistoryKind.TAKEN),
            history.map { it.kind },
        )
        // A correction shows the new count; the rest show the amount that moved.
        assertEquals(listOf(4.0, 10.0, 0.0, 1.0, 1.0), history.map { it.amount })
    }

    @Test
    fun build_undoneIntakeStaysInHistory() {
        val movements = listOf(
            movement(MovementType.UNDO, 1.0, 5.0, 200),
            movement(MovementType.INTAKE, -1.0, 4.0, 100),
        )

        val history = HistoryRules.build(movements, emptyList(), dose = 1.0)

        assertEquals(listOf(HistoryKind.UNDO, HistoryKind.TAKEN), history.map { it.kind })
    }

    @Test
    fun build_intakeAtZeroShowsTheDose() {
        val history = HistoryRules.build(listOf(movement(MovementType.INTAKE, 0.0, 0.0, 100)), emptyList(), dose = 1.5)

        assertEquals(1.5, history.single().amount, 0.0)
    }

    @Test
    fun build_ignoresEventsThatAreNotMissed() {
        val pending = IntakeEvent(id = "1-1-x", medicationId = 1, scheduleId = 1, scheduledAt = 1)

        assertEquals(emptyList<HistoryEntry>(), HistoryRules.build(emptyList(), listOf(pending), dose = 1.0))
    }
}
