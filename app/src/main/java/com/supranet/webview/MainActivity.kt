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

        fun hideSystemUI() {
            // Enables regular immersive mode.
            // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
            // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                    // Set the content to appear under the system bars so that the
                    // content doesn't resize when the system bars hide and show.
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    // Hide the nav bar and status bar
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }

        // Shows the system bars by removing all the flags
        // except for the ones that make the content appear under the system bars.
        fun showSystemUI() {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }

        // Habilita o deshabilita el modo pantalla completa
        if (fullscreenPreference) {
            hideSystemUI()
        } else {
            showSystemUI()
        }
    }
}