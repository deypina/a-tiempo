package ec.atiempo.app.ui.theme

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ec.atiempo.app.R
import ec.atiempo.app.domain.MedicationColors

/** Light and dark versions of one medication color (instrucciones-estilo-libreta.md §2). */
private data class MedicationColorValues(val light: Color, val dark: Color, @param:StringRes val name: Int)

private val VALUES: Map<String, MedicationColorValues> = mapOf(
    "azul" to MedicationColorValues(Color(0xFF3D6A8E), Color(0xFF7FA6C8), R.string.color_azul),
    "rosa" to MedicationColorValues(Color(0xFFB0566B), Color(0xFFCF8396), R.string.color_rosa),
    "mostaza" to MedicationColorValues(Color(0xFFC99A2E), Color(0xFFD9B158), R.string.color_mostaza),
    "oliva" to MedicationColorValues(Color(0xFF6E7F3A), Color(0xFFA3B26A), R.string.color_oliva),
    "ciruela" to MedicationColorValues(Color(0xFF7A5A8C), Color(0xFFA98BBA), R.string.color_ciruela),
    "blanco" to MedicationColorValues(Color(0xFFFFFDF8), Color(0xFFECE4D6), R.string.color_blanco),
)

private fun values(key: String): MedicationColorValues =
    VALUES.getValue(MedicationColors.normalize(key))

/** True when the app is showing its dark (night) paper. */
@Composable
private fun isDarkPaper(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** Fill color of a medication, in the version that fits the current light or dark paper. */
@Composable
fun medicationColor(key: String): Color {
    val values = values(key)
    return if (isDarkPaper()) values.dark else values.light
}

/** Spoken and written name of a medication color, e.g. "Azul". */
@StringRes
fun medicationColorName(key: String): Int = values(key).name

/** "Blanco" is close to the paper, so it always gets an outline to stay visible. */
fun needsOutline(key: String): Boolean = MedicationColors.normalize(key) == "blanco"

/**
 * Decorative filled circle of a medication's color. No semantics: the medication's name
 * next to it already says which one it is, and the color is never the only signal.
 */
@Composable
fun ColorDot(key: String, size: Dp, modifier: Modifier = Modifier) {
    val outlined = if (needsOutline(key)) {
        Modifier.border(1.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .size(size)
            .background(medicationColor(key), CircleShape)
            .then(outlined),
    )
}
