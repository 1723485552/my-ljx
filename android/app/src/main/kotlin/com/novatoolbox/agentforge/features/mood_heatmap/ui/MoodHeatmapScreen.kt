package com.novatoolbox.agentforge.features.mood_heatmap.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novatoolbox.agentforge.core.theme.NativeDesignTokens
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

enum class MoodLevel(
    val label: String,
    val color: Color,
) {
    EMPTY("未打卡", Color(0xFF1C1C20)),
    CALM("平静", Color(0xFF38BDF8)),       // 科技天蓝
    FULFILLED("充实", Color(0xFF10B981)),  // 翡翠绿
    HAPPY("愉悦", Color(0xFFFBBF24)),      // 暖琥珀
    ANXIOUS("焦虑", Color(0xFFA855F7)),    // 极光紫
    TIRED("低落", Color(0xFFEF4444)),      // 警示红
}

data class DayMoodRecord(
    val date: LocalDate,
    val mood: MoodLevel = MoodLevel.EMPTY,
    val note: String = "",
)

/**
 * 365 情绪热力方阵（极简易点大像素月历版）。
 *
 * 解决痛点：
 * - 7 列大方块月历，触控面积相比微型点阵提升约 400%。
 * - 高对比边框（选中白边/今日蓝边/心绪色边）与底色，告别糊成一片。
 * - 顶部 1~12 月快速翻阅，保留全年 365 天追踪能力（内存数据池）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodHeatmapScreen() {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }

    var currentYearMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var selectedDate by remember { mutableStateOf(today) }

    val moodRecords = remember {
        mutableStateMapOf<LocalDate, DayMoodRecord>().apply {
            put(today, DayMoodRecord(today, MoodLevel.FULFILLED, "完成架构与 UI 优化"))
            put(today.minusDays(1), DayMoodRecord(today.minusDays(1), MoodLevel.CALM, "梳理系统流程"))
            put(today.minusDays(2), DayMoodRecord(today.minusDays(2), MoodLevel.HAPPY, "真机测试通过"))
            put(today.minusDays(4), DayMoodRecord(today.minusDays(4), MoodLevel.ANXIOUS, "调试悬浮窗穿透"))
        }
    }

    var currentNoteInput by remember(selectedDate) {
        mutableStateOf(moodRecords[selectedDate]?.note ?: "")
    }

    val totalLoggedThisMonth = (1..currentYearMonth.lengthOfMonth()).count { day ->
        val d = currentYearMonth.atDay(day)
        moodRecords[d]?.mood != null && moodRecords[d]?.mood != MoodLevel.EMPTY
    }

    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NativeDesignTokens.bgDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = NativeDesignTokens.spacingMd, vertical = NativeDesignTokens.spacingSm),
    ) {
            // 1. 月份切换与打卡概览卡片
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { currentYearMonth = currentYearMonth.minusMonths(1) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronLeft,
                                contentDescription = "上个月",
                                tint = NativeDesignTokens.textPrimary,
                            )
                        }

                        Text(
                            text = currentYearMonth.format(DateTimeFormatter.ofPattern("yyyy年 MM月")),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NativeDesignTokens.textPrimary,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )

                        IconButton(
                            onClick = { currentYearMonth = currentYearMonth.plusMonths(1) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = "下个月",
                                tint = NativeDesignTokens.textPrimary,
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = {
                            if (isExporting) return@FilledTonalButton
                            isExporting = true
                            scope.launch {
                                Toast.makeText(context, "正在生成心绪海报...", Toast.LENGTH_SHORT).show()
                                val result = MoodPosterExporter.exportPosterToGallery(
                                    context = context,
                                    yearMonth = currentYearMonth,
                                    records = moodRecords,
                                )
                                isExporting = false
                                result.onSuccess { path ->
                                    Toast.makeText(context, "已成功保存至系统相册！\n$path", Toast.LENGTH_LONG).show()
                                }.onFailure { e ->
                                    Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isExporting,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = NativeDesignTokens.cardDark,
                            contentColor = NativeDesignTokens.accentPrimary,
                        ),
                        shape = RoundedCornerShape(NativeDesignTokens.radiusSm),
                        modifier = Modifier.height(28.dp),
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isExporting) "导出中..." else "导出", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "本月已点亮 $totalLoggedThisMonth 天 · 点击方格切换日期",
                    fontSize = 11.sp,
                    color = NativeDesignTokens.textSecondary,
                    modifier = Modifier.padding(start = 4.dp),
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MoodLevel.values().filter { it != MoodLevel.EMPTY }.forEach { level ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(level.color),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = level.label,
                                fontSize = 10.sp,
                                color = NativeDesignTokens.textSecondary,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(NativeDesignTokens.spacingMd))

            // 2. 核心：大方块 7 列月历方阵（高对比度 + 超大触控面）
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
                ) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach { dayLabel ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = dayLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NativeDesignTokens.textMuted,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val firstDayOfMonth = currentYearMonth.atDay(1)
                val daysInMonth = currentYearMonth.lengthOfMonth()
                val startDayOfWeek = firstDayOfMonth.dayOfWeek.value
                val totalCells = ((daysInMonth + startDayOfWeek - 1 + 6) / 7) * 7

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (row in 0 until (totalCells / 7)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            for (col in 1..7) {
                                val cellIndex = row * 7 + col
                                val dayNumber = cellIndex - startDayOfWeek + 1

                                if (dayNumber in 1..daysInMonth) {
                                    val cellDate = currentYearMonth.atDay(dayNumber)
                                    val isSelected = cellDate == selectedDate
                                    val isToday = cellDate == today
                                    val record = moodRecords[cellDate]
                                    val hasMood = record != null && record.mood != MoodLevel.EMPTY
                                    val cellMood = record?.mood ?: MoodLevel.EMPTY

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(NativeDesignTokens.radiusSm))
                                            .background(
                                                if (hasMood) {
                                                    cellMood.color.copy(alpha = 0.22f)
                                                } else {
                                                    Color(0xFF141416)
                                                },
                                            )
                                            .border(
                                                width = if (isSelected) {
                                                    2.dp
                                                } else if (isToday) {
                                                    1.5.dp
                                                } else {
                                                    1.dp
                                                },
                                                color = when {
                                                    isSelected -> NativeDesignTokens.textPrimary
                                                    isToday -> NativeDesignTokens.accentPrimary
                                                    hasMood -> cellMood.color
                                                    else -> Color(0xFF27272A)
                                                },
                                                shape = RoundedCornerShape(NativeDesignTokens.radiusSm),
                                            )
                                            .clickable { selectedDate = cellDate },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                        ) {
                                            Text(
                                                text = dayNumber.toString(),
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected || isToday || hasMood) {
                                                    FontWeight.Bold
                                                } else {
                                                    FontWeight.Normal
                                                },
                                                color = when {
                                                    isSelected -> NativeDesignTokens.textPrimary
                                                    hasMood -> cellMood.color
                                                    isToday -> NativeDesignTokens.accentPrimary
                                                    else -> NativeDesignTokens.textSecondary
                                                },
                                            )
                                            if (hasMood) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(cellMood.color),
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(NativeDesignTokens.spacingMd))

            // 3. 底部打卡与备忘录
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
                        text = selectedDate.format(DateTimeFormatter.ofPattern("MM月dd日 心绪打卡")),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NativeDesignTokens.textPrimary,
                    )
                    Text(
                        text = if (selectedDate == today) "今天" else "所选日期",
                        fontSize = 11.sp,
                        color = if (selectedDate == today) {
                            NativeDesignTokens.accentPrimary
                        } else {
                            NativeDesignTokens.textSecondary
                        },
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val currentRecord = moodRecords[selectedDate]
                    MoodLevel.values().filter { it != MoodLevel.EMPTY }.forEach { level ->
                        val isCurrent = currentRecord?.mood == level
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(NativeDesignTokens.radiusSm))
                                .background(
                                    if (isCurrent) {
                                        level.color.copy(alpha = 0.25f)
                                    } else {
                                        Color(0xFF141416)
                                    },
                                )
                                .border(
                                    width = if (isCurrent) 1.5.dp else 1.dp,
                                    color = if (isCurrent) level.color else Color(0xFF27272A),
                                    shape = RoundedCornerShape(NativeDesignTokens.radiusSm),
                                )
                                .clickable {
                                    moodRecords[selectedDate] =
                                        DayMoodRecord(selectedDate, level, currentNoteInput)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(level.color),
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = level.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) {
                                        NativeDesignTokens.textPrimary
                                    } else {
                                        NativeDesignTokens.textSecondary
                                    },
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextField(
                    value = currentNoteInput,
                    onValueChange = {
                        currentNoteInput = it
                        val currentMood = moodRecords[selectedDate]?.mood ?: MoodLevel.CALM
                        moodRecords[selectedDate] = DayMoodRecord(selectedDate, currentMood, it)
                    },
                    placeholder = {
                        Text(
                            "记录今天的一句备忘...",
                            fontSize = 12.sp,
                            color = NativeDesignTokens.textMuted,
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF141416),
                        unfocusedContainerColor = Color(0xFF141416),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = NativeDesignTokens.textPrimary,
                        unfocusedTextColor = NativeDesignTokens.textPrimary,
                    ),
                    shape = RoundedCornerShape(NativeDesignTokens.radiusSm),
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(NativeDesignTokens.spacingXl))
    }
}
