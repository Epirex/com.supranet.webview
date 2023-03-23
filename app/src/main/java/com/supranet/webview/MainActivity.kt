package com.supranet.webview

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.webkit.WebChromeClient
import androidx.appcompat.app.AppCompatActivity
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.preference.PreferenceManager

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var sharedPreferences: SharedPreferences
    private var startY: Float = 0f
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        webView.webViewClient = WebViewClient()
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Configurar WebView
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true

        // Cargar URL
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val urlPreference = sharedPrefs.getString("url_preference", "http://www.supranet.ar")
        webView.loadUrl(urlPreference.toString())

        // check toolbar
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val hideToolbarPref = prefs.getBoolean("hide_toolbar", false)
        supportActionBar?.apply {
            if (hideToolbarPref) {
                hide()
            } else {
                show()
            }
        }

        // Agregar el listener onTouch para mostrar u ocultar la ActionBar
        webView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.y // Registrar la posición inicial del dedo
                }
                MotionEvent.ACTION_UP -> {
                    val endY = event.y // Registrar la posición final del dedo
                    val showActionBar = sharedPreferences.getBoolean("show_toolbar", true)
                    supportActionBar?.let {
                        if (showActionBar && endY - startY > 0) { // Comparar las posiciones para determinar si el usuario deslizó el dedo hacia abajo
                            it.show()
                        } else {
                            it.hide()
                        }
                    }
                }
            }
            false
        }

        // Pantalla completa
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val fullscreenPreference = preferences.getBoolean("fullscreen", false)

        if (preferences.getBoolean("fullscreen", false)) {
            // Habilita la pantalla completa
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        } else {
            // Deshabilita la pantalla completa si la opción no está marcada
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }
}