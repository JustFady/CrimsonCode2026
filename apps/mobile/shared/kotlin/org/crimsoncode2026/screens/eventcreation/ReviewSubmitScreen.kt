package org.crimsoncode2026.screens.eventcreation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.crimsoncode2026.data.BroadcastType
import org.crimsoncode2026.data.Category
import org.crimsoncode2026.data.Severity

/**
 * Data class containing all event data to review
 */
data class EventReviewData(
    val category: Category,
    val severity: Severity,
    val broadcastScope: BroadcastType,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val description: String
)

/**
 * Review and submit screen for event creation wizard step 6.
 *
 * Features:
 * - Summary of all selections: category, severity, broadcast scope, location, description
 * - "Create Alert" primary button
 * - "Cancel" button
 * - Loading state support
 * - Error state support
 *
 * @param reviewData Event data to review
 * @param onSubmit Callback when user submits the event
 * @param onCancel Callback when user cancels the event creation
 * @param isLoading Whether submission is in progress
 * @param errorMessage Optional error message to display
 * @param onRetry Optional callback for retry action
 * @param isRetrying Whether a retry is in progress
 */
@Composable
fun ReviewSubmitScreen(
    reviewData: EventReviewData,
    onSubmit: () -> Unit,
    onCancel: () -> Unit = {},
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    isRetrying: Boolean = false
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            ReviewSubmitHeader(onCancel = onCancel)

            Spacer(modifier = Modifier.height(16.dp))

            // Review content (scrollable)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category card
                ReviewDetailCard(
                    title = "Category",
                    value = reviewData.category.displayName,
                    icon = categoryIcon(reviewData.category),
                    iconColor = categoryColor(reviewData.category)
                )

                // Severity card
                ReviewDetailCard(
                    title = "Severity",
                    value = severityName(reviewData.severity),
                    icon = androidx.compose.material.icons.filled.Warning,
                    iconColor = severityColor(reviewData.severity)
                )

                // Broadcast scope card
                ReviewDetailCard(
                    title = "Broadcast Scope",
                    value = scopeName(reviewData.broadcastScope),
                    icon = if (reviewData.broadcastScope == BroadcastType.PUBLIC) {
                        androidx.compose.material.icons.filled.Public
                    } else {
                        androidx.compose.material.icons.filled.People
                    },
                    iconColor = scopeColor(reviewData.broadcastScope)
                )

                // Location card
                ReviewDetailCard(
                    title = "Location",
                    value = reviewData.location,
                    icon = Icons.Default.LocationOn,
                    iconColor = MaterialTheme.colorScheme.primary
                )

                // Description card
                ReviewDescriptionCard(description = reviewData.description)

                // Error message if present
                if (errorMessage != null) {
                    ErrorCard(
                        message = errorMessage,
                        onRetry = onRetry,
                        isRetrying = isRetrying
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Submit button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onCancel,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = onSubmit,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Creating...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Alert")
                    }
                }
            }
        }
    }
}

/**
 * Header for review and submit screen
 */
@Composable
private fun ReviewSubmitHeader(onCancel: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel")
            }
            Text(
                text = "Review Event",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Review your emergency alert details before creating",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Card displaying a single review detail
 */
@Composable
private fun ReviewDetailCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = iconColor,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Card displaying the event description
 */
@Composable
private fun ReviewDescriptionCard(description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Description",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${description.length}/500 characters",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

/**
 * Error card for displaying submission errors
 */
@Composable
private fun ErrorCard(
    message: String,
    onRetry: (() -> Unit)? = null,
    isRetrying: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.error
        )
    ) {
        if (onRetry != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Button(
                    onClick = onRetry,
                    enabled = !isRetrying,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    if (isRetrying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retrying...", style = MaterialTheme.typography.labelSmall)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

/**
 * Helper functions for display names and colors
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

private fun categoryColor(category: Category): Color {
    return when (category) {
        Category.MEDICAL -> Color(0xFFEF4444)
        Category.FIRE -> Color(0xFFF97316)
        Category.WEATHER -> Color(0xFF9333EA)
        Category.CRIME -> Color(0xFFDC2626)
        Category.NATURAL_DISASTER -> Color(0xFF991B1B)
        Category.INFRASTRUCTURE -> Color(0xFFEAB308)
        Category.SEARCH_RESCUE -> Color(0xFFEF4444)
        Category.TRAFFIC -> Color(0xFF22C55E)
        Category.OTHER -> Color(0xFF3B82F6)
    }
}

private fun severityName(severity: Severity): String {
    return when (severity) {
        Severity.ALERT -> "Warning"
        Severity.CRISIS -> "Crisis"
    }
}

private fun severityColor(severity: Severity): Color {
    return when (severity) {
        Severity.ALERT -> Color(0xFFF97316)
        Severity.CRISIS -> Color(0xFFDC2626)
    }
}

private fun scopeName(scope: BroadcastType): String {
    return when (scope) {
        BroadcastType.PUBLIC -> "Public (50-mile radius)"
        BroadcastType.PRIVATE -> "Private (selected contacts)"
    }
}

private fun scopeColor(scope: BroadcastType): Color {
    return when (scope) {
        BroadcastType.PUBLIC -> Color(0xFF3B82F6)
        BroadcastType.PRIVATE -> Color(0xFF8B5CF6)
    }
}
