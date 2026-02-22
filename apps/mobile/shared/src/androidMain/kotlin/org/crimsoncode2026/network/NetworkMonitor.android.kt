package org.crimsoncode2026.network

import android.content.Context
import android.net.ConnectivityManager
import org.crimsoncode2026.auth.ContextProvider

/**
 * Factory function for Android platform
 */
fun createNetworkMonitor(scope: CoroutineScope): NetworkMonitor {
    val context = ContextProvider.getApplicationContext()
    return NetworkMonitorImpl(context, scope)
}

/**
 * Android implementation of NetworkMonitor
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Android implementation of NetworkMonitor
 *
 * Uses ConnectivityManager.NetworkCallback to monitor network changes.
 * Requires android.permission.ACCESS_NETWORK_STATE permission.
 */
class NetworkMonitorImpl(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : NetworkMonitor {

    private val _networkStatus = MutableStateFlow<NetworkInfo>(NetworkInfo(NetworkStatus.UNKNOWN))
    override val networkStatus: StateFlow<NetworkInfo> = _networkStatus.asStateFlow()

    private val connectivityManager: ConnectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            scope.launch {
                updateNetworkStatus(network)
            }
        }

        override fun onLost(network: Network) {
            scope.launch {
                _networkStatus.value = NetworkInfo(
                    status = NetworkStatus.OFFLINE,
                    isMetered = false,
                    connectionType = ConnectionType.UNKNOWN
                )
            }
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            scope.launch {
                updateNetworkStatus(network, networkCapabilities)
            }
        }
    }

    private var isMonitoring = false

    override fun startMonitoring() {
        if (isMonitoring) return

        try {
            val request = NetworkRequest.Builder().build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
            isMonitoring = true

            // Get initial status
            scope.launch {
                val activeNetwork = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    connectivityManager.activeNetwork
                } else {
                    @Suppress("DEPRECATION")
                    connectivityManager.activeNetworkInfo
                }

                if (activeNetwork != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        updateNetworkStatus(activeNetwork as Network)
                    } else {
                        // For API < 23, use activeNetworkInfo
                        _networkStatus.value = NetworkInfo(
                            status = NetworkStatus.ONLINE,
                            isMetered = false,
                            connectionType = ConnectionType.UNKNOWN
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback to unknown status if monitoring fails
            _networkStatus.value = NetworkInfo(NetworkStatus.UNKNOWN)
        }
    }

    override fun stopMonitoring() {
        if (!isMonitoring) return

        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            isMonitoring = false
        } catch (e: Exception) {
            // Ignore errors during cleanup
        }
    }

    private fun updateNetworkStatus(
        network: Network,
        capabilities: NetworkCapabilities? = null
    ) {
        try {
            val caps = capabilities ?: connectivityManager.getNetworkCapabilities(network)

            if (caps == null) {
                _networkStatus.value = NetworkInfo(NetworkStatus.OFFLINE)
                return
            }

            val isMetered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            val connectionType = determineConnectionType(caps)

            _networkStatus.value = NetworkInfo(
                status = NetworkStatus.ONLINE,
                isMetered = isMetered,
                connectionType = connectionType
            )
        } catch (e: Exception) {
            _networkStatus.value = NetworkInfo(NetworkStatus.UNKNOWN)
        }
    }

    private fun determineConnectionType(caps: NetworkCapabilities): ConnectionType {
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> ConnectionType.VPN
            else -> ConnectionType.UNKNOWN
        }
    }
}
