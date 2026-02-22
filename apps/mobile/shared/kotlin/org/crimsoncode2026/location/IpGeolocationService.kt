package org.crimsoncode2026.location

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.head
import kotlinx.serialization.Serializable
import org.crimsoncode2026.location.IpGeolocationService.IpGeolocationResponse

/**
 * IP-based geolocation service
 *
 * Provides fallback location via IP address when GPS/WiFi/Cellular are unavailable.
 */
class IpGeolocationService(
    private val httpClient: HttpClient
) {
    companion object {
        private const val IP_API_URL = "https://ipapi.co/json/"
    }

    /**
     * Get location based on IP address
     *
     * @return LocationData with IP-based location, null if request fails
     */
    suspend fun getIpLocation(): LocationData? {
        return try {
            val response: IpGeolocationResponse = httpClient.get(IP_API_URL).body()

            if (response.latitude != null && response.longitude != null) {
                LocationData(
                    coordinates = dev.icerock.moko.geo.coordinates.LatLon(
                        latitude = response.latitude,
                        longitude = response.longitude
                    ),
                    accuracyMeters = null, // IP geolocation doesn't provide accurate precision
                    timestamp = System.currentTimeMillis(),
                    source = LocationSource.IP
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if IP geolocation is available (network connectivity)
     */
    suspend fun isAvailable(): Boolean {
        return try {
            httpClient.head(IP_API_URL).status.value in 200..299
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Response from IP geolocation API
     */
    @Serializable
    data class IpGeolocationResponse(
        val ip: String? = null,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val city: String? = null,
        val region: String? = null,
        val country: String? = null,
        val timezone: String? = null
    )
}
