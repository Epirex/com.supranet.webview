package com.supranet.webview

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ProgressBar
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class Streaming : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.streaming)

        val videoView = findViewById<VideoView>(R.id.video_view)
        val videoUri =
            Uri.parse("https://live-01-02-eltrece.vodgc.net/eltrecetv/index.m3u8?PlaylistM3UCL")
        videoView.setVideoURI(videoUri)
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        progressBar.visibility = View.VISIBLE // Hacer visible la vista de ProgressBar

        videoView.setOnPreparedListener {
            progressBar.visibility = View.GONE // Ocultar la vista de ProgressBar cuando el video esté preparado
            videoView.start()
            val fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out)
            fadeOutAnimation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}

                override fun onAnimationRepeat(animation: Animation?) {}

                override fun onAnimationEnd(animation: Animation?) {
                    progressBar.visibility = View.GONE
                }
            })
            progressBar.startAnimation(fadeOutAnimation)
        }

        val timer = Timer()
        val task = object : TimerTask() {
            var isStreamingActivityShowing = false
            override fun run() {
                runOnUiThread {
                    if (isStreamingActivityShowing) {
                        val intent = Intent(this@Streaming, MainActivity::class.java)
                        startActivity(intent)
                        timer.cancel()
                        Runtime.getRuntime().gc()
                        System.gc()
                        finish()
                    } else {
                        isStreamingActivityShowing = true
                    }
                }
            }
        }
        timer.schedule(task, 0, 1 * 30 * 1000)
    }
    override fun onDestroy() {
        super.onDestroy()
        System.gc()
    }
}


