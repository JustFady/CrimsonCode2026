package org.crimsoncode2026.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Badge
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventNote
import org.crimsoncode2026.compose.MapView
import org.crimsoncode2026.compose.EventMarkers
import org.crimsoncode2026.compose.UserLocationMarker
import org.crimsoncode2026.location.LocationPermissionHandler
import org.crimsoncode2026.location.LocationData
import org.crimsoncode2026.screens.publicevents.EventDetailsPanel
import org.koin.compose.koinInject

/**
 * Main app screen with event map, settings, FAB, and event list toggle button
 *
 * Features:
 * - Event map view using MapLibre with OpenStreetMap tiles
 * - Event markers displayed from MainMapViewModel
 * - Settings button in top app bar
 * - FAB button to launch event creation wizard
 * - Event list toggle button showing event count
 * - Event details panel for selected event
 * - Map camera control for zooming to events
 *
 * @param onNavigateToSettings Callback when user navigates to settings
 * @param onCreateEvent Callback when user taps FAB to create event
 * @param onShowEventList Callback when user taps event list button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit = {},
    onCreateEvent: () -> Unit = {},
    onShowEventList: () -> Unit = {}
) {
    val locationPermissionHandler: LocationPermissionHandler = koinInject()
    val viewModel: MainMapViewModel = koinInject()
    val state by viewModel.state.collectAsState()

    // Request location permissions on screen load
    LaunchedEffect(Unit) {
        if (!locationPermissionHandler.hasLocationPermission()) {
            locationPermissionHandler.requestLocationPermission()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CrimsonCode") },
                actions = {
                    Row {
                        // Event list toggle button with badge
                        Box {
                            IconButton(onClick = onShowEventList) {
                                Icon(Icons.Default.EventNote, contentDescription = "Event List")
                            }
                            // Event count badge
                            if (state.loadedEvents.isNotEmpty()) {
                                Badge(
                                    modifier = Modifier.align(Alignment.TopEnd),
                                    containerColor = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = state.loadedEvents.size.toString(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Settings button
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateEvent
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Event")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Map view with event markers
            MapView(
                modifier = Modifier.fillMaxSize(),
                onMapReady = {
                    // Center map on user location when available
                    state.userLocation?.let { location ->
                        viewModel.zoomToLocation(location.latitude, location.longitude, 12.0)
                    }
                },
                onCameraChanged = { cameraPosition ->
                    // Update map bounds in ViewModel for querying events
                    org.crimsoncode2026.compose.calculateMapBoundsFromZoom(
                        center = cameraPosition.center,
                        zoom = cameraPosition.zoom
                    ).let { bounds ->
                        viewModel.updateMapBounds(bounds)
                    }
                },
                targetLocation = state.cameraPosition?.let {
                    it.latitude to it.longitude
                },
                targetZoom = state.cameraPosition?.zoom
            ) {
                // User location marker with accuracy circle (MVP: centered overlay)
                state.userLocation?.let { location ->
                    UserLocationMarker(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracyInMeters = location.accuracyMeters
                    )
                }

                // Event markers layer
                EventMarkers(
                    events = state.loadedEvents.map { it.event },
                    onEventClick = { event ->
                        viewModel.selectEvent(event, state.loadedEvents
                            .find { it.event.id == event.id }?.creator)
                    }
                )
            }

            // Event details panel (bottom sheet) when event is selected
            state.selectedEvent?.let { mapEvent ->
                EventDetailsPanel(
                    event = mapEvent.event,
                    creator = mapEvent.creator,
                    onDismiss = { viewModel.clearSelectedEvent() },
                    onClear = {
                        viewModel.clearEvent(mapEvent.event.id)
                    },
                    onNavigateToLocation = {
                        // Navigate to event location - center map on event
                        viewModel.zoomToEvent(mapEvent.event.id)
                    }
                )
            }
        }
    }
}
