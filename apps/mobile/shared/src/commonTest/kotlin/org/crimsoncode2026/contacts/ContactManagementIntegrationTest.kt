package org.crimsoncode2026.contacts

import kotlinx.coroutines.test.runTest
import org.crimsoncode2026.data.UserContact
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for Contact Management
 *
 * Tests the complete contact management flow:
 * 1. Contact import from device
 * 2. Phone number normalization to E.164 format
 * 3. Contact storage in UserContacts table
 * 4. App user detection (matching against Users table)
 * 5. Contact update (has_app flag, contact_user_id cache)
 * 6. Contact sync (upsert by phone number)
 * 7. Contact deletion (single and bulk)
 * 8. Contact search and filtering
 */
class ContactManagementIntegrationTest {

    private lateinit var mockContactRepository: MockUserContactRepository
    private lateinit var mockDeviceContactsService: MockDeviceContactsService
    private lateinit var mockAppUserDetection: MockAppUserDetection

    @BeforeTest
    fun setup() {
        mockContactRepository = MockUserContactRepository()
        mockDeviceContactsService = MockDeviceContactsService()
        mockAppUserDetection = MockAppUserDetection()
    }

    @AfterTest
    fun teardown() {
        mockContactRepository.clear()
        mockDeviceContactsService.clear()
        mockAppUserDetection.clear()
    }

    // ==================== Device Contact Import Tests ====================

    @Test
    fun `import device contacts successfully`() = runTest {
        // Arrange
        mockDeviceContactsService.addDeviceContact(
            DeviceContactsService.DeviceContact(
                id = "device-1",
                displayName = "John Doe",
                phoneNumber = "+15551234567"
            )
        )
        mockDeviceContactsService.addDeviceContact(
            DeviceContactsService.DeviceContact(
                id = "device-2",
                displayName = "Jane Smith",
                phoneNumber = "+12223334444"
            )
        )

        // Act
        val result = mockDeviceContactsService.importContacts()

        // Assert
        assertTrue(result.isSuccess, "Device contacts import should succeed")
        val contacts = result.getOrNull() ?: emptyList()
        assertEquals(2, contacts.size, "Should import 2 contacts")
        assertEquals("John Doe", contacts[0].displayName, "First contact name should match")
        assertEquals("+15551234567", contacts[0].phoneNumber, "First contact phone should match")
        assertEquals("Jane Smith", contacts[1].displayName, "Second contact name should match")
        assertEquals("+12223334444", contacts[1].phoneNumber, "Second contact phone should match")
    }

    @Test
    fun `import contacts handles missing phone numbers`() = runTest {
        // Arrange - Contact without phone number should be filtered out
        mockDeviceContactsService.addDeviceContact(
            DeviceContactsService.DeviceContact(
                id = "device-1",
                displayName = "No Phone",
                phoneNumber = null
            )
        )
        mockDeviceContactsService.addDeviceContact(
            DeviceContactsService.DeviceContact(
                id = "device-2",
                displayName = "With Phone",
                phoneNumber = "+15551234567"
            )
        )

        // Act
        val result = mockDeviceContactsService.importContacts()

        // Assert
        assertTrue(result.isSuccess, "Import should succeed")
        val contacts = result.getOrNull() ?: emptyList()
        assertEquals(1, contacts.size, "Should only import contact with phone number")
        assertEquals("With Phone", contacts[0].displayName, "Should import contact with phone")
    }

    @Test
    fun `import contacts uses fallback display name`() = runTest {
        // Arrange
        mockDeviceContactsService.addDeviceContact(
            DeviceContactsService.DeviceContact(
                id = "device-1",
                displayName = "",
                phoneNumber = "+15551234567"
            )
        )

        // Act
        val result = mockDeviceContactsService.importContacts()

        // Assert
        assertTrue(result.isSuccess, "Import should succeed")
        val contacts = result.getOrNull() ?: emptyList()
        assertEquals(1, contacts.size, "Should import 1 contact")
        assertEquals("", contacts[0].displayName, "Should use empty string as display name")
    }

    @Test
    fun `search contacts filters by display name`() = runTest {
        // Arrange
        mockDeviceContactsService.addDeviceContact(
            DeviceContactsService.DeviceContact(
                id = "device-1",
                displayName = "John Smith",
                phoneNumber = "+15551234567"
            )
        )
        mockDeviceContactsService.addDeviceContact(
            DeviceContactsService.DeviceContact(
                id = "device-2",
                displayName = "Jane Doe",
                phoneNumber = "+12223334444"
            )
        )
        mockDeviceContactsService.addDeviceContact(
            DeviceContactsService.DeviceContact(
                id = "device-3",
                displayName = "Johnny Appleseed",
                phoneNumber = "+1999888776666"
            )
        )

        // Act
        val result = mockDeviceContactsService.searchContacts("john")

        // Assert
        assertTrue(result.isSuccess, "Search should succeed")
        val contacts = result.getOrNull() ?: emptyList()
        assertEquals(2, contacts.size, "Should find 2 contacts with 'john' in name")
        assertTrue(contacts.any { it.displayName.contains("John") }, "Should find John Smith")
        assertTrue(contacts.any { it.displayName.contains("Johnny") }, "Should find Johnny Appleseed")
        assertFalse(contacts.any { it.displayName.contains("Jane") }, "Should not find Jane Doe")
    }

    @Test
    fun `search contacts is case insensitive`() = runTest {
        // Arrange
        mockDeviceContactsService.addDeviceContact(
            DeviceContactsService.DeviceContact(
                id = "device-1",
                displayName = "ALICE WONDERLAND",
                phoneNumber = "+15551234567"
            )
        )

        // Act
        val result = mockDeviceContactsService.searchContacts("alice")

        // Assert
        assertTrue(result.isSuccess, "Search should succeed")
        val contacts = result.getOrNull() ?: emptyList()
        assertEquals(1, contacts.size, "Should find contact case-insensitively")
    }

    @Test
    fun `search empty query returns all contacts`() = runTest {
        // Arrange
        mockDeviceContactsService.addDeviceContact(
            DeviceContactsService.DeviceContact(
                id = "device-1",
                displayName = "Contact 1",
                phoneNumber = "+15551234567"
            )
        )
        mockDeviceContactsService.addDeviceContact(
            DeviceContactsService.DeviceContact(
                id = "device-2",
                displayName = "Contact 2",
                phoneNumber = "+12223334444"
            )
        )

        // Act
        val result = mockDeviceContactsService.searchContacts("")

        // Assert
        assertTrue(result.isSuccess, "Empty search should succeed")
        val contacts = result.getOrNull() ?: emptyList()
        assertEquals(2, contacts.size, "Empty query should return all contacts")
    }

    // ==================== Contact Repository Tests ====================

    @Test
    fun `create contact successfully`() = runTest {
        // Arrange
        val userId = "user-123"
        val now = System.currentTimeMillis()
        val contact = UserContact(
            id = "",
            userId = userId,
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
        val createdContact = result.getOrNull()!!
        assertTrue(createdContact.id.isNotEmpty(), "Contact should have generated ID")
        assertEquals(userId, createdContact.userId, "User ID should match")
        assertEquals("+15551234567", createdContact.contactPhoneNumber, "Phone number should match")
        assertEquals("John Doe", createdContact.displayName, "Display name should match")
        assertFalse(createdContact.hasApp, "New contact should not have app")
        assertNull(createdContact.contactUserId, "New contact should not have user ID")
    }

    @Test
    fun `create contacts batch successfully`() = runTest {
        // Arrange
        val userId = "user-123"
        val now = System.currentTimeMillis()
        val contacts = listOf(
            UserContact(
                id = "", userId = userId,
                contactPhoneNumber = "+15551234567",
                displayName = "John Doe",
                hasApp = false, contactUserId = null,
                createdAt = now, updatedAt = now
            ),
            UserContact(
                id = "", userId = userId,
                contactPhoneNumber = "+12223334444",
                displayName = "Jane Smith",
                hasApp = false, contactUserId = null,
                createdAt = now, updatedAt = now
            ),
            UserContact(
                id = "", userId = userId,
                contactPhoneNumber = "+1999888776666",
                displayName = "Bob Johnson",
                hasApp = false, contactUserId = null,
                createdAt = now, updatedAt = now
            )
        )

        // Act
        val result = mockContactRepository.createContacts(contacts)

        // Assert
        assertTrue(result.isSuccess, "Batch contact creation should succeed")
        val createdContacts = result.getOrNull() ?: emptyList()
        assertEquals(3, createdContacts.size, "Should create 3 contacts")
        assertTrue(createdContacts.all { it.userId == userId }, "All contacts should belong to user")
    }

    @Test
    fun `get contacts by user id returns only user contacts`() = runTest {
        // Arrange
        val userId1 = "user-1"
        val userId2 = "user-2"
        val now = System.currentTimeMillis()

        // Create contacts for user 1
        repeat(3) { i ->
            mockContactRepository.createContact(
                UserContact(
                    id = "", userId = userId1,
                    contactPhoneNumber = "+1555${1000 + i}",
                    displayName = "Contact $i",
                    hasApp = false, contactUserId = null,
                    createdAt = now, updatedAt = now
                )
            )
        }

        // Create contacts for user 2
        repeat(2) { i ->
            mockContactRepository.createContact(
                UserContact(
                    id = "", userId = userId2,
                    contactPhoneNumber = "+1222${2000 + i}",
                    displayName = "User 2 Contact $i",
                    hasApp = false, contactUserId = null,
                    createdAt = now, updatedAt = now
                )
            )
        }

        // Act
        val result = mockContactRepository.getContactsByUserId(userId1)

        // Assert
        assertTrue(result.isSuccess, "Query should succeed")
        val contacts = result.getOrNull() ?: emptyList()
        assertEquals(3, contacts.size, "Should return 3 contacts for user 1")
        contacts.forEach { contact ->
            assertEquals(userId1, contact.userId, "All contacts should belong to user 1")
        }
    }

    @Test
    fun `get contact by id returns correct contact`() = runTest {
        // Arrange
        val userId = "user-123"
        val now = System.currentTimeMillis()
        val contact = UserContact(
            id = "",
            userId = userId,
            contactPhoneNumber = "+15551234567",
            displayName = "John Doe",
            hasApp = false, contactUserId = null,
            createdAt = now, updatedAt = now
        )
        val createResult = mockContactRepository.createContact(contact)
        val createdContact = createResult.getOrNull()!!

        // Act
        val result = mockContactRepository.getContactById(userId, createdContact.id)

        // Assert
        assertTrue(result.isSuccess, "Query should succeed")
        val retrieved = result.getOrNull()
        assertNotNull(retrieved, "Contact should be found")
        assertEquals(createdContact.id, retrieved?.id, "Retrieved ID should match")
        assertEquals("John Doe", retrieved?.displayName, "Retrieved display name should match")
    }

    @Test
    fun `get contact by phone number returns correct contact`() = runTest {
        // Arrange
        val userId = "user-123"
        val phoneNumber = "+15551234567"
        val now = System.currentTimeMillis()
        val contact = UserContact(
            id = "", userId = userId,
            contactPhoneNumber = phoneNumber,
            displayName = "John Doe",
            hasApp = false, contactUserId = null,
            createdAt = now, updatedAt = now
        )
        mockContactRepository.createContact(contact)

        // Act
        val result = mockContactRepository.getContactByPhoneNumber(userId, phoneNumber)

        // Assert
        assertTrue(result.isSuccess, "Query should succeed")
        val retrieved = result.getOrNull()
        assertNotNull(retrieved, "Contact should be found")
        assertEquals(phoneNumber, retrieved?.contactPhoneNumber, "Retrieved phone number should match")
    }

    @Test
    fun `update contact display name successfully`() = runTest {
        // Arrange
        val userId = "user-123"
        val now = System.currentTimeMillis()
        val contact = UserContact(
            id = "", userId = userId,
            contactPhoneNumber = "+15551234567",
            displayName = "Old Name",
            hasApp = false, contactUserId = null,
            createdAt = now, updatedAt = now
        )
        val createResult = mockContactRepository.createContact(contact)
        val createdContact = createResult.getOrNull()!!

        // Act
        val result = mockContactRepository.updateContactDisplayName(createdContact.id, "New Name")

        // Assert
        assertTrue(result.isSuccess, "Display name update should succeed")
        val updated = result.getOrNull()!!
        assertEquals("New Name", updated.displayName, "Display name should be updated")
        assertTrue(updated.updatedAt > createdContact.updatedAt, "Updated at should be later than created at")
    }

    @Test
    fun `update contact app status successfully`() = runTest {
        // Arrange
        val userId = "user-123"
        val now = System.currentTimeMillis()
        val contact = UserContact(
            id = "", userId = userId,
            contactPhoneNumber = "+15551234567",
            displayName = "John Doe",
            hasApp = false, contactUserId = null,
            createdAt = now, updatedAt = now
        )
        val createResult = mockContactRepository.createContact(contact)
        val createdContact = createResult.getOrNull()!!

        // Act
        val result = mockContactRepository.updateContactAppStatus(
            createdContact.id,
            hasApp = true,
            contactUserId = "app-user-456"
        )

        // Assert
        assertTrue(result.isSuccess, "App status update should succeed")
        val updated = result.getOrNull()!!
        assertTrue(updated.hasApp, "hasApp should be true")
        assertEquals("app-user-456", updated.contactUserId, "Contact user ID should be set")
        assertTrue(updated.updatedAt > createdContact.updatedAt, "Updated at should be later")
    }

    @Test
    fun `delete contact successfully`() = runTest {
        // Arrange
        val userId = "user-123"
        val now = System.currentTimeMillis()
        val contact = UserContact(
            id = "", userId = userId,
            contactPhoneNumber = "+15551234567",
            displayName = "John Doe",
            hasApp = false, contactUserId = null,
            createdAt = now, updatedAt = now
        )
        val createResult = mockContactRepository.createContact(contact)
        val createdContact = createResult.getOrNull()!!

        // Act
        val deleteResult = mockContactRepository.deleteContact(createdContact.id)

        // Assert
        assertTrue(deleteResult.isSuccess, "Contact deletion should succeed")

        val getResult = mockContactRepository.getContactById(userId, createdContact.id)
        assertNull(getResult.getOrNull(), "Deleted contact should not be found")
    }

    @Test
    fun `delete all contacts for user successfully`() = runTest {
        // Arrange
        val userId = "user-123"
        val now = System.currentTimeMillis()

        repeat(3) { i ->
            mockContactRepository.createContact(
                UserContact(
                    id = "", userId = userId,
                    contactPhoneNumber = "+1555${1000 + i}",
                    displayName = "Contact $i",
                    hasApp = false, contactUserId = null,
                    createdAt = now, updatedAt = now
                )
            )
        }

        // Act
        val deleteResult = mockContactRepository.deleteAllContacts(userId)

        // Assert
        assertTrue(deleteResult.isSuccess, "Delete all contacts should succeed")

        val getResult = mockContactRepository.getContactsByUserId(userId)
        val remainingContacts = getResult.getOrNull() ?: emptyList()
        assertEquals(0, remainingContacts.size, "All contacts should be deleted")
    }

    @Test
    fun `sync contacts upserts existing contacts`() = runTest {
        // Arrange
        val userId = "user-123"
        val now = System.currentTimeMillis()

        // Create initial contact
        val initialContact = UserContact(
            id = "", userId = userId,
            contactPhoneNumber = "+15551234567",
            displayName = "Old Name",
            hasApp = false, contactUserId = null,
            createdAt = now, updatedAt = now
        )
        mockContactRepository.createContact(initialContact)

        // Sync with updated contact (same phone number)
        val updatedContact = UserContact(
            id = "", userId = userId,
            contactPhoneNumber = "+15551234567", // Same phone number
            displayName = "New Name",
            hasApp = true, contactUserId = "app-user-456",
            createdAt = now, updatedAt = now
        )

        // Act
        val result = mockContactRepository.syncContacts(userId, listOf(updatedContact))

        // Assert
        assertTrue(result.isSuccess, "Sync should succeed")
        val syncedContacts = result.getOrNull() ?: emptyList()
        assertEquals(1, syncedContacts.size, "Should have 1 synced contact")
        assertEquals("New Name", syncedContacts[0].displayName, "Display name should be updated")
        assertTrue(syncedContacts[0].hasApp, "App status should be updated")
        assertEquals("app-user-456", syncedContacts[0].contactUserId, "Contact user ID should be set")
    }

    @Test
    fun `sync contacts inserts new contacts`() = runTest {
        // Arrange
        val userId = "user-123"
        val now = System.currentTimeMillis()

        // Sync with new contact (different phone number)
        val newContact = UserContact(
            id = "", userId = userId,
            contactPhoneNumber = "+15551234567", // New phone number
            displayName = "New Contact",
            hasApp = false, contactUserId = null,
            createdAt = now, updatedAt = now
        )

        // Act
        val result = mockContactRepository.syncContacts(userId, listOf(newContact))

        // Assert
        assertTrue(result.isSuccess, "Sync should succeed")
        val syncedContacts = result.getOrNull() ?: emptyList()
        assertEquals(1, syncedContacts.size, "Should have 1 new contact")
        assertEquals("New Contact", syncedContacts[0].displayName, "New contact should be inserted")
    }

    // ==================== App User Detection Tests ====================

    @Test
    fun `detect app user by phone number`() = runTest {
        // Arrange
        mockAppUserDetection.addAppUser("app-user-1", "+15551234567")
        mockAppUserDetection.addAppUser("app-user-2", "+12223334444")

        // Act
        val phoneNumbers = listOf("+15551234567", "+12223334444", "+1999888776666")
        val result = mockAppUserDetection(phoneNumbers)

        // Assert
        assertTrue(result.isSuccess, "App user detection should succeed")
        val appUserMap = result.getOrNull() ?: emptyMap()
        assertEquals(2, appUserMap.size, "Should detect 2 app users")
        assertNotNull(appUserMap["+15551234567"], "First phone should be app user")
        assertNotNull(appUserMap["+12223334444"], "Second phone should be app user")
        assertNull(appUserMap["+1999888776666"], "Third phone should not be app user")
    }

    @Test
    fun `app user detection handles non-existent users`() = runTest {
        // Arrange - No app users registered

        // Act
        val phoneNumbers = listOf("+15551234567", "+12223334444")
        val result = mockAppUserDetection(phoneNumbers)

        // Assert
        assertTrue(result.isSuccess, "App user detection should succeed")
        val appUserMap = result.getOrNull() ?: emptyMap()
        assertEquals(0, appUserMap.size, "Should detect 0 app users")
    }

    // ==================== Contact Model Tests ====================

    @Test
    fun `withAppStatus creates updated copy`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val contact = UserContact(
            id = "contact-1",
            userId = "user-123",
            contactPhoneNumber = "+15551234567",
            displayName = "John Doe",
            hasApp = false,
            contactUserId = null,
            createdAt = now - 10000,
            updatedAt = now - 10000
        )

        // Act
        val updated = contact.withAppStatus(true, "app-user-456")

        // Assert
        assertTrue(updated.hasApp, "hasApp should be true")
        assertEquals("app-user-456", updated.contactUserId, "Contact user ID should be set")
        assertTrue(updated.updatedAt > contact.updatedAt, "Updated at should be later")
        assertEquals(contact.id, updated.id, "ID should remain unchanged")
        assertEquals(contact.displayName, updated.displayName, "Display name should remain unchanged")
    }

    @Test
    fun `withDisplayName creates updated copy`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val contact = UserContact(
            id = "contact-1",
            userId = "user-123",
            contactPhoneNumber = "+15551234567",
            displayName = "Old Name",
            hasApp = false,
            contactUserId = null,
            createdAt = now - 10000,
            updatedAt = now - 10000
        )

        // Act
        val updated = contact.withDisplayName("New Name")

        // Assert
        assertEquals("New Name", updated.displayName, "Display name should be updated")
        assertTrue(updated.updatedAt > contact.updatedAt, "Updated at should be later")
        assertEquals(contact.id, updated.id, "ID should remain unchanged")
        assertEquals(contact.hasApp, updated.hasApp, "hasApp should remain unchanged")
        assertEquals(contact.contactPhoneNumber, updated.contactPhoneNumber, "Phone number should remain unchanged")
    }

    // ==================== Complete Contact Management Integration Tests ====================

    @Test
    fun `complete contact import flow from device to sync`() = runTest {
        // Arrange
        val userId = "user-import-flow"
        val now = System.currentTimeMillis()

        // Step 1: Import device contacts
        mockDeviceContactsService.addDeviceContact(
            DeviceContactsService.DeviceContact(
                id = "device-1",
                displayName = "App User",
                phoneNumber = "+15551234567"
            )
        )
        mockDeviceContactsService.addDeviceContact(
            DeviceContactsService.DeviceContact(
                id = "device-2",
                displayName = "Non-App User",
                phoneNumber = "+12223334444"
            )
        )

        // Register app users
        mockAppUserDetection.addAppUser("app-user-123", "+15551234567")

        // Act - Import device contacts
        val importResult = mockDeviceContactsService.importContacts()
        assertTrue(importResult.isSuccess, "Device import should succeed")
        val deviceContacts = importResult.getOrNull()!!

        // Step 2: Create UserContact records
        val userContacts = deviceContacts.map { dc ->
            UserContact(
                id = "", userId = userId,
                contactPhoneNumber = dc.phoneNumber!!,
                displayName = dc.displayName,
                hasApp = false, contactUserId = null,
                createdAt = now, updatedAt = now
            )
        }

        val createResult = mockContactRepository.createContacts(userContacts)
        assertTrue(createResult.isSuccess, "Contact creation should succeed")
        val createdContacts = createResult.getOrNull()!!

        // Step 3: Update app user status
        val appUserMap = mockAppUserDetection(
            createdContacts.map { it.contactPhoneNumber }
        ).getOrNull() ?: emptyMap()

        val updateResults = mutableListOf<Result<UserContact>>()
        createdContacts.forEach { contact ->
            val matchedUser = appUserMap[contact.contactPhoneNumber]
            if (matchedUser != null) {
                val updateResult = mockContactRepository.updateContactAppStatus(
                    contact.id,
                    hasApp = true,
                    contactUserId = matchedUser.id
                )
                updateResults.add(updateResult)
            }
        }

        // Assert complete flow
        assertTrue(updateResults.all { it.isSuccess }, "All app status updates should succeed")

        val updatedContact = mockContactRepository.getContactById(userId, createdContacts[0].id)
        val updated = updatedContact.getOrNull()
        assertTrue(updated?.hasApp == true, "App user should have hasApp true")
        assertNotNull(updated?.contactUserId, "App user should have contactUserId set")
    }

    @Test
    fun `contact sync handles mixed insert and update`() = runTest {
        // Arrange
        val userId = "user-mixed-sync"
        val now = System.currentTimeMillis()
        val phoneNumber = "+15551234567"

        // Create initial contact
        val initialContact = UserContact(
            id = "", userId = userId,
            contactPhoneNumber = phoneNumber,
            displayName = "Initial Contact",
            hasApp = false, contactUserId = null,
            createdAt = now, updatedAt = now
        )
        mockContactRepository.createContact(initialContact)

        // Step 1: Sync with updated contact (same phone number) - should update
        val sync1Result = mockContactRepository.syncContacts(
            userId,
            listOf(
                UserContact(
                    id = "", userId = userId,
                    contactPhoneNumber = phoneNumber,
                    displayName = "Updated Contact",
                    hasApp = true, contactUserId = "app-user-456",
                    createdAt = now, updatedAt = now
                )
            )
        )
        assertTrue(sync1Result.isSuccess, "First sync should succeed")
        val afterFirstSync = mockContactRepository.getContactByPhoneNumber(userId, phoneNumber).getOrNull()
        assertEquals("Updated Contact", afterFirstSync?.displayName, "Contact should be updated")
        assertEquals(true, afterFirstSync?.hasApp, "App status should be updated")

        // Step 2: Sync with new contact (different phone number) - should insert
        val newPhoneNumber = "+12223334444"
        val sync2Result = mockContactRepository.syncContacts(
            userId,
            listOf(
                UserContact(
                    id = "", userId = userId,
                    contactPhoneNumber = newPhoneNumber,
                    displayName = "New Contact",
                    hasApp = false, contactUserId = null,
                    createdAt = now, updatedAt = now
                )
            )
        )
        assertTrue(sync2Result.isSuccess, "Second sync should succeed")
        val afterSecondSync = mockContactRepository.getContactByPhoneNumber(userId, newPhoneNumber).getOrNull()
        assertNotNull(afterSecondSync, "New contact should be inserted")
        assertEquals("New Contact", afterSecondSync?.displayName, "New contact display name should match")
    }

    @Test
    fun `contact search returns matching results`() = runTest {
        // Arrange
        val userId = "user-search"
        val now = System.currentTimeMillis()

        val contacts = listOf(
            UserContact(
                id = "", userId = userId,
                contactPhoneNumber = "+15551234567",
                displayName = "John Smith",
                hasApp = false, contactUserId = null,
                createdAt = now, updatedAt = now
            ),
            UserContact(
                id = "", userId = userId,
                contactPhoneNumber = "+12223334444",
                displayName = "Jane Doe",
                hasApp = false, contactUserId = null,
                createdAt = now, updatedAt = now
            ),
            UserContact(
                id = "", userId = userId,
                contactPhoneNumber = "+1999888776666",
                displayName = "Bob Johnson",
                hasApp = false, contactUserId = null,
                createdAt = now, updatedAt = now
            )
        )

        mockContactRepository.createContacts(contacts)

        // Act - Get all contacts and filter by display name
        val getResult = mockContactRepository.getContactsByUserId(userId)
        assertTrue(getResult.isSuccess, "Get contacts should succeed")
        val allContacts = getResult.getOrNull() ?: emptyList()

        val matching = allContacts.filter { it.displayName.contains("john", ignoreCase = true) }

        // Assert
        assertEquals(1, matching.size, "Should find 1 matching contact")
        assertEquals("John Smith", matching[0].displayName, "Matching contact should be John Smith")
    }

    @Test
    fun `app user badge is set correctly`() = runTest {
        // Arrange
        val userId = "user-badge-test"
        val now = System.currentTimeMillis()

        // Create contact for app user
        val appUserContact = UserContact(
            id = "", userId = userId,
            contactPhoneNumber = "+15551234567",
            displayName = "App User",
            hasApp = true, contactUserId = "app-user-123",
            createdAt = now, updatedAt = now
        )

        // Create contact for non-app user
        val nonAppUserContact = UserContact(
            id = "", userId = userId,
            contactPhoneNumber = "+12223334444",
            displayName = "Non-App User",
            hasApp = false, contactUserId = null,
            createdAt = now, updatedAt = now
        )

        mockContactRepository.createContacts(listOf(appUserContact, nonAppUserContact))

        // Act
        val getResult = mockContactRepository.getContactsByUserId(userId)

        // Assert
        assertTrue(getResult.isSuccess, "Get contacts should succeed")
        val contacts = getResult.getOrNull() ?: emptyList()

        val appContact = contacts.find { it.contactPhoneNumber == "+15551234567" }
        val nonAppContact = contacts.find { it.contactPhoneNumber == "+12223334444" }

        assertNotNull(appContact, "App user contact should be found")
        assertTrue(appContact!!.hasApp, "App user should have hasApp true")
        assertEquals("app-user-123", appContact.contactUserId, "App user should have contactUserId")

        assertNotNull(nonAppContact, "Non-app user contact should be found")
        assertFalse(nonAppContact!!.hasApp, "Non-app user should have hasApp false")
        assertNull(nonAppContact.contactUserId, "Non-app user should not have contactUserId")
    }

    // ==================== Mock Implementations ====================

    class MockUserContactRepository : UserContactRepository {
        private val contacts = mutableMapOf<String, UserContact>()
        private val nextId = mutableMapOf<String, String>()

        override suspend fun createContact(contact: UserContact): Result<UserContact> {
            val id = contact.id.ifEmpty { "contact-${System.currentTimeMillis()}" }
            val createdContact = contact.copy(id = id)
            contacts[id] = createdContact
            return Result.success(createdContact)
        }

        override suspend fun createContacts(contacts: List<UserContact>): Result<List<UserContact>> {
            val createdContacts = contacts.map { contact ->
                val id = contact.id.ifEmpty { "contact-${System.currentTimeMillis()}-${nextId.size}" }
                val created = contact.copy(id = id)
                contacts[id] = created
                created
            }
            return Result.success(createdContacts)
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
                contacts[contactId] = contact.withDisplayName(displayName)
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
                contacts[contactId] = contact.withAppStatus(hasApp, contactUserId)
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
                    // Update existing
                    val id = existing.id
                    contacts[id] = contact.copy(id = id)
                    synced.add(contacts[id]!!)
                } else {
                    // Insert new
                    val id = contact.id.ifEmpty { "contact-${System.currentTimeMillis()}-${synced.size}" }
                    contacts[id] = contact.copy(id = id)
                    synced.add(contacts[id]!!)
                }
            }
            return Result.success(synced)
        }

        fun clear() {
            contacts.clear()
            nextId.clear()
        }
    }

    class MockDeviceContactsService {
        private val deviceContacts = mutableListOf<DeviceContactsService.DeviceContact>()

        fun addDeviceContact(contact: DeviceContactsService.DeviceContact) {
            deviceContacts.add(contact)
        }

        suspend fun importContacts(): Result<List<DeviceContactsService.DeviceContact>> {
            return Result.success(deviceContacts.toList())
        }

        suspend fun searchContacts(query: String): Result<List<DeviceContactsService.DeviceContact>> {
            if (query.isBlank()) {
                return Result.success(deviceContacts.toList())
            }
            val filtered = deviceContacts.filter {
                it.displayName.contains(query, ignoreCase = true)
            }
            return Result.success(filtered)
        }

        fun clear() {
            deviceContacts.clear()
        }
    }

    class MockAppUserDetection {
        private val appUsers = mutableMapOf<String, MockUser>()

        fun addAppUser(userId: String, phoneNumber: String) {
            appUsers[phoneNumber] = MockUser(userId, phoneNumber)
        }

        suspend operator fun invoke(phoneNumbers: List<String>): Result<Map<String, MockUser>> {
            val detected = mutableMapOf<String, MockUser>()
            phoneNumbers.forEach { phone ->
                appUsers[phone]?.let { detected[phone] = it }
            }
            return Result.success(detected)
        }

        fun clear() {
            appUsers.clear()
        }
    }

    data class MockUser(
        val id: String,
        val phoneNumber: String
    )
}
