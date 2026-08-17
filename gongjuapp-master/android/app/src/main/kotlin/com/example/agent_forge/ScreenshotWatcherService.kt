package com.example.agent_forge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat

class ScreenshotWatcherService : Service() {
    private lateinit var windowManager: WindowManager
    private var floatingView: android.view.View? = null
    private var countdownTimer: CountDownTimer? = null
    private var contentObserver: ContentObserver? = null
    private var lastHandledId: Long = -1

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundNotification()
        registerScreenshotObserver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 被系统回收后自动重启，保持全局监听常驻
        return START_STICKY
    }

    private fun startForegroundNotification() {
        val channelId = "screenshot_watcher_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "截屏自动清理服务",
                NotificationManager.IMPORTANCE_LOW,
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("NovaToolBox 截屏守护中")
            .setContentText("已开启全局截屏自毁监听")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()

        startForeground(1001, notification)
    }

    private fun registerScreenshotObserver() {
        contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                checkLatestScreenshot()
            }
        }

        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver!!,
        )
    }

    private fun checkLatestScreenshot() {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID),
                )
                val dateAdded = cursor.getLong(
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED),
                )
                val currentSec = System.currentTimeMillis() / 1000

                // 仅响应 10 秒内产生的新截屏且不重复触发
                if (id != lastHandledId && (currentSec - dateAdded) <= 10) {
                    lastHandledId = id
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id,
                    )
                    showGlobalFloatingCapsule(contentUri)
                }
            }
        }
    }

    private fun showGlobalFloatingCapsule(uri: Uri) {
        if (!Settings.canDrawOverlays(this)) return

        Handler(Looper.getMainLooper()).post {
            removeFloatingView()

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = 120
            }

            val container = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(36, 20, 36, 20)
                setBackgroundColor(Color.parseColor("#EE1E1E1E"))
                elevation = 16f
            }

            val textInfo = TextView(this).apply {
                setTextColor(Color.WHITE)
                textSize = 13f
                text = "📸 刚截屏 · 30s 自毁"
            }

            val btnDelete = Button(this).apply {
                text = "立即删除"
                textSize = 12f
                setTextColor(Color.parseColor("#FF5252"))
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener {
                    removeFloatingView()
                    TransparentDeleteActivity.launch(this@ScreenshotWatcherService, uri)
                }
            }

            val btnKeep = Button(this).apply {
                text = "保留"
                textSize = 12f
                setTextColor(Color.parseColor("#9E9E9E"))
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener {
                    removeFloatingView()
                }
            }

            container.addView(textInfo)
            container.addView(btnDelete)
            container.addView(btnKeep)

            floatingView = container
            windowManager.addView(floatingView, params)

            // 启动 30 秒自毁倒计时
            countdownTimer?.cancel()
            countdownTimer = object : CountDownTimer(30000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    textInfo.text = "📸 刚截屏 · ${millisUntilFinished / 1000}s 自毁"
                }

                override fun onFinish() {
                    removeFloatingView()
                    TransparentDeleteActivity.launch(this@ScreenshotWatcherService, uri)
                }
            }.start()
        }
    }

    private fun removeFloatingView() {
        countdownTimer?.cancel()
        countdownTimer = null
        if (floatingView != null) {
            windowManager.removeView(floatingView)
            floatingView = null
        }
    }

    override fun onDestroy() {
        removeFloatingView()
        contentObserver?.let { contentResolver.unregisterContentObserver(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
