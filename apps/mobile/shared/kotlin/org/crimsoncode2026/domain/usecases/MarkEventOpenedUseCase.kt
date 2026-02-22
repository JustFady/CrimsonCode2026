package org.crimsoncode2026.domain.usecases

import org.crimsoncode2026.data.EventRecipient
import org.crimsoncode2026.data.EventRecipientRepository
import org.crimsoncode2026.domain.UserSessionManager

/**
 * Result of marking event as opened
 */
sealed class MarkEventOpenedResult {
    data class Success(
        val recipient: EventRecipient
    ) : MarkEventOpenedResult()

    data class Error(val message: String) : MarkEventOpenedResult()
}

/**
 * Mark Event Opened Use Case
 *
 * Marks a received event as opened for the current user.
 * Updates the opened_at timestamp in event_recipients table.
 *
 * Spec requirement: Track per-user opened state for private events.
 */
class MarkEventOpenedUseCase(
    private val eventRecipientRepository: EventRecipientRepository,
    private val userSessionManager: UserSessionManager
) {

    /**
     * Mark an event as opened for the current user
     *
     * @param eventId Event ID to mark as opened
     * @return MarkEventOpenedResult with updated recipient or error
     */
    suspend operator fun invoke(eventId: String): MarkEventOpenedResult {
        // Step 1: Get current user ID
        val userId = userSessionManager.getCurrentUserId()
            ?: return MarkEventOpenedResult.Error("User not authenticated")

        // Step 2: Mark event as opened
        val result = eventRecipientRepository.markAsOpened(
            eventId = eventId,
            userId = userId
        )

        return when (result) {
            is Result.Success -> {
                val recipient = result.getOrNull()
                if (recipient != null) {
                    MarkEventOpenedResult.Success(recipient)
                } else {
                    MarkEventOpenedResult.Error("Event not found or not received by user")
                }
            }
            is Result.Failure -> MarkEventOpenedResult.Error(
                result.exceptionOrNull()?.message ?: "Failed to mark event as opened"
            )
        }
    }
}
