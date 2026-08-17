package com.novatoolbox.agentforge.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 原生端设计令牌 (Native Design Tokens)
 *
 * 集中管理颜色、间距、圆角、物理尺寸，禁止业务/UI 层出现无语义硬编码。
 */
object NativeDesignTokens {
    // 间距
    val spacingXs = 4.dp
    val spacingSm = 8.dp
    val spacingMd = 16.dp
    val spacingLg = 24.dp
    val spacingXl = 32.dp

    // 圆角
    val radiusSm = 4.dp
    val radiusMd = 12.dp
    val radiusLg = 16.dp
    val radiusCapsule = 24.dp

    // 悬浮层级与物理尺寸
    val elevationOverlay = 16.dp
    val floatingCapsuleWidth = 240.dp
    val floatingCapsuleHeight = 56.dp

    // 极简工业深色调色板
    val bgDark = Color(0xFF09090B)
    val surfaceDark = Color(0xFF18181B)
    val cardDark = Color(0xFF101012)
    val borderDark = Color(0xFF27272A)
    val textPrimary = Color(0xFFF4F4F5)
    val textSecondary = Color(0xFFA1A1AA)
    val textMuted = Color(0xFF71717A)

    // 功能高亮色
    val accentPrimary = Color(0xFF38BDF8)
    val accentSuccess = Color(0xFF10B981)
    val accentWarning = Color(0xFFFBBF24)
    val accentDanger = Color(0xFFEF4444)
    val accentPurple = Color(0xFFA855F7)
}
