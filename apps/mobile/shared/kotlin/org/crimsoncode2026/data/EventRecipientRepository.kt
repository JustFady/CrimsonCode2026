package org.crimsoncode2026.data

import io.github.jan-tennert.supabase.postgrest.Postgrest

/**
 * Repository interface for EventRecipient data operations
 * Note: Event clearing/dismissal is client-side only (stored in local device cache), not in the database.
 */
interface EventRecipientRepository {

    /**
     * Create recipient records for an event (batch insert for private events)
     * @param recipients List of EventRecipient to create
     * @return Result with created recipients or error
     */
    suspend fun createRecipients(recipients: List<EventRecipient>): Result<List<EventRecipient>>

    /**
     * Get all recipients for an event
     * @param eventId Event ID
     * @return Result with list of recipients or error
     */
    suspend fun getRecipientsByEvent(eventId: String): Result<List<EventRecipient>>

    /**
     * Get a specific recipient record
     * @param eventId Event ID
     * @param userId User ID
     * @return Result with recipient or null if not found
     */
    suspend fun getRecipient(eventId: String, userId: String): Result<EventRecipient?>

    /**
     * Get all events received by a user (via event_recipients)
     * @param userId User ID
     * @param includeExpired Whether to include expired events (default: false)
     * @return Result with list of events or error
     */
    suspend fun getEventsReceivedByUser(userId: String, includeExpired: Boolean = false): Result<List<EventRecipient>>

    /**
     * Get pending recipients (not yet notified)
     * @param eventId Event ID
     * @return Result with list of pending recipients or error
     */
    suspend fun getPendingRecipients(eventId: String): Result<List<EventRecipient>>

    /**
     * Get failed recipients
     * @param eventId Event ID
     * @return Result with list of failed recipients or error
     */
    suspend fun getFailedRecipients(eventId: String): Result<List<EventRecipient>>

    /**
     * Update delivery status
     * @param eventId Event ID
     * @param userId User ID
     * @param status New delivery status
     * @return Result with updated recipient or error
     */
    suspend fun updateDeliveryStatus(
        eventId: String,
        userId: String,
        status: DeliveryStatus
    ): Result<EventRecipient>

    /**
     * Mark recipient as sent (delivery notification sent)
     * @param eventId Event ID
     * @param userId User ID
     * @return Result with updated recipient or error
     */
    suspend fun markAsSent(eventId: String, userId: String): Result<EventRecipient>

    /**
     * Mark recipient as failed (delivery failed)
     * @param eventId Event ID
     * @param userId User ID
     * @return Result with updated recipient or error
     */
    suspend fun markAsFailed(eventId: String, userId: String): Result<EventRecipient>

    /**
     * Mark recipient as opened (user opened event)
     * @param eventId Event ID
     * @param userId User ID
     * @return Result with updated recipient or error
     */
    suspend fun markAsOpened(eventId: String, userId: String): Result<EventRecipient>

    /**
     * Delete recipient record
     * @param eventId Event ID
     * @param userId User ID
     * @return Result indicating success or error
     */
    suspend fun deleteRecipient(eventId: String, userId: String): Result<Unit>

    /**
     * Delete all recipients for an event
     * @param eventId Event ID
     * @return Result indicating success or error
     */
    suspend fun deleteAllRecipientsForEvent(eventId: String): Result<Unit>
}

/**
 * Supabase implementation of EventRecipientRepository
 */
class EventRecipientRepositoryImpl(
    private val postgrest: Postgrest
) : EventRecipientRepository {

    companion object {
        private const val TABLE = EventRecipient.TABLE_NAME
    }

    override suspend fun createRecipients(recipients: List<EventRecipient>): Result<List<EventRecipient>> = try {
        val result = postgrest.from(TABLE)
            .insert(recipients)
            .decodeList<EventRecipient>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getRecipientsByEvent(eventId: String): Result<List<EventRecipient>> = try {
        val result = postgrest.from(TABLE)
            .select {
                filter {
                    eq("event_id", eventId)
                }
            }
            .decodeList<EventRecipient>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getRecipient(
        eventId: String,
        userId: String
    ): Result<EventRecipient?> = try {
        val result = postgrest.from(TABLE)
            .select {
                filter {
                    eq("event_id", eventId)
                    eq("user_id", userId)
                }
            }
            .decodeSingleOrNull<EventRecipient>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getEventsReceivedByUser(
        userId: String,
        includeExpired: Boolean
    ): Result<List<EventRecipient>> = try {
        val result = if (includeExpired) {
            postgrest.from(TABLE)
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<EventRecipient>()
        } else {
            // Note: Expired events are determined by joining with events table
            // For MVP, we return all and filter on client side or use a view
            postgrest.from(TABLE)
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<EventRecipient>()
        }

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getPendingRecipients(eventId: String): Result<List<EventRecipient>> = try {
        val result = postgrest.from(TABLE)
            .select {
                filter {
                    eq("event_id", eventId)
                    eq("delivery_status", DeliveryStatus.PENDING.value)
                }
            }
            .decodeList<EventRecipient>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getFailedRecipients(eventId: String): Result<List<EventRecipient>> = try {
        val result = postgrest.from(TABLE)
            .select {
                filter {
                    eq("event_id", eventId)
                    eq("delivery_status", DeliveryStatus.FAILED.value)
                }
            }
            .decodeList<EventRecipient>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateDeliveryStatus(
        eventId: String,
        userId: String,
        status: DeliveryStatus
    ): Result<EventRecipient> = try {
        val updateMap = mutableMapOf<String, Any>("delivery_status" to status.value)
        if (status == DeliveryStatus.SENT) {
            updateMap["notified_at"] = System.currentTimeMillis()
        }

        val result = postgrest.from(TABLE)
            .update(updateMap) {
                filter {
                    eq("event_id", eventId)
                    eq("user_id", userId)
                }
            }
            .decodeSingle<EventRecipient>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun markAsSent(
        eventId: String,
        userId: String
    ): Result<EventRecipient> = try {
        val now = System.currentTimeMillis()
        val result = postgrest.from(TABLE)
            .update(
                mapOf(
                    "delivery_status" to DeliveryStatus.SENT.value,
                    "notified_at" to now
                )
            ) {
                filter {
                    eq("event_id", eventId)
                    eq("user_id", userId)
                }
            }
            .decodeSingle<EventRecipient>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun markAsFailed(
        eventId: String,
        userId: String
    ): Result<EventRecipient> = try {
        val now = System.currentTimeMillis()
        val result = postgrest.from(TABLE)
            .update(
                mapOf(
                    "delivery_status" to DeliveryStatus.FAILED.value,
                    "notified_at" to now
                )
            ) {
                filter {
                    eq("event_id", eventId)
                    eq("user_id", userId)
                }
            }
            .decodeSingle<EventRecipient>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun markAsOpened(
        eventId: String,
        userId: String
    ): Result<EventRecipient> = try {
        val result = postgrest.from(TABLE)
            .update(
                mapOf(
                    "opened_at" to System.currentTimeMillis()
                )
            ) {
                filter {
                    eq("event_id", eventId)
                    eq("user_id", userId)
                }
            }
            .decodeSingle<EventRecipient>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteRecipient(
        eventId: String,
        userId: String
    ): Result<Unit> = try {
        postgrest.from(TABLE)
            .delete {
                filter {
                    eq("event_id", eventId)
                    eq("user_id", userId)
                }
            }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteAllRecipientsForEvent(eventId: String): Result<Unit> = try {
        postgrest.from(TABLE)
            .delete {
                filter {
                    eq("event_id", eventId)
                }
            }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
