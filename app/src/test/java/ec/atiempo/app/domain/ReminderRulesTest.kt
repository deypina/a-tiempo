package ec.atiempo.app.domain

import ec.atiempo.app.domain.ReminderRules.RepeatDecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderRulesTest {

    private val zone = ZoneId.of("America/Guayaquil")

    private fun at(day: Int, hour: Int, minute: Int, second: Int = 0) =
        ZonedDateTime.of(2026, 11, day, hour, minute, second, 0, zone)

    private fun event(status: IntakeStatus = IntakeStatus.PENDING, repeats: Int = 0) =
        IntakeEvent(id = "7-1-20261109", medicationId = 7, scheduleId = 1, scheduledAt = 0, status = status, snoozeCount = repeats)

    // --- Next occurrence ---

    @Test
    fun nextOccurrence_laterToday_isToday() {
        assertEquals(at(9, 21, 0), ReminderRules.nextOccurrence(DoseTime(21, 0), at(9, 20, 58, 30)))
    }

    @Test
    fun nextOccurrence_alreadyPassed_isTomorrow() {
        assertEquals(at(10, 9, 0), ReminderRules.nextOccurrence(DoseTime(9, 0), at(9, 9, 1)))
    }

    @Test
    fun nextOccurrence_exactlyNow_isTomorrow() {
        // After the alarm fires at 9:00 the next one must be tomorrow, never again today.
        assertEquals(at(10, 9, 0), ReminderRules.nextOccurrence(DoseTime(9, 0), at(9, 9, 0)))
    }

    @Test
    fun nextOccurrence_crossesMonthEnd() {
        assertEquals(at(1, 8, 0).withMonth(12), ReminderRules.nextOccurrence(DoseTime(8, 0), at(30, 22, 0)))
    }

    @Test
    fun nextOccurrence_dstGap_movesForward() {
        // In New York 2:30 a.m. does not exist on 2026-03-08; the alarm still happens that day.
        val ny = ZoneId.of("America/New_York")
        val next = ReminderRules.nextOccurrence(DoseTime(2, 30), ZonedDateTime.of(2026, 3, 8, 1, 0, 0, 0, ny))
        assertEquals(8, next.dayOfMonth)
        assertEquals(3, next.hour)
    }

    // --- Daily alarm ---

    @Test
    fun openIntake_newDay_usesFreshEvent() {
        val fresh = event()
        assertEquals(fresh, ReminderRules.openIntake(null, fresh))
    }

    @Test
    fun openIntake_alreadyTaken_showsNothing() {
        assertNull(ReminderRules.openIntake(event(IntakeStatus.TAKEN), event()))
    }

    @Test
    fun openIntake_alreadyMissed_showsNothing() {
        assertNull(ReminderRules.openIntake(event(IntakeStatus.MISSED), event()))
    }

    @Test
    fun openIntake_pendingAgain_restartsRepeats() {
        val result = ReminderRules.openIntake(event(IntakeStatus.SNOOZED, repeats = 2), event())!!
        assertEquals(IntakeStatus.PENDING, result.status)
        assertEquals(0, result.snoozeCount)
    }

    // --- Repeats ---

    @Test
    fun onRepeatDue_repeatsUpToMaxThenMisses() {
        var current = event()
        repeat(3) { n ->
            val decision = ReminderRules.onRepeatDue(current, maxRepeats = 3)
            assertTrue(decision is RepeatDecision.Remind)
            decision as RepeatDecision.Remind
            assertFalse(decision.afterSnooze)
            assertEquals(n + 1, decision.event.snoozeCount)
            current = decision.event
        }
        val last = ReminderRules.onRepeatDue(current, maxRepeats = 3)
        assertTrue(last is RepeatDecision.Miss)
        assertEquals(IntakeStatus.MISSED, (last as RepeatDecision.Miss).event.status)
    }

    @Test
    fun onRepeatDue_missedNeverTouchesQuantity() {
        val decision = ReminderRules.onRepeatDue(event(repeats = 3), maxRepeats = 3) as RepeatDecision.Miss
        assertEquals(0.0, decision.event.quantity, 0.0)
        assertNull(decision.event.takenAt)
    }

    @Test
    fun onRepeatDue_afterTaken_ignores() {
        assertEquals(RepeatDecision.Ignore, ReminderRules.onRepeatDue(event(IntakeStatus.TAKEN), 3))
    }

    @Test
    fun onRepeatDue_afterMissed_ignores() {
        assertEquals(RepeatDecision.Ignore, ReminderRules.onRepeatDue(event(IntakeStatus.MISSED), 3))
    }

    @Test
    fun onRepeatDue_afterSnooze_remindsAsFreshCycle() {
        val decision = ReminderRules.onRepeatDue(event(IntakeStatus.SNOOZED), 3) as RepeatDecision.Remind
        assertTrue(decision.afterSnooze)
        assertEquals(IntakeStatus.PENDING, decision.event.status)
        assertEquals(0, decision.event.snoozeCount)
    }

    // --- Snooze ---

    @Test
    fun snooze_pending_becomesSnoozedAndResetsRepeats() {
        val result = ReminderRules.snooze(event(repeats = 2))!!
        assertEquals(IntakeStatus.SNOOZED, result.status)
        assertEquals(0, result.snoozeCount)
    }

    @Test
    fun snooze_afterTaken_doesNothing() {
        assertNull(ReminderRules.snooze(event(IntakeStatus.TAKEN)))
    }

    @Test
    fun snooze_afterMissed_doesNothing() {
        assertNull(ReminderRules.snooze(event(IntakeStatus.MISSED)))
    }

    // --- Missed daily alarms ---

    @Test
    fun shouldCatchUp_recentMissedAlarm_reminds() {
        assertTrue(ReminderRules.shouldCatchUp(armedAt = 1_000L, now = 1_000L + 10 * 60_000L))
    }

    @Test
    fun shouldCatchUp_tooOld_skips() {
        assertFalse(ReminderRules.shouldCatchUp(armedAt = 1_000L, now = 1_000L + ReminderRules.CATCH_UP_WINDOW_MS + 1))
    }

    @Test
    fun shouldCatchUp_futureOrUnknown_skips() {
        assertFalse(ReminderRules.shouldCatchUp(armedAt = 5_000L, now = 1_000L))
        assertFalse(ReminderRules.shouldCatchUp(armedAt = null, now = 1_000L))
    }

    @Test
    fun isStaleAlarm_olderThanArmed_isStale() {
        assertTrue(ReminderRules.isStaleAlarm(armedAt = 2_000L, scheduledAt = 1_000L))
        assertFalse(ReminderRules.isStaleAlarm(armedAt = 1_000L, scheduledAt = 1_000L))
        assertFalse(ReminderRules.isStaleAlarm(armedAt = null, scheduledAt = 1_000L))
    }
}
