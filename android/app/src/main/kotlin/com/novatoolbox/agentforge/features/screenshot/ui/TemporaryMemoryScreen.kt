package com.novatoolbox.agentforge.features.screenshot.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.novatoolbox.agentforge.core.config.TemporaryMemoryConfig
import com.novatoolbox.agentforge.core.theme.NativeDesignTokens
import com.novatoolbox.agentforge.features.screenshot.data.MediaStoreScreenshotRepository
import com.novatoolbox.agentforge.features.screenshot.data.ScreenshotWatcherService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 瞬时暂存器（Temporary Memory Hub）—— Compose 版。
 *
 * 升级要点：
 * - 灵动微胶囊（高度 38dp）视觉无感，自毁时长 3~60s 自由配置，即改即生效。
 * - 倒计时归零 100% 静默删除（后台直删，0 弹窗 0 跳转）。
 * - 全品牌截屏识别 + 相册动态权限 + 悬浮窗前置授权说明。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemporaryMemoryScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { MediaStoreScreenshotRepository(context.contentResolver) }

    var isServiceRunning by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var currentUri by remember { mutableStateOf<Uri?>(null) }

    var customSeconds by remember {
        mutableStateOf(TemporaryMemoryConfig.getCountdownDuration(context))
    }
    val presetDurations = listOf(3, 5, 10, 15, 30, 60)

    var hasManageStorage by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                true
            },
        )
    }

    // 拉起前台截屏监听服务（兼容 Android 8+ 前台服务约束）
    fun startWatcher(ctx: Context) {
        val intent = Intent(ctx, ScreenshotWatcherService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(intent)
        } else {
            ctx.startService(intent)
        }
    }

    val manageStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        hasManageStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    val overlayLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        if (Settings.canDrawOverlays(context)) {
            isServiceRunning = true
            startWatcher(context)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        currentUri = repository.queryLatestScreenshot()
    }

    LaunchedEffect(Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        }
        val isGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!isGranted) {
            permissionLauncher.launch(permissions)
        } else {
            currentUri = repository.queryLatestScreenshot()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NativeDesignTokens.bgDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = NativeDesignTokens.spacingMd, vertical = NativeDesignTokens.spacingSm),
    ) {
            // 核心服务总开关卡片
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
                    .padding(NativeDesignTokens.spacingMd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "后台截屏监听与自毁",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NativeDesignTokens.textPrimary,
                    )
                    Text(
                        text = "检测截屏弹窗显示剩余 ${customSeconds}s，归零静默自焚",
                        fontSize = 12.sp,
                        color = NativeDesignTokens.textSecondary,
                    )
                }
                Switch(
                    checked = isServiceRunning,
                    onCheckedChange = { enable ->
                        if (enable) {
                            if (!Settings.canDrawOverlays(context)) {
                                showPermissionDialog = true
                            } else {
                                isServiceRunning = true
                                startWatcher(context)
                            }
                        } else {
                            isServiceRunning = false
                            context.stopService(
                                Intent(context, ScreenshotWatcherService::class.java),
                            )
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NativeDesignTokens.accentPrimary,
                        checkedTrackColor = NativeDesignTokens.surfaceDark,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(NativeDesignTokens.spacingMd))

            // 自定义自毁倒计时卡片
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
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = NativeDesignTokens.accentPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "自毁倒计时设定：${customSeconds} 秒",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NativeDesignTokens.textPrimary,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(presetDurations) { sec ->
                        val isSelected = customSeconds == sec
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(NativeDesignTokens.radiusSm))
                                .background(
                                    if (isSelected) {
                                        NativeDesignTokens.accentPrimary
                                    } else {
                                        NativeDesignTokens.cardDark
                                    },
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) {
                                        NativeDesignTokens.accentPrimary
                                    } else {
                                        NativeDesignTokens.borderDark
                                    },
                                    RoundedCornerShape(NativeDesignTokens.radiusSm),
                                )
                                .clickable {
                                    customSeconds = sec
                                    TemporaryMemoryConfig.setCountdownDuration(context, sec)
                                    Toast.makeText(
                                        context,
                                        "自毁时长已更新为 ${sec}s",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "${sec}s",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) {
                                    NativeDesignTokens.bgDark
                                } else {
                                    NativeDesignTokens.textSecondary
                                },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Slider(
                    value = customSeconds.toFloat(),
                    onValueChange = { newValue ->
                        customSeconds = newValue.toInt()
                        TemporaryMemoryConfig.setCountdownDuration(context, customSeconds)
                    },
                    valueRange = 3f..60f,
                    steps = 57,
                    colors = SliderDefaults.colors(
                        thumbColor = NativeDesignTokens.accentPrimary,
                        activeTrackColor = NativeDesignTokens.accentPrimary,
                        inactiveTrackColor = NativeDesignTokens.borderDark,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("极速 3s", fontSize = 10.sp, color = NativeDesignTokens.textSecondary)
                    Text("标准 10s", fontSize = 10.sp, color = NativeDesignTokens.textSecondary)
                    Text("充裕 60s", fontSize = 10.sp, color = NativeDesignTokens.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(NativeDesignTokens.spacingMd))

            // 免确认静默权限引导卡片（Android 11+ 未授权时）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasManageStorage) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(NativeDesignTokens.radiusMd))
                        .background(NativeDesignTokens.surfaceDark)
                        .border(
                            1.dp,
                            NativeDesignTokens.accentWarning,
                            RoundedCornerShape(NativeDesignTokens.radiusMd),
                        )
                        .padding(NativeDesignTokens.spacingMd),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = NativeDesignTokens.accentWarning,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "开启 100% 免弹窗静默删除",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NativeDesignTokens.textPrimary,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "授权后，倒计时归零时系统将 0 弹窗、0 确认、后台自动销毁截图。",
                        fontSize = 11.sp,
                        color = NativeDesignTokens.textSecondary,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            ).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            manageStorageLauncher.launch(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NativeDesignTokens.accentWarning,
                        ),
                        shape = RoundedCornerShape(NativeDesignTokens.radiusSm),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                    ) {
                        Text(
                            "立即授权静默自毁权限",
                            fontSize = 12.sp,
                            color = NativeDesignTokens.bgDark,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(NativeDesignTokens.spacingMd))
            }

            // 当前抓取状态卡片
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
                Text(
                    text = "最新抓取状态",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NativeDesignTokens.textPrimary,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = currentUri?.toString() ?: "尚未检测到最新截屏",
                    fontSize = 11.sp,
                    color = if (currentUri != null) {
                        NativeDesignTokens.accentPrimary
                    } else {
                        NativeDesignTokens.textSecondary
                    },
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { currentUri = repository.queryLatestScreenshot() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NativeDesignTokens.surfaceDark,
                        ),
                        modifier = Modifier.border(
                            1.dp,
                            NativeDesignTokens.borderDark,
                            RoundedCornerShape(NativeDesignTokens.radiusSm),
                        ),
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("手动刷新", fontSize = 12.sp)
                    }

                    if (currentUri != null) {
                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    repository.deleteScreenshot(currentUri!!)
                                    currentUri = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NativeDesignTokens.accentDanger,
                            ),
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("测试直接自毁", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                title = { Text("开启自毁胶囊需要悬浮窗权限", color = NativeDesignTokens.textPrimary) },
                text = {
                    Text(
                        "以便在检测到截屏时在顶部显示极窄倒计时微胶囊。",
                        color = NativeDesignTokens.textSecondary,
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showPermissionDialog = false
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}"),
                        )
                        overlayLauncher.launch(intent)
                    }) {
                        Text("前往授权", color = NativeDesignTokens.accentPrimary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionDialog = false }) {
                        Text("取消", color = NativeDesignTokens.textSecondary)
                    }
                },
                containerColor = NativeDesignTokens.surfaceDark,
            )
        }
}
