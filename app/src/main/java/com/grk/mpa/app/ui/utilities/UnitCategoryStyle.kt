package com.grk.mpa.app.ui.utilities

import androidx.compose.ui.graphics.Color

/** Colours and badge for one unit category. Colours are 0xAARRGGBB so they can be unit tested. */
data class UnitCategoryStyle(
    val emoji: String,
    val lightTint: Long,
    val darkTint: Long,
    val lightAccent: Long,
    val darkAccent: Long
) {
    fun tint(dark: Boolean) = Color(if (dark) darkTint else lightTint)
    fun accent(dark: Boolean) = Color(if (dark) darkAccent else lightAccent)
}

object UnitCategoryStyles {
    private val fallback = UnitCategoryStyle("🔁", 0xFFE6EBF2, 0xFF262B36, 0xFF5B6B85, 0xFF9DB0CF)

    private val styles = mapOf(
        "length" to UnitCategoryStyle("📏", 0xFFE3EEFF, 0xFF1F2A40, 0xFF2457C5, 0xFF9DB9FF),
        "time" to UnitCategoryStyle("⏱️", 0xFFEDE7FF, 0xFF2A2440, 0xFF6A4FC4, 0xFFC6B6FF),
        "weight" to UnitCategoryStyle("⚖️", 0xFFFFEBDD, 0xFF3D2A20, 0xFFC4602A, 0xFFFFBE99),
        "temperature" to UnitCategoryStyle("🌡️", 0xFFFFE4E1, 0xFF3D2423, 0xFFC4403A, 0xFFFFB2AD),
        "volume" to UnitCategoryStyle("🧪", 0xFFDDF4F0, 0xFF1B3330, 0xFF00796B, 0xFF78D8CA),
        "speed" to UnitCategoryStyle("🚀", 0xFFFFF4D1, 0xFF3A331A, 0xFF9A7400, 0xFFF2D36B),
        "area" to UnitCategoryStyle("🗺️", 0xFFE4F5DC, 0xFF1F3320, 0xFF3F8A22, 0xFFA5DB8C),
        "data" to UnitCategoryStyle("💾", 0xFFE6EBF2, 0xFF262B36, 0xFF5B6B85, 0xFF9DB0CF)
    )

    fun of(categoryId: String): UnitCategoryStyle = styles[categoryId] ?: fallback
}
