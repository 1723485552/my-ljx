package com.novatoolbox.agentforge.features.mockup_studio.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novatoolbox.agentforge.core.theme.NativeDesignTokens
import com.novatoolbox.agentforge.features.mockup_studio.data.MockupBackground
import com.novatoolbox.agentforge.features.mockup_studio.data.MockupFrameType
import com.novatoolbox.agentforge.features.mockup_studio.data.MockupParams
import com.novatoolbox.agentforge.features.mockup_studio.data.MockupRatio
import com.novatoolbox.agentforge.features.mockup_studio.data.MockupRenderer
import com.novatoolbox.agentforge.features.mockup_studio.data.MockupStudioExporter
import kotlinx.coroutines.launch

@Composable
fun MockupStudioScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var dominantColor by remember { mutableStateOf(AndroidColor.parseColor("#38BDF8")) }

    // 核心渲染参数
    var pitch by remember { mutableStateOf(10f) }
    var yaw by remember { mutableStateOf(-14f) }
    var roll by remember { mutableStateOf(0f) }
    var scale by remember { mutableStateOf(0.95f) }
    var shadowIntensity by remember { mutableStateOf(0.65f) }
    var enableGlassReflection by remember { mutableStateOf(true) }

    var selectedFrameType by remember { mutableStateOf(MockupFrameType.IPHONE_16_PRO) }
    var selectedRatio by remember { mutableStateOf(MockupRatio.XIAOHONGSHU) }
    var selectedBackground by remember { mutableStateOf(MockupBackground.AMBIENT_GLOW) }

    var isExporting by remember { mutableStateOf(false) }
    var exportedPath by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            exportedPath = null
            scope.launch {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream)
                        loadedBitmap = bmp
                        dominantColor = MockupRenderer.extractDominantColor(bmp)
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "加载图片失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val currentParams = MockupParams(
        pitch = pitch,
        yaw = yaw,
        roll = roll,
        scale = scale,
        shadowIntensity = shadowIntensity,
        enableGlassReflection = enableGlassReflection,
        frameType = selectedFrameType,
        ratio = selectedRatio,
        background = selectedBackground
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NativeDesignTokens.bgDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = NativeDesignTokens.spacingMd, vertical = NativeDesignTokens.spacingSm)
    ) {
        // 1. 100% 同构真机视口
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NativeDesignTokens.radiusMd))
                .background(NativeDesignTokens.surfaceDark)
                .border(1.dp, NativeDesignTokens.borderDark, RoundedCornerShape(NativeDesignTokens.radiusMd))
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.GridView, contentDescription = null, tint = NativeDesignTokens.accentPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("100% 同构真机渲染视口", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NativeDesignTokens.textPrimary)
                }

                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NativeDesignTokens.cardDark,
                        contentColor = NativeDesignTokens.accentPrimary
                    ),
                    shape = RoundedCornerShape(NativeDesignTokens.radiusSm),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text(if (selectedImageUri == null) "选取截图" else "更换截图", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp, max = 300.dp)
                    .clip(RoundedCornerShape(NativeDesignTokens.radiusSm))
                    .background(Color(0xFF09090B))
                    .border(1.dp, NativeDesignTokens.borderDark, RoundedCornerShape(NativeDesignTokens.radiusSm)),
                contentAlignment = Alignment.Center
            ) {
                if (loadedBitmap == null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { imagePickerLauncher.launch("image/*") }
                    ) {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, tint = NativeDesignTokens.textMuted, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("点击选取截图，呈现真机硬件透视", fontSize = 11.sp, color = NativeDesignTokens.textMuted)
                    }
                } else {
                    Canvas(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(selectedRatio.widthRatio / selectedRatio.heightRatio)
                    ) {
                        val nativeCanvas = drawContext.canvas.nativeCanvas
                        MockupRenderer.render(
                            canvas = nativeCanvas,
                            width = size.width,
                            height = size.height,
                            sourceBitmap = loadedBitmap,
                            params = currentParams,
                            dominantColor = dominantColor
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(NativeDesignTokens.spacingMd))

        // 2. 5 大真机硬件形态选择
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NativeDesignTokens.radiusMd))
                .background(NativeDesignTokens.surfaceDark)
                .border(1.dp, NativeDesignTokens.borderDark, RoundedCornerShape(NativeDesignTokens.radiusMd))
                .padding(10.dp)
        ) {
            Text("真机硬件型号选择", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NativeDesignTokens.textPrimary)

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                MockupFrameType.values().forEach { ft ->
                    val isSelected = selectedFrameType == ft
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) NativeDesignTokens.accentPrimary else NativeDesignTokens.cardDark)
                            .clickable { selectedFrameType = ft }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ft.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) NativeDesignTokens.bgDark else NativeDesignTokens.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text("社交画幅比例", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NativeDesignTokens.textPrimary)

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MockupRatio.values().forEach { r ->
                    val isSelected = selectedRatio == r
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) NativeDesignTokens.accentPrimary else NativeDesignTokens.cardDark)
                            .clickable { selectedRatio = r }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = r.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) NativeDesignTokens.bgDark else NativeDesignTokens.textSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(NativeDesignTokens.spacingMd))

        // 3. 通透光影与镜面反光控制
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NativeDesignTokens.radiusMd))
                .background(NativeDesignTokens.surfaceDark)
                .border(1.dp, NativeDesignTokens.borderDark, RoundedCornerShape(NativeDesignTokens.radiusMd))
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("通透光影方案", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NativeDesignTokens.textPrimary)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { enableGlassReflection = !enableGlassReflection }
                ) {
                    Text(
                        text = if (enableGlassReflection) "镜面高光: 开" else "镜面高光: 关",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (enableGlassReflection) NativeDesignTokens.accentPrimary else NativeDesignTokens.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                MockupBackground.values().forEach { bg ->
                    val isSelected = selectedBackground == bg
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) NativeDesignTokens.accentPrimary else NativeDesignTokens.cardDark)
                            .clickable { selectedBackground = bg }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = bg.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) NativeDesignTokens.bgDark else NativeDesignTokens.textSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(NativeDesignTokens.spacingMd))

        // 4. 经典 3D 姿态预设
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NativeDesignTokens.radiusMd))
                .background(NativeDesignTokens.surfaceDark)
                .border(1.dp, NativeDesignTokens.borderDark, RoundedCornerShape(NativeDesignTokens.radiusMd))
                .padding(10.dp)
        ) {
            Text("经典 3D 姿态预设", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NativeDesignTokens.textPrimary)

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    Triple("平面极简", Triple(0f, 0f, 0f), 0.95f),
                    Triple("苹果发布会", Triple(12f, -15f, 3f), 0.95f),
                    Triple("等轴透视", Triple(20f, 22f, -8f), 0.90f),
                    Triple("悬浮破空", Triple(-10f, 14f, 3f), 1.05f)
                ).forEach { (name, angles, s) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(NativeDesignTokens.cardDark)
                            .clickable {
                                pitch = angles.first
                                yaw = angles.second
                                roll = angles.third
                                scale = s
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NativeDesignTokens.textSecondary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(NativeDesignTokens.spacingMd))

        // 5. 3D 物理参数精细微调
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NativeDesignTokens.radiusMd))
                .background(NativeDesignTokens.surfaceDark)
                .border(1.dp, NativeDesignTokens.borderDark, RoundedCornerShape(NativeDesignTokens.radiusMd))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text("物理透视与阴影微调", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NativeDesignTokens.textPrimary)

            Spacer(modifier = Modifier.height(6.dp))

            // 俯仰 (Pitch)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("俯仰倾斜 (Pitch)", fontSize = 11.sp, color = NativeDesignTokens.textSecondary)
                Text("${pitch.toInt()}°", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = NativeDesignTokens.accentPrimary)
            }
            Slider(
                value = pitch,
                onValueChange = { pitch = it },
                valueRange = -30f..30f,
                modifier = Modifier.height(24.dp),
                colors = SliderDefaults.colors(thumbColor = NativeDesignTokens.accentPrimary, activeTrackColor = NativeDesignTokens.accentPrimary, inactiveTrackColor = NativeDesignTokens.borderDark)
            )

            // 偏航 (Yaw)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("偏航朝向 (Yaw)", fontSize = 11.sp, color = NativeDesignTokens.textSecondary)
                Text("${yaw.toInt()}°", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = NativeDesignTokens.accentPrimary)
            }
            Slider(
                value = yaw,
                onValueChange = { yaw = it },
                valueRange = -30f..30f,
                modifier = Modifier.height(24.dp),
                colors = SliderDefaults.colors(thumbColor = NativeDesignTokens.accentPrimary, activeTrackColor = NativeDesignTokens.accentPrimary, inactiveTrackColor = NativeDesignTokens.borderDark)
            )

            // 阴影深度
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("3D 悬浮软阴影", fontSize = 11.sp, color = NativeDesignTokens.textSecondary)
                Text("${(shadowIntensity * 100).toInt()}%", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = NativeDesignTokens.accentPrimary)
            }
            Slider(
                value = shadowIntensity,
                onValueChange = { shadowIntensity = it },
                valueRange = 0f..1f,
                modifier = Modifier.height(24.dp),
                colors = SliderDefaults.colors(thumbColor = NativeDesignTokens.accentPrimary, activeTrackColor = NativeDesignTokens.accentPrimary, inactiveTrackColor = NativeDesignTokens.borderDark)
            )
        }

        Spacer(modifier = Modifier.height(NativeDesignTokens.spacingMd))

        // 6. 导出路径展示
        if (exportedPath != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(NativeDesignTokens.radiusMd))
                    .background(NativeDesignTokens.surfaceDark)
                    .border(1.dp, NativeDesignTokens.accentPrimary.copy(alpha = 0.6f), RoundedCornerShape(NativeDesignTokens.radiusMd))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = NativeDesignTokens.accentPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("100% 同构 4K 超清图已保存", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NativeDesignTokens.textPrimary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "相册路径：$exportedPath", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = NativeDesignTokens.textSecondary)
            }
            Spacer(modifier = Modifier.height(NativeDesignTokens.spacingMd))
        }

        // 7. 一键无损 4K 导出
        Button(
            onClick = {
                val uri = selectedImageUri
                if (uri == null) {
                    Toast.makeText(context, "请先选取一张截图", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isExporting = true
                exportedPath = null

                scope.launch {
                    val result = MockupStudioExporter.exportMockupToGallery(context, uri, currentParams)
                    isExporting = false
                    result.onSuccess { path ->
                        exportedPath = path
                        Toast.makeText(context, "4K 渲染图已成功导出至相册！", Toast.LENGTH_SHORT).show()
                    }.onFailure { err ->
                        Toast.makeText(context, "渲染失败: ${err.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            enabled = selectedImageUri != null && !isExporting,
            colors = ButtonDefaults.buttonColors(
                containerColor = NativeDesignTokens.accentPrimary,
                disabledContainerColor = NativeDesignTokens.cardDark
            ),
            shape = RoundedCornerShape(NativeDesignTokens.radiusSm),
            modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
            if (isExporting) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NativeDesignTokens.bgDark, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("正在执行 4K 真机光影渲染...", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NativeDesignTokens.bgDark)
            } else {
                Icon(Icons.Rounded.Camera, contentDescription = null, tint = NativeDesignTokens.bgDark, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("导出 4K 极客带壳渲染图", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NativeDesignTokens.bgDark)
            }
        }

        Spacer(modifier = Modifier.height(NativeDesignTokens.spacingXl))
    }
}
