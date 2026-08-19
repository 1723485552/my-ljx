package com.novatoolbox.agentforge.features.translator.ui

import android.content.ClipData
import android.content.ClipboardManager as PlatformClipboardManager
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import com.novatoolbox.agentforge.core.theme.NativeDesignTokens
import com.novatoolbox.agentforge.features.translator.data.SupportLanguage
import com.novatoolbox.agentforge.features.translator.data.TranslationEngine
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorScreen() {
    val context = LocalContext.current
    val clipboard = remember { context.getSystemService<PlatformClipboardManager>() }

    var sourceLang by remember { mutableStateOf(SupportLanguage.ZH_CN) }
    var targetLang by remember { mutableStateOf(SupportLanguage.EN) }

    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }
    var isTranslating by remember { mutableStateOf(false) }

    var showSourceMenu by remember { mutableStateOf(false) }
    var showTargetMenu by remember { mutableStateOf(false) }

    // 系统级 TTS 发音引擎
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val tts = TextToSpeech(context) { /* 初始化完成 */ }
        ttsEngine = tts
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    // 核心：无感实时自动翻译机制（500ms 智能防抖）
    LaunchedEffect(inputText, sourceLang, targetLang) {
        val query = inputText.trim()
        if (query.isEmpty()) {
            outputText = ""
            isTranslating = false
            return@LaunchedEffect
        }

        isTranslating = true
        // 500ms 防抖等待：用户停止输入后再发出网络请求
        delay(500L)

        val result = TranslationEngine.translate(
            text = query,
            sourceLang = sourceLang,
            targetLang = targetLang
        )
        isTranslating = false
        result.onSuccess { translated ->
            outputText = translated
        }.onFailure {
            // 静默失败，保持当前输入状态
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NativeDesignTokens.bgDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = NativeDesignTokens.spacingMd, vertical = NativeDesignTokens.spacingSm)
    ) {
        // 1. 顶部语言切换对调选择栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NativeDesignTokens.radiusMd))
                .background(NativeDesignTokens.surfaceDark)
                .border(1.dp, NativeDesignTokens.borderDark, RoundedCornerShape(NativeDesignTokens.radiusMd))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 源语言下拉
            Box(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(NativeDesignTokens.radiusSm))
                        .clickable { showSourceMenu = true }
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = sourceLang.displayName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NativeDesignTokens.textPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Rounded.ArrowDropDown,
                        contentDescription = null,
                        tint = NativeDesignTokens.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = showSourceMenu,
                    onDismissRequest = { showSourceMenu = false },
                    modifier = Modifier.background(NativeDesignTokens.surfaceDark)
                ) {
                    SupportLanguage.values().forEach { lang ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    lang.displayName,
                                    color = if (lang == sourceLang) NativeDesignTokens.accentPrimary else NativeDesignTokens.textPrimary
                                )
                            },
                            onClick = {
                                sourceLang = lang
                                showSourceMenu = false
                            }
                        )
                    }
                }
            }

            // 语言快速对调
            IconButton(
                onClick = {
                    val temp = sourceLang
                    sourceLang = targetLang
                    targetLang = temp
                    if (outputText.isNotBlank()) {
                        inputText = outputText
                    }
                },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NativeDesignTokens.cardDark)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SwapHoriz,
                    contentDescription = "对调语言",
                    tint = NativeDesignTokens.accentPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // 目标语言下拉
            Box(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(NativeDesignTokens.radiusSm))
                        .clickable { showTargetMenu = true }
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = targetLang.displayName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NativeDesignTokens.textPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Rounded.ArrowDropDown,
                        contentDescription = null,
                        tint = NativeDesignTokens.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = showTargetMenu,
                    onDismissRequest = { showTargetMenu = false },
                    modifier = Modifier.background(NativeDesignTokens.surfaceDark)
                ) {
                    SupportLanguage.values().forEach { lang ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    lang.displayName,
                                    color = if (lang == targetLang) NativeDesignTokens.accentPrimary else NativeDesignTokens.textPrimary
                                )
                            },
                            onClick = {
                                targetLang = lang
                                showTargetMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(NativeDesignTokens.spacingMd))

        // 2. 原文输入卡片
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NativeDesignTokens.radiusMd))
                .background(NativeDesignTokens.surfaceDark)
                .border(1.dp, NativeDesignTokens.borderDark, RoundedCornerShape(NativeDesignTokens.radiusMd))
                .padding(NativeDesignTokens.spacingMd)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "输入文本 (${inputText.length} 字符)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NativeDesignTokens.textPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (inputText.isNotEmpty()) {
                        Text(
                            text = "清空",
                            fontSize = 12.sp,
                            color = NativeDesignTokens.accentDanger,
                            modifier = Modifier
                                .clickable {
                                    inputText = ""
                                    outputText = ""
                                }
                                .padding(4.dp)
                        )
                    }
                    Text(
                        text = "粘贴",
                        fontSize = 12.sp,
                        color = NativeDesignTokens.accentPrimary,
                        modifier = Modifier
                            .clickable {
                                val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                                if (!clip.isNullOrEmpty()) {
                                    inputText = clip
                                }
                            }
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("在此输入或粘贴，即刻自动翻译...", fontSize = 13.sp, color = NativeDesignTokens.textMuted) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = NativeDesignTokens.cardDark,
                    unfocusedContainerColor = NativeDesignTokens.cardDark,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = NativeDesignTokens.textPrimary,
                    unfocusedTextColor = NativeDesignTokens.textPrimary
                ),
                shape = RoundedCornerShape(NativeDesignTokens.radiusSm),
                minLines = 4,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(NativeDesignTokens.spacingMd))

        // 3. 译文呈现卡片（带实时状态指示器）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NativeDesignTokens.radiusMd))
                .background(NativeDesignTokens.surfaceDark)
                .border(
                    1.dp,
                    if (outputText.isNotEmpty()) NativeDesignTokens.accentPrimary.copy(alpha = 0.5f) else NativeDesignTokens.borderDark,
                    RoundedCornerShape(NativeDesignTokens.radiusMd)
                )
                .padding(NativeDesignTokens.spacingMd)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = NativeDesignTokens.accentPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "翻译结果",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NativeDesignTokens.textPrimary
                    )

                    // 实时翻译指示器
                    if (isTranslating) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            color = NativeDesignTokens.accentPrimary,
                            strokeWidth = 1.5.dp
                        )
                    }
                }

                if (outputText.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // 语音朗读
                        IconButton(
                            onClick = {
                                ttsEngine?.language = targetLang.locale
                                ttsEngine?.speak(outputText, TextToSpeech.QUEUE_FLUSH, null, "translate_tts")
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = "朗读",
                                tint = NativeDesignTokens.accentPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // 一键复制
                        FilledTonalButton(
                            onClick = {
                                clipboard?.setPrimaryClip(ClipData.newPlainText("translate", outputText))
                                Toast.makeText(context, "已复制译文", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = NativeDesignTokens.accentPrimary,
                                contentColor = NativeDesignTokens.bgDark
                            ),
                            shape = RoundedCornerShape(NativeDesignTokens.radiusSm),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("复制", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(NativeDesignTokens.radiusSm))
                    .background(NativeDesignTokens.cardDark)
                    .padding(12.dp)
            ) {
                Text(
                    text = when {
                        outputText.isNotEmpty() -> outputText
                        isTranslating -> "正在极速翻译中..."
                        else -> "译文将在此自动呈现..."
                    },
                    fontSize = 13.sp,
                    color = if (outputText.isNotEmpty()) NativeDesignTokens.textPrimary else NativeDesignTokens.textMuted,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(NativeDesignTokens.spacingXl))
    }
}
