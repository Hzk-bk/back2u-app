package com.hzk.back2u.ui.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hzk.back2u.domain.ItemType
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemFormScreen(
    viewModel: ItemFormViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    // After a successful save the app returns to the list.
    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditMode) "Edit item" else "New item") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Cancel") } }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                state.loadError != null -> Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = state.loadError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = viewModel::load) { Text("Retry") }
                }

                else -> FormFields(
                    state = state,
                    viewModel = viewModel,
                    onPickDate = { showDatePicker = true }
                )
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.dateMillis?.let(::localToPickerMillis)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { viewModel.onDateSelected(pickerToLocalMillis(it)) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun FormFields(
    state: ItemFormUiState,
    viewModel: ItemFormViewModel,
    onPickDate: () -> Unit
) {
    var categoryMenuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TypeOption("Lost", state.type == ItemType.LOST) { viewModel.onTypeChange(ItemType.LOST) }
            TypeOption("Found", state.type == ItemType.FOUND) { viewModel.onTypeChange(ItemType.FOUND) }
        }

        OutlinedTextField(
            value = state.title,
            onValueChange = viewModel::onTitleChange,
            label = { Text("Title") },
            singleLine = true,
            isError = state.errors.title != null,
            supportingText = state.errors.title?.let { error -> { Text(error) } },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.description,
            onValueChange = viewModel::onDescriptionChange,
            label = { Text("Description (optional)") },
            minLines = 3,
            isError = state.errors.description != null,
            supportingText = state.errors.description?.let { error -> { Text(error) } },
            modifier = Modifier.fillMaxWidth()
        )

        Box {
            OutlinedButton(
                onClick = { categoryMenuOpen = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (state.categoryName.isBlank()) "Choose a category"
                    else "Category: ${state.categoryName}"
                )
            }
            DropdownMenu(
                expanded = categoryMenuOpen,
                onDismissRequest = { categoryMenuOpen = false }
            ) {
                state.categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        onClick = {
                            viewModel.onCategorySelected(category)
                            categoryMenuOpen = false
                        }
                    )
                }
            }
        }
        state.errors.category?.let { FieldError(it) }

        OutlinedTextField(
            value = state.location,
            onValueChange = viewModel::onLocationChange,
            label = { Text("Location") },
            singleLine = true,
            isError = state.errors.location != null,
            supportingText = state.errors.location?.let { error -> { Text(error) } },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedButton(onClick = onPickDate, modifier = Modifier.fillMaxWidth()) {
            Text(
                state.dateMillis?.let { "Date: ${DateFormat.getDateInstance().format(Date(it))}" }
                    ?: "Choose a date"
            )
        }
        state.errors.date?.let { FieldError(it) }

        state.saveError?.let { FieldError(it) }

        Button(
            onClick = viewModel::save,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.isSaving) "Saving..." else "Save")
        }
    }
}

@Composable
private fun TypeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun FieldError(text: String) {
    Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}

// The date picker works in UTC midnight; the app stores the start of the chosen local day.
private fun pickerToLocalMillis(utcMillis: Long): Long =
    Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun localToPickerMillis(millis: Long): Long =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()