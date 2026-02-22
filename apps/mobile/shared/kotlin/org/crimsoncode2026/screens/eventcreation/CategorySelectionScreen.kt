package org.crimsoncode2026.screens.eventcreation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.crimsoncode2026.data.Category

/**
 * Category selection screen for event creation wizard step 1.
 *
 * Features:
 * - Grid layout of all 9 event categories
 * - Category icon and label display
 * - Category name displayed on selection
 * - Emits selected category callback
 *
 * Categories (from spec):
 * - Medical: Red, Medical cross icon
 * - Fire: Orange, Flame icon
 * - Weather: Purple, Cloud with lightning icon
 * - Crime: Red, Shield icon
 * - Natural Disaster: Dark red, Mountain peak icon
 * - Infrastructure: Yellow, Gear icon
 * - Search & Rescue: Bright red, Magnifying glass icon
 * - Traffic: Green, Car icon
 * - Other: Blue, Question mark icon
 *
 * @param onCategorySelected Callback when user selects a category
 * @param onCancel Callback when user cancels selection
 */
@Composable
fun CategorySelectionScreen(
    onCategorySelected: (Category) -> Unit,
    onCancel: () -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            CategorySelectionHeader(
                onCancel = onCancel,
                selectedCategory = selectedCategory
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(Category.entries) { category ->
                    CategoryGridItem(
                        category = category,
                        isSelected = selectedCategory == category,
                        onClick = {
                            selectedCategory = category
                        }
                    )
                }
            }
        }

        // Continue button (shows after selection)
        if (selectedCategory != null) {
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 8.dp
            ) {
                androidx.compose.material3.Button(
                    onClick = { selectedCategory?.let(onCategorySelected) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    Text("Continue with ${selectedCategory?.displayName}")
                }
            }
        }
    }
}

/**
 * Header for category selection screen
 */
@Composable
private fun CategorySelectionHeader(
    onCancel: () -> Unit,
    selectedCategory: Category?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel")
            }
            Text(
                text = "Select Category",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "What type of emergency are you reporting?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // Show selected category description below
        if (selectedCategory != null) {
            Spacer(modifier = Modifier.height(16.dp))
            CategoryDescriptionCard(selectedCategory)
        }
    }
}

/**
 * Category description card showing selected category details
 */
@Composable
private fun CategoryDescriptionCard(category: Category) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = categoryColor(category).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = categoryColor(category),
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CategoryIcon(category)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = categoryColor(category)
                )
                Text(
                    text = categoryDescription(category),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Single category grid item
 */
@Composable
private fun CategoryGridItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (isSelected) 3.dp else 1.dp,
            color = if (isSelected) {
                categoryColor(category)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                categoryColor(category).copy(alpha = 0.15f)
            } else {
                categoryColor(category).copy(alpha = 0.05f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = categoryColor(category),
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CategoryIcon(category)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = category.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Category icon based on category type
 */
@Composable
private fun CategoryIcon(category: Category) {
    Icon(
        imageVector = categoryIcon(category),
        contentDescription = category.displayName,
        tint = Color.White,
        modifier = Modifier.size(28.dp)
    )
}

/**
 * Get icon for each category
 */
private fun categoryIcon(category: Category): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category) {
        Category.MEDICAL -> androidx.compose.material.icons.filled.LocalHospital
        Category.FIRE -> androidx.compose.material.icons.filled.LocalFireDepartment
        Category.WEATHER -> androidx.compose.material.icons.filled.Cloud
        Category.CRIME -> androidx.compose.material.icons.filled.Shield
        Category.NATURAL_DISASTER -> androidx.compose.material.icons.filled.Landscape
        Category.INFRASTRUCTURE -> androidx.compose.material.icons.filled.Settings
        Category.SEARCH_RESCUE -> androidx.compose.material.icons.filled.Search
        Category.TRAFFIC -> androidx.compose.material.icons.filled.DirectionsCar
        Category.OTHER -> androidx.compose.material.icons.filled.Help
    }
}

/**
 * Get color for each category (from spec)
 */
private fun categoryColor(category: Category): Color {
    return when (category) {
        Category.MEDICAL -> Color(0xFFEF4444) // Red
        Category.FIRE -> Color(0xFFF97316) // Orange
        Category.WEATHER -> Color(0xFF9333EA) // Purple
        Category.CRIME -> Color(0xFFDC2626) // Red
        Category.NATURAL_DISASTER -> Color(0xFF991B1B) // Dark red
        Category.INFRASTRUCTURE -> Color(0xFFEAB308) // Yellow
        Category.SEARCH_RESCUE -> Color(0xFFEF4444) // Bright red
        Category.TRAFFIC -> Color(0xFF22C55E) // Green
        Category.OTHER -> Color(0xFF3B82F6) // Blue
    }
}

/**
 * Get description for each category
 */
private fun categoryDescription(category: Category): String {
    return when (category) {
        Category.MEDICAL -> "Medical emergencies requiring immediate assistance"
        Category.FIRE -> "Fire incidents or fire hazards"
        Category.WEATHER -> "Severe weather events (storms, floods, etc.)"
        Category.CRIME -> "Dangerous or criminal activity"
        Category.NATURAL_DISASTER -> "Earthquakes, tornadoes, or other disasters"
        Category.INFRASTRUCTURE -> "Power outages, water issues, or infrastructure failures"
        Category.SEARCH_RESCUE -> "Missing persons or rescue operations"
        Category.TRAFFIC -> "Accidents, road closures, or traffic hazards"
        Category.OTHER -> "Other emergency situations not covered above"
    }
}
