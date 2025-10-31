package com.mobilegpt.student.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.mobilegpt.student.data.local.TokenPreferences
import com.mobilegpt.student.presentation.navigation.NavGraph
import com.mobilegpt.student.presentation.navigation.Routes
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main Activity
 * Entry point of the application
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenPreferences: TokenPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MobileGPTTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MobileGPTApp(
                        startDestination = if (tokenPreferences.isLoggedIn()) {
                            Routes.SESSION_CODE
                        } else {
                            Routes.LOGIN
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MobileGPTApp(
    startDestination: String
) {
    val navController = rememberNavController()

    NavGraph(
        navController = navController,
        startDestination = startDestination
    )
}

@Composable
fun MobileGPTTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF2196F3),
            onPrimary = androidx.compose.ui.graphics.Color.White,
            background = androidx.compose.ui.graphics.Color(0xFFFAFAFA),
            primaryContainer = androidx.compose.ui.graphics.Color(0xFFBBDEFB),
            onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF0D47A1)
        ),
        content = content
    )
}
