package org.crimsoncode2026.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Event severity level
 */
enum class Severity(val value: String) {
    @SerialName("ALERT")
    ALERT("ALERT"),

    @SerialName("CRISIS")
    CRISIS("CRISIS");

    companion object {
        fun fromValue(value: String?): Severity? = values().find { it.value == value }
    }
}

/**
 * Event category for marker icon/color + filtering
 */
enum class Category(val value: String, val displayName: String) {
    @SerialName("MEDICAL")
    MEDICAL("MEDICAL", "Medical"),

    @SerialName("FIRE")
    FIRE("FIRE", "Fire"),

    @SerialName("WEATHER")
    WEATHER("WEATHER", "Weather"),

    @SerialName("CRIME")
    CRIME("CRIME", "Crime"),

    @SerialName("NATURAL_DISASTER")
    NATURAL_DISASTER("NATURAL_DISASTER", "Natural Disaster"),

    @SerialName("INFRASTRUCTURE")
    INFRASTRUCTURE("INFRASTRUCTURE", "Infrastructure"),

    @SerialName("SEARCH_RESCUE")
    SEARCH_RESCUE("SEARCH_RESCUE", "Search & Rescue"),

    @SerialName("TRAFFIC")
    TRAFFIC("TRAFFIC", "Traffic"),

    @SerialName("OTHER")
    OTHER("OTHER", "Other");

    companion object {
        fun fromValue(value: String?): Category? = values().find { it.value == value }
    }
}

/**
 * Broadcast type for event delivery routing
 */
enum class BroadcastType(val value: String) {
    @SerialName("PUBLIC")
    PUBLIC("PUBLIC"),

    @SerialName("PRIVATE")
    PRIVATE("PRIVATE");

    companion object {
        fun fromValue(value: String?): BroadcastType? = values().find { it.value == value }
    }
}

/**
 * Event data model
 *
 * The canonical record of emergency alerts.
 *
 * @property id Event identity (UUID)
 * @property creatorId Creator reference (foreign key to users.id)
 * @property severity Alert vs crisis behavior/UI
 * @property category Marker icon/color + filtering
 * @property lat Map placement latitude
 * @property lon Map placement longitude
 * @property locationOverride Manual location text (nullable)
 * @property broadcastType Public/private routing
 * @property description Human-readable details (max 500 chars)
 * @property isAnonymous Hide creator identity for public alerts
 * @property createdAt Event time (milliseconds since epoch)
 * @property expiresAt Auto-expiration cutoff (milliseconds since epoch)
 * @property deletedAt Optional moderation/soft delete (nullable, milliseconds since epoch)
 */
@Serializable
data class Event(
    @SerialName("id")
    val id: String,

    @SerialName("creator_id")
    val creatorId: String,

    @SerialName("severity")
    val severity: String,

    @SerialName("category")
    val category: String,

    @SerialName("lat")
    val lat: Double,

    @SerialName("lon")
    val lon: Double,

    @SerialName("location_override")
    val locationOverride: String? = null,

    @SerialName("broadcast_type")
    val broadcastType: String,

    @SerialName("description")
    val description: String,

    @SerialName("is_anonymous")
    val isAnonymous: Boolean = true,

    @SerialName("created_at")
    val createdAt: Long,

    @SerialName("expires_at")
    val expiresAt: Long,

    @SerialName("deleted_at")
    val deletedAt: Long? = null
) {
    companion object {
        const val TABLE_NAME = "events"

        const val DEFAULT_EXPIRATION_HOURS = 48
        val DEFAULT_EXPIRATION_MS = DEFAULT_EXPIRATION_HOURS * 60 * 60 * 1000L

        /**
         * Creates expiresAt timestamp from createdAt
         */
        fun calculateExpiration(createdAt: Long = System.currentTimeMillis()): Long {
            return createdAt + DEFAULT_EXPIRATION_MS
        }
    }

    /**
     * Converts severity string to Severity enum
     */
    val severityEnum: Severity?
        get() = Severity.fromValue(severity)

    /**
     * Converts category string to Category enum
     */
    val categoryEnum: Category?
        get() = Category.fromValue(category)

    /**
     * Converts broadcastType string to BroadcastType enum
     */
    val broadcastTypeEnum: BroadcastType?
        get() = BroadcastType.fromValue(broadcastType)

    /**
     * Checks if event is expired
     */
    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAt

    /**
     * Checks if event is deleted
     */
    val isDeleted: Boolean
        get() = deletedAt != null

    /**
     * Checks if event is active (not expired and not deleted)
     */
    val isActive: Boolean
        get() = !isExpired && !isDeleted

    /**
     * Checks if this is a public event
     */
    val isPublic: Boolean
        get() = broadcastTypeEnum == BroadcastType.PUBLIC

    /**
     * Checks if this is a private event
     */
    val isPrivate: Boolean
        get() = broadcastTypeEnum == BroadcastType.PRIVATE
}
