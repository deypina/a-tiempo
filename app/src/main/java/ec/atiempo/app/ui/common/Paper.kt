package ec.atiempo.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import ec.atiempo.app.R
import ec.atiempo.app.domain.InventoryRules
import ec.atiempo.app.domain.Medication
import ec.atiempo.app.domain.StockLevel
import ec.atiempo.app.domain.TodayRules
import ec.atiempo.app.ui.theme.Libreta
import androidx.compose.material3.Button as M3Button
import androidx.compose.material3.OutlinedButton as M3OutlinedButton

/*
 * "Libreta" look (docs/ESPECIFICACION_TECNICA.md §5): almost square corners and hairlines instead
 * of pills and shadows. Buttons are the only things that look like buttons: filled green for the
 * main action, green outline for the secondary one, underlined text for small actions.
 * Screens import these instead of Material's.
 */

private val MinButtonHeight = 56.dp
private val MinLinkTouch = 44.dp

/** Main action: filled green, 6dp corners, at least 56dp tall. */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    M3Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = MinButtonHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
            disabledElevation = 0.dp,
        ),
        content = content,
    )
}

/** Secondary action: 1dp green border and green text, at least 56dp tall. */
@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val green = MaterialTheme.colorScheme.primary
    M3OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = MinButtonHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = green),
        border = BorderStroke(1.dp, if (enabled) green else MaterialTheme.colorScheme.outline),
        content = content,
    )
}

/**
 * Small action as underlined text ("Deshacer", "Recordar en 5 minutos").
 * The touch area is at least 44dp and TalkBack announces it as a button.
 */
@Composable
fun LinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier = modifier
            .heightIn(min = MinLinkTouch)
            .widthIn(min = MinLinkTouch)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            color = color,
            // bodyLarge is 18sp, above the 16sp minimum for small actions.
            style = MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.Underline),
        )
    }
}

/** Two 1dp ink lines 2dp apart, like the margin of a diary (CSS `3px double`). */
@Composable
fun DoubleRule(modifier: Modifier = Modifier) {
    val ink = Libreta.colors.doubleLine
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(ink))
        Spacer(Modifier.height(2.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(ink))
    }
}

/** The 1dp line between rows of a list. */
@Composable
fun RowRule(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Libreta.colors.rule))
}

/**
 * The stock line of a medication in its status color: brick "Se acabó · no le quedan pastillas"
 * when it ran out, ocre when low, [okColor] otherwise. The words always say it too, never the color alone.
 */
@Composable
fun StockLineText(
    medication: Medication,
    scheduleCount: Int,
    okColor: Color,
    modifier: Modifier = Modifier,
) {
    val style = MaterialTheme.typography.bodyLarge
    when (TodayRules.stockLevel(medication)) {
        StockLevel.OUT -> Text(
            stringResource(R.string.stock_out_line),
            style = style,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            modifier = modifier,
        )
        StockLevel.LOW -> Text(
            stockLine(medication.stock, InventoryRules.daysLeft(medication, scheduleCount)),
            style = style,
            fontWeight = FontWeight.Bold,
            color = Libreta.colors.ocre,
            modifier = modifier,
        )
        StockLevel.OK -> Text(
            stockLine(medication.stock, InventoryRules.daysLeft(medication, scheduleCount)),
            style = style,
            color = okColor,
            modifier = modifier,
        )
    }
}
