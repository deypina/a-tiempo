package ec.atiempo.app

import android.content.Context
import ec.atiempo.app.data.ATiempoDatabase
import ec.atiempo.app.data.IntakeRepository
import ec.atiempo.app.data.MedicationRepository
import ec.atiempo.app.data.SettingsRepository
import ec.atiempo.app.reminders.AlarmScheduler
import ec.atiempo.app.reminders.ReminderManager
import ec.atiempo.app.reminders.ReminderNotifier
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** Manual dependency container, created once in [ATiempoApp]. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val clock: Clock = PhoneClock()

    private val database: ATiempoDatabase by lazy { ATiempoDatabase.build(appContext) }

    val medicationRepository: MedicationRepository by lazy { MedicationRepository(database, clock) }

    val intakeRepository: IntakeRepository by lazy { IntakeRepository(database, clock) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }

    val notifier: ReminderNotifier by lazy { ReminderNotifier(appContext) }

    val reminderManager: ReminderManager by lazy {
        ReminderManager(
            medications = medicationRepository,
            intakes = intakeRepository,
            settings = settingsRepository,
            scheduler = AlarmScheduler(appContext, clock),
            notifier = notifier,
            clock = clock,
        )
    }
}

/**
 * System clock that always reads the phone's current time zone. Clock.systemDefaultZone()
 * would keep the zone it had at startup and miss a zone change while the app is running.
 */
private class PhoneClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()
    override fun withZone(zone: ZoneId): Clock = Clock.system(zone)
    override fun instant(): Instant = Instant.now()
}
