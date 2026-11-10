package ec.atiempo.app.ui.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.atiempo.app.data.SettingsRepository
import ec.atiempo.app.reminders.ReminderManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class WelcomeViewModel(
    private val settings: SettingsRepository,
    private val reminders: ReminderManager,
) : ViewModel() {

    private var saveJob: Job? = null

    fun saveName(name: String) {
        saveJob = viewModelScope.launch { settings.setUserName(name) }
    }

    /** Ends the welcome flow. Alarms are re-armed in case exact alarms were just allowed. */
    fun finish() {
        viewModelScope.launch {
            saveJob?.join()
            reminders.rescheduleAll()
            settings.completeOnboarding()
        }
    }
}
