package org.crimsoncode2026.data

import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for Data Repositories
 *
 * Tests core data repository operations:
 * 1. UserRepository: CRUD operations, device binding, FCM token management
 * 2. EventRepository: Event CRUD, queries by bounds/radius, expiration handling
 * 3. UserContactRepository: Contact CRUD, sync operations
 * 4. EventRecipientRepository: Recipient tracking, delivery status management
 * 5. Data model validation and property accessors
 */
class DataRepositoryUnitTest {

    private lateinit var mockUserRepository: MockUserRepository
    private lateinit var mockEventRepository: MockEventRepository
    private lateinit var mockContactRepository: MockUserContactRepository
    private lateinit var mockRecipientRepository: MockEventRecipientRepository

    @BeforeTest
    fun setup() {
        mockUserRepository = MockUserRepository()
        mockEventRepository = MockEventRepository()
        mockContactRepository = MockUserContactRepository()
        mockRecipientRepository = MockEventRecipientRepository()
    }

    @AfterTest
    fun teardown() {
        mockUserRepository.clear()
        mockEventRepository.clear()
        mockContactRepository.clear()
        mockRecipientRepository.clear()
    }

    // ==================== User Repository Tests ====================

    @Test
    fun `create user successfully`() = runTest {
        // Arrange
        val user = User(
            id = "",
            phoneNumber = "+15551234567",
            displayName = "John Doe",
            deviceId = "device-123",
            platform = "ANDROID",
            isActive = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            lastActiveAt = System.currentTimeMillis()
        )

        // Act
        val result = mockUserRepository.createUser(user)

        // Assert
        assertTrue(result.isSuccess, "User creation should succeed")
        val createdUser = result.getOrNull()!!
        assertTrue(createdUser.id.isNotEmpty(), "User should have generated ID")
        assertEquals("John Doe", createdUser.displayName, "Display name should match")
        assertEquals("+15551234567", createdUser.phoneNumber, "Phone number should match")
    }

    @Test
    fun `get user by id returns correct user`() = runTest {
        // Arrange
        val user = User(
            id = "user-123",
            phoneNumber = "+15551234567",
            displayName = "John Doe",
            deviceId = "device-123",
            platform = "ANDROID",
            isActive = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            lastActiveAt = System.currentTimeMillis()
        )
        mockUserRepository.createUser(user)

        // Act
        val result = mockUserRepository.getUserById("user-123")

        // Assert
        assertTrue(result.isSuccess, "Query should succeed")
        val retrieved = result.getOrNull()
        assertNotNull(retrieved, "User should be found")
        assertEquals("user-123", retrieved?.id, "ID should match")
        assertEquals("John Doe", retrieved?.displayName, "Display name should match")
    }

    @Test
    fun `get user by phone number returns correct user`() = runTest {
        // Arrange
        val user = User(
            id = "user-phone-123",
            phoneNumber = "+15551234567",
            displayName = "Phone User",
            deviceId = "device-123",
            platform = "IOS",
            isActive = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            lastActiveAt = System.currentTimeMillis()
        )
        mockUserRepository.createUser(user)

        // Act
        val result = mockUserRepository.getUserByPhoneNumber("+15551234567")

        // Assert
        assertTrue(result.isSuccess, "Query should succeed")
        val retrieved = result.getOrNull()
        assertNotNull(retrieved, "User should be found")
        assertEquals("+15551234567", retrieved?.phoneNumber, "Phone number should match")
    }

    @Test
    fun `get user by device id returns correct user`() = runTest {
        // Arrange
        val user = User(
            id = "user-device-123",
            phoneNumber = "+1999888776666",
            displayName = "Device User",
            deviceId = "device-456",
            platform = "ANDROID",
            isActive = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            lastActiveAt = System.currentTimeMillis()
        )
        mockUserRepository.createUser(user)

        // Act
        val result = mockUserRepository.getUserByDeviceId("device-456")

        // Assert
        assertTrue(result.isSuccess, "Query should succeed")
        val retrieved = result.getOrNull()
        assertNotNull(retrieved, "User should be found")
        assertEquals("device-456", retrieved?.deviceId, "Device ID should match")
    }

    @Test
    fun `update display name successfully`() = runTest {
        // Arrange
        val user = User(
            id = "user-update-123",
            phoneNumber = "+15551234567",
            displayName = "Old Name",
            deviceId = "device-123",
            platform = "ANDROID",
            isActive = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            lastActiveAt = System.currentTimeMillis()
        )
        mockUserRepository.createUser(user)

        // Act
        val result = mockUserRepository.updateDisplayName("user-update-123", "New Name")

        // Assert
        assertTrue(result.isSuccess, "Display name update should succeed")
        val updated = result.getOrNull()!!
        assertEquals("New Name", updated.displayName, "Display name should be updated")
        assertTrue(updated.updatedAt > user.updatedAt, "Updated at should be later")
    }

    @Test
    fun `update FCM token successfully`() = runTest {
        // Arrange
        val user = User(
            id = "user-fcm-123",
            phoneNumber = "+15551234567",
            displayName = "FCM User",
            deviceId = "device-123",
            platform = "ANDROID",
            isActive = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            lastActiveAt = System.currentTimeMillis()
        )
        mockUserRepository.createUser(user)

        // Act
        val result = mockUserRepository.updateFcmToken("user-fcm-123", "new-fcm-token-456")

        // Assert
        assertTrue(result.isSuccess, "FCM token update should succeed")
        val updated = result.getOrNull()!!
        assertEquals("new-fcm-token-456", updated.fcmToken, "FCM token should be updated")
        assertTrue(updated.updatedAt > user.updatedAt, "Updated at should be later")
    }

    @Test
    fun `update last active timestamp successfully`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val user = User(
            id = "user-last-active-123",
            phoneNumber = "+15551234567",
            displayName = "Last Active User",
            deviceId = "device-123",
            platform = "ANDROID",
            isActive = true,
            createdAt = now - 10000,
            updatedAt = now - 10000,
            lastActiveAt = now - 10000
        )
        mockUserRepository.createUser(user)

        // Act
        val result = mockUserRepository.updateLastActive("user-last-active-123")

        // Assert
        assertTrue(result.isSuccess, "Last active update should succeed")
        val updated = result.getOrNull()!!
        assertTrue(updated.lastActiveAt > user.lastActiveAt, "Last active should be updated")
    }

    @Test
    fun `update device id successfully`() = runTest {
        // Arrange
        val user = User(
            id = "user-device-update-123",
            phoneNumber = "+15551234567",
            displayName = "Device Update User",
            deviceId = "old-device-123",
            platform = "ANDROID",
            isActive = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            lastActiveAt = System.currentTimeMillis()
        )
        mockUserRepository.createUser(user)

        // Act
        val result = mockUserRepository.updateDeviceId("user-device-update-123", "new-device-456")

        // Assert
        assertTrue(result.isSuccess, "Device ID update should succeed")
        val updated = result.getOrNull()!!
        assertEquals("new-device-456", updated.deviceId, "Device ID should be updated")
    }

    @Test
    fun `deactivate user successfully`() = runTest {
        // Arrange
        val user = User(
            id = "user-deactivate-123",
            phoneNumber = "+15551234567",
            displayName = "Deactivate User",
            deviceId = "device-123",
            platform = "ANDROID",
            isActive = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            lastActiveAt = System.currentTimeMillis()
        )
        mockUserRepository.createUser(user)

        // Act
        val result = mockUserRepository.deactivateUser("user-deactivate-123")

        // Assert
        assertTrue(result.isSuccess, "User deactivation should succeed")
        val deactivated = mockUserRepository.getUserById("user-deactivate-123").getOrNull()
        assertNotNull(deactivated, "User should still be found")
        assertFalse(deactivated?.isActive, "User should be inactive")
    }

    @Test
    fun `delete user successfully`() = runTest {
        // Arrange
        val user = User(
            id = "user-delete-123",
            phoneNumber = "+15551234567",
            displayName = "Delete User",
            deviceId = "device-123",
            platform = "ANDROID",
            isActive = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            lastActiveAt = System.currentTimeMillis()
        )
        mockUserRepository.createUser(user)

        // Act
        val deleteResult = mockUserRepository.deleteUser("user-delete-123")

        // Assert
        assertTrue(deleteResult.isSuccess, "User deletion should succeed")

        val getResult = mockUserRepository.getUserById("user-delete-123")
        assertNull(getResult.getOrNull(), "Deleted user should not be found")
    }

    // ==================== Event Repository Tests ====================

    @Test
    fun `create event successfully`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val event = Event(
            id = "",
            creatorId = "user-123",
            severity = Severity.ALERT.value,
            category = Category.MEDICAL.value,
            lat = 40.7128,
            lon = -74.0060,
            description = "Test event",
            broadcastType = BroadcastType.PUBLIC.value,
            isAnonymous = true,
            createdAt = now,
            expiresAt = now + Event.DEFAULT_EXPIRATION_MS
        )

        // Act
        val result = mockEventRepository.createEvent(event)

        // Assert
        assertTrue(result.isSuccess, "Event creation should succeed")
        val created = result.getOrNull()!!
        assertTrue(created.id.isNotEmpty(), "Event should have generated ID")
        assertEquals("user-123", created.creatorId, "Creator ID should match")
        assertEquals(Severity.ALERT.value, created.severity, "Severity should match")
        assertEquals(Category.MEDICAL.value, created.category, "Category should match")
    }

    @Test
    fun `get event by id returns correct event`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val event = Event(
            id = "event-123",
            creatorId = "user-123",
            severity = Severity.ALERT.value,
            category = Category.MEDICAL.value,
            lat = 40.7128,
            lon = -74.0060,
            description = "Test event",
            broadcastType = BroadcastType.PUBLIC.value,
            isAnonymous = true,
            createdAt = now,
            expiresAt = now + Event.DEFAULT_EXPIRATION_MS
        )
        mockEventRepository.createEvent(event)

        // Act
        val result = mockEventRepository.getEventById("event-123")

        // Assert
        assertTrue(result.isSuccess, "Query should succeed")
        val retrieved = result.getOrNull()
        assertNotNull(retrieved, "Event should be found")
        assertEquals("event-123", retrieved?.id, "ID should match")
    }

    @Test
    fun `get events by creator returns correct events`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val userId = "user-creator-123"

        // Create active events
        repeat(3) { i ->
            val event = Event(
                id = "event-creator-$i",
                creatorId = userId,
                severity = Severity.ALERT.value,
                category = Category.MEDICAL.value,
                lat = 40.7128,
                lon = -74.0060,
                description = "Active event $i",
                broadcastType = BroadcastType.PUBLIC.value,
                isAnonymous = true,
                createdAt = now,
                expiresAt = now + Event.DEFAULT_EXPIRATION_MS
            )
            mockEventRepository.createEvent(event)
        }

        // Create expired event
        val expiredEvent = Event(
            id = "event-expired",
            creatorId = userId,
            severity = Severity.ALERT.value,
            category = Category.FIRE.value,
            lat = 40.7128,
            lon = -74.0060,
            description = "Expired event",
            broadcastType = BroadcastType.PUBLIC.value,
            isAnonymous = true,
            createdAt = now - Event.DEFAULT_EXPIRATION_MS,
            expiresAt = now - 1000
        )
        mockEventRepository.createEvent(expiredEvent)

        // Act
        val result = mockEventRepository.getEventsByCreator(userId, includeInactive = false)

        // Assert
        assertTrue(result.isSuccess, "Query should succeed")
        val events = result.getOrNull() ?: emptyList()
        assertEquals(3, events.size, "Should return 3 active events only")
        events.forEach { event ->
            assertEquals(userId, event.creatorId, "All events should belong to creator")
        }
    }

    @Test
    fun `get active events returns only active events`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()

        // Create active event
        val activeEvent = Event(
            id = "event-active-1",
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

        // Create expired event
        val expiredEvent = Event(
            id = "event-expired-1",
            creatorId = "user-456",
            severity = Severity.ALERT.value,
            category = Category.FIRE.value,
            lat = 40.7128,
            lon = -74.0060,
            description = "Expired event",
            broadcastType = BroadcastType.PUBLIC.value,
            isAnonymous = true,
            createdAt = now - Event.DEFAULT_EXPIRATION_MS,
            expiresAt = now - 1000,
            deletedAt = null
        )
        mockEventRepository.createEvent(expiredEvent)

        // Create deleted event
        val deletedEvent = Event(
            id = "event-deleted-1",
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
        assertEquals("event-active-1", events[0].id, "Should return active event")
    }

    @Test
    fun `delete event successfully`() = runTest {
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
        mockEventRepository.createEvent(event)

        // Act
        val deleteResult = mockEventRepository.deleteEvent("event-delete-1")

        // Assert
        assertTrue(deleteResult.isSuccess, "Event deletion should succeed")

        val getResult = mockEventRepository.getEventById("event-delete-1")
        val deletedEvent = getResult.getOrNull()
        assertNotNull(deletedEvent, "Deleted event should still exist (soft delete)")
        assertNotNull(deletedEvent?.deletedAt, "Deleted at should be set")
    }

    // ==================== EventRecipient Repository Tests ====================

    @Test
    fun `create recipients successfully`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val recipients = listOf(
            EventRecipient(
                eventId = "event-123",
                userId = "user-456",
                deliveryStatus = DeliveryStatus.PENDING.value,
                notifiedAt = null,
                openedAt = null
            ),
            EventRecipient(
                eventId = "event-123",
                userId = "user-789",
                deliveryStatus = DeliveryStatus.PENDING.value,
                notifiedAt = null,
                openedAt = null
            )
        )

        // Act
        val result = mockRecipientRepository.createRecipients(recipients)

        // Assert
        assertTrue(result.isSuccess, "Recipients creation should succeed")
        val created = result.getOrNull()!!
        assertEquals(2, created.size, "Should create 2 recipients")
    }

    @Test
    fun `get recipients by event returns correct recipients`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val recipients = listOf(
            EventRecipient(
                eventId = "event-recipient-1",
                userId = "user-456",
                deliveryStatus = DeliveryStatus.PENDING.value,
                notifiedAt = null,
                openedAt = null
            ),
            EventRecipient(
                eventId = "event-recipient-1",
                userId = "user-789",
                deliveryStatus = DeliveryStatus.PENDING.value,
                notifiedAt = null,
                openedAt = null
            )
        )
        mockRecipientRepository.createRecipients(recipients)

        // Act
        val result = mockRecipientRepository.getRecipientsByEvent("event-recipient-1")

        // Assert
        assertTrue(result.isSuccess, "Query should succeed")
        val retrieved = result.getOrNull() ?: emptyList()
        assertEquals(2, retrieved.size, "Should return 2 recipients")
    }

    @Test
    fun `mark as sent successfully`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val recipient = EventRecipient(
            eventId = "event-sent-1",
            userId = "user-123",
            deliveryStatus = DeliveryStatus.PENDING.value,
            notifiedAt = null,
            openedAt = null
        )
        mockRecipientRepository.createRecipients(listOf(recipient))

        // Act
        val result = mockRecipientRepository.markAsSent("event-sent-1", "user-123")

        // Assert
        assertTrue(result.isSuccess, "Mark as sent should succeed")
        val marked = result.getOrNull()!!
        assertEquals(DeliveryStatus.SENT.value, marked.deliveryStatus, "Status should be SENT")
        assertNotNull(marked.notifiedAt, "Notified at should be set")
        assertTrue(marked.isNotified, "isNotified should be true")
        assertFalse(marked.isFailed, "isFailed should be false")
    }

    @Test
    fun `mark as failed successfully`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val recipient = EventRecipient(
            eventId = "event-failed-1",
            userId = "user-123",
            deliveryStatus = DeliveryStatus.PENDING.value,
            notifiedAt = null,
            openedAt = null
        )
        mockRecipientRepository.createRecipients(listOf(recipient))

        // Act
        val result = mockRecipientRepository.markAsFailed("event-failed-1", "user-123")

        // Assert
        assertTrue(result.isSuccess, "Mark as failed should succeed")
        val marked = result.getOrNull()!!
        assertEquals(DeliveryStatus.FAILED.value, marked.deliveryStatus, "Status should be FAILED")
        assertNotNull(marked.notifiedAt, "Notified at should be set even on failure")
        assertFalse(marked.isNotified, "isNotified should be false")
        assertTrue(marked.isFailed, "isFailed should be true")
    }

    @Test
    fun `mark as opened successfully`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val recipient = EventRecipient(
            eventId = "event-opened-1",
            userId = "user-123",
            deliveryStatus = DeliveryStatus.SENT.value,
            notifiedAt = now - 10000,
            openedAt = null
        )
        mockRecipientRepository.createRecipients(listOf(recipient))

        // Act
        val result = mockRecipientRepository.markAsOpened("event-opened-1", "user-123")

        // Assert
        assertTrue(result.isSuccess, "Mark as opened should succeed")
        val marked = result.getOrNull()!!
        assertNotNull(marked.openedAt, "Opened at should be set")
        assertTrue(marked.isOpened, "isOpened should be true")
        assertTrue(marked.isNotified, "isNotified should remain true")
    }

    // ==================== UserContact Repository Tests ====================

    @Test
    fun `create contact successfully`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val contact = UserContact(
            id = "",
            userId = "user-123",
            contactPhoneNumber = "+15551234567",
            displayName = "John Doe",
            hasApp = false,
            contactUserId = null,
            createdAt = now,
            updatedAt = now
        )

        // Act
        val result = mockContactRepository.createContact(contact)

        // Assert
        assertTrue(result.isSuccess, "Contact creation should succeed")
        val created = result.getOrNull()!!
        assertTrue(created.id.isNotEmpty(), "Contact should have generated ID")
        assertEquals("John Doe", created.displayName, "Display name should match")
    }

    @Test
    fun `sync contacts upserts existing`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val userId = "user-sync-123"

        // Create initial contact
        val initialContact = UserContact(
            id = "contact-1",
            userId = userId,
            contactPhoneNumber = "+15551234567",
            displayName = "Old Name",
            hasApp = false,
            contactUserId = null,
            createdAt = now,
            updatedAt = now
        )
        mockContactRepository.createContact(initialContact)

        // Sync with updated contact
        val updatedContact = UserContact(
            id = "",
            userId = userId,
            contactPhoneNumber = "+15551234567",
            displayName = "New Name",
            hasApp = true,
            contactUserId = "app-user-456",
            createdAt = now,
            updatedAt = now
        )

        // Act
        val result = mockContactRepository.syncContacts(userId, listOf(updatedContact))

        // Assert
        assertTrue(result.isSuccess, "Sync should succeed")
        val synced = result.getOrNull() ?: emptyList()
        assertEquals(1, synced.size, "Should have 1 synced contact")
        assertEquals("New Name", synced[0].displayName, "Display name should be updated")
        assertTrue(synced[0].hasApp, "hasApp should be updated")
    }

    @Test
    fun `delete contact successfully`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val contact = UserContact(
            id = "contact-delete-1",
            userId = "user-123",
            contactPhoneNumber = "+15551234567",
            displayName = "Delete Me",
            hasApp = false,
            contactUserId = null,
            createdAt = now,
            updatedAt = now
        )
        mockContactRepository.createContact(contact)

        // Act
        val deleteResult = mockContactRepository.deleteContact("contact-delete-1")

        // Assert
        assertTrue(deleteResult.isSuccess, "Contact deletion should succeed")

        val getResult = mockContactRepository.getContactById("user-123", "contact-delete-1")
        assertNull(getResult.getOrNull(), "Deleted contact should not be found")
    }

    // ==================== Data Model Tests ====================

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
    fun `event is expired when past expiration`() = runTest {
        // Arrange
        val pastTime = System.currentTimeMillis() - 100000
        val event = Event(
            id = "event-expired",
            creatorId = "user-123",
            severity = Severity.ALERT.value,
            category = Category.MEDICAL.value,
            lat = 40.7128,
            lon = -74.0060,
            description = "Expired event",
            broadcastType = BroadcastType.PUBLIC.value,
            isAnonymous = true,
            createdAt = pastTime - 172800000,
            expiresAt = pastTime,
            deletedAt = null
        )

        // Act & Assert
        assertTrue(event.isExpired, "Event with past expiration should be expired")
        assertFalse(event.isActive, "Expired event should not be active")
    }

    @Test
    fun `event is not expired when future expiration`() = runTest {
        // Arrange
        val futureTime = System.currentTimeMillis() + 100000
        val event = Event(
            id = "event-active",
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
        assertFalse(event.isExpired, "Event with future expiration should not be expired")
        assertTrue(event.isActive, "Active event should be active")
    }

    @Test
    fun `event is not active when deleted`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val event = Event(
            id = "event-deleted",
            creatorId = "user-123",
            severity = Severity.ALERT.value,
            category = Category.MEDICAL.value,
            lat = 40.7128,
            lon = -74.0060,
            description = "Deleted event",
            broadcastType = BroadcastType.PUBLIC.value,
            isAnonymous = true,
            createdAt = now,
            expiresAt = now + Event.DEFAULT_EXPIRATION_MS,
            deletedAt = now
        )

        // Act & Assert
        assertTrue(event.isDeleted, "Event with deleted_at should be marked as deleted")
        assertFalse(event.isActive, "Deleted event should not be active")
    }

    @Test
    fun `event enum conversions work correctly`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val event = Event(
            id = "event-enum",
            creatorId = "user-123",
            severity = Severity.CRISIS.value,
            category = Category.MEDICAL.value,
            lat = 40.7128,
            lon = -74.0060,
            description = "Enum test",
            broadcastType = BroadcastType.PRIVATE.value,
            isAnonymous = false,
            createdAt = now,
            expiresAt = now + Event.DEFAULT_EXPIRATION_MS
        )

        // Act & Assert
        assertEquals(Severity.CRISIS, event.severityEnum, "Severity enum should convert correctly")
        assertEquals(Category.MEDICAL, event.categoryEnum, "Category enum should convert correctly")
        assertEquals(BroadcastType.PRIVATE, event.broadcastTypeEnum, "Broadcast type enum should convert correctly")
        assertTrue(event.isPrivate, "PRIVATE broadcast type should be private")
        assertFalse(event.isPublic, "PRIVATE event should not be public")
        assertFalse(event.isAnonymous, "Private event should not be anonymous")
    }

    @Test
    fun `event recipient composite key is generated correctly`() = runTest {
        // Arrange
        val eventId = "event-123"
        val userId = "user-456"
        val expectedKey = "$eventId:$userId"

        // Act
        val actualKey = EventRecipient.compositeKey(eventId, userId)

        // Assert
        assertEquals(expectedKey, actualKey, "Composite key should match expected")
    }

    @Test
    fun `event recipient mark sent creates updated copy`() = runTest {
        // Arrange
        val recipient = EventRecipient(
            eventId = "event-1",
            userId = "user-1",
            deliveryStatus = DeliveryStatus.PENDING.value,
            notifiedAt = null,
            openedAt = null
        )

        // Act
        val marked = recipient.markSent()

        // Assert
        assertEquals(DeliveryStatus.SENT.value, marked.deliveryStatus, "Status should be SENT")
        assertNotNull(marked.notifiedAt, "Notified at should be set")
        assertTrue(marked.isNotified, "isNotified should be true")
    }

    @Test
    fun `event recipient mark failed creates updated copy`() = runTest {
        // Arrange
        val recipient = EventRecipient(
            eventId = "event-1",
            userId = "user-1",
            deliveryStatus = DeliveryStatus.PENDING.value,
            notifiedAt = null,
            openedAt = null
        )

        // Act
        val marked = recipient.markFailed()

        // Assert
        assertEquals(DeliveryStatus.FAILED.value, marked.deliveryStatus, "Status should be FAILED")
        assertNotNull(marked.notifiedAt, "Notified at should be set even on failure")
        assertFalse(marked.isNotified, "isNotified should be false")
        assertTrue(marked.isFailed, "isFailed should be true")
    }

    @Test
    fun `event recipient mark opened creates updated copy`() = runTest {
        // Arrange
        val recipient = EventRecipient(
            eventId = "event-1",
            userId = "user-1",
            deliveryStatus = DeliveryStatus.SENT.value,
            notifiedAt = System.currentTimeMillis(),
            openedAt = null
        )

        // Act
        val marked = recipient.markOpened()

        // Assert
        assertNotNull(marked.openedAt, "Opened at should be set")
        assertTrue(marked.isOpened, "isOpened should be true")
        assertTrue(marked.isNotified, "isNotified should remain true")
    }

    @Test
    fun `event recipient flags are correct for pending`() = runTest {
        // Arrange
        val recipient = EventRecipient(
            eventId = "event-1",
            userId = "user-1",
            deliveryStatus = DeliveryStatus.PENDING.value,
            notifiedAt = null,
            openedAt = null
        )

        // Act & Assert
        assertFalse(recipient.isNotified, "PENDING recipient should not be notified")
        assertFalse(recipient.isFailed, "PENDING recipient should not be failed")
        assertFalse(recipient.isOpened, "PENDING recipient should not be opened")
    }

    @Test
    fun `invalid enum values return null`() = runTest {
        // Act & Assert
        assertNull(Severity.fromValue("INVALID_SEVERITY"), "Invalid severity should return null")
        assertNull(Category.fromValue("INVALID_CATEGORY"), "Invalid category should return null")
        assertNull(BroadcastType.fromValue("INVALID_TYPE"), "Invalid broadcast type should return null")
        assertNull(DeliveryStatus.fromValue("INVALID_STATUS"), "Invalid delivery status should return null")
    }

    // ==================== Mock Implementations ====================

    class MockUserRepository : UserRepository {
        private val users = mutableMapOf<String, User>()

        override suspend fun createUser(user: User): Result<User> {
            val id = if (user.id.isEmpty()) "user-${System.currentTimeMillis()}" else user.id
            val createdUser = user.copy(id = id)
            users[id] = createdUser
            return Result.success(createdUser)
        }

        override suspend fun getUserById(userId: String): Result<User?> {
            return Result.success(users[userId])
        }

        override suspend fun getUserByPhoneNumber(phoneNumber: String): Result<User?> {
            return Result.success(users.values.find { it.phoneNumber == phoneNumber })
        }

        override suspend fun getUserByDeviceId(deviceId: String): Result<User?> {
            return Result.success(users.values.find { it.deviceId == deviceId })
        }

        override suspend fun updateDisplayName(userId: String, displayName: String): Result<User> {
            val user = users[userId]
            if (user != null) {
                users[userId] = user.copy(displayName = displayName, updatedAt = System.currentTimeMillis())
                return Result.success(users[userId]!!)
            }
            return Result.failure(IllegalArgumentException("User not found"))
        }

        override suspend fun updateFcmToken(userId: String, fcmToken: String): Result<User> {
            val user = users[userId]
            if (user != null) {
                users[userId] = user.copy(fcmToken = fcmToken, updatedAt = System.currentTimeMillis())
                return Result.success(users[userId]!!)
            }
            return Result.failure(IllegalArgumentException("User not found"))
        }

        override suspend fun updateLastActive(userId: String): Result<User> {
            val user = users[userId]
            if (user != null) {
                users[userId] = user.copy(lastActiveAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
                return Result.success(users[userId]!!)
            }
            return Result.failure(IllegalArgumentException("User not found"))
        }

        override suspend fun updateDeviceId(userId: String, deviceId: String): Result<User> {
            val user = users[userId]
            if (user != null) {
                users[userId] = user.copy(deviceId = deviceId, updatedAt = System.currentTimeMillis())
                return Result.success(users[userId]!!)
            }
            return Result.failure(IllegalArgumentException("User not found"))
        }

        override suspend fun deactivateUser(userId: String): Result<Unit> {
            val user = users[userId]
            if (user != null) {
                users[userId] = user.copy(isActive = false, updatedAt = System.currentTimeMillis())
                return Result.success(Unit)
            }
            return Result.failure(IllegalArgumentException("User not found"))
        }

        override suspend fun deleteUser(userId: String): Result<Unit> {
            users.remove(userId)
            return Result.success(Unit)
        }

        fun clear() {
            users.clear()
        }
    }

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
            val filtered = if (includeInactive) {
                events.values.filter { it.creatorId == creatorId }
            } else {
                events.values.filter { it.creatorId == creatorId && it.isActive }
            }
            return Result.success(filtered)
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
            events.keys.filter { events[it]?.expiresAt!! < now }.forEach { events.remove(it) }
            return Result.success(expiredCount)
        }

        fun clear() {
            events.clear()
        }
    }

    class MockUserContactRepository : UserContactRepository {
        private val contacts = mutableMapOf<String, UserContact>()

        override suspend fun createContact(contact: UserContact): Result<UserContact> {
            val id = contact.id.ifEmpty { "contact-${System.currentTimeMillis()}" }
            val createdContact = contact.copy(id = id)
            contacts[id] = createdContact
            return Result.success(createdContact)
        }

        override suspend fun createContacts(contacts: List<UserContact>): Result<List<UserContact>> {
            val created = contacts.map { contact ->
                val id = contact.id.ifEmpty { "contact-${System.currentTimeMillis()}" }
                contact.copy(id = id)
            }
            created.forEach { contacts[it.id] = it }
            return Result.success(created)
        }

        override suspend fun getContactsByUserId(userId: String): Result<List<UserContact>> {
            val userContacts = contacts.values.filter { it.userId == userId }
            return Result.success(userContacts)
        }

        override suspend fun getContactById(userId: String, contactId: String): Result<UserContact?> {
            val contact = contacts[contactId]
            return if (contact?.userId == userId) {
                Result.success(contact)
            } else {
                Result.success(null)
            }
        }

        override suspend fun getContactByPhoneNumber(userId: String, phoneNumber: String): Result<UserContact?> {
            val contact = contacts.values.find { it.userId == userId && it.contactPhoneNumber == phoneNumber }
            return Result.success(contact)
        }

        override suspend fun updateContactDisplayName(contactId: String, displayName: String): Result<UserContact> {
            val contact = contacts[contactId]
            if (contact != null) {
                contacts[contactId] = contact.copy(displayName = displayName, updatedAt = System.currentTimeMillis())
                return Result.success(contacts[contactId]!!)
            }
            return Result.failure(IllegalArgumentException("Contact not found"))
        }

        override suspend fun updateContactAppStatus(
            contactId: String,
            hasApp: Boolean,
            contactUserId: String?
        ): Result<UserContact> {
            val contact = contacts[contactId]
            if (contact != null) {
                contacts[contactId] = contact.copy(
                    hasApp = hasApp,
                    contactUserId = contactUserId,
                    updatedAt = System.currentTimeMillis()
                )
                return Result.success(contacts[contactId]!!)
            }
            return Result.failure(IllegalArgumentException("Contact not found"))
        }

        override suspend fun deleteContact(contactId: String): Result<Unit> {
            contacts.remove(contactId)
            return Result.success(Unit)
        }

        override suspend fun deleteAllContacts(userId: String): Result<Unit> {
            contacts.keys.filter { contacts[it]?.userId == userId }.forEach { contacts.remove(it) }
            return Result.success(Unit)
        }

        override suspend fun syncContacts(userId: String, contacts: List<UserContact>): Result<List<UserContact>> {
            val synced = mutableListOf<UserContact>()
            for (contact in contacts) {
                val existing = contacts.values.find {
                    it.userId == userId && it.contactPhoneNumber == contact.contactPhoneNumber
                }
                if (existing != null) {
                    val id = existing.id
                    contacts[id] = contact.copy(id = id)
                    synced.add(contacts[id]!!)
                } else {
                    val id = contact.id.ifEmpty { "contact-${System.currentTimeMillis()}-${synced.size}" }
                    contacts[id] = contact.copy(id = id)
                    synced.add(contacts[id]!!)
                }
            }
            return Result.success(synced)
        }

        fun clear() {
            contacts.clear()
        }
    }

    class MockEventRecipientRepository : EventRecipientRepository {
        private val recipients = mutableMapOf<String, EventRecipient>()

        override suspend fun createRecipients(recipients: List<EventRecipient>): Result<List<EventRecipient>> {
            val created = recipients.map { recipient ->
                val key = recipient.compositeKey
                recipients[key] = recipient
                recipient
            }
            return Result.success(created)
        }

        override suspend fun getRecipientsByEvent(eventId: String): Result<List<EventRecipient>> {
            val eventRecipients = recipients.values.filter { it.eventId == eventId }
            return Result.success(eventRecipients)
        }

        override suspend fun getRecipient(
            eventId: String,
            userId: String
        ): Result<EventRecipient?> {
            return Result.success(recipients[EventRecipient.compositeKey(eventId, userId)])
        }

        override suspend fun getEventsReceivedByUser(
            userId: String,
            includeExpired: Boolean
        ): Result<List<EventRecipient>> {
            val userRecipients = recipients.values.filter { it.userId == userId }
            return Result.success(userRecipients)
        }

        override suspend fun getPendingRecipients(eventId: String): Result<List<EventRecipient>> {
            val pending = recipients.values.filter {
                it.eventId == eventId && it.deliveryStatusEnum == DeliveryStatus.PENDING
            }
            return Result.success(pending)
        }

        override suspend fun getFailedRecipients(eventId: String): Result<List<EventRecipient>> {
            val failed = recipients.values.filter {
                it.eventId == eventId && it.deliveryStatusEnum == DeliveryStatus.FAILED
            }
            return Result.success(failed)
        }

        override suspend fun updateDeliveryStatus(
            eventId: String,
            userId: String,
            status: DeliveryStatus
        ): Result<EventRecipient> {
            val recipient = recipients[EventRecipient.compositeKey(eventId, userId)]
            if (recipient != null) {
                val now = System.currentTimeMillis()
                val updated = recipient.copy(
                    deliveryStatus = status.value,
                    notifiedAt = if (status == DeliveryStatus.SENT) now else recipient.notifiedAt
                )
                recipients[recipient.compositeKey] = updated
                return Result.success(updated)
            }
            return Result.failure(IllegalArgumentException("Recipient not found"))
        }

        override suspend fun markAsSent(eventId: String, userId: String): Result<EventRecipient> {
            return updateDeliveryStatus(eventId, userId, DeliveryStatus.SENT)
        }

        override suspend fun markAsFailed(eventId: String, userId: String): Result<EventRecipient> {
            return updateDeliveryStatus(eventId, userId, DeliveryStatus.FAILED)
        }

        override suspend fun markAsOpened(eventId: String, userId: String): Result<EventRecipient> {
            val recipient = recipients[EventRecipient.compositeKey(eventId, userId)]
            if (recipient != null) {
                val updated = recipient.copy(openedAt = System.currentTimeMillis())
                recipients[recipient.compositeKey] = updated
                return Result.success(updated)
            }
            return Result.failure(IllegalArgumentException("Recipient not found"))
        }

        override suspend fun deleteRecipient(eventId: String, userId: String): Result<Unit> {
            recipients.remove(EventRecipient.compositeKey(eventId, userId))
            return Result.success(Unit)
        }

        override suspend fun deleteAllRecipientsForEvent(eventId: String): Result<Unit> {
            recipients.keys.filter { it.startsWith("$eventId:") }.forEach { recipients.remove(it) }
            return Result.success(Unit)
        }

        fun clear() {
            recipients.clear()
        }
    }
}
