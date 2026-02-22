package org.crimsoncode2026.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import platform.Network.NWPathMonitor
import platform.Network.NWInterface
import platform.Network.NWInterfaceType
import platform.darwin.NSObject

/**
 * Factory function for iOS platform
 */
fun createNetworkMonitor(scope: CoroutineScope): NetworkMonitor {
    return NetworkMonitorImpl(scope)
}

/**
 * iOS implementation of NetworkMonitor
 *
 * Uses NWPathMonitor to monitor network changes on iOS.
 */
class NetworkMonitorImpl(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : NetworkMonitor {

    private val _networkStatus = MutableStateFlow<NetworkInfo>(NetworkInfo(NetworkStatus.UNKNOWN))
    override val networkStatus: StateFlow<NetworkInfo> = _networkStatus.asStateFlow()

    private var pathMonitor: NWPathMonitor? = null

    private var isMonitoring = false

    override fun startMonitoring() {
        if (isMonitoring) return

        try {
            pathMonitor = NWPathMonitor()

            // Set up the update handler
            pathMonitor?.setPathUpdateHandler { path ->
                scope.launch {
                    updateNetworkStatus(path)
                }
            }

            // Start monitoring
            pathMonitor?.start(queue = platform.darwin.dispatch_queue_main)

            // Get initial status
            val currentPath = pathMonitor?.currentPath
            if (currentPath != null) {
                updateNetworkStatus(currentPath)
            }

            isMonitoring = true
        } catch (e: Exception) {
            _networkStatus.value = NetworkInfo(NetworkStatus.UNKNOWN)
        }
    }

    override fun stopMonitoring() {
        if (!isMonitoring) return

        try {
            pathMonitor?.cancel()
            pathMonitor = null
            isMonitoring = false
        } catch (e: Exception) {
            // Ignore errors during cleanup
        }
    }

    private fun updateNetworkStatus(path: NWPath) {
        try {
            if (path.status == NWPathStatus.Satisfied) {
                val isMetered = path.isExpensive
                val connectionType = determineConnectionType(path)

                _networkStatus.value = NetworkInfo(
                    status = NetworkStatus.ONLINE,
                    isMetered = isMetered,
                    connectionType = connectionType
                )
            } else {
                _networkStatus.value = NetworkInfo(
                    status = NetworkStatus.OFFLINE,
                    isMetered = false,
                    connectionType = ConnectionType.UNKNOWN
                )
            }
        } catch (e: Exception) {
            _networkStatus.value = NetworkInfo(NetworkStatus.UNKNOWN)
        }
    }

    private fun determineConnectionType(path: NWPath): ConnectionType {
        return if (path.availableInterfaces.isNotEmpty()) {
            when (path.availableInterfaces.first().type) {
                NWInterfaceType.Wifi -> ConnectionType.WIFI
                NWInterfaceType.Cellular -> ConnectionType.CELLULAR
                NWInterfaceType.WiredEthernet -> ConnectionType.ETHERNET
                NWInterfaceType.Other -> ConnectionType.UNKNOWN
                else -> ConnectionType.UNKNOWN
            }
        } else {
            ConnectionType.UNKNOWN
        }
    }
}

// NWPathStatus enum values
private enum class NWPathStatus {
    Satisfied,
    Unsatisfied,
    RequiresConnection
}
