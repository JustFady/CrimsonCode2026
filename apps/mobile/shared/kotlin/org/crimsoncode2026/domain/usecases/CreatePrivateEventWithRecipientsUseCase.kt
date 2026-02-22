package org.crimsoncode2026.domain.usecases

import org.crimsoncode2026.data.Event
import org.crimsoncode2026.data.EventRecipient
import org.crimsoncode2026.data.EventRecipientRepository
import org.crimsoncode2026.data.EventRepository
import org.crimsoncode2026.data.User
import org.crimsoncode2026.data.UserContact
import org.crimsoncode2026.data.UserContactRepository
import org.crimsoncode2026.data.UserRepository
import org.crimsoncode2026.domain.UserSessionManager
import org.crimsoncode2026.notifications.SendPushNotificationUseCase
import org.crimsoncode2026.notifications.SendPushNotificationResult

/**
 * Result of creating private event with recipients
 */
sealed class CreatePrivateEventWithRecipientsResult {
    data class Success(
        val event: Event,
        val recipients: List<EventRecipient>
    ) : CreatePrivateEventWithRecipientsResult()

    data class Error(val message: String) : CreatePrivateEventWithRecipientsResult()
}

/**
 * Create Private Event with Recipients Use Case
 *
 * Creates an event and for private broadcasts, creates EventRecipient records
 * for each selected contact. Matches contact phone numbers to Users table to get
 * recipient user IDs.
 *
 * Spec requirements:
 * - "When user creates a private event, create EventRecipient records for each selected contact"
 * - "Query UserContacts to get matched users, create recipient entries with PENDING status"
 */
class CreatePrivateEventWithRecipientsUseCase(
    private val eventRepository: EventRepository,
    private val eventRecipientRepository: EventRecipientRepository,
    private val userContactRepository: UserContactRepository,
    private val userRepository: UserRepository,
    private val userSessionManager: UserSessionManager,
    private val sendPushNotificationUseCase: SendPushNotificationUseCase
) {

    /**
     * Create a private event with recipients
     *
     * For public events, only creates the event without recipients.
     * For private events, creates the event and EventRecipient records for each
     * matched contact user.
     *
     * @param event Event to create
     * @param selectedContactIds List of selected contact IDs (for private events only)
     * @return CreatePrivateEventWithRecipientsResult with created event/recipients or error
     */
    suspend operator fun invoke(
        event: Event,
        selectedContactIds: List<String> = emptyList()
    ): CreatePrivateEventWithRecipientsResult {
        // Step 1: Get current user ID
        val creatorId = userSessionManager.getCurrentUserId()
            ?: return CreatePrivateEventWithRecipientsResult.Error("User not authenticated")

        // Step 2: Validate event has correct creator ID
        val eventWithCreator = event.copy(creatorId = creatorId)

        // Step 3: Create the event
        val eventResult = eventRepository.createEvent(eventWithCreator)
        val createdEvent = when (eventResult) {
            is Result.Success -> eventResult.getOrNull()
            is Result.Failure -> return CreatePrivateEventWithRecipientsResult.Error(
                eventResult.exceptionOrNull()?.message ?: "Failed to create event"
            )
        } ?: return CreatePrivateEventWithRecipientsResult.Error("Failed to create event")

        // Step 4: If public event, return early (no recipients needed)
        if (eventWithCreator.isPublic) {
            return CreatePrivateEventWithRecipientsResult.Success(
                event = createdEvent,
                recipients = emptyList()
            )
        }

        // Step 5: For private events, get selected contacts
        if (selectedContactIds.isEmpty()) {
            return CreatePrivateEventWithRecipientsResult.Error(
                "Private events require at least one selected contact"
            )
        }

        val contactsResult = userContactRepository.getContactsByUserId(creatorId)
        val contacts = when (contactsResult) {
            is Result.Success -> contactsResult.getOrNull() ?: emptyList()
            is Result.Failure -> return CreatePrivateEventWithRecipientsResult.Error(
                contactsResult.exceptionOrNull()?.message ?: "Failed to load contacts"
            )
        }

        val selectedContacts = contacts.filter { it.id in selectedContactIds }

        if (selectedContacts.isEmpty()) {
            return CreatePrivateEventWithRecipientsResult.Error("No valid contacts selected")
        }

        // Step 6: For each selected contact, look up matching user by phone number
        val recipients = mutableListOf<EventRecipient>()

        for (contact in selectedContacts) {
            // Skip contacts that don't have a registered user
            if (contact.contactUserId == null) {
                continue
            }

            // Look up the user to get their current phone number
            val userResult = userRepository.getUserById(contact.contactUserId)
            val user = when (userResult) {
                is Result.Success -> userResult.getOrNull()
                is Result.Failure -> continue
            } ?: continue

            // Verify the user still has the same phone number as the contact
            if (user.phoneNumber != contact.contactPhoneNumber) {
                continue
            }

            // Create recipient record with PENDING status
            recipients.add(
                EventRecipient(
                    eventId = createdEvent.id,
                    userId = user.id,
                    deliveryStatus = org.crimsoncode2026.data.DeliveryStatus.PENDING.value,
                    notifiedAt = null,
                    openedAt = null
                )
            )
        }

        // Step 7: If no valid recipients found, return error
        if (recipients.isEmpty()) {
            return CreatePrivateEventWithRecipientsResult.Error(
                "None of the selected contacts are app users"
            )
        }

        // Step 8: Batch insert recipient records
        val recipientsResult = eventRecipientRepository.createRecipients(recipients)

        // Step 9: Trigger push notifications for private events
        if (recipients is kotlin.Result.Success) {
            when (val pushResult = sendPushNotificationUseCase(
                eventId = createdEvent.id,
                severity = createdEvent.severity,
                category = createdEvent.category,
                description = createdEvent.description,
                lat = createdEvent.lat,
                lon = createdEvent.lon
            )) {
                is SendPushNotificationResult.Success -> {
                    // Push notifications sent successfully
                    // notifications_sent count can be logged if needed
                }
                is SendPushNotificationResult.Error -> {
                    // Log error but don't fail the entire operation
                    // Push notification failure is non-critical for event creation
                }
            }
        }

        return when (recipientsResult) {
            is Result.Success -> {
                CreatePrivateEventWithRecipientsResult.Success(
                    event = createdEvent,
                    recipients = recipientsResult.getOrNull() ?: emptyList()
                )
            }
            is Result.Failure -> {
                // Note: Event was created but recipients failed
                // Consider implementing cleanup/retry logic in future
                CreatePrivateEventWithRecipientsResult.Error(
                    recipientsResult.exceptionOrNull()?.message ?: "Failed to create recipients"
                )
            }
        }
    }

    /**
     * Create a public event (no recipients)
     *
     * @param event Event to create
     * @return CreatePrivateEventWithRecipientsResult with created event or error
     */
    suspend fun createPublicEvent(event: Event): CreatePrivateEventWithRecipientsResult {
        return invoke(event, selectedContactIds = emptyList())
    }
}
