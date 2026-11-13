package ec.atiempo.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.atiempo.app.data.IntakeRepository
import ec.atiempo.app.data.MedicationRepository
import ec.atiempo.app.data.SettingsRepository
import ec.atiempo.app.domain.StockNotice
import ec.atiempo.app.domain.TodayIntake
import ec.atiempo.app.domain.TodayPlan
import ec.atiempo.app.domain.TodayRules
import ec.atiempo.app.reminders.ReminderManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

sealed interface TodayUiState {
    data object Loading : TodayUiState
    data class Ready(
        val date: LocalDate,
        val plan: TodayPlan,
        val notices: List<StockNotice>,
        val snoozeMinutes: Int,
        val hasMedications: Boolean,
    ) : TodayUiState
}

/** "Hoy" (docs/PRD.md F4): today's intakes with their status, "Ya la tomé", "Deshacer" and "Recordar". */
@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    settings: SettingsRepository,
    medications: MedicationRepository,
    intakes: IntakeRepository,
    private val reminders: ReminderManager,
    private val clock: Clock,
) : ViewModel() {

    val userName: StateFlow<String> = settings.settings
        .map { it.userName }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /** Refreshed when the screen comes back, so the list follows the day and new snooze times. */
    private val refresh = MutableStateFlow(Refresh(LocalDate.now(clock), 0))

    /** The current time, once a minute, so "AHORA" turns into "ATRASADA" while the screen is open. */
    private val now: Flow<Long> = flow {
        while (true) {
            emit(clock.millis())
            delay(TICK_MS)
        }
    }

    private val snoozeMinutes: Flow<Int> = settings.settings.map { it.snoozeMinutes }.distinctUntilChanged()

    val state: StateFlow<TodayUiState> = refresh
        .flatMapLatest { tick ->
            combine(
                medications.observeMedications(),
                intakes.observeBetween(TodayRules.dayRange(tick.date, clock.zone)),
                snoozeMinutes,
                now,
            ) { meds, events, minutes, nowMillis ->
                val list = TodayRules.build(meds, events, tick.date, reminders::snoozedUntil)
                TodayUiState.Ready(
                    date = tick.date,
                    plan = TodayRules.plan(list, nowMillis, clock.zone),
                    notices = TodayRules.stockNotices(meds),
                    snoozeMinutes = minutes,
                    hasMedications = meds.isNotEmpty(),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState.Loading)

    fun onResume() {
        refresh.value = Refresh(LocalDate.now(clock), refresh.value.count + 1)
    }

    /** "Ya la tomé" for the intake on screen, on the day of the list (see TodayIntake.date). */
    fun onTaken(item: TodayIntake) {
        viewModelScope.launch { reminders.onTakenToday(item.medication.id, item.schedule.id, item.date) }
    }

    fun onUndo(item: TodayIntake) {
        viewModelScope.launch { reminders.onUndo(item.eventId) }
    }

    /** "Recordar en X minutos": the same rules as "Recordar" from the notification. */
    fun onSnooze(item: TodayIntake) {
        viewModelScope.launch { reminders.onSnoozeToday(item.medication.id, item.schedule.id, item.date) }
    }

    private data class Refresh(val date: LocalDate, val count: Int)

    private companion object {
        const val TICK_MS = 60_000L
    }
}
