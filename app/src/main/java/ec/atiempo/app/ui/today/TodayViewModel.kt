package ec.atiempo.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.atiempo.app.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Phase 3 only shows the greeting and reminder warnings; the intake list arrives in phase 5. */
class TodayViewModel(settings: SettingsRepository) : ViewModel() {
    val userName: StateFlow<String> = settings.settings
        .map { it.userName }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
}
