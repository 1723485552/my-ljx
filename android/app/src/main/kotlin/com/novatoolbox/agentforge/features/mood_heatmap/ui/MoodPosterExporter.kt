package com.novatoolbox.agentforge.features.mood_heatmap.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

object MoodPosterExporter {

    /**
     * 生成当前月份/全年的高清心绪海报并保存到系统相册
     */
    suspend fun exportPosterToGallery(
        context: Context,
        yearMonth: YearMonth,
        records: Map<LocalDate, DayMoodRecord>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val width = 1080
            val height = 1560
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 1. 全局背景与边框
            canvas.drawColor(android.graphics.Color.parseColor("#09090B"))

            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#18181B")
                style = Paint.Style.FILL
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#27272A")
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }

            // 绘制主内容卡片
            val mainCardRect = RectF(60f, 60f, width - 60f, height - 60f)
            canvas.drawRoundRect(mainCardRect, 32f, 32f, cardPaint)
            canvas.drawRoundRect(mainCardRect, 32f, 32f, borderPaint)

            // 2. 头部文字
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#F4F4F5")
                textSize = 52f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("NovaToolBox · 心绪方阵", 110f, 160f, titlePaint)

            val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#A1A1AA")
                textSize = 28f
            }
            val loggedDays = (1..yearMonth.lengthOfMonth()).count { day ->
                val d = yearMonth.atDay(day)
                records[d]?.mood != null && records[d]?.mood != MoodLevel.EMPTY
            }
            val monthStr = yearMonth.format(DateTimeFormatter.ofPattern("yyyy年 MM月"))
            canvas.drawText("$monthStr · 已点亮 $loggedDays 天", 110f, 215f, subTitlePaint)

            // 3. 图例绘制
            var legendX = 110f
            val legendY = 285f
            val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 24f }
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

            MoodLevel.values().filter { it != MoodLevel.EMPTY }.forEach { level ->
                dotPaint.color = level.color.toArgb()
                canvas.drawCircle(legendX, legendY - 8f, 10f, dotPaint)
                legendPaint.color = android.graphics.Color.parseColor("#A1A1AA")
                canvas.drawText(level.label, legendX + 20f, legendY, legendPaint)
                legendX += 170f
            }

            // 4. 星期表头
            val weekDays = listOf("一", "二", "三", "四", "五", "六", "日")
            val gridLeft = 110f
            val gridTop = 360f
            val gridWidth = (width - 220f)
            val cellSize = (gridWidth - (6 * 16f)) / 7f

            val weekHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#71717A")
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            weekDays.forEachIndexed { index, label ->
                val cx = gridLeft + index * (cellSize + 16f) + cellSize / 2f
                canvas.drawText(label, cx, gridTop, weekHeaderPaint)
            }

            // 5. 月历方格矩阵绘制
            val firstDayOfMonth = yearMonth.atDay(1)
            val daysInMonth = yearMonth.lengthOfMonth()
            val startDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1..7
            val totalCells = ((daysInMonth + startDayOfWeek - 1 + 6) / 7) * 7

            val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
            val cellBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            val dateTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 28f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val cellsStartTop = gridTop + 30f

            for (row in 0 until (totalCells / 7)) {
                for (col in 1..7) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - startDayOfWeek + 1

                    val left = gridLeft + (col - 1) * (cellSize + 16f)
                    val top = cellsStartTop + row * (cellSize + 16f)
                    val rect = RectF(left, top, left + cellSize, top + cellSize)

                    if (dayNumber in 1..daysInMonth) {
                        val cellDate = yearMonth.atDay(dayNumber)
                        val record = records[cellDate]
                        val hasMood = record != null && record.mood != MoodLevel.EMPTY
                        val cellMood = record?.mood ?: MoodLevel.EMPTY

                        // 填充背景
                        cellPaint.color = if (hasMood) {
                            adjustAlpha(cellMood.color.toArgb(), 0.35f)
                        } else {
                            android.graphics.Color.parseColor("#141416")
                        }
                        canvas.drawRoundRect(rect, 14f, 14f, cellPaint)

                        // 边框
                        cellBorderPaint.color = if (hasMood) {
                            cellMood.color.toArgb()
                        } else {
                            android.graphics.Color.parseColor("#27272A")
                        }
                        canvas.drawRoundRect(rect, 14f, 14f, cellBorderPaint)

                        // 日期文字
                        dateTextPaint.color = if (hasMood) {
                            cellMood.color.toArgb()
                        } else {
                            android.graphics.Color.parseColor("#A1A1AA")
                        }
                        val fontMetrics = dateTextPaint.fontMetrics
                        val textY = rect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2f
                        canvas.drawText(dayNumber.toString(), rect.centerX(), textY, dateTextPaint)
                    }
                }
            }

            // 6. 底部品牌与生成时间水印
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#71717A")
                textSize = 22f
            }
            val nowStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            canvas.drawText("Generated by NovaToolBox · $nowStr", 110f, height - 110f, footerPaint)

            // 7. 保存到 MediaStore 相册
            val filename = "Nova_Mood_${yearMonth.format(DateTimeFormatter.ofPattern("yyyyMM"))}_${System.currentTimeMillis()}.png"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/NovaToolBox")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.failure(Exception("无法创建媒体库记录"))

            context.contentResolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            } ?: return@withContext Result.failure(Exception("无法打开图片输出流"))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }

            Result.success("Pictures/NovaToolBox/$filename")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = (android.graphics.Color.alpha(color) * factor).toInt()
        val red = android.graphics.Color.red(color)
        val green = android.graphics.Color.green(color)
        val blue = android.graphics.Color.blue(color)
        return android.graphics.Color.argb(alpha, red, green, blue)
    }

    private fun androidx.compose.ui.graphics.Color.toArgb(): Int {
        return android.graphics.Color.argb(
            (alpha * 255.0f + 0.5f).toInt(),
            (red * 255.0f + 0.5f).toInt(),
            (green * 255.0f + 0.5f).toInt(),
            (blue * 255.0f + 0.5f).toInt()
        )
    }
}
