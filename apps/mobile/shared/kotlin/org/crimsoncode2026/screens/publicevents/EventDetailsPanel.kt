package org.crimsoncode2026.screens.publicevents

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.crimsoncode2026.data.BroadcastType
import org.crimsoncode2026.data.Category
import org.crimsoncode2026.data.Event
import org.crimsoncode2026.data.Severity
import org.crimsoncode2026.data.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Event Details Bottom Panel
 *
 * Features:
 * - Bottom sheet panel showing event details
 * - Severity, category, description, location, time
 * - Creator display for private events
 * - Anonymous display for public events (no creator info)
 * - "Clear from list" button for local dismissal
 *
 * @param event The event to display
 * @param creator User who created the event (for private events, null for public)
 * @param onDismiss Callback when panel is dismissed
 * @param onClear Callback when user clears event from their list (local cache only)
 * @param onNavigateToLocation Callback when user wants to navigate to event location
 */
@Composable
fun EventDetailsPanel(
    event: Event,
    creator: User?,
    onDismiss: () -> Unit = {},
    onClear: () -> Unit = {},
    onNavigateToLocation: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with dismiss button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Event Details",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Drag handle indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Event header with severity and category
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Severity badge
                Surface(
                    color = severityColor(event.severityEnum),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = severityName(event.severityEnum),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Category icon
                Surface(
                    color = categoryColor(event.categoryEnum).copy(alpha = 0.1f),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = categoryIcon(event.categoryEnum),
                        contentDescription = null,
                        tint = categoryColor(event.categoryEnum),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = event.categoryEnum?.displayName ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium,
                    color = categoryColor(event.categoryEnum)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Broadcast type indicator
                if (event.isPrivate) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Private",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Public",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Creator info (for private events only)
            if (event.isPrivate && creator != null) {
                CreatorInfoCard(creator)
                Spacer(modifier = Modifier.height(12.dp))
            } else if (event.isPublic) {
                AnonymousInfoCard()
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Description
            if (event.description.isNotBlank()) {
                DescriptionCard(event.description)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Location and time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LocationTimeCard(
                    location = event.locationOverride ?: formatCoordinates(event.lat, event.lon),
                    timestamp = event.createdAt
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear from List")
                }

                androidx.compose.material3.Button(
                    onClick = onNavigateToLocation,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Navigate to Location")
                }
            }
        }
    }
}

/**
 * Creator info card for private events
 */
@Composable
private fun CreatorInfoCard(creator: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with initials
            val initials = creator.displayName
                .split(" ")
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
                .joinToString("")

            Surface(
                color = MaterialTheme.colorScheme.secondary,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials.ifEmpty { "?" },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = creator.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Sent by contact",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Anonymous info card for public events
 */
@Composable
private fun AnonymousInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Anonymous Public Alert",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Description card
 */
@Composable
private fun DescriptionCard(description: String) {
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
        }
    }
}

/**
 * Location and time card
 */
@Composable
private fun LocationTimeCard(
    location: String,
    timestamp: Long
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Location
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Location",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1
                    )
                }
            }
        }

        // Time
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.filled.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Time",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTimestamp(timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Format coordinates for display
 */
private fun formatCoordinates(lat: Double, lon: Double): String {
    return String.format("%.4f, %.4f", lat, lon)
}

/**
 * Format timestamp for display
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffMs = now - timestamp
    val diffMinutes = diffMs / (1000 * 60)
    val diffHours = diffMs / (1000 * 60 * 60)

    return when {
        diffMinutes < 1 -> "Just now"
        diffMinutes < 60 -> "${diffMinutes.toInt()} min ago"
        diffHours < 24 -> "${diffHours.toInt()}h ago"
        else -> {
            val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            formatter.format(Date(timestamp))
        }
    }
}

/**
 * Get display name for severity
 */
private fun severityName(severity: Severity?): String {
    return when (severity) {
        Severity.ALERT -> "Warning"
        Severity.CRISIS -> "Crisis"
        null -> "Unknown"
    }
}

/**
 * Get color for severity
 */
private fun severityColor(severity: Severity?): Color {
    return when (severity) {
        Severity.ALERT -> Color(0xFFF97316) // Orange
        Severity.CRISIS -> Color(0xFFDC2626) // Red
        null -> MaterialTheme.colorScheme.outline
    }
}

/**
 * Get icon for category
 */
private fun categoryIcon(category: Category?): androidx.compose.ui.graphics.vector.ImageVector {
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
        null -> androidx.compose.material.icons.filled.QuestionMark
    }
}

/**
 * Get color for category
 */
private fun categoryColor(category: Category?): Color {
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
        null -> MaterialTheme.colorScheme.outline
    }
}
