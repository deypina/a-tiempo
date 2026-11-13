package ec.atiempo.app.ui.theme

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ec.atiempo.app.R
import ec.atiempo.app.domain.Appearance

// "Libreta" (docs/ESPECIFICACION_TECNICA.md §5): ruled paper, near-black ink, one pharmacy-green
// accent, hairlines instead of shadows. Containers match the paper so nothing looks tinted or floating.
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

/**
 * Notebook colors Material has no slot for. Every text color here has at least 4.5:1 against
 * its paper (light: ocre 5.2, muted 5.3; night: ocre 9.4, muted 6.8).
 */
@Immutable
data class LibretaColors(
    /** "Quedan pocas". */
    val ocre: Color,
    /** 1dp line between rows. */
    val rule: Color,
    /** Double line above each list and the bottom bar. */
    val doubleLine: Color,
    /** Warm grey for secondary text and unselected tabs. */
    val muted: Color,
)

private val LightLibreta = LibretaColors(
    ocre = Color(0xFF8A5A12),
    rule = Color(0xFFC9BBA3),
    doubleLine = Ink,
    muted = Color(0xFF6B6058),
)

private val NightLibreta = LibretaColors(
    ocre = Color(0xFFE3B25E),
    rule = Color(0xFF3F3933),
    doubleLine = NightInk,
    muted = Color(0xFFA89C8A),
)

val LocalLibretaColors = staticCompositionLocalOf { LightLibreta }

/** Access to the notebook extras, e.g. `Libreta.colors.ocre`. */
object Libreta {
    val colors: LibretaColors
        @Composable get() = LocalLibretaColors.current
}

// Almost square corners: a notebook, not pills. 6dp on buttons and fields.
private val ATiempoShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(12.dp),
)

// Bundled fonts (SIL OFL 1.1, licenses/fonts/): nothing is downloaded and there is no INTERNET.
// Literata is a variable font; each weight picks its point on the "wght" axis.
@OptIn(ExperimentalTextApi::class)
private val Literata = FontFamily(
    Font(R.font.literata, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.literata, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.literata, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.literata, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

// Atkinson Hyperlegible: made for low vision readers.
private val Atkinson = FontFamily(
    Font(R.font.atkinson_regular, FontWeight.Normal),
    Font(R.font.atkinson_bold, FontWeight.Bold),
)

// Reading text is at least 18sp (docs/PRD.md §7); only short labels use 15–16sp.
// Sizes are in sp, so they follow the system font size. Titles in Literata, text in Atkinson.
private val BaseTypography = Typography()
private val ATiempoTypography = BaseTypography.copy(
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = Literata, fontWeight = FontWeight.SemiBold),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = Literata, fontWeight = FontWeight.SemiBold),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = Literata, fontWeight = FontWeight.SemiBold),
    titleLarge = BaseTypography.titleLarge.copy(
        fontFamily = Literata,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = Literata, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 28.sp),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = Literata, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 26.sp),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = Atkinson, fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = Atkinson, fontSize = 18.sp, lineHeight = 26.sp),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = Atkinson, fontSize = 16.sp, lineHeight = 22.sp),
    labelLarge = BaseTypography.labelLarge.copy(fontFamily = Atkinson, fontSize = 18.sp, lineHeight = 24.sp),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = Atkinson, fontSize = 16.sp, lineHeight = 22.sp),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = Atkinson, fontSize = 15.sp, lineHeight = 20.sp),
)

/**
 * [fontScale] is "Tamaño de letra" from Ajustes. It multiplies the phone's own font size,
 * so a large system font and "Muy grande" add up (C-27).
 * [appearance] is "Apariencia": Claro unless the person chose otherwise, even if the phone is dark.
 */
@Composable
fun ATiempoTheme(
    fontScale: Float = 1f,
    appearance: Appearance = Appearance.LIGHT,
    content: @Composable () -> Unit,
) {
    val dark = when (appearance) {
        Appearance.LIGHT -> false
        Appearance.DARK -> true
        Appearance.SYSTEM -> isSystemInDarkTheme()
    }
    SystemBarIcons(dark)
    val theme: @Composable () -> Unit = {
        CompositionLocalProvider(LocalLibretaColors provides if (dark) NightLibreta else LightLibreta) {
            MaterialTheme(
                colorScheme = if (dark) DarkColors else LightColors,
                typography = ATiempoTypography,
                shapes = ATiempoShapes,
                content = content,
            )
        }
    }
    // At "Normal" the phone's own density is kept untouched (Android 14+ scales big fonts non-linearly).
    if (fontScale == 1f) {
        theme()
        return
    }
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale * fontScale), content = theme)
}

/** Dark icons on light paper, light icons on night paper, following the chosen appearance. */
@Composable
private fun SystemBarIcons(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val activity = view.context.findComponentActivity() ?: return
    DisposableEffect(activity, dark) {
        val transparent = android.graphics.Color.TRANSPARENT
        val style = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
        activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        onDispose { }
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}
