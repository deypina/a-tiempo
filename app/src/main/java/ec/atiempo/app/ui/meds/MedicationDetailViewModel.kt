package ec.atiempo.app.ui.meds

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.atiempo.app.data.MedicationRepository
import ec.atiempo.app.domain.MedicationWithSchedules
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface MedicationDetailUiState {
    data object Loading : MedicationDetailUiState

    /** The medication no longer exists (deleted, or the id was not valid): the screen closes. */
    data object Gone : MedicationDetailUiState

    data class Ready(val item: MedicationWithSchedules) : MedicationDetailUiState
}

/** "Ver más": one medication with its actions. Follows the database, so a purchase shows at once. */
class MedicationDetailViewModel(
    repository: MedicationRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val medicationId: Long? = savedStateHandle.get<Long>(ARG_MEDICATION_ID)?.takeIf { it > 0 }

    val state: StateFlow<MedicationDetailUiState> = (
        medicationId?.let { id ->
            // The id comes from a route: it only shows a medication that exists.
            repository.observeMedications().map { list ->
                list.firstOrNull { it.medication.id == id }
                    ?.let { MedicationDetailUiState.Ready(it) }
                    ?: MedicationDetailUiState.Gone
            }
        } ?: flowOf(MedicationDetailUiState.Gone)
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MedicationDetailUiState.Loading)

    companion object {
        const val ARG_MEDICATION_ID = "medicationId"
    }
}
