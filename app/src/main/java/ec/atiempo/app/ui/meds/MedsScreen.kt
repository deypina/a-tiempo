package ec.atiempo.app.ui.meds

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ec.atiempo.app.R
import ec.atiempo.app.domain.MedicationWithSchedules
import ec.atiempo.app.domain.StockLevel
import ec.atiempo.app.domain.TodayRules
import ec.atiempo.app.ui.common.AppViewModelFactory
import ec.atiempo.app.ui.common.Button
import ec.atiempo.app.ui.common.DoubleRule
import ec.atiempo.app.ui.common.OutlinedButton
import ec.atiempo.app.ui.common.RowRule
import ec.atiempo.app.ui.common.StockLineText
import ec.atiempo.app.ui.common.formatTime
import ec.atiempo.app.ui.common.joinWithDots
import ec.atiempo.app.ui.common.pillQuantity
import ec.atiempo.app.ui.theme.ColorDot
import ec.atiempo.app.ui.theme.Libreta

/** Decorative arrow after "Ver más". */
private const val MORE_GLYPH = "›"

private val FullWidthButton = Modifier
    .fillMaxWidth()
    .heightIn(min = 56.dp)

@Composable
fun MedsScreen(
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onPurchase: (Long) -> Unit,
    viewModel: MedsViewModel = viewModel(factory = AppViewModelFactory.instance),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
    ) {
        item {
            Text(stringResource(R.string.meds_title), style = MaterialTheme.typography.headlineLarge)
        }
        item {
            OutlinedButton(onClick = onAdd, modifier = Modifier.padding(top = 12.dp).then(FullWidthButton)) {
                Text(stringResource(R.string.meds_add))
            }
        }
        val current = state
        if (current is MedsUiState.Ready) {
            if (current.medications.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.today_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            } else {
                item { DoubleRule(Modifier.padding(top = 20.dp)) }
                items(current.medications, key = { it.medication.id }) { item ->
                    MedicationRow(
                        item = item,
                        onOpen = { onOpen(item.medication.id) },
                        onPurchase = { onPurchase(item.medication.id) },
                    )
                    RowRule()
                }
            }
        }
    }
}

/**
 * One medication as a row: color, name and "Ver más ›" (the whole top line opens the detail),
 * then dose and times, the stock line and, only when needed, "Registrar compra".
 */
@Composable
private fun MedicationRow(
    item: MedicationWithSchedules,
    onOpen: () -> Unit,
    onPurchase: () -> Unit,
) {
    val med = item.medication
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(role = Role.Button, onClick = onOpen),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ColorDot(med.colorKey, 20.dp)
            Spacer(Modifier.width(12.dp))
            Text(med.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.meds_more),
                style = MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.Underline),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                " $MORE_GLYPH",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
        Text(
            joinWithDots(listOf(pillQuantity(med.dosePerIntake)) + item.schedules.map { formatTime(it.time) }),
            style = MaterialTheme.typography.bodyLarge,
        )
        StockLineText(med, item.schedules.size, okColor = MaterialTheme.colorScheme.primary)
        if (!med.active) {
            Text(
                stringResource(R.string.med_paused),
                style = MaterialTheme.typography.bodyLarge,
                color = Libreta.colors.muted,
            )
        }
        when (TodayRules.stockLevel(med)) {
            StockLevel.OUT -> Button(onClick = onPurchase, modifier = Modifier.padding(top = 4.dp).then(FullWidthButton)) {
                Text(stringResource(R.string.purchase_btn))
            }
            StockLevel.LOW -> OutlinedButton(onClick = onPurchase, modifier = Modifier.padding(top = 4.dp).then(FullWidthButton)) {
                Text(stringResource(R.string.purchase_btn))
            }
            StockLevel.OK -> Unit
        }
    }
}
