package org.crimsoncode2026.notifications

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.crimsoncode2026.data.Category
import org.crimsoncode2026.data.Event
import org.crimsoncode2026.data.Severity

/**
 * Unit tests for PushNotificationPayload
 *
 * Tests payload construction, deep link building, and event ID parsing.
 */
class PushNotificationPayloadTest {

    // ==================== Deep Link Constants Tests ====================

    @Test
    fun `DEEP_LINK_BASE has correct value`() {
        assertEquals("crimsoncode://event/", PushNotificationPayload.DEEP_LINK_BASE)
    }

    // ==================== FromEvent Factory Tests ====================

    @Test
    fun `fromEvent creates payload with all fields`() = runTest {
        // Arrange
        val event = Event(
            id = "event-123",
            creatorId = "user-456",
            severity = Severity.CRISIS.value,
            category = Category.FIRE.value,
            lat = 37.7749,
            lon = -122.4194,
            locationOverride = "Manual location",
            broadcastType = "PUBLIC",
            description = "Test fire emergency",
            isAnonymous = true,
            createdAt = 1234567890L,
            expiresAt = 1234567890L + 172800000L,
            deletedAt = null
        )

        // Act
        val payload = PushNotificationPayload.fromEvent(event)

        // Assert
        assertEquals("event-123", payload.eventId)
        assertEquals(Severity.CRISIS.value, payload.severity)
        assertEquals(Category.FIRE.value, payload.category)
        assertEquals("Test fire emergency", payload.description)
        assertEquals(37.7749, payload.lat)
        assertEquals(-122.4194, payload.lon)
        assertEquals("Manual location", payload.locationOverride)
    }

    @Test
    fun `fromEvent creates deep link with correct base`() = runTest {
        // Arrange
        val event = Event(
            id = "event-xyz",
            creatorId = "user-123",
            severity = Severity.ALERT.value,
            category = Category.MEDICAL.value,
            lat = 40.0,
            lon = -74.0,
            description = "Medical alert",
            isAnonymous = true,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 172800000L,
            deletedAt = null,
            locationOverride = null,
            broadcastType = "PUBLIC"
        )

        // Act
        val payload = PushNotificationPayload.fromEvent(event)

        // Assert
        assertEquals("crimsoncode://event/event-xyz", payload.deepLink)
    }

    @Test
    fun `fromEvent handles null location override`() = runTest {
        // Arrange
        val event = Event(
            id = "event-null-loc",
            creatorId = "user-123",
            severity = Severity.ALERT.value,
            category = Category.WEATHER.value,
            lat = 35.0,
            lon = -120.0,
            locationOverride = null,
            broadcastType = "PUBLIC",
            description = "Weather alert",
            isAnonymous = true,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 172800000L,
            deletedAt = null
        )

        // Act
        val payload = PushNotificationPayload.fromEvent(event)

        // Assert
        assertNull(payload.locationOverride, "Null location override should remain null")
    }

    @Test
    fun `fromEvent includes coordinates`() = runTest {
        // Arrange
        val event = Event(
            id = "event-with-coords",
            creatorId = "user-123",
            severity = Severity.CRISIS.value,
            category = Category.TRAFFIC.value,
            lat = 37.5,
            lon = -122.0,
            description = "Traffic accident",
            isAnonymous = true,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 172800000L,
            deletedAt = null,
            locationOverride = null,
            broadcastType = "PUBLIC"
        )

        // Act
        val payload = PushNotificationPayload.fromEvent(event)

        // Assert
        assertEquals(37.5, payload.lat, 0.001)
        assertEquals(-122.0, payload.lon, 0.001)
    }

    // ==================== createDeepLink Tests ====================

    @Test
    fun `createDeepLink creates correct deep link for event ID`() = runTest {
        // Arrange
        val eventId = "test-event-abc"

        // Act
        val deepLink = PushNotificationPayload.createDeepLink(eventId)

        // Assert
        assertEquals("crimsoncode://event/test-event-abc", deepLink)
    }

    @Test
    fun `createDeepLink handles complex event ID`() = runTest {
        // Arrange
        val eventId = "event-123-456-789"

        // Act
        val deepLink = PushNotificationPayload.createDeepLink(eventId)

        // Assert
        assertEquals("crimsoncode://event/event-123-456-789", deepLink)
    }

    // ==================== parseEventIdFromDeepLink Tests ====================

    @Test
    fun `parseEventIdFromDeepLink extracts event ID correctly`() = runTest {
        // Arrange
        val deepLink = "crimsoncode://event/test-event-456"

        // Act
        val eventId = PushNotificationPayload.parseEventIdFromDeepLink(deepLink)

        // Assert
        assertEquals("test-event-456", eventId)
    }

    @Test
    fun `parseEventIdFromDeepLink returns null for invalid scheme`() = runTest {
        // Arrange
        val deepLink = "https://example.com/event/123"

        // Act
        val eventId = PushNotificationPayload.parseEventIdFromDeepLink(deepLink)

        // Assert
        assertNull(eventId, "Should return null for invalid scheme")
    }

    @Test
    fun `parseEventIdFromDeepLink returns null for missing event segment`() = runTest {
        // Arrange
        val deepLink = "crimsoncode://event/"

        // Act
        val eventId = PushNotificationPayload.parseEventIdFromDeepLink(deepLink)

        // Assert
        assertNull(eventId)
    }

    @Test
    fun `parseEventIdFromDeepLink returns null for null input`() = runTest {
        // Arrange
        val deepLink: String? = null

        // Act
        val eventId = PushNotificationPayload.parseEventIdFromDeepLink(deepLink)

        // Assert
        assertNull(eventId)
    }

    @Test
    fun `parseEventIdFromDeepLink returns null for empty string`() = runTest {
        // Arrange
        val deepLink = ""

        // Act
        val eventId = PushNotificationPayload.parseEventIdFromDeepLink(deepLink)

        // Assert
        assertNull(eventId)
    }

    // ==================== Extension Property Tests ====================

    @Test
    fun `severityEnum returns CRISIS for CRISIS string`() = runTest {
        // Arrange
        val payload = PushNotificationPayload(
            eventId = "test",
            severity = Severity.CRISIS.value,
            category = Category.FIRE.value,
            description = "Test",
            deepLink = "crimsoncode://event/test"
        )

        // Assert
        assertEquals(Severity.CRISIS, payload.severityEnum)
    }

    @Test
    fun `severityEnum returns ALERT for ALERT string`() = runTest {
        // Arrange
        val payload = PushNotificationPayload(
            eventId = "test",
            severity = Severity.ALERT.value,
            category = Category.MEDICAL.value,
            description = "Test",
            deepLink = "crimsoncode://event/test"
        )

        // Assert
        assertEquals(Severity.ALERT, payload.severityEnum)
    }

    @Test
    fun `severityEnum returns null for invalid severity`() = runTest {
        // Arrange
        val payload = PushNotificationPayload(
            eventId = "test",
            severity = "INVALID",
            category = Category.OTHER.value,
            description = "Test",
            deepLink = "crimsoncode://event/test"
        )

        // Assert
        assertNull(payload.severityEnum, "Should return null for invalid severity")
    }

    @Test
    fun `categoryEnum returns correct category`() = runTest {
        // Arrange
        val payload = PushNotificationPayload(
            eventId = "test",
            severity = Severity.ALERT.value,
            category = Category.WEATHER.value,
            description = "Test",
            deepLink = "crimsoncode://event/test"
        )

        // Assert
        assertEquals(Category.WEATHER, payload.categoryEnum)
    }

    @Test
    fun `categoryEnum returns null for invalid category`() = runTest {
        // Arrange
        val payload = PushNotificationPayload(
            eventId = "test",
            severity = Severity.ALERT.value,
            category = "INVALID_CATEGORY",
            description = "Test",
            deepLink = "crimsoncode://event/test"
        )

        // Assert
        assertNull(payload.categoryEnum, "Should return null for invalid category")
    }

    // ==================== isCrisis Property Tests ====================

    @Test
    fun `isCrisis returns true for CRISIS severity`() = runTest {
        // Arrange
        val payload = PushNotificationPayload(
            eventId = "test",
            severity = Severity.CRISIS.value,
            category = Category.FIRE.value,
            description = "Test",
            deepLink = "crimsoncode://event/test"
        )

        // Assert
        assertTrue(payload.isCrisis, "CRISIS should be crisis")
    }

    @Test
    fun `isCrisis returns false for ALERT severity`() = runTest {
        // Arrange
        val payload = PushNotificationPayload(
            eventId = "test",
            severity = Severity.ALERT.value,
            category = Category.MEDICAL.value,
            description = "Test",
            deepLink = "crimsoncode://event/test"
        )

        // Assert
        assertFalse(payload.isCrisis, "ALERT should not be crisis")
    }

    @Test
    fun `isCrisis returns false for null severity`() = runTest {
        // Arrange
        val payload = PushNotificationPayload(
            eventId = "test",
            severity = "INVALID",
            category = Category.OTHER.value,
            description = "Test",
            deepLink = "crimsoncode://event/test"
        )

        // Assert
        assertFalse(payload.isCrisis, "Invalid severity should not be crisis")
    }

    // ==================== notificationTitle Property Tests ====================

    @Test
    fun `notificationTitle formats CRISIS with category`() = runTest {
        // Arrange
        val payload = PushNotificationPayload(
            eventId = "test",
            severity = Severity.CRISIS.value,
            category = Category.MEDICAL.value,
            description = "Medical emergency",
            deepLink = "crimsoncode://event/test"
        )

        // Act
        val title = payload.notificationTitle

        // Assert
        assertEquals("CRISIS - Medical", title)
    }

    @Test
    fun `notificationTitle formats ALERT with category`() = runTest {
        // Arrange
        val payload = PushNotificationPayload(
            eventId = "test",
            severity = Severity.ALERT.value,
            category = Category.FIRE.value,
            description = "Fire in building",
            deepLink = "crimsoncode://event/test"
        )

        // Act
        val title = payload.notificationTitle

        // Assert
        assertEquals("ALERT - Fire", title)
    }

    @Test
    fun `notificationTitle handles invalid severity`() = runTest {
        // Arrange
        val payload = PushNotificationPayload(
            eventId = "test",
            severity = "INVALID",
            category = Category.OTHER.value,
            description = "Test alert",
            deepLink = "crimsoncode://event/test"
        )

        // Act
        val title = payload.notificationTitle

        // Assert
        // Should still format with uppercased severity string
        assertTrue(title.contains("INVALID"), "Should include severity in title")
        assertTrue(title.contains("- Other"), "Should include category in title")
    }

    @Test
    fun `notificationTitle handles null severity enum`() = runTest {
        // Arrange
        val payload = PushNotificationPayload(
            eventId = "test",
            severity = "INVALID",
            category = Category.OTHER.value,
            description = "Test alert",
            deepLink = "crimsoncode://event/test"
        )

        // Act
        val title = payload.notificationTitle

        // Assert
        // Should use category display name if enum is null
        assertTrue(title.contains("Other"), "Should include category display name")
    }
}
