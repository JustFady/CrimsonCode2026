package org.crimsoncode2026.screens.privateevents

import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.crimsoncode2026.data.RealtimeChannelStatus
import org.crimsoncode2026.data.RealtimeEventPayload
import org.crimsoncode2026.domain.usecases.GetReceivedEventsResult
import org.crimsoncode2026.domain.usecases.GetReceivedEventsUseCase
import org.crimsoncode2026.domain.usecases.MarkEventOpenedResult
import org.crimsoncode2026.domain.usecases.MarkEventOpenedUseCase
import org.crimsoncode2026.domain.usecases.SubscribeToPrivateEventsUseCase
import org.crimsoncode2026.domain.usecases.IncomingPrivateEvent

/**
 * UI state for received events list
 */
sealed class PrivateEventsUiState {
    data object Loading : PrivateEventsUiState()
    data class Success(
        val receivedEvents: List<org.crimsoncode2026.domain.usecases.ReceivedEvent>
    ) : PrivateEventsUiState()
    data class Error(val message: String) : PrivateEventsUiState()
    data object Empty : PrivateEventsUiState()
}

/**
 * Private Events ViewModel for received events state management.
 *
 * Manages:
 * - Received events list with loading and error states
 * - Real-time subscription to incoming private events
 * - Event opened state tracking
 * - Connection status for realtime channel
 *
 * Spec requirements:
 * - "Private events show creator's display name"
 * - "Real-time message sent to each recipient's private channel"
 * - Track opened state via event_recipients.opened_at
 */
@Stable
class PrivateEventsViewModel(
    private val getReceivedEventsUseCase: GetReceivedEventsUseCase,
    private val markEventOpenedUseCase: MarkEventOpenedUseCase,
    private val subscribeToPrivateEventsUseCase: SubscribeToPrivateEventsUseCase,
    private val scope: CoroutineScope
) {
    // Private mutable state
    private val _uiState = MutableStateFlow<PrivateEventsUiState>(PrivateEventsUiState.Loading)
    private val _isSubscribed = MutableStateFlow(false)
    private val _selectedEventId = MutableStateFlow<String?>(null)
    private val _connectionStatus = MutableStateFlow(RealtimeChannelStatus.DISCONNECTED)

    // Public read-only state flows
    val uiState: StateFlow<PrivateEventsUiState> = _uiState.asStateFlow()
    val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()
    val selectedEventId: StateFlow<String?> = _selectedEventId.asStateFlow()
    val connectionStatus: StateFlow<RealtimeChannelStatus> = _connectionStatus.asStateFlow()

    /**
     * Number of unopened events
     */
    val unopenedCount: Int
        get() = when (val state = _uiState.value) {
            is PrivateEventsUiState.Success -> state.receivedEvents.count { !it.isOpened }
            else -> 0
        }

    /**
     * Initialize ViewModel: load received events and subscribe to realtime updates
     */
    fun initialize() {
        loadReceivedEvents()
        subscribeToRealtime()
    }

    /**
     * Load received events from server
     */
    fun loadReceivedEvents() {
        scope.launch {
            _uiState.value = PrivateEventsUiState.Loading

            when (val result = getReceivedEventsUseCase()) {
                is GetReceivedEventsResult.Success -> {
                    val events = result.receivedEvents
                    if (events.isEmpty()) {
                        _uiState.value = PrivateEventsUiState.Empty
                    } else {
                        _uiState.value = PrivateEventsUiState.Success(events)
                    }
                }
                is GetReceivedEventsResult.Error -> {
                    _uiState.value = PrivateEventsUiState.Error(result.message)
                }
            }
        }
    }

    /**
     * Subscribe to private events realtime channel
     */
    private fun subscribeToRealtime() {
        if (subscribeToPrivateEventsUseCase.isSubscribed()) {
            _isSubscribed.value = true
            return
        }

        scope.launch {
            when (subscribeToPrivateEventsUseCase(object : SubscribeToPrivateEventsUseCase.PrivateEventListener {
                override fun onIncomingEvent(event: IncomingPrivateEvent) {
                    handleIncomingEvent(event)
                }

                override fun onEventDeleted(payload: RealtimeEventPayload) {
                    handleEventDeleted(payload)
                }

                override fun onError(error: Throwable) {
                    _uiState.value = PrivateEventsUiState.Error(error.message ?: "Realtime error")
                }

                override fun onStatusChanged(status: RealtimeChannelStatus) {
                    _connectionStatus.value = status
                }
            }) {
                is SubscribeToPrivateEventsUseCase.Result.Success -> {
                    _isSubscribed.value = true
                }
                is SubscribeToPrivateEventsUseCase.Result.Error -> {
                    _isSubscribed.value = false
                }
            }
        }
    }

    /**
     * Handle incoming realtime event
     */
    private fun handleIncomingEvent(event: IncomingPrivateEvent) {
        val currentUiState = _uiState.value
        if (currentUiState !is PrivateEventsUiState.Success) {
            // Reload events if not in success state
            loadReceivedEvents()
            return
        }

        val currentState = currentUiState as PrivateEventsUiState.Success
        val existingEvent = currentState.receivedEvents.find { it.event.id == event.payload.eventId }

        if (event.isNew && existingEvent == null) {
            // New event received - fetch full details and add to list
            scope.launch {
                val newEvent = fetchEventDetails(event.payload.eventId)
                if (newEvent != null) {
                    val updatedList = listOf(newEvent) + currentState.receivedEvents
                    _uiState.value = PrivateEventsUiState.Success(updatedList)
                }
            }
        } else if (!event.isNew && existingEvent != null) {
            // Existing event updated - update in list
            val updatedList = currentState.receivedEvents.map {
                if (it.event.id == event.payload.eventId) {
                    it.copy(event = it.event.copy(
                        description = event.payload.description,
                        severity = event.payload.severity,
                        category = event.payload.category
                    ))
                } else {
                    it
                }
            }
            _uiState.value = PrivateEventsUiState.Success(updatedList)
        }
    }

    /**
     * Handle event deletion from realtime
     */
    private fun handleEventDeleted(payload: RealtimeEventPayload) {
        val currentUiState = _uiState.value
        if (currentUiState !is PrivateEventsUiState.Success) return

        val currentState = currentUiState as PrivateEventsUiState.Success
        val updatedList = currentState.receivedEvents.filter { it.event.id != payload.eventId }

        if (updatedList.isEmpty()) {
            _uiState.value = PrivateEventsUiState.Empty
        } else {
            _uiState.value = PrivateEventsUiState.Success(updatedList)
        }
    }

    /**
     * Fetch event details for a single event
     */
    private suspend fun fetchEventDetails(eventId: String): org.crimsoncode2026.domain.usecases.ReceivedEvent? {
        val result = getReceivedEventsUseCase()
        return when (result) {
            is GetReceivedEventsResult.Success -> {
                result.receivedEvents.find { it.event.id == eventId }
            }
            else -> null
        }
    }

    /**
     * Mark an event as opened
     *
     * @param eventId Event ID to mark as opened
     */
    fun markEventAsOpened(eventId: String) {
        scope.launch {
            when (markEventOpenedUseCase(eventId)) {
                is MarkEventOpenedResult.Success -> {
                    // Update local state with opened status
                    updateEventOpenedState(eventId)
                }
                is MarkEventOpenedResult.Error -> {
                    _uiState.value = PrivateEventsUiState.Error("Failed to mark event as opened")
                }
            }
        }
    }

    /**
     * Update event opened state in local list
     */
    private fun updateEventOpenedState(eventId: String) {
        val currentUiState = _uiState.value
        if (currentUiState !is PrivateEventsUiState.Success) return

        val currentState = currentUiState as PrivateEventsUiState.Success
        val updatedList = currentState.receivedEvents.map {
            if (it.event.id == eventId) {
                it.copy(recipient = it.recipient.copy(openedAt = System.currentTimeMillis()))
            } else {
                it
            }
        }
        _uiState.value = PrivateEventsUiState.Success(updatedList)
    }

    /**
     * Select an event for viewing
     *
     * @param eventId Event ID to select
     */
    fun selectEvent(eventId: String) {
        _selectedEventId.value = eventId
    }

    /**
     * Clear event selection
     */
    fun clearSelection() {
        _selectedEventId.value = null
    }

    /**
     * Refresh received events list
     */
    fun refresh() {
        loadReceivedEvents()
    }

    /**
     * Clear event locally (client-side only)
     *
     * Spec note: Event clearing is client-side only (stored in local device cache),
     * not in the database. This matches the "local device cache to hide events" requirement.
     *
     * @param eventId Event ID to clear from list
     */
    fun clearEvent(eventId: String) {
        val currentUiState = _uiState.value
        if (currentUiState !is PrivateEventsUiState.Success) return

        val currentState = currentUiState as PrivateEventsUiState.Success
        val updatedList = currentState.receivedEvents.filter { it.event.id != eventId }

        if (updatedList.isEmpty()) {
            _uiState.value = PrivateEventsUiState.Empty
        } else {
            _uiState.value = PrivateEventsUiState.Success(updatedList)
        }
    }

    /**
     * Clear all events locally (client-side only)
     */
    fun clearAllEvents() {
        _uiState.value = PrivateEventsUiState.Empty
        _selectedEventId.value = null
    }

    /**
     * Get selected event details
     *
     * @return Selected ReceivedEvent or null if none selected
     */
    fun getSelectedEvent(): org.crimsoncode2026.domain.usecases.ReceivedEvent? {
        val currentUiState = _uiState.value
        if (currentUiState !is PrivateEventsUiState.Success) return null

        val selectedId = _selectedEventId.value ?: return null
        return (currentUiState as PrivateEventsUiState.Success).receivedEvents
            .find { it.event.id == selectedId }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        if (_uiState.value is PrivateEventsUiState.Error) {
            _uiState.value = PrivateEventsUiState.Loading
            loadReceivedEvents()
        }
    }

    /**
     * Cleanup when ViewModel is no longer needed
     */
    fun cleanup() {
        subscribeToPrivateEventsUseCase.unsubscribe()
        _isSubscribed.value = false
    }
}
