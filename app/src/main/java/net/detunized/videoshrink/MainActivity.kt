package net.detunized.videoshrink

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import net.ypresto.androidtranscoder.MediaTranscoder
import net.ypresto.androidtranscoder.format.MediaFormatStrategyPresets
import java.io.File
import java.lang.Exception
import android.provider.MediaStore



class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        processSendIntent()
    }

    private fun processSendIntent() {
        if (intent.action == Intent.ACTION_SEND && intent.type.startsWith("video/")) {
            (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let { uri ->
                processVideo(uri)
            }
        }
    }

    private fun processVideo(uri: Uri) {
        getContentFilename(uri)?.let { path ->
            processVideo(path)
        }
    }

    private fun processVideo(path: String) {
        Log.d(TAG, "Converting a video: $path")

        val outputFile = File(getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES), "shrunk")
        val preset = MediaFormatStrategyPresets.createAndroid720pStrategy(8000 * 1000)
        val listener = object : MediaTranscoder.Listener {
            override fun onTranscodeCompleted() {
                Log.d(TAG, "Completed")
            }

            override fun onTranscodeProgress(progress: Double) {
                Log.d(TAG, "Progress: $progress")
            }

            override fun onTranscodeCanceled() {
                Log.d(TAG, "Canceled")
            }

            override fun onTranscodeFailed(exception: Exception?) {
                Log.d(TAG, "Failed")
            }
        }

        MediaTranscoder.getInstance().transcodeVideo(path, outputFile.path, preset, listener)
    }

    private fun getContentFilename(uri: Uri): String? {
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
        return applicationContext.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst())
                cursor.getString(0)
            else
                null
        }
    }

    companion object {
        private const val TAG = "video-shrink"
    }
}
