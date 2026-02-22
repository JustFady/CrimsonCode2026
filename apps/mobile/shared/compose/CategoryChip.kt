package org.crimsoncode2026.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.crimsoncode2026.data.Category

/**
 * Category chip composable for event list filtering
 *
 * Displays a category as a clickable filter chip showing:
 * - Category icon
 * - Category name
 * - Optional close button for removing filter
 *
 * Used in event list filtering UI. Selected state shows active styling.
 *
 * @param category The event category
 * @param isSelected Whether this chip is currently selected/active
 * @param onClick Callback when chip is clicked
 * @param onClose Callback when close button is clicked (null to hide close button)
 * @param modifier Modifier for the chip
 */
@Composable
fun CategoryChip(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryIconSimple(
                    category = category,
                    size = 16.dp,
                    tintColor = if (isSelected) {
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = category.CategoryColor,
                            selectedLabelColor = category.CategoryColor
                        ).selectedLabelColor
                    } else {
                        category.CategoryColor
                    }
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = category.displayName,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                if (onClose != null) {
                    Spacer(modifier = Modifier.width(4.dp))

                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove filter",
                        modifier = Modifier
                            .width(16.dp)
                            .clickable(onClick = onClose)
                    )
                }
            }
        },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = category.CategoryColor,
            selectedLabelColor = category.CategoryColor,
            selectedLeadingIconColor = category.CategoryColor
        ),
        border = null
    )
}
