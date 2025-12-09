package com.example.mobilegpt.stepdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobilegpt.network.*
import com.example.mobilegpt.viewmodel.StepViewModel
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.*

@Composable
fun StepDetailScreen(
    sessionId: String,
    index: Int,
    viewModel: StepViewModel,
    onBack: () -> Unit
) {
    val step = viewModel.steps[index]

    var title by remember { mutableStateOf(step["title"]?.toString() ?: "") }
    var description by remember { mutableStateOf(step["description"]?.toString() ?: "") }
    var text by remember { mutableStateOf(step["text"]?.toString() ?: "") }

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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Step 수정",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    )
                }
            }

            // 입력 폼
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 제목
                InputField(
                    label = "제목",
                    icon = Icons.Default.Info,
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "제목을 입력하세요",
                    singleLine = true
                )

                // 설명
                InputField(
                    label = "설명",
                    icon = Icons.Default.Create,
                    value = description,
                    onValueChange = { description = it },
                    placeholder = "설명을 입력하세요",
                    minLines = 5
                )

                // 텍스트
                InputField(
                    label = "텍스트",
                    icon = Icons.Default.Edit,
                    value = text,
                    onValueChange = { text = it },
                    placeholder = "추가 텍스트를 입력하세요",
                    minLines = 5
                )

                // 안내 메시지
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF6FF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "변경사항은 저장 버튼을 눌러야 적용됩니다",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF1E40AF)
                            )
                        )
                    }
                }
            }

            // 하단 고정 버튼
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .height(56.dp),
                    onClick = {
                        viewModel.updateStep(index, title, description, text)

                        // 서버 update_step 호출
                        val gson = Gson()
                        val json = gson.toJson(
                            mapOf(
                                "session_id" to sessionId,
                                "step_index" to index,
                                "title" to title,
                                "description" to description,
                                "text" to text
                            )
                        )
                        val body = json.toRequestBody("application/json".toMediaType())

                        ApiClient.api.updateStep(body)
                            .enqueue(object : Callback<BasicResponse> {
                                override fun onResponse(call: Call<BasicResponse>, res: Response<BasicResponse>) { }
                                override fun onFailure(call: Call<BasicResponse>, t: Throwable) { }
                            })

                        onBack()
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
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "저장",
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

@Composable
fun InputField(
    label: String,
    icon: ImageVector,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = false,
    minLines: Int = 1
) {
    Column {
        // 라벨
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF6B7280),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color(0xFF374151),
                    fontWeight = FontWeight.Medium
                )
            )
        }

        // 입력 필드
        var isFocused by remember { mutableStateOf(false) }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = if (isFocused) Color(0xFF2196F3) else Color(0xFFE5E7EB),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textStyle = TextStyle(
                    color = Color(0xFF1F2937),
                    fontSize = 16.sp
                ),
                singleLine = singleLine,
                minLines = if (singleLine) 1 else minLines,
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFF9CA3AF)
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}
