package com.novatoolbox.agentforge.features.visual_card.data

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class CardTemplate(val label: String) {
    TERMINAL("技术终端"),
    BRIEFING("工业简报"),
    QUOTE("极简引言"),
    NOTE("极客便签")
}

enum class CardTheme(val label: String, val bgHex: String, val cardHex: String, val textHex: String, val accentHex: String) {
    DARK_INDUSTRIAL("极黑工业", "#09090B", "#18181B", "#F4F4F5", "#38BDF8"),
    CYBER_SLATE("深海石板", "#0F172A", "#1E293B", "#F8FAFC", "#38BDF8"),
    PAPER_CLEAN("极简米白", "#E2E8F0", "#FFFFFF", "#0F172A", "#2563EB"),
    EMERALD_NIGHT("暗夜绿幽", "#061A14", "#0C2E24", "#ECFDF5", "#10B981")
}

data class VisualCardData(
    val title: String,
    val content: String,
    val tag: String,
    val author: String,
    val template: CardTemplate,
    val theme: CardTheme,
    val showWatermark: Boolean = true
)

object VisualCardExporter {

    suspend fun exportToGallery(context: Context, data: VisualCardData): Result<String> = withContext(Dispatchers.IO) {
        try {
            val width = 1080
            val cardPadding = 60f
            val innerPadding = 64f
            val cardWidth = width - (cardPadding * 2)
            val contentWidth = (cardWidth - (innerPadding * 2)).toInt()

            // 1. 预先计算内容高度
            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(data.theme.textHex)
                textSize = 52f
                typeface = if (data.template == CardTemplate.TERMINAL) Typeface.MONOSPACE else Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(data.theme.textHex)
                textSize = 34f
                typeface = if (data.template == CardTemplate.TERMINAL) Typeface.MONOSPACE else Typeface.DEFAULT
            }

            val titleLayout = if (data.title.isNotBlank()) {
                StaticLayout.Builder.obtain(data.title, 0, data.title.length, titlePaint, contentWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.15f)
                    .build()
            } else null

            val bodyLayout = StaticLayout.Builder.obtain(data.content, 0, data.content.length, bodyPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.25f)
                .build()

            // 计算卡片动态总高度
            var cardContentHeight = innerPadding * 2 + bodyLayout.height + 100f
            if (titleLayout != null) cardContentHeight += titleLayout.height + 40f
            if (data.template == CardTemplate.TERMINAL) cardContentHeight += 60f
            if (data.template == CardTemplate.BRIEFING) cardContentHeight += 70f
            if (data.showWatermark) cardContentHeight += 60f

            val totalHeight = (cardContentHeight + (cardPadding * 2)).toInt().coerceAtLeast(1080)

            // 2. 创建高清 Bitmap 与 Canvas
            val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 绘制画布背景
            canvas.drawColor(Color.parseColor(data.theme.bgHex))

            // 绘制主卡片背景与描边
            val cardRect = RectF(cardPadding, cardPadding, width - cardPadding, totalHeight - cardPadding)
            val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(data.theme.cardHex)
                style = Paint.Style.FILL
            }
            val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(data.theme.accentHex)
                alpha = 60
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawRoundRect(cardRect, 28f, 28f, cardBgPaint)
            canvas.drawRoundRect(cardRect, 28f, 28f, cardBorderPaint)

            var currentY = cardPadding + innerPadding

            // 3. 根据模板绘制顶部装饰
            when (data.template) {
                CardTemplate.TERMINAL -> {
                    // macOS 终端三色圆点
                    val dotRadius = 12f
                    val dotColors = listOf("#EF4444", "#FBBF24", "#10B981")
                    var dotX = cardPadding + innerPadding
                    dotColors.forEach { hex ->
                        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(hex) }
                        canvas.drawCircle(dotX + dotRadius, currentY + dotRadius, dotRadius, p)
                        dotX += 36f
                    }

                    // 终端右侧标签
                    val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor(data.theme.accentHex)
                        textSize = 24f
                        typeface = Typeface.MONOSPACE
                        textAlign = Paint.Align.RIGHT
                    }
                    val tagText = if (data.tag.isNotBlank()) "# ${data.tag}" else "bash"
                    canvas.drawText(tagText, width - cardPadding - innerPadding, currentY + 18f, tagPaint)

                    currentY += 60f
                }

                CardTemplate.BRIEFING -> {
                    // 工业分类胶囊
                    if (data.tag.isNotBlank()) {
                        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.parseColor(data.theme.accentHex)
                            alpha = 40
                        }
                        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.parseColor(data.theme.accentHex)
                            textSize = 24f
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        }
                        val tagW = badgeTextPaint.measureText(data.tag) + 32f
                        val badgeRect = RectF(cardPadding + innerPadding, currentY, cardPadding + innerPadding + tagW, currentY + 44f)
                        canvas.drawRoundRect(badgeRect, 8f, 8f, badgePaint)
                        canvas.drawText(data.tag, cardPadding + innerPadding + 16f, currentY + 31f, badgeTextPaint)
                    }

                    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#71717A")
                        textSize = 24f
                        textAlign = Paint.Align.RIGHT
                    }
                    val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
                    canvas.drawText(dateStr, width - cardPadding - innerPadding, currentY + 31f, datePaint)

                    currentY += 70f
                }

                CardTemplate.QUOTE -> {
                    // 极简大引言符号
                    val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor(data.theme.accentHex)
                        textSize = 100f
                        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    }
                    canvas.drawText("“", cardPadding + innerPadding, currentY + 60f, quotePaint)
                    currentY += 70f
                }

                CardTemplate.NOTE -> {
                    currentY += 10f
                }
            }

            // 4. 绘制标题
            if (titleLayout != null) {
                canvas.save()
                canvas.translate(cardPadding + innerPadding, currentY)
                titleLayout.draw(canvas)
                canvas.restore()
                currentY += titleLayout.height + 30f

                // 细分割线
                val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor(data.theme.textHex)
                    alpha = 30
                    strokeWidth = 1.5f
                }
                canvas.drawLine(cardPadding + innerPadding, currentY, width - cardPadding - innerPadding, currentY, divPaint)
                currentY += 30f
            }

            // 5. 绘制正文
            canvas.save()
            canvas.translate(cardPadding + innerPadding, currentY)
            bodyLayout.draw(canvas)
            canvas.restore()
            currentY += bodyLayout.height + 40f

            // 6. 底部作者与水印
            if (data.author.isNotBlank()) {
                val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor(data.theme.textHex)
                    alpha = 180
                    textSize = 26f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText("— ${data.author}", cardPadding + innerPadding, currentY + 20f, authorPaint)
            }

            if (data.showWatermark) {
                val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#71717A")
                    textSize = 22f
                    textAlign = Paint.Align.RIGHT
                }
                canvas.drawText("NovaToolBox · 极简卡片工坊", width - cardPadding - innerPadding, totalHeight - cardPadding - 30f, wmPaint)
            }

            // 7. 保存到系统相册 Pictures/NovaToolBox
            val filename = "Nova_Card_${System.currentTimeMillis()}.png"
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
}
