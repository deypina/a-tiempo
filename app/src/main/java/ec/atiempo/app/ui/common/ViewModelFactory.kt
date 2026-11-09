package ec.atiempo.app.ui.common

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ec.atiempo.app.ATiempoApp
import ec.atiempo.app.ui.meds.EditMedicationViewModel
import ec.atiempo.app.ui.meds.MedsViewModel

/** Builds ViewModels from the manual [ec.atiempo.app.AppContainer]. */
object AppViewModelFactory {
    val instance = viewModelFactory {
        initializer {
            MedsViewModel(app().container.medicationRepository)
        }
        initializer {
            EditMedicationViewModel(app().container.medicationRepository, createSavedStateHandle())
        }
    }

    private fun androidx.lifecycle.viewmodel.CreationExtras.app(): ATiempoApp =
        this[APPLICATION_KEY] as ATiempoApp
}
