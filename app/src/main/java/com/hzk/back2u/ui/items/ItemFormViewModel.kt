package com.hzk.back2u.ui.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hzk.back2u.data.Category
import com.hzk.back2u.data.ItemRepository
import com.hzk.back2u.domain.ItemErrors
import com.hzk.back2u.domain.ItemInput
import com.hzk.back2u.domain.ItemType
import com.hzk.back2u.domain.ItemValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class ItemFormUiState(
    val title: String = "",
    val description: String = "",
    val type: String = ItemType.LOST,
    val categoryId: String = "",
    val categoryName: String = "",
    val location: String = "",
    val dateMillis: Long? = null,
    val categories: List<Category> = emptyList(),
    val errors: ItemErrors = ItemErrors(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val loadError: String? = null,
    val saveError: String? = null,
    val saved: Boolean = false
)

class ItemFormViewModel(
    private val repository: ItemRepository,
    private val itemId: String?,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    val isEditMode: Boolean = itemId != null

    private val _uiState = MutableStateFlow(ItemFormUiState())
    val uiState: StateFlow<ItemFormUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    // Loads the category list and, in edit mode, the item. Also used by Retry.
    fun load() {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            val categories = repository.getCategories().getOrNull()
            val item = itemId?.let { repository.getItem(it).getOrNull() }
            if (categories == null || (itemId != null && item == null)) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadError = "Could not load the form. Check your connection and try again."
                    )
                }
                return@launch
            }
            _uiState.update { state ->
                if (item == null) {
                    state.copy(isLoading = false, categories = categories)
                } else {
                    state.copy(
                        isLoading = false,
                        categories = categories,
                        title = item.title,
                        description = item.description,
                        type = item.type,
                        categoryId = item.categoryId,
                        categoryName = item.categoryName,
                        location = item.location,
                        dateMillis = item.dateOccurred?.time
                    )
                }
            }
        }
    }

    fun onTitleChange(value: String) =
        _uiState.update { it.copy(title = value, errors = it.errors.copy(title = null), saveError = null) }

    fun onDescriptionChange(value: String) =
        _uiState.update { it.copy(description = value, errors = it.errors.copy(description = null), saveError = null) }

    fun onTypeChange(value: String) =
        _uiState.update { it.copy(type = value, errors = it.errors.copy(location = null), saveError = null) }

    fun onCategorySelected(category: Category) =
        _uiState.update {
            it.copy(
                categoryId = category.id,
                categoryName = category.name,
                errors = it.errors.copy(category = null),
                saveError = null
            )
        }

    fun onLocationChange(value: String) =
        _uiState.update { it.copy(location = value, errors = it.errors.copy(location = null), saveError = null) }

    fun onDateSelected(millis: Long) =
        _uiState.update { it.copy(dateMillis = millis, errors = it.errors.copy(date = null), saveError = null) }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return

        val input = ItemInput(
            title = state.title,
            description = state.description,
            type = state.type,
            categoryId = state.categoryId,
            categoryName = state.categoryName,
            location = state.location,
            dateMillis = state.dateMillis
        )

        // Invalid input never reaches the repository.
        val errors = ItemValidator.validate(input, clock())
        if (errors.hasErrors) {
            _uiState.update { it.copy(errors = errors) }
            return
        }

        _uiState.update { it.copy(isSaving = true, saveError = null, errors = ItemErrors()) }
        viewModelScope.launch {
            // Firestore waits for the server, so an offline save would hang: give up after 15 s.
            val result = withTimeoutOrNull(SAVE_TIMEOUT_MS) {
                if (itemId == null) repository.addItem(input).map { }
                else repository.updateItem(itemId, input)
            }
            if (result != null && result.isSuccess) {
                _uiState.update { it.copy(isSaving = false, saved = true) }
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveError = "Could not save the item. Check your connection and try again."
                    )
                }
            }
        }
    }

    private companion object {
        const val SAVE_TIMEOUT_MS = 15_000L
    }
}