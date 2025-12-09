package com.example.mobilegpt.viewmodel

import androidx.lifecycle.ViewModel

class StepViewModel : ViewModel() {

    var steps: MutableList<MutableMap<String, Any?>> = mutableListOf()

    fun updateStep(index: Int, title: String, description: String, text: String) {
        steps[index]["title"] = title
        steps[index]["description"] = description
        steps[index]["text"] = text
    }

    fun deleteStep(index: Int) {
        steps.removeAt(index)

        // 🔥 step 번호 자동 재정렬 (UI 표시용)
        steps.forEachIndexed { i, step ->
            step["step"] = i + 1
        }
    }
}
