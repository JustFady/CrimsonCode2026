package org.crimsoncode2026.screens.privateevents

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.crimsoncode2026.compose.CategoryColor
import org.crimsoncode2026.compose.CategoryIcon
import org.crimsoncode2026.data.RealtimeChannelStatus
import org.crimsoncode2026.data.Severity
import org.crimsoncode2026.domain.usecases.ReceivedEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Events List Screen - displays received private events.
 *
 * Features:
 * - List of received events with category icons and severity indicators
 * - Creator name displayed for private events
 * - Opened/unopened status indicators
 * - Pull-to-refresh functionality
 * - Empty state when no events received
 * - Loading and error states
 *
 * Spec requirements:
 * - "Private events show creator's display name"
 * - "Mark event as opened"
 * - "Clear from list" (local device cache)
 */
@Composable
fun EventsListScreen(
    onBack: () -> Unit,
    onEventClick: (String) -> Unit
) {
    val viewModel: PrivateEventsViewModel by inject()

    val uiState by viewModel.uiState.collectAsState()
    val selectedEventId by viewModel.selectedEventId.collectAsState()
    val isSubscribed by viewModel.isSubscribed.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    // Initialize on first composition
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    // Cleanup when leaving screen
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            viewModel.cleanup()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            EventsListHeader(
                onBack = onBack,
                onRefresh = { viewModel.refresh() },
                isSubscribed = isSubscribed,
                connectionStatus = connectionStatus,
                unopenedCount = viewModel.unopenedCount
            )

            // Events list or empty/error states
            when (val state = uiState) {
                is PrivateEventsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is PrivateEventsUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        ErrorView(
                            message = state.message,
                            onRetry = { viewModel.clearError() }
                        )
                    }
                }
                is PrivateEventsUiState.Empty -> {
                    EmptyEventsView()
                }
                is PrivateEventsUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                    ) {
                        items(
                            items = state.receivedEvents,
                            key = { it.event.id }
                        ) { event ->
                            EventListItem(
                                event = event,
                                onClick = {
                                    viewModel.selectEvent(event.event.id)
                                    onEventClick(event.event.id)
                                },
                                onMarkOpened = { viewModel.markEventAsOpened(event.event.id) },
                                onClear = { viewModel.clearEvent(event.event.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Event details bottom sheet when an event is selected
    selectedEventId?.let { eventId ->
        val selectedEvent = viewModel.getSelectedEvent()
        if (selectedEvent != null) {
            EventDetailsBottomSheet(
                event = selectedEvent,
                isVisible = true,
                onDismiss = { viewModel.clearSelection() },
                onMarkOpened = { viewModel.markEventAsOpened(eventId) },
                onClear = {
                    viewModel.clearEvent(eventId)
                    viewModel.clearSelection()
                }
            )
        }
    }
}

/**
 * Header for events list screen
 */
@Composable
private fun EventsListHeader(
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    isSubscribed: Boolean,
    connectionStatus: RealtimeChannelStatus,
    unopenedCount: Int
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = androidx.compose.foundation.layout.WindowInsets(0).getBottom(androidx.compose.foundation.layout.WindowInsetsSides.Horizontal)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Received Events",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (unopenedCount > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Text(
                            text = unopenedCount.toString().coerceAtMost(9),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                IconButton(onClick = onRefresh) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = when (connectionStatus) {
                            RealtimeChannelStatus.CONNECTED -> MaterialTheme.colorScheme.primary
                            RealtimeChannelStatus.CONNECTING -> MaterialTheme.colorScheme.outline
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }
    }
}

/**
 * Single event item in the list
 */
@Composable
private fun EventListItem(
    event: ReceivedEvent,
    onClick: () -> Unit,
    onMarkOpened: () -> Unit,
    onClear: () -> Unit
) {
    val category = event.event.categoryEnum
    val severity = event.event.severityEnum

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top row: category, severity badge, time, clear button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Category icon
                    category?.let { cat ->
                        Box(modifier = Modifier.size(40.dp)) {
                            CategoryIcon(
                                category = cat,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    // Event info column
                    Column(modifier = Modifier.weight(1f)) {
                        // Creator name and severity
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (event.creatorDisplayName != null) {
                                Text(
                                    text = "Private Alert",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            if (event.creatorDisplayName != null && severity != null) {
                                Surface(
                                    color = getSeverityColor(severity),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = severity.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            // Category name
                            category?.let { cat ->
                                Text(
                                    text = cat.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Time ago
                        Text(
                            text = formatEventTime(event.event.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Opened indicator and actions
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!event.isOpened) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Not opened",
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        IconButton(
                            onClick = onClear,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = event.event.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Creator info for private events
            event.creatorDisplayName?.let { creatorName ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "From: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = creatorName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Open button for unopened events
            if (!event.isOpened) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onMarkOpened,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mark as Opened")
                }
            }
        }
    }
}

/**
 * Empty state view when no events received
 */
@Composable
private fun EmptyEventsView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Received Events",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "When someone sends you a private alert, it will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Error view when loading fails
 */
@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Close,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

/**
 * Get color for severity badge
 */
private fun getSeverityColor(severity: Severity): Color {
    return when (severity) {
        Severity.ALERT -> Color(0xFFFF9800) // Orange
        Severity.CRISIS -> Color(0xFFFF4436) // Red
    }
}

/**
 * Format event timestamp to relative time
 */
private fun formatEventTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffMs = now - timestamp

    return when {
        diffMs < 60_000 -> "Just now"
        diffMs < 3600_000 -> "${diffMs / 60_000}m ago"
        diffMs < 86400_000 -> "${diffMs / 3600_000}h ago"
        diffMs < 604800_000 -> "${diffMs / 86400_000}d ago"
        else -> {
            val date = Date(timestamp)
            SimpleDateFormat("MMM d", Locale.US).format(date)
        }
    }
}

/**
 * Event Details Bottom Sheet - shows full event details
 *
 * @param event The received event to display
 * @param isVisible Whether the bottom sheet is currently visible
 * @param onDismiss Callback when bottom sheet is dismissed
 * @param onMarkOpened Callback when user marks event as opened
 * @param onClear Callback when user clears event
 */
@Composable
fun EventDetailsBottomSheet(
    event: ReceivedEvent,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onMarkOpened: () -> Unit,
    onClear: () -> Unit
) {
    val category = event.event.categoryEnum
    val severity = event.event.severityEnum

    androidx.compose.material3.BottomSheetScaffold(
        sheetContent = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp, 16.dp, 0.dp, 0.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            // Category and severity badges
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                category?.let { cat ->
                                    Box(modifier = Modifier.size(48.dp)) {
                                        CategoryIcon(
                                            category = cat,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                severity?.let { sev ->
                                    Surface(
                                        color = getSeverityColor(sev),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = sev.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            // Creator name for private events
                            event.creatorDisplayName?.let { creator ->
                                Text(
                                    text = "From: $creator",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Description
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.event.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Event metadata
                    EventMetadataRow(
                        label = "Severity",
                        value = severity?.displayName ?: "Unknown"
                    )
                    EventMetadataRow(
                        label = "Category",
                        value = category?.displayName ?: "Unknown"
                    )
                    EventMetadataRow(
                        label = "Received",
                        value = formatEventTime(event.event.createdAt)
                    )

                    if (event.recipient.notifiedAt != null) {
                        EventMetadataRow(
                            label = "Notified",
                            value = formatEventTime(event.recipient.notifiedAt)
                        )
                    }

                    if (event.recipient.openedAt != null) {
                        EventMetadataRow(
                            label = "Opened",
                            value = "Not yet"
                        )
                    } else {
                        EventMetadataRow(
                            label = "Opened",
                            value = formatEventTime(event.recipient.openedAt)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!event.isOpened) {
                            Button(
                                onClick = onMarkOpened,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Mark as Opened")
                            }
                        }
                        Button(
                            onClick = {
                                onClear()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Clear Event")
                        }
                    }
                }
            }
        },
        sheetPeekHeight = androidx.compose.material3.BottomSheetScaffoldDefaults.SheetPeekHeight,
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    )
}

/**
 * Row displaying metadata label and value
 */
@Composable
private fun EventMetadataRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
