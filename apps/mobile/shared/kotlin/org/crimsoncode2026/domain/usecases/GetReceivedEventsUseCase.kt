package org.crimsoncode2026.domain.usecases

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.crimsoncode2026.data.Event
import org.crimsoncode2026.data.EventRecipient
import org.crimsoncode2026.data.EventRecipientRepository
import org.crimsoncode2026.data.EventRepository
import org.crimsoncode2026.data.User
import org.crimsoncode2026.data.UserRepository
import org.crimsoncode2026.domain.UserSessionManager

/**
 * Result of getting received events
 */
sealed class GetReceivedEventsResult {
    data class Success(
        val receivedEvents: List<ReceivedEvent>
    ) : GetReceivedEventsResult()

    data class Error(val message: String) : GetReceivedEventsResult()
}

/**
 * Received event with event details and recipient state
 *
 * Combines Event data with EventRecipient state to provide
 * the complete view of events received by the current user.
 *
 * @property event The full event details
 * @property recipient The recipient record with delivery/opened state
 * @property creatorDisplayName Creator's display name (null for anonymous public events)
 */
data class ReceivedEvent(
    val event: Event,
    val recipient: EventRecipient,
    val creatorDisplayName: String? = null
) {
    /**
     * Checks if this event is private
     */
    val isPrivate: Boolean
        get() = event.isPrivate

    /**
     * Checks if notification has been sent
     */
    val isNotified: Boolean
        get() = recipient.isNotified

    /**
     * Checks if user has opened this event
     */
    val isOpened: Boolean
        get() = recipient.isOpened

    /**
     * Checks if delivery failed
     */
    val isFailed: Boolean
        get() = recipient.isFailed
}

/**
 * Get Received Events Use Case
 *
 * Fetches private events received by the current user along with:
 * - Event details (via EventRepository)
 * - Recipient state (delivery status, opened at, etc.)
 * - Creator's display name for private events (via UserRepository)
 *
 * Spec requirement: "Private events show creator's display name, public events anonymous"
 */
class GetReceivedEventsUseCase(
    private val eventRecipientRepository: EventRecipientRepository,
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
    private val userSessionManager: UserSessionManager
) {

    /**
     * Get all received events for the current user
     *
     * @param includeExpired Whether to include expired events (default: false)
     * @return GetReceivedEventsResult with received events or error
     */
    suspend operator fun invoke(includeExpired: Boolean = false): GetReceivedEventsResult {
        // Step 1: Get current user ID
        val userId = userSessionManager.getCurrentUserId()
            ?: return GetReceivedEventsResult.Error("User not authenticated")

        // Step 2: Get event recipient records for the user
        val recipientsResult = eventRecipientRepository.getEventsReceivedByUser(
            userId = userId,
            includeExpired = includeExpired
        )

        val recipients = when (recipientsResult) {
            is Result.Success -> recipientsResult.getOrNull() ?: emptyList()
            is Result.Failure -> return GetReceivedEventsResult.Error(
                recipientsResult.exceptionOrNull()?.message ?: "Failed to fetch received events"
            )
        }

        if (recipients.isEmpty()) {
            return GetReceivedEventsResult.Success(emptyList())
        }

        // Step 3: Fetch all events in parallel
        val receivedEvents = recipients.map { recipient ->
            async {
                // Get the full event
                val eventResult = eventRepository.getEventById(recipient.eventId)
                val event = when (eventResult) {
                    is Result.Success -> eventResult.getOrNull()
                    is Result.Failure -> null
                }

                if (event == null) {
                    return@async null
                }

                // For private events, fetch creator's display name
                val creatorDisplayName = if (event.isPrivate) {
                    val userResult = userRepository.getUserById(event.creatorId)
                    when (userResult) {
                        is Result.Success -> userResult.getOrNull()?.displayName
                        is Result.Failure -> null
                    }
                } else {
                    // Public events are anonymous
                    null
                }

                ReceivedEvent(
                    event = event,
                    recipient = recipient,
                    creatorDisplayName = creatorDisplayName
                )
            }
        }

        // Step 4: Await all event fetches and filter nulls
        val events = receivedEvents.awaitAll().filterNotNull()

        return GetReceivedEventsResult.Success(events)
    }

    /**
     * Get received events filtered by open status
     *
     * @param isOpened Filter for opened (true) or unopened (false) events
     * @param includeExpired Whether to include expired events (default: false)
     * @return GetReceivedEventsResult with filtered events or error
     */
    suspend fun invokeByOpenStatus(
        isOpened: Boolean,
        includeExpired: Boolean = false
    ): GetReceivedEventsResult {
        val result = invoke(includeExpired)

        return when (result) {
            is GetReceivedEventsResult.Success -> {
                val filtered = result.receivedEvents.filter {
                    it.isOpened == isOpened
                }
                GetReceivedEventsResult.Success(filtered)
            }
            is GetReceivedEventsResult.Error -> result
        }
    }

    /**
     * Get received events filtered by delivery status
     *
     * @param isNotified Filter for notified (true) or not notified (false) events
     * @param includeExpired Whether to include expired events (default: false)
     * @return GetReceivedEventsResult with filtered events or error
     */
    suspend fun invokeByDeliveryStatus(
        isNotified: Boolean,
        includeExpired: Boolean = false
    ): GetReceivedEventsResult {
        val result = invoke(includeExpired)

        return when (result) {
            is GetReceivedEventsResult.Success -> {
                val filtered = result.receivedEvents.filter {
                    it.isNotified == isNotified
                }
                GetReceivedEventsResult.Success(filtered)
            }
            is GetReceivedEventsResult.Error -> result
        }
    }
}
