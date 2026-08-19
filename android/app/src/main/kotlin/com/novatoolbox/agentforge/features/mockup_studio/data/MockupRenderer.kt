package com.novatoolbox.agentforge.features.mockup_studio.data

import android.graphics.*

enum class MockupFrameType(val label: String) {
    IPHONE_16_PRO("iPhone 16 Pro"),
    FOLD_SCREEN("折叠大屏"),
    S24_ULTRA("S24 Ultra"),
    MAC_WINDOW("Mac 窗口"),
    IPAD_PRO("iPad Pro")
}

enum class MockupRatio(val label: String, val widthRatio: Float, val heightRatio: Float) {
    XIAOHONGSHU("3:4 小红书", 3f, 4f),
    SQUARE("1:1 朋友圈", 1f, 1f),
    BANNER("16:9 横幅", 16f, 9f),
    FULLSCREEN("9:16 全屏", 9f, 16f)
}

enum class MockupBackground(val label: String) {
    AMBIENT_GLOW("智能极光"),
    STUDIO_WHITE("天幕棚拍"),
    SUNSET_LIGHT("晨曦流光"),
    FROSTED_CLEAR("晶透虚化"),
    TRUE_BLACK("纯粹极黑")
}

data class MockupParams(
    val pitch: Float = 10f,         // 俯仰角 (-30..30)
    val yaw: Float = -14f,          // 偏航角 (-30..30)
    val roll: Float = 0f,           // 翻转角 (-20..20)
    val scale: Float = 0.95f,       // 缩放 (0.6..1.3)
    val shadowIntensity: Float = 0.65f, // 阴影深度
    val enableGlassReflection: Boolean = true, // 镜面通透高光
    val frameType: MockupFrameType = MockupFrameType.IPHONE_16_PRO,
    val ratio: MockupRatio = MockupRatio.XIAOHONGSHU,
    val background: MockupBackground = MockupBackground.AMBIENT_GLOW
)

object MockupRenderer {

    fun extractDominantColor(bitmap: Bitmap?): Int {
        if (bitmap == null) return Color.parseColor("#38BDF8")
        var redSum = 0L
        var greenSum = 0L
        var blueSum = 0L
        var count = 0

        val step = (bitmap.width * bitmap.height / 500).coerceAtLeast(1)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (i in pixels.indices step step) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            val brightness = (r * 299 + g * 587 + b * 114) / 1000
            if (brightness in 40..240) {
                redSum += r
                greenSum += g
                blueSum += b
                count++
            }
        }

        return if (count > 0) {
            Color.rgb((redSum / count).toInt(), (greenSum / count).toInt(), (blueSum / count).toInt())
        } else {
            Color.parseColor("#38BDF8")
        }
    }

    /**
     * 单管线渲染核心：UI 实时预览与 4K 导出共用此唯一逻辑
     */
    fun render(
        canvas: Canvas,
        width: Float,
        height: Float,
        sourceBitmap: Bitmap?,
        params: MockupParams,
        dominantColor: Int
    ) {
        // 1. 绘制高透亮背景
        drawVibrantBackground(canvas, width, height, sourceBitmap, params, dominantColor)

        if (sourceBitmap == null) return

        // 2. 根据真机硬件几何参数构建实体
        val phoneBitmap: Bitmap
        val phoneTotalW: Float
        val phoneTotalH: Float

        when (params.frameType) {
            MockupFrameType.IPHONE_16_PRO -> {
                // iPhone 16 Pro 物理硬件比例 19.5:9 (超窄钛金属边框 + 实体物理按键)
                val screenW = 1000f
                val screenH = screenW * (19.5f / 9f) // 2166.6f
                val bezel = 22f // 极致窄边框
                val buttonExtraW = 8f

                phoneTotalW = screenW + (bezel * 2) + (buttonExtraW * 2)
                phoneTotalH = screenH + (bezel * 2)
                val bodyW = screenW + (bezel * 2)
                val bodyLeft = buttonExtraW

                phoneBitmap = Bitmap.createBitmap(phoneTotalW.toInt(), phoneTotalH.toInt(), Bitmap.Config.ARGB_8888)
                val fc = Canvas(phoneBitmap)

                // 绘制侧边物理实体按键 (Action 按键、音量键、电源键、相机触控按键)
                val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#3A3A3C") }
                // 左侧 Action 按键 & 音量加减
                fc.drawRoundRect(RectF(0f, 260f, buttonExtraW + 2f, 320f), 4f, 4f, btnPaint)
                fc.drawRoundRect(RectF(0f, 360f, buttonExtraW + 2f, 470f), 4f, 4f, btnPaint)
                fc.drawRoundRect(RectF(0f, 500f, buttonExtraW + 2f, 610f), 4f, 4f, btnPaint)
                // 右侧电源键 & 相机触控按键
                fc.drawRoundRect(RectF(bodyLeft + bodyW - 2f, 380f, phoneTotalW, 540f), 4f, 4f, btnPaint)
                fc.drawRoundRect(RectF(bodyLeft + bodyW - 2f, 680f, phoneTotalW, 800f), 4f, 4f, btnPaint)

                // 钛金属机身中框 (深空灰钛金双倒角)
                val bodyRect = RectF(bodyLeft, 0f, bodyLeft + bodyW, phoneTotalH)
                val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1C1C1E") }
                fc.drawRoundRect(bodyRect, 78f, 78f, bodyPaint)

                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#48484A")
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                }
                fc.drawRoundRect(bodyRect, 78f, 78f, strokePaint)

                // 天线隔断条 (4 处天线断点)
                val antennaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#09090B")
                    strokeWidth = 3f
                }
                fc.drawLine(bodyLeft, 220f, bodyLeft + bezel, 220f, antennaPaint)
                fc.drawLine(bodyLeft, phoneTotalH - 220f, bodyLeft + bezel, phoneTotalH - 220f, antennaPaint)
                fc.drawLine(bodyLeft + bodyW - bezel, 220f, bodyLeft + bodyW, 220f, antennaPaint)
                fc.drawLine(bodyLeft + bodyW - bezel, phoneTotalH - 220f, bodyLeft + bodyW, phoneTotalH - 220f, antennaPaint)

                // 屏幕贴图区域 (G3 连续曲率圆角)
                val screenRect = RectF(bodyLeft + bezel, bezel, bodyLeft + bodyW - bezel, phoneTotalH - bezel)
                val screenPath = Path().apply { addRoundRect(screenRect, 56f, 56f, Path.Direction.CW) }
                fc.save()
                fc.clipPath(screenPath)
                drawCenterCropBitmap(fc, sourceBitmap, screenRect)

                if (params.enableGlassReflection) {
                    drawGlassSpecularHighlight(fc, screenRect)
                }

                // 灵动岛 (带精细听筒微缝与双摄微光)
                val islandW = 210f
                val islandH = 58f
                val islandLeft = bodyLeft + (bodyW - islandW) / 2f
                val islandTop = bezel + 20f
                val islandRect = RectF(islandLeft, islandTop, islandLeft + islandW, islandTop + islandH)
                fc.drawRoundRect(islandRect, islandH / 2f, islandH / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK })

                // 灵动岛右侧镜头反光
                fc.drawCircle(islandLeft + islandW - 28f, islandTop + islandH / 2f, 9f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#1E293B")
                })
                fc.drawCircle(islandLeft + islandW - 28f, islandTop + islandH / 2f, 4f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#0284C7")
                })
                fc.restore()
            }

            MockupFrameType.FOLD_SCREEN -> {
                // 折叠屏展开态 1 : 1.15 方屏工业比例
                val screenW = 1450f
                val screenH = 1450f * 1.12f // 1624f 宽大展开态
                val bezel = 20f

                phoneTotalW = screenW + (bezel * 2)
                phoneTotalH = screenH + (bezel * 2)

                phoneBitmap = Bitmap.createBitmap(phoneTotalW.toInt(), phoneTotalH.toInt(), Bitmap.Config.ARGB_8888)
                val fc = Canvas(phoneBitmap)

                val bodyRect = RectF(0f, 0f, phoneTotalW, phoneTotalH)
                fc.drawRoundRect(bodyRect, 40f, 40f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#18181B") })
                fc.drawRoundRect(bodyRect, 40f, 40f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#3F3F46")
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                })

                val screenRect = RectF(bezel, bezel, phoneTotalW - bezel, phoneTotalH - bezel)
                val screenPath = Path().apply { addRoundRect(screenRect, 24f, 24f, Path.Direction.CW) }
                fc.save()
                fc.clipPath(screenPath)
                drawCenterCropBitmap(fc, sourceBitmap, screenRect)

                // 中央铰链屏幕折痕微光 (Crease Highlight & Shadow)
                val midX = phoneTotalW / 2f
                val creasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        midX - 22f, 0f, midX + 22f, 0f,
                        intArrayOf(Color.TRANSPARENT, Color.argb(45, 0, 0, 0), Color.argb(40, 255, 255, 255), Color.argb(30, 0, 0, 0), Color.TRANSPARENT),
                        floatArrayOf(0f, 0.40f, 0.50f, 0.60f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                fc.drawRect(midX - 22f, bezel, midX + 22f, phoneTotalH - bezel, creasePaint)

                // 上下铰链保护帽 (Hinge Cap Caps)
                val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#27272A") }
                fc.drawRoundRect(RectF(midX - 12f, 0f, midX + 12f, bezel + 6f), 4f, 4f, capPaint)
                fc.drawRoundRect(RectF(midX - 12f, phoneTotalH - bezel - 6f, midX + 12f, phoneTotalH), 4f, 4f, capPaint)

                if (params.enableGlassReflection) {
                    drawGlassSpecularHighlight(fc, screenRect)
                }

                // 右上屏微打孔
                fc.drawCircle(phoneTotalW * 0.75f, bezel + 28f, 12f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK })
                fc.restore()
            }

            MockupFrameType.S24_ULTRA -> {
                // S24 Ultra 极客微方角 (R=16 工业直角边)
                val screenW = 1000f
                val screenH = screenW * (19.5f / 9f)
                val bezel = 18f

                phoneTotalW = screenW + (bezel * 2)
                phoneTotalH = screenH + (bezel * 2)

                phoneBitmap = Bitmap.createBitmap(phoneTotalW.toInt(), phoneTotalH.toInt(), Bitmap.Config.ARGB_8888)
                val fc = Canvas(phoneBitmap)

                val bodyRect = RectF(0f, 0f, phoneTotalW, phoneTotalH)
                fc.drawRoundRect(bodyRect, 22f, 22f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1C1C1E") })
                fc.drawRoundRect(bodyRect, 22f, 22f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#52525B")
                    style = Paint.Style.STROKE
                    strokeWidth = 3.5f
                })

                val screenRect = RectF(bezel, bezel, phoneTotalW - bezel, phoneTotalH - bezel)
                val screenPath = Path().apply { addRoundRect(screenRect, 10f, 10f, Path.Direction.CW) }
                fc.save()
                fc.clipPath(screenPath)
                drawCenterCropBitmap(fc, sourceBitmap, screenRect)

                if (params.enableGlassReflection) {
                    drawGlassSpecularHighlight(fc, screenRect)
                }

                // 居中星环极微单打孔
                val holeX = phoneTotalW / 2f
                val holeY = bezel + 24f
                fc.drawCircle(holeX, holeY, 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK })
                fc.drawCircle(holeX, holeY, 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#27272A")
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                })
                fc.restore()
            }

            MockupFrameType.MAC_WINDOW -> {
                // macOS 标准 16:10 宽屏窗口
                val winW = 1500f
                val winH = winW * (10f / 16f)
                val headerH = 68f

                phoneTotalW = winW
                phoneTotalH = winH + headerH

                phoneBitmap = Bitmap.createBitmap(phoneTotalW.toInt(), phoneTotalH.toInt(), Bitmap.Config.ARGB_8888)
                val fc = Canvas(phoneBitmap)

                val winRect = RectF(0f, 0f, phoneTotalW, phoneTotalH)
                fc.drawRoundRect(winRect, 28f, 28f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#27272A") })
                fc.drawRoundRect(winRect, 28f, 28f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#3F3F46")
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                })

                // 水晶三色控制灯
                var dotX = 32f
                listOf("#FF5F56", "#FFBD2E", "#27C93F").forEach { hex ->
                    fc.drawCircle(dotX, headerH / 2f, 10f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(hex) })
                    dotX += 28f
                }

                val searchRect = RectF(phoneTotalW * 0.28f, 14f, phoneTotalW * 0.72f, headerH - 14f)
                fc.drawRoundRect(searchRect, 10f, 10f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#18181B") })

                val contentRect = RectF(0f, headerH, phoneTotalW, phoneTotalH)
                val contentPath = Path().apply {
                    val radii = floatArrayOf(0f, 0f, 0f, 0f, 28f, 28f, 28f, 28f)
                    addRoundRect(contentRect, radii, Path.Direction.CW)
                }
                fc.save()
                fc.clipPath(contentPath)
                drawCenterCropBitmap(fc, sourceBitmap, contentRect)
                if (params.enableGlassReflection) {
                    drawGlassSpecularHighlight(fc, contentRect)
                }
                fc.restore()
            }

            MockupFrameType.IPAD_PRO -> {
                // iPad Pro 4:3 生产力大平板
                val padW = 1400f
                val padH = padW * (3f / 4f)
                val bezel = 36f

                phoneTotalW = padW
                phoneTotalH = padH

                phoneBitmap = Bitmap.createBitmap(phoneTotalW.toInt(), phoneTotalH.toInt(), Bitmap.Config.ARGB_8888)
                val fc = Canvas(phoneBitmap)

                val bodyRect = RectF(0f, 0f, phoneTotalW, phoneTotalH)
                fc.drawRoundRect(bodyRect, 48f, 48f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#18181B") })
                fc.drawRoundRect(bodyRect, 48f, 48f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#3F3F46")
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                })

                val screenRect = RectF(bezel, bezel, phoneTotalW - bezel, phoneTotalH - bezel)
                val screenPath = Path().apply { addRoundRect(screenRect, 28f, 28f, Path.Direction.CW) }
                fc.save()
                fc.clipPath(screenPath)
                drawCenterCropBitmap(fc, sourceBitmap, screenRect)
                if (params.enableGlassReflection) {
                    drawGlassSpecularHighlight(fc, screenRect)
                }

                // 顶部横置居中前置摄像头
                fc.drawCircle(phoneTotalW / 2f, bezel / 2f, 6f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK })
                fc.restore()
            }
        }

        // 3. 3D 空间矩阵投影 (Camera + Matrix)
        val camera = Camera()
        val matrix = Matrix()

        camera.save()
        camera.rotateX(-params.pitch)
        camera.rotateY(params.yaw)
        camera.rotateZ(-params.roll)
        camera.getMatrix(matrix)
        camera.restore()

        val fitScale = (height * 0.72f / phoneTotalH) * params.scale
        matrix.preScale(fitScale, fitScale)
        matrix.preTranslate(-phoneTotalW / 2f, -phoneTotalH / 2f)
        matrix.postTranslate(width / 2f, height / 2f)

        // 4. 绘制高拟真软漫反射立体阴影
        if (params.shadowIntensity > 0.05f) {
            val shadowMatrix = Matrix(matrix).apply {
                postTranslate(params.yaw * 1.8f, params.pitch * 1.8f + (45f * (height / 1000f)))
            }
            val shadowAlpha = (params.shadowIntensity * 140).toInt().coerceIn(0, 255)
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = PorterDuffColorFilter(Color.argb(shadowAlpha, 0, 0, 0), PorterDuff.Mode.SRC_IN)
                maskFilter = BlurMaskFilter(48f * (height / 1000f), BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawBitmap(phoneBitmap, shadowMatrix, shadowPaint)
        }

        // 5. 绘制机模实体
        canvas.drawBitmap(phoneBitmap, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        phoneBitmap.recycle()
    }

    /**
     * CenterCrop 等比填充贴图 (杜绝拉伸变形)
     */
    private fun drawCenterCropBitmap(canvas: Canvas, bitmap: Bitmap, dst: RectF) {
        val srcW = bitmap.width.toFloat()
        val srcH = bitmap.height.toFloat()
        val dstW = dst.width()
        val dstH = dst.height()

        val scale = Math.max(dstW / srcW, dstH / srcH)
        val scaledW = srcW * scale
        val scaledH = srcH * scale
        val left = dst.left + (dstW - scaledW) / 2f
        val top = dst.top + (dstH - scaledH) / 2f

        canvas.drawBitmap(
            bitmap,
            null,
            RectF(left, top, left + scaledW, top + scaledH),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )
    }

    /**
     * 绘制 45° 摄影棚镜面通透漫反射高光
     */
    private fun drawGlassSpecularHighlight(canvas: Canvas, rect: RectF) {
        val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                rect.left, rect.top,
                rect.right, rect.bottom,
                intArrayOf(
                    Color.argb(70, 255, 255, 255),
                    Color.argb(20, 255, 255, 255),
                    Color.TRANSPARENT,
                    Color.argb(30, 255, 255, 255)
                ),
                floatArrayOf(0f, 0.35f, 0.65f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(rect, glassPaint)
    }

    /**
     * 绘制透亮背景方案
     */
    private fun drawVibrantBackground(
        canvas: Canvas,
        w: Float,
        h: Float,
        sourceBitmap: Bitmap?,
        params: MockupParams,
        dominantColor: Int
    ) {
        when (params.background) {
            MockupBackground.AMBIENT_GLOW -> {
                canvas.drawColor(Color.parseColor("#0C0E14"))
                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        w / 2f,
                        h / 2f,
                        w * 0.70f,
                        intArrayOf(dominantColor and 0x00FFFFFF or 0x85000000.toInt(), Color.TRANSPARENT),
                        floatArrayOf(0f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, w, h, glowPaint)
            }

            MockupBackground.STUDIO_WHITE -> {
                canvas.drawColor(Color.parseColor("#F4F4F5"))
                val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        w / 2f,
                        h * 0.3f,
                        w * 0.8f,
                        intArrayOf(Color.parseColor("#FFFFFF"), Color.parseColor("#E4E4E7")),
                        floatArrayOf(0f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, w, h, lightPaint)
            }

            MockupBackground.SUNSET_LIGHT -> {
                val sunsetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, 0f, w, h,
                        intArrayOf(Color.parseColor("#1E1B4B"), Color.parseColor("#311042"), Color.parseColor("#0F172A")),
                        floatArrayOf(0f, 0.5f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, w, h, sunsetPaint)
                val warmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(w * 0.8f, h * 0.2f, w * 0.6f, Color.argb(90, 245, 158, 11), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                }
                canvas.drawRect(0f, 0f, w, h, warmPaint)
            }

            MockupBackground.FROSTED_CLEAR -> {
                canvas.drawColor(Color.parseColor("#09090B"))
                if (sourceBitmap != null) {
                    val scaled = Bitmap.createScaledBitmap(sourceBitmap, 120, (120 * (sourceBitmap.height.toFloat() / sourceBitmap.width)).toInt(), false)
                    val blurPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                        alpha = 180
                    }
                    canvas.drawBitmap(scaled, null, RectF(0f, 0f, w, h), blurPaint)
                    scaled.recycle()
                }
                canvas.drawRect(0f, 0f, w, h, Paint().apply { color = Color.argb(40, 0, 0, 0) })
            }

            MockupBackground.TRUE_BLACK -> {
                canvas.drawColor(Color.parseColor("#000000"))
            }
        }
    }
}
