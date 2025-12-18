package com.mobilegpt.student.detector

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.mobilegpt.student.detector.models.UISnapshot

/**
 * AccessibilityNodeInfo 트리를 순회하여 UISnapshot을 생성하는 빌더
 *
 * 사용 예시:
 * ```kotlin
 * val rootNode = rootInActiveWindow
 * val snapshot = UISnapshotBuilder.build(rootNode, packageName)
 * rootNode?.recycle()
 * ```
 */
object UISnapshotBuilder {

    private const val TAG = "UISnapshotBuilder"

    /**
     * 최대 수집할 viewId 개수 (성능 보호)
     */
    private const val MAX_VISIBLE_VIEWS = 500

    /**
     * 최대 수집할 텍스트 노드 개수 (성능 보호)
     */
    private const val MAX_TEXT_NODES = 200

    /**
     * AccessibilityNodeInfo 트리에서 UISnapshot 생성
     *
     * @param rootNode 접근성 트리의 루트 노드
     * @param packageName 현재 포그라운드 앱의 패키지명
     * @return 생성된 UISnapshot
     */
    fun build(rootNode: AccessibilityNodeInfo?, packageName: String): UISnapshot {
        val visibleViews = mutableListOf<String>()
        val textNodes = mutableListOf<String>()
        var activityName: String? = null

        if (rootNode != null) {
            // 트리 순회하여 정보 수집
            traverseNode(rootNode, visibleViews, textNodes)

            // Activity 이름 추출 시도
            activityName = extractActivityName(rootNode)
        }

        Log.d(TAG, "=== Built UISnapshot ===")
        Log.d(TAG, "  Package: $packageName")
        Log.d(TAG, "  Activity: $activityName")
        Log.d(TAG, "  Total views: ${visibleViews.size}")
        Log.d(TAG, "  Total texts: ${textNodes.size}")

        // 처음 5개의 viewId 출력 (디버깅용)
        if (visibleViews.isNotEmpty()) {
            Log.d(TAG, "  Sample viewIds:")
            visibleViews.take(5).forEach { viewId ->
                Log.d(TAG, "    - $viewId")
            }
        }

        // 처음 5개의 텍스트 출력 (디버깅용)
        if (textNodes.isNotEmpty()) {
            Log.d(TAG, "  Sample texts:")
            textNodes.take(5).forEach { text ->
                val truncated = if (text.length > 30) text.take(30) + "..." else text
                Log.d(TAG, "    - \"$truncated\"")
            }
        }

        return UISnapshot(
            packageName = packageName,
            activityName = activityName,
            visibleViews = visibleViews.take(MAX_VISIBLE_VIEWS),
            textNodes = textNodes.take(MAX_TEXT_NODES)
        )
    }

    /**
     * 노드 트리를 재귀적으로 순회하여 viewId와 텍스트 수집
     *
     * @param node 현재 노드
     * @param visibleViews viewId 수집 리스트
     * @param textNodes 텍스트 수집 리스트
     */
    private fun traverseNode(
        node: AccessibilityNodeInfo,
        visibleViews: MutableList<String>,
        textNodes: MutableList<String>
    ) {
        // 성능 보호: 너무 많은 노드 수집 방지
        if (visibleViews.size >= MAX_VISIBLE_VIEWS && textNodes.size >= MAX_TEXT_NODES) {
            return
        }

        try {
            // 1. viewIdResourceName 수집
            node.viewIdResourceName?.let { viewId ->
                if (viewId.isNotBlank() && visibleViews.size < MAX_VISIBLE_VIEWS) {
                    visibleViews.add(viewId)
                }
            }

            // 2. 텍스트 수집 (text)
            node.text?.toString()?.let { text ->
                if (text.isNotBlank() && textNodes.size < MAX_TEXT_NODES) {
                    textNodes.add(text)
                }
            }

            // 3. contentDescription 수집
            node.contentDescription?.toString()?.let { desc ->
                if (desc.isNotBlank() && textNodes.size < MAX_TEXT_NODES) {
                    // text와 다른 경우에만 추가 (중복 방지)
                    if (node.text?.toString() != desc) {
                        textNodes.add(desc)
                    }
                }
            }

            // 4. 자식 노드 순회
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                try {
                    traverseNode(child, visibleViews, textNodes)
                } finally {
                    child.recycle()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error traversing node", e)
        }
    }

    /**
     * 루트 노드에서 Activity 이름 추출 시도
     *
     * Activity 이름은 Window 제목이나 특정 패턴에서 추출할 수 있습니다.
     * 추출 실패 시 null 반환.
     */
    private fun extractActivityName(rootNode: AccessibilityNodeInfo?): String? {
        if (rootNode == null) return null

        return try {
            // Window 제목에서 Activity 이름 추출 시도
            val className = rootNode.className?.toString()
            if (className != null && className.contains("Activity")) {
                className.substringAfterLast(".")
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract activity name", e)
            null
        }
    }

    /**
     * 특정 노드와 그 자식들에서만 스냅샷 생성
     * (부분 스냅샷 생성용)
     *
     * @param node 대상 노드
     * @param packageName 패키지명
     * @return 부분 UISnapshot
     */
    fun buildFromNode(node: AccessibilityNodeInfo?, packageName: String): UISnapshot {
        val visibleViews = mutableListOf<String>()
        val textNodes = mutableListOf<String>()

        if (node != null) {
            traverseNode(node, visibleViews, textNodes)
        }

        return UISnapshot(
            packageName = packageName,
            activityName = null,
            visibleViews = visibleViews,
            textNodes = textNodes
        )
    }

    /**
     * 빠른 패키지 확인용 경량 스냅샷 생성
     * (전체 트리 순회 없이 패키지명만 확인)
     *
     * @param rootNode 루트 노드
     * @param packageName 패키지명
     * @return 경량 UISnapshot (visibleViews, textNodes는 비어있음)
     */
    fun buildLightweight(rootNode: AccessibilityNodeInfo?, packageName: String): UISnapshot {
        return UISnapshot(
            packageName = packageName,
            activityName = extractActivityName(rootNode),
            visibleViews = emptyList(),
            textNodes = emptyList()
        )
    }
}
