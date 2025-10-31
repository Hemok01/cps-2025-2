package com.mobilegpt.student.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mobilegpt.student.presentation.screen.LoginScreen
import com.mobilegpt.student.presentation.screen.SessionCodeScreen
import com.mobilegpt.student.presentation.screen.SessionScreen

/**
 * Navigation Routes
 */
object Routes {
    const val LOGIN = "login"
    const val SESSION_CODE = "session_code"
    const val SESSION = "session"
}

/**
 * Navigation Graph
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 로그인 화면
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.SESSION_CODE) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // 세션 코드 입력 화면
        composable(Routes.SESSION_CODE) {
            SessionCodeScreen(
                onJoinSuccess = {
                    navController.navigate(Routes.SESSION) {
                        popUpTo(Routes.SESSION_CODE) { inclusive = true }
                    }
                }
            )
        }

        // 세션 진행 화면
        composable(Routes.SESSION) {
            SessionScreen()
        }
    }
}
