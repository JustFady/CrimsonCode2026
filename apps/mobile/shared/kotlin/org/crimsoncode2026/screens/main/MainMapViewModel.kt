package org.crimsoncode2026.screens.main

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.crimsoncode2026.compose.CameraPosition
import org.crimsoncode2026.compose.MapBounds
import org.crimsoncode2026.data.Event
import org.crimsoncode2026.data.User
import org.crimsoncode2026.domain.usecases.GetReceivedEventsUseCase
import org.crimsoncode2026.domain.usecases.QueryPublicEventsParams
import org.crimsoncode2026.domain.usecases.QueryPublicEventsUseCase
import org.crimsoncode2026.location.LocationData

/**
 * Map event with creator info for private events
 */
data class MapEvent(
    val event: Event,
    val creator: User? = null
)

/**
 * Main map state
 */
data class MainMapState(
    val userLocation: LocationData? = null,
    val mapBounds: MapBounds? = null,
    val loadedEvents: List<MapEvent> = emptyList(),
    val selectedEvent: MapEvent? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    /**
     * Check if there are any events to display
     */
    val hasEvents: Boolean
        get() = loadedEvents.isNotEmpty()

    /**
     * Check if an event is currently selected
     */
    val isEventSelected: Boolean
        get() = selectedEvent != null
}

/**
 * Main Map ViewModel
 *
 * Manages map state including:
 * - User location tracking
 * - Current map bounds
 * - Loaded events (private + public)
 * - Selected event for details panel
 *
 * Coordinates:
 * - QueryPublicEventsUseCase for public events within bounds
 * - LocationState for user location updates
 * - GetReceivedEventsUseCase for private events
 *
 * @param queryPublicEventsUseCase Use case for querying public events by bounds
 * @param getReceivedEventsUseCase Use case for getting received private events
 * @param locationState Location state manager
 * @param scope Coroutine scope for ViewModel
 */
class MainMapViewModel(
    private val queryPublicEventsUseCase: QueryPublicEventsUseCase,
    private val getReceivedEventsUseCase: GetReceivedEventsUseCase,
    private val locationState: org.crimsoncode2026.location.LocationState,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(MainMapState())
    val state: StateFlow<MainMapState> = _state.asStateFlow()

    init {
        // Collect user location updates
        collectUserLocation()

        // Load initial private events
        loadPrivateEvents()
    }

    /**
     * Update map bounds and query public events
     *
     * @param bounds New map bounds
     */
    fun updateMapBounds(bounds: MapBounds) {
        _state.value = _state.value.copy(
            mapBounds = bounds,
            isLoading = true,
            error = null
        )

        scope.launch {
            loadPublicEvents(bounds)
        }
    }

    /**
     * Select an event to show in details panel
     *
     * @param event Event to select
     * @param creator User who created the event (for private events)
     */
    fun selectEvent(event: Event, creator: User? = null) {
        _state.value = _state.value.copy(
            selectedEvent = MapEvent(event, creator)
        )
    }

    /**
     * Clear the selected event (close details panel)
     */
    fun clearSelectedEvent() {
        _state.value = _state.value.copy(
            selectedEvent = null
        )
    }

    /**
     * Clear all events from the map (local cache only)
     */
    fun clearAllEvents() {
        _state.value = _state.value.copy(
            loadedEvents = emptyList(),
            selectedEvent = null
        )
    }

    /**
     * Clear a specific event from the list (local cache only)
     *
     * @param eventId ID of event to clear
     */
    fun clearEvent(eventId: String) {
        _state.value = _state.value.copy(
            loadedEvents = _state.value.loadedEvents.filterNot { it.event.id == eventId },
            selectedEvent = if (_state.value.selectedEvent?.event?.id == eventId) {
                null
            } else {
                _state.value.selectedEvent
            }
        )
    }

    /**
     * Refresh all events (reload private and public)
     */
    fun refreshEvents() {
        loadPrivateEvents()
        _state.value.mapBounds?.let { bounds ->
            scope.launch {
                loadPublicEvents(bounds)
            }
        }
    }

    /**
     * Reload public events with current bounds
     */
    fun reloadPublicEvents() {
        _state.value.mapBounds?.let { bounds ->
            scope.launch {
                loadPublicEvents(bounds)
            }
        }
    }

    /**
     * Reload private events
     */
    fun reloadPrivateEvents() {
        loadPrivateEvents()
    }

    /**
     * Get current user location
     */
    fun getCurrentLocation(): LocationData? {
        return locationState.lastKnownLocation
    }

    /**
     * Request a single location update
     */
    suspend fun requestLocationUpdate(): LocationData? {
        return locationState.requestSingleLocation()
    }

    /**
     * Collect user location updates
     */
    private fun collectUserLocation() {
        scope.launch {
            locationState.locationData.collect { location ->
                _state.value = _state.value.copy(
                    userLocation = location
                )
            }
        }
    }

    /**
     * Load private events
     */
    private fun loadPrivateEvents() {
        scope.launch {
            when (val result = getReceivedEventsUseCase()) {
                is org.crimsoncode2026.domain.usecases.GetReceivedEventsResult.Success -> {
                    val privateMapEvents = result.events.map { event ->
                        // Note: Creator info would need to be fetched separately
                        // For now, we create MapEvent with null creator
                        MapEvent(event, null)
                    }
                    _state.value = _state.value.copy(
                        loadedEvents = privateMapEvents,
                        isLoading = false
                    )
                }
                is org.crimsoncode2026.domain.usecases.GetReceivedEventsResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    /**
     * Load public events within map bounds
     *
     * @param bounds Map bounds to query
     */
    private suspend fun loadPublicEvents(bounds: MapBounds) {
        when (val result = queryPublicEventsUseCase(
            QueryPublicEventsParams(
                minLat = bounds.south,
                maxLat = bounds.north,
                minLon = bounds.west,
                maxLon = bounds.east
            )
        )) {
            is org.crimsoncode2026.domain.usecases.QueryPublicEventsResult.Success -> {
                val publicMapEvents = result.events.map { event ->
                    // Public events have no creator info
                    MapEvent(event, null)
                }

                // Combine with existing private events
                val privateEvents = _state.value.loadedEvents.filter { it.event.isPrivate }
                _state.value = _state.value.copy(
                    loadedEvents = privateEvents + publicMapEvents,
                    isLoading = false,
                    error = null
                )
            }
            is org.crimsoncode2026.domain.usecases.QueryPublicEventsResult.Error -> {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = result.message
                )
            }
        }
    }
}
