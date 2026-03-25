package com.campcooking.ar

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity

/**
 * 学生端通用基类：在 Activity 顶层叠加一个透明圆圈光标，
 * 并在按住/滑动触摸时持续跟随手指显示。
 *
 * 触摸监听放在 Activity.dispatchTouchEvent 里，避免改动各页面业务代码。
 */
open class TouchCursorBaseActivity : AppCompatActivity() {

    private var overlayView: TouchCursorOverlayView? = null
    private var activePointerId: Int = MotionEvent.INVALID_POINTER_ID
    private val tmpLocationOnScreen = IntArray(2)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // overlayView 会在 setContentView 后确保添加
    }

    override fun setContentView(view: View) {
        super.setContentView(view)
        ensureOverlay()
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        ensureOverlay()
    }

    private fun ensureOverlay() {
        if (overlayView != null) return

        val content = findViewById<ViewGroup>(android.R.id.content) ?: return
        overlayView = TouchCursorOverlayView(this).apply {
            // 禁用该 View 的触摸命中，避免影响原页面交互。
            isEnabled = false
            isClickable = false
            isFocusable = false
            isLongClickable = false
            // 不拦截触摸；真正的光标位置由 dispatchTouchEvent 计算并驱动 show/hide。
            setOnTouchListener { _, _ -> false }
        }
        content.addView(
            overlayView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        overlayView?.hide()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        ensureOverlay()
        val overlay = overlayView

        if (overlay != null) {
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    activePointerId = ev.getPointerId(ev.actionIndex)
                    updateCursorFromEvent(ev)
                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    // 选择当前按下的那根手指作为“跟随指针”
                    activePointerId = ev.getPointerId(ev.actionIndex)
                    updateCursorFromEvent(ev)
                }

                MotionEvent.ACTION_MOVE -> {
                    updateCursorFromEvent(ev)
                }

                MotionEvent.ACTION_UP -> {
                    activePointerId = MotionEvent.INVALID_POINTER_ID
                    overlay.hide()
                }

                MotionEvent.ACTION_POINTER_UP -> {
                    val liftedPointerId = ev.getPointerId(ev.actionIndex)
                    if (liftedPointerId == activePointerId) {
                        // 还有其他手指，则切换到另一根继续跟随；否则隐藏。
                        if (ev.pointerCount > 1) {
                            val newIndex = if (ev.actionIndex == 0) 1 else 0
                            activePointerId = ev.getPointerId(newIndex)
                            updateCursorFromEvent(ev)
                        } else {
                            activePointerId = MotionEvent.INVALID_POINTER_ID
                            overlay.hide()
                        }
                    }
                }

                MotionEvent.ACTION_CANCEL -> {
                    activePointerId = MotionEvent.INVALID_POINTER_ID
                    overlay.hide()
                }
            }
        }

        return super.dispatchTouchEvent(ev)
    }

    private fun updateCursorFromEvent(ev: MotionEvent) {
        val overlay = overlayView ?: return

        val pointerIndex = if (activePointerId != MotionEvent.INVALID_POINTER_ID) {
            ev.findPointerIndex(activePointerId)
        } else {
            ev.actionIndex
        }

        if (pointerIndex < 0 || pointerIndex >= ev.pointerCount) return

        overlay.getLocationOnScreen(tmpLocationOnScreen)
        val localX = ev.getRawX(pointerIndex) - tmpLocationOnScreen[0]
        val localY = ev.getRawY(pointerIndex) - tmpLocationOnScreen[1]
        overlay.showAt(localX, localY)
    }
}

