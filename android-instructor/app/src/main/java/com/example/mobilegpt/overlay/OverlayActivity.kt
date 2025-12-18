package com.example.mobilegpt.overlay

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.material3.MaterialTheme
import com.example.mobilegpt.MyAccessibilityService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OverlayActivity : ComponentActivity() {

    companion object {
        private const val TAG = "OverlayActivity"
    }

    private var isRecording by mutableStateOf(false)
    private var currentRecordingId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFinishOnTouchOutside(false)

        setContent {
            MaterialTheme {
                FloatingOverlayUI(
                    isRecording = isRecording,
                    onStart = {
                        // 녹화 시작 - Django 서버에 녹화 생성 요청
                        val title = "녹화_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}"

                        MyAccessibilityService.startRecordingWithServer(
                            title = title,
                            onSuccess = { recordingId ->
                                currentRecordingId = recordingId
                                isRecording = true
                                Log.i(TAG, "Recording started: ID=$recordingId")
                            },
                            onError = { errorMsg ->
                                Toast.makeText(this@OverlayActivity, "녹화 시작 실패: $errorMsg", Toast.LENGTH_SHORT).show()
                                Log.e(TAG, "Recording start failed: $errorMsg")
                            }
                        )
                    },
                    onStop = {
                        // 녹화 종료 - Django 서버에 녹화 종료 요청
                        MyAccessibilityService.stopRecordingWithServer { recordingId ->
                            Log.i(TAG, "Recording stopped: ID=$recordingId")
                            isRecording = false
                            currentRecordingId = null
                            finish()  // overlay 닫기
                        }
                    }
                )
            }
        }
    }
}
