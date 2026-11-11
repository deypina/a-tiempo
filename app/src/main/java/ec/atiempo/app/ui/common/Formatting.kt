package ec.atiempo.app.ui.common

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import ec.atiempo.app.R
import ec.atiempo.app.domain.DoseTime
import ec.atiempo.app.domain.Quantity
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val appLocale: Locale = Locale.forLanguageTag("es-EC")

/** "9:00 p. m." or "21:00", following the phone's 12/24-hour setting. */
@Composable
fun formatTime(time: DoseTime): String {
    val pattern = if (DateFormat.is24HourFormat(LocalContext.current)) "HH:mm" else "h:mm a"
    return LocalTime.of(time.hour, time.minute).format(DateTimeFormatter.ofPattern(pattern, appLocale))
}

/** Time of an epoch-millis moment in the phone's zone, e.g. "Tomada a las 8:05 a. m.". */
@Composable
fun formatTime(millis: Long): String {
    val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
    return formatTime(LocalContext.current, time)
}

/** "mié., 11 nov. 2026" for history lines. */
fun formatDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        .format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", appLocale))

/** Same as [formatTime] for code outside Compose (notifications). */
fun formatTime(context: Context, time: LocalTime): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    return time.format(DateTimeFormatter.ofPattern(pattern, appLocale))
}

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

/** "Le quedan 8 pastillas · le alcanza para 8 días". */
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
