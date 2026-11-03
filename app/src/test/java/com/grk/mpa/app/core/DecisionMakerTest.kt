package com.grk.mpa.app.core

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class DecisionMakerTest {

    @Test
    fun `default categories contain six predefined categories`() {
        val categories = DecisionMakerDefaults.freshCategories()

        assertEquals(6, categories.size)
        assertTrue(categories.all { !it.isCustom })
        assertEquals("Morning Routine", categories.first().name)
    }

    @Test
    fun `normalize name trims and collapses whitespace`() {
        assertEquals(
            "Morning Routine",
            DecisionMakerOperations.normalizeName("  Morning   Routine  ")
        )
    }

    @Test
    fun `add custom category succeeds`() {
        val result = DecisionMakerOperations.addCategory(
            DecisionMakerData(),
            "  Weekend Plans  ",
            "weekend"
        )

        assertTrue(result.isSuccess)
        val category = result.getOrThrow().categories.last()
        assertEquals("Weekend Plans", category.name)
        assertTrue(category.isCustom)
    }

    @Test
    fun `add category rejects duplicate name ignoring case`() {
        val result = DecisionMakerOperations.addCategory(
            DecisionMakerData(),
            "morning routine",
            "another"
        )

        assertEquals(
            DecisionMakerError.DUPLICATE_NAME,
            (result.exceptionOrNull() as DecisionMakerValidationException).error
        )
    }

    @Test
    fun `add category rejects empty name`() {
        val result = DecisionMakerOperations.addCategory(
            DecisionMakerData(),
            "   ",
            "empty"
        )

        assertEquals(
            DecisionMakerError.EMPTY_NAME,
            (result.exceptionOrNull() as DecisionMakerValidationException).error
        )
    }

    @Test
    fun `rename custom category succeeds`() {
        val data = DecisionMakerOperations.addCategory(
            DecisionMakerData(), "Weekend", "weekend"
        ).getOrThrow()

        val result = DecisionMakerOperations.renameCategory(
            data, "weekend", "Holiday Plans"
        )

        assertEquals("Holiday Plans", result.getOrThrow().categories.last().name)
    }

    @Test
    fun `predefined category cannot be renamed`() {
        val result = DecisionMakerOperations.renameCategory(
            DecisionMakerData(), "morning", "New Name"
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `custom category can be deleted`() {
        val data = DecisionMakerOperations.addCategory(
            DecisionMakerData(), "Weekend", "weekend"
        ).getOrThrow()

        val result = DecisionMakerOperations.deleteCategory(data, "weekend")

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow().categories.any { it.id == "weekend" })
    }

    @Test
    fun `cannot delete predefined category`() {
        val result = DecisionMakerOperations.deleteCategory(
            DecisionMakerData(), "morning"
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `adding option trims whitespace`() {
        val result = DecisionMakerOperations.addOption(
            DecisionMakerData(), "morning", "  Go   running  "
        )

        assertEquals(
            "Go running",
            result.getOrThrow().categories.first().options.last()
        )
    }

    @Test
    fun `adding duplicate option is rejected ignoring case`() {
        val result = DecisionMakerOperations.addOption(
            DecisionMakerData(), "morning", "WAKE UP EARLY"
        )

        assertEquals(
            DecisionMakerError.DUPLICATE_OPTION,
            (result.exceptionOrNull() as DecisionMakerValidationException).error
        )
    }

    @Test
    fun `empty option is rejected`() {
        val result = DecisionMakerOperations.addOption(
            DecisionMakerData(), "morning", "  "
        )

        assertEquals(
            DecisionMakerError.EMPTY_OPTION,
            (result.exceptionOrNull() as DecisionMakerValidationException).error
        )
    }

    @Test
    fun `editing option updates correct item`() {
        val result = DecisionMakerOperations.editOption(
            DecisionMakerData(), "morning", 0, "Start the day"
        )

        assertEquals(
            "Start the day",
            result.getOrThrow().categories.first().options.first()
        )
    }

    @Test
    fun `removing option reduces option count`() {
        val originalCount = DecisionMakerData().categories.first().options.size

        val result = DecisionMakerOperations.removeOption(
            DecisionMakerData(), "morning", 0
        )

        assertEquals(
            originalCount - 1,
            result.getOrThrow().categories.first().options.size
        )
    }

    @Test
    fun `reset options empties selected category`() {
        val result = DecisionMakerOperations.resetOptions(
            DecisionMakerData(), "morning"
        )

        assertTrue(result.getOrThrow().categories.first().options.isEmpty())
    }

    @Test
    fun `restore defaults restores predefined options`() {
        val data = DecisionMakerOperations.resetOptions(
            DecisionMakerData(), "morning"
        ).getOrThrow()

        val result = DecisionMakerOperations.restoreDefaults(data, "morning")

        assertEquals(
            DecisionMakerDefaults.categories.first().options,
            result.getOrThrow().categories.first().options
        )
    }

    @Test
    fun `pick one winner returns exactly one option`() {
        val result = DecisionMakerOperations.pickWinners(
            listOf("A", "B", "C", "D"),
            1,
            Random(10)
        )

        assertEquals(1, result.getOrThrow().size)
        assertTrue(listOf("A", "B", "C", "D").contains(result.getOrThrow().first()))
    }

    @Test
    fun `pick multiple winners returns unique options`() {
        val result = DecisionMakerOperations.pickWinners(
            listOf("A", "B", "C", "D"),
            3,
            Random(20)
        )

        assertEquals(3, result.getOrThrow().size)
        assertEquals(3, result.getOrThrow().distinct().size)
    }

    @Test
    fun `picking winners requires at least two options`() {
        val result = DecisionMakerOperations.pickWinners(
            listOf("Only one"),
            1
        )

        assertEquals(
            DecisionMakerError.INSUFFICIENT_OPTIONS,
            (result.exceptionOrNull() as DecisionMakerValidationException).error
        )
    }

    @Test
    fun `winner count cannot exceed three`() {
        val result = DecisionMakerOperations.pickWinners(
            listOf("A", "B", "C", "D"),
            4
        )

        assertEquals(
            DecisionMakerError.INVALID_WINNER_COUNT,
            (result.exceptionOrNull() as DecisionMakerValidationException).error
        )
    }

    @Test
    fun `history keeps only latest ten entries`() {
        var data = DecisionMakerData()

        repeat(12) { index ->
            data = DecisionMakerOperations.addHistory(
                data = data,
                categoryName = "Morning Routine",
                winners = listOf("Option $index"),
                pickedAtMillis = index.toLong(),
                id = index.toLong()
            )
        }

        assertEquals(10, data.history.size)
        assertEquals(11L, data.history.first().id)
        assertEquals(2L, data.history.last().id)
    }

    @Test
    fun `clear history removes all entries`() {
        val data = DecisionMakerOperations.addHistory(
            DecisionMakerData(),
            "Morning Routine",
            listOf("Exercise"),
            100L,
            1L
        )

        val result = DecisionMakerOperations.clearHistory(data)

        assertTrue(result.history.isEmpty())
    }
}
