package ec.atiempo.app.ui.welcome

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import ec.atiempo.app.R
import ec.atiempo.app.data.SettingsRepository
import ec.atiempo.app.reminders.ReminderPermissions
import ec.atiempo.app.ui.common.AppViewModelFactory

/** Welcome flow (docs/PRD.md F1): name, then only the permissions still missing. */
enum class WelcomeStep { NAME, NOTIFICATIONS, EXACT_ALARM, BATTERY }

private fun WelcomeStep.isNeeded(context: Context): Boolean = when (this) {
    WelcomeStep.NAME -> true
    WelcomeStep.NOTIFICATIONS ->
        ReminderPermissions.notificationPermissionNeeded && !ReminderPermissions.notificationsAllowed(context)
    WelcomeStep.EXACT_ALARM -> !ReminderPermissions.exactAlarmsAllowed(context)
    WelcomeStep.BATTERY -> !ReminderPermissions.batteryUnrestricted(context)
}

private fun nextStep(after: WelcomeStep, context: Context): WelcomeStep? =
    WelcomeStep.entries.firstOrNull { it.ordinal > after.ordinal && it.isNeeded(context) }

private val buttonModifier = Modifier
    .fillMaxWidth()
    .heightIn(min = 56.dp)

@Composable
fun WelcomeScreen(viewModel: WelcomeViewModel = viewModel(factory = AppViewModelFactory.instance)) {
    val context = LocalContext.current
    var step by rememberSaveable { mutableStateOf(WelcomeStep.NAME) }
    var name by rememberSaveable { mutableStateOf("") }
    // True while the phone's settings screen is open, so we move on when the person comes back.
    var waitingForSettings by rememberSaveable { mutableStateOf(false) }

    fun advance() {
        val next = nextStep(step, context)
        if (next == null) viewModel.finish() else step = next
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { advance() }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (waitingForSettings) {
            waitingForSettings = false
            advance()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (step) {
                WelcomeStep.NAME -> NameStep(
                    name = name,
                    onNameChange = { name = it.take(SettingsRepository.MAX_NAME_LENGTH) },
                    onContinue = {
                        viewModel.saveName(name)
                        advance()
                    },
                )
                WelcomeStep.NOTIFICATIONS -> PermissionStep(
                    title = stringResource(R.string.perm_notif_title),
                    body = stringResource(R.string.perm_notif_body),
                    onAllow = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    onLater = { advance() },
                )
                WelcomeStep.EXACT_ALARM -> PermissionStep(
                    title = stringResource(R.string.perm_alarm_title),
                    body = stringResource(R.string.perm_alarm_body),
                    onAllow = {
                        waitingForSettings = true
                        ReminderPermissions.openExactAlarmSettings(context)
                    },
                    onLater = { advance() },
                )
                WelcomeStep.BATTERY -> PermissionStep(
                    title = stringResource(R.string.perm_battery_title),
                    body = stringResource(R.string.perm_battery_body),
                    extra = stringResource(R.string.perm_battery_steps),
                    onAllow = {
                        waitingForSettings = true
                        ReminderPermissions.openBatterySettings(context)
                    },
                    onLater = { advance() },
                )
            }
        }
    }
}

@Composable
private fun NameStep(name: String, onNameChange: (String) -> Unit, onContinue: () -> Unit) {
    Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.headlineMedium)
    Text(stringResource(R.string.welcome_body), style = MaterialTheme.typography.bodyLarge)
    Text(stringResource(R.string.welcome_name_question), style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        placeholder = { Text(stringResource(R.string.welcome_name_hint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
    Button(onClick = onContinue, modifier = buttonModifier) {
        Text(stringResource(R.string.welcome_continue))
    }
    Text(stringResource(R.string.disclaimer), style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun PermissionStep(
    title: String,
    body: String,
    onAllow: () -> Unit,
    onLater: () -> Unit,
    extra: String? = null,
) {
    Text(title, style = MaterialTheme.typography.headlineMedium)
    Text(body, style = MaterialTheme.typography.bodyLarge)
    Button(onClick = onAllow, modifier = buttonModifier) {
        Text(stringResource(R.string.perm_allow))
    }
    OutlinedButton(onClick = onLater, modifier = buttonModifier) {
        Text(stringResource(R.string.perm_later))
    }
    if (extra != null) {
        Text(extra, style = MaterialTheme.typography.bodyMedium)
    }
}
