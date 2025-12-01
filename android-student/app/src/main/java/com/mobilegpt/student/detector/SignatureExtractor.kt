package com.mobilegpt.student.detector

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * AccessibilityEvent에서 UI 시그니처를 추출하는 유틸리티
 * GitHub UI_comparison 프로젝트의 SignatureExtractor 기반
 */
object SignatureExtractor {

    /**
     * AccessibilityEvent로부터 UI 시그니처 맵 추출
     *
     * @param event AccessibilityEvent
     * @return 시그니처 맵 (package, className, text, contentDescription, viewId, eventType)
     */
    fun fromEvent(event: AccessibilityEvent): Map<String, String?> {
        val sourceNode = event.source

        val signature = mutableMapOf<String, String?>(
            "package" to event.packageName?.toString(),
            "className" to event.className?.toString(),
            "text" to event.text?.joinToString(" "),
            "contentDescription" to sourceNode?.contentDescription?.toString(),
            "viewId" to sourceNode?.viewIdResourceName,
            "eventType" to event.eventType.toString()
        )

        // 노드에서 추가 정보 추출
        sourceNode?.let { node ->
            signature["nodeClassName"] = node.className?.toString()
            signature["isClickable"] = node.isClickable.toString()
            signature["isEditable"] = node.isEditable.toString()
            signature["isCheckable"] = node.isCheckable.toString()
            signature["isChecked"] = node.isChecked.toString()

            // 부모 노드 정보
            node.parent?.let { parent ->
                signature["parentClassName"] = parent.className?.toString()
                signature["parentViewId"] = parent.viewIdResourceName
                parent.recycle()
            }
        }

        sourceNode?.recycle()
        return signature
    }

    /**
     * AccessibilityNodeInfo로부터 UI 시그니처 맵 추출
     *
     * @param node AccessibilityNodeInfo
     * @param packageName 패키지명
     * @return 시그니처 맵
     */
    fun fromNode(node: AccessibilityNodeInfo?, packageName: String?): Map<String, String?> {
        if (node == null) {
            return mapOf(
                "package" to packageName,
                "className" to null,
                "text" to null,
                "contentDescription" to null,
                "viewId" to null
            )
        }

        return mapOf(
            "package" to packageName,
            "className" to node.className?.toString(),
            "text" to node.text?.toString(),
            "contentDescription" to node.contentDescription?.toString(),
            "viewId" to node.viewIdResourceName,
            "isClickable" to node.isClickable.toString(),
            "isEditable" to node.isEditable.toString()
        )
    }
}
