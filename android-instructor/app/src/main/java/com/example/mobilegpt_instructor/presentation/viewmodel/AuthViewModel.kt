package com.example.mobilegpt_instructor.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilegpt_instructor.data.model.UserResponse
import com.example.mobilegpt_instructor.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 인증 상태를 관리하는 ViewModel
 * 로그인, 로그아웃, 사용자 정보 관리
 */
class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    // UI 상태
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // 현재 사용자
    private val _currentUser = MutableStateFlow<UserResponse?>(null)
    val currentUser: StateFlow<UserResponse?> = _currentUser.asStateFlow()

    // 로그인 상태
    val isLoggedIn = repository.isLoggedIn

    init {
        // 로그인 상태라면 사용자 정보 로드
        viewModelScope.launch {
            repository.isLoggedIn.collect { loggedIn ->
                if (loggedIn && _currentUser.value == null) {
                    loadCurrentUser()
                }
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("이메일과 비밀번호를 입력해주세요")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            repository.login(email, password)
                .onSuccess { user ->
                    _currentUser.value = user
                    _uiState.value = AuthUiState.Success
                }
                .onFailure { e ->
                    _uiState.value = AuthUiState.Error(e.message ?: "로그인 실패")
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _currentUser.value = null
            _uiState.value = AuthUiState.Initial
        }
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            repository.getCurrentUser()
                .onSuccess { user ->
                    _currentUser.value = user
                }
        }
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Initial
        }
    }
}

sealed class AuthUiState {
    object Initial : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
