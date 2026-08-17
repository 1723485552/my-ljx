package com.example.agent_forge

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
import java.io.File
import java.nio.ByteBuffer

object MediaProcessorHelper {

    // 1. 获取视频元数据 (时长、分辨率、体积、码率)
    fun getVideoInfo(context: Context, uriString: String): Map<String, Any> {
        val retriever = MediaMetadataRetriever()
        return try {
            val uri = Uri.parse(uriString)
            retriever.setDataSource(context, uri)

            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull() ?: 0L
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0

            mapOf(
                "durationMs" to durationMs,
                "width" to width,
                "height" to height,
                "bitrate" to bitrate,
                "rotation" to rotation
            )
        } catch (e: Exception) {
            emptyMap()
        } finally {
            retriever.release()
        }
    }

    // 2. 毫秒级无损音频剥离 (Demuxing without Re-encoding)
    fun extractAudioLossless(context: Context, videoUriString: String): Map<String, Any> {
        val extractor = MediaExtractor()
        val tempOutputFile = File(context.cacheDir, "extracted_${System.currentTimeMillis()}.m4a")

        try {
            val uri = Uri.parse(videoUriString)
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

            if (audioTrackIndex < 0 || audioFormat == null) {
                throw Exception("该视频未检测到有效音轨")
            }

            extractor.selectTrack(audioTrackIndex)

            val muxer = MediaMuxer(tempOutputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val writeTrackIndex = muxer.addTrack(audioFormat)
            muxer.start()

            val maxBufferSize = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            } else {
                1024 * 1024
            }

            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    bufferInfo.size = 0
                    break
                }
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(writeTrackIndex, buffer, bufferInfo)
                extractor.advance()
            }

            muxer.stop()
            muxer.release()

            return mapOf(
                "success" to true,
                "outputPath" to tempOutputFile.absolutePath,
                "sizeBytes" to tempOutputFile.length()
            )
        } finally {
            extractor.release()
        }
    }

    // 3. 将提取的音频保存至系统音乐库 (Music/NovaToolBox)
    fun saveAudioToMusicLibrary(context: Context, filePath: String, title: String): Boolean {
        val srcFile = File(filePath)
        if (!srcFile.exists()) return false

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$title.m4a")
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/NovaToolBox")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values) ?: return false

        resolver.openOutputStream(uri)?.use { out ->
            srcFile.inputStream().use { input ->
                input.copyTo(out)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }

        return true
    }
}
