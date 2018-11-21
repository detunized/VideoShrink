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
import android.os.StrictMode
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import net.ypresto.androidtranscoder.MediaTranscoder
import net.ypresto.androidtranscoder.format.MediaFormatStrategyPresets
import java.io.File

@TargetApi(Build.VERSION_CODES.M)
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (isSendIntent()) {
            allowPrivateFileAccessByOtherApps()
            requestNeededPermissionsAndProcessSendIntent()
        }
    }

    private fun allowPrivateFileAccessByOtherApps() {
        // It's hacky, but allows to keep the default settings minus the uri exposure thing.
        // I tried to implement the proposed solution from here https://stackoverflow.com/a/50265329/362938
        // using the FileProvider approach. It works and doesn't require this hack. The problem is
        // that the video gets shared as a file attachment on Telegram instead of a playable video.
        StrictMode::class.java.getMethod("disableDeathOnFileUriExposure").invoke(null)
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
            showToastAndQuit(R.string.read_permission_denied)
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
        val preset = MediaFormatStrategyPresets.createAndroid720pStrategy(4000 * 1000)
        val listener = object : MediaTranscoder.Listener {
            override fun onTranscodeCompleted() {
                progressBar.progress = progressBar.max
                shareVideoWithTelegram(outputFile)
                finish()
            }

            override fun onTranscodeProgress(progress: Double) {
                progressBar.progress = Math.round(progressBar.max * progress).toInt()
            }

            override fun onTranscodeCanceled() {
                showToastAndQuit(R.string.transcode_canceled)
            }

            override fun onTranscodeFailed(exception: Exception?) {
                showToastAndQuit(R.string.transcode_failed)
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
        startActivity(Intent.createChooser(makeSendIntent(file), "Share video"))
    }

    private fun shareVideoWithTelegram(file: File) {
        startActivity(makeSendIntent(file).apply {
            setClassName("org.telegram.messenger", "org.telegram.ui.LaunchActivity")
        })
    }

    private fun makeSendIntent(file: File): Intent {
        return Intent().apply {
            action = Intent.ACTION_SEND
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
        }
    }

    private fun showToastAndQuit(id: Int) {
        Toast.makeText(applicationContext, getString(id), Toast.LENGTH_LONG).show()
        finish()
    }

    companion object {
        private const val TAG = "video-shrink"
        private const val READ_EXTERNAL_STORAGE_CODE = 1337
    }
}
