package com.novatoolbox.agentforge.core.security

import android.net.Uri
import android.provider.MediaStore

/**
 * 军规级安全边界：防止跨进程越权删除非相册目录文件。
 *
 * 仅允许属于外部相册图片集合 [MediaStore.Images.Media.EXTERNAL_CONTENT_URI] 的 URI。
 */
object MediaUriValidator {
    // 相册图片集合根 URI（ContentProvider authority 稳定，不依赖 Android 静态初始化顺序，
    // 便于在 JVM 单元测试中验证边界）。
    private const val VALID_PREFIX: String = "content://media/external/images/media"

    fun isValidImageUri(uri: Uri?): Boolean {
        if (uri == null) return false
        return uri.toString().startsWith(VALID_PREFIX)
    }

    /** 校验不通过时抛出安全异常（供删除入口强校验）。 */
    fun requireValidImageUri(uri: Uri?): Uri {
        if (!isValidImageUri(uri)) {
            throw SecurityException("SECURITY_REJECTED: 非相册合法 URI 拒绝操作: $uri")
        }
        return uri!!
    }
}
