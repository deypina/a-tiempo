package ec.atiempo.app

import android.content.Context
import ec.atiempo.app.data.ATiempoDatabase
import ec.atiempo.app.data.MedicationRepository
import java.time.Clock

/** Manual dependency container, created once in [ATiempoApp]. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val clock: Clock = Clock.systemDefaultZone()

    private val database: ATiempoDatabase by lazy { ATiempoDatabase.build(appContext) }

    val medicationRepository: MedicationRepository by lazy { MedicationRepository(database, clock) }
}
