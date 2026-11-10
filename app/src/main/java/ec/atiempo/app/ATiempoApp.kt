package ec.atiempo.app

import android.app.Application

class ATiempoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Idempotent and cheap: channels must exist before the first reminder.
        container.notifier.createChannels()
    }
}
