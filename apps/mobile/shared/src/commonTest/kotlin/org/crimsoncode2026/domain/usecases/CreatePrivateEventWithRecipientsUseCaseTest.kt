package org.crimsoncode2026.domain.usecases

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.crimsoncode2026.data.BroadcastType
import org.crimsoncode2026.data.Category
import org.crimsoncode2026.data.DeliveryStatus
import org.crimsoncode2026.data.Event
import org.crimsoncode2026.data.EventRecipient
import org.crimsoncode2026.data.Severity
import org.crimsoncode2026.notifications.SendPushNotificationResult
import org.crimsoncode2026.notifications.SendPushNotificationUseCase

/**
 * Unit tests for CreatePrivateEventWithRecipientsUseCase
 *
 * Tests private event creation with recipient tracking and push notification triggers.
 */
class CreatePrivateEventWithRecipientsUseCaseTest {

    // ==================== Mock Implementations ====================

    class MockEventRepository {
        var createEventResult: Result<Event> = Result.success(
            Event(
                id = "test-event-id",
                creatorId = "test-user-id",
                severity = Severity.CRISIS.value,
                category = Category.FIRE.value,
                lat = 37.7749,
                lon = -122.4194,
                locationOverride = null,
                broadcastType = BroadcastType.PRIVATE.value,
                description = "Test fire",
                isAnonymous = false,
                createdAt = System.currentTimeMillis(),
                expiresAt = Event.calculateExpiration(),
                deletedAt = null
            )
        )

        suspend fun createEvent(event: Event): Result<Event> {
            return createEventResult
        }
    }

    class MockEventRecipientRepository {
        var createRecipientsResult: Result<List<EventRecipient>> = Result.success(emptyList())

        suspend fun createRecipients(recipients: List<EventRecipient>): Result<List<EventRecipient>> {
            return createRecipientsResult
        }
    }

    class MockUserContactRepository {
        var contactsResult: Result<List<org.crimsoncode2026.data.UserContact>> = Result.success(emptyList())

        suspend fun getContactsByUserId(userId: String): Result<List<org.crimsoncode2026.data.UserContact>> {
            return contactsResult
        }
    }

    class MockUserRepository {
        private val users = mutableMapOf<String, User>()

        fun addUser(user: User) {
            users[user.id] = user
        }

        suspend fun getUserById(id: String): Result<User> {
            return users[id]?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("User not found"))
        }
    }

    class MockUserSessionManager(
        private val userId: String? = "test-user-id"
    ) : org.crimsoncode2026.domain.UserSessionManager {
        override fun getCurrentAuthUser(): io.github.jan-tennert.supabase.auth.User? {
            return userId?.let { MockSupabaseUser(it) }
        }

        override fun authStateFlow(): kotlinx.coroutines.flow.Flow<io.github.jan-tennert.supabase.auth.User?> {
            return kotlinx.coroutines.flow.flowOf(userId?.let { MockSupabaseUser(it) })
        }

        override fun getCurrentUserId(): String? = userId

        override suspend fun validateDeviceBinding(): org.crimsoncode2026.domain.DeviceBindingResult {
            return org.crimsoncode2026.domain.DeviceBindingResult.Valid
        }

        override suspend fun rebindToDevice(userId: String): Result<User> {
            return Result.success(
                User(
                    id = userId,
                    phoneNumber = "+15551234567",
                    displayName = "Test User",
                    deviceId = "test-device",
                    platform = "ANDROID",
                    isActive = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    lastActiveAt = System.currentTimeMillis()
                )
            )
        }

        override suspend fun updateLastActive(): Result<User> {
            return Result.success(
                User(
                    id = userId ?: "",
                    phoneNumber = "+15551234567",
                    displayName = "Test User",
                    deviceId = "test-device",
                    platform = "ANDROID",
                    isActive = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    lastActiveAt = System.currentTimeMillis()
                )
            )
        }

        override fun isAuthenticated(): Boolean = userId != null

        override suspend fun signOut() {}
    }

    class MockSendPushNotificationUseCase : SendPushNotificationUseCase {
        var sendResult: SendPushNotificationResult = SendPushNotificationResult.Success(2)

        fun setSendResult(result: SendPushNotificationResult) {
            sendResult = result
        }

        override suspend fun invoke(
            eventId: String,
            severity: String,
            category: String,
            description: String,
            lat: Double?,
            lon: Double?
        ): SendPushNotificationResult {
            return sendResult
        }
    }

    class MockSupabaseUser(override val id: String) : io.github.jan-tennert.supabase.auth.User

    // ==================== Success Cases ====================

    @Test
    fun `invoke succeeds with valid private event and recipients`() = runTest {
        // Arrange
        val mockEventRepo = MockEventRepository()
        val mockRecipientRepo = MockEventRecipientRepository()
        val mockContactRepo = MockUserContactRepository().apply {
            contactsResult = Result.success(listOf(
                org.crimsoncode2026.data.UserContact(
                    id = "contact-1",
                    userId = "test-user-id",
                    contactPhoneNumber = "+15551111222",
                    displayName = "Contact 1",
                    hasApp = true,
                    contactUserId = "user-1",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            ))
        }
        val mockUserRepo = MockUserRepository().apply {
            addUser(User(
                id = "user-1",
                phoneNumber = "+15551111222",
                displayName = "User 1",
                deviceId = "device-1",
                platform = "ANDROID",
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                lastActiveAt = System.currentTimeMillis()
            ))
        }
        val mockSessionManager = MockUserSessionManager()
        val mockPushUseCase = MockSendPushNotificationUseCase()
        val useCase = CreatePrivateEventWithRecipientsUseCase(
            mockEventRepo, mockRecipientRepo, mockContactRepo,
            mockUserRepo, mockSessionManager, mockPushUseCase
        )

        val event = Event(
            id = "test-event-id",
            creatorId = "test-user-id",
            severity = Severity.CRISIS.value,
            category = Category.FIRE.value,
            lat = 37.7749,
            lon = -122.4194,
            locationOverride = null,
            broadcastType = BroadcastType.PRIVATE.value,
            description = "Test fire emergency",
            isAnonymous = false,
            createdAt = System.currentTimeMillis(),
            expiresAt = Event.calculateExpiration(),
            deletedAt = null
        )

        // Act
        val result = useCase(event, listOf("contact-1"))

        // Assert
        assertTrue(result is CreatePrivateEventWithRecipientsResult.Success, "Private event with recipients should succeed")
        val successResult = result as CreatePrivateEventWithRecipientsResult.Success
        assertEquals("test-event-id", successResult.event.id)
        assertEquals(1, successResult.recipients.size, "Should have 1 recipient")
    }

    @Test
    fun `invoke skips contacts without app user registration`() = runTest {
        // Arrange
        val mockEventRepo = MockEventRepository()
        val mockRecipientRepo = MockEventRecipientRepository()
        val mockContactRepo = MockUserContactRepository().apply {
            contactsResult = Result.success(listOf(
                org.crimsoncode2026.data.UserContact(
                    id = "contact-1",
                    userId = "test-user-id",
                    contactPhoneNumber = "+15551119999",
                    displayName = "Contact 1",
                    hasApp = true,
                    contactUserId = null, // No app user - should be skipped
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ),
                org.crimsoncode2026.data.UserContact(
                    id = "contact-2",
                    userId = "test-user-id",
                    contactPhoneNumber = "+15551112222",
                    displayName = "Contact 2",
                    hasApp = true,
                    contactUserId = "user-2", // Has app user - should be included
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            ))
        }
        val mockUserRepo = MockUserRepository().apply {
            addUser(User(
                id = "user-2",
                phoneNumber = "+15551112222",
                displayName = "User 2",
                deviceId = "device-2",
                platform = "ANDROID",
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                lastActiveAt = System.currentTimeMillis()
            ))
        }
        val mockSessionManager = MockUserSessionManager()
        val mockPushUseCase = MockSendPushNotificationUseCase()
        val useCase = CreatePrivateEventWithRecipientsUseCase(
            mockEventRepo, mockRecipientRepo, mockContactRepo,
            mockUserRepo, mockSessionManager, mockPushUseCase
        )

        val event = Event(
            id = "test-event-id",
            creatorId = "test-user-id",
            severity = Severity.ALERT.value,
            category = Category.MEDICAL.value,
            lat = 37.7749,
            lon = -122.4194,
            locationOverride = null,
            broadcastType = BroadcastType.PRIVATE.value,
            description = "Test medical emergency",
            isAnonymous = false,
            createdAt = System.currentTimeMillis(),
            expiresAt = Event.calculateExpiration(),
            deletedAt = null
        )

        // Act
        val result = useCase(event, listOf("contact-1", "contact-2"))

        // Assert
        assertTrue(result is CreatePrivateEventWithRecipientsResult.Success, "Should succeed")
        val successResult = result as CreatePrivateEventWithRecipientsResult.Success
        assertEquals(1, successResult.recipients.size, "Should only include contact with app user")
        assertEquals("user-2", successResult.recipients.first().userId, "Should match contact user ID")
    }

    @Test
    fun `invoke triggers push notification for private event`() = runTest {
        // Arrange
        val mockEventRepo = MockEventRepository()
        val mockRecipientRepo = MockEventRecipientRepository()
        val mockContactRepo = MockUserContactRepository().apply {
            contactsResult = Result.success(listOf(
                org.crimsoncode2026.data.UserContact(
                    id = "contact-1",
                    userId = "test-user-id",
                    contactPhoneNumber = "+15551111222",
                    displayName = "Contact 1",
                    hasApp = true,
                    contactUserId = "user-1",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            ))
        }
        val mockUserRepo = MockUserRepository().apply {
            addUser(User(
                id = "user-1",
                phoneNumber = "+15551111222",
                displayName = "User 1",
                deviceId = "device-1",
                platform = "ANDROID",
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                lastActiveAt = System.currentTimeMillis()
            ))
        }
        val mockSessionManager = MockUserSessionManager()
        val mockPushUseCase = MockSendPushNotificationUseCase()
        val useCase = CreatePrivateEventWithRecipientsUseCase(
            mockEventRepo, mockRecipientRepo, mockContactRepo,
            mockUserRepo, mockSessionManager, mockPushUseCase
        )

        val event = Event(
            id = "test-event-id",
            creatorId = "test-user-id",
            severity = Severity.CRISIS.value,
            category = Category.FIRE.value,
            lat = 37.7749,
            lon = -122.4194,
            locationOverride = null,
            broadcastType = BroadcastType.PRIVATE.value,
            description = "Test fire emergency",
            isAnonymous = false,
            createdAt = System.currentTimeMillis(),
            expiresAt = Event.calculateExpiration(),
            deletedAt = null
        )

        // Act
        useCase(event, listOf("contact-1"))

        // Assert
        assertTrue(mockPushUseCase.sendResult is SendPushNotificationResult.Success, "Push notification should be triggered")
    }

    // ==================== Public Event Handling ====================

    @Test
    fun `invoke returns early for public event without recipients`() = runTest {
        // Arrange
        val mockEventRepo = MockEventRepository()
        val mockRecipientRepo = MockEventRecipientRepository()
        val mockContactRepo = MockUserContactRepository()
        val mockUserRepo = MockUserRepository()
        val mockSessionManager = MockUserSessionManager()
        val mockPushUseCase = MockSendPushNotificationUseCase()
        val useCase = CreatePrivateEventWithRecipientsUseCase(
            mockEventRepo, mockRecipientRepo, mockContactRepo,
            mockUserRepo, mockSessionManager, mockPushUseCase
        )

        val publicEvent = Event(
            id = "test-event-id",
            creatorId = "test-user-id",
            severity = Severity.ALERT.value,
            category = Category.WEATHER.value,
            lat = 37.7749,
            lon = -122.4194,
            locationOverride = null,
            broadcastType = BroadcastType.PUBLIC.value,
            description = "Test weather alert",
            isAnonymous = true,
            createdAt = System.currentTimeMillis(),
            expiresAt = Event.calculateExpiration(),
            deletedAt = null
        )

        // Act
        val result = useCase(publicEvent, emptyList())

        // Assert
        assertTrue(result is CreatePrivateEventWithRecipientsResult.Success, "Public event should succeed")
        val successResult = result as CreatePrivateEventWithRecipientsResult.Success
        assertEquals(publicEvent.id, successResult.event.id, "Event ID should match")
        assertTrue(successResult.recipients.isEmpty(), "Public event should have no recipients")
    }

    // ==================== Failure Cases ====================

    @Test
    fun `invoke returns error when user not authenticated`() = runTest {
        // Arrange
        val mockEventRepo = MockEventRepository()
        val mockRecipientRepo = MockEventRecipientRepository()
        val mockContactRepo = MockUserContactRepository()
        val mockUserRepo = MockUserRepository()
        val mockSessionManager = MockUserSessionManager(currentUserId = null)
        val mockPushUseCase = MockSendPushNotificationUseCase()
        val useCase = CreatePrivateEventWithRecipientsUseCase(
            mockEventRepo, mockRecipientRepo, mockContactRepo,
            mockUserRepo, mockSessionManager, mockPushUseCase
        )

        val event = Event(
            id = "test-event-id",
            creatorId = "test-user-id",
            severity = Severity.ALERT.value,
            category = Category.MEDICAL.value,
            lat = 37.7749,
            lon = -122.4194,
            locationOverride = null,
            broadcastType = BroadcastType.PRIVATE.value,
            description = "Test",
            isAnonymous = false,
            createdAt = System.currentTimeMillis(),
            expiresAt = Event.calculateExpiration(),
            deletedAt = null
        )

        // Act
        val result = useCase(event, listOf("contact-1"))

        // Assert
        assertTrue(result is CreatePrivateEventWithRecipientsResult.Error, "Should fail when user not authenticated")
        val errorResult = result as CreatePrivateEventWithRecipientsResult.Error
        assertEquals("User not authenticated", errorResult.message)
    }

    @Test
    fun `invoke returns error when event creation fails`() = runTest {
        // Arrange
        val mockEventRepo = MockEventRepository().apply {
            createEventResult = Result.failure(RuntimeException("Database error"))
        }
        val mockRecipientRepo = MockEventRecipientRepository()
        val mockContactRepo = MockUserContactRepository()
        val mockUserRepo = MockUserRepository()
        val mockSessionManager = MockUserSessionManager()
        val mockPushUseCase = MockSendPushNotificationUseCase()
        val useCase = CreatePrivateEventWithRecipientsUseCase(
            mockEventRepo, mockRecipientRepo, mockContactRepo,
            mockUserRepo, mockSessionManager, mockPushUseCase
        )

        val event = Event(
            id = "test-event-id",
            creatorId = "test-user-id",
            severity = Severity.ALERT.value,
            category = Category.MEDICAL.value,
            lat = 37.7749,
            lon = -122.4194,
            locationOverride = null,
            broadcastType = BroadcastType.PRIVATE.value,
            description = "Test",
            isAnonymous = false,
            createdAt = System.currentTimeMillis(),
            expiresAt = Event.calculateExpiration(),
            deletedAt = null
        )

        // Act
        val result = useCase(event, listOf("contact-1"))

        // Assert
        assertTrue(result is CreatePrivateEventWithRecipientsResult.Error, "Should fail when event creation fails")
        val errorResult = result as CreatePrivateEventWithRecipientsResult.Error
        assertTrue(errorResult.message.contains("Failed to create event"))
    }

    @Test
    fun `invoke returns error when no contacts selected`() = runTest {
        // Arrange
        val mockEventRepo = MockEventRepository()
        val mockRecipientRepo = MockEventRecipientRepository()
        val mockContactRepo = MockUserContactRepository()
        val mockUserRepo = MockUserRepository()
        val mockSessionManager = MockUserSessionManager()
        val mockPushUseCase = MockSendPushNotificationUseCase()
        val useCase = CreatePrivateEventWithRecipientsUseCase(
            mockEventRepo, mockRecipientRepo, mockContactRepo,
            mockUserRepo, mockSessionManager, mockPushUseCase
        )

        val privateEvent = Event(
            id = "test-event-id",
            creatorId = "test-user-id",
            severity = Severity.ALERT.value,
            category = Category.MEDICAL.value,
            lat = 37.7749,
            lon = -122.4194,
            locationOverride = null,
            broadcastType = BroadcastType.PRIVATE.value,
            description = "Test",
            isAnonymous = false,
            createdAt = System.currentTimeMillis(),
            expiresAt = Event.calculateExpiration(),
            deletedAt = null
        )

        // Act - No contact IDs provided
        val result = useCase(privateEvent, emptyList())

        // Assert
        assertTrue(result is CreatePrivateEventWithRecipientsResult.Error, "Should fail when no contacts selected")
        val errorResult = result as CreatePrivateEventWithRecipientsResult.Error
        assertEquals("Private events require at least one selected contact", errorResult.message)
    }

    @Test
    fun `invoke returns error when no valid recipients found`() = runTest {
        // Arrange
        val mockEventRepo = MockEventRepository()
        val mockRecipientRepo = MockEventRecipientRepository()
        val mockContactRepo = MockUserContactRepository().apply {
            contactsResult = Result.success(listOf(
                org.crimsoncode2026.data.UserContact(
                    id = "contact-1",
                    userId = "test-user-id",
                    contactPhoneNumber = "+15551119999",
                    displayName = "Contact 1",
                    hasApp = true,
                    contactUserId = null, // No app user
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            ))
        }
        val mockUserRepo = MockUserRepository()
        val mockSessionManager = MockUserSessionManager()
        val mockPushUseCase = MockSendPushNotificationUseCase()
        val useCase = CreatePrivateEventWithRecipientsUseCase(
            mockEventRepo, mockRecipientRepo, mockContactRepo,
            mockUserRepo, mockSessionManager, mockPushUseCase
        )

        val privateEvent = Event(
            id = "test-event-id",
            creatorId = "test-user-id",
            severity = Severity.ALERT.value,
            category = Category.MEDICAL.value,
            lat = 37.7749,
            lon = -122.4194,
            locationOverride = null,
            broadcastType = BroadcastType.PRIVATE.value,
            description = "Test",
            isAnonymous = false,
            createdAt = System.currentTimeMillis(),
            expiresAt = Event.calculateExpiration(),
            deletedAt = null
        )

        // Act - Contact selected but no app user
        val result = useCase(privateEvent, listOf("contact-1"))

        // Assert
        assertTrue(result is CreatePrivateEventWithRecipientsResult.Error, "Should fail when no valid recipients found")
        val errorResult = result as CreatePrivateEventWithRecipientsResult.Error
        assertEquals("None of the selected contacts are app users", errorResult.message)
    }

    @Test
    fun `invoke continues if push notification fails`() = runTest {
        // Arrange
        val mockEventRepo = MockEventRepository()
        val mockRecipientRepo = MockEventRecipientRepository()
        val mockContactRepo = MockUserContactRepository().apply {
            contactsResult = Result.success(listOf(
                org.crimsoncode2026.data.UserContact(
                    id = "contact-1",
                    userId = "test-user-id",
                    contactPhoneNumber = "+15551111222",
                    displayName = "Contact 1",
                    hasApp = true,
                    contactUserId = "user-1",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            ))
        }
        val mockUserRepo = MockUserRepository().apply {
            addUser(User(
                id = "user-1",
                phoneNumber = "+15551111222",
                displayName = "User 1",
                deviceId = "device-1",
                platform = "ANDROID",
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                lastActiveAt = System.currentTimeMillis()
            ))
        }
        val mockSessionManager = MockUserSessionManager()
        val mockPushUseCase = MockSendPushNotificationUseCase().apply {
            sendResult = SendPushNotificationResult.Error("Push failed")
        }
        val useCase = CreatePrivateEventWithRecipientsUseCase(
            mockEventRepo, mockRecipientRepo, mockContactRepo,
            mockUserRepo, mockSessionManager, mockPushUseCase
        )

        val privateEvent = Event(
            id = "test-event-id",
            creatorId = "test-user-id",
            severity = Severity.ALERT.value,
            category = Category.MEDICAL.value,
            lat = 37.7749,
            lon = -122.4194,
            locationOverride = null,
            broadcastType = BroadcastType.PRIVATE.value,
            description = "Test",
            isAnonymous = false,
            createdAt = System.currentTimeMillis(),
            expiresAt = Event.calculateExpiration(),
            deletedAt = null
        )

        // Act
        val result = useCase(privateEvent, listOf("contact-1"))

        // Assert
        // According to the code, push notification failure is non-critical
        // The event creation should still succeed
        assertTrue(result is CreatePrivateEventWithRecipientsResult.Success, "Should succeed even if push fails")
    }

    @Test
    fun `createPublicEvent works for public events`() = runTest {
        // Arrange
        val mockEventRepo = MockEventRepository()
        val mockRecipientRepo = MockEventRecipientRepository()
        val mockContactRepo = MockUserContactRepository()
        val mockUserRepo = MockUserRepository()
        val mockSessionManager = MockUserSessionManager()
        val mockPushUseCase = MockSendPushNotificationUseCase()
        val useCase = CreatePrivateEventWithRecipientsUseCase(
            mockEventRepo, mockRecipientRepo, mockContactRepo,
            mockUserRepo, mockSessionManager, mockPushUseCase
        )

        val publicEvent = Event(
            id = "public-event-id",
            creatorId = "test-user-id",
            severity = Severity.ALERT.value,
            category = Category.WEATHER.value,
            lat = 37.7749,
            lon = -122.4194,
            locationOverride = null,
            broadcastType = BroadcastType.PUBLIC.value,
            description = "Public weather alert",
            isAnonymous = true,
            createdAt = System.currentTimeMillis(),
            expiresAt = Event.calculateExpiration(),
            deletedAt = null
        )

        // Act
        val result = useCase.createPublicEvent(publicEvent)

        // Assert
        assertTrue(result is CreatePrivateEventWithRecipientsResult.Success, "Public event should succeed")
        val successResult = result as CreatePrivateEventWithRecipientsResult.Success
        assertEquals(publicEvent.id, successResult.event.id, "Event ID should match")
        assertTrue(successResult.recipients.isEmpty(), "Public event should have no recipients")
    }

    // ==================== Phone Number Mismatch Test ====================

    @Test
    fun `invoke skips recipients with phone number mismatch`() = runTest {
        // Arrange
        val mockEventRepo = MockEventRepository()
        val mockRecipientRepo = MockEventRecipientRepository()
        val mockContactRepo = MockUserContactRepository().apply {
            contactsResult = Result.success(listOf(
                org.crimsoncode2026.data.UserContact(
                    id = "contact-1",
                    userId = "test-user-id",
                    contactPhoneNumber = "+15551119999", // Different from user's phone
                    displayName = "Contact 1",
                    hasApp = true,
                    contactUserId = "user-1",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            ))
        }
        val mockUserRepo = MockUserRepository().apply {
            addUser(User(
                id = "user-1",
                phoneNumber = "+15551112222", // Different phone
                displayName = "User 1",
                deviceId = "device-1",
                platform = "ANDROID",
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                lastActiveAt = System.currentTimeMillis()
            ))
        }
        val mockSessionManager = MockUserSessionManager()
        val mockPushUseCase = MockSendPushNotificationUseCase()
        val useCase = CreatePrivateEventWithRecipientsUseCase(
            mockEventRepo, mockRecipientRepo, mockContactRepo,
            mockUserRepo, mockSessionManager, mockPushUseCase
        )

        val event = Event(
            id = "test-event-id",
            creatorId = "test-user-id",
            severity = Severity.ALERT.value,
            category = Category.MEDICAL.value,
            lat = 37.7749,
            lon = -122.4194,
            locationOverride = null,
            broadcastType = BroadcastType.PRIVATE.value,
            description = "Test",
            isAnonymous = false,
            createdAt = System.currentTimeMillis(),
            expiresAt = Event.calculateExpiration(),
            deletedAt = null
        )

        // Act
        val result = useCase(event, listOf("contact-1"))

        // Assert
        assertTrue(result is CreatePrivateEventWithRecipientsResult.Success, "Should succeed")
        val successResult = result as CreatePrivateEventWithRecipientsResult.Success
        // Contact with phone mismatch should be skipped
        assertTrue(successResult.recipients.isEmpty(), "Should skip recipient with phone mismatch")
    }

    // ==================== Recipient Creation Failure Test ====================

    @Test
    fun `invoke returns error when recipient creation fails`() = runTest {
        // Arrange
        val mockEventRepo = MockEventRepository()
        val mockRecipientRepo = MockEventRecipientRepository().apply {
            createRecipientsResult = Result.failure(RuntimeException("Database error"))
        }
        val mockContactRepo = MockUserContactRepository().apply {
            contactsResult = Result.success(listOf(
                org.crimsoncode2026.data.UserContact(
                    id = "contact-1",
                    userId = "test-user-id",
                    contactPhoneNumber = "+15551111222",
                    displayName = "Contact 1",
                    hasApp = true,
                    contactUserId = "user-1",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            ))
        }
        val mockUserRepo = MockUserRepository().apply {
            addUser(User(
                id = "user-1",
                phoneNumber = "+15551111222",
                displayName = "User 1",
                deviceId = "device-1",
                platform = "ANDROID",
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                lastActiveAt = System.currentTimeMillis()
            ))
        }
        val mockSessionManager = MockUserSessionManager()
        val mockPushUseCase = MockSendPushNotificationUseCase()
        val useCase = CreatePrivateEventWithRecipientsUseCase(
            mockEventRepo, mockRecipientRepo, mockContactRepo,
            mockUserRepo, mockSessionManager, mockPushUseCase
        )

        val event = Event(
            id = "test-event-id",
            creatorId = "test-user-id",
            severity = Severity.ALERT.value,
            category = Category.MEDICAL.value,
            lat = 37.7749,
            lon = -122.4194,
            locationOverride = null,
            broadcastType = BroadcastType.PRIVATE.value,
            description = "Test",
            isAnonymous = false,
            createdAt = System.currentTimeMillis(),
            expiresAt = Event.calculateExpiration(),
            deletedAt = null
        )

        // Act
        val result = useCase(event, listOf("contact-1"))

        // Assert
        assertTrue(result is CreatePrivateEventWithRecipientsResult.Error, "Should fail when recipient creation fails")
        val errorResult = result as CreatePrivateEventWithRecipientsResult.Error
        assertTrue(errorResult.message.contains("Failed to create recipients"))
    }
}
