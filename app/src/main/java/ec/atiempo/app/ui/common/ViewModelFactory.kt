package ec.atiempo.app.ui.common

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ec.atiempo.app.ATiempoApp
import ec.atiempo.app.ui.meds.EditMedicationViewModel
import ec.atiempo.app.ui.meds.MedsViewModel
import ec.atiempo.app.ui.purchase.QuantityEntryViewModel
import ec.atiempo.app.ui.today.TodayViewModel
import ec.atiempo.app.ui.welcome.WelcomeViewModel

/** Builds ViewModels from the manual [ec.atiempo.app.AppContainer]. */
object AppViewModelFactory {
    val instance = viewModelFactory {
        initializer {
            MedsViewModel(app().container.medicationRepository)
        }
        initializer {
            EditMedicationViewModel(
                app().container.medicationRepository,
                app().container.reminderManager,
                createSavedStateHandle(),
            )
        }
        initializer {
            QuantityEntryViewModel(
                app().container.medicationRepository,
                app().container.reminderManager,
                createSavedStateHandle(),
            )
        }
        initializer {
            WelcomeViewModel(app().container.settingsRepository, app().container.reminderManager)
        }
        initializer {
            TodayViewModel(app().container.settingsRepository)
        }
    }

    private fun androidx.lifecycle.viewmodel.CreationExtras.app(): ATiempoApp =
        this[APPLICATION_KEY] as ATiempoApp
}
