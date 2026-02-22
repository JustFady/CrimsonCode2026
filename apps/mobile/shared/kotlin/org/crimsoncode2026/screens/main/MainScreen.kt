package org.crimsoncode2026.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.encodeURLParameter
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.maplibre.compose.expressions.dsl.all
import org.maplibre.compose.expressions.dsl.asBoolean
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.eq
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.crimsoncode2026.compose.MapView
import org.crimsoncode2026.compose.calculateMapBoundsFromZoom
import org.crimsoncode2026.compose.LatLng
import org.crimsoncode2026.data.BroadcastType
import org.crimsoncode2026.data.Event
import org.crimsoncode2026.data.User
import org.crimsoncode2026.runtime.DeviceLocationProvider
import org.crimsoncode2026.screens.publicevents.EventDetailsPanel
import org.crimsoncode2026.screens.publicevents.EventListItem
import org.crimsoncode2026.screens.publicevents.EventListView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    supabaseUrl: String = "",
    supabaseAnonKey: String = "",
    accessToken: String = "",
    currentUserId: String = "",
    currentPhoneNumber: String = "",
    currentDeviceId: String = "",
    onNavigateToSettings: () -> Unit = {},
    onCreateEvent: () -> Unit = {},
    onShowEventList: () -> Unit = {}
) {
    val events = remember { mutableStateListOf(*sampleEvents().toTypedArray()) }
    var statusMessage by remember { mutableStateOf("Loading public events...") }
    var showList by remember { mutableStateOf(false) }
    var selectedEvent by remember { mutableStateOf<EventListItem?>(null) }
    var currentLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var mapTarget by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var mapTargetZoom by remember { mutableStateOf<Double?>(null) }
    var mapCenter by remember { mutableStateOf<Pair<Double, Double>?>(47.6062 to -122.3321) }
    var mapZoom by remember { mutableStateOf(3.8) }
    var locating by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var createMessage by remember { mutableStateOf("") }
    var creatingEvent by remember { mutableStateOf(false) }

    val publicEventsApi = remember { PublicEventsApi() }
    val createEventApi = remember { CreateEventApi() }
    val scope = rememberCoroutineScope()
    val queryCenterLat = mapCenter?.first?.quantizeQueryCoord()
    val queryCenterLon = mapCenter?.second?.quantizeQueryCoord()
    val queryZoom = mapZoom.quantizeQueryZoom()

    fun refreshPublicEvents(center: Pair<Double, Double>? = mapCenter, zoom: Double = mapZoom) {
        val url = supabaseUrl.trim()
        val key = supabaseAnonKey.trim()
        if (url.isBlank() || key.isBlank() || url.contains("your-project-ref")) {
            statusMessage = "Using sample events (Supabase credentials missing)."
            return
        }
        scope.launch {
            publicEventsApi.fetchPublicEvents(url, key, center, zoom).onSuccess { loaded ->
                val privateLocal = events.filter { it.event.isPrivate }
                events.clear()
                events.addAll(privateLocal)
                events.addAll(loaded)
                statusMessage = if (loaded.isEmpty()) {
                    "Connected. No public events in this area/zoom. Tap + to create one."
                } else {
                    "Loaded ${loaded.size} public events (zoom ${zoom.formatZoom()})."
                }
            }.onFailure { e ->
                statusMessage = "Live public events failed: ${e.message ?: "unknown error"}"
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            publicEventsApi.close()
            createEventApi.close()
        }
    }

    LaunchedEffect(supabaseUrl, supabaseAnonKey, queryCenterLat, queryCenterLon, queryZoom) {
        delay(650)
        refreshPublicEvents(
            center = if (queryCenterLat != null && queryCenterLon != null) queryCenterLat to queryCenterLon else mapCenter,
            zoom = queryZoom
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CrimsonCode") },
                actions = {
                    TextButton(onClick = {
                        if (locating) return@TextButton
                        locating = true
                        scope.launch {
                            runCatching { DeviceLocationProvider.getCurrentLocation() }
                                .onSuccess { loc ->
                                    if (loc != null) {
                                        val acc = loc.accuracyMeters
                                        if (acc != null && acc > 1000f) {
                                            statusMessage = "Location is low accuracy (±${acc.toInt()}m). If using emulator, set location in Extended controls > Location."
                                        } else {
                                            currentLocation = loc.latitude to loc.longitude
                                            mapTarget = loc.latitude to loc.longitude
                                            mapTargetZoom = 14.0
                                            statusMessage = buildString {
                                                append("Location updated (")
                                                append(loc.latitude.formatCoord())
                                                append(", ")
                                                append(loc.longitude.formatCoord())
                                                append(")")
                                                loc.accuracyMeters?.let { append(" ±${it.toInt()}m") }
                                            }
                                        }
                                    } else {
                                        statusMessage = "Location unavailable. Grant location permission and enable GPS."
                                    }
                                }
                                .onFailure { e ->
                                    statusMessage = "Location error: ${e.message ?: "unknown error"}"
                                }
                            locating = false
                        }
                    }, enabled = !locating) { Text(if (locating) "Locating..." else "Locate") }
                    IconButton(onClick = {
                        showList = true
                        onShowEventList()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Events")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                selectedEvent = null
                showCreateDialog = true
                onCreateEvent()
            }) {
                Icon(Icons.Default.Add, contentDescription = "Create Event")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            MapView(
                modifier = Modifier.fillMaxSize(),
                initialZoom = 3.8,
                initialLatitude = 39.8283,
                initialLongitude = -98.5795,
                targetLocation = mapTarget,
                targetZoom = mapTargetZoom ?: if (mapTarget != null) 12.0 else null,
                onCameraChanged = { camera ->
                    mapCenter = camera.center.latitude to camera.center.longitude
                    mapZoom = camera.zoom
                }
            ) {
                EventMapMarkers(
                    events = events.map { it.event },
                    selectedEventId = selectedEvent?.event?.id
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.94f),
                tonalElevation = 2.dp
            ) {
                Column {
                    TextButton(
                        onClick = {
                            mapCenter?.let {
                                mapTarget = it
                                mapTargetZoom = (mapZoom + 1.0).coerceAtMost(19.0)
                            }
                        }
                    ) { Text("+") }
                    TextButton(
                        onClick = {
                            mapCenter?.let {
                                mapTarget = it
                                mapTargetZoom = (mapZoom - 1.0).coerceAtLeast(2.0)
                            }
                        }
                    ) { Text("-") }
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.95f),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF1B5E20), RoundedCornerShape(99.dp))
                    )
                    Column {
                        Text(
                            "Map + Events",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (locating) "Getting location..." else statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF5F5F5F)
                        )
                        Text(
                            "Center ${mapCenter?.first?.formatCoord() ?: "--"}, ${mapCenter?.second?.formatCoord() ?: "--"} | Zoom ${mapZoom.formatZoom()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7A7A7A)
                        )
                        if (createMessage.isNotBlank()) {
                            Text(
                                createMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (createMessage.contains("failed", true) || createMessage.contains("missing", true)) Color(0xFFB3261E) else Color(0xFF1B5E20)
                            )
                        }
                    }
                }
            }

            if (showList) {
                Surface(
                    modifier = Modifier.fillMaxSize().padding(top = 64.dp),
                    color = Color(0xFFF5F2EC)
                ) {
                    EventListView(
                        events = events,
                        onEventClick = { clicked ->
                            selectedEvent = events.firstOrNull { it.event.id == clicked.id }
                            showList = false
                            mapTarget = clicked.lat to clicked.lon
                            mapTargetZoom = 13.0
                        },
                        onClearAll = {
                            events.clear()
                            selectedEvent = null
                        },
                        onDismiss = { showList = false }
                    )
                }
            }

            selectedEvent?.let { item ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    EventDetailsPanel(
                        event = item.event,
                        creator = item.creator,
                        onDismiss = { selectedEvent = null },
                        onClear = {
                            val id = item.event.id
                            events.removeAll { it.event.id == id }
                            selectedEvent = null
                        },
                        onNavigateToLocation = {
                            mapTarget = item.event.lat to item.event.lon
                            mapTargetZoom = 14.0
                            selectedEvent = null
                        }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateEventDialog(
            creating = creatingEvent,
            currentLocation = currentLocation ?: mapCenter,
            onDismiss = { if (!creatingEvent) showCreateDialog = false },
            onSubmit = { form ->
                val center = (currentLocation ?: mapCenter)
                if (center == null) {
                    createMessage = "Create failed: location not available yet."
                    return@CreateEventDialog
                }
                if (supabaseUrl.isBlank() || supabaseAnonKey.isBlank()) {
                    createMessage = "Create failed: missing Supabase config."
                    return@CreateEventDialog
                }
                if (accessToken.isBlank() || currentUserId.isBlank()) {
                    createMessage = "Create failed: missing auth session. Re-login and verify OTP again."
                    return@CreateEventDialog
                }
                creatingEvent = true
                scope.launch {
                    createEventApi.createEvent(
                        supabaseUrl = supabaseUrl,
                        anonKey = supabaseAnonKey,
                        accessToken = accessToken,
                        creatorId = currentUserId,
                        phoneNumber = currentPhoneNumber,
                        deviceId = currentDeviceId,
                        lat = center.first,
                        lon = center.second,
                        description = form.description,
                        category = form.category,
                        severity = form.severity,
                        broadcastType = if (form.isPrivate) BroadcastType.PRIVATE.value else BroadcastType.PUBLIC.value,
                        isAnonymous = !form.isPrivate && form.anonymousPublic
                    ).onSuccess { created ->
                        events.removeAll { it.event.id == created.event.id }
                        events.add(0, created)
                        createMessage = "Event created successfully."
                        showCreateDialog = false
                        showList = true
                        mapTarget = created.event.lat to created.event.lon
                        mapTargetZoom = 14.0
                        refreshPublicEvents()
                    }.onFailure { e ->
                        createMessage = "Create failed: ${e.message ?: "unknown error"}"
                    }
                    creatingEvent = false
                }
            }
        )
    }
}

@Composable
private fun CreateEventDialog(
    creating: Boolean,
    currentLocation: Pair<Double, Double>?,
    onDismiss: () -> Unit,
    onSubmit: (CreateEventForm) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("OTHER") }
    var severity by remember { mutableStateOf("ALERT") }
    var isPrivate by remember { mutableStateOf(false) }
    var anonymousPublic by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    currentLocation?.let { "Location: ${it.first.formatCoord()}, ${it.second.formatCoord()}" }
                        ?: "Location: unavailable",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(500) },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it.uppercase() },
                    label = { Text("Category (e.g. MEDICAL, FIRE, WEATHER)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = severity,
                    onValueChange = { severity = it.uppercase() },
                    label = { Text("Severity (ALERT or CRISIS)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Private event")
                    Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
                }
                if (!isPrivate) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Anonymous public")
                        Switch(checked = anonymousPublic, onCheckedChange = { anonymousPublic = it })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !creating && description.isNotBlank(),
                onClick = {
                    onSubmit(
                        CreateEventForm(
                            description = description.trim(),
                            category = category.trim().ifBlank { "OTHER" },
                            severity = severity.trim().ifBlank { "ALERT" },
                            isPrivate = isPrivate,
                            anonymousPublic = anonymousPublic
                        )
                    )
                }
            ) {
                Text(if (creating) "Creating..." else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !creating) { Text("Cancel") }
        }
    )
}

private data class CreateEventForm(
    val description: String,
    val category: String,
    val severity: String,
    val isPrivate: Boolean,
    val anonymousPublic: Boolean
)

@Composable
private fun EventMapMarkers(
    events: List<Event>,
    selectedEventId: String?
) {
    if (events.isEmpty()) return

    val source = rememberGeoJsonSource(
        data = GeoJsonData.JsonString(buildMarkersGeoJson(events, selectedEventId)),
        options = GeoJsonOptions(cluster = false)
    )

    CircleLayer(
        id = "events-shadow",
        source = source,
        color = const(Color(0x55000000)),
        radius = const(9.dp),
        blur = const(0.18f)
    )

    CircleLayer(
        id = "events-public-alert",
        source = source,
        filter = all(
            feature["is_private"].asBoolean() eq const(false),
            feature["is_crisis"].asBoolean() eq const(false)
        ),
        color = const(Color(0xFFF97316)),
        radius = const(7.dp),
        strokeColor = const(Color.White),
        strokeWidth = const(2.dp)
    )

    CircleLayer(
        id = "events-public-crisis",
        source = source,
        filter = all(
            feature["is_private"].asBoolean() eq const(false),
            feature["is_crisis"].asBoolean()
        ),
        color = const(Color(0xFFDC2626)),
        radius = const(8.dp),
        strokeColor = const(Color.White),
        strokeWidth = const(2.dp)
    )

    CircleLayer(
        id = "events-private",
        source = source,
        filter = feature["is_private"].asBoolean(),
        color = const(Color(0xFF4F46E5)),
        radius = const(7.dp),
        strokeColor = const(Color.White),
        strokeWidth = const(2.dp)
    )

    CircleLayer(
        id = "events-selected-ring",
        source = source,
        filter = feature["is_selected"].asBoolean(),
        color = const(Color.Transparent),
        radius = const(11.dp),
        strokeColor = const(Color(0xFF111827)),
        strokeWidth = const(2.dp)
    )
}

private fun buildMarkersGeoJson(events: List<Event>, selectedEventId: String?): String = buildString {
    append("""{"type":"FeatureCollection","features":[""")
    events.forEachIndexed { index, event ->
        if (index > 0) append(',')
        append("""{"type":"Feature","geometry":{"type":"Point","coordinates":[""")
        append(event.lon)
        append(',')
        append(event.lat)
        append("]}")
        append(""","properties":{"is_private":""")
        append(if (event.isPrivate) "true" else "false")
        append(""","is_crisis":""")
        append(if (event.severity.equals("CRISIS", ignoreCase = true)) "true" else "false")
        append(""","is_selected":""")
        append(if (event.id == selectedEventId) "true" else "false")
        append("}}")
    }
    append("]}")
}

private class PublicEventsApi {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient { install(ContentNegotiation) { json(json) } }

    suspend fun fetchPublicEvents(
        supabaseUrl: String,
        anonKey: String,
        center: Pair<Double, Double>?,
        zoom: Double
    ): Result<List<EventListItem>> = runCatching {
        val bounded = center != null && zoom >= 5.0
        val bounds = center?.let { calculateMapBoundsFromZoom(LatLng(it.first, it.second), zoom) }
        val limit = when {
            zoom >= 12.0 -> 200
            zoom >= 8.0 -> 120
            else -> 80
        }
        val url = buildString {
            append(supabaseUrl.trimEnd('/'))
            append("/rest/v1/events")
            append("?select=*")
            append("&broadcast_type=eq.PUBLIC")
            append("&deleted_at=is.null")
            append("&expires_at=gt.")
            append(java.time.Instant.now().toString().encodeURLParameter())
            if (bounded && bounds != null) {
                append("&lat=gte.")
                append(bounds.south.toString().encodeURLParameter())
                append("&lat=lte.")
                append(bounds.north.toString().encodeURLParameter())
                append("&lon=gte.")
                append(bounds.west.toString().encodeURLParameter())
                append("&lon=lte.")
                append(bounds.east.toString().encodeURLParameter())
            }
            append("&order=created_at.desc")
            append("&limit=$limit")
        }

        val rows: List<SupabaseEventRow> = client.get(url) {
            header("apikey", anonKey)
            header("Authorization", "Bearer $anonKey")
        }.body()

        rows.mapNotNull { row -> row.toEventOrNull()?.let { EventListItem(it, null) } }
    }

    fun close() = client.close()
}

private class CreateEventApi {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient { install(ContentNegotiation) { json(json) } }

    suspend fun createEvent(
        supabaseUrl: String,
        anonKey: String,
        accessToken: String,
        creatorId: String,
        phoneNumber: String,
        deviceId: String,
        lat: Double,
        lon: Double,
        description: String,
        category: String,
        severity: String,
        broadcastType: String,
        isAnonymous: Boolean
    ): Result<EventListItem> = runCatching {
        ensureUserProfile(
            supabaseUrl = supabaseUrl,
            anonKey = anonKey,
            accessToken = accessToken,
            userId = creatorId,
            phoneNumber = phoneNumber,
            deviceId = deviceId
        )

        val now = System.currentTimeMillis()
        val expires = now + 48L * 60 * 60 * 1000
        val payload = CreateEventRequest(
            creatorId = creatorId,
            severity = severity,
            category = category,
            lat = lat,
            lon = lon,
            broadcastType = broadcastType,
            description = description,
            isAnonymous = isAnonymous,
            expiresAt = java.time.Instant.ofEpochMilli(expires).toString()
        )

        val response = client.post(supabaseUrl.trimEnd('/') + "/rest/v1/events?select=*") {
            header("apikey", anonKey)
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header("Prefer", "return=representation")
            contentType(ContentType.Application.Json)
            setBody(listOf(payload))
        }

        val body = response.bodyAsText()
        if (!response.status.value.toString().startsWith("2")) {
            throw IllegalStateException("HTTP ${response.status.value}: ${extractSupabaseError(body)}")
        }

        val rows: List<SupabaseEventRow> = runCatching {
            json.decodeFromString<List<SupabaseEventRow>>(body)
        }.getOrElse {
            throw IllegalStateException("Unexpected create response: ${body.take(300)}")
        }

        rows.firstOrNull()?.toEventOrNull()?.let { EventListItem(it, null) }
            ?: error("Create succeeded but no event was returned.")
    }

    private suspend fun ensureUserProfile(
        supabaseUrl: String,
        anonKey: String,
        accessToken: String,
        userId: String,
        phoneNumber: String,
        deviceId: String
    ) {
        if (userId.isBlank()) error("Missing session user id.")
        val normalizedPhone = phoneNumber.filterNot(Char::isWhitespace)
        if (!normalizedPhone.matches(Regex("""^\+1\d{10}$"""))) {
            error("Phone number must be a US E.164 number like +15551234567.")
        }
        val normalizedDeviceId = deviceId.trim().ifBlank { "android-$userId" }

        val response = client.post(supabaseUrl.trimEnd('/') + "/rest/v1/users?on_conflict=id&select=id") {
            header("apikey", anonKey)
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header("Prefer", "resolution=merge-duplicates,return=representation")
            contentType(ContentType.Application.Json)
            setBody(
                listOf(
                    CreateUserProfileRequest(
                        id = userId,
                        phoneNumber = normalizedPhone,
                        displayName = "Responder",
                        deviceId = normalizedDeviceId,
                        platform = "ANDROID"
                    )
                )
            )
        }
        val body = response.bodyAsText()
        if (!response.status.value.toString().startsWith("2")) {
            throw IllegalStateException("User profile sync failed (HTTP ${response.status.value}): ${extractSupabaseError(body)}")
        }
    }

    fun close() = client.close()
}

@Serializable
private data class CreateUserProfileRequest(
    val id: String,
    @SerialName("phone_number") val phoneNumber: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("device_id") val deviceId: String,
    val platform: String
)

@Serializable
private data class CreateEventRequest(
    @SerialName("creator_id") val creatorId: String,
    val severity: String,
    val category: String,
    val lat: Double,
    val lon: Double,
    @SerialName("broadcast_type") val broadcastType: String,
    val description: String,
    @SerialName("is_anonymous") val isAnonymous: Boolean,
    @SerialName("expires_at") val expiresAt: String
)

@Serializable
private data class SupabaseEventRow(
    val id: String,
    @SerialName("creator_id") val creatorId: String? = null,
    val severity: String? = null,
    val category: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    @SerialName("location_override") val locationOverride: String? = null,
    @SerialName("broadcast_type") val broadcastType: String? = null,
    val description: String? = null,
    @SerialName("is_anonymous") val isAnonymous: Boolean? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null
) {
    fun toEventOrNull(): Event? {
        val sev = severity ?: return null
        val cat = category ?: return null
        val la = lat ?: return null
        val lo = lon ?: return null
        val bt = broadcastType ?: return null
        val desc = description ?: return null
        val created = parseTimestampToMillis(createdAt) ?: System.currentTimeMillis()
        val expires = parseTimestampToMillis(expiresAt) ?: (created + 48 * 60 * 60 * 1000L)
        return Event(
            id = id,
            creatorId = creatorId ?: "unknown",
            severity = sev,
            category = cat,
            lat = la,
            lon = lo,
            locationOverride = locationOverride,
            broadcastType = bt,
            description = desc,
            isAnonymous = isAnonymous ?: true,
            createdAt = created,
            expiresAt = expires
        )
    }
}

private fun parseTimestampToMillis(value: String?): Long? {
    if (value.isNullOrBlank()) return null
    val normalized = value.trim().replace(" ", "T").let { if (it.endsWith("Z")) it else "${it}Z" }
    return runCatching { java.time.Instant.parse(normalized).toEpochMilli() }.getOrNull()
}

@Serializable
private data class SupabaseErrorBody(
    val code: String? = null,
    val message: String? = null,
    val details: String? = null,
    val hint: String? = null,
    val msg: String? = null
)

private fun extractSupabaseError(body: String): String {
    val trimmed = body.trim()
    if (trimmed.isBlank()) return "empty response body"
    return runCatching {
        val parsed = Json { ignoreUnknownKeys = true }.decodeFromString<SupabaseErrorBody>(trimmed)
        listOfNotNull(parsed.code, parsed.message ?: parsed.msg, parsed.details, parsed.hint)
            .joinToString(" | ")
            .ifBlank { trimmed.take(300) }
    }.getOrElse { trimmed.take(300) }
}

private fun sampleEvents(): List<EventListItem> {
    val now = System.currentTimeMillis()
    val creator = User(
        id = "user-1",
        phoneNumber = "+15099905058",
        displayName = "Field Responder",
        deviceId = "device-1",
        platform = "ANDROID",
        createdAt = now - 86_400_000L,
        updatedAt = now - 60_000L
    )
    return listOf(
        EventListItem(
            event = Event(
                id = "evt-public-1",
                creatorId = "anon",
                severity = "ALERT",
                category = "WEATHER",
                lat = 47.6588,
                lon = -117.4260,
                broadcastType = BroadcastType.PUBLIC.value,
                description = "Heavy wind advisory with possible debris on roads.",
                isAnonymous = true,
                createdAt = now - 15 * 60_000L,
                expiresAt = now + 24 * 60 * 60_000L
            )
        ),
        EventListItem(
            event = Event(
                id = "evt-private-1",
                creatorId = creator.id,
                severity = "CRISIS",
                category = "MEDICAL",
                lat = 47.6150,
                lon = -117.3000,
                broadcastType = BroadcastType.PRIVATE.value,
                description = "Need immediate assistance for injury at trailhead parking lot.",
                isAnonymous = false,
                createdAt = now - 5 * 60_000L,
                expiresAt = now + 24 * 60 * 60_000L
            ),
            creator = creator
        )
    )
}

private fun Double.formatCoord(): String = String.format(java.util.Locale.US, "%.5f", this)
private fun Double.formatZoom(): String = String.format(java.util.Locale.US, "%.1f", this)
private fun Double.quantizeQueryCoord(): Double = kotlin.math.round(this * 200.0) / 200.0 // ~0.005 deg
private fun Double.quantizeQueryZoom(): Double = kotlin.math.round(this * 4.0) / 4.0 // 0.25 zoom steps
