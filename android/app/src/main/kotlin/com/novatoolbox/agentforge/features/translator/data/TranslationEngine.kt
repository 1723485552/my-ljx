package com.novatoolbox.agentforge.features.translator.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

enum class SupportLanguage(
    val code: String,
    val myMemoryCode: String,
    val displayName: String,
    val locale: Locale
) {
    ZH_CN("zh", "zh-CN", "中文 (简体)", Locale.SIMPLIFIED_CHINESE),
    EN("en", "en", "英语 (English)", Locale.ENGLISH),
    JA("ja", "ja", "日语 (日本語)", Locale.JAPANESE),
    KO("ko", "ko", "韩语 (한국어)", Locale.KOREAN),
    FR("fr", "fr", "法语 (Français)", Locale.FRENCH),
    DE("de", "de", "德语 (Deutsch)", Locale.GERMAN),
    RU("ru", "ru", "俄语 (Русский)", Locale("ru")),
    ES("es", "es", "西班牙语 (Español)", Locale("es"))
}

object TranslationEngine {

    /**
     * 国内/海外双通道高可用翻译调度
     */
    suspend fun translate(
        text: String,
        sourceLang: SupportLanguage,
        targetLang: SupportLanguage
    ): Result<String> = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext Result.success("")

        // 通道 1：MyMemory 全球开放高速通道（国内直连畅通）
        val result1 = requestMyMemory(text, sourceLang.myMemoryCode, targetLang.myMemoryCode)
        if (result1.isSuccess && !result1.getOrNull().isNullOrBlank()) {
            return@withContext result1
        }

        // 通道 2：备用轻量镜像通道
        val result2 = requestFallbackMirror(text, sourceLang.code, targetLang.code)
        if (result2.isSuccess && !result2.getOrNull().isNullOrBlank()) {
            return@withContext result2
        }

        Result.failure(Exception("翻译服务暂时不可达，请稍后重试"))
    }

    private fun requestMyMemory(text: String, sl: String, tl: String): Result<String> {
        return try {
            val encoded = URLEncoder.encode(text.trim(), "UTF-8")
            val urlStr = "https://api.mymemory.translated.net/get?q=$encoded&langpair=$sl|$tl"
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
            }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val status = json.optInt("responseStatus", 0)
                if (status == 200 || json.has("responseData")) {
                    val responseData = json.getJSONObject("responseData")
                    val translatedText = responseData.getString("translatedText")
                    Result.success(translatedText)
                } else {
                    Result.failure(Exception("MyMemory status: $status"))
                }
            } else {
                Result.failure(Exception("HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun requestFallbackMirror(text: String, sl: String, tl: String): Result<String> {
        return try {
            val encoded = URLEncoder.encode(text.trim(), "UTF-8")
            val urlStr = "https://lingva.ml/api/v1/$sl/$tl/$encoded"
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
            }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val translation = json.optString("translation", "")
                if (translation.isNotEmpty()) {
                    Result.success(translation)
                } else {
                    Result.failure(Exception("Empty translation"))
                }
            } else {
                Result.failure(Exception("HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
