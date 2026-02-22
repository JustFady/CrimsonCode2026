package org.crimsoncode2026.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.crimsoncode2026.data.Category

/**
 * Category icon mappings
 *
 * Icons based on technical specification:
 * - Medical: Cross (LocalHospital)
 * - Fire: Flame (LocalFireDepartment)
 * - Weather: Cloud with lightning (Thunderstorm)
 * - Crime: Shield (Shield)
 * - Natural Disaster: Mountain (Terrain)
 * - Infrastructure: Gear (Build)
 * - Search & Rescue: Magnifying glass (Search)
 * - Traffic: Car (DirectionsCar)
 * - Other: Question mark (Help)
 */

internal val CategoryIconVector: ImageVector
    get() = when (this) {
        Category.MEDICAL -> Icons.Filled.LocalHospital
        Category.FIRE -> Icons.Filled.LocalFireDepartment
        Category.WEATHER -> Icons.Filled.Thunderstorm
        Category.CRIME -> Icons.Filled.Shield
        Category.NATURAL_DISASTER -> Icons.Filled.Terrain
        Category.INFRASTRUCTURE -> Icons.Filled.Build
        Category.SEARCH_RESCUE -> Icons.Filled.Search
        Category.TRAFFIC -> Icons.Filled.DirectionsCar
        Category.OTHER -> Icons.Filled.Help
    }

/**
 * Displays a category icon with styling
 *
 * @param category The event category
 * @param contentDescription Accessibility description (defaults to category display name)
 * @param modifier Modifier for the icon
 * @param iconSize Size of the icon (default 24dp)
 * @param backgroundColor Background color behind the icon (defaults to category color)
 * @param tintColor Tint color for the icon (defaults to white or category color depending on background)
 */
@Composable
fun CategoryIcon(
    category: Category,
    contentDescription: String? = category.displayName,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    backgroundColor: Color = category.CategoryColor,
    tintColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .size(iconSize + 8.dp)
            .background(backgroundColor, CircleShape)
    ) {
        Icon(
            imageVector = category.CategoryIconVector,
            contentDescription = contentDescription,
            tint = tintColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Displays a category icon without background circle
 *
 * @param category The event category
 * @param contentDescription Accessibility description (defaults to category display name)
 * @param modifier Modifier for the icon
 * @param size Size of the icon (default 24dp)
 * @param tintColor Tint color for the icon (defaults to category color)
 */
@Composable
fun CategoryIconSimple(
    category: Category,
    contentDescription: String? = category.displayName,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tintColor: Color = category.CategoryColor
) {
    Icon(
        imageVector = category.CategoryIconVector,
        contentDescription = contentDescription,
        tint = tintColor,
        modifier = modifier.size(size)
    )
}
