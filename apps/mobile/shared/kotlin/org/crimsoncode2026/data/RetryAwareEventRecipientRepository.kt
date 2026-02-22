package org.crimsoncode2026.data

import org.crimsoncode2026.network.RetryHandler

/**
 * Retry-aware wrapper for EventRecipientRepository
 *
 * Adds automatic retry logic with exponential backoff for all network operations.
 * Wraps an existing EventRecipientRepository instance.
 *
 * Spec Stage 9 requirement: Retry handling for event acknowledgments
 */
class RetryAwareEventRecipientRepository(
    private val delegate: EventRecipientRepository,
    private val retryHandler: RetryHandler = RetryHandler()
) : EventRecipientRepository {

    override suspend fun createRecipients(recipients: List<EventRecipient>): Result<List<EventRecipient>> {
        return retryHandler.retryResult {
            delegate.createRecipients(recipients)
        }
    }

    override suspend fun getRecipientsByEvent(eventId: String): Result<List<EventRecipient>> {
        return retryHandler.retryResult {
            delegate.getRecipientsByEvent(eventId)
        }
    }

    override suspend fun getRecipient(eventId: String, userId: String): Result<EventRecipient?> {
        return retryHandler.retryResult {
            delegate.getRecipient(eventId, userId)
        }
    }

    override suspend fun getEventsReceivedByUser(userId: String, includeExpired: Boolean): Result<List<EventRecipient>> {
        return retryHandler.retryResult {
            delegate.getEventsReceivedByUser(userId, includeExpired)
        }
    }

    override suspend fun getPendingRecipients(eventId: String): Result<List<EventRecipient>> {
        return retryHandler.retryResult {
            delegate.getPendingRecipients(eventId)
        }
    }

    override suspend fun getFailedRecipients(eventId: String): Result<List<EventRecipient>> {
        return retryHandler.retryResult {
            delegate.getFailedRecipients(eventId)
        }
    }

    override suspend fun updateDeliveryStatus(
        eventId: String,
        userId: String,
        status: DeliveryStatus
    ): Result<EventRecipient> {
        return retryHandler.retryResult {
            delegate.updateDeliveryStatus(eventId, userId, status)
        }
    }

    override suspend fun markAsSent(eventId: String, userId: String): Result<EventRecipient> {
        return retryHandler.retryResult {
            delegate.markAsSent(eventId, userId)
        }
    }

    override suspend fun markAsFailed(eventId: String, userId: String): Result<EventRecipient> {
        return retryHandler.retryResult {
            delegate.markAsFailed(eventId, userId)
        }
    }

    override suspend fun markAsOpened(eventId: String, userId: String): Result<EventRecipient> {
        return retryHandler.retryResult {
            delegate.markAsOpened(eventId, userId)
        }
    }

    override suspend fun deleteRecipient(eventId: String, userId: String): Result<Unit> {
        return retryHandler.retryResult {
            delegate.deleteRecipient(eventId, userId)
        }
    }

    override suspend fun deleteAllRecipientsForEvent(eventId: String): Result<Unit> {
        return retryHandler.retryResult {
            delegate.deleteAllRecipientsForEvent(eventId)
        }
    }
}
