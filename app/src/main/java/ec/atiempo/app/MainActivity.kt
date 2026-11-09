package ec.atiempo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ec.atiempo.app.ui.ATiempoRoot
import ec.atiempo.app.ui.theme.ATiempoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ATiempoTheme {
                ATiempoRoot()
            }
        }
    }
}
