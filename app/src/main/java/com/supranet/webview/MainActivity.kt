package com.supranet.webview

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var sharedPreferences: SharedPreferences
    private val actionBarThreshold = 200
    private var startY: Float = 0f
    val handler = Handler()
    val delayMillis = 1000 // 1 segundos

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
        setTheme(R.style.Theme_Webview)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        webView.webViewClient = WebViewClient()
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Configurar WebView
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true
        webSettings.domStorageEnabled = true
        webSettings.useWideViewPort = true

        // Cargar URL
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val urlPreference = sharedPrefs.getString("url_preference", "http://www.supranet.ar")
        webView.loadUrl(urlPreference.toString())

        // Cargar URL local
        val loadLocalHtml = sharedPreferences.getBoolean("enable_local", false)
        if (loadLocalHtml) {
            val file = File(getExternalFilesDir(null), "index.html")
            if (!file.exists()) {
                Toast.makeText(this, "El archivo HTML no existe!", Toast.LENGTH_SHORT).show()
                return
            }
            webView.loadDataWithBaseURL("file://${file.parent}/", file.readText(), "text/html", "UTF-8", null)
        }

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
                MotionEvent.ACTION_MOVE -> {
                    val endY = event.y // Registrar la posición final del dedo
                    val showActionBar = sharedPreferences.getBoolean("show_toolbar", true)
                    supportActionBar?.let {
                        if (showActionBar && endY - startY > 0 && event.rawY < actionBarThreshold) { // Verificar que el usuario deslizó el dedo hacia abajo cerca del borde superior
                            it.show()
                        } else {
                            it.hide()
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    handler.postDelayed({
                        supportActionBar?.hide()
                    }, delayMillis.toLong())
                }
            }
            false
        }
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        //startActivity(Intent.createChooser(intent, "Selecciona una aplicación de inicio"))
    }
}