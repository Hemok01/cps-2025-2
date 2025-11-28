package com.mobilegpt.student.presentation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.mobilegpt.student.data.local.TokenPreferences
import com.mobilegpt.student.presentation.navigation.NavGraph
import com.mobilegpt.student.presentation.navigation.Routes
import com.mobilegpt.student.service.ScreenCaptureService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main Activity
 * Entry point of the application
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"

        // MediaProjection 권한 요청 콜백 저장
        private var mediaProjectionCallback: ((Boolean) -> Unit)? = null

        /**
         * MediaProjection 권한 요청
         * @param activity MainActivity 인스턴스
         * @param onResult 권한 결과 콜백 (granted: Boolean)
         */
        fun requestMediaProjectionPermission(activity: MainActivity, onResult: (Boolean) -> Unit) {
            mediaProjectionCallback = onResult
            val projectionManager = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            activity.mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
        }
    }

    @Inject
    lateinit var tokenPreferences: TokenPreferences

    private var deepLinkSessionCode: String? = null

    // MediaProjection 권한 요청 런처
    private lateinit var mediaProjectionLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // MediaProjection 권한 결과 처리 런처 등록
        mediaProjectionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                Log.d(TAG, "MediaProjection permission granted")
                // ScreenCaptureService에 결과 저장
                ScreenCaptureService.setMediaProjectionResult(result.resultCode, result.data)
                mediaProjectionCallback?.invoke(true)
            } else {
                Log.d(TAG, "MediaProjection permission denied")
                mediaProjectionCallback?.invoke(false)
            }
            mediaProjectionCallback = null
        }

        // Handle deep link intent
        handleIntent(intent)

        setContent {
            MobileGPTTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MobileGPTApp(
                        tokenPreferences = tokenPreferences,
                        startDestination = if (tokenPreferences.isRegistered()) {
                            Routes.SESSION_CODE
                        } else {
                            Routes.REGISTER
                        },
                        initialSessionCode = deepLinkSessionCode
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data
        if (data != null && data.scheme == "mobilegpt" && data.host == "join") {
            // Extract session code from deep link
            // URL format: mobilegpt://join/ABC123
            val pathSegments = data.pathSegments
            if (pathSegments.isNotEmpty()) {
                deepLinkSessionCode = pathSegments[0]
                Log.d("MainActivity", "Deep link session code: $deepLinkSessionCode")
            }
        }
    }
}

@Composable
fun MobileGPTApp(
    tokenPreferences: TokenPreferences,
    startDestination: String,
    initialSessionCode: String? = null
) {
    val navController = rememberNavController()

    NavGraph(
        navController = navController,
        startDestination = startDestination,
        tokenPreferences = tokenPreferences,
        initialSessionCode = initialSessionCode
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
