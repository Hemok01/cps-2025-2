package com.example.mobilegpt_instructor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.mobilegpt_instructor.data.network.NetworkModule
import com.example.mobilegpt_instructor.presentation.navigation.NavGraph
import com.example.mobilegpt_instructor.presentation.navigation.Screen
import com.example.mobilegpt_instructor.presentation.theme.MobileGPTInstructorTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 시작 화면 결정 (로그인 상태에 따라)
        val startDestination = determineStartDestination()

        setContent {
            MobileGPTInstructorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(startDestination = startDestination)
                }
            }
        }
    }

    /**
     * 앱 시작 시 로그인 상태를 확인하여 시작 화면 결정
     */
    private fun determineStartDestination(): String {
        return runBlocking {
            val isLoggedIn = NetworkModule.getTokenManager().isLoggedIn.first()
            if (isLoggedIn) Screen.Home.route else Screen.Login.route
        }
    }
}

@Composable
fun MainScreen(startDestination: String) {
    val navController = rememberNavController()

    NavGraph(
        navController = navController,
        startDestination = startDestination
    )
}
