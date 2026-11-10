package ec.atiempo.app.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ec.atiempo.app.R
import ec.atiempo.app.reminders.ReminderPermissions
import ec.atiempo.app.ui.common.AppViewModelFactory

@Composable
fun TodayScreen(viewModel: TodayViewModel = viewModel(factory = AppViewModelFactory.instance)) {
    val context = LocalContext.current
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    var notificationsAllowed by remember { mutableStateOf(true) }
    var exactAllowed by remember { mutableStateOf(true) }
    var batteryOk by remember { mutableStateOf(true) }

    // Re-checked every time the screen comes back, e.g. after the phone's settings.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsAllowed = ReminderPermissions.notificationsAllowed(context)
        exactAllowed = ReminderPermissions.exactAlarmsAllowed(context)
        batteryOk = ReminderPermissions.batteryUnrestricted(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val greeting = if (userName.isBlank()) {
            stringResource(R.string.today_greeting_no_name)
        } else {
            stringResource(R.string.today_greeting, userName)
        }
        Text(greeting, style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.today_subtitle), style = MaterialTheme.typography.bodyLarge)

        if (!notificationsAllowed) {
            WarningCard(stringResource(R.string.warn_notif_denied)) {
                ReminderPermissions.openNotificationSettings(context)
            }
        }
        if (!exactAllowed) {
            WarningCard(stringResource(R.string.warn_exact_denied)) {
                ReminderPermissions.openExactAlarmSettings(context)
            }
        }
        if (!batteryOk) {
            WarningCard(stringResource(R.string.warn_battery)) {
                ReminderPermissions.openBatterySettings(context)
            }
        }
    }
}

@Composable
private fun WarningCard(text: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(16.dp))
    }
}
