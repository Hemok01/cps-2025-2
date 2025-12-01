package com.mobilegpt.student.detector

import java.util.concurrent.ConcurrentLinkedDeque

/**
 * 현재 UI 시그니처 상태를 유지하는 싱글톤 홀더
 * 최근 이벤트들의 시그니처를 버퍼로 관리
 */
object CurrentSignatureHolder {

    private const val MAX_BUFFER_SIZE = 10
    private val signatureBuffer = ConcurrentLinkedDeque<SignatureEntry>()

    /**
     * 시그니처 엔트리 (시그니처 + 타임스탬프)
     */
    data class SignatureEntry(
        val signature: Map<String, String?>,
        val timestamp: Long = System.currentTimeMillis(),
        val eventType: Int = 0
    )

    /**
     * 현재 시그니처 업데이트
     */
    fun update(signature: Map<String, String?>, eventType: Int = 0) {
        val entry = SignatureEntry(
            signature = signature,
            timestamp = System.currentTimeMillis(),
            eventType = eventType
        )

        signatureBuffer.addLast(entry)

        // 버퍼 크기 유지
        while (signatureBuffer.size > MAX_BUFFER_SIZE) {
            signatureBuffer.removeFirst()
        }
    }

    /**
     * 현재 (가장 최근) 시그니처 가져오기
     */
    fun getCurrent(): Map<String, String?>? {
        return signatureBuffer.peekLast()?.signature
    }

    /**
     * 현재 엔트리 가져오기
     */
    fun getCurrentEntry(): SignatureEntry? {
        return signatureBuffer.peekLast()
    }

    /**
     * 최근 N개의 시그니처 가져오기
     */
    fun getRecent(count: Int): List<SignatureEntry> {
        return signatureBuffer.toList().takeLast(count)
    }

    /**
     * 특정 이벤트 타입의 최근 시그니처 가져오기
     */
    fun getRecentByEventType(eventType: Int, count: Int = 5): List<SignatureEntry> {
        return signatureBuffer
            .filter { it.eventType == eventType }
            .takeLast(count)
    }

    /**
     * 버퍼 초기화
     */
    fun clear() {
        signatureBuffer.clear()
    }

    /**
     * 현재 버퍼 크기
     */
    fun size(): Int = signatureBuffer.size
}
