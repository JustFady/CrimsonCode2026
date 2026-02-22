package org.crimsoncode2026.domain.usecases

import org.crimsoncode2026.data.BroadcastType
import org.crimsoncode2026.data.Category
import org.crimsoncode2026.data.Event
import org.crimsoncode2026.data.EventRepository
import org.crimsoncode2026.data.Severity
import org.crimsoncode2026.domain.UserSessionManager

/**
 * Result of creating a public event
 */
sealed class CreatePublicEventResult {
    data class Success(val event: Event) : CreatePublicEventResult()
    data class Error(val message: String) : CreatePublicEventResult()
}

/**
 * Create Public Event Use Case
 *
 * Creates public events using EventRepository.createEvent.
 * Sets broadcastType=PUBLIC, isAnonymous=true, calculates expiresAt 48 hours from now.
 *
 * Spec requirements:
 * - Public broadcasts include 50-mile radius logic (handled by query layer)
 * - Public events do not expose creator identity
 * - Events expire after 48 hours
 *
 * @param eventRepository Repository for event operations
 * @param userSessionManager Manager for current user session
 */
class CreatePublicEventUseCase(
    private val eventRepository: EventRepository,
    private val userSessionManager: UserSessionManager
) {

    /**
     * Create a public event
     *
     * @param severity Event severity (ALERT or CRISIS)
     * @param category Event category for marker icon/color
     * @param lat Latitude of event location
     * @param lon Longitude of event location
     * @param locationOverride Optional manual location text
     * @param description Human-readable details (max 500 characters)
     * @return CreatePublicEventResult with created event or error
     */
    suspend operator fun invoke(
        severity: Severity,
        category: Category,
        lat: Double,
        lon: Double,
        locationOverride: String? = null,
        description: String
    ): CreatePublicEventResult {
        // Step 1: Get current user ID
        val creatorId = userSessionManager.getCurrentUserId()
            ?: return CreatePublicEventResult.Error("User not authenticated")

        // Step 2: Calculate expiration (48 hours from now)
        val now = System.currentTimeMillis()
        val expiresAt = Event.calculateExpiration(now)

        // Step 3: Create public event
        val event = Event(
            id = java.util.UUID.randomUUID().toString(),
            creatorId = creatorId,
            severity = severity.value,
            category = category.value,
            lat = lat,
            lon = lon,
            locationOverride = locationOverride,
            broadcastType = BroadcastType.PUBLIC.value,
            description = description,
            isAnonymous = true,
            createdAt = now,
            expiresAt = expiresAt
        )

        // Step 4: Insert event via repository
        val result = eventRepository.createEvent(event)

        return when (result) {
            is Result.Success -> {
                val createdEvent = result.getOrNull()
                if (createdEvent != null) {
                    CreatePublicEventResult.Success(createdEvent)
                } else {
                    CreatePublicEventResult.Error("Failed to create event")
                }
            }
            is Result.Failure -> {
                CreatePublicEventResult.Error(
                    result.exceptionOrNull()?.message ?: "Failed to create event"
                )
            }
        }
    }

    /**
     * Create a public event from an existing Event object
     *
     * Ensures the event has proper public settings before creation.
     *
     * @param event Event object to convert to public
     * @return CreatePublicEventResult with created event or error
     */
    suspend operator fun invoke(event: Event): CreatePublicEventResult {
        // Step 1: Get current user ID
        val creatorId = userSessionManager.getCurrentUserId()
            ?: return CreatePublicEventResult.Error("User not authenticated")

        // Step 2: Calculate expiration if not set
        val now = System.currentTimeMillis()
        val expiresAt = if (event.expiresAt > 0) {
            event.expiresAt
        } else {
            Event.calculateExpiration(now)
        }

        // Step 3: Create public event with required settings
        val publicEvent = event.copy(
            creatorId = creatorId,
            broadcastType = BroadcastType.PUBLIC.value,
            isAnonymous = true,
            createdAt = if (event.createdAt > 0) event.createdAt else now,
            expiresAt = expiresAt
        )

        // Step 4: Insert event via repository
        val result = eventRepository.createEvent(publicEvent)

        return when (result) {
            is Result.Success -> {
                val createdEvent = result.getOrNull()
                if (createdEvent != null) {
                    CreatePublicEventResult.Success(createdEvent)
                } else {
                    CreatePublicEventResult.Error("Failed to create event")
                }
            }
            is Result.Failure -> {
                CreatePublicEventResult.Error(
                    result.exceptionOrNull()?.message ?: "Failed to create event"
                )
            }
        }
    }
}
