package com.example.ui.persons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.database.entities.MovementWithPerson
import com.example.data.local.database.entities.PersonEntity
import com.example.data.repository.MovementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

data class PersonFormState(
    val id: Long? = null,
    val personName: String = "",
    val personCode: String = "",
    val designation: String = "",
    val department: String = "",
    val mobile: String = "",
    val address: String = "",
    val notes: String = "",
    val photoPath: String? = null,
    val isActive: Boolean = true,
    val currentStatus: String = "INSIDE",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class PersonsViewModel(
    private val movementRepository: MovementRepository,
    private val context: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow("ALL") // ALL, INSIDE, OUTSIDE, INACTIVE
    val statusFilter = _statusFilter.asStateFlow()

    private val _formState = MutableStateFlow(PersonFormState())
    val formState = _formState.asStateFlow()

    private val _selectedPersonForDetail = MutableStateFlow<PersonEntity?>(null)
    val selectedPersonForDetail = _selectedPersonForDetail.asStateFlow()

    private val _selectedPersonMovements = MutableStateFlow<List<MovementWithPerson>>(emptyList())
    val selectedPersonMovements = _selectedPersonMovements.asStateFlow()

    val filteredPersons: StateFlow<List<PersonEntity>> = combine(
        movementRepository.allPersons,
        _searchQuery,
        _statusFilter
    ) { persons, query, status ->
        persons.filter { p ->
            val matchesQuery = query.isBlank() ||
                    p.personName.contains(query, ignoreCase = true) ||
                    p.personCode.contains(query, ignoreCase = true) ||
                    p.department.contains(query, ignoreCase = true) ||
                    p.designation.contains(query, ignoreCase = true)

            val matchesStatus = when (status) {
                "ALL" -> true
                "INSIDE" -> p.currentStatus == "INSIDE" && p.isActive
                "OUTSIDE" -> p.currentStatus == "OUTSIDE" && p.isActive
                "INACTIVE" -> !p.isActive
                else -> true
            }

            matchesQuery && matchesStatus
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchChanged(q: String) { _searchQuery.value = q }
    fun onStatusFilterChanged(s: String) { _statusFilter.value = s }

    fun selectPersonForDetail(person: PersonEntity) {
        _selectedPersonForDetail.value = person
        viewModelScope.launch {
            movementRepository.getMovementsForPerson(person.id).collect {
                _selectedPersonMovements.value = it
            }
        }
    }

    fun clearSelectedPerson() {
        _selectedPersonForDetail.value = null
        _selectedPersonMovements.value = emptyList()
    }

    fun prepareNewPersonForm() {
        _formState.value = PersonFormState()
    }

    fun prepareEditPersonForm(person: PersonEntity) {
        _formState.value = PersonFormState(
            id = person.id,
            personName = person.personName,
            personCode = person.personCode,
            designation = person.designation,
            department = person.department,
            mobile = person.mobile ?: "",
            address = person.address ?: "",
            notes = person.notes ?: "",
            photoPath = person.photoPath,
            isActive = person.isActive,
            currentStatus = person.currentStatus
        )
    }

    fun updateFormName(name: String) { _formState.value = _formState.value.copy(personName = name, errorMessage = null) }
    fun updateFormCode(code: String) { _formState.value = _formState.value.copy(personCode = code, errorMessage = null) }
    fun updateFormDesignation(desig: String) { _formState.value = _formState.value.copy(designation = desig, errorMessage = null) }
    fun updateFormDepartment(dept: String) { _formState.value = _formState.value.copy(department = dept, errorMessage = null) }
    fun updateFormMobile(mob: String) { _formState.value = _formState.value.copy(mobile = mob) }
    fun updateFormAddress(addr: String) { _formState.value = _formState.value.copy(address = addr) }
    fun updateFormNotes(notes: String) { _formState.value = _formState.value.copy(notes = notes) }
    fun updateFormActive(active: Boolean) { _formState.value = _formState.value.copy(isActive = active) }
    fun clearFormMessage() { _formState.value = _formState.value.copy(errorMessage = null, successMessage = null) }

    fun savePhotoFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val photosDir = File(context.filesDir, "person_photos").apply { mkdirs() }
                val photoFile = File(photosDir, "person_${System.currentTimeMillis()}.jpg")

                context.contentResolver.openInputStream(uri)?.use { input: InputStream ->
                    val bitmap = BitmapFactory.decodeStream(input)
                    FileOutputStream(photoFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                }
                _formState.value = _formState.value.copy(photoPath = photoFile.absolutePath)
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(errorMessage = "Failed to save photo: ${e.message}")
            }
        }
    }

    fun savePerson(onSuccess: () -> Unit) {
        val state = _formState.value
        if (state.personName.isBlank()) {
            _formState.value = state.copy(errorMessage = "Person Name is required.")
            return
        }
        if (state.personCode.isBlank()) {
            _formState.value = state.copy(errorMessage = "Person ID / Employee ID is required.")
            return
        }
        if (state.designation.isBlank()) {
            _formState.value = state.copy(errorMessage = "Designation is required.")
            return
        }
        if (state.department.isBlank()) {
            _formState.value = state.copy(errorMessage = "Department is required.")
            return
        }

        _formState.value = state.copy(isSaving = true, errorMessage = null)

        viewModelScope.launch {
            val entity = PersonEntity(
                id = state.id ?: 0L,
                personName = state.personName.trim(),
                personCode = state.personCode.trim(),
                photoPath = state.photoPath,
                designation = state.designation.trim(),
                department = state.department.trim(),
                mobile = state.mobile.trim().ifBlank { null },
                address = state.address.trim().ifBlank { null },
                notes = state.notes.trim().ifBlank { null },
                isActive = state.isActive,
                currentStatus = state.currentStatus
            )

            val result = if (state.id == null || state.id == 0L) {
                movementRepository.registerPerson(entity).map { }
            } else {
                movementRepository.updatePerson(entity)
            }

            result.fold(
                onSuccess = {
                    _formState.value = _formState.value.copy(
                        isSaving = false,
                        successMessage = "Person ${state.personName} saved successfully!"
                    )
                    onSuccess()
                },
                onFailure = { error ->
                    _formState.value = _formState.value.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Failed to save person."
                    )
                }
            )
        }
    }

    class Factory(
        private val movementRepository: MovementRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PersonsViewModel(movementRepository, context) as T
        }
    }
}
