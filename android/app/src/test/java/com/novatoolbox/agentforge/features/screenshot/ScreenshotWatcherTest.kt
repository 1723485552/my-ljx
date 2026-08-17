package com.novatoolbox.agentforge.features.screenshot

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.novatoolbox.agentforge.core.security.MediaUriValidator
import com.novatoolbox.agentforge.features.screenshot.data.MediaStoreScreenshotRepository
import com.novatoolbox.agentforge.features.screenshot.data.ScreenshotRepository
import com.novatoolbox.agentforge.features.screenshot.domain.usecase.CountDownTimerUseCase
import com.novatoolbox.agentforge.features.screenshot.domain.usecase.GetLatestScreenshotUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 截屏监听与安全边界单测（军规级验收）。
 *
 * 覆盖：
 * - 缺陷 3：MediaUriValidator 仅放行相册图片 URI，拒绝对接越权路径。
 * - 缺陷 2/空安全：GetLatestScreenshotUseCase 在 uri 为 null / 非法 / 不新鲜时返回 null。
 * - CountDownTimerUseCase：从 startSeconds 递减到 0，序列正确。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ScreenshotWatcherTest {

    private val validImageUri: Uri =
        Uri.parse("content://media/external/images/media/123")
    private val invalidUri: Uri = Uri.parse("content://com.other.app/file/secret")

    @Test
    fun mediaUriValidator_acceptsGalleryImage_rejectsForeign() {
        assertThat(MediaUriValidator.isValidImageUri(validImageUri)).isTrue()
        assertThat(MediaUriValidator.isValidImageUri(invalidUri)).isFalse()
        assertThat(MediaUriValidator.isValidImageUri(null)).isFalse()
    }

    @Test(expected = SecurityException::class)
    fun mediaUriValidator_requireValid_throwsOnInvalid() {
        MediaUriValidator.requireValidImageUri(invalidUri)
    }

    @Test
    fun countDown_emitsDescendingSequenceEndingAtZero() = runTest {
        val seq = CountDownTimerUseCase()(5).toList()
        assertThat(seq).containsExactly(5, 4, 3, 2, 1, 0).inOrder()
    }

    @Test
    fun getLatestScreenshot_nullWhenUriNull() = runTest {
        val fakeRepo = FakeScreenshotRepository(null)
        val useCase = GetLatestScreenshotUseCase(fakeRepo)
        val result = useCase(freshnessThresholdSeconds = 30).first()
        assertThat(result).isNull()
    }

    @Test
    fun getLatestScreenshot_nullWhenUriNotValidImage() = runTest {
        val fakeRepo = FakeScreenshotRepository(invalidUri, isFresh = true)
        val useCase = GetLatestScreenshotUseCase(fakeRepo)
        val result = useCase(freshnessThresholdSeconds = 30).first()
        assertThat(result).isNull()
    }

    @Test
    fun getLatestScreenshot_returnsUriWhenValidAndFresh() = runTest {
        val fakeRepo = FakeScreenshotRepository(validImageUri, isFresh = true)
        val useCase = GetLatestScreenshotUseCase(fakeRepo)
        val result = useCase(freshnessThresholdSeconds = 30).first()
        assertThat(result).isEqualTo(validImageUri)
    }

    @Test
    fun getLatestScreenshot_nullWhenStale() = runTest {
        val fakeRepo = FakeScreenshotRepository(validImageUri, isFresh = false)
        val useCase = GetLatestScreenshotUseCase(fakeRepo)
        val result = useCase(freshnessThresholdSeconds = 30).first()
        assertThat(result).isNull()
    }

    /** 测试替身：不依赖真实 ContentResolver。 */
    private class FakeScreenshotRepository(
        private val uri: Uri?,
        private val isFresh: Boolean = true,
    ) : ScreenshotRepository {
        override val latestScreenshotStream: kotlinx.coroutines.flow.Flow<Uri?> =
            kotlinx.coroutines.flow.flow { emit(uri) }
        override fun isImageFresh(uri: Uri, thresholdSeconds: Long): Boolean = isFresh
        override suspend fun deleteScreenshot(uri: Uri): Result<Unit> = Result.success(Unit)
        override fun queryLatestScreenshot(): Uri? = uri
    }
}
