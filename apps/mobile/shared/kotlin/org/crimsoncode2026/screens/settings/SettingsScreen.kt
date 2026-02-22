package org.crimsoncode2026.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.crimsoncode2026.data.User
import org.crimsoncode2026.domain.UserSessionManager
import org.crimsoncode2026.domain.usecases.UpdateDisplayNameResult
import org.crimsoncode2026.domain.usecases.UpdateDisplayNameUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * Settings screen with user preferences sections.
 *
 * Per spec, the following are stored locally:
 * - Notification preferences (master toggle, crisis, warning, private alerts, vibration)
 * - Location preferences (high precision mode)
 * - Account settings (display name, logout)
 *
 * Public alert opt-out is also stored locally per spec.
 *
 * Spec reference: lines 598-656 in technical specification
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val viewModel: SettingsViewModel by inject {
        parametersOf(
            androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycleScope.coroutineScope
        )
    }

    val userSessionManager: UserSessionManager by inject()
    val updateDisplayNameUseCase: UpdateDisplayNameUseCase by inject()

    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val crisisAlertsEnabled by viewModel.crisisAlertsEnabled.collectAsState()
    val warningAlertsEnabled by viewModel.warningAlertsEnabled.collectAsState()
    val privateAlertsEnabled by viewModel.privateAlertsEnabled.collectAsState()
    val publicAlertsEnabled by viewModel.publicAlertsEnabled.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val highPrecisionLocation by viewModel.highPrecisionLocation.collectAsState()

    val currentUser = userSessionManager.getCurrentAuthUser()
    val displayName = currentUser?.userMetadata?.get("display_name") as? String ?: ""
    val phoneNumber = currentUser?.phone ?: ""

    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf(displayName) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadPreferences()
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            editNameInput = currentUser.userMetadata?.get("display_name") as? String ?: ""
        }
    }

    // Handle display name edit
    val handleSaveDisplayName = {
        if (editNameInput.isNotBlank()) {
            androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycleScope.coroutineScope.launch {
                val userId = userSessionManager.getCurrentUserId()
                if (userId != null) {
                    when (updateDisplayNameUseCase(userId, editNameInput)) {
                        is UpdateDisplayNameResult.Success -> {
                            showEditNameDialog = false
                        }
                        is UpdateDisplayNameResult.Error -> {
                            // Show error (for MVP, just close dialog)
                            showEditNameDialog = false
                        }
                    }
                }
            }
        }
    }

    // Handle logout
    val handleLogout = {
        androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycleScope.coroutineScope.launch {
            userSessionManager.signOut()
            onNavigateToLogin()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            NotificationSettingsSection(
                notificationsEnabled = notificationsEnabled,
                crisisAlertsEnabled = crisisAlertsEnabled,
                warningAlertsEnabled = warningAlertsEnabled,
                privateAlertsEnabled = privateAlertsEnabled,
                publicAlertsEnabled = publicAlertsEnabled,
                vibrationEnabled = vibrationEnabled,
                onToggleNotifications = { viewModel.toggleNotifications(it) },
                onToggleCrisisAlert = { viewModel.toggleCrisisAlert(it) },
                onToggleWarningAlert = { viewModel.toggleWarningAlert(it) },
                onTogglePrivateAlerts = { viewModel.togglePrivateAlerts(it) },
                onTogglePublicAlerts = { viewModel.togglePublicAlertOptOut(!it) },
                onToggleVibration = { viewModel.toggleVibration(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            LocationSettingsSection(
                highPrecisionLocation = highPrecisionLocation,
                onToggleHighPrecision = { viewModel.toggleHighPrecisionLocation(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            AccountSettingsSection(
                displayName = displayName,
                phoneNumber = phoneNumber,
                onEditDisplayName = { showEditNameDialog = true },
                onLogout = { showLogoutDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Edit Display Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Display Name") },
            text = {
                Column {
                    Text(
                        text = "This name will be shown to your contacts for private events.",
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = handleSaveDisplayName,
                    enabled = editNameInput.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout? You will need to verify your phone number to sign in again.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        handleLogout()
                    }
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Notification Settings section with toggle switches
 *
 * Per spec (lines 615-626):
 * - Master toggle: Enable/disable all notifications (local)
 * - Crisis alerts: Toggle (local)
 * - Warning alerts: Toggle (local)
 * - Public alerts: Toggle - user can opt-out (local)
 * - Private alerts: Toggle (local)
 * - Vibration: Toggle (local)
 */
@Composable
fun NotificationSettingsSection(
    notificationsEnabled: Boolean,
    crisisAlertsEnabled: Boolean,
    warningAlertsEnabled: Boolean,
    privateAlertsEnabled: Boolean,
    publicAlertsEnabled: Boolean,
    vibrationEnabled: Boolean,
    onToggleNotifications: (Boolean) -> Unit,
    onToggleCrisisAlert: (Boolean) -> Unit,
    onToggleWarningAlert: (Boolean) -> Unit,
    onTogglePrivateAlerts: (Boolean) -> Unit,
    onTogglePublicAlerts: (Boolean) -> Unit,
    onToggleVibration: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            icon = Icons.Default.Notifications,
            title = "Notifications"
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsSwitch(
            label = "Enable Notifications",
            description = "Receive emergency alerts",
            checked = notificationsEnabled,
            onCheckedChange = onToggleNotifications
        )

        if (notificationsEnabled) {
            SettingsSwitch(
                label = "Crisis Alerts",
                description = "High priority emergencies",
                checked = crisisAlertsEnabled,
                onCheckedChange = onToggleCrisisAlert
            )

            SettingsSwitch(
                label = "Warning Alerts",
                description = "Standard priority alerts",
                checked = warningAlertsEnabled,
                onCheckedChange = onToggleWarningAlert
            )

            SettingsSwitch(
                label = "Public Alerts",
                description = "Nearby public emergency alerts",
                checked = publicAlertsEnabled,
                onCheckedChange = onTogglePublicAlerts
            )

            SettingsSwitch(
                label = "Private Alerts",
                description = "Alerts from your contacts",
                checked = privateAlertsEnabled,
                onCheckedChange = onTogglePrivateAlerts
            )

            SettingsSwitch(
                label = "Vibration",
                description = "Vibrate on alerts",
                checked = vibrationEnabled,
                onCheckedChange = onToggleVibration
            )
        }
    }
}

/**
 * Section header with icon and title
 */
@Composable
fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium
        )
    }
}

/**
 * Settings toggle switch with label and description
 */
@Composable
fun SettingsSwitch(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Location Settings section
 *
 * Per spec (lines 627-635):
 * - Location Accuracy: High precision mode toggle (local)
 * - Default: Balanced mode (local)
 * - Background Location: Not included in MVP
 */
@Composable
fun LocationSettingsSection(
    highPrecisionLocation: Boolean,
    onToggleHighPrecision: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            icon = Icons.Default.LocationOn,
            title = "Location"
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsSwitch(
            label = "High Precision Location",
            description = "Use GPS for high accuracy (increases battery usage)",
            checked = highPrecisionLocation,
            onCheckedChange = onToggleHighPrecision
        )
    }
}

/**
 * Account Settings section
 *
 * Per spec (lines 636-656):
 * - Display Name: Edit display name (stored in database)
 * - Shown to contacts for private events
 * - Required at registration
 * - Historical events update to reflect new name when changed
 * - Logout button
 *
 * Note: Phone number display and UpdateDisplayNameUseCase integration
 * will be implemented in Account Settings section UI task.
 */
@Composable
fun AccountSettingsSection(
    displayName: String,
    phoneNumber: String,
    onEditDisplayName: () -> Unit,
    onLogout: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            icon = Icons.Default.Person,
            title = "Account"
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = onEditDisplayName
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Display Name",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = displayName,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Phone Number",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = phoneNumber,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        androidx.compose.material3.Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }
    }
}
