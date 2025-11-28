package com.mobilegpt.student.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mobilegpt.student.data.local.TokenPreferences
import com.mobilegpt.student.util.DeviceIdHelper

/**
 * Register Screen
 * 간편 등록 화면 - 이름만 입력받습니다.
 * 기기 고유값은 자동으로 수집됩니다.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RegisterScreen(
    tokenPreferences: TokenPreferences,
    onRegisterComplete: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var name by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 이름 유효성 검사
    val isNameValid = name.trim().length >= 2
    val canSubmit = isNameValid && !isLoading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        // 아이콘
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 제목
        Text(
            text = "환영합니다!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "강의에 참여하기 위해\n이름을 입력해주세요.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 이름 입력 필드
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                errorMessage = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("이름") },
            placeholder = { Text("홍길동") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    if (canSubmit) {
                        performRegister(
                            context = context,
                            name = name.trim(),
                            tokenPreferences = tokenPreferences,
                            onSuccess = onRegisterComplete,
                            onError = { errorMessage = it },
                            setLoading = { isLoading = it }
                        )
                    }
                }
            ),
            isError = errorMessage != null,
            supportingText = {
                when {
                    errorMessage != null -> Text(errorMessage!!)
                    name.isNotEmpty() && !isNameValid -> Text("이름은 2자 이상 입력해주세요")
                }
            },
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 안내 텍스트
        Text(
            text = "입력한 이름은 강의자에게 표시됩니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.weight(1f))

        // 등록 버튼
        Button(
            onClick = {
                keyboardController?.hide()
                performRegister(
                    context = context,
                    name = name.trim(),
                    tokenPreferences = tokenPreferences,
                    onSuccess = onRegisterComplete,
                    onError = { errorMessage = it },
                    setLoading = { isLoading = it }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = canSubmit,
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "시작하기",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * 등록 처리
 */
private fun performRegister(
    context: android.content.Context,
    name: String,
    tokenPreferences: TokenPreferences,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    setLoading: (Boolean) -> Unit
) {
    if (name.length < 2) {
        onError("이름은 2자 이상 입력해주세요")
        return
    }

    setLoading(true)

    try {
        // 기기 고유값 획득
        val deviceId = DeviceIdHelper.getDeviceId(context)

        // 저장
        tokenPreferences.setSimpleRegister(deviceId, name)

        setLoading(false)
        onSuccess()
    } catch (e: Exception) {
        setLoading(false)
        onError("등록 중 오류가 발생했습니다: ${e.message}")
    }
}
