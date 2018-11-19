package net.detunized.videoshrink

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        processSendIntent()
    }

    private fun processSendIntent() {
        if (intent.action == Intent.ACTION_SEND && intent.type.startsWith("video/")) {
            (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let  {
                processVideo(it)
            }
        }
    }

    private fun processVideo(uri: Uri) {
        Log.d(TAG, "Received a video ${uri.path}")
    }

    companion object {
        private const val TAG = "video-shrink"
    }
}
