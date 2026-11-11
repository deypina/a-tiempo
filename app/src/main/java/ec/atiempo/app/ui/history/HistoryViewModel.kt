package ec.atiempo.app.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.atiempo.app.data.IntakeRepository
import ec.atiempo.app.data.MedicationRepository
import ec.atiempo.app.domain.HistoryEntry
import ec.atiempo.app.domain.HistoryRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.stateIn

sealed interface HistoryUiState {
    data object Loading : HistoryUiState

    /** The medication no longer exists (or the id was not valid): the screen closes. */
    data object Gone : HistoryUiState

    data class Ready(val medicationName: String, val entries: List<HistoryEntry>) : HistoryUiState
}

/** "Historial de {medicamento}" (docs/PRD.md F8). */
class HistoryViewModel(
    medications: MedicationRepository,
    intakes: IntakeRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val medicationId: Long? = savedStateHandle.get<Long>(ARG_MEDICATION_ID)?.takeIf { it > 0 }

    val state: StateFlow<HistoryUiState> = flow<HistoryUiState> {
        // The id comes from a route: it is checked against the database.
        val medication = medicationId?.let { medications.getMedication(it) }?.medication
        if (medication == null) {
            emit(HistoryUiState.Gone)
            return@flow
        }
        val entries: Flow<HistoryUiState> = combine(
            medications.observeMovements(medication.id),
            intakes.observeMissed(medication.id),
        ) { movements, missed ->
            HistoryUiState.Ready(medication.name, HistoryRules.build(movements, missed, medication.dosePerIntake))
        }
        emitAll(entries)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState.Loading)

    companion object {
        const val ARG_MEDICATION_ID = "medicationId"
    }
}
