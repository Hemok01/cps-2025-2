package com.example.mobilegpt_instructor.presentation.screen

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mobilegpt_instructor.presentation.viewmodel.RecordingUiState
import com.example.mobilegpt_instructor.presentation.viewmodel.RecordingViewModel
import com.example.mobilegpt_instructor.service.RecordingAccessibilityService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel,
    onBack: () -> Unit,
    onRecordingComplete: (Int) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val currentRecording by viewModel.currentRecording.collectAsState()

    // 접근성 서비스 활성화 상태
    var isAccessibilityEnabled by remember { mutableStateOf(false) }

    // 접근성 서비스 상태 확인
    LaunchedEffect(Unit) {
        isAccessibilityEnabled = RecordingAccessibilityService.isEnabled(context)
    }

    // 녹화 완료 시 화면 전환
    LaunchedEffect(uiState) {
        if (uiState is RecordingUiState.Completed) {
            currentRecording?.let { recording ->
                onRecordingComplete(recording.id)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("녹화") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 현재 녹화 정보
            currentRecording?.let { recording ->
                Text(
                    text = recording.title,
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "이벤트: ${recording.eventCount}개",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            // 접근성 서비스 미활성화 경고
            if (!isAccessibilityEnabled) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "접근성 서비스가 비활성화 상태입니다",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "녹화를 위해 접근성 서비스를 활성화해주세요",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                )
                            }
                        ) {
                            Text("설정으로 이동")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // 녹화 상태에 따른 UI
            when (uiState) {
                is RecordingUiState.Loading -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("준비 중...")
                }
                is RecordingUiState.Created -> {
                    // 녹화 시작 버튼
                    Button(
                        onClick = { viewModel.startRecording() },
                        modifier = Modifier
                            .size(120.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        enabled = isAccessibilityEnabled
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "시작",
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "녹화 시작",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                is RecordingUiState.Recording -> {
                    // 녹화 중 - 중지 버튼
                    Button(
                        onClick = { viewModel.stopRecording() },
                        modifier = Modifier
                            .size(120.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "중지",
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "녹화 중...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "다른 앱으로 이동하여 작업을 수행하세요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is RecordingUiState.Error -> {
                    Text(
                        text = (uiState as RecordingUiState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBack) {
                        Text("돌아가기")
                    }
                }
                else -> {}
            }
        }
    }
}
