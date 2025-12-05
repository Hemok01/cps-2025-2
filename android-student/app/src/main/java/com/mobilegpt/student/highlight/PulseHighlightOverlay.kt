package com.mobilegpt.student.highlight

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout

/**
 * PulseHighlightOverlay
 * 특정 UI 요소를 강조하기 위한 펄스 애니메이션 오버레이 뷰
 *
 * 펄스 효과:
 * - 바깥 원이 확대되면서 투명해지는 애니메이션
 * - 안쪽 테두리는 고정 상태로 유지
 * - 약 1.5초 주기로 무한 반복
 */
class PulseHighlightOverlay(context: Context) : FrameLayout(context) {

    companion object {
        private const val TAG = "PulseHighlightOverlay"

        // 펄스 애니메이션 설정
        private const val PULSE_DURATION_MS = 1500L
        private const val PULSE_SCALE_START = 1.0f
        private const val PULSE_SCALE_END = 1.8f
        private const val PULSE_ALPHA_START = 0.8f
        private const val PULSE_ALPHA_END = 0.0f

        // 색상 설정
        private const val HIGHLIGHT_COLOR = 0xFFFF6B00.toInt()  // 주황색
        private const val INNER_RING_ALPHA = 255
        private const val OUTER_RING_ALPHA = 200

        // 크기 설정
        private const val RING_STROKE_WIDTH = 6f
        private const val CORNER_RADIUS = 16f
        private const val PADDING = 12
    }

    // 펄스 애니메이션용 외부 뷰
    private val pulseView: PulseRingView
    // 고정 테두리용 내부 뷰
    private val innerRingView: InnerRingView

    private var pulseAnimator: AnimatorSet? = null
    private var targetBounds: Rect? = null

    init {
        // 배경 투명
        setBackgroundColor(Color.TRANSPARENT)

        // 펄스 링 뷰 (애니메이션 대상)
        pulseView = PulseRingView(context)
        addView(pulseView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // 내부 고정 테두리 뷰
        innerRingView = InnerRingView(context)
        addView(innerRingView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    /**
     * 타겟 요소의 좌표 설정
     * WindowManager에서 이 뷰의 위치를 설정할 때 사용
     */
    fun setTargetBounds(bounds: Rect) {
        targetBounds = bounds
        Log.d(TAG, "setTargetBounds: $bounds")
    }

    /**
     * 펄스 애니메이션 시작 (무한 반복)
     */
    fun startPulseAnimation() {
        Log.d(TAG, "startPulseAnimation")
        pulseAnimator?.cancel()

        // 스케일 애니메이션
        val scaleX = ObjectAnimator.ofFloat(pulseView, "scaleX", PULSE_SCALE_START, PULSE_SCALE_END)
        val scaleY = ObjectAnimator.ofFloat(pulseView, "scaleY", PULSE_SCALE_START, PULSE_SCALE_END)

        // 투명도 애니메이션
        val alpha = ObjectAnimator.ofFloat(pulseView, "alpha", PULSE_ALPHA_START, PULSE_ALPHA_END)

        pulseAnimator = AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = PULSE_DURATION_MS
            interpolator = AccelerateDecelerateInterpolator()

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // 초기 상태로 리셋 후 재시작 (무한 반복)
                    pulseView.scaleX = PULSE_SCALE_START
                    pulseView.scaleY = PULSE_SCALE_START
                    pulseView.alpha = PULSE_ALPHA_START
                    animation.start()
                }
            })
        }

        // 초기 상태 설정
        pulseView.scaleX = PULSE_SCALE_START
        pulseView.scaleY = PULSE_SCALE_START
        pulseView.alpha = PULSE_ALPHA_START

        pulseAnimator?.start()
    }

    /**
     * 펄스 애니메이션 중지
     */
    fun stopPulseAnimation() {
        Log.d(TAG, "stopPulseAnimation")
        pulseAnimator?.cancel()
        pulseAnimator = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopPulseAnimation()
    }

    /**
     * 펄스 링 뷰 (확대되면서 사라지는 외부 원)
     */
    private inner class PulseRingView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = RING_STROKE_WIDTH * 2
            color = HIGHLIGHT_COLOR
            alpha = OUTER_RING_ALPHA
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val rectF = RectF(
                RING_STROKE_WIDTH,
                RING_STROKE_WIDTH,
                width - RING_STROKE_WIDTH,
                height - RING_STROKE_WIDTH
            )
            canvas.drawRoundRect(rectF, CORNER_RADIUS, CORNER_RADIUS, paint)
        }
    }

    /**
     * 내부 고정 테두리 뷰
     */
    private inner class InnerRingView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = RING_STROKE_WIDTH
            color = HIGHLIGHT_COLOR
            alpha = INNER_RING_ALPHA
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val rectF = RectF(
                RING_STROKE_WIDTH / 2,
                RING_STROKE_WIDTH / 2,
                width - RING_STROKE_WIDTH / 2,
                height - RING_STROKE_WIDTH / 2
            )
            canvas.drawRoundRect(rectF, CORNER_RADIUS, CORNER_RADIUS, paint)
        }
    }

    /**
     * 권장 오버레이 크기 계산
     * bounds에 padding을 추가한 크기 반환
     */
    fun getRecommendedSize(bounds: Rect): Pair<Int, Int> {
        val width = bounds.width() + PADDING * 2
        val height = bounds.height() + PADDING * 2
        return Pair(width, height)
    }

    /**
     * 권장 오버레이 위치 계산
     * bounds에서 padding을 뺀 위치 반환
     */
    fun getRecommendedPosition(bounds: Rect): Pair<Int, Int> {
        val x = bounds.left - PADDING
        val y = bounds.top - PADDING
        return Pair(x, y)
    }
}
