package com.novatoolbox.agentforge.features.screenshot.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.novatoolbox.agentforge.core.config.TemporaryMemoryConfig
import com.novatoolbox.agentforge.features.screenshot.domain.usecase.GetLatestScreenshotUseCase
import com.novatoolbox.agentforge.features.screenshot.ui.FloatingOverlayController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Collections

/**
 * 全局截屏监听与悬浮自毁服务（灵动微胶囊版 + 去重防抖）。
 *
 * - 抓取逻辑委托 [GetLatestScreenshotUseCase]（兼容全品牌截屏识别）。
 * - 悬浮微胶囊委托 [FloatingOverlayController]，胶囊自带用户自定义时长自毁倒计时。
 * - 已处理 URI 去重池：阻断 ContentObserver 删除后二次触发导致的死循环/卡死。
 * - 前台服务：onCreate 必须拉起前台通知，否则 Android 8+ 直接崩溃。
 */
class ScreenshotWatcherService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private lateinit var getLatestScreenshotUseCase: GetLatestScreenshotUseCase
    private lateinit var overlayController: FloatingOverlayController

    // 已处理/已弹窗 URI 缓存池，防止 ContentObserver 变动引起的死循环
    private val handledUris = Collections.synchronizedSet(HashSet<String>())

    override fun onCreate() {
        super.onCreate()
        val repository = MediaStoreScreenshotRepository(contentResolver)
        getLatestScreenshotUseCase = GetLatestScreenshotUseCase(repository)
        overlayController = FloatingOverlayController(this)
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            val threshold = TemporaryMemoryConfig.getCountdownDuration(applicationContext)
                .toLong() + 10L
            getLatestScreenshotUseCase(freshnessThresholdSeconds = threshold)
                .collectLatest { uri: Uri? ->
                    if (uri != null) {
                        val uriKey = uri.toString()
                        // 仅当首次遇到该截屏时弹窗
                        if (!handledUris.contains(uriKey)) {
                            handledUris.add(uriKey)
                            // 控制缓存池大小
                            if (handledUris.size > 50) {
                                handledUris.clear()
                                handledUris.add(uriKey)
                            }
                            overlayController.showCapsule(uri)
                        }
                    }
                }
        }
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
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("NovaToolBox 截屏守护中")
            .setContentText("已开启全局截屏自毁监听")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()
        startForeground(1001, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        overlayController.dismiss()
        overlayController.release()
        handledUris.clear()
    }

    override fun onBind(intent: Intent?): android.os.IBinder? = null
}
