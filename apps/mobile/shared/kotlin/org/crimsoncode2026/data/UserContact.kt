package org.crimsoncode2026.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * User Contact data model
 *
 * Each user's private emergency contact list.
 *
 * @property id Stable row identifier (UUID)
 * @property userId Contact-list owner (foreign key to users.id)
 * @property contactPhoneNumber Canonical routing key for matching (E.164 format, max 20 chars)
 * @property displayName User-local contact label
 * @property hasApp Cached UI indicator
 * @property contactUserId Cached match when contact has registered (nullable FK to users.id)
 * @property createdAt Audit trail timestamp (milliseconds since epoch)
 * @property updatedAt Audit trail timestamp (milliseconds since epoch)
 */
@Serializable
data class UserContact(
    @SerialName("id")
    val id: String,

    @SerialName("user_id")
    val userId: String,

    @SerialName("contact_phone_number")
    val contactPhoneNumber: String,

    @SerialName("display_name")
    val displayName: String,

    @SerialName("has_app")
    val hasApp: Boolean = false,

    @SerialName("contact_user_id")
    val contactUserId: String? = null,

    @SerialName("created_at")
    val createdAt: Long,

    @SerialName("updated_at")
    val updatedAt: Long
) {
    companion object {
        const val TABLE_NAME = "user_contacts"
    }

    /**
     * Creates a copy with hasApp and contactUserId updated
     */
    fun withAppStatus(hasApp: Boolean, contactUserId: String? = null): UserContact {
        return copy(
            hasApp = hasApp,
            contactUserId = contactUserId,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Creates a copy with display name updated
     */
    fun withDisplayName(newDisplayName: String): UserContact {
        return copy(
            displayName = newDisplayName,
            updatedAt = System.currentTimeMillis()
        )
    }
}
