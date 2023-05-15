package com.supranet.webview

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class Streaming : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.streaming)

        val videoView = findViewById<VideoView>(R.id.video_view)
        val videoUri =
            Uri.parse("https://stream.ichibantv.com:3741/live/aniplustvlive.m3u8?PlaylistM3UCL")
        videoView.setVideoURI(videoUri)
        videoView.start()

        val timer = Timer()
        val task = object : TimerTask() {
            var isStreamingActivityShowing = false
            override fun run() {
                runOnUiThread {
                    if (isStreamingActivityShowing) {
                        val intent = Intent(this@Streaming, MainActivity::class.java)
                        startActivity(intent)
                        isStreamingActivityShowing = false
                        timer.cancel()
                    } else {
                        isStreamingActivityShowing = true
                    }
                }
            }
        }
        timer.schedule(task, 0, 1 * 60 * 1000)
    }
}


