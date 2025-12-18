package com.example.mobilegpt_instructor.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mobilegpt_instructor.data.model.AnalyzedStep
import com.example.mobilegpt_instructor.presentation.viewmodel.AnalysisUiState
import com.example.mobilegpt_instructor.presentation.viewmodel.AnalysisViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepReviewScreen(
    viewModel: AnalysisViewModel,
    onBack: () -> Unit,
    onConversionComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val steps by viewModel.steps.collectAsState()

    var showConvertDialog by remember { mutableStateOf(false) }
    var editingStepIndex by remember { mutableStateOf<Int?>(null) }

    // 변환 완료 시 화면 전환
    LaunchedEffect(uiState) {
        if (uiState is AnalysisUiState.Converted) {
            onConversionComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("단계 검토 (${steps.size}개)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    // 저장 버튼
                    IconButton(onClick = { viewModel.saveSteps() }) {
                        Icon(Icons.Default.Save, contentDescription = "저장")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("취소")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = { showConvertDialog = true },
                        modifier = Modifier.weight(1f),
                        enabled = steps.isNotEmpty()
                    ) {
                        Text("강의로 변환")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (steps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "분석된 단계가 없습니다",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(steps) { index, step ->
                        StepCard(
                            step = step,
                            index = index,
                            onEdit = { editingStepIndex = index },
                            onDelete = { viewModel.removeStep(index) }
                        )
                    }
                }
            }

            // 로딩 상태
            if (uiState is AnalysisUiState.Saving || uiState is AnalysisUiState.Converting) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (uiState is AnalysisUiState.Saving) "저장 중..." else "강의로 변환 중..."
                            )
                        }
                    }
                }
            }
        }
    }

    // 강의 변환 다이얼로그
    if (showConvertDialog) {
        ConvertToLectureDialog(
            onDismiss = { showConvertDialog = false },
            onConvert = { title, description ->
                showConvertDialog = false
                viewModel.convertToLecture(title, description)
            }
        )
    }

    // 단계 수정 다이얼로그
    editingStepIndex?.let { index ->
        val step = steps.getOrNull(index)
        if (step != null) {
            EditStepDialog(
                step = step,
                onDismiss = { editingStepIndex = null },
                onSave = { updatedStep ->
                    viewModel.updateStep(index, updatedStep)
                    editingStepIndex = null
                }
            )
        }
    }
}

@Composable
private fun StepCard(
    step: AnalyzedStep,
    index: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 단계 번호
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "${step.step}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                // 액션 버튼
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "수정",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "삭제",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 제목
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleMedium
            )

            // 설명
            if (step.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 이벤트 타입
            step.eventType?.let {
                Row {
                    Text(
                        text = "액션: ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // 패키지명
            step.packageName?.let {
                Row {
                    Text(
                        text = "앱: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ConvertToLectureDialog(
    onDismiss: () -> Unit,
    onConvert: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("강의로 변환") },
        text = {
            Column {
                Text(
                    text = "분석된 단계들을 강의로 변환합니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("강의 제목") },
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
                onClick = { onConvert(title, description) },
                enabled = title.isNotBlank()
            ) {
                Text("변환")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@Composable
private fun EditStepDialog(
    step: AnalyzedStep,
    onDismiss: () -> Unit,
    onSave: (AnalyzedStep) -> Unit
) {
    var title by remember { mutableStateOf(step.title) }
    var description by remember { mutableStateOf(step.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("단계 수정") },
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
                    label = { Text("설명") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(step.copy(title = title, description = description))
                },
                enabled = title.isNotBlank()
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
