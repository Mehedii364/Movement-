package com.example.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.database.entities.MovementWithPerson
import com.example.data.local.database.entities.PersonEntity
import com.example.data.repository.DashboardMetrics
import com.example.data.repository.MovementRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val movementRepository: MovementRepository
) : ViewModel() {

    val metrics: StateFlow<DashboardMetrics> = movementRepository.getDashboardMetrics()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardMetrics()
        )

    val recentMovements: StateFlow<List<MovementWithPerson>> = movementRepository.getRecentMovements(10)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Real-time ticking for live outside duration updates
    private val _currentTimeMillis = MutableStateFlow(System.currentTimeMillis())
    val currentTimeMillis = _currentTimeMillis.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(30_000) // update live ticker every 30s
                _currentTimeMillis.value = System.currentTimeMillis()
            }
        }
    }

    val searchResults: StateFlow<List<PersonEntity>> = combine(
        movementRepository.allPersons,
        _searchQuery
    ) { persons, query ->
        if (query.isBlank()) {
            emptyList()
        } else {
            persons.filter {
                it.personName.contains(query, ignoreCase = true) ||
                it.personCode.contains(query, ignoreCase = true) ||
                it.department.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    class Factory(private val movementRepository: MovementRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(movementRepository) as T
        }
    }
}
