package org.crimsoncode2026.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.crimsoncode2026.data.Category

/**
 * Category selection grid for event creation
 *
 * Displays a 3x3 grid of selectable category icons.
 * Each item shows the category icon and label.
 * Selection highlights the chosen category.
 * Grid is responsive for different screen sizes.
 *
 * @param selectedCategory Currently selected category (null for none)
 * @param onCategorySelected Callback when a category is selected
 * @param modifier Modifier for the grid
 * @param itemPadding Padding around each grid item
 * @param gridSpacing Spacing between grid items
 */
@Composable
fun CategorySelectionGrid(
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit,
    modifier: Modifier = Modifier,
    itemPadding: Dp = 8.dp,
    gridSpacing: Dp = 8.dp
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(gridSpacing)
    ) {
        // Row 1: Medical, Fire, Weather
        CategoryGridRow(
            categories = listOf(Category.MEDICAL, Category.FIRE, Category.WEATHER),
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected,
            itemPadding = itemPadding,
            gridSpacing = gridSpacing
        )

        // Row 2: Crime, Natural Disaster, Infrastructure
        CategoryGridRow(
            categories = listOf(Category.CRIME, Category.NATURAL_DISASTER, Category.INFRASTRUCTURE),
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected,
            itemPadding = itemPadding,
            gridSpacing = gridSpacing
        )

        // Row 3: Search & Rescue, Traffic, Other
        CategoryGridRow(
            categories = listOf(Category.SEARCH_RESCUE, Category.TRAFFIC, Category.OTHER),
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected,
            itemPadding = itemPadding,
            gridSpacing = gridSpacing
        )
    }
}

@Composable
private fun CategoryGridRow(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit,
    itemPadding: Dp,
    gridSpacing: Dp
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        categories.forEach { category ->
            CategoryGridItem(
                category = category,
                isSelected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                modifier = Modifier.weight(1f),
                itemPadding = itemPadding
            )
            if (category != categories.last()) {
                Spacer(modifier = Modifier.width(gridSpacing))
            }
        }
    }
}

@Composable
private fun CategoryGridItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    itemPadding: Dp = 8.dp
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = modifier
            .padding(itemPadding)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        ),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CategoryIcon(
                category = category,
                iconSize = 32.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = category.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}
