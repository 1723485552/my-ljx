package com.novatoolbox.agentforge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novatoolbox.agentforge.core.model.ToolItem
import com.novatoolbox.agentforge.core.theme.NativeDesignTokens

@Composable
fun HomeScreen(
    tools: List<ToolItem>,
    onToolClick: (ToolItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NativeDesignTokens.bgDark)
            .padding(horizontal = NativeDesignTokens.spacingMd),
    ) {
        Spacer(modifier = Modifier.height(NativeDesignTokens.spacingLg))
        Text(
            text = "NovaToolBox",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = NativeDesignTokens.textPrimary,
        )
        Text(
            text = "极简工业级多维原生工具箱",
            fontSize = 12.sp,
            color = NativeDesignTokens.textSecondary,
        )
        Spacer(modifier = Modifier.height(NativeDesignTokens.spacingMd))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(NativeDesignTokens.spacingSm),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(tools) { tool ->
                ToolCard(tool = tool, onClick = { onToolClick(tool) })
            }
            item {
                Spacer(modifier = Modifier.height(NativeDesignTokens.spacingXl))
            }
        }
    }
}

@Composable
fun ToolCard(
    tool: ToolItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NativeDesignTokens.radiusMd))
            .background(NativeDesignTokens.surfaceDark)
            .border(
                1.dp,
                NativeDesignTokens.borderDark,
                RoundedCornerShape(NativeDesignTokens.radiusMd),
            )
            .clickable { onClick() }
            .padding(NativeDesignTokens.spacingMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(NativeDesignTokens.radiusSm))
                .background(NativeDesignTokens.cardDark),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = tool.name,
                tint = NativeDesignTokens.accentPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.width(NativeDesignTokens.spacingMd))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tool.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = NativeDesignTokens.textPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = tool.description,
                fontSize = 12.sp,
                color = NativeDesignTokens.textSecondary,
                maxLines = 1,
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = NativeDesignTokens.textMuted,
            modifier = Modifier.size(20.dp),
        )
    }
}
