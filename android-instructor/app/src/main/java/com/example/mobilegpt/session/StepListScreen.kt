package com.example.mobilegpt.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobilegpt.network.ApiClient
import com.example.mobilegpt.data.remote.dto.response.SubtaskResponse
import com.example.mobilegpt.data.remote.dto.response.RecordingSubtasksResponse
import com.example.mobilegpt.viewmodel.StepViewModel
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope

// 스텝 색상 팔레트
private val stepColors = listOf(
    Pair(Color(0xFF2196F3), Color(0xFF3F51B5)),  // Blue to Indigo
    Pair(Color(0xFF9C27B0), Color(0xFFE91E63)),  // Purple to Pink
    Pair(Color(0xFF00BCD4), Color(0xFF2196F3)),  // Cyan to Blue
    Pair(Color(0xFF4CAF50), Color(0xFF009688)),  // Green to Emerald
    Pair(Color(0xFFFF9800), Color(0xFFF44336)),  // Orange to Red
)

@Composable
fun StepListScreen(
    sessionId: String,
    viewModel: StepViewModel,
    onEdit: (Int) -> Unit
) {
    var loaded by remember { mutableStateOf(false) }
    var subtasks by remember { mutableStateOf<List<SubtaskResponse>>(emptyList()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var lectureTitle by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val recordingId = sessionId.toLongOrNull() ?: return@LaunchedEffect
            val response = withContext(Dispatchers.IO) {
                ApiClient.recordingApi.getSubtasksByRecording(recordingId)
            }

            // 성공 또는 404(분석 안됨/변환 안됨)인 경우 모두 처리
            val body = if (response.isSuccessful) {
                response.body()
            } else if (response.code() == 404) {
                // 404 응답의 JSON 본문 파싱
                val errorBody = response.errorBody()?.string()
                if (errorBody != null) {
                    try {
                        Gson().fromJson(errorBody, RecordingSubtasksResponse::class.java)
                    } catch (e: Exception) {
                        null
                    }
                } else null
            } else null

            if (body != null) {
                subtasks = body.subtasks
                lectureTitle = body.lectureTitle
                // 변환되지 않은 경우 메시지 표시
                if (body.error != null) {
                    statusMessage = body.message ?: body.error
                }
                // ViewModel에도 저장 (기존 호환성)
                viewModel.steps = subtasks.map { subtask ->
                    mutableMapOf<String, Any?>(
                        "id" to subtask.id,
                        "step" to subtask.step,
                        "title" to subtask.title,
                        "description" to subtask.description,
                        "text" to subtask.text,
                        "target_action" to subtask.targetAction,
                        "target_package" to subtask.targetPackage,
                        "target_class" to subtask.targetClass,
                        "ui_hint" to subtask.uiHint,
                        "guide_text" to subtask.guideText,
                        "voice_guide_text" to subtask.voiceGuideText,
                        "time" to subtask.time,
                        "content_description" to subtask.contentDescription,
                        "view_id" to subtask.viewId,
                        "bounds" to subtask.bounds
                    )
                }.toMutableList()
            } else {
                statusMessage = "오류: ${response.code()}"
            }
        } catch (e: Exception) {
            statusMessage = "오류: ${e.message}"
        } finally {
            loaded = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 헤더 (스티키)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = lectureTitle ?: "생성된 Step 목록",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "녹화 ID: $sessionId",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF6B7280)
                        )
                    )
                }
            }

            // 상태 메시지 표시
            if (statusMessage != null && viewModel.steps.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    color = Color(0xFFFFF3CD),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusMessage ?: "",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF856404)
                        )
                    )
                }
            }

            // 콘텐츠
            if (!loaded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = Color(0xFF2196F3),
                        strokeWidth = 4.dp
                    )
                }
            } else if (viewModel.steps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "📝",
                            fontSize = 64.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "생성된 스텝이 없습니다",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF6B7280)
                            )
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 20.dp,
                            bottom = 100.dp  // 하단 버튼 공간
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(viewModel.steps.size) { index ->
                            StepCard(
                                step = viewModel.steps[index],
                                index = index,
                                colorPair = stepColors[index % stepColors.size],
                                onEdit = { onEdit(index) },
                                onDelete = {
                                    // 서버에도 삭제 요청
                                    val subtaskId = (viewModel.steps.getOrNull(index)?.get("id") as? Number)?.toLong()
                                    if (subtaskId != null) {
                                        coroutineScope.launch {
                                            try {
                                                withContext(Dispatchers.IO) {
                                                    ApiClient.recordingApi.deleteSubtask(subtaskId)
                                                }
                                            } catch (e: Exception) {
                                                // 오류 무시
                                            }
                                        }
                                    }
                                    viewModel.deleteStep(index)
                                }
                            )
                        }
                    }

                    // 하단 고정 버튼
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        color = Color.White,
                        shadowElevation = 8.dp
                    ) {
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                                .height(56.dp),
                            onClick = {
                                // TODO: Bulk update 구현 시 활성화
                                // 현재는 개별 수정을 사용
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF2196F3),
                                                Color(0xFF3F51B5)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "전체 저장",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepCard(
    step: Map<String, Any?>,
    index: Int,
    colorPair: Pair<Color, Color>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 카드 헤더
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 스텝 번호 배지
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(colorPair.first, colorPair.second)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${step["step"]}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = step["title"]?.toString() ?: "",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    ),
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF)
                )
            }

            // 확장 콘텐츠
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                ) {
                    Divider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // 설명
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF9FAFB)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "설명",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF6B7280)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = step["description"]?.toString() ?: "",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFF374151)
                                )
                            )
                        }
                    }

                    // 텍스트 (있을 경우)
                    step["text"]?.toString()?.let { text ->
                        if (text.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF9FAFB)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "텍스트",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF6B7280)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF374151),
                                            fontWeight = FontWeight.Normal
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 버튼 영역
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 수정 버튼
                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            onClick = onEdit,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF2196F3),
                                                Color(0xFF3F51B5)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "수정",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // 삭제 버튼
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = Color(0xFFFEF2F2),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }
        }
    }
}
