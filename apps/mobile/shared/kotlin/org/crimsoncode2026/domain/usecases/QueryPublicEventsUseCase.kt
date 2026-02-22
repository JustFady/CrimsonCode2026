package org.crimsoncode2026.domain.usecases

import org.crimsoncode2026.data.Event

/**
 * Result of querying public events
 */
sealed class QueryPublicEventsResult {
    data class Success(val events: List<Event>) : QueryPublicEventsResult()
    data class Error(val message: String) : QueryPublicEventsResult()
}

/**
 * Parameters for querying public events within map bounds
 */
data class QueryPublicEventsParams(
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double
)

/**
 * Query Public Events Use Case
 *
 * Wraps EventRepository.getPublicEventsByBounds with opt-out filtering.
 * Checks user's public alert preference and returns filtered results.
 *
 * Spec requirements:
 * - Public events within map bounds
 * - Respect user public-alert opt-out preference
 * - Only return active events (not expired, not deleted)
 *
 * @param eventRepository Repository for event data operations
 * @param preferencesStorage Local preferences storage for opt-out checking
 */
class QueryPublicEventsUseCase(
    private val eventRepository: org.crimsoncode2026.data.EventRepository,
    private val preferencesStorage: org.crimsoncode2026.storage.PreferencesStorage
) {

    /**
     * Query public events within specified map bounds
     *
     * @param params Map bounds parameters
     * @return QueryPublicEventsResult with filtered events or error
     */
    suspend operator fun invoke(params: QueryPublicEventsParams): QueryPublicEventsResult {
        // Check if user has opted out of public alerts
        val hasOptedOut = preferencesStorage.getPublicAlertOptOut()

        // Return empty list if user opted out
        if (hasOptedOut) {
            return QueryPublicEventsResult.Success(emptyList())
        }

        // Validate bounds parameters
        val validationError = validateParams(params)
        if (validationError != null) {
            return QueryPublicEventsResult.Error(validationError)
        }

        // Query repository for public events within bounds
        return when (val result = eventRepository.getPublicEventsByBounds(
            minLat = params.minLat,
            maxLat = params.maxLat,
            minLon = params.minLon,
            maxLon = params.maxLon
        )) {
            is kotlin.Result.Success -> {
                QueryPublicEventsResult.Success(result.getOrNull() ?: emptyList())
            }
            is kotlin.Result.Failure -> {
                QueryPublicEventsResult.Error(
                    result.exceptionOrNull()?.message ?: "Failed to query public events"
                )
            }
        }
    }

    /**
     * Validate map bounds parameters
     *
     * @param params Parameters to validate
     * @return Error message if invalid, null if valid
     */
    private fun validateParams(params: QueryPublicEventsParams): String? {
        // Validate latitude ranges (-90 to 90)
        if (params.minLat < -90.0 || params.minLat > 90.0) {
            return "Invalid minimum latitude. Must be between -90 and 90"
        }
        if (params.maxLat < -90.0 || params.maxLat > 90.0) {
            return "Invalid maximum latitude. Must be between -90 and 90"
        }

        // Validate longitude ranges (-180 to 180)
        if (params.minLon < -180.0 || params.minLon > 180.0) {
            return "Invalid minimum longitude. Must be between -180 and 180"
        }
        if (params.maxLon < -180.0 || params.maxLon > 180.0) {
            return "Invalid maximum longitude. Must be between -180 and 180"
        }

        // Validate min is less than max
        if (params.minLat >= params.maxLat) {
            return "Minimum latitude must be less than maximum latitude"
        }
        if (params.minLon >= params.maxLon) {
            return "Minimum longitude must be less than maximum longitude"
        }

        return null
    }

    /**
     * Query public events using map bounds data class
     *
     * @param bounds Map bounds with north/south/east/west
     * @return QueryPublicEventsResult with filtered events or error
     */
    suspend operator fun invoke(bounds: org.crimsoncode2026.compose.MapBounds): QueryPublicEventsResult {
        return invoke(
            QueryPublicEventsParams(
                minLat = bounds.south,
                maxLat = bounds.north,
                minLon = bounds.west,
                maxLon = bounds.east
            )
        )
    }
}
