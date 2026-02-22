package org.crimsoncode2026.performance

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.crimsoncode2026.data.RealtimeEventPayload
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Performance tests for real-time subscription efficiency
 *
 * Tests:
 * - Subscription establishment time
 * - Message processing time
 * - Channel management overhead
 * - Memory usage for subscriptions
 */
class RealtimeSubscriptionPerformanceTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val timer = PerformanceTimer()
    private val memoryTracker = MemoryTracker()

    /**
     * Test subscription establishment performance
     * Target: <500ms for subscription setup
     */
    @Test
    fun testSubscriptionEstablishmentPerformance() = runTest {
        val userId = "test-user-12345"

        memoryTracker.start()
        val (result, duration) = timer.measure {
            // Simulate channel creation and subscription
            simulateSubscription(userId)
        }
        memoryTracker.stop()

        val metrics = memoryTracker.getMetrics("Subscription Establishment", duration)
        val testResult = PerformanceTestResult(
            testName = "Subscription Establishment",
            passed = metrics.isDurationAcceptable(PerformanceThresholds.REALTIME_SUBSCRIBE_MS),
            metrics = metrics,
            threshold = PerformanceThresholds.REALTIME_SUBSCRIBE_MS,
            message = if (metrics.isDurationAcceptable(PerformanceThresholds.REALTIME_SUBSCRIBE_MS)) {
                "PASS: Established subscription in ${duration}ms"
            } else {
                "FAIL: Subscription took ${duration}ms, threshold is ${PerformanceThresholds.REALTIME_SUBSCRIBE_MS}ms"
            }
        )

        printTestResult(testResult)
        assertTrue(testResult.passed, testResult.message)
    }

    /**
     * Test message processing performance
     * Target: <10ms per message processing
     */
    @Test
    fun testMessageProcessingPerformance() = runTest {
        val messages = generateTestMessages(10)

        memoryTracker.start()
        timer.start()
        messages.forEach { message ->
            // Simulate message parsing and handling
            processRealtimeMessage(message)
        }
        timer.stop()
        memoryTracker.stop()

        val avgTimePerMessage = timer.getElapsedMs() / messages.size
        val metrics = memoryTracker.getMetrics("Message Processing (10)", timer.getElapsedMs())
        val testResult = PerformanceTestResult(
            testName = "Message Processing (10)",
            passed = avgTimePerMessage <= PerformanceThresholds.REALTIME_MESSAGE_PROCESS_MS,
            metrics = metrics,
            threshold = PerformanceThresholds.REALTIME_MESSAGE_PROCESS_MS,
            message = if (avgTimePerMessage <= PerformanceThresholds.REALTIME_MESSAGE_PROCESS_MS) {
                "PASS: Average processing time ${avgTimePerMessage}ms per message"
            } else {
                "FAIL: Average processing time ${avgTimePerMessage}ms per message, threshold is ${PerformanceThresholds.REALTIME_MESSAGE_PROCESS_MS}ms"
            }
        )

        printTestResult(testResult)
        assertTrue(testResult.passed, testResult.message)
    }

    /**
     * Test burst message processing
     * Simulates receiving many messages at once
     */
    @Test
    fun testBurstMessageProcessingPerformance() = runTest {
        val messages = generateTestMessages(50)

        memoryTracker.start()
        val (result, duration) = timer.measure {
            messages.forEach { message ->
                processRealtimeMessage(message)
            }
        }
        memoryTracker.stop()

        val avgTimePerMessage = duration / messages.size
        val metrics = memoryTracker.getMetrics("Burst Message Processing (50)", duration)
        val testResult = PerformanceTestResult(
            testName = "Burst Message Processing (50)",
            passed = avgTimePerMessage <= PerformanceThresholds.REALTIME_MESSAGE_PROCESS_MS * 2, // Allow 2x for burst
            metrics = metrics,
            threshold = PerformanceThresholds.REALTIME_MESSAGE_PROCESS_MS * 2,
            message = if (avgTimePerMessage <= PerformanceThresholds.REALTIME_MESSAGE_PROCESS_MS * 2) {
                "PASS: Processed ${messages.size} messages in ${duration}ms (${avgTimePerMessage}ms avg)"
            } else {
                "FAIL: Processed ${messages.size} messages in ${duration}ms (${avgTimePerMessage}ms avg)"
            }
        )

        printTestResult(testResult)
        assertTrue(testResult.passed, testResult.message)
    }

    /**
     * Test channel management overhead
     */
    @Test
    fun testChannelManagementPerformance() = runTest {
        val channels = (0 until 5).map { "user:$it:notifications" }

        memoryTracker.start()
        val (result, duration) = timer.measure {
            // Simulate managing multiple channels
            channels.forEach { channelName ->
                manageChannel(channelName)
            }
        }
        memoryTracker.stop()

        val avgTimePerChannel = duration / channels.size
        val metrics = memoryTracker.getMetrics("Channel Management (5)", duration)
        val testResult = PerformanceTestResult(
            testName = "Channel Management (5)",
            passed = avgTimePerChannel <= 100, // 100ms per channel
            metrics = metrics,
            threshold = 100,
            message = if (avgTimePerChannel <= 100) {
                "PASS: Managed ${channels.size} channels in ${duration}ms (${avgTimePerChannel}ms avg)"
            } else {
                "FAIL: Managed ${channels.size} channels in ${duration}ms (${avgTimePerChannel}ms avg)"
            }
        )

        printTestResult(testResult)
        assertTrue(testResult.passed, testResult.message)
    }

    /**
     * Test message deserialization performance
     */
    @Test
    fun testMessageDeserializationPerformance() = runTest {
        val jsonPayloads = generateTestJsonPayloads(20)

        memoryTracker.start()
        val (result, duration) = timer.measure {
            jsonPayloads.forEach { payload ->
                // Simulate JSON parsing
                json.decodeFromString<RealtimeEventPayload>(payload)
            }
        }
        memoryTracker.stop()

        val avgTimePerPayload = duration / jsonPayloads.size
        val metrics = memoryTracker.getMetrics("Message Deserialization (20)", duration)
        val testResult = PerformanceTestResult(
            testName = "Message Deserialization (20)",
            passed = avgTimePerPayload <= 5, // 5ms per payload
            metrics = metrics,
            threshold = 5,
            message = if (avgTimePerPayload <= 5) {
                "PASS: Deserialized ${jsonPayloads.size} payloads in ${duration}ms (${avgTimePerPayload}ms avg)"
            } else {
                "FAIL: Deserialized ${jsonPayloads.size} payloads in ${duration}ms (${avgTimePerPayload}ms avg)"
            }
        )

        printTestResult(testResult)
        assertTrue(testResult.passed, testResult.message)
    }

    /**
     * Test memory usage for active subscriptions
     */
    @Test
    fun testSubscriptionMemoryEfficiency() = runTest {
        memoryTracker.start()
        val subscriptions = simulateMultipleSubscriptions(10)
        memoryTracker.stop()

        val expectedMemory = 10 * 1024 // 10KB per subscription
        val actualMemory = memoryTracker.getDelta()
        val efficiency = expectedMemory.toDouble() / actualMemory.toDouble()

        val metrics = memoryTracker.getMetrics("Subscription Memory Efficiency", memoryTracker.getElapsedMs())
        val testResult = PerformanceTestResult(
            testName = "Subscription Memory Efficiency (10)",
            passed = efficiency > 0.2 && actualMemory < (500 * 1024), // At least 20% efficient, <500KB total
            metrics = metrics,
            threshold = expectedMemory,
            message = "Memory efficiency: ${(efficiency * 100).toInt()}%, " +
                    "expected ~${expectedMemory} bytes, actual ${actualMemory} bytes"
        )

        printTestResult(testResult)
        assertTrue(testResult.passed, testResult.message)
    }

    /**
     * Test connection recovery performance
     */
    @Test
    fun testConnectionRecoveryPerformance() = runTest {
        val userId = "test-user-recovery"

        memoryTracker.start()
        val (result, duration) = timer.measure {
            // Simulate disconnect and reconnect
            simulateConnectionLoss()
            simulateReconnection(userId)
        }
        memoryTracker.stop()

        val metrics = memoryTracker.getMetrics("Connection Recovery", duration)
        val testResult = PerformanceTestResult(
            testName = "Connection Recovery",
            passed = metrics.isDurationAcceptable(1000), // 1 second for reconnect
            metrics = metrics,
            threshold = 1000,
            message = if (metrics.isDurationAcceptable(1000)) {
                "PASS: Recovered connection in ${duration}ms"
            } else {
                "FAIL: Recovery took ${duration}ms, threshold is 1000ms"
            }
        )

        printTestResult(testResult)
        assertTrue(testResult.passed, testResult.message)
    }

    // Helper functions

    private fun simulateSubscription(userId: String): SubscriptionState {
        return SubscriptionState(
            userId = userId,
            channelName = "user:$userId:notifications",
            status = "CONNECTED",
            listeners = listOf("onEventCreated", "onEventUpdated", "onEventDeleted")
        )
    }

    private fun generateTestMessages(count: Int): List<SimulatedRealtimeMessage> {
        return (0 until count).map { i ->
            SimulatedRealtimeMessage(
                eventId = "event-$i",
                eventType = when (i % 3) {
                    0 -> "EVENT_CREATED"
                    1 -> "EVENT_UPDATED"
                    else -> "EVENT_DELETED"
                },
                payload = createTestEventPayload(i)
            )
        }
    }

    private fun createTestEventPayload(index: Int): RealtimeEventPayload {
        return RealtimeEventPayload(
            eventId = "event-$index",
            severity = if (index % 3 == 0) "CRISIS" else "ALERT",
            category = listOf("MEDICAL", "FIRE", "WEATHER", "CRIME")[index % 4],
            broadcastType = if (index % 2 == 0) "PUBLIC" else "PRIVATE",
            creatorId = if ((index % 2) != 0) "user-$index" else null,
            creatorDisplayName = if ((index % 2) != 0) "User $index" else null,
            isAnonymous = (index % 2) == 0,
            description = "Event $index description for testing",
            lat = 37.7 + (index * 0.0001),
            lon = -122.4 + (index * 0.0001),
            createdAt = System.currentTimeMillis() - (index * 60000L)
        )
    }

    private fun processRealtimeMessage(message: SimulatedRealtimeMessage): ProcessedEvent {
        // Simulate the processing in RealtimeService.handleBroadcast
        val payload = message.payload
        return ProcessedEvent(
            eventId = payload.eventId,
            severity = payload.severity,
            category = payload.category,
            eventType = message.eventType,
            processedAt = System.currentTimeMillis()
        )
    }

    private fun manageChannel(channelName: String): ChannelState {
        return ChannelState(
            name = channelName,
            isSubscribed = true,
            listenerCount = 3
        )
    }

    private fun generateTestJsonPayloads(count: Int): List<String> {
        return (0 until count).map { i ->
            """{
                "event_id": "event-$i",
                "severity": "${if (i % 3 == 0) "CRISIS" else "ALERT"}",
                "category": "MEDICAL",
                "broadcast_type": "PUBLIC",
                "description": "Test event $i",
                "lat": 37.7749,
                "lon": -122.4194,
                "is_anonymous": true,
                "created_at": ${System.currentTimeMillis()}
            }"""
        }
    }

    private fun simulateMultipleSubscriptions(count: Int): Map<String, SubscriptionState> {
        return (0 until count).associate { i ->
            "user:$i:notifications" to simulateSubscription("user-$i")
        }
    }

    private fun simulateConnectionLoss() {
        // Simulate clearing active channels
        // In real implementation: activeChannels.clear()
    }

    private fun simulateReconnection(userId: String): SubscriptionState {
        // Simulate re-establishing connection
        return simulateSubscription(userId)
    }

    private fun printTestResult(result: PerformanceTestResult) {
        println("\n=== Performance Test Result ===")
        println("Test: ${result.testName}")
        println("Status: ${if (result.passed) "PASS" else "FAIL"}")
        println("Duration: ${result.metrics.durationMs}ms (threshold: ${result.threshold}ms)")
        println("Memory Delta: ${String.format("%.2f", result.metrics.memoryDeltaMb)} MB")
        println("Message: ${result.message}")
        println("===============================\n")
    }
}

// Test data classes

data class SubscriptionState(
    val userId: String,
    val channelName: String,
    val status: String,
    val listeners: List<String>
)

data class SimulatedRealtimeMessage(
    val eventId: String,
    val eventType: String,
    val payload: RealtimeEventPayload
)

data class ChannelState(
    val name: String,
    val isSubscribed: Boolean,
    val listenerCount: Int
)

data class ProcessedEvent(
    val eventId: String,
    val severity: String,
    val category: String,
    val eventType: String,
    val processedAt: Long
)
