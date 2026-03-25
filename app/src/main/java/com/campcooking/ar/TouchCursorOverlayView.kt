package com.campcooking.ar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

/**
 * 用于演示/讲解时显示“手指触摸位置”的半透明圆圈光标。
 * 只负责绘制，不参与触摸事件处理（触摸事件在 BaseActivity 里统一捕获）。
 */
class TouchCursorOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    // 外圈：更“深”一点，提供强对比和可见性
    private val outerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        // 粉红描边：更贴合演示/投屏观感
        color = Color.argb(230, 139, 0, 0) // DarkRed
        strokeWidth = dp(5.5f)
    }

    // 内圈：亮色描边，让光标在浅色背景上也清晰
    private val innerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(235, 255, 80, 80) // 亮红色描边（提高可见性）
        strokeWidth = dp(2.8f)
    }

    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        // 中心填充改成深红色（你希望中间都是深红色）
        color = Color.argb(210, 139, 0, 0) // DarkRed
    }

    private val cursorRadiusPx = dp(24f)
    private var showCursor = false
    private var cursorX = 0f
    private var cursorY = 0f

    fun showAt(x: Float, y: Float) {
        cursorX = x
        cursorY = y
        if (!showCursor) showCursor = true
        invalidate()
    }

    fun hide() {
        if (showCursor) {
            showCursor = false
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!showCursor) return

        // 先画外圈（深色大描边）
        canvas.drawCircle(cursorX, cursorY, cursorRadiusPx, outerRingPaint)
        // 再画内圈（亮色小描边）
        canvas.drawCircle(cursorX, cursorY, cursorRadiusPx, innerRingPaint)
        // 最后画中心填充（更醒目）
        canvas.drawCircle(cursorX, cursorY, max(2f, cursorRadiusPx * 0.22f), centerPaint)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}

