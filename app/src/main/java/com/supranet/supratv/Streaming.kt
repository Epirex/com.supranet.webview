package com.supranet.supratv

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
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
    private var isWebViewEnabled = true
    private var currentChannelIndex = 0
    private var channels: List<String> = emptyList()
    private var urls: MutableList<String> = mutableListOf()
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
        val color: Int = Color.parseColor("#D50002")
        webView.setBackgroundColor(color)

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

        // Activa la publicidad minima por defecto
        minimalAdvertising()

        // Chequeo del timer para la publicidad parcial
        if (timerActive) {
            partialAdvertising()
        }
    }


    // Lectura del listado m3u
    private fun loadChannels(onChannelsLoaded: () -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://supranet.ar/webview/elnegrito/negrito.m3u")
                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val lines = mutableListOf<String>()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.startsWith("http")) {
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
            videoView.setOnErrorListener { _, _, _ ->
                showToastChannel("El canal anterior no estaba disponible", Toast.LENGTH_LONG)
                switchToNextChannel()
                true
            }
            videoView.start()
        }
    }

    private fun showToastChannel(message: String, duration: Int) {
        runOnUiThread {
            Toast.makeText(this@Streaming, message, duration).show()
        }
    }

    // Configuracion de los botones del control remoto
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                switchToNextChannel()
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                switchToPreviousChannel()
                return true
            }
            KeyEvent.KEYCODE_1 -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    isWebViewEnabled = !isWebViewEnabled
                    showToast("Publicidad minima ${if (isWebViewEnabled) "activada" else "desactivada"}")
                    if (!isWebViewEnabled && webViewVisible) {
                        webView.visibility = View.GONE
                        webViewVisible = false
                    }
                    return true
                }
            }
            KeyEvent.KEYCODE_2 -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    if (timer != null) {
                        timer?.cancel()
                        timer = null
                        showToast("Publicidad parcial desactivada")
                        sharedPreferences.edit().putBoolean("timerActive", false).apply()
                    } else {
                        partialAdvertising()
                        showToast("Publicidad parcial activada")
                        sharedPreferences.edit().putBoolean("timerActive", true).apply()
                    }
                    return true
                }
            }
            KeyEvent.KEYCODE_3 -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    showToast("Publicidad completa activada")
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    return true
                }
            }
            KeyEvent.KEYCODE_4 -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    showToast("Todos los modos de publicidad han sido desactivados")
                    disableAllAdvertising()
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
    private var currentUrlIndex = 0

    private fun minimalAdvertising() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://supranet.ar/webview/elnegrito/urlstvbar.txt")
                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val lines = mutableListOf<String>()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    lines.add(line!!)
                }
                urls = lines
                connection.disconnect()

                runOnUiThread {
                    handler.postDelayed({
                        if (isWebViewEnabled) {
                            if (webViewVisible) {
                                webView.visibility = View.GONE
                            } else {
                                webView.visibility = View.VISIBLE
                                if (currentUrlIndex < urls.size) {
                                    webView.loadUrl(urls[currentUrlIndex])
                                    currentUrlIndex = (currentUrlIndex + 1) % urls.size
                                }
                            }
                            webViewVisible = !webViewVisible
                        }
                        minimalAdvertising()
                    }, 35 * 1000)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Timer para la publicidad parcial
    private fun partialAdvertising() {
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

    private fun disableAllAdvertising() {
        timer?.cancel()
        timer = null
        webView.visibility = View.GONE
        webViewVisible = false
    }

    override fun onResume() {
        super.onResume()
        if (!::videoView.isInitialized) {
            videoView = findViewById(R.id.videoview)
        }
        videoView.start()
    }

    override fun onPause() {
        super.onPause()
        videoView.pause()
        sharedPreferences.edit().putInt("currentChannelIndex", currentChannelIndex).apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        timer?.cancel()
        timer = null
    }
}