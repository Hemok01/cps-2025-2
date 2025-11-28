package com.mobilegpt.student.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mobilegpt.student.domain.model.SessionData
import com.mobilegpt.student.domain.model.SessionSummary

/**
 * Session Summary Screen
 * 세션 완료 후 결과 요약 화면
 */
@Composable
fun SessionSummaryScreen(
    sessionData: SessionData,
    summary: SessionSummary,
    onGoHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // 완료 아이콘
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Celebration,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 완료 메시지
        Text(
            text = "강의 완료!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = sessionData.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 통계 카드들
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 소요 시간
                SummaryItem(
                    icon = Icons.Default.Timer,
                    label = "소요 시간",
                    value = formatDuration(summary.durationMinutes),
                    color = MaterialTheme.colorScheme.primary
                )

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                // 완료한 단계
                SummaryItem(
                    icon = Icons.Default.CheckCircle,
                    label = "완료한 단계",
                    value = "${summary.completedSteps} / ${summary.totalSteps}",
                    color = MaterialTheme.colorScheme.tertiary
                )

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                // 기록된 활동
                SummaryItem(
                    icon = Icons.Default.DataUsage,
                    label = "기록된 활동",
                    value = "${summary.eventsLogged}개",
                    color = MaterialTheme.colorScheme.secondary
                )

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                // 도움 요청
                SummaryItem(
                    icon = Icons.Default.Help,
                    label = "도움 요청",
                    value = "${summary.helpRequestCount}회",
                    color = if (summary.helpRequestCount > 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.outline
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 완료율 표시
        CompletionRate(
            completed = summary.completedSteps,
            total = summary.totalSteps
        )

        Spacer(modifier = Modifier.weight(1f))

        // 홈으로 버튼
        Button(
            onClick = onGoHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "홈으로 돌아가기",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * 요약 항목
 */
@Composable
private fun SummaryItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 아이콘
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(12.dp),
            color = color.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = color
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 라벨
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        // 값
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * 완료율 표시
 */
@Composable
private fun CompletionRate(
    completed: Int,
    total: Int
) {
    val rate = if (total > 0) (completed.toFloat() / total * 100).toInt() else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                rate >= 80 -> MaterialTheme.colorScheme.primaryContainer
                rate >= 50 -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "완료율",
                style = MaterialTheme.typography.labelLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$rate%",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = when {
                    rate >= 80 -> MaterialTheme.colorScheme.primary
                    rate >= 50 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when {
                    rate >= 100 -> "완벽해요! 모든 단계를 완료했습니다."
                    rate >= 80 -> "훌륭해요! 거의 다 완료했습니다."
                    rate >= 50 -> "좋아요! 절반 이상 완료했습니다."
                    else -> "다음에는 더 많이 완료해보세요!"
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 시간 포맷팅
 */
private fun formatDuration(minutes: Long): String {
    return when {
        minutes < 1 -> "1분 미만"
        minutes < 60 -> "${minutes}분"
        else -> {
            val hours = minutes / 60
            val mins = minutes % 60
            if (mins == 0L) "${hours}시간" else "${hours}시간 ${mins}분"
        }
    }
}
