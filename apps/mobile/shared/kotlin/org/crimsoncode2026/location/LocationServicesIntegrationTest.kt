package org.crimsoncode2026.location

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Integration tests for location services including IpGeolocationService
 */
class LocationServicesIntegrationTest {

    @Test
    fun `IpGeolocationService parses valid response correctly`() {
        val jsonResponse = """
            {
                "ip": "8.8.8.8",
                "latitude": 37.7510,
                "longitude": -97.8220,
                "city": "Mountain View",
                "region": "California",
                "country": "US",
                "timezone": "America/Los_Angeles"
            }
        """.trimIndent()

        val mockEngine = MockEngine { _ ->
            respond(
                content = jsonResponse,
                status = io.ktor.http.HttpStatusCode.OK,
                headers = io.ktor.http.headers.headersOf(
                    io.ktor.http.HttpHeaders.ContentType to "application/json"
                )
            )
        }

        val httpClient = HttpClient(mockEngine)
        val service = IpGeolocationService(httpClient)

        val result = runBlocking {
            service.getIpLocation()
        }

        assertNotNull(result)
        assertEquals(LocationSource.IP, result.source)
        assertEquals(37.7510, result.coordinates.latitude)
        assertEquals(-97.8220, result.coordinates.longitude)
        assertEquals(null, result.accuracyMeters)
    }

    @Test
    fun `IpGeolocationService returns null on invalid response`() {
        val jsonResponse = """
            {
                "ip": "8.8.8.8",
                "city": "Mountain View"
            }
        """.trimIndent()

        val mockEngine = MockEngine { _ ->
            respond(
                content = jsonResponse,
                status = io.ktor.http.HttpStatusCode.OK,
                headers = io.ktor.http.headers.headersOf(
                    io.ktor.http.HttpHeaders.ContentType to "application/json"
                )
            )
        }

        val httpClient = HttpClient(mockEngine)
        val service = IpGeolocationService(httpClient)

        val result = runBlocking {
            service.getIpLocation()
        }

        assertNull(result)
    }

    @Test
    fun `IpGeolocationService returns null on network error`() {
        val mockEngine = MockEngine { _ ->
            throw Exception("Network error")
        }

        val httpClient = HttpClient(mockEngine)
        val service = IpGeolocationService(httpClient)

        val result = runBlocking {
            service.getIpLocation()
        }

        assertNull(result)
    }

    @Test
    fun `IpGeolocationService isAvailable returns true on successful response`() {
        val mockEngine = MockEngine { _ ->
            respond(
                content = "",
                status = io.ktor.http.HttpStatusCode.OK
            )
        }

        val httpClient = HttpClient(mockEngine)
        val service = IpGeolocationService(httpClient)

        val result = runBlocking {
            service.isAvailable()
        }

        assertEquals(true, result)
    }

    @Test
    fun `IpGeolocationService isAvailable returns false on network error`() {
        val mockEngine = MockEngine { _ ->
            throw Exception("Network error")
        }

        val httpClient = HttpClient(mockEngine)
        val service = IpGeolocationService(httpClient)

        val result = runBlocking {
            service.isAvailable()
        }

        assertEquals(false, result)
    }

    @Test
    fun `LocationSource enum contains all expected values`() {
        val sources = LocationSource.entries
        assertEquals(6, sources.size)
        assertEquals(true, sources.contains(LocationSource.GPS))
        assertEquals(true, sources.contains(LocationSource.WIFI))
        assertEquals(true, sources.contains(LocationSource.CELLULAR))
        assertEquals(true, sources.contains(LocationSource.IP))
        assertEquals(true, sources.contains(LocationSource.MANUAL))
        assertEquals(true, sources.contains(LocationSource.UNKNOWN))
    }

    @Test
    fun `AccuracyLevel enum contains all expected values`() {
        val levels = AccuracyLevel.entries
        assertEquals(5, levels.size)
        assertEquals(true, levels.contains(AccuracyLevel.HIGH))
        assertEquals(true, levels.contains(AccuracyLevel.GOOD))
        assertEquals(true, levels.contains(AccuracyLevel.FAIR))
        assertEquals(true, levels.contains(AccuracyLevel.LOW))
        assertEquals(true, levels.contains(AccuracyLevel.UNKNOWN))
    }
}

/**
 * Helper function to run suspend functions in tests
 */
private fun <T> runBlocking(block: suspend () -> T): T {
    return kotlinx.coroutines.runBlocking { block() }
}
