package org.crimsoncode2026.screens.eventcreation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.crimsoncode2026.data.Category
import org.crimsoncode2026.data.Severity
import org.crimsoncode2026.domain.usecases.CreateEventParams
import org.crimsoncode2026.domain.usecases.CreateEventResult
import org.crimsoncode2026.domain.usecases.CreateEventUseCase
import org.crimsoncode2026.location.LocationData

/**
 * Wizard step for event creation
 *
 * Sequential steps guide user through creating an emergency event:
 * 1. Category selection
 * 2. Severity selection
 * 3. Broadcast scope
 * 4. Location confirmation
 * 5. Description input
 * 6. Review and submit
 */
enum class WizardStep(val stepNumber: Int, val title: String) {
    CATEGORY(1, "Select Category"),
    SEVERITY(2, "Severity"),
    BROADCAST_SCOPE(3, "Broadcast Scope"),
    LOCATION(4, "Location"),
    DESCRIPTION(5, "Description"),
    REVIEW(6, "Review & Submit");

    val hasNext: Boolean
        get() = stepNumber < REVIEW.stepNumber

    val hasPrevious: Boolean
        get() = stepNumber > CATEGORY.stepNumber

    fun next(): WizardStep = entries.getOrNull(stepNumber) ?: this

    fun previous(): WizardStep = entries.getOrNull(stepNumber - 2) ?: this
}

/**
 * Broadcast scope for event delivery
 *
 * PUBLIC: All nearby users within 50 miles
 * PRIVATE: Only selected emergency contacts
 */
enum class BroadcastScope(val displayName: String, val isPublic: Boolean) {
    PUBLIC("All nearby users within 50 miles", true),
    PRIVATE("Your selected emergency contacts", false);

    companion object {
        val default = PUBLIC
    }
}

/**
 * Event creation wizard state
 */
data class EventCreationState(
    val currentStep: WizardStep = WizardStep.CATEGORY,
    val selectedCategory: Category? = null,
    val selectedSeverity: Severity = Severity.ALERT,
    val selectedBroadcastScope: BroadcastScope = BroadcastScope.default,
    val location: LocationData? = null,
    val locationOverride: String? = null,
    val description: String = "",
    val selectedContactIds: List<String> = emptyList(),
    val isSubmitting: Boolean = false,
    val submitResult: CreateEventResult? = null,
    val canProceed: Boolean = false
) {
    val isReviewStep: Boolean
        get() = currentStep == WizardStep.REVIEW

    val isCategoryStep: Boolean
        get() = currentStep == WizardStep.CATEGORY

    val isSeverityStep: Boolean
        get() = currentStep == WizardStep.SEVERITY

    val isBroadcastScopeStep: Boolean
        get() = currentStep == WizardStep.BROADCAST_SCOPE

    val isLocationStep: Boolean
        get() = currentStep == WizardStep.LOCATION

    val isDescriptionStep: Boolean
        get() = currentStep == WizardStep.DESCRIPTION

    val descriptionCharCount: Int
        get() = description.length

    val descriptionRemainingChars: Int
        get() = 500 - description.length

    val isDescriptionValid: Boolean
        get() = description.isNotBlank() && description.length <= 500

    val isLocationValid: Boolean
        get() = location != null
}

/**
 * Event Creation Wizard ViewModel
 *
 * Manages wizard state for event creation flow.
 * Handles navigation between steps and submission using CreateEventUseCase.
 *
 * Spec requirements:
 * - Wizard with 6 sequential steps
 * - Default severity: ALERT (Warning)
 * - Default broadcast scope: PUBLIC (50-mile radius)
 * - Description max 500 characters
 * - Public events: broadcastType=PUBLIC, isAnonymous=true
 * - Private events: broadcastType=PRIVATE, requires selected contacts
 *
 * @param createEventUseCase Use case for creating events
 * @param scope Coroutine scope for ViewModel
 */
class EventCreationViewModel(
    private val createEventUseCase: CreateEventUseCase,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(EventCreationState())
    val state: StateFlow<EventCreationState> = _state.asStateFlow()

    init {
        updateCanProceed()
    }

    /**
     * Navigate to the next wizard step
     */
    fun nextStep() {
        val currentState = _state.value
        if (currentState.currentStep.hasNext) {
            _state.value = currentState.copy(
                currentStep = currentState.currentStep.next()
            )
            updateCanProceed()
        }
    }

    /**
     * Navigate to the previous wizard step
     */
    fun previousStep() {
        val currentState = _state.value
        if (currentState.currentStep.hasPrevious) {
            _state.value = currentState.copy(
                currentStep = currentState.currentStep.previous()
            )
            updateCanProceed()
        }
    }

    /**
     * Navigate to a specific wizard step
     */
    fun goToStep(step: WizardStep) {
        _state.value = _state.value.copy(currentStep = step)
        updateCanProceed()
    }

    /**
     * Set selected category
     */
    fun setCategory(category: Category) {
        _state.value = _state.value.copy(selectedCategory = category)
        updateCanProceed()
    }

    /**
     * Set selected severity
     */
    fun setSeverity(severity: Severity) {
        _state.value = _state.value.copy(selectedSeverity = severity)
        updateCanProceed()
    }

    /**
     * Set selected broadcast scope
     */
    fun setBroadcastScope(scope: BroadcastScope) {
        _state.value = _state.value.copy(selectedBroadcastScope = scope)
        updateCanProceed()
    }

    /**
     * Set event location
     */
    fun setLocation(location: LocationData) {
        _state.value = _state.value.copy(location = location, locationOverride = null)
        updateCanProceed()
    }

    /**
     * Set manual location override
     */
    fun setLocationOverride(override: String) {
        _state.value = _state.value.copy(locationOverride = override)
    }

    /**
     * Clear location and location override
     */
    fun clearLocation() {
        _state.value = _state.value.copy(location = null, locationOverride = null)
        updateCanProceed()
    }

    /**
     * Set description text
     */
    fun setDescription(text: String) {
        _state.value = _state.value.copy(description = text.take(500))
        updateCanProceed()
    }

    /**
     * Set selected contact IDs for private events
     */
    fun setSelectedContactIds(ids: List<String>) {
        _state.value = _state.value.copy(selectedContactIds = ids)
    }

    /**
     * Submit the event for creation
     */
    fun submitEvent() {
        val currentState = _state.value

        if (!isFormValid()) {
            return
        }

        scope.launch {
            _state.value = currentState.copy(isSubmitting = true)

            val result = createEventUseCase(
                params = CreateEventParams(
                    severity = currentState.selectedSeverity,
                    category = currentState.selectedCategory!!,
                    lat = currentState.location!!.coordinates.latitude,
                    lon = currentState.location!!.coordinates.longitude,
                    locationOverride = currentState.locationOverride,
                    description = currentState.description,
                    isPublic = currentState.selectedBroadcastScope.isPublic,
                    selectedContactIds = currentState.selectedContactIds
                )
            )

            _state.value = _state.value.copy(
                isSubmitting = false,
                submitResult = result
            )
        }
    }

    /**
     * Check if the form is valid for submission
     */
    private fun isFormValid(): Boolean {
        val currentState = _state.value
        return currentState.selectedCategory != null &&
                currentState.location != null &&
                currentState.description.isNotBlank() &&
                currentState.description.length <= 500 &&
                (currentState.selectedBroadcastScope.isPublic || currentState.selectedContactIds.isNotEmpty())
    }

    /**
     * Update whether user can proceed to next step
     */
    private fun updateCanProceed() {
        val currentState = _state.value
        val canProceed = when (currentState.currentStep) {
            WizardStep.CATEGORY -> currentState.selectedCategory != null
            WizardStep.SEVERITY -> true // Severity has default value
            WizardStep.BROADCAST_SCOPE -> true // Broadcast scope has default value
            WizardStep.LOCATION -> currentState.location != null
            WizardStep.DESCRIPTION -> currentState.isDescriptionValid
            WizardStep.REVIEW -> isFormValid()
        }
        _state.value = currentState.copy(canProceed = canProceed)
    }

    /**
     * Reset the wizard state for a new event
     */
    fun reset() {
        _state.value = EventCreationState()
        updateCanProceed()
    }

    /**
     * Clear submit result
     */
    fun clearSubmitResult() {
        _state.value = _state.value.copy(submitResult = null)
    }
}
