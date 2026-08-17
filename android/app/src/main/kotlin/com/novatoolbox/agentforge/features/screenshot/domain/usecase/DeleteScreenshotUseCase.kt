package com.novatoolbox.agentforge.features.screenshot.domain.usecase

import android.net.Uri
import com.novatoolbox.agentforge.core.security.MediaUriValidator
import com.novatoolbox.agentforge.features.screenshot.data.ScreenshotRepository

/**
 * 删除截图用例（跨进程删除职责拆分模块）。
 *
 * 缺陷 3 修复：删除入口强制 URI 白名单校验，非法域一律拒绝。
 */
class DeleteScreenshotUseCase(private val repository: ScreenshotRepository) {
    suspend operator fun invoke(uri: Uri): Result<Unit> {
        if (!MediaUriValidator.isValidImageUri(uri)) {
            return Result.failure(SecurityException("SECURITY_REJECTED: 非相册合法 URI 拒绝删除"))
        }
        return repository.deleteScreenshot(uri)
    }
}
