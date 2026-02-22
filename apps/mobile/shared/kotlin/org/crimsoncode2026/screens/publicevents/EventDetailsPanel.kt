package org.crimsoncode2026.screens.publicevents

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.crimsoncode2026.data.Category
import org.crimsoncode2026.data.Event
import org.crimsoncode2026.data.Severity
import org.crimsoncode2026.data.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Event Details", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BadgeChip(
                    label = event.severityEnum?.name ?: "UNKNOWN",
                    color = severityColor(event.severityEnum)
                )
                BadgeChip(
                    label = event.categoryEnum?.displayName ?: "Unknown",
                    color = categoryColor(event.categoryEnum)
                )
                BadgeChip(
                    label = if (event.isPrivate) "Private" else "Public",
                    color = if (event.isPrivate) Color(0xFF6A1B9A) else Color(0xFF1565C0)
                )
            }

            InfoCard("Description", event.description.ifBlank { "No description" })
            InfoCard(
                "Location",
                event.locationOverride ?: "${event.lat.formatCoord()}, ${event.lon.formatCoord()}"
            )
            InfoCard("Time", formatEventTimestamp(event.createdAt))
            InfoCard(
                "Reported By",
                when {
                    event.isPublic -> "Anonymous (public event)"
                    creator != null -> creator.displayName
                    else -> "Unknown sender"
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(onClick = onClear, modifier = Modifier.weight(1f)) {
                    Text("Clear")
                }
                Button(onClick = onNavigateToLocation, modifier = Modifier.weight(1f)) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("Navigate")
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun BadgeChip(label: String, color: Color) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

private fun severityColor(severity: Severity?): Color = when (severity) {
    Severity.CRISIS -> Color(0xFFDC2626)
    Severity.ALERT -> Color(0xFFF97316)
    null -> Color(0xFF6B7280)
}

private fun categoryColor(category: Category?): Color = when (category) {
    Category.MEDICAL -> Color(0xFFD32F2F)
    Category.FIRE -> Color(0xFFEF6C00)
    Category.WEATHER -> Color(0xFF1976D2)
    Category.CRIME -> Color(0xFF6A1B9A)
    Category.NATURAL_DISASTER -> Color(0xFF2E7D32)
    Category.INFRASTRUCTURE -> Color(0xFF455A64)
    Category.SEARCH_RESCUE -> Color(0xFF00897B)
    Category.TRAFFIC -> Color(0xFF5D4037)
    Category.OTHER -> Color(0xFF546E7A)
    null -> Color(0xFF6B7280)
}

private fun formatEventTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

private fun Double.formatCoord(): String = String.format(Locale.US, "%.5f", this)
