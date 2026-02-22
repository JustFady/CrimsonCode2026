package org.crimsoncode2026.screens.publicevents

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.crimsoncode2026.data.BroadcastType
import org.crimsoncode2026.data.Category
import org.crimsoncode2026.data.Event
import org.crimsoncode2026.data.Severity
import org.crimsoncode2026.data.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EventListItem(
    val event: Event,
    val creator: User? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListView(
    events: List<EventListItem>,
    onEventClick: (Event) -> Unit = {},
    onClearAll: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    var severityFilter by remember { mutableStateOf<Severity?>(null) }
    var broadcastFilter by remember { mutableStateOf<BroadcastType?>(null) }
    var categoryFilter by remember { mutableStateOf<Category?>(null) }

    val filtered = events.filter { item ->
        (severityFilter == null || item.event.severityEnum == severityFilter) &&
            (broadcastFilter == null || item.event.broadcastTypeEnum == broadcastFilter) &&
            (categoryFilter == null || item.event.categoryEnum == categoryFilter)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Events (${filtered.size})") },
            navigationIcon = {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            },
            actions = {
                if (filtered.isNotEmpty()) {
                    TextButton(onClick = onClearAll) { Text("Clear All") }
                }
            }
        )

        FilterBar(
            severityFilter = severityFilter,
            broadcastFilter = broadcastFilter,
            categoryFilter = categoryFilter,
            onSeverityChange = { severityFilter = it },
            onBroadcastChange = { broadcastFilter = it },
            onCategoryChange = { categoryFilter = it }
        )

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.List, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(if (events.isEmpty()) "No events yet" else "No matching events", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (events.isEmpty()) {
                            "Tap + on the map to create the first event, or wait for nearby public events."
                        } else {
                            "Try clearing one or more filters."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtered, key = { it.event.id }) { item ->
                    EventRow(item, onClick = { onEventClick(item.event) })
                }
            }
        }
    }
}

@Composable
private fun FilterBar(
    severityFilter: Severity?,
    broadcastFilter: BroadcastType?,
    categoryFilter: Category?,
    onSeverityChange: (Severity?) -> Unit,
    onBroadcastChange: (BroadcastType?) -> Unit,
    onCategoryChange: (Category?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = severityFilter == null, onClick = { onSeverityChange(null) }, label = { Text("All") })
            FilterChip(selected = severityFilter == Severity.ALERT, onClick = { onSeverityChange(Severity.ALERT) }, label = { Text("Alert") })
            FilterChip(selected = severityFilter == Severity.CRISIS, onClick = { onSeverityChange(Severity.CRISIS) }, label = { Text("Crisis") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = broadcastFilter == null, onClick = { onBroadcastChange(null) }, label = { Text("Any") })
            FilterChip(selected = broadcastFilter == BroadcastType.PUBLIC, onClick = { onBroadcastChange(BroadcastType.PUBLIC) }, label = { Text("Public") })
            FilterChip(selected = broadcastFilter == BroadcastType.PRIVATE, onClick = { onBroadcastChange(BroadcastType.PRIVATE) }, label = { Text("Private") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = categoryFilter == null, onClick = { onCategoryChange(null) }, label = { Text("All Categories") })
            FilterChip(selected = categoryFilter == Category.MEDICAL, onClick = { onCategoryChange(Category.MEDICAL) }, label = { Text("Medical") })
            FilterChip(selected = categoryFilter == Category.FIRE, onClick = { onCategoryChange(Category.FIRE) }, label = { Text("Fire") })
        }
    }
}

@Composable
private fun EventRow(item: EventListItem, onClick: () -> Unit) {
    val event = item.event
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            when (event.severityEnum) {
                                Severity.CRISIS -> Color(0xFFDC2626)
                                Severity.ALERT -> Color(0xFFF97316)
                                null -> Color(0xFF6B7280)
                            },
                            CircleShape
                        )
                )
                Spacer(Modifier.size(8.dp))
                Text(event.categoryEnum?.displayName ?: "Unknown", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (event.isPrivate) Color(0xFFEDE7F6) else Color(0xFFE3F2FD)
                ) {
                    Text(
                        if (event.isPrivate) "Private" else "Public",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Text(
                event.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.creator?.displayName ?: if (event.isPublic) "Anonymous" else "Unknown",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.weight(1f))
                Text(formatRelativeTime(event.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val diffMinutes = (System.currentTimeMillis() - timestamp) / 60000L
    return when {
        diffMinutes < 1 -> "Just now"
        diffMinutes < 60 -> "${diffMinutes}m ago"
        diffMinutes < 24 * 60 -> "${diffMinutes / 60}h ago"
        else -> SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp))
    }
}
