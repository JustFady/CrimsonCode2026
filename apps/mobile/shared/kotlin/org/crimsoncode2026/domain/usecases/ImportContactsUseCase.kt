package org.crimsoncode2026.domain.usecases

import org.crimsoncode2026.contacts.DeviceContactsService
import org.crimsoncode2026.contacts.DeviceContactsService.DeviceContact
import org.crimsoncode2026.data.UserContact
import org.crimsoncode2026.data.UserContactRepository
import org.crimsoncode2026.data.User

/**
 * Result of contact import operation.
 */
sealed class ImportContactsResult {
    data object Success : ImportContactsResult()
    data object PermissionDenied : ImportContactsResult()
    data class Error(val message: String) : ImportContactsResult()
}

/**
 * Use case for importing and syncing device contacts.
 *
 * Orchestrates the complete contact import flow:
 * 1. Import device contacts
 * 2. Detect which contacts have the app installed
 * 3. Sync contacts to Supabase
 * 4. Update has_app status for app users
 *
 * @param userContactRepository Repository for user contact operations
 * @param deviceContactsService Service for accessing device contacts
 * @param appUserDetectionUseCase Use case for detecting app users
 */
class ImportContactsUseCase(
    private val userContactRepository: UserContactRepository,
    private val deviceContactsService: DeviceContactsService,
    private val appUserDetectionUseCase: AppUserDetectionUseCase
) {

    /**
     * Imports and syncs all device contacts for the current user.
     *
     * Process:
     * 1. Import all device contacts
     * 2. Detect which contacts have the app (match against Users table)
     * 3. Create UserContact records for each contact
     * 4. Update has_app and contact_user_id for app users
     *
     * @param userId Current user ID
     * @return ImportContactsResult indicating success or failure
     */
    suspend operator fun invoke(userId: String): ImportContactsResult {
        return try {
            // Step 1: Import all device contacts
            val deviceContactsResult = deviceContactsService.importContacts()
            val deviceContacts = deviceContactsResult.getOrNull() ?: emptyList()

            if (deviceContacts.isEmpty()) {
                return ImportContactsResult.Error("No contacts found on device")
            }

            // Step 2: Detect which contacts have the app installed
            val appUserContacts = mutableListOf<UserContact>()
            val userIdToUserMap = mutableMapOf<String, User>()

            // Use the detection use case to find app users
            for (deviceContact in deviceContacts) {
                val phoneNumber = deviceContact.phoneNumber ?: continue

                // Create UserContact record
                val now = System.currentTimeMillis()
                val contact = UserContact(
                    id = "", // Will be assigned by Supabase
                    userId = userId,
                    contactPhoneNumber = phoneNumber,
                    displayName = deviceContact.displayName,
                    hasApp = false,
                    contactUserId = null,
                    createdAt = now,
                    updatedAt = now
                )
                appUserContacts.add(contact)
            }

            // Step 3: Sync all contacts to Supabase
            // The syncContacts method handles creating/updating based on phone number
            val syncResult = userContactRepository.syncContacts(userId, appUserContacts)

            if (!syncResult.isSuccess) {
                return ImportContactsResult.Error(
                    syncResult.exceptionOrNull()?.message ?: "Failed to sync contacts"
                )
            }

            // Step 4: Update has_app and contact_user_id for app users
            val syncedContacts = syncResult.getOrNull() ?: emptyList()
            val updateResults = mutableListOf<Result<UserContact>>()

            // Get map of phone numbers to app users
            val phoneNumberMap = deviceContacts
                .filter { it.phoneNumber != null }
                .associate { it.phoneNumber!! to it }

            // Query app users for all contact phone numbers
            val phoneNumbers = syncedContacts.map { it.contactPhoneNumber }
            val appUserMapResult = appUserDetectionUseCase(phoneNumbers)

            if (!appUserMapResult.isSuccess) {
                // App user detection failed, but contacts are synced
                // has_app will remain false for now
                return ImportContactsResult.Success
            }

            val appUserMap = appUserMapResult.getOrNull() ?: emptyMap()

            // Update each synced contact with app user status
            for (contact in syncedContacts) {
                val matchedUser = appUserMap[contact.contactPhoneNumber]

                if (matchedUser != null) {
                    // Contact has the app, update their record
                    val updateResult = userContactRepository.updateContactAppStatus(
                        contactId = contact.id,
                        hasApp = true,
                        contactUserId = matchedUser.id
                    )
                    updateResults.add(updateResult)
                }
            }

            // Check if any updates failed
            val failedUpdates = updateResults.filter { !it.isSuccess }
            if (failedUpdates.isNotEmpty()) {
                return ImportContactsResult.Error(
                    "Failed to update app user status for ${failedUpdates.size} contacts"
                )
            }

            ImportContactsResult.Success
        } catch (e: Exception) {
            ImportContactsResult.Error(e.message ?: "Failed to import contacts")
        }
    }

    /**
     * Searches device contacts and syncs matching contacts.
     *
     * Useful for searching contacts by name before displaying in UI.
     *
     * @param userId Current user ID
     * @param query Search query for display names
     * @return ImportContactsResult indicating success or failure
     */
    suspend operator fun invoke(userId: String, query: String): ImportContactsResult {
        return try {
            if (query.isBlank()) {
                // No search query, perform full import
                return invoke(userId)
            }

            // Step 1: Search device contacts
            val deviceContactsResult = deviceContactsService.searchContacts(query)
            val deviceContacts = deviceContactsResult.getOrNull() ?: emptyList()

            if (deviceContacts.isEmpty()) {
                return ImportContactsResult.Success // No matching contacts, not an error
            }

            // Step 2: Create UserContact records for matching contacts
            val appUserContacts = mutableListOf<UserContact>()
            val now = System.currentTimeMillis()

            for (deviceContact in deviceContacts) {
                val phoneNumber = deviceContact.phoneNumber ?: continue

                val contact = UserContact(
                    id = "",
                    userId = userId,
                    contactPhoneNumber = phoneNumber,
                    displayName = deviceContact.displayName,
                    hasApp = false,
                    contactUserId = null,
                    createdAt = now,
                    updatedAt = now
                )
                appUserContacts.add(contact)
            }

            // Step 3: Sync searched contacts to Supabase
            val syncResult = userContactRepository.syncContacts(userId, appUserContacts)

            if (!syncResult.isSuccess) {
                return ImportContactsResult.Error(
                    syncResult.exceptionOrNull()?.message ?: "Failed to sync contacts"
                )
            }

            ImportContactsResult.Success
        } catch (e: Exception) {
            ImportContactsResult.Error(e.message ?: "Failed to search contacts")
        }
    }
}
