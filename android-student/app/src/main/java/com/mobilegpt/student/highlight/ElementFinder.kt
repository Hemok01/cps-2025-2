package com.mobilegpt.student.highlight

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

/**
 * ElementFinder
 * AccessibilityService의 UI 트리에서 특정 요소를 찾는 유틸리티
 *
 * 주요 기능:
 * - viewId로 노드 검색
 * - 텍스트로 노드 검색 (폴백)
 * - 노드의 화면 좌표(bounds) 추출
 */
object ElementFinder {

    private const val TAG = "ElementFinder"

    /**
     * 요소 검색 결과
     */
    data class FindResult(
        val found: Boolean,
        val bounds: Rect? = null,
        val viewId: String? = null,
        val text: String? = null
    )

    /**
     * viewId로 요소를 찾아 화면 좌표 반환
     *
     * @param rootNode UI 트리의 루트 노드 (rootInActiveWindow)
     * @param targetViewId 찾을 viewId (예: "btn_settings" 또는 "com.example:id/btn_settings")
     * @return 검색 결과 (좌표 포함)
     */
    fun findByViewId(rootNode: AccessibilityNodeInfo, targetViewId: String): FindResult {
        Log.d(TAG, "findByViewId: Searching for '$targetViewId'")

        // 1. 전체 viewId 형식으로 검색 시도
        val fullViewId = if (targetViewId.contains(":id/")) {
            targetViewId
        } else {
            // 짧은 ID인 경우 현재 패키지명 추가
            val packageName = rootNode.packageName?.toString()
            if (packageName != null) {
                "$packageName:id/$targetViewId"
            } else {
                targetViewId
            }
        }

        Log.d(TAG, "findByViewId: Full viewId = '$fullViewId'")

        // viewId로 검색
        val nodesByViewId = rootNode.findAccessibilityNodeInfosByViewId(fullViewId)
        if (nodesByViewId.isNotEmpty()) {
            // 화면에 보이는 첫 번째 노드 선택
            val visibleNode = nodesByViewId.firstOrNull { it.isVisibleToUser }
            if (visibleNode != null) {
                val bounds = extractBounds(visibleNode)
                val result = FindResult(
                    found = true,
                    bounds = bounds,
                    viewId = visibleNode.viewIdResourceName,
                    text = getNodeText(visibleNode)
                )
                Log.d(TAG, "findByViewId: Found! bounds=$bounds")

                // 노드 재활용
                nodesByViewId.forEach { it.recycle() }
                return result
            }
            nodesByViewId.forEach { it.recycle() }
        }

        // 2. 짧은 viewId로 재검색 (패키지명 없이)
        if (!targetViewId.contains(":id/")) {
            val nodesShortId = searchByShortViewId(rootNode, targetViewId)
            if (nodesShortId != null) {
                val bounds = extractBounds(nodesShortId)
                val result = FindResult(
                    found = true,
                    bounds = bounds,
                    viewId = nodesShortId.viewIdResourceName,
                    text = getNodeText(nodesShortId)
                )
                Log.d(TAG, "findByViewId: Found by short ID! bounds=$bounds")
                nodesShortId.recycle()
                return result
            }
        }

        Log.d(TAG, "findByViewId: Not found by viewId, trying text search...")

        // 3. 텍스트로 검색 (폴백)
        val nodesByText = rootNode.findAccessibilityNodeInfosByText(targetViewId)
        if (nodesByText.isNotEmpty()) {
            val visibleNode = nodesByText.firstOrNull { it.isVisibleToUser && it.isClickable }
            if (visibleNode != null) {
                val bounds = extractBounds(visibleNode)
                val result = FindResult(
                    found = true,
                    bounds = bounds,
                    viewId = visibleNode.viewIdResourceName,
                    text = getNodeText(visibleNode)
                )
                Log.d(TAG, "findByViewId: Found by text! bounds=$bounds")
                nodesByText.forEach { it.recycle() }
                return result
            }
            nodesByText.forEach { it.recycle() }
        }

        Log.d(TAG, "findByViewId: Element not found")
        return FindResult(found = false)
    }

    /**
     * 짧은 viewId로 전체 트리 검색 (재귀)
     * 예: "btn_settings"를 찾으면 "com.example:id/btn_settings"와 매칭
     */
    private fun searchByShortViewId(node: AccessibilityNodeInfo, shortId: String): AccessibilityNodeInfo? {
        val nodeViewId = node.viewIdResourceName
        if (nodeViewId != null && nodeViewId.endsWith(":id/$shortId")) {
            if (node.isVisibleToUser) {
                return node
            }
        }

        // 자식 노드 탐색
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = searchByShortViewId(child, shortId)
            if (result != null) {
                if (result != child) {
                    child.recycle()
                }
                return result
            }
            child.recycle()
        }

        return null
    }

    /**
     * 노드의 화면 좌표 추출
     */
    fun extractBounds(node: AccessibilityNodeInfo): Rect {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return rect
    }

    /**
     * 노드의 텍스트 추출 (text 또는 contentDescription)
     */
    private fun getNodeText(node: AccessibilityNodeInfo): String? {
        return when {
            !node.text.isNullOrEmpty() -> node.text.toString()
            !node.contentDescription.isNullOrEmpty() -> node.contentDescription.toString()
            else -> null
        }
    }

    /**
     * 현재 포커스된 앱의 패키지명 가져오기
     */
    fun getActivePackageName(rootNode: AccessibilityNodeInfo): String? {
        return rootNode.packageName?.toString()
    }
}
