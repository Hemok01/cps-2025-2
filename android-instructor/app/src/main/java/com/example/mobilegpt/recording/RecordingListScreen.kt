package com.example.mobilegpt.recording

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobilegpt.data.remote.dto.request.ConvertToTaskRequest
import com.example.mobilegpt.data.remote.dto.response.RecordingResponse
import com.example.mobilegpt.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 녹화 목록 화면
 * - 녹화 목록 표시
 * - 상태에 따른 버튼 표시 (분석/변환/단계보기)
 */
@Composable
fun RecordingListScreen(
    onRecordingSelected: (String) -> Unit
) {
    var recordings by remember { mutableStateOf<List<RecordingResponse>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // 녹화 목록 새로고침 함수
    suspend fun refreshRecordings() {
        try {
            val response = withContext(Dispatchers.IO) {
                ApiClient.recordingApi.getRecordings()
            }
            if (response.isSuccessful) {
                recordings = response.body()?.results ?: emptyList()
                errorMessage = null
            } else {
                errorMessage = "오류: ${response.code()}"
            }
        } catch (e: Exception) {
            errorMessage = "네트워크 오류: ${e.message}"
        }
    }

    LaunchedEffect(Unit) {
        refreshRecordings()
        loading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 헤더
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "녹화 목록",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    )
                    // 새로고침 버튼
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                loading = true
                                refreshRecordings()
                                loading = false
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "새로고침",
                            tint = Color(0xFF6B7280)
                        )
                    }
                }
            }

            // 콘텐츠
            when {
                loading -> {
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
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "⚠️", fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = errorMessage ?: "알 수 없는 오류",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFFDC2626)
                                )
                            )
                        }
                    }
                }
                recordings.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "📭", fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "저장된 녹화가 없습니다",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFF6B7280)
                                )
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(recordings) { index, recording ->
                            RecordingCard(
                                recording = recording,
                                onViewSubtasks = { onRecordingSelected(recording.id.toString()) },
                                onAnalyze = {
                                    coroutineScope.launch {
                                        try {
                                            val response = withContext(Dispatchers.IO) {
                                                ApiClient.recordingApi.analyzeRecording(recording.id)
                                            }
                                            if (response.isSuccessful) {
                                                Toast.makeText(context, "분석이 시작되었습니다", Toast.LENGTH_SHORT).show()
                                                // 상태 새로고침을 위해 잠시 후 목록 갱신
                                                delay(1000)
                                                refreshRecordings()
                                            } else {
                                                Toast.makeText(context, "분석 시작 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "오류: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onConvert = {
                                    coroutineScope.launch {
                                        try {
                                            val request = ConvertToTaskRequest(
                                                title = recording.title,
                                                description = "${recording.title} 녹화에서 생성된 과제"
                                            )
                                            val response = withContext(Dispatchers.IO) {
                                                ApiClient.recordingApi.convertToTask(recording.id, request)
                                            }
                                            if (response.isSuccessful) {
                                                Toast.makeText(context, "과제 변환이 시작되었습니다", Toast.LENGTH_SHORT).show()
                                                delay(1000)
                                                refreshRecordings()
                                            } else {
                                                Toast.makeText(context, "변환 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "오류: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 녹화 카드 컴포넌트
 */
@Composable
fun RecordingCard(
    recording: RecordingResponse,
    onViewSubtasks: () -> Unit,
    onAnalyze: () -> Unit,
    onConvert: () -> Unit
) {
    val hasTask = recording.task != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 상단: 제목 및 상태
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recording.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F2937)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF6B7280)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = recording.createdAt ?: "-",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF6B7280)
                            )
                        )
                    }
                }

                // 상태 배지
                StatusBadge(status = recording.status, hasTask = hasTask)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 하단: 액션 버튼
            when {
                hasTask -> {
                    // 변환 완료 - 단계 보기 버튼
                    Button(
                        onClick = onViewSubtasks,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("단계 보기", fontWeight = FontWeight.Medium)
                    }
                }
                recording.status == "ANALYZED" -> {
                    // 분석 완료 - 과제로 변환 버튼
                    Button(
                        onClick = onConvert,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("과제로 변환", fontWeight = FontWeight.Medium)
                    }
                }
                recording.status == "COMPLETED" -> {
                    // 녹화 완료 - 분석 시작 버튼
                    Button(
                        onClick = onAnalyze,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("분석 시작", fontWeight = FontWeight.Medium)
                    }
                }
                recording.status == "PROCESSING" -> {
                    // 처리 중 - 비활성 버튼
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE5E7EB),
                            disabledContainerColor = Color(0xFFE5E7EB)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFF6B7280),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("분석 중...", color = Color(0xFF6B7280), fontWeight = FontWeight.Medium)
                    }
                }
                recording.status == "FAILED" -> {
                    // 실패 - 재시도 버튼
                    Button(
                        onClick = onAnalyze,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("다시 시도", fontWeight = FontWeight.Medium)
                    }
                }
                else -> {
                    // 녹화 중 - 대기
                    Text(
                        text = "녹화 진행 중...",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

/**
 * 상태 배지 컴포넌트
 */
@Composable
fun StatusBadge(status: String, hasTask: Boolean) {
    val (text, bgColor, textColor) = when {
        hasTask -> Triple("변환 완료", Color(0xFFDCFCE7), Color(0xFF166534))
        status == "ANALYZED" -> Triple("분석 완료", Color(0xFFE0E7FF), Color(0xFF3730A3))
        status == "PROCESSING" -> Triple("처리 중", Color(0xFFFEF3C7), Color(0xFF92400E))
        status == "COMPLETED" -> Triple("변환 대기", Color(0xFFF3F4F6), Color(0xFF374151))
        status == "FAILED" -> Triple("실패", Color(0xFFFEE2E2), Color(0xFFDC2626))
        else -> Triple("녹화 중", Color(0xFFFEF3C7), Color(0xFF92400E))
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        )
    }
}
