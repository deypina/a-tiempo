package ec.atiempo.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import ec.atiempo.app.R
import ec.atiempo.app.reminders.ReminderPermissions

/** "Revisar permisos y batería" (docs/PRD.md F9): only what is still missing, rechecked on return. */
@Composable
fun PermissionsScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var notificationsAllowed by remember { mutableStateOf(true) }
    var exactAllowed by remember { mutableStateOf(true) }
    var batteryOk by remember { mutableStateOf(true) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsAllowed = ReminderPermissions.notificationsAllowed(context)
        exactAllowed = ReminderPermissions.exactAlarmsAllowed(context)
        batteryOk = ReminderPermissions.batteryUnrestricted(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconButton(onClick = onClose, modifier = Modifier.size(56.dp)) {
            Icon(painter = painterResource(R.drawable.ic_back), contentDescription = stringResource(R.string.common_back))
        }
        Text(stringResource(R.string.settings_permissions), style = MaterialTheme.typography.headlineMedium)

        if (notificationsAllowed && exactAllowed && batteryOk) {
            Text(stringResource(R.string.perm_all_ok), style = MaterialTheme.typography.bodyLarge)
        }
        if (!notificationsAllowed) {
            PermissionItem(R.string.perm_notif_title, R.string.perm_notif_body) {
                ReminderPermissions.openNotificationSettings(context)
            }
        }
        if (!exactAllowed) {
            PermissionItem(R.string.perm_alarm_title, R.string.perm_alarm_body) {
                ReminderPermissions.openExactAlarmSettings(context)
            }
        }
        if (!batteryOk) {
            PermissionItem(R.string.perm_battery_title, R.string.perm_battery_body) {
                ReminderPermissions.openBatterySettings(context)
            }
        }
        // Samsung can still put the app to sleep with every permission granted.
        Text(stringResource(R.string.perm_battery_steps), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PermissionItem(title: Int, body: Int, onAllow: () -> Unit) {
    Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
    Text(stringResource(body), style = MaterialTheme.typography.bodyLarge)
    Button(
        onClick = onAllow,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
    ) {
        Text(stringResource(R.string.perm_allow))
    }
}
