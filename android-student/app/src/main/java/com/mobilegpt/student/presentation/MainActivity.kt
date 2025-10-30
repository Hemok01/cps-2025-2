package com.mobilegpt.student.presentation

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilegpt.student.data.local.SessionPreferences
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity
 * Entry point of the application
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MobileGPTTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val sessionPreferences = remember { SessionPreferences(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "MobileGPT 학습",
            fontSize = 32.sp,
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "시니어를 위한 디지털 교육 도우미",
            fontSize = 20.sp,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = {
                // MVP: 테스트용 세션 ID 설정 (백엔드에서 생성한 TEST001 세션)
                // 실제로는 세션 참가 API를 호출하여 얻은 세션 ID를 사용
                sessionPreferences.setSessionId(1) // TEST001 세션의 ID를 1로 가정
                Toast.makeText(
                    context,
                    "테스트 세션에 참가했습니다 (Session ID: 1)",
                    Toast.LENGTH_SHORT
                ).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Text(
                text = "세션 참가 (테스트)",
                fontSize = 24.sp
            )
        }
    }
}

@Composable
fun MobileGPTTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF2196F3),
            onPrimary = androidx.compose.ui.graphics.Color.White,
            background = androidx.compose.ui.graphics.Color(0xFFFAFAFA)
        ),
        content = content
    )
}
