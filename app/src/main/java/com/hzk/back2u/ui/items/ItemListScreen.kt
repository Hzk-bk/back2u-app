package com.hzk.back2u.ui.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.hzk.back2u.data.Item
import com.hzk.back2u.domain.ItemStatus
import java.text.DateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemListScreen(
    viewModel: ItemListViewModel,
    onLogout: () -> Unit,
    onAddClick: () -> Unit,
    onItemClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Short confirmation message after a delete or status change (or an error if it failed).
    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long)
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My items") },
                actions = { TextButton(onClick = onLogout) { Text("Log out") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add item")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Search field and "Hide returned" switch: only when there is something to filter.
            if (!state.isLoading && state.error == null && state.items.isNotEmpty()) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    label = { Text("Search") },
                    singleLine = true,
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Hide returned items", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = state.hideReturned,
                        onCheckedChange = viewModel::onHideReturnedChange
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                    state.error != null -> Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = state.error.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = viewModel::startListening) { Text("Retry") }
                    }

                    state.items.isEmpty() -> Text(
                        text = "No items yet. Tap + to add one.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        textAlign = TextAlign.Center
                    )

                    state.visibleItems.isEmpty() -> Text(
                        text = if (state.query.isBlank()) {
                            "All items are returned. Turn off \"Hide returned items\" to see them."
                        } else {
                            "No items match \"${state.query}\"."
                        },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        textAlign = TextAlign.Center
                    )

                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.visibleItems, key = { it.id }) { item ->
                            ItemRow(
                                item = item,
                                onClick = { onItemClick(item.id) },
                                onToggleStatusClick = { viewModel.onToggleStatus(item) },
                                onDeleteClick = { viewModel.onDeleteClick(item) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog: names the item that will be deleted.
    state.itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = viewModel::onDeleteDismiss,
            title = { Text("Delete item?") },
            text = { Text("\"${item.title}\" will be deleted permanently.") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::onDeleteConfirm,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDeleteDismiss) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ItemRow(
    item: Item,
    onClick: () -> Unit,
    onToggleStatusClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val returned = item.status == ItemStatus.RETURNED

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // A returned item looks different: faded, struck-through title and a label.
            Column(
                Modifier
                    .weight(1f)
                    .alpha(if (returned) 0.55f else 1f)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (returned) TextDecoration.LineThrough else null
                )
                Text(
                    text = "${item.type.replaceFirstChar { it.uppercase() }} · ${item.categoryName}",
                    style = MaterialTheme.typography.bodyMedium
                )
                val details = listOfNotNull(
                    item.location.takeIf { it.isNotBlank() },
                    item.dateOccurred?.let { DateFormat.getDateInstance().format(it) }
                )
                if (details.isNotEmpty()) {
                    Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                }
                if (returned) {
                    Text(
                        text = "Returned",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onToggleStatusClick) {
                    Text(if (returned) "Reopen" else "Mark returned")
                }
                TextButton(
                    onClick = onDeleteClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            }
        }
    }
}