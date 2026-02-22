package org.crimsoncode2026.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Platform type for the device
 */
enum class Platform(val value: String) {
    @SerialName("ANDROID")
    ANDROID("ANDROID"),
    @SerialName("IOS")
    IOS("IOS");

    companion object {
        fun fromValue(value: String?): Platform? = values().find { it.value == value }
    }
}

/**
 * User data model
 *
 * Represents the app user profile tied to Supabase Auth and one active device.
 *
 * @property id UUID matching auth.users.id and app identity
 * @property phoneNumber Primary account identity (E.164 format, max 20 chars)
 * @property displayName Shown to private recipients
 * @property deviceId One-device-per-account binding
 * @property fcmToken Push notification targeting (nullable)
 * @property platform Android/iOS-specific behavior/debugging
 * @property isActive Soft-disable account if needed
 * @property createdAt Audit trail timestamp (milliseconds since epoch)
 * @property updatedAt Audit trail timestamp (milliseconds since epoch)
 * @property lastActiveAt Activity tracking and cleanup (milliseconds since epoch)
 */
@Serializable
data class User(
    @SerialName("id")
    val id: String,

    @SerialName("phone_number")
    val phoneNumber: String,

    @SerialName("display_name")
    val displayName: String,

    @SerialName("device_id")
    val deviceId: String,

    @SerialName("fcm_token")
    val fcmToken: String? = null,

    @SerialName("platform")
    val platform: String,

    @SerialName("is_active")
    val isActive: Boolean = true,

    @SerialName("created_at")
    val createdAt: Long,

    @SerialName("updated_at")
    val updatedAt: Long,

    @SerialName("last_active_at")
    val lastActiveAt: Long? = null
) {
    companion object {
        const val TABLE_NAME = "users"
    }

    /**
     * Converts platform string to Platform enum
     */
    val platformEnum: Platform?
        get() = Platform.fromValue(platform)

    /**
     * Creates a copy of the user with the display name updated
     */
    fun withDisplayName(newDisplayName: String): User {
        return copy(
            displayName = newDisplayName,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Creates a copy of the user with FCM token updated
     */
    fun withFcmToken(newFcmToken: String): User {
        return copy(
            fcmToken = newFcmToken,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Creates a copy of the user with last active timestamp updated
     */
    fun withLastActive(): User {
        return copy(
            lastActiveAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
}
