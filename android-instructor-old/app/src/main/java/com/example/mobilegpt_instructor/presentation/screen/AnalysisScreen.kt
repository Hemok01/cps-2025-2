package com.example.mobilegpt_instructor.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mobilegpt_instructor.presentation.viewmodel.AnalysisUiState
import com.example.mobilegpt_instructor.presentation.viewmodel.AnalysisViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    viewModel: AnalysisViewModel,
    recordingId: Int,
    onBack: () -> Unit,
    onAnalysisComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // 분석 시작
    LaunchedEffect(recordingId) {
        viewModel.startAnalysis(recordingId)
    }

    // 분석 완료 시 화면 전환
    LaunchedEffect(uiState) {
        if (uiState is AnalysisUiState.Completed) {
            onAnalysisComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GPT 분석") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (uiState) {
                    is AnalysisUiState.Starting -> {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("분석 요청 중...")
                    }
                    is AnalysisUiState.Processing -> {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "GPT가 녹화를 분석하고 있습니다",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "잠시만 기다려 주세요...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        LinearProgressIndicator(
                            modifier = Modifier
                                .width(200.dp)
                                .height(4.dp)
                        )
                    }
                    is AnalysisUiState.Completed -> {
                        val completed = uiState as AnalysisUiState.Completed
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "완료",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "분석 완료!",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${completed.totalSteps}개의 단계가 생성되었습니다",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "분석된 이벤트: ${completed.analyzedEvents}개",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is AnalysisUiState.Error -> {
                        val error = uiState as AnalysisUiState.Error
                        Text(
                            text = "분석 실패",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row {
                            OutlinedButton(onClick = onBack) {
                                Text("돌아가기")
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Button(onClick = { viewModel.startAnalysis(recordingId) }) {
                                Text("다시 시도")
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}
