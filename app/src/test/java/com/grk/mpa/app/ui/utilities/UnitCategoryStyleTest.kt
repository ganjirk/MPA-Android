package com.grk.mpa.app.ui.utilities

import com.grk.mpa.app.core.UnitCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnitCategoryStyleTest {
    @Test
    fun everyCategoryHasItsOwnStyle() {
        val styles = UnitCategories.all.map { UnitCategoryStyles.of(it.id) }
        assertEquals(UnitCategories.all.size, styles.map { it.lightTint }.toSet().size)
        assertEquals(UnitCategories.all.size, styles.map { it.emoji }.toSet().size)
    }

    @Test
    fun unknownCategoryFallsBack() {
        assertTrue(UnitCategoryStyles.of("nope").emoji.isNotEmpty())
    }

    @Test
    fun darkTintsAreDarkerThanLightTints() {
        UnitCategories.all.forEach {
            val s = UnitCategoryStyles.of(it.id)
            assertNotEquals(s.lightTint, s.darkTint)
            assertTrue(s.darkTint and 0xFF < 0x80)
            assertTrue(s.lightTint and 0xFF > 0xC0)
        }
    }
}
