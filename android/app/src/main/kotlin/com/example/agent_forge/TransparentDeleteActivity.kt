package com.example.agent_forge

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

class TransparentDeleteActivity : ComponentActivity() {
    private lateinit var deleteLauncher: ActivityResultLauncher<IntentSenderRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        deleteLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) {
            // 无论用户确认或取消，中转页使命结束
            finish()
        }

        val uriStr = intent.getStringExtra("target_uri")
        if (uriStr.isNullOrEmpty()) {
            finish()
            return
        }
        val uri = Uri.parse(uriStr)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val deleteRequest = MediaStore.createDeleteRequest(contentResolver, listOf(uri))
            val request = IntentSenderRequest.Builder(deleteRequest.intentSender).build()
            deleteLauncher.launch(request)
        } else {
            // 低版本直接删除后关闭中转页
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
