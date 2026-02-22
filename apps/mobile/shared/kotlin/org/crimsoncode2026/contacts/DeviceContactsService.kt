package org.crimsoncode2026.contacts

import io.github.dev-bilal-azzam.kontacts.Contact
import io.github.dev-bilal-azzam.kontacts.Kontacts
import org.crimsoncode2026.utils.PhoneNumberUtils

/**
 * Service for importing device contacts using Kontacts library.
 *
 * Provides access to the device's address book and normalizes contact data.
 */
class DeviceContactsService {

    /**
     * Represents a device contact with normalized phone number.
     *
     * @property id Unique identifier from device (may vary by platform)
     * @property displayName Contact's display name from device
     * @property phoneNumber Normalized E.164 phone number, or null if invalid
     */
    data class DeviceContact(
        val id: String,
        val displayName: String,
        val phoneNumber: String?
    )

    /**
     * Imports all contacts from the device.
     *
     * @return Result with list of device contacts containing at least one valid phone number
     */
    suspend fun importContacts(): Result<List<DeviceContact>> = try {
        val contacts = Kontacts.fetchContacts()
        val deviceContacts = mutableListOf<DeviceContact>()

        for (contact in contacts) {
            // Get display name (fallback to empty string if not available)
            val name = contact.displayName ?: contact.name?.first ?: ""

            // Process phone numbers - use first valid normalized number
            val phoneNumber = contact.phoneNumbers
                .mapNotNull { it.number }
                .firstNotNullOfOrNull { PhoneNumberUtils.normalizeToE164(it) }

            // Only include contacts with valid phone numbers
            if (phoneNumber != null) {
                deviceContacts.add(
                    DeviceContact(
                        id = contact.id,
                        displayName = name,
                        phoneNumber = phoneNumber
                    )
                )
            }
        }

        Result.success(deviceContacts)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Imports contacts from the device and filters by name query.
     *
     * @param query Search query to filter contacts by display name
     * @return Result with filtered list of device contacts
     */
    suspend fun searchContacts(query: String): Result<List<DeviceContact>> = try {
        if (query.isBlank()) {
            importContacts()
        } else {
            val allContacts = importContacts().getOrNull() ?: emptyList()
            val filtered = allContacts.filter { contact ->
                contact.displayName.contains(query, ignoreCase = true)
            }
            Result.success(filtered)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
