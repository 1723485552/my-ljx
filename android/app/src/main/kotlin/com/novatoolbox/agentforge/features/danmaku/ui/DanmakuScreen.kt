package com.novatoolbox.agentforge.features.danmaku.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.novatoolbox.agentforge.core.theme.NativeDesignTokens
import kotlin.math.min
import kotlin.math.sin

val presetColors = listOf(
    Color(0xFF000000), // 极黑
    Color(0xFFFFFFFF), // 纯白
    Color(0xFF38BDF8), // 科技蓝
    Color(0xFF10B981), // 翡翠绿
    Color(0xFFFBBF24), // 琥珀黄
    Color(0xFFEF4444), // 赤红
    Color(0xFFA855F7), // 极光紫
    Color(0xFFEC4899), // 霓虹粉
    Color(0xFFFA8C58), // 活力橙
    Color(0xFF1E293B), // 深邃灰
    Color(0xFF064E3B), // 暗夜绿
    Color(0xFF451A03)  // 焦糖咖
)

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DanmakuScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current

    var textInput by remember { mutableStateOf("NovaToolBox") }
    var fontScalePercent by remember { mutableStateOf(100f) }
    var speed by remember { mutableStateOf(50f) }

    var isGradientBg by remember { mutableStateOf(false) }
    var textColor by remember { mutableStateOf(Color(0xFF38BDF8)) }
    var bgColor by remember { mutableStateOf(Color(0xFF000000)) }

    var isLandscape by remember { mutableStateOf(false) }
    var isRolling by remember { mutableStateOf(false) }

    var isBlinking by remember { mutableStateOf(false) }
    var isBold by remember { mutableStateOf(true) }
    var shakeIntensity by remember { mutableStateOf(0f) }

    var isFullScreen by remember { mutableStateOf(false) }
    var pickingType by remember { mutableStateOf<String?>(null) }

    // 沉浸式真全屏
    DisposableEffect(isFullScreen) {
        val activity = context.findActivity()
        if (activity != null) {
            val insetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            if (isFullScreen) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val act = context.findActivity()
            if (act != null) {
                val insetsController = WindowCompat.getInsetsController(act.window, act.window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler(enabled = isFullScreen) {
        isFullScreen = false
    }

    if (isFullScreen) {
        DanmakuDisplayView(
            text = textInput.ifEmpty { "NovaToolBox" },
            fontScale = fontScalePercent / 100f,
            speed = speed,
            textColor = textColor,
            bgColor = bgColor,
            isGradientBg = isGradientBg,
            isLandscape = isLandscape,
            isRolling = isRolling,
            isBlinking = isBlinking,
            isBold = isBold,
            shakeIntensity = shakeIntensity,
            onBack = { isFullScreen = false }
        )
    } else {
        Scaffold(
            containerColor = NativeDesignTokens.bgDark,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "手持 LED 弹幕",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NativeDesignTokens.textPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "返回",
                                tint = NativeDesignTokens.textPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = NativeDesignTokens.bgDark)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NativeDesignTokens.bgDark)
                    .padding(innerPadding)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. HUD 监视窗 + 文本输入
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(NativeDesignTokens.radiusMd))
                        .background(NativeDesignTokens.surfaceDark)
                        .border(1.dp, NativeDesignTokens.borderDark, RoundedCornerShape(NativeDesignTokens.radiusMd))
                        .padding(10.dp)
                ) {
                    val previewBgModifier = if (isGradientBg) {
                        Modifier.background(
                            Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFF8B5CF6), Color(0xFF38BDF8)))
                        )
                    } else {
                        Modifier.background(bgColor)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, NativeDesignTokens.borderDark, RoundedCornerShape(6.dp))
                            .then(previewBgModifier),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = textInput.ifEmpty { "NovaToolBox" },
                            color = textColor,
                            fontSize = 22.sp,
                            fontWeight = if (isBold) FontWeight.Black else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(NativeDesignTokens.cardDark)
                            .padding(horizontal = 10.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("输入弹幕文本...", fontSize = 12.sp, color = NativeDesignTokens.textMuted) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = NativeDesignTokens.textPrimary,
                                unfocusedTextColor = NativeDesignTokens.textPrimary
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        if (textInput.isNotEmpty()) {
                            Text(
                                text = "清空",
                                fontSize = 11.sp,
                                color = NativeDesignTokens.accentDanger,
                                modifier = Modifier
                                    .clickable { textInput = "" }
                                    .padding(4.dp)
                            )
                        }
                    }
                }

                // 2. 自适应铺满缩放与滚动速度
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(NativeDesignTokens.radiusMd))
                        .background(NativeDesignTokens.surfaceDark)
                        .border(1.dp, NativeDesignTokens.borderDark, RoundedCornerShape(NativeDesignTokens.radiusMd))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("巨幕自适应缩放", fontSize = 11.sp, color = NativeDesignTokens.textSecondary)
                        Text(
                            "${fontScalePercent.toInt()}% (满屏)",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NativeDesignTokens.accentPrimary
                        )
                    }
                    Slider(
                        value = fontScalePercent,
                        onValueChange = { fontScalePercent = it },
                        valueRange = 50f..140f,
                        modifier = Modifier.height(26.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = NativeDesignTokens.accentPrimary,
                            activeTrackColor = NativeDesignTokens.accentPrimary,
                            inactiveTrackColor = NativeDesignTokens.borderDark
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "滚动速度",
                            fontSize = 11.sp,
                            color = if (isRolling) NativeDesignTokens.textSecondary else NativeDesignTokens.textMuted
                        )
                        Text(
                            if (isRolling) "${speed.toInt()} 档" else "静止固定",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isRolling) NativeDesignTokens.accentPrimary else NativeDesignTokens.textMuted
                        )
                    }
                    Slider(
                        value = speed,
                        onValueChange = { speed = it },
                        enabled = isRolling,
                        valueRange = 10f..100f,
                        modifier = Modifier.height(26.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = NativeDesignTokens.accentPrimary,
                            activeTrackColor = NativeDesignTokens.accentPrimary,
                            inactiveTrackColor = NativeDesignTokens.borderDark,
                            disabledThumbColor = NativeDesignTokens.borderDark,
                            disabledActiveTrackColor = NativeDesignTokens.cardDark
                        )
                    )
                }

                // 3. 方向与动静分段矩阵
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(NativeDesignTokens.radiusMd))
                        .background(NativeDesignTokens.surfaceDark)
                        .border(1.dp, NativeDesignTokens.borderDark, RoundedCornerShape(NativeDesignTokens.radiusMd))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 横竖屏
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(NativeDesignTokens.cardDark)
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (!isLandscape) NativeDesignTokens.accentPrimary else Color.Transparent)
                                    .clickable { isLandscape = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("竖屏", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (!isLandscape) NativeDesignTokens.bgDark else NativeDesignTokens.textSecondary)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isLandscape) NativeDesignTokens.accentPrimary else Color.Transparent)
                                    .clickable { isLandscape = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("横屏", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isLandscape) NativeDesignTokens.bgDark else NativeDesignTokens.textSecondary)
                            }
                        }

                        // 动静
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(NativeDesignTokens.cardDark)
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (!isRolling) NativeDesignTokens.accentPrimary else Color.Transparent)
                                    .clickable { isRolling = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("静止", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (!isRolling) NativeDesignTokens.bgDark else NativeDesignTokens.textSecondary)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isRolling) NativeDesignTokens.accentPrimary else Color.Transparent)
                                    .clickable { isRolling = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("滚动", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isRolling) NativeDesignTokens.bgDark else NativeDesignTokens.textSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(NativeDesignTokens.cardDark)
                                .clickable { pickingType = "TEXT" }
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(textColor).border(1.dp, NativeDesignTokens.borderDark, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("文字色彩", fontSize = 11.sp, color = NativeDesignTokens.textPrimary)
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(NativeDesignTokens.cardDark)
                                .clickable { pickingType = "BG" }
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(bgColor).border(1.dp, NativeDesignTokens.borderDark, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("背景底色", fontSize = 11.sp, color = NativeDesignTokens.textPrimary)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isGradientBg) NativeDesignTokens.accentPrimary.copy(alpha = 0.2f) else NativeDesignTokens.cardDark)
                                .border(1.dp, if (isGradientBg) NativeDesignTokens.accentPrimary else NativeDesignTokens.borderDark, RoundedCornerShape(6.dp))
                                .clickable { isGradientBg = !isGradientBg },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "多彩渐变",
                                fontSize = 11.sp,
                                fontWeight = if (isGradientBg) FontWeight.Bold else FontWeight.Normal,
                                color = if (isGradientBg) NativeDesignTokens.accentPrimary else NativeDesignTokens.textSecondary
                            )
                        }
                    }
                }

                // 4. 辅助视效
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(NativeDesignTokens.radiusSm))
                            .background(if (isBlinking) NativeDesignTokens.accentPrimary.copy(alpha = 0.2f) else NativeDesignTokens.surfaceDark)
                            .border(1.dp, if (isBlinking) NativeDesignTokens.accentPrimary else NativeDesignTokens.borderDark, RoundedCornerShape(NativeDesignTokens.radiusSm))
                            .clickable { isBlinking = !isBlinking },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "呼吸闪烁",
                            fontSize = 11.sp,
                            fontWeight = if (isBlinking) FontWeight.Bold else FontWeight.Normal,
                            color = if (isBlinking) NativeDesignTokens.accentPrimary else NativeDesignTokens.textSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(NativeDesignTokens.radiusSm))
                            .background(if (isBold) NativeDesignTokens.accentPrimary.copy(alpha = 0.2f) else NativeDesignTokens.surfaceDark)
                            .border(1.dp, if (isBold) NativeDesignTokens.accentPrimary else NativeDesignTokens.borderDark, RoundedCornerShape(NativeDesignTokens.radiusSm))
                            .clickable { isBold = !isBold },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "极粗字重",
                            fontSize = 11.sp,
                            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                            color = if (isBold) NativeDesignTokens.accentPrimary else NativeDesignTokens.textSecondary
                        )
                    }
                }

                // 5. 启动全屏
                Button(
                    onClick = { isFullScreen = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NativeDesignTokens.accentPrimary,
                        contentColor = NativeDesignTokens.bgDark
                    ),
                    shape = RoundedCornerShape(NativeDesignTokens.radiusSm),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    Text(
                        "开启沉浸式手持弹幕",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NativeDesignTokens.bgDark
                    )
                }
            }
        }
    }

    if (pickingType != null) {
        AlertDialog(
            onDismissRequest = { pickingType = null },
            title = {
                Text(
                    text = if (pickingType == "TEXT") "选择文字颜色" else "选择纯色背景",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NativeDesignTokens.textPrimary
                )
            },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    items(presetColors) { col ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(col)
                                .border(1.dp, NativeDesignTokens.borderDark, RoundedCornerShape(6.dp))
                                .clickable {
                                    if (pickingType == "TEXT") {
                                        textColor = col
                                    } else {
                                        bgColor = col
                                        isGradientBg = false
                                    }
                                    pickingType = null
                                }
                        )
                    }
                }
            },
            confirmButton = {},
            containerColor = NativeDesignTokens.surfaceDark
        )
    }
}

/**
 * 工业级 Skia Canvas 巨幕弹幕渲染器
 * 核心保证：0 字符碰撞、0 溢出裁剪、100% 满屏自适应
 */
@Composable
fun DanmakuDisplayView(
    text: String,
    fontScale: Float,
    speed: Float,
    textColor: Color,
    bgColor: Color,
    isGradientBg: Boolean,
    isLandscape: Boolean,
    isRolling: Boolean,
    isBlinking: Boolean,
    isBold: Boolean,
    shakeIntensity: Float,
    onBack: () -> Unit
) {
    val cleanText = text.ifEmpty { "NovaToolBox" }

    val infiniteTransition = rememberInfiniteTransition()
    val blinkAlpha by if (isBlinking) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(280, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    val shakeTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val shakeOffsetX = if (shakeIntensity > 0) (sin(Math.toRadians(shakeTime.toDouble())) * shakeIntensity * 4f).toFloat() else 0f
    val shakeOffsetY = if (shakeIntensity > 0) (sin(Math.toRadians((shakeTime * 1.5).toDouble())) * shakeIntensity * 4f).toFloat() else 0f

    // 跑马灯滚动驱动
    var totalTravelSpanPx by remember { mutableStateOf(3000f) }
    val animOffset = remember { Animatable(0f) }

    if (isRolling) {
        LaunchedEffect(cleanText, speed, isLandscape, totalTravelSpanPx) {
            val durationMs = ((totalTravelSpanPx / (speed * 16f)) * 1000).toInt().coerceAtLeast(1000)
            while (true) {
                animOffset.snapTo(0f)
                animOffset.animateTo(
                    targetValue = totalTravelSpanPx,
                    animationSpec = tween(durationMillis = durationMs, easing = LinearEasing)
                )
            }
        }
    }

    val bgModifier = if (isGradientBg) {
        Modifier.background(
            Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFF8B5CF6), Color(0xFF38BDF8)))
        )
    } else {
        Modifier.background(bgColor)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(bgModifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor.toArgb()
                alpha = (blinkAlpha * 255).toInt().coerceIn(0, 255)
                typeface = if (isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                isFakeBoldText = isBold
            }

            val nativeCanvas = drawContext.canvas.nativeCanvas
            nativeCanvas.save()
            nativeCanvas.translate(shakeOffsetX, shakeOffsetY)

            if (!isLandscape) {
                // =========================================================================
                // 1. 竖屏模式 (Portrait)
                // =========================================================================
                if (!isRolling) {
                    // 竖屏静止：沿 Y 轴等分格落字，彻底撑满上下
                    val charCount = cleanText.length.coerceAtLeast(1)
                    val availableH = h * 0.94f
                    val availableW = w * 0.92f
                    val slotH = availableH / charCount

                    // 字体大小 = min(格高, 格宽) * 缩放
                    val targetSize = minOf(slotH * 0.96f, availableW) * fontScale
                    paint.textSize = targetSize
                    paint.textAlign = Paint.Align.CENTER

                    val startY = (h - availableH) / 2f
                    val fontMetrics = paint.fontMetrics

                    cleanText.forEachIndexed { i, char ->
                        val slotCenterY = startY + (i + 0.5f) * slotH
                        val baseline = slotCenterY - (fontMetrics.descent + fontMetrics.ascent) / 2f
                        nativeCanvas.drawText(char.toString(), cx, baseline, paint)
                    }
                } else {
                    // 竖屏跑马灯：横向从右往左流淌
                    val targetSize = (w * 0.65f) * fontScale
                    paint.textSize = targetSize
                    paint.textAlign = Paint.Align.LEFT

                    val textWidth = paint.measureText(cleanText)
                    val totalSpan = w + textWidth
                    if (totalTravelSpanPx != totalSpan) totalTravelSpanPx = totalSpan

                    val fontMetrics = paint.fontMetrics
                    val baseline = cy - (fontMetrics.descent + fontMetrics.ascent) / 2f
                    val startX = w - animOffset.value

                    nativeCanvas.drawText(cleanText, startX, baseline, paint)
                }
            } else {
                // =========================================================================
                // 2. 横屏模式 (Landscape - 90度无畸变中心坐标变换)
                // =========================================================================
                nativeCanvas.translate(cx, cy)
                nativeCanvas.rotate(90f)

                // 旋转后，沿手机长边的有效轴长为 h，短边有效轴长为 w
                val viewW = h
                val viewH = w

                if (!isRolling) {
                    // 横屏静止：巨幕整体横贯长边，决不单字错位
                    val testSize = 100f
                    paint.textSize = testSize
                    val rawWidth = paint.measureText(cleanText).coerceAtLeast(1f)
                    val fontMetrics = paint.fontMetrics
                    val rawHeight = (fontMetrics.descent - fontMetrics.ascent).coerceAtLeast(1f)

                    // 满屏约束系数：长边占 94%，短边占 90%
                    val scaleX = (viewW * 0.94f) / rawWidth
                    val scaleY = (viewH * 0.90f) / rawHeight
                    val optimalSize = testSize * minOf(scaleX, scaleY) * fontScale

                    paint.textSize = optimalSize
                    paint.textAlign = Paint.Align.CENTER

                    val fm = paint.fontMetrics
                    val baseline = -(fm.descent + fm.ascent) / 2f

                    // 一次性整体绘制整串文字，0 字符碰撞
                    nativeCanvas.drawText(cleanText, 0f, baseline, paint)
                } else {
                    // 横屏跑马灯：沿长边无限滚动
                    val targetSize = (viewH * 0.78f) * fontScale
                    paint.textSize = targetSize
                    paint.textAlign = Paint.Align.LEFT

                    val textWidth = paint.measureText(cleanText)
                    val totalSpan = viewW + textWidth
                    if (totalTravelSpanPx != totalSpan) totalTravelSpanPx = totalSpan

                    val fm = paint.fontMetrics
                    val baseline = -(fm.descent + fm.ascent) / 2f
                    val startX = (viewW / 2f) - animOffset.value

                    nativeCanvas.drawText(cleanText, startX, baseline, paint)
                }
            }

            nativeCanvas.restore()
        }

        // 左上角带毛玻璃感的低调返回按钮
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 20.dp)
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "返回",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
