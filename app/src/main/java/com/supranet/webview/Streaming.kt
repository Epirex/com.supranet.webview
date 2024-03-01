package com.supranet.webview

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import java.util.*

class Streaming : AppCompatActivity() {

    private var timer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_streaming)

        // Lottie time!
        val lottieAnimationView = findViewById<LottieAnimationView>(R.id.lottieAnimationView)
        lottieAnimationView.playAnimation()

        // Ponemos un handler hasta que definamos una animacion final
        Handler().postDelayed({
            lottieAnimationView.cancelAnimation()
            lottieAnimationView.visibility = View.GONE
        }, 5000)

        val videoView = findViewById<VideoView>(R.id.videoview)
        val videoUri =
            Uri.parse("https://live-01-02-eltrece.vodgc.net/eltrecetv/index.m3u8?PlaylistM3UCL")
        videoView.setVideoURI(videoUri)
        videoView.start()

        // Timer para el bucle entre actividades
        if (timer == null) {
            timer = Timer()
            val task = object : TimerTask() {
                var isStreamingActivityShowing = false
                override fun run() {
                    runOnUiThread {
                        if (isStreamingActivityShowing) {
                            val intent = Intent(this@Streaming, MainActivity::class.java)
                            startActivity(intent)
                            isStreamingActivityShowing = false
                            timer?.cancel()
                            timer = null
                            finish()
                        } else {
                            isStreamingActivityShowing = true
                        }
                    }
                }
            }
            timer?.schedule(task, 0, 1 * 60 * 1000)
        }
    }
}