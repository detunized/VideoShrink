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
import android.widget.ProgressBar
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

@TargetApi(Build.VERSION_CODES.M)
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (isSendIntent())
            requestNeededPermissionsAndProcessSendIntent()
    }

    private fun requestNeededPermissionsAndProcessSendIntent() {
        if (needReadPermission())
            requestReadPermission() // This should call processSendIntent on success
        else
            processSendIntent()
    }

    private fun needReadPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
    }

    private fun requestReadPermission() {
        requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), READ_EXTERNAL_STORAGE_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        val granted = requestCode == READ_EXTERNAL_STORAGE_CODE
                && permissions.size == 1
                && permissions[0] == Manifest.permission.READ_EXTERNAL_STORAGE
                && grantResults.size == 1
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
        if (granted)
            processSendIntent()
        else
            Toast.makeText(applicationContext, getString(R.string.read_permission_denied), Toast.LENGTH_LONG).show()
    }

    private fun isSendIntent() = intent.action == Intent.ACTION_SEND

    private fun processSendIntent() {
        assert(isSendIntent())
        if (intent.type.startsWith("video/")) {
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
        val preset = MediaFormatStrategyPresets.createAndroid720pStrategy(5000 * 1000)
        val listener = object : MediaTranscoder.Listener {
            override fun onTranscodeCompleted() {
                progressBar.progress = progressBar.max
                shareVideo(outputFile)
            }

            override fun onTranscodeProgress(progress: Double) {
                progressBar.progress = Math.round(progressBar.max * progress).toInt()
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

    private fun shareVideo(file: File) {
        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
            type = "video/avc"
        }
        startActivity(Intent.createChooser(shareIntent, "Share video"))
    }

    companion object {
        private const val TAG = "video-shrink"
        private const val READ_EXTERNAL_STORAGE_CODE = 1337
    }
}
