package com.hzk.back2u.ui.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hzk.back2u.data.Item
import com.hzk.back2u.data.ItemRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class ItemListUiState(
    val isLoading: Boolean = true,
    val items: List<Item> = emptyList(),
    val error: String? = null,
    val itemToDelete: Item? = null,
    val message: String? = null
)

class ItemListViewModel(
    private val repository: ItemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ItemListUiState())
    val uiState: StateFlow<ItemListUiState> = _uiState.asStateFlow()

    private var listenJob: Job? = null

    init {
        startListening()
    }

    // Also used by the Retry button. Cancelling the job closes the Firestore listener.
    fun startListening() {
        listenJob?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null) }
        listenJob = viewModelScope.launch {
            repository.observeMyItems()
                .catch {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Could not load items. Check your connection and try again."
                        )
                    }
                }
                .collect { list ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            // sorted here because Firestore would need an index for filter + orderBy
                            items = list.sortedByDescending { item -> item.dateOccurred }
                        )
                    }
                }
        }
    }

    // Step 1: the user taps Delete on a row. Nothing is deleted yet, the dialog opens.
    fun onDeleteClick(item: Item) {
        _uiState.update { it.copy(itemToDelete = item) }
    }

    // Cancel (or tapping outside the dialog) leaves the item untouched.
    fun onDeleteDismiss() {
        _uiState.update { it.copy(itemToDelete = null) }
    }

    // Step 2: the user confirms. The list updates by itself through the live listener.
    fun onDeleteConfirm() {
        val item = _uiState.value.itemToDelete ?: return
        _uiState.update { it.copy(itemToDelete = null) }
        viewModelScope.launch {
            val result = withTimeoutOrNull(DELETE_TIMEOUT_MS) { repository.deleteItem(item.id) }
            val message = if (result != null && result.isSuccess) {
                "Deleted \"${item.title}\""
            } else {
                "Could not delete \"${item.title}\". Try again."
            }
            _uiState.update { it.copy(message = message) }
        }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    private companion object {
        const val DELETE_TIMEOUT_MS = 15_000L
    }
}