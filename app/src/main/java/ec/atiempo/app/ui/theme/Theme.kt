package ec.atiempo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// "Cuaderno cálido": paper background, near-black ink, one pharmacy-green accent, hairlines
// instead of shadows. Containers match the paper so nothing looks tinted or floating.
// Status colors are always paired with text (docs/PRD.md §7).
private val Paper = Color(0xFFF5EFE3)
private val Ink = Color(0xFF1E1A15)
private val NightPaper = Color(0xFF17140F)
private val NightInk = Color(0xFFECE4D6)

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E5A44),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE8DF),
    onPrimaryContainer = Color(0xFF0F2A1D),
    secondaryContainer = Color(0xFFE6DCC8),
    onSecondaryContainer = Ink,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEDE5D5),
    onSurfaceVariant = Color(0xFF5A5145),
    surfaceContainerLowest = Paper,
    surfaceContainerLow = Paper,
    surfaceContainer = Paper,
    surfaceContainerHigh = Color(0xFFF9F5EC),
    surfaceContainerHighest = Color(0xFFEDE5D5),
    outline = Color(0xFF8A7F6E),
    outlineVariant = Color(0xFFD8CDB8),
    error = Color(0xFFA13A22),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF6DDD3),
    onErrorContainer = Color(0xFF5C1A0B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CC9AE),
    onPrimary = Color(0xFF0D2A1C),
    primaryContainer = Color(0xFF23402F),
    onPrimaryContainer = Color(0xFFCDE6D6),
    secondaryContainer = Color(0xFF3A3328),
    onSecondaryContainer = NightInk,
    background = NightPaper,
    onBackground = NightInk,
    surface = NightPaper,
    onSurface = NightInk,
    surfaceVariant = Color(0xFF2A251E),
    onSurfaceVariant = Color(0xFFBDB2A1),
    surfaceContainerLowest = NightPaper,
    surfaceContainerLow = NightPaper,
    surfaceContainer = NightPaper,
    surfaceContainerHigh = Color(0xFF211D17),
    surfaceContainerHighest = Color(0xFF2A251E),
    outline = Color(0xFF8C8170),
    outlineVariant = Color(0xFF3B342A),
    error = Color(0xFFF0A58E),
    onError = Color(0xFF4A1D10),
    errorContainer = Color(0xFF4A1D10),
    onErrorContainer = Color(0xFFF6DDD3),
)

// Almost square corners: a notebook, not pills.
private val ATiempoShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(12.dp),
)

// Base text is 18sp (docs/PRD.md §7). Sizes are in sp, so they follow the system font size.
// Titles use the phone's serif font (nothing is downloaded); body text stays sans for reading.
private val BaseTypography = Typography()
private val ATiempoTypography = BaseTypography.copy(
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = FontFamily.Serif),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = FontFamily.Serif),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = FontFamily.Serif),
    bodyLarge = BaseTypography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = BaseTypography.bodyMedium.copy(fontSize = 18.sp, lineHeight = 26.sp),
    labelLarge = BaseTypography.labelLarge.copy(fontSize = 18.sp, lineHeight = 24.sp),
    labelMedium = BaseTypography.labelMedium.copy(fontSize = 16.sp, lineHeight = 22.sp),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = FontFamily.Serif, fontSize = 24.sp, lineHeight = 32.sp),
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
            shapes = ATiempoShapes,
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
