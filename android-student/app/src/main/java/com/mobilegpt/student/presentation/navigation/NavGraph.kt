package com.mobilegpt.student.presentation.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mobilegpt.student.data.local.TokenPreferences
import com.mobilegpt.student.domain.model.ConnectionStatus
import com.mobilegpt.student.domain.model.SessionSummary
import com.mobilegpt.student.presentation.MainActivity
import com.mobilegpt.student.presentation.screen.*
import com.mobilegpt.student.presentation.viewmodel.SessionViewModel
import com.mobilegpt.student.service.ScreenCaptureService
import androidx.lifecycle.repeatOnLifecycle

/**
 * Navigation Routes
 */
object Routes {
    const val REGISTER = "register"                  // 간편 등록 (이름 입력)
    const val SESSION_CODE = "session_code"          // 세션 코드 입력
    const val PERMISSIONS = "permissions"            // 권한 설정
    const val SESSION_WAITING = "session_waiting"    // 세션 대기
    const val SESSION_ACTIVE = "session_active"      // 세션 진행
    const val SESSION_SUMMARY = "session_summary"    // 결과 요약

    // Legacy (호환성 유지)
    const val LOGIN = "login"
    const val SESSION = "session"
}

/**
 * Navigation Graph
 * 전체 앱 플로우를 관리합니다.
 *
 * 플로우:
 * REGISTER → SESSION_CODE → PERMISSIONS → SESSION_WAITING → SESSION_ACTIVE → SESSION_SUMMARY
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    tokenPreferences: TokenPreferences,
    initialSessionCode: String? = null
) {
    // 공유 ViewModel (세션 관련 상태)
    val sessionViewModel: SessionViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ==================== 간편 등록 화면 ====================
        composable(Routes.REGISTER) {
            RegisterScreen(
                tokenPreferences = tokenPreferences,
                onRegisterComplete = {
                    navController.navigate(Routes.SESSION_CODE) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                }
            )
        }

        // ==================== 세션 코드 입력 화면 ====================
        composable(Routes.SESSION_CODE) {
            SessionCodeScreen(
                initialSessionCode = initialSessionCode,
                onJoinSuccess = {
                    navController.navigate(Routes.PERMISSIONS) {
                        popUpTo(Routes.SESSION_CODE) { inclusive = true }
                    }
                },
                viewModel = sessionViewModel
            )
        }

        // ==================== 권한 설정 화면 ====================
        composable(Routes.PERMISSIONS) {
            PermissionScreen(
                onAllPermissionsGranted = {
                    navController.navigate(Routes.SESSION_WAITING) {
                        popUpTo(Routes.PERMISSIONS) { inclusive = true }
                    }
                }
            )
        }

        // ==================== 세션 대기 화면 ====================
        composable(Routes.SESSION_WAITING) {
            val joinState by sessionViewModel.joinSessionState.collectAsState()
            val connectionState by sessionViewModel.connectionState.collectAsState()
            val sessionStatus by sessionViewModel.sessionStatus.collectAsState()

            // 세션 데이터 가져오기
            val sessionData = (joinState as? JoinSessionUiState.Success)?.response?.session

            if (sessionData != null) {
                SessionWaitingScreen(
                    sessionData = sessionData,
                    userName = tokenPreferences.getDisplayName() ?: "사용자",
                    connectionStatus = connectionState.toConnectionStatus(),
                    onLeaveSession = {
                        sessionViewModel.resetSession()  // 세션 상태 초기화
                        navController.navigate(Routes.SESSION_CODE) {
                            popUpTo(Routes.SESSION_WAITING) { inclusive = true }
                        }
                    }
                )

                // 세션 시작 감지 (WebSocket으로 실시간 업데이트되는 sessionStatus 감시)
                LaunchedEffect(sessionStatus) {
                    android.util.Log.d("NavGraph", "SESSION_WAITING: sessionStatus changed to $sessionStatus")
                    if (sessionStatus == "IN_PROGRESS") {
                        android.util.Log.d("NavGraph", "SESSION_WAITING: Navigating to SESSION_ACTIVE")
                        navController.navigate(Routes.SESSION_ACTIVE) {
                            popUpTo(Routes.SESSION_WAITING) { inclusive = true }
                        }
                    }
                }
            } else {
                // sessionData가 null인 경우 - 로딩 표시
                android.util.Log.e("NavGraph", "SessionWaitingScreen: sessionData is NULL! joinState=$joinState")
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "세션에 참여하는 중...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // ==================== 세션 활성 화면 ====================
        composable(Routes.SESSION_ACTIVE) {
            val context = LocalContext.current
            val joinState by sessionViewModel.joinSessionState.collectAsState()
            val connectionState by sessionViewModel.connectionState.collectAsState()
            val sessionStatus by sessionViewModel.sessionStatus.collectAsState()
            val currentStep by sessionViewModel.currentStep.collectAsState()
            val totalSteps by sessionViewModel.totalSteps.collectAsState()
            val currentStepTitle by sessionViewModel.currentStepTitle.collectAsState()
            val currentSubtaskId by sessionViewModel.currentSubtaskId.collectAsState()
            val isMediaProjectionPermissionGranted by sessionViewModel.isMediaProjectionPermissionGranted.collectAsState()
            val isScreenCaptureActive by sessionViewModel.isScreenCaptureActive.collectAsState()

            val sessionData = (joinState as? JoinSessionUiState.Success)?.response?.session

            // ★ 앱이 포그라운드로 돌아올 때 진행도 새로고침
            val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
            LaunchedEffect(lifecycleOwner) {
                lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
                    android.util.Log.d("NavGraph", "SESSION_ACTIVE: App resumed - refreshing progress")
                    sessionViewModel.refreshProgressFromPreferences()
                }
            }

            // 세션 활성 화면 진입 시 MediaProjection 권한 요청
            LaunchedEffect(Unit) {
                android.util.Log.d("NavGraph", "SESSION_ACTIVE: Checking MediaProjection permission")
                if (!ScreenCaptureService.hasMediaProjectionPermission()) {
                    android.util.Log.d("NavGraph", "SESSION_ACTIVE: Requesting MediaProjection permission")
                    // MainActivity에서 권한 요청
                    val activity = context as? MainActivity
                    if (activity != null) {
                        MainActivity.requestMediaProjectionPermission(activity) { granted ->
                            android.util.Log.d("NavGraph", "SESSION_ACTIVE: MediaProjection permission result: $granted")
                            sessionViewModel.onMediaProjectionPermissionResult(granted)
                        }
                    } else {
                        android.util.Log.e("NavGraph", "SESSION_ACTIVE: Context is not MainActivity")
                    }
                } else {
                    android.util.Log.d("NavGraph", "SESSION_ACTIVE: MediaProjection permission already granted, starting capture")
                    // 이미 권한이 있으면 바로 스크린캡처 시작
                    sessionViewModel.onMediaProjectionPermissionResult(true)
                }
            }

            if (sessionData != null) {
                // ★ 현재 단계 정보를 ViewModel 상태에서 동적으로 생성
                // sessionData.currentSubtaskDetail은 초기 값이라 변하지 않음
                val currentStepDetail = remember(currentSubtaskId, currentStepTitle) {
                    if (currentSubtaskId != null) {
                        com.mobilegpt.student.domain.model.SubtaskDetail(
                            id = currentSubtaskId!!,
                            title = currentStepTitle,
                            orderIndex = currentStep - 1  // 0-based index
                        )
                    } else {
                        sessionData.currentSubtaskDetail
                    }
                }

                android.util.Log.d("NavGraph", "SessionActiveScreen: currentStep=$currentStep, title=$currentStepTitle, id=$currentSubtaskId")
                SessionActiveScreen(
                    sessionData = sessionData,
                    currentStep = currentStepDetail,
                    currentStepIndex = currentStep,
                    totalSteps = totalSteps,
                    connectionStatus = connectionState.toConnectionStatus(),
                    onStepComplete = {
                        sessionViewModel.completeCurrentStep()
                    },
                    onHelpRequest = {
                        sessionViewModel.requestHelp()
                    },
                    onStartPractice = {
                        sessionViewModel.startOverlay()
                    },
                    onBackToApp = {
                        sessionViewModel.stopOverlay()
                    }
                )

                // 세션 종료 감지 (WebSocket으로 실시간 업데이트되는 sessionStatus 감시)
                LaunchedEffect(sessionStatus) {
                    android.util.Log.d("NavGraph", "SESSION_ACTIVE: sessionStatus changed to $sessionStatus")
                    if (sessionStatus == "REVIEW_MODE" || sessionStatus == "ENDED") {
                        android.util.Log.d("NavGraph", "SESSION_ACTIVE: Navigating to SESSION_SUMMARY")
                        navController.navigate(Routes.SESSION_SUMMARY) {
                            popUpTo(Routes.SESSION_ACTIVE) { inclusive = true }
                        }
                    }
                }
            } else {
                // sessionData가 null인 경우 - 로딩 또는 에러 표시
                android.util.Log.e("NavGraph", "SessionActiveScreen: sessionData is NULL! joinState=$joinState")
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "세션 데이터 로딩 중...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // ==================== 결과 요약 화면 ====================
        composable(Routes.SESSION_SUMMARY) {
            val joinState by sessionViewModel.joinSessionState.collectAsState()
            val sessionSummary by sessionViewModel.sessionSummary.collectAsState()

            val sessionData = (joinState as? JoinSessionUiState.Success)?.response?.session

            if (sessionData != null) {
                SessionSummaryScreen(
                    sessionData = sessionData,
                    summary = sessionSummary,
                    onGoHome = {
                        sessionViewModel.resetSession()
                        navController.navigate(Routes.SESSION_CODE) {
                            popUpTo(Routes.SESSION_SUMMARY) { inclusive = true }
                        }
                    }
                )
            }
        }

        // ==================== Legacy Routes ====================
        composable(Routes.LOGIN) {
            // LoginScreen으로 리다이렉트 또는 RegisterScreen 사용
            RegisterScreen(
                tokenPreferences = tokenPreferences,
                onRegisterComplete = {
                    navController.navigate(Routes.SESSION_CODE) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SESSION) {
            // SessionScreen은 기존 코드와 호환성을 위해 유지
            SessionScreen()
        }
    }
}

/**
 * WebSocketConnectionState → ConnectionStatus 변환
 */
private fun com.mobilegpt.student.data.websocket.WebSocketConnectionState.toConnectionStatus(): ConnectionStatus {
    return when (this) {
        com.mobilegpt.student.data.websocket.WebSocketConnectionState.CONNECTED -> ConnectionStatus.CONNECTED
        com.mobilegpt.student.data.websocket.WebSocketConnectionState.CONNECTING -> ConnectionStatus.CONNECTING
        com.mobilegpt.student.data.websocket.WebSocketConnectionState.DISCONNECTED -> ConnectionStatus.DISCONNECTED
        com.mobilegpt.student.data.websocket.WebSocketConnectionState.ERROR -> ConnectionStatus.ERROR
    }
}

/**
 * JoinSession UI State
 */
sealed class JoinSessionUiState {
    object Idle : JoinSessionUiState()
    object Loading : JoinSessionUiState()
    data class Success(val response: com.mobilegpt.student.data.api.JoinSessionResponse) : JoinSessionUiState()
    data class Error(val message: String) : JoinSessionUiState()
}
