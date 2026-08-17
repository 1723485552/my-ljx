package com.novatoolbox.agentforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarViewMonth
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Transform
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novatoolbox.agentforge.core.model.ToolItem
import com.novatoolbox.agentforge.core.theme.NativeDesignTokens
import com.novatoolbox.agentforge.features.mood_heatmap.ui.MoodHeatmapScreen
import com.novatoolbox.agentforge.features.screenshot.ui.TemporaryMemoryScreen
import com.novatoolbox.agentforge.features.text_tools.ui.TextReverserScreen
import com.novatoolbox.agentforge.ui.screens.CategoryScreen
import com.novatoolbox.agentforge.ui.screens.HomeScreen
import com.novatoolbox.agentforge.ui.screens.SettingsScreen

enum class MainTab(val label: String, val icon: ImageVector) {
    HOME("首页", Icons.Rounded.Home),
    CATEGORY("分类", Icons.Rounded.GridView),
    MINE("我的", Icons.Rounded.Person),
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val toolRegistry = listOf(
            ToolItem(
                id = "temporary_memory",
                name = "瞬时暂存器",
                description = "截屏自动装填、悬浮倒计时与相册安全自毁",
                category = "常用与效率",
                icon = Icons.Rounded.HourglassBottom,
                screen = { onBack ->
                    ToolRunnerScaffold(title = "瞬时暂存器", onBack = onBack) {
                        TemporaryMemoryScreen()
                    }
                },
            ),
            ToolItem(
                id = "mood_heatmap",
                name = "365 情绪热力图",
                description = "GitHub 风格年度心绪像素方阵与海报导出",
                category = "生活与记录",
                icon = Icons.Rounded.CalendarViewMonth,
                screen = { onBack ->
                    ToolRunnerScaffold(title = "365 情绪热力图", onBack = onBack) {
                        MoodHeatmapScreen()
                    }
                },
            ),
            ToolItem(
                id = "text_reverser",
                name = "文本反转工坊",
                description = "即时文本倒序、字母大小写与去噪转换",
                category = "常用与效率",
                icon = Icons.Rounded.Transform,
                screen = { onBack ->
                    ToolRunnerScaffold(title = "文本反转工坊", onBack = onBack) {
                        TextReverserScreen()
                    }
                },
            ),
        )

        setContent {
            var selectedTab by remember { mutableStateOf(MainTab.HOME) }
            var activeTool by remember { mutableStateOf<ToolItem?>(null) }

            // 拦截系统返回键：工具容器内优先返回工具列表
            BackHandler(enabled = activeTool != null) {
                activeTool = null
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = NativeDesignTokens.bgDark,
            ) {
                if (activeTool != null) {
                    activeTool!!.screen { activeTool = null }
                } else {
                    Scaffold(
                        bottomBar = {
                            NavigationBar(
                                containerColor = NativeDesignTokens.surfaceDark,
                                tonalElevation = 0.dp,
                            ) {
                                MainTab.values().forEach { tab ->
                                    NavigationBarItem(
                                        selected = selectedTab == tab,
                                        onClick = { selectedTab = tab },
                                        icon = {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.label,
                                            )
                                        },
                                        label = { Text(text = tab.label, fontSize = 11.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = NativeDesignTokens.bgDark,
                                            selectedTextColor = NativeDesignTokens.textPrimary,
                                            indicatorColor = NativeDesignTokens.accentPrimary,
                                            unselectedIconColor = NativeDesignTokens.textMuted,
                                            unselectedTextColor = NativeDesignTokens.textMuted,
                                        ),
                                    )
                                }
                            }
                        },
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            when (selectedTab) {
                                MainTab.HOME -> HomeScreen(tools = toolRegistry) { activeTool = it }
                                MainTab.CATEGORY -> CategoryScreen(tools = toolRegistry) { activeTool = it }
                                MainTab.MINE -> SettingsScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolRunnerScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        containerColor = NativeDesignTokens.bgDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NativeDesignTokens.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "返回",
                            tint = NativeDesignTokens.textPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NativeDesignTokens.bgDark,
                    titleContentColor = NativeDesignTokens.textPrimary,
                    navigationIconContentColor = NativeDesignTokens.textPrimary,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NativeDesignTokens.bgDark)
                .padding(padding),
        ) {
            content()
        }
    }
}
