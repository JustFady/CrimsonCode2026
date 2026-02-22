package org.crimsoncode2026

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.crimsoncode2026.runtime.RuntimeDeviceId
import org.crimsoncode2026.runtime.RuntimeSecrets
import org.crimsoncode2026.screens.main.MainScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

private enum class AppShellScreen {
    PhoneEntry,
    OtpVerify,
    MainMap,
    Contacts,
    Settings
}

private data class ShellContact(
    val id: String,
    val displayName: String,
    val phoneNumber: String,
    val hasApp: Boolean
)

@Composable
fun App() {
    var screen by rememberSaveable { mutableStateOf(AppShellScreen.PhoneEntry) }
    var phone by rememberSaveable { mutableStateOf("") }
    var otp by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("Responder") }
    var diagnosticsMessage by rememberSaveable { mutableStateOf("No diagnostics run yet.") }
    var authMessage by rememberSaveable { mutableStateOf("Not signed in") }
    var authInFlight by rememberSaveable { mutableStateOf(false) }
    var useRealAuth by rememberSaveable { mutableStateOf(true) }
    var sessionAccessToken by rememberSaveable { mutableStateOf("") }
    var sessionUserId by rememberSaveable { mutableStateOf("") }
    val contacts = remember {
        mutableStateListOf(
            ShellContact("c1", "Alice Nguyen", "+12065550101", hasApp = true),
            ShellContact("c2", "Ben Carter", "+12065550102", hasApp = false),
            ShellContact("c3", "Maya Patel", "+14255550103", hasApp = true)
        )
    }
    var selectedContactIds by remember { mutableStateOf(setOf<String>()) }
    val runtimeSupabaseUrl = RuntimeSecrets.supabaseUrl()
        .takeUnless { it.isBlank() || it == "https://your-project-ref.supabase.co" }
        ?: ""
    val runtimeSupabaseAnonKey = RuntimeSecrets.supabaseAnonKey()
        .takeUnless { it.isBlank() || it == "your-anon-key-here" }
        ?: ""

    var supabaseUrl by rememberSaveable { mutableStateOf(runtimeSupabaseUrl) }
    var supabaseAnonKey by rememberSaveable { mutableStateOf(runtimeSupabaseAnonKey) }
    val runtimeDeviceId = remember { RuntimeDeviceId.value() }
    val scope = rememberCoroutineScope()
    val authApi = remember { SupabaseOtpApi() }

    DisposableEffect(Unit) {
        onDispose { authApi.close() }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF5F2EC)
        ) {
            when (screen) {
                AppShellScreen.PhoneEntry -> PhoneEntryShell(
                    phone = phone,
                    onPhoneChange = { phone = it },
                    useRealAuth = useRealAuth,
                    onToggleRealAuth = { useRealAuth = it },
                    authInFlight = authInFlight,
                    authMessage = authMessage,
                    onOpenSettings = { screen = AppShellScreen.Settings },
                    onContinue = {
                        if (!useRealAuth) {
                            authMessage = "OTP requested for ${phone.ifBlank { "(empty)" }} (shell mode)"
                            diagnosticsMessage = authMessage
                            screen = AppShellScreen.OtpVerify
                            return@PhoneEntryShell
                        }

                        val url = supabaseUrl.trim()
                        val key = supabaseAnonKey.trim()
                        if (url.isBlank() || key.isBlank()) {
                            authMessage = "Open Settings and enter your Supabase URL + anon key."
                            return@PhoneEntryShell
                        }

                        authInFlight = true
                        scope.launch {
                            val result = authApi.sendOtp(url, key, phone.trim())
                            authInFlight = false
                            authMessage = result.message
                            diagnosticsMessage = result.message
                            if (result.success) {
                                screen = AppShellScreen.OtpVerify
                            }
                        }
                    }
                )

                AppShellScreen.OtpVerify -> OtpShell(
                    phone = phone,
                    otp = otp,
                    useRealAuth = useRealAuth,
                    onOtpChange = { otp = it },
                    onBack = { screen = AppShellScreen.PhoneEntry },
                    authInFlight = authInFlight,
                    authMessage = authMessage,
                    onVerify = {
                        if (!useRealAuth) {
                            diagnosticsMessage = "Verified locally in shell mode at runtime (no backend call)."
                            authMessage = diagnosticsMessage
                            if (displayName.isBlank()) displayName = "Responder"
                            screen = AppShellScreen.MainMap
                            return@OtpShell
                        }

                        val url = supabaseUrl.trim()
                        val key = supabaseAnonKey.trim()
                        if (url.isBlank() || key.isBlank()) {
                            authMessage = "Missing Supabase URL/key."
                            return@OtpShell
                        }

                        authInFlight = true
                        scope.launch {
                            val result = authApi.verifyOtp(url, key, phone.trim(), otp.trim())
                            authInFlight = false
                            authMessage = result.message
                            diagnosticsMessage = result.message
                            if (result.success) {
                                sessionAccessToken = result.accessToken.orEmpty()
                                sessionUserId = result.userId.orEmpty()
                                if (displayName.isBlank()) displayName = "Responder"
                                screen = AppShellScreen.MainMap
                            }
                        }
                    }
                )

                AppShellScreen.MainMap -> MainScreen(
                    supabaseUrl = supabaseUrl.trim(),
                    supabaseAnonKey = supabaseAnonKey.trim(),
                    accessToken = sessionAccessToken,
                    currentUserId = sessionUserId,
                    currentPhoneNumber = phone.trim(),
                    currentDeviceId = runtimeDeviceId,
                    onNavigateToSettings = { screen = AppShellScreen.Settings },
                    onNavigateToContacts = { screen = AppShellScreen.Contacts },
                    onCreateEvent = {
                        diagnosticsMessage = "Event creation wizard is not restored yet."
                    },
                    onShowEventList = {
                        diagnosticsMessage = "Event list is not restored yet."
                    }
                )

                AppShellScreen.Contacts -> ContactSelectionShell(
                    contacts = contacts,
                    selectedContactIds = selectedContactIds,
                    onBack = { screen = AppShellScreen.MainMap },
                    onSave = { ids ->
                        selectedContactIds = ids
                        diagnosticsMessage = "Saved ${ids.size} contact(s) for private alerts."
                        screen = AppShellScreen.MainMap
                    },
                    onAddContact = { name, rawPhone ->
                        val phoneDigits = rawPhone.filter { it.isDigit() }
                        val normalized = when {
                            rawPhone.trim().startsWith("+") -> rawPhone.trim()
                            phoneDigits.length == 10 -> "+1$phoneDigits"
                            phoneDigits.length == 11 && phoneDigits.startsWith("1") -> "+$phoneDigits"
                            else -> rawPhone.trim()
                        }
                        val safeName = name.trim().ifBlank { "Contact ${contacts.size + 1}" }
                        if (normalized.isBlank()) {
                            diagnosticsMessage = "Contact phone number is required."
                            return@ContactSelectionShell
                        }
                        val existing = contacts.firstOrNull { it.phoneNumber == normalized }
                        if (existing != null) {
                            selectedContactIds = selectedContactIds + existing.id
                            diagnosticsMessage = "Contact already exists. Selected ${existing.displayName}."
                            return@ContactSelectionShell
                        }
                        val id = "c-${contacts.size + 1}-${System.currentTimeMillis()}"
                        contacts.add(
                            0,
                            ShellContact(
                                id = id,
                                displayName = safeName,
                                phoneNumber = normalized,
                                hasApp = normalized.endsWith("01") || normalized.endsWith("03") || normalized.endsWith("58")
                            )
                        )
                        selectedContactIds = selectedContactIds + id
                        diagnosticsMessage = "Added and selected $safeName."
                    }
                )

                AppShellScreen.Settings -> SettingsShell(
                    displayName = displayName,
                    onNameChange = { displayName = it },
                    useRealAuth = useRealAuth,
                    onToggleRealAuth = { useRealAuth = it },
                    supabaseUrl = supabaseUrl,
                    onSupabaseUrlChange = { supabaseUrl = it },
                    supabaseAnonKey = supabaseAnonKey,
                    onSupabaseAnonKeyChange = { supabaseAnonKey = it },
                    onBack = { screen = AppShellScreen.MainMap }
                )
            }
        }
    }
}

@Composable
private fun ContactSelectionShell(
    contacts: List<ShellContact>,
    selectedContactIds: Set<String>,
    onBack: () -> Unit,
    onSave: (Set<String>) -> Unit,
    onAddContact: (name: String, phone: String) -> Unit
) {
    var search by rememberSaveable { mutableStateOf("") }
    var showOnlyAppUsers by rememberSaveable { mutableStateOf(false) }
    var draftName by rememberSaveable { mutableStateOf("") }
    var draftPhone by rememberSaveable { mutableStateOf("") }
    var localSelected by remember(selectedContactIds) { mutableStateOf(selectedContactIds) }

    val filtered = contacts
        .filter { c ->
            val matchesSearch = search.isBlank() ||
                c.displayName.contains(search, ignoreCase = true) ||
                c.phoneNumber.contains(search)
            val matchesApp = !showOnlyAppUsers || c.hasApp
            matchesSearch && matchesApp
        }
        .sortedWith(compareBy<ShellContact> { !it.hasApp }.thenBy { it.displayName.lowercase() })

    ShellScreenFrame {
        CardBlock("Filters") {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search contacts") },
                singleLine = true
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Only contacts with app")
                Switch(checked = showOnlyAppUsers, onCheckedChange = { showOnlyAppUsers = it })
            }
            Text(
                "${localSelected.size} selected",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5F5F5F)
            )
        }

        CardBlock("Add Contact") {
            OutlinedTextField(
                value = draftName,
                onValueChange = { draftName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name") },
                singleLine = true
            )
            OutlinedTextField(
                value = draftPhone,
                onValueChange = { draftPhone = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Phone") },
                placeholder = { Text("+1 555 123 4567") },
                singleLine = true
            )
            Button(
                onClick = {
                    onAddContact(draftName, draftPhone)
                    draftName = ""
                    draftPhone = ""
                },
                enabled = draftPhone.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add & Select")
            }
        }

        CardBlock("Contacts") {
            if (filtered.isEmpty()) {
                Text(
                    "No contacts found. Add one manually to continue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5F5F5F)
                )
            } else {
                filtered.forEach { contact ->
                    val selected = contact.id in localSelected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selected) Color(0xFFE8F5E9) else Color(0xFFF7F7F7),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(contact.displayName, fontWeight = FontWeight.SemiBold)
                            Text(
                                contact.phoneNumber,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF5F5F5F)
                            )
                            Text(
                                if (contact.hasApp) "Has app" else "SMS only",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (contact.hasApp) Color(0xFF1B5E20) else Color(0xFF8A5A00)
                            )
                        }
                        Button(
                            onClick = {
                                localSelected = if (selected) {
                                    localSelected - contact.id
                                } else {
                                    localSelected + contact.id
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(if (selected) "Selected" else "Select")
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("Back")
            }
            Button(onClick = { onSave(localSelected) }, modifier = Modifier.weight(1f)) {
                Text("Save Contacts")
            }
        }
    }
}

@Composable
private fun PhoneEntryShell(
    phone: String,
    onPhoneChange: (String) -> Unit,
    useRealAuth: Boolean,
    onToggleRealAuth: (Boolean) -> Unit,
    authInFlight: Boolean,
    authMessage: String,
    onOpenSettings: () -> Unit,
    onContinue: () -> Unit
) {
    ShellScreenFrame {
        CardBlock("Phone Login") {
            Text(
                "Use shell mode or real Supabase OTP auth. Configure Supabase URL and anon key in Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5F5F5F)
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Use real Supabase OTP")
                Switch(checked = useRealAuth, onCheckedChange = onToggleRealAuth)
            }
            Spacer(Modifier.height(8.dp))
            if (useRealAuth) {
                TextButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit Supabase Settings")
                }
                Spacer(Modifier.height(4.dp))
            }
            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Phone number") },
                singleLine = true,
                placeholder = { Text("+1 555 123 4567") }
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onContinue,
                enabled = !authInFlight,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(if (authInFlight) "Sending..." else "Send OTP")
            }
            Spacer(Modifier.height(8.dp))
            AuthMessageBanner(authMessage)
        }
    }
}

@Composable
private fun OtpShell(
    phone: String,
    otp: String,
    useRealAuth: Boolean,
    onOtpChange: (String) -> Unit,
    onBack: () -> Unit,
    authInFlight: Boolean,
    authMessage: String,
    onVerify: () -> Unit
) {
    ShellScreenFrame {
        CardBlock("Verify Code") {
            Text(
                if (useRealAuth) {
                    "Enter the SMS code sent to ${phone.ifBlank { "your phone" }}."
                } else {
                    "Shell mode: use any 6 digits to continue for ${phone.ifBlank { "your phone" }}."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5F5F5F)
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = otp,
                onValueChange = onOtpChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("OTP code") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(Modifier.height(12.dp))
            AuthMessageBanner(authMessage)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Text("Back")
                }
                Button(onClick = onVerify, enabled = !authInFlight, modifier = Modifier.weight(1f)) {
                    Text(if (authInFlight) "Verifying..." else "Verify")
                }
            }
        }
    }
}

@Composable
private fun AuthMessageBanner(message: String) {
    val trimmed = message.trim()
    if (trimmed.isBlank()) return

    val (bg, fg) = when {
        trimmed.contains("success", ignoreCase = true) ||
            trimmed.contains("verified", ignoreCase = true) ||
            trimmed.contains("sent", ignoreCase = true) -> Color(0xFFE6F4EA) to Color(0xFF1B5E20)
        trimmed.contains("failed", ignoreCase = true) ||
            trimmed.contains("error", ignoreCase = true) -> Color(0xFFFDECEA) to Color(0xFFB3261E)
        else -> Color(0xFFF1F3F4) to Color(0xFF444746)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Text(
            text = trimmed,
            style = MaterialTheme.typography.bodySmall,
            color = fg
        )
    }
}

@Composable
private fun MainMapShell(
    displayName: String,
    diagnosticsMessage: String,
    authMessage: String,
    onOpenSettings: () -> Unit,
    onRunDiagnostics: () -> Unit,
    onLogout: () -> Unit
) {
    ShellScreenFrame {
        HeroPanel("CrimsonCode Main", "Map-first recovery shell for $displayName")
        MapShellCard()
        CardBlock("Nearby Alerts") {
            Text(
                "Map backend is still being restored. This shell mirrors the expected main-page flow while OTP auth works.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5F5F5F)
            )
            Spacer(Modifier.height(4.dp))
            AlertPreviewRow("Medical", "0.8 mi", Color(0xFFD34A32))
            AlertPreviewRow("Fire", "1.4 mi", Color(0xFFEF6C00))
            AlertPreviewRow("Road Hazard", "2.1 mi", Color(0xFF1565C0))
        }
        StatusPanel(diagnosticsMessage = diagnosticsMessage)
        CardBlock("Session") {
            Text(authMessage, style = MaterialTheme.typography.bodySmall, color = Color(0xFF5F5F5F))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onRunDiagnostics,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text("Diagnostics")
            }
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text("Settings")
            }
        }
        ModuleCard("Public Alerts", "Real MapLibre map/data modules are the next restore target.")
        ModuleCard("Private Alerts", "Recipient inbox will be reconnected after the map stack compiles.")
        ModuleCard("Notifications", "Push token sync/click routing remain disabled in the recovery build.")
        TextButton(onClick = onLogout) {
            Text("Sign out (shell)")
        }
    }
}

@Composable
private fun MapShellCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Main Map",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Recovery UI",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF7A1717)
                )
            }
            HorizontalDivider()
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(Color(0xFFEAF1F4), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFFD7E0E5), RoundedCornerShape(16.dp))
                    .padding(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .align(Alignment.Center)
                        .background(Color(0xFFC8D3D8), RoundedCornerShape(99.dp))
                )
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .fillMaxSize()
                        .align(Alignment.Center)
                        .background(Color(0xFFC8D3D8), RoundedCornerShape(99.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(8.dp)
                        .align(Alignment.TopCenter)
                        .padding(top = 34.dp)
                        .background(Color(0xFFC8D3D8), RoundedCornerShape(99.dp))
                )
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(90.dp)
                        .align(Alignment.Center)
                        .background(Color(0x2234A853), RoundedCornerShape(99.dp))
                        .border(1.dp, Color(0x6634A853), RoundedCornerShape(99.dp))
                )
                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .height(14.dp)
                        .align(Alignment.Center)
                        .background(Color(0xFF1E88E5), RoundedCornerShape(99.dp))
                        .border(2.dp, Color.White, RoundedCornerShape(99.dp))
                )
                MapPin("M", Color(0xFFD34A32), Modifier.align(Alignment.TopStart).padding(start = 28.dp, top = 30.dp))
                MapPin("F", Color(0xFFEF6C00), Modifier.align(Alignment.CenterEnd).padding(end = 24.dp))
                MapPin("R", Color(0xFF1565C0), Modifier.align(Alignment.BottomStart).padding(start = 44.dp, bottom = 28.dp))
                Text(
                    "Map view placeholder while the real MapLibre screen is restored",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4A5B63)
                )
            }
        }
    }
}

@Composable
private fun MapPin(label: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(28.dp)
            .height(28.dp)
            .background(color, RoundedCornerShape(99.dp))
            .border(2.dp, Color.White, RoundedCornerShape(99.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AlertPreviewRow(category: String, distance: String, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF7F7F7), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .height(10.dp)
                    .background(accent, RoundedCornerShape(99.dp))
            )
            Text(category)
        }
        Text(distance, style = MaterialTheme.typography.bodySmall, color = Color(0xFF5F5F5F))
    }
}

@Composable
private fun SettingsShell(
    displayName: String,
    onNameChange: (String) -> Unit,
    useRealAuth: Boolean,
    onToggleRealAuth: (Boolean) -> Unit,
    supabaseUrl: String,
    onSupabaseUrlChange: (String) -> Unit,
    supabaseAnonKey: String,
    onSupabaseAnonKeyChange: (String) -> Unit,
    onBack: () -> Unit
) {
    ShellScreenFrame {
        CardBlock("Profile") {
            OutlinedTextField(
                value = displayName,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Display name") },
                singleLine = true
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Changes are local to the shell UI until settings backend integration is restored.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5F5F5F)
            )
        }
        CardBlock("Supabase Auth") {
            if (supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()) {
                Text(
                    "Using build-time Supabase credentials (auto-filled from environment/local properties).",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF1B5E20)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Use real OTP auth")
                Switch(checked = useRealAuth, onCheckedChange = onToggleRealAuth)
            }
            OutlinedTextField(
                value = supabaseUrl,
                onValueChange = onSupabaseUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Supabase URL") },
                singleLine = true
            )
            OutlinedTextField(
                value = supabaseAnonKey,
                onValueChange = onSupabaseAnonKeyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Supabase anon key") },
                singleLine = true
            )
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Home")
        }
    }
}

@Composable
private fun ShellScreenFrame(content: @Composable ColumnScopeShim.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ColumnScopeShim().content()
    }
}

private class ColumnScopeShim

@Composable
private fun HeroPanel(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    listOf(Color(0xFF7A1717), Color(0xFFB93B2A))
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFFE8E0)
            )
        }
    }
}

@Composable
private fun StatusPanel(diagnosticsMessage: String) {
    CardBlock("Recovery Status") {
        Text("Build: OK", color = Color(0xFF1B5E20))
        Text("Launch: OK", color = Color(0xFF1B5E20))
        Text("Auth flow: shell mode", color = Color(0xFF8A5A00))
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        Text(
            diagnosticsMessage,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF5F5F5F)
        )
    }
}

@Composable
private fun ModuleCard(title: String, detail: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(42.dp)
                    .background(Color(0xFFD34A32), RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5F5F5F)
                )
            }
        }
    }
}

@Composable
private fun CardBlock(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()
            content()
        }
    }
}

@Serializable
private data class SendOtpRequest(
    val phone: String,
    val create_user: Boolean = true
)

@Serializable
private data class VerifyOtpRequest(
    val phone: String,
    val token: String,
    val type: String = "sms"
)

private data class AuthCallResult(
    val success: Boolean,
    val message: String,
    val accessToken: String? = null,
    val userId: String? = null
)

private class SupabaseOtpApi {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }
    private val parser = Json { ignoreUnknownKeys = true }

    suspend fun sendOtp(url: String, anonKey: String, phone: String): AuthCallResult {
        if (phone.isBlank()) return AuthCallResult(false, "Phone number is required.")
        return runCatching {
            client.post(url.trimEnd('/') + "/auth/v1/otp") {
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $anonKey")
                contentType(ContentType.Application.Json)
                setBody(SendOtpRequest(phone = phone))
            }
        }.fold(
            onSuccess = { response ->
                if (response.status.value in 200..299) {
                    AuthCallResult(true, "OTP sent successfully.")
                } else {
                    AuthCallResult(false, "OTP request failed (${response.status.value}). ${safeErrorBody(response)}")
                }
            },
            onFailure = { e ->
                AuthCallResult(false, "OTP request error: ${e.message ?: "unknown error"}")
            }
        )
    }

    suspend fun verifyOtp(url: String, anonKey: String, phone: String, token: String): AuthCallResult {
        if (phone.isBlank()) return AuthCallResult(false, "Phone number is required.")
        if (token.isBlank()) return AuthCallResult(false, "OTP code is required.")
        return runCatching {
            client.post(url.trimEnd('/') + "/auth/v1/verify") {
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $anonKey")
                contentType(ContentType.Application.Json)
                setBody(VerifyOtpRequest(phone = phone, token = token))
            }
        }.fold(
            onSuccess = { response ->
                if (response.status.value in 200..299) {
                    val raw = runCatching { response.bodyAsText() }.getOrNull().orEmpty()
                    val payload = runCatching { parser.decodeFromString(VerifyOtpSuccessResponse.serializer(), raw) }.getOrNull()
                    AuthCallResult(
                        success = true,
                        message = "OTP verified successfully.",
                        accessToken = payload?.accessToken,
                        userId = payload?.user?.id
                    )
                } else {
                    AuthCallResult(false, "OTP verify failed (${response.status.value}). ${safeErrorBody(response)}")
                }
            },
            onFailure = { e ->
                AuthCallResult(false, "OTP verify error: ${e.message ?: "unknown error"}")
            }
        )
    }

    fun close() {
        client.close()
    }

    private suspend fun safeErrorBody(response: io.ktor.client.statement.HttpResponse): String {
        val raw = runCatching { response.bodyAsText() }.getOrNull().orEmpty()
        val compact = raw.replace('\n', ' ').replace(Regex("\\s+"), " ").trim()
        return when {
            compact.contains("phone_provider_disabled", ignoreCase = true) ->
                "Phone provider is disabled in Supabase Auth."
            compact.contains("Unsupported phone provider", ignoreCase = true) ->
                "Unsupported phone provider. Configure a supported SMS provider in Supabase."
            compact.contains("invalid username", ignoreCase = true) ->
                "SMS provider credentials are invalid (check provider username/SID)."
            compact.contains("invalid password", ignoreCase = true) ->
                "SMS provider credentials are invalid (check provider password/token)."
            compact.isBlank() -> ""
            else -> compact.take(180)
        }
    }
}

@Serializable
private data class VerifyOtpSuccessResponse(
    @SerialName("access_token") val accessToken: String? = null,
    val user: VerifyOtpUser? = null
)

@Serializable
private data class VerifyOtpUser(
    val id: String? = null
)
