package com.example.ui.movement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.database.entities.PersonEntity
import com.example.data.local.preferences.AppPreferences
import com.example.data.repository.MovementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MovementEntryUiState(
    val selectedPerson: PersonEntity? = null,
    val movementType: String = "OUT", // "OUT" or "IN"
    val purpose: String = "Official Work",
    val customPurpose: String = "",
    val destination: String = "Outside Factory",
    val customDestination: String = "",
    val remarks: String = "",
    val recordedBy: String = "Security Supervisor",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class MovementEntryViewModel(
    private val movementRepository: MovementRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovementEntryUiState())
    val uiState: StateFlow<MovementEntryUiState> = _uiState.asStateFlow()

    val allPersons: StateFlow<List<PersonEntity>> = movementRepository.allPersons
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val customPurposes: StateFlow<List<String>> = appPreferences.customPurposesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val customDestinations: StateFlow<List<String>> = appPreferences.customDestinationsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val basePurposes = listOf(
        "Official Work",
        "Personal Work",
        "Medical",
        "Lunch / Break",
        "Meeting",
        "Emergency",
        "Shift End",
        "Other"
    )

    val baseDestinations = listOf(
        "Outside Factory",
        "Head Office",
        "Client Location",
        "Bank / Commercial",
        "Hospital / Clinic",
        "Main Plant Annex",
        "Home / Residence",
        "Custom Location"
    )

    init {
        // Load initial user session & select Rahim Uddin by default if available
        viewModelScope.launch {
            val session = appPreferences.currentUserFlow.first()
            _uiState.value = _uiState.value.copy(recordedBy = session.fullName)

            movementRepository.allPersons.collect { persons ->
                if (_uiState.value.selectedPerson == null && persons.isNotEmpty()) {
                    val defaultPerson = persons.find { it.personCode == "EMP-1025" } ?: persons.first()
                    _uiState.value = _uiState.value.copy(
                        selectedPerson = defaultPerson,
                        // If selected person is currently OUTSIDE, default movement type to IN
                        movementType = if (defaultPerson.currentStatus == "OUTSIDE") "IN" else "OUT"
                    )
                }
            }
        }
    }

    fun selectPerson(person: PersonEntity) {
        _uiState.value = _uiState.value.copy(
            selectedPerson = person,
            movementType = if (person.currentStatus == "OUTSIDE") "IN" else "OUT",
            errorMessage = null,
            successMessage = null
        )
    }

    fun setMovementType(type: String) {
        val person = _uiState.value.selectedPerson
        var warning: String? = null
        if (person != null) {
            if (type == "OUT" && person.currentStatus == "OUTSIDE") {
                warning = "Notice: ${person.personName} is already recorded as OUTSIDE."
            } else if (type == "IN" && person.currentStatus == "INSIDE") {
                warning = "Notice: ${person.personName} is already recorded as INSIDE."
            }
        }
        _uiState.value = _uiState.value.copy(
            movementType = type,
            errorMessage = warning
        )
    }

    fun setPurpose(purpose: String) {
        _uiState.value = _uiState.value.copy(purpose = purpose, errorMessage = null)
    }

    fun setCustomPurpose(custom: String) {
        _uiState.value = _uiState.value.copy(customPurpose = custom)
    }

    fun setDestination(dest: String) {
        _uiState.value = _uiState.value.copy(destination = dest, errorMessage = null)
    }

    fun setCustomDestination(custom: String) {
        _uiState.value = _uiState.value.copy(customDestination = custom)
    }

    fun setRemarks(remarks: String) {
        _uiState.value = _uiState.value.copy(remarks = remarks)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    fun submitMovement() {
        val state = _uiState.value
        val person = state.selectedPerson ?: run {
            _uiState.value = state.copy(errorMessage = "Please select a registered person.")
            return
        }

        val effectivePurpose = if (state.purpose == "Other" && state.customPurpose.isNotBlank()) {
            state.customPurpose.trim()
        } else {
            state.purpose
        }

        val effectiveDestination = if (state.destination == "Custom Location" && state.customDestination.isNotBlank()) {
            state.customDestination.trim()
        } else {
            state.destination
        }

        if (effectiveDestination.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter or select a destination.")
            return
        }

        _uiState.value = state.copy(isSubmitting = true, errorMessage = null, successMessage = null)

        viewModelScope.launch {
            // Save custom purpose/destination if added
            if (state.purpose == "Other" && state.customPurpose.isNotBlank()) {
                appPreferences.addCustomPurpose(state.customPurpose.trim())
            }
            if (state.destination == "Custom Location" && state.customDestination.isNotBlank()) {
                appPreferences.addCustomDestination(state.customDestination.trim())
            }

            val result = if (state.movementType == "OUT") {
                movementRepository.recordOutMovement(
                    personId = person.id,
                    purpose = effectivePurpose,
                    destination = effectiveDestination,
                    remarks = state.remarks.trim(),
                    recordedBy = state.recordedBy
                )
            } else {
                movementRepository.recordInMovement(
                    personId = person.id,
                    purpose = effectivePurpose,
                    destination = effectiveDestination,
                    remarks = state.remarks.trim(),
                    recordedBy = state.recordedBy
                )
            }

            result.fold(
                onSuccess = {
                    val updatedPerson = movementRepository.getPersonByIdDirect(person.id)
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        selectedPerson = updatedPerson,
                        movementType = if (updatedPerson?.currentStatus == "OUTSIDE") "IN" else "OUT",
                        remarks = "",
                        successMessage = "Movement ${state.movementType} record successfully saved for ${person.personName}!"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = error.message ?: "Failed to record movement."
                    )
                }
            )
        }
    }

    class Factory(
        private val movementRepository: MovementRepository,
        private val appPreferences: AppPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MovementEntryViewModel(movementRepository, appPreferences) as T
        }
    }
}
