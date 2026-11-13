package ec.atiempo.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import ec.atiempo.app.ui.theme.Libreta
import androidx.compose.material3.Button as M3Button
import androidx.compose.material3.Card as M3Card
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

/** A sheet of the notebook: paper color, 1dp line, no shadow. */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    if (onClick != null) {
        M3Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, elevation = elevation, border = border, content = content)
    } else {
        M3Card(modifier = modifier, shape = shape, colors = colors, elevation = elevation, border = border, content = content)
    }
}
