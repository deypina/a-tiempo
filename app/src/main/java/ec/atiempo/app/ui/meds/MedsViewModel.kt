package ec.atiempo.app.ui.meds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.atiempo.app.data.MedicationRepository
import ec.atiempo.app.domain.MedicationWithSchedules
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface MedsUiState {
    data object Loading : MedsUiState
    data class Ready(val medications: List<MedicationWithSchedules>) : MedsUiState
}

class MedsViewModel(repository: MedicationRepository) : ViewModel() {
    val state: StateFlow<MedsUiState> = repository.observeMedications()
        .map { MedsUiState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MedsUiState.Loading)
}
