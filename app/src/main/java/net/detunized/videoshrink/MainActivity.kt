package net.detunized.videoshrink

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import net.ypresto.androidtranscoder.MediaTranscoder
import net.ypresto.androidtranscoder.format.MediaFormatStrategyPresets
import java.io.File
import java.lang.Exception
import android.provider.MediaStore


@TargetApi(Build.VERSION_CODES.M)
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestNeededPermissions()
        processSendIntent()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        Log.d(TAG, "onRequestPermissionsResult: $requestCode ${permissions[0]} ${grantResults[0]}")
    }

    private fun requestNeededPermissions() {
        requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun requestPermission(perm: String) {
        if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(perm), READ_EXTERNAL_STORAGE_CODE)
        }
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
        val outputFile = File(getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES), "shrunk.mp4")
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

        Log.d(TAG, "Converting a video from '$path' to '$outputFile'")

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
        private const val READ_EXTERNAL_STORAGE_CODE = 1337
    }
}
