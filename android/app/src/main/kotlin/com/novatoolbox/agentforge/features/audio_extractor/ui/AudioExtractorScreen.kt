package com.novatoolbox.agentforge.features.audio_extractor.ui

import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.novatoolbox.agentforge.features.audio_extractor.data.AudioDemuxEngine
import com.novatoolbox.agentforge.features.audio_extractor.data.VideoAudioMetadata
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AudioExtractorScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var metaData by remember { mutableStateOf<VideoAudioMetadata?>(null) }
    var isProbing by remember { mutableStateOf(false) }

    var isExtracting by remember { mutableStateOf(false) }
    var extractProgress by remember { mutableStateOf(0f) }
    var extractedAudioFile by remember { mutableStateOf<File?>(null) }

    var isSaving by remember { mutableStateOf(false) }
    var exportedPath by remember { mutableStateOf<String?>(null) }

    // 本地原生播放器
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableStateOf(0) }
    var totalDurationMs by remember { mutableStateOf(0) }

    fun releasePlayer() {
        mediaPlayer?.runCatching {
            stop()
            release()
        }
        mediaPlayer = null
        isPlaying = false
        currentPositionMs = 0
        totalDurationMs = 0
    }

    DisposableEffect(Unit) {
        onDispose { releasePlayer() }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying && isActive) {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    currentPositionMs = player.currentPosition
                }
            }
            delay(150L)
        }
    }

    fun playLocalAudio(file: File) {
        releasePlayer()
        try {
            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnPreparedListener { mp ->
                    totalDurationMs = mp.duration
                    mp.start()
                    isPlaying = true
                }
                setOnCompletionListener {
                    isPlaying = false
                    currentPositionMs = 0
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Toast.makeText(context, "加载音频播放失败: ${e.message?.take(40)}", Toast.LENGTH_SHORT).show()
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedVideoUri = uri
            extractedAudioFile = null
            exportedPath = null
            releasePlayer()
            isProbing = true
            scope.launch {
                val result = AudioDemuxEngine.probeVideoAudio(context, uri)
                isProbing = false
                result.onSuccess { meta ->
                    metaData = meta
                }.onFailure { err ->
                    metaData = null
                    Toast.makeText(context, "解析失败: ${err.message?.take(40)}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NativeDesignTokens.bgDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = NativeDesignTokens.spacingMd, vertical = NativeDesignTokens.spacingSm)
    ) {
        // 1. 本地视频选择卡片
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NativeDesignTokens.radiusMd))
                .background(NativeDesignTokens.surfaceDark)
                .border(1.dp, NativeDesignTokens.borderDark, RoundedCornerShape(NativeDesignTokens.radiusMd))
                .padding(NativeDesignTokens.spacingMd)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.VideoFile,
                        contentDescription = null,
                        tint = NativeDesignTokens.accentPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "本地视频文件",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NativeDesignTokens.textPrimary
                    )
                }

                Button(
                    onClick = { videoPickerLauncher.launch("video/*") },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NativeDesignTokens.cardDark,
                        contentColor = NativeDesignTokens.accentPrimary
                    ),
                    shape = RoundedCornerShape(NativeDesignTokens.radiusSm),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(if (selectedVideoUri == null) "选择视频" else "重新选择", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (selectedVideoUri == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp)
                        .clip(RoundedCornerShape(NativeDesignTokens.radiusSm))
                        .background(NativeDesignTokens.cardDark)
                        .border(1.dp, NativeDesignTokens.borderDark, RoundedCornerShape(NativeDesignTokens.radiusSm))
                        .clickable { videoPickerLauncher.launch("video/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.FileUpload, contentDescription = null, tint = NativeDesignTokens.textMuted, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("点击选取本地视频 (.mp4 / .mkv / .mov)", fontSize = 11.sp, color = NativeDesignTokens.textMuted)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(NativeDesignTokens.radiusSm))
                        .background(NativeDesignTokens.cardDark)
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = metaData?.fileName ?: "解析中...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NativeDesignTokens.textPrimary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "文件大小: ${metaData?.fileSizeFormatted ?: "--"} · 时长: ${(metaData?.durationMs ?: 0L) / 1000}s",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NativeDesignTokens.textSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(NativeDesignTokens.spacingMd))

        // 2. 物理音轨流硬件参数监视
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NativeDesignTokens.radiusMd))
                .background(NativeDesignTokens.surfaceDark)
                .border(1.dp, NativeDesignTokens.borderDark, RoundedCornerShape(NativeDesignTokens.radiusMd))
                .padding(NativeDesignTokens.spacingMd)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = NativeDesignTokens.accentPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "物理音轨参数",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NativeDesignTokens.textPrimary
                    )
                }

                Text(
                    text = "100% 原始位流",
                    fontSize = 10.sp,
                    color = NativeDesignTokens.accentPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isProbing) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NativeDesignTokens.accentPrimary, strokeWidth = 2.dp)
                }
            } else if (metaData != null) {
                val meta = metaData!!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(NativeDesignTokens.radiusSm))
                            .background(NativeDesignTokens.cardDark)
                            .padding(8.dp)
                    ) {
                        Text("编码格式", fontSize = 10.sp, color = NativeDesignTokens.textMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = meta.audioMime.removePrefix("audio/").uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = NativeDesignTokens.textPrimary
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(NativeDesignTokens.radiusSm))
                            .background(NativeDesignTokens.cardDark)
                            .padding(8.dp)
                    ) {
                        Text("采样率", fontSize = 10.sp, color = NativeDesignTokens.textMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${meta.sampleRate} Hz",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = NativeDesignTokens.textPrimary
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(NativeDesignTokens.radiusSm))
                            .background(NativeDesignTokens.cardDark)
                            .padding(8.dp)
                    ) {
                        Text("声道布局", fontSize = 10.sp, color = NativeDesignTokens.textMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (meta.channelCount == 2) "双声道" else "${meta.channelCount} 声道",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NativeDesignTokens.textPrimary
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("请选取视频以分析原生音频流", fontSize = 11.sp, color = NativeDesignTokens.textMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(NativeDesignTokens.spacingMd))

        // 3. 提取执行按钮
        if (extractedAudioFile == null) {
            Button(
                onClick = {
                    val uri = selectedVideoUri ?: return@Button
                    isExtracting = true
                    extractProgress = 0f
                    exportedPath = null
                    releasePlayer()

                    scope.launch {
                        val tempAudio = File(context.cacheDir, "ready_lossless_${System.currentTimeMillis()}.m4a")
                        val result = AudioDemuxEngine.extractLosslessAudioToFile(context, uri, tempAudio) { prog ->
                            extractProgress = prog
                        }
                        isExtracting = false
                        result.onSuccess { file ->
                            extractedAudioFile = file
                            playLocalAudio(file)
                        }.onFailure { err ->
                            Toast.makeText(context, "提取失败: ${err.message?.take(40)}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                enabled = selectedVideoUri != null && metaData != null && !isExtracting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NativeDesignTokens.accentPrimary,
                    disabledContainerColor = NativeDesignTokens.cardDark
                ),
                shape = RoundedCornerShape(NativeDesignTokens.radiusSm),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                if (isExtracting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NativeDesignTokens.bgDark, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("物理无损抽取中 (${(extractProgress * 100).toInt()}%)...", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NativeDesignTokens.bgDark)
                } else {
                    Icon(Icons.Rounded.Bolt, contentDescription = null, tint = NativeDesignTokens.bgDark, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("一键物理无损分离", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NativeDesignTokens.bgDark)
                }
            }
        }

        // 4. 抽取就绪后的内置播放器卡片
        if (extractedAudioFile != null) {
            val audioFile = extractedAudioFile!!

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(NativeDesignTokens.radiusMd))
                    .background(NativeDesignTokens.surfaceDark)
                    .border(1.dp, NativeDesignTokens.accentPrimary.copy(alpha = 0.5f), RoundedCornerShape(NativeDesignTokens.radiusMd))
                    .padding(NativeDesignTokens.spacingMd)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Headphones, contentDescription = null, tint = NativeDesignTokens.accentPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("无损音轨就绪", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NativeDesignTokens.textPrimary)
                    }
                    Text(
                        text = if (isPlaying) "正在播放" else "已暂停",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = NativeDesignTokens.accentPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = metaData?.fileName ?: "抽取音频",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NativeDesignTokens.textPrimary,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            mediaPlayer?.let { player ->
                                if (player.isPlaying) {
                                    player.pause()
                                    isPlaying = false
                                } else {
                                    player.start()
                                    isPlaying = true
                                }
                            } ?: playLocalAudio(audioFile)
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(NativeDesignTokens.accentPrimary)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = NativeDesignTokens.bgDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Slider(
                            value = if (totalDurationMs > 0) currentPositionMs.toFloat() else 0f,
                            onValueChange = { newPos ->
                                currentPositionMs = newPos.toInt()
                                mediaPlayer?.seekTo(newPos.toInt())
                            },
                            valueRange = 0f..(totalDurationMs.toFloat().coerceAtLeast(1f)),
                            modifier = Modifier.height(20.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = NativeDesignTokens.accentPrimary,
                                activeTrackColor = NativeDesignTokens.accentPrimary,
                                inactiveTrackColor = NativeDesignTokens.borderDark
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val curSec = currentPositionMs / 1000
                            val totalSec = totalDurationMs / 1000
                            Text(
                                text = String.format("%02d:%02d", curSec / 60, curSec % 60),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = NativeDesignTokens.textSecondary
                            )
                            Text(
                                text = String.format("%02d:%02d", totalSec / 60, totalSec % 60),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = NativeDesignTokens.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(NativeDesignTokens.spacingMd))

            // 保存入库按钮
            Button(
                onClick = {
                    isSaving = true
                    exportedPath = null
                    scope.launch {
                        val baseName = metaData?.fileName?.substringBeforeLast(".") ?: "Audio"
                        val result = AudioDemuxEngine.saveAudioToMusicStore(context, audioFile, baseName)
                        isSaving = false
                        result.onSuccess { path ->
                            exportedPath = path
                            Toast.makeText(context, "已成功存入系统音乐库！", Toast.LENGTH_SHORT).show()
                        }.onFailure { err ->
                            Toast.makeText(context, "保存失败: ${err.message?.take(40)}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NativeDesignTokens.accentPrimary,
                    disabledContainerColor = NativeDesignTokens.cardDark
                ),
                shape = RoundedCornerShape(NativeDesignTokens.radiusSm),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NativeDesignTokens.bgDark, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("正在存入媒体库...", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NativeDesignTokens.bgDark)
                } else {
                    Icon(Icons.Rounded.Download, contentDescription = null, tint = NativeDesignTokens.bgDark, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("保存至系统音乐库 (Music/NovaToolBox)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NativeDesignTokens.bgDark)
                }
            }
        }

        // 5. 导出成功展示
        if (exportedPath != null) {
            Spacer(modifier = Modifier.height(NativeDesignTokens.spacingMd))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(NativeDesignTokens.radiusMd))
                    .background(NativeDesignTokens.surfaceDark)
                    .border(1.dp, NativeDesignTokens.accentPrimary.copy(alpha = 0.6f), RoundedCornerShape(NativeDesignTokens.radiusMd))
                    .padding(NativeDesignTokens.spacingMd)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = NativeDesignTokens.accentPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("音频已无损存入系统音乐库", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NativeDesignTokens.textPrimary)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "保存路径：$exportedPath",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = NativeDesignTokens.textSecondary,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(NativeDesignTokens.spacingXl))
    }
}
