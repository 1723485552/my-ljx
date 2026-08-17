package com.novatoolbox.agentforge.features.text_tools.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FormatColorText
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import android.content.ClipboardManager as PlatformClipboardManager
import android.content.ClipData
import androidx.core.content.getSystemService
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novatoolbox.agentforge.core.theme.NativeDesignTokens

enum class TransformMode(
    val title: String,
    val desc: String,
    val icon: ImageVector,
) {
    REVERSE_ALL("字符倒序", "abc 123 → 321 cba", Icons.Rounded.SwapHoriz),
    REVERSE_WORDS("单词倒序", "hello world → world hello", Icons.Rounded.SyncAlt),
    REVERSE_LINES("按行倒序", "多行文本首尾行翻转", Icons.Rounded.SwapVert),
    SHUFFLE_CHARS("字符打乱", "随机打乱字符次序", Icons.Rounded.Shuffle),
    UPPERCASE("全大写", "转换为 UPPERCASE", Icons.Rounded.TextFields),
    LOWERCASE("全小写", "转换为 lowercase", Icons.Rounded.FormatColorText),
}

/**
 * 文本反转/变换工坊（等价原 Flutter TextReverserPlugin 的全部 6 种模式）。
 *
 * 纯本地即时转换：输入/模式变更即重算，无任何网络或外部依赖。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextReverserScreen() {
    val context = LocalContext.current
    val clipboard = remember { context.getSystemService<PlatformClipboardManager>() }

    var inputText by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(TransformMode.REVERSE_ALL) }

    val outputText by remember(inputText, selectedMode) {
        derivedStateOf {
            if (inputText.isEmpty()) return@derivedStateOf ""
            when (selectedMode) {
                TransformMode.REVERSE_ALL -> inputText.reversed()
                TransformMode.REVERSE_WORDS ->
                    inputText.split(Regex("(?<=\\s)|(?=\\s)"))
                        .reversed().joinToString("")
                TransformMode.REVERSE_LINES ->
                    inputText.lines().reversed().joinToString("\n")
                TransformMode.SHUFFLE_CHARS ->
                    inputText.toList().shuffled().joinToString("")
                TransformMode.UPPERCASE -> inputText.uppercase()
                TransformMode.LOWERCASE -> inputText.lowercase()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NativeDesignTokens.bgDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = NativeDesignTokens.spacingMd, vertical = NativeDesignTokens.spacingSm),
    ) {
            // 1. 模式矩阵：2 列等宽规则网格
            Text(
                text = "变换模式",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NativeDesignTokens.textSecondary,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.height(NativeDesignTokens.spacingSm))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val modes = TransformMode.values()
                for (i in modes.indices step 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ModeCard(
                            mode = modes[i],
                            isSelected = selectedMode == modes[i],
                            onClick = { selectedMode = modes[i] },
                            modifier = Modifier.weight(1f),
                        )
                        if (i + 1 < modes.size) {
                            ModeCard(
                                mode = modes[i + 1],
                                isSelected = selectedMode == modes[i + 1],
                                onClick = { selectedMode = modes[i + 1] },
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(NativeDesignTokens.spacingLg))

            // 2. 原始输入工作台卡片
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(NativeDesignTokens.radiusMd))
                    .background(NativeDesignTokens.surfaceDark)
                    .border(
                        1.dp,
                        NativeDesignTokens.borderDark,
                        RoundedCornerShape(NativeDesignTokens.radiusMd),
                    )
                    .padding(NativeDesignTokens.spacingMd),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "输入文本 (${inputText.length} 字符)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NativeDesignTokens.textPrimary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (inputText.isNotEmpty()) {
                            Text(
                                text = "清空",
                                fontSize = 12.sp,
                                color = NativeDesignTokens.accentDanger,
                                modifier = Modifier
                                    .clickable { inputText = "" }
                                    .padding(4.dp),
                            )
                        }
                        Text(
                            text = "粘贴",
                            fontSize = 12.sp,
                            color = NativeDesignTokens.accentPrimary,
                            modifier = Modifier
                                .clickable {
                                    val clip = clipboard?.primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        inputText = clip.getItemAt(0).text?.toString() ?: ""
                                    }
                                }
                                .padding(4.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            "在此输入或粘贴需要处理的文本...",
                            fontSize = 13.sp,
                            color = NativeDesignTokens.textMuted,
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = NativeDesignTokens.cardDark,
                        unfocusedContainerColor = NativeDesignTokens.cardDark,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedTextColor = NativeDesignTokens.textPrimary,
                        unfocusedTextColor = NativeDesignTokens.textPrimary,
                    ),
                    shape = RoundedCornerShape(NativeDesignTokens.radiusSm),
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(NativeDesignTokens.spacingMd))

            // 3. 处理结果卡片
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(NativeDesignTokens.radiusMd))
                    .background(NativeDesignTokens.surfaceDark)
                    .border(
                        1.dp,
                        if (outputText.isNotEmpty()) {
                            NativeDesignTokens.accentPrimary.copy(alpha = 0.5f)
                        } else {
                            NativeDesignTokens.borderDark
                        },
                        RoundedCornerShape(NativeDesignTokens.radiusMd),
                    )
                    .padding(NativeDesignTokens.spacingMd),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = NativeDesignTokens.accentPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "处理结果",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NativeDesignTokens.textPrimary,
                        )
                    }

                    if (outputText.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = {
                                clipboard?.setPrimaryClip(
                                    ClipData.newPlainText("transformed", outputText),
                                )
                                Toast.makeText(
                                    context,
                                    "已复制结果至剪贴板",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 10.dp,
                                vertical = 4.dp,
                            ),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = NativeDesignTokens.accentPrimary,
                                contentColor = NativeDesignTokens.bgDark,
                            ),
                            shape = RoundedCornerShape(NativeDesignTokens.radiusSm),
                            modifier = Modifier.height(28.dp),
                        ) {
                            Icon(
                                Icons.Rounded.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("一键复制", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(NativeDesignTokens.radiusSm))
                        .background(NativeDesignTokens.cardDark)
                        .padding(12.dp),
                ) {
                    Text(
                        text = if (outputText.isNotEmpty()) outputText else "处理结果将在此实时呈现...",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (outputText.isNotEmpty()) {
                            NativeDesignTokens.textPrimary
                        } else {
                            NativeDesignTokens.textMuted
                        },
                        lineHeight = 18.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(NativeDesignTokens.spacingXl))
        }
    }

@Composable
fun ModeCard(
    mode: TransformMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(NativeDesignTokens.radiusMd))
            .background(if (isSelected) NativeDesignTokens.surfaceDark else NativeDesignTokens.cardDark)
            .border(
                1.dp,
                if (isSelected) NativeDesignTokens.accentPrimary else NativeDesignTokens.borderDark,
                RoundedCornerShape(NativeDesignTokens.radiusMd),
            )
            .clickable { onClick() }
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(NativeDesignTokens.radiusSm))
                    .background(
                        if (isSelected) {
                            NativeDesignTokens.accentPrimary.copy(alpha = 0.15f)
                        } else {
                            NativeDesignTokens.surfaceDark
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = mode.icon,
                    contentDescription = null,
                    tint = if (isSelected) {
                        NativeDesignTokens.accentPrimary
                    } else {
                        NativeDesignTokens.textSecondary
                    },
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = mode.title,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) {
                        NativeDesignTokens.textPrimary
                    } else {
                        NativeDesignTokens.textSecondary
                    },
                )
                Text(
                    text = mode.desc,
                    fontSize = 9.sp,
                    color = NativeDesignTokens.textMuted,
                    maxLines = 1,
                )
            }
        }
    }
}
