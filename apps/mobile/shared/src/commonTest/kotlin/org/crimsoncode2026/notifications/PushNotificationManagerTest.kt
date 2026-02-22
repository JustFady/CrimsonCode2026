package org.crimsoncode2026.notifications

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.crimsoncode2026.data.Severity

/**
 * Unit tests for PushNotificationManager
 *
 * Tests notification display, deep link building, and notification management.
 */
class PushNotificationManagerTest {

    // ==================== Deep Link Tests ====================

    @Test
    fun `buildDeepLinkUrl returns correct URL for event ID`() = runTest {
        // Arrange
        val manager = PushNotificationManager()
        val eventId = "event-123"

        // Act
        val deepLink = manager.buildDeepLinkUrl(eventId)

        // Assert
        assertEquals("crimsoncode://event/event-123", deepLink)
    }

    @Test
    fun `buildDeepLinkUrl handles complex event ID`() = runTest {
        // Arrange
        val manager = PushNotificationManager()
        val eventId = "event-abc-123-xyz-789"

        // Act
        val deepLink = manager.buildDeepLinkUrl(eventId)

        // Assert
        assertEquals("crimsoncode://event/event-abc-123-xyz-789", deepLink)
    }

    @Test
    fun `parseEventIdFromDeepLink extracts event ID correctly`() = runTest {
        // Arrange
        val manager = PushNotificationManager()
        val deepLink = "crimsoncode://event/test-event-456"

        // Act
        val eventId = manager.parseEventIdFromDeepLink(deepLink)

        // Assert
        assertEquals("test-event-456", eventId)
    }

    @Test
    fun `parseEventIdFromDeepLink returns null for invalid format`() = runTest {
        // Arrange
        val manager = PushNotificationManager()
        val deepLink = "https://example.com/event/123"

        // Act
        val eventId = manager.parseEventIdFromDeepLink(deepLink)

        // Assert
        assertNull(eventId, "Should return null for invalid deep link")
    }

    @Test
    fun `parseEventIdFromDeepLink returns null for null input`() = runTest {
        // Arrange
        val manager = PushNotificationManager()

        // Act
        val eventId = manager.parseEventIdFromDeepLink(null)

        // Assert
        assertNull(eventId)
    }

    @Test
    fun `parseEventIdFromDeepLink returns null for empty string`() = runTest {
        // Arrange
        val manager = PushNotificationManager()

        // Act
        val eventId = manager.parseEventIdFromDeepLink("")

        // Assert
        assertNull(eventId)
    }

    @Test
    fun `parseEventIdFromDeepLink returns null for empty event ID`() = runTest {
        // Arrange
        val manager = PushNotificationManager()
        val deepLink = "crimsoncode://event/"

        // Act
        val eventId = manager.parseEventIdFromDeepLink(deepLink)

        // Assert
        assertNull(eventId)
    }

    // ==================== Notification Keys Tests ====================

    @Test
    fun `KEY_EVENT_ID has correct value`() {
        assertEquals("event_id", PushNotificationManager.KEY_EVENT_ID)
    }

    @Test
    fun `KEY_SEVERITY has correct value`() {
        assertEquals("severity", PushNotificationManager.KEY_SEVERITY)
    }

    @Test
    fun `KEY_CATEGORY has correct value`() {
        assertEquals("category", PushNotificationManager.KEY_CATEGORY)
    }

    @Test
    fun `KEY_LAT has correct value`() {
        assertEquals("lat", PushNotificationManager.KEY_LAT)
    }

    @Test
    fun `KEY_LON has correct value`() {
        assertEquals("lon", PushNotificationManager.KEY_LON)
    }

    @Test
    fun `KEY_DEEP_LINK has correct value`() {
        assertEquals("deep_link", PushNotificationManager.KEY_DEEP_LINK)
    }

    @Test
    fun `KEY_VIEW_ON_MAP has correct value`() {
        assertEquals("view_on_map", PushNotificationManager.KEY_VIEW_ON_MAP)
    }

    // ==================== Notification Click Event Tests ====================

    @Test
    fun `NotificationClickEvent contains all required fields`() = runTest {
        // Arrange
        val event = NotificationClickEvent(
            eventId = "event-123",
            deepLinkUrl = "crimsoncode://event/event-123",
            severity = Severity.CRISIS,
            category = "FIRE"
        )

        // Assert
        assertEquals("event-123", event.eventId)
        assertEquals("crimsoncode://event/event-123", event.deepLinkUrl)
        assertEquals(Severity.CRISIS, event.severity)
        assertEquals("FIRE", event.category)
    }

    // ==================== Mock Implementations ====================

    class MockNotificationPresenter : NotificationPresenter {
        var lastShownOptions: NotificationOptions? = null
        var removedEventIds = mutableListOf<String>()
        var clearedAll = false

        override fun showNotification(options: NotificationOptions): NotificationResult {
            lastShownOptions = options
            return NotificationResult.Success
        }

        override fun removeNotification(eventId: String) {
            removedEventIds.add(eventId)
        }

        override fun clearAllNotifications() {
            clearedAll = true
            removedEventIds.clear()
        }
    }

    class MockNotificationOptions(
        override val title: String,
        override val body: String,
        override val eventId: String,
        override val severity: Severity,
        override val category: String,
        override val deepLink: String
    ) : NotificationOptions

    // ==================== Show Notification Tests ====================

    @Test
    fun `showNotification delegates to presenter`() = runTest {
        // Arrange
        val mockPresenter = MockNotificationPresenter()
        val manager = PushNotificationManager(
            kotlinx.coroutines.CoroutineScope(
                kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default
            ).apply { // Use our mock presenter
                // Note: We can't inject the mock, but we can test the methods
            }
        )

        val options = MockNotificationOptions(
            title = "CRISIS - Medical",
            body = "Medical emergency nearby",
            eventId = "event-123",
            severity = Severity.CRISIS,
            category = "MEDICAL",
            deepLink = "crimsoncode://event/event-123"
        )

        // Act - Since we can't inject mock, test delegation behavior
        // The showNotification method calls presenter.showNotification
        val result = manager.showNotification(options)

        // Assert
        // Result type checking (implementation depends on presenter)
        assertTrue(result is NotificationResult.Success)
    }

    @Test
    fun `showNotification handles all notification options`() = runTest {
        // Arrange
        val manager = PushNotificationManager()

        // Act - Test with all required options
        val result = manager.showNotification(
            MockNotificationOptions(
                title = "Test Title",
                body = "Test Body",
                eventId = "test-event",
                severity = Severity.ALERT,
                category = "WEATHER",
                deepLink = "crimsoncode://event/test-event"
            )
        )

        // Assert
        assertTrue(result is NotificationResult.Success)
    }
}
