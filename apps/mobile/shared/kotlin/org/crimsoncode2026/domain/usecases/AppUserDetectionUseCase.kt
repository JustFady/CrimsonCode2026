package org.crimsoncode2026.domain.usecases

import org.crimsoncode2026.contacts.DeviceContactsService
import org.crimsoncode2026.contacts.DeviceContactsService.DeviceContact
import org.crimsoncode2026.data.User

/**
 * Represents a detected app user contact.
 *
 * @param deviceContact The original device contact information
 * @param user The user record if found, null otherwise
 */
data class AppUserContact(
    val deviceContact: DeviceContact,
    val user: User?
)

/**
 * Use case for detecting which device contacts have the app installed.
 *
 * Queries the Users table by phone number to find matches among device contacts.
 * Used for displaying "has app" badges in the contact list.
 *
 * @param userRepository Repository for user queries
 * @param deviceContactsService Service for accessing device contacts
 */
class AppUserDetectionUseCase(
    private val userRepository: org.crimsoncode2026.data.UserRepository,
    private val deviceContactsService: DeviceContactsService
) {

    /**
     * Detects which of the device contacts have the app installed.
     *
     * Matches device contacts against the Users table by phone number.
     * Returns a list of contacts with user information when found.
     *
     * @return Result with list of detected app user contacts
     */
    suspend operator fun invoke(): Result<List<AppUserContact>> {
        return try {
            // Step 1: Import all device contacts
            val deviceContactsResult = deviceContactsService.importContacts()
            val deviceContacts = deviceContactsResult.getOrNull() ?: emptyList()

            // Step 2: Query users for each contact phone number
            val appUserContacts = mutableListOf<AppUserContact>()

            for (contact in deviceContacts) {
                val phoneNumber = contact.phoneNumber ?: continue
                val userResult = userRepository.getUserByPhoneNumber(phoneNumber)

                if (userResult.isSuccess) {
                    val user = userResult.getOrNull()
                    appUserContacts.add(
                        AppUserContact(deviceContact = contact, user = user)
                    )
                }
            }

            Result.success(appUserContacts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Detects app users for a specific list of phone numbers.
     *
     * Useful for refreshing app user status after import or when contacts change.
     *
     * @param phoneNumbers List of E.164 phone numbers to check
     * @return Result with map of phone number to user (null if not found)
     */
    suspend operator fun invoke(phoneNumbers: List<String>): Result<Map<String, User?>> {
        return try {
            val userMap = mutableMapOf<String, User?>()

            for (phoneNumber in phoneNumbers) {
                val userResult = userRepository.getUserByPhoneNumber(phoneNumber)
                if (userResult.isSuccess) {
                    userMap[phoneNumber] = userResult.getOrNull()
                } else {
                    userMap[phoneNumber] = null
                }
            }

            Result.success(userMap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
