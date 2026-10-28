package com.example.ui.movement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.database.entities.MovementWithPerson
import com.example.data.local.preferences.AppPreferences
import com.example.data.repository.MovementRepository
import com.example.domain.utils.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DateFilterOption {
    ALL, TODAY, YESTERDAY, LAST_7_DAYS, THIS_MONTH
}

class MovementHistoryViewModel(
    private val movementRepository: MovementRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _dateFilter = MutableStateFlow(DateFilterOption.ALL)
    val dateFilter = _dateFilter.asStateFlow()

    private val _typeFilter = MutableStateFlow("ALL") // ALL, IN, OUT
    val typeFilter = _typeFilter.asStateFlow()

    private val _purposeFilter = MutableStateFlow("ALL")
    val purposeFilter = _purposeFilter.asStateFlow()

    private val _departmentFilter = MutableStateFlow("ALL")
    val departmentFilter = _departmentFilter.asStateFlow()

    private val _sortNewestFirst = MutableStateFlow(true)
    val sortNewestFirst = _sortNewestFirst.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage = _actionMessage.asStateFlow()

    val currentSession = appPreferences.currentUserFlow

    val filteredMovements: StateFlow<List<MovementWithPerson>> = combine(
        movementRepository.allMovementsWithPerson,
        _searchQuery,
        _dateFilter,
        _typeFilter,
        _purposeFilter
    ) { movements, query, dateOpt, typeOpt, purposeOpt ->
        Tuple5(movements, query, dateOpt, typeOpt, purposeOpt)
    }.combine(
        combine(_departmentFilter, _sortNewestFirst) { dept, newest ->
            Pair(dept, newest)
        }
    ) { t5, pair ->
        val rawList = t5.first
        val query = t5.second.trim()
        val dateOpt = t5.third
        val typeOpt = t5.fourth
        val purposeOpt = t5.fifth
        val deptOpt = pair.first
        val newestFirst = pair.second

        val now = System.currentTimeMillis()
        val startOfToday = TimeUtils.getStartOfDay(now)
        val endOfToday = TimeUtils.getEndOfDay(now)
        val startOfYesterday = TimeUtils.getStartOfDay(now - 86400000L)
        val endOfYesterday = TimeUtils.getEndOfDay(now - 86400000L)
        val start7DaysAgo = TimeUtils.getStartOfDaysAgo(7)
        val startThisMonth = TimeUtils.getStartOfMonth()

        val filtered = rawList.filter { item ->
            val m = item.movement
            val p = item.person

            // Query match
            val matchesQuery = query.isBlank() ||
                    p.personName.contains(query, ignoreCase = true) ||
                    p.personCode.contains(query, ignoreCase = true) ||
                    m.destination.contains(query, ignoreCase = true) ||
                    m.purpose.contains(query, ignoreCase = true)

            // Date match
            val matchesDate = when (dateOpt) {
                DateFilterOption.ALL -> true
                DateFilterOption.TODAY -> m.movementTimestamp in startOfToday..endOfToday
                DateFilterOption.YESTERDAY -> m.movementTimestamp in startOfYesterday..endOfYesterday
                DateFilterOption.LAST_7_DAYS -> m.movementTimestamp >= start7DaysAgo
                DateFilterOption.THIS_MONTH -> m.movementTimestamp >= startThisMonth
            }

            // Type match
            val matchesType = typeOpt == "ALL" || m.movementType.equals(typeOpt, ignoreCase = true)

            // Purpose match
            val matchesPurpose = purposeOpt == "ALL" || m.purpose.equals(purposeOpt, ignoreCase = true)

            // Department match
            val matchesDept = deptOpt == "ALL" || p.department.equals(deptOpt, ignoreCase = true)

            matchesQuery && matchesDate && matchesType && matchesPurpose && matchesDept
        }

        if (newestFirst) {
            filtered.sortedByDescending { it.movement.movementTimestamp }
        } else {
            filtered.sortedBy { it.movement.movementTimestamp }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchChanged(query: String) { _searchQuery.value = query }
    fun onDateFilterChanged(filter: DateFilterOption) { _dateFilter.value = filter }
    fun onTypeFilterChanged(type: String) { _typeFilter.value = type }
    fun onPurposeFilterChanged(purpose: String) { _purposeFilter.value = purpose }
    fun onDepartmentFilterChanged(dept: String) { _departmentFilter.value = dept }
    fun toggleSortOrder() { _sortNewestFirst.value = !_sortNewestFirst.value }

    fun clearActionMessage() { _actionMessage.value = null }

    fun correctRecord(
        movementId: Long,
        newPurpose: String,
        newDestination: String,
        newRemarks: String,
        operatorName: String,
        correctionReason: String
    ) {
        viewModelScope.launch {
            val result = movementRepository.correctMovementRecord(
                movementId = movementId,
                newPurpose = newPurpose,
                newDestination = newDestination,
                newRemarks = newRemarks,
                operatorName = operatorName,
                correctionReason = correctionReason
            )
            result.fold(
                onSuccess = { _actionMessage.value = "Movement record corrected with audit note." },
                onFailure = { _actionMessage.value = "Failed: ${it.message}" }
            )
        }
    }

    fun deleteRecord(movementId: Long) {
        viewModelScope.launch {
            val result = movementRepository.deleteMovementRecord(movementId)
            result.fold(
                onSuccess = { _actionMessage.value = "Movement record deleted." },
                onFailure = { _actionMessage.value = "Failed: ${it.message}" }
            )
        }
    }

    class Factory(
        private val movementRepository: MovementRepository,
        private val appPreferences: AppPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MovementHistoryViewModel(movementRepository, appPreferences) as T
        }
    }
}

private data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
