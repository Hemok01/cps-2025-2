package com.example.mobilegpt.api

import com.example.mobilegpt.data.remote.api.RecordingApiService
import com.example.mobilegpt.data.remote.dto.request.BulkUpdateSubtasksRequest
import com.example.mobilegpt.data.remote.dto.request.SubtaskUpdateItem
import com.example.mobilegpt.data.remote.dto.response.BulkUpdateResponse
import com.example.mobilegpt.data.remote.dto.response.SubtaskResponse
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * RecordingApiService API 테스트
 * MockWebServer를 사용하여 실제 API 호출 시뮬레이션
 */
class RecordingApiServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: RecordingApiService

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val gson = GsonBuilder().create()
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        apiService = retrofit.create(RecordingApiService::class.java)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `bulkUpdateSubtasks API 호출 성공`() = runBlocking {
        // Given: 서버 응답 설정
        val responseJson = """
            {
                "status": "updated",
                "count": 2
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseJson)
                .addHeader("Content-Type", "application/json")
        )

        // When: API 호출
        val request = BulkUpdateSubtasksRequest(
            subtasks = listOf(
                SubtaskUpdateItem(
                    title = "단계 1",
                    description = "첫 번째 단계",
                    time = 1731207000L,
                    text = "버튼1",
                    targetAction = "CLICK"
                ),
                SubtaskUpdateItem(
                    title = "단계 2",
                    description = "두 번째 단계",
                    time = 1731207005L,
                    text = "입력필드",
                    targetAction = "INPUT"
                )
            )
        )

        val response = apiService.bulkUpdateSubtasks(taskId = 123L, request = request)

        // Then: 응답 확인
        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals("updated", body?.status)
        assertEquals(2, body?.count)

        // Then: 요청 검증
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("PUT", recordedRequest.method)
        assertEquals("/api/tasks/123/subtasks/bulk/", recordedRequest.path)
        assertTrue(recordedRequest.body.readUtf8().contains("\"subtasks\""))
    }

    @Test
    fun `getSubtasksByRecording API 응답에 새 필드 포함`() = runBlocking {
        // Given: 새 필드가 포함된 서버 응답
        val responseJson = """
            [
                {
                    "id": 1,
                    "step": 1,
                    "title": "홈 화면 열기",
                    "description": "홈 버튼을 클릭합니다",
                    "order_index": 0,
                    "target_action": "CLICK",
                    "target_package": "com.sec.android.app.launcher",
                    "target_class": "Button",
                    "ui_hint": "홈 버튼",
                    "guide_text": "홈 버튼을 클릭하세요",
                    "voice_guide_text": "홈 버튼을 클릭하시기 바랍니다",
                    "time": 1731207000,
                    "text": "홈",
                    "content_description": "홈버튼",
                    "view_id": "com.sec.android:id/home_btn",
                    "bounds": "[0,100][200,150]"
                }
            ]
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseJson)
                .addHeader("Content-Type", "application/json")
        )

        // When
        val response = apiService.getSubtasksByRecording(recordingId = 456L)

        // Then
        assertTrue(response.isSuccessful)
        val subtasks = response.body()
        assertNotNull(subtasks)
        assertEquals(1, subtasks?.size)

        val subtask = subtasks?.get(0)
        assertNotNull(subtask)

        // 기존 필드 확인
        assertEquals("홈 화면 열기", subtask?.title)
        assertEquals("CLICK", subtask?.targetAction)

        // 새 필드 확인
        assertEquals(1731207000L, subtask?.time)
        assertEquals("홈", subtask?.text)
        assertEquals("홈버튼", subtask?.contentDescription)
        assertEquals("com.sec.android:id/home_btn", subtask?.viewId)
        assertEquals("[0,100][200,150]", subtask?.bounds)

        // 요청 경로 확인
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("GET", recordedRequest.method)
        assertEquals("/api/sessions/recordings/456/subtasks/", recordedRequest.path)
    }

    @Test
    fun `bulkUpdateSubtasks 요청 본문에 snake_case 필드명 사용`() = runBlocking {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"status": "updated", "count": 1}""")
                .addHeader("Content-Type", "application/json")
        )

        val request = BulkUpdateSubtasksRequest(
            subtasks = listOf(
                SubtaskUpdateItem(
                    title = "테스트",
                    description = "테스트 설명",
                    contentDescription = "접근성 설명",
                    viewId = "com.example:id/test",
                    targetPackage = "com.example.app",
                    targetClass = "Button",
                    targetAction = "CLICK"
                )
            )
        )

        // When
        apiService.bulkUpdateSubtasks(taskId = 1L, request = request)

        // Then: 요청 본문 검증
        val recordedRequest = mockWebServer.takeRequest()
        val requestBody = recordedRequest.body.readUtf8()

        assertTrue("content_description이 snake_case여야 함", requestBody.contains("\"content_description\""))
        assertTrue("view_id가 snake_case여야 함", requestBody.contains("\"view_id\""))
        assertTrue("target_package가 snake_case여야 함", requestBody.contains("\"target_package\""))
        assertTrue("target_class가 snake_case여야 함", requestBody.contains("\"target_class\""))
        assertTrue("target_action이 snake_case여야 함", requestBody.contains("\"target_action\""))
    }

    @Test
    fun `API 에러 응답 처리`() = runBlocking {
        // Given: 404 에러 응답
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("""{"error": "Task not found"}""")
                .addHeader("Content-Type", "application/json")
        )

        // When
        val request = BulkUpdateSubtasksRequest(subtasks = emptyList())
        val response = apiService.bulkUpdateSubtasks(taskId = 999L, request = request)

        // Then
        assertFalse(response.isSuccessful)
        assertEquals(404, response.code())
    }
}
