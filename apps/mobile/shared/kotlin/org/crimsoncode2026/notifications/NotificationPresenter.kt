package org.crimsoncode2026.notifications

import io.github.mirzemehdi.kmpnotifier.NotifierManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.crimsoncode2026.data.Severity

/**
 * Result of showing a notification
 */
sealed class NotificationResult {
    data object Success : NotificationResult()
    data class Error(val message: String) : NotificationResult()
}

/**
 * Notification configuration options
 */
data class NotificationOptions(
    val eventId: String,
    val severity: Severity,
    val category: String,
    val description: String,
    val lat: Double? = null,
    val lon: Double? = null
)

/**
 * Notification Presenter
 *
 * Wraps KmpNotifier for displaying push notifications with severity-based styling.
 *
 * Spec requirements:
 * - Crisis Alert: Aggressive vibration, High priority, "View on Map" action
 * - Alert (Warning): Standard vibration, Normal priority, "View on Map" action
 * - Payload: Title (Severity + Category), Body (description), Data (event_id, coordinates, deep link)
 */
class NotificationPresenter(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {

    companion object {
        private const val DEEP_LINK_BASE = "crimsoncode://event/"

        /**
         * Payload data keys for notification data
         */
        const val KEY_EVENT_ID = "event_id"
        const val KEY_SEVERITY = "severity"
        const val KEY_CATEGORY = "category"
        const val KEY_LAT = "lat"
        const val KEY_LON = "lon"
        const val KEY_DEEP_LINK = "deep_link"
        const val KEY_VIEW_ON_MAP = "view_on_map"
        const val KEY_CHANNEL_ID = "channel_id"
    }

    /**
     * Get local notifier instance from KmpNotifier
     */
    private val localNotifier
        get() = NotifierManager.getLocalNotifier()

    /**
     * Show notification for an emergency event
     *
     * @param options Notification configuration including event data
     * @return NotificationResult indicating success or error
     */
    fun showNotification(options: NotificationOptions): NotificationResult {
        return try {
            scope.launch {
                localNotifier.notify {
                    id = options.eventId.hashCode()
                    title = buildNotificationTitle(options.severity, options.category)
                    body = options.description
                    payloadData = buildPayloadData(options)
                }
            }
            NotificationResult.Success
        } catch (e: Exception) {
            NotificationResult.Error(e.message ?: "Failed to show notification")
        }
    }

    /**
     * Build notification title based on severity and category
     *
     * Spec: "Severity + Category" (e.g., "CRISIS - Medical", "ALERT - Fire")
     *
     * @param severity Event severity
     * @param category Event category
     * @return Formatted notification title
     */
    private fun buildNotificationTitle(severity: Severity, category: String): String {
        val severityLabel = when (severity) {
            Severity.CRISIS -> "CRISIS"
            Severity.ALERT -> "ALERT"
        }
        return "$severityLabel - $category"
    }

    /**
     * Build payload data map for notification
     *
     * Includes event_id, coordinates, severity, category, deep link URL, and channel ID
     *
     * @param options Notification configuration
     * @return Map of payload data
     */
    private fun buildPayloadData(options: NotificationOptions): Map<String, Any?> {
        return buildMap {
            put(KEY_EVENT_ID, options.eventId)
            put(KEY_SEVERITY, options.severity.value)
            put(KEY_CATEGORY, options.category)
            put(KEY_CHANNEL_ID, NotificationChannelProvider.getChannelId(options.severity == Severity.CRISIS))

            if (options.lat != null && options.lon != null) {
                put(KEY_LAT, options.lat)
                put(KEY_LON, options.lon)
            }

            put(KEY_DEEP_LINK, "$DEEP_LINK_BASE${options.eventId}")
            put(KEY_VIEW_ON_MAP, true)
        }
    }

    /**
     * Remove notification by event ID
     *
     * @param eventId Event ID to remove
     */
    fun removeNotification(eventId: String) {
        try {
            localNotifier.remove(eventId.hashCode())
        } catch (e: Exception) {
            // Silently fail - notification may already be dismissed
        }
    }

    /**
     * Remove all notifications
     */
    fun clearAllNotifications() {
        try {
            localNotifier.removeAll()
        } catch (e: Exception) {
            // Silently fail - no notifications to clear
        }
    }

    /**
     * Get deep link URL for an event
     *
     * @param eventId Event ID
     * @return Deep link URL
     */
    fun getDeepLinkUrl(eventId: String): String {
        return "$DEEP_LINK_BASE$eventId"
    }

    /**
     * Parse event ID from deep link URL
     *
     * @param deepLink Deep link URL
     * @return Event ID, or null if invalid format
     */
    fun parseEventIdFromDeepLink(deepLink: String?): String? {
        if (deepLink == null || !deepLink.startsWith(DEEP_LINK_BASE)) {
            return null
        }
        return deepLink.removePrefix(DEEP_LINK_BASE).takeIf { it.isNotEmpty() }
    }
}
