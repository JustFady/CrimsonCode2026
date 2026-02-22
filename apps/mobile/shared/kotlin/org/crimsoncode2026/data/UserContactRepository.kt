package org.crimsoncode2026.data

import io.github.jan-tennert.supabase.postgrest.Postgrest

/**
 * Repository interface for UserContact data operations
 */
interface UserContactRepository {

    /**
     * Create a new contact for a user
     * @param contact UserContact to create
     * @return Result with created contact or error
     */
    suspend fun createContact(contact: UserContact): Result<UserContact>

    /**
     * Create multiple contacts for a user (batch insert)
     * @param contacts List of contacts to create
     * @return Result with created contacts or error
     */
    suspend fun createContacts(contacts: List<UserContact>): Result<List<UserContact>>

    /**
     * Get all contacts for a user
     * @param userId User ID
     * @return Result with list of contacts or error
     */
    suspend fun getContactsByUserId(userId: String): Result<List<UserContact>>

    /**
     * Get a specific contact
     * @param userId User ID (owner of the contact)
     * @param contactId Contact ID
     * @return Result with contact or null if not found
     */
    suspend fun getContactById(userId: String, contactId: String): Result<UserContact?>

    /**
     * Get contact by phone number for a user
     * @param userId User ID (owner of the contact)
     * @param phoneNumber Contact phone number
     * @return Result with contact or null if not found
     */
    suspend fun getContactByPhoneNumber(userId: String, phoneNumber: String): Result<UserContact?>

    /**
     * Update contact display name
     * @param contactId Contact ID
     * @param displayName New display name
     * @return Result with updated contact or error
     */
    suspend fun updateContactDisplayName(contactId: String, displayName: String): Result<UserContact>

    /**
     * Update contact app status
     * @param contactId Contact ID
     * @param hasApp Whether contact has the app
     * @param contactUserId Optional contact user ID if app user
     * @return Result with updated contact or error
     */
    suspend fun updateContactAppStatus(
        contactId: String,
        hasApp: Boolean,
        contactUserId: String? = null
    ): Result<UserContact>

    /**
     * Delete a contact
     * @param contactId Contact ID
     * @return Result indicating success or error
     */
    suspend fun deleteContact(contactId: String): Result<Unit>

    /**
     * Delete all contacts for a user
     * @param userId User ID
     * @return Result indicating success or error
     */
    suspend fun deleteAllContacts(userId: String): Result<Unit>

    /**
     * Sync contacts - update or insert based on phone number
     * Contacts are uniquely identified by (user_id, contact_phone_number)
     * @param userId User ID
     * @param contacts List of contacts to sync
     * @return Result with updated contacts or error
     */
    suspend fun syncContacts(userId: String, contacts: List<UserContact>): Result<List<UserContact>>
}

/**
 * Supabase implementation of UserContactRepository
 */
class UserContactRepositoryImpl(
    private val postgrest: Postgrest
) : UserContactRepository {

    companion object {
        private const val TABLE = UserContact.TABLE_NAME
    }

    override suspend fun createContact(contact: UserContact): Result<UserContact> = try {
        val result = postgrest.from(TABLE)
            .insert(contact)
            .decodeSingle<UserContact>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun createContacts(contacts: List<UserContact>): Result<List<UserContact>> = try {
        val result = postgrest.from(TABLE)
            .insert(contacts)
            .decodeList<UserContact>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getContactsByUserId(userId: String): Result<List<UserContact>> = try {
        val result = postgrest.from(TABLE)
            .select {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<UserContact>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getContactById(userId: String, contactId: String): Result<UserContact?> = try {
        val result = postgrest.from(TABLE)
            .select {
                filter {
                    eq("user_id", userId)
                    eq("id", contactId)
                }
            }
            .decodeSingleOrNull<UserContact>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getContactByPhoneNumber(
        userId: String,
        phoneNumber: String
    ): Result<UserContact?> = try {
        val result = postgrest.from(TABLE)
            .select {
                filter {
                    eq("user_id", userId)
                    eq("contact_phone_number", phoneNumber)
                }
            }
            .decodeSingleOrNull<UserContact>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateContactDisplayName(
        contactId: String,
        displayName: String
    ): Result<UserContact> = try {
        val result = postgrest.from(TABLE)
            .update(
                mapOf(
                    "display_name" to displayName,
                    "updated_at" to System.currentTimeMillis()
                )
            ) {
                filter {
                    eq("id", contactId)
                }
            }
            .decodeSingle<UserContact>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateContactAppStatus(
        contactId: String,
        hasApp: Boolean,
        contactUserId: String?
    ): Result<UserContact> = try {
        val updateMap = mutableMapOf(
            "has_app" to hasApp,
            "updated_at" to System.currentTimeMillis()
        )
        contactUserId?.let { updateMap["contact_user_id"] = it }

        val result = postgrest.from(TABLE)
            .update(updateMap) {
                filter {
                    eq("id", contactId)
                }
            }
            .decodeSingle<UserContact>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteContact(contactId: String): Result<Unit> = try {
        postgrest.from(TABLE)
            .delete {
                filter {
                    eq("id", contactId)
                }
            }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteAllContacts(userId: String): Result<Unit> = try {
        postgrest.from(TABLE)
            .delete {
                filter {
                    eq("user_id", userId)
                }
            }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun syncContacts(
        userId: String,
        contacts: List<UserContact>
    ): Result<List<UserContact>> = try {
        val syncedContacts = mutableListOf<UserContact>()

        for (contact in contacts) {
            // Try to find existing contact by phone number
            val existingResult = getContactByPhoneNumber(userId, contact.contactPhoneNumber)

            if (existingResult.isSuccess) {
                val existing = existingResult.getOrNull()
                if (existing != null) {
                    // Update existing contact
                    val updated = postgrest.from(TABLE)
                        .update(
                            mapOf(
                                "display_name" to contact.displayName,
                                "updated_at" to System.currentTimeMillis()
                            )
                        ) {
                            filter {
                                eq("id", existing.id)
                            }
                        }
                        .decodeSingle<UserContact>()
                    syncedContacts.add(updated)
                } else {
                    // Create new contact
                    val created = postgrest.from(TABLE)
                        .insert(contact)
                        .decodeSingle<UserContact>()
                    syncedContacts.add(created)
                }
            }
        }

        Result.success(syncedContacts)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
