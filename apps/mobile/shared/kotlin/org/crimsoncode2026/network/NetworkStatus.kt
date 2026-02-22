package org.crimsoncode2026.network

/**
 * Network status for connectivity monitoring
 */
enum class NetworkStatus {
    ONLINE,
    OFFLINE,
    UNKNOWN
}

/**
 * Network connectivity details
 */
data class NetworkInfo(
    val status: NetworkStatus,
    val isMetered: Boolean = false,
    val connectionType: ConnectionType = ConnectionType.UNKNOWN
) {
    val isOnline: Boolean
        get() = status == NetworkStatus.ONLINE
}

/**
 * Type of network connection
 */
enum class ConnectionType {
    WIFI,
    CELLULAR,
    ETHERNET,
    VPN,
    UNKNOWN
}
