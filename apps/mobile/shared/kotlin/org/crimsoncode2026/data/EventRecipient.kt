package org.crimsoncode2026.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Event delivery status
 */
enum class DeliveryStatus(val value: String) {
    @SerialName("PENDING")
    PENDING("PENDING"),

    @SerialName("SENT")
    SENT("SENT"),

    @SerialName("FAILED")
    FAILED("FAILED");

    companion object {
        fun fromValue(value: String?): DeliveryStatus? = values().find { it.value == value }
    }
}

/**
 * Event Recipient data model
 *
 * Per-recipient tracking for private events only.
 * Note: Event clearing/dismissal is client-side only (stored in local device cache), not in the database.
 *
 * @property eventId Which event (foreign key to events.id) - part of composite PK
 * @property userId Which recipient (foreign key to users.id) - part of composite PK
 * @property deliveryStatus PENDING/SENT/FAILED delivery tracking
 * @property notifiedAt Push or realtime sent time (nullable, milliseconds since epoch)
 * @property openedAt User opened event (nullable, milliseconds since epoch)
 */
@Serializable
data class EventRecipient(
    @SerialName("event_id")
    val eventId: String,

    @SerialName("user_id")
    val userId: String,

    @SerialName("delivery_status")
    val deliveryStatus: String = DeliveryStatus.PENDING.value,

    @SerialName("notified_at")
    val notifiedAt: Long? = null,

    @SerialName("opened_at")
    val openedAt: Long? = null
) {
    companion object {
        const val TABLE_NAME = "event_recipients"

        /**
         * Creates the composite primary key identifier
         */
        fun compositeKey(eventId: String, userId: String): String {
            return "$eventId:$userId"
        }
    }

    /**
     * Converts deliveryStatus string to DeliveryStatus enum
     */
    val deliveryStatusEnum: DeliveryStatus?
        get() = DeliveryStatus.fromValue(deliveryStatus)

    /**
     * Checks if notification was sent
     */
    val isNotified: Boolean
        get() = notifiedAt != null && deliveryStatusEnum == DeliveryStatus.SENT

    /**
     * Checks if notification failed
     */
    val isFailed: Boolean
        get() = deliveryStatusEnum == DeliveryStatus.FAILED

    /**
     * Checks if recipient has opened the event
     */
    val isOpened: Boolean
        get() = openedAt != null

    /**
     * Gets composite key for this recipient
     */
    val compositeKey: String
        get() = compositeKey(eventId, userId)

    /**
     * Creates a copy with delivery status updated to sent
     */
    fun markSent(): EventRecipient {
        return copy(
            deliveryStatus = DeliveryStatus.SENT.value,
            notifiedAt = System.currentTimeMillis()
        )
    }

    /**
     * Creates a copy with delivery status updated to failed
     */
    fun markFailed(): EventRecipient {
        return copy(
            deliveryStatus = DeliveryStatus.FAILED.value,
            notifiedAt = System.currentTimeMillis()
        )
    }

    /**
     * Creates a copy with opened timestamp
     */
    fun markOpened(): EventRecipient {
        return copy(
            openedAt = System.currentTimeMillis()
        )
    }
}
