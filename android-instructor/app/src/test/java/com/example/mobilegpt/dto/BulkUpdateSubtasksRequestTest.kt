package com.example.mobilegpt.dto

import com.example.mobilegpt.data.remote.dto.request.BulkUpdateSubtasksRequest
import com.example.mobilegpt.data.remote.dto.request.SubtaskUpdateItem
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * BulkUpdateSubtasksRequest 직렬화 테스트
 * snake_case 필드명이 올바르게 직렬화되는지 확인
 */
class BulkUpdateSubtasksRequestTest {

    private lateinit var gson: Gson

    @Before
    fun setup() {
        gson = GsonBuilder().create()
    }

    @Test
    fun `SubtaskUpdateItem을 JSON으로 직렬화 시 snake_case 필드명 사용`() {
        // Given
        val item = SubtaskUpdateItem(
            title = "홈 화면 열기",
            description = "홈 버튼을 클릭합니다",
            time = 1731207000L,
            text = "홈",
            contentDescription = "홈버튼",
            viewId = "com.sec.android:id/home_btn",
            bounds = "[0,100][200,150]",
            targetPackage = "com.sec.android.app.launcher",
            targetClass = "Button",
            targetAction = "CLICK",
            uiHint = "홈 버튼",
            guideText = "홈 버튼을 클릭하세요",
            voiceGuideText = "홈 버튼을 클릭하시기 바랍니다"
        )

        // When
        val json = gson.toJson(item)

        // Then: snake_case 필드명 확인
        assertTrue("content_description 필드명이 snake_case여야 함", json.contains("\"content_description\""))
        assertTrue("view_id 필드명이 snake_case여야 함", json.contains("\"view_id\""))
        assertTrue("target_package 필드명이 snake_case여야 함", json.contains("\"target_package\""))
        assertTrue("target_class 필드명이 snake_case여야 함", json.contains("\"target_class\""))
        assertTrue("target_action 필드명이 snake_case여야 함", json.contains("\"target_action\""))
        assertTrue("ui_hint 필드명이 snake_case여야 함", json.contains("\"ui_hint\""))
        assertTrue("guide_text 필드명이 snake_case여야 함", json.contains("\"guide_text\""))
        assertTrue("voice_guide_text 필드명이 snake_case여야 함", json.contains("\"voice_guide_text\""))

        // Then: 값 확인
        assertTrue(json.contains("\"홈버튼\""))
        assertTrue(json.contains("\"com.sec.android:id/home_btn\""))
        assertTrue(json.contains("\"com.sec.android.app.launcher\""))
    }

    @Test
    fun `BulkUpdateSubtasksRequest 직렬화`() {
        // Given
        val request = BulkUpdateSubtasksRequest(
            subtasks = listOf(
                SubtaskUpdateItem(
                    title = "단계 1",
                    description = "첫 번째 단계",
                    time = 1731207000L,
                    text = "버튼1",
                    bounds = "[0,0][100,50]",
                    targetAction = "CLICK"
                ),
                SubtaskUpdateItem(
                    title = "단계 2",
                    description = "두 번째 단계",
                    time = 1731207005L,
                    text = "입력필드",
                    bounds = "[0,50][100,100]",
                    targetAction = "INPUT"
                )
            )
        )

        // When
        val json = gson.toJson(request)

        // Then
        assertTrue(json.contains("\"subtasks\""))
        assertTrue(json.contains("\"단계 1\""))
        assertTrue(json.contains("\"단계 2\""))
        assertTrue(json.contains("\"CLICK\""))
        assertTrue(json.contains("\"INPUT\""))
    }

    @Test
    fun `null 필드는 직렬화에서 제외되거나 null로 표시`() {
        // Given: 필수 필드만 있는 경우
        val item = SubtaskUpdateItem(
            title = "단계 1",
            description = "첫 번째 단계"
        )

        // When
        val json = gson.toJson(item)

        // Then: title과 description은 있어야 함
        assertTrue(json.contains("\"title\""))
        assertTrue(json.contains("\"description\""))
        assertTrue(json.contains("\"단계 1\""))
        assertTrue(json.contains("\"첫 번째 단계\""))
    }

    @Test
    fun `JSON에서 BulkUpdateSubtasksRequest로 역직렬화`() {
        // Given
        val json = """
            {
                "subtasks": [
                    {
                        "title": "단계 1",
                        "description": "첫 번째 단계",
                        "time": 1731207000,
                        "text": "버튼1",
                        "content_description": "버튼 설명",
                        "view_id": "com.example:id/btn1",
                        "bounds": "[0,0][100,50]",
                        "target_package": "com.example.app",
                        "target_class": "Button",
                        "target_action": "CLICK"
                    }
                ]
            }
        """.trimIndent()

        // When
        val request = gson.fromJson(json, BulkUpdateSubtasksRequest::class.java)

        // Then
        assertEquals(1, request.subtasks.size)
        val item = request.subtasks[0]
        assertEquals("단계 1", item.title)
        assertEquals("첫 번째 단계", item.description)
        assertEquals(1731207000L, item.time)
        assertEquals("버튼1", item.text)
        assertEquals("버튼 설명", item.contentDescription)
        assertEquals("com.example:id/btn1", item.viewId)
        assertEquals("[0,0][100,50]", item.bounds)
        assertEquals("com.example.app", item.targetPackage)
        assertEquals("Button", item.targetClass)
        assertEquals("CLICK", item.targetAction)
    }
}
