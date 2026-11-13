package ec.atiempo.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.atiempo.app.data.Settings
import ec.atiempo.app.data.SettingsRepository
import ec.atiempo.app.domain.Appearance
import ec.atiempo.app.domain.DoseTime
import ec.atiempo.app.domain.FontSize
import ec.atiempo.app.reminders.ReminderManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Ajustes (docs/PRD.md F9). Every change is saved right away; there is no "Guardar" button. */
class SettingsViewModel(
    private val settings: SettingsRepository,
    private val reminders: ReminderManager,
) : ViewModel() {

    val state: StateFlow<Settings?> = settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** What the name field shows while typing. Null until the saved name is loaded. */
    private val _nameDraft = MutableStateFlow<String?>(null)
    val nameDraft: StateFlow<String?> = _nameDraft.asStateFlow()

    init {
        viewModelScope.launch { _nameDraft.value = settings.settings.first().userName }
    }

    /** The next notifications use the new name (C-25). */
    fun onNameChange(text: String) {
        val limited = text.take(SettingsRepository.MAX_NAME_LENGTH)
        _nameDraft.value = limited
        viewModelScope.launch { settings.setUserName(limited) }
    }

    /** Applies to the next "Recordar" (C-26). */
    fun onSnoozeMinutes(minutes: Int) {
        viewModelScope.launch { settings.setSnoozeMinutes(minutes) }
    }

    /** Re-arms the daily urgent purchase reminders at the new time. */
    fun onUrgentTime(time: DoseTime) {
        viewModelScope.launch {
            settings.setUrgentReminderTime(time)
            reminders.rescheduleAll()
        }
    }

    fun onFontSize(size: FontSize) {
        viewModelScope.launch { settings.setFontSize(size) }
    }

    fun onAppearance(appearance: Appearance) {
        viewModelScope.launch { settings.setAppearance(appearance) }
    }
}
