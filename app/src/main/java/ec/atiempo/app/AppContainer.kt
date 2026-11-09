package ec.atiempo.app

import android.content.Context

/**
 * Manual dependency container, created once in [ATiempoApp].
 * Repositories, the database and the alarm scheduler are added here in later phases.
 */
class AppContainer(
    @Suppress("unused") private val context: Context,
)
