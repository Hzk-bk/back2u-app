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

data class ItemDetailUiState(
    val isLoading: Boolean = true,
    val item: Item? = null,
    val error: String? = null,
    val showDeleteDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val deleted: Boolean = false,
    val message: String? = null
)

class ItemDetailViewModel(
    private val repository: ItemRepository,
    private val itemId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ItemDetailUiState())
    val uiState: StateFlow<ItemDetailUiState> = _uiState.asStateFlow()

    private var listenJob: Job? = null

    init {
        startListening()
    }

    // Uses the same live listener as the list, so the screen is always up to date
    // (for example after the item was edited). Also used by the Retry button.
    fun startListening() {
        listenJob?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null) }
        listenJob = viewModelScope.launch {
            repository.observeMyItems()
                .catch {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Could not load the item. Check your connection and try again."
                        )
                    }
                }
                .collect { list ->
                    val item = list.firstOrNull { it.id == itemId }
                    _uiState.update { state ->
                        when {
                            // While deleting, the item disappears from the list: not an error.
                            state.isDeleting || state.deleted -> state
                            item != null -> state.copy(isLoading = false, item = item, error = null)
                            else -> state.copy(
                                isLoading = false,
                                item = null,
                                error = "This item no longer exists."
                            )
                        }
                    }
                }
        }
    }

    fun onDeleteClick() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }

    // Cancel (or tapping outside the dialog) leaves the item untouched.
    fun onDeleteDismiss() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun onDeleteConfirm() {
        val title = _uiState.value.item?.title ?: return
        _uiState.update { it.copy(showDeleteDialog = false, isDeleting = true) }
        viewModelScope.launch {
            val result = withTimeoutOrNull(DELETE_TIMEOUT_MS) { repository.deleteItem(itemId) }
            if (result != null && result.isSuccess) {
                _uiState.update { it.copy(deleted = true) }
            } else {
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        message = "Could not delete \"$title\". Try again."
                    )
                }
            }
        }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    private companion object {
        const val DELETE_TIMEOUT_MS = 15_000L
    }
}