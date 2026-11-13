package ec.atiempo.app.ui.common

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ec.atiempo.app.R
import ec.atiempo.app.domain.DoseTime
import ec.atiempo.app.domain.Quantity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val appLocale: Locale = Locale.forLanguageTag("es-EC")

/**
 * The one place that formats a time: always 12 h, whatever the phone's 12/24 h setting
 * (docs/ESPECIFICACION_TECNICA.md §5). "7:00 a. m.", "1:25 p. m."; midnight is "12:00 a. m."
 * and noon "12:00 p. m.". The suffix is built here, not taken from the locale, so it never varies.
 */
fun formatTime12(time: LocalTime): String {
    val hour12 = if (time.hour % 12 == 0) 12 else time.hour % 12
    val minutes = time.minute.toString().padStart(2, '0')
    // Non-breaking spaces: "4:12 p. m." never splits across two lines.
    val suffix = if (time.hour < 12) "a.\u00A0m." else "p.\u00A0m."
    return "$hour12:$minutes\u00A0$suffix"
}

/**
 * [formatTime12] split at its first (non-breaking) space: "3:30" and "p. m.", for the hour column of Hoy,
 * where the suffix sits below the time in a small label.
 */
fun splitTime12(time: LocalTime): Pair<String, String> {
    val text = formatTime12(time)
    val space = text.indexOf('\u00A0')
    return text.substring(0, space) to text.substring(space + 1)
}

/** A scheduled time as its two parts, see [splitTime12]. */
fun timeParts(time: DoseTime): Pair<String, String> = splitTime12(LocalTime.of(time.hour, time.minute))

/** "Jueves 12 de noviembre" for the top of Hoy. Spanish names, first letter capitalized. */
fun formatLongDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", appLocale))
        .replaceFirstChar { it.titlecase(appLocale) }

/** A scheduled time, e.g. "9:00 p. m.". */
@Composable
fun formatTime(time: DoseTime): String = formatTime12(LocalTime.of(time.hour, time.minute))

/** Time of an epoch-millis moment in the phone's zone, e.g. "Tomada a las 8:05 a. m.". */
@Composable
fun formatTime(millis: Long): String =
    formatTime12(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime())

/** "mié., 11 nov. 2026" for history lines. */
fun formatDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        .format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", appLocale))

/** Same as [pillQuantity] for code outside Compose (notifications). */
fun pillQuantity(context: Context, value: Double): String {
    val text = Quantity.format(value)
    val res = if (Quantity.isSingular(value)) R.string.qty_pill_one else R.string.qty_pill_other
    return context.getString(res, text)
}

/** "½ pastilla", "1 pastilla", "1½ pastillas". */
@Composable
fun pillQuantity(value: Double): String {
    val text = Quantity.format(value)
    return if (Quantity.isSingular(value)) {
        stringResource(R.string.qty_pill_one, text)
    } else {
        stringResource(R.string.qty_pill_other, text)
    }
}

/** "Quedan 8 pastillas · le alcanza para 8 días". */
@Composable
fun stockLine(stock: Double, daysLeft: Int?): String {
    if (stock <= 0.0) return stringResource(R.string.stock_empty_line)
    val text = Quantity.format(stock)
    val quantity = if (Quantity.isSingular(stock)) {
        stringResource(R.string.stock_qty_one, text)
    } else {
        stringResource(R.string.stock_qty_other, text)
    }
    val days = when {
        daysLeft == null -> return quantity
        daysLeft == 0 -> stringResource(R.string.stock_days_less)
        daysLeft == 1 -> stringResource(R.string.stock_days_one)
        else -> stringResource(R.string.stock_days_many, daysLeft)
    }
    return stringResource(R.string.join_dot, quantity, days)
}

@Composable
fun joinWithDots(parts: List<String>): String {
    var result = parts.firstOrNull() ?: return ""
    for (part in parts.drop(1)) {
        result = stringResource(R.string.join_dot, result, part)
    }
    return result
}
