package com.example.mobilegpt.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilegpt.data.local.TokenManager
import com.example.mobilegpt.data.remote.dto.request.LoginRequest
import com.example.mobilegpt.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null,
    val email: String = "",
    val password: String = ""
)

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun login() {
        val state = _uiState.value

        // 입력 검증
        if (state.email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "이메일을 입력해주세요.") }
            return
        }
        if (state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "비밀번호를 입력해주세요.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val request = LoginRequest(
                    email = state.email.trim(),
                    password = state.password
                )
                val response = ApiClient.authApi.login(request)

                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!

                    // 토큰 저장
                    val tokenManager = ApiClient.getTokenManager()
                    tokenManager.saveTokens(
                        access = authResponse.access,
                        refresh = authResponse.refresh ?: ""
                    )

                    _uiState.update {
                        it.copy(isLoading = false, isLoggedIn = true)
                    }
                } else {
                    val errorMsg = when (response.code()) {
                        401 -> "이메일 또는 비밀번호가 올바르지 않습니다."
                        404 -> "등록되지 않은 이메일입니다."
                        else -> "로그인 실패: ${response.code()}"
                    }
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = errorMsg)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "네트워크 오류: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
