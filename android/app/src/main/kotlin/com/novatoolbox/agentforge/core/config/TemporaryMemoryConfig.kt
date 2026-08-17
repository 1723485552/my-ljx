package com.novatoolbox.agentforge.core.config

import android.content.Context
import android.content.SharedPreferences

/**
 * 瞬时暂存器轻量配置器（用户自定义自毁时长持久化）。
 *
 * 数据特性：纯本机 SharedPreferences，零网络、零外部追踪。
 */
object TemporaryMemoryConfig {
    private const val PREF_NAME = "nova_temporary_memory_pref"
    private const val KEY_DURATION_SECONDS = "key_duration_seconds"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /** 获取用户自定义倒计时秒数（默认 10 秒，约束 3~120）。 */
    fun getCountdownDuration(context: Context): Int {
        return getPrefs(context).getInt(KEY_DURATION_SECONDS, 10).coerceIn(3, 120)
    }

    /** 保存用户自定义倒计时秒数。 */
    fun setCountdownDuration(context: Context, seconds: Int) {
        getPrefs(context).edit().putInt(KEY_DURATION_SECONDS, seconds.coerceIn(3, 120)).apply()
    }
}
