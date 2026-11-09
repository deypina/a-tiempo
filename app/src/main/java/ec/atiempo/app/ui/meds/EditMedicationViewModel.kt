package ec.atiempo.app.ui.meds

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.atiempo.app.data.MedicationRepository
import ec.atiempo.app.domain.DoseTime
import ec.atiempo.app.domain.Medication
import ec.atiempo.app.domain.MedicationError
import ec.atiempo.app.domain.MedicationForm
import ec.atiempo.app.domain.MedicationValidator
import ec.atiempo.app.domain.Quantity
import ec.atiempo.app.domain.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Dose shortcuts shown as big buttons (docs/PRD.md F2). */
val DOSE_SHORTCUTS = listOf(0.5, 1.0, 1.5, 2.0)

data class EditMedicationUiState(
    val loading: Boolean = false,
    val isNew: Boolean = true,
    val name: String = "",
    /** Selected shortcut, or null when "Otra cantidad" is used. */
    val doseShortcut: Double? = 1.0,
    val doseOther: String = "",
    val times: List<DoseTime> = emptyList(),
    val stock: String = "",
    val warn: String = Quantity.format(Medication.DEFAULT_WARN_THRESHOLD),
    val urgent: String = Quantity.format(Medication.DEFAULT_URGENT_THRESHOLD),
    val note: String = "",
    val active: Boolean = true,
    val errors: Set<MedicationError> = emptySet(),
    val saving: Boolean = false,
    /** True once saved or deleted: the screen closes. */
    val finished: Boolean = false,
)

class EditMedicationViewModel(
    private val repository: MedicationRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val medicationId: Long? =
        savedStateHandle.get<Long>(ARG_MEDICATION_ID)?.takeIf { it > 0 }

    private val _state = MutableStateFlow(EditMedicationUiState(loading = medicationId != null))
    val state: StateFlow<EditMedicationUiState> = _state.asStateFlow()

    init {
        if (medicationId != null) load(medicationId)
    }

    private fun load(id: Long) = viewModelScope.launch {
        val item = repository.getMedication(id)
        if (item == null) {
            _state.update { it.copy(loading = false, finished = true) }
            return@launch
        }
        val med = item.medication
        val isShortcut = med.dosePerIntake in DOSE_SHORTCUTS
        _state.update {
            it.copy(
                loading = false,
                isNew = false,
                name = med.name,
                doseShortcut = med.dosePerIntake.takeIf { isShortcut },
                doseOther = if (isShortcut) "" else Quantity.format(med.dosePerIntake),
                times = item.schedules.map { s -> s.time },
                stock = Quantity.format(med.stock),
                warn = Quantity.format(med.warnThreshold),
                urgent = Quantity.format(med.urgentThreshold),
                note = med.note.orEmpty(),
                active = med.active,
            )
        }
    }

    fun onNameChange(value: String) =
        _state.update { it.copy(name = value.take(MedicationValidator.MAX_NAME_LENGTH)) }

    fun onDoseShortcut(value: Double) = _state.update { it.copy(doseShortcut = value) }
    fun onDoseOtherSelected() = _state.update { it.copy(doseShortcut = null) }
    fun onDoseOtherChange(value: String) = _state.update { it.copy(doseOther = value.take(8)) }
    fun onStockChange(value: String) = _state.update { it.copy(stock = value.take(8)) }
    fun onWarnChange(value: String) = _state.update { it.copy(warn = value.take(8)) }
    fun onUrgentChange(value: String) = _state.update { it.copy(urgent = value.take(8)) }

    fun onNoteChange(value: String) =
        _state.update { it.copy(note = value.take(MedicationValidator.MAX_NOTE_LENGTH)) }

    fun onAddTime(time: DoseTime) = _state.update {
        if (time in it.times) it else it.copy(times = (it.times + time).sorted())
    }

    fun onRemoveTime(time: DoseTime) = _state.update { it.copy(times = it.times - time) }

    fun save() {
        val current = _state.value
        if (current.saving) return
        val form = MedicationForm(
            name = current.name,
            dose = current.doseShortcut ?: Quantity.parse(current.doseOther),
            times = current.times,
            stock = Quantity.parse(current.stock),
            warnThreshold = Quantity.parse(current.warn),
            urgentThreshold = Quantity.parse(current.urgent),
            note = current.note,
        )
        when (val result = MedicationValidator.validate(form)) {
            is ValidationResult.Invalid -> _state.update { it.copy(errors = result.errors) }
            is ValidationResult.Valid -> {
                _state.update { it.copy(errors = emptySet(), saving = true) }
                viewModelScope.launch {
                    repository.saveMedication(medicationId, result.input)
                    _state.update { it.copy(saving = false, finished = true) }
                }
            }
        }
    }

    fun toggleActive() {
        val id = medicationId ?: return
        val newActive = !_state.value.active
        _state.update { it.copy(active = newActive) }
        viewModelScope.launch { repository.setActive(id, newActive) }
    }

    fun delete() {
        val id = medicationId ?: return
        viewModelScope.launch {
            repository.deleteMedication(id)
            _state.update { it.copy(finished = true) }
        }
    }

    companion object {
        const val ARG_MEDICATION_ID = "medicationId"
    }
}
