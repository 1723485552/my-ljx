package com.novatoolbox.agentforge.features.screenshot.data

import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.novatoolbox.agentforge.core.security.MediaUriValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreScreenshotRepository(
    private val contentResolver: ContentResolver,
) : ScreenshotRepository {

    override val latestScreenshotStream: Flow<Uri?> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                trySend(queryLatestScreenshot())
            }
        }

        try {
            contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                observer,
            )
        } catch (ignored: Exception) {
            // 无读取权限时静默
        }

        trySend(queryLatestScreenshot())

        awaitClose {
            try {
                contentResolver.unregisterContentObserver(observer)
            } catch (ignored: Exception) {
                // 观察器未注册时忽略
            }
        }
    }

    override fun queryLatestScreenshot(): Uri? {
        val projection = mutableListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection.add(MediaStore.Images.Media.RELATIVE_PATH)
        }

        val selection = buildString {
            append("(")
            append("${MediaStore.Images.Media.DISPLAY_NAME} LIKE '%screenshot%' OR ")
            append("${MediaStore.Images.Media.DISPLAY_NAME} LIKE '%Screenshot%' OR ")
            append("${MediaStore.Images.Media.DISPLAY_NAME} LIKE '%截屏%' OR ")
            append("${MediaStore.Images.Media.DISPLAY_NAME} LIKE '%截图%'")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                append(" OR ${MediaStore.Images.Media.RELATIVE_PATH} LIKE '%Screenshots%'")
                append(" OR ${MediaStore.Images.Media.RELATIVE_PATH} LIKE '%DCIM/Screenshots%'")
                append(" OR ${MediaStore.Images.Media.RELATIVE_PATH} LIKE '%截屏%'")
            }
            append(")")
        }

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        try {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection.toTypedArray(),
                selection,
                null,
                sortOrder,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(
                        cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID),
                    )
                    return ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id,
                    )
                }
            }
        } catch (e: Exception) {
            return null
        }
        return null
    }

    override fun isImageFresh(uri: Uri, thresholdSeconds: Long): Boolean {
        if (!MediaUriValidator.isValidImageUri(uri)) return false
        val projection = arrayOf(MediaStore.Images.Media.DATE_ADDED)
        try {
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val dateAdded = cursor.getLong(
                        cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED),
                    )
                    val now = System.currentTimeMillis() / 1000L
                    return (now - dateAdded) <= thresholdSeconds.coerceAtLeast(30L)
                }
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }

    override suspend fun deleteScreenshot(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        if (!MediaUriValidator.isValidImageUri(uri)) {
            return@withContext Result.failure(SecurityException("INVALID_URI"))
        }

        try {
            // 先尝试绝对物理路径直接删除（API < 29 可用，>=29 通常为 null，忽略即可）
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val path = cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA),
                    )
                    if (!path.isNullOrEmpty()) {
                        val file = File(path)
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                }
            }

            // 同步从媒体库注销
            contentResolver.delete(uri, null, null)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
