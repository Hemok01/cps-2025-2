package com.example.mobilegpt.viewmodel

import androidx.lifecycle.ViewModel

/**
 * 단계(Subtask) 관리 ViewModel
 */
class SubtaskViewModel : ViewModel() {

    var subtasks: MutableList<MutableMap<String, Any?>> = mutableListOf()

    fun updateSubtask(index: Int, title: String, description: String, text: String) {
        subtasks[index]["title"] = title
        subtasks[index]["description"] = description
        subtasks[index]["text"] = text
    }

    fun deleteSubtask(index: Int) {
        subtasks.removeAt(index)

        // 단계 번호 자동 재정렬 (UI 표시용)
        subtasks.forEachIndexed { i, subtask ->
            subtask["step"] = i + 1
        }
    }
}
