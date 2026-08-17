package com.novatoolbox.agentforge.features.screenshot.domain.usecase

import android.net.Uri
import com.novatoolbox.agentforge.core.security.MediaUriValidator
import com.novatoolbox.agentforge.features.screenshot.data.ScreenshotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 获取最新截图用例（抓取职责拆分模块）。
 *
 * 修正：原指令在 uri 可能为 null 时使用 `uri!!` 会崩溃；
 * 此处先判非空再调用 isImageFresh，确保空安全。
 */
class GetLatestScreenshotUseCase(private val repository: ScreenshotRepository) {
    operator fun invoke(freshnessThresholdSeconds: Long = 10): Flow<Uri?> {
        return repository.latestScreenshotStream.map { uri ->
            if (uri != null &&
                MediaUriValidator.isValidImageUri(uri) &&
                repository.isImageFresh(uri, freshnessThresholdSeconds)
            ) {
                uri
            } else {
                null
            }
        }
    }
}
