package ec.atiempo.app.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ec.atiempo.app.R
import ec.atiempo.app.domain.IntakeStatus
import ec.atiempo.app.domain.StockLevel
import ec.atiempo.app.domain.StockNotice
import ec.atiempo.app.domain.TodayIntake
import ec.atiempo.app.reminders.ReminderPermissions
import ec.atiempo.app.ui.common.AppViewModelFactory
import ec.atiempo.app.ui.common.Button
import ec.atiempo.app.ui.common.DoubleRule
import ec.atiempo.app.ui.common.LinkButton
import ec.atiempo.app.ui.common.RowRule
import ec.atiempo.app.ui.common.StockLineText
import ec.atiempo.app.ui.common.formatLongDate
import ec.atiempo.app.ui.common.formatTime
import ec.atiempo.app.ui.common.pillQuantity
import ec.atiempo.app.ui.common.timeParts
import ec.atiempo.app.ui.theme.ColorDot
import ec.atiempo.app.ui.theme.Libreta

/** Glyph before "Tomada a las…"; the words say it too. */
private const val CHECK_GLYPH = "✓ "

@Composable
fun TodayScreen(
    onPurchase: (Long) -> Unit,
    viewModel: TodayViewModel = viewModel(factory = AppViewModelFactory.instance),
) {
    val context = LocalContext.current
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var notificationsAllowed by remember { mutableStateOf(true) }
    var exactAllowed by remember { mutableStateOf(true) }
    var batteryOk by remember { mutableStateOf(true) }

    // Re-checked every time the screen comes back, e.g. after the phone's settings.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsAllowed = ReminderPermissions.notificationsAllowed(context)
        exactAllowed = ReminderPermissions.exactAlarmsAllowed(context)
        batteryOk = ReminderPermissions.batteryUnrestricted(context)
        viewModel.onResume()
    }

    val current = state
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Greeting: the date small on top, then "Hola, [nombre]" in Literata.
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (current is TodayUiState.Ready) {
                Text(
                    formatLongDate(current.date),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Libreta.colors.muted,
                )
            }
            val greeting = if (userName.isBlank()) {
                stringResource(R.string.today_greeting_no_name)
            } else {
                stringResource(R.string.today_greeting, userName)
            }
            Text(greeting, style = MaterialTheme.typography.headlineLarge)
        }

        if (!notificationsAllowed) {
            PermissionNotice(stringResource(R.string.warn_notif_denied)) {
                ReminderPermissions.openNotificationSettings(context)
            }
        }
        if (!exactAllowed) {
            PermissionNotice(stringResource(R.string.warn_exact_denied)) {
                ReminderPermissions.openExactAlarmSettings(context)
            }
        }
        if (!batteryOk) {
            PermissionNotice(stringResource(R.string.warn_battery)) {
                ReminderPermissions.openBatterySettings(context)
            }
        }

        if (current is TodayUiState.Ready) {
            current.notices.forEach { notice ->
                StockNoticeBlock(notice, onPurchase = { onPurchase(notice.medication.id) })
            }

            if (!current.hasMedications) {
                Text(stringResource(R.string.today_empty), style = MaterialTheme.typography.bodyLarge)
            }

            val plan = current.plan
            if (plan.next != null || plan.rest.isNotEmpty()) {
                Column {
                    DoubleRule()
                    plan.next?.let { next ->
                        NextIntakeRow(
                            item = next,
                            late = plan.nextIsLate,
                            snoozeMinutes = current.snoozeMinutes.takeIf { plan.canSnoozeNext },
                            onTaken = { viewModel.onTaken(next) },
                            onSnooze = { viewModel.onSnooze(next) },
                        )
                        RowRule()
                    }
                    plan.rest.forEach { item ->
                        CompactIntakeRow(item, onUndo = { viewModel.onUndo(item) })
                        RowRule()
                    }
                }
            }
        }
    }
}

/**
 * One row of the list: the hour in a left column with a thin vertical line, details on the right.
 * The column is at least 78dp and grows with a large font, so the hour is never clipped.
 */
@Composable
private fun HourRow(
    item: TodayIntake,
    hourColor: Color,
    label: (@Composable () -> Unit)?,
    content: @Composable () -> Unit,
) {
    val (time, suffix) = timeParts(item.schedule.time)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 78.dp)
                .padding(top = 14.dp, bottom = 14.dp, end = 12.dp),
        ) {
            Text(time, style = MaterialTheme.typography.titleLarge, color = hourColor, softWrap = false)
            Text(suffix, style = MaterialTheme.typography.labelMedium, color = Libreta.colors.muted, softWrap = false)
            label?.invoke()
        }
        Spacer(
            Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(Libreta.colors.rule),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            content()
        }
    }
}

/** The next intake: first and large, with "Ya la tomé" and, when allowed, "Recordar en X minutos". */
@Composable
private fun NextIntakeRow(
    item: TodayIntake,
    late: Boolean,
    snoozeMinutes: Int?,
    onTaken: () -> Unit,
    onSnooze: () -> Unit,
) {
    val med = item.medication
    val labelColor = if (late) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    HourRow(
        item = item,
        hourColor = labelColor,
        label = {
            Text(
                stringResource(if (late) R.string.today_late else R.string.today_now),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = labelColor,
                softWrap = false,
            )
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorDot(med.colorKey, 20.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                med.name,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp, lineHeight = 32.sp),
            )
        }
        Text(pillQuantity(med.dosePerIntake), style = MaterialTheme.typography.bodyLarge)
        StockLineText(med, item.scheduleCount, okColor = Libreta.colors.muted)
        when (item.status) {
            IntakeStatus.SNOOZED -> item.snoozedUntil?.let {
                Text(
                    stringResource(R.string.status_snoozed, formatTime(it)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Libreta.colors.muted,
                )
            }
            IntakeStatus.MISSED -> Text(
                stringResource(R.string.status_missed),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
            else -> Unit
        }
        Button(
            onClick = onTaken,
            modifier = Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
                .heightIn(min = 60.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.btn_taken), style = MaterialTheme.typography.labelLarge)
        }
        if (snoozeMinutes != null) {
            LinkButton(text = stringResource(R.string.today_snooze, snoozeMinutes), onClick = onSnooze)
        }
    }
}

/** Any other intake of the day, compact: name and status; a taken one offers "Deshacer". */
@Composable
private fun CompactIntakeRow(item: TodayIntake, onUndo: () -> Unit) {
    val med = item.medication
    HourRow(item = item, hourColor = MaterialTheme.colorScheme.onSurface, label = null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorDot(med.colorKey, 16.dp)
            Spacer(Modifier.width(10.dp))
            Text(med.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
        if (item.status == IntakeStatus.TAKEN) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val takenAt = item.takenAt?.let { formatTime(it) } ?: formatTime(item.schedule.time)
                Text(
                    CHECK_GLYPH + stringResource(R.string.status_taken, takenAt),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                LinkButton(text = stringResource(R.string.btn_undo), onClick = onUndo)
            }
        } else {
            Text(
                statusText(item),
                style = MaterialTheme.typography.bodyLarge,
                color = if (item.status == IntakeStatus.MISSED) MaterialTheme.colorScheme.error else Libreta.colors.muted,
            )
        }
    }
}

@Composable
private fun statusText(item: TodayIntake): String {
    val snoozedUntil = item.snoozedUntil
    return when {
        item.status == IntakeStatus.SNOOZED && snoozedUntil != null ->
            stringResource(R.string.status_snoozed, formatTime(snoozedUntil))
        item.status == IntakeStatus.MISSED -> stringResource(R.string.status_missed)
        else -> stringResource(R.string.status_pending)
    }
}

/**
 * "IMPORTANTE" block: no box and no tinted background. The small word in brick, then the message
 * with its key part bold and colored, then the action as an underlined link.
 */
@Composable
private fun ImportantBlock(message: AnnotatedString, action: String?, actionColor: Color, onAction: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            stringResource(R.string.today_important),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = MaterialTheme.colorScheme.error,
        )
        if (message.isNotEmpty()) {
            Text(message, style = MaterialTheme.typography.bodyLarge)
        }
        if (action != null) {
            LinkButton(text = action, onClick = onAction, color = actionColor)
        }
    }
}

/** "Ya no le queda X." with "Registrar la compra", or "Le quedan pocas pastillas de X.". */
@Composable
private fun StockNoticeBlock(notice: StockNotice, onPurchase: () -> Unit) {
    val name = notice.medication.name
    val out = notice.level == StockLevel.OUT
    val text = stringResource(if (out) R.string.important_out else R.string.important_low, name)
    val keyColor = if (out) MaterialTheme.colorScheme.error else Libreta.colors.ocre
    ImportantBlock(
        message = highlight(text, name, keyColor),
        action = if (out) stringResource(R.string.important_register) else null,
        actionColor = MaterialTheme.colorScheme.error,
        onAction = onPurchase,
    )
}

/**
 * A permission warning as an IMPORTANTE block. Its first sentence is the key part; the rest
 * ("Toque aquí para…") becomes the underlined action that opens the phone's settings.
 */
@Composable
private fun PermissionNotice(text: String, onClick: () -> Unit) {
    val split = text.indexOf(". ")
    val (problem, action) = if (split > 0) {
        text.substring(0, split + 1) to text.substring(split + 2)
    } else {
        "" to text
    }
    val error = MaterialTheme.colorScheme.error
    ImportantBlock(
        message = highlight(problem, problem, error),
        action = action,
        actionColor = error,
        onAction = onClick,
    )
}

/** [text] with its first [key] in bold and [color]. */
private fun highlight(text: String, key: String, color: Color): AnnotatedString = buildAnnotatedString {
    val start = if (key.isEmpty()) -1 else text.indexOf(key)
    if (start < 0) {
        append(text)
        return@buildAnnotatedString
    }
    append(text.substring(0, start))
    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = color)) { append(key) }
    append(text.substring(start + key.length))
}
