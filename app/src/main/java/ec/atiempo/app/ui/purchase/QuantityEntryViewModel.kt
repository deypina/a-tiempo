package ec.atiempo.app.ui.purchase

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.atiempo.app.data.MedicationRepository
import ec.atiempo.app.domain.PurchaseRules
import ec.atiempo.app.domain.Quantity
import ec.atiempo.app.reminders.ReminderManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** "Registrar compra" (docs/PRD.md F6) or "Corregir cantidad" (F7). */
enum class QuantityMode { PURCHASE, ADJUST }

data class QuantityEntryUiState(
    val loading: Boolean = true,
    val mode: QuantityMode = QuantityMode.PURCHASE,
    val medicationName: String = "",
    /** Selected purchase shortcut, or null when the field is used. */
    val shortcut: Double? = null,
    val text: String = "",
    val error: Boolean = false,
    val saving: Boolean = false,
    /** True once saved, or when the medication no longer exists: the screen closes. */
    val finished: Boolean = false,
)

class QuantityEntryViewModel(
    private val repository: MedicationRepository,
    private val reminders: ReminderManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val medicationId: Long? = savedStateHandle.get<Long>(ARG_MEDICATION_ID)?.takeIf { it > 0 }
    private val mode: QuantityMode = savedStateHandle.get<String>(ARG_MODE)
        ?.let { name -> QuantityMode.entries.firstOrNull { it.name == name } }
        ?: QuantityMode.PURCHASE

    private val _state = MutableStateFlow(QuantityEntryUiState(mode = mode))
    val state: StateFlow<QuantityEntryUiState> = _state.asStateFlow()

    init {
        load()
    }

    /** The id comes from a notification or a route: it is checked against the database. */
    private fun load() = viewModelScope.launch {
        val medication = medicationId?.let { repository.getMedication(it) }?.medication
        if (medication == null) {
            _state.update { it.copy(loading = false, finished = true) }
            return@launch
        }
        _state.update {
            it.copy(
                loading = false,
                medicationName = medication.name,
                text = if (mode == QuantityMode.ADJUST) Quantity.format(medication.stock) else "",
            )
        }
    }

    fun onShortcut(value: Double) = _state.update { it.copy(shortcut = value, text = "", error = false) }

    fun onTextChange(value: String) = _state.update { it.copy(text = value.take(8), shortcut = null, error = false) }

    fun save() {
        val current = _state.value
        val id = medicationId ?: return
        if (current.saving || current.loading) return
        val value = when (mode) {
            QuantityMode.PURCHASE -> current.shortcut ?: PurchaseRules.parsePurchase(current.text)
            QuantityMode.ADJUST -> PurchaseRules.parseAdjustment(current.text)
        }
        if (value == null) {
            _state.update { it.copy(error = true) }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            when (mode) {
                QuantityMode.PURCHASE -> reminders.onPurchase(id, value)
                QuantityMode.ADJUST -> reminders.onAdjust(id, value)
            }
            _state.update { it.copy(saving = false, finished = true) }
        }
    }

    companion object {
        const val ARG_MEDICATION_ID = "medicationId"
        const val ARG_MODE = "mode"
    }
}
