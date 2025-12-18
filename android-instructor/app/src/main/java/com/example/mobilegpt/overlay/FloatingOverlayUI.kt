package com.example.mobilegpt.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun FloatingOverlayUI(
    isRecording: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Box(
            Modifier
                .size(60.dp)
                .shadow(6.dp, CircleShape)
                .background(if (isRecording) Color.Red else Color.Green, CircleShape)
                .clickable {
                    if (isRecording) onStop() else onStart()
                },
            contentAlignment = Alignment.Center
        ) {
            Text(if (isRecording) "■" else "▶", color = Color.White)
        }
    }
}
