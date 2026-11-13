package ec.atiempo.app.ui.meds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import ec.atiempo.app.ui.common.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import ec.atiempo.app.ui.common.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ec.atiempo.app.R
import ec.atiempo.app.domain.DoseTime
import ec.atiempo.app.domain.MedicationColors
import ec.atiempo.app.domain.MedicationError
import ec.atiempo.app.domain.Quantity
import ec.atiempo.app.ui.common.AppViewModelFactory
import ec.atiempo.app.ui.common.TimePickerDialog
import ec.atiempo.app.ui.common.formatTime
import ec.atiempo.app.ui.theme.medicationColor
import ec.atiempo.app.ui.theme.medicationColorName
import ec.atiempo.app.ui.theme.needsOutline

private val ButtonModifier = Modifier
    .fillMaxWidth()
    .heightIn(min = 56.dp)

@Composable
fun EditMedicationScreen(
    onClose: () -> Unit,
    viewModel: EditMedicationViewModel = viewModel(factory = AppViewModelFactory.instance),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.finished) {
        if (state.finished) onClose()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Header(isNew = state.isNew, onBack = onClose)
        if (!state.loading) {
            // Name
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.med_name)) },
                placeholder = { Text(stringResource(R.string.med_name_hint)) },
                singleLine = true,
                isError = MedicationError.NAME_MISSING in state.errors,
                supportingText = errorText(MedicationError.NAME_MISSING in state.errors, R.string.med_error_name),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            // Color
            ColorSection(selected = state.colorKey, onSelect = viewModel::onColorChange)

            // Dose
            DoseSection(
                shortcut = state.doseShortcut,
                other = state.doseOther,
                hasError = MedicationError.DOSE_INVALID in state.errors,
                onShortcut = viewModel::onDoseShortcut,
                onOtherSelected = viewModel::onDoseOtherSelected,
                onOtherChange = viewModel::onDoseOtherChange,
            )

            // Times
            Text(stringResource(R.string.med_times), style = MaterialTheme.typography.titleLarge)
            state.times.forEach { time ->
                TimeRow(time = time, onRemove = { viewModel.onRemoveTime(time) })
            }
            OutlinedButton(onClick = { showTimePicker = true }, modifier = ButtonModifier) {
                Text(stringResource(if (state.times.isEmpty()) R.string.med_add_first_time else R.string.med_add_time))
            }
            if (MedicationError.TIME_MISSING in state.errors) {
                Text(
                    stringResource(R.string.med_error_time),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            // Stock and thresholds
            NumberField(
                value = state.stock,
                onValueChange = viewModel::onStockChange,
                label = stringResource(R.string.med_stock),
                error = if (MedicationError.STOCK_INVALID in state.errors) R.string.med_error_stock else null,
            )
            NumberField(
                value = state.warn,
                onValueChange = viewModel::onWarnChange,
                label = stringResource(R.string.med_warn),
                error = if (MedicationError.WARN_INVALID in state.errors) R.string.med_error_number else null,
            )
            NumberField(
                value = state.urgent,
                onValueChange = viewModel::onUrgentChange,
                label = stringResource(R.string.med_urgent),
                error = when {
                    MedicationError.URGENT_INVALID in state.errors -> R.string.med_error_number
                    MedicationError.THRESHOLDS_INVALID in state.errors -> R.string.med_error_thresholds
                    else -> null
                },
            )

            // Note
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text(stringResource(R.string.med_note)) },
                placeholder = { Text(stringResource(R.string.med_note_hint)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(onClick = viewModel::save, enabled = !state.saving, modifier = ButtonModifier) {
                Text(stringResource(R.string.med_save))
            }

            if (!state.isNew) {
                OutlinedButton(onClick = viewModel::toggleActive, modifier = ButtonModifier) {
                    Text(stringResource(if (state.active) R.string.med_pause else R.string.med_resume))
                }
                TextButton(onClick = { showDeleteConfirm = true }, modifier = ButtonModifier) {
                    Text(stringResource(R.string.med_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            title = stringResource(R.string.med_times),
            initial = DoseTime(8, 0),
            onDismiss = { showTimePicker = false },
            onConfirm = {
                viewModel.onAddTime(it)
                showTimePicker = false
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            text = {
                Text(
                    stringResource(R.string.med_delete_confirm, state.name),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.delete()
                    },
                    modifier = Modifier.heightIn(min = 56.dp),
                ) {
                    Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }, modifier = Modifier.heightIn(min = 56.dp)) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun Header(isNew: Boolean, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(56.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_back),
                contentDescription = stringResource(R.string.common_back),
            )
        }
        Text(
            stringResource(if (isNew) R.string.meds_add else R.string.med_edit_title),
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}

/** "Color para reconocerla": six big circles; the chosen one has an ink ring and a check mark. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSection(selected: String, onSelect: (String) -> Unit) {
    Text(stringResource(R.string.med_color), style = MaterialTheme.typography.titleLarge)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.selectableGroup(),
    ) {
        MedicationColors.KEYS.forEach { key ->
            ColorChoice(key = key, selected = key == selected, onClick = { onSelect(key) })
        }
    }
    Text(stringResource(R.string.med_color_help), style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun ColorChoice(key: String, selected: Boolean, onClick: () -> Unit) {
    val name = stringResource(medicationColorName(key))
    val description = if (selected) stringResource(R.string.color_selected, name) else name
    val ink = MaterialTheme.colorScheme.onBackground
    val fill = medicationColor(key)
    val ring = when {
        selected -> Modifier.border(3.dp, ink, CircleShape)
        needsOutline(key) -> Modifier.border(1.dp, ink, CircleShape)
        else -> Modifier
    }
    // 56dp touch target around a 48dp circle.
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics { contentDescription = description },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .then(ring)
                .padding(if (selected) 5.dp else 0.dp)
                .background(fill, CircleShape),
        ) {
            if (selected) CheckMark(color = if (fill.luminance() > 0.4f) CHECK_DARK else CHECK_LIGHT)
        }
    }
}

/** Ink and paper of the light theme: the check stays readable on every palette color. */
private val CHECK_DARK = Color(0xFF1E1A15)
private val CHECK_LIGHT = Color(0xFFFFFDF8)

/** Check mark drawn as a path, so no glyph or extra drawable is needed. Decorative. */
@Composable
private fun CheckMark(color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.52f)
            lineTo(size.width * 0.40f, size.height * 0.80f)
            lineTo(size.width * 0.88f, size.height * 0.22f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DoseSection(
    shortcut: Double?,
    other: String,
    hasError: Boolean,
    onShortcut: (Double) -> Unit,
    onOtherSelected: () -> Unit,
    onOtherChange: (String) -> Unit,
) {
    Text(stringResource(R.string.med_dose), style = MaterialTheme.typography.titleLarge)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DOSE_SHORTCUTS.forEach { value ->
            FilterChip(
                selected = shortcut == value,
                onClick = { onShortcut(value) },
                label = { Text(Quantity.format(value), style = MaterialTheme.typography.titleLarge) },
                modifier = Modifier.heightIn(min = 56.dp),
            )
        }
        FilterChip(
            selected = shortcut == null,
            onClick = onOtherSelected,
            label = { Text(stringResource(R.string.med_dose_other), style = MaterialTheme.typography.bodyLarge) },
            modifier = Modifier.heightIn(min = 56.dp),
        )
    }
    if (shortcut == null) {
        NumberField(
            value = other,
            onValueChange = onOtherChange,
            label = stringResource(R.string.med_dose_other),
            error = if (hasError) R.string.med_error_dose else null,
        )
    }
}

@Composable
private fun TimeRow(time: DoseTime, onRemove: () -> Unit) {
    val label = formatTime(time)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = onRemove, modifier = Modifier.size(56.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.med_remove_time, label),
            )
        }
    }
}

@Composable
private fun NumberField(value: String, onValueChange: (String) -> Unit, label: String, error: Int?) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = errorText(error != null, error ?: 0),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun errorText(show: Boolean, message: Int): (@Composable () -> Unit)? =
    if (show) {
        { Text(stringResource(message)) }
    } else {
        null
    }
