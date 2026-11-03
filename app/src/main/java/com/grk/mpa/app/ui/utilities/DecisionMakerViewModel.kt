package com.grk.mpa.app.ui.utilities

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.grk.mpa.app.core.DecisionCategory
import com.grk.mpa.app.core.DecisionMakerData
import com.grk.mpa.app.core.DecisionMakerDefaults
import com.grk.mpa.app.core.DecisionMakerOperations
import com.grk.mpa.app.data.DecisionMakerRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

enum class DecisionDisplayMode { WHEEL, LIST }

class DecisionMakerViewModel(
    private val repository: DecisionMakerRepository,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    var data by mutableStateOf(DecisionMakerData())
        private set
    var selectedCategoryId by mutableStateOf(DecisionMakerDefaults.categories.first().id)
        private set
    var winnerCount by mutableStateOf(1)
        private set
    var displayMode by mutableStateOf(DecisionDisplayMode.WHEEL)
        private set
    var winners by mutableStateOf<List<String>>(emptyList())
        private set
    var spinToken by mutableStateOf(0)
        private set
    var isLoaded by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    private var saveJob: Job? = null

    val categories: List<DecisionCategory> get() = data.categories
    val selectedCategory: DecisionCategory?
        get() = categories.firstOrNull { it.id == selectedCategoryId }
    val history get() = data.history
    val canPick: Boolean get() = isLoaded && (selectedCategory?.options?.size ?: 0) >= 2
    val canAddOption: Boolean get() = isLoaded && selectedCategory != null

    init {
        viewModelScope.launch {
            try {
                data = repository.load()
                if (data.categories.none { it.id == selectedCategoryId }) {
                    selectedCategoryId = data.categories.firstOrNull()?.id.orEmpty()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                errorMessage = error.message ?: "Could not load saved decisions."
            } finally {
                isLoaded = true
            }
        }
    }

    fun selectCategory(id: String) {
        if (categories.none { it.id == id }) return
        selectedCategoryId = id
        winners = emptyList()
        errorMessage = null
        winnerCount = winnerCount.coerceAtMost(categories.first { it.id == id }.options.size.coerceAtLeast(1))
    }

    fun updateWinnerCount(count: Int) {
        winnerCount = count.coerceIn(1, minOf(DecisionMakerDefaults.MAX_WINNERS, selectedCategory?.options?.size ?: 1))
    }

    fun updateDisplayMode(mode: DecisionDisplayMode) {
        displayMode = mode
    }

    fun addCategory(name: String) {
        val result = DecisionMakerOperations.addCategory(data, name, UUID.randomUUID().toString())
        applyResult(result)
        if (result.isSuccess) {
            selectedCategoryId = data.categories.last().id
            winners = emptyList()
        }
    }

    fun renameCategory(id: String, name: String) {
        applyResult(DecisionMakerOperations.renameCategory(data, id, name))
    }

    fun deleteCategory(id: String) {
        val result = DecisionMakerOperations.deleteCategory(data, id)
        if (applyResult(result)) {
            if (selectedCategoryId == id) {
                selectedCategoryId = data.categories.firstOrNull()?.id.orEmpty()
                winners = emptyList()
            }
        }
    }

    fun addOption(value: String) {
        if (applyResult(DecisionMakerOperations.addOption(data, selectedCategoryId, value))) {
            winners = emptyList()
        }
    }

    fun editOption(index: Int, value: String) {
        if (applyResult(DecisionMakerOperations.editOption(data, selectedCategoryId, index, value))) {
            winners = emptyList()
        }
    }

    fun removeOption(index: Int) {
        if (applyResult(DecisionMakerOperations.removeOption(data, selectedCategoryId, index))) {
            winners = emptyList()
            setWinnerCount(winnerCount)
        }
    }

    fun resetOptions() {
        if (applyResult(DecisionMakerOperations.resetOptions(data, selectedCategoryId))) {
            winners = emptyList()
            winnerCount = 1
        }
    }

    fun restoreDefaults() {
        if (applyResult(DecisionMakerOperations.restoreDefaults(data, selectedCategoryId))) {
            winners = emptyList()
            winnerCount = 1
        }
    }

    fun pickAgain() = pickWinners()

    fun pickWinners() {
        val category = selectedCategory ?: return
        val result = DecisionMakerOperations.pickWinners(category.options, winnerCount)
        result.onSuccess { selected ->
            winners = selected
            spinToken += 1
            val now = clock()
            data = DecisionMakerOperations.addHistory(
                data = data,
                categoryName = category.name,
                winners = selected,
                pickedAtMillis = now,
                id = now
            )
            persist()
            errorMessage = null
        }.onFailure { error ->
            errorMessage = error.message ?: "Unable to pick a winner."
        }
    }

    fun clearWinners() {
        winners = emptyList()
    }

    fun clearHistory() {
        if (!isLoaded) return
        data = DecisionMakerOperations.clearHistory(data)
        persist()
    }

    fun clearError() {
        errorMessage = null
    }

    private fun applyResult(result: Result<DecisionMakerData>): Boolean {
        if (!isLoaded) return false
        result.onSuccess {
            data = it
            persist()
            errorMessage = null
        }.onFailure {
            errorMessage = when (it) {
                is IllegalArgumentException -> when (it.message) {
                    "EMPTY_NAME" -> "Enter a category name."
                    "NAME_TOO_LONG" -> "Category names can contain up to 50 characters."
                    "DUPLICATE_NAME" -> "A category with this name already exists."
                    "EMPTY_OPTION" -> "Enter an option."
                    "OPTION_TOO_LONG" -> "Options can contain up to 100 characters."
                    "DUPLICATE_OPTION" -> "This option already exists."
                    "INSUFFICIENT_OPTIONS" -> "Add at least two options before picking."
                    else -> it.message ?: "That change is not allowed."
                }
                else -> it.message ?: "That change could not be completed."
            }
        }
        return result.isSuccess
    }

    private fun persist() {
        val snapshot = data
        val previous = saveJob
        saveJob = viewModelScope.launch {
            previous?.join()
            try {
                repository.save(snapshot)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                errorMessage = error.message ?: "Could not save your changes."
            }
        }
    }

    class Factory(private val repository: DecisionMakerRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(DecisionMakerViewModel::class.java))
            return DecisionMakerViewModel(repository) as T
        }
    }
}
