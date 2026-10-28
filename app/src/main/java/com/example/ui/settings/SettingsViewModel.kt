package com.example.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.database.entities.UserEntity
import com.example.data.local.preferences.AppPreferences
import com.example.data.local.preferences.UserSession
import com.example.data.repository.SyncRepository
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val appPreferences: AppPreferences,
    private val userRepository: UserRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    val currentSession: StateFlow<UserSession> = appPreferences.currentUserFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSession("admin", "ADMIN", "Security Supervisor")
        )

    val currentLanguage: StateFlow<String> = appPreferences.languageFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "en"
        )

    val currentTheme: StateFlow<String> = appPreferences.themeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "SYSTEM"
        )

    val currentTimeFormat: StateFlow<String> = appPreferences.timeFormatFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "12H"
        )

    val backendUrl: StateFlow<String> = appPreferences.backendUrlFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "https://example.com/api"
        )

    val lastSyncTime: StateFlow<String> = appPreferences.lastSyncTimeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Never"
        )

    val pendingSyncCount: StateFlow<Int> = syncRepository.pendingSyncCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val allUsers: StateFlow<List<UserEntity>> = userRepository.allUsers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _syncStatusMessage = MutableStateFlow<String?>(null)
    val syncStatusMessage = _syncStatusMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    fun setLanguage(lang: String) {
        viewModelScope.launch { appPreferences.setLanguage(lang) }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { appPreferences.setTheme(theme) }
    }

    fun setTimeFormat(format: String) {
        viewModelScope.launch { appPreferences.setTimeFormat(format) }
    }

    fun setBackendUrl(url: String) {
        viewModelScope.launch { appPreferences.setBackendUrl(url.trim()) }
    }

    fun clearSyncMessage() {
        _syncStatusMessage.value = null
    }

    fun triggerSync() {
        _isSyncing.value = true
        _syncStatusMessage.value = null
        viewModelScope.launch {
            val result = syncRepository.performSync()
            result.fold(
                onSuccess = { _syncStatusMessage.value = it },
                onFailure = { _syncStatusMessage.value = "Sync Error: ${it.message}" }
            )
            _isSyncing.value = false
        }
    }

    fun switchUser(user: UserEntity) {
        viewModelScope.launch {
            userRepository.switchUserQuick(user)
        }
    }

    fun createNewUser(username: String, pass: String, fullName: String, role: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val res = userRepository.createUser(username, pass, fullName, role)
            res.fold(
                onSuccess = {
                    _syncStatusMessage.value = "User '$username' created successfully!"
                    onDone()
                },
                onFailure = {
                    _syncStatusMessage.value = "Failed: ${it.message}"
                }
            )
        }
    }

    fun addCustomPurpose(p: String) {
        viewModelScope.launch {
            appPreferences.addCustomPurpose(p)
        }
    }

    fun addCustomDestination(d: String) {
        viewModelScope.launch {
            appPreferences.addCustomDestination(d)
        }
    }

    class Factory(
        private val appPreferences: AppPreferences,
        private val userRepository: UserRepository,
        private val syncRepository: SyncRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(appPreferences, userRepository, syncRepository) as T
        }
    }
}
