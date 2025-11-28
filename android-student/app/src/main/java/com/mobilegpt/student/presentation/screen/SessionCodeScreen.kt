package com.mobilegpt.student.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilegpt.student.presentation.navigation.JoinSessionUiState
import com.mobilegpt.student.presentation.viewmodel.SessionViewModel

/**
 * Session Code Screen
 */
@Composable
fun SessionCodeScreen(
    initialSessionCode: String? = null,
    onJoinSuccess: () -> Unit,
    viewModel: SessionViewModel
) {
    var sessionCode by remember {
        mutableStateOf(initialSessionCode?.uppercase() ?: "NUCP8M")
    }

    val joinUiState by viewModel.joinSessionState.collectAsState()

    // Auto-join if session code is provided via deep link
    LaunchedEffect(initialSessionCode) {
        if (!initialSessionCode.isNullOrBlank() && joinUiState is JoinSessionUiState.Idle) {
            viewModel.joinSession(initialSessionCode.uppercase())
        }
    }

    LaunchedEffect(joinUiState) {
        if (joinUiState is JoinSessionUiState.Success) {
            onJoinSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "세션 참가",
            fontSize = 28.sp,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "강사가 제공한 세션 코드를 입력하세요",
            fontSize = 14.sp,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 세션 코드 입력
        OutlinedTextField(
            value = sessionCode,
            onValueChange = { sessionCode = it.uppercase() },
            label = { Text("세션 코드") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = joinUiState !is JoinSessionUiState.Loading,
            textStyle = LocalTextStyle.current.copy(
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 참가 버튼
        Button(
            onClick = {
                if (sessionCode.isNotBlank()) {
                    viewModel.joinSession(sessionCode)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = joinUiState !is JoinSessionUiState.Loading &&
                    sessionCode.isNotBlank()
        ) {
            if (joinUiState is JoinSessionUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("참가하기", fontSize = 18.sp)
            }
        }

        // 에러 메시지
        if (joinUiState is JoinSessionUiState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = (joinUiState as JoinSessionUiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
