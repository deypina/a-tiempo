package ec.atiempo.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ec.atiempo.app.R
import ec.atiempo.app.domain.HistoryEntry
import ec.atiempo.app.domain.HistoryKind
import ec.atiempo.app.ui.common.AppViewModelFactory
import ec.atiempo.app.ui.common.RowRule
import ec.atiempo.app.ui.common.formatDate
import ec.atiempo.app.ui.common.formatTime
import ec.atiempo.app.ui.common.pillQuantity

@Composable
fun HistoryScreen(
    onClose: () -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = AppViewModelFactory.instance),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if (state is HistoryUiState.Gone) onClose()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            IconButton(onClick = onClose, modifier = Modifier.size(56.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.common_back),
                )
            }
        }
        val current = state
        if (current is HistoryUiState.Ready) {
            item {
                Text(
                    stringResource(R.string.history_title, current.medicationName),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            if (current.entries.isEmpty()) {
                item {
                    Text(stringResource(R.string.history_empty), style = MaterialTheme.typography.bodyLarge)
                }
            }
            items(current.entries) { entry ->
                HistoryRow(entry)
                RowRule()
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry) {
    // Read as one line by TalkBack: date, time and what happened.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            stringResource(R.string.join_dot, formatDate(entry.at), formatTime(entry.at)),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            when (entry.kind) {
                HistoryKind.TAKEN -> stringResource(R.string.history_taken, pillQuantity(entry.amount))
                HistoryKind.MISSED -> stringResource(R.string.history_missed)
                HistoryKind.PURCHASE -> stringResource(R.string.history_purchase, pillQuantity(entry.amount))
                HistoryKind.ADJUSTMENT -> stringResource(R.string.history_adjust, pillQuantity(entry.amount))
                HistoryKind.UNDO -> stringResource(R.string.history_undo)
            },
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
