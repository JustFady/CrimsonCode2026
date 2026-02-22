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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.crimsoncode2026.data.Event
import org.crimsoncode2026.data.Severity
import org.crimsoncode2026.data.Category
import org.crimsoncode2026.data.User
import org.crimsoncode2026.data.BroadcastType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Event item in the list with event preview
 */
data class EventListItem(
    val event: Event,
    val creator: User? = null
)

/**
 * Event List View Composable
 *
 * Features:
 * - List of received private events and nearby public events
 * - Tapping event zooms map to location
 * - "Clear All" button to remove all events from list (local cache only)
 * - Shows event severity, category, description preview, time
 * - Separate sections for private and public events
 * - Filter options for severity, category, and broadcast type
 *
 * @param events List of events to display with creator info for private events
 * @param onEventClick Callback when user taps an event
 * @param onClearAll Callback when user taps Clear All button
 * @param onDismiss Callback when user dismisses the list view
 * @param selectedSeverity Selected severity filter (null = show all)
 * @param selectedCategory Selected category filter (null = show all)
 * @param selectedBroadcastType Selected broadcast type filter (null = show all)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListView(
    events: List<EventListItem>,
    onEventClick: (Event) -> Unit = {},
    onClearAll: () -> Unit = {},
    onDismiss: () -> Unit = {},
    selectedSeverity: Severity? = null,
    selectedCategory: Category? = null,
    selectedBroadcastType: BroadcastType? = null
) {
    val scrollState = rememberLazyListState()
    val topAppBarState = rememberTopAppBarState()

    // Filter events based on selected filters
    val privateEvents = events.filter { item ->
        item.event.isPrivate &&
        matchesSeverity(item.event.severityEnum) &&
        matchesCategory(item.event.categoryEnum)
    }
    val publicEvents = events.filter { item ->
        item.event.isPublic &&
        matchesSeverity(item.event.severityEnum) &&
        matchesCategory(item.event.categoryEnum) &&
        matchesBroadcastType(item.event.broadcastTypeEnum)
    }

    /**
     * Check if event severity matches the selected filter
     */
    fun matchesSeverity(severity: Severity?): Boolean {
        return selectedSeverity == null || severity == selectedSeverity
    }

    /**
     * Check if event category matches the selected filter
     */
    fun matchesCategory(category: Category?): Boolean {
        return selectedCategory == null || category == selectedCategory
    }

    /**
     * Check if event broadcast type matches the selected filter
     */
    fun matchesBroadcastType(type: BroadcastType?): Boolean {
        return selectedBroadcastType == null || type == selectedBroadcastType
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top app bar
            EventListTopAppBar(
                eventCount = events.size,
                onDismiss = onDismiss,
                onClearAll = if (events.isNotEmpty()) onClearAll else null,
                scrollBehavior = topAppBarState
            )

            // Filter chips row
            FilterChipsRow(
                selectedSeverity = selectedSeverity,
                selectedCategory = selectedCategory,
                selectedBroadcastType = selectedBroadcastType,
                onSeveritySelected = { /* TODO: Update filter state */ },
                onCategorySelected = { /* TODO: Update filter state */ },
                onBroadcastTypeSelected = { /* TODO: Update filter state */ }
            )

            // Event list
            if (events.isEmpty()) {
                EmptyEventsList(
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Private events section
                    if (privateEvents.isNotEmpty()) {
                        item {
                            EventSectionHeader("Private Events")
                        }

                        items(
                            items = privateEvents,
                            key = { it.event.id }
                        ) { item ->
                            EventListItemCard(
                                event = item.event,
                                creator = item.creator,
                                onClick = { onEventClick(item.event) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Public events section
                    if (publicEvents.isNotEmpty()) {
                        item {
                            EventSectionHeader("Public Events Nearby")
                        }

                        items(
                            items = publicEvents,
                            key = { it.event.id }
                        ) { item ->
                            EventListItemCard(
                                event = item.event,
                                creator = null,
                                onClick = { onEventClick(item.event) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Top app bar for event list view
 */
@Composable
private fun EventListTopAppBar(
    eventCount: Int,
    onDismiss: () -> Unit,
    onClearAll: (() -> Unit)?,
    scrollBehavior: TopAppBarScrollBehavior
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("My Events ($eventCount)")
            }
        },
        navigationIcon = {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        },
        actions = {
            onClearAll?.let { clearAll ->
                TextButton(onClick = clearAll) {
                    Text("Clear All")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors()
    )
}

/**
 * Section header for event list
 */
@Composable
private fun EventSectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
        Spacer(
            modifier = Modifier
                .height(1.dp)
                .weight(1f)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}

/**
 * Event list item card
 */
@Composable
private fun EventListItemCard(
    event: Event,
    creator: User?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Severity indicator
            Surface(
                color = severityColor(event.severityEnum),
                shape = CircleShape,
                modifier = Modifier.size(12.dp)
            ) {}

            Spacer(modifier = Modifier.width(12.dp))

            // Event info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.categoryEnum?.displayName ?: "Unknown",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Broadcast type indicator
                    if (event.isPrivate) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Private",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Public",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Description preview
                Text(
                    text = event.description.take(60) + if (event.description.length > 60) "..." else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Creator info (private only) and time
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (event.isPrivate && creator != null) {
                        val initials = creator.displayName
                            .split(" ")
                            .take(2)
                            .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
                            .joinToString("")

                        Surface(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = CircleShape,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials.ifEmpty { "?" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = creator.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                    } else if (event.isPublic) {
                        Icon(
                            imageVector = androidx.compose.material.icons.filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Anonymous",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Time
                    Text(
                        text = formatTimestamp(event.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Chevron icon
            Icon(
                imageVector = androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/**
 * Empty state when no events are in the list
 */
@Composable
private fun EmptyEventsList(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        padding = 32.dp
    ) {
        Icon(
            imageVector = Icons.Default.Event,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Events",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "You haven't received any emergency alerts yet.\nTap the + button to create one.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
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
        diffMinutes < 60 -> "${diffMinutes.toInt()}m ago"
        diffHours < 24 -> "${diffHours.toInt()}h ago"
        else -> {
            val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            formatter.format(Date(timestamp))
        }
    }
}

/**
 * Get color for severity
 */
private fun severityColor(severity: org.crimsoncode2026.data.Severity?): androidx.compose.ui.graphics.Color {
    return when (severity) {
        org.crimsoncode2026.data.Severity.ALERT -> androidx.compose.ui.graphics.Color(0xFFF97316) // Orange
        org.crimsoncode2026.data.Severity.CRISIS -> androidx.compose.ui.graphics.Color(0xFFDC2626) // Red
        null -> MaterialTheme.colorScheme.outline
    }
}

/**
 * Filter chips row
 *
 * Horizontal scrollable row of filter chips for severity, category, and broadcast type
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipsRow(
    selectedSeverity: Severity?,
    selectedCategory: Category?,
    selectedBroadcastType: BroadcastType?,
    onSeveritySelected: (Severity?) -> Unit = {},
    onCategorySelected: (Category?) -> Unit = {},
    onBroadcastTypeSelected: (BroadcastType?) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Severity filter chip
        SeverityFilterChip(
            selected = selectedSeverity,
            onSelected = onSeveritySelected
        )

        // Category filter chip
        CategoryFilterChip(
            selected = selectedCategory,
            onSelected = onCategorySelected
        )

        // Broadcast type filter chip
        BroadcastTypeFilterChip(
            selected = selectedBroadcastType,
            onSelected = onBroadcastTypeSelected
        )
    }
}

/**
 * Severity filter chip
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeverityFilterChip(
    selected: Severity?,
    onSelected: (Severity?) -> Unit = {}
) {
    FilterChip(
        selected = selected != null,
        onClick = { onSelected(if (selected == null) null else null) },
        label = "Severity"
    )
}

/**
 * Category filter chip
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterChip(
    selected: Category?,
    onSelected: (Category?) -> Unit = {}
) {
    FilterChip(
        selected = selected != null,
        onClick = { onSelected(if (selected == null) null else null) },
        label = "Category"
    )
}

/**
 * Broadcast type filter chip
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BroadcastTypeFilterChip(
    selected: BroadcastType?,
    onSelected: (BroadcastType?) -> Unit = {}
) {
    FilterChip(
        selected = selected != null,
        onClick = { onSelected(if (selected == null) null else null) },
        label = "Broadcast"
    )
}
