package org.crimsoncode2026.notifications

import io.github.mirzemehdi.kmpnotifier.NotifierManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.crimsoncode2026.data.Severity

/**
 * Notification click event containing deep link data
 */
data class NotificationClickEvent(
    val eventId: String,
    val deepLinkUrl: String,
    val severity: Severity,
    val category: String
)

/**
 * PushNotificationManager
 *
 * Manages push notification display for the app.
 *
 * Provides methods for:
 * - Showing notifications with severity-based styling
 * - Removing notifications
 * - Building and parsing deep links
 *
 * This class wraps NotificationPresenter for displaying notifications.
 * Notification click handling is done through NotificationClickState
 * which is populated by platform-specific code (MainActivity, iOSApp).
 *
 * Spec requirements:
 * - Crisis Alert: Aggressive vibration, High priority
 * - Alert (Warning): Standard vibration, Normal priority
 * - Deep link: crimsoncode://event/{eventId}
 * - Actions: "View on Map"
 */
class PushNotificationManager(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
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
    }

    private val notificationPresenter = NotificationPresenter(scope)

    /**
     * Initialize the push notification manager
     *
     * Currently a no-op as notification click handling is done
     * through platform-specific code.
     * May be expanded in the future for additional initialization.
     */
    fun initialize() {
        // No-op for now - initialization is handled at platform level
    }

    /**
     * Show a notification for an emergency event
     *
     * Delegates to NotificationPresenter for display.
     *
     * @param options Notification configuration including event data
     * @return NotificationResult indicating success or error
     */
    fun showNotification(options: NotificationOptions): NotificationResult {
        return notificationPresenter.showNotification(options)
    }

    /**
     * Remove notification by event ID
     *
     * @param eventId Event ID to remove
     */
    fun removeNotification(eventId: String) {
        notificationPresenter.removeNotification(eventId)
    }

    /**
     * Remove all notifications
     */
    fun clearAllNotifications() {
        notificationPresenter.clearAllNotifications()
    }

    /**
     * Build deep link URL for an event
     *
     * @param eventId Event ID
     * @return Deep link URL
     */
    fun buildDeepLinkUrl(eventId: String): String {
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
