package ec.atiempo.app.ui.common

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import ec.atiempo.app.R
import ec.atiempo.app.domain.DoseTime
import ec.atiempo.app.domain.Quantity
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val appLocale: Locale = Locale.forLanguageTag("es-EC")

/** "9:00 p. m." or "21:00", following the phone's 12/24-hour setting. */
@Composable
fun formatTime(time: DoseTime): String {
    val pattern = if (DateFormat.is24HourFormat(LocalContext.current)) "HH:mm" else "h:mm a"
    return LocalTime.of(time.hour, time.minute).format(DateTimeFormatter.ofPattern(pattern, appLocale))
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
