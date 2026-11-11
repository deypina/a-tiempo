package ec.atiempo.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.atiempo.app.data.IntakeRepository
import ec.atiempo.app.data.MedicationRepository
import ec.atiempo.app.data.SettingsRepository
import ec.atiempo.app.domain.TodayIntake
import ec.atiempo.app.domain.TodayRules
import ec.atiempo.app.reminders.ReminderManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

sealed interface TodayUiState {
    data object Loading : TodayUiState
    data class Ready(val intakes: List<TodayIntake>, val hasMedications: Boolean) : TodayUiState
}

/** "Hoy" (docs/PRD.md F4): today's intakes with their status, "Ya la tomé" and "Deshacer". */
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

    val state: StateFlow<TodayUiState> = refresh
        .flatMapLatest { tick ->
            combine(
                medications.observeMedications(),
                intakes.observeBetween(TodayRules.dayRange(tick.date, clock.zone)),
            ) { meds, events ->
                TodayUiState.Ready(
                    intakes = TodayRules.build(meds, events, tick.date, reminders::snoozedUntil),
                    hasMedications = meds.isNotEmpty(),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState.Loading)

    fun onResume() {
        refresh.value = Refresh(LocalDate.now(clock), refresh.value.count + 1)
    }

    fun onTaken(item: TodayIntake) {
        viewModelScope.launch { reminders.onTakenToday(item.medication.id, item.schedule.id) }
    }

    fun onUndo(item: TodayIntake) {
        viewModelScope.launch { reminders.onUndo(item.eventId) }
    }

    private data class Refresh(val date: LocalDate, val count: Int)
}
