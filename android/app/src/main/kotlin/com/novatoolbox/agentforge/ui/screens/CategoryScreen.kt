package com.novatoolbox.agentforge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.novatoolbox.agentforge.core.model.ToolItem
import com.novatoolbox.agentforge.core.theme.NativeDesignTokens

@Composable
fun CategoryScreen(
    tools: List<ToolItem>,
    onToolClick: (ToolItem) -> Unit,
) {
    val grouped = tools.groupBy { it.category }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NativeDesignTokens.bgDark)
            .padding(horizontal = NativeDesignTokens.spacingMd),
    ) {
        Spacer(modifier = Modifier.height(NativeDesignTokens.spacingLg))
        Text(
            text = "工具分类",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = NativeDesignTokens.textPrimary,
        )
        Spacer(modifier = Modifier.height(NativeDesignTokens.spacingMd))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(NativeDesignTokens.spacingMd),
            modifier = Modifier.fillMaxSize(),
        ) {
            grouped.forEach { (categoryName, categoryTools) ->
                item {
                    Text(
                        text = categoryName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NativeDesignTokens.accentPrimary,
                        modifier = Modifier.padding(vertical = NativeDesignTokens.spacingXs),
                    )
                }
                items(categoryTools) { tool ->
                    ToolCard(tool = tool, onClick = { onToolClick(tool) })
                }
            }
            item {
                Spacer(modifier = Modifier.height(NativeDesignTokens.spacingXl))
            }
        }
    }
}
