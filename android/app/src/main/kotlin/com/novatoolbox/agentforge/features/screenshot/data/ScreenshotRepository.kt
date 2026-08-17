package com.novatoolbox.agentforge.features.screenshot.data

import android.net.Uri
import kotlinx.coroutines.flow.Flow

/**
 * 截图仓储抽象（Clean Architecture 端口）。
 *
 * 拆分为纯数据端口 [ScreenshotRepository] 与可注入的实现依赖 [Dependency]，
 * 便于在单测中以假对象替换 ContentResolver 交互（无 Android 依赖）。
 */
interface ScreenshotRepository {
    /** 监听最新截图变化的流（基于 ContentObserver）。 */
    val latestScreenshotStream: Flow<Uri?>

    /** 判断 uri 对应图片是否在新鲜度阈值内。 */
    fun isImageFresh(uri: Uri, thresholdSeconds: Long): Boolean

    /** 请求删除截图（低版本直接删；高版本由调用方走系统确认流程）。 */
    suspend fun deleteScreenshot(uri: Uri): Result<Unit>

    /** 查询最新一张符合截屏命名规律的图片 uri。 */
    fun queryLatestScreenshot(): Uri?
}

/** 仓储可替换实现依赖（测试替身入口）。 */
interface Dependency : ScreenshotRepository
