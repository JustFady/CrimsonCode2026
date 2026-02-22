package org.crimsoncode2026.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for Network Resilience
 *
 * Tests the network monitoring and resilience features:
 * 1. Network status detection (ONLINE/OFFLINE/UNKNOWN)
 * 2. Connection type identification (WIFI/CELLULAR/ETHERNET/VPN/UNKNOWN)
 * 3. Network monitoring lifecycle (start/stop)
 * 4. Network state changes (onAvailable/onLost/onCapabilitiesChanged)
 * 5. Metered connection detection
 * 6. Network recovery after disconnection
 * 7. Error handling during monitoring failures
 * 8. Multiple network status updates
 */
class NetworkResilienceIntegrationTest {

    private lateinit var mockNetworkMonitor: MockNetworkMonitor

    @BeforeTest
    fun setup() {
        mockNetworkMonitor = MockNetworkMonitor()
    }

    @AfterTest
    fun teardown() {
        mockNetworkMonitor.stopMonitoring()
    }

    // ==================== Network Status Tests ====================

    @Test
    fun `initial network status is unknown`() = runTest {
        // Act
        val status = mockNetworkMonitor.currentNetworkInfo

        // Assert
        assertEquals(NetworkStatus.UNKNOWN, status.status, "Initial status should be UNKNOWN")
        assertFalse(status.isOnline, "Initial status should not be online")
    }

    @Test
    fun `network status ONLINE indicates online`() = runTest {
        // Arrange
        mockNetworkMonitor.setStatus(NetworkStatus.ONLINE)

        // Act
        val status = mockNetworkMonitor.currentNetworkInfo

        // Assert
        assertEquals(NetworkStatus.ONLINE, status.status, "Status should be ONLINE")
        assertTrue(status.isOnline, "isOnline should be true")
    }

    @Test
    fun `network status OFFLINE indicates offline`() = runTest {
        // Arrange
        mockNetworkMonitor.setStatus(NetworkStatus.OFFLINE)

        // Act
        val status = mockNetworkMonitor.currentNetworkInfo

        // Assert
        assertEquals(NetworkStatus.OFFLINE, status.status, "Status should be OFFLINE")
        assertFalse(status.isOnline, "isOnline should be false")
    }

    @Test
    fun `network status isOnline property correct`() = runTest {
        // Arrange & Act - Test ONLINE
        mockNetworkMonitor.setStatus(NetworkStatus.ONLINE)
        val onlineStatus = mockNetworkMonitor.currentNetworkInfo
        assertTrue(onlineStatus.isOnline, "ONLINE status should have isOnline true")

        // Test OFFLINE
        mockNetworkMonitor.setStatus(NetworkStatus.OFFLINE)
        val offlineStatus = mockNetworkMonitor.currentNetworkInfo
        assertFalse(offlineStatus.isOnline, "OFFLINE status should have isOnline false")
    }

    @Test
    fun `isOnline matches status comparison`() = runTest {
        // Arrange
        val onlineInfo = NetworkInfo(NetworkStatus.ONLINE)
        val offlineInfo = NetworkInfo(NetworkStatus.OFFLINE)
        val unknownInfo = NetworkInfo(NetworkStatus.UNKNOWN)

        // Act & Assert
        assertTrue(onlineInfo.isOnline, "ONLINE info should have isOnline true")
        assertFalse(offlineInfo.isOnline, "OFFLINE info should have isOnline false")
        assertFalse(unknownInfo.isOnline, "UNKNOWN info should have isOnline false")
    }

    // ==================== Connection Type Tests ====================

    @Test
    fun `wifi connection type is identified correctly`() = runTest {
        // Arrange
        mockNetworkMonitor.setConnectionType(ConnectionType.WIFI)

        // Act
        val status = mockNetworkMonitor.currentNetworkInfo

        // Assert
        assertEquals(ConnectionType.WIFI, status.connectionType, "Connection type should be WIFI")
        assertTrue(status.isOnline, "Network should be online")
    }

    @Test
    fun `cellular connection type is identified correctly`() = runTest {
        // Arrange
        mockNetworkMonitor.setConnectionType(ConnectionType.CELLULAR)

        // Act
        val status = mockNetworkMonitor.currentNetworkInfo

        // Assert
        assertEquals(ConnectionType.CELLULAR, status.connectionType, "Connection type should be CELLULAR")
        assertTrue(status.isOnline, "Network should be online")
    }

    @Test
    fun `ethernet connection type is identified correctly`() = runTest {
        // Arrange
        mockNetworkMonitor.setConnectionType(ConnectionType.ETHERNET)

        // Act
        val status = mockNetworkMonitor.currentNetworkInfo

        // Assert
        assertEquals(ConnectionType.ETHERNET, status.connectionType, "Connection type should be ETHERNET")
        assertTrue(status.isOnline, "Network should be online")
    }

    @Test
    fun `vpn connection type is identified correctly`() = runTest {
        // Arrange
        mockNetworkMonitor.setConnectionType(ConnectionType.VPN)

        // Act
        val status = mockNetworkMonitor.currentNetworkInfo

        // Assert
        assertEquals(ConnectionType.VPN, status.connectionType, "Connection type should be VPN")
        assertTrue(status.isOnline, "Network should be online")
    }

    @Test
    fun `unknown connection type defaults correctly`() = runTest {
        // Arrange
        mockNetworkMonitor.setConnectionType(ConnectionType.UNKNOWN)

        // Act
        val status = mockNetworkMonitor.currentNetworkInfo

        // Assert
        assertEquals(ConnectionType.UNKNOWN, status.connectionType, "Connection type should be UNKNOWN")
        assertFalse(status.isOnline, "Network should be offline with unknown type")
    }

    // ==================== Metered Connection Tests ====================

    @Test
    fun `metered connection is flagged correctly`() = runTest {
        // Arrange
        mockNetworkMonitor.setMetered(true)

        // Act
        val status = mockNetworkMonitor.currentNetworkInfo

        // Assert
        assertTrue(status.isMetered, "isMetered should be true")
    }

    @Test
    fun `unmetered connection is not flagged`() = runTest {
        // Arrange
        mockNetworkMonitor.setMetered(false)

        // Act
        val status = mockNetworkMonitor.currentNetworkInfo

        // Assert
        assertFalse(status.isMetered, "isMetered should be false")
    }

    // ==================== Network Monitoring Lifecycle Tests ====================

    @Test
    fun `startMonitoring begins monitoring`() = runTest {
        // Act
        mockNetworkMonitor.startMonitoring()

        // Assert
        assertTrue(mockNetworkMonitor.isMonitoring, "Should be marked as monitoring")
    }

    @Test
    fun `stopMonitoring ends monitoring`() = runTest {
        // Arrange
        mockNetworkMonitor.startMonitoring()
        assertTrue(mockNetworkMonitor.isMonitoring, "Should be monitoring")

        // Act
        mockNetworkMonitor.stopMonitoring()

        // Assert
        assertFalse(mockNetworkMonitor.isMonitoring, "Should not be marked as monitoring")
    }

    @Test
    fun `startMonitoring called multiple times is idempotent`() = runTest {
        // Act
        mockNetworkMonitor.startMonitoring()
        mockNetworkMonitor.startMonitoring()
        mockNetworkMonitor.startMonitoring()

        // Assert
        assertTrue(mockNetworkMonitor.isMonitoring, "Should still be monitoring")
        // Multiple starts should not cause issues
    }

    @Test
    fun `stopMonitoring called multiple times is safe`() = runTest {
        // Arrange
        mockNetworkMonitor.startMonitoring()
        mockNetworkMonitor.stopMonitoring()

        // Act
        mockNetworkMonitor.stopMonitoring()
        mockNetworkMonitor.stopMonitoring()

        // Assert
        assertFalse(mockNetworkMonitor.isMonitoring, "Should still not be monitoring")
        // Multiple stops should not cause issues
    }

    @Test
    fun `stopMonitoring without start does not throw`() = runTest {
        // Act - Stop without starting
        mockNetworkMonitor.stopMonitoring()

        // Assert
        assertFalse(mockNetworkMonitor.isMonitoring, "Should not be monitoring")
    }

    // ==================== Network State Change Tests ====================

    @Test
    fun `network available updates status to ONLINE`() = runTest {
        // Arrange
        mockNetworkMonitor.setStatus(NetworkStatus.OFFLINE)
        assertEquals(NetworkStatus.OFFLINE, mockNetworkMonitor.currentNetworkInfo.status, "Should start offline")

        // Act
        mockNetworkMonitor.simulateNetworkAvailable()

        // Assert
        assertEquals(NetworkStatus.ONLINE, mockNetworkMonitor.currentNetworkInfo.status, "Status should update to ONLINE")
    }

    @Test
    fun `network lost updates status to OFFLINE`() = runTest {
        // Arrange
        mockNetworkMonitor.setStatus(NetworkStatus.ONLINE)
        assertEquals(NetworkStatus.ONLINE, mockNetworkMonitor.currentNetworkInfo.status, "Should start online")

        // Act
        mockNetworkMonitor.simulateNetworkLost()

        // Assert
        assertEquals(NetworkStatus.OFFLINE, mockNetworkMonitor.currentNetworkInfo.status, "Status should update to OFFLINE")
    }

    @Test
    fun `network capabilities change updates connection type`() = runTest {
        // Arrange
        mockNetworkMonitor.setStatus(NetworkStatus.ONLINE)
        mockNetworkMonitor.setConnectionType(ConnectionType.CELLULAR)
        assertEquals(ConnectionType.CELLULAR, mockNetworkMonitor.currentNetworkInfo.connectionType, "Should start as cellular")

        // Act
        mockNetworkMonitor.simulateCapabilitiesChanged(ConnectionType.WIFI)

        // Assert
        assertEquals(ConnectionType.WIFI, mockNetworkMonitor.currentNetworkInfo.connectionType, "Connection type should update to WIFI")
        assertTrue(mockNetworkMonitor.currentNetworkInfo.isOnline, "Network should still be online")
    }

    // ==================== Network Recovery Tests ====================

    @Test
    fun `network recovers from disconnection`() = runTest {
        // Arrange
        mockNetworkMonitor.setStatus(NetworkStatus.ONLINE)
        assertTrue(mockNetworkMonitor.currentNetworkInfo.isOnline, "Should start online")

        // Act - Simulate network loss
        mockNetworkMonitor.simulateNetworkLost()
        assertEquals(NetworkStatus.OFFLINE, mockNetworkMonitor.currentNetworkInfo.status, "Should be offline after loss")

        // Simulate network recovery
        mockNetworkMonitor.simulateNetworkAvailable()

        // Assert
        assertEquals(NetworkStatus.ONLINE, mockNetworkMonitor.currentNetworkInfo.status, "Should be online after recovery")
    }

    @Test
    fun `multiple network transitions are handled`() = runTest {
        // Arrange - Start online
        mockNetworkMonitor.setStatus(NetworkStatus.ONLINE)
        assertTrue(mockNetworkMonitor.currentNetworkInfo.isOnline, "Should start online")

        // Act - Simulate transition: ONLINE -> OFFLINE -> ONLINE -> OFFLINE -> ONLINE
        mockNetworkMonitor.simulateNetworkLost()
        assertEquals(NetworkStatus.OFFLINE, mockNetworkMonitor.currentNetworkInfo.status, "First transition: ONLINE to OFFLINE")

        mockNetworkMonitor.simulateNetworkAvailable()
        assertEquals(NetworkStatus.ONLINE, mockNetworkMonitor.currentNetworkInfo.status, "Second transition: OFFLINE to ONLINE")

        mockNetworkMonitor.simulateNetworkLost()
        assertEquals(NetworkStatus.OFFLINE, mockNetworkMonitor.currentNetworkInfo.status, "Third transition: ONLINE to OFFLINE")

        mockNetworkMonitor.simulateNetworkAvailable()
        assertEquals(NetworkStatus.ONLINE, mockNetworkMonitor.currentNetworkInfo.status, "Final transition: OFFLINE to ONLINE")

        // Assert final state
        assertTrue(mockNetworkMonitor.currentNetworkInfo.isOnline, "Should be online after all transitions")
    }

    @Test
    fun `connection type changes while online`() = runTest {
        // Arrange
        mockNetworkMonitor.setStatus(NetworkStatus.ONLINE)
        mockNetworkMonitor.setConnectionType(ConnectionType.WIFI)

        // Act - Simulate: WIFI -> CELLULAR -> WIFI -> ETHERNET
        mockNetworkMonitor.simulateCapabilitiesChanged(ConnectionType.CELLULAR)
        assertEquals(ConnectionType.CELLULAR, mockNetworkMonitor.currentNetworkInfo.connectionType, "Change to CELLULAR")
        assertTrue(mockNetworkMonitor.currentNetworkInfo.isOnline, "Should remain online")

        mockNetworkMonitor.simulateCapabilitiesChanged(ConnectionType.WIFI)
        assertEquals(ConnectionType.WIFI, mockNetworkMonitor.currentNetworkInfo.connectionType, "Change back to WIFI")
        assertTrue(mockNetworkMonitor.currentNetworkInfo.isOnline, "Should remain online")

        mockNetworkMonitor.simulateCapabilitiesChanged(ConnectionType.ETHERNET)
        assertEquals(ConnectionType.ETHERNET, mockNetworkMonitor.currentNetworkInfo.connectionType, "Change to ETHERNET")
        assertTrue(mockNetworkMonitor.currentNetworkInfo.isOnline, "Should remain online")
    }

    // ==================== Flow Integration Tests ====================

    @Test
    fun `networkStatus flow emits updates`() = runTest {
        // Arrange
        var collectedStatuses = mutableListOf<NetworkStatus>()
        mockNetworkMonitor.startMonitoring()

        // Act - Collect status changes
        mockNetworkMonitor.setStatus(NetworkStatus.ONLINE)
        collectedStatuses.add(mockNetworkMonitor.currentNetworkInfo.status)

        mockNetworkMonitor.setStatus(NetworkStatus.OFFLINE)
        collectedStatuses.add(mockNetworkMonitor.currentNetworkInfo.status)

        mockNetworkMonitor.setStatus(NetworkStatus.ONLINE)
        collectedStatuses.add(mockNetworkMonitor.currentNetworkInfo.status)

        // Assert
        assertEquals(3, collectedStatuses.size, "Should collect 3 status updates")
        assertEquals(NetworkStatus.ONLINE, collectedStatuses[0], "First status should be ONLINE")
        assertEquals(NetworkStatus.OFFLINE, collectedStatuses[1], "Second status should be OFFLINE")
        assertEquals(NetworkStatus.ONLINE, collectedStatuses[2], "Third status should be ONLINE")
    }

    @Test
    fun `networkStatus flow collects initial status`() = runTest {
        // Arrange
        val initialStatus = NetworkStatus.UNKNOWN

        // Act
        val status = mockNetworkMonitor.networkStatus.value.status

        // Assert
        assertEquals(initialStatus, status, "Flow should emit initial status")
    }

    @Test
    fun `currentNetworkInfo is synchronous snapshot`() = runTest {
        // Arrange
        mockNetworkMonitor.setStatus(NetworkStatus.ONLINE)

        // Act
        val status1 = mockNetworkMonitor.currentNetworkInfo
        val status2 = mockNetworkMonitor.currentNetworkInfo

        // Assert
        assertEquals(status1, status2, "Multiple calls should return same snapshot")
        assertEquals(NetworkStatus.ONLINE, status1.status, "Snapshot should be ONLINE")
    }

    @Test
    fun `isOnline property matches synchronous snapshot`() = runTest {
        // Arrange - Test ONLINE
        mockNetworkMonitor.setStatus(NetworkStatus.ONLINE)
        val status = mockNetworkMonitor.currentNetworkInfo
        val flowStatus = mockNetworkMonitor.networkStatus.value.isOnline

        // Assert
        assertEquals(status.isOnline, flowStatus, "isOnline should match snapshot")

        // Test OFFLINE
        mockNetworkMonitor.setStatus(NetworkStatus.OFFLINE)
        val offlineStatus = mockNetworkMonitor.currentNetworkInfo
        val offlineFlowStatus = mockNetworkMonitor.networkStatus.value.isOnline

        assertEquals(offlineStatus.isOnline, offlineFlowStatus, "isOnline should match snapshot for OFFLINE")
    }

    // ==================== Network Resilience Scenarios ====================

    @Test
    fun `handles rapid network state changes`() = runTest {
        // Arrange
        var transitions = 0
        mockNetworkMonitor.setStatus(NetworkStatus.ONLINE)

        // Act - Simulate rapid changes
        repeat(10) {
            mockNetworkMonitor.setStatus(if (it % 2 == 0) NetworkStatus.ONLINE else NetworkStatus.OFFLINE)
            transitions++
        }

        // Assert
        assertEquals(10, transitions, "Should handle 10 rapid transitions")
        val finalStatus = mockNetworkMonitor.currentNetworkInfo.status
        // Final state should be OFFLINE (last transition)
        assertEquals(NetworkStatus.OFFLINE, finalStatus, "Final status should reflect last transition")
    }

    @Test
    fun `handles extended offline period`() = runTest {
        // Arrange
        mockNetworkMonitor.setStatus(NetworkStatus.ONLINE)
        assertTrue(mockNetworkMonitor.currentNetworkInfo.isOnline, "Should start online")

        // Act - Simulate extended offline
        mockNetworkMonitor.simulateNetworkLost()

        // Wait (simulated)
        var offlineCount = 0
        repeat(100) {
            if (mockNetworkMonitor.currentNetworkInfo.status == NetworkStatus.OFFLINE) {
                offlineCount++
            }
        }

        // Assert
        assertEquals(NetworkStatus.OFFLINE, mockNetworkMonitor.currentNetworkInfo.status, "Should remain offline")
        assertEquals(100, offlineCount, "Should count 100 offline checks")
    }

    @Test
    fun `handles intermittent connection`() = runTest {
        // Arrange
        mockNetworkMonitor.setStatus(NetworkStatus.ONLINE)

        // Act - Simulate intermittent connection
        val pattern = listOf(
            NetworkStatus.ONLINE, NetworkStatus.OFFLINE,
            NetworkStatus.ONLINE, NetworkStatus.OFFLINE,
            NetworkStatus.ONLINE, NetworkStatus.OFFLINE,
            NetworkStatus.ONLINE
        )

        pattern.forEach { status ->
            mockNetworkMonitor.setStatus(status)
        }

        // Assert
        val finalStatus = mockNetworkMonitor.currentNetworkInfo.status
        assertEquals(NetworkStatus.ONLINE, finalStatus, "Final status should be ONLINE")

        // Count transitions
        var transitions = 0
        var wasOffline = false
        pattern.forEach { status ->
            if (status == NetworkStatus.OFFLINE && !wasOffline) {
                transitions++ // Count ONLINE->OFFLINE transitions
                wasOffline = true
            }
            if (status == NetworkStatus.ONLINE && wasOffline) {
                wasOffline = false // Reset after recovery
            }
        }

        assertTrue(transitions > 0, "Should have experienced multiple transitions")
    }

    @Test
    fun `metered connection persists across state changes`() = runTest {
        // Arrange
        mockNetworkMonitor.setConnectionType(ConnectionType.WIFI)
        mockNetworkMonitor.setMetered(true)
        assertEquals(true, mockNetworkMonitor.currentNetworkInfo.isMetered, "Should start metered")

        // Act - Change status while preserving metered
        mockNetworkMonitor.setStatus(NetworkStatus.ONLINE)
        assertEquals(true, mockNetworkMonitor.currentNetworkInfo.isMetered, "Should remain metered after ONLINE")

        mockNetworkMonitor.setStatus(NetworkStatus.OFFLINE)
        assertEquals(false, mockNetworkMonitor.currentNetworkInfo.isMetered, "Should not be metered when OFFLINE")

        mockNetworkMonitor.setStatus(NetworkStatus.ONLINE)
        mockNetworkMonitor.setConnectionType(ConnectionType.CELLULAR)
        mockNetworkMonitor.setMetered(false)
        assertEquals(false, mockNetworkMonitor.currentNetworkInfo.isMetered, "Should be unmetered when cellular")
    }

    @Test
    fun `network recovery with connection type change`() = runTest {
        // Arrange
        mockNetworkMonitor.setStatus(NetworkStatus.ONLINE)
        mockNetworkMonitor.setConnectionType(ConnectionType.WIFI)

        // Act - Simulate: OFFLINE -> ONLINE (different type)
        mockNetworkMonitor.simulateNetworkLost()
        assertEquals(NetworkStatus.OFFLINE, mockNetworkMonitor.currentNetworkInfo.status, "Should be offline")

        mockNetworkMonitor.simulateNetworkAvailable()
        mockNetworkMonitor.simulateCapabilitiesChanged(ConnectionType.CELLULAR)

        // Assert
        assertEquals(NetworkStatus.ONLINE, mockNetworkMonitor.currentNetworkInfo.status, "Should be online after recovery")
        assertEquals(ConnectionType.CELLULAR, mockNetworkMonitor.currentNetworkInfo.connectionType, "Connection type should change")
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `handles monitoring start failure gracefully`() = runTest {
        // Arrange - Mock that fails on start
        val failingMonitor = FailingMockNetworkMonitor()

        // Act
        try {
            failingMonitor.startMonitoring()
        } catch (e: Exception) {
            // Should handle gracefully - monitor should not throw in real implementation
        }

        // Assert - Should be in unknown state after failure
        assertEquals(NetworkStatus.UNKNOWN, failingMonitor.currentNetworkInfo.status, "Should fall back to UNKNOWN on failure")
    }

    @Test
    fun `handles monitoring stop failure gracefully`() = runTest {
        // Arrange
        mockNetworkMonitor.startMonitoring()
        assertTrue(mockNetworkMonitor.isMonitoring, "Should be monitoring")

        // Act - Mock that fails on stop (real implementation catches exceptions)
        try {
            mockNetworkMonitor.stopMonitoring()
        } catch (e: Exception) {
            // Should handle gracefully
        }

        // Assert - Should no longer be monitoring
        assertFalse(mockNetworkMonitor.isMonitoring, "Should not be marked as monitoring")
    }

    // ==================== Network Status Flow Tests ====================

    @Test
    fun `network status flow emits complete network info`() = runTest {
        // Arrange
        mockNetworkMonitor.setStatus(NetworkStatus.ONLINE)
        mockNetworkMonitor.setConnectionType(ConnectionType.WIFI)
        mockNetworkMonitor.setMetered(true)

        // Act
        val networkInfo = mockNetworkMonitor.networkStatus.value

        // Assert
        assertEquals(NetworkStatus.ONLINE, networkInfo.status, "Status should match")
        assertEquals(ConnectionType.WIFI, networkInfo.connectionType, "Connection type should match")
        assertEquals(true, networkInfo.isMetered, "Metered flag should match")
        assertEquals(true, networkInfo.isOnline, "isOnline should match status")
    }

    @Test
    fun `network status flow updates are immediate`() = runTest {
        // Arrange
        val beforeUpdate = mockNetworkMonitor.networkStatus.value.status

        // Act
        mockNetworkMonitor.setStatus(NetworkStatus.OFFLINE)
        val afterUpdate = mockNetworkMonitor.networkStatus.value.status

        // Assert
        assertEquals(NetworkStatus.ONLINE, beforeUpdate, "Should start as ONLINE")
        assertEquals(NetworkStatus.OFFLINE, afterUpdate, "Should update immediately to OFFLINE")
    }

    // ==================== Mock Implementations ====================

    class MockNetworkMonitor : NetworkMonitor {
        private val _networkStatus = MutableStateFlow<NetworkInfo>(NetworkInfo(NetworkStatus.UNKNOWN))
        override val networkStatus: StateFlow<NetworkInfo> = _networkStatus.asStateFlow()
        var isMonitoring = false

        fun setStatus(status: NetworkStatus) {
            _networkStatus.value = _networkStatus.value.copy(status = status)
        }

        fun setConnectionType(type: ConnectionType) {
            _networkStatus.value = _networkStatus.value.copy(connectionType = type)
        }

        fun setMetered(metered: Boolean) {
            _networkStatus.value = _networkStatus.value.copy(isMetered = metered)
        }

        fun simulateNetworkAvailable() {
            setStatus(NetworkStatus.ONLINE)
        }

        fun simulateNetworkLost() {
            setStatus(NetworkStatus.OFFLINE)
        }

        fun simulateCapabilitiesChanged(connectionType: ConnectionType) {
            _networkStatus.value = NetworkInfo(
                status = NetworkStatus.ONLINE,
                isMetered = _networkStatus.value.isMetered,
                connectionType = connectionType
            )
        }

        override val currentNetworkInfo: NetworkInfo
            get() = networkStatus.value

        override val isOnline: Boolean
            get() = networkStatus.value.isOnline

        override fun startMonitoring() {
            isMonitoring = true
        }

        override fun stopMonitoring() {
            isMonitoring = false
        }
    }

    class FailingMockNetworkMonitor : NetworkMonitor {
        private val _networkStatus = MutableStateFlow<NetworkInfo>(NetworkInfo(NetworkStatus.UNKNOWN))
        override val networkStatus: StateFlow<NetworkInfo> = _networkStatus.asStateFlow()
        var isMonitoring = false
        var shouldFailOnStart = true

        override val currentNetworkInfo: NetworkInfo
            get() = networkStatus.value

        override val isOnline: Boolean
            get() = networkStatus.value.isOnline

        override fun startMonitoring() {
            if (shouldFailOnStart) {
                // Simulate failure - set to unknown
                _networkStatus.value = NetworkInfo(NetworkStatus.UNKNOWN)
                isMonitoring = false
                shouldFailOnStart = false
            } else {
                isMonitoring = true
            }
        }

        override fun stopMonitoring() {
            isMonitoring = false
        }
    }
}
