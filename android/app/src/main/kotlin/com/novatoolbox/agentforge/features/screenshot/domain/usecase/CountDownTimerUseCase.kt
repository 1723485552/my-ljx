package com.novatoolbox.agentforge.features.screenshot.domain.usecase

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 自毁倒计时用例（独立生命周期计时模块）。
 *
 * 职责边界（SRP）：仅负责 1Hz 滴答与到期，不感知抓取/删除/悬浮窗。
 */
class CountDownTimerUseCase {
    operator fun invoke(startSeconds: Int): Flow<Int> = flow {
        for (i in startSeconds downTo 0) {
            emit(i)
            if (i > 0) delay(1000L)
        }
    }
}
