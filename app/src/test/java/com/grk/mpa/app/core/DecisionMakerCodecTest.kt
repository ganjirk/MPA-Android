package com.grk.mpa.app.core

import org.junit.Assert.*
import org.junit.Test

class DecisionMakerCodecTest {

    @Test
    fun `encode and decode preserves default categories`() {
        val original = DecisionMakerData()

        val decoded = DecisionMakerCodec.decode(
            DecisionMakerCodec.encode(original)
        )

        assertEquals(original.categories, decoded.categories)
        assertEquals(original.history, decoded.history)
    }

    @Test
    fun `encode and decode preserves custom categories and options`() {
        val original = DecisionMakerOperations.addCategory(
            DecisionMakerData(),
            "Weekend Plans",
            "weekend"
        ).getOrThrow()

        val updated = DecisionMakerOperations.addOption(
            original,
            "weekend",
            "Visit a park"
        ).getOrThrow()

        val decoded = DecisionMakerCodec.decode(
            DecisionMakerCodec.encode(updated)
        )

        val category = decoded.categories.last()
        assertEquals("Weekend Plans", category.name)
        assertTrue(category.isCustom)
        assertEquals(listOf("Visit a park"), category.options)
    }

    @Test
    fun `encode and decode preserves winner history`() {
        val original = DecisionMakerOperations.addHistory(
            data = DecisionMakerData(),
            categoryName = "Morning Routine",
            winners = listOf("Exercise", "Meditation"),
            pickedAtMillis = 123456L,
            id = 10L
        )

        val decoded = DecisionMakerCodec.decode(
            DecisionMakerCodec.encode(original)
        )

        assertEquals(original.history, decoded.history)
    }

    @Test
    fun `empty input returns default data`() {
        val decoded = DecisionMakerCodec.decode("")

        assertEquals(
            DecisionMakerDefaults.categories,
            decoded.categories
        )
        assertTrue(decoded.history.isEmpty())
    }

    @Test
    fun `invalid json returns default data`() {
        val decoded = DecisionMakerCodec.decode("{invalid-json")

        assertEquals(
            DecisionMakerDefaults.categories,
            decoded.categories
        )
        assertTrue(decoded.history.isEmpty())
    }

    @Test
    fun `decoder ignores unknown json fields`() {
        val decoded = DecisionMakerCodec.decode(
            """{"categories":[],"history":[],"futureField":"test"}"""
        )

        assertTrue(decoded.categories.isEmpty())
        assertTrue(decoded.history.isEmpty())
    }
}
