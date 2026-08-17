package com.novatoolbox.agentforge.features.screenshot.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.novatoolbox.agentforge.core.config.TemporaryMemoryConfig
import com.novatoolbox.agentforge.features.screenshot.data.MediaStoreScreenshotRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 灵动微胶囊控制器（悬浮窗职责拆分模块）。
 *
 * 军规级要点：
 * - 归零自毁安全：协程用 [SupervisorJob] 隔离异常，倒计时跑完在主线程安全移除 UI 后删除。
 * - 手势穿透：FLAG_NOT_FOCUSABLE + FLAG_NOT_TOUCH_MODAL，胶囊外触控 100% 原样穿透，绝不影响屏幕其它区域。
 * - 主线程调度 + 安全 try-catch，杜绝系统安全机制拦截崩溃。
 */
class FloatingOverlayController(private val context: Context) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val repository = MediaStoreScreenshotRepository(context.contentResolver)
    private var overlayView: View? = null
    private var countdownJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private fun dp2px(dp: Float): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics,
        ).toInt()

    @SuppressLint("InflateParams")
    fun showCapsule(uri: Uri) {
        if (!Settings.canDrawOverlays(context)) return

        mainHandler.post {
            // 先清理旧视图，再创建新视图，避免叠加残留
            removeOverlayViewInternal()

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                dp2px(38f),
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = dp2px(42f)
            }

            val capsuleLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp2px(12f), dp2px(4f), dp2px(6f), dp2px(4f))
                background = GradientDrawable().apply {
                    setColor(0xEE121214.toInt())
                    cornerRadius = dp2px(20f).toFloat()
                    setStroke(dp2px(1f), 0xFF38BDF8.toInt())
                }
                elevation = dp2px(8f).toFloat()
            }

            val totalSeconds = TemporaryMemoryConfig.getCountdownDuration(context)
            val tvCountdown = TextView(context).apply {
                text = "自毁 ${totalSeconds}s"
                setTextColor(0xFFF4F4F5.toInt())
                textSize = 12f
                typeface = android.graphics.Typeface.MONOSPACE
                gravity = Gravity.CENTER_VERTICAL
            }

            val btnDelete = Button(context).apply {
                text = "删除"
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 11f
                includeFontPadding = false
                setPadding(dp2px(10f), 0, dp2px(10f), 0)
                background = GradientDrawable().apply {
                    setColor(0xFFEF4444.toInt())
                    cornerRadius = dp2px(14f).toFloat()
                }
                setOnClickListener { performSilentDestruct(uri) }
            }

            val btnKeep = Button(context).apply {
                text = "保留"
                setTextColor(0xFFA1A1AA.toInt())
                textSize = 11f
                includeFontPadding = false
                setPadding(dp2px(10f), 0, dp2px(10f), 0)
                background = GradientDrawable().apply {
                    setColor(0xFF27272A.toInt())
                    cornerRadius = dp2px(14f).toFloat()
                }
                setOnClickListener { dismiss() }
            }

            val deleteParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp2px(28f),
            ).apply { leftMargin = dp2px(10f) }

            val keepParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp2px(28f),
            ).apply { leftMargin = dp2px(6f) }

            capsuleLayout.addView(tvCountdown)
            capsuleLayout.addView(btnDelete, deleteParams)
            capsuleLayout.addView(btnKeep, keepParams)

            overlayView = capsuleLayout

            try {
                windowManager.addView(capsuleLayout, params)
                startCountdown(tvCountdown, uri, totalSeconds)
            } catch (e: Exception) {
                overlayView = null
            }
        }
    }

    private fun performSilentDestruct(uri: Uri) {
        // 先彻底移除 UI，再在后台删除，避免 UI 挂死
        dismiss()
        scope.launch(Dispatchers.IO) {
            repository.deleteScreenshot(uri)
        }
    }

    private fun startCountdown(tv: TextView, uri: Uri, startSeconds: Int) {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            for (i in startSeconds downTo 1) {
                withContext(Dispatchers.Main) {
                    tv.text = "自毁 ${i}s"
                }
                delay(1000L)
            }
            // 倒计时自然跑完，安全调用自毁
            withContext(Dispatchers.Main) {
                performSilentDestruct(uri)
            }
        }
    }

    private fun removeOverlayViewInternal() {
        val view = overlayView ?: return
        overlayView = null
        try {
            if (view.isAttachedToWindow) {
                windowManager.removeViewImmediate(view)
            } else {
                windowManager.removeView(view)
            }
        } catch (ignored: Exception) {
            // 视图已移除时忽略
        }
    }

    fun dismiss() {
        countdownJob?.cancel()
        countdownJob = null
        mainHandler.post {
            removeOverlayViewInternal()
        }
    }

    fun release() {
        dismiss()
        scope.cancel()
    }
}
