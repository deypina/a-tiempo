package ec.atiempo.app.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ec.atiempo.app.MainActivity
import ec.atiempo.app.R
import ec.atiempo.app.domain.IntakeEvent
import ec.atiempo.app.domain.Medication
import ec.atiempo.app.domain.Quantity
import ec.atiempo.app.ui.common.formatTime
import ec.atiempo.app.ui.common.pillQuantity
import java.time.Instant
import java.time.ZoneId

/**
 * Channels and notifications for intakes. Texts come from docs/TEXTOS_UI.md.
 * Intake notifications are public on the lock screen (docs/PRD.md F3), so they show the medication name.
 * Nothing here is logged.
 */
class ReminderNotifier(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    fun createChannels() {
        val system = context.getSystemService(NotificationManager::class.java)
        val intake = NotificationChannel(
            CHANNEL_INTAKE,
            context.getString(R.string.channel_intake_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_intake_desc)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val purchase = NotificationChannel(
            CHANNEL_PURCHASE,
            context.getString(R.string.channel_purchase_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_purchase_desc)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val info = NotificationChannel(
            CHANNEL_INFO,
            context.getString(R.string.channel_info_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.channel_info_desc)
        }
        system.createNotificationChannels(listOf(intake, purchase, info))
    }

    /** "{nombre}, es hora de su {medicamento}" with "Ya la tomé" and "Recordar en X min". */
    fun showIntake(medication: Medication, event: IntakeEvent, userName: String, snoozeMinutes: Int, isRepeat: Boolean) {
        val name = userName.trim()
        val title = when {
            isRepeat && name.isNotEmpty() -> context.getString(R.string.notif_repeat_title, name, medication.name)
            isRepeat -> context.getString(R.string.notif_repeat_title_no_name, medication.name)
            name.isNotEmpty() -> context.getString(R.string.notif_intake_title, name, medication.name)
            else -> context.getString(R.string.notif_intake_title_no_name, medication.name)
        }
        val quantity = pillQuantity(context, medication.dosePerIntake)
        val body = if (medication.stock <= 0.0) {
            context.getString(R.string.notif_intake_body_nostock, quantity)
        } else {
            context.getString(R.string.notif_intake_body, quantity, medication.note.orEmpty()).trim()
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_INTAKE)
            .setSmallIcon(R.drawable.ic_tab_today)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setWhen(event.scheduledAt)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setContentIntent(openTodayIntent(event.scheduleId))
            .addAction(0, context.getString(R.string.notif_action_taken), actionIntent(NotificationActionReceiver.ACTION_TAKEN, event))
            .addAction(
                0,
                context.getString(R.string.notif_action_snooze, snoozeMinutes),
                actionIntent(NotificationActionReceiver.ACTION_SNOOZE, event),
            )
            .build()
        post(intakeId(event.scheduleId), notification)
    }

    /** Quiet "No confirmó su toma de {medicamento} de las {hora}." */
    fun showMissed(medication: Medication, event: IntakeEvent) {
        val time = Instant.ofEpochMilli(event.scheduledAt).atZone(ZoneId.systemDefault()).toLocalTime()
        val text = context.getString(R.string.notif_missed, medication.name, formatTime(context, time))
        val notification = NotificationCompat.Builder(context, CHANNEL_INFO)
            .setSmallIcon(R.drawable.ic_tab_today)
            .setContentTitle(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openTodayIntent(event.scheduleId))
            .build()
        post(missedId(event.scheduleId), notification)
    }

    fun cancelIntake(scheduleId: Long) = manager.cancel(intakeId(scheduleId))

    fun cancelAllFor(scheduleId: Long) {
        manager.cancel(intakeId(scheduleId))
        manager.cancel(missedId(scheduleId))
    }

    /** "Listo. Le quedan 7 pastillas de {medicamento}." after "Ya la tomé". */
    fun showTakenToast(medication: Medication) {
        val n = Quantity.format(medication.stock)
        val text = when {
            medication.stock <= 0.0 -> context.getString(R.string.notif_taken_toast_empty, medication.name)
            Quantity.isSingular(medication.stock) -> context.getString(R.string.notif_taken_toast_one, n, medication.name)
            else -> context.getString(R.string.notif_taken_toast, n, medication.name)
        }
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun post(id: Int, notification: Notification) {
        if (!manager.areNotificationsEnabled()) return
        try {
            notifyChecked(id, notification)
        } catch (_: SecurityException) {
            // Notifications were turned off between the check and the call.
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun notifyChecked(id: Int, notification: Notification) = manager.notify(id, notification)

    private fun openTodayIntent(scheduleId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            AlarmScheduler.dailyCode(scheduleId),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun actionIntent(action: String, event: IntakeEvent): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java)
            .setAction(action)
            .putExtra(AlarmScheduler.EXTRA_EVENT_ID, event.id)
        return PendingIntent.getBroadcast(
            context,
            AlarmScheduler.dailyCode(event.scheduleId),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        const val CHANNEL_INTAKE = "tomas"
        const val CHANNEL_PURCHASE = "compras"
        const val CHANNEL_INFO = "info"

        /** Notification ids per schedule. Purchase alerts (phase 4) use their own range. */
        fun intakeId(scheduleId: Long): Int = scheduleId.toInt()
        fun missedId(scheduleId: Long): Int = MISSED_ID_OFFSET + scheduleId.toInt()

        private const val MISSED_ID_OFFSET = 200_000
    }
}
