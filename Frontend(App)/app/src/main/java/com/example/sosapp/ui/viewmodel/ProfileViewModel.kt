package com.example.sosapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sosapp.data.model.AuthResult
import com.example.sosapp.data.model.ProfileUiState
import com.example.sosapp.data.model.UserData
import com.example.sosapp.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // First try to get cached user data
            val cachedUser = authRepository.getCurrentUser()
            if (cachedUser != null) {
                _userData.value = cachedUser
            }

            // Then refresh from server
            when (val result = authRepository.refreshUserData()) {
                is AuthResult.Success -> {
                    _userData.value = result.data
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = null
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.error.message
                    )
                    // If we have cached data and server fails, keep using cached data
                    if (cachedUser == null) {
                        _userData.value = null
                    }
                }
            }
        }
    }

    fun updateEmergencyContacts(contacts: List<String>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdating = true, errorMessage = null)

            when (val result = authRepository.updateEmergencyContacts(contacts)) {
                is AuthResult.Success -> {
                    _userData.value = result.data
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        successMessage = "Emergency contacts updated successfully"
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        errorMessage = result.error.message
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _userData.value = null
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}