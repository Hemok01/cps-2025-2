package com.example.mobilegpt_instructor.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.mobilegpt_instructor.presentation.screen.*
import com.example.mobilegpt_instructor.presentation.viewmodel.AnalysisViewModel
import com.example.mobilegpt_instructor.presentation.viewmodel.AuthViewModel
import com.example.mobilegpt_instructor.presentation.viewmodel.RecordingViewModel

/**
 * 네비게이션 목적지 정의
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Recording : Screen("recording")
    object Analysis : Screen("analysis/{recordingId}") {
        fun createRoute(recordingId: Int) = "analysis/$recordingId"
    }
    object StepReview : Screen("step_review")
}

/**
 * 네비게이션 그래프 - 모든 화면 간의 이동 경로 정의
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    // ViewModel들을 최상위에서 생성하여 공유
    val authViewModel: AuthViewModel = viewModel()
    val recordingViewModel: RecordingViewModel = viewModel()
    val analysisViewModel: AnalysisViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 로그인 화면
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // 홈 화면 (녹화 목록)
        composable(Screen.Home.route) {
            HomeScreen(
                authViewModel = authViewModel,
                recordingViewModel = recordingViewModel,
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onStartNewRecording = {
                    navController.navigate(Screen.Recording.route)
                },
                onRecordingClick = { recording ->
                    recordingViewModel.selectRecording(recording)

                    // 상태에 따라 다른 화면으로 이동
                    when (recording.status) {
                        "COMPLETED" -> {
                            // 녹화 완료 - 분석 화면으로
                            navController.navigate(Screen.Analysis.createRoute(recording.id))
                        }
                        "ANALYZED" -> {
                            // 분석 완료 - 단계 검토 화면으로
                            analysisViewModel.resetState()
                            navController.navigate(Screen.StepReview.route)
                        }
                        else -> {
                            // 녹화 중 또는 다른 상태 - 녹화 화면으로
                            navController.navigate(Screen.Recording.route)
                        }
                    }
                }
            )
        }

        // 녹화 화면
        composable(Screen.Recording.route) {
            RecordingScreen(
                viewModel = recordingViewModel,
                onBack = {
                    navController.popBackStack()
                },
                onRecordingComplete = { recordingId ->
                    navController.navigate(Screen.Analysis.createRoute(recordingId)) {
                        popUpTo(Screen.Recording.route) { inclusive = true }
                    }
                }
            )
        }

        // 분석 화면
        composable(
            route = Screen.Analysis.route,
            arguments = listOf(
                navArgument("recordingId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val recordingId = backStackEntry.arguments?.getInt("recordingId") ?: return@composable

            AnalysisScreen(
                viewModel = analysisViewModel,
                recordingId = recordingId,
                onBack = {
                    navController.popBackStack()
                },
                onAnalysisComplete = {
                    navController.navigate(Screen.StepReview.route) {
                        popUpTo(Screen.Analysis.route) { inclusive = true }
                    }
                }
            )
        }

        // 단계 검토 화면
        composable(Screen.StepReview.route) {
            StepReviewScreen(
                viewModel = analysisViewModel,
                onBack = {
                    navController.popBackStack()
                },
                onConversionComplete = {
                    // 강의 변환 완료 - 홈으로 돌아가기
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
