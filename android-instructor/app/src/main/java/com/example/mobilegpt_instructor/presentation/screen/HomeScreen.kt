package com.example.mobilegpt_instructor.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mobilegpt_instructor.data.model.RecordingResponse
import com.example.mobilegpt_instructor.presentation.viewmodel.AuthViewModel
import com.example.mobilegpt_instructor.presentation.viewmodel.RecordingUiState
import com.example.mobilegpt_instructor.presentation.viewmodel.RecordingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    recordingViewModel: RecordingViewModel,
    onLogout: () -> Unit,
    onStartNewRecording: () -> Unit,
    onRecordingClick: (RecordingResponse) -> Unit
) {
    val user by authViewModel.currentUser.collectAsState()
    val uiState by recordingViewModel.uiState.collectAsState()
    val recordings by recordingViewModel.recordings.collectAsState()

    var showNewRecordingDialog by remember { mutableStateOf(false) }

    // 녹화 목록 로드
    LaunchedEffect(Unit) {
        recordingViewModel.loadRecordings()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("녹화 목록") },
                actions = {
                    IconButton(onClick = {
                        authViewModel.logout()
                        onLogout()
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "로그아웃")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewRecordingDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "새 녹화")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 사용자 정보
            user?.let {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "안녕하세요, ${it.username}님",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = it.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 녹화 목록
            when (uiState) {
                is RecordingUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is RecordingUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = (uiState as RecordingUiState.Error).message,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { recordingViewModel.loadRecordings() }) {
                                Text("다시 시도")
                            }
                        }
                    }
                }
                else -> {
                    if (recordings.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "녹화가 없습니다.\n새 녹화를 시작해보세요!",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(recordings) { recording ->
                                RecordingItem(
                                    recording = recording,
                                    onClick = { onRecordingClick(recording) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 새 녹화 다이얼로그
    if (showNewRecordingDialog) {
        NewRecordingDialog(
            onDismiss = { showNewRecordingDialog = false },
            onCreate = { title, description ->
                showNewRecordingDialog = false
                recordingViewModel.createRecording(title, description)
                onStartNewRecording()
            }
        )
    }
}

@Composable
private fun RecordingItem(
    recording: RecordingResponse,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recording.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "이벤트: ${recording.eventCount}개 | 상태: ${recording.status}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (recording.stepCount > 0) {
                    Text(
                        text = "분석된 단계: ${recording.stepCount}개",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            StatusChip(status = recording.status)
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (color, text) = when (status) {
        "RECORDING" -> MaterialTheme.colorScheme.error to "녹화중"
        "COMPLETED" -> MaterialTheme.colorScheme.secondary to "완료"
        "PROCESSING" -> MaterialTheme.colorScheme.tertiary to "분석중"
        "ANALYZED" -> MaterialTheme.colorScheme.primary to "분석완료"
        "FAILED" -> MaterialTheme.colorScheme.error to "실패"
        else -> MaterialTheme.colorScheme.outline to status
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = color,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun NewRecordingDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 녹화") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("제목") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("설명 (선택)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(title, description) },
                enabled = title.isNotBlank()
            ) {
                Text("생성")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
