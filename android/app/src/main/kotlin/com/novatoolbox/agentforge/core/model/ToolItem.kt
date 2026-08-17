package com.novatoolbox.agentforge.core.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 工具元数据与注册表项。
 *
 * 所有独立工具（瞬时暂存器、365 情绪热力图、文本工坊）以卡片形式在首页/分类页展示，
 * 点击进入全屏工具容器 [screen]，通过 [onBack] 回调支持顶部返回与系统返回键拦截。
 */
data class ToolItem(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val icon: ImageVector,
    val screen: @Composable (onBack: () -> Unit) -> Unit,
)
