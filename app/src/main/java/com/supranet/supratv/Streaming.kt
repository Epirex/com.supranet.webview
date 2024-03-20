package com.supranet.supratv

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewClientCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class Streaming : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var webView: android.webkit.WebView
    private val handler = Handler()
    private var webViewVisible = false
    private var currentChannelIndex = 0
    private var channels: List<String> = emptyList()
    private var timer: Timer? = null
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_streaming)

        // SharedPreferences settings
        sharedPreferences = getSharedPreferences("StreamingPreferences", Context.MODE_PRIVATE)
        currentChannelIndex = sharedPreferences.getInt("currentChannelIndex", 0)
        val timerActive = sharedPreferences.getBoolean("timerActive", false)

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

        // Carga de listado de canales
        loadChannels {
            startVideo()
        }

        // Loop de publicidad en streaming
        startWebViewLoop()

        // Chequeo del timer para la publicidad completa
        if (timerActive) {
            startTimer()
        }
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences.edit().putInt("currentChannelIndex", currentChannelIndex).apply()
    }

    // Lectura del listado m3u
    private fun loadChannels(onChannelsLoaded: () -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://m3u.cl/lista/AR.m3u")
                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val lines = mutableListOf<String>()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.startsWith("http")) { // Filter out non-URL lines
                        lines.add(line!!)
                    }
                }
                channels = lines
                connection.disconnect()

                onChannelsLoaded()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Reproduccion del streaming en primer plano
    private fun startVideo() {
        runOnUiThread {
            videoView = findViewById(R.id.videoview)
            val videoUri = Uri.parse(channels[currentChannelIndex])
            videoView.setVideoURI(videoUri)
            videoView.start()
        }
    }

    // Configuracion de los botones del control remoto
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
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    if (timer != null) {
                        timer?.cancel()
                        timer = null
                        showToast("Publicidad completa desactivada")
                        sharedPreferences.edit().putBoolean("timerActive", false).apply()
                    } else {
                        startTimer()
                        showToast("Publicidad completa activada")
                        sharedPreferences.edit().putBoolean("timerActive", true).apply()
                    }
                    return true
                }
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

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this@Streaming, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Configuracion de la publicidad en streaming
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

    // Timer para la publicidad completa
    private fun startTimer() {
        if (timer == null) {
            timer = Timer()
            val task = object : TimerTask() {
                override fun run() {
                    runOnUiThread {
                        val intent = Intent(this@Streaming, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
            }
            timer?.schedule(task, 60 * 1000)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        timer?.cancel()
        timer = null
    }
}