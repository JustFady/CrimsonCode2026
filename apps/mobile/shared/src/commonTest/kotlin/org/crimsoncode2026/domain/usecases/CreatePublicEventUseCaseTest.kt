package org.crimsoncode2026.domain.usecases

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.crimsoncode2026.data.BroadcastType
import org.crimsoncode2026.data.Category
import org.crimsoncode2026.data.Event
import org.crimsoncode2026.data.Severity

/**
 * Unit tests for CreatePublicEventUseCase
 *
 * Tests public event creation including validation, expiration calculation,
 * and repository interaction.
 */
class CreatePublicEventUseCaseTest {

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
                broadcastType = BroadcastType.PUBLIC.value,
                description = "Test fire",
                isAnonymous = true,
                createdAt = System.currentTimeMillis(),
                expiresAt = Event.calculateExpiration(),
                deletedAt = null
            )
        )

        suspend fun createEvent(event: Event): Result<Event> {
            return createEventResult
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

        override suspend fun rebindToDevice(userId: String): Result<org.crimsoncode2026.data.User> {
            return Result.success(
                org.crimsoncode2026.data.User(
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

        override suspend fun updateLastActive(): Result<org.crimsoncode2026.data.User> {
            return Result.success(
                org.crimsoncode2026.data.User(
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

    class MockSupabaseUser(override val id: String) : io.github.jan-tennert.supabase.auth.User

    // ==================== Success Cases ====================

    @Test
    fun `invoke with params creates public event`() = runTest {
        // Arrange
        val mockEventRepo = MockEventRepository()
        val mockSessionManager = MockUserSessionManager()
        val useCase = CreatePublicEventUseCase(mockEventRepo, mockSessionManager)

        // Act
        val result = useCase(
            severity = Severity.CRISIS,
            category = Category.FIRE,
            lat = 37.7749,
            lon = -122.4194,
            description = "Test fire emergency"
        )

        // Assert
        assertTrue(result is CreatePublicEventResult.Success, "Should create public event successfully")
        val event = (result as CreatePublicEventResult.Success).event
        assertNotNull(event.id)
        assertEquals("test-user-id", event.creatorId)
        assertEquals(Severity.CRISIS.value, event.severity)
        assertEquals(Category.FIRE.value, event.category)
        assertEquals(37.7749, event.lat, 0.001)
        assertEquals(-122.4194, event.lon, 0.001)
        assertEquals(BroadcastType.PUBLIC.value, event.broadcastType)
        assertEquals("Test fire emergency", event.description)
        assertTrue(event.isAnonymous, "Public events should be anonymous")
        assertTrue(event.expiresAt > System.currentTimeMillis(), "Should have future expiration")
    }

    @Test
    fun `invoke with location override creates public event`() = runTest {
        // Arrange
        val mockEventRepo = MockEventRepository()
        val mockSessionManager = MockUserSessionManager()
        val useCase = CreatePublicEventUseCase(mockEventRepo, mockSessionManager)

        // Act
        val result = useCase(
            severity = Severity.ALERT,
            category = Category.MEDICAL,
            lat = 37.7749,
            lon = -122.4194,
            locationOverride = "Manual entered location",
            description = "Medical emergency"
        )

        // Assert
        assertTrue(result is CreatePublicEventResult.Success, "Should create public event with location override")
        val event = (result as CreatePublicEventResult.Success).event
        assertEquals("Manual entered location", event.locationOverride)
    }

    @Test
    fun `invoke calculates 48 hour expiration`() = runTest {
        // Arrange
        val mockEventRepo = MockEventRepository()
        val mockSessionManager = MockUserSessionManager()
        val useCase = CreatePublicEventUseCase(mockEventRepo, mockSessionManager)

        val now = System.currentTimeMillis()
        val expectedExpiration = now + Event.DEFAULT_EXPIRATION_MS

        // Act
        val result = useCase(
            severity = Severity.ALERT,
            category = Category.TRAFFIC,
            lat = 37.7749,
            lon = -122.4194,
            description = "Traffic accident"
        )

        // Assert
        assertTrue(result is CreatePublicEventResult.Success)
        val event = (result as CreatePublicEventResult.Success).event
        assertEquals(expectedExpiration, event.expiresAt, 1000, "Expiration should be 48 hours from now")
    }

    // ==================== Failure Cases ====================

    @Test
    fun `invoke returns error when user not authenticated`() = runTest {
        // Arrange
        val mockEventRepo = MockEventRepository()
        val mockSessionManager = MockUserSessionManager(currentUserId = null)
        val useCase = CreatePublicEventUseCase(mockEventRepo, mockSessionManager)

        // Act
        val result = useCase(
            severity = Severity.ALERT,
            category = Category.WEATHER,
            lat = 37.7749,
            lon = -122.4194,
            description = "Storm warning"
        )

        // Assert
        assertTrue(result is CreatePublicEventResult.Error, "Should fail when user not authenticated")
        val error = result as CreatePublicEventResult.Error
        assertEquals("User not authenticated", error.message)
    }

    @Test
    fun `invoke returns error when repository fails`() = runTest {
        // Arrange
        val mockEventRepo = MockEventRepository().apply {
            createEventResult = Result.failure(RuntimeException("Database error"))
        }
        val mockSessionManager = MockUserSessionManager()
        val useCase = CreatePublicEventUseCase(mockEventRepo, mockSessionManager)

        // Act
        val result = useCase(
            severity = Severity.ALERT,
            category = Category.WEATHER,
            lat = 37.7749,
            lon = -122.4194,
            description = "Storm warning"
        )

        // Assert
        assertTrue(result is CreatePublicEventResult.Error, "Should fail when repository fails")
        val error = result as CreatePublicEventResult.Error
        assertTrue(error.message.contains("Failed to create event"), "Error should indicate failure")
    }

    @Test
    fun `invoke returns error when repository returns null`() = runTest {
        // Arrange
        val mockEventRepo = MockEventRepository().apply {
            createEventResult = Result.success(null)
        }
        val mockSessionManager = MockUserSessionManager()
        val useCase = CreatePublicEventUseCase(mockEventRepo, mockSessionManager)

        // Act
        val result = useCase(
            severity = Severity.ALERT,
            category = Category.WEATHER,
            lat = 37.7749,
            lon = -122.4194,
            description = "Storm warning"
        )

        // Assert
        assertTrue(result is CreatePublicEventResult.Error, "Should fail when repository returns null")
        val error = result as CreatePublicEventResult.Error
        assertEquals("Failed to create event", error.message)
    }

    // ==================== Event Object Tests ====================

    @Test
    fun `invoke with Event object uses provided expiration`() = runTest {
        // Arrange
        val customExpiration = System.currentTimeMillis() + 1000000L
        val mockEventRepo = MockEventRepository()
        val mockSessionManager = MockUserSessionManager()
        val useCase = CreatePublicEventUseCase(mockEventRepo, mockSessionManager)

        val event = Event(
            id = "custom-event-id",
            creatorId = "test-user-id",
            severity = Severity.ALERT.value,
            category = Category.OTHER.value,
            lat = 37.7749,
            lon = -122.4194,
            locationOverride = null,
            broadcastType = BroadcastType.PUBLIC.value,
            description = "Test",
            isAnonymous = false,
            createdAt = System.currentTimeMillis(),
            expiresAt = customExpiration // Custom expiration
        )

        // Act
        val result = useCase(event)

        // Assert
        assertTrue(result is CreatePublicEventResult.Success)
        val createdEvent = (result as CreatePublicEventResult.Success).event
        assertEquals(customExpiration, createdEvent.expiresAt, "Should use provided expiration")
    }

    @Test
    fun `invoke with Event object overrides isAnonymous`() = runTest {
        // Arrange
        val mockEventRepo = MockEventRepository()
        val mockSessionManager = MockUserSessionManager()
        val useCase = CreatePublicEventUseCase(mockEventRepo, mockSessionManager)

        val event = Event(
            id = "test-event-id",
            creatorId = "test-user-id",
            severity = Severity.ALERT.value,
            category = Category.OTHER.value,
            lat = 37.7749,
            lon = -122.4194,
            locationOverride = null,
            broadcastType = "NOT_PUBLIC", // Wrong broadcast type
            description = "Test",
            isAnonymous = false, // Wrong anonymity
            createdAt = System.currentTimeMillis(),
            expiresAt = 0 // Will be calculated
        )

        // Act
        val result = useCase(event)

        // Assert
        assertTrue(result is CreatePublicEventResult.Success)
        val createdEvent = (result as CreatePublicEventResult.Success).event
        assertEquals(BroadcastType.PUBLIC.value, createdEvent.broadcastType, "Should override to PUBLIC")
        assertTrue(createdEvent.isAnonymous, "Should override isAnonymous to true")
    }

    @Test
    fun `invoke with Event object overrides createdAt if not set`() = runTest {
        // Arrange
        val customCreatedAt = 1234567890L
        val mockEventRepo = MockEventRepository()
        val mockSessionManager = MockUserSessionManager()
        val useCase = CreatePublicEventUseCase(mockEventRepo, mockSessionManager)

        val event = Event(
            id = "test-event-id",
            creatorId = "test-user-id",
            severity = Severity.ALERT.value,
            category = Category.OTHER.value,
            lat = 37.7749,
            lon = -122.4194,
            locationOverride = null,
            broadcastType = BroadcastType.PUBLIC.value,
            description = "Test",
            isAnonymous = true,
            createdAt = customCreatedAt,
            expiresAt = Event.calculateExpiration(customCreatedAt) // Will be overridden
        )

        // Act
        val result = useCase(event)

        // Assert
        assertTrue(result is CreatePublicEventResult.Success)
        val createdEvent = (result as CreatePublicEventResult.Success).event
        assertEquals(customCreatedAt, createdEvent.createdAt, "Should preserve createdAt if set")
    }

    @Test
    fun `invoke with Event object sets creatorId`() = runTest {
        // Arrange
        val customCreatorId = "custom-creator-id"
        val mockEventRepo = MockEventRepository()
        val mockSessionManager = MockUserSessionManager(currentUserId = "session-user-id")
        val useCase = CreatePublicEventUseCase(mockEventRepo, mockSessionManager)

        val event = Event(
            id = "test-event-id",
            creatorId = customCreatorId, // Will be overridden
            severity = Severity.ALERT.value,
            category = Category.OTHER.value,
            lat = 37.7749,
            lon = -122.4194,
            locationOverride = null,
            broadcastType = BroadcastType.PUBLIC.value,
            description = "Test",
            isAnonymous = true,
            createdAt = System.currentTimeMillis(),
            expiresAt = 0
        )

        // Act
        val result = useCase(event)

        // Assert
        assertTrue(result is CreatePublicEventResult.Success)
        val createdEvent = (result as CreatePublicEventResult.Success).event
        assertEquals("session-user-id", createdEvent.creatorId, "Should use session user ID as creator")
    }
}
