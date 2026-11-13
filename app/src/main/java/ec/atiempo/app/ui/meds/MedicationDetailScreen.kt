package ec.atiempo.app.ui.meds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ec.atiempo.app.R
import ec.atiempo.app.ui.common.AppViewModelFactory
import ec.atiempo.app.ui.common.Button
import ec.atiempo.app.ui.common.DoubleRule
import ec.atiempo.app.ui.common.OutlinedButton
import ec.atiempo.app.ui.common.StockLineText
import ec.atiempo.app.ui.common.formatTime
import ec.atiempo.app.ui.common.joinWithDots
import ec.atiempo.app.ui.common.pillQuantity
import ec.atiempo.app.ui.theme.ColorDot
import ec.atiempo.app.ui.theme.Libreta

private val FullWidthButton = Modifier
    .fillMaxWidth()
    .heightIn(min = 56.dp)

/**
 * "Ver más" (docs/ESPECIFICACION_TECNICA.md §5): name with its color, dose, times, stock line,
 * and the actions that left the list: purchase, correct the count, history and edit.
 */
@Composable
fun MedicationDetailScreen(
    onClose: () -> Unit,
    onPurchase: (Long) -> Unit,
    onAdjust: (Long) -> Unit,
    onHistory: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: MedicationDetailViewModel = viewModel(factory = AppViewModelFactory.instance),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if (state is MedicationDetailUiState.Gone) onClose()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconButton(onClick = onClose, modifier = Modifier.size(56.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_back),
                contentDescription = stringResource(R.string.common_back),
            )
        }
        val current = state as? MedicationDetailUiState.Ready ?: return@Column
        val med = current.item.medication
        val id = med.id

        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorDot(med.colorKey, 24.dp)
            Spacer(Modifier.width(12.dp))
            Text(med.name, style = MaterialTheme.typography.headlineMedium)
        }
        DoubleRule()
        Text(pillQuantity(med.dosePerIntake), style = MaterialTheme.typography.bodyLarge)
        if (current.item.schedules.isNotEmpty()) {
            Text(
                joinWithDots(current.item.schedules.map { formatTime(it.time) }),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        StockLineText(med, current.item.schedules.size, okColor = MaterialTheme.colorScheme.primary)
        if (!med.active) {
            Text(
                stringResource(R.string.med_paused),
                style = MaterialTheme.typography.bodyLarge,
                color = Libreta.colors.muted,
            )
        }
        med.note?.takeIf { it.isNotBlank() }?.let { note ->
            Text(note, style = MaterialTheme.typography.bodyLarge, color = Libreta.colors.muted)
        }

        Spacer(Modifier.size(4.dp))
        Button(onClick = { onPurchase(id) }, modifier = FullWidthButton) {
            Text(stringResource(R.string.purchase_btn))
        }
        OutlinedButton(onClick = { onAdjust(id) }, modifier = FullWidthButton) {
            Text(stringResource(R.string.adjust_btn))
        }
        OutlinedButton(onClick = { onHistory(id) }, modifier = FullWidthButton) {
            Text(stringResource(R.string.history_btn))
        }
        OutlinedButton(onClick = { onEdit(id) }, modifier = FullWidthButton) {
            Text(stringResource(R.string.med_edit_btn))
        }
    }
}
