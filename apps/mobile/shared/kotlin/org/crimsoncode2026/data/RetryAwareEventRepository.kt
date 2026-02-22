package org.crimsoncode2026.data

import org.crimsoncode2026.network.RetryHandler

/**
 * Retry-aware wrapper for EventRepository
 *
 * Adds automatic retry logic with exponential backoff for all network operations.
 * Wraps an existing EventRepository instance.
 *
 * Spec Stage 9 requirement: Retry handling for event creation and acknowledgments
 */
class RetryAwareEventRepository(
    private val delegate: EventRepository,
    private val retryHandler: RetryHandler = RetryHandler()
) : EventRepository {

    override suspend fun createEvent(event: Event): Result<Event> {
        return retryHandler.retryResult {
            delegate.createEvent(event)
        }
    }

    override suspend fun getEventById(eventId: String): Result<Event?> {
        return retryHandler.retryResult {
            delegate.getEventById(eventId)
        }
    }

    override suspend fun getEventsByCreator(
        creatorId: String,
        includeInactive: Boolean
    ): Result<List<Event>> {
        return retryHandler.retryResult {
            delegate.getEventsByCreator(creatorId, includeInactive)
        }
    }

    override suspend fun getPublicEventsByBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): Result<List<Event>> {
        return retryHandler.retryResult {
            delegate.getPublicEventsByBounds(minLat, maxLat, minLon, maxLon)
        }
    }

    override suspend fun getPublicEventsByRadius(
        lat: Double,
        lon: Double,
        radiusMiles: Double
    ): Result<List<Event>> {
        return retryHandler.retryResult {
            delegate.getPublicEventsByRadius(lat, lon, radiusMiles)
        }
    }

    override suspend fun getActiveEvents(): Result<List<Event>> {
        return retryHandler.retryResult {
            delegate.getActiveEvents()
        }
    }

    override suspend fun getPrivateEventsForUser(userId: String): Result<List<Event>> {
        return retryHandler.retryResult {
            delegate.getPrivateEventsForUser(userId)
        }
    }

    override suspend fun deleteEvent(eventId: String): Result<Unit> {
        return retryHandler.retryResult {
            delegate.deleteEvent(eventId)
        }
    }

    override suspend fun cleanupExpiredEvents(): Result<Int> {
        return retryHandler.retryResult {
            delegate.cleanupExpiredEvents()
        }
    }
}
