package org.crimsoncode2026.screens.contacts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloseCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.crimsoncode2026.compose.ManualContactEntryDialog
import org.crimsoncode2026.contacts.permissions.ContactsPermissionHandler
import org.crimsoncode2026.contacts.permissions.ContactsPermissionState
import org.crimsoncode2026.data.UserContact
import org.crimsoncode2026.data.UserContactRepository
import org.crimsoncode2026.data.UserRepository
import org.crimsoncode2026.domain.usecases.ImportContactsResult
import org.crimsoncode2026.domain.usecases.ImportContactsUseCase
import org.crimsoncode2026.domain.UserSessionManager
import org.crimsoncode2026.utils.PhoneNumberUtils
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Contact selection screen for selecting emergency contacts.
 *
 * Features:
 * - Multi-select contacts for private alert delivery
 * - Search/filter contacts by name
 * - Toggle to show only app users
 * - Display contact name and masked phone number
 * - Badge/icon for app user status
 * - Save button to persist selections
 *
 * @param onBack Callback when user navigates back
 * @param onContactsSaved Callback when user saves selected contacts
 */
@Composable
fun ContactSelectionScreen(
    onBack: () -> Unit,
    onContactsSaved: (List<UserContact>) -> Unit
) {
    val contactsPermissionHandler: ContactsPermissionHandler by inject()
    val importContactsUseCase: ImportContactsUseCase by inject()
    val userContactRepository: UserContactRepository by inject()
    val userRepository: UserRepository by inject()
    val userSessionManager: UserSessionManager by inject()

    var searchQuery by remember { mutableStateOf(TextFieldValue()) }
    var showOnlyAppUsers by remember { mutableStateOf(false) }
    var contacts by remember { mutableStateOf<List<UserContact>>(emptyList()) }
    var selectedContactIds by remember { mutableStateMapOf<String, Boolean>() }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showManualEntryDialog by remember { mutableStateOf(false) }

    val permissionState by contactsPermissionHandler.contactsPermissionState().collectAsState(
        initial = ContactsPermissionState.Denied
    )

    val coroutineScope = rememberCoroutineScope()

    // Load contacts when screen opens
    LaunchedEffect(Unit) {
        val userId = userSessionManager.getCurrentUserId()
        if (userId != null) {
            loadContacts(userId)
        }
    }

    // Reload contacts when permission is granted
    LaunchedEffect(permissionState) {
        if (permissionState is ContactsPermissionState.Granted) {
            val userId = userSessionManager.getCurrentUserId()
            if (userId != null) {
                loadContacts(userId)
            }
        }
    }

    fun loadContacts(userId: String) {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null

            // First, import contacts from device
            when (importContactsUseCase(userId)) {
                is ImportContactsResult.Success -> {
                    // After import, get contacts from repository
                    val contactsResult = userContactRepository.getContactsByUserId(userId)
                    if (contactsResult.isSuccess) {
                        contacts = contactsResult.getOrNull() ?: emptyList()
                    } else {
                        errorMessage = "Failed to load contacts"
                    }
                    isLoading = false
                }
                is ImportContactsResult.PermissionDenied -> {
                    errorMessage = "Contacts permission required"
                    isLoading = false
                }
                is ImportContactsResult.Error -> {
                    errorMessage = "Failed to load contacts"
                    isLoading = false
                }
            }
        }
    }

    // Filter contacts based on search query and app user filter
    val filteredContacts = contacts.filter { contact ->
        val matchesSearch = searchQuery.text.isBlank() ||
                contact.displayName.contains(searchQuery.text, ignoreCase = true)
        val matchesAppFilter = !showOnlyAppUsers || contact.hasApp
        matchesSearch && matchesAppFilter
    }

    // Sort: app users first, then alphabetically by name
    val sortedContacts = filteredContacts.sortedWith(compareBy<UserContact> { !it.hasApp }
        .thenBy { it.displayName.lowercase() })

    fun handleManualContactEntry(phoneNumber: String, displayName: String) {
        coroutineScope.launch {
            val userId = userSessionManager.getCurrentUserId() ?: return@launch

            // Check if contact already exists
            val existingContact = userContactRepository.getContactByPhoneNumber(userId, phoneNumber)
                .getOrNull()

            if (existingContact != null) {
                // Contact already exists, just select it
                selectedContactIds[existingContact.id] = true
                errorMessage = "Contact already in your list"
            } else {
                // Check if contact has the app
                val appUser = userRepository.getUserByPhoneNumber(phoneNumber).getOrNull()
                val now = System.currentTimeMillis()

                // Create new contact
                val newContact = UserContact(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = userId,
                    contactPhoneNumber = phoneNumber,
                    displayName = displayName,
                    hasApp = appUser != null,
                    contactUserId = appUser?.id,
                    createdAt = now,
                    updatedAt = now
                )

                val result = userContactRepository.createContact(newContact)
                if (result.isSuccess) {
                    val createdContact = result.getOrNull()
                    if (createdContact != null) {
                        contacts = contacts + createdContact
                        selectedContactIds[createdContact.id] = true
                    }
                } else {
                    errorMessage = "Failed to add contact: ${result.exceptionOrNull()?.message}"
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            ContactSelectionHeader(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                showOnlyAppUsers = showOnlyAppUsers,
                onAppUsersToggleChange = { showOnlyAppUsers = it },
                selectedCount = selectedContactIds.size,
                onBack = onBack,
                onAddManualContact = { showManualEntryDialog = true },
                onSave = { onContactsSaved(sortedContacts.filter { selectedContactIds[it.id] == true }) }
            )

            // Contacts list
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    ErrorView(
                        message = errorMessage!!,
                        onRetry = {
                            val userId = userSessionManager.getCurrentUserId()
                            if (userId != null) loadContacts(userId)
                        }
                    )
                }
                permissionState !is ContactsPermissionState.Granted -> {
                    PermissionDeniedView(
                        onRequestPermission = {
                            coroutineScope.launch {
                                contactsPermissionHandler.requestContactsPermission()
                            }
                        },
                        onAddManualContact = { showManualEntryDialog = true }
                    )
                }
                sortedContacts.isEmpty() -> {
                    EmptyContactsView(searchQuery.text, showOnlyAppUsers)
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = sortedContacts,
                            key = { it.id }
                        ) { contact ->
                            ContactListItem(
                                contact = contact,
                                isSelected = selectedContactIds[contact.id] == true,
                                onClick = {
                                    selectedContactIds[contact.id] = selectedContactIds[contact.id] != true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Manual contact entry dialog
    if (showManualEntryDialog) {
        ManualContactEntryDialog(
            onDismiss = { showManualEntryDialog = false },
            onContactAdded = { phoneNumber, displayName ->
                handleManualContactEntry(phoneNumber, displayName)
                showManualEntryDialog = false
            }
        )
    }
}

/**
 * Contact list item displaying contact info with selection indicator.
 *
 * Shows:
 * - Contact name
 * - Masked phone number
 * - App user badge
 * - Selection check circle
 *
 * @param contact The contact to display
 * @param isSelected Whether the contact is selected
 * @param onClick Callback when item is clicked
 */
@Composable
private fun ContactListItem(
    contact: UserContact,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contact info
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar placeholder with initials
                val initials = contact.displayName
                    .split(" ")
                    .take(2)
                    .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
                    .joinToString("")

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = if (contact.hasApp) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = initials.ifEmpty { "?" },
                            style = MaterialTheme.typography.titleMedium,
                            color = if (contact.hasApp) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Name and phone
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = contact.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // App user badge
                        if (contact.hasApp) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AppRegistration,
                                    contentDescription = "Has app",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = PhoneNumberUtils.maskPhoneNumber(contact.contactPhoneNumber),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // Selection indicator
            Surface(
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                },
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (isSelected) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Outlined.CheckCircleOutline
                    },
                    contentDescription = if (isSelected) "Selected" else "Not selected",
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * Header for contact selection screen with search and filters
 */
@Composable
private fun ContactSelectionHeader(
    searchQuery: TextFieldValue,
    onSearchChange: (TextFieldValue) -> Unit,
    showOnlyAppUsers: Boolean,
    onAppUsersToggleChange: (Boolean) -> Unit,
    selectedCount: Int,
    onBack: () -> Unit,
    onAddManualContact: () -> Unit,
    onSave: (List<UserContact>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, contentDescription = "Back")
            }
            Text(
                text = "Emergency Contacts",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search contacts...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AppRegistration,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "App Users Only",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Switch(
                checked = showOnlyAppUsers,
                onCheckedChange = onAppUsersToggleChange
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Add contact manually button
        TextButton(
            onClick = onAddManualContact,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Contact Manually")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { onSave(emptyList()) },
            enabled = selectedCount > 0,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (selectedCount == 1) "Save 1 Contact" else "Save $selectedCount Contacts")
        }
    }
}

/**
 * Error view when loading contacts fails
 */
@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CloseCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

/**
 * View when contacts permission is denied
 */
@Composable
private fun PermissionDeniedView(
    onRequestPermission: () -> Unit,
    onAddManualContact: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CloseCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Contacts Permission Required",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Allow access to your contacts to select emergency contacts for private alerts.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onAddManualContact) {
            Text("Add Contact Manually")
        }
    }
}

/**
 * Empty state view when no contacts match filters
 */
@Composable
private fun EmptyContactsView(
    searchQuery: String,
    showOnlyAppUsers: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when {
                searchQuery.isNotBlank() -> "No contacts match \"$searchQuery\""
                showOnlyAppUsers -> "No app users in your contacts"
                else -> "No contacts found"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
