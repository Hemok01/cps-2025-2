package com.mobilegpt.student.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mobilegpt.student.domain.model.SubtaskDetail
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session Preferences Helper
 * 세션 정보를 SharedPreferences에 저장/조회
 */
@Singleton
class SessionPreferences @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "mobilegpt_session"
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_SUBTASK_ID = "subtask_id"
        private const val KEY_CURRENT_SUBTASK_DETAIL = "current_subtask_detail"
        private const val KEY_COMPLETED_SUBTASKS = "completed_subtasks"
        private const val KEY_ALL_SUBTASKS = "all_subtasks"
    }

    /**
     * 세션 ID 저장
     */
    fun setSessionId(sessionId: Int) {
        prefs.edit().putInt(KEY_SESSION_ID, sessionId).apply()
    }

    /**
     * 세션 ID 가져오기
     */
    fun getSessionId(): Int? {
        val id = prefs.getInt(KEY_SESSION_ID, -1)
        return if (id == -1) null else id
    }

    /**
     * 서브태스크 ID 저장
     */
    fun setSubtaskId(subtaskId: Int) {
        prefs.edit().putInt(KEY_SUBTASK_ID, subtaskId).apply()
    }

    /**
     * 서브태스크 ID 가져오기
     */
    fun getSubtaskId(): Int? {
        val id = prefs.getInt(KEY_SUBTASK_ID, -1)
        return if (id == -1) null else id
    }

    /**
     * 세션 정보 초기화
     */
    fun clear() {
        prefs.edit().clear().apply()
    }

    // ==================== Subtask Detail Methods ====================

    /**
     * 현재 단계 상세 정보 저장
     */
    fun saveCurrentSubtaskDetail(subtask: SubtaskDetail) {
        val json = gson.toJson(subtask)
        prefs.edit().putString(KEY_CURRENT_SUBTASK_DETAIL, json).apply()
    }

    /**
     * 현재 단계 상세 정보 가져오기
     */
    fun getCurrentSubtaskDetail(): SubtaskDetail? {
        val json = prefs.getString(KEY_CURRENT_SUBTASK_DETAIL, null) ?: return null
        return try {
            gson.fromJson(json, SubtaskDetail::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 모든 단계 목록 저장
     */
    fun saveAllSubtasks(subtasks: List<SubtaskDetail>) {
        val json = gson.toJson(subtasks)
        prefs.edit().putString(KEY_ALL_SUBTASKS, json).apply()
    }

    /**
     * 모든 단계 목록 가져오기
     */
    fun getAllSubtasks(): List<SubtaskDetail> {
        val json = prefs.getString(KEY_ALL_SUBTASKS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<SubtaskDetail>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==================== Completion Status Methods ====================

    /**
     * 단계 완료 상태 저장
     */
    fun saveSubtaskCompletionStatus(subtaskId: Int, isCompleted: Boolean) {
        val completedSet = getCompletedSubtaskIds().toMutableSet()
        if (isCompleted) {
            completedSet.add(subtaskId)
        } else {
            completedSet.remove(subtaskId)
        }
        val json = gson.toJson(completedSet.toList())
        prefs.edit().putString(KEY_COMPLETED_SUBTASKS, json).apply()
    }

    /**
     * 단계 완료 상태 가져오기
     */
    fun getSubtaskCompletionStatus(subtaskId: Int): Boolean {
        return getCompletedSubtaskIds().contains(subtaskId)
    }

    /**
     * 완료된 모든 단계 ID 목록 가져오기
     */
    fun getCompletedSubtaskIds(): Set<Int> {
        val json = prefs.getString(KEY_COMPLETED_SUBTASKS, null) ?: return emptySet()
        return try {
            val type = object : TypeToken<List<Int>>() {}.type
            val list: List<Int> = gson.fromJson(json, type) ?: emptyList()
            list.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    /**
     * 완료 상태 초기화 (세션 시작 시)
     */
    fun clearCompletionStatus() {
        prefs.edit().remove(KEY_COMPLETED_SUBTASKS).apply()
    }
}
