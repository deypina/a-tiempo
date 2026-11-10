package ec.atiempo.app.ui.purchase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import ec.atiempo.app.ui.theme.ATiempoTheme

/**
 * Opened by "Ya compré" in a purchase notification. Not exported: only this app's
 * PendingIntent reaches it, and the medication id is checked against the database.
 */
class PurchaseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ATiempoTheme {
                Scaffold { innerPadding ->
                    QuantityEntryScreen(onClose = { finish() }, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
