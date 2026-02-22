package org.crimsoncode2026.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.RealtimeMessage
import io.github.jan.supabase.realtime.channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * Realtime event types
 */
enum class RealtimeEventType(val value: String) {
    EVENT_CREATED("event_created"),
    EVENT_UPDATED("event_updated"),
    EVENT_DELETED("event_deleted");

    companion object {
        fun fromValue(value: String?): RealtimeEventType? =
            values().find { it.value == value }
    }
}

/**
 * Realtime message payload for event broadcasts
 */
@Serializable
data class RealtimeEventPayload(
    @SerialName("event_id")
    val eventId: String,

    @SerialName("severity")
    val severity: String,

    @SerialName("category")
    val category: String,

    @SerialName("broadcast_type")
    val broadcastType: String,

    @SerialName("creator_id")
    val creatorId: String? = null,

    @SerialName("creator_display_name")
    val creatorDisplayName: String? = null,

    @SerialName("is_anonymous")
    val isAnonymous: Boolean = true,

    @SerialName("description")
    val description: String,

    @SerialName("lat")
    val lat: Double,

    @SerialName("lon")
    val lon: Double,

    @SerialName("created_at")
    val createdAt: Long
)

/**
 * Listener for realtime events
 */
interface RealtimeEventListener {
    fun onEventCreated(payload: RealtimeEventPayload) {}
    fun onEventUpdated(payload: RealtimeEventPayload) {}
    fun onEventDeleted(payload: RealtimeEventPayload) {}
    fun onError(error: Throwable) {}
    fun onStatusChanged(status: RealtimeChannelStatus) {}
}

/**
 * Realtime channel status
 */
enum class RealtimeChannelStatus(val value: String) {
    CONNECTING("JOINING"),
    CONNECTED("SUBSCRIBED"),
    DISCONNECTED("CLOSED"),
    ERROR("CHANNEL_ERROR");

    companion object {
        fun fromValue(value: String?): RealtimeChannelStatus? =
            values().find { it.value == value }
    }
}

/**
 * Repository interface for Realtime service operations
 */
interface RealtimeService {

    /**
     * Current connection status
     */
    val connectionStatus: StateFlow<RealtimeChannelStatus>

    /**
     * Subscribe to user's private notification channel
     * @param userId User ID for the private channel
     * @param listener Event listener
     * @return Job that can be cancelled to unsubscribe
     */
    fun subscribeToUserChannel(
        userId: String,
        listener: RealtimeEventListener
    ): Job

    /**
     * Subscribe to public events channel (for geo-fencing broadcasts)
     * @param listener Event listener
     * @return Job that can be cancelled to unsubscribe
     */
    fun subscribeToPublicChannel(
        listener: RealtimeEventListener
    ): Job

    /**
     * Unsubscribe from all channels
     */
    fun unsubscribeAll()

    /**
     * Check if user channel is subscribed
     */
    fun isUserChannelSubscribed(userId: String): Boolean
}

/**
 * Supabase implementation of RealtimeService
 *
 * Channel naming strategy:
 * - Private user channel: `user:{userId}:notifications`
 * - Public events channel: `public:events`
 *
 * Uses broadcast with private channels for security and RLS policy enforcement.
 */
class RealtimeServiceImpl(
    private val realtime: Realtime,
    private val scope: CoroutineScope,
    private val json: Json = SupabaseConfig.jsonSerializer
) : RealtimeService {

    companion object {
        private const val USER_CHANNEL_PREFIX = "user"
        private const val PUBLIC_CHANNEL_NAME = "public:events"
        private const val NOTIFICATIONS_SUFFIX = "notifications"

        /**
         * Build user channel name
         * Pattern: user:{userId}:notifications
         */
        fun buildUserChannelName(userId: String): String =
            "$USER_CHANNEL_PREFIX:$userId:$NOTIFICATIONS_SUFFIX"
    }

    private val _connectionStatus = MutableStateFlow<RealtimeChannelStatus>(RealtimeChannelStatus.DISCONNECTED)
    override val connectionStatus: StateFlow<RealtimeChannelStatus> = _connectionStatus

    private val activeChannels = mutableMapOf<String, RealtimeChannel>()
    private val channelJobs = mutableMapOf<String, Job>()

    override fun subscribeToUserChannel(
        userId: String,
        listener: RealtimeEventListener
    ): Job {
        val channelName = buildUserChannelName(userId)

        // Check if already subscribed
        if (activeChannels.containsKey(channelName)) {
            return channelJobs[channelName] ?: Job()
        }

        return subscribeToChannel(
            channelName = channelName,
            listener = listener,
            isPrivate = true
        )
    }

    override fun subscribeToPublicChannel(listener: RealtimeEventListener): Job {
        // Check if already subscribed
        if (activeChannels.containsKey(PUBLIC_CHANNEL_NAME)) {
            return channelJobs[PUBLIC_CHANNEL_NAME] ?: Job()
        }

        return subscribeToChannel(
            channelName = PUBLIC_CHANNEL_NAME,
            listener = listener,
            isPrivate = true // Use private for RLS policy enforcement
        )
    }

    private fun subscribeToChannel(
        channelName: String,
        listener: RealtimeEventListener,
        isPrivate: Boolean
    ): Job {
        return scope.launch {
            try {
                _connectionStatus.value = RealtimeChannelStatus.CONNECTING

                val channel = realtime.channel(channelName) {
                    config {
                        broadcast {
                            self = true // Receive own messages
                            ack = true // Get acknowledgment
                        }
                        if (isPrivate) {
                            private = true // Require authentication and RLS
                        }
                    }
                }

                // Register event listeners
                channel
                    .onBroadcast(
                        event = RealtimeEventType.EVENT_CREATED.value
                    ) { payload ->
                        handleBroadcast(payload, listener, RealtimeEventType.EVENT_CREATED)
                    }
                    .onBroadcast(
                        event = RealtimeEventType.EVENT_UPDATED.value
                    ) { payload ->
                        handleBroadcast(payload, listener, RealtimeEventType.EVENT_UPDATED)
                    }
                    .onBroadcast(
                        event = RealtimeEventType.EVENT_DELETED.value
                    ) { payload ->
                        handleBroadcast(payload, listener, RealtimeEventType.EVENT_DELETED)
                    }
                    .subscribe { status, error ->
                        val newStatus = RealtimeChannelStatus.fromValue(status)
                        if (newStatus != null) {
                            _connectionStatus.value = newStatus
                            listener.onStatusChanged(newStatus)
                        }
                        if (error != null) {
                            listener.onError(error)
                        }
                    }

                activeChannels[channelName] = channel
            } catch (e: Exception) {
                _connectionStatus.value = RealtimeChannelStatus.ERROR
                listener.onError(e)
            }
        }.also { job ->
            channelJobs[channelName] = job
            job.invokeOnCompletion {
                // Clean up when job is cancelled
                activeChannels.remove(channelName)
                channelJobs.remove(channelName)
            }
        }
    }

    private fun handleBroadcast(
        payload: RealtimeMessage,
        listener: RealtimeEventListener,
        eventType: RealtimeEventType
    ) {
        try {
            val eventPayload = json.decodeFromJsonElement<RealtimeEventPayload>(payload.data)

            when (eventType) {
                RealtimeEventType.EVENT_CREATED -> listener.onEventCreated(eventPayload)
                RealtimeEventType.EVENT_UPDATED -> listener.onEventUpdated(eventPayload)
                RealtimeEventType.EVENT_DELETED -> listener.onEventDeleted(eventPayload)
            }
        } catch (e: Exception) {
            listener.onError(e)
        }
    }

    override fun unsubscribeAll() {
        activeChannels.values.forEach { channel ->
            realtime.removeChannel(channel)
        }
        activeChannels.clear()
        channelJobs.values.forEach { it.cancel() }
        channelJobs.clear()
        _connectionStatus.value = RealtimeChannelStatus.DISCONNECTED
    }

    override fun isUserChannelSubscribed(userId: String): Boolean {
        val channelName = buildUserChannelName(userId)
        return activeChannels.containsKey(channelName)
    }
}
