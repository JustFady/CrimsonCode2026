package org.crimsoncode2026.notifications

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.call.bodyOrNull
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.statement.HttpRequestBuilder
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.crimsoncode2026.di.AppConfig

/**
 * Request body for send-push-notification edge function
 */
@Serializable
data class SendPushNotificationRequest(
    val event_id: String,
    val severity: String,
    val category: String,
    val description: String,
    val lat: Double? = null,
    val lon: Double? = null
)

/**
 * Success response from send-push-notification edge function
 */
@Serializable
data class SendPushNotificationSuccessResponse(
    val success: Boolean,
    val notifications_sent: Int,
    val recipients: List<RecipientInfo>
)

/**
 * Error response from send-push-notification edge function
 */
@Serializable
data class SendPushNotificationErrorResponse(
    val success: Boolean,
    val error: String
)

/**
 * Recipient info from response
 */
@Serializable
data class RecipientInfo(
    val user_id: String,
    val fcm_token: String
)

/**
 * Result of sending push notification
 */
sealed class SendPushNotificationResult {
    data class Success(val notificationsSent: Int) : SendPushNotificationResult()
    data class Error(val message: String) : SendPushNotificationResult()
}

/**
 * Send Push Notification Use Case
 *
 * Calls the send-push-notification Supabase Edge Function to trigger FCM
 * notifications for private events.
 *
 * Spec requirements:
 * - Send FCM notifications to event recipients with event data payload
 * - Only used for private events (public events have no push fanout in MVP)
 * - Payload includes: event_id, severity, category, description, lat, lon, deep_link
 */
class SendPushNotificationUseCase(
    private val httpClient: HttpClient
) {

    companion object {
        private const val EDGE_FUNCTION_PATH = "functions/v1/send-push-notification"
    }

    /**
     * Send push notification for an event
     *
     * @param eventId Event ID
     * @param severity Severity (ALERT or CRISIS)
     * @param category Event category
     * @param description Event description
     * @param lat Event latitude (optional)
     * @param lon Event longitude (optional)
     * @return SendPushNotificationResult with sent count or error
     */
    suspend operator fun invoke(
        eventId: String,
        severity: String,
        category: String,
        description: String,
        lat: Double? = null,
        lon: Double? = null
    ): SendPushNotificationResult {
        return try {
            val url = "${AppConfig.supabaseUrl}/$EDGE_FUNCTION_PATH"

            val request = SendPushNotificationRequest(
                event_id = eventId,
                severity = severity,
                category = category,
                description = description,
                lat = lat,
                lon = lon
            )

            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
                headers {
                    append("Authorization", "Bearer ${AppConfig.supabaseAnonKey}")
                }
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val body = response.bodyOrNull<String>()
                    if (body != null && body.contains("\"success\":true")) {
                        // Try to parse as success response
                        try {
                            val successResponse = json.decodeFromString<SendPushNotificationSuccessResponse>(body)
                            SendPushNotificationResult.Success(successResponse.notifications_sent)
                        } catch (e: Exception) {
                            // Parsing failed but request succeeded
                            SendPushNotificationResult.Success(0)
                        }
                    } else {
                        // Response indicates failure
                        try {
                            val errorResponse = json.decodeFromString<SendPushNotificationErrorResponse>(body)
                            SendPushNotificationResult.Error(errorResponse.error)
                        } catch (e: Exception) {
                            SendPushNotificationResult.Error("Unknown error from edge function")
                        }
                    }
                }
                else -> {
                    // HTTP error
                    SendPushNotificationResult.Error(
                        "HTTP ${response.status.value}: ${response.status.description}"
                    )
                }
            }
        } catch (e: Exception) {
            SendPushNotificationResult.Error(e.message ?: "Failed to send push notification")
        }
    }
}
