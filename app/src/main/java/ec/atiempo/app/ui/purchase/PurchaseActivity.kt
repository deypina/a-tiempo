package ec.atiempo.app.ui.purchase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ec.atiempo.app.ATiempoApp
import ec.atiempo.app.ui.theme.ATiempoTheme

/**
 * Opened by "Ya compré" in a purchase notification. Not exported: only this app's
 * PendingIntent reaches it, and the medication id is checked against the database.
 */
class PurchaseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = (application as ATiempoApp).container.settingsRepository.settings
        setContent {
            val current by settings.collectAsStateWithLifecycle(initialValue = null)
            ATiempoTheme(fontScale = current?.fontSize?.scale ?: 1f) {
                Scaffold { innerPadding ->
                    QuantityEntryScreen(onClose = { finish() }, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
