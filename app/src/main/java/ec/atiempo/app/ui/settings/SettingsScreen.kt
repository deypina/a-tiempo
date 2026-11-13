package ec.atiempo.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ec.atiempo.app.R
import ec.atiempo.app.domain.Appearance
import ec.atiempo.app.domain.FontSize
import ec.atiempo.app.domain.ReminderRules
import ec.atiempo.app.ui.common.AppViewModelFactory
import ec.atiempo.app.ui.common.DoubleRule
import ec.atiempo.app.ui.common.RowRule
import ec.atiempo.app.ui.common.TimePickerDialog
import ec.atiempo.app.ui.common.formatTime
import ec.atiempo.app.ui.theme.Libreta

/** Decorative arrow at the end of a list row. */
private const val ROW_GLYPH = "›"

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

        SectionTitle(stringResource(R.string.settings_appearance))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Appearance.entries.forEach { appearance ->
                val (label, icon) = appearanceLabelAndIcon(appearance)
                val selected = current.appearance == appearance
                OptionBox(
                    selected = selected,
                    onClick = { viewModel.onAppearance(appearance) },
                    modifier = Modifier.weight(1f),
                ) {
                    // The text below already names the option for TalkBack.
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = optionContentColor(selected),
                        modifier = Modifier.size(32.dp),
                    )
                    Spacer(Modifier.size(4.dp))
                    OptionText(stringResource(label), selected)
                }
            }
        }

        SectionTitle(stringResource(R.string.settings_snooze))
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReminderRules.SNOOZE_OPTIONS.forEach { minutes ->
                val selected = current.snoozeMinutes == minutes
                OptionBox(selected = selected, onClick = { viewModel.onSnoozeMinutes(minutes) }) {
                    OptionText(stringResource(R.string.settings_minutes, minutes), selected)
                }
            }
        }

        SectionTitle(stringResource(R.string.settings_font))
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FontSize.entries.forEach { size ->
                val selected = current.fontSize == size
                val label = when (size) {
                    FontSize.NORMAL -> R.string.font_normal
                    FontSize.LARGE -> R.string.font_large
                    FontSize.XLARGE -> R.string.font_xlarge
                }
                OptionBox(selected = selected, onClick = { viewModel.onFontSize(size) }) {
                    OptionText(stringResource(label), selected)
                }
            }
        }

        // List rows, as in a notebook: a double line on top and a ruled line under each one.
        Column(modifier = Modifier.padding(top = 8.dp)) {
            DoubleRule()
            ListRow(
                label = stringResource(R.string.settings_urgent_hour),
                value = formatTime(current.urgentReminderTime),
                onClick = { showTimePicker = true },
            )
            RowRule()
            ListRow(label = stringResource(R.string.settings_permissions), onClick = onPermissions)
            RowRule()
            ListRow(label = stringResource(R.string.settings_about), onClick = onAbout)
            RowRule()
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

private fun appearanceLabelAndIcon(appearance: Appearance): Pair<Int, Int> = when (appearance) {
    Appearance.LIGHT -> R.string.appearance_light to R.drawable.ic_sun
    Appearance.DARK -> R.string.appearance_dark to R.drawable.ic_moon
    Appearance.SYSTEM -> R.string.appearance_system to R.drawable.ic_phone
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun optionContentColor(selected: Boolean) =
    if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

/**
 * One choice of a group (appearance, snooze minutes, font size). The chosen one has a 2dp green
 * border, a very soft green background and bold text, never just a slightly darker tone.
 * TalkBack reads it as a radio button and says which one is chosen.
 */
@Composable
private fun OptionBox(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.small
    val frame = if (selected) {
        Modifier
            .background(MaterialTheme.colorScheme.primaryContainer, shape)
            .border(2.dp, MaterialTheme.colorScheme.primary, shape)
    } else {
        Modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape)
    }
    Column(
        modifier = modifier
            .heightIn(min = 56.dp)
            .widthIn(min = 56.dp)
            .clip(shape)
            .then(frame)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}

@Composable
private fun OptionText(text: String, selected: Boolean) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        color = optionContentColor(selected),
        textAlign = TextAlign.Center,
    )
}

/** A tappable list row: the label, an optional value in green on the right, and "›". */
@Composable
private fun ListRow(label: String, onClick: () -> Unit, value: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (value != null) {
            Spacer(Modifier.width(12.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            ROW_GLYPH,
            style = MaterialTheme.typography.bodyLarge,
            color = Libreta.colors.muted,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}
