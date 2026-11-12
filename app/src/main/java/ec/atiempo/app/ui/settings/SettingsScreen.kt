package ec.atiempo.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import ec.atiempo.app.ui.common.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ec.atiempo.app.R
import ec.atiempo.app.domain.FontSize
import ec.atiempo.app.domain.ReminderRules
import ec.atiempo.app.ui.common.AppViewModelFactory
import ec.atiempo.app.ui.common.TimePickerDialog
import ec.atiempo.app.ui.common.formatTime

private val ButtonModifier = Modifier
    .fillMaxWidth()
    .heightIn(min = 56.dp)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onPermissions: () -> Unit,
    onAbout: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelFactory.instance),
) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val nameDraft by viewModel.nameDraft.collectAsStateWithLifecycle()
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineLarge)
        val current = settings ?: return@Column
        val name = nameDraft ?: return@Column

        OutlinedTextField(
            value = name,
            onValueChange = viewModel::onNameChange,
            label = { Text(stringResource(R.string.settings_name)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )

        SectionTitle(stringResource(R.string.settings_snooze))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ReminderRules.SNOOZE_OPTIONS.forEach { minutes ->
                Choice(
                    label = stringResource(R.string.settings_minutes, minutes),
                    selected = current.snoozeMinutes == minutes,
                    onClick = { viewModel.onSnoozeMinutes(minutes) },
                )
            }
        }

        SectionTitle(stringResource(R.string.settings_urgent_hour))
        OutlinedButton(onClick = { showTimePicker = true }, modifier = ButtonModifier) {
            Text(formatTime(current.urgentReminderTime))
        }

        SectionTitle(stringResource(R.string.settings_font))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FontSize.entries.forEach { size ->
                Choice(
                    label = stringResource(
                        when (size) {
                            FontSize.NORMAL -> R.string.font_normal
                            FontSize.LARGE -> R.string.font_large
                            FontSize.XLARGE -> R.string.font_xlarge
                        },
                    ),
                    selected = current.fontSize == size,
                    onClick = { viewModel.onFontSize(size) },
                )
            }
        }

        OutlinedButton(onClick = onPermissions, modifier = ButtonModifier) {
            Text(stringResource(R.string.settings_permissions))
        }
        OutlinedButton(onClick = onAbout, modifier = ButtonModifier) {
            Text(stringResource(R.string.settings_about))
        }

        if (showTimePicker) {
            TimePickerDialog(
                title = stringResource(R.string.settings_urgent_hour),
                initial = current.urgentReminderTime,
                onDismiss = { showTimePicker = false },
                onConfirm = {
                    viewModel.onUrgentTime(it)
                    showTimePicker = false
                },
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        modifier = Modifier.heightIn(min = 56.dp),
    )
}
