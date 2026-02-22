package org.crimsoncode2026.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Push notification data model for FCM payload
 *
 * Defines the structure of push notification data sent from server to client.
 * Used by edge functions for constructing FCM payloads and by clients for parsing received notifications.
 *
 * @property eventId Unique identifier for the emergency event
 * @property severity Event severity (ALERT or CRISIS) for styling/vibration
 * @property category Event category (MEDICAL, FIRE, etc.) for icon/label
 * @property description Brief description of the emergency event
 * @property lat Optional latitude coordinate for map navigation
 * @property lon Optional longitude coordinate for map navigation
 * @property deepLink Deep link URL to open app to specific event (crimsoncode://event/{eventId})
 */
@Serializable
data class PushNotificationPayload(
    @SerialName("event_id")
    val eventId: String,

    @SerialName("severity")
    val severity: String,

    @SerialName("category")
    val category: String,

    @SerialName("description")
    val description: String,

    @SerialName("lat")
    val lat: Double? = null,

    @SerialName("lon")
    val lon: Double? = null,

    @SerialName("deep_link")
    val deepLink: String
) {
    companion object {
        const val DEEP_LINK_BASE = "crimsoncode://event/"

        /**
         * Creates a PushNotificationPayload from an Event
         *
         * @param event The event to create notification payload for
         * @return PushNotificationPayload with event data
         */
        fun fromEvent(event: Event): PushNotificationPayload {
            return PushNotificationPayload(
                eventId = event.id,
                severity = event.severity,
                category = event.category,
                description = event.description,
                lat = event.lat,
                lon = event.lon,
                deepLink = "$DEEP_LINK_BASE${event.id}"
            )
        }

        /**
         * Creates deep link URL for an event ID
         *
         * @param eventId Event ID
         * @return Deep link URL (crimsoncode://event/{eventId})
         */
        fun createDeepLink(eventId: String): String {
            return "$DEEP_LINK_BASE$eventId"
        }

        /**
         * Parses event ID from deep link URL
         *
         * @param deepLink Deep link URL to parse
         * @return Event ID, or null if invalid format
         */
        fun parseEventIdFromDeepLink(deepLink: String?): String? {
            if (deepLink == null || !deepLink.startsWith(DEEP_LINK_BASE)) {
                return null
            }
            return deepLink.removePrefix(DEEP_LINK_BASE).takeIf { it.isNotEmpty() }
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
     * Checks if this is a crisis notification for high-priority handling
     */
    val isCrisis: Boolean
        get() = severityEnum == Severity.CRISIS

    /**
     * Builds notification title from severity and category
     *
     * Format: "CRISIS - Medical" or "ALERT - Fire"
     */
    val notificationTitle: String
        get() {
            val severityLabel = when (severityEnum) {
                Severity.CRISIS -> "CRISIS"
                Severity.ALERT -> "ALERT"
                null -> severity.uppercase()
            }
            return "$severityLabel - ${categoryEnum?.displayName ?: category}"
        }
}
