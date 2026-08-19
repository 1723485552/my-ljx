package com.novatoolbox.agentforge.features.visual_card.ui

import android.graphics.Color as AndroidColor
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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novatoolbox.agentforge.core.theme.NativeDesignTokens
import com.novatoolbox.agentforge.features.visual_card.data.CardTemplate
import com.novatoolbox.agentforge.features.visual_card.data.CardTheme
import com.novatoolbox.agentforge.features.visual_card.data.VisualCardData
import com.novatoolbox.agentforge.features.visual_card.data.VisualCardExporter
import kotlinx.coroutines.launch

@Composable
fun VisualCardScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var titleInput by remember { mutableStateOf("NovaToolBox 工业设计宣言") }
    var contentInput by remember {
        mutableStateOf("工具的本质在于回归纯粹与极度可靠。\n零冗余依赖，原生毫秒级响应，将每个功能做到极致简洁。")
    }
    var tagInput by remember { mutableStateOf("核心架构") }
    var authorInput by remember { mutableStateOf("Nova Developer") }

    var selectedTemplate by remember { mutableStateOf(CardTemplate.BRIEFING) }
    var selectedTheme by remember { mutableStateOf(CardTheme.DARK_INDUSTRIAL) }
    var showWatermark by remember { mutableStateOf(true) }

    var isExporting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NativeDesignTokens.bgDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = NativeDesignTokens.spacingMd, vertical = NativeDesignTokens.spacingSm)
    ) {
        // 1. 实时视效监视窗口 (HUD Card Preview)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NativeDesignTokens.radiusMd))
                .background(Color(AndroidColor.parseColor(selectedTheme.bgHex)))
                .border(1.dp, NativeDesignTokens.borderDark, RoundedCornerShape(NativeDesignTokens.radiusMd))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "实时视效预览",
                    fontSize = 11.sp,
                    color = NativeDesignTokens.textMuted,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${selectedTemplate.label} · ${selectedTheme.label}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(AndroidColor.parseColor(selectedTheme.accentHex))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 预览内卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(NativeDesignTokens.radiusSm))
                    .background(Color(AndroidColor.parseColor(selectedTheme.cardHex)))
                    .border(
                        1.dp,
                        Color(AndroidColor.parseColor(selectedTheme.accentHex)).copy(alpha = 0.25f),
                        RoundedCornerShape(NativeDesignTokens.radiusSm)
                    )
                    .padding(14.dp)
            ) {
                Column {
                    // 模板顶栏预览
                    if (selectedTemplate == CardTemplate.TERMINAL) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFBBF24)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    } else if (selectedTemplate == CardTemplate.BRIEFING && tagInput.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(AndroidColor.parseColor(selectedTheme.accentHex)).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tagInput,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(AndroidColor.parseColor(selectedTheme.accentHex))
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (titleInput.isNotBlank()) {
                        Text(
                            text = titleInput,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(AndroidColor.parseColor(selectedTheme.textHex))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Text(
                        text = contentInput.ifEmpty { "输入正文内容..." },
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = Color(AndroidColor.parseColor(selectedTheme.textHex)).copy(alpha = 0.9f)
                    )

                    if (authorInput.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "— $authorInput",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(AndroidColor.parseColor(selectedTheme.textHex)).copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(NativeDesignTokens.spacingMd))

        // 2. 文本内容输入卡片
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NativeDesignTokens.radiusMd))
                .background(NativeDesignTokens.surfaceDark)
                .border(1.dp, NativeDesignTokens.borderDark, RoundedCornerShape(NativeDesignTokens.radiusMd))
                .padding(NativeDesignTokens.spacingMd)
        ) {
            Text("图文内容编排", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NativeDesignTokens.textPrimary)

            Spacer(modifier = Modifier.height(8.dp))

            // 标题输入
            TextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                placeholder = { Text("卡片标题 (可选)...", fontSize = 12.sp, color = NativeDesignTokens.textMuted) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = NativeDesignTokens.cardDark,
                    unfocusedContainerColor = NativeDesignTokens.cardDark,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = NativeDesignTokens.textPrimary,
                    unfocusedTextColor = NativeDesignTokens.textPrimary
                ),
                shape = RoundedCornerShape(NativeDesignTokens.radiusSm),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 正文输入
            TextField(
                value = contentInput,
                onValueChange = { contentInput = it },
                placeholder = { Text("在此输入图文正文、金句、随笔或代码...", fontSize = 12.sp, color = NativeDesignTokens.textMuted) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = NativeDesignTokens.cardDark,
                    unfocusedContainerColor = NativeDesignTokens.cardDark,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = NativeDesignTokens.textPrimary,
                    unfocusedTextColor = NativeDesignTokens.textPrimary
                ),
                shape = RoundedCornerShape(NativeDesignTokens.radiusSm),
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 标签与作者两列输入
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    placeholder = { Text("分类标签", fontSize = 11.sp, color = NativeDesignTokens.textMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = NativeDesignTokens.cardDark,
                        unfocusedContainerColor = NativeDesignTokens.cardDark,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = NativeDesignTokens.textPrimary,
                        unfocusedTextColor = NativeDesignTokens.textPrimary
                    ),
                    shape = RoundedCornerShape(NativeDesignTokens.radiusSm),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                TextField(
                    value = authorInput,
                    onValueChange = { authorInput = it },
                    placeholder = { Text("作者署名", fontSize = 11.sp, color = NativeDesignTokens.textMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = NativeDesignTokens.cardDark,
                        unfocusedContainerColor = NativeDesignTokens.cardDark,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = NativeDesignTokens.textPrimary,
                        unfocusedTextColor = NativeDesignTokens.textPrimary
                    ),
                    shape = RoundedCornerShape(NativeDesignTokens.radiusSm),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(NativeDesignTokens.spacingMd))

        // 3. 模板与视觉主题矩阵
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NativeDesignTokens.radiusMd))
                .background(NativeDesignTokens.surfaceDark)
                .border(1.dp, NativeDesignTokens.borderDark, RoundedCornerShape(NativeDesignTokens.radiusMd))
                .padding(NativeDesignTokens.spacingMd)
        ) {
            Text("排版风格预设", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NativeDesignTokens.textPrimary)

            Spacer(modifier = Modifier.height(8.dp))

            // 4 档模板分段矩阵
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CardTemplate.values().forEach { tpl ->
                    val isSelected = selectedTemplate == tpl
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) NativeDesignTokens.accentPrimary else NativeDesignTokens.cardDark)
                            .clickable { selectedTemplate = tpl }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tpl.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) NativeDesignTokens.bgDark else NativeDesignTokens.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text("色彩质感方案", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NativeDesignTokens.textPrimary)

            Spacer(modifier = Modifier.height(8.dp))

            // 4 档主题色盘选择
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CardTheme.values().forEach { thm ->
                    val isSelected = selectedTheme == thm
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(AndroidColor.parseColor(thm.cardHex)))
                            .border(
                                1.5.dp,
                                if (isSelected) NativeDesignTokens.accentPrimary else NativeDesignTokens.borderDark,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { selectedTheme = thm }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = thm.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = Color(AndroidColor.parseColor(thm.textHex))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 水印开关
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(NativeDesignTokens.cardDark)
                    .clickable { showWatermark = !showWatermark }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("底部极简品牌签名", fontSize = 12.sp, color = NativeDesignTokens.textPrimary)
                Switch(
                    checked = showWatermark,
                    onCheckedChange = { showWatermark = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NativeDesignTokens.accentPrimary,
                        checkedTrackColor = NativeDesignTokens.surfaceDark
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(NativeDesignTokens.spacingLg))

        // 4. 一键生成高清大图按钮
        Button(
            onClick = {
                if (contentInput.isBlank()) {
                    Toast.makeText(context, "请输入正文内容", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isExporting = true
                scope.launch {
                    val cardData = VisualCardData(
                        title = titleInput.trim(),
                        content = contentInput.trim(),
                        tag = tagInput.trim(),
                        author = authorInput.trim(),
                        template = selectedTemplate,
                        theme = selectedTheme,
                        showWatermark = showWatermark
                    )
                    val result = VisualCardExporter.exportToGallery(context, cardData)
                    isExporting = false
                    result.onSuccess { path ->
                        Toast.makeText(context, "已成功生成并保存至系统相册！\n$path", Toast.LENGTH_LONG).show()
                    }.onFailure { err ->
                        Toast.makeText(context, "生成失败: ${err.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            enabled = !isExporting,
            colors = ButtonDefaults.buttonColors(
                containerColor = NativeDesignTokens.accentPrimary,
                contentColor = NativeDesignTokens.bgDark
            ),
            shape = RoundedCornerShape(NativeDesignTokens.radiusSm),
            modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
            if (isExporting) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NativeDesignTokens.bgDark, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("正在高清渲染中...", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NativeDesignTokens.bgDark)
            } else {
                Icon(Icons.Rounded.Image, contentDescription = null, tint = NativeDesignTokens.bgDark, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("生成 2K 高清图文卡片", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NativeDesignTokens.bgDark)
            }
        }

        Spacer(modifier = Modifier.height(NativeDesignTokens.spacingXl))
    }
}
