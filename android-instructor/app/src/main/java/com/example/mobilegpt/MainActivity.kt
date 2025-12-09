package com.example.mobilegpt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mobilegpt.network.ApiClient
import com.example.mobilegpt.recording.RecordingScreen
import com.example.mobilegpt.session.SessionListScreen
import com.example.mobilegpt.session.StepListScreen
import com.example.mobilegpt.stepdetail.StepDetailScreen
import com.example.mobilegpt.ui.auth.LoginScreen
import com.example.mobilegpt.viewmodel.StepViewModel
import com.example.mobilegpt.ui.theme.MobilegptTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ApiClient 초기화 (JWT 토큰 관리를 위해 필수)
        ApiClient.initialize(applicationContext)

        // 시작 화면 결정: 토큰이 있으면 메인, 없으면 로그인
        val startDestination = if (ApiClient.getTokenManager().isLoggedIn()) {
            "recording"
        } else {
            "login"
        }

        setContent {
            MobilegptTheme {
                val nav = rememberNavController()
                val vm: StepViewModel = viewModel()

                NavHost(navController = nav, startDestination = startDestination) {

                    // 로그인 화면
                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = {
                                nav.navigate("recording") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }

                    // 메인 녹화 화면
                    composable("recording") {
                        RecordingScreen(
                            onGotoSessionList = { nav.navigate("sessionList") },
                            onLogout = {
                                ApiClient.getTokenManager().clearTokens()
                                nav.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    // 녹화/세션 목록
                    composable("sessionList") {
                        SessionListScreen { sessionId ->
                            nav.navigate("stepList/$sessionId")
                        }
                    }

                    // Step 목록
                    composable("stepList/{sessionId}") { backStack ->
                        val sessionId = backStack.arguments?.getString("sessionId")
                            ?: return@composable
                        StepListScreen(
                            sessionId = sessionId,
                            viewModel = vm,
                            onEdit = { index ->
                                nav.navigate("stepDetail/$sessionId/$index")
                            }
                        )
                    }

                    // Step 상세
                    composable("stepDetail/{sessionId}/{index}") { backStack ->
                        val sessionId = backStack.arguments?.getString("sessionId")
                            ?: return@composable
                        val index = backStack.arguments?.getString("index")?.toIntOrNull()
                            ?: return@composable

                        StepDetailScreen(
                            sessionId = sessionId,
                            index = index,
                            viewModel = vm,
                            onBack = { nav.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
