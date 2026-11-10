package ec.atiempo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import ec.atiempo.app.data.Settings
import ec.atiempo.app.ui.ATiempoRoot
import ec.atiempo.app.ui.theme.ATiempoTheme
import ec.atiempo.app.ui.welcome.WelcomeScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val container get() = (application as ATiempoApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings: Flow<Settings?> = container.settingsRepository.settings.map { it }
        setContent {
            ATiempoTheme {
                val current by settings.collectAsStateWithLifecycle(initialValue = null)
                val loaded = current
                when {
                    // Brief blank frame while preferences load, so the welcome never flashes.
                    loaded == null -> Surface(Modifier.fillMaxSize()) { Box(Modifier.fillMaxSize()) }
                    !loaded.onboardingDone -> WelcomeScreen()
                    else -> ATiempoRoot()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Also covers a force stop, which deletes alarms without telling the app.
        lifecycleScope.launch { container.reminderManager.rescheduleAll() }
    }
}
