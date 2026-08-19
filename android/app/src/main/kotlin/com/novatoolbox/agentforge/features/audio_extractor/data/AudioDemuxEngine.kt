package com.novatoolbox.agentforge.features.audio_extractor.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer

data class VideoAudioMetadata(
    val fileName: String,
    val fileSizeFormatted: String,
    val durationMs: Long,
    val audioMime: String,
    val sampleRate: Int,
    val channelCount: Int,
    val bitrate: Int
)

object AudioDemuxEngine {

    /**
     * 解析本地视频及其物理音轨硬件参数（纯本地，零网络）。
     */
    suspend fun probeVideoAudio(context: Context, uri: Uri): Result<VideoAudioMetadata> = withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: "0"
            val durationMs = durationStr.toLongOrNull() ?: 0L

            val extractor = MediaExtractor()
            extractor.setDataSource(context, uri, null)

            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioFormat = format
                    break
                }
            }
            extractor.release()
            retriever.release()

            if (audioFormat == null) {
                return@withContext Result.failure(Exception("所选视频未包含有效音频轨道"))
            }

            var fileName = "本地视频"
            var fileSize = 0L
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: "本地视频"
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: "audio/unknown"
            val sampleRate = if (audioFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 0
            val channelCount = if (audioFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 0
            val bitrate = if (audioFormat.containsKey(MediaFormat.KEY_BIT_RATE)) audioFormat.getInteger(MediaFormat.KEY_BIT_RATE) else 0

            val sizeMb = String.format("%.2f MB", fileSize.toDouble() / (1024 * 1024))

            Result.success(
                VideoAudioMetadata(
                    fileName = fileName,
                    fileSizeFormatted = sizeMb,
                    durationMs = durationMs,
                    audioMime = mime,
                    sampleRate = sampleRate,
                    channelCount = channelCount,
                    bitrate = bitrate
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 100% 物理位流无损解复用（Stream Copy / Demuxing）。
     * 仅接受本地视频 Uri（来自 SAF 文件选择），不触碰任何网络资源。
     */
    suspend fun extractLosslessAudioToFile(
        context: Context,
        uri: Uri,
        targetAudioFile: File,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            extractor.setDataSource(context, uri, null)
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null) {
                return@withContext Result.failure(Exception("视频未包含有效音频流"))
            }

            extractor.selectTrack(audioTrackIndex)

            muxer = MediaMuxer(targetAudioFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val dstAudioTrackIndex = muxer.addTrack(audioFormat)
            muxer.start()

            val maxInputSize = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            } else {
                1024 * 1024
            }
            val buffer = ByteBuffer.allocate(maxInputSize)
            val bufferInfo = MediaCodec.BufferInfo()
            val durationUs = if (audioFormat.containsKey(MediaFormat.KEY_DURATION)) audioFormat.getLong(MediaFormat.KEY_DURATION) else 1L

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    break
                }
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags

                muxer.writeSampleData(dstAudioTrackIndex, buffer, bufferInfo)

                if (durationUs > 0) {
                    val progress = (bufferInfo.presentationTimeUs.toFloat() / durationUs.toFloat()).coerceIn(0f, 1f)
                    onProgress(progress)
                }

                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            muxer = null
            extractor.release()

            Result.success(targetAudioFile)
        } catch (e: Exception) {
            muxer?.runCatching { release() }
            extractor.runCatching { release() }
            targetAudioFile.delete()
            Result.failure(e)
        }
    }

    /**
     * 将本地已提取的无损音频写入系统音乐库 (Music/NovaToolBox)。
     */
    suspend fun saveAudioToMusicStore(
        context: Context,
        sourceAudioFile: File,
        baseTitle: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanTitle = baseTitle.replace(Regex("[\\\\/:*?\"<>|\\r\\n]"), "_").take(35)
            val filename = "${cleanTitle}_Lossless_${System.currentTimeMillis()}.m4a"

            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, filename)
                put(MediaStore.Audio.Media.TITLE, baseTitle)
                put(MediaStore.Audio.Media.ARTIST, "NovaToolBox")
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/NovaToolBox")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
            }

            val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.failure(Exception("无法创建系统音频库记录"))

            context.contentResolver.openOutputStream(uri)?.use { outStream ->
                FileInputStream(sourceAudioFile).use { inStream ->
                    inStream.copyTo(outStream)
                }
            } ?: return@withContext Result.failure(Exception("写入音频数据失败"))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }

            Result.success("Music/NovaToolBox/$filename")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
