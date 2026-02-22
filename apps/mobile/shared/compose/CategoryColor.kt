package org.crimsoncode2026.compose

import androidx.compose.ui.graphics.Color
import org.crimsoncode2026.data.Category

/**
 * Color mappings for event categories
 *
 * Based on technical specification:
 * - Medical: Red
 * - Fire: Orange
 * - Weather: Purple
 * - Crime: Red
 * - Natural Disaster: Dark Red
 * - Infrastructure: Yellow
 * - Search & Rescue: Bright Red
 * - Traffic: Green
 * - Other: Blue
 */

// Category colors
val CategoryColor: Color
    get() = when (this) {
        Category.MEDICAL -> CategoryColors.Medical
        Category.FIRE -> CategoryColors.Fire
        Category.WEATHER -> CategoryColors.Weather
        Category.CRIME -> CategoryColors.Crime
        Category.NATURAL_DISASTER -> CategoryColors.NaturalDisaster
        Category.INFRASTRUCTURE -> CategoryColors.Infrastructure
        Category.SEARCH_RESCUE -> CategoryColors.SearchRescue
        Category.TRAFFIC -> CategoryColors.Traffic
        Category.OTHER -> CategoryColors.Other
    }

object CategoryColors {
    val Medical = Color(0xFFFF0000)
    val Fire = Color(0xFFFF6600)
    val Weather = Color(0xFF9C27B0)
    val Crime = Color(0xFFFF0000)
    val NaturalDisaster = Color(0xFF8B0000)
    val Infrastructure = Color(0xFFFFC107)
    val SearchRescue = Color(0xFFFF4444)
    val Traffic = Color(0xFF4CAF50)
    val Other = Color(0xFF2196F3)

    /**
     * Color for selected state in UI elements
     */
    val Selected = Color(0xFF1565C0)
}

/**
 * Gets color for a category, with optional selected state override
 */
fun getCategoryColor(category: Category, isSelected: Boolean = false): Color {
    return if (isSelected) CategoryColors.Selected else category.CategoryColor
}
