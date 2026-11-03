package com.grk.mpa.app.core

import kotlin.random.Random
import kotlinx.serialization.Serializable

@Serializable
data class DecisionCategory(
    val id: String,
    val name: String,
    val options: List<String> = emptyList(),
    val isCustom: Boolean = false
)

@Serializable
data class DecisionHistoryEntry(
    val id: Long,
    val categoryName: String,
    val winners: List<String>,
    val pickedAtMillis: Long
)

@Serializable
data class DecisionMakerData(
    val categories: List<DecisionCategory> = DecisionMakerDefaults.categories,
    val history: List<DecisionHistoryEntry> = emptyList()
)

enum class DecisionMakerError {
    EMPTY_NAME,
    NAME_TOO_LONG,
    DUPLICATE_NAME,
    EMPTY_OPTION,
    OPTION_TOO_LONG,
    DUPLICATE_OPTION,
    INSUFFICIENT_OPTIONS,
    INVALID_WINNER_COUNT
}

/**
 * Calculates a clockwise final rotation that places the selected wheel segment
 * under the fixed pointer at the top of the wheel.
 */
object DecisionWheelMath {
    fun targetRotation(
        currentRotation: Float,
        winnerIndex: Int,
        optionCount: Int,
        fullSpins: Int = 5
    ): Float {
        require(optionCount > 0) { "The wheel must contain at least one option." }
        require(winnerIndex in 0 until optionCount) { "Winner index is outside the wheel options." }
        require(fullSpins >= 1) { "The wheel must complete at least one full spin." }

        val segmentSweep = 360f / optionCount
        val segmentCenter = (winnerIndex + 0.5f) * segmentSweep
        val currentAngle = ((currentRotation % 360f) + 360f) % 360f
        val alignment = (360f - ((currentAngle + segmentCenter) % 360f)) % 360f

        return currentRotation + fullSpins * 360f + alignment
    }
}

object DecisionMakerDefaults {
    const val MAX_CATEGORY_NAME_LENGTH = 50
    const val MAX_OPTION_LENGTH = 100
    const val MAX_WINNERS = 3
    const val HISTORY_LIMIT = 10

    val categories = listOf(
        DecisionCategory(
            id = "morning",
            name = "Morning Routine",
            options = listOf(
                "Wake up early", "Exercise", "Meditation",
                "Read a book", "Healthy breakfast", "Plan the day"
            )
        ),
        DecisionCategory(
            id = "work",
            name = "Work / Study",
            options = listOf(
                "Practice coding", "Learn something new", "Read documentation",
                "Work on a project", "Take a short break", "Review progress"
            )
        ),
        DecisionCategory(
            id = "health",
            name = "Health & Fitness",
            options = listOf(
                "Walking", "Yoga", "Workout",
                "Stretching", "Cycling", "Meditation"
            )
        ),
        DecisionCategory(
            id = "household",
            name = "Household Tasks",
            options = listOf(
                "Clean the room", "Do laundry", "Organize desk",
                "Grocery shopping", "Prepare a meal", "Water plants"
            )
        ),
        DecisionCategory(
            id = "meals",
            name = "Meal Ideas",
            options = listOf(
                "Breakfast", "Lunch", "Dinner",
                "Biryani", "Dosa", "Salad"
            )
        ),
        DecisionCategory(
            id = "evening",
            name = "Evening Routine",
            options = listOf(
                "Read a book", "Go for a walk", "Plan tomorrow",
                "Relax", "Spend time with family", "Sleep early"
            )
        )
    )

    fun freshCategories(): List<DecisionCategory> =
        categories.map { it.copy(options = it.options.toList()) }
}

object DecisionMakerOperations {

    fun normalizeName(value: String): String =
        value.trim().replace(Regex("\\s+"), " ")

    fun addCategory(
        data: DecisionMakerData,
        name: String,
        id: String
    ): Result<DecisionMakerData> {
        val cleaned = normalizeName(name)

        if (cleaned.isEmpty()) {
            return Result.failure(DecisionMakerValidationException(DecisionMakerError.EMPTY_NAME))
        }
        if (cleaned.length > DecisionMakerDefaults.MAX_CATEGORY_NAME_LENGTH) {
            return Result.failure(DecisionMakerValidationException(DecisionMakerError.NAME_TOO_LONG))
        }
        if (data.categories.any { it.name.equals(cleaned, ignoreCase = true) }) {
            return Result.failure(DecisionMakerValidationException(DecisionMakerError.DUPLICATE_NAME))
        }
        if (data.categories.any { it.id == id } || id.isBlank()) {
            return Result.failure(IllegalArgumentException("Category ID must be unique and non-empty."))
        }

        return Result.success(
            data.copy(
                categories = data.categories + DecisionCategory(
                    id = id,
                    name = cleaned,
                    isCustom = true
                )
            )
        )
    }

    fun renameCategory(
        data: DecisionMakerData,
        categoryId: String,
        newName: String
    ): Result<DecisionMakerData> {
        val category = data.categories.firstOrNull { it.id == categoryId }
            ?: return Result.failure(IllegalArgumentException("Category not found."))

        if (!category.isCustom) {
            return Result.failure(IllegalArgumentException("Predefined categories cannot be renamed."))
        }

        val cleaned = normalizeName(newName)
        if (cleaned.isEmpty()) {
            return Result.failure(DecisionMakerValidationException(DecisionMakerError.EMPTY_NAME))
        }
        if (cleaned.length > DecisionMakerDefaults.MAX_CATEGORY_NAME_LENGTH) {
            return Result.failure(DecisionMakerValidationException(DecisionMakerError.NAME_TOO_LONG))
        }
        if (data.categories.any {
                it.id != categoryId && it.name.equals(cleaned, ignoreCase = true)
            }) {
            return Result.failure(DecisionMakerValidationException(DecisionMakerError.DUPLICATE_NAME))
        }

        return Result.success(
            data.copy(
                categories = data.categories.map {
                    if (it.id == categoryId) it.copy(name = cleaned) else it
                }
            )
        )
    }

    fun deleteCategory(
        data: DecisionMakerData,
        categoryId: String
    ): Result<DecisionMakerData> {
        val category = data.categories.firstOrNull { it.id == categoryId }
            ?: return Result.failure(IllegalArgumentException("Category not found."))

        if (!category.isCustom) {
            return Result.failure(IllegalArgumentException("Predefined categories cannot be deleted."))
        }

        val remaining = data.categories.filterNot { it.id == categoryId }
        return Result.success(
            data.copy(
                categories = if (remaining.isEmpty()) DecisionMakerDefaults.freshCategories() else remaining
            )
        )
    }

    fun addOption(
        data: DecisionMakerData,
        categoryId: String,
        value: String
    ): Result<DecisionMakerData> {
        val category = data.categories.firstOrNull { it.id == categoryId }
            ?: return Result.failure(IllegalArgumentException("Category not found."))

        val cleaned = normalizeName(value)
        if (cleaned.isEmpty()) {
            return Result.failure(DecisionMakerValidationException(DecisionMakerError.EMPTY_OPTION))
        }
        if (cleaned.length > DecisionMakerDefaults.MAX_OPTION_LENGTH) {
            return Result.failure(DecisionMakerValidationException(DecisionMakerError.OPTION_TOO_LONG))
        }
        if (category.options.any { it.equals(cleaned, ignoreCase = true) }) {
            return Result.failure(DecisionMakerValidationException(DecisionMakerError.DUPLICATE_OPTION))
        }

        return Result.success(
            data.copy(
                categories = data.categories.map {
                    if (it.id == categoryId) it.copy(options = it.options + cleaned) else it
                }
            )
        )
    }

    fun editOption(
        data: DecisionMakerData,
        categoryId: String,
        index: Int,
        value: String
    ): Result<DecisionMakerData> {
        val category = data.categories.firstOrNull { it.id == categoryId }
            ?: return Result.failure(IllegalArgumentException("Category not found."))

        if (index !in category.options.indices) {
            return Result.failure(IndexOutOfBoundsException("Option index is invalid."))
        }

        val cleaned = normalizeName(value)
        if (cleaned.isEmpty()) {
            return Result.failure(DecisionMakerValidationException(DecisionMakerError.EMPTY_OPTION))
        }
        if (cleaned.length > DecisionMakerDefaults.MAX_OPTION_LENGTH) {
            return Result.failure(DecisionMakerValidationException(DecisionMakerError.OPTION_TOO_LONG))
        }
        if (category.options.withIndex().any {
                it.index != index && it.value.equals(cleaned, ignoreCase = true)
            }) {
            return Result.failure(DecisionMakerValidationException(DecisionMakerError.DUPLICATE_OPTION))
        }

        return Result.success(
            data.copy(
                categories = data.categories.map {
                    if (it.id == categoryId) {
                        it.copy(options = it.options.toMutableList().also { options ->
                            options[index] = cleaned
                        })
                    } else it
                }
            )
        )
    }

    fun removeOption(
        data: DecisionMakerData,
        categoryId: String,
        index: Int
    ): Result<DecisionMakerData> {
        val category = data.categories.firstOrNull { it.id == categoryId }
            ?: return Result.failure(IllegalArgumentException("Category not found."))

        if (index !in category.options.indices) {
            return Result.failure(IndexOutOfBoundsException("Option index is invalid."))
        }

        return Result.success(
            data.copy(
                categories = data.categories.map {
                    if (it.id == categoryId) {
                        it.copy(options = it.options.filterIndexed { i, _ -> i != index })
                    } else it
                }
            )
        )
    }

    fun resetOptions(
        data: DecisionMakerData,
        categoryId: String
    ): Result<DecisionMakerData> {
        if (data.categories.none { it.id == categoryId }) {
            return Result.failure(IllegalArgumentException("Category not found."))
        }

        return Result.success(
            data.copy(
                categories = data.categories.map {
                    if (it.id == categoryId) it.copy(options = emptyList()) else it
                }
            )
        )
    }

    fun restoreDefaults(
        data: DecisionMakerData,
        categoryId: String
    ): Result<DecisionMakerData> {
        val original = DecisionMakerDefaults.categories.firstOrNull { it.id == categoryId }
            ?: return Result.failure(IllegalArgumentException("No predefined defaults exist."))

        return Result.success(
            data.copy(
                categories = data.categories.map {
                    if (it.id == categoryId) original.copy() else it
                }
            )
        )
    }

    fun pickWinners(
        options: List<String>,
        winnerCount: Int,
        random: Random = Random.Default
    ): Result<List<String>> {
        if (options.size < 2) {
            return Result.failure(
                DecisionMakerValidationException(DecisionMakerError.INSUFFICIENT_OPTIONS)
            )
        }
        if (winnerCount !in 1..minOf(DecisionMakerDefaults.MAX_WINNERS, options.size)) {
            return Result.failure(
                DecisionMakerValidationException(DecisionMakerError.INVALID_WINNER_COUNT)
            )
        }

        return Result.success(options.shuffled(random).take(winnerCount))
    }

    fun addHistory(
        data: DecisionMakerData,
        categoryName: String,
        winners: List<String>,
        pickedAtMillis: Long,
        id: Long
    ): DecisionMakerData {
        val entry = DecisionHistoryEntry(
            id = id,
            categoryName = categoryName,
            winners = winners.toList(),
            pickedAtMillis = pickedAtMillis
        )
        return data.copy(
            history = (listOf(entry) + data.history).take(DecisionMakerDefaults.HISTORY_LIMIT)
        )
    }

    fun clearHistory(data: DecisionMakerData): DecisionMakerData =
        data.copy(history = emptyList())
}

class DecisionMakerValidationException(
    val error: DecisionMakerError
) : IllegalArgumentException(error.name)
