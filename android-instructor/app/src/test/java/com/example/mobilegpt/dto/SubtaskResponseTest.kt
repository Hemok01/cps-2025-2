package com.example.mobilegpt.dto

import com.example.mobilegpt.data.remote.dto.response.SubtaskResponse
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * SubtaskResponse 역직렬화 테스트
 * 백엔드에서 추가된 7개 필드가 정상적으로 파싱되는지 확인
 */
class SubtaskResponseTest {

    private lateinit var gson: Gson

    @Before
    fun setup() {
        gson = GsonBuilder().create()
    }

    @Test
    fun `백엔드 JSON을 SubtaskResponse로 역직렬화`() {
        // Given: 백엔드에서 반환하는 JSON (새 필드 포함)
        val json = """
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
        """.trimIndent()

        // When
        val response = gson.fromJson(json, SubtaskResponse::class.java)

        // Then: 기존 필드 확인
        assertEquals(1L, response.id)
        assertEquals(1, response.step)
        assertEquals("홈 화면 열기", response.title)
        assertEquals("홈 버튼을 클릭합니다", response.description)
        assertEquals(0, response.orderIndex)
        assertEquals("CLICK", response.targetAction)
        assertEquals("com.sec.android.app.launcher", response.targetPackage)
        assertEquals("Button", response.targetClass)
        assertEquals("홈 버튼", response.uiHint)
        assertEquals("홈 버튼을 클릭하세요", response.guideText)
        assertEquals("홈 버튼을 클릭하시기 바랍니다", response.voiceGuideText)

        // Then: 새로 추가된 필드 확인 (5개)
        assertEquals(1731207000L, response.time)
        assertEquals("홈", response.text)
        assertEquals("홈버튼", response.contentDescription)
        assertEquals("com.sec.android:id/home_btn", response.viewId)
        assertEquals("[0,100][200,150]", response.bounds)
    }

    @Test
    fun `새 필드가 null인 경우 정상 파싱`() {
        // Given: 새 필드가 없는 JSON (하위 호환성 테스트)
        val json = """
            {
                "id": 2,
                "step": 2,
                "title": "검색창 열기",
                "description": "검색 버튼을 클릭합니다",
                "order_index": 1,
                "target_action": "CLICK",
                "target_package": "com.google.android.youtube",
                "target_class": "ImageView",
                "ui_hint": null,
                "guide_text": null,
                "voice_guide_text": null
            }
        """.trimIndent()

        // When
        val response = gson.fromJson(json, SubtaskResponse::class.java)

        // Then: 기존 필드 확인
        assertEquals(2L, response.id)
        assertEquals(2, response.step)
        assertEquals("검색창 열기", response.title)

        // Then: 새 필드는 null
        assertNull(response.time)
        assertNull(response.text)
        assertNull(response.contentDescription)
        assertNull(response.viewId)
        assertNull(response.bounds)
    }

    @Test
    fun `SubtaskResponse 목록 역직렬화`() {
        // Given
        val json = """
            [
                {
                    "id": 1,
                    "step": 1,
                    "title": "단계 1",
                    "description": "첫 번째 단계",
                    "order_index": 0,
                    "target_action": "CLICK",
                    "time": 1731207000,
                    "text": "버튼1",
                    "bounds": "[0,0][100,50]"
                },
                {
                    "id": 2,
                    "step": 2,
                    "title": "단계 2",
                    "description": "두 번째 단계",
                    "order_index": 1,
                    "target_action": "INPUT",
                    "time": 1731207005,
                    "text": "입력필드",
                    "bounds": "[0,50][100,100]"
                }
            ]
        """.trimIndent()

        // When
        val responses = gson.fromJson(json, Array<SubtaskResponse>::class.java)

        // Then
        assertEquals(2, responses.size)
        assertEquals("단계 1", responses[0].title)
        assertEquals("버튼1", responses[0].text)
        assertEquals("단계 2", responses[1].title)
        assertEquals("입력필드", responses[1].text)
    }
}
