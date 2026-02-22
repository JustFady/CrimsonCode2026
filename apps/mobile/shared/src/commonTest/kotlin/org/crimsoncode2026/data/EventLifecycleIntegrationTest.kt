package org.crimsoncode2026.data

import kotlinx.coroutines.test.runTest
import org.crimsoncode2026.data.BroadcastType
import org.crimsoncode2026.data.Category
import org.crimsoncode2026.data.DeliveryStatus
import org.crimsoncode2026.data.Event
import org.crimsoncode2026.data.EventRecipient
import org.crimsoncode2026.data.Severity
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for Event Lifecycle
 *
 * Tests the complete event lifecycle from creation through expiration:
 * 1. Event creation (public/private)
 * 2. Event validation (lat/lon bounds, description length)
 * 3. Event expiration (48-hour auto-expire)
 * 4. Event soft delete (deleted_at timestamp)
 * 5. Event recipient tracking (private events only)
 * 6. Event queries (by bounds, radius, creator)
 * 7. Event delivery status tracking (PENDING -> SENT/FAILED)
 */
class EventLifecycleIntegrationTest {

    private lateinit var mockEventRepository: MockEventRepository
    private lateinit var mockEventRecipientRepository: MockEventRecipientRepository

    @BeforeTest
    fun setup() {
        mockEventRepository = MockEventRepository()
        mockEventRecipientRepository = MockEventRecipientRepository()
    }

    @AfterTest
    fun teardown() {
        mockEventRepository.clear()
        mockEventRecipientRepository.clear()
    }

    // ==================== Event Creation Tests ====================

    @Test
    fun `create public event successfully`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val event = Event(
            id = "",
            creatorId = "user-123",
            severity = Severity.ALERT.value,
            category = Category.MEDICAL.value,
            lat = 40.7128,
            lon = -74.0060,
            description = "Medical emergency at location",
            broadcastType = BroadcastType.PUBLIC.value,
            isAnonymous = true,
            createdAt = now,
            expiresAt = now + Event.DEFAULT_EXPIRATION_MS
        )

        // Act
        val result = mockEventRepository.createEvent(event)

        // Assert
        assertTrue(result.isSuccess, "Event creation should succeed")
        val createdEvent = result.getOrNull()!!
        assertTrue(createdEvent.id.isNotEmpty(), "Event should have generated ID")
        assertEquals("user-123", createdEvent.creatorId, "Creator ID should match")
        assertEquals(Severity.ALERT.value, createdEvent.severity, "Severity should match")
        assertEquals(Category.MEDICAL.value, createdEvent.category, "Category should match")
        assertEquals(BroadcastType.PUBLIC.value, createdEvent.broadcastType, "Broadcast type should be PUBLIC")
        assertTrue(createdEvent.isPublic, "Event should be public")
        assertTrue(createdEvent.isAnonymous, "Public event should be anonymous")
    }

    @Test
    fun `create private event successfully`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val event = Event(
            id = "",
            creatorId = "user-123",
            severity = Severity.CRISIS.value,
            category = Category.FIRE.value,
            lat = 40.7128,
            lon = -74.0060,
            description = "Fire at location",
            broadcastType = BroadcastType.PRIVATE.value,
            isAnonymous = false,
            createdAt = now,
            expiresAt = now + Event.DEFAULT_EXPIRATION_MS
        )

        // Act
        val result = mockEventRepository.createEvent(event)

        // Assert
        assertTrue(result.isSuccess, "Event creation should succeed")
        val createdEvent = result.getOrNull()!!
        assertEquals(BroadcastType.PRIVATE.value, createdEvent.broadcastType, "Broadcast type should be PRIVATE")
        assertTrue(createdEvent.isPrivate, "Event should be private")
        assertFalse(createdEvent.isAnonymous, "Private event should NOT be anonymous")
    }

    @Test
    fun `create event with all categories`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val categories = Category.values()

        // Act & Assert
        categories.forEach { category ->
            val event = Event(
                id = "",
                creatorId = "user-123",
                severity = Severity.ALERT.value,
                category = category.value,
                lat = 40.7128,
                lon = -74.0060,
                description = "Test event for ${category.displayName}",
                broadcastType = BroadcastType.PUBLIC.value,
                isAnonymous = true,
                createdAt = now,
                expiresAt = now + Event.DEFAULT_EXPIRATION_MS
            )
            val result = mockEventRepository.createEvent(event)
            assertTrue(result.isSuccess, "Event with ${category.displayName} should be created")
        }
    }

    @Test
    fun `create event with all severities`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()

        // Act & Assert - Test ALERT
        val alertEvent = Event(
            id = "", creatorId = "user-123",
            severity = Severity.ALERT.value,
            category = Category.OTHER.value,
            lat = 40.7128, lon = -74.0060,
            description = "Alert level event",
            broadcastType = BroadcastType.PUBLIC.value,
            isAnonymous = true,
            createdAt = now,
            expiresAt = now + Event.DEFAULT_EXPIRATION_MS
        )
        val alertResult = mockEventRepository.createEvent(alertEvent)
        assertTrue(alertResult.isSuccess, "ALERT severity event should be created")

        // Act & Assert - Test CRISIS
        val crisisEvent = Event(
            id = "", creatorId = "user-456",
            severity = Severity.CRISIS.value,
            category = Category.OTHER.value,
            lat = 40.7128, lon = -74.0060,
            description = "Crisis level event",
            broadcastType = BroadcastType.PUBLIC.value,
            isAnonymous = true,
            createdAt = now,
            expiresAt = now + Event.DEFAULT_EXPIRATION_MS
        )
        val crisisResult = mockEventRepository.createEvent(crisisEvent)
        assertTrue(crisisResult.isSuccess, "CRISIS severity event should be created")
    }

    @Test
    fun `create event with location override`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val locationOverride = "Near Central Park entrance"
        val event = Event(
            id = "",
            creatorId = "user-123",
            severity = Severity.ALERT.value,
            category = Category.TRAFFIC.value,
            lat = 40.7829,
            lon = -73.9654,
            locationOverride = locationOverride,
            description = "Traffic accident",
            broadcastType = BroadcastType.PUBLIC.value,
            isAnonymous = true,
            createdAt = now,
            expiresAt = now + Event.DEFAULT_EXPIRATION_MS
        )

        // Act
        val result = mockEventRepository.createEvent(event)

        // Assert
        assertTrue(result.isSuccess, "Event with location override should be created")
        val createdEvent = result.getOrNull()!!
        assertEquals(locationOverride, createdEvent.locationOverride, "Location override should be saved")
    }

    // ==================== Event Expiration Tests ====================

    @Test
    fun `event expiration calculates correctly`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val expectedExpiration = now + Event.DEFAULT_EXPIRATION_MS

        // Act
        val actualExpiration = Event.calculateExpiration(now)

        // Assert
        assertEquals(expectedExpiration, actualExpiration, "Expiration should be 48 hours from creation")
    }

    @Test
    fun `active event is not expired`() = runTest {
        // Arrange
        val futureTime = System.currentTimeMillis() + 1000000
        val event = Event(
            id = "event-123",
            creatorId = "user-123",
            severity = Severity.ALERT.value,
            category = Category.MEDICAL.value,
            lat = 40.7128,
            lon = -74.0060,
            description = "Active event",
            broadcastType = BroadcastType.PUBLIC.value,
            isAnonymous = true,
            createdAt = System.currentTimeMillis(),
            expiresAt = futureTime
        )

        // Act & Assert
        assertFalse(event.isExpired, "Event with future expiration should not be expired")
    }

    @Test
    fun `expired event is marked as expired`() = runTest {
        // Arrange
        val pastTime = System.currentTimeMillis() - 1000000
        val event = Event(
            id = "event-456",
            creatorId = "user-123",
            severity = Severity.ALERT.value,
            category = Category.MEDICAL.value,
            lat = 40.7128,
            lon = -74.0060,
            description = "Expired event",
            broadcastType = BroadcastType.PUBLIC.value,
            isAnonymous = true,
            createdAt = System.currentTimeMillis() - 2000000,
            expiresAt = pastTime
        )

        // Act & Assert
        assertTrue(event.isExpired, "Event with past expiration should be expired")
    }

    @Test
    fun `active event is active`() = runTest {
        // Arrange
        val futureTime = System.currentTimeMillis() + 1000000
        val event = Event(
            id = "event-789",
            creatorId = "user-123",
            severity = Severity.ALERT.value,
            category = Category.MEDICAL.value,
            lat = 40.7128,
            lon = -74.0060,
            description = "Active event",
            broadcastType = BroadcastType.PUBLIC.value,
            isAnonymous = true,
            createdAt = System.currentTimeMillis(),
            expiresAt = futureTime,
            deletedAt = null
        )

        // Act & Assert
        assertTrue(event.isActive, "Active event should be marked as active")
    }

    @Test
    fun `deleted event is not active`() = runTest {
        // Arrange
        val futureTime = System.currentTimeMillis() + 1000000
        val event = Event(
            id = "event-abc",
            creatorId = "user-123",
            severity = Severity.ALERT.value,
            category = Category.MEDICAL.value,
            lat = 40.7128,
            lon = -74.0060,
            description = "Deleted event",
            broadcastType = BroadcastType.PUBLIC.value,
            isAnonymous = true,
            createdAt = System.currentTimeMillis(),
            expiresAt = futureTime,
            deletedAt = System.currentTimeMillis()
        )

        // Act & Assert
        assertFalse(event.isActive, "Deleted event should not be marked as active")
    }

    @Test
    fun `expired event is not active`() = runTest {
        // Arrange
        val pastTime = System.currentTimeMillis() - 1000000
        val event = Event(
            id = "event-def",
            creatorId = "user-123",
            severity = Severity.ALERT.value,
            category = Category.MEDICAL.value,
            lat = 40.7128,
            lon = -74.0060,
            description = "Expired event",
            broadcastType = BroadcastType.PUBLIC.value,
            isAnonymous = true,
            createdAt = System.currentTimeMillis() - 2000000,
            expiresAt = pastTime,
            deletedAt = null
        )

        // Act & Assert
        assertFalse(event.isActive, "Expired event should not be marked as active")
    }

    // ==================== Event Soft Delete Tests ====================

    @Test
    fun `soft delete event sets deleted at timestamp`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val event = Event(
            id = "event-delete-1",
            creatorId = "user-123",
            severity = Severity.ALERT.value,
            category = Category.MEDICAL.value,
            lat = 40.7128,
            lon = -74.0060,
            description = "Event to delete",
            broadcastType = BroadcastType.PUBLIC.value,
            isAnonymous = true,
            createdAt = now,
            expiresAt = now + Event.DEFAULT_EXPIRATION_MS,
            deletedAt = null
        )

        // Act
        mockEventRepository.createEvent(event)
        val deleteResult = mockEventRepository.deleteEvent("event-delete-1")

        // Assert
        assertTrue(deleteResult.isSuccess, "Event soft delete should succeed")
        val deletedEvent = mockEventRepository.getEventById("event-delete-1").getOrNull()
        assertNotNull(deletedEvent, "Deleted event should still be retrievable")
        assertNotNull(deletedEvent?.deletedAt, "Deleted at timestamp should be set")
        assertTrue(deletedEvent!!.isDeleted, "Event should be marked as deleted")
    }

    @Test
    fun `deleted event has isDeleted true`() = runTest {
        // Arrange
        val event = Event(
            id = "event-delete-2",
            creatorId = "user-123",
            severity = Severity.ALERT.value,
            category = Category.MEDICAL.value,
            lat = 40.7128,
            lon = -74.0060,
            description = "Deleted event",
            broadcastType = BroadcastType.PUBLIC.value,
            isAnonymous = true,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + Event.DEFAULT_EXPIRATION_MS,
            deletedAt = System.currentTimeMillis()
        )

        // Act & Assert
        assertTrue(event.isDeleted, "Event with deletedAt should be marked as deleted")
    }

    @Test
    fun `non-deleted event has isDeleted false`() = runTest {
        // Arrange
        val event = Event(
            id = "event-not-deleted",
            creatorId = "user-123",
            severity = Severity.ALERT.value,
            category = Category.MEDICAL.value,
            lat = 40.7128,
            lon = -74.0060,
            description = "Active event",
            broadcastType = BroadcastType.PUBLIC.value,
            isAnonymous = true,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + Event.DEFAULT_EXPIRATION_MS,
            deletedAt = null
        )

        // Act & Assert
        assertFalse(event.isDeleted, "Event without deletedAt should not be marked as deleted")
    }

    // ==================== Event Recipient Tests ====================

    @Test
    fun `create event recipient successfully`() = runTest {
        // Arrange
        val recipient = EventRecipient(
            eventId = "event-123",
            userId = "user-456",
            deliveryStatus = DeliveryStatus.PENDING.value,
            notifiedAt = null,
            openedAt = null
        )

        // Act
        val result = mockEventRecipientRepository.createRecipient(recipient)

        // Assert
        assertTrue(result.isSuccess, "Event recipient creation should succeed")
    }

    @Test
    fun `mark recipient as sent updates status and timestamp`() = runTest {
        // Arrange
        val recipient = EventRecipient(
            eventId = "event-123",
            userId = "user-456",
            deliveryStatus = DeliveryStatus.PENDING.value,
            notifiedAt = null,
            openedAt = null
        )

        // Act
        val updated = recipient.markSent()

        // Assert
        assertEquals(DeliveryStatus.SENT.value, updated.deliveryStatus, "Delivery status should be SENT")
        assertNotNull(updated.notifiedAt, "Notified at should be set")
        assertTrue(updated.isNotified, "Recipient should be marked as notified")
        assertFalse(updated.isFailed, "Recipient should not be marked as failed")
    }

    @Test
    fun `mark recipient as failed updates status`() = runTest {
        // Arrange
        val recipient = EventRecipient(
            eventId = "event-123",
            userId = "user-456",
            deliveryStatus = DeliveryStatus.PENDING.value,
            notifiedAt = null,
            openedAt = null
        )

        // Act
        val updated = recipient.markFailed()

        // Assert
        assertEquals(DeliveryStatus.FAILED.value, updated.deliveryStatus, "Delivery status should be FAILED")
        assertNotNull(updated.notifiedAt, "Notified at should be set even on failure")
        assertFalse(updated.isNotified, "Failed recipient should not be marked as notified")
        assertTrue(updated.isFailed, "Recipient should be marked as failed")
    }

    @Test
    fun `mark recipient as opened sets timestamp`() = runTest {
        // Arrange
        val recipient = EventRecipient(
            eventId = "event-123",
            userId = "user-456",
            deliveryStatus = DeliveryStatus.SENT.value,
            notifiedAt = System.currentTimeMillis(),
            openedAt = null
        )

        // Act
        val updated = recipient.markOpened()

        // Assert
        assertNotNull(updated.openedAt, "Opened at should be set")
        assertTrue(updated.isOpened, "Recipient should be marked as opened")
        assertTrue(updated.isNotified, "Recipient should still be marked as notified")
    }

    @Test
    fun `composite key is generated correctly`() = runTest {
        // Arrange
        val eventId = "event-abc123"
        val userId = "user-xyz456"
        val expectedKey = "$eventId:$userId"

        // Act
        val actualKey = EventRecipient.compositeKey(eventId, userId)
        val recipientKey = EventRecipient(
            eventId = eventId,
            userId = userId,
            deliveryStatus = DeliveryStatus.PENDING.value
        ).compositeKey

        // Assert
        assertEquals(expectedKey, actualKey, "Composite key should match expected")
        assertEquals(expectedKey, recipientKey, "Recipient composite key should match static method")
    }

    @Test
    fun `pending recipient has correct flags`() = runTest {
        // Arrange
        val recipient = EventRecipient(
            eventId = "event-123",
            userId = "user-456",
            deliveryStatus = DeliveryStatus.PENDING.value,
            notifiedAt = null,
            openedAt = null
        )

        // Act & Assert
        assertFalse(recipient.isNotified, "PENDING recipient should not be notified")
        assertFalse(recipient.isFailed, "PENDING recipient should not be failed")
        assertFalse(recipient.isOpened, "PENDING recipient should not be opened")
    }

    // ==================== Event Query Tests ====================

    @Test
    fun `get event by id returns correct event`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val event = Event(
            id = "event-query-1",
            creatorId = "user-123",
            severity = Severity.ALERT.value,
            category = Category.MEDICAL.value,
            lat = 40.7128,
            lon = -74.0060,
            description = "Query test event",
            broadcastType = BroadcastType.PUBLIC.value,
            isAnonymous = true,
            createdAt = now,
            expiresAt = now + Event.DEFAULT_EXPIRATION_MS
        )
        mockEventRepository.createEvent(event)

        // Act
        val result = mockEventRepository.getEventById("event-query-1")

        // Assert
        assertTrue(result.isSuccess, "Event retrieval should succeed")
        val retrieved = result.getOrNull()
        assertNotNull(retrieved, "Event should be found")
        assertEquals("event-query-1", retrieved?.id, "Retrieved ID should match")
        assertEquals("Query test event", retrieved?.description, "Retrieved description should match")
    }

    @Test
    fun `get event by non-existent id returns null`() = runTest {
        // Act
        val result = mockEventRepository.getEventById("non-existent-id")

        // Assert
        assertTrue(result.isSuccess, "Query should not throw error")
        assertNull(result.getOrNull(), "Non-existent event should return null")
    }

    @Test
    fun `get events by creator returns only creator events`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val userId1 = "user-creator-1"
        val userId2 = "user-creator-2"

        // Create events for user 1
        for (i in 1..3) {
            val event = Event(
                id = "event-$userId1-$i",
                creatorId = userId1,
                severity = Severity.ALERT.value,
                category = Category.MEDICAL.value,
                lat = 40.7128,
                lon = -74.0060,
                description = "Event $i by user 1",
                broadcastType = BroadcastType.PUBLIC.value,
                isAnonymous = true,
                createdAt = now,
                expiresAt = now + Event.DEFAULT_EXPIRATION_MS
            )
            mockEventRepository.createEvent(event)
        }

        // Create events for user 2
        for (i in 1..2) {
            val event = Event(
                id = "event-$userId2-$i",
                creatorId = userId2,
                severity = Severity.ALERT.value,
                category = Category.FIRE.value,
                lat = 40.7128,
                lon = -74.0060,
                description = "Event $i by user 2",
                broadcastType = BroadcastType.PUBLIC.value,
                isAnonymous = true,
                createdAt = now,
                expiresAt = now + Event.DEFAULT_EXPIRATION_MS
            )
            mockEventRepository.createEvent(event)
        }

        // Act
        val result = mockEventRepository.getEventsByCreator(userId1, includeInactive = false)

        // Assert
        assertTrue(result.isSuccess, "Query should succeed")
        val events = result.getOrNull() ?: emptyList()
        assertEquals(3, events.size, "Should return 3 events for user 1")
        events.forEach { event ->
            assertEquals(userId1, event.creatorId, "All events should belong to user 1")
        }
    }

    @Test
    fun `get public events by bounds filters correctly`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()

        // Event inside bounds
        val insideEvent = Event(
            id = "event-inside",
            creatorId = "user-123",
            severity = Severity.ALERT.value,
            category = Category.MEDICAL.value,
            lat = 40.7128,
            lon = -74.0060,
            description = "Inside bounds event",
            broadcastType = BroadcastType.PUBLIC.value,
            isAnonymous = true,
            createdAt = now,
            expiresAt = now + Event.DEFAULT_EXPIRATION_MS
        )
        mockEventRepository.createEvent(insideEvent)

        // Event outside bounds (different latitude)
        val outsideEvent = Event(
            id = "event-outside",
            creatorId = "user-456",
            severity = Severity.ALERT.value,
            category = Category.FIRE.value,
            lat = 35.0,
            lon = -74.0060,
            description = "Outside bounds event",
            broadcastType = BroadcastType.PUBLIC.value,
            isAnonymous = true,
            createdAt = now,
            expiresAt = now + Event.DEFAULT_EXPIRATION_MS
        )
        mockEventRepository.createEvent(outsideEvent)

        // Act - Query bounds around inside event
        val result = mockEventRepository.getPublicEventsByBounds(
            minLat = 40.0,
            maxLat = 41.0,
            minLon = -75.0,
            maxLon = -73.0
        )

        // Assert
        assertTrue(result.isSuccess, "Query should succeed")
        val events = result.getOrNull() ?: emptyList()
        assertEquals(1, events.size, "Should return only 1 event within bounds")
        assertEquals("event-inside", events.first().id, "Should return inside event")
    }

    @Test
    fun `get active events returns only active events`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()

        // Active event
        val activeEvent = Event(
            id = "event-active",
            creatorId = "user-123",
            severity = Severity.ALERT.value,
            category = Category.MEDICAL.value,
            lat = 40.7128,
            lon = -74.0060,
            description = "Active event",
            broadcastType = BroadcastType.PUBLIC.value,
            isAnonymous = true,
            createdAt = now,
            expiresAt = now + Event.DEFAULT_EXPIRATION_MS,
            deletedAt = null
        )
        mockEventRepository.createEvent(activeEvent)

        // Expired event
        val expiredEvent = Event(
            id = "event-expired",
            creatorId = "user-456",
            severity = Severity.ALERT.value,
            category = Category.FIRE.value,
            lat = 40.7128,
            lon = -74.0060,
            description = "Expired event",
            broadcastType = BroadcastType.PUBLIC.value,
            isAnonymous = true,
            createdAt = now - Event.DEFAULT_EXPIRATION_MS - 1000,
            expiresAt = now - 1000,
            deletedAt = null
        )
        mockEventRepository.createEvent(expiredEvent)

        // Deleted event
        val deletedEvent = Event(
            id = "event-deleted",
            creatorId = "user-789",
            severity = Severity.ALERT.value,
            category = Category.TRAFFIC.value,
            lat = 40.7128,
            lon = -74.0060,
            description = "Deleted event",
            broadcastType = BroadcastType.PUBLIC.value,
            isAnonymous = true,
            createdAt = now,
            expiresAt = now + Event.DEFAULT_EXPIRATION_MS,
            deletedAt = now
        )
        mockEventRepository.createEvent(deletedEvent)

        // Act
        val result = mockEventRepository.getActiveEvents()

        // Assert
        assertTrue(result.isSuccess, "Query should succeed")
        val events = result.getOrNull() ?: emptyList()
        assertEquals(1, events.size, "Should return only 1 active event")
        assertEquals("event-active", events.first().id, "Should return active event")
    }

    @Test
    fun `get private events for user returns private events`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val userId = "user-private-1"

        // Private event for user
        val privateEvent = Event(
            id = "event-private",
            creatorId = userId,
            severity = Severity.ALERT.value,
            category = Category.MEDICAL.value,
            lat = 40.7128,
            lon = -74.0060,
            description = "Private event",
            broadcastType = BroadcastType.PRIVATE.value,
            isAnonymous = false,
            createdAt = now,
            expiresAt = now + Event.DEFAULT_EXPIRATION_MS
        )
        mockEventRepository.createEvent(privateEvent)

        // Public event for user
        val publicEvent = Event(
            id = "event-public",
            creatorId = userId,
            severity = Severity.ALERT.value,
            category = Category.FIRE.value,
            lat = 40.7128,
            lon = -74.0060,
            description = "Public event",
            broadcastType = BroadcastType.PUBLIC.value,
            isAnonymous = true,
            createdAt = now,
            expiresAt = now + Event.DEFAULT_EXPIRATION_MS
        )
        mockEventRepository.createEvent(publicEvent)

        // Act
        val result = mockEventRepository.getPrivateEventsForUser(userId)

        // Assert
        assertTrue(result.isSuccess, "Query should succeed")
        val events = result.getOrNull() ?: emptyList()
        assertEquals(1, events.size, "Should return only 1 private event")
        assertEquals("event-private", events.first().id, "Should return private event")
    }

    // ==================== Complete Event Lifecycle Integration Tests ====================

    @Test
    fun `complete event lifecycle from creation to expiration`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()

        // Step 1: Create event
        val event = Event(
            id = "",
            creatorId = "user-lifecycle-1",
            severity = Severity.CRISIS.value,
            category = Category.FIRE.value,
            lat = 40.7128,
            lon = -74.0060,
            description = "Fire emergency",
            broadcastType = BroadcastType.PUBLIC.value,
            isAnonymous = true,
            createdAt = now,
            expiresAt = now + Event.DEFAULT_EXPIRATION_MS
        )
        val createResult = mockEventRepository.createEvent(event)
        assertTrue(createResult.isSuccess, "Event creation should succeed")
        val createdEvent = createResult.getOrNull()!!
        assertTrue(createdEvent.isActive, "New event should be active")
        assertFalse(createdEvent.isExpired, "New event should not be expired")
        assertFalse(createdEvent.isDeleted, "New event should not be deleted")

        // Step 2: Retrieve event
        val retrievedResult = mockEventRepository.getEventById(createdEvent.id)
        assertTrue(retrievedResult.isSuccess, "Event retrieval should succeed")
        val retrievedEvent = retrievedResult.getOrNull()
        assertNotNull(retrievedEvent, "Event should be found")
        assertEquals(createdEvent.id, retrievedEvent?.id, "Retrieved ID should match")

        // Step 3: Get active events (should include this event)
        val activeEventsResult = mockEventRepository.getActiveEvents()
        assertTrue(activeEventsResult.isSuccess, "Active events query should succeed")
        val activeEvents = activeEventsResult.getOrNull() ?: emptyList()
        assertTrue(activeEvents.any { it.id == createdEvent.id }, "Event should be in active events")

        // Step 4: Simulate expiration
        val expiredEvent = createdEvent.copy(
            expiresAt = now - 1000
        )
        assertTrue(expiredEvent.isExpired, "Simulated expired event should be expired")
        assertFalse(expiredEvent.isActive, "Expired event should not be active")

        // Step 5: Soft delete (if needed)
        val deleteResult = mockEventRepository.deleteEvent(createdEvent.id)
        assertTrue(deleteResult.isSuccess, "Event soft delete should succeed")
        val deletedEventResult = mockEventRepository.getEventById(createdEvent.id)
        assertTrue(deletedEventResult.getOrNull()?.isDeleted == true, "Event should be marked as deleted")
    }

    @Test
    fun `private event with recipients lifecycle`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val creatorId = "user-creator-private"
        val recipientId1 = "user-recipient-1"
        val recipientId2 = "user-recipient-2"

        // Step 1: Create private event
        val event = Event(
            id = "",
            creatorId = creatorId,
            severity = Severity.CRISIS.value,
            category = Category.MEDICAL.value,
            lat = 40.7128,
            lon = -74.0060,
            description = "Private emergency",
            broadcastType = BroadcastType.PRIVATE.value,
            isAnonymous = false,
            createdAt = now,
            expiresAt = now + Event.DEFAULT_EXPIRATION_MS
        )
        val createResult = mockEventRepository.createEvent(event)
        assertTrue(createResult.isSuccess, "Private event creation should succeed")
        val createdEvent = createResult.getOrNull()!!

        // Step 2: Create recipients
        val recipient1 = EventRecipient(
            eventId = createdEvent.id,
            userId = recipientId1,
            deliveryStatus = DeliveryStatus.PENDING.value
        )
        val recipient2 = EventRecipient(
            eventId = createdEvent.id,
            userId = recipientId2,
            deliveryStatus = DeliveryStatus.PENDING.value
        )

        val r1Result = mockEventRecipientRepository.createRecipient(recipient1)
        val r2Result = mockEventRecipientRepository.createRecipient(recipient2)
        assertTrue(r1Result.isSuccess, "Recipient 1 creation should succeed")
        assertTrue(r2Result.isSuccess, "Recipient 2 creation should succeed")

        // Step 3: Mark first recipient as sent
        val sentRecipient = recipient1.markSent()
        assertTrue(sentRecipient.isNotified, "Sent recipient should be marked as notified")
        assertFalse(sentRecipient.isFailed, "Sent recipient should not be marked as failed")

        // Step 4: Mark first recipient as opened
        val openedRecipient = sentRecipient.markOpened()
        assertTrue(openedRecipient.isOpened, "Opened recipient should be marked as opened")

        // Step 5: Mark second recipient as failed
        val failedRecipient = recipient2.markFailed()
        assertTrue(failedRecipient.isFailed, "Failed recipient should be marked as failed")

        // Assert complete flow
        assertTrue(createdEvent.isPrivate, "Event should be private")
        assertFalse(createdEvent.isAnonymous, "Private event should not be anonymous")
    }

    @Test
    fun `public event is anonymous`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val event = Event(
            id = "",
            creatorId = "user-anonymous",
            severity = Severity.ALERT.value,
            category = Category.WEATHER.value,
            lat = 40.7128,
            lon = -74.0060,
            description = "Public anonymous event",
            broadcastType = BroadcastType.PUBLIC.value,
            isAnonymous = true,
            createdAt = now,
            expiresAt = now + Event.DEFAULT_EXPIRATION_MS
        )

        // Act
        val result = mockEventRepository.createEvent(event)

        // Assert
        assertTrue(result.isSuccess, "Public event creation should succeed")
        val createdEvent = result.getOrNull()!!
        assertTrue(createdEvent.isPublic, "Event should be public")
        assertFalse(createdEvent.isPrivate, "Public event should not be private")
        assertTrue(createdEvent.isAnonymous, "Public event should be anonymous")
    }

    @Test
    fun `event enum conversions work correctly`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val event = Event(
            id = "event-enum-test",
            creatorId = "user-123",
            severity = Severity.CRISIS.value,
            category = Category.MEDICAL.value,
            lat = 40.7128,
            lon = -74.0060,
            description = "Enum test event",
            broadcastType = BroadcastType.PRIVATE.value,
            isAnonymous = false,
            createdAt = now,
            expiresAt = now + Event.DEFAULT_EXPIRATION_MS
        )

        // Act & Assert
        assertEquals(Severity.CRISIS, event.severityEnum, "Severity enum should convert correctly")
        assertEquals(Category.MEDICAL, event.categoryEnum, "Category enum should convert correctly")
        assertEquals(BroadcastType.PRIVATE, event.broadcastTypeEnum, "Broadcast type enum should convert correctly")
    }

    @Test
    fun `invalid enum values return null`() = runTest {
        // Act & Assert
        assertNull(Severity.fromValue("INVALID_SEVERITY"), "Invalid severity should return null")
        assertNull(Category.fromValue("INVALID_CATEGORY"), "Invalid category should return null")
        assertNull(BroadcastType.fromValue("INVALID_TYPE"), "Invalid broadcast type should return null")
    }

    // ==================== Mock Implementations ====================

    class MockEventRepository : EventRepository {
        private val events = mutableMapOf<String, Event>()

        override suspend fun createEvent(event: Event): Result<Event> {
            val id = if (event.id.isEmpty()) "event-${System.currentTimeMillis()}" else event.id
            val createdEvent = event.copy(id = id)
            events[id] = createdEvent
            return Result.success(createdEvent)
        }

        override suspend fun getEventById(eventId: String): Result<Event?> {
            return Result.success(events[eventId])
        }

        override suspend fun getEventsByCreator(
            creatorId: String,
            includeInactive: Boolean
        ): Result<List<Event>> {
            val filtered = events.values.filter { it.creatorId == creatorId }
            return if (includeInactive) {
                Result.success(filtered)
            } else {
                Result.success(filtered.filter { it.isActive })
            }
        }

        override suspend fun getPublicEventsByBounds(
            minLat: Double,
            maxLat: Double,
            minLon: Double,
            maxLon: Double
        ): Result<List<Event>> {
            val filtered = events.values.filter {
                it.broadcastTypeEnum == BroadcastType.PUBLIC &&
                it.lat >= minLat && it.lat <= maxLat &&
                it.lon >= minLon && it.lon <= maxLon &&
                it.isActive
            }
            return Result.success(filtered)
        }

        override suspend fun getPublicEventsByRadius(
            lat: Double,
            lon: Double,
            radiusMiles: Double
        ): Result<List<Event>> {
            // Simple bounding box for radius (haversine approximation not needed for mock)
            val latRadius = radiusMiles / 69.0
            val minLat = lat - latRadius
            val maxLat = lat + latRadius
            val minLon = lon - latRadius
            val maxLon = lon + latRadius
            return getPublicEventsByBounds(minLat, maxLat, minLon, maxLon)
        }

        override suspend fun getActiveEvents(): Result<List<Event>> {
            val active = events.values.filter { it.isActive }
            return Result.success(active)
        }

        override suspend fun getPrivateEventsForUser(userId: String): Result<List<Event>> {
            val private = events.values.filter {
                it.broadcastTypeEnum == BroadcastType.PRIVATE && it.isActive
            }
            return Result.success(private)
        }

        override suspend fun deleteEvent(eventId: String): Result<Unit> {
            val event = events[eventId]
            if (event != null) {
                events[eventId] = event.copy(deletedAt = System.currentTimeMillis())
            }
            return Result.success(Unit)
        }

        override suspend fun cleanupExpiredEvents(): Result<Int> {
            val now = System.currentTimeMillis()
            val expiredCount = events.values.count { it.expiresAt < now }
            // Remove expired events from store
            events.keys.filter { events[it]?.expiresAt!! < now }.forEach { events.remove(it) }
            return Result.success(expiredCount)
        }

        fun clear() {
            events.clear()
        }
    }

    class MockEventRecipientRepository {
        private val recipients = mutableMapOf<String, EventRecipient>()

        suspend fun createRecipient(recipient: EventRecipient): Result<EventRecipient> {
            val key = recipient.compositeKey
            recipients[key] = recipient
            return Result.success(recipient)
        }

        fun clear() {
            recipients.clear()
        }
    }
}
