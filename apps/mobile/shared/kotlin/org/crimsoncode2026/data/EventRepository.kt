package org.crimsoncode2026.data

import io.github.jan.supabase.postgrest.Postgrest

/**
 * Repository interface for Event data operations
 */
interface EventRepository {

    /**
     * Create a new event
     * @param event Event to create
     * @return Result with created event or error
     */
    suspend fun createEvent(event: Event): Result<Event>

    /**
     * Get event by ID
     * @param eventId Event ID
     * @return Result with event or null if not found
     */
    suspend fun getEventById(eventId: String): Result<Event?>

    /**
     * Get events by creator ID
     * @param creatorId Creator user ID
     * @param includeInactive Whether to include inactive/expired events (default: false)
     * @return Result with list of events or error
     */
    suspend fun getEventsByCreator(creatorId: String, includeInactive: Boolean = false): Result<List<Event>>

    /**
     * Get public events within map bounds
     * @param minLat Minimum latitude
     * @param maxLat Maximum latitude
     * @param minLon Minimum longitude
     * @param maxLon Maximum longitude
     * @return Result with list of active public events or error
     */
    suspend fun getPublicEventsByBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): Result<List<Event>>

    /**
     * Get public events within radius of a point (haversine formula approximation)
     * @param lat Center latitude
     * @param lon Center longitude
     * @param radiusMiles Radius in miles (default: 50)
     * @return Result with list of active public events or error
     */
    suspend fun getPublicEventsByRadius(
        lat: Double,
        lon: Double,
        radiusMiles: Double = 50.0
    ): Result<List<Event>>

    /**
     * Get all active events (not expired, not deleted)
     * @return Result with list of active events or error
     */
    suspend fun getActiveEvents(): Result<List<Event>>

    /**
     * Get private events for a recipient user
     * @param userId Recipient user ID
     * @return Result with list of active private events or error
     */
    suspend fun getPrivateEventsForUser(userId: String): Result<List<Event>>

    /**
     * Soft delete an event (set deleted_at timestamp)
     * @param eventId Event ID
     * @return Result indicating success or error
     */
    suspend fun deleteEvent(eventId: String): Result<Unit>

    /**
     * Clean up expired events (admin/edge function use only)
     * @return Result with number of deleted events or error
     */
    suspend fun cleanupExpiredEvents(): Result<Int>
}

/**
 * Supabase implementation of EventRepository
 */
class EventRepositoryImpl(
    private val postgrest: Postgrest
) : EventRepository {

    companion object {
        private const val TABLE = Event.TABLE_NAME
        private const val DEFAULT_RADIUS_MILES = 50.0

        /**
         * Convert miles to degrees (approximate for latitude)
         * 1 degree of latitude ≈ 69 miles
         */
        private fun milesToDegreesLat(miles: Double): Double = miles / 69.0

        /**
         * Convert miles to degrees (approximate for longitude)
         * 1 degree of longitude ≈ 69 miles * cos(latitude)
         */
        private fun milesToDegreesLon(miles: Double, lat: Double): Double =
            miles / (69.0 * kotlin.math.cos(kotlin.math.PI * lat / 180.0))
    }

    override suspend fun createEvent(event: Event): Result<Event> = try {
        val result = postgrest.from(TABLE)
            .insert(event)
            .decodeSingle<Event>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getEventById(eventId: String): Result<Event?> = try {
        val result = postgrest.from(TABLE)
            .select {
                filter {
                    eq("id", eventId)
                }
            }
            .decodeSingleOrNull<Event>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getEventsByCreator(
        creatorId: String,
        includeInactive: Boolean
    ): Result<List<Event>> = try {
        val result = if (includeInactive) {
            postgrest.from(TABLE)
                .select {
                    filter {
                        eq("creator_id", creatorId)
                    }
                }
                .decodeList<Event>()
        } else {
            postgrest.from(TABLE)
                .select {
                    filter {
                        eq("creator_id", creatorId)
                        gt("expires_at", System.currentTimeMillis())
                        `is`("deleted_at", null)
                    }
                }
                .decodeList<Event>()
        }

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getPublicEventsByBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): Result<List<Event>> = try {
        val now = System.currentTimeMillis()

        val result = postgrest.from(TABLE)
            .select {
                filter {
                    eq("broadcast_type", BroadcastType.PUBLIC.value)
                    gte("lat", minLat)
                    lte("lat", maxLat)
                    gte("lon", minLon)
                    lte("lon", maxLon)
                    gt("expires_at", now)
                    `is`("deleted_at", null)
                }
            }
            .decodeList<Event>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getPublicEventsByRadius(
        lat: Double,
        lon: Double,
        radiusMiles: Double
    ): Result<List<Event>> = try {
        val latRadius = milesToDegreesLat(radiusMiles)
        val lonRadius = milesToDegreesLon(radiusMiles, lat)
        val minLat = lat - latRadius
        val maxLat = lat + latRadius
        val minLon = lon - lonRadius
        val maxLon = lon + lonRadius
        val now = System.currentTimeMillis()

        val result = postgrest.from(TABLE)
            .select {
                filter {
                    eq("broadcast_type", BroadcastType.PUBLIC.value)
                    gte("lat", minLat)
                    lte("lat", maxLat)
                    gte("lon", minLon)
                    lte("lon", maxLon)
                    gt("expires_at", now)
                    `is`("deleted_at", null)
                }
            }
            .decodeList<Event>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getActiveEvents(): Result<List<Event>> = try {
        val now = System.currentTimeMillis()

        val result = postgrest.from(TABLE)
            .select {
                filter {
                    gt("expires_at", now)
                    `is`("deleted_at", null)
                }
            }
            .decodeList<Event>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getPrivateEventsForUser(userId: String): Result<List<Event>> = try {
        val now = System.currentTimeMillis()

        val result = postgrest.from(TABLE)
            .select {
                filter {
                    eq("broadcast_type", BroadcastType.PRIVATE.value)
                    gt("expires_at", now)
                    `is`("deleted_at", null)
                }
            }
            .decodeList<Event>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteEvent(eventId: String): Result<Unit> = try {
        postgrest.from(TABLE)
            .update(
                mapOf(
                    "deleted_at" to System.currentTimeMillis()
                )
            ) {
                filter {
                    eq("id", eventId)
                }
            }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun cleanupExpiredEvents(): Result<Int> = try {
        val now = System.currentTimeMillis()

        postgrest.from(TABLE)
            .delete {
                filter {
                    lt("expires_at", now)
                }
            }

        Result.success(0)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
