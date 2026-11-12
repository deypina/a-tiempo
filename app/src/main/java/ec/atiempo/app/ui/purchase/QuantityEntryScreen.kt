package ec.atiempo.app.ui.purchase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import ec.atiempo.app.ui.common.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ec.atiempo.app.R
import ec.atiempo.app.domain.PurchaseRules
import ec.atiempo.app.domain.Quantity
import ec.atiempo.app.ui.common.AppViewModelFactory

/** "¿Cuántas pastillas de {medicamento} compró?" or "¿… tiene realmente?". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuantityEntryScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuantityEntryViewModel = viewModel(factory = AppViewModelFactory.instance),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isPurchase = state.mode == QuantityMode.PURCHASE

    LaunchedEffect(state.finished) {
        if (state.finished) onClose()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconButton(onClick = onClose, modifier = Modifier.size(56.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_back),
                contentDescription = stringResource(R.string.common_back),
            )
        }
        if (state.loading) return@Column

        Text(
            stringResource(if (isPurchase) R.string.purchase_title else R.string.adjust_title, state.medicationName),
            style = MaterialTheme.typography.headlineMedium,
        )

        if (isPurchase) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PurchaseRules.PURCHASE_SHORTCUTS.forEach { value ->
                    FilterChip(
                        selected = state.shortcut == value,
                        onClick = { viewModel.onShortcut(value) },
                        label = { Text(Quantity.format(value), style = MaterialTheme.typography.titleLarge) },
                        modifier = Modifier.heightIn(min = 56.dp),
                    )
                }
            }
        }

        val errorMessage = if (isPurchase) R.string.purchase_error else R.string.med_error_stock
        OutlinedTextField(
            value = state.text,
            onValueChange = viewModel::onTextChange,
            label = { Text(stringResource(if (isPurchase) R.string.purchase_other else R.string.med_stock)) },
            singleLine = true,
            isError = state.error,
            supportingText = if (state.error) {
                { Text(stringResource(errorMessage)) }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = viewModel::save,
                enabled = !state.saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                Text(stringResource(if (isPurchase) R.string.purchase_save else R.string.adjust_save))
            }
        }
    }
}
