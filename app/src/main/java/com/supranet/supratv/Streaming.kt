package com.supranet.supratv

import android.content.*
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewClientCompat
import com.tapadoo.alerter.Alerter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class Streaming : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var webView: android.webkit.WebView
    private var webViewVisible = false
    private var isWebViewEnabled = true
    private var currentChannelIndex = 0
    private var channels: List<String> = emptyList()
    private var urls: MutableList<String> = mutableListOf()
    private lateinit var sharedPreferences: SharedPreferences
    private var scheduledExecutorService: ScheduledExecutorService? = null
    private var scheduledFuture: ScheduledFuture<*>? = null
    private var connectivityReceiver: ConnectivityReceiver? = null

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

        // Activa la publicidad base por defecto
        baseAdvertising()

        // Chequeo del timer para la publicidad mixta
        if (timerActive) {
            mixedAdvertising()
        }

        // Receptor de conectividad
        connectivityReceiver = ConnectivityReceiver()
        val filter = IntentFilter()
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(connectivityReceiver, filter)

        // Verificar la conexión a Internet
        checkInternetConnection()

        // Verificacion del ciclo de vida de la app
        val disableAdvertisingIntent = intent.getBooleanExtra("disableAdvertisingIntent", false)
        if (disableAdvertisingIntent) {
            disableAlmostAdvertising()
            showToast("Publicidad completa desactivada. Pulse el botón número 4 para desactivar la publicidad base.")
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
                showToastChannel("El canal anterior no se encontraba disponible.")
                switchToNextChannel()
                true
            }
            videoView.start()
        }
    }

    // Libreria Alerter para notificaciones de canales
    private fun showToastChannel(message: String) {
        runOnUiThread {
            Alerter.create(this)
                .setTitle(message)
                .setIcon(R.drawable.supranet)
                .setTitleAppearance(R.style.AlerterTitleTextAppearance)
                .setIconSize(R.dimen.custom_icon_size)
                .setBackgroundColorRes(R.color.md_theme_light_outline)
                .show()
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
                        disableAllAdvertising()
                        baseAdvertising()
                        showToast("Publicidad base activada.")
                    return true
                }
            }
            KeyEvent.KEYCODE_2 -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                        disableAllAdvertising()
                        mixedAdvertising()
                        showToast("Publicidad mixta activada.")
                        sharedPreferences.edit().putBoolean("timerActive", true).apply()
                    return true
                }
            }
            KeyEvent.KEYCODE_3 -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    disableAllAdvertising()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    return true
                }
            }
            KeyEvent.KEYCODE_4 -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    showToast("Todos los modos de publicidad han sido desactivados.")
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

    // Libreria Alerter para notificaciones de publicidades
    private fun showToast(message: String) {
        runOnUiThread {
            Alerter.create(this)
                .setTitle(message)
                .setIcon(R.drawable.supranet)
                .setTitleAppearance(R.style.AlerterTitleTextAppearance)
                .setLayoutGravity(Gravity.BOTTOM)
                .setIconSize(R.dimen.custom_icon_size)
                .setBackgroundColorRes(R.color.md_theme_light_outline)
                .show()
        }
    }

    // Logica de publicidad base
    private var currentUrlIndex = 0

    private fun baseAdvertising() {
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
                    scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
                    scheduledFuture = scheduledExecutorService?.scheduleAtFixedRate({
                        if (isWebViewEnabled) {
                            runOnUiThread {
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
                        }
                    }, 0, 35, TimeUnit.SECONDS)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopBaseAdvertising() {
        scheduledFuture?.cancel(true)
        scheduledExecutorService?.shutdown()
        webView.visibility = View.GONE
        webViewVisible = false
    }

    // Logica de publicidad mixta
    private fun mixedAdvertising() {
        stopMixedAdvertising()
        if (scheduledExecutorService == null) {
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
            scheduledExecutorService?.schedule({
                runOnUiThread {
                    val intent = Intent(this@Streaming, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }, 60, TimeUnit.SECONDS)
        }
    }

    private fun stopMixedAdvertising() {
        scheduledFuture?.cancel(true)
        scheduledExecutorService?.shutdownNow()
        scheduledExecutorService = null
    }

    // Logica para desactivar las publicidades en diferentes estados
    private fun disableAllAdvertising() {
        sharedPreferences.edit().putBoolean("timerActive", false).apply()
        stopBaseAdvertising()
        stopMixedAdvertising()
    }

    private fun disableAlmostAdvertising() {
        sharedPreferences.edit().putBoolean("timerActive", false).apply()
        stopMixedAdvertising()
    }

    private fun checkInternetConnection() {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        val isConnected = networkInfo != null && networkInfo.isConnectedOrConnecting
        if (!isConnected) {
            Alerter.create(this)
                .setTitle("No hay conexión a Internet. Reintentando...")
                .enableInfiniteDuration(true)
                .setIcon(R.drawable.supranet)
                .setTitleAppearance(R.style.AlerterTitleTextAppearance)
                .setIconSize(R.dimen.custom_icon_size)
                .setBackgroundColorRes(R.color.md_theme_light_outline)
                .show()
        }
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
        stopMixedAdvertising()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMixedAdvertising()
        stopBaseAdvertising()
        connectivityReceiver?.let {
            unregisterReceiver(it)
        }
    }

    // Clase para obtener cambios en la conectividad
    inner class ConnectivityReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
                val connectivityManager =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val networkInfo = connectivityManager.activeNetworkInfo
                val isConnected = networkInfo != null && networkInfo.isConnectedOrConnecting
                if (isConnected) {
                    Alerter.hide()
                    videoView.stopPlayback()
                    startVideo()
                    webView.reload()
                } else {
                    checkInternetConnection()
                }
            }
        }
    }
}