package ec.atiempo.app.ui.meds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ec.atiempo.app.R
import ec.atiempo.app.domain.InventoryRules
import ec.atiempo.app.domain.MedicationWithSchedules
import ec.atiempo.app.ui.common.AppViewModelFactory
import ec.atiempo.app.ui.common.formatTime
import ec.atiempo.app.ui.common.joinWithDots
import ec.atiempo.app.ui.common.pillQuantity
import ec.atiempo.app.ui.common.stockLine

private val CardButtonModifier = Modifier
    .fillMaxWidth()
    .heightIn(min = 56.dp)

@Composable
fun MedsScreen(
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onPurchase: (Long) -> Unit,
    onAdjust: (Long) -> Unit,
    viewModel: MedsViewModel = viewModel(factory = AppViewModelFactory.instance),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(stringResource(R.string.meds_title), style = MaterialTheme.typography.headlineLarge)
        }
        item {
            Button(
                onClick = onAdd,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                Text(stringResource(R.string.meds_add))
            }
        }
        val current = state
        if (current is MedsUiState.Ready) {
            if (current.medications.isEmpty()) {
                item {
                    Text(stringResource(R.string.today_empty), style = MaterialTheme.typography.bodyLarge)
                }
            }
            items(current.medications, key = { it.medication.id }) { item ->
                MedicationCard(
                    item = item,
                    onClick = { onOpen(item.medication.id) },
                    onPurchase = { onPurchase(item.medication.id) },
                    onAdjust = { onAdjust(item.medication.id) },
                )
            }
        }
    }
}

@Composable
private fun MedicationCard(
    item: MedicationWithSchedules,
    onClick: () -> Unit,
    onPurchase: () -> Unit,
    onAdjust: () -> Unit,
) {
    val med = item.medication
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(med.name, style = MaterialTheme.typography.titleLarge)
            Text(
                joinWithDots(listOf(pillQuantity(med.dosePerIntake)) + item.schedules.map { formatTime(it.time) }),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                stockLine(med.stock, InventoryRules.daysLeft(med, item.schedules.size)),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (!med.active) {
                Text(
                    stringResource(R.string.med_paused),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onPurchase, modifier = CardButtonModifier) {
                Text(stringResource(R.string.purchase_btn))
            }
            OutlinedButton(onClick = onAdjust, modifier = CardButtonModifier) {
                Text(stringResource(R.string.adjust_btn))
            }
        }
    }
}
