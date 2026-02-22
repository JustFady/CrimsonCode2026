package org.crimsoncode2026.domain.usecases

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import org.crimsoncode2026.data.RealtimeChannelStatus
import org.crimsoncode2026.data.RealtimeEventPayload
import org.crimsoncode2026.data.RealtimeService
import org.crimsoncode2026.domain.UserSessionManager

/**
 * Result of subscribing to private events
 */
sealed class SubscribeToPrivateEventsResult {
    data object Success : SubscribeToPrivateEventsResult()
    data class Error(val message: String) : SubscribeToPrivateEventsResult()
}

/**
 * Incoming private event data
 */
data class IncomingPrivateEvent(
    val payload: RealtimeEventPayload,
    val isNew: Boolean = true
)

/**
 * Subscribe to Private Events Use Case
 *
 * Subscribes to the current user's private notification channel
 * to receive real-time updates for incoming private events.
 *
 * Channel pattern: user:{userId}:notifications
 *
 * Spec requirement: "Real-time message sent to each recipient's private channel"
 */
class SubscribeToPrivateEventsUseCase(
    private val realtimeService: RealtimeService,
    private val userSessionManager: UserSessionManager
) {

    private var subscriptionJob: Job? = null

    /**
     * Connection status flow from realtime service
     */
    val connectionStatus: StateFlow<RealtimeChannelStatus>
        get() = realtimeService.connectionStatus

    /**
     * Check if user is currently subscribed
     *
     * @return True if subscribed to private channel
     */
    fun isSubscribed(): Boolean {
        val userId = userSessionManager.getCurrentUserId()
        return userId != null && realtimeService.isUserChannelSubscribed(userId)
    }

    /**
     * Subscribe to private events channel for current user
     *
     * @param listener Event listener for incoming private events
     * @return SubscribeToPrivateEventsResult indicating success or error
     */
    operator fun invoke(listener: PrivateEventListener): SubscribeToPrivateEventsResult {
        // Step 1: Get current user ID
        val userId = userSessionManager.getCurrentUserId()
            ?: return SubscribeToPrivateEventsResult.Error("User not authenticated")

        // Step 2: Check if already subscribed
        if (realtimeService.isUserChannelSubscribed(userId)) {
            return SubscribeToPrivateEventsResult.Success
        }

        // Step 3: Subscribe to user channel
        subscriptionJob = realtimeService.subscribeToUserChannel(
            userId = userId,
            listener = object : org.crimsoncode2026.data.RealtimeEventListener {
                override fun onEventCreated(payload: RealtimeEventPayload) {
                    val incomingEvent = IncomingPrivateEvent(
                        payload = payload,
                        isNew = true
                    )
                    listener.onIncomingEvent(incomingEvent)
                }

                override fun onEventUpdated(payload: RealtimeEventPayload) {
                    val incomingEvent = IncomingPrivateEvent(
                        payload = payload,
                        isNew = false
                    )
                    listener.onIncomingEvent(incomingEvent)
                }

                override fun onEventDeleted(payload: RealtimeEventPayload) {
                    listener.onEventDeleted(payload)
                }

                override fun onError(error: Throwable) {
                    listener.onError(error)
                }

                override fun onStatusChanged(status: RealtimeChannelStatus) {
                    listener.onStatusChanged(status)
                }
            }
        )

        return SubscribeToPrivateEventsResult.Success
    }

    /**
     * Unsubscribe from private events channel
     */
    fun unsubscribe() {
        subscriptionJob?.cancel()
        subscriptionJob = null
    }

    /**
     * Listener interface for private events
     */
    interface PrivateEventListener {
        /**
         * Called when a new event is received
         */
        fun onIncomingEvent(event: IncomingPrivateEvent)

        /**
         * Called when an event is deleted
         */
        fun onEventDeleted(payload: RealtimeEventPayload)

        /**
         * Called when an error occurs
         */
        fun onError(error: Throwable)

        /**
         * Called when connection status changes
         */
        fun onStatusChanged(status: RealtimeChannelStatus)
    }
}
