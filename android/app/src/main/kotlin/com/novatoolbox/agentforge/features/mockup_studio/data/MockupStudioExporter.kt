package com.novatoolbox.agentforge.features.mockup_studio.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

object MockupStudioExporter {

    suspend fun exportMockupToGallery(
        context: Context,
        imageUri: Uri,
        params: MockupParams
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val sourceBitmap = BitmapFactory.decodeStream(inputStream)
                ?: return@withContext Result.failure(Exception("无法解析选取的图片"))

            val dominantColor = MockupRenderer.extractDominantColor(sourceBitmap)

            // 4K 级基准输出画布（基准宽 2800px）
            val canvasW = 2800f
            val canvasH = canvasW * (params.ratio.heightRatio / params.ratio.widthRatio)

            val outputBitmap = Bitmap.createBitmap(canvasW.toInt(), canvasH.toInt(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(outputBitmap)

            // 调用与 UI 预览完全一致的单管线渲染器
            MockupRenderer.render(
                canvas = canvas,
                width = canvasW,
                height = canvasH,
                sourceBitmap = sourceBitmap,
                params = params,
                dominantColor = dominantColor
            )

            // 保存入系统相册
            val filename = "Nova_Mockup_${System.currentTimeMillis()}.png"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/NovaToolBox")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.failure(Exception("无法创建系统媒体库记录"))

            context.contentResolver.openOutputStream(uri)?.use { stream ->
                outputBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            } ?: return@withContext Result.failure(Exception("写入图片数据失败"))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }

            outputBitmap.recycle()
            sourceBitmap.recycle()
            Result.success("Pictures/NovaToolBox/$filename")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
