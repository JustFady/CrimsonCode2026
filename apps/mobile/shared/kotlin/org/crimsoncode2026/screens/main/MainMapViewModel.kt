package org.crimsoncode2026.screens.main

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.crimsoncode2026.compose.CameraPosition
import org.crimsoncode2026.compose.MapBounds
import org.crimsoncode2026.data.Event
import org.crimsoncode2026.data.RealtimeEventPayload
import org.crimsoncode2026.data.User
import org.crimsoncode2026.domain.usecases.GetReceivedEventsUseCase
import org.crimsoncode2026.domain.usecases.QueryPublicEventsParams
import org.crimsoncode2026.domain.usecases.QueryPublicEventsUseCase
import org.crimsoncode2026.domain.usecases.SubscribeToPrivateEventsUseCase
import org.crimsoncode2026.location.LocationData
import org.crimsoncode2026.storage.PreferencesStorage

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
    val cameraPosition: CameraPosition? = null,
    val loadedEvents: List<MapEvent> = emptyList(),
    val selectedEvent: MapEvent? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val clearedEventIds: Set<String> = emptySet()
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
 * Incoming private event from realtime
 *
 * @property payload The event payload
 * @property isNew Whether this is a new event (true) or an update (false)
 */
data class IncomingPrivateEvent(
    val payload: RealtimeEventPayload,
    val isNew: Boolean = true
)

/**
 * Main Map ViewModel
 *
 * Manages map state including:
 * - User location tracking
 * - Current map bounds
 * - Loaded events (private + public)
 * - Selected event for details panel
 * - Realtime subscription for private events
 * - Cleared events persistence (local device cache)
 *
 * Coordinates:
 * - QueryPublicEventsUseCase for public events within bounds
 * - LocationState for user location updates
 * - GetReceivedEventsUseCase for private events
 * - SubscribeToPrivateEventsUseCase for realtime private event updates
 * - PreferencesStorage for cleared events persistence
 *
 * @param queryPublicEventsUseCase Use case for querying public events by bounds
 * @param getReceivedEventsUseCase Use case for getting received private events
 * @param subscribeToPrivateEventsUseCase Use case for subscribing to realtime private events
 * @param locationState Location state manager
 * @param preferencesStorage Local storage for user preferences including cleared events
 * @param scope Coroutine scope for ViewModel
 */
class MainMapViewModel(
    private val queryPublicEventsUseCase: QueryPublicEventsUseCase,
    private val getReceivedEventsUseCase: GetReceivedEventsUseCase,
    private val subscribeToPrivateEventsUseCase: SubscribeToPrivateEventsUseCase,
    private val locationState: org.crimsoncode2026.location.LocationState,
    private val preferencesStorage: PreferencesStorage,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(MainMapState())
    val state: StateFlow<MainMapState> = _state.asStateFlow()

    init {
        // Collect user location updates
        collectUserLocation()

        // Load cleared event IDs from storage
        loadClearedEventIds()

        // Load initial private events
        loadPrivateEvents()

        // Start realtime subscription for private events
        startRealtimeSubscription()
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
     * Saves all current event IDs to cleared events storage.
     */
    fun clearAllEvents() {
        val currentEventIds = _state.value.loadedEvents.map { it.event.id }
        scope.launch {
            currentEventIds.forEach { eventId ->
                preferencesStorage.addClearedEventId(eventId)
            }
        }
        _state.value = _state.value.copy(
            loadedEvents = emptyList(),
            selectedEvent = null
        )
    }

    /**
     * Clear a specific event from the list (local cache only)
     * Saves event ID to cleared events storage.
     *
     * @param eventId ID of event to clear
     */
    fun clearEvent(eventId: String) {
        scope.launch {
            preferencesStorage.addClearedEventId(eventId)
        }
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
     * Zoom map to a specific location
     *
     * @param latitude Target latitude
     * @param longitude Target longitude
     * @param zoom Optional zoom level (defaults to 14.0)
     */
    fun zoomToLocation(latitude: Double, longitude: Double, zoom: Double = 14.0) {
        _state.value = _state.value.copy(
            cameraPosition = CameraPosition(
                latitude = latitude,
                longitude = longitude,
                zoom = zoom
            )
        )
    }

    /**
     * Zoom map to an event location and select it
     *
     * @param eventId ID of the event to zoom to
     */
    fun zoomToEvent(eventId: String) {
        val targetEvent = _state.value.loadedEvents.find { it.event.id == eventId }
        if (targetEvent != null) {
            _state.value = _state.value.copy(
                cameraPosition = CameraPosition(
                    latitude = targetEvent.event.lat,
                    longitude = targetEvent.event.lon,
                    zoom = 14.0
                ),
                selectedEvent = targetEvent
            )
        }
    }

    /**
     * Start realtime subscription for private events
     */
    private fun startRealtimeSubscription() {
        subscribeToPrivateEventsUseCase(
            listener = object : SubscribeToPrivateEventsUseCase.PrivateEventListener {
                override fun onIncomingEvent(event: IncomingPrivateEvent) {
                    handleRealtimeEvent(event)
                }

                override fun onEventDeleted(payload: RealtimeEventPayload) {
                    // Remove event from loadedEvents if it exists
                    _state.value = _state.value.copy(
                        loadedEvents = _state.value.loadedEvents.filterNot { it.event.id == payload.eventId },
                        selectedEvent = if (_state.value.selectedEvent?.event?.id == payload.eventId) {
                            null
                        } else {
                            _state.value.selectedEvent
                        }
                    )
                }

                override fun onError(error: Throwable) {
                    _state.value = _state.value.copy(
                        error = "Realtime error: ${error.message}"
                    )
                }

                override fun onStatusChanged(status: org.crimsoncode2026.data.RealtimeChannelStatus) {
                    // Optionally track connection status in state
                    // For MVP, we can just log this
                }
            }
        )
    }

    /**
     * Handle incoming realtime event
     *
     * @param incomingEvent The incoming private event
     */
    private fun handleRealtimeEvent(incomingEvent: IncomingPrivateEvent) {
        val payload = incomingEvent.payload

        // Check if event was previously cleared by user
        if (payload.eventId in _state.value.clearedEventIds) {
            // Event was cleared, don't add it to loaded events
            return
        }

        // Create Event from payload
        val event = Event(
            id = payload.eventId,
            creatorId = payload.creatorId ?: "",
            severity = payload.severity,
            category = payload.category,
            lat = payload.lat,
            lon = payload.lon,
            locationOverride = null,
            broadcastType = payload.broadcastType,
            description = payload.description,
            isAnonymous = payload.isAnonymous,
            createdAt = payload.createdAt,
            expiresAt = Event.calculateExpiration(payload.createdAt),
            deletedAt = null
        )

        // For private events, creator info is available in the payload
        val creator = if (payload.creatorId != null && payload.creatorDisplayName != null) {
            User(
                id = payload.creatorId,
                phoneNumber = "", // Not in realtime payload
                displayName = payload.creatorDisplayName,
                deviceId = "",
                platform = org.crimsoncode2026.data.Platform.ANDROID,
                isActive = true,
                fcmToken = "",
                createdAt = 0,
                updatedAt = 0,
                lastActiveAt = 0
            )
        } else {
            null
        }

        // Create MapEvent
        val mapEvent = MapEvent(event, creator)

        // Add to loaded events if not already present (for updates)
        val currentEvents = _state.value.loadedEvents
        val existingIndex = currentEvents.indexOfFirst { it.event.id == event.id }

        if (existingIndex >= 0) {
            // Update existing event
            _state.value = _state.value.copy(
                loadedEvents = currentEvents.toMutableList().apply {
                    this[existingIndex] = mapEvent
                }
            )
        } else {
            // Add new event
            _state.value = _state.value.copy(
                loadedEvents = currentEvents + mapEvent
            )
        }
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
     * Load cleared event IDs from preferences storage
     * This ensures events cleared by the user remain hidden across app restarts.
     */
    private fun loadClearedEventIds() {
        scope.launch {
            val clearedIds = preferencesStorage.getClearedEventIds()
            _state.value = _state.value.copy(
                clearedEventIds = clearedIds
            )
        }
    }

    /**
     * Filter events to exclude cleared ones
     *
     * @param events List of events to filter
     * @return Filtered list excluding cleared events
     */
    private fun filterClearedEvents(events: List<MapEvent>): List<MapEvent> {
        val clearedIds = _state.value.clearedEventIds
        return events.filterNot { it.event.id in clearedIds }
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
                    val filteredEvents = filterClearedEvents(privateMapEvents)
                    _state.value = _state.value.copy(
                        loadedEvents = filteredEvents,
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

                // Filter out cleared public events
                val filteredPublicEvents = filterClearedEvents(publicMapEvents)

                // Combine with existing private events
                val privateEvents = _state.value.loadedEvents.filter { it.event.isPrivate }
                _state.value = _state.value.copy(
                    loadedEvents = privateEvents + filteredPublicEvents,
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
