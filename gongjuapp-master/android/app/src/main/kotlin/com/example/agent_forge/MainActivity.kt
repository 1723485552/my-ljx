package com.example.agent_forge

import android.Manifest
import android.app.Activity
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.ByteArrayOutputStream

class MainActivity : FlutterFragmentActivity() {
    private val CLIPBOARD_CHANNEL = "com.novatoolbox/clipboard_image"
    private val MEDIA_CHANNEL = "com.novatoolbox/media_cleaner"
    private var pendingDeleteResult: MethodChannel.Result? = null
    private var pendingScreenshotResult: MethodChannel.Result? = null
    private val READ_PERMISSION_REQUEST_CODE = 1001

    // 注册 Android 11+ 系统合规删除确认意图回调
    private val deleteMediaLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingDeleteResult?.success(true)
        } else {
            pendingDeleteResult?.success(false)
        }
        pendingDeleteResult = null
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        // 剪贴板图片读取通道
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CLIPBOARD_CHANNEL)
            .setMethodCallHandler { call, result ->
                if (call.method == "getClipboardImage") {
                    handleGetClipboardImage(result)
                } else {
                    result.notImplemented()
                }
            }

        // 相册截图读取与合规删除通道
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, MEDIA_CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "getLatestScreenshot" -> fetchLatestScreenshot(result)
                    "deleteMediaUri" -> {
                        val uriString = call.argument<String>("uri")
                        if (uriString == null) {
                            result.error("INVALID_URI", "URI 不能为空", null)
                            return@setMethodCallHandler
                        }
                        executeDelete(Uri.parse(uriString), result)
                    }
                    "checkOverlayPermission" -> {
                        result.success(Settings.canDrawOverlays(this))
                    }
                    "requestOverlayPermission" -> {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName"),
                        )
                        startActivity(intent)
                        result.success(true)
                    }
                    "startGlobalWatcher" -> {
                        val intent = Intent(this, ScreenshotWatcherService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(intent)
                        } else {
                            startService(intent)
                        }
                        result.success(true)
                    }
                    "stopGlobalWatcher" -> {
                        val intent = Intent(this, ScreenshotWatcherService::class.java)
                        stopService(intent)
                        result.success(true)
                    }
                    else -> result.notImplemented()
                }
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_PERMISSION_REQUEST_CODE) {
            val pending = pendingScreenshotResult
            pendingScreenshotResult = null
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                // 用户授权后自动重试截图查询
                pending?.let { fetchLatestScreenshot(it) }
            } else {
                pending?.error("PERMISSION_DENIED", "请授予相册读取权限以自动抓取截图", null)
            }
        }
    }

    private fun handleGetClipboardImage(result: MethodChannel.Result) {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (!clipboard.hasPrimaryClip()) {
                result.success(null)
                return
            }

            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val uri: Uri? = clipData.getItemAt(0).uri
                if (uri != null) {
                    val bitmap: Bitmap =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ImageDecoder.decodeBitmap(
                                ImageDecoder.createSource(contentResolver, uri),
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            MediaStore.Images.Media.getBitmap(contentResolver, uri)
                        }

                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    val byteArray = stream.toByteArray()
                    bitmap.recycle()
                    stream.close()
                    result.success(byteArray)
                    return
                }
            }
            result.success(null)
        } catch (e: Exception) {
            result.error("CLIPBOARD_ERROR", e.message, null)
        }
    }

    private fun fetchLatestScreenshot(result: MethodChannel.Result) {
        // 1. 权限自检 (Android 13+ 检查 READ_MEDIA_IMAGES, 低版本检查 READ_EXTERNAL_STORAGE)
        val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, requiredPermission)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // 请求权限，并在 onRequestPermissionsResult 中自动重试
            pendingScreenshotResult = result
            ActivityCompat.requestPermissions(
                this,
                arrayOf(requiredPermission),
                READ_PERMISSION_REQUEST_CODE,
            )
            return
        }

        try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
            )
            // 仅查询图片，按添加时间降序
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameColumn =
                        cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val dateAddedColumn =
                        cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn) // 秒级时间戳

                    // 2. 新鲜度过滤：仅当图片是最近 3 分钟 (180秒) 内创建的才判定为新鲜截图
                    val currentSeconds = System.currentTimeMillis() / 1000
                    val isRecent = (currentSeconds - dateAdded) <= 180

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id,
                    )

                    val bitmap: Bitmap =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ImageDecoder.decodeBitmap(
                                ImageDecoder.createSource(contentResolver, contentUri),
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            MediaStore.Images.Media.getBitmap(contentResolver, contentUri)
                        }

                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 85, stream)
                    val bytes = stream.toByteArray()

                    val responseMap = hashMapOf<String, Any>(
                        "uri" to contentUri.toString(),
                        "name" to name,
                        "bytes" to bytes,
                        "isRecent" to isRecent,
                    )
                    result.success(responseMap)
                    return
                }
            }
            result.success(null)
        } catch (e: Exception) {
            result.error("QUERY_FAILED", e.message, null)
        }
    }

    private fun executeDelete(uri: Uri, result: MethodChannel.Result) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val deleteRequest = MediaStore.createDeleteRequest(
                    contentResolver,
                    listOf(uri),
                )
                pendingDeleteResult = result
                val intentSenderRequest = IntentSenderRequest.Builder(deleteRequest.intentSender)
                    .build()
                deleteMediaLauncher.launch(intentSenderRequest)
            } else {
                val rows = contentResolver.delete(uri, null, null)
                result.success(rows > 0)
            }
        } catch (e: Exception) {
            result.error("DELETE_FAILED", e.message, null)
        }
    }
}
