package ec.atiempo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp

// Sober palette with a green accent. Status colors are always paired with an icon and text.
private val LightColors = lightColorScheme(
    primary = Color(0xFF1B5E20),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF00210B),
    background = Color(0xFFFAFAF7),
    onBackground = Color(0xFF1A1C19),
    surface = Color(0xFFFAFAF7),
    onSurface = Color(0xFF1A1C19),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CD69E),
    onPrimary = Color(0xFF00390F),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFC8E6C9),
    background = Color(0xFF121412),
    onBackground = Color(0xFFE2E3DD),
    surface = Color(0xFF121412),
    onSurface = Color(0xFFE2E3DD),
    error = Color(0xFFF2B8B5),
)

// Base text is 18sp (docs/PRD.md §7). Sizes are in sp, so they follow the system font size.
private val BaseTypography = Typography()
private val ATiempoTypography = BaseTypography.copy(
    bodyLarge = BaseTypography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = BaseTypography.bodyMedium.copy(fontSize = 18.sp, lineHeight = 26.sp),
    labelLarge = BaseTypography.labelLarge.copy(fontSize = 18.sp, lineHeight = 24.sp),
    labelMedium = BaseTypography.labelMedium.copy(fontSize = 16.sp, lineHeight = 22.sp),
    titleLarge = BaseTypography.titleLarge.copy(fontSize = 24.sp, lineHeight = 32.sp),
)

/**
 * [fontScale] is "Tamaño de letra" from Ajustes. It multiplies the phone's own font size,
 * so a large system font and "Muy grande" add up (C-27).
 */
@Composable
fun ATiempoTheme(fontScale: Float = 1f, content: @Composable () -> Unit) {
    val theme: @Composable () -> Unit = {
        MaterialTheme(
            colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
            typography = ATiempoTypography,
            content = content,
        )
    }
    // At "Normal" the phone's own density is kept untouched (Android 14+ scales big fonts non-linearly).
    if (fontScale == 1f) {
        theme()
        return
    }
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale * fontScale), content = theme)
}
