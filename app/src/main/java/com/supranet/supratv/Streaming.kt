package com.supranet.supratv

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewClientCompat

class Streaming : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var webView: android.webkit.WebView
    private val handler = Handler()
    private var webViewVisible = false
    private var currentChannelIndex = 0
    private val channels = listOf(
        "https://live-01-02-eltrece.vodgc.net/eltrecetv/index.m3u8?PlaylistM3UCL",
        "https://stmv6.voxtvhd.com.br/crossingtv/crossingtv/playlist.m3u8?PlaylistM3UCL",
        "https://video06.logicahost.com.br/retroplustv/retroplustv/playlist.m3u8?PlaylistM3UCL",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_streaming)

        videoView = findViewById(R.id.videoview)
        val videoUri = Uri.parse(channels[currentChannelIndex])
        videoView.setVideoURI(videoUri)
        videoView.start()

        // Webview settings
        webView = android.webkit.WebView(this)
        webView.setBackgroundResource(R.drawable.fondocata);
        webView.setBackgroundColor(0x00000000);
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        layoutParams.gravity = Gravity.CENTER
        webView.layoutParams = layoutParams
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClientCompat()

        val container = findViewById<FrameLayout>(R.id.video_container)
        container.addView(webView)
        webView.visibility = View.GONE

        startWebViewLoop()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                switchToNextChannel()
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                switchToPreviousChannel()
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun switchToNextChannel() {
        currentChannelIndex = (currentChannelIndex + 1) % channels.size
        val videoUri = Uri.parse(channels[currentChannelIndex])
        videoView.setVideoURI(videoUri)
        videoView.start()
    }

    private fun switchToPreviousChannel() {
        currentChannelIndex = if (currentChannelIndex == 0) {
            channels.size - 1
        } else {
            currentChannelIndex - 1
        }
        val videoUri = Uri.parse(channels[currentChannelIndex])
        videoView.setVideoURI(videoUri)
        videoView.start()
    }

    private fun startWebViewLoop() {
        handler.postDelayed({
            if (webViewVisible) {
                webView.visibility = View.GONE
            } else {
                webView.visibility = View.VISIBLE
                webView.loadUrl("https://estebanguzzo.com.ar/publicidadcata/")
            }
            webViewVisible = !webViewVisible
            startWebViewLoop()
        }, 30 * 1000)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}