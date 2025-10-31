package com.mobilegpt.student.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilegpt.student.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Login ViewModel
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * 로그인
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading

            val result = authRepository.login(email, password)
            _uiState.value = if (result.isSuccess) {
                val userName = authRepository.getUserName()
                LoginUiState.Success(userName)
            } else {
                LoginUiState.Error(result.exceptionOrNull()?.message ?: "로그인 실패")
            }
        }
    }

    /**
     * 로그인 여부 확인
     */
    fun checkLoginStatus(): Boolean {
        return authRepository.isLoggedIn()
    }

    /**
     * 에러 상태 초기화
     */
    fun clearError() {
        _uiState.value = LoginUiState.Idle
    }
}

/**
 * Login UI State
 */
sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val userName: String) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}
