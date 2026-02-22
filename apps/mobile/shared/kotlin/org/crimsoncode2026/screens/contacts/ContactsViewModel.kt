package org.crimsoncode2026.screens.contacts

import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.crimsoncode2026.contacts.permissions.ContactsPermissionHandler
import org.crimsoncode2026.contacts.permissions.ContactsPermissionState
import org.crimsoncode2026.data.UserContact
import org.crimsoncode2026.data.UserContactRepository
import org.crimsoncode2026.domain.usecases.ImportContactsResult
import org.crimsoncode2026.domain.usecases.ImportContactsUseCase
import org.crimsoncode2026.domain.UserSessionManager

/**
 * Contacts ViewModel for contact selection screen state management.
 *
 * Manages:
 * - Contacts list with loading and error states
 * - Contact selection state (multi-select)
 * - Search and filter functionality
 * - Permission handling
 *
 * Follows the project's @Stable state holder pattern with StateFlow exposure.
 */
@Stable
class ContactsViewModel(
    private val userContactRepository: UserContactRepository,
    private val importContactsUseCase: ImportContactsUseCase,
    private val contactsPermissionHandler: ContactsPermissionHandler,
    private val userSessionManager: UserSessionManager,
    private val scope: CoroutineScope
) {
    // Private mutable state
    private val _contacts = MutableStateFlow<List<UserContact>>(emptyList())
    private val _selectedContactIds = MutableStateFlow<Set<String>>(emptySet())
    private val _searchQuery = MutableStateFlow("")
    private val _showOnlyAppUsers = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _permissionState = MutableStateFlow<ContactsPermissionState>(ContactsPermissionState.Denied)

    // Public read-only state flows
    val contacts: StateFlow<List<UserContact>> = _contacts.asStateFlow()
    val selectedContactIds: StateFlow<Set<String>> = _selectedContactIds.asStateFlow()
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val showOnlyAppUsers: StateFlow<Boolean> = _showOnlyAppUsers.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    val permissionState: StateFlow<ContactsPermissionState> = _permissionState.asStateFlow()

    // Permission state as a flow from handler
    private val _handlerPermissionState = contactsPermissionHandler.contactsPermissionState()
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ContactsPermissionState.Denied
        )

    /**
     * Number of currently selected contacts
     */
    val selectedCount: Int
        get() = _selectedContactIds.value.size

    /**
     * Load contacts for the current user
     */
    fun loadContacts() {
        val userId = userSessionManager.getCurrentUserId()
        if (userId == null) {
            _errorMessage.value = "User not logged in"
            return
        }

        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            when (importContactsUseCase(userId)) {
                is ImportContactsResult.Success -> {
                    val contactsResult = userContactRepository.getContactsByUserId(userId)
                    if (contactsResult.isSuccess) {
                        _contacts.value = contactsResult.getOrNull() ?: emptyList()
                    } else {
                        _errorMessage.value = "Failed to load contacts"
                    }
                    _isLoading.value = false
                }
                is ImportContactsResult.PermissionDenied -> {
                    _errorMessage.value = "Contacts permission required"
                    _isLoading.value = false
                }
                is ImportContactsResult.Error -> {
                    _errorMessage.value = "Failed to load contacts"
                    _isLoading.value = false
                }
            }
        }
    }

    /**
     * Toggle selection of a contact
     *
     * @param contactId The ID of the contact to toggle
     */
    fun toggleContactSelection(contactId: String) {
        val currentSelection = _selectedContactIds.value.toMutableSet()
        if (currentSelection.contains(contactId)) {
            currentSelection.remove(contactId)
        } else {
            currentSelection.add(contactId)
        }
        _selectedContactIds.value = currentSelection
    }

    /**
     * Select a contact (add to selection)
     *
     * @param contactId The ID of the contact to select
     */
    fun selectContact(contactId: String) {
        val currentSelection = _selectedContactIds.value.toMutableSet()
        currentSelection.add(contactId)
        _selectedContactIds.value = currentSelection
    }

    /**
     * Deselect a contact (remove from selection)
     *
     * @param contactId The ID of the contact to deselect
     */
    fun deselectContact(contactId: String) {
        val currentSelection = _selectedContactIds.value.toMutableSet()
        currentSelection.remove(contactId)
        _selectedContactIds.value = currentSelection
    }

    /**
     * Select all visible contacts
     *
     * @param visibleContacts List of contacts currently visible in the UI
     */
    fun selectAllVisible(visibleContacts: List<UserContact>) {
        val currentSelection = _selectedContactIds.value.toMutableSet()
        visibleContacts.forEach { contact ->
            currentSelection.add(contact.id)
        }
        _selectedContactIds.value = currentSelection
    }

    /**
     * Deselect all contacts
     */
    fun clearSelection() {
        _selectedContactIds.value = emptySet()
    }

    /**
     * Update search query
     *
     * @param query New search query
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Toggle "app users only" filter
     *
     * @param showOnlyAppUsers Whether to show only app users
     */
    fun toggleAppUsersFilter(showOnlyAppUsers: Boolean) {
        _showOnlyAppUsers.value = showOnlyAppUsers
    }

    /**
     * Get currently selected contacts
     *
     * @return List of UserContact objects for selected contact IDs
     */
    fun getSelectedContacts(): List<UserContact> {
        val selectedIds = _selectedContactIds.value
        return _contacts.value.filter { it.id in selectedIds }
    }

    /**
     * Get filtered and sorted contacts based on current search and filter state
     *
     * @return List of contacts matching current filters, sorted appropriately
     */
    fun getFilteredContacts(): List<UserContact> {
        val query = _searchQuery.value
        val appUsersOnly = _showOnlyAppUsers.value

        return _contacts.value
            .filter { contact ->
                val matchesSearch = query.isBlank() ||
                        contact.displayName.contains(query, ignoreCase = true)
                val matchesAppFilter = !appUsersOnly || contact.hasApp
                matchesSearch && matchesAppFilter
            }
            .sortedWith(compareBy<UserContact> { !it.hasApp }
                .thenBy { it.displayName.lowercase() })
    }

    /**
     * Request contacts permission
     */
    fun requestPermission() {
        scope.launch {
            contactsPermissionHandler.requestContactsPermission()
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Refresh permission state from handler
     */
    fun refreshPermissionState() {
        scope.launch {
            val state = contactsPermissionHandler.contactsPermissionState()
                .stateIn(
                    scope = scope,
                    started = SharingStarted.Eagerly,
                    initialValue = ContactsPermissionState.Denied
                ).value
            _permissionState.value = state
        }
    }

    /**
     * Check if a specific contact is selected
     *
     * @param contactId The contact ID to check
     * @return true if contact is selected, false otherwise
     */
    fun isContactSelected(contactId: String): Boolean {
        return _selectedContactIds.value.contains(contactId)
    }
}
