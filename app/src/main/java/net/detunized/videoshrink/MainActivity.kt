package net.detunized.videoshrink

import android.app.Activity
import android.os.Bundle
import net.ypresto.androidtranscoder.MediaTranscoder
import net.ypresto.androidtranscoder.format.MediaFormatStrategyPresets

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
