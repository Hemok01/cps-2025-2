package com.example.mobilegpt.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun RecordingOverlayUI(
    isRecording: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .wrapContentSize()
            .padding(end = 24.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.End
    ) {

        AnimatedVisibility(visible = expanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                OverlayButtonItem(
                    label = "녹화 종료",
                    icon = Icons.Default.Stop,
                    background = Color(0xFFFFE5E5)
                ) {
                    expanded = false
                    onStop()
                }
            }
        }

        FloatingActionButton(
            modifier = Modifier.size(70.dp),
            containerColor =
                if (isRecording) Color(0xFFEF4444) else Color(0xFF4ADE80),
            onClick = {
                if (!isRecording) {
                    expanded = true
                    onStart()
                } else {
                    expanded = !expanded
                }
            }
        ) {
            Icon(
                imageVector =
                    if (isRecording) Icons.Default.FiberManualRecord
                    else Icons.Default.Edit,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun OverlayButtonItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = background,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(10.dp))
            Icon(icon, contentDescription = null)
        }
    }
}
