package org.crimsoncode2026.domain.usecases

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.crimsoncode2026.data.BroadcastType
import org.crimsoncode2026.data.Category
import org.crimsoncode2026.data.DeliveryStatus
import org.crimsoncode2026.data.Event
import org.crimsoncode2026.data.EventRecipient
import org.crimsoncode2026.data.Severity

/**
 * Unit tests for CreateEventUseCase
 *
 * Tests unified event creation with validation for both public and private events.
 */
class CreateEventUseCaseTest {

    // ==================== Mock Implementations ====================

    class MockCreatePublicEventUseCase : CreatePublicEventUseCase {
        var shouldSucceed = true
        var errorMessage: String? = null
        var createdEvent: Event? = null

        suspend operator fun invoke(
            severity: Severity,
            category: Category,
            lat: Double,
            lon: Double,
            locationOverride: String?,
            description: String
        ): CreatePublicEventResult {
            return if (shouldSucceed) {
                val event = Event(
                    id = "test-event-id",
                    creatorId = "test-user-id",
                    severity = severity.value,
                    category = category.value,
                    lat = lat,
                    lon = lon,
                    locationOverride = locationOverride,
                    broadcastType = BroadcastType.PUBLIC.value,
                    description = description,
                    isAnonymous = true,
                    createdAt = System.currentTimeMillis(),
                    expiresAt = Event.calculateExpiration()
                )
                createdEvent = event
                CreatePublicEventResult.Success(event)
            } else {
                CreatePublicEventResult.Error(errorMessage ?: "Failed to create event")
            }
        }

        suspend operator fun invoke(event: Event): CreatePublicEventResult {
            return if (shouldSucceed) {
                createdEvent = event
                CreatePublicEventResult.Success(event)
            } else {
                CreatePublicEventResult.Error(errorMessage ?: "Failed to create event")
            }
        }
    }

    class MockCreatePrivateEventWithRecipientsUseCase : CreatePrivateEventWithRecipientsUseCase {
        var shouldSucceed = true
        var errorMessage: String? = null
        var recipients = listOf<EventRecipient>()

        suspend operator fun invoke(
            event: Event,
            selectedContactIds: List<String>
        ): CreatePrivateEventWithRecipientsResult {
            return if (shouldSucceed) {
                CreatePrivateEventWithRecipientsResult.Success(
                    event = event,
                    recipients = recipients
                )
            } else {
                CreatePrivateEventWithRecipientsResult.Error(errorMessage ?: "Failed to create event")
            }
        }
    }

    // ==================== Public Event Tests ====================

    @Test
    fun `create public event succeeds with valid params`() = runTest {
        // Arrange
        val mockPublicUseCase = MockCreatePublicEventUseCase().apply { shouldSucceed = true }
        val mockPrivateUseCase = MockCreatePrivateEventWithRecipientsUseCase()
        val useCase = CreateEventUseCase(mockPublicUseCase, mockPrivateUseCase)

        val params = CreateEventParams(
            severity = Severity.ALERT,
            category = Category.MEDICAL,
            lat = 37.7749,
            lon = -122.4194,
            description = "Test medical emergency",
            isPublic = true
        )

        // Act
        val result = useCase(params)

        // Assert
        assertTrue(result is CreateEventResult.Success, "Public event creation should succeed")
        val successResult = result as CreateEventResult.Success
        assertTrue(successResult.recipients.isEmpty(), "Public event should have no recipients")
        assertNotNull(successResult.event)
        assertEquals(BroadcastType.PUBLIC.value, successResult.event.broadcastType)
        assertTrue(successResult.event.isAnonymous, "Public events should be anonymous")
    }

    // ==================== Private Event Tests ====================

    @Test
    fun `create private event succeeds with valid params and contacts`() = runTest {
        // Arrange
        val mockPublicUseCase = MockCreatePublicEventUseCase().apply { shouldSucceed = true }
        val mockPrivateUseCase = MockCreatePrivateEventWithRecipientsUseCase().apply {
            shouldSucceed = true
            recipients = listOf(
                EventRecipient("event-id", "user-1", DeliveryStatus.PENDING.value)
            )
        }
        val useCase = CreateEventUseCase(mockPublicUseCase, mockPrivateUseCase)

        val params = CreateEventParams(
            severity = Severity.CRISIS,
            category = Category.FIRE,
            lat = 37.7749,
            lon = -122.4194,
            description = "Test fire emergency",
            isPublic = false,
            selectedContactIds = listOf("contact-1", "contact-2")
        )

        // Act
        val result = useCase(params)

        // Assert
        assertTrue(result is CreateEventResult.Success, "Private event creation should succeed")
        val successResult = result as CreateEventResult.Success
        assertEquals(1, successResult.recipients.size, "Private event should have 1 recipient")
        assertEquals(BroadcastType.PRIVATE.value, successResult.event.broadcastType)
        assertFalse(successResult.event.isAnonymous, "Private events should not be anonymous")
    }

    // ==================== Validation Tests ====================

    @Test
    fun `create event fails when latitude is too low`() = runTest {
        // Arrange
        val mockPublicUseCase = MockCreatePublicEventUseCase()
        val mockPrivateUseCase = MockCreatePrivateEventWithRecipientsUseCase()
        val useCase = CreateEventUseCase(mockPublicUseCase, mockPrivateUseCase)

        val params = CreateEventParams(
            severity = Severity.ALERT,
            category = Category.MEDICAL,
            lat = -91.0, // Invalid: below -90
            lon = -122.4194,
            description = "Test",
            isPublic = true
        )

        // Act
        val result = useCase(params)

        // Assert
        assertTrue(result is CreateEventResult.Error, "Should fail with invalid latitude")
        val errorResult = result as CreateEventResult.Error
        assertTrue(errorResult.message.contains("latitude"), "Error should mention latitude")
    }

    @Test
    fun `create event fails when latitude is too high`() = runTest {
        // Arrange
        val mockPublicUseCase = MockCreatePublicEventUseCase()
        val mockPrivateUseCase = MockCreatePrivateEventWithRecipientsUseCase()
        val useCase = CreateEventUseCase(mockPublicUseCase, mockPrivateUseCase)

        val params = CreateEventParams(
            severity = Severity.ALERT,
            category = Category.MEDICAL,
            lat = 90.1, // Invalid: above 90
            lon = -122.4194,
            description = "Test",
            isPublic = true
        )

        // Act
        val result = useCase(params)

        // Assert
        assertTrue(result is CreateEventResult.Error, "Should fail with invalid latitude")
    }

    @Test
    fun `create event fails when longitude is too low`() = runTest {
        // Arrange
        val mockPublicUseCase = MockCreatePublicEventUseCase()
        val mockPrivateUseCase = MockCreatePrivateEventWithRecipientsUseCase()
        val useCase = CreateEventUseCase(mockPublicUseCase, mockPrivateUseCase)

        val params = CreateEventParams(
            severity = Severity.ALERT,
            category = Category.MEDICAL,
            lat = 37.7749,
            lon = -180.1, // Invalid: below -180
            description = "Test",
            isPublic = true
        )

        // Act
        val result = useCase(params)

        // Assert
        assertTrue(result is CreateEventResult.Error, "Should fail with invalid longitude")
    }

    @Test
    fun `create event fails when longitude is too high`() = runTest {
        // Arrange
        val mockPublicUseCase = MockCreatePublicEventUseCase()
        val mockPrivateUseCase = MockCreatePrivateEventWithRecipientsUseCase()
        val useCase = CreateEventUseCase(mockPublicUseCase, mockPrivateUseCase)

        val params = CreateEventParams(
            severity = Severity.ALERT,
            category = Category.MEDICAL,
            lat = 37.7749,
            lon = 180.1, // Invalid: above 180
            description = "Test",
            isPublic = true
        )

        // Act
        val result = useCase(params)

        // Assert
        assertTrue(result is CreateEventResult.Error, "Should fail with invalid longitude")
    }

    @Test
    fun `create event fails when description is empty`() = runTest {
        // Arrange
        val mockPublicUseCase = MockCreatePublicEventUseCase()
        val mockPrivateUseCase = MockCreatePrivateEventWithRecipientsUseCase()
        val useCase = CreateEventUseCase(mockPublicUseCase, mockPrivateUseCase)

        val params = CreateEventParams(
            severity = Severity.ALERT,
            category = Category.MEDICAL,
            lat = 37.7749,
            lon = -122.4194,
            description = "", // Invalid: empty
            isPublic = true
        )

        // Act
        val result = useCase(params)

        // Assert
        assertTrue(result is CreateEventResult.Error, "Should fail with empty description")
        val errorResult = result as CreateEventResult.Error
        assertTrue(errorResult.message.contains("Description"), "Error should mention description")
    }

    @Test
    fun `create event fails when description is too long`() = runTest {
        // Arrange
        val mockPublicUseCase = MockCreatePublicEventUseCase()
        val mockPrivateUseCase = MockCreatePrivateEventWithRecipientsUseCase()
        val useCase = CreateEventUseCase(mockPublicUseCase, mockPrivateUseCase)

        val params = CreateEventParams(
            severity = Severity.ALERT,
            category = Category.MEDICAL,
            lat = 37.7749,
            lon = -122.4194,
            description = "A".repeat(501), // Invalid: 501 chars
            isPublic = true
        )

        // Act
        val result = useCase(params)

        // Assert
        assertTrue(result is CreateEventResult.Error, "Should fail with too long description")
        val errorResult = result as CreateEventResult.Error
        assertTrue(errorResult.message.contains("500"), "Error should mention 500 character limit")
    }

    @Test
    fun `create event accepts description at max length`() = runTest {
        // Arrange
        val mockPublicUseCase = MockCreatePublicEventUseCase().apply { shouldSucceed = true }
        val mockPrivateUseCase = MockCreatePrivateEventWithRecipientsUseCase()
        val useCase = CreateEventUseCase(mockPublicUseCase, mockPrivateUseCase)

        val params = CreateEventParams(
            severity = Severity.ALERT,
            category = Category.MEDICAL,
            lat = 37.7749,
            lon = -122.4194,
            description = "A".repeat(500), // Valid: exactly 500 chars
            isPublic = true
        )

        // Act
        val result = useCase(params)

        // Assert
        assertTrue(result is CreateEventResult.Success, "Should accept description at max length")
    }

    @Test
    fun `create private event fails when no contacts selected`() = runTest {
        // Arrange
        val mockPublicUseCase = MockCreatePublicEventUseCase()
        val mockPrivateUseCase = MockCreatePrivateEventWithRecipientsUseCase()
        val useCase = CreateEventUseCase(mockPublicUseCase, mockPrivateUseCase)

        val params = CreateEventParams(
            severity = Severity.ALERT,
            category = Category.MEDICAL,
            lat = 37.7749,
            lon = -122.4194,
            description = "Test",
            isPublic = false,
            selectedContactIds = emptyList() // Invalid: private event needs contacts
        )

        // Act
        val result = useCase(params)

        // Assert
        assertTrue(result is CreateEventResult.Error, "Should fail when no contacts selected for private event")
        val errorResult = result as CreateEventResult.Error
        assertTrue(errorResult.message.contains("contact"), "Error should mention contacts")
    }

    // ==================== Delegate Error Tests ====================

    @Test
    fun `create public event fails when public use case returns error`() = runTest {
        // Arrange
        val mockPublicUseCase = MockCreatePublicEventUseCase().apply {
            shouldSucceed = false
            errorMessage = "Public event creation failed"
        }
        val mockPrivateUseCase = MockCreatePrivateEventWithRecipientsUseCase()
        val useCase = CreateEventUseCase(mockPublicUseCase, mockPrivateUseCase)

        val params = CreateEventParams(
            severity = Severity.ALERT,
            category = Category.MEDICAL,
            lat = 37.7749,
            lon = -122.4194,
            description = "Test",
            isPublic = true
        )

        // Act
        val result = useCase(params)

        // Assert
        assertTrue(result is CreateEventResult.Error, "Should fail when public use case fails")
        val errorResult = result as CreateEventResult.Error
        assertEquals("Public event creation failed", errorResult.message)
    }

    @Test
    fun `create private event fails when private use case returns error`() = runTest {
        // Arrange
        val mockPublicUseCase = MockCreatePublicEventUseCase()
        val mockPrivateUseCase = MockCreatePrivateEventWithRecipientsUseCase().apply {
            shouldSucceed = false
            errorMessage = "Private event creation failed"
        }
        val useCase = CreateEventUseCase(mockPublicUseCase, mockPrivateUseCase)

        val params = CreateEventParams(
            severity = Severity.CRISIS,
            category = Category.FIRE,
            lat = 37.7749,
            lon = -122.4194,
            description = "Test",
            isPublic = false,
            selectedContactIds = listOf("contact-1")
        )

        // Act
        val result = useCase(params)

        // Assert
        assertTrue(result is CreateEventResult.Error, "Should fail when private use case fails")
        val errorResult = result as CreateEventResult.Error
        assertEquals("Private event creation failed", errorResult.message)
    }

    // ==================== Event Object Tests ====================

    @Test
    fun `created public event has correct properties`() = runTest {
        // Arrange
        val mockPublicUseCase = MockCreatePublicEventUseCase().apply { shouldSucceed = true }
        val mockPrivateUseCase = MockCreatePrivateEventWithRecipientsUseCase()
        val useCase = CreateEventUseCase(mockPublicUseCase, mockPrivateUseCase)

        val params = CreateEventParams(
            severity = Severity.CRISIS,
            category = Category.NATURAL_DISASTER,
            lat = 37.7749,
            lon = -122.4194,
            locationOverride = "Manual location",
            description = "Test event",
            isPublic = true
        )

        // Act
        val result = useCase(params)
        val event = (result as CreateEventResult.Success).event

        // Assert
        assertEquals(Severity.CRISIS.value, event.severity)
        assertEquals(Category.NATURAL_DISASTER.value, event.category)
        assertEquals(37.7749, event.lat, 0.001)
        assertEquals(-122.4194, event.lon, 0.001)
        assertEquals("Manual location", event.locationOverride)
        assertEquals("Test event", event.description)
        assertEquals(BroadcastType.PUBLIC.value, event.broadcastType)
        assertTrue(event.isAnonymous, "Public event should be anonymous")
        assertTrue(event.expiresAt > System.currentTimeMillis(), "Should have future expiration")
    }

    @Test
    fun `created private event has correct properties`() = runTest {
        // Arrange
        val mockPublicUseCase = MockCreatePublicEventUseCase()
        val mockPrivateUseCase = MockCreatePrivateEventWithRecipientsUseCase().apply {
            shouldSucceed = true
            recipients = listOf(EventRecipient("event-id", "user-1", DeliveryStatus.PENDING.value))
        }
        val useCase = CreateEventUseCase(mockPublicUseCase, mockPrivateUseCase)

        val params = CreateEventParams(
            severity = Severity.ALERT,
            category = Category.MEDICAL,
            lat = 37.7749,
            lon = -122.4194,
            description = "Test private event",
            isPublic = false,
            selectedContactIds = listOf("contact-1")
        )

        // Act
        val result = useCase(params)
        val event = (result as CreateEventResult.Success).event

        // Assert
        assertEquals(Severity.ALERT.value, event.severity)
        assertEquals(Category.MEDICAL.value, event.category)
        assertEquals(37.7749, event.lat, 0.001)
        assertEquals(-122.4194, event.lon, 0.001)
        assertEquals(BroadcastType.PRIVATE.value, event.broadcastType)
        assertFalse(event.isAnonymous, "Private event should not be anonymous")
        assertEquals("Test private event", event.description)
    }

    // ==================== Convenience Method Tests ====================

    @Test
    fun `createPublic method sets isPublic to true`() = runTest {
        // Arrange
        val mockPublicUseCase = MockCreatePublicEventUseCase().apply { shouldSucceed = true }
        val mockPrivateUseCase = MockCreatePrivateEventWithRecipientsUseCase()
        val useCase = CreateEventUseCase(mockPublicUseCase, mockPrivateUseCase)

        val params = CreateEventParams(
            severity = Severity.ALERT,
            category = Category.MEDICAL,
            lat = 37.7749,
            lon = -122.4194,
            description = "Test",
            isPublic = false // Will be overridden by createPublic
        )

        // Act
        val result = useCase.createPublic(params)

        // Assert
        val event = (result as CreateEventResult.Success).event
        assertEquals(BroadcastType.PUBLIC.value, event.broadcastType)
    }

    @Test
    fun `createPrivate method sets isPublic to false`() = runTest {
        // Arrange
        val mockPublicUseCase = MockCreatePublicEventUseCase()
        val mockPrivateUseCase = MockCreatePrivateEventWithRecipientsUseCase().apply {
            shouldSucceed = true
            recipients = listOf(EventRecipient("event-id", "user-1", DeliveryStatus.PENDING.value))
        }
        val useCase = CreateEventUseCase(mockPublicUseCase, mockPrivateUseCase)

        val params = CreateEventParams(
            severity = Severity.CRISIS,
            category = Category.FIRE,
            lat = 37.7749,
            lon = -122.4194,
            description = "Test",
            selectedContactIds = listOf("contact-1"),
            isPublic = true // Will be overridden by createPrivate
        )

        // Act
        val result = useCase.createPrivate(params)

        // Assert
        val event = (result as CreateEventResult.Success).event
        assertEquals(BroadcastType.PRIVATE.value, event.broadcastType)
        assertFalse(event.isAnonymous, "Private event should not be anonymous")
    }
}
