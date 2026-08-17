package com.novatoolbox.agentforge.features.screenshot.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.novatoolbox.agentforge.core.security.MediaUriValidator

/**
 * 跨应用呼起系统删除弹窗的透明中转 Activity。
 *
 * 缺陷 3 修复：删除入口严密校验 uri 归属，非法直接拒绝对话。
 */
class TransparentDeleteActivity : ComponentActivity() {
    private lateinit var deleteLauncher: ActivityResultLauncher<IntentSenderRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        deleteLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) {
            finish()
        }

        val uriStr = intent.getStringExtra("target_uri")
        if (uriStr.isNullOrEmpty()) {
            finish()
            return
        }
        val uri = Uri.parse(uriStr)

        if (!MediaUriValidator.isValidImageUri(uri)) {
            finish()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val deleteRequest = MediaStore.createDeleteRequest(contentResolver, listOf(uri))
            val request = IntentSenderRequest.Builder(deleteRequest.intentSender).build()
            deleteLauncher.launch(request)
        } else {
            runCatching { contentResolver.delete(uri, null, null) }
            finish()
        }
    }

    companion object {
        fun launch(context: Context, uri: Uri) {
            val intent = Intent(context, TransparentDeleteActivity::class.java).apply {
                putExtra("target_uri", uri.toString())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
