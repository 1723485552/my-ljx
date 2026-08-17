package com.novatoolbox.agentforge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novatoolbox.agentforge.core.theme.NativeDesignTokens

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NativeDesignTokens.bgDark)
            .padding(horizontal = NativeDesignTokens.spacingMd),
    ) {
        Spacer(modifier = Modifier.height(NativeDesignTokens.spacingLg))
        Text(
            text = "个人与设置",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = NativeDesignTokens.textPrimary,
        )
        Spacer(modifier = Modifier.height(NativeDesignTokens.spacingMd))

        // 架构与版本卡片
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = NativeDesignTokens.accentPrimary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(NativeDesignTokens.spacingSm))
                Text(
                    text = "应用架构信息",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NativeDesignTokens.textPrimary,
                )
            }
            Spacer(modifier = Modifier.height(NativeDesignTokens.spacingSm))
            Text(text = "架构方案：纯原生 Kotlin + Jetpack Compose", fontSize = 12.sp, color = NativeDesignTokens.textSecondary)
            Text(text = "版本状态：1.0.0 (Clean Architecture)", fontSize = 12.sp, color = NativeDesignTokens.textSecondary)
            Text(text = "数据特性：纯内存驻留 / 零外部网络追踪", fontSize = 12.sp, color = NativeDesignTokens.textSecondary)
        }

        Spacer(modifier = Modifier.height(NativeDesignTokens.spacingMd))

        // 安全合规声明卡片
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
                    tint = NativeDesignTokens.accentSuccess,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(NativeDesignTokens.spacingSm))
                Text(
                    text = "隐私与安全边界",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NativeDesignTokens.textPrimary,
                )
            }
            Spacer(modifier = Modifier.height(NativeDesignTokens.spacingSm))
            Text(text = "• MediaUriValidator 强校验相册合法 URI", fontSize = 12.sp, color = NativeDesignTokens.textSecondary)
            Text(text = "• 悬浮窗 SYSTEM_ALERT_WINDOW 前置授权说明", fontSize = 12.sp, color = NativeDesignTokens.textSecondary)
        }
    }
}
