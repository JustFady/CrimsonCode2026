package org.crimsoncode2026.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.crimsoncode2026.data.Event
import org.crimsoncode2026.data.Severity
import org.crimsoncode2026.data.Category
import org.crimsoncode2026.data.User
import org.crimsoncode2026.data.BroadcastType

/**
 * OPTIMIZED Event List View Composable
 *
 * Performance Optimizations:
 * - Uses derivedStateOf to memoize filtered results
 * - Avoids recomputing filters on every composition
 * - Only recomputes when filters or events change
 * - Reduces filter computation from O(n) on every recomposition to O(1)
 *
 * Spec requirements:
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
 * @param onDismiss Callback when user dismisses list view
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimizedEventListView(
    events: List<EventListItem>,
    onEventClick: (Event) -> Unit = {},
    onClearAll: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val scrollState = rememberLazyListState()
    val topAppBarState = rememberTopAppBarState()

    // Local filter state management
    var selectedSeverity by remember { mutableStateOf<Severity?>(null) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedBroadcastType by remember { mutableStateOf<BroadcastType?>(null) }

    // PERFORMANCE OPTIMIZATION: Use derivedStateOf to memoize filtered results
    // This prevents recomputing filters on every composition
    // Only recomputes when events or any filter changes
    val (privateEvents, publicEvents) by remember(events, selectedSeverity, selectedCategory, selectedBroadcastType) {
        derivedStateOf {
            // Split events into private and public first (O(n))
            val (privEvents, pubEvents) = events.partition { it.event.isPrivate }

            // Apply filters to each group
            val filteredPrivate = privEvents.filter { item ->
                matchesSeverity(item.event.severityEnum, selectedSeverity) &&
                        matchesCategory(item.event.categoryEnum, selectedCategory)
            }

            val filteredPublic = pubEvents.filter { item ->
                matchesSeverity(item.event.severityEnum, selectedSeverity) &&
                        matchesCategory(item.event.categoryEnum, selectedCategory) &&
                        matchesBroadcastType(item.event.broadcastTypeEnum, selectedBroadcastType)
            }

            Pair(filteredPrivate, filteredPublic)
        }
    }

    // Pre-compute event counts for display
    val privateCount by remember(privateEvents) { derivedStateOf { privateEvents.size } }
    val publicCount by remember(publicEvents) { derivedStateOf { publicEvents.size } }
    val totalCount = privateCount.value + publicCount.value

    EventListViewContent(
        events = events,
        privateEvents = privateEvents.value,
        publicEvents = publicEvents.value,
        eventCount = totalCount,
        selectedSeverity = selectedSeverity,
        selectedCategory = selectedCategory,
        selectedBroadcastType = selectedBroadcastType,
        scrollState = scrollState,
        topAppBarState = topAppBarState,
        onEventClick = onEventClick,
        onClearAll = if (events.isNotEmpty()) onClearAll else null,
        onDismiss = onDismiss,
        onSeveritySelected = { selectedSeverity = it },
        onCategorySelected = { selectedCategory = it },
        onBroadcastTypeSelected = { selectedBroadcastType = it }
    )
}

/**
 * Event item in list with event preview
 */
data class EventListItem(
    val event: Event,
    val creator: User? = null
)

/**
 * Content composable for the event list (separated for better composition control)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventListViewContent(
    events: List<EventListItem>,
    privateEvents: List<EventListItem>,
    publicEvents: List<EventListItem>,
    eventCount: Int,
    selectedSeverity: Severity?,
    selectedCategory: Category?,
    selectedBroadcastType: BroadcastType?,
    scrollState: androidx.compose.foundation.lazy.LazyListState,
    topAppBarState: androidx.compose.material3.TopAppBarState,
    onEventClick: (Event) -> Unit,
    onClearAll: (() -> Unit)?,
    onDismiss: () -> Unit,
    onSeveritySelected: (Severity?) -> Unit,
    onCategorySelected: (Category?) -> Unit,
    onBroadcastTypeSelected: (BroadcastType?) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Top app bar
        EventListTopAppBar(
            eventCount = eventCount,
            onDismiss = onDismiss,
            onClearAll = onClearAll,
            scrollBehavior = topAppBarState
        )

        // Filter chips row
        OptimizedFilterChipsRow(
            selectedSeverity = selectedSeverity,
            selectedCategory = selectedCategory,
            selectedBroadcastType = selectedBroadcastType,
            onSeveritySelected = onSeveritySelected,
            onCategorySelected = onCategorySelected,
            onBroadcastTypeSelected = onBroadcastTypeSelected
        )

        // Event list
        if (privateEvents.isEmpty() && publicEvents.isEmpty()) {
            EmptyEventsList(modifier = Modifier.weight(1f))
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

/**
 * OPTIMIZED Filter chips row
 *
 * Uses stable callbacks to avoid unnecessary recompositions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptimizedFilterChipsRow(
    selectedSeverity: Severity?,
    selectedCategory: Category?,
    selectedBroadcastType: BroadcastType?,
    onSeveritySelected: (Severity?) -> Unit,
    onCategorySelected: (Category?) -> Unit,
    onBroadcastTypeSelected: (BroadcastType?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Severity filter
        SeverityFilterRow(
            selected = selectedSeverity,
            onSelected = onSeveritySelected
        )

        // Category filter
        CategoryFilterRow(
            selected = selectedCategory,
            onSelected = onCategorySelected
        )

        // Broadcast type filter
        BroadcastTypeFilterRow(
            selected = selectedBroadcastType,
            onSelected = onBroadcastTypeSelected
        )
    }
}

/**
 * Severity filter row using SegmentedButton
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeverityFilterRow(
    selected: Severity?,
    onSelected: (Severity?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(
            text = "Severity:",
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(end = 8.dp),
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SegmentedButton(
                selected = selected == null,
                onClick = { onSelected(null) },
                shape = SegmentedButtonDefaults.itemShape(0, 3),
                icon = {}
            ) {
                Text("All", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
            }
            SegmentedButton(
                selected = selected == Severity.ALERT,
                onClick = { onSelected(Severity.ALERT) },
                shape = SegmentedButtonDefaults.itemShape(1, 3),
                icon = {}
            ) {
                Text("Alert", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
            }
            SegmentedButton(
                selected = selected == Severity.CRISIS,
                onClick = { onSelected(Severity.CRISIS) },
                shape = SegmentedButtonDefaults.itemShape(2, 3),
                icon = {}
            ) {
                Text("Crisis", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/**
 * Category filter row - simplified for hackathon MVP with "All" + top categories
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterRow(
    selected: Category?,
    onSelected: (Category?) -> Unit
) {
    Column {
        Text(
            text = "Category:",
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 0.dp, vertical = 4.dp),
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val topCategories = listOf(
                null,
                Category.MEDICAL,
                Category.FIRE,
                Category.WEATHER,
                Category.CRIME,
                Category.TRAFFIC,
                Category.OTHER
            )
            topCategories.forEachIndexed { index, category ->
                SegmentedButton(
                    selected = selected == category,
                    onClick = { onSelected(category) },
                    shape = SegmentedButtonDefaults.itemShape(index, topCategories.size),
                    icon = {}
                ) {
                    Text(
                        text = category?.displayName ?: "All",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

/**
 * Broadcast type filter row using SegmentedButton
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BroadcastTypeFilterRow(
    selected: BroadcastType?,
    onSelected: (BroadcastType?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(
            text = "Broadcast:",
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(end = 8.dp),
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SegmentedButton(
                selected = selected == null,
                onClick = { onSelected(null) },
                shape = SegmentedButtonDefaults.itemShape(0, 3),
                icon = {}
            ) {
                Text("All", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
            }
            SegmentedButton(
                selected = selected == BroadcastType.PUBLIC,
                onClick = { onSelected(BroadcastType.PUBLIC) },
                shape = SegmentedButtonDefaults.itemShape(1, 3),
                icon = {}
            ) {
                Text("Public", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
            }
            SegmentedButton(
                selected = selected == BroadcastType.PRIVATE,
                onClick = { onSelected(BroadcastType.PRIVATE) },
                shape = SegmentedButtonDefaults.itemShape(2, 3),
                icon = {}
            ) {
                Text("Private", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/**
 * Empty state when no events are in list
 */
@Composable
private fun EmptyEventsList(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        padding = 32.dp
    ) {
        androidx.compose.material3.Text(
            text = "No Events",
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        androidx.compose.material3.Text(
            text = "You haven't received any emergency alerts yet.\nTap + button to create one.",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
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
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
        Spacer(
            modifier = Modifier
                .height(1.dp)
                .weight(1f)
                .background(androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant)
        )
    }
}

// Helper functions

/**
 * Check if event severity matches the selected filter
 */
private fun matchesSeverity(severity: Severity?, selected: Severity?): Boolean {
    return selected == null || severity == selected
}

/**
 * Check if event category matches the selected filter
 */
private fun matchesCategory(category: Category?, selected: Category?): Boolean {
    return selected == null || category == selected
}

/**
 * Check if event broadcast type matches the selected filter
 */
private fun matchesBroadcastType(type: BroadcastType?, selected: BroadcastType?): Boolean {
    return selected == null || type == selected
}

/**
 * Event list item card (placeholder - would reference existing implementation)
 */
@Composable
private fun EventListItemCard(
    event: Event,
    creator: User?,
    onClick: () -> Unit
) {
    // This would use the existing EventListItemCard implementation
    // Referenced here for completeness
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
        )
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            androidx.compose.material3.Surface(
                color = severityColor(event.severityEnum),
                shape = androidx.compose.foundation.shape.CircleShape,
                modifier = Modifier.size(12.dp)
            ) {}

            Spacer(modifier = Modifier.width(12.dp))

            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                androidx.compose.material3.Text(
                    text = event.categoryEnum?.displayName ?: "Unknown",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun severityColor(severity: Severity?): androidx.compose.ui.graphics.Color {
    return when (severity) {
        Severity.ALERT -> androidx.compose.ui.graphics.Color(0xFFF97316) // Orange
        Severity.CRISIS -> androidx.compose.ui.graphics.Color(0xFFDC2626) // Red
        null -> androidx.compose.material3.MaterialTheme.colorScheme.outline
    }
}
